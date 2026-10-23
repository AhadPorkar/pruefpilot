package com.ahadporkar.pruefpilot.api;

import java.time.*;
import java.util.*;
import com.ahadporkar.pruefpilot.auth.Actor;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.inspection.InspectionService;
import com.ahadporkar.pruefpilot.action.ActionService;
import com.ahadporkar.pruefpilot.evidence.EvidenceService;
import com.ahadporkar.pruefpilot.shared.Db;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.ahadporkar.pruefpilot.shared.Db.*;

@Service
public class WorkspaceService {
    private final Db db;private final AuditService audit;private final CatalogService catalog;private final Clock clock;private final boolean demo;
    public WorkspaceService(Db db,AuditService audit,CatalogService catalog,Clock clock,@Value("${app.demo}") boolean demo) {this.db=db;this.audit=audit;this.catalog=catalog;this.clock=clock;this.demo=demo;}
    @Transactional(readOnly=true)
    public Map<String,Object> workspace(Actor actor) {
        UUID org=actor.organizationId();var state=new LinkedHashMap<String,Object>();
        var organization=db.one("SELECT id,name,timezone FROM organization WHERE id=?",org);
        state.put("organization",organization);state.put("user",actor.publicView());state.put("demo",demo);
        state.put("today",LocalDate.now(clock.withZone(ZoneId.of(str(organization,"timezone")))).toString());
        state.put("serverNow",clock.instant().toString());
        state.put("sites",db.list("SELECT id,name,code,address FROM site WHERE organization_id=? ORDER BY name LIMIT 500",org));
        state.put("assets",db.list("SELECT "+CatalogService.ASSET_COLUMNS+" FROM asset WHERE organization_id=? ORDER BY archived,code LIMIT 1000",org));
        state.put("templates",db.list("SELECT "+CatalogService.TEMPLATE_COLUMNS+" FROM inspection_template WHERE organization_id=? ORDER BY created_at DESC LIMIT 500",org));
        state.put("templateItems",db.list("SELECT "+CatalogService.ITEM_COLUMNS+" FROM template_item WHERE organization_id=? ORDER BY template_id,position LIMIT 5000",org));
        state.put("schedules",db.list("SELECT "+CatalogService.SCHEDULE_COLUMNS+" FROM inspection_schedule WHERE organization_id=? ORDER BY active DESC,next_due LIMIT 2000",org));
        state.put("inspections",db.list("SELECT "+InspectionService.COLUMNS+" FROM inspection WHERE organization_id=? ORDER BY created_at DESC LIMIT 500",org));
        state.put("answers",db.list("SELECT inspection_id,item_id,value,note,outcome FROM inspection_answer WHERE organization_id=? AND inspection_id IN (SELECT id FROM inspection WHERE organization_id=? ORDER BY created_at DESC LIMIT 500)",org,org));
        state.put("findings",db.list("SELECT "+ActionService.COLUMNS+" FROM finding WHERE organization_id=? ORDER BY (status='CLOSED'),created_at DESC LIMIT 1000",org));
        state.put("evidence",db.list("SELECT "+EvidenceService.COLUMNS+" FROM evidence WHERE organization_id=? ORDER BY created_at DESC LIMIT 1000",org));
        state.put("users",db.list("SELECT id,"+(actor.role().equals("ADMIN")?"email,":"")+"display_name,role,active FROM app_user WHERE organization_id=? ORDER BY display_name LIMIT 500",org));
        state.put("audit",audit.list(actor,0));
        state.put("historyTruncated",db.count("SELECT count(*) FROM inspection WHERE organization_id=?",org)>500 || db.count("SELECT count(*) FROM finding WHERE organization_id=?",org)>1000 || db.count("SELECT count(*) FROM evidence WHERE organization_id=?",org)>1000);
        return state;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> passport(Actor actor,UUID assetId,int page) {
        var result=new LinkedHashMap<String,Object>();result.put("asset",catalog.asset(actor,assetId));
        UUID org=actor.organizationId();int offset=Math.max(0,page)*100;
        var inspections=db.list("SELECT "+InspectionService.COLUMNS+" FROM inspection WHERE organization_id=? AND asset_id=? ORDER BY created_at DESC,id LIMIT 100 OFFSET ?",org,assetId,offset);
        result.put("inspections",inspections);result.put("page",page);
        result.put("hasMore",db.count("SELECT count(*) FROM inspection WHERE organization_id=? AND asset_id=?",org,assetId)>offset+100);
        result.put("answers",db.list("SELECT inspection_id,item_id,value,note,outcome FROM inspection_answer WHERE organization_id=? AND inspection_id IN (SELECT id FROM inspection WHERE organization_id=? AND asset_id=? ORDER BY created_at DESC,id LIMIT 100 OFFSET ?)",org,org,assetId,offset));
        result.put("findings",db.list("SELECT "+ActionService.COLUMNS+" FROM finding WHERE organization_id=? AND asset_id=? ORDER BY created_at DESC LIMIT 1000",org,assetId));
        result.put("evidence",db.list("SELECT "+EvidenceService.COLUMNS+" FROM evidence WHERE organization_id=? AND asset_id=? ORDER BY created_at DESC LIMIT 1000",org,assetId));
        result.put("audit",db.list("SELECT e.id,e.actor_id,u.display_name AS actor_name,e.entity_type,e.entity_id,e.action,e.detail,e.occurred_at FROM audit_event e LEFT JOIN app_user u ON u.id=e.actor_id AND u.organization_id=e.organization_id WHERE e.organization_id=? AND (e.entity_id=? OR e.entity_id IN(SELECT id FROM inspection WHERE organization_id=? AND asset_id=?) OR e.entity_id IN(SELECT id FROM finding WHERE organization_id=? AND asset_id=?) OR e.entity_id IN(SELECT id FROM evidence WHERE organization_id=? AND asset_id=?)) ORDER BY e.occurred_at DESC LIMIT 100",org,assetId,org,assetId,org,assetId,org,assetId));
        return result;
    }
}
