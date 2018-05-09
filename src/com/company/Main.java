package com.company;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static ConverterInterface.DataPortionInterface getDataPortion(int id, ConverterInterface.Channel channel) {
        return new ConverterInterface.DataPortionInterface() {
            @Override
            public int id() {
                return id;
            }

            @Override
            public int[] data() {
                return new int[0];
            }

            @Override
            public ConverterInterface.Channel channel() {
                return channel;
            }
        };
    }

    public static void main(String[] args) {
        List<ConverterInterface.DataPortionInterface> channelDataToConvert = new ArrayList<>();
        List<ConverterInterface.DataPortionInterface> channelDataToConvert2 = new ArrayList<>();

        channelDataToConvert.add(getDataPortion(1, ConverterInterface.Channel.LEFT_CHANNEL));
        channelDataToConvert.add(getDataPortion(3, ConverterInterface.Channel.LEFT_CHANNEL));
        channelDataToConvert.add(getDataPortion(3, ConverterInterface.Channel.RIGHT_CHANNEL));
        channelDataToConvert.add(getDataPortion(2, ConverterInterface.Channel.RIGHT_CHANNEL));

        ConversionManagement conversionManagement = new ConversionManagement();

        ConverterInterface converter = new ConverterInterface() {
            @Override
            public long convert(DataPortionInterface data) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return data.id();
            }
        };

        ConversionManagementInterface.ConversionReceiverInterface receiver = new ConversionManagementInterface.ConversionReceiverInterface() {
            @Override
            public void result(ConversionManagementInterface.ConversionResult result) {
                //cos tam rob se
                return;
            }
        };

        conversionManagement.setConverter(converter);
        conversionManagement.setReceiver(receiver);
        channelDataToConvert.forEach(conversionManagement::addDataPortion);
        conversionManagement.setCores(2);

        //2 RAZ
        channelDataToConvert = new ArrayList<>();

        channelDataToConvert.add(getDataPortion(2, ConverterInterface.Channel.LEFT_CHANNEL));
        //channelDataToConvert.add(getDataPortion(2, ConverterInterface.Channel.RIGHT_CHANNEL));
        channelDataToConvert.add(getDataPortion(1, ConverterInterface.Channel.RIGHT_CHANNEL));
        channelDataToConvert.add(getDataPortion(4, ConverterInterface.Channel.LEFT_CHANNEL));
        channelDataToConvert.add(getDataPortion(4, ConverterInterface.Channel.RIGHT_CHANNEL));

        channelDataToConvert.forEach(conversionManagement::addDataPortion);
        conversionManagement.setCores(1);
    }
}

interface ConverterInterface {
    /**
     * Nazwa kana�?u.
     */
    public enum Channel {
        LEFT_CHANNEL, RIGHT_CHANNEL;
    }

    /**
     * Dane do przetworzenia
     */
    public interface DataPortionInterface {
        /**
         * Numer identyfikacyjny porcji danych. Numery dla danego kana�?u s�? zawsze
         * unikalne. Numer identyfikacyjny pierwszej porcji danych to 1. Ta sama warto�?�?
         * numeru identyfikacyjnego zg�?aszana jest dwa razy: jeden raz dla LEFT_CHANNEL
         * i jeden raz dla RIGHT_CHANNEL.
         *
         * @return numer identyfikacyjny
         */
        public int id();

        /**
         * Dane przekazywane w porcji danych.
         *
         * @return dane do przetworzenia
         */
        public int[] data();

        /**
         * Identyfikacja kana�?u powi�?zanego z danymi.
         *
         * @return kana�?
         */
        public Channel channel();
    }

    /**
     * Metoda realizuj�?ca konwersj�? danych. Dane zawarte w obiekcie zgodnym z
     * DataPortionInterface przetwarzane s�? do jednej liczby typu long. Metoda moşe
     * by�? wywo�?ywana wspó�?bieşnie.
     *
     * @param data dane wej�?ciowe
     * @return wynik przetwarzania danych.
     */
    public long convert(DataPortionInterface data);
}

/**
 * Interfejs systemu zarz�?dzania konwesjami.
 */
interface ConversionManagementInterface {

    /**
     * Klasa ConversionResult jest wynikiem konwersji. Zawiera ona pola z wynikiem
     * konwersji i pola, w ktorych maj�? zosta�? umieszczone referencje do danych,
     * ktore by�?y konwersji poddane.
     */
    public class ConversionResult {
        public final ConverterInterface.DataPortionInterface leftChannelData;
        public final ConverterInterface.DataPortionInterface rightChannelData;
        public final long leftChannelConversionResult;
        public final long rightChannelConversionResult;

        public ConversionResult(ConverterInterface.DataPortionInterface leftChannelData,
                                ConverterInterface.DataPortionInterface rightChannelData, long leftChannelConversionResult,
                                long rightChannelConversionResult) {
            this.leftChannelData = leftChannelData;
            this.rightChannelData = rightChannelData;
            this.leftChannelConversionResult = leftChannelConversionResult;
            this.rightChannelConversionResult = rightChannelConversionResult;
        }

    }

    /**
     * Interfejs pozwalaj�?cy na przekazanie wyniku konwersji.
     */
    public interface ConversionReceiverInterface {
        /**
         * Metoda pozwalaj�?ca na przekazanie wyniku konwersji. Metody nie wolno uşywa�?
         * wspó�?bieşnie - nowy wynik moşe zosta�? przekazany dopiero po zako�?czeniu
         * metody result dla wyniku wcze�?niejszego. Wyniki musz�? by�? przekazywane wg.
         * rosn�?cego numeru identyfikuj�?cego porcje danych.
         *
         * @param result wynik konwersji
         */
        public void result(ConversionResult result);
    }

    /**
     * Metoda ustala ilo�?�? rdzeni, których moşna uşywa�? do konwersji danych. Liczba
     * ta jest ograniczeniem na maksymaln�? ilo�?�? równoczesnych wywo�?a�? metody
     * convert. W przypadku zwi�?kszenia liczby dost�?pnych rdzeni moşliwe jest ich
     * natychmiastowe uşycie w celu zwi�?kszenia liczby jednocze�?nie realizowanych
     * konwersji. W przypadku zmniejszenia liczby dost�?pnych rdzeni nie wymaga si�?
     * przerywania konwersji, które s�? w toku - wystarczy aby w przez pewien okres
     * czasu program zarz�?dzaj�?cy konwersjami nie uruchamia�? nowych konwersji - nowe
     * konwersje moşna uruchomi�? dopiero gdy liczba realizowanych konwersji spadnie
     * ponişej ustawionego przez t�? metod�? limitu.
     *
     * @param cores ograniczenie liczby rdzeni, które moga by�? uşywane przez system do
     *              konwersji danych.
     */
    public void setCores(int cores);

    /**
     * Metoda pozwala na przekazanie obiektu odpowiedzialnego za wykonywanie
     * konwersji danych.
     *
     * @param converter konwerter danych.
     */
    public void setConverter(ConverterInterface converter);

    /**
     * Metoda umoşliwia przekazanie obiektu, do którego naleşy przekazywa�? wyniki
     * konwersji.
     *
     * @param receiver obiekt odbieraj�?cy dane
     */
    public void setReceiver(ConversionReceiverInterface receiver);

    /**
     * Za pomoc�? tej metody uşytkownik przekazuje do systemu porcj�? danych do
     * konwersji. System ma pozwoli�? na wspó�?bieşne przekazywanie danych. Metoda nie
     * moşe blokowa�? pracy w�?tku przekazuj�?cego porcj�? danych na zbyt d�?ugi okres
     * czasu, czyli jej zadaniem jest zapami�?tanie danych przeznaczonych do
     * konwersji a nie jej wykonywanie.
     *
     * @param data dane przeznaczone do konwersji.
     */
    public void addDataPortion(ConverterInterface.DataPortionInterface data);
}

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

    private void addDataToReceive(ConverterInterface.DataPortionInterface data, long value) {
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

            //TODO Nowy wynik można przekazać użytkownikowi dopiero, gdy odebrany zostanie poprzedni (np. dane o ID 12 wysyłamy dopiero po tym, jak wywołanie metody result dla ID 11 już się zakończy).
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

        //TODO Wyniki obliczeń należy przekazywać użytkownikowi zawsze sekwencyjnie (tylko jeden w danej chwili)
        //synchronized ?
        receiver.result(finalConversionResult);
    }

    //endregion

    //Przetwarzać wolno tylko tyle porcji danych na ile pozwala limit możliwych do użycia rdzeni. (Do obliczeń)
    //nie może blokować wątku, który ją wywołuje na zbyt długi okres czasu.
    @Override
    public void setCores(int cores) {
        //TODO jeden glowny THREAD do obslugi pozostalych
        //startuje ile można
        for (int i = 0; i < cores; i++) {
            new Thread("Watek " + i) {
                @Override
                public void run() {
                    //Program nie może zajmować zasobów CPU w przypadku, gdy system nie wykonuje żadnych operacji (nie ma danych do prowadzenia obliczeń). Oczekuje się, że utworzone przez system wątki (o ile takie będą) zostaną wstrzymane za pomocą metody wait() lub jej odpowiednika.
                    while (true) {
                        try {
                            if (channelDataToConvert.isEmpty()) {
                                synchronized (lockHelper) {
                                    lockHelper.wait();
                                }
                            }
                            //channelDataToConvert.notify();
                            System.out.println("WATEK: " + getName() + " ***robi robote");
                            doConvertInThread();
                            System.out.println("WATEK: " + getName() + " ---SKONCZYL robote");
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
    public void setReceiver(ConversionReceiverInterface receiver) {
        this.receiver = receiver;
    }

    //Zadaniem metody odbierającej porcje danych jest ich zapamiętanie
    //Same obliczenia mają zostać wykonane w dogodnej chwili i za pomocą innego wątku.
    //nie może blokować wątku, który ją wywołuje na zbyt długi okres czasu.
    @Override
    public void addDataPortion(ConverterInterface.DataPortionInterface data) {
        //czy tu trzeba synchronized
        synchronized (lockHelper) {
            this.channelDataToConvert.add(data);
            lockHelper.notifyAll();
        }

    }


}