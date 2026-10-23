# Contributing

Use the developer and architecture guides. Changes should preserve the bounded product: asset identity, evidence-backed inspections, explicit schedule decisions and independently verified corrective work.

Create a branch, add tests with behavior changes, run frontend build/tests and Java `mvn verify` against a disposable PostgreSQL database, and include EN/DE labels. A new business query must include organization scope. A new mutation needs role/ownership checks, state/version checks, transaction/audit behavior and negative tests. Never edit an applied Flyway migration.

Do not add real customer data, secrets, copyrighted customer templates or unsupported regulatory claims. Keep fictional examples visibly fictional. Submit security issues privately according to SECURITY.md.

Review generated/AI-assisted code as ordinary untrusted code: inspect it, run the tests and own the release decision. The presence of tests or a generated document is not proof of production readiness. Never mark an unexecuted check passed.

The initial source is MIT licensed. Contributions should be compatible with that license. No external runtime fonts, images or icon packages are bundled. Do not introduce dependencies without version, license and security review.
