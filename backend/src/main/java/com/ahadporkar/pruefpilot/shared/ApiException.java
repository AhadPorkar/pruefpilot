package com.ahadporkar.pruefpilot.shared;

public final class ApiException extends RuntimeException {
    public final int status;
    public final String code;
    public ApiException(int status, String code, String message) { super(message); this.status=status; this.code=code; }
    public static ApiException missing() { return new ApiException(404,"notFound","Record not found in this workspace."); }
}
