# Research and version provenance

Research date: **2026-10-23**. These are primary vendor/government sources consulted for the chosen baseline and product boundary. Versions can change; re-check before a later release. Public page access is not equivalent to downloading, compiling or testing the dependency.

| Decision | Source and observed baseline |
|---|---|
| Spring Boot 4.1.2, Java up to 27 | https://docs.spring.io/spring-boot/system-requirements.html ; https://spring.io/blog/2026/10/23/spring-boot-4-1-2-available-now/ |
| PostgreSQL 18.6 stable baseline | https://www.postgresql.org/docs/release/18.6/ ; https://www.postgresql.org/download/ |
| Maven 3.10.0 | https://maven.apache.org/download.cgi ; official Docker manifest below |
| Node 26.12.1 Current (build-only) | https://nodejs.org/en/download |
| TypeScript 7.0.2 | https://registry.npmjs.org/typescript/latest |
| Playwright 1.64.1 | https://registry.npmjs.org/@playwright/test/latest |
| Java 27 GA and Temurin image | https://jdk.java.net/27/ ; https://raw.githubusercontent.com/docker-library/official-images/master/library/eclipse-temurin |
| Maven Java 27 image | https://raw.githubusercontent.com/docker-library/official-images/master/library/maven |
| Spring Boot Jackson 3 integration | https://docs.spring.io/spring-boot/reference/features/json.html |
| Spring Security headers API | https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/config/annotation/web/configurers/HeadersConfigurer.html |
| CI action releases | https://github.com/actions/checkout/releases ; https://github.com/actions/setup-node/releases ; https://github.com/actions/setup-java/releases ; https://github.com/actions/upload-artifact/releases |
| Broad inspection competitor | https://lumiformapp.com/ |
| German inspection-record boundary | https://www.gesetze-im-internet.de/betrsichv_2015/__14.html |
| Privacy engineering context | https://www.bfdi.bund.de/DE/Fachthemen/Inhalte/Technik/SDM.html |
| Owner-provided portfolio references | https://ahadporkar.com/ ; https://github.com/AhadPorkar ; https://www.linkedin.com/in/seapr/ |

The public website and GitHub reference were inspected for attribution/context. LinkedIn could not be retrieved in the research environment; no claims are based on unseen profile content. No private repository was read or modified. No application was published to GitHub or deployed to a host by this delivery.

Stable/GA lines were preferred to release candidates. Java 27 and Node Current satisfy the requested current-version direction, but non-LTS maintenance cadence must be accepted by the operator. Spring Boot's BOM selects compatible transitive dependencies instead of independently forcing every library to its newest unrelated release. Docker images and CI actions use verified version tags; immutable digest pinning remains a release gate.

The chosen TypeScript and Playwright versions could not be installed in the restricted build environment. A trustworthy transitive npm lockfile was therefore not fabricated. The delivered compiled frontend was built with the available TypeScript 5.8.3 and tested with Node 22.16.0; see the verification ledger.

The pricing range originated in the user's product hypothesis and is not independently validated market data. No customer interviews, paid pilots, statutory approval, trademark clearance or penetration test are claimed.
