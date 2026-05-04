# Anexa A: Tabel detaliat al testelor de analiză de frontieră

## Convenții

**Constante** identice în toate testele:

- `highRiskCountries = {"KP", "IR", "MM"}`
- `blacklistedMerchants = {"GAMBLING", "CRYPTO_EXCHANGE", "FIREARMS"}`

**Baseline neutru** (valorile parametrilor nemodificați explicit; produc
`riskScore = 0` &rarr; `APPROVED`):

| Parametru               | Valoare baseline   |
|-------------------------|--------------------|
| `amount`                | `100.0`            |
| `hourOfDay`             | `12`               |
| `newBeneficiary`        | `false`            |
| `countryCode`           | `"RO"`             |
| `merchantCategory`      | `"GROCERY STORES"` |
| `transactionsLast24h`   | `5`                |
| `averagePreviousAmount` | `10000.0`          |

`averagePreviousAmount` este intenționat mare pentru ca regula
`amount > 3 · avgPrev` să nu se activeze accidental când variem `amount`.
Pentru testele dedicate acestei reguli, `avgPrev` se redefinește local la `500.0`.

**Notație** în coloana *Calcul scor*: fiecare termen este un increment aditiv
declanșat de o regulă; suma este `riskScore` final. `0` înseamnă că nicio regulă
nu se activează. Bara orizontală înseamnă că execuția se oprește înainte de calcul
(excepție de validare).

## Frontiere de validare (F&#8209;V)

Capetele domeniilor valide pentru parametrii numerici. Pentru fiecare capăt se
testează două valori: una **chiar pe limita validă** și una **la cel mai mic
pas reprezentabil dincolo de limită** (`±1` pentru `int`, `±0.01` pentru sume
monetare `double`).

| Test         | `amount` | `hour` | `newBen` | `country` | `tx24` | `avgPrev` | Calcul scor | Decizie                    | Obiectiv urmărit                                                                                                            |
|--------------|----------|--------|----------|-----------|--------|-----------|-------------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| TF&#8209;V1  | `0.0`    | `12`   | `false`  | `"RO"`    | `5`    | `10000.0` | &mdash;     | `IllegalArgumentException` | Valoare sub frontiera validă a `amount`.                                                                                    |
| TF&#8209;V2  | `0.01`   | `12`   | `false`  | `"RO"`    | `5`    | `10000.0` | `0`         | `APPROVED`                 | Minim valid pentru `amount`. Acoperă și capătul inferior `riskScore = 0`.                                                   |
| TF&#8209;V3  | `100.0`  | `-1`   | `false`  | `"RO"`    | `5`    | `10000.0` | &mdash;     | `IllegalArgumentException` | Valoare sub limita inferioară a `hourOfDay`. Verifică prezența condiției `hourOfDay < 0` la validare.                       |
| TF&#8209;V4  | `100.0`  | `0`    | `false`  | `"RO"`    | `5`    | `10000.0` | `15`        | `APPROVED`                 | Minim valid pentru `hourOfDay`. Multi‑frontieră: validează și activarea regulii `hourOfDay < 6`.                            |
| TF&#8209;V5  | `100.0`  | `23`   | `false`  | `"RO"`    | `5`    | `10000.0` | `15`        | `APPROVED`                 | Maxim pentru `hourOfDay`. Multi‑frontieră: validează și activarea regulii nocturne `hourOfDay > 22`.                        |
| TF&#8209;V6  | `100.0`  | `24`   | `false`  | `"RO"`    | `5`    | `10000.0` | &mdash;     | `IllegalArgumentException` | Valoare peste limita superioară a `hourOfDay`. Verifică prezența condiției `hourOfDay > 23` la validare.                    |
| TF&#8209;V7  | `100.0`  | `12`   | `false`  | `"RO"`    | `-1`   | `10000.0` | &mdash;     | `IllegalArgumentException` | Valoare sub limita inferioară a `transactionsLast24h`. Verifică prezența condiției `transactionsLast24h < 0` la validare.   |
| TF&#8209;V8  | `100.0`  | `12`   | `false`  | `"RO"`    | `0`    | `10000.0` | `0`         | `APPROVED`                 | Minim valid pentru `transactionsLast24h`. Confirmă acceptarea valorii limită inferioare.                                    |
| TF&#8209;V9  | `100.0`  | `12`   | `false`  | `"RO"`    | `5`    | `-0.01`   | &mdash;     | `IllegalArgumentException` | Valoare sub limita inferioară a `averagePreviousAmount`. Verifică prezența validării `averagePreviousAmount < 0`.           |
| TF&#8209;V10 | `100.0`  | `12`   | `false`  | `"RO"`    | `5`    | `0.0`     | `0`         | `APPROVED`                 | Minim valid pentru `averagePreviousAmount`. Confirmă că validatorul nu este `<= 0` (care ar exclude valoarea legitimă `0`). |

## Frontiere ale regulilor de scor (F&#8209;S)

Pragurile interne ale fiecărei reguli aditive de scor. Toate sunt formulate cu
inegalitate strictă (`>` sau `<`); pentru fiecare prag se testează două valori:
una **chiar pe prag** (la care regula NU trebuie să se activeze) și
una **la cel mai mic pas reprezentabil de cealaltă parte** (la care regula
trebuie să se activeze).

| Test         | `amount`  | `hour` | `newBen` | `country` | `tx24` | `avgPrev` | Calcul scor    | Decizie        | Obiectiv urmărit                                                                                                                     |
|--------------|-----------|--------|----------|-----------|--------|-----------|----------------|----------------|--------------------------------------------------------------------------------------------------------------------------------------|
| TF&#8209;S1  | `1000.0`  | `12`   | `false`  | `"RO"`    | `5`    | `10000.0` | `0`            | `APPROVED`     | Valoare chiar pe pragul `amount > 1000`: la egalitate regula NU trebuie să fie activă. Prinde defectul `>=` în loc de `>`.           |
| TF&#8209;S2  | `1000.01` | `12`   | `false`  | `"RO"`    | `5`    | `10000.0` | `10`           | `APPROVED`     | Valoare la pasul minim peste pragul `amount > 1000`: regula se activează. Acoperă și prima valoare nenulă realizabilă a `riskScore`. |
| TF&#8209;S3  | `5000.0`  | `12`   | `false`  | `"RO"`    | `5`    | `10000.0` | `10`           | `APPROVED`     | Valoare chiar pe pragul `amount > 5000`: la egalitate regula nu se activează (rămâne doar `+10` din `> 1000`).                       |
| TF&#8209;S4  | `5000.01` | `12`   | `false`  | `"RO"`    | `5`    | `10000.0` | `10 + 20 = 30` | `REQUIRES_2FA` | Valoare la pasul minim peste pragul `amount > 5000`: regula se cumulează cu `> 1000` (`+10 + 20`).                                   |
| TF&#8209;S5  | `3000.0`  | `12`   | `true`   | `"RO"`    | `5`    | `10000.0` | `10`           | `APPROVED`     | Valoare chiar pe pragul `newBeneficiary && amount > 3000`: la `amount = 3000` regula nu se activează; rămâne doar `+10`.             |
| TF&#8209;S6  | `3000.01` | `12`   | `true`   | `"RO"`    | `5`    | `10000.0` | `10 + 25 = 35` | `REQUIRES_2FA` | Valoare la pasul minim peste pragul `newBeneficiary && amount > 3000`: regula activă (`+25`).                                        |
| TF&#8209;S7  | `100.0`   | `5`    | `false`  | `"RO"`    | `5`    | `10000.0` | `15`           | `APPROVED`     | Ultima valoare la care regula `hourOfDay < 6` se aplică (`+15`). Prinde defectul `< 5` sau `<= 5`.                                   |
| TF&#8209;S8  | `100.0`   | `6`    | `false`  | `"RO"`    | `5`    | `10000.0` | `0`            | `APPROVED`     | Valoare chiar pe pragul `hourOfDay < 6`: la `hour = 6` regula nu se mai activează. Prinde defectul `<= 6`.                           |
| TF&#8209;S9  | `100.0`   | `22`   | `false`  | `"RO"`    | `5`    | `10000.0` | `0`            | `APPROVED`     | Valoare chiar pe pragul `hourOfDay > 22`: la egalitate regula nu se activează. Prinde defectul `>= 22`.                              |
| TF&#8209;S10 | `100.0`   | `12`   | `false`  | `"RO"`    | `10`   | `10000.0` | `0`            | `APPROVED`     | Valoare chiar pe pragul `transactionsLast24h > 10`: la egalitate regula nu se activează. Prinde defectul `>= 10`.                    |
| TF&#8209;S11 | `100.0`   | `12`   | `false`  | `"RO"`    | `11`   | `10000.0` | `20`           | `APPROVED`     | Valoare la pasul minim peste pragul `transactionsLast24h > 10`: la `transactionsLast24h = 11` regula este activă (`+20`).            |
| TF&#8209;S12 | `1500.0`  | `12`   | `false`  | `"RO"`    | `5`    | `500.0`   | `10`           | `APPROVED`     | Valoare chiar pe pragul `amount > 3 * averagePreviousAmount`: regula nu se activează; doar `+10` din `> 1000`.                       |
| TF&#8209;S13 | `1500.01` | `12`   | `false`  | `"RO"`    | `5`    | `500.0`   | `10 + 25 = 35` | `REQUIRES_2FA` | Valoare la pasul minim peste pragul `amount > 3 * averagePreviousAmount`: regula activă (`+25`), cumulată cu `> 1000` (`+10`).       |

> Capătul inferior al domeniului `riskScore` (`0`, când nicio regulă nu se
> activează) este prins implicit de **TF‑V2** din tabelul F‑V
> (`amount = 0.01`, scor `0`, `APPROVED`); nu necesită un test F‑S dedicat.

> Frontiera `amount > 3000` în combinație cu `highRiskCountries.contains(country)`
> este simetrică cu TF‑S5/S6 (același prag, același operator, increment `+30`
> în loc de `+25`); se acoperă implicit prin TF‑D4 și TF‑D6 de mai jos.

## Frontiere ale deciziilor (F&#8209;D)

Pragurile de clasificare `30`, `60`, `90` se compară cu `>=`; pentru fiecare
prag se testează două valori: una **chiar pe prag** (la care decizia trece în
clasa superioară) și una **cel mai mare scor realizabil sub prag** (la care
decizia rămâne în clasa inferioară). Pentru că toate incrementele sunt multipli
de `5`, valorile de sub prag realizabile sunt `25`, `55`, `85`.

| Test        | `amount` | `hour` | `newBen` | `country` | `tx24` | `avgPrev` | Calcul scor                   | Decizie         | Obiectiv urmărit                                                                                                                                   |
|-------------|----------|--------|----------|-----------|--------|-----------|-------------------------------|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| TF&#8209;D1 | `1500.0` | `23`   | `false`  | `"RO"`    | `5`    | `10000.0` | `10 + 15 = 25`                | `APPROVED`      | Cel mai mare scor realizabil sub pragul `>= 30`: decizia rămâne `APPROVED`. Prinde defectul `> 30` (ar lăsa scor `30` în `APPROVED`).              |
| TF&#8209;D2 | `5500.0` | `12`   | `false`  | `"RO"`    | `5`    | `10000.0` | `10 + 20 = 30`                | `REQUIRES_2FA`  | Scor chiar pe pragul `>= 30`: la egalitate decizia trece în `REQUIRES_2FA`.                                                                        |
| TF&#8209;D3 | `5500.0` | `12`   | `true`   | `"RO"`    | `5`    | `10000.0` | `10 + 20 + 25 = 55`           | `REQUIRES_2FA`  | Cel mai mare scor realizabil sub pragul `>= 60`: scor `55` rămâne sub prag. Prinde defectul `> 60` sau `>= 55`.                                    |
| TF&#8209;D4 | `5500.0` | `12`   | `false`  | `"KP"`    | `5`    | `10000.0` | `10 + 20 + 30 = 60`           | `MANUAL_REVIEW` | Scor chiar pe pragul `>= 60`: la egalitate decizia trece în `MANUAL_REVIEW`. Acoperă implicit și frontiera `amount > 3000` cu `highRiskCountries`. |
| TF&#8209;D5 | `4000.0` | `12`   | `true`   | `"KP"`    | `11`   | `10000.0` | `10 + 25 + 30 + 20 = 85`      | `MANUAL_REVIEW` | Cel mai mare scor realizabil sub pragul `>= 90`: scor `85` rămâne în `MANUAL_REVIEW`. Prinde defectul `> 90` sau `>= 85`.                          |
| TF&#8209;D6 | `5500.0` | `23`   | `true`   | `"RO"`    | `11`   | `10000.0` | `10 + 20 + 25 + 15 + 20 = 90` | `BLOCKED`       | Scor chiar pe pragul `>= 90`: la egalitate decizia trece în `BLOCKED` prin scor.                                                                   |

## Sinteză

| Categorie | Număr teste | Tip frontieră                                 | Scopul principal                                                |
|-----------|-------------|-----------------------------------------------|-----------------------------------------------------------------|
| F&#8209;V | 10          | Capete ale domeniilor valide ale parametrilor | Validează că validatorii folosesc operatorul corect (`<=`/`<`). |
| F&#8209;S | 13          | Praguri ale regulilor aditive de scor         | Validează că fiecare regulă folosește `>` strict (nu `>=`).     |
| F&#8209;D | 6           | Praguri de clasificare ale `riskScore`        | Validează că fiecare prag de decizie folosește `>=` (nu `>`).   |
| **Total** | **29**      |                                               |                                                                 |
