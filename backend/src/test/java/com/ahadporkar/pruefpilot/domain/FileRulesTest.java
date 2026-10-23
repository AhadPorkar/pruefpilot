package com.ahadporkar.pruefpilot.domain;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FileRulesTest {
 @Test void signatures(){assertEquals("application/pdf",FileRules.mime("%PDF-1.7".getBytes(StandardCharsets.US_ASCII)));assertEquals("image/jpeg",FileRules.mime(new byte[]{(byte)255,(byte)216,(byte)255}));}
 @Test void rejectsHtmlAndEmpty(){assertThrows(RuleViolation.class,()->FileRules.mime("<script>".getBytes(StandardCharsets.US_ASCII)));assertThrows(RuleViolation.class,()->FileRules.mime(new byte[0]));}
 @Test void sizeLimit(){assertThrows(RuleViolation.class,()->FileRules.mime(new byte[FileRules.MAX_BYTES+1]));}
 @Test void pathIsRemoved(){assertEquals("passwd.pdf",FileRules.filename("../../passwd","application/pdf"));assertEquals("photo.jpg",FileRules.filename("C:\\temp\\photo.jpg","image/jpeg"));}
 @Test void knownHash(){assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",FileRules.sha256("abc".getBytes(StandardCharsets.US_ASCII)));}
}
