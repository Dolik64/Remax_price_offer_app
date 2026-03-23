# STA Builder

Srovnávací tržní analýza — webová aplikace pro realitní makléře.

## Požadavky

- Java 17+
- Maven 3.8+

## Spuštění

```bash
cd sta-builder
mvn spring-boot:run
```

Aplikace poběží na **http://localhost:8080**

## Otevření v IntelliJ IDEA

1. File → Open → vyberte složku `sta-builder`
2. IntelliJ automaticky rozpozná Maven projekt
3. Počkejte na stažení závislostí
4. Spusťte `StaBuilderApplication.java` (Shift+F10)

## Struktura projektu

```
sta-builder/
├── pom.xml                              # Maven konfigurace
├── README.md
└── src/main/
    ├── java/cz/stabuilder/
    │   ├── StaBuilderApplication.java   # Spring Boot vstupní bod
    │   ├── model/
    │   │   ├── Subject.java             # Předmět prodeje
    │   │   ├── ComparableProperty.java  # Srovnatelná nemovitost
    │   │   ├── Pricing.java             # Cenové doporučení
    │   │   └── StaProject.java          # Celý projekt (root)
    │   ├── controller/
    │   │   ├── StaController.java       # REST API (/api/*)
    │   │   └── PageController.java      # HTML stránka
    │   └── service/
    │       ├── StaService.java          # Logika, CRUD, upload fotek
    │       └── DocxExportService.java   # Generování DOCX (Apache POI)
    └── resources/
        ├── application.properties       # Konfigurace serveru
        ├── static/
        │   ├── style.css                # Styly
        │   └── app.js                   # Frontend logika
        └── templates/
            └── index.html               # Hlavní stránka
```

## Workflow

1. **Předmět** — vyplníte klienta, adresu, popis, nahrajete fotky
2. **Srovnání** — přidáváte karty srovnatelných nemovitostí (nabídka / prodáno)
3. **Tabulka** — automaticky se generuje srovnávací tabulka
4. **Cena** — klady, zápory, cenové rozmezí
5. **Export** — stáhnete DOCX nebo JSON

## TODO (rozšíření)

- [ ] Persistentní ukládání do SQLite / H2
- [ ] Drag & drop řazení fotek
- [ ] Šablona DOCX s logem RE/MAX
- [ ] Import dat z realitních portálů (Sreality, Bezrealitky)
- [ ] PDF export jako alternativa
