package com.ahadporkar.pruefpilot.api;
import java.time.*;import java.util.*;
import org.junit.jupiter.api.Test;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.shared.Db;
import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;import static org.junit.jupiter.api.Assertions.*;
import static com.ahadporkar.pruefpilot.TestActors.*;
class WorkspaceServiceTest {
 @Test void businessDayUsesWorkspaceTimezone(){var db=mock(Db.class);var a=actor("ADMIN");when(db.one(anyString(),eq(a.organizationId()))).thenReturn(Map.of("id",a.organizationId(),"name","Test","timezone","Europe/Berlin"));var s=new WorkspaceService(db,mock(AuditService.class),mock(CatalogService.class),Clock.fixed(Instant.parse("2026-10-23T23:30:00Z"),ZoneOffset.UTC),false);var result=s.workspace(a);assertEquals("2026-10-24",result.get("today"));assertEquals(false,result.get("demo"));assertEquals(false,result.get("historyTruncated"));}
}
