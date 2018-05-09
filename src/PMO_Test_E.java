import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Test sprawdza poprawność priorytetyzacji paczek danych
 * o mniejszych numerach ID. Idea: wprowadzane są paczki
 * o wysokich numerach ID. Wątki są blokowane za pomocą
 * PMO_Barrier. Przed zwolnieniem jednego wątku dodawana
 * jest nowa paczka danych o mniejszym ID - musi
 * dotrzeć do konwertera jako pierwsza.
 */
public class PMO_Test_E extends PMO_Test_A {
    protected static final int MAX_CORES = 20;
    protected static final int RESULTS_EXPECTED =
            (DATA_PORTIONS_PER_SENDER * DATA_PORTIONS_SENDERS + MAX_CORES) / 2;

    private PMO_Barrier barrier;
    private Queue<PMO_Test_DataPortion> dataPortionsWithPriority;
    private final PMO_Test_DataPortionGenerator generatorL = new PMO_Test_DataPortionGenerator();

    @Override
    public long getRequiredTime() {
        return 30000;
    }

    /**
     * Kod do wykonania w momencie spotkania wszystkich watków
     */
    class RendezVous implements Runnable {
        @Override
        public void run() {
            log("Doszło do spotkania wszystkich wątków na PMO_Barrier, uwalniam jeden");
            barrier.releaseOneThread();
        }
    }

    class OneThreadRelease implements Runnable {
        @Override
        public void run() {
            PMO_Test_DataPortion dataPortion = dataPortionsWithPriority.poll();
            if (dataPortion != null) {
                management.addDataPortion(dataPortion);
                dataConverter.setNextDataPortion(dataPortion);
                log("Dodano priorytetową paczke " + dataPortion );
                log( "Za chwilę zostanie uwolniony jeden, losowy wątek");
            } else {
                log("Brak priorytetowych paczek.");
                dataConverter.setNextDataPortion(null);
                dataConverter.setAddNewDataPortionBarrier(null);
                barrier.releaseAllThreads();
                log("Odblokowano wszystkie wątki.");
            }
        }
    }

    @Override
    protected void prepareInitialDataPortions() {
        generator.add(MAX_CORES/2 + 1, RESULTS_EXPECTED );
    }

    @Override
    protected void testDependentPreparations() {
        generatorL.add(1, MAX_CORES / 2);
        generatorL.shuffle();
        dataPortionsWithPriority = new ConcurrentLinkedDeque<>();
        dataPortionsWithPriority.addAll(generatorL.getAsList());

        RendezVous rv = new RendezVous();
        OneThreadRelease ot = new OneThreadRelease();
        barrier = new PMO_Barrier(MAX_CORES, rv);
        barrier.setCode2RunBeforeThreadRelease(ot);
        dataConverter.setAddNewDataPortionBarrier(barrier);
    }

    protected void prepareConversionManagement() {
        prepareConversionManagement(MAX_CORES);
    }

    protected void prepareTestEnvironment() {
        prepareTestEnvironment(MAX_CORES);
    }

    protected boolean parametricTest() {
        return parametricTest(MAX_CORES, RESULTS_EXPECTED);
    }

    @Override
    protected void testDependentPreparationsBeforeTestOK() {
        generator.merge( generatorL );
    }
}
