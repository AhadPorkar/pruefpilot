# Architektur und Codeübersicht

Die ausführliche Referenz mit Klassenkarte, Datenmodell, Zustandsdiagrammen und Entscheidungen steht in [ARCHITECTURE.md](../architecture/ARCHITECTURE.md); alle HTTP-Verträge in [API.md](../architecture/API.md).

## Aufbau

PrüfPilot ist ein modularer Monolith: eine Spring-Boot-Anwendung liefert die native TypeScript-Oberfläche und die API unter derselben Origin aus. PostgreSQL speichert Datensätze, Nachweisdateien und Audit-Ereignisse. Ein ausdrücklicher, fiktiver Vorschauadapter ist strikt vom Serverbetrieb getrennt.

Die Organisation und Rolle stammen aus dem authentifizierten Principal, niemals aus einem vom Client gewählten Organisationsfeld. Alle Service-Abfragen sind organisationsbezogen. Zusammengesetzte Fremdschlüssel verhindern organisationsfremde Referenzen. PostgreSQL-RLS wird nicht verwendet und nicht behauptet. Ein Deployment provisioniert in Version 1 einen Arbeitsbereich.

## Module

`api` enthält validierte Eingaben, Controller und Leseprojektionen. `auth` verwaltet Sitzungen, CSRF, Rollen, Konten, BCrypt und Sitzungsentzug. `catalog` betreut Standorte, Anlagen, Vorlagen und Pläne. `inspection` orchestriert Entwurf, Einreichung, Mangelanlage und Begutachtung. `action` erzwingt Zuweisung, Reparatur und unabhängigen Abschluss. `evidence` prüft Elternrechte, Zustand, Dateityp und Kontingent. `report` erzeugt konsistente ZIP-Pakete. `audit` schreibt Ereignisse innerhalb der fachlichen Transaktion. `shared` enthält SQL-Adapter, Fehlerbehandlung und Erstbereitstellung. `domain` ist frameworkfrei und separat mit Java testbar.

JDBC macht Mandantenfilter, PostgreSQL-Sperren und Exportabfragen ausdrücklich sichtbar. Der Preis dafür sind manuelle Zuordnung und echte Datenbanktests. H2 ersetzt diese Tests nicht.

## Transaktionen und Konkurrenz

Prüfungsstarts verwenden einen organisationsbezogenen Idempotenzschlüssel und Advisory-Transaktionssperren. Gleicher Schlüssel mit gleichen Daten liefert denselben Datensatz; abweichende Daten erzeugen einen Konflikt. Ein partieller Unique-Index erlaubt nur eine laufende Prüfung pro Plan.

Einreichung sperrt die Prüfung, kontrolliert Person/Zustand/Version, validiert alle Antworten, berechnet Ergebnisse serverseitig, erzeugt Mängel und Audit-Ereignisse und schreibt den Status atomar. Fehler rollen alles zurück. Die Begutachtung muss durch eine andere berechtigte Person erfolgen. Nur Zustimmung schreibt den ursprünglichen Kalenderrhythmus fort.

Reparaturen verwenden Zeilensperren und optimistische Versionen. Nur die zugewiesene Person darf reparieren. Fertigmeldung erfordert Text und Nachweis. Verifizierung verlangt eine andere berechtigte Person. Upload und Abschluss sperren denselben Elternsatz, damit kein verspäteter Upload in einen bereits abgeschlossenen Zustand rutscht. Das Anlagenkontingent wird ebenfalls unter Sperre geprüft.

## Zeit und Historie

Ereignisse liegen als UTC-Zeitpunkte vor, Termine als Kalendertage. Die Arbeitsbereichszeitzone bestimmt den heutigen Tag und die für den Rhythmus relevante Ausführungsdatumssicht. Ein Zeitstempel im System ist kein unabhängiger Nachweis des realen Untersuchungszeitpunkts.

Freigegebene Vorlagen und eingereichte Antworten werden nicht still geändert. Neue Versionen beziehungsweise neue Prüfungen erhalten neue Identitäten. Ein Trigger blockiert normale Audit-Updates und -Löschungen, jedoch keinen privilegierten Datenbankbetreiber. Aufbewahrung und Pseudonymisierung benötigen einen geregelten Betreiberprozess.

## Frontend

`types.ts` definiert Verträge, `i18n.ts` EN/DE, `api.ts` Sitzung/CSRF, `demo.ts` die ausdrückliche Vorschau. `views.ts` und `dialogs.ts` erzeugen maskierte HTML-Inhalte. `app.ts` verwaltet Routing, Ereignisse, Formulare, Dateien und Kamera. `qr.ts` erzeugt echte QR-Codes ohne externen Dienst, `zip.ts` Vorschauarchive. CSS enthält responsive, helle/dunkle, Fokus-, Bewegungs- und Druckregeln.

Kundendaten werden nicht an QR-Dienste, Fonts, CDNs, Analytics oder KI-Modelle gesendet. Es gibt keinen Service Worker und keine Offline-Synchronisation. Browserprüfungen sind Komfort; der Server entscheidet autoritativ.

## Export und Grenzen

Nachweise liegen atomar mit Metadaten in PostgreSQL. Das vereinfacht Konsistenz und Sicherung für den begrenzten Umfang, erhöht jedoch Datenbank- und Backupvolumen. Exporte verwenden Repeatable Read und enthalten HTML, JSON, CSV, Originale und Prüfsummen. Formeln in CSV werden neutralisiert, HTML-Inhalte maskiert. Prüfsummen sind keine digitale Signatur, Dateisignaturen keine Virenprüfung.

Vor Erweiterungen immer Berechtigung, Organisationsfilter, Transaktion, fachliche Regeln, Migration, EN/DE, Unit-/Integrationstests und Dokumentation ergänzen. OIDC/MFA, Wiederherstellung, kontrollierte Stammdatenkorrekturen, gescannter Objektspeicher und Aufbewahrung sind sinnvolle Folgeschritte. Predictive Maintenance, Pflichtsensoren und automatische Zertifizierung bleiben außerhalb Version 1.
