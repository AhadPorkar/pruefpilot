package com.ahadporkar.pruefpilot.domain;

/** Escaping at export boundaries prevents HTML injection and spreadsheet formula execution. */
public final class ExportRules {
    private ExportRules() {}
    public static String html(Object value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    public static String csv(Object value) {
        String text = String.valueOf(value == null ? "" : value);
        String trimmed = text.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0 || text.startsWith("\t") || text.startsWith("\r")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
