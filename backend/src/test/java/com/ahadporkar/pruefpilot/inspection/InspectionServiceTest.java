package com.ahadporkar.pruefpilot.inspection;
import java.util.*;import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import com.ahadporkar.pruefpilot.api.Commands;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.shared.Db;
import com.ahadporkar.pruefpilot.domain.RuleViolation;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;
import static com.ahadporkar.pruefpilot.TestActors.*;
class InspectionServiceTest {
 @Test void technicianCannotStart(){var db=mock(Db.class);var s=new InspectionService(db,mock(CatalogService.class),mock(AuditService.class),Clock.systemUTC());assertThrows(AccessDeniedException.class,()->s.start(actor("TECHNICIAN"),new Commands.StartInspection(UUID.randomUUID(),UUID.randomUUID(),null,UUID.randomUUID())));verifyNoInteractions(db);}
 @Test void inspectorCannotReviewOwnRecord(){var db=mock(Db.class);var audit=mock(AuditService.class);var a=actor("SUPERVISOR");var id=UUID.randomUUID();when(db.one(anyString(),eq(a.organizationId()),eq(id))).thenReturn(Map.of("status","SUBMITTED","version",1,"inspectorId",a.id().toString()));var s=new InspectionService(db,mock(CatalogService.class),audit,Clock.systemUTC());assertEquals("independentReview",assertThrows(RuleViolation.class,()->s.review(a,id,new Commands.Review(1,true,"Reviewed record"))).code());verifyNoInteractions(audit);}
}
