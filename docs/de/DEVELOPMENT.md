# Entwicklerhandbuch

## Einstieg und Werkzeuge

Zuerst README, [Architektur](ARCHITECTURE.md) und [API-Verträge](../architecture/API.md) lesen. Der Server entscheidet autoritativ. Die Vorschau muss ausdrücklich ausgewählt und als fiktiv gekennzeichnet bleiben. Backendfehler dürfen nicht zu Beispieldaten umgeleitet werden.

Vorgesehen sind Java 27, Maven 3.10.0, PostgreSQL 18.6, Node 26.12.1 und TypeScript 7.0.2. Docker pinnt den Build-Stack. Die Erstellungsumgebung besaß nur Java 21, Node 22 und TypeScript 5.8.3; deren dokumentierte Testergebnisse ersetzen keinen vollständigen Zielsystem-Build.

## NetBeans und lokaler Betrieb

`backend/pom.xml` als Maven-Projekt in einer zum JDK passenden NetBeans-Installation öffnen. Java 27 als Plattform auswählen und bei Bedarf Maven 3.10.0 konfigurieren. Die IDE ist kein Laufzeitbestandteil.

```bash
python scripts/setup.py --demo
docker compose -f compose.yml -f compose.dev.yml up -d db
python scripts/run-backend.py
```

Das Entwicklungsskript liest die lokale `.env`, setzt die Datenbankadresse auf localhost und startet Maven ohne Geheimnisausgabe. Produktionskonfiguration wird abgewiesen. Unter Windows muss `mvn.cmd` auf PATH liegen. Bei Start aus NetBeans dieselben Umgebungsvariablen konfigurieren.

In einem zweiten Terminal:

```bash
cd frontend
npm install
npm run build
node scripts/serve.mjs
```

`http://127.0.0.1:4173/` verwendet den echten Server über den lokalen Proxy; `?demo=1` wählt die fiktive Vorschau. `API_PROXY` verweist standardmäßig auf Port 8080. Nach Änderungen erneut kompilieren; automatisches Hot Reload ist nicht enthalten. Die bereits mitgelieferte `dist` erlaubt Vorschau ohne npm-Download.

## Tests

Nach `npm run build` führt `npm test` Node-Tests für Regeln, Rollen, Vorschauablauf, Export, QR/ZIP, Ansichten, Dialoge und HTTP-Adapter aus. `npm run test:e2e` prüft die ausdrückliche Vorschau mit Desktop-/Mobil-Chromium. Serverauthentifizierung wird davon getrennt im Java-Integrationstest geprüft.

`mvn test` startet Unit-Tests, `mvn verify` zusätzlich `WorkflowIT` gegen echtes PostgreSQL. Die Tests erzeugen eigene Testarbeitsbereiche und versuchen absichtlich ungültige Schreibzugriffe. Niemals eine Kundendatenbank als Testziel verwenden.

`sh scripts/test-domain.sh` prüft die tatsächlichen frameworkfreien Java-Regeln offline mit 61 Assertions. Es testet weder Spring-Konfiguration noch SQL. `python -m unittest discover -s scripts/tests -v` prüft Hilfsskripte. GitHub Actions baut anschließend den Containerverbund, sofern die vorherigen Jobs erfolgreich sind.

## Erweiterungskonventionen

Zuerst fachliche Regel, Rolle und Eigentümerschaft definieren. Validierten Eingabetyp in `Commands` ergänzen; Organisation oder Zielstatus nie unkontrolliert vom Client übernehmen. Mandantenfilter, Sperren und Transaktion im Service implementieren, Datenbankinvarianten durch Constraints und neue Migration absichern. Controller schlank halten. UI-Verträge, EN/DE, Maskierung und Tests aktualisieren.

Bereits eingesetzte Migrationen nicht bearbeiten. Veröffentlichte Vorlagen und eingereichte Antworten nicht still umschreiben. Neue Identitäts-/Speicheranbieter müssen Personenzuordnung, unabhängige Prüfung, Kontingente und geschützte Downloads erhalten.

Das Frontend nutzt Standard-DOM, ein Custom Element, native Dialoge und Ereignisdelegation. Gemeinsame Helfer maskieren Benutzerdaten. CSS-Layer und Theme-Tokens strukturieren das Design. Fokus, Beschriftungen, reduzierte Bewegung und Erhalt ungespeicherter Formulardaten beachten. Keine Passwörter, CSRF-Token oder Berichtsdaten in localStorage speichern.

## Veröffentlichung

Pull Request mit Tests und bereinigten Screenshots erstellen. Ein CI-Badge erst nach tatsächlichem Erfolg. Beim ersten Netzbuild Lockdatei erzeugen, prüfen und committen; danach `npm ci` nutzen. Image-/Action-Digests festlegen, Abhängigkeiten prüfen und SBOM erzeugen. `v1.0.0` erst nach Freigabekriterien taggen, vorher RC-Bezeichnung beibehalten. Fiktive Beispiele niemals als echte Kundenerfahrung darstellen.
