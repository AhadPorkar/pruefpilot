# Quellcode veröffentlichen und Release vorbereiten

## Portfolio-Repository

Vor der Veröffentlichung die MIT-Lizenz prüfen: Sie erlaubt auch Dritten die kommerzielle Nutzung und Weitergabe. Keine echten Kundendaten, Zugangsdaten, Datenbank-Dumps oder internen Infrastrukturdetails veröffentlichen. Screenshots und Beispielberichte dieses Pakets enthalten fiktive Daten.

Im eigenen GitHub-Konto ein leeres Repository `pruefpilot` erstellen. Im entpackten Projektverzeichnis:

```bash
git init -b main
git add .
git status --short
git diff --cached --stat
# Die vorgemerkten Dateien kontrollieren. .env und Kundendaten niemals aufnehmen.
git commit -m "Initial PruefPilot release candidate"
git remote add origin git@github.com:AhadPorkar/pruefpilot.git
git push -u origin main
```

Bei einem bereits belegten Namen ein anderes Repository verwenden. Kein bestehendes Projekt durch Force-Push überschreiben. Die eigene GitHub-Authentifizierung ist erforderlich; das Paket enthält keinen Zugangstoken.

Actions aktivieren und die tatsächlichen Frontend-, Backend- und Container-Ergebnisse kontrollieren. Ein erfolgreiches Badge erst nach einem echten erfolgreichen Lauf hinzufuegen. Die beim ersten vernetzten Installieren erzeugte `frontend/package-lock.json` prüfen und einchecken; danach `npm install` in den Build-Schritten durch `npm ci` ersetzen und erneut testen.

Als Beschreibung eignet sich: **Anlagenprüfungen, Nachweise und unabhängig bestätigte Korrekturmaßnahmen. Spring Boot, PostgreSQL, TypeScript. EN/DE.** Das Repository im Profil anheften. Anlagenpass, echte Oberflächen-Screenshots, Architekturentscheidungen und nachvollziehbare Testergebnisse in den Vordergrund stellen. Fiktive Beispiele niemals als produktive Kundenreferenzen ausgeben.

## Demonstration und kommerzielle Freigabe

Die lokale Vorschau startet mit `node frontend/scripts/serve.mjs`. Eine separat gehostete statische Vorschau muss `frontend/dist` per HTTPS bereitstellen und mit `?demo=1` geöffnet werden. Vorschauhinweis beibehalten; keine Kundendaten oder Backend-Geheimnisse verwenden. Das Paket veröffentlicht keine Website automatisch.

Vor der kommerziellen Freigabe die Produkt-Freigabekriterien abschliessen: Ziel-Toolchain und vollständige Integration/E2E testen, Abhängigkeiten und Image-Digests prüfen, Sicherheits- und Wiederherstellungstests durchführen sowie geeignete fachliche Prüfung von Vorlagen und Fristen veranlassen. Hosting, Support, Datenschutz, Aufbewahrung und Verträge organisatorisch festlegen. Ein unsignierter Bericht ist kein Zertifikat.

Erst nach dokumentierter Freigabe Versionsdateien aktualisieren, Release-Tag erstellen und Artefakte mit Prüfsummen veröffentlichen. Nicht ausgeführte Tests bleiben als solche erkennbar.
