package com.ahadporkar.pruefpilot.domain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class ExportRulesTest {
 @Test void htmlEscapes(){assertFalse(ExportRules.html("<script>alert('x')</script>").contains("<script>"));}
 @ParameterizedTest @ValueSource(strings={"=SUM(A1)","+123","-1","@cmd","\t=2"}) void formulaNeutralized(String s){assertTrue(ExportRules.csv(s).contains("'"));}
 @Test void nullSafe(){assertEquals("",ExportRules.html(null));}
}
