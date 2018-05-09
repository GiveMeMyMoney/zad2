
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

class ConversionManagement implements ConversionManagementInterface {

    AtomicInteger cores = new AtomicInteger(0);
    AtomicInteger workingThreads = new AtomicInteger(0);
    ConverterInterface converter;
    ConversionReceiverInterface receiver;
    PriorityBlockingQueue<ConverterInterface.DataPortionInterface> dataPortions;
    final Object dataPortionLock = new Object();
    final Object threadNumberLock = new Object();

    ConversionManagement() {
        dataPortions = new PriorityBlockingQueue<>(10, Comparator.comparingInt(ConverterInterface.DataPortionInterface::id));
        new Thread(new Publisher()).start();
    }

    @Override
    public void setCores(int cores) {
        synchronized (threadNumberLock) {
            int previousNumberOfCores = this.cores.get();
            this.cores.set(cores);
            if(cores > previousNumberOfCores) {
                int desiredNumberOfThreads = cores - previousNumberOfCores;
                for(int i = 0; i < desiredNumberOfThreads; i++) {
                    new Thread(new Converter(dataPortions)).start();
                    workingThreads.incrementAndGet();
                }
            }
        }
    }

    @Override
    public void setConverter(ConverterInterface converter) {
        this.converter = converter;
    }

    @Override
    public void setConversionReceiver(ConversionReceiverInterface receiver) {
        this.receiver = receiver;
    }

    @Override
    public void addDataPortion(ConverterInterface.DataPortionInterface data) {
        synchronized (dataPortionLock) {
            dataPortions.put(data);
            dataPortionLock.notify();
        }
    }


    class Converter implements Runnable {
        PriorityBlockingQueue<ConverterInterface.DataPortionInterface> dataPortions;

        public Converter(PriorityBlockingQueue<ConverterInterface.DataPortionInterface> dataPortions) {
            this.dataPortions = dataPortions;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    ConverterInterface.DataPortionInterface dataPortion;
                    while(true){
                        synchronized (dataPortionLock) {
                            if(dataPortions.isEmpty()) {
                                dataPortionLock.wait();
                            }
                            synchronized (threadNumberLock) {
                                if(workingThreads.get() > cores.get()){
                                    workingThreads.decrementAndGet();
                                    return;
                                }
                            }
                            dataPortion = dataPortions.poll();
                            if(dataPortion != null) {
                                break;
                            }
                        }
                    }

                    long conversionResult = converter.convert(dataPortion);
                    synchronized (cacheResultsLock) {
                        ConvertedValue cachedValue =  readyDataPortions.get(dataPortion.id());
                        if(cachedValue == null){
                            readyDataPortions.put(dataPortion.id(),new ConvertedValue(dataPortion,conversionResult));
                        } else {
                            readyDataPortions.remove(cachedValue.getDataPortionInterface().id());
                            synchronized (publishingLock) {
                                if(cachedValue.getDataPortionInterface().channel() == ConverterInterface.Channel.LEFT_CHANNEL) {
                                    results.put(new ConversionResult(cachedValue.getDataPortionInterface(),dataPortion,cachedValue.getConvertedResult(),conversionResult));
                                } else {
                                    results.put(new ConversionResult(dataPortion,cachedValue.getDataPortionInterface(),conversionResult,cachedValue.getConvertedResult()));
                                }
                                publishingLock.notifyAll();
                            }
                        }
                    }
                    synchronized (threadNumberLock) {
                        if(workingThreads.get() > cores.get()){
                            workingThreads.decrementAndGet();
                            return;
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted");
                }
            }
        }
    }

    class ConvertedValue {
        ConverterInterface.DataPortionInterface dataPortionInterface;
        long convertedResult;

        public ConvertedValue(ConverterInterface.DataPortionInterface dataPortionInterface, long convertedResult) {
            this.dataPortionInterface = dataPortionInterface;
            this.convertedResult = convertedResult;
        }

        public ConverterInterface.DataPortionInterface getDataPortionInterface() {
            return dataPortionInterface;
        }

        public long getConvertedResult() {
            return convertedResult;
        }
    }

    Object cacheResultsLock = new Object();
    Map<Integer, ConvertedValue> readyDataPortions = new HashMap<>();
    BlockingQueue<ConversionResult> results = new PriorityBlockingQueue<>(10,Comparator.comparingInt(this::toInt));

    private int toInt(ConversionResult conversionResult) {
        return conversionResult.leftChannelData.id();
    }

    final Object publishingLock = new Object();

    int getId(ConversionResult conversionResult) {
        return conversionResult.leftChannelData.id();
    }

    AtomicInteger idToBePublished = new AtomicInteger(1);


    class Publisher implements Runnable {

        @Override
        public void run() {

            while(true) {
                synchronized (publishingLock) {
                    while(true) {
                        if(results.isEmpty()){
                            try {
                                publishingLock.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException("Interrupted");
                            }
                        }
                        ConversionResult conversionResult = results.peek();
                        if(getId(conversionResult) == idToBePublished.get()) {
                            break;
                        } else {
                            try {
                                publishingLock.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException("Interrupted");
                            }
                        }
                    }

                ConversionResult conversionResult = results.poll();
                receiver.result(conversionResult);
                idToBePublished.incrementAndGet();
                }
            }
        }
    }
}
