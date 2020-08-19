package org.uu.nl.embedding.kale.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Random;

import org.uu.nl.embedding.bca.util.BCV;
import org.uu.nl.embedding.util.InMemoryRdfGraph;

public class DataGenerator {
	
	
	private final InMemoryRdfGraph graph;
	private final int[][] inVerts;
	private final int[][] outVerts;
	private final int[][] inEdges;
	private final int[][] outEdges;
	
	private double validationPercentage;
	private double testingPercentage;
	private int[] trainSet, validSet, testSet;
	
	private boolean undirected;

	private String FILEPATH = "/Users/euan/eclipse-workspace/graphLogicEmbed/data/input";
	private String fileExtension;
	public String fnTrainingTriples;
	public String fnValidateTriples;
	public String fnTestingTriples;
	public String fnTrainingRules;
	public String fnGloveVectors;
	
	private String separator;
	
	public DataGenerator(final InMemoryRdfGraph graph, final boolean undirected,
							final int[][] inVerts, final int[][] outVerts,
							final int[][] inEdges, final int[][] outEdges) {
		this.graph = graph;
		this.inVerts = inVerts;
		this.outVerts = outVerts;
		this.inEdges = inEdges;
		this.outEdges = outEdges;
		
		this.undirected = undirected;
		
		this.validationPercentage = 0.2;
		this.testingPercentage = 0.3;
		
		this.separator = "\t";
	}
	
	public void Initialize() throws Exception {
		randomDataSeed();
		
		this.fileExtension = ".txt";
		this.fnTrainingTriples = generateFileDir("training_triples");
		this.fnValidateTriples = generateFileDir("validation_triples");
		this.fnTestingTriples = generateFileDir("testing_triples");
		this.fnTrainingRules = generateFileDir("training_rules");
		this.fnGloveVectors = generateFileDir("glove_vectors");
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private void randomDataSeed() throws Exception {
	    if ((this.validationPercentage + this.testingPercentage) > 0.9) 
	    	throw new Exception("Combination of validation and testing percentage of "
	    						+ "the dataset is too large: It may not exceed a total "
	    						+ "of 0.9 amount of the dataset.");
	    
	    Random rand = new Random();
	    int iNumVerts = this.outVerts.length;
	    ArrayList<Integer> takenInts = new ArrayList<Integer>();
	    
	    // Calculate number of elements per dataset.
	    int iNumValidElems = (int) (iNumVerts * this.validationPercentage);
	    int iNumTestElems = (int) (iNumVerts * this.testingPercentage);
	    int iNumTrainElems = iNumVerts - (iNumValidElems + iNumTestElems);
	    // Initialize datasets.
	    this.trainSet = new int[iNumTrainElems];
	    this.validSet = new int[iNumValidElems];
	    this.testSet = new int[iNumTestElems];
	    
	    // Fill training set with random vertices. Omitting
	    // duplicates.
	    int i = 0, randIdx;
	    while (i < iNumTrainElems) {
	    	randIdx = rand.nextInt(iNumVerts);
	    	if (!takenInts.contains(randIdx)) {
	    		this.trainSet[i] = randIdx;
	    		takenInts.add(randIdx);
	    		i++;
	    	}
	    }
	    // Fill validation set with random vertices. Omitting
	    // duplicates.
	    i = 0;
	    while (i < iNumValidElems) {
	    	randIdx = rand.nextInt(iNumVerts);
	    	if (!takenInts.contains(randIdx)) {
	    		this.validSet[i] = randIdx;
	    		takenInts.add(randIdx);
	    		i++;
	    	}
	    }
	    // Fill test set with remaining vertices. Omitting
	    // duplicates.
	    i = 0;
	    while (i < iNumTestElems) {
	    	for (int j = 0; j < iNumVerts; j++) {
	    		if (!takenInts.contains(j)) {
	    			this.testSet[i] = j;
	    			i++;
	    		}
	    	}
	    }
	    
	}
	
	/**
	 * Reads the graph and converts them to file specific format.
	 * @throws Exception
	 */
	public void generateTrainingTriples() throws Exception {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(this.fnTrainingTriples), "UTF-8"));
		
		String line = "";
		String sep = this.separator;
		for (int i = 0; i < this.trainSet.length; i++) {
			for (int j = 0; j < this.outVerts[this.trainSet[i]].length; j++) {
				line += Integer.toString(this.trainSet[i]) + sep;
				line += Integer.toString(this.outVerts[this.trainSet[i]][j]) + sep;
				line += Integer.toString(this.outEdges[this.trainSet[i]][j]) + " \n";
			}
			writer.write(line.trim());
		}
		if (this.undirected) {
			for (int i = 0; i < this.trainSet.length; i++) {
				for (int j = 0; j < this.inVerts[this.trainSet[i]].length; j++) {
					line += Integer.toString(this.trainSet[i]) + sep;
					line += Integer.toString(this.inVerts[this.trainSet[i]][j]) + sep;
					line += Integer.toString(this.inEdges[this.trainSet[i]][j]) + " \n";
				}
				writer.write(line.trim());
			}
		}
		
		writer.close();
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void generateValidateTriples() throws Exception {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(this.fnValidateTriples), "UTF-8"));
		
		String line = "";
		String sep = this.separator;
		for (int i = 0; i < this.validSet.length; i++) {
			for (int j = 0; j < this.outVerts[this.validSet[i]].length; j++) {
				line += Integer.toString(this.validSet[i]) + sep;
				line += Integer.toString(this.outVerts[this.validSet[i]][j]) + sep;
				line += Integer.toString(this.outEdges[this.validSet[i]][j]) + " \n";
			}
			writer.write(line.trim());
		}
		if (this.undirected) {
			for (int i = 0; i < this.validSet.length; i++) {
				for (int j = 0; j < this.inVerts[this.validSet[i]].length; j++) {
					line += Integer.toString(this.validSet[i]) + sep;
					line += Integer.toString(this.inVerts[this.validSet[i]][j]) + sep;
					line += Integer.toString(this.inEdges[this.validSet[i]][j]) + " \n";
				}
				writer.write(line.trim());
			}
		}
		
		writer.close();
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void generateTestingTriples() throws Exception {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(this.fnTestingTriples), "UTF-8"));
		
		String line = "";
		String sep = this.separator;
		for (int i = 0; i < this.testSet.length; i++) {
			for (int j = 0; j < this.outVerts[this.testSet[i]].length; j++) {
				line += Integer.toString(this.testSet[i]) + sep;
				line += Integer.toString(this.outVerts[this.testSet[i]][j]) + sep;
				line += Integer.toString(this.outEdges[this.testSet[i]][j]) + " \n";
			}
			writer.write(line.trim());
		}
		if (this.undirected) {
			for (int i = 0; i < this.testSet.length; i++) {
				for (int j = 0; j < this.inVerts[this.testSet[i]].length; j++) {
					line += Integer.toString(this.testSet[i]) + sep;
					line += Integer.toString(this.inVerts[this.testSet[i]][j]) + sep;
					line += Integer.toString(this.inEdges[this.testSet[i]][j]) + " \n";
				}
				writer.write(line.trim());
			}
		}
		
		writer.close();
	}
	
	public void generateTrainingRules() throws Exception {
		// Create list with rules.
		ArrayList<String> rulesList = new ArrayList<String>();
		
		rulesList.add("_birthdate(x,y)" + this.separator + "&" + this.separator + "_deathdate(x,z)" + this.separator + "==>" + this.separator + "_is_same_or_before(y,z)");
		rulesList.add("_birthdate(x,y)" + this.separator + "==>" + this.separator + "_deathdate(x,z)" + this.separator + "&" + this.separator + "_is_same_or_before(y,z)");
		rulesList.add("!" + this.separator + "_is_same_or_before(y,z)" + this.separator + "==>" + this.separator + 
				"!" + this.separator + "_birthdate(x,y)" + this.separator + 
				"|" + this.separator + "!" + this.separator + "_deathdate(x,z)");
		
		rulesList.add("_birthdate(x,y)" + this.separator + "&" + this.separator + "_baptised_on(x,z)" + this.separator + "==>" + this.separator + "_is_same_or_before(y,z)");
		rulesList.add("_birthdate(x,y)" + this.separator + "==>" + this.separator + "_baptised_on(x,z)" + this.separator + "&" + this.separator + "_is_same_or_before(y,z)");
		rulesList.add("!" + this.separator + "_is_same_or_before(y,z)" + this.separator + "==>" + this.separator + 
				"!" + this.separator + "_birthdate(x,y)" + this.separator + 
				"|" + this.separator + "!" + this.separator + "_baptised_on(x,z)");
		
		rulesList.add("_baptised_on(x,y)" + this.separator + "&" + this.separator + "_deathdate(x,z)" + this.separator + "==>" + this.separator + "_is_same_or_before(y,z)");
		rulesList.add("_baptised_on(x,y)" + this.separator + "==>" + this.separator + "_deathdate(x,z)" + this.separator + "&" + this.separator + "_is_same_or_before(y,z)");
		rulesList.add("!" + this.separator + "_is_same_or_before(y,z)" + this.separator + "==>" + this.separator + 
				"!" + this.separator + "_baptised_on(x,y)" + this.separator + 
				"|" + this.separator + "!" + this.separator + "_deathdate(x,z)");
		
		// Write all rules to file and close it.
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(this.fnTrainingRules), "UTF-8"));
		for (int i = 0; i < rulesList.size(); i++) writer.write(rulesList.get(i).trim());

		writer.close();
	}
	
	/**
	 * 
	 * @param bcvs
	 * @throws Exception
	 */
	public void generateGloveVectors(final BCV[] bcvs) throws Exception {
		/*
		 * FILE FORMAT:
		 * 
		 * line1 <- [NEIGHBORS\t nodeID1\t neighborID1\t neighborID2\t ...\t neighborIDn]
		 * line2 <- [VALUES\t nodeID1\t value1\t value2\t ...\t value_n]
		 * ...
		 */
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(this.fnGloveVectors), "UTF-8"));
		
		String line1, line2;
		for (int i = 0; i < bcvs.length; i++) {
			line1 = "[NEIGHBORS" + this.separator + bcvs[i].getRootNode();
			line2 = "[VALUES" + this.separator + bcvs[i].getRootNode();
			for (int j = 0; j < bcvs[i].size(); j++) {
				line1 += this.separator + j;
				line2 += this.separator + bcvs[i].get(j);
			}
			// Finish the lines.
			line1 += "]";
			line2 += "]";
			// Write lines to file.
			writer.write(line1.trim());
			writer.write(line2.trim());
		}
		writer.close();
	}
	
	/**
	 * 
	 * @param bcvs
	 * @param singleLines
	 * @throws Exception
	 */
	public void generateGloveVectors(final BCV[] bcvs, final boolean singleLines) throws Exception {
		/*
		 * FILE FORMAT:
		 * 
		 * line <- "nodeID1\t neighborID1: value1\t neighborID2: value2\t ...\t neighborIDn: valueN"
		 * ...
		 */
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(this.fnGloveVectors), "UTF-8"));
		
		String line = "";
		for (int i = 0; i < bcvs.length; i++) {
			// Write line to file.
			line = bcvs[i].toString();
			writer.write(line.trim());
		}
		writer.close();
	}
	
	/**
	 * Generates file directory without overwriting existing
	 * files by iterating through numbers at start of file.
	 * 
	 * @param fnDirName
	 * @return
	 */
	public String generateFileDir(final String fnDirName) {
		int num = 0;
		String fnDir = this.FILEPATH + "/" + num + "_" + fnDirName + "_KALE_GLOVE";
		File f = new File(fnDir);
		while (f.exists()) {
			num++;
			fnDir = this.FILEPATH + "/" + num + "_" + fnDirName + "_KALE_GLOVE";
			f = new File(fnDir);
		}
		return fnDir;
	}
	
	
	
	/*
	 * Below: getter and setter methods.
	 */
	
	/**
	 * 
	 * @return
	 */
	public int[] getTrainSet() {
		return this.trainSet;
	}
	
	/**
	 * 
	 * @return
	 */
	public int[] getValidationSet() {
		return this.validSet;
	}
	/**
	 * 
	 * @return
	 */
	public int[] getTestSet() {
		return this.testSet;
	}
	
	/**
	 * 
	 * @param isUndirected
	 */
	public void setUndirected(final boolean isUndirected) {
		this.undirected = isUndirected;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isUndirected() {
		return this.undirected;
	}
	
	/**
	 * 
	 * @param percentage
	 */
	public void setValidationPercentage(final double percentage) throws Exception {
		this.validationPercentage = percentage;
		randomDataSeed();
	}
	
	/**
	 * 
	 * @return
	 */
	public double getValidationPercentage() {
		return this.validationPercentage;
	}
	
	/**
	 * 
	 * @param percentage
	 */
	public void setTestingPercentage(final double percentage) throws Exception {
		this.testingPercentage = percentage;
		randomDataSeed();
	}
	
	/**
	 * 
	 * @return
	 */
	public double getTestingPercentage() {
		return this.testingPercentage;
	}
	
	/**
	 * 
	 * @param separator
	 */
	public void setSeparator(final String separator) {
		this.separator = separator;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getSeparator() {
		return this.separator;
	}
	
	/**
	 * 
	 * @param filePath
	 * @throws Exception
	 */
	public void setFilePath(final String filePath) throws Exception {

		File f = new File(filePath);
		// Simple check
		if (!f.exists()) { 
			throw new Exception("Provided directory in DataGenerator does not exist.");
		}
		this.FILEPATH = filePath;

		this.fnTrainingTriples = generateFileDir("training_triples");
		this.fnValidateTriples = generateFileDir("validation_triples");
		this.fnTestingTriples = generateFileDir("testing_triples");
		this.fnTrainingRules = generateFileDir("training_rules");
		this.fnGloveVectors = generateFileDir("glove_vectors");
	}
	
	/**
	 * 
	 * @return
	 */
	public String getFilePath() {
		return this.FILEPATH;
	}
	
	/**
	 * 
	 * @param fileExtension
	 * @throws Exception
	 */
	public void setFileExtension(final String fileExtension) throws Exception {
		// Simple check
		if (fileExtension.charAt(0) != '.') { 
			throw new Exception("Provided file extension in KaleRunner has invalid format.");
		}
		this.fileExtension = fileExtension;

		this.fnTrainingTriples = generateFileDir("training_triples");
		this.fnValidateTriples = generateFileDir("validation_triples");
		this.fnTestingTriples = generateFileDir("testing_triples");
		this.fnTrainingRules = generateFileDir("training_rules");
		this.fnGloveVectors = generateFileDir("glove_vectors");
	}
	
	/**
	 * 
	 * @return
	 */
	public String getFileExtension() {
		return this.fileExtension;
	}

}
