# PrüfPilot user guide

**Version 1.0.0-rc.1 | English | English is the application default**

## 1. What this application records

PrüfPilot records operational inspections and coordinates corrective work on identifiable assets. It connects a due check to a specific asset, an inspector, the answers and evidence, a repair owner and an independent reviewer. It does not decide whether equipment is safe to use, determine statutory inspection intervals, verify a person's professional competence or issue a legal certificate.

All sample organizations, people, defects, equipment records and evidence are fictional. The demonstration template and its arbitrary 30-day interval must never be treated as a maintenance recommendation.

## 2. Choose the correct mode

**Live/server mode** is the normal application at your deployment URL. Sign in with an administrator-issued account. Data is stored in PostgreSQL. A backend connection error stays an error; the app does not replace customer data with samples.

**Explicit preview mode** has `?demo=1` in the URL and a visible preview strip. You may switch among sample roles without authentication. Changes are only in the current tab's memory; reload or Reset restores the sample. Do not enter customer information. This mode is useful for a recruiter walkthrough, training and UI testing, not business recordkeeping.

A third possibility is a **demo-seeded backend**: normal authentication and persistent database storage, but with fictional starting records and a demo-data banner. It is enabled only during initial provisioning and must not be used for production customer data.

## 3. Sign in, language and display

Open the deployment URL and enter your email and password. There is no public registration. Passwords must have at least 14 characters and at most 72 UTF-8 bytes; long non-ASCII passwords can reach the byte limit sooner. Use an individual account, not a shared team login.

The top-bar language control switches **EN / DE**. English is selected on a new browser. The language preference and light/dark theme are stored locally; passwords and session tokens are not stored in localStorage. Business records are not automatically translated. Template labels have explicit English and German fields; free-text observations retain the author's wording.

Use the sidebar for Overview, Assets, Inspections, Schedules, Corrective actions, Reports, Templates, Team, Audit and Help. On a phone, open the menu button. Press **Ctrl+K** or **Cmd+K** to search navigation and assets. The bell shortcut opens the review-related work view; it is not an email or push-notification subscription.

Click your account panel to change your password. A successful change ends the current session; other sessions are rejected on their next API request. Sign out on shared devices. Sessions expire after 30 minutes of inactivity.

## 4. Roles and responsibilities

| Capability | Administrator | Supervisor | Inspector | Technician | Viewer |
|---|---|---|---|---|---|
| Read workspace and asset records | Yes | Yes | Yes | Yes | Yes |
| Create sites/assets/templates/schedules | Yes | Yes | No | No | No |
| Perform an inspection | Yes | Yes | Yes | No | No |
| Review another person's inspection | Yes | Yes | No | No | No |
| Assign a finding | Yes | Yes | No | No | No |
| Repair a finding assigned to own account | Yes | Yes | No | Yes | No |
| Independently verify a repair | Yes | Yes | No | No | No |
| Export complete customer report | Yes | Yes | No | No | Yes |
| Create/disable accounts | Yes | No | No | No | No |

Permissions are enforced by the server, not only hidden buttons. An administrator cannot bypass the independent-review rule. An inspector cannot edit another inspector's draft. An administrator or supervisor must be the assigned repairer to document a repair.

**Viewer is workspace-wide.** Do not give an external customer this role unless they are authorized to see every record in the workspace. Customer-facing handover in v1 is a selected asset report package, not a customer login portal.

## 5. Create the register

In **Assets**, select **New site**. Enter a meaningful site name, a unique short code and an address. Then select **New asset** and choose its site. Enter its unique asset code, name, category, manufacturer/model, serial number, location note and operational criticality. Criticality is a sorting/communication attribute, not a statutory risk classification.

Use a stable code such as `BER-LIFT-001`. Codes accept letters, numbers, underscores and hyphens. Asset identity should match the physical label and local asset register. The deployment permits 250 active assets. Archiving preserves records and can free an active-asset slot, but first resolve open findings, finish running inspections and pause schedules. This release has no general metadata-edit form, hard-delete control or bulk import; agree the initial naming convention before populating a live workspace.

Select an asset to open its **passport**. Confirm the code and serial number before acting. The passport shows metadata, next due check, recent inspection history, findings, files and the event timeline. Use the older-history control to page through inspections. The overview is intentionally bounded; a truncation notice means older records are not all in that overview. Full asset exports use database history rather than only the visible page.

## 6. QR labels and identification

From an asset passport, choose **Print label / QR** and print the label using the browser. The QR encodes an asset deep link when its UTF-8 content fits the supported capacity; unusually long deployment URLs fall back to the stable asset code with a visible notice. The implementation uses a real QR encoder, not a decorative pattern.

Attach labels only according to the operator's equipment-labeling policy. Never cover safety labels or statutory markings. Scan using the application's identification dialog on a browser with camera and BarcodeDetector support, or use the phone's normal camera for a deep link. Camera access requires a supported secure browser context and permission. Manual asset-code entry always remains available. A QR does not grant access: server authentication and workspace authorization still apply.

## 7. Configure a reviewed template

Open **Templates**, create a template, and provide both English and German names and check labels. Add up to 100 items. Supported types are:

- **Pass / Fail / Not applicable:** failures and not-applicable answers require an explanation of at least five characters.
- **Number:** optional minimum/maximum and unit. The server determines pass/fail from the configured bounds; out-of-range values require a note. Use decimal points in the current number entry, not thousands separators.
- **Text:** an observation. A required text answer cannot be empty; an optional empty answer is not applicable.

The template starts as a draft and cannot yet be used for an inspection or schedule. An administrator or supervisor reviews it, records the review basis and publishes it. This application records that decision; it does not confirm the reviewer's domain qualifications. Published templates cannot be edited. Clone one to a new version when changes are needed. Existing records remain linked to the original item labels and limits.

There is no built-in statutory checklist library. Arrange qualified review for each real equipment class, relevant operating conditions and applicable obligations before use.

## 8. Create a recurring schedule

Open **Schedules**, choose an asset, a reviewed template, a responsible user, the first due date and an interval in calendar days (1–3,650). Record the **basis of the interval and qualified review** in the reason field. A meaningful reason is mandatory. The software never suggests a legal interval.

Only one active schedule for the same asset/template pair is allowed. A schedule owner is a coordinator; the system does not certify their competence. Due dates use the workspace time zone (Europe/Berlin by default), while stored event instants use UTC.

A submitted inspection does **not** advance the due date. Approval by a different supervisor advances the original calendar cadence to the next period after the performed date. Missed periods are skipped, not replaced with invented inspections. For example, an explicitly configured 30-day schedule anchored on 1 September and performed on 5 November advances to 30 November. Rejected inspections leave the schedule unchanged.

Pause a schedule only after its current draft/submitted inspection is resolved. To revise an interval, retain the original schedule, pause it, and create a new reviewed schedule. Existing historical records are not rewritten. No automatic reminder emails are sent in v1; use the due queue as the daily worklist.

## 9. Perform an inspection

Start the due check from **Schedules** or create an ad-hoc inspection from an asset passport. Identify the physical asset first. Only a reviewed template is selectable. A schedule permits one running draft/submitted inspection at a time. Resume an existing draft from **Inspections** instead of starting another.

Complete every check. For a failure, describe what was observed rather than only writing “failed”. For not applicable, explain why. Record units and meaningful numeric values. Add PNG, JPEG or PDF evidence while the inspection is still a draft. Evidence is attached to this record; the application does not evaluate its authenticity or adequacy.

**Save draft** stores progress on the server. Internet connectivity is required. There is no offline queue or background synchronization. A failed request leaves the visible form available, but unsaved work is lost on reload or browser closure. Save frequently. Do not interpret a toast or a disconnected page as a completed submission; verify the resulting status.

**Submit** validates all answers and atomically creates one finding per failed check. Submission changes the record to **Submitted**, assigns server-side timestamps and makes answers immutable through the API. The timestamp records submission/performance entry in the system; it is not independent proof of when a physical check took place. The original inspector owns the draft and submission.

## 10. Review the inspection

A different administrator or supervisor opens a submitted inspection, examines the answers and available evidence and records a meaningful review note. **Approve** verifies the recorded inspection and advances its schedule. **Reject** retains the rejected record and its findings, and leaves the schedule due. Start a new inspection after resolving the reason for rejection; do not overwrite the rejected record.

Approving an inspection means the record was reviewed. It does **not** automatically close failures, declare equipment safe or authorize continued operation. Follow the organization's safety and isolation procedures outside this software.

## 11. Corrective actions and verified closure

The board has four states: **Open → In progress → Awaiting verification → Closed**. No drag-and-drop shortcut bypasses these transitions.

A supervisor opens a finding, assigns an active administrator/supervisor/technician, sets a due date and chooses severity. The assigned person starts the repair. Other accounts cannot write the repair on their behalf, including administrators who are not the assignee.

The repairer documents the work in at least ten characters and uploads at least one evidence file. Completion without evidence is rejected. **Request verification** records the repair and moves the finding to Awaiting verification.

A different administrator or supervisor reviews the repair note and original files, then records a verification note. Approve closes the finding and records the verifier; reject returns it to In progress for further work. The repairer cannot approve their own work, even if they are an administrator. This separation is a product rule, not a statement that all legal inspection-signature requirements have been met.

## 12. Evidence and exports

Accepted evidence: PNG, JPEG and PDF; maximum **10 MiB per file** and **50 MiB per asset**. Detection uses file-content prefixes, not the extension alone. Files are delivered as authenticated downloads, not inline HTML. The application does not run an antivirus scanner. Inspect or scan files under your organization's security process before sharing or opening them. Avoid unnecessary faces, private contact details and unrelated personal information.

In **Reports**, choose one asset and English or German, then download the ZIP. It contains:

| File | Purpose |
|---|---|
| `report.html` | Printable customer-facing summary; open after extracting the complete ZIP |
| `data.json` | Structured record data, schema version and generation time |
| `inspections.csv` | Inspection history and reviewers |
| `answers.csv` | Check values, notes, outcomes and bilingual labels |
| `corrective-actions.csv` | Repair and verification records |
| `evidence/` | Original attached files |
| `manifest.json` | SHA-256 and size for every other file |

Keep the folder structure intact so evidence links work. Use the browser's print command on `report.html` to create a PDF where needed. A PDF export is a browser print operation, not a separately signed certificate. The HTML summary is intentionally concise; use JSON/CSV for complete structured details.

Verify a package with `python scripts/verify-report.py path/to/report.zip`. Matching checksums show consistency with the supplied manifest. Because the manifest is not signed, an attacker who replaces both data and manifest can produce matching checksums. Verification therefore does not authenticate the issuer or certify the asset.

Share packages only with authorized recipients using an approved secure channel. Reports can include staff names and operational information. The application does not email packages or manage external download permissions.

## 13. Team and audit

Administrators create named accounts and issue initial passwords securely. Password reset by email and self-service signup are not included. Disable departing users; do not reuse their identity for a replacement employee. You cannot disable your own administrator account. Disabling a user or changing their password invalidates their existing session on the next API call.

The **Audit** view records creation, submission, review, assignment, evidence and export events. It is read-only through the application, and the database trigger rejects normal updates/deletes. This is not a cryptographically sealed or privileged-admin-proof ledger. The operator controls database access and backups.

## 14. Troubleshooting

| Message/symptom | Action |
|---|---|
| Session expired / unauthorized | Preserve necessary unsaved notes securely, sign in again, reload the record and retry |
| Record changed / conflict | Refresh; compare the current version before repeating the action |
| Independent review required | Ask a different authorized supervisor; do not create a shared identity to bypass it |
| Evidence required | Upload an accepted file while the repair is In progress |
| Cannot pause/archive | Resolve running inspections/open findings and pause active schedules first |
| Unsupported file | Supply actual PNG/JPEG/PDF content within the limits; renaming an extension is insufficient |
| Camera unavailable | Enter the code manually or scan the deep link with the device's camera |
| Connection failed | Check the server/network; preview is separate and must not be used to replace a real record |
| No more active asset capacity | Archive genuinely inactive assets or agree a separately engineered capacity increase |
| Truncation notice | Use the asset passport and full export; do not assume the overview contains all historical records |

For a defect report, include the release, browser, language, role, steps and sanitized message. Never include passwords, `.env`, session cookies or real customer evidence in a public GitHub issue.
