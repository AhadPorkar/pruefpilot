# Publish the source and prepare a release

## Publish an honest portfolio repository

Review the MIT license before publishing: it allows others to use and redistribute the source, including commercially. Remove real customer data, credentials and private infrastructure details. The provided screenshots and sample exports use fictional data.

Create an empty GitHub repository named `pruefpilot` under your account. From the extracted project root, run:

```bash
git init -b main
git add .
git status --short
git diff --cached --stat
# Inspect the staged file list. Never stage .env, dumps, uploads or customer records.
git commit -m "Initial PruefPilot release candidate"
git remote add origin git@github.com:AhadPorkar/pruefpilot.git
git push -u origin main
```

Use a different remote when that name is already occupied. Do not force-push over an existing repository. These commands require your own GitHub authentication; no token or publishing permission is bundled.

Enable repository Actions and inspect the actual frontend, backend and container jobs. Do not add a passing badge or claim the backend was tested until the real run succeeds. The first networked install generates `frontend/package-lock.json`; review and commit it, change install steps to `npm ci`, and run the workflow again.

Suggested description: **Asset inspections, evidence and independently verified corrective actions. Spring Boot, PostgreSQL, TypeScript. EN/DE.** Suggested topics: `spring-boot`, `postgresql`, `typescript`, `inspection-management`, `maintenance`, `bilingual`, `modular-monolith`.

Pin the repository on your profile. Lead with the actual screenshots and the asset-passport workflow. Keep the verification ledger and release-candidate status visible. Explain the transaction, authorization and independent-review decisions in interviews rather than presenting fictional seed data as real customers.

## Demonstrate without exposing customer data

The bundled preview runs locally with `node frontend/scripts/serve.mjs`. A separately hosted static preview must serve `frontend/dist` over HTTPS and use the explicit `?demo=1` entry URL. It must contain only fictional data, expose no backend secrets and retain the preview banner. This package does not configure a hosting provider or automatically publish a site.

## Promote to a commercial release

Complete the product release gates, run the target-toolchain build and full integration/E2E suites, review supply-chain locks/digests, complete security and restoration checks, and obtain qualified review of the intended templates and schedules. Define the operator's hosting, support, privacy, retention and contractual obligations. Do not call an unsigned report a certificate.

After a real approval decision, update release/version files consistently, record the actual evidence, create the release tag and attach checksummed artifacts. Preserve RC history instead of rewriting failed or unexecuted checks as passes.
