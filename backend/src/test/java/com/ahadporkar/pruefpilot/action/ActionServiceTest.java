package com.ahadporkar.pruefpilot.action;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.shared.Db;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import static com.ahadporkar.pruefpilot.TestActors.*;
class ActionServiceTest {
 @Test void onlyAssignedRepairerCanAct(){var s=new ActionService(mock(Db.class),mock(CatalogService.class),mock(AuditService.class));var a=actor("TECHNICIAN");assertThrows(AccessDeniedException.class,()->s.assertRepairer(a,Map.of("assigneeId",UUID.randomUUID().toString())));assertDoesNotThrow(()->s.assertRepairer(a,Map.of("assigneeId",a.id().toString())));}
 @Test void inspectorCannotRepair(){var s=new ActionService(mock(Db.class),mock(CatalogService.class),mock(AuditService.class));var a=actor("INSPECTOR");assertThrows(AccessDeniedException.class,()->s.assertRepairer(a,Map.of("assigneeId",a.id().toString())));}
}
