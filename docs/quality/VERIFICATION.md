# Verification ledger

**Release:** 1.0.0-rc.1. **Recorded:** 23 October 2026. This ledger separates executed evidence from tests supplied for a later networked run.

## Executed in the authoring environment

| Check | Actual result | Evidence |
|---|---|---|
| Frontend strict compilation | Passed using installed TypeScript 5.8.3, not configured 7.0.2 | `tsc -p frontend/tsconfig.json`; compiled dist included |
| Node frontend tests | **145 passed; 0 failed; 0 skipped** on Node 22.16.0 | `frontend-tests.tap` |
| Actual framework-free Java rules + limiter | **61 assertions passed** on installed Java 21 | `java-domain-tests.txt`; `scripts/DomainSmoke.java` |
| Isolated interactive component checks | **22 checks passed**, no uncaught component errors | `component-verification.json` |
| Static-render layout checks | **66 checks**, no document-width overflow at tested viewports/dialogs | `layout-verification.json` |
| QR interoperability sample | Generated SVG rendered and decoded to the expected asset URL | `layout-verification.json` `qrDecoded` |
| Operator tool unit tests | **17 passed** using Python standard library | `tool-tests.txt`; `scripts/tests/test_tools.py` |
| Fictional EN/DE report packages | Both manifests verified: six payload files each | `docs/examples/DEMO-report-*.zip`; report verifier |
| Offline documentation UI | **6 checks passed**, language selection and 390/768/1440px overflow | `documentation-verification.json` |
| Python/shell/Node syntax checks | Passed for supplied setup/verification scripts and local server | Commands in operator/developer guide |

The component harness mounted the actual TypeScript custom element, views and preview adapter into an isolated Chromium document. It exercised language/theme events, route changes, QR modal, assigned repair upload, preservation of unsaved notes, independent closure, Viewer restrictions, site creation and mobile navigation. Because browser navigation was blocked by managed environment policy, the harness explicitly forced the preview adapter and supplied test-only crypto primitives in `about:blank`. It did **not** test live URLs, HTTP cookies, CSRF, real camera access or server persistence. Browser policy was not changed.

Static screenshots were generated from actual application render functions with fictional seed data, not from a running authenticated backend. Static layout checks are not an accessibility certification or browser E2E substitute. The single QR decode is useful interoperability evidence, not exhaustive standards conformance certification.

## Supplied but NOT executed here

| Suite / operation | Included artifact | Status |
|---|---|---|
| Java JUnit unit suite | 19 unit-test classes across domain/auth/catalog/inspection/action/evidence/report/audit/api/shared | Not executed: Maven/dependencies unavailable |
| Real PostgreSQL service/constraint/HTTP tests | `WorkflowIT.java`, 10 test methods | Not executed: PostgreSQL/Docker unavailable |
| Intended Java 27 / Spring Boot 4.1.2 build | `backend/pom.xml`, Dockerfile, CI | Not executed with target toolchain |
| TypeScript 7.0.2 build | `frontend/package.json` | Not executed: registry installation unavailable |
| Playwright served-URL E2E | Four scenarios, desktop/mobile Chromium projects | Not executed: managed browser navigation restriction |
| Compose startup and migration-role separation | Dockerfile, compose.yml, init-db.sh | Not executed: no Docker runtime |
| CI | `.github/workflows/ci.yml` | Not run or published to the user's GitHub |
| Load, penetration, restore, accessibility audits | Release checklist / runbooks | Not claimed; operator gates |

## Module test map

| Module | Java unit evidence supplied | Other coverage |
|---|---|---|
| Inspection decisions | `InspectionRulesTest` | Actual domain harness; frontend outcome tests |
| Cadence | `ScheduleRulesTest` | Domain harness; preview approval/rejection tests |
| Corrective-action rules | `ActionRulesTest` | Domain harness; component repair-to-closure |
| File and export rules | `FileRulesTest`, `ExportRulesTest` | Domain harness; frontend MIME/hash/CSV tests |
| Auth and accounts | `ActorTest`, `LoginLimiterTest`, `MemberServiceTest`, `AuthControllerTest` | `WorkflowIT` HTTP/CSRF/revocation; frontend adapter mocks |
| Catalog | `CatalogServiceTest` | `WorkflowIT` constraints/archive; preview and component create |
| Inspection orchestration | `InspectionServiceTest` | `WorkflowIT` atomicity/idempotency/concurrency/self-review |
| Corrective-action orchestration | `ActionServiceTest` | `WorkflowIT` and preview/component full lifecycle |
| Evidence | `EvidenceServiceTest` | `WorkflowIT` storage/download; preview limits/hash |
| Report | `ReportServiceTest` | `WorkflowIT` manifest; frontend original-evidence ZIP tests |
| Audit | `AuditServiceTest` | `WorkflowIT` append-only trigger |
| API/read model | `CommandsTest`, `WorkspaceServiceTest` | HTTP integration, EN/DE view/dialog tests |
| Shared error/data helpers | `DbTest`, `ErrorsTest` | Integration SQL/rollback and startup wiring |
| UI/navigation/QR/ZIP | Frontend test files | Component/layout checks; Playwright supplied |

No claim of 100% line/branch coverage is made. A test's existence is not an executed pass. The full Java/Spring boundary cannot be established by the standalone domain harness. Source and CI are delivered so the remaining checks can be run in a normal supported environment.

## Reproduction

```bash
sh scripts/test-domain.sh
python -m unittest discover -s scripts/tests -v
cd frontend
npm install
npm run build
npm test
npx playwright install --with-deps chromium
npm run test:e2e
cd ../backend
# Dedicated disposable PostgreSQL database configured via DATABASE_* variables
mvn verify
```

Then run the Compose smoke test and an isolated backup/restore drill. Review the generated lockfile before switching to `npm ci`. Preserve actual logs and release image digests. Never change this ledger to mark a check passed based solely on source review.
