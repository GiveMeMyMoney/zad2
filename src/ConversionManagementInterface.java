/**
 * Interfejs systemu zarządzania konwesjami.
 *
 */
public interface ConversionManagementInterface {

	/**
	 * Klasa ConversionResult jest wynikiem konwersji. Zawiera ona pola z wynikiem
	 * konwersji i pola, w ktorych mają zostać umieszczone referencje do danych,
	 * ktore były konwersji poddane.
	 *
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
	 * Interfejs pozwalający na przekazanie wyniku konwersji.
	 */
	public interface ConversionReceiverInterface {
		/**
		 * Metoda pozwalająca na przekazanie wyniku konwersji. Metody nie wolno używać
		 * współbieżnie - nowy wynik może zostać przekazany dopiero po zakończeniu
		 * metody result dla wyniku wcześniejszego. Wyniki muszą być przekazywane wg.
		 * rosnącego numeru identyfikującego porcje danych.
		 * 
		 * @param result
		 *            wynik konwersji
		 */
		public void result(ConversionResult result);
	}

	/**
	 * Metoda ustala ilość rdzeni, których można używać do konwersji danych. Liczba
	 * ta jest ograniczeniem na maksymalną ilość równoczesnych wywołań metody
	 * convert. W przypadku zwiększenia liczby dostępnych rdzeni możliwe jest ich
	 * natychmiastowe użycie w celu zwiększenia liczby jednocześnie realizowanych
	 * konwersji. W przypadku zmniejszenia liczby dostępnych rdzeni nie wymaga się
	 * przerywania konwersji, które są w toku - wystarczy aby w przez pewien okres
	 * czasu program zarządzający konwersjami nie uruchamiał nowych konwersji - nowe
	 * konwersje można uruchomić dopiero gdy liczba realizowanych konwersji spadnie
	 * poniżej ustawionego przez tą metodę limitu.
	 * 
	 * @param cores
	 *            ograniczenie liczby rdzeni, które moga być używane przez system do
	 *            konwersji danych.
	 */
	public void setCores(int cores);

	/**
	 * Metoda pozwala na przekazanie obiektu odpowiedzialnego za wykonywanie
	 * konwersji danych.
	 * 
	 * @param converter
	 *            konwerter danych.
	 */
	public void setConverter(ConverterInterface converter);

	/**
	 * Metoda umożliwia przekazanie obiektu, do którego należy przekazywać wyniki
	 * konwersji.
	 * 
	 * @param receiver
	 *            obiekt odbierający dane
	 */
	public void setConversionReceiver(ConversionReceiverInterface receiver);

	/**
	 * Za pomocą tej metody użytkownik przekazuje do systemu porcję danych do
	 * konwersji. System ma pozwolić na współbieżne przekazywanie danych. Metoda nie
	 * może blokować pracy wątku przekazującego porcję danych na zbyt długi okres
	 * czasu, czyli jej zadaniem jest zapamiętanie danych przeznaczonych do
	 * konwersji a nie jej wykonywanie.
	 * 
	 * @param data
	 *            dane przeznaczone do konwersji.
	 */
	public void addDataPortion(ConverterInterface.DataPortionInterface data);
}
