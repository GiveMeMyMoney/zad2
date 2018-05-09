import java.util.*;
import java.util.stream.IntStream;

public class PMO_Test_DataPortionGenerator {
    private final List<PMO_Test_DataPortion> data = new ArrayList<>();
    private Random rnd = new Random();
    private static int counter;
    private int higherIndex;

    private int[] generateData(int id, ConverterInterface.Channel channel) {
        int[] data = new int[PMO_Test_Consts.DATA_LENGTH];

        data[0] = id;
        data[1] = counter++;
        data[2] = channel.ordinal();
        for (int i = 3; i < PMO_Test_Consts.DATA_LENGTH; i++)
            data[i] = rnd.nextInt(10 );

        return data;
    }

    public void add( int id, ConverterInterface.Channel channel ) {
        data.add(new PMO_Test_DataPortion(id, generateData(id, channel), channel));
    }

    public void add(int id) {
        add( id, ConverterInterface.Channel.RIGHT_CHANNEL );
        add( id, ConverterInterface.Channel.LEFT_CHANNEL );
    }

    public void add( int from, int to ) {
        IntStream.rangeClosed( from, to ).forEach( this::add );
    }

    public void shuffle() {
        Collections.shuffle(data);
    }

    public List<PMO_Test_DataPortion> getAsList() {
        return data;
    }

    public Set<PMO_Test_DataPortion> getAsSet() {
        return new TreeSet<>( data );
    }

    public int getNumberOfDataPortions() {
        return data.size();
    }

    public List<PMO_Test_DataPortion> getSubList( int size ) {
        List<PMO_Test_DataPortion> result;
        int newIndex;
        if ( higherIndex < getNumberOfDataPortions() ) {
            newIndex = Math.min( getNumberOfDataPortions(), higherIndex + size );
            result = new ArrayList<>( data.subList(higherIndex, newIndex ));
            higherIndex = newIndex;
        } else result = null;
        return result;
    }

    public void merge( PMO_Test_DataPortionGenerator generator2 ) {
        data.addAll( generator2.data );
    }
}
