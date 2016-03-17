package br.unb.hugowschneider.fastautilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

public class NucleotidePatternCounter {

	public enum OutputType {
		CSV
	}

	public enum CountType {
		NUCLEOTIDE("ACGT"), NUCLEOTIDE_ALL("ACGTRYKMSWBDHVN"), AMINO_ACID("ABCDEFGHIJKLMNOPQRSTUVWYZX");
		//
		private String alphabet;

		private CountType(String alphabet) {
			this.alphabet = alphabet;
		}

		public Map<CharSequence, Integer> createCountMap(int size) {
			Map<CharSequence, Integer> map = new HashMap<CharSequence, Integer>();
			Permute permute = new Permute(this.alphabet);
			List<String> permutations = permute.permute(size, "");
			for (String string : permutations) {
				map.put(string, 0);
			}
			return map;
		}
	}

	private File input;
	private Integer minPatternSize;
	private Integer maxPatternSize;

	public NucleotidePatternCounter(File input, Integer minPatternSize, Integer maxPatternSize)
			throws FileNotFoundException {
		setInput(input);
		setMinPatternSize(minPatternSize);
		setMaxPatternSize(maxPatternSize);
	}

	public void count(CountType type, OutputType outputType, Appendable output, boolean percent)
			throws ParseException, IOException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(getInput(), "r");
		try {
			char c = (char) randomAccessFile.read();
			if (c != '>') {
				System.out.println(c);
				while (randomAccessFile.length() > randomAccessFile.getFilePointer()) {
					System.out.println((char) randomAccessFile.read());
				}
				throw new ParseException("Does not seem to be a valid fasta file. Missing '>' at the begining of file",
						(int) randomAccessFile.getFilePointer());

			}
			int min = getMinPatternSize();
			int max = getMaxPatternSize();

			Map<CharSequence, Integer> count = type.createCountMap(min);
			for (int i = min + 1; i <= max; i++) {
				count.putAll(type.createCountMap(i));
			}
			Vector<CharSequence> headers = new Vector<>(count.keySet());
			headers.sort(new Comparator<CharSequence>() {
				@Override
				public int compare(CharSequence o1, CharSequence o2) {
					return String.valueOf(o1).compareTo(String.valueOf(o2));
				}
			});
			headers.insertElementAt("Sequence", 0);
			headers.insertElementAt("Size", 0);
			headers.insertElementAt("Sequence Id", 0);
			CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])).withQuote('"')
					.withQuoteMode(QuoteMode.NON_NUMERIC).print(output);
			double last = 0;
			int size = 0;
			while (randomAccessFile.length() > randomAccessFile.getFilePointer()) {
				String sequenceName = randomAccessFile.readLine();
				if (sequenceName.startsWith(">")) {
					size = 0;
					sequenceName = sequenceName.substring(1);
				}
				sequenceName = sequenceName.trim();
				count = type.createCountMap(min);
				for (int i = min + 1; i <= max; i++) {
					count.putAll(type.createCountMap(i));
				}
				StringBuilder pattern = new StringBuilder();
				StringBuilder sequence = new StringBuilder();
				while (randomAccessFile.length() > randomAccessFile.getFilePointer()) {
					double current = (double) randomAccessFile.getFilePointer() * 100.0
							/ (double) randomAccessFile.length();
					if (current - last > 0.5) {
						System.err.println(String.format("%1$.4f%%", current));
						last = current;
					}
					c = (char) randomAccessFile.read();
					while (c == '\n' || c == '\r') {
						c = (char) randomAccessFile.read();
					}
					if (c == '>') {
						size = 0;
						break;
					}
					size++;
					sequence.append(String.valueOf(c));
					pattern.append(String.valueOf(c).toUpperCase());
					if (pattern.length() == max) {
						for (int i = min; i <= max; i++) {
							Integer lastCount = count.get(pattern.subSequence(0, i));
							if (lastCount != null) {
								count.put(pattern.subSequence(0, i), lastCount + 1);
							} else {
								System.err.println(
										String.format("Unrecognized pattern: %1$s", pattern.subSequence(0, i)));
							}

						}
						pattern.replace(0, 1, "");
					}

				}
				while (pattern.length() > 0) {
					for (int i = min; i <= pattern.length(); i++) {
						Integer lastCount = count.get(pattern.subSequence(0, i));
						if (lastCount != null) {
							count.put(pattern.subSequence(0, i), lastCount + 1);
						} else {
							System.err.println(String.format("Unrecognized pattern: %1$s", pattern.subSequence(0, i)));
						}
					}
					pattern.replace(0, 1, "");
				}

				List<Object> record = new ArrayList<>();
				record.add(sequenceName);
				record.add(size);
				record.add(sequence);
				Map<Integer, Double> totals = new HashMap<>();
				for (int i = min; i <= max; i++) {
					totals.put(i, 0.0);
				}
				if (percent) {
					for (Map.Entry<CharSequence, Integer> entry : count.entrySet()) {
						totals.put(entry.getKey().length(), totals.get(entry.getKey().length()) + entry.getValue());
					}

				}
				for (CharSequence charSequence : headers) {
					if (charSequence.equals("Sequence") || charSequence.equals("Size")
							|| charSequence.equals("Sequence Id")) {
						continue;
					}
					if (percent) {
						Double r = ((double) count.get(charSequence)) / totals.get(charSequence.length());
						if (r > 1) {
							System.out.println();
						}
						record.add(r);
					} else {
						record.add(count.get(charSequence));
					}
				}
				csvPrinter.printRecord(record);
			}
		} finally {
			randomAccessFile.close();
		}
	}

	public File getInput() {
		return input;
	}

	public void setInput(File input) {
		this.input = input;
	}

	public Integer getMinPatternSize() {
		return minPatternSize;
	}

	public Integer getMaxPatternSize() {
		return maxPatternSize;
	}

	public void setMinPatternSize(Integer minPatternSize) {
		this.minPatternSize = minPatternSize;
	}

	public void setMaxPatternSize(Integer maxPatternSize) {
		this.maxPatternSize = maxPatternSize;
	}
}
