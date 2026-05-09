# Proiect pentru cursul de **Testarea sistemelor software**

## Testarea unitară în Java a unui sistem de identificare a tranzacțiilor bancare suspecte

Student: Irina Chihăiței (căs. Moroșanu) &mdash; Informatică ID, anul 2, grupa 1

## Descriere

Aplicația implementează un **detector de fraudă pentru tranzacții bancare/card**. Pe baza datelor unei tranzacții și a
unor liste de risc, sistemul calculează un scor de risc și returnează o decizie privind procesarea tranzacției.

Componenta principală este clasa `facultate.tss.TransactionFraudDetector`, care expune metoda
`evaluateTransaction(...)`. Decizia returnată este de tipul `facultate.tss.TransactionDecision`.

## Configurație software

- **Sistem de operare**: Windows 10
- **JDK**: Java 21
- **Build tool**: Apache Maven 3.9.9 (`maven-surefire-plugin` 3.5.2)
- **Framework de testare**: JUnit 5.11.4 (Jupiter)
- **Mutation testing**: PIT 1.20.0 (rulat prin pluginul *PIT mutation testing IDEA-plugin*)
- **IDE**: IntelliJ IDEA 2025.3.3

## Configurație hardware

- **CPU**: 11th Gen Intel Core i7 2.30 GHz
- **RAM**: 24 GB

## Structura proiectului

- `src/main/java/` &ndash; clasa sub test și enum-ul deciziei.
- `src/test/java/` &ndash; suitele de teste (partiționare, valori frontieră, structurale, omorâre mutanți).
- `docs/` &ndash; documentația de testare, anexa analizei valorilor de frontieră și raportul AI.
- `docs/screenshots/` &ndash; toate capturile de ecran referențiate din documentație.
- `pom.xml` &ndash; configurația Maven.

## Documentație

Documentația completă se găsește în:
[docs/documentatie-testare.md](docs/documentatie-testare.md)

## Raport AI

Raportul despre folosirea unui tool AI se găsește în:
[docs/raport-ai.md](docs/raport-ai.md)

## Rulare teste

Toate testele sunt rulate cu Maven Surefire. Din directorul rădăcină al proiectului:

```bash
# rulează toate suitele
mvn test

# rulează o singură clasă de test
mvn test -Dtest=TransactionFraudDetectorBoundaryTest

# rulează o singură metodă de test
mvn test -Dtest=TransactionFraudDetectorBoundaryTest#scoreRuleBoundary_returnsExpectedDecision
```

## Rulare coverage / mutation testing

Atât acoperirea structurală, cât și raportul PIT sunt rulate din IntelliJ IDEA.

### Acoperire (statement / branch)

1. Click dreapta pe directorul `src/test/java/facultate/tss` (sau pe o clasă de test individuală)
   &rarr; *More Run/Debug* &rarr; *Run with Coverage*.
2. După finalizare, panoul *Coverage* afișează procentele pe clase, metode, linii și ramuri pentru
   `facultate.tss.TransactionFraudDetector`.

### Mutation testing (PIT)

1. Asigură-te că pluginul *PIT mutation testing IDEA-plugin* este instalat
   (*Settings* &rarr; *Plugins* &rarr; *Marketplace*).
2. Click dreapta pe directorul `src/test/java/facultate/tss` &rarr; *Run 'Pitest of `facultate.tss`'*.
3. Raportul HTML este scris în `target/report/` și se poate deschide cu *Open in Browser* peste
   `target/report/index.html`.


