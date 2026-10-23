# Security policy and threat boundaries

## Reporting

Do not open a public issue containing credentials, session cookies, customer records, backups or exploit details that could endanger a live deployment. Before public launch, enable GitHub private vulnerability reporting for the repository and publish a monitored security contact. Until that contact exists, use the repository owner's established private contact channel; this document does not invent a security mailbox or SLA.

This is a release candidate with no independent penetration-test or compliance certification. Supported-version policy and response targets must be assigned by the operator before customer onboarding.

## Implemented controls

Same-origin HttpOnly/SameSite=Strict sessions; Secure cookies required by the production guard; Spring CSRF including login/logout; BCrypt cost 12 and bounded passwords; per-process/IP login rate limit; per-request active/role/password-hash checks; server role and original-author/assignee checks; independent reviews; explicit organization predicates and composite tenant foreign keys; prepared SQL; row locks, unique constraints and optimistic versions; bounded uploads, signature allowlist, sanitized filenames, attachment/no-sniff downloads; escaped HTML and formula-neutralized CSV; CSP with self-only scripts and no object/frame embedding; read-only non-root app container and separate runtime DB role in Compose.

The policy allows inline **styles**, not inline scripts, because charts/theme fragments use generated style attributes. No third-party runtime script, font, analytics or QR service is required. Static frontend sources and preview are intentionally public; business APIs require authentication.

## Threat model

| Threat | Control | Residual boundary |
|---|---|---|
| Cross-workspace record access | Principal-derived org predicates and composite FKs | No RLS; review every new query and privileged operator access |
| Shared identity/self-certification | Named accounts and independent-review comparisons | Software cannot prove real-world identity, competence or absence of collusion |
| Stale/concurrent mutation | Row locks, versions, idempotent start and DB constraints | No universal automatic retry; load/race testing remains required |
| Credential guessing | BCrypt and bounded per-IP limiter | No MFA/OIDC; multi-node/edge limiter and recovery process required |
| Stored script/spreadsheet execution | Escaped views/reports, CSP, CSV prefixing | Uploaded PDFs/images may still be malicious; no antivirus engine |
| Tampered evidence package | Original-file hashes and manifest verification | Manifest unsigned; no authenticity, legal-signature or malware guarantee |
| Audit alteration | API read-only audit, trigger, limited runtime role | Privileged DB owner/host compromise can bypass controls |
| Resource exhaustion | Asset/file/quota/request/export bounds | Historical data can grow; no measured load capacity or distributed quotas |
| Privacy leakage | Authenticated downloads, involved-person report list, no tracking | Viewer is whole-workspace; staff names/notes may contain personal data |

## Production conditions

TLS and trusted reverse proxy; production flag and secure cookie enabled; no demo seed; fresh secrets and restricted secret storage; dependency lock/digest review; migration credentials separated from runtime where feasible; encrypted off-host backups and restore drills; monitoring; vulnerability review; appropriate file scanning/quarantine; defined retention/erasure and lawful purpose; operator-specific contracts/privacy notices and staff training.

Do not promise GDPR compliance from the code alone. Do not call stored records legally certified inspections. Do not use the preview for real business data. Do not expose the Node development proxy publicly. Do not run integration tests on a live database.

## Explicit omissions

No MFA, OIDC/SAML, email account recovery, session sharing across replicas, malware scanner, e-signature, cryptographically sealed audit chain, automated retention/erasure, customer-scoped portal or self-service billing. These are not hidden completed features. Scope and release gates are in `docs/en/PRODUCT.md`.
