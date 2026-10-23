package com.ahadporkar.pruefpilot.auth;

import java.nio.charset.StandardCharsets;
import java.util.*;
import com.ahadporkar.pruefpilot.api.Commands;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.shared.*;
import com.ahadporkar.pruefpilot.domain.RuleViolation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.ahadporkar.pruefpilot.shared.Db.*;

@Service
public class MemberService {
    private final Db db;private final PasswordEncoder encoder;private final AuditService audit;
    public MemberService(Db db,PasswordEncoder encoder,AuditService audit){this.db=db;this.encoder=encoder;this.audit=audit;}
    public static void validatePassword(String password) {
        if(password==null||password.length()<14||password.getBytes(StandardCharsets.UTF_8).length>72) throw new RuleViolation("passwordPolicy","Use at least 14 characters and no more than 72 UTF-8 bytes.");
    }
    public List<Map<String,Object>> list(Actor actor) {actor.require("ADMIN");return db.list("SELECT id,email,display_name,role,active,created_at FROM app_user WHERE organization_id=? ORDER BY display_name",actor.organizationId());}
    @Transactional public Map<String,Object> create(Actor actor,Commands.User input) {
        actor.require("ADMIN");validatePassword(input.password());UUID id=UUID.randomUUID();
        db.update("INSERT INTO app_user(id,organization_id,email,display_name,password_hash,role) VALUES(?,?,?,?,?,?)",id,actor.organizationId(),input.email().strip().toLowerCase(Locale.ROOT),input.displayName().strip(),encoder.encode(input.password()),input.role());
        audit.record(actor,"MEMBER",id,"MEMBER_CREATED",input.role());return Map.of("id",id);
    }
    @Transactional public void setActive(Actor actor,UUID id,boolean active) {
        actor.require("ADMIN");if(id.equals(actor.id())) throw new RuleViolation("selfDisable","You cannot disable your own account.");
        db.one("SELECT id FROM app_user WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),id);
        db.update("UPDATE app_user SET active=? WHERE organization_id=? AND id=?",active,actor.organizationId(),id);audit.record(actor,"MEMBER",id,active?"MEMBER_ENABLED":"MEMBER_DISABLED","");
    }
    @Transactional public void changePassword(Actor actor,Commands.Password input) {
        validatePassword(input.newPassword());var row=db.one("SELECT password_hash FROM app_user WHERE organization_id=? AND id=? FOR UPDATE",actor.organizationId(),actor.id());
        if(!encoder.matches(input.currentPassword(),str(row,"passwordHash"))) throw new ApiException(403,"invalidCredentials","Current password is incorrect.");
        db.update("UPDATE app_user SET password_hash=? WHERE organization_id=? AND id=?",encoder.encode(input.newPassword()),actor.organizationId(),actor.id());
        audit.record(actor,"MEMBER",actor.id(),"PASSWORD_CHANGED","");
    }
}
