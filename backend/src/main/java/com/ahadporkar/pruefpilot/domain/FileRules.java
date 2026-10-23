package com.ahadporkar.pruefpilot.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import static com.ahadporkar.pruefpilot.domain.InspectionRules.fail;

/** Content-signature allowlist, not an antivirus engine. Files are always served as attachments. */
public final class FileRules {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    private FileRules() {}
    public static String mime(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) fail("fileSize", "Files must be between 1 byte and 10 MiB.");
        if (starts(bytes, new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if (starts(bytes, new byte[]{(byte)255,(byte)216,(byte)255})) return "image/jpeg";
        if (starts(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII))) return "application/pdf";
        throw new RuleViolation("fileType", "Only PNG, JPEG and PDF evidence is accepted.");
    }
    private static boolean starts(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (data[i] != prefix[i]) return false;
        return true;
    }
    public static String filename(String supplied, String mime) {
        String safe = supplied == null ? "evidence" : supplied.replace('\\','/');
        safe = safe.substring(safe.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._ -]", "_");
        safe = safe.replaceAll("^[. ]+", "");
        if (safe.isBlank()) safe = "evidence";
        if (safe.length() > 140) safe = safe.substring(0, 140);
        String extension = mime.equals("image/png") ? ".png" : mime.equals("image/jpeg") ? ".jpg" : ".pdf";
        return safe.toLowerCase().endsWith(extension) ? safe : safe + extension;
    }
    public static String sha256(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
