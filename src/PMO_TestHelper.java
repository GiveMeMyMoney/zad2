import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class PMO_TestHelper {
    public static boolean executeTests(List<? extends PMO_Testable> tests) {
        boolean result = true;
        boolean testResult;
        int errorCounter = PMO_Consts.THE_SAME_ERROR_REPETITIONS_LIMIT;

        for (PMO_Testable testOK : tests) {
            testResult = testOK.testOK();
            if (testResult == false)
                errorCounter--;
            result &= testResult;
            if (errorCounter == 0) {
                PMO_CommonErrorLog.error("Przekroczono limit prezentacji bledow - koniec testu");
                return false;
            }
        }
        return result;
    }

    public static String stackTrace2String( Throwable e ) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

}
