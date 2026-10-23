# Produkt, Pilot und kommerzielle Freigabe

## Zielkunde und Nutzen

Der erste Zielkunde ist ein Werkstatt-/Facility-Betreiber oder Instandhaltungsdienstleister mit überschaubarem Anlagenregister, wiederkehrenden betrieblichen Kontrollen und einer verantwortlichen Person für unabhängige Abschlussprüfung. Käufer ist typischerweise Betrieb, Facility Management oder Instandhaltungskoordination. Ein betreuter B2B-Pilot ist passender als sofortiger unbeaufsichtigter Self-Service-SaaS-Vertrieb.

Das Versprechen lautet: **Fällige Arbeit, anlagenbezogene Beobachtungen, offene Mängel und verifizierte Reparaturen in einem Anlagenpass.** Nicht versprochen werden automatische Rechtskonformität, zertifizierte Arbeitsmittel oder gesetzlich berechnete Fristen.

## Differenzierung bleibt eine Hypothese

Lumiform bietet bereits digitale Prüfungen, Offline-Erfassung, Maßnahmen, Berichte und Unternehmensfunktionen an. Diese Merkmale allein belegen keine Marktlücke. Die Hypothese von PrüfPilot ist ein enger, anlagenzentrierter Ablauf mit klarer Reparaturverantwortung, Vier-Augen-Abschluss und kontrollierter Kundenübergabe. Der Code beweist technische Umsetzung, nicht Kaufbereitschaft.

Für Interviews eine konkrete betriebliche Nische wählen, beispielsweise interne Sichtbeobachtungs- und Maßnahmendokumentation einer Werkstattgruppe. Die fiktiven Hebebühnen-, Kompressor- und Leiterbeispiele enthalten keine freigegebenen Fachverfahren. Vor echten Spezialvorlagen einen qualifizierten Fachpartner einbeziehen.

Lassen Sie sich mit Einwilligung zeigen, wie eine Anlage identifiziert, der letzte Nachweis gefunden, ein Fehler zugewiesen und „repariert“ von „verifiziert“ unterschieden wird. Reine Meinungen über ein attraktives Dashboard reichen nicht.

## Pilot und Preis

Vorher/Nachher messen: Suchzeit nach Anlage/Nachweis, Anteil überfälliger Kontrollen, Alter offener Maßnahmen, abgelehnte Reparaturen und Aufwand für die Kundenübergabe. Keine erfundenen Einsparquoten nennen. Netzwerkabdeckung, individuelle Konten und praktische Vier-Augen-Verfügbarkeit prüfen. Es gibt keine Offline-Synchronisation.

EUR 79–249 pro Standort und Monat oder ein Organisationsplan mit klaren Anlagen-/Speichergrenzen ist eine **unvalidierte Preishypothese**. Hosting, Einführung, Sicherung, Support und fachliche Vorlagenprüfung vertraglich abgrenzen. Keine unbegrenzten Nachweise anbieten, solange die Implementierung begrenzten Datenbankspeicher nutzt.

Checkout, Abrechnung, Self-Service-Mandantenanlage und ein kundenspezifisch abgeschottetes Portal sind nicht implementiert. Nach Freigabe kann ein betreuter Installations-/Hostingdienst angeboten werden. MIT erlaubt kommerzielle Nutzung und Weiterverteilung; es schafft keine proprietäre Exklusivität.

## Rechtliche und datenschutzbezogene Grenze

Aufzeichnungspflichten können über eine gespeicherte Checkliste hinausgehen. BetrSichV § 14 behandelt unter anderem bestimmte Prüfaufzeichnungen und Angaben/Unterschriften der verantwortlichen Person. Anwendbarkeit, Inhalt, Befähigung und ausreichende elektronische Signatur sind für den konkreten Fall fachkundig zu prüfen. Ein Kontoeintrag oder unsigniertes ZIP-Manifest ist nicht automatisch eine ausreichende Signatur.

Keine Aussagen wie „DGUV-zertifiziert“ oder „rechtssicher“ ohne belastbare Grundlage. Keine gesetzlichen Intervalle aus der Kategorie erfinden. Die Demo-Frist ist ausdrücklich fiktiv.

Vor Kundendaten Verantwortlichkeiten, Zweck, Rechtsgrundlage, Aufbewahrung, Hosting, Unterauftragnehmer, Zugriffe, Vorfallbehandlung und Betroffenenrechte festlegen. EU-Hosting allein beweist keine DSGVO-Konformität. Betreiberbezogene Impressums-/Datenschutzangaben, Verträge, AVV und Supportzusagen müssen gesondert geprüft werden; die Vorlage erfindet keine Adresse, Steuernummer oder Rechtsberatung.

Quellen: [BetrSichV § 14](https://www.gesetze-im-internet.de/betrsichv_2015/__14.html), [BfDI SDM](https://www.bfdi.bund.de/DE/Fachthemen/Inhalte/Technik/SDM.html), [Lumiform](https://lumiformapp.com/).

## Freigabekriterien

- [ ] Vollständiger Netzbuild mit Zielversionen sowie Java-, PostgreSQL-/HTTP-, Frontend-, Browser- und Container-CI erfolgreich.
- [ ] npm-Lockdatei geprüft/committet, `npm ci` eingerichtet, Image-/Action-Digests gepinnt, Abhängigkeiten und SBOM geprüft.
- [ ] Neuinstallation, Neustart, Upgrade und isolierter Restore-Test auf Zielhosting bestanden; Überwachung, Support und Vorfallprozess festgelegt.
- [ ] Berechtigungen, Organisationsgrenzen, CSRF, Sitzungsentzug, Uploads und Exportdatenschutz unabhängig geprüft; angemessener Penetrationstest geplant/abgeschlossen.
- [ ] Virenprüfung/Quarantäne sowie Aufbewahrung, Löschung und Pseudonymisierung für Kundendaten geregelt und getestet.
- [ ] Repräsentative Last, Historie, Dateimengen und Parallelität getestet; Leistungs- und Wiederherstellungsziele vereinbart.
- [ ] Reale Mobilgeräte, Tastatur, Screenreader, Fokus und Kontrast geprüft; keine unbelegte Barrierefreiheitszertifizierung.
- [ ] Fachkundige Freigabe realer Vorlagen, Intervalle, Befähigung und Signaturanforderungen; keine Demo-Daten im Kundenbetrieb.
- [ ] Pilot validiert Nische, Preis und Arbeitsablauf; Kontowiederherstellung und unabhängige Begutachtung praktisch abgesichert.
- [ ] Betreiberbezogene Rechtstexte, AVV, Hosting-/Supportvertrag, Lizenzstrategie und Markenverfügbarkeit geprüft.

## Vorstellung bei Recruitern

Vorschau starten, Anlagenpass und EN/DE-/Dark-Mode zeigen. Als Techniker einen Nachweis hochladen, als Supervisor unabhängig abschließen. Bericht exportieren und Manifest prüfen. Danach die entsprechenden Serverregeln und Tests zeigen. Architekturentscheidungen und bewusst begrenzten Umfang erläutern.

Das Projekt als nachvollziehbaren Portfolio-Release-Candidate vorstellen, nicht als erfundene Kundeninstallation oder Berufserfahrung. Ein echter erfolgreicher GitHub-CI-Lauf und eine klar fiktive gehostete Demo sind aussagekräftiger als unbelegte Produktionsreife-Badges.
