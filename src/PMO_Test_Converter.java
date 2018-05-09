import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PMO_Test_Converter implements ConverterInterface, PMO_LogSource, PMO_Testable {

    private AtomicInteger coresLimit; // aktualny limit rdzeni
    private PMO_AtomicCounter coresInUse; // aktualna liczba uzywanych rdzeni
    private final List<PMO_Test_DataPortion> history = new ArrayList<>();
    private final AtomicBoolean stateOK = new AtomicBoolean(true);
    private final Map<DataPortionInterface, Long> conversionResults = new ConcurrentHashMap<>();
    private Map<Integer, Integer> historySize2coresLimit;
    private final AtomicReference<CyclicBarrier> coresLimitModificationBarrier = new AtomicReference<>();
    private ConversionManagementInterface management;
    private Semaphore semaphore;
    private long additionalSlowdownCoefficient;
    private final AtomicReference<DataPortionInterface> nextDataPortion
            = new AtomicReference<>();
    private final AtomicReference<PMO_Barrier> addNewDataPortionBarrier
            = new AtomicReference<>();

    private long result(DataPortionInterface data) {
        long sum = data.id();
        sum = 10000 * sum + data.data()[1];
        sum = 1000 * sum + (1+data.data()[2]);
        sum = 10 * sum;
        for (int i = 3; i < Math.min(5, PMO_Test_Consts.DATA_LENGTH); i++) {
            sum = 10 * sum + data.data()[i];
        }
        conversionResults.put(data, sum);
        return sum;
    }

    private void errorRegistration(String txt) {
        error(txt);
        stateOK.set(false);
    }

    public void setManagement( ConversionManagementInterface management ) {
        this.management = management;
    }

    public void setNextDataPortion(DataPortionInterface data) {
        nextDataPortion.set( data );
    }

    public void setAddNewDataPortionBarrier( PMO_Barrier addNewDataPortionBarrier ) {
        this.addNewDataPortionBarrier.set( addNewDataPortionBarrier );
    }

    public void setSemaphore( Semaphore semaphore, long additionalSlowdownCoeficient ) {
        this.semaphore = semaphore;
        this.additionalSlowdownCoefficient = additionalSlowdownCoeficient;
    }

    public void setHistorySize2coresLimit(Map<Integer, Integer> historySize2coresLimit) {
        this.historySize2coresLimit = historySize2coresLimit;
    }

    public boolean wasConverted(DataPortionInterface data) {
        return conversionResults.containsKey(data);
    }

    public long conversionResult(DataPortionInterface data) {
        if (wasConverted(data))
            return conversionResults.get(data);
        return 0;
    }

    @Override
    public long convert(DataPortionInterface data) {
        assert coresLimit != null;
        assert coresInUse != null;

        int cores = coresInUse.incAndStoreMax();

        log("Start konwersji " + data + ", w uzyciu " + cores + " rdzeni(e)");

        // test przekrocenia limitu cores
        if (cores > coresLimit.get()) {
            errorRegistration("Przekroczono liczbe dostępnych rdzeni, jest " + cores + " a limit " + coresLimit.get());
        }

        // test priorytetyzacji
        if (nextDataPortion.get() != null) {
            if (!data.equals(nextDataPortion.get())) {
                errorRegistration("Zla kolejnosc przekazywania danych");
                errorRegistration("Do convert powinno trafić " + nextDataPortion + ", a jest " + data);
            }
        }

        // test czy data nie null i czy dane w srodku nie null

        boolean dataOK = true;
        if ( data == null ) {
            errorRegistration( "Do konwersji dostarczono zamiast danych NULL");
            dataOK = false;
        } else {
            if ( data.data() == null ) {
                errorRegistration( "Metoda data() w dostarczonych danych zwraca NULL");
                dataOK = false;
            } else {
                if ( data.data().length != PMO_Test_Consts.DATA_LENGTH ) {
                    errorRegistration( "Metoda data() zwraca tablice o błędnym rozmiarze. Jest " +
                            data.data().length + " powinno być " + PMO_Test_Consts.DATA_LENGTH );
                    dataOK = false;
                }
            }
            if ( data.channel() == null ) {
                errorRegistration( "Metoda channel() w dostarczonych danych zwraca NULL");
                dataOK = false;
            }
        }

        long conversionResult = -1;
        if ( dataOK ) {

            // test powtorzenia konwersji
            if (wasConverted(data)) {
                errorRegistration("Do convert przekazano dane, ktore juz wczesniej przetworzono " + data);
            }

            int conversionNumber;
            // synchronizacja konieczna aby zabezpieczyć history oraz aby
            // mozna bylo prawidlowo pobrac numer konwertowanej paczki danych
            synchronized ( this ) {
                history.add((PMO_Test_DataPortion)data);
                conversionNumber = history.size();
            }

            if ( ( historySize2coresLimit != null ) && historySize2coresLimit.containsKey( conversionNumber ) ) {
                log( "Konwersja numer " + conversionNumber +
                        " znajduje sie w mapie. Przygotowanie do zmiany limitu rdzeni.");

                int newCoresLimit = historySize2coresLimit.get( conversionNumber );
                int oldLimit = coresLimit.get();
                assert management != null;

                class CoresLimitModification implements Runnable {
                    @Override
                    public void run() {
                        log("Watki konwertujace spotkaly sie na CyclicBarrier");
                        coresLimitModificationBarrier.set( null );
                        log( "Wyłączono barierę zmiany limitu wątków." );

                        // jeśli limit ma wzrosnąć to najpierw zmieniam limit po mojej stronie
                        if ( newCoresLimit > oldLimit ) {
                            coresLimit.set( newCoresLimit );
                        }
                        try {
                            long execTime = PMO_TimeHelper.executionTime( () ->
                                    management.setCores( newCoresLimit )
                            );
                            if ( execTime > PMO_Test_Consts.SET_CORES_CALL_TIME_LIMIT) {
                                errorRegistration( "Metoda setCores zbyt długo blokuje watek. Jest " +
                                execTime + " limit " + PMO_Test_Consts.SET_CORES_CALL_TIME_LIMIT);
                            }
                        } catch ( Exception e ) {
                            exception2error( "W trakcie zamiany limitu watkow doszlo do wyjatku", e );
                            stateOK.set( false );
                        }
                        // jeśli limit ma spaść, to najpierw informuje management
                        if ( newCoresLimit < oldLimit ) {
                            coresLimit.set( newCoresLimit );
                        }

                        log( "Wykonano zmiane limitu watkow z " + oldLimit + " na " + newCoresLimit );
                    }
                }

                coresLimitModificationBarrier.set( new CyclicBarrier(coresLimit.get(),
                        new Runnable() {
                            @Override
                            public void run() {
                                Thread th = PMO_ThreadsHelper.createThreadAndRegister( new CoresLimitModification());
                                th.start();
                            }
                        }));
                log( "Dodano barierę zmiany limitu wątków.");
            }

            conversionResult = result(data);
        }

        boolean additionalSlowdown = false;
        if ( semaphore != null ) {
            additionalSlowdown = semaphore.tryAcquire();
        }
        long conversionSlowdown = randomConversionSlowdown();

        if ( additionalSlowdown ) {
            log( "Konwersja " + data + " zostanie dodatkowo spowolniona" );
            conversionSlowdown *= additionalSlowdownCoefficient;
        }

        PMO_TimeHelper.asleep( conversionSlowdown );

        log("Koniec konwersji " + data + " wynik " + conversionResult );

        if ( coresLimitModificationBarrier.get() != null ) {
            PMO_ThreadsHelper.wait( coresLimitModificationBarrier.get() );
            PMO_TimeHelper.asleep(PMO_Test_Consts.SET_CORES_EXTRA_SLOWDOWN);
        }

        coresInUse.dec();

        if ( additionalSlowdown ) {
            semaphore.release();
            log( "Wykonano semaphore.release()");
        }

        if ( addNewDataPortionBarrier.get() != null ) {
            addNewDataPortionBarrier.get().await();
        }

        return conversionResult;
    }

    private long randomConversionSlowdown() {
        Random rnd = ThreadLocalRandom.current();
        return PMO_Test_Consts.CONVERSION_SLOWDOWN +
                rnd.nextInt( (int)PMO_Test_Consts.CONVERSION_SLOWDOWN_DELTA )
                - PMO_Test_Consts.CONVERSION_SLOWDOWN_DELTA_HALF;
    }

    public List<PMO_Test_DataPortion> getHistory() {
        return history;
    }

    @Override
    public boolean testOK() {
        return stateOK.get();
    }

    public PMO_Test_Converter(AtomicInteger coresLimit, PMO_AtomicCounter coresInUse) {
        this.coresLimit = coresLimit;
        this.coresInUse = coresInUse;
    }
}
