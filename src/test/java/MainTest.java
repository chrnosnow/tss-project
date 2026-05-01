import facultate.tss.Main;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainTest {
    @Test
    void testHello() {
        assertEquals("hello", Main.hello());
    }
}
