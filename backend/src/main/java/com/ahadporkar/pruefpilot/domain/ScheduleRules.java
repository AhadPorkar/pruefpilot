package com.ahadporkar.pruefpilot.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import static com.ahadporkar.pruefpilot.domain.InspectionRules.fail;

/** Operational calendar cadence, not a determination of a statutory inspection interval. */
public final class ScheduleRules {
    private ScheduleRules() {}
    public static void validate(int intervalDays, String reason) {
        if (intervalDays < 1 || intervalDays > 3650) fail("invalidInterval", "Interval must be 1 to 3650 days.");
        if (reason == null || reason.strip().length() < 10 || reason.length() > 2000)
            fail("scheduleReason", "Document the basis and qualified reviewer of this interval.");
    }
    public static LocalDate nextDue(LocalDate scheduledDate, int days, LocalDate performedDate) {
        validate(days, "Explicitly configured operational cadence");
        long periods = Math.max(1, Math.floorDiv(ChronoUnit.DAYS.between(scheduledDate, performedDate), days) + 1);
        return scheduledDate.plusDays(Math.multiplyExact(periods, days));
    }
}
