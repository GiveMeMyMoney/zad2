
public interface ConverterInterface {
	/**
	 * Nazwa kanału.
	 */
	public enum Channel {
		LEFT_CHANNEL, RIGHT_CHANNEL;
	}

	/**
	 * Dane do przetworzenia
	 */
	public interface DataPortionInterface {
		/**
		 * Numer identyfikacyjny porcji danych. Numery dla danego kanału są zawsze
		 * unikalne. Numer identyfikacyjny pierwszej porcji danych to 1. Ta sama wartość
		 * numeru identyfikacyjnego zgłaszana jest dwa razy: jeden raz dla LEFT_CHANNEL
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
		 * Identyfikacja kanału powiązanego z danymi.
		 * 
		 * @return kanał
		 */
		public Channel channel();
	}

	/**
	 * Metoda realizująca konwersję danych. Dane zawarte w obiekcie zgodnym z
	 * DataPortionInterface przetwarzane są do jednej liczby typu long. Metoda może
	 * być wywoływana współbieżnie.
	 * 
	 * @param data
	 *            dane wejściowe
	 * @return wynik przetwarzania danych.
	 */
	public long convert(DataPortionInterface data);
}
