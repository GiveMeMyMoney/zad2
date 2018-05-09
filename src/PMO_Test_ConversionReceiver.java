import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PMO_Test_ConversionReceiver implements ConversionManagementInterface.ConversionReceiverInterface, PMO_LogSource, PMO_Testable {

    private final PMO_Test_Converter converter;
    private final AtomicBoolean stateOK = new AtomicBoolean( true );
    private final AtomicInteger nextExpectedID = new AtomicInteger( 1 );
    private final PMO_AtomicCounter usageCounterMax = PMO_CountersFactory.createCommonMaxStorageCounter();
    private final PMO_AtomicCounter usageCounter = PMO_CountersFactory.createCounterWithMaxStorageSet();

    public PMO_Test_ConversionReceiver( PMO_Test_Converter converter ) {
        this.converter = converter;
    }

    private Optional<Integer> getID( ConversionManagementInterface.ConversionResult result ) {
        if ( nullInResult(  result ) ) return Optional.empty();

        if (notTheSameID(result)) return Optional.empty();

        return Optional.of( result.leftChannelData.id() );
    }

    private boolean nullInResult( ConversionManagementInterface.ConversionResult result ) {

        if ( result == null ) {
            errorRegistration( "Do metody result przeslano null");
            return true;
        }

        if ( result.leftChannelData == null ) {
            errorRegistration( "leftChannelData ustawiony na null");
            return true;
        }

        if ( result.rightChannelData == null ) {
            errorRegistration( "rightChannelData ustawiony na null");
            return true;
        }

        if ( result.leftChannelData.channel() == null ) {
            errorRegistration( "Dane w result dla lewego kanalu maja wpisany null");
        }

        if ( result.rightChannelData.channel() == null ) {
            errorRegistration( "Dane w result dla prawego kanalu maja wpisany null");
        }

        return false;
    }

    private boolean notTheSameID(ConversionManagementInterface.ConversionResult result ) {
        if ( result.leftChannelData.id() != result.rightChannelData.id() ) {
            errorRegistration( "Dane w result maja różne ID: " +
                    result.leftChannelData.id() + " oraz " + result.rightChannelData.id() );
            return true;
        }
        return false;
    }

    private void testResult( ConversionManagementInterface.ConversionResult result ) {

        if ( nullInResult(result ) ) return;

        if ( ! converter.wasConverted( result.leftChannelData ) ) {
            errorRegistration( "Dane dla lewego kanalu nie byly wcześniej poddane konwersji");
        }

        if ( ! converter.wasConverted( result.rightChannelData ) ) {
            errorRegistration( "Dane dla prawego kanalu nie byly wcześniej poddane konwersji");
        }

        if ( converter.conversionResult( result.rightChannelData ) != result.rightChannelConversionResult ) {
            errorRegistration( "Wynik konwersji dla prawego kanału jest błędny");
        }

        if ( converter.conversionResult( result.leftChannelData ) != result.leftChannelConversionResult ) {
            errorRegistration( "Wynik konwersji dla lewego kanału jest błędny");
        }

        if (notTheSameID(result)) {
            errorRegistration( "UWAGA: Ponieważ ID dla prawego i lewego kanału nie są zgodne, nie można określić" +
            " wartości ID dla tego rezultatu oraz następnej poprawnej wartości ID");
            return;
        }

        if ( result.leftChannelData.id() != nextExpectedID.get() ) {
            errorRegistration( "Oczekiwano innego wyniku. Oczekiwano " + nextExpectedID.get() +
             " dotarł " + result.leftChannelData.id() );
        }

        if ( result.leftChannelData.channel() == ConverterInterface.Channel.RIGHT_CHANNEL ) {
            errorRegistration( "Dane w result dla lewego kanału mają wpisany prawy");
        }

        if ( result.rightChannelData.channel() == ConverterInterface.Channel.LEFT_CHANNEL ) {
            errorRegistration( "Dane w result dla prawego kanału mają wpisany lewy");
        }
    }

    private void errorRegistration( String txt ) {
        error( txt );
        stateOK.set( false );
    }

    private String conversionResult2String( ConversionManagementInterface.ConversionResult result ) {
        return "ConversionResult[ [ " +
                result.leftChannelData.id() +
                ", " +
                result.leftChannelConversionResult +
                " ][ " +
                result.rightChannelData.id() +
                ", " +
                result.rightChannelConversionResult +
                " ] ]";
    }

    @Override
    public void result(ConversionManagementInterface.ConversionResult result ) {
        usageCounter.incAndStoreMax();

        String resultStr = conversionResult2String(result);

        log( "Odebrano dane " + resultStr );

        if ( usageCounterMax.get() > 1 ) {
            errorRegistration( "Metoda result uruchomiona została współbieżnie");
        }

        testResult(result);


        PMO_TimeHelper.asleep( PMO_Test_Consts.RESULT_RECEIVER_SLOWDOWN );

        Optional<Integer> id = getID(result);
        if ( id.isPresent() ) {
            nextExpectedID.incrementAndGet();
        }

        usageCounter.dec();
    }

    public int nextExpectedID() {
        return nextExpectedID.get();
    }

    @Override
    public boolean testOK() {
        return stateOK.get();
    }
}
