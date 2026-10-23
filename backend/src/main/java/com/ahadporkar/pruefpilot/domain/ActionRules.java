package com.ahadporkar.pruefpilot.domain;

import static com.ahadporkar.pruefpilot.domain.InspectionRules.*;

/** Explicit state transitions and the four-eyes closure requirement. */
public final class ActionRules {
    private ActionRules() {}
    public static void mayComplete(String state, String repairNote, int evidenceCount) {
        requireState(state, "IN_PROGRESS");
        if (repairNote == null || repairNote.strip().length() < 10 || repairNote.length() > 4000)
            fail("repairRequired", "Describe the repair in at least ten characters.");
        if (evidenceCount < 1) fail("evidenceRequired", "Upload repair evidence before requesting verification.");
    }
    public static void mayVerify(String state, String completedBy, String reviewer, String note, int evidenceCount) {
        requireState(state, "AWAITING_VERIFICATION");
        assertIndependent(reviewer, completedBy);
        if (note == null || note.strip().length() < 5 || note.length() > 2000)
            fail("noteRequired", "Document the verification result.");
        if (evidenceCount < 1) fail("evidenceRequired", "A verified repair must retain its evidence.");
    }
}
