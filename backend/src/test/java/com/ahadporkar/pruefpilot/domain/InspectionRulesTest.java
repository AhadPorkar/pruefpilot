package com.ahadporkar.pruefpilot.domain;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class InspectionRulesTest {
 final InspectionRules.Item check=new InspectionRules.Item("PASS_FAIL",true,null,null);
 @Test void passNeedsNoNote(){assertEquals("PASS",InspectionRules.outcome(check,"PASS",""));}
 @ParameterizedTest @ValueSource(strings={"FAIL","NA"}) void exceptionsNeedExplanation(String v){assertThrows(RuleViolation.class,()->InspectionRules.outcome(check,v,""));assertEquals(v,InspectionRules.outcome(check,v,"Documented reason"));}
 @ParameterizedTest @ValueSource(strings={"","pass","UNKNOWN"}) void invalidCheck(String v){assertThrows(RuleViolation.class,()->InspectionRules.outcome(check,v,""));}
 @Test void optionalEmpty(){assertEquals("NA",InspectionRules.outcome(new InspectionRules.Item("TEXT",false,null,null),"",null));}
 @Test void numericBoundsAreInclusive(){var item=new InspectionRules.Item("NUMBER",true,new BigDecimal("2"),new BigDecimal("8"));for(String n:new String[]{"2","8","5.5"})assertEquals("PASS",InspectionRules.outcome(item,n,""));assertEquals("FAIL",InspectionRules.outcome(item,"1.9","Below configured minimum"));}
 @ParameterizedTest @ValueSource(strings={"NaN","Infinity","1,2","1000000000000","0.1234567"}) void invalidDecimal(String v){assertThrows(RuleViolation.class,()->InspectionRules.outcome(new InspectionRules.Item("NUMBER",true,null,null),v,""));}
 @Test void independentReview(){assertThrows(RuleViolation.class,()->InspectionRules.assertIndependent("a","a"));assertDoesNotThrow(()->InspectionRules.assertIndependent("a","b"));}
 @Test void stateAndVersion(){assertThrows(RuleViolation.class,()->InspectionRules.requireState("CLOSED","OPEN"));assertEquals("conflict",assertThrows(RuleViolation.class,()->InspectionRules.requireVersion(2,1)).code());}
 @Test void lengthBound(){assertThrows(RuleViolation.class,()->InspectionRules.outcome(check,"PASS","x".repeat(2001)));}
}
