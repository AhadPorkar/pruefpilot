# PrüfPilot Benutzerhandbuch

**Version 1.0.0-rc.1 | Deutsch | Standardsprache der Anwendung: Englisch**

## 1. Zweck und fachliche Grenze

PrüfPilot dokumentiert betriebliche Prüfungen und koordiniert Maßnahmen an eindeutig identifizierten Anlagen. Ein fälliger Termin wird mit einer Anlage, einer prüfenden Person, Antworten, Nachweisen, einer Reparatur und einer unabhängigen Verifizierung verbunden.

Die Anwendung entscheidet nicht über die sichere Verwendung eines Arbeitsmittels, bestimmt keine gesetzlichen Prüffristen, bestätigt keine fachliche Befähigung und stellt kein rechtsverbindliches Prüfzertifikat aus. Alle Beispieldaten sind fiktiv. Die Demo-Vorlage und ihr willkürliches 30-Tage-Intervall sind keine fachliche Empfehlung.

## 2. Betriebsarten unterscheiden

Im **Serverbetrieb** melden Sie sich mit einem vom Administrator eingerichteten Konto an. Daten werden in PostgreSQL gespeichert. Bei einer Verbindungsstörung bleibt die Störung sichtbar; echte Daten werden niemals automatisch durch Beispieldaten ersetzt.

Die **ausdrückliche Vorschau** enthält `?demo=1` in der Adresse und einen sichtbaren Hinweis mit Rollenauswahl. Es gibt dort keine echte Anmeldung. Änderungen bestehen nur im Arbeitsspeicher des aktuellen Tabs und verschwinden beim Neuladen oder Zurücksetzen. Keine Kundendaten eingeben. Die Vorschau eignet sich für Vorstellung, Schulung und Oberflächentests, nicht für Nachweispflichten.

Ein **Backend mit Demo-Startdaten** ist dagegen eine echte, persistente Installation mit Anmeldung und fiktiven Ausgangsdaten. Diese Option gilt nur bei erstmaliger Initialisierung. Sie darf nicht als produktive Kundenumgebung weiterverwendet werden.

## 3. Anmeldung, Sprache und Bedienung

Öffnen Sie die Adresse Ihrer Installation und geben Sie E-Mail und Passwort ein. Es gibt keine öffentliche Registrierung. Passwörter benötigen mindestens 14 Zeichen und dürfen 72 UTF-8-Bytes nicht überschreiten. Sonderzeichen können mehrere Bytes belegen. Jede Person verwendet ein eigenes Konto.

Mit **EN / DE** in der Kopfzeile wechseln Sie die Sprache. Ein neuer Browser startet auf Englisch. Sprach- und Designpräferenz werden lokal gespeichert, keine Passwörter oder Sitzungstoken. Freitext wird nicht automatisch übersetzt. Vorlagen besitzen getrennte englische und deutsche Beschriftungen.

Die Navigation enthält Übersicht, Anlagen, Prüfungen, Prüfpläne, Maßnahmen, Berichte, Vorlagen, Team, Audit und Hilfe. Auf dem Smartphone öffnet die Menütaste die Navigation. **Strg+K / Cmd+K** durchsucht Navigation und Anlagen. Das Glockensymbol ist ein Einstieg in prüfungsbezogene Aufgaben, keine E-Mail- oder Push-Benachrichtigungsfunktion.

Über den Kontobereich ändern Sie Ihr Passwort. Danach ist eine erneute Anmeldung erforderlich; andere Sitzungen werden bei ihrem nächsten API-Aufruf abgewiesen. Melden Sie sich auf gemeinsam genutzten Geräten ab. Sitzungen laufen nach 30 Minuten Inaktivität ab.

## 4. Rollen und Verantwortlichkeiten

| Aufgabe | Administrator | Supervisor | Prüfer | Techniker | Viewer |
|---|---|---|---|---|---|
| Arbeitsbereich lesen | Ja | Ja | Ja | Ja | Ja |
| Standorte, Anlagen, Vorlagen und Pläne anlegen | Ja | Ja | Nein | Nein | Nein |
| Prüfung durchführen | Ja | Ja | Ja | Nein | Nein |
| Fremde Prüfung begutachten | Ja | Ja | Nein | Nein | Nein |
| Mangel zuweisen | Ja | Ja | Nein | Nein | Nein |
| Selbst zugewiesene Reparatur dokumentieren | Ja | Ja | Nein | Ja | Nein |
| Reparatur unabhängig verifizieren | Ja | Ja | Nein | Nein | Nein |
| Vollständiges Berichtspaket exportieren | Ja | Ja | Nein | Nein | Ja |
| Konten anlegen und deaktivieren | Ja | Nein | Nein | Nein | Nein |

Der Server erzwingt diese Rechte. Auch Administratoren dürfen ihre eigene Prüfung oder Reparatur nicht verifizieren. Ein Entwurf gehört der ursprünglich prüfenden Person. Eine Reparatur darf nur die zugewiesene Person bearbeiten.

**VIEWER sieht den gesamten Arbeitsbereich.** Diese Rolle ist kein kundenspezifisch abgeschottetes Portal. Externen Personen nur dann Zugriff erteilen, wenn sie alle Datensätze sehen dürfen. Für Kundenübergaben sind in Version 1 ausgewählte Berichtspakete vorgesehen.

## 5. Anlagenregister aufbauen

Öffnen Sie **Anlagen**, wählen Sie **Neuer Standort** und erfassen Sie Namen, eindeutigen Kurzcode und Adresse. Legen Sie anschließend eine Anlage an und ordnen Sie sie einem Standort zu. Erfassen Sie Anlagencode, Namen, Kategorie, Hersteller, Modell, Seriennummer, Ortsangabe und betriebliche Kritikalität. Die Kritikalität ist keine gesetzliche Risikoklassifizierung.

Verwenden Sie stabile Kennungen wie `BER-LIFT-001`. Zulässig sind Buchstaben, Ziffern, Unterstriche und Bindestriche. Kennung und Seriennummer müssen zum realen Arbeitsmittel passen. Pro Installation sind 250 aktive Anlagen vorgesehen.

Archivierung erhält die Historie und gibt gegebenenfalls einen aktiven Platz frei. Zuvor sind offene Mängel zu bearbeiten, laufende Prüfungen abzuschließen und aktive Pläne zu pausieren. Diese Version bietet keine allgemeine Stammdatenbearbeitung, keinen Massenimport und keine endgültige Löschung. Legen Sie Namenskonventionen vor der produktiven Erfassung fest.

Der **Anlagenpass** zeigt Identität, Fälligkeit, Prüfungen, offene Maßnahmen, Nachweise und Ereignisse. Kontrollieren Sie Kennung und Seriennummer vor jeder Aktion. Ältere Prüfungen lassen sich seitenweise nachladen. Ein Begrenzungshinweis bedeutet, dass die Übersicht nicht die gesamte Historie enthält. Ein vollständiger Anlagenexport ist nicht auf die sichtbare Seite begrenzt.

## 6. QR-Kennzeichnung

Im Anlagenpass öffnen Sie **Etikett / QR drucken**. Der QR-Code enthält einen direkten Anlagenlink, sofern dessen UTF-8-Länge in die unterstützte Kapazität passt. Bei sehr langen Installationsadressen wird stattdessen der stabile Anlagencode verwendet und darauf hingewiesen. Es handelt sich um einen echten QR-Encoder.

Etiketten nur entsprechend der betrieblichen Kennzeichnungsregeln anbringen. Sicherheits- und Pflichtkennzeichnungen nicht verdecken. Die integrierte Kameraerkennung benötigt einen unterstützten Browser, einen sicheren Kontext und eine Kamerafreigabe. Alternativ den Link mit der normalen Smartphone-Kamera öffnen oder den Anlagencode manuell eingeben. Ein QR-Code ersetzt keine Anmeldung oder Berechtigung.

## 7. Vorlage anlegen und freigeben

Unter **Vorlagen** erfassen Sie englischen und deutschen Namen, Beschreibung und bis zu 100 Prüfpunkte. Jeder Prüfpunkt erhält beide Sprachfassungen.

**Bestanden / Nicht bestanden / Nicht anwendbar:** Bei Nicht bestanden oder Nicht anwendbar ist eine Begründung mit mindestens fünf Zeichen erforderlich. **Zahl:** Optionale Unter- und Obergrenze sowie Einheit; das Ergebnis berechnet der Server. Grenzwertverletzungen erfordern eine Notiz. Die aktuelle Eingabe erwartet einen Dezimalpunkt ohne Tausendertrennzeichen. **Text:** Pflichttexte dürfen nicht leer sein; ein leerer optionaler Text gilt als nicht anwendbar.

Eine neue Vorlage ist ein Entwurf und kann noch nicht für Prüfungen verwendet werden. Administrator oder Supervisor prüft die Vorlage, dokumentiert die Grundlage und gibt sie frei. Die Software protokolliert diese Entscheidung, bestätigt aber keine fachliche Qualifikation.

Freigegebene Vorlagen sind unveränderlich. Für Änderungen klonen Sie die Vorlage in eine neue Version. Alte Prüfungen bleiben mit den damaligen Beschriftungen und Grenzen verbunden. Eine gesetzlich validierte Vorlagenbibliothek wird nicht mitgeliefert. Fachliche Prüfung je Anlagenklasse und Einsatzbedingung ist vor realer Nutzung erforderlich.

## 8. Wiederkehrenden Prüfplan einrichten

Wählen Sie unter **Prüfpläne** Anlage, freigegebene Vorlage, verantwortliche Person, ersten Fälligkeitstag und Intervall von 1 bis 3.650 Kalendertagen. Dokumentieren Sie **Grundlage und qualifizierte Prüfung des Intervalls**. Die Anwendung schlägt keine gesetzliche Frist vor.

Pro Anlage/Vorlage ist nur ein aktiver Plan erlaubt. Die verantwortliche Person ist eine organisatorische Zuordnung, kein Befähigungsnachweis. Fälligkeitstage verwenden die Zeitzone des Arbeitsbereichs, standardmäßig Europe/Berlin; Ereigniszeitpunkte werden in UTC gespeichert.

Das Einreichen einer Prüfung verschiebt den Termin noch nicht. Erst die Freigabe durch eine andere berechtigte Person setzt den ursprünglichen Kalenderrhythmus fort. Versäumte Perioden werden übersprungen, nicht als erfundene Prüfungen angelegt. Beispiel: Ein ausdrücklich konfigurierter 30-Tage-Rhythmus ab 1. September, ausgeführt am 5. November, wird auf 30. November fortgeschrieben. Eine Ablehnung verändert die Fälligkeit nicht.

Pausieren ist erst möglich, wenn laufende Entwürfe/eingereichte Prüfungen erledigt sind. Für ein anderes Intervall pausieren Sie den alten Plan und erstellen einen neuen mit dokumentierter Grundlage. Historische Datensätze werden nicht umgeschrieben. Version 1 versendet keine automatischen E-Mail-Erinnerungen; die Fälligkeitsliste ist die tägliche Arbeitsgrundlage.

## 9. Prüfung durchführen

Starten Sie eine fällige Prüfung aus dem Plan oder eine anlassbezogene Prüfung aus dem Anlagenpass. Identifizieren Sie zuerst das reale Arbeitsmittel. Nur freigegebene Vorlagen sind nutzbar. Ein Plan erlaubt nur eine laufende Prüfung; vorhandene Entwürfe unter **Prüfungen** fortsetzen statt erneut beginnen.

Bearbeiten Sie jeden Prüfpunkt. Beschreiben Sie bei Abweichungen die konkrete Beobachtung. Begründen Sie Nicht anwendbar. Zahlen mit sinnvoller Einheit erfassen. PNG-, JPEG- oder PDF-Nachweise können während des Entwurfs hochgeladen werden. Ihre fachliche Aussagekraft oder Echtheit wird nicht automatisch beurteilt.

**Entwurf speichern** speichert auf dem Server. Eine Netzwerkverbindung ist erforderlich; es gibt keine Offline-Warteschlange und keine Hintergrundsynchronisation. Bei Fehlern bleibt das sichtbare Formular erhalten, ungespeicherte Angaben gehen jedoch beim Neuladen oder Schließen verloren. Regelmäßig speichern und den erreichten Status kontrollieren.

**Einreichen** validiert alle Antworten und erzeugt atomar je fehlgeschlagenem Punkt einen Mangel. Der Status wechselt zu Eingereicht. Zeitstempel werden serverseitig gesetzt, Antworten sind danach über die API unveränderlich. Der Zeitpunkt dokumentiert die Erfassung/Einreichung im System, nicht unabhängig den Zeitpunkt einer realen Untersuchung. Nur die ursprünglich prüfende Person kann den Entwurf einreichen.

## 10. Prüfung begutachten

Ein anderer Administrator oder Supervisor kontrolliert Antworten und Nachweise und dokumentiert eine aussagekräftige Notiz. **Freigeben** bestätigt die Prüfdokumentation und schreibt gegebenenfalls den Plan fort. **Ablehnen** erhält den Datensatz samt Mängeln und lässt den Termin fällig. Nach Klärung wird eine neue Prüfung begonnen; der abgelehnte Datensatz bleibt erhalten.

Die Freigabe einer Prüfung schließt Mängel nicht automatisch, erklärt keine Anlage für sicher und erlaubt keinen Weiterbetrieb. Betriebliche Sicherheits-, Sperr- und Freigabeverfahren sind unabhängig einzuhalten.

## 11. Maßnahmen und verifizierter Abschluss

Die vier Zustände lauten **Offen → In Bearbeitung → Zur Verifizierung → Geschlossen**. Es gibt keinen Drag-and-drop-Weg, der die Regeln umgeht.

Ein Supervisor weist den Mangel einer aktiven Person mit Rolle Administrator, Supervisor oder Techniker zu und setzt Termin und Priorität. Nur diese Person startet und dokumentiert die Reparatur. Ein nicht zugewiesener Administrator darf dies nicht stellvertretend tun.

Die Reparaturbeschreibung benötigt mindestens zehn Zeichen. Mindestens ein Nachweis ist vor der Verifizierungsanfrage hochzuladen. Ohne Nachweis wird die Fertigmeldung abgewiesen. Mit **Verifizierung anfordern** wird der Vorgang zur Prüfung bereitgestellt.

Ein anderer Administrator oder Supervisor begutachtet Reparatur und Originaldateien und hält das Ergebnis fest. Zustimmung schließt den Vorgang mit dokumentierter verifizierender Person. Ablehnung setzt ihn auf In Bearbeitung zurück. Selbstverifizierung ist auch für Administratoren verboten. Das Vier-Augen-Prinzip ersetzt keine gesondert erforderliche Signatur oder Fachprüfung.

## 12. Nachweise und Berichtspakete

Zugelassen sind PNG, JPEG und PDF mit höchstens **10 MiB je Datei** und **50 MiB je Anlage**. Geprüft werden Dateisignatur-Präfixe, nicht nur Endungen. Dateien werden als authentifizierte Downloads ausgeliefert. Ein Virenscanner ist nicht integriert. Vor Öffnung oder Weitergabe gelten die betrieblichen Sicherheitsregeln. Unnötige personenbezogene Daten vermeiden.

Unter **Berichte** wählen Sie Anlage und Sprache und laden das ZIP herunter:

| Inhalt | Verwendung |
|---|---|
| `report.html` | Druckbare Zusammenfassung nach vollständigem Entpacken |
| `data.json` | Strukturierte Daten mit Schemaversion und Erzeugungszeit |
| `inspections.csv` | Prüfungen und begutachtende Personen |
| `answers.csv` | Antworten, Notizen, Ergebnisse und beide Beschriftungen |
| `corrective-actions.csv` | Reparaturen und Verifizierung |
| `evidence/` | Originalnachweise |
| `manifest.json` | Dateigrößen und SHA-256-Prüfsummen aller anderen Dateien |

Die Ordnerstruktur erhalten, damit Nachweislinks funktionieren. Über den Browserdruck von `report.html` kann eine PDF erzeugt werden. Diese ist kein signiertes Zertifikat. JSON/CSV enthalten weitere strukturierte Details, die die kompakte HTML-Zusammenfassung nicht vollständig darstellt.

Mit `python scripts/verify-report.py bericht.zip` prüfen Sie die Konsistenz mit dem enthaltenen Manifest. Da das Manifest nicht signiert ist, beweist ein positives Ergebnis weder Urheberschaft noch rechtliche Gültigkeit. Wer Dateien und Manifest gemeinsam ersetzt, kann erneut passende Prüfsummen erzeugen.

Pakete enthalten gegebenenfalls Namen und Betriebsinformationen. Nur an berechtigte Empfänger und über freigegebene sichere Kanäle senden. Die Anwendung verschickt keine E-Mails und verwaltet keine externen Downloadrechte.

## 13. Team und Audit

Administratoren erstellen persönliche Konten und übermitteln Anfangspasswörter sicher. Eine Passwortzurücksetzung per E-Mail ist nicht enthalten. Ausgeschiedene Personen deaktivieren; deren Identität nicht für Nachfolger wiederverwenden. Das eigene Administratorkonto kann nicht selbst deaktiviert werden. Deaktivierung und Passwortänderung wirken bei bestehenden Sitzungen mit dem nächsten API-Aufruf.

**Audit** zeigt Erfassung, Einreichung, Begutachtung, Zuweisung, Nachweise und Exporte. Die Anwendung erlaubt keine Bearbeitung; ein Datenbanktrigger blockiert normale Updates und Löschungen. Das ist kein kryptografisch versiegeltes oder gegen privilegierte Datenbankadministratoren unveränderliches Register. Datenbankzugriff und Sicherungen liegen beim Betreiber.

## 14. Fehlerbehebung

| Situation | Vorgehen |
|---|---|
| Sitzung abgelaufen | Notwendige ungespeicherte Notizen sicher erhalten, erneut anmelden und Datensatz neu laden |
| Versionskonflikt | Aktualisieren und den neuen Stand vergleichen, bevor Sie erneut handeln |
| Unabhängige Prüfung erforderlich | Andere berechtigte Person beauftragen; keine geteilte Identität zum Umgehen nutzen |
| Nachweis fehlt | Während In Bearbeitung eine zulässige Datei hochladen |
| Pausieren/Archivieren gesperrt | Laufende Prüfungen und offene Mängel erledigen, aktive Pläne pausieren |
| Datei abgewiesen | Tatsächliches PNG/JPEG/PDF innerhalb der Grenzen verwenden; Umbenennen reicht nicht |
| Kamera nicht verfügbar | Code manuell erfassen oder normale Gerätekamera verwenden |
| Server nicht erreichbar | Netzwerk/Server prüfen; die Vorschau ersetzt keinen echten Datensatz |
| Anlagenlimit erreicht | Tatsächlich inaktive Anlagen archivieren oder Kapazitätsausbau separat planen |
| Begrenzte Historie angezeigt | Anlagenpass und vollständigen Export verwenden |

Fehlermeldungen mit Version, Browser, Sprache, Rolle und reproduzierbaren Schritten melden. Niemals Passwörter, `.env`, Sitzungscookies oder echte Kundennachweise in öffentliche Issues hochladen.
