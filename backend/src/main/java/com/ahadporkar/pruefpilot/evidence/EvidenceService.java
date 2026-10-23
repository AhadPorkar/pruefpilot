package com.ahadporkar.pruefpilot.evidence;

import java.io.IOException;
import java.util.*;
import com.ahadporkar.pruefpilot.auth.Actor;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.action.ActionService;
import com.ahadporkar.pruefpilot.shared.*;
import com.ahadporkar.pruefpilot.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import static com.ahadporkar.pruefpilot.shared.Db.*;
import static com.ahadporkar.pruefpilot.domain.InspectionRules.*;

/** Transactional bytea storage keeps metadata and content atomic. Bounded for this v1 product. */
@Service
public class EvidenceService {
    public static final String COLUMNS="id,asset_id,inspection_id,finding_id,uploaded_by,filename,mime,size,sha256,created_at";
    public static final long ASSET_QUOTA=50L*1024*1024;
    private final Db db;private final AuditService audit;private final ActionService actions;
    public EvidenceService(Db db,AuditService audit,ActionService actions){this.db=db;this.audit=audit;this.actions=actions;}
    public Map<String,Object> metadata(Actor actor,UUID id) {return db.one("SELECT "+COLUMNS+" FROM evidence WHERE organization_id=? AND id=?",actor.organizationId(),id);}
    public byte[] download(Actor actor,UUID id) {metadata(actor,id);return db.bytes("SELECT data FROM evidence WHERE organization_id=? AND id=?",actor.organizationId(),id);}
    @Transactional public Map<String,Object> upload(Actor actor,UUID parent,boolean finding,MultipartFile file) throws IOException {
        if(file.getSize()>FileRules.MAX_BYTES) throw new ApiException(413,"fileSize","Maximum file size is 10 MiB.");
        return store(actor,parent,finding,file.getOriginalFilename(),file.getBytes());
    }
    @Transactional public Map<String,Object> store(Actor actor,UUID parent,boolean finding,String filename,byte[] data) {
        UUID inspectionId=null,findingId=null,assetId;
        if(finding) {
            var row=db.one("SELECT asset_id,inspection_id,assignee_id,status FROM finding WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),parent);
            actions.assertRepairer(actor,row);requireState(str(row,"status"),"IN_PROGRESS");
            assetId=uuid(row,"assetId");inspectionId=uuid(row,"inspectionId");findingId=parent;
        } else {
            actor.require("ADMIN","SUPERVISOR","INSPECTOR");
            var row=db.one("SELECT asset_id,inspector_id,status FROM inspection WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),parent);
            if(!Objects.equals(uuid(row,"inspectorId"),actor.id())) throw new AccessDeniedException("Only the inspector can upload inspection evidence.");
            requireState(str(row,"status"),"DRAFT");assetId=uuid(row,"assetId");inspectionId=parent;
        }
        String mime=FileRules.mime(data);String safe=FileRules.filename(filename,mime);
        db.one("SELECT id FROM asset WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),assetId);
        long used=db.count("SELECT COALESCE(sum(size),0) FROM evidence WHERE organization_id=? AND asset_id=?",actor.organizationId(),assetId);
        if(used+data.length>ASSET_QUOTA) throw new RuleViolation("storageLimit","This asset's 50 MiB evidence quota would be exceeded.");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO evidence(id,organization_id,asset_id,inspection_id,finding_id,uploaded_by,filename,mime,size,sha256,data) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            id,actor.organizationId(),assetId,inspectionId,findingId,actor.id(),safe,mime,data.length,FileRules.sha256(data),data);
        audit.record(actor,"EVIDENCE",id,"EVIDENCE_ATTACHED",safe+" | sha256="+FileRules.sha256(data));return metadata(actor,id);
    }
}
