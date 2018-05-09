import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class PMO_Test_A implements PMO_RunTestTimeout, PMO_LogSource {

    private AtomicBoolean finished = new AtomicBoolean(false);
    private List<PMO_Test_DataPortionSender> dataSenders = new ArrayList<>();
    protected ConversionManagementInterface management;
    protected PMO_Test_DataPortionGenerator generator;

    private PMO_AtomicCounter coresInUse;
    private PMO_AtomicCounter coresInUseMax;
    private AtomicInteger coresLimit;
    private PMO_Test_ConversionReceiver conversionReceiver;
    protected PMO_Test_Converter dataConverter;

    protected static final int DATA_PORTIONS_PER_SENDER = 50;
    protected static final int DATA_PORTIONS_SENDERS = 10;
    protected static final int RESULTS_EXPECTED = DATA_PORTIONS_PER_SENDER * DATA_PORTIONS_SENDERS / 2;
    protected static final int MAX_CORES = 3;

    {
        generator = new PMO_Test_DataPortionGenerator();
    }

    @Override
    public long getRequiredTime() {
        return 24000;
    }

    protected void prepareInitialDataPortions() {
        generator.add(1, RESULTS_EXPECTED );
    }

    protected void prepareSenders( int dataPortionsPerSender, int dataPortionsSenders ) {
        dataSenders = new ArrayList<>();
        generator.shuffle();
        CyclicBarrier barrier = new CyclicBarrier(dataPortionsSenders, () ->
                log("Do bariery dotarly wszystkie watki odpowiedzialne za przekazywania danych"));

        IntStream.rangeClosed(1, dataPortionsSenders).forEach(i -> {
            PMO_Test_DataPortionSender sender = new PMO_Test_DataPortionSender(management,
                    generator.getSubList(dataPortionsPerSender) );
            sender.setCyclicBarrier(barrier);
            dataSenders.add(sender);
        });
    }

    protected void prepareConversionManagement( int maxCores ) {
        try {
            management = (ConversionManagementInterface)
                    PMO_GeneralPurposeFabric.fabric("ConversionManagement", "ConversionManagementInterface");
            management.setCores(maxCores);
            management.setConversionReceiver(conversionReceiver);
            management.setConverter(dataConverter);
        } catch (Exception e) {
            exception2error("W trakcie przygotowania obiektu ConversionManagement doszło do wyjątku", e);
            PMO_CommonErrorLog.criticalMistake();
        }
    }

    protected void prepareTestEnvironment( int maxCores ) {
        coresLimit = new AtomicInteger(maxCores);
        coresInUseMax = PMO_CountersFactory.createCommonMaxStorageCounter();
        coresInUse = PMO_CountersFactory.createCounterWithMaxStorageSet();

        dataConverter = new PMO_Test_Converter(coresLimit, coresInUse);
        conversionReceiver = new PMO_Test_ConversionReceiver(dataConverter);
    }

    protected void prepareSenders() {
        prepareSenders( DATA_PORTIONS_PER_SENDER, DATA_PORTIONS_SENDERS );
    }

    protected void prepareConversionManagement() {
        prepareConversionManagement( MAX_CORES );
    }

    protected void prepareTestEnvironment() {
        prepareTestEnvironment( MAX_CORES );
    }

    protected boolean parametricTest( ) {
        return parametricTest( MAX_CORES, RESULTS_EXPECTED );
    }

    protected  void testDependentPreparations() {
    }

    protected  void testDependentPreparationsBeforeTestOK() {
    }

    @Override
    public void run() {
        log("Przygotowanie obiektow srodowiska testu");
        prepareTestEnvironment();
        PMO_SleepTracker.getRef().start();

        log( "Przygotowanie Systemu konwersji");
        prepareConversionManagement();

        log("Przygotowanie porcji danych");
        prepareInitialDataPortions();

        log("Przygotowanie obiektów, które będą przekazywać dane");
        prepareSenders();

        log( "Dodatkowe przygotowania zależne od testu");
        testDependentPreparations();

        log("Uruchomienie watkow");
        List<Thread> threads = PMO_ThreadsHelper.createAndStartRegisteredThreads(dataSenders, true);

        log("Oczekiwanie na zakonczenie watkow");
        PMO_ThreadsHelper.joinThreads(threads);

        log("Watki przekazujace dane zakonczyly prace");
        finished.set(true);
    }


    protected boolean parametricTest( int maxCores, int resultsExpected ) {
        boolean result = true;

        // uzycie cores
        if ( coresInUseMax.get() == maxCores ) {
            PMO_SystemOutRedirect.println( "Do konwersji wykorzystywano wszystkie dostępne rdzenie");
        } else {
            error( "Problem z poprawnym użyciem zasobów. Do konwersji użyto maksymalnie " + coresInUseMax.get() +
                    " rdzeni, oczekiwano " + maxCores );
            result = false;
        }

        // czy dostarczono poprawną liczbę wyników
        if ( conversionReceiver.nextExpectedID() == resultsExpected + 1 ) {
            PMO_SystemOutRedirect.println( "Przekazano prawidłową liczbę wynikow konwersji czyli " + resultsExpected );
        } else {
            error( "Błędna liczba przekazanych wyników konwersji. Oczekiwano " + resultsExpected +
                    " otrzymano " + ( conversionReceiver.nextExpectedID() - 1 ) ) ;
            result = false;
        }
        return result;
    }

    @Override
    public boolean testOK() {

        if (!finished.get()) {
            PMO_CommonErrorLog.error("Rozpoczęto test, choć metoda run() jeszcze się nie zakończyła");
            return false;
        }

        testDependentPreparationsBeforeTestOK();

        boolean result;

        // czy sender nie stwierdzil bledu
        result = PMO_TestHelper.executeTests(dataSenders);

        if (result) {
            PMO_SystemOutRedirect.println("Obiekty wysyłające dane nie stwierdziły błędu");
        } else {
            error("Błąd na poziomie wysyłania danych - reszta testów nie ma większego sensu");
            return false;
        }

        // czy konwerter nie stwierdzil bledu
        if (dataConverter.testOK()) {
            PMO_SystemOutRedirect.println("Obiekt wykonujący konwersję danych nie stwierdził błędu");
        } else {
            error("Obiekt konwertujący dane wykrył błąd");
            result = false;
        }

        // czy receiver nie stwiedzil bledu
        if (conversionReceiver.testOK()) {
            PMO_SystemOutRedirect.println("Obiekt odbierający dane nie stwierdził błędu");
        } else {
            error("Obiekt odbierający dane wykrył błąd");
            result = false;
        }

        // czy wszystkie konwersje policzono
        List<PMO_Test_DataPortion> converted = dataConverter.getHistory();
        List<PMO_Test_DataPortion> dataPortions = generator.getAsList();

        if ( converted.containsAll(dataPortions) && dataPortions.containsAll(converted ) ) {
            PMO_SystemOutRedirect.println( "Wszystkie porcje danych zostaly poddane konwersji");
        } else {
            error( "Nie przetworzono wszystkich porcji danych. Dane " + dataPortions.size() +
                " konwersje " + converted.size() );
            result = false;
        }

        // sredni czas wprowadzania zadania
        long sum = 0;
        for ( PMO_Test_DataPortionSender sender : dataSenders ) {
            sum += sender.getSumAddDataPortionCallTime();
        }
        long avg = sum / dataPortions.size();

        if ( avg < PMO_Test_Consts.ADD_DATA_PORTION_CALL_TIME_LIMIT ) {
            PMO_SystemOutRedirect.println( "Sredni czas wprowadzania do systemu porcji danych jest poprawny: " + avg
                    + "msec, suma to " + sum );
        } else {
            error( "Czas wprowadzania porcji danych do systemu jest zbyt długi. Oczekiwano " +
            PMO_Test_Consts.ADD_DATA_PORTION_CALL_TIME_LIMIT + " jest " + avg );
        }

        // nie wykryto śpiących wątków
        if ( PMO_SleepTracker.getRef().testOK() ) {
            PMO_SystemOutRedirect.println( "Nie wykryto wątków wykonujących metodę sleep");
        } else {
            error( "Wykryto wątki w stanie snu (sleep)");
            result = false;
        }

        result &= parametricTest();

        return result;
    }
}
