package br.unb.hugowschneider.FastaUtilities;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import br.unb.hugowschneider.FastaUtilities.NucleotidePatternCounter.CountType;
import br.unb.hugowschneider.FastaUtilities.NucleotidePatternCounter.OutputType;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		Options options = new Options();

		Option fastaOption = Option.builder("f").numberOfArgs(1).argName("fasta file").desc("Fasta file")
				.longOpt("fasta").required(true).build();
		Option countOption = Option.builder("c").numberOfArgs(2).valueSeparator(',').argName("min,max")
				.desc("Count nucleotide patterns of fasta seguence with minimum and maximum pattern sizes").build();
		Option csvOption = Option.builder().desc("Output type CSV").longOpt("csv").build();
		Option helpOption = Option.builder("h").desc("Print this help").longOpt("help").build();

		OptionGroup group = new OptionGroup();
		group.setRequired(true);
		group.addOption(Option.builder("n").desc("Nucleotide fasta file").build());
		group.addOption(Option.builder("a").desc("Amino acids fasta file").build());

		OptionGroup groupCount = new OptionGroup();
		groupCount.addOption(Option.builder().desc("Nucleotide fasta file").longOpt("percent").build());
		groupCount.addOption(Option.builder().desc("Amino acids fasta file").longOpt("abs").build());

		options.addOption(fastaOption);
		options.addOption(countOption);
		options.addOption(csvOption);
		options.addOption(helpOption);
		options.addOptionGroup(group);
		options.addOptionGroup(groupCount);

		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("c")) {

				File file = new File(cmd.getOptionValue("f"));
				if (!file.exists()) {
					usage("File does not exists", options);
					return;
				}
				String[] minMax = cmd.getOptionValues("c");
				try {
					Integer min = Integer.parseInt(minMax[0]);
					Integer max = Integer.parseInt(minMax[1]);
					NucleotidePatternCounter counter = new NucleotidePatternCounter(file, min, max);
					counter.count(cmd.hasOption("a") ? CountType.AMINO_ACID : CountType.NUCLEOTIDE, OutputType.CSV,
							System.out, cmd.hasOption("percent"));
				} catch (NumberFormatException | java.text.ParseException | IOException e) {
					e.printStackTrace();
					usage(e.getMessage(), options);
				}
			}
		} catch (ParseException e) {
			usage(e.getMessage(), options);
		}

	}

	private static void usage(String text, Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(text, options);
	}
}
