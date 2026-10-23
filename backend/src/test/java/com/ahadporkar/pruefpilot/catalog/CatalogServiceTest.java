package com.ahadporkar.pruefpilot.catalog;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import com.ahadporkar.pruefpilot.api.Commands;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.shared.Db;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.ahadporkar.pruefpilot.TestActors.*;
class CatalogServiceTest {
 @Test void viewerCannotWrite(){var db=mock(Db.class);var service=new CatalogService(db,mock(AuditService.class));assertThrows(AccessDeniedException.class,()->service.createSite(actor("VIEWER"),new Commands.Site("Site","AB","Address")));verifyNoInteractions(db);}
 @Test void assetLookupIsTenantScoped(){var db=mock(Db.class);var a=actor("ADMIN");var id=UUID.randomUUID();var service=new CatalogService(db,mock(AuditService.class));service.asset(a,id);verify(db).one("SELECT "+CatalogService.ASSET_COLUMNS+" FROM asset WHERE organization_id=? AND id=?",a.organizationId(),id);}
}
