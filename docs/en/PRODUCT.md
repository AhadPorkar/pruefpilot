# Product positioning and commercial launch

## The chosen first customer

A workshop/facilities operator or maintenance contractor with a modest, identifiable asset register, recurring operational checks and a supervisor who needs evidence-backed closure. The buyer is an operations/facilities manager or maintenance coordinator. The first sales motion should be a guided B2B pilot, not an unattended self-service SaaS launch.

The central promise is operational: **show due work, asset-specific observations, unresolved defects and independently verified repairs in one passport**. Do not promise automatic legal compliance, certified equipment or a statutory interval engine.

## Differentiation that still needs validation

Lumiform already markets digital inspections, offline capture, corrective actions, reports and enterprise capabilities. Those features alone are not a defensible market gap. PrüfPilot's hypothesis is a narrower asset-centric workflow with explicit repair ownership, four-eyes closure and a controlled customer handover package. This implementation is a technical starting point; it does not prove that buyers prefer it over established products.

Pick one operational sub-niche for interviews: for example, a workshop group coordinating its internal visual-observation and corrective-work records. The example lift/compressor/ladder records are fictional and do not define approved procedures for those potentially regulated asset classes. Engage a qualified domain partner before distributing specialist templates.

Ask operators to demonstrate how they currently identify an asset, find the most recent check, assign a failed item, distinguish “repaired” from “verified”, and prepare a customer handover. Observe actual records with permission rather than collecting only opinions about dashboards.

## Pilot success measures

Agree a baseline and measure time to identify the correct asset, time to find its latest evidence, percentage of overdue checks, age of open corrective work, rejected repair submissions and time to prepare a customer package. Track whether staff use individual identities and whether independent review is practical. Do not invent an efficiency percentage before observing the pilot.

Keep the pilot within the supported deployment/asset/evidence limits. Use operator-approved templates and explicit intervals. Confirm network coverage: there is no offline synchronization. Record support burden, export size and DB growth. Interview the supervisor and the technician separately; the person buying and the person uploading evidence have different constraints.

## Packaging and pricing hypothesis

Test EUR 79–249 per site/month, or an organization plan with agreed active-asset and storage limits. This is a research hypothesis, not an implemented price page, billing system or validated revenue forecast. Quote onboarding, hosting, backup/recovery, support hours and domain-template review separately where appropriate. Avoid “unlimited evidence” while the implementation uses bounded database storage.

The initial deployment can be sold as a managed installation/service after release gates and contract review. There is no checkout, invoice engine, subscription metering, public organization signup or customer-scoped portal. Do not describe those as completed features. The MIT source license permits commercial use but also redistribution; selling hosting/support under MIT is different from selling exclusive proprietary code.

## Legal and privacy boundary

German equipment-inspection record obligations can include more than a saved checklist. For example, BetrSichV section 14 addresses specified inspection records and responsible-person information/signature requirements. Applicability, exact record content, competence and electronic-signature adequacy require qualified review for the actual scenario. A normal account event or unsigned ZIP manifest is not automatically a legally sufficient signature.

Do not label templates “DGUV-certified”, “legally compliant” or “authorized certification” without a valid basis. Do not manufacture statutory intervals from asset category. The supplied arbitrary demo cadence is clearly marked as fictional.

For customer data, agree controller/processor responsibilities, lawful purpose, retention, hosting region, subprocessors, access, breach response and data-subject request handling. No product can establish all of these solely by providing an EU-hosted database. Public commercial presence may require operator-specific legal notices and contracts; this repository does not invent a postal address, tax number, DPA, SLA or legal advice for its owner.

Primary context: [BetrSichV section 14](https://www.gesetze-im-internet.de/betrsichv_2015/__14.html), [BfDI Standard Data Protection Model](https://www.bfdi.bund.de/DE/Fachthemen/Inhalte/Technik/SDM.html), [Lumiform](https://lumiformapp.com/). See `docs/SOURCES.md` for the research date and scope.

## Release gates

Do not remove the RC label until the relevant gates are signed off with real evidence:

- [ ] Networked build succeeds with Java 27, Spring Boot 4.1.2, PostgreSQL 18.6 and TypeScript 7.0.2; every unit/integration/browser/container CI job passes.
- [ ] Generated npm lockfile reviewed and committed; CI/build moved to `npm ci`; image/action digests pinned; dependencies audited and SBOM archived.
- [ ] Clean install, restart, upgrade, migration and isolated restore drill verified on intended hosting; monitoring/support contacts and incident runbook assigned.
- [ ] Authorization, cross-workspace access, CSRF, upload handling, session revocation and export privacy independently reviewed; penetration testing scoped to deployment risks.
- [ ] Malware scanning/quarantine process chosen and enforced for real customer uploads; evidence retention and deletion/pseudonymization designed and tested.
- [ ] Representative history/evidence load and simultaneous operations tested; acceptable performance, recovery objectives and resource limits agreed rather than assumed.
- [ ] Real mobile devices, keyboard/screen-reader paths, contrast and focus checked; no claim of WCAG certification until an appropriate audit exists.
- [ ] Qualified domain reviewer approves real templates, intervals, competence and required signatures; demo records excluded; safety/stop-use procedures documented outside the app.
- [ ] Customer pilot validates niche value, pricing and actual staff workflow; account recovery and two-person supervision are operationally workable.
- [ ] Operator-specific privacy/legal notices, DPA/hosting terms, license strategy, support scope and contracts reviewed; brand/trademark availability checked.

## Recruiter walkthrough

Start the compiled preview. Show the overview and an asset passport, switch EN/DE and dark mode, then use the preview roles to upload repair evidence as the technician and verify it as the supervisor. Export a report and verify the manifest. Open the actual service and test files explaining the same rules on the server. Explain why the project is a modular monolith and why evidence is bounded.

Present this as an independently reviewable portfolio release candidate, not a claimed customer deployment or invented employment achievement. A successful personal GitHub CI run and a small hosted fictional demo will strengthen the portfolio more than unsupported “production-ready” badges.
