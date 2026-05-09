package facultate.tss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionFraudDetectorIndependentPathsTest {

    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    private final Set<String> highRiskCountries = Set.of("KP", "IR", "MM");

    private final Set<String> blacklistedMerchants = Set.of("GAMBLING", "CRYPTO_EXCHANGE", "FIREARMS");

    /**
     * TS1-TS11: reluate din suita pentru acoperirea la nivel de instructiune; acopera toate cele 20 de drumuri
     * liniar independente (P1-P20) din multimea de baza pentru V(G) = 20.
     **/

    // TS1-TS5: cele 5 blocuri "throw" reprezentabile in CSV (amount, hour, country, merchant, history).
    // Fiecare comuta o singura decizie fata de drumul de baza:
    //   TS1 -> P2 (D1 = true), TS2 -> P3 (D2 = true), TS3 -> P4 (D3 = true),
    //   TS4 -> P5 (D4 = true), TS5 -> P6 (D5 = true).
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, country={4}, merchant={5}, tx24={6}, avg={7} -> " +
            "IllegalArgumentException")
    @CsvSource(value = {
            "TS1, -1.0,  12, false, RO,   GROCERY, 5,  50.0",
            "TS2, 100.0, 24, false, RO,   GROCERY, 5,  50.0",
            "TS3, 100.0, 12, false, null, GROCERY, 5,  50.0",
            "TS4, 100.0, 12, false, RO,   null,    5,  50.0",
            "TS5, 100.0, 12, false, RO,   GROCERY, -1, 50.0"
    }, nullValues = "null")
    void invalidInput_throwsIllegalArgumentException(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, String merchant, int tx24, double avgPrev) {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        amount, hour, newBeneficiary, country, merchant,
                        tx24, avgPrev, highRiskCountries, blacklistedMerchants));
    }

    // TS6 -> P7: comutare pe D6 (highRiskCountries == null).
    @Test
    void TS6_riskSetsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        5, 50.0, null, blacklistedMerchants));
    }

    // TS7-TS11: scurtcircuit blacklist + cele patru decizii finale.
    //   TS7  -> P8                                        (D7 = true, scurtcircuit blacklist).
    //   TS8  -> P10, P11, P12, P14, P15, P16, P17, P18    (toate cele 7 reguli active + match in `for`).
    //   TS9  -> P1, P13                                   (drum de baza + D12 = false la epuizarea iteratorului).
    //   TS10 -> P9, P13, P16, P20                         (D8, D15, D19 + D12 = false la epuizarea iteratorului).
    //   TS11 -> P10, P11, P12, P13, P19                   (D9-D11, D18 + D12 = false la epuizarea iteratorului).
    // P13 (D12 = false) este exersat in TS9-TS11 de hasNext() care returneaza false dupa parcurgerea
    // completa a setului highRiskCountries fara potrivire (country = "RO" nu apare in {KP, IR, MM}).
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, newBen={3}, country={4}, merchant={5}, tx24={6}, avg={7} ->" +
            " {8}")
    @CsvSource({
            "TS7,  100.0,   12, false, RO, GAMBLING, 5,  50.0,    BLOCKED",
            "TS8,  10000.0, 23, true,  KP, GROCERY,  15, 100.0,   BLOCKED",
            "TS9,  100.0,   12, false, RO, GROCERY,  5,  50.0,    APPROVED",
            "TS10, 2500.0,  12, false, RO, GROCERY,  15, 2000.0,  REQUIRES_2FA",
            "TS11, 5500.0,  23, true,  RO, GROCERY,  5,  10000.0, MANUAL_REVIEW"
    })
    void validInput_returnsExpectedDecision(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, String merchant, int tx24, double avgPrev,
            TransactionDecision expected) {
        assertEquals(expected,
                detector.evaluateTransaction(
                        amount, hour, newBeneficiary, country, merchant,
                        tx24, avgPrev, highRiskCountries, blacklistedMerchants));
    }
}
