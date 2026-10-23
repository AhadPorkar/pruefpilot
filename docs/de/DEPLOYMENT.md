# Installation, Betrieb und Wiederherstellung

Version 1.0.0-rc.1. Dieses Handbuch beschreibt den vorgesehenen Betrieb. Es behauptet keinen bereits erfolgreich ausgeführten Container- oder Datenbanktest.

## Lokale Erprobung

Voraussetzungen: Docker mit Compose, Python 3 und Internetzugriff auf Container-, npm- und Maven-Registries.

```bash
python scripts/setup.py --demo --email owner@example.test
docker compose up --build -d
docker compose ps
docker compose logs --tail 100 app
```

Anmeldung unter `http://localhost:8080`. `ADMIN_EMAIL` und `ADMIN_PASSWORD` stehen in der neu erzeugten `.env`. Das Skript erzeugt zufällige Geheimnisse und überschreibt keine vorhandene Datei. Unter Windows die Dateiberechtigungen auf das eigene Konto beschränken. Die Datei niemals committen oder in Issues hochladen.

Ohne `--demo` entsteht ein leerer Arbeitsbereich. Initialisierung erfolgt nur bei leerer Datenbank. Ein Neustart setzt keine Passwörter zurück. Eine Demo-Datenbank nicht durch bloßes Umschalten von `DEMO_DATA` zur Kundenumgebung machen, sondern sauber getrennt neu bereitstellen.

## Aufbau und Rechte

Ein Image enthält Frontend und Spring-Boot-JAR; beide werden unter derselben Origin ausgeliefert. PostgreSQL besitzt standardmäßig keinen Host-Port. Die Anwendung bindet den Host-Port nur an `127.0.0.1`. Sie läuft als UID/GID 10001 mit schreibgeschütztem Root-Dateisystem, beschränktem `/tmp` und entfernten Linux-Capabilities. Das PostgreSQL-18-Volume wird unter `/var/lib/postgresql` eingebunden.

`pruefpilot_owner` dient Migrationen und Schemaänderungen; `pruefpilot_app` besitzt nur die vorgesehenen DML-Rechte. Die Laufzeitrolle kann den Audit-Trigger nicht deaktivieren und keine Tabellen truncaten. Das privilegierte `postgres`-Konto wird nicht an die Anwendung weitergegeben.

Im Starter-Deployment sind Migrationszugangsdaten beim Anwendungsstart vorhanden. Für strengeren Produktionsbetrieb Migrationen in einen separaten Release-Schritt verlagern, danach `MIGRATIONS_ENABLED=false` setzen und Migrationsgeheimnisse aus der Laufzeit entfernen. Die Initialisierungsskripte laufen nur bei leerem Volume. Eine geänderte `.env` rotiert bestehende Datenbankpasswörter nicht.

Die Architektur ist ein einzelner Knoten mit lokalen Sitzungen und prozesslokalen Limits. Mehrere Replikate benötigen gemeinsame Sitzungen, globale Ratenbegrenzung und Lasttests. Ein Deployment provisioniert einen Arbeitsbereich; es ist keine vollständige Self-Service-SaaS-Plattform.

## Wichtige Einstellungen

| Einstellung | Bedeutung |
|---|---|
| `DATABASE_USER`, `DATABASE_PASSWORD` | Laufzeitrolle und Geheimnis |
| `MIGRATION_USER`, `MIGRATION_PASSWORD` | Flyway-/Schema-Rolle, möglichst nur im Release-Job |
| `POSTGRES_ADMIN_PASSWORD` | Initiales privilegiertes PostgreSQL-Geheimnis |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Einmalige Erstbereitstellung, keine spätere Passwortzurücksetzung |
| `ORGANIZATION_NAME`, `ORGANIZATION_TIMEZONE` | Initialer Arbeitsbereich und IANA-Zeitzone |
| `PUBLIC_URL` | Öffentliche URL für Startvalidierung; QR nutzt die Browser-Origin |
| `PRODUCTION`, `COOKIE_SECURE` | Für öffentlichen HTTPS-Betrieb beide `true` |
| `DEMO_DATA` | Im Produktionsbetrieb zwingend `false` |

Ein HTTPS-Textwert ersetzt kein echtes TLS. Secure-Cookies funktionieren nicht wie erwartet über unverschlüsseltes HTTP.

## Öffentliches Hosting

TLS an einem gepflegten Reverse Proxy terminieren, HTTP auf HTTPS umleiten und nur den vorgesehenen Hostnamen auf den lokalen Anwendungsport weiterleiten. `PRODUCTION=true`, `COOKIE_SECURE=true`, `DEMO_DATA=false` sowie korrekte HTTPS-URL setzen. Weiterleitungsheader nur vom vertrauenswürdigen Proxy akzeptieren; fremde Header dort ersetzen. `SERVER_FORWARD_HEADERS_STRATEGY=framework` erst nach Einrichtung dieser Vertrauensgrenze verwenden.

Am Proxy HSTS nach Erprobung, ein geeignetes Uploadlimit, Zeitlimits, eine IP-basierte Loginbegrenzung und datensparsame Protokollierung einrichten. Der prozesslokale Loginbegrenzer sieht ohne vertrauenswürdige Proxykonfiguration die Verbindungsadresse des Proxys. Beliebige `X-Forwarded-For`-Werte nicht als echte Clientadresse behandeln.

Geheimnisverwaltung, Hosthärtung, verschlüsselte Datenträger und Sicherungen, Patchprozess und Alarmierung sind Betreiberaufgaben. Nach erfolgreichem Build npm-Lockdatei prüfen/committen und Images/Actions auf geprüfte Digests festlegen. Source Maps sind für Entwicklung enthalten und können aus einer gehärteten Veröffentlichung entfernt werden. Compose ist kein Nachweis für Hochverfügbarkeit oder Datenschutzkonformität.

## Überwachung

`docker compose ps`, Health-Endpunkt und bereinigte Anwendungs-/Datenbanklogs prüfen. Verfügbarkeit, Fehlerquote, Latenz, Anmeldefehler, Datenbankverbindungen, Speicherplatz/WAL, Nachweisvolumen und Sicherungsalter überwachen. Es wurden keine produktiven Leistungs- oder Wiederherstellungskennzahlen gemessen.

Der Starter begrenzt den App-Container auf 1 GiB und `/tmp` auf 128 MiB. Zwei parallele Exporte sind zulässig; Nachweise werden dabei im Speicher verarbeitet. Das Anlagenlimit ist kein Benchmark. Vor Erhöhung mit realistischen Dateien und Historien testen.

## Sicherung und Wiederherstellung

Metadaten und Nachweise liegen gemeinsam in PostgreSQL. Eine konsistente Datenbanksicherung umfasst daher beides.

```bash
sh scripts/backup.sh
```

Das Skript schreibt ein Custom-Format-Dump nach `backups/`, verschlüsselt es aber nicht. Anschließend verschlüsseln, getrennt vom Host aufbewahren und nach vereinbarter Frist löschen. Konfiguration und Geheimnisse separat sicher sichern. Ein DB-Dump sichert keine globalen Rollen oder `.env`.

Unter Windows eine POSIX-Shell oder geprüfte Backupsoftware verwenden; ungeeignete Textumleitung älterer PowerShell-Versionen kann Binärdumps beschädigen.

Wiederherstellung nur in einer bewusst ausgewählten, isolierten Umgebung mit vorbereiteten Rollen:

```bash
sh scripts/restore.sh backups/beispiel.dump --confirm-overwrite
```

Das Skript stoppt die App und überschreibt Zielobjekte mit `pg_restore --clean --if-exists --exit-on-error`. Es startet die App absichtlich nicht. Zuerst Migrationsstand, Datensatzzahlen, Dateihashes, Konten und einen Beispielbericht prüfen, dann `docker compose up -d app`. Tatsächliche Wiederherstellungsdauer und Datenverlustfenster protokollieren. Integrationstests niemals gegen Kundendatenbanken ausführen.

## Aktualisierung und Rückfall

Vor Updates sichern, Release auf einer wiederhergestellten Staging-Datenbank testen, Migrationen lesen und Image-Digests festhalten. Bereits eingesetzte Flyway-Migrationen nicht verändern, sondern neue Versionen hinzufügen. Ein Code-Rollback ist nur bei Schemakompatibilität sicher; ansonsten geprüfte Sicherung mit ausdrücklicher Entscheidung über Datenverlust wiederherstellen.

`docker compose down` erhält das Volume. **`docker compose down -v` löscht Daten** und ist nur für Wegwerf-Testumgebungen vorgesehen.

## Kontowiederherstellung und Aufbewahrung

Es gibt keine E-Mail-Passwortzurücksetzung. Zwei benannte Administratoren und einen geschützten Wiederherstellungsprozess vorhalten. Im Notfall darf ein autorisierter Betreiber mit einem separat geprüften BCrypt-Werkzeug einen Hash erzeugen und ihn durch eine eng begrenzte, dokumentierte SQL-Änderung setzen. Keine Klartextpasswörter in SQL, Logs oder Issues kopieren. Danach Passwortwechsel und Vorfallprüfung veranlassen.

Automatisierte Aufbewahrung und Löschung fehlen. Vor Kundeneinsatz Rechtsgrundlage, Fristen, Sperrvermerke, Auskunft, Pseudonymisierung und Löschung festlegen. Ein Append-only-Audit rechtfertigt keine unbegrenzte Speicherung personenbezogener Daten. Fachkundige Datenschutz-/Rechtsprüfung sowie getestete Wartungsverfahren sind erforderlich.

Vor produktivem Einsatz alle [Freigabekriterien](PRODUCT.md) einschließlich echter CI-Läufe, Sicherheitsprüfung, Restore-Test, fachlicher Vorlagenprüfung, AV-Entscheidung, Verträgen und Schulung abschließen.
