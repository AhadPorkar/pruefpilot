# Developer guide

## Start with the boundaries

Read the root README, architecture and API contract before editing. The live server is authoritative. The preview adapter must remain explicitly selected and visibly fictional. Do not replace failed backend requests with sample data. Do not add equipment-certification claims or automatic statutory intervals.

## Toolchain

The intended build uses Java 27, Maven 3.10.0, PostgreSQL 18.6, Node 26.12.1 and TypeScript 7.0.2. Current version sources are recorded in `docs/SOURCES.md`. The Docker build pins the toolchain versions. A complete first build needs normal access to Maven Central, npm and container registries.

The authoring environment only had Java 21, Node 22 and TypeScript 5.8.3; its successful tests are explicitly identified in the verification report. Those results do not replace a build with the intended stack.

## NetBeans workflow

Open `backend/pom.xml` as a Maven project in a NetBeans release that supports your installed Java 27 toolchain. Select that JDK in project/platform settings and configure Maven 3.10.0 if the bundled Maven differs. NetBeans is an editor/launcher here, not a runtime dependency, and no particular unverified IDE version is required.

Start a dedicated PostgreSQL database, configure the datasource and migration environment variables, and run `PruefPilotApplication`. For local container-backed DB development:

```bash
python scripts/setup.py --demo
docker compose -f compose.yml -f compose.dev.yml up -d db
python scripts/run-backend.py
```

`run-backend.py` reads local `.env`, maps the DB URL to localhost, and invokes Maven without printing secrets. It is development-only and refuses production configuration. On Windows install Maven on PATH (`mvn.cmd` is supported). Set the equivalent environment in NetBeans when launching from the IDE.

In a second terminal:

```bash
cd frontend
npm install
npm run build
node scripts/serve.mjs
```

Open `http://127.0.0.1:4173/` for live mode through the local API proxy, or append `?demo=1` for fictional preview. `API_PROXY` defaults to `http://127.0.0.1:8080`. The frontend server is not a hardened public reverse proxy. Re-run the compiler after edits; automatic hot reload is not included.

The source ZIP includes `frontend/dist`, so preview needs only Node. Production builds compile fresh sources; do not treat checked-in generated output as the source of truth.

## Tests

`npm test` runs built-JavaScript Node tests. Run `npm run build` first. The tests cover client rules, dates, permissions, the explicit preview contract, workflow state, export hashes, QR/ZIP helpers, all views/dialogs and HTTP adapter failure handling. `npm run test:e2e` runs Playwright against the locally served explicit preview in desktop/mobile Chromium. It does not test server authentication; Java HTTP/PostgreSQL integration covers that separately.

`mvn test` runs Java unit tests. `mvn verify` additionally runs `WorkflowIT` using an actual dedicated PostgreSQL datasource. Integration tests create their own workspaces and exercise the actual services, database constraints and HTTP session/CSRF boundary. Never point them at a customer database. CI provisions a disposable test database.

`sh scripts/test-domain.sh` compiles the actual framework-free rules and limiter with Java 21+ and runs 61 assertions, without Spring. It is useful offline but cannot validate Spring wiring, JDBC SQL or migration behavior.

`python -m unittest discover -s scripts/tests -v` checks configuration generation and ZIP-manifest verification. The GitHub workflow also builds and starts the Compose stack after frontend/backend jobs succeed.

## Adding a feature

Define the domain behavior and role/ownership rules first. Add a validated `Commands` record; never bind a database row or trust organization/status fields from the client. Add explicit tenant predicates and row locks in a transactional service. Back invariants with database constraints and a new Flyway migration. Expose a thin controller route, update `types.ts`/adapter/UI, add both language labels and escape all record strings. Add unit, PostgreSQL and relevant browser tests, then update the API and user guide.

A new file-storage provider must preserve atomic metadata/content lifecycle, quota checks, authenticated download and clear malware-scanning status. A new identity provider must preserve original author/assignee identity and independent review. Avoid direct DOM status edits that bypass the API state machine.

## Frontend conventions

The UI is a standard custom element with explicit routing and event delegation. Shared helpers escape attributes and text. CSS layers separate reset, base, layout, components, pages and responsive rules. Theme tokens define surfaces/text/borders rather than scattering hardcoded dark-mode patches. Prefer logical layout properties, visible focus, native form labels and reduced-motion support.

Add EN and DE keys together. The typed dictionaries catch key drift. Free-text business data is intentionally not machine-translated. Preserve form values during upload-triggered refresh. Never store credentials, CSRF tokens or report data in localStorage.

## Release workflow

Create a feature branch; run the tests; review changed migrations and EN/DE UI; open a pull request with sanitized screenshots and test output. The CI file is provided but must have a real successful run before a release badge is added. Review and commit the generated npm lockfile; then use `npm ci`. Pin verified image/action digests, audit dependencies and generate an SBOM in your release environment.

Tag `v1.0.0` only after the documented gates pass. Until then retain `1.0.0-rc.1`. Never copy demonstration names, screenshots or placeholder files into claims of actual customer usage.
