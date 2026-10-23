package com.ahadporkar.pruefpilot.action;

import java.util.*;
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
public class ActionService {
    public static final String COLUMNS="id,asset_id,inspection_id,item_id,title,description,severity,assignee_id,due_date,status,repair_note,completed_by,verified_by,verify_note,resolved_at,version,created_at";
    private final Db db;private final CatalogService catalog;private final AuditService audit;
    public ActionService(Db db,CatalogService catalog,AuditService audit) {this.db=db;this.catalog=catalog;this.audit=audit;}
    public Map<String,Object> get(Actor actor,UUID id) {return db.one("SELECT "+COLUMNS+" FROM finding WHERE organization_id=? AND id=?",actor.organizationId(),id);}
    private Map<String,Object> lock(Actor actor,UUID id,int version) {
        var row=db.one("SELECT "+COLUMNS+" FROM finding WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),id);requireVersion(number(row,"version"),version);return row;
    }
    @Transactional public Map<String,Object> assign(Actor actor,UUID id,Commands.Assign input) {
        actor.require("ADMIN","SUPERVISOR");var current=lock(actor,id,input.version());
        if(!List.of("OPEN","IN_PROGRESS").contains(str(current,"status"))) fail("invalidTransition","Only open work can be assigned.");
        catalog.activeMember(actor,input.assigneeId());
        db.one("SELECT id FROM app_user WHERE organization_id=? AND id=? AND role IN ('ADMIN','SUPERVISOR','TECHNICIAN')",actor.organizationId(),input.assigneeId());
        db.update("UPDATE finding SET assignee_id=?,due_date=?,severity=?,version=version+1 WHERE organization_id=? AND id=?",input.assigneeId(),input.dueDate(),input.severity(),actor.organizationId(),id);
        audit.record(actor,"FINDING",id,"FINDING_ASSIGNED",input.assigneeId()+" / "+input.dueDate());return get(actor,id);
    }
    public void assertRepairer(Actor actor,Map<String,Object> finding) {
        actor.require("ADMIN","SUPERVISOR","TECHNICIAN");
        if(!Objects.equals(uuid(finding,"assigneeId"),actor.id())) throw new AccessDeniedException("Only the assigned person can document repair.");
    }
    @Transactional public Map<String,Object> start(Actor actor,UUID id,int version) {
        var current=lock(actor,id,version);assertRepairer(actor,current);requireState(str(current,"status"),"OPEN");
        db.update("UPDATE finding SET status='IN_PROGRESS',version=version+1 WHERE organization_id=? AND id=?",actor.organizationId(),id);
        audit.record(actor,"FINDING",id,"REPAIR_STARTED","");return get(actor,id);
    }
    @Transactional public Map<String,Object> complete(Actor actor,UUID id,Commands.Repair input) {
        var current=lock(actor,id,input.version());assertRepairer(actor,current);
        int evidence=(int)db.count("SELECT count(*) FROM evidence WHERE organization_id=? AND finding_id=?",actor.organizationId(),id);
        ActionRules.mayComplete(str(current,"status"),input.note(),evidence);
        db.update("UPDATE finding SET status='AWAITING_VERIFICATION',repair_note=?,completed_by=?,version=version+1 WHERE organization_id=? AND id=?",input.note().strip(),actor.id(),actor.organizationId(),id);
        audit.record(actor,"FINDING",id,"REPAIR_DOCUMENTED",input.note());return get(actor,id);
    }
    @Transactional public Map<String,Object> verify(Actor actor,UUID id,Commands.Review input) {
        actor.require("ADMIN","SUPERVISOR");var current=lock(actor,id,input.version());
        int evidence=(int)db.count("SELECT count(*) FROM evidence WHERE organization_id=? AND finding_id=?",actor.organizationId(),id);
        ActionRules.mayVerify(str(current,"status"),str(current,"completedBy"),actor.id().toString(),input.note(),evidence);
        db.update("UPDATE finding SET status=?,verified_by=?,verify_note=?,resolved_at=CASE WHEN ? THEN now() ELSE NULL END,version=version+1 WHERE organization_id=? AND id=?",
            input.approved()?"CLOSED":"IN_PROGRESS",actor.id(),input.note().strip(),input.approved(),actor.organizationId(),id);
        audit.record(actor,"FINDING",id,input.approved()?"CLOSURE_VERIFIED":"REPAIR_REJECTED",input.note());return get(actor,id);
    }
}
