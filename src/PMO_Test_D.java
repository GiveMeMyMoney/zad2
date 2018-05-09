import java.util.concurrent.Semaphore;

/**
 * Zadaniem testu jest sprawdzenie efektywnosci pracy systemu.
 * Czesc z konwersji bedzie wykonywana wolniej, chodzi o to, aby
 * pozostale konwersje byly kontynuowane normalnie a nie wstrzymywane
 * do czasu zakonczenie wolniejszej.
 */
public class PMO_Test_D extends PMO_Test_A {
    private final static long ADDITIONAL_SLOWDOWN_MUL = 3;
    private Semaphore semaphore = new Semaphore((int)ADDITIONAL_SLOWDOWN_MUL);
    protected static final int MAX_CORES = 5;

    @Override
    public long getRequiredTime() {
        return 26000;
    }

    protected void prepareConversionManagement() {
        prepareConversionManagement( MAX_CORES );
    }

    protected void prepareTestEnvironment() {
        prepareTestEnvironment( MAX_CORES );
    }

    protected  void testDependentPreparations() {
        dataConverter.setSemaphore( semaphore, ADDITIONAL_SLOWDOWN_MUL );
    }

    protected boolean parametricTest( ) {
        return parametricTest( MAX_CORES, RESULTS_EXPECTED );
    }

}
