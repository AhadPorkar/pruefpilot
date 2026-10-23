package com.ahadporkar.pruefpilot.report;
import java.util.*;import java.nio.charset.StandardCharsets;import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.shared.*;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import static com.ahadporkar.pruefpilot.TestActors.*;
class ReportServiceTest {
 @Test void csvHasBomAndNeutralizesFormula(){String csv=new String(ReportService.csvTable(List.of(Map.of("note","=SUM(A1)")),List.of("note")),StandardCharsets.UTF_8);assertTrue(csv.startsWith("\ufeffnote\r\n"));assertTrue(csv.contains("'=SUM(A1)"));}
 @Test void unsupportedLanguageRejected(){var db=mock(Db.class);var s=new ReportService(db,mock(CatalogService.class),mock(AuditService.class),JsonMapper.builder().build(),Clock.systemUTC());assertEquals(400,assertThrows(ApiException.class,()->s.export(actor("ADMIN"),UUID.randomUUID(),"xx")).status);verifyNoInteractions(db);}
 @Test void technicianCannotExport(){var db=mock(Db.class);var s=new ReportService(db,mock(CatalogService.class),mock(AuditService.class),JsonMapper.builder().build(),Clock.systemUTC());assertThrows(AccessDeniedException.class,()->s.export(actor("TECHNICIAN"),UUID.randomUUID(),"en"));}
}
