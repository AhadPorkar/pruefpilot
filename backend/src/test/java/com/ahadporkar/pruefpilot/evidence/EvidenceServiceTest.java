package com.ahadporkar.pruefpilot.evidence;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import com.ahadporkar.pruefpilot.action.ActionService;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.domain.FileRules;
import com.ahadporkar.pruefpilot.shared.*;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import static com.ahadporkar.pruefpilot.TestActors.*;
class EvidenceServiceTest {
 @Test void oversizedFileRejectedBeforeReadingBytes() throws Exception {var db=mock(Db.class);var file=mock(MultipartFile.class);when(file.getSize()).thenReturn((long)FileRules.MAX_BYTES+1);var s=new EvidenceService(db,mock(AuditService.class),mock(ActionService.class));assertEquals(413,assertThrows(ApiException.class,()->s.upload(actor("INSPECTOR"),UUID.randomUUID(),false,file)).status);verify(file,never()).getBytes();verifyNoInteractions(db);}
 @Test void metadataIsTenantScoped(){var db=mock(Db.class);var a=actor("VIEWER");var id=UUID.randomUUID();new EvidenceService(db,mock(AuditService.class),mock(ActionService.class)).metadata(a,id);verify(db).one("SELECT "+EvidenceService.COLUMNS+" FROM evidence WHERE organization_id=? AND id=?",a.organizationId(),id);}
}
