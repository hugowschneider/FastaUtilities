package br.unb.hugowschneider.FastaUtilities;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class Permute {

	private String alphabet = "ACGT";

	public Permute(String alphabet) {
		this.alphabet = alphabet;
	}

	public List<String> permute(int level, String prefix) {

		if (level == 0) {
			return Arrays.asList(prefix);
		}
		List<String> permutations = new Vector<String>();
		for (int i = 0; i < alphabet.length(); i++) {
			permutations.addAll(permute(level - 1, prefix + alphabet.charAt(i)));
		}
		return permutations;
	}

}