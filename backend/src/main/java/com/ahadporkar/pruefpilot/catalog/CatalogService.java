package com.ahadporkar.pruefpilot.catalog;

import java.util.*;
import com.ahadporkar.pruefpilot.api.Commands;
import com.ahadporkar.pruefpilot.auth.Actor;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.shared.*;
import com.ahadporkar.pruefpilot.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.ahadporkar.pruefpilot.shared.Db.*;

@Service
public class CatalogService {
    public static final String ASSET_COLUMNS="id,site_id,code,name,category,manufacturer,model,serial_number,location_note,criticality,archived,created_at";
    public static final String TEMPLATE_COLUMNS="id,name,name_de,description,version,status,reviewed_by,reviewed_at,created_at";
    public static final String ITEM_COLUMNS="id,template_id,position,label,label_de,kind,required,min_value,max_value,unit";
    public static final String SCHEDULE_COLUMNS="id,asset_id,template_id,owner_id,interval_days,next_due,reason,reviewed_by,active,created_at";
    private final Db db; private final AuditService audit;
    public CatalogService(Db db,AuditService audit) {this.db=db;this.audit=audit;}
    public Map<String,Object> asset(Actor actor,UUID id) {return db.one("SELECT "+ASSET_COLUMNS+" FROM asset WHERE organization_id=? AND id=?",actor.organizationId(),id);}
    public Map<String,Object> template(Actor actor,UUID id) {return db.one("SELECT "+TEMPLATE_COLUMNS+" FROM inspection_template WHERE organization_id=? AND id=?",actor.organizationId(),id);}
    public List<Map<String,Object>> items(Actor actor,UUID templateId) {return db.list("SELECT "+ITEM_COLUMNS+" FROM template_item WHERE organization_id=? AND template_id=? ORDER BY position",actor.organizationId(),templateId);}
    public void activeMember(Actor actor,UUID id) {
        if(db.count("SELECT count(*) FROM app_user WHERE organization_id=? AND id=? AND active",actor.organizationId(),id)!=1) throw ApiException.missing();
    }
    @Transactional public Map<String,Object> createSite(Actor actor,Commands.Site input) {
        actor.require("ADMIN","SUPERVISOR");UUID id=UUID.randomUUID();
        db.update("INSERT INTO site(id,organization_id,name,code,address) VALUES(?,?,?,?,?)",id,actor.organizationId(),input.name().strip(),input.code().toUpperCase(Locale.ROOT),input.address().strip());
        audit.record(actor,"SITE",id,"SITE_CREATED",input.code()); return Map.of("id",id);
    }
    @Transactional public Map<String,Object> createAsset(Actor actor,Commands.Asset input) {
        actor.require("ADMIN","SUPERVISOR");
        // Lock the organization row so concurrent requests cannot exceed the commercial cap.
        db.one("SELECT id FROM organization WHERE id=? FOR UPDATE",actor.organizationId());
        if(db.count("SELECT count(*) FROM asset WHERE organization_id=? AND NOT archived",actor.organizationId())>=250)
            throw new RuleViolation("assetLimit","This deployment supports 250 active assets. Archive unused assets first.");
        db.one("SELECT id FROM site WHERE organization_id=? AND id=?",actor.organizationId(),input.siteId());
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO asset(id,organization_id,site_id,code,name,category,manufacturer,model,serial_number,location_note,criticality) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            id,actor.organizationId(),input.siteId(),input.code().toUpperCase(Locale.ROOT),input.name().strip(),input.category().strip(),empty(input.manufacturer()),empty(input.model()),empty(input.serialNumber()),empty(input.locationNote()),input.criticality());
        audit.record(actor,"ASSET",id,"ASSET_CREATED",input.code());return asset(actor,id);
    }
    @Transactional public void archiveAsset(Actor actor,UUID id) {
        actor.require("ADMIN","SUPERVISOR");db.one("SELECT id FROM asset WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),id);
        if(db.count("SELECT count(*) FROM finding WHERE organization_id=? AND asset_id=? AND status<>'CLOSED'",actor.organizationId(),id)>0 ||
           db.count("SELECT count(*) FROM inspection_schedule WHERE organization_id=? AND asset_id=? AND active",actor.organizationId(),id)>0 ||
           db.count("SELECT count(*) FROM inspection WHERE organization_id=? AND asset_id=? AND status IN ('DRAFT','SUBMITTED')",actor.organizationId(),id)>0)
            throw new RuleViolation("openWork","Resolve outstanding work and pause schedules before archiving.");
        db.update("UPDATE asset SET archived=true WHERE organization_id=? AND id=?",actor.organizationId(),id);audit.record(actor,"ASSET",id,"ASSET_ARCHIVED","");
    }
    @Transactional public Map<String,Object> createTemplate(Actor actor,Commands.Template input) {
        actor.require("ADMIN","SUPERVISOR");UUID id=UUID.randomUUID();
        db.update("INSERT INTO inspection_template(id,organization_id,name,name_de,description,version) VALUES(?,?,?,?,?,?)",id,actor.organizationId(),input.name().strip(),input.nameDe().strip(),empty(input.description()),input.version());
        int position=0;
        for(var item:input.items()) {
            if(item.minValue()!=null && item.maxValue()!=null && item.minValue().compareTo(item.maxValue())>0) throw new RuleViolation("validation","Minimum must not exceed maximum.");
            if(!item.kind().equals("NUMBER") && (item.minValue()!=null || item.maxValue()!=null)) throw new RuleViolation("validation","Only numeric checks can have a range.");
            db.update("INSERT INTO template_item(id,organization_id,template_id,position,label,label_de,kind,required,min_value,max_value,unit) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(),actor.organizationId(),id,position++,item.label().strip(),item.labelDe().strip(),item.kind(),item.required(),item.minValue(),item.maxValue(),empty(item.unit()));
        }
        audit.record(actor,"TEMPLATE",id,"TEMPLATE_CREATED",input.name()+" v"+input.version());return template(actor,id);
    }
    @Transactional public void reviewTemplate(Actor actor,UUID id,String note) {
        actor.require("ADMIN","SUPERVISOR");
        var current=db.one("SELECT status FROM inspection_template WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),id);
        InspectionRules.requireState(str(current,"status"),"DRAFT");
        if(note==null||note.strip().length()<5) throw new RuleViolation("noteRequired","Document the review basis.");
        db.update("UPDATE inspection_template SET status='REVIEWED',reviewed_by=?,reviewed_at=now() WHERE organization_id=? AND id=?",actor.id(),actor.organizationId(),id);
        audit.record(actor,"TEMPLATE",id,"TEMPLATE_REVIEWED",note);
    }
    @Transactional public Map<String,Object> createSchedule(Actor actor,Commands.Schedule input) {
        actor.require("ADMIN","SUPERVISOR");ScheduleRules.validate(input.intervalDays(),input.reason());
        var asset=db.one("SELECT "+ASSET_COLUMNS+" FROM asset WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),input.assetId());if(bool(asset,"archived")) throw new RuleViolation("archived","Archived assets cannot be scheduled.");
        InspectionRules.requireState(str(template(actor,input.templateId()),"status"),"REVIEWED");activeMember(actor,input.ownerId());
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO inspection_schedule(id,organization_id,asset_id,template_id,owner_id,interval_days,next_due,reason,reviewed_by) VALUES(?,?,?,?,?,?,?,?,?)",
            id,actor.organizationId(),input.assetId(),input.templateId(),input.ownerId(),input.intervalDays(),input.nextDue(),input.reason().strip(),actor.id());
        audit.record(actor,"SCHEDULE",id,"SCHEDULE_CREATED",input.reason());return Map.of("id",id);
    }
    @Transactional public void pauseSchedule(Actor actor,UUID id) {
        actor.require("ADMIN","SUPERVISOR");db.one("SELECT id FROM inspection_schedule WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),id);
        if(db.count("SELECT count(*) FROM inspection WHERE organization_id=? AND schedule_id=? AND status IN ('DRAFT','SUBMITTED')",actor.organizationId(),id)>0) throw new RuleViolation("openWork","Finish the current inspection before pausing this schedule.");
        db.update("UPDATE inspection_schedule SET active=false WHERE organization_id=? AND id=?",actor.organizationId(),id);audit.record(actor,"SCHEDULE",id,"SCHEDULE_PAUSED","");
    }
    private static String empty(String value) {return value==null?"":value.strip();}
}
