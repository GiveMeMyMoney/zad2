/**
 * Klasa testuje problem brak danych.
 * Oczekuje się, że wszystkie konwersje zostaną wykonane, ale
 * nie wszystkie wyniki dostarczone.
 */
public class PMO_Test_B extends PMO_Test_A {

    protected static final int RESULTS_EXPECTED = 20;
    protected static final int DATA_PORTIONS_SENDERS = 5;
    protected static final int DATA_PORTIONS_PER_SENDER = RESULTS_EXPECTED * 4 / DATA_PORTIONS_SENDERS ;

    @Override
    public long getRequiredTime() {
        return 20000;
    }

    protected void prepareInitialDataPortions() {
        generator.add(1, RESULTS_EXPECTED );

        // Brak danych dla kanału prawego
        generator.add(RESULTS_EXPECTED+1, ConverterInterface.Channel.LEFT_CHANNEL);

        generator.add( RESULTS_EXPECTED+2, RESULTS_EXPECTED * 2 );
    }

    protected void prepareSenders() {
        prepareSenders( DATA_PORTIONS_PER_SENDER, DATA_PORTIONS_SENDERS );
    }

    protected boolean parametricTest( ) {
        return parametricTest( MAX_CORES, RESULTS_EXPECTED );
    }

    protected void prepareConversionManagement() {
        prepareConversionManagement( MAX_CORES );
    }

    protected void prepareTestEnvironment() {
        prepareTestEnvironment( MAX_CORES );
    }
}
