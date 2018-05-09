import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class PMO_Test_DataPortionSender implements PMO_LogSource, PMO_RunnableAndTestable {

    private List<PMO_Test_DataPortion> data2send;
    private ConversionManagementInterface management;
    private CyclicBarrier barrier;
    private long callTimeSum;
    private long sent;
    private boolean stateOK = true;

    public PMO_Test_DataPortionSender( ConversionManagementInterface management,
                                       List<PMO_Test_DataPortion> data2send) {
        this.data2send = data2send;
        this.management = management;
    }

    public void setCyclicBarrier( CyclicBarrier barrier ) {
        this.barrier = barrier;
    }

    @Override
    public boolean testOK() {
        if ( sent != data2send.size() ) {
            error( "PMO_Test_DataPortionSender nie byl w stanie dostarczyc wszystkich danych, powinno byc "
            + data2send.size() + ", jest " + sent);
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        assert management != null;
        assert data2send != null;
        log( "Oczkiwanie na innych wysylaczy danych");

        PMO_ThreadsHelper.wait( barrier );

        log( "PMO_Test_DataPortionSender rozpoczyna przekazywania danych");
        for ( ConverterInterface.DataPortionInterface data : data2send ) {
            callTimeSum += PMO_TimeHelper.executionTime(
                    () -> {
                        try {
                            management.addDataPortion(data);
                            sent++;
                            log( "PMO_Test_DataPortionSender przekazał " + data );
                        }
                        catch ( Exception e ) {
                            exception2error( "W trakcie pracy addDataPortion doszło do wyjątku", e );
                            stateOK = false;
                        }
                    }
            );
        }
        log( "PMO_Test_DataPortionSender zakonczył przekazywanie danych, przekazano " + sent + " porcji");
    }

    public long getSumAddDataPortionCallTime() {
        return callTimeSum;
    }
}
