package facultate.tss;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionFraudDetectorMutationKillTest {

    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    private final Set<String> highRiskCountries = Set.of("KP", "IR", "MM");

    private final Set<String> blacklistedMerchants = Set.of("GAMBLING", "CRYPTO_EXCHANGE", "FIREARMS");

    private static final String DEFAULT_MERCHANT = "GROCERY STORES";

    private TransactionDecision evaluate(
            double amount, int hour, boolean newBeneficiary,
            String country, int tx24, double avgPrev) {
        return detector.evaluateTransaction(
                amount, hour, newBeneficiary, country,
                DEFAULT_MERCHANT, tx24, avgPrev,
                highRiskCountries, blacklistedMerchants);
    }

    /**
     * Cazuri pe valoarea de frontiera pentru a omori mutantii ConditionalsBoundary.
     * <p>
     * Pentru fiecare rand: cu operatorul original, scorul ramane sub pragul 30
     * (decizie {@code APPROVED}); cu operatorul mutant ({@code >=} / {@code <=}),
     * scorul creste si traverseaza pragul 30 (decizie {@code REQUIRES_2FA}),
     * astfel mutantul este omorat de asertia care asteapta {@code APPROVED}.
     */
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, newBen={3}, country={4}, tx24={5}, avgPrev={6} -> {7}")
    @CsvSource({
            // TF-M1: amount = 1000.0, scor original = 25 (regula avgPrev*3),
            // scor mutant (amount >= 1000) = 35 -> REQUIRES_2FA
            "TF-M1, 1000.0, 12, false, RO, 0,  100.0, APPROVED",

            // TF-M2: hour = 6, scor original = 25 (regula avgPrev*3),
            // scor mutant (hour <= 6) = 40 -> REQUIRES_2FA
            "TF-M2, 200.0,  6,  false, RO, 0,  50.0,  APPROVED",

            // TF-M3: hour = 22, scor original = 25 (regula avgPrev*3),
            // scor mutant (hour >= 22) = 40 -> REQUIRES_2FA
            "TF-M3, 200.0,  22, false, RO, 0,  50.0,  APPROVED",

            // TF-M4: amount = 3000.0 si tara cu risc ridicat,
            // scor original = 10 (doar amount > 1000),
            // scor mutant (amount >= 3000) = 40 -> REQUIRES_2FA
            "TF-M4, 3000.0, 12, false, KP, 0,  10000.0, APPROVED",

            // TF-M5: tx24 = 10, scor original = 25 (regula avgPrev*3),
            // scor mutant (tx24 >= 10) = 45 -> REQUIRES_2FA
            "TF-M5, 200.0,  12, false, RO, 10, 50.0,  APPROVED"
    })
    void boundaryMutation_killsSurvivingMutants(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, int tx24, double avgPrev,
            TransactionDecision expected) {
        assertEquals(expected,
                evaluate(amount, hour, newBeneficiary, country, tx24, avgPrev));
    }
}
