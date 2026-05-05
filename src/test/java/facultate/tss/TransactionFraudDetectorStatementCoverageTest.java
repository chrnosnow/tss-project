package facultate.tss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionFraudDetectorStatementCoverageTest {

    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    private final Set<String> highRiskCountries = Set.of("KP", "IR", "MM");

    private final Set<String> blacklistedMerchants = Set.of("GAMBLING", "CRYPTO_EXCHANGE", "FIREARMS");

    // TS1-TS5: cele 5 blocuri "throw" reprezentabile in CSV (amount, hour, country, merchant, history)
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

    // TS6: validarea seturilor null
    @Test
    void TS6_riskSetsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        5, 50.0, null, blacklistedMerchants));
    }

    // TS7-TS11: scurtcircuit blacklist + cele patru decizii finale.
    // TS8 activeaza simultan toate cele 7 reguli de scor si corpul buclei "for" (break).
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
