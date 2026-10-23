# HTTP API reference

Base path `/api`, same origin. JSON uses camelCase; UUIDs are strings, dates `YYYY-MM-DD`, instants ISO UTC. The server derives organization and role from the session. Unknown JSON properties are rejected. This document describes implemented routes, not a generated OpenAPI server or a promise of SDK compatibility.

## Authentication

1. `GET /api/auth/csrf` returns `{ "token": "masked-token", "headerName": "X-CSRF-TOKEN" }` and establishes the session as needed.
2. `POST /api/auth/login` with `application/x-www-form-urlencoded` fields `username` and `password`, plus the returned CSRF header and the session cookie. Success is 204; bad credentials 401.
3. Fetch CSRF again after login because authentication rotates token/session state.
4. Send the token header on POST/PUT/PATCH and retain the HttpOnly `PP_SESSION` cookie. Do not put it in URLs or localStorage.
5. `POST /api/auth/logout` with CSRF returns 204 and invalidates the session.

`GET /auth/me` returns id, organizationId, email, displayName and role. No password hash is returned. `POST /auth/password` accepts `{currentPassword,newPassword}` and returns 204; sign in again afterwards. The new password is at least 14 characters and at most 72 UTF-8 bytes. Login attempts are limited per process/IP; a 429 includes Retry-After.

## Read models

| Method/path | Response / access |
|---|---|
| `GET /workspace` | Current actor, organization, local today, UTC serverNow, demo flag, sites/assets/templates/templateItems/schedules/inspections/answers/findings/evidence/users/audit and historyTruncated; all authenticated roles |
| `GET /assets/{id}/passport?page=0` | Asset; inspections/answers page of 100; findings/evidence (up to 1,000 each); recent audit; page/hasMore |
| `GET /inspections/{id}` | `{inspection,answers}` in actor workspace |
| `GET /audit?page=0` | 100 audit events, newest first |
| `GET /members` | Account list including emails; ADMIN only |
| `GET /evidence/{id}/download` | Original bytes, attachment disposition, MIME, no-store, no-sniff |
| `GET /assets/{id}/report?lang=en` | Customer ZIP, `en` or `de`; ADMIN/SUPERVISOR/VIEWER |

The workspace projection is bounded: 500 sites, 1,000 assets (250 active cap), 500 templates, 5,000 template items, 2,000 schedules, 500 recent inspections, 1,000 findings, 1,000 evidence metadata rows and 500 people. Open findings are prioritized before closed ones. `historyTruncated` signals overflow in inspections/findings/evidence; other catalog bounds are operational v1 limits and do not each produce an independent warning. This is not an unbounded analytics API. Full exports query asset history independently of the screen projection.

## Catalog commands

ADMIN/SUPERVISOR unless noted. Create responses are 201; archive/review/pause are 204.

### `POST /sites`

```json
{"name":"Berlin workshop","code":"BER","address":"Operator-provided address"}
```

Name 1–160; code 2–32 matching `[A-Za-z0-9_-]`; address 1–400. Code normalized to uppercase and unique in workspace. Returns `{id}`.

### `POST /assets`

```json
{"siteId":"UUID","code":"BER-LIFT-001","name":"Workshop lift","category":"Lifting equipment","manufacturer":"Example","model":"Model A","serialNumber":"S-001","locationNote":"Bay 1","criticality":"HIGH"}
```

Code 2–40 and workspace unique; name 160, category 80, manufacturer/model/serial 120 each, location 240. Criticality LOW/MEDIUM/HIGH/CRITICAL. Returns full asset projection. Active cap 250. `POST /assets/{id}/archive` has no body and rejects open findings, running inspections or active schedules. No generic asset update/delete endpoint exists in this release.

### `POST /templates`

```json
{"name":"Visual observations","nameDe":"Sichtkontrolle","description":"Operator-reviewed scope","version":1,"items":[{"label":"Identity readable","labelDe":"Kennzeichnung lesbar","kind":"PASS_FAIL","required":true,"minValue":null,"maxValue":null,"unit":""}]}
```

Names 160, description 2,000; positive version; 1–100 items. Labels 400 each; kinds PASS_FAIL/NUMBER/TEXT; unit 24. Numeric limits have at most 12 integer and 6 fraction digits and minimum must not exceed maximum. Non-number items cannot define numeric bounds. Returns template; items are in `/workspace`.

`POST /templates/{id}/review` accepts the shared review shape `{ "version":0, "approved":true, "note":"Qualified review basis" }`. Only `note` is used for template publication; the current server state must be DRAFT. Note 5–2,000. Publication always means review/publish; use inspection/finding endpoints for approve/reject behavior. Published templates are immutable; create a clone with a new identity/version to revise.

### `POST /schedules`

```json
{"assetId":"UUID","templateId":"UUID","ownerId":"UUID","intervalDays":30,"nextDue":"2026-11-01","reason":"Explicit operator decision after qualified review; example only"}
```

Requires active asset, reviewed template, active owner; interval 1–3,650 calendar days, reason 10–2,000. One active schedule per asset/template. Returns `{id}`. `POST /schedules/{id}/pause` rejects a current DRAFT/SUBMITTED inspection. The API does not calculate legally correct intervals.

## Inspection commands

### `POST /inspections` (ADMIN/SUPERVISOR/INSPECTOR)

```json
{"assetId":"UUID","templateId":"UUID","scheduleId":null,"requestKey":"CLIENT-GENERATED-UUID"}
```

201 with full inspection. For a scheduled check supply its schedule UUID. Reuse the same requestKey only for the same logical retry, including same actor/input. The service returns the same record; changed input conflicts. A unique running-schedule constraint protects additional starts. A network failure is not proof that the command did not commit; inspect current records before generating another key.

### `PUT /inspections/{id}/draft` and `POST /inspections/{id}/submit`

```json
{"version":0,"answers":[{"itemId":"UUID","value":"FAIL","note":"Observed damage at the connection"}]}
```

Original inspector only. `version` is required logically and nonnegative; always send the last returned value. Answers have value/note max 2,000, unique item IDs and exact template membership. Final submission includes every item, including an optional empty answer. Drafts may be partial. Final outcomes are server-calculated, never accepted from the client. Each FAIL creates a finding. Success returns updated inspection with incremented version.

### `POST /inspections/{id}/review`

ADMIN/SUPERVISOR, must differ from inspector. Body `{version,approved,note}` with note 5–2,000. Requires SUBMITTED. Approval returns VERIFIED and atomically advances an attached schedule; rejection returns REJECTED without advancing. Submitted/reviewed records have no edit/delete endpoint.

## Corrective-action commands

All return the updated finding; all require its current optimistic version.

| POST path | Body | Rule |
|---|---|---|
| `/findings/{id}/assign` | `{version,assigneeId,dueDate,severity}` | ADMIN/SUPERVISOR; OPEN/IN_PROGRESS; active assignee with ADMIN/SUPERVISOR/TECHNICIAN |
| `/findings/{id}/start` | `{version}` | Assigned repairer; OPEN only |
| `/findings/{id}/complete` | `{version,note}` | Assigned repairer; IN_PROGRESS; note 10–4,000 and at least one evidence file |
| `/findings/{id}/verify` | `{version,approved,note}` | ADMIN/SUPERVISOR, different from completedBy; AWAITING_VERIFICATION; note 5–2,000 and retained evidence |

Approval closes; rejection returns to IN_PROGRESS. UI hiding is not the security boundary. No direct status patch exists.

## Evidence upload

`POST /inspections/{id}/evidence` or `POST /findings/{id}/evidence`, multipart field **file**, returns 201 metadata. Do not manually set multipart Content-Type in browser code; let FormData generate the boundary.

Inspection evidence requires DRAFT and original inspector. Finding evidence requires IN_PROGRESS and the assigned repairer. Limits 10 MiB/file and 50 MiB/asset; accepted signatures PNG/JPEG/PDF. Metadata: id, assetId, inspectionId, findingId, uploadedBy, filename, mime, size, sha256, createdAt. File bytes are never in the workspace JSON. No delete endpoint is provided. Detection is not an antivirus or file-validity guarantee.

## Team commands

`POST /members` ADMIN only, 201 `{id}`. Fields email (max 254), displayName (max 120), role (ADMIN/SUPERVISOR/INSPECTOR/TECHNICIAN/VIEWER), password. Email is globally unique and normalized lowercase. Password minimum 14 characters / maximum 72 UTF-8 bytes.

`PATCH /members/{id}` ADMIN only, `{ "active": false }`, 204. Self-disable rejected. Historical references remain. No email invitation, password-reset token, role-edit or account-delete endpoint exists.

## Errors

Application validation failures return `application/problem+json` with status, detail and stable `code`. Authentication filters return a minimal compatible status/code JSON envelope without detailed internals. Do not depend on English human-readable messages; the frontend maps codes to EN/DE.

| Status | Typical meaning |
|---|---|
| 400 | Invalid request/schema/UUID/query/language |
| 401 | Missing, expired or revoked session / invalid login |
| 403 | CSRF or role/ownership denied |
| 404 | Record absent in this workspace |
| 409 | Stale optimistic version, duplicate or conflicting update |
| 413 | Multipart file too large |
| 422 | Business rule such as independentReview, evidenceRequired, invalidTransition, noteRequired, archived, openWork, assetLimit |
| 429 | Login/export limiter |

Never retry a mutation blindly after 409/422. Reload and reconcile. SQL details and stack traces are not exposed. Client-side validation is convenience; server constraints remain authoritative.

## Export integrity

Manifest shape: `{algorithm:"SHA-256", files:[{path,bytes,sha256}], notice:...}`. `manifest.json` excludes itself. Report schema version is 1. Verify with the supplied Python script; it checks safe names, duplicates, total size, exact membership, size and hashes without extracting. A matching unsigned manifest is not proof of authenticity, legal certification or malware safety.
