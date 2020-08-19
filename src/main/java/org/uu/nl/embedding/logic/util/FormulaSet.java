package org.uu.nl.embedding.logic.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.uu.nl.embedding.logic.LogicRule;

public class FormulaSet {
	
	private final TreeSet<String> ruleSet;
	private final Literal[] literals;
	private final ArrayList<String> strLiterals;
	
	public FormulaSet(final String line) throws Exception {
		this.ruleSet = new TreeSet<String>();
		
		this.strLiterals = new ArrayList<String>();
		String[] tokens = line.split("\t");
		ArrayList<String> nots = new ArrayList<String>();
		
		int counter = 0;
		boolean implication = false;
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i] == "==>") implication = true;
			
			if (!tokens[i].contains("!") && !implication) nots.add(tokens[i]); 
			else if (tokens[i].contains("!") && implication) nots.add(tokens[i]);
			
			if (!tokens[i].contains("!") && !tokens[i].contains("|") && !tokens[i].contains("&") && !tokens[i].contains("==>")) {
				this.strLiterals.add(counter, tokens[i]);
				counter++;
			}
		}
		
		if (counter > 3) throw new Exception("load error in FormulaSet: data format incorrect");
		
		
		this.literals = new Literal[this.strLiterals.size()];
		for (int i = 0; i < this.strLiterals.size(); i++) {
			this.literals[i] = new Literal(extractTerms(this.strLiterals.get(i)));
			
			if (this.strLiterals.get(i).contains("_birthdate") && this.literals[i].name == "_birthdate") {
				this.ruleSet.add(this.literals[i].name);
				
			} else if (this.strLiterals.get(i).contains("_deathdate") && this.literals[i].name == "_deathdate") {
				this.ruleSet.add(this.literals[i].name);
				
			} else if (this.strLiterals.get(i).contains("_baptised_on") && this.literals[i].name == "_baptised_on") {
				this.ruleSet.add(this.literals[i].name);
				
			} else if (this.strLiterals.get(i).contains("_is_same_or_before") && this.literals[i].name == "_is_same_or_before") {
				this.ruleSet.add(this.literals[i].name);
				
			}
		}
		
	}
	
	public String[] extractTerms(String literal) {
		String[] resTokens = literal.split("[(,)]");
		System.out.println(resTokens);
		return resTokens;
	}
	
	public String[][] getLiteralSet() {
		String[][] set = new String[this.literals.length][];
		String[] split = new String[5];
		for (int i = 0; i < this.literals.length; i++) {
			split[0] = this.literals[i].name;
			split[1] = this.literals[i].head;
			split[2] = this.literals[i].tail;
			split[3] = this.literals[i].headMeaning;
			split[4] = this.literals[i].tailMeaning;
			set[i] = split;
		}
		return set;
	}
	
	public String[] getTokens() {
		String[] tokens = new String[(this.literals.length * 3)];
		int counter = 0;
		for (Literal lit : this.literals) {
			tokens[counter++] = lit.head;
			tokens[counter++] = lit.name;
			tokens[counter++] = lit.tail;
		}
		return tokens;
	}
	
	public String[] getOrigArgs() {
		String[] tokens = new String[(this.literals.length * 3)];
		int counter = 0;
		for (Literal lit : this.literals) {
			tokens[counter++] = lit.head;
			tokens[counter++] = lit.name;
			tokens[counter++] = lit.tail;
		}
		return tokens;
	}
	
	/**
	 * Inner class: Literal.
	 * @author euan
	 *
	 */
	private class Literal {
		
		private final String[] origArgs;
		public String name;
		public String head;
		public String tail;
		public String headMeaning = "";
		public String tailMeaning = "";
		
		public Literal(final String[] args) {
			this.origArgs = args;
			this.name = args[0];
			this.head = args[1];
			this.tail = args[2];
		}
		
	}

}
