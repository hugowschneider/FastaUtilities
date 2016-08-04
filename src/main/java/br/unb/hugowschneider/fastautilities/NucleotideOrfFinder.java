package br.unb.hugowschneider.fastautilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class NucleotideOrfFinder {

	private File input;

	public NucleotideOrfFinder(File input) throws FileNotFoundException {
		setInput(input);
	}

	public void find(OutputType outputType, Appendable output) throws ParseException, IOException {
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

			Vector<CharSequence> headers = new Vector<>(2);
			headers.add("Sequence");
			headers.add("First ORF");
			headers.add("Fisrt Start");
			headers.add("Fisrt End");
			headers.add("Longest ORF");
			headers.add("Longest Start");
			headers.add("Longest End");
			CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])).withQuote('"')
					.withQuoteMode(QuoteMode.NON_NUMERIC).print(output);
			double last = 0;

			while (randomAccessFile.length() > randomAccessFile.getFilePointer()) {
				String sequenceName = randomAccessFile.readLine();
				if (sequenceName.startsWith(">")) {
					sequenceName = sequenceName.substring(1);
				}
				sequenceName = sequenceName.trim();
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
					if (c == '>' || c == -1 || c == '\uFFFF') {
						break;
					}
					sequence.append(String.valueOf(c));
				}

				List<Object> record = new ArrayList<>();
				record.add(sequenceName);

				StringBuilder orf = new StringBuilder();
				int i = sequence.indexOf("ATG");
				Map<Integer, String> orfs = new HashMap<Integer, String>();
				while (i >= 0) {
					Iterable<String> codons = Splitter.fixedLength(3).split(sequence.substring(i));
					for (String codon : codons) {
						if (Arrays.asList("TAA", "TAG", "TGA").contains(codon)) {
							orf.append(codon);
							break;
						}
						orf.append(codon);
					}
//					if (orf.toString().endsWith("TAA") || orf.toString().endsWith("TAG")
//							|| orf.toString().endsWith("TGA")) {
						orfs.put(i, orf.toString());
//					}
					orf = new StringBuilder();
					i = sequence.indexOf("ATG", i + 1);
				}
				if (!orfs.isEmpty()) {

					Integer index = Collections.min(orfs.keySet());
					String orfSeq = orfs.get(index);

					record.add(orfSeq);
					record.add(index);
					record.add(orfSeq.length() + index);

					Map.Entry<Integer, String> max = Collections.max(orfs.entrySet(),
							new Comparator<Map.Entry<Integer, String>>() {
								@Override
								public int compare(Map.Entry<Integer, String> o1, Map.Entry<Integer, String> o2) {
									return o1.getValue().length() - o2.getValue().length();
								}
							});

					record.add(max.getValue());
					record.add(max.getKey());
					record.add(max.getValue().length() + max.getKey());
				} else {
					record.add("");
					record.add("");
					record.add("");
					record.add("");
					record.add("");
					record.add("");
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
}
