package com.ahadporkar.pruefpilot.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Framework-free checklist validation. Outcomes are always calculated on the server. */
public final class InspectionRules {
    private InspectionRules() {}
    public record Item(String kind, boolean required, BigDecimal min, BigDecimal max) {}

    public static String outcome(Item item, String value, String note) {
        value = Objects.requireNonNullElse(value, "").strip();
        note = Objects.requireNonNullElse(note, "").strip();
        if (value.length() > 2000 || note.length() > 2000) fail("validation", "Answer is too long.");
        if (value.isEmpty()) {
            if (item.required()) fail("answerRequired", "Complete all required checks.");
            return "NA";
        }
        String result;
        switch (item.kind()) {
            case "PASS_FAIL" -> {
                if (!value.equals("PASS") && !value.equals("FAIL") && !value.equals("NA"))
                    fail("validation", "Unknown checklist response.");
                result = value;
                if (result.equals("NA") && note.length() < 5) fail("noteRequired", "Explain why the check is not applicable.");
            }
            case "NUMBER" -> {
                BigDecimal number;
                try { number = new BigDecimal(value); }
                catch (NumberFormatException ex) { throw new RuleViolation("invalidNumber", "Enter a finite decimal number."); }
                if (number.precision() > 18 || number.scale() > 6 || number.abs().compareTo(new BigDecimal("999999999999")) > 0)
                    fail("invalidNumber", "Number is outside the supported precision.");
                result = (item.min() != null && number.compareTo(item.min()) < 0)
                    || (item.max() != null && number.compareTo(item.max()) > 0) ? "FAIL" : "PASS";
            }
            case "TEXT" -> result = "PASS";
            default -> throw new RuleViolation("validation", "Unknown check type.");
        }
        if (result.equals("FAIL") && note.length() < 5) fail("noteRequired", "Describe each failed check (at least five characters).");
        return result;
    }

    public static void assertIndependent(String actor, String author) {
        if (Objects.equals(actor, author)) fail("independentReview", "A different supervisor must verify this record.");
    }
    public static void requireState(String current, String expected) {
        if (!Objects.equals(current, expected)) fail("invalidTransition", "The record is no longer in the expected state.");
    }
    public static void requireVersion(int current, int supplied) {
        if (current != supplied) fail("conflict", "This record changed. Refresh before retrying.");
    }
    public static void fail(String code, String message) { throw new RuleViolation(code, message); }
}
