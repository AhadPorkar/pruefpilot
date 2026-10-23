package com.ahadporkar.pruefpilot.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** HTTP input contracts. Unknown JSON fields are rejected; never bind database objects. */
public final class Commands {
    private Commands() {}
    public record Site(@NotBlank @Size(max=160) String name,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{2,32}") String code,@NotBlank @Size(max=400) String address) {}
    public record Asset(@NotNull UUID siteId,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{2,40}") String code,@NotBlank @Size(max=160) String name,
        @NotBlank @Size(max=80) String category,@Size(max=120) String manufacturer,@Size(max=120) String model,@Size(max=120) String serialNumber,
        @Size(max=240) String locationNote,@NotBlank @Pattern(regexp="LOW|MEDIUM|HIGH|CRITICAL") String criticality) {}
    public record Item(@NotBlank @Size(max=400) String label,@NotBlank @Size(max=400) String labelDe,
        @NotBlank @Pattern(regexp="PASS_FAIL|NUMBER|TEXT") String kind,boolean required,@Digits(integer=12,fraction=6) BigDecimal minValue,@Digits(integer=12,fraction=6) BigDecimal maxValue,@Size(max=24) String unit) {}
    public record Template(@NotBlank @Size(max=160) String name,@NotBlank @Size(max=160) String nameDe,@Size(max=2000) String description,
        @Min(1) int version,@NotEmpty @Size(max=100) List<@Valid Item> items) {}
    public record Schedule(@NotNull UUID assetId,@NotNull UUID templateId,@NotNull UUID ownerId,@Min(1) @Max(3650) int intervalDays,
        @NotNull LocalDate nextDue,@NotBlank @Size(min=10,max=2000) String reason) {}
    public record StartInspection(@NotNull UUID assetId,@NotNull UUID templateId,UUID scheduleId,@NotNull UUID requestKey) {}
    public record Answer(@NotNull UUID itemId,@NotNull @Size(max=2000) String value,@Size(max=2000) String note) {}
    public record SubmitInspection(@Min(0) int version,@NotEmpty @Size(max=100) List<@Valid Answer> answers) {}
    public record Review(@Min(0) int version,boolean approved,@NotBlank @Size(min=5,max=2000) String note) {}
    public record Assign(@Min(0) int version,@NotNull UUID assigneeId,@NotNull LocalDate dueDate,@Pattern(regexp="LOW|MEDIUM|HIGH|CRITICAL") @NotBlank String severity) {}
    public record Version(@Min(0) int version) {}
    public record Repair(@Min(0) int version,@NotBlank @Size(min=10,max=4000) String note) {}
    public record User(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=120) String displayName,
        @NotBlank @Pattern(regexp="ADMIN|SUPERVISOR|INSPECTOR|TECHNICIAN|VIEWER") String role,@NotBlank @Size(min=14,max=72) String password) {}
    public record Active(boolean active) {}
    public record Password(@NotBlank String currentPassword,@NotBlank @Size(min=14,max=72) String newPassword) {}
}
