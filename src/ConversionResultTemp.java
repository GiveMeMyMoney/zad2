import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Klasa pomocnicza do zbierania danych do struktury
 */
///
class ConversionResultTemp implements Comparable {
    private int id;
    private ConverterInterface.DataPortionInterface leftChannelData;
    private ConverterInterface.DataPortionInterface rightChannelData;
    private long leftChannelConversionResult;
    private long rightChannelConversionResult;

    public ConversionResultTemp(ConverterInterface.Channel channel, ConverterInterface.DataPortionInterface channelData, long channelConversionResult) {
        this.id = channelData.id();
        if (channel.equals(ConverterInterface.Channel.LEFT_CHANNEL)) {
            this.leftChannelData = channelData;
            this.leftChannelConversionResult = channelConversionResult;
        } else {
            this.rightChannelData = channelData;
            this.rightChannelConversionResult = channelConversionResult;
        }
    }

    //region GETTER % SETTER
    public int getId() {
        return id;
    }

    public long getLeftChannelConversionResult() {
        return leftChannelConversionResult;
    }

    public long getRightChannelConversionResult() {
        return rightChannelConversionResult;
    }

    public ConverterInterface.DataPortionInterface getLeftChannelData() {
        return leftChannelData;
    }

    public ConverterInterface.DataPortionInterface getRightChannelData() {
        return rightChannelData;
    }

    public void setLeftChannelData(ConverterInterface.DataPortionInterface leftChannelData) {
        this.leftChannelData = leftChannelData;
    }

    public void setRightChannelData(ConverterInterface.DataPortionInterface rightChannelData) {
        this.rightChannelData = rightChannelData;
    }

    public void setLeftChannelConversionResult(long leftChannelConversionResult) {
        this.leftChannelConversionResult = leftChannelConversionResult;
    }

    public void setRightChannelConversionResult(long rightChannelConversionResult) {
        this.rightChannelConversionResult = rightChannelConversionResult;
    }
    //endregion

    @Override
    public int compareTo(Object o) {
        ConversionResultTemp that = (ConversionResultTemp) o;
        return Integer.compare(this.id, that.id);
    }

    public boolean isFinished() {
        return leftChannelData != null && rightChannelData != null;
    }

    public boolean isPresentedData(ConverterInterface.DataPortionInterface data) {
        return this.id == data.id();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversionResultTemp that = (ConversionResultTemp) o;

        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return getId();
    }
}

/**
 * Klasa glowna
 */
class ConversionManagement implements ConversionManagementInterface {
    private final Object lockHelper = new Object();
    private final Comparator<ConverterInterface.DataPortionInterface> comparatorForData = (p1, p2) -> Integer.compare(p1.id(), p2.id());

    private AtomicInteger actualIdReceive; //wzorzec Command/Polecenie (?)
    private PriorityBlockingQueue<ConverterInterface.DataPortionInterface> channelDataToConvert;
    private PriorityBlockingQueue<ConversionResultTemp> dataToReceiveList; //Lista danych uzupelniana podczas pracy programu i jezeli wszystkie dane sa skompletowane to rekord zostaje zwrocony i usuniety z listy

    private ConverterInterface converter;
    private ConversionReceiverInterface receiver;

    private PriorityBlockingQueue<Thread> threads;

    //region pomocnicze metody do zmiennych

    private ConverterInterface.DataPortionInterface getNextDataToConvert() {
        ConverterInterface.DataPortionInterface data;
        //TODO sprawdzic czy bierze najmniejsza
        data = channelDataToConvert.poll(); // Retrieves and removes the head of this queue, or returns null if this queue is empty.
        return data;
    }

    private synchronized void addDataToReceive(ConverterInterface.DataPortionInterface data, long value) {
        ConversionResultTemp conversionResultDataTemp = dataToReceiveList.stream()
                .filter(conversionResultTemp -> conversionResultTemp.isPresentedData(data))
                .findFirst()
                .orElse(null);

        //nie ma takiego w liscie
        if (conversionResultDataTemp == null) {
            // wpisuje poczatkowe dane do ktorych bede dopisywal drugi kanal
            conversionResultDataTemp = new ConversionResultTemp(data.channel(), data, value);
            dataToReceiveList.add(conversionResultDataTemp);
            /*dataToReceiveList = dataToReceiveList.stream()
                    .sorted()
                    .collect(Collectors.toCollection());*/ //TODO czy trzeba zwracac
        } //jest, wiec uzupelniam dane drugiego kanalu i sprawdzam czy wysylac do result
        else {
            if (data.channel().equals(ConverterInterface.Channel.LEFT_CHANNEL)) {
                conversionResultDataTemp.setLeftChannelData(data);
                conversionResultDataTemp.setLeftChannelConversionResult(value);
            } else {
                conversionResultDataTemp.setRightChannelData(data);
                conversionResultDataTemp.setRightChannelConversionResult(value);
            }

            //TODO Nowy wynik mo¿na przekazaæ u¿ytkownikowi dopiero, gdy odebrany zostanie poprzedni (np. dane o ID 12 wysy³amy dopiero po tym, jak wywo³anie metody result dla ID 11 ju¿ siê zakoñczy).
            sendResultToReceiverAndRemoveTemp(conversionResultDataTemp);
        }
    }

    //endregion

    public ConversionManagement() {
        this.actualIdReceive = new AtomicInteger(1);
        channelDataToConvert = new PriorityBlockingQueue<>(100, comparatorForData);
        dataToReceiveList = new PriorityBlockingQueue<>();
        threads = new PriorityBlockingQueue<>();
        //this.coreInUse = 0;
    }

    //region pomocnicze metody

    private void doConvertInThread() {
        ConverterInterface.DataPortionInterface data = getNextDataToConvert();
        if (data != null) {
            long value = converter.convert(data);
            addDataToReceive(data, value);
        }
    }

    private synchronized void sendResultToReceiverAndRemoveTemp(ConversionResultTemp tempData) {
        //czy kolej na nastepny result
        int id = tempData.getId();
        if (actualIdReceive.compareAndSet(id, id + 1)) {
            ConversionResultTemp dataForResult = dataToReceiveList.poll();
            if (dataForResult != null) {
                sendResult(dataForResult); //sprawdzic czy bierze najnizszy
            }
            //dataToReceiveList.remove(tempData);
            //pozostale ktore mozna zwrocic //TODO zmiana na streama
            for (ConversionResultTemp result : dataToReceiveList) {
                id = result.getId();
                if (result.isFinished() && actualIdReceive.compareAndSet(id, id + 1)) {
                    dataForResult = dataToReceiveList.poll(); //sprawdzic czy bierze najnizszy
                    if (dataForResult != null) {
                        sendResult(dataForResult);
                    }
                } else {
                    break;
                }
            }
        }
    }

    private synchronized void sendResult(ConversionResultTemp tempData) {
        ConversionResult finalConversionResult = new ConversionResult(tempData.getLeftChannelData(), tempData.getRightChannelData(),
                tempData.getLeftChannelConversionResult(), tempData.getRightChannelConversionResult());

        //TODO Wyniki obliczeñ nale¿y przekazywaæ u¿ytkownikowi zawsze sekwencyjnie (tylko jeden w danej chwili)
        //synchronized ?
        receiver.result(finalConversionResult);
    }

    //endregion

    //Przetwarzaæ wolno tylko tyle porcji danych na ile pozwala limit mo¿liwych do u¿ycia rdzeni. (Do obliczeñ)
    //nie mo¿e blokowaæ w¹tku, który j¹ wywo³uje na zbyt d³ugi okres czasu.
    @Override
    public void setCores(int cores) {
        //TODO jeden glowny THREAD do obslugi pozostalych
        //startuje ile mo¿na
        for (int i = 0; i < cores; i++) {
            new Thread("Watek " + i) {
                @Override
                public void run() {
                    //Program nie mo¿e zajmowaæ zasobów CPU w przypadku, gdy system nie wykonuje ¿adnych operacji (nie ma danych do prowadzenia obliczeñ). Oczekuje siê, ¿e utworzone przez system w¹tki (o ile takie bêd¹) zostan¹ wstrzymane za pomoc¹ metody wait() lub jej odpowiednika.
                    while (true) {
                        try {
                            if (channelDataToConvert.isEmpty()) {
                                synchronized (lockHelper) {
                                    lockHelper.wait();
                                }
                            }
                            //channelDataToConvert.notify();
                            //System.out.println("WATEK: " + getName() + " ***robi robote");
                            doConvertInThread();
                            //System.out.println("WATEK: " + getName() + " ---SKONCZYL robote");
                        } catch (InterruptedException e) {
                            System.out.println("InterruptedException dla: " + getName() + " BLAD: " + e.getMessage());
                        }
                    }

                    //jesli nie ma danych do przetwarzania = to czekaj
                    /*if (channelDataToConvert.size() == 0) {
                        synchronized (this) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                System.out.println("ERROR w wait WATEK: " + getName() + " msg: " + e.getMessage());
                            }
                        }


                        //TODO metoda zrob konwersje najnizszego
                        doConvertInThread();
                    }*/


                }
            }.start();

            //threads.add(thread);
        }

        //threads.forEach(Thread::start);


        /*int sizeRazy = channelDataToConvert.size();

        //TODO w watkach
        for (int i = 0; i < sizeRazy; i++) {
            doConvertInThread();
        }*/

    }

    @Override
    public void setConverter(ConverterInterface converter) {
        this.converter = converter;
    }

    @Override
    public void setConversionReceiver(ConversionReceiverInterface receiver) {
        this.receiver = receiver;
    }

    //Zadaniem metody odbieraj¹cej porcje danych jest ich zapamiêtanie
    //Same obliczenia maj¹ zostaæ wykonane w dogodnej chwili i za pomoc¹ innego w¹tku.
    //nie mo¿e blokowaæ w¹tku, który j¹ wywo³uje na zbyt d³ugi okres czasu.
    @Override
    public void addDataPortion(ConverterInterface.DataPortionInterface data) {
        //czy tu trzeba synchronized
        synchronized (lockHelper) {
            this.channelDataToConvert.add(data);
            lockHelper.notify();
        }

    }


}