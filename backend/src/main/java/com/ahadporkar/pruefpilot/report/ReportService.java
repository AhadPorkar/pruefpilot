package com.ahadporkar.pruefpilot.report;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.zip.*;
import com.ahadporkar.pruefpilot.auth.Actor;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.inspection.InspectionService;
import com.ahadporkar.pruefpilot.action.ActionService;
import com.ahadporkar.pruefpilot.evidence.EvidenceService;
import com.ahadporkar.pruefpilot.shared.*;
import com.ahadporkar.pruefpilot.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import static com.ahadporkar.pruefpilot.shared.Db.*;
import static com.ahadporkar.pruefpilot.domain.ExportRules.*;

/** Customer handover package: printable HTML, CSV, JSON, original evidence and SHA-256 manifest.
 * Checksums detect changes relative to the manifest. They are NOT a digital signature. */
@Service
public class ReportService {
    private final Db db;private final CatalogService catalog;private final AuditService audit;private final JsonMapper json;private final Clock clock;
    private final Semaphore exportSlots=new Semaphore(2);
    public ReportService(Db db,CatalogService catalog,AuditService audit,JsonMapper json,Clock clock) {this.db=db;this.catalog=catalog;this.audit=audit;this.json=json;this.clock=clock;}
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public byte[] export(Actor actor,UUID assetId,String language) throws IOException {
        actor.require("ADMIN","SUPERVISOR","VIEWER");
        if(!Set.of("en","de").contains(language)) throw new ApiException(400,"validation","Choose en or de.");
        if(!exportSlots.tryAcquire()) throw new ApiException(429,"rateLimited","Two report exports are already running. Retry shortly.");
        try {
            var asset=catalog.asset(actor,assetId);
            var organization=db.one("SELECT name,timezone FROM organization WHERE id=?",actor.organizationId());
            var inspections=db.list("SELECT "+InspectionService.COLUMNS+" FROM inspection WHERE organization_id=? AND asset_id=? ORDER BY created_at",actor.organizationId(),assetId);
            var findings=db.list("SELECT "+ActionService.COLUMNS+" FROM finding WHERE organization_id=? AND asset_id=? ORDER BY created_at",actor.organizationId(),assetId);
            var evidence=db.list("SELECT "+EvidenceService.COLUMNS+" FROM evidence WHERE organization_id=? AND asset_id=? ORDER BY created_at",actor.organizationId(),assetId);
            var answers=db.list("SELECT a.inspection_id,a.item_id,t.label,t.label_de,t.kind,t.unit,a.value,a.note,a.outcome FROM inspection_answer a JOIN inspection i ON i.id=a.inspection_id AND i.organization_id=a.organization_id JOIN template_item t ON t.id=a.item_id AND t.organization_id=a.organization_id WHERE i.organization_id=? AND i.asset_id=? ORDER BY i.created_at,t.position",actor.organizationId(),assetId);
            Set<String> involved=new HashSet<>();
            for(var row:inspections) for(String key:List.of("inspectorId","reviewedBy")) if(row.get(key)!=null)involved.add(str(row,key));
            for(var row:findings) for(String key:List.of("assigneeId","completedBy","verifiedBy")) if(row.get(key)!=null)involved.add(str(row,key));
            for(var row:evidence) involved.add(str(row,"uploadedBy"));
            var people=db.list("SELECT id,display_name,role FROM app_user WHERE organization_id=? ORDER BY display_name",actor.organizationId()).stream().filter(row->involved.contains(str(row,"id"))).toList();
            var schedules=db.list("SELECT "+CatalogService.SCHEDULE_COLUMNS+" FROM inspection_schedule WHERE organization_id=? AND asset_id=?",actor.organizationId(),assetId);
            var templates=db.list("SELECT "+CatalogService.TEMPLATE_COLUMNS+" FROM inspection_template WHERE organization_id=? AND id IN (SELECT template_id FROM inspection WHERE organization_id=? AND asset_id=?)",actor.organizationId(),actor.organizationId(),assetId);
            String notice=language.equals("de")?"Betriebliche Dokumentation, kein Pruefzertifikat. Keine qualifizierte elektronische Signatur. Fristen und fachliche Eignung sind vom Betreiber zu pruefen.":"Operational record, not a certificate. No qualified electronic signature. The operator must validate inspection intervals and professional competence.";
            var payload=new LinkedHashMap<String,Object>();
            payload.put("schemaVersion",1);payload.put("generatedAt",clock.instant().toString());payload.put("language",language);payload.put("notice",notice);
            payload.put("organization",organization);payload.put("asset",asset);payload.put("inspections",inspections);payload.put("answers",answers);
            payload.put("findings",findings);payload.put("evidence",evidence);payload.put("people",people);payload.put("schedules",schedules);payload.put("templates",templates);
            var files=new LinkedHashMap<String,byte[]>();
            files.put("data.json",json.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload));
            files.put("inspections.csv",csvTable(inspections,List.of("id","status","inspectorId","performedAt","reviewedBy","reviewNote")));
            files.put("corrective-actions.csv",csvTable(findings,List.of("id","title","severity","status","dueDate","repairNote","verifyNote")));
            files.put("answers.csv",csvTable(answers,List.of("inspectionId","label","labelDe","kind","unit","value","note","outcome")));
            files.put("report.html",htmlReport(payload,language).getBytes(StandardCharsets.UTF_8));
            for(var file:evidence) files.put("evidence/"+str(file,"id")+"_"+str(file,"filename"),db.bytes("SELECT data FROM evidence WHERE organization_id=? AND id=?",actor.organizationId(),uuid(file,"id")));
            var entries=new ArrayList<Map<String,Object>>();
            files.forEach((path,bytes)->entries.add(Map.of("path",path,"bytes",bytes.length,"sha256",FileRules.sha256(bytes))));
            files.put("manifest.json",json.writerWithDefaultPrettyPrinter().writeValueAsBytes(Map.of("algorithm","SHA-256","files",entries,"notice","The manifest is not digitally signed and excludes itself.")));
            var output=new ByteArrayOutputStream();
            try(var zip=new ZipOutputStream(output,StandardCharsets.UTF_8)) {
                for(var file:files.entrySet()) {ZipEntry entry=new ZipEntry(file.getKey());entry.setTime(0);zip.putNextEntry(entry);zip.write(file.getValue());zip.closeEntry();}
            }
            audit.record(actor,"ASSET",assetId,"REPORT_EXPORTED",language+" | "+files.size()+" files");
            return output.toByteArray();
        } finally {exportSlots.release();}
    }
    static byte[] csvTable(List<Map<String,Object>> rows,List<String> columns) {
        StringBuilder text=new StringBuilder("\ufeff");text.append(String.join(",",columns)).append("\r\n");
        for(var row:rows) {text.append(String.join(",",columns.stream().map(key->csv(row.get(key))).toList())).append("\r\n");}
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }
    @SuppressWarnings("unchecked") private String htmlReport(Map<String,Object> report,String lang) {
        boolean de=lang.equals("de");var asset=(Map<String,Object>)report.get("asset");
        StringBuilder out=new StringBuilder("<!doctype html><html lang=\"").append(lang).append("\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"><title>PruefPilot | ").append(html(asset.get("code"))).append("</title><style>")
            .append("body{font:14px system-ui;color:#142b30;margin:40px auto;max-width:1100px;padding:24px}h1{font-size:38px}h2{margin-top:40px}table{border-collapse:collapse;width:100%;font-size:12px}td,th{padding:10px;border-bottom:1px solid #ddd;text-align:left;overflow-wrap:anywhere}th{background:#eaf4f0}aside{padding:18px;background:#fff4de}a{color:#087665}@media print{body{margin:0;padding:12px}tr{break-inside:avoid}}")
            .append("</style></head><body><p>PRUEFPILOT / ").append(de?"ANLAGENPASS":"ASSET PASSPORT").append("</p><h1>").append(html(asset.get("name"))).append("</h1><p>")
            .append(html(asset.get("code"))).append(" | ").append(html(report.get("generatedAt"))).append(" (UTC)</p><aside>").append(html(report.get("notice"))).append("</aside>");
        section(out,de?"Pruefungen":"Inspections",(List<Map<String,Object>>)report.get("inspections"),List.of("id","status","performedAt","reviewNote"),de?List.of("ID","Status","Durchgefuehrt","Pruefvermerk"):List.of("ID","Status","Performed","Review note"));
        section(out,de?"Pruefergebnisse":"Check results",(List<Map<String,Object>>)report.get("answers"),List.of("inspectionId",de?"labelDe":"label","value","unit","note","outcome"),de?List.of("Pruefung","Pruefpunkt","Wert","Einheit","Notiz","Ergebnis"):List.of("Inspection","Check","Value","Unit","Note","Outcome"));
        section(out,de?"Massnahmen":"Corrective actions",(List<Map<String,Object>>)report.get("findings"),List.of("title","severity","status","repairNote","verifyNote"),de?List.of("Titel","Prioritaet","Status","Reparatur","Verifizierung"):List.of("Title","Severity","Status","Repair","Verification"));
        section(out,de?"Beteiligte":"People",(List<Map<String,Object>>)report.get("people"),List.of("id","displayName","role"),de?List.of("ID","Name","Rolle"):List.of("ID","Name","Role"));
        out.append("<h2>").append(de?"Nachweise":"Evidence").append("</h2><ul>");
        for(var file:(List<Map<String,Object>>)report.get("evidence")) out.append("<li><a href=\"evidence/").append(html(file.get("id"))).append("_").append(html(file.get("filename"))).append("\">").append(html(file.get("filename"))).append("</a> <small>SHA-256: ").append(html(file.get("sha256"))).append("</small></li>");
        return out.append("</ul><footer>PruefPilot / Ahad Porkar / 1.0.0-rc.1</footer></body></html>").toString();
    }
    private static void section(StringBuilder out,String title,List<Map<String,Object>> rows,List<String> keys,List<String> labels) {
        out.append("<h2>").append(html(title)).append("</h2><table><thead><tr>");labels.forEach(label->out.append("<th>").append(html(label)).append("</th>"));out.append("</tr></thead><tbody>");
        for(var row:rows) {out.append("<tr>");keys.forEach(key->out.append("<td>").append(html(row.get(key))).append("</td>"));out.append("</tr>");}out.append("</tbody></table>");
    }
}
