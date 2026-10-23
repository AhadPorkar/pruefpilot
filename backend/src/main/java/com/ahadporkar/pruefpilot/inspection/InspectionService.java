package com.ahadporkar.pruefpilot.inspection;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import com.ahadporkar.pruefpilot.api.Commands;
import com.ahadporkar.pruefpilot.auth.Actor;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.shared.*;
import com.ahadporkar.pruefpilot.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.ahadporkar.pruefpilot.shared.Db.*;
import static com.ahadporkar.pruefpilot.domain.InspectionRules.*;

@Service
public class InspectionService {
    public static final String COLUMNS="id,asset_id,template_id,schedule_id,inspector_id,status,planned_due,performed_at,submitted_at,reviewed_by,review_note,version,created_at";
    private final Db db;private final CatalogService catalog;private final AuditService audit;private final Clock clock;
    public InspectionService(Db db,CatalogService catalog,AuditService audit,Clock clock) {this.db=db;this.catalog=catalog;this.audit=audit;this.clock=clock;}
    public Map<String,Object> get(Actor actor,UUID id) {return db.one("SELECT "+COLUMNS+" FROM inspection WHERE organization_id=? AND id=?",actor.organizationId(),id);}
    public List<Map<String,Object>> answers(Actor actor,UUID id) {get(actor,id);return db.list("SELECT inspection_id,item_id,value,note,outcome FROM inspection_answer WHERE organization_id=? AND inspection_id=?",actor.organizationId(),id);}
    private Map<String,Object> lock(Actor actor,UUID id) {return db.one("SELECT "+COLUMNS+" FROM inspection WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),id);}
    @Transactional public Map<String,Object> start(Actor actor,Commands.StartInspection input) {
        actor.require("ADMIN","SUPERVISOR","INSPECTOR");
        // Serialize requests carrying the same idempotency key without globally serializing inspections.
        db.one("SELECT pg_advisory_xact_lock(hashtextextended(?,0)) AS locked",actor.organizationId()+":"+input.requestKey());
        var existing=db.optional("SELECT "+COLUMNS+" FROM inspection WHERE organization_id=? AND request_key=?",actor.organizationId(),input.requestKey());
        if(existing.isPresent()) {
            var row=existing.get();
            if(!Objects.equals(uuid(row,"assetId"),input.assetId()) || !Objects.equals(uuid(row,"templateId"),input.templateId()) || !Objects.equals(uuid(row,"scheduleId"),input.scheduleId()) || !Objects.equals(uuid(row,"inspectorId"),actor.id()))
                throw new ApiException(409,"conflict","The request key was already used with different input.");
            return row;
        }
        if(bool(db.one("SELECT archived FROM asset WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),input.assetId()),"archived")) fail("archived","Archived assets cannot be inspected.");
        requireState(str(catalog.template(actor,input.templateId()),"status"),"REVIEWED");
        LocalDate planned=null;
        if(input.scheduleId()!=null) {
            var schedule=db.one("SELECT asset_id,template_id,next_due,active FROM inspection_schedule WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),input.scheduleId());
            if(!bool(schedule,"active") || !Objects.equals(uuid(schedule,"assetId"),input.assetId()) || !Objects.equals(uuid(schedule,"templateId"),input.templateId())) fail("validation","Schedule does not match the active asset and template.");
            planned=LocalDate.parse(str(schedule,"nextDue"));
        }
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO inspection(id,organization_id,asset_id,template_id,schedule_id,inspector_id,request_key,status,planned_due) VALUES(?,?,?,?,?,?,?,'DRAFT',?)",
            id,actor.organizationId(),input.assetId(),input.templateId(),input.scheduleId(),actor.id(),input.requestKey(),planned);
        audit.record(actor,"INSPECTION",id,"INSPECTION_STARTED","");return get(actor,id);
    }
    @Transactional public Map<String,Object> submit(Actor actor,UUID id,Commands.SubmitInspection input,boolean draft) {
        actor.require("ADMIN","SUPERVISOR","INSPECTOR");var current=lock(actor,id);
        if(!Objects.equals(uuid(current,"inspectorId"),actor.id())) throw new AccessDeniedException("Only the original inspector can submit.");
        requireState(str(current,"status"),"DRAFT");requireVersion(number(current,"version"),input.version());
        var items=catalog.items(actor,uuid(current,"templateId"));
        var submitted=new HashMap<UUID,Commands.Answer>();
        for(var answer:input.answers()) if(submitted.put(answer.itemId(),answer)!=null) fail("validation","Duplicate checklist item.");
        var known=items.stream().map(item->uuid(item,"id")).collect(Collectors.toSet());
        if(!known.containsAll(submitted.keySet())) fail("validation","An answer belongs to a different template.");
        if(!draft && submitted.size()!=items.size()) fail("answerRequired","Submit every checklist item, including optional empty items.");
        for(var item:items) {
            var answer=submitted.get(uuid(item,"id"));if(answer==null)continue;
            String value=answer.value().strip();String note=Objects.requireNonNullElse(answer.note(),"").strip();
            String outcome=draft?"NA":InspectionRules.outcome(new InspectionRules.Item(str(item,"kind"),bool(item,"required"),decimal(item.get("minValue")),decimal(item.get("maxValue"))),value,note);
            db.update("INSERT INTO inspection_answer(organization_id,inspection_id,item_id,value,note,outcome) VALUES(?,?,?,?,?,?) ON CONFLICT(inspection_id,item_id) DO UPDATE SET value=excluded.value,note=excluded.note,outcome=excluded.outcome",
                actor.organizationId(),id,answer.itemId(),value,note,outcome);
            if(!draft && outcome.equals("FAIL")) {
                UUID findingId=UUID.randomUUID();
                db.update("INSERT INTO finding(id,organization_id,asset_id,inspection_id,item_id,title,description,severity,status) VALUES(?,?,?,?,?,?,?,'MEDIUM','OPEN')",
                    findingId,actor.organizationId(),uuid(current,"assetId"),id,answer.itemId(),str(item,"label"),note);
                audit.record(actor,"FINDING",findingId,"FINDING_CREATED",str(item,"label"));
            }
        }
        if(draft) db.update("UPDATE inspection SET version=version+1 WHERE organization_id=? AND id=?",actor.organizationId(),id);
        else db.update("UPDATE inspection SET status='SUBMITTED',performed_at=?,submitted_at=?,version=version+1 WHERE organization_id=? AND id=?",clock.instant(),clock.instant(),actor.organizationId(),id);
        audit.record(actor,"INSPECTION",id,draft?"DRAFT_SAVED":"INSPECTION_SUBMITTED","");return get(actor,id);
    }
    @Transactional public Map<String,Object> review(Actor actor,UUID id,Commands.Review input) {
        actor.require("ADMIN","SUPERVISOR");var current=lock(actor,id);
        requireState(str(current,"status"),"SUBMITTED");requireVersion(number(current,"version"),input.version());
        assertIndependent(actor.id().toString(),str(current,"inspectorId"));
        db.update("UPDATE inspection SET status=?,reviewed_by=?,review_note=?,version=version+1 WHERE organization_id=? AND id=?",input.approved()?"VERIFIED":"REJECTED",actor.id(),input.note().strip(),actor.organizationId(),id);
        if(input.approved() && uuid(current,"scheduleId")!=null) {
            var schedule=db.one("SELECT next_due,interval_days FROM inspection_schedule WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),uuid(current,"scheduleId"));
            LocalDate planned=LocalDate.parse(str(current,"plannedDue"));
            if(!planned.equals(LocalDate.parse(str(schedule,"nextDue")))) throw new ApiException(409,"conflict","The schedule changed during inspection.");
            String zone=str(db.one("SELECT timezone FROM organization WHERE id=?",actor.organizationId()),"timezone");
            LocalDate performed=Instant.parse(str(current,"performedAt")).atZone(ZoneId.of(zone)).toLocalDate();
            LocalDate next=ScheduleRules.nextDue(planned,number(schedule,"intervalDays"),performed);
            db.update("UPDATE inspection_schedule SET next_due=? WHERE organization_id=? AND id=?",next,actor.organizationId(),uuid(current,"scheduleId"));
            audit.record(actor,"SCHEDULE",uuid(current,"scheduleId"),"SCHEDULE_ADVANCED",next.toString());
        }
        audit.record(actor,"INSPECTION",id,input.approved()?"INSPECTION_VERIFIED":"INSPECTION_REJECTED",input.note());return get(actor,id);
    }
    private static BigDecimal decimal(Object value) {return value==null?null:new BigDecimal(value.toString());}
}
