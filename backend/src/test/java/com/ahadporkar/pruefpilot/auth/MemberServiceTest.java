package com.ahadporkar.pruefpilot.auth;
import org.junit.jupiter.api.Test;
import com.ahadporkar.pruefpilot.domain.RuleViolation;
import static org.junit.jupiter.api.Assertions.*;
class MemberServiceTest {
 @Test void passwordBounds(){assertThrows(RuleViolation.class,()->MemberService.validatePassword("short"));assertThrows(RuleViolation.class,()->MemberService.validatePassword("x".repeat(73)));assertDoesNotThrow(()->MemberService.validatePassword("Long unique test password"));}
 @Test void utf8ByteLimit(){assertThrows(RuleViolation.class,()->MemberService.validatePassword("\u00fc".repeat(37)));}
}
