# Raport AI &ndash; comparație suite de teste proprii vs. autogenerate

Raportul de față documentează **utilizarea unui asistent AI** (ChatGPT) pentru generarea
automată de suite de teste unitare pentru clasa `facultate.tss.TransactionFraudDetector`
și **comparația** cu suitele proprii, derivate manual din specificație. Pentru fiecare
tehnică de testare aplicată în proiect (partiționare în clase de echivalență, analiză a
valorilor de frontieră, testare structurală, mutation testing etc.) se va adăuga o
secțiune dedicată, urmând aceeași structură: tool folosit, prompt, răspuns, rulare,
comparație cu suita proprie, interpretare și referințe bibliografice.

Obiectivele raportului:

- evidențierea **diferențelor cantitative și calitative** între suitele autogenerate și
  cele construite manual (acoperire a claselor de echivalență/frontierelor/ramurilor,
  corectitudinea oracolelor, trasabilitatea către specificație);
- discutarea **limitărilor LLM-urilor** în sarcini de proiectare a testelor, cu
  citate din literatura de specialitate;
- păstrarea unei **dovezi reproductibile** (prompturi, capturi de ecran și fișiere
  sursă commit-ate în repository).

## Partiționare în clase de echivalență

### Tool folosit

Pentru generarea automată a suitei de teste prin partiționare în clase de echivalență am
folosit **ChatGPT** (modelul GPT-5.3, interfața web, conversație din 03.05.2026). Captura
integrală a dialogului este arhivată în
[
`screenshots/ai/eq-bva-screencapture-chatgpt-c-69f71939-7070-83eb-bb1b-39926fd91f64-2026-05-04-21_43_59.pdf`](screenshots/ai/eq-bva-screencapture-chatgpt-c-69f71939-7070-83eb-bb1b-39926fd91f64-2026-05-04-21_43_59.pdf).

ChatGPT a fost ales pentru că este un *Large Language Model* generalist, antrenat pe
volume mari de date textuale și multimodale, inclusiv date de cod, provenite din surse publice,
licențiate și generate/provizionate în procesul de antrenare [[1]](#bibliografie).

### Prompt

Promptul a fost minimalist (*zero-shot*, fără atribuire de rol și fără exemple) și a
folosit o abordare strict **black-box**: am furnizat doar **specificația** programului
(descriere generală, parametri, validări, reguli de decizie și ieșiri - copiată ca
text din [`documentatie-testare.md`](documentatie-testare.md)), urmată de o singură
propoziție în limba română:

> *\[specificația completă a programului\]*
>
> *Scrie setul minim de teste unitare pentru partitionarea in clase de echivalenta
> utilizand libraria junit5 (java/maven).*

Această formulare minimalistă este suficientă pentru generațiile recente de modele
(GPT-4 și ulterioare): un studiu sistematic pe 4 modele și 162 de roluri arată că
**atribuirea unui rol în system prompt nu îmbunătățește semnificativ acuratețea** pe
benchmark-uri obiective (uneori chiar o degradează), iar efectul este
imprevizibil de la un task la altul [[3]](#bibliografie). În consecință, am preferat un
prompt scurt, lăsând modelul să-și aplice politica implicită de generare; eventualele
limitări observate ulterior sunt astfel atribuibile modelului, nu unei formulări
subdimensionate a cererii. Furnizarea **specificației** (nu a codului) este, în plus,
fidelă tehnicii black-box [[4]](#bibliografie): modelul nu poate "ghici" decizia citind
implementarea, ci trebuie să o deducă din regulile descrise.

### Răspuns

ChatGPT a răspuns cu o suită de **10 metode de test** (transcrise integral în
[`src/test/java/ai/AIEqPartTest.java`](../src/test/java/ai/AIEqPartTest.java)),
grupate prin comentarii în două secțiuni: `// --- VALIDARI ---` (5 teste) și
`// --- REGULI FUNCTIONALE ---` (5 teste). Modelul a justificat alegerea ca *„set
minimal”* prin acoperirea explicită a:

- claselor de echivalență valide pentru fiecare interval de scor (APPROVED, REQUIRES_2FA,
  MANUAL_REVIEW, BLOCKED);
- claselor invalide pentru fiecare validare (`amount`, `hour`, `null`, valori negative,
  seturi `null`);
- regulii speciale de scurtcircuit prin blacklist.

În răspuns, modelul a inclus o **observație autocritică explicită** privind testul
`shouldRequireManualReviewForHighRisk`:

> *„Acest test acceptă două rezultate (`MANUAL_REVIEW` sau `BLOCKED`). Asta pentru că
> scorul poate depăși 90 în funcție de implementare - iar în practică ar fi mai
> bine să controlezi exact scorul dacă vrei test strict.”*

Această dublă acceptare se traduce într-un `assertTrue(result == MANUAL_REVIEW ||
result == BLOCKED)` - testul trece atâta timp cât rezultatul nu este `APPROVED`
sau `REQUIRES_2FA`. Modelul a recunoscut explicit că nu poate calcula deterministic
scorul așteptat și a slăbit criteriul de acceptare ca să evite un eșec.

### Rulare

Suita generată a fost rulată cu runner-ul JUnit din IntelliJ IDEA. **9 din 10 teste
trec**, iar testul `shouldRequire2FAForMediumRisk` **a picat**:

![Rulare teste AI - 9 trec, 1 pică](screenshots/ai/ai-eq-part-tests.jpg)

```
Expected :REQUIRES_2FA
Actual   :MANUAL_REVIEW
at ai.AIEqPartTest.shouldRequire2FAForMediumRisk(AIEqPartTest.java:74)
```

Pentru intrările `(amount=1500, hour=23, newBen=false, "RO", "FOOD", tx24=11,
avgPrev=100)`, comentariul modelului justifica scorul ca `+10 (>1000) + 15 (noapte)+ 20 (tx>10) = 45`,
însă a omis regula `+25 dacă averagePreviousAmount > 0 și amount > 3 * averagePreviousAmount`
(`1500 > 3 * 100 = 300`). Scorul real este `10 + 15 + 20 + 25 = 70`, ceea ce încadrează rezultatul
în `MANUAL_REVIEW`. Deși regula era explicit prezentă în prompt, modelul nu a omis-o din ignoranță,
ci dintr-o greșeală aritmetică, o limitare cunoscută a LLM-urilor pe sarcini de calcul precis [[5]](#bibliografie).

### Comparație cu suita proprie

Suita proprie (`TransactionFraudDetectorEquivalenceTest`, 16 teste) a fost derivată
sistematic din partițiile documentate în [`documentatie-testare.md`](documentatie-testare.md)
(11 clase invalide I1-I11, 18 clase valide V1-V18, 5 clase de ieșire
O1-O5). Suita generată de ChatGPT (10 teste, 1 picat) acoperă doar parțial aceleași
partiții.

#### Comparație între teste

| Aspect                            | Suită proprie | Suită AI (ChatGPT)                         |
|-----------------------------------|---------------|--------------------------------------------|
| Număr total de teste              | **16**        | 10 (1 picat)                               |
| Teste pentru clase invalide       | 11            | 5                                          |
| Teste pentru clase valide         | 5             | 5                                          |
| Clase invalide acoperite (I1-I11) | **11 / 11**   | 5 / 11                                     |
| Clase de ieșire acoperite (O1-O5) | **5 / 5**     | 3 / 5 strict, 4 / 5 cu aserțiune permisivă |
| Teste care **trec**               | 16 / 16       | 9 / 10                                     |

#### Diferențe pe clasele invalide

| Clasă | Condiție                       | Suită proprie | Suită AI                                             |
|-------|--------------------------------|---------------|------------------------------------------------------|
| I1    | `amount <= 0`                  | TI1 (`-100`)  | `shouldThrowExceptionForInvalidAmount` (`0`)         |
| I2    | `hourOfDay < 0`                | TI2 (`-10`)   | **lipsă**                                            |
| I3    | `hourOfDay > 23`               | TI3 (`26`)    | `shouldThrowExceptionForInvalidHour` (`24`)          |
| I4    | `countryCode == null`          | TI4           | `shouldThrowExceptionForNullCountry`                 |
| I5    | `countryCode.isBlank()`        | TI5           | **lipsă**                                            |
| I6    | `merchantCategory == null`     | TI6           | **lipsă**                                            |
| I7    | `merchantCategory.isBlank()`   | TI7           | **lipsă**                                            |
| I8    | `transactionsLast24h < 0`      | TI8 (`-3`)    | `shouldThrowExceptionForNegativeTransactions` (`-1`) |
| I9    | `averagePreviousAmount < 0`    | TI9           | **lipsă**                                            |
| I10   | `highRiskCountries == null`    | TI10          | `shouldThrowExceptionForNullSets`                    |
| I11   | `blacklistedMerchants == null` | TI11          | **lipsă**                                            |

**Probleme metodologice ale suitei AI**:

1. **Subacoperire a claselor invalide** (5/11): lipsesc `hourOfDay < 0`,
   `countryCode.isBlank`, ambele forme de invalidate ale `merchantCategory`,
   `averagePreviousAmount < 0` și `blacklistedMerchants = null`. Modelul a tratat
   `countryCode == null/blank` și `merchantCategory == null/blank` ca pe o singură clasă,
   în pofida faptului că specificația din prompt le tratează ca pe două condiții
   distincte.
2. **Eroare aritmetică la testul** `shouldRequire2FAForMediumRisk`: modelul a
   omis o regulă din specificație (multiplul mediei), generând un test care pică.
3. **Aserțiune permisivă** (`assertTrue(a || b)`) la
   `shouldRequireManualReviewForHighRisk`: când modelul nu reușește să calculeze
   deterministic rezultatul, acceptă mai multe rezultate posibile în loc să rafineze
   datele de intrare.

#### Diferențe pe clasele valide și de ieșire

| Clasă                        | Acoperire suită proprie | Acoperire suită AI                                                      |
|------------------------------|-------------------------|-------------------------------------------------------------------------|
| V1-V4 (`amount`)             | toate 4 (TV1-TV4)       | V1, V2, V4 - **lipsesc** V3 (`3000 < amount <= 5000`)                   |
| V5-V7 (`hourOfDay`)          | toate 3                 | V6, V7 - **lipsește** V5 (interval nocturn de jos)                      |
| V10/V11 (`countryCode`)      | ambele                  | ambele (V10 doar în testul de blocare prin scor extrem)                 |
| V12/V13 (`merchantCategory`) | ambele                  | ambele                                                                  |
| V16-V18 (`avgPrev`)          | toate 3                 | doar V18 - **lipsesc** V16, V17                                         |
| O1 `APPROVED`                | TV1                     | `shouldReturnApprovedForLowRisk`                                        |
| O2 `REQUIRES_2FA`            | TV2                     | `shouldRequire2FAForMediumRisk` - **picat**, deci O2 efectiv neacoperit |
| O3 `MANUAL_REVIEW`           | TV3                     | `shouldRequireManualReviewForHighRisk` - doar prin aserțiune permisivă  |
| O4 `BLOCKED` prin scor       | TV4                     | `shouldBlockForVeryHighRiskScore`                                       |
| O5 `BLOCKED` prin blacklist  | TV5                     | `shouldBlockImmediatelyForBlacklistedMerchant`                          |

ChatGPT a produs o suită **incompletă și parțial incorectă**:

- **Puncte forte**: sintaxă JUnit 5 corectă, separare clară între validări și reguli
  funcționale, izolarea clasei `I10` într-un singur test (un singur `null` per apel),
  recunoașterea explicită a propriei limite în testul `shouldRequireManualReviewForHighRisk`.
- **Puncte slabe**:

1. deși specificația era explicită și completă în prompt,
   modelul a *grupat* clasele invalide apropiate semantic (string-uri `null`/blank,
   ambele seturi `null`) și a sărit peste 6 din 11 clase. Comportamentul este consistent
   cu observațiile din literatură: LLM-urile generează teste cu acoperire **mai mică**
   decât suitele derivate manual din specificație, în special pe ramurile de
   validare/excepție [[2]](#bibliografie).
2. greșeala aritmetică din `shouldRequire2FAForMediumRisk`
   arată că modelul nu execută programul, ci aproximează scorul din specificație. Pentru
   un test cu mai mult de 3-4 reguli aditive, riscul de eroare crește substanțial.
3. aserțiunea permisivă din
   `shouldRequireManualReviewForHighRisk` (`assertTrue(MANUAL_REVIEW || BLOCKED)`) este un
   *test smell* clasic (*Conditional Test Logic*) [[6]](#bibliografie). Modelul a preferat
   să slăbească verificarea în loc să aleagă date de intrare care produc un scor
   neambiguu.

## Analiza valorilor de frontieră

### Tool și prompt

Aceeași sesiune ChatGPT din secțiunea anterioară, continuată cu o cerere nouă (arhiva
completă a conversației este în
[
`screenshots/ai/eq-bva-screencapture-chatgpt-c-69f71939-7070-83eb-bb1b-39926fd91f64-2026-05-04-21_43_59.pdf`](screenshots/ai/eq-bva-screencapture-chatgpt-c-69f71939-7070-83eb-bb1b-39926fd91f64-2026-05-04-21_43_59.pdf)):

> *Scrie acum si setul minim de teste pentru analiza valorilor de frontiera, cu aceeasi
> librarie.*

Specificația rămăsese în context din promptul anterior, deci nu a mai fost recopiată.

### Răspuns și rulare

ChatGPT a livrat **16 metode de test** (transcrise în
[`src/test/java/ai/AiBvaTest.java`](../src/test/java/ai/AiBvaTest.java)): 11 teste de
validare la frontieră (`amount`, `hour`, `tx`, `avg`), 4 teste de prag pe `amount`
(`1000`, `1000.01`, `5000`, `5000.01`) și 1 test pe frontiera nocturnă `hour = 5/6`
&mdash; **acesta a picat**:

![Rulare teste AI BVA &mdash; 15 trec, 1 pică](screenshots/ai/ai-bva-tests.jpg)

```
org.opentest4j.AssertionFailedError: expected: not equal but was: <APPROVED>
at ai.AiBvaTest.hourNightBoundary(AiBvaTest.java:156)
```

Testul `hourNightBoundary` verifică faptul că deciziile la `hour = 6` (zi) și
`hour = 5` (noapte) nu sunt egale, presupunând că trecerea peste pragul `hour < 6` modifică decizia. La
inputurile alese (baseline `amount = 100`), însă, scorul rămâne sub pragul `30` în ambele
cazuri (`0` la zi, `15` la noapte), iar decizia este `APPROVED`. Modelul a confundat
**frontiera regulii de scor** (`hour < 6` adaugă `+15`) cu **frontiera deciziei**
(`riskScore >= 30` schimbă clasa). Testul a fost comentat pentru a permite rularea cu
acoperire.

### Comparație cu suita proprie

Suita proprie (`TransactionFraudDetectorBoundaryTest`, **29 teste**) acoperă sistematic
cele trei categorii F-V / F-S / F-D; suita AI (**16 teste, 1 picat**) acoperă parțial
F-V, doar 4 din 13 frontiere F-S, iar **F-D lipsește integral**.

| Categorie                      | Suită proprie | Suită AI                                                        |
|--------------------------------|---------------|-----------------------------------------------------------------|
| F-V invalide                   | 5             | 6 (1 redundantă: `amount = -0.01` + `amount = 0` aceeași clasă) |
| F-V valide                     | 5             | 5 (doar `assertDoesNotThrow`, fără verificarea deciziei)        |
| F-S `amount > 1000` / `> 5000` | 4             | 4 (3 cu aserțiuni permisive: `assertNotEquals`, `assertTrue ‖`) |
| F-S `newBen && amount > 3000`  | 2             | **0**                                                           |
| F-S `hour < 6` / `> 22`        | 4             | 1 picat (comentat)                                              |
| F-S `tx > 10`                  | 2             | **0**                                                           |
| F-S `amount > 3 · avgPrev`     | 2             | **0** (activată însă prin baseline `AVG = 100`)                 |
| F-D praguri `30` / `60` / `90` | 6             | **0**                                                           |
| Teste care **trec**            | 29 / 29       | 15 / 16                                                         |

Suita AI BVA introduce două probleme specifice:

1. **Confuzia frontieră-de-regulă vs. frontieră-de-decizie.** Modelul a tratat o variație de scor ca implicând
   automat o variație de decizie, ipoteză falsă când scorurile rămân de aceeași parte a
   unui prag de decizie. Consecința: F-D lipsește integral (nicio combinație de reguli
   activate care să exerseze pragurile `30` / `60` / `90`).
2. **Valoarea implicită `AVG = 100`** activează nedorit regula `amount > 3 * avgPrev`
   pentru orice `amount > 300`, deci toate testele de prag pe
   `amount` includ implicit un `+25`, iar testele pot trece din motive
   greșite (scor compus diferit de cel intenționat).

## Bibliografie

1. <a id="bibliografie"></a>**OpenAI**, *How ChatGPT and our foundation models are developed*. Disponibil online
   la:
   <https://help.openai.com/en/articles/7842364-how-chatgpt-and-our-foundation-models-are-developed> (accesat la 03.
   05.2026).
2. M. Schäfer, S. Nadi, A. Eghbali, F. Tip, *An Empirical Evaluation of Using Large
   Language Models for Automated Unit Test Generation* (2024). IEEE Transactions on Software
   Engineering, vol. 50, nr. 1, pp. 85-105. DOI:
   [10.1109/TSE.2023.3334955](https://doi.org/10.1109/TSE.2023.3334955).
3. M. Zheng, J. Pei, L. Logeswaran, M. Lee, D. Jurgens, *When "A Helpful
   Assistant" Is Not Really Helpful: Personas in System Prompts Do Not Improve
   Performances of Large Language Models*, EMNLP 2024: Findings of the Association for
   Computational Linguistics, pp. 15126-15154. arXiv:2311.10054.
   Disponibil la: <https://arxiv.org/abs/2311.10054>.
4. G. J. Myers, C. Sandler, T. Badgett (2011). *The Art of Software Testing*, ed. a 3-a,
   Wiley, ISBN 978-1118031964. Disponibil la:
   <https://malenezi.github.io/malenezi/SE401/Books/114-the-art-of-software-testing-3-edition.pdf>
5. I. Mirzadeh, K. Alizadeh, H. Shahrokhi, O. Tuzel, S. Bengio, M. Farajtabar (2024)
   *GSM-Symbolic: Understanding the Limitations of Mathematical Reasoning in Large
   Language Models*, arXiv:2410.05229. Disponibil la: <https://arxiv.org/abs/2410.05229>.
6. M. L. Siddiq, J. C. Da Silva Santos, R. H. Tanvir, N. Ulfat, F. Al Rifat, V. C. Lopes (2024). *Using Large Language
   Models to Generate JUnit Tests: An Empirical Study*. EASE '24: Proceedings of the 28th International Conference on
   Evaluation and Assessment in Software Engineering. p. 313-211.
   DOI: [10.1145/3661167.3661216](https://doi.org/10.1145/3661167.3661216). 


