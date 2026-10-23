package com.ahadporkar.pruefpilot.domain;

/** Stable, language-neutral code is translated by the client; detail is safe for operators. */
public final class RuleViolation extends RuntimeException {
    private final String code;
    public RuleViolation(String code, String detail) { super(detail); this.code = code; }
    public String code() { return code; }
}
