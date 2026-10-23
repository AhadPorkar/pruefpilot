package com.ahadporkar.pruefpilot.shared;
import org.junit.jupiter.api.Test;
import com.ahadporkar.pruefpilot.domain.RuleViolation;
import static org.junit.jupiter.api.Assertions.*;
class ErrorsTest {
 @Test void conflictUses409(){var response=new Errors().rule(new RuleViolation("conflict","Refresh first"));assertEquals(409,response.getStatusCode().value());assertEquals("conflict",response.getBody().getProperties().get("code"));}
 @Test void businessRuleUses422(){assertEquals(422,new Errors().rule(new RuleViolation("independentReview","Another person must review")).getStatusCode().value());}
 @Test void sqlErrorDetailsNotReturned(){assertFalse(new Errors().conflict().getBody().getDetail().contains("SELECT"));}
}
