package csit.semit.semitchecker;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class LocalizationTest {
    @Test
    void testCheckMarkedStd() throws IOException {
        ResourceBundle rb = ResourceBundle.getBundle("resourcesbundles.interfaces.mainpage-labels", new Locale("uk"));
        System.out.println(rb.getString("ref.filename"));
    }
}
