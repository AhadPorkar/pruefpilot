package com.ahadporkar.pruefpilot.domain;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ActionRulesTest {
 @Test void repairRequiresEvidence(){assertEquals("evidenceRequired",assertThrows(RuleViolation.class,()->ActionRules.mayComplete("IN_PROGRESS","Replaced the damaged hose",0)).code());}
 @Test void cannotSkipStart(){assertThrows(RuleViolation.class,()->ActionRules.mayComplete("OPEN","Replaced the damaged hose",1));}
 @Test void meaningfulRepairNote(){assertThrows(RuleViolation.class,()->ActionRules.mayComplete("IN_PROGRESS","fixed",1));}
 @Test void validCompletion(){assertDoesNotThrow(()->ActionRules.mayComplete("IN_PROGRESS","Replaced the damaged hose",1));}
 @Test void independentClosure(){assertThrows(RuleViolation.class,()->ActionRules.mayVerify("AWAITING_VERIFICATION","a","a","Checked function",1));}
 @Test void validClosure(){assertDoesNotThrow(()->ActionRules.mayVerify("AWAITING_VERIFICATION","a","b","Checked function",1));}
 @Test void closedCannotCloseAgain(){assertThrows(RuleViolation.class,()->ActionRules.mayVerify("CLOSED","a","b","Checked function",1));}
}
