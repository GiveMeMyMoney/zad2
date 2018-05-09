import java.util.Arrays;
import java.util.Objects;

public class PMO_Test_DataPortion implements ConverterInterface.DataPortionInterface {

    private final int id;
    private final ConverterInterface.Channel channel;
    private final int[] data;

    public PMO_Test_DataPortion(int id, int[] data, ConverterInterface.Channel channel) {
        this.id = id;
        this.data = data;
        this.channel = channel;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public int[] data() {
        return data;
    }

    @Override
    public ConverterInterface.Channel channel() {
        return channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PMO_Test_DataPortion that = (PMO_Test_DataPortion) o;
        return id == that.id &&
                channel == that.channel &&
                Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(id, channel);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PMO_Test_DataPortion[ id: ");
        sb.append(id);
        sb.append(" channel: ");
        if ( channel.name().equals( "LEFT_CHANNEL"))
            sb.append(" ");
        sb.append(channel.name());
        sb.append(" ]");
        return sb.toString();
    }
}
