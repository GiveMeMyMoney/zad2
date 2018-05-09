import java.util.HashMap;
import java.util.Map;

public class PMO_Test_C extends PMO_Test_A {

    // mapa niezabezpieczona, bo po inicjacji będzie już tylko
    // odczytywana
    private Map<Integer,Integer> historySize2coresLimit =
            new HashMap<>();

    // poczatkowa liczba rdzeni
    protected static final int INITIAL_MAX_CORES = 3;
    // najwieksza dopuszczalna liczba rdzeni
    protected static final int MAX_MAX_CORES = 8;

    protected static final int RESULTS_EXPECTED = DATA_PORTIONS_PER_SENDER * DATA_PORTIONS_SENDERS / 2;

    @Override
    public long getRequiredTime() {
        return 25000;
    }

    // tu jest przygotowywana mapa z informacja kiedy
    // dany limit ma byc wprowadzony
    private void prepareCoresLimitMap() {
        historySize2coresLimit.put( 60, 2 );
        historySize2coresLimit.put( 90, 6 );
        historySize2coresLimit.put( 180, 4 );
        historySize2coresLimit.put( 240, 8 );
        historySize2coresLimit.put( 320, 3 );
        historySize2coresLimit.put( 360, 8 );
        historySize2coresLimit.put( 450, 3 );
    }

    protected void prepareTestEnvironment() {
        prepareTestEnvironment( INITIAL_MAX_CORES );
    }

    protected void prepareConversionManagement() {
        prepareConversionManagement( INITIAL_MAX_CORES );
    }

    protected  void testDependentPreparations() {
        dataConverter.setManagement( management );
        prepareCoresLimitMap();
        dataConverter.setHistorySize2coresLimit( historySize2coresLimit );
    }

    protected boolean parametricTest( ) {
        return parametricTest( MAX_MAX_CORES, RESULTS_EXPECTED );
    }
}
