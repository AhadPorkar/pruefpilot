package com.ahadporkar.pruefpilot.audit;

import java.util.*;
import com.ahadporkar.pruefpilot.auth.Actor;
import com.ahadporkar.pruefpilot.shared.Db;
import org.springframework.stereotype.Service;

/** Called within the surrounding business transaction, so state and audit commit together. */
@Service
public class AuditService {
    private final Db db;
    public AuditService(Db db) {this.db=db;}
    public void record(Actor actor,String type,UUID id,String action,String detail) {
        db.update("INSERT INTO audit_event(id,organization_id,actor_id,entity_type,entity_id,action,detail) VALUES(?,?,?,?,?,?,?)",
            UUID.randomUUID(),actor.organizationId(),actor.id(),type,id,action,detail);
    }
    public List<Map<String,Object>> list(Actor actor,int page) {
        return db.list("SELECT e.id,e.actor_id,u.display_name AS actor_name,e.entity_type,e.entity_id,e.action,e.detail,e.occurred_at FROM audit_event e LEFT JOIN app_user u ON u.id=e.actor_id AND u.organization_id=e.organization_id WHERE e.organization_id=? ORDER BY e.occurred_at DESC,e.id LIMIT 100 OFFSET ?",actor.organizationId(),Math.max(0,page)*100);
    }
}
