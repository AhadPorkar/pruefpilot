package com.ahadporkar.pruefpilot.audit;
import java.util.*;import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.ahadporkar.pruefpilot.shared.Db;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;
import static com.ahadporkar.pruefpilot.TestActors.*;
class AuditServiceTest {
 @Test void eventIncludesOrganizationActorAndEntity(){var db=mock(Db.class);var a=actor("ADMIN");var id=UUID.randomUUID();new AuditService(db).record(a,"ASSET",id,"CREATED","Details");verify(db).update(startsWith("INSERT INTO audit_event"),any(UUID.class),eq(a.organizationId()),eq(a.id()),eq("ASSET"),eq(id),eq("CREATED"),eq("Details"));}
}
