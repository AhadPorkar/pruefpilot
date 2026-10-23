package com.ahadporkar.pruefpilot.domain;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class ScheduleRulesTest {
 @ParameterizedTest @ValueSource(ints={-1,0,3651}) void invalidInterval(int d){assertThrows(RuleViolation.class,()->ScheduleRules.validate(d,"Approved operator basis"));}
 @Test void explicitReasonRequired(){assertThrows(RuleViolation.class,()->ScheduleRules.validate(30,""));}
 @Test void keepsCalendarAnchor(){assertEquals(LocalDate.parse("2026-11-30"),ScheduleRules.nextDue(LocalDate.parse("2026-09-01"),30,LocalDate.parse("2026-11-05")));}
 @Test void earlyCheckStillAdvancesOnePeriod(){assertEquals(LocalDate.parse("2026-10-31"),ScheduleRules.nextDue(LocalDate.parse("2026-10-01"),30,LocalDate.parse("2026-09-20")));}
 @Test void leapDay(){assertEquals(LocalDate.parse("2028-02-29"),ScheduleRules.nextDue(LocalDate.parse("2028-02-28"),1,LocalDate.parse("2028-02-28")));}
}
