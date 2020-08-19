package org.uu.nl.embedding.kale.model;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.uu.nl.embedding.kale.struct.RuleSet;
import org.uu.nl.embedding.kale.struct.Triple;
import org.uu.nl.embedding.kale.struct.KaleMatrix;
import org.uu.nl.embedding.kale.struct.TripleRule;
import org.uu.nl.embedding.kale.struct.TripleSet;
import org.uu.nl.embedding.kale.util.MetricMonitor;
import org.uu.nl.embedding.kale.util.NegativeRuleGenerator;
import org.uu.nl.embedding.kale.util.NegativeTripleGenerator;
import org.uu.nl.embedding.kale.util.StringSplitter;
import org.uu.nl.embedding.util.InMemoryRdfGraph;
import org.uu.nl.embedding.util.config.Configuration;


/**
 * Class imported from iieir-km / KALE on GitHub
 * (https://github.com/iieir-km/KALE/tree/817474edb0da54a76b562bed2328e96284557b87)
 *
 */
public class KaleModel {
	public TripleSet m_TrainingTriples;
	public TripleSet m_ValidateTriples;
	public TripleSet m_TestingTriples;
	public TripleSet m_Triples;
	public RuleSet m_TrainingRules;
	
	public KaleMatrix m_Entity_Factor_MatrixE;
	public KaleMatrix m_Relation_Factor_MatrixR;
	public KaleMatrix m_MatrixEGradient;
	public KaleMatrix m_MatrixRGradient;
	
	public KaleVectorMatrix kaleVectorMatrix;
	private InMemoryRdfGraph graph;
	private Configuration config;
	
	public int m_NumRelation;
	public int m_NumEntity;
	public int m_NumGloveVecs;
	public String m_MatrixE_prefix = "";
	public String m_MatrixR_prefix = "";
	
	public int m_NumFactor = 20;
	public int m_NumMiniBatch = 100;
	public double m_Delta = 0.1;
	public double m_GammaE = 0.01;
	public double m_GammaR = 0.01;
	public int m_NumIteration = 1000;
	public int m_OutputIterSkip = 50;
	public double m_Weight = 0.01;
	
	public boolean isGlove;
	private ArrayList<Integer> orderedCoOccurrenceIdx_I = null;
	private ArrayList<Integer> coOccurrenceIdx_I = null;
	private ArrayList<Integer> coOccurrenceIdx_J = null;
	private ArrayList<Float> coOccurrenceValues = null;
	private int iFirstEdge;
	
	/**
	 * 
	 */
	public String fileExtension = ".txt";
	
	java.text.DecimalFormat decimalFormat = new java.text.DecimalFormat("#.######");
	
	public KaleModel() {
	}
	
	/**
	 * 
	 * @param iNumRelation
	 * @param iNumEntity
	 * @param fnTrainingTriples
	 * @param fnValidateTriples
	 * @param fnTestingTriples
	 * @param fnTrainingRules
	 * @throws Exception
	 * @author iieir-km, Euan Westenbroek
	 */
	public void Initialization(final int iNumRelation, final int iNumEntity,
			final String fnTrainingTriples, final String fnValidateTriples, final String fnTestingTriples,
			final String fnTrainingRules, final String fnGloveVectors, 
			final InMemoryRdfGraph graph, final Configuration config) throws Exception {
		
		this.m_NumRelation = iNumRelation;
		this.m_NumEntity = iNumEntity;
		this.m_NumGloveVecs = iNumRelation + iNumEntity;
		
		if (fnGloveVectors != null) this.isGlove = true;
		else this.isGlove = false;
		
		this.m_MatrixE_prefix = "MatrixE-k" + this.m_NumFactor 
				+ "-d" + decimalFormat.format(m_Delta)
				+ "-ge" + decimalFormat.format(m_GammaE) 
				+ "-gr" + decimalFormat.format(m_GammaR)
				+ "-w" +  decimalFormat.format(m_Weight);
		this.m_MatrixR_prefix = "MatrixR-k" + this.m_NumFactor 
				+ "-d" + decimalFormat.format(m_Delta)
				+ "-ge" + decimalFormat.format(m_GammaE) 
				+ "-gr" + decimalFormat.format(m_GammaR)
				+ "-w" +  decimalFormat.format(m_Weight);
		
		System.out.println("\nLoading training and validate triples");
		this.m_TrainingTriples = new TripleSet(this.m_NumEntity, this.m_NumRelation);
		this.m_ValidateTriples = new TripleSet(this.m_NumEntity, this.m_NumRelation);
		this.m_Triples = new TripleSet();
		this.m_TrainingTriples.load(fnTrainingTriples);
		this.m_ValidateTriples.subload(fnValidateTriples);
		this.m_Triples.loadStr(fnTrainingTriples);
		this.m_Triples.loadStr(fnValidateTriples);
		this.m_Triples.loadStr(fnTestingTriples);
		System.out.println("Success.");
		
		System.out.println("\nLoading grounding rules");
		this.m_TrainingRules = new RuleSet(this.m_NumEntity, this.m_NumRelation);
		this.m_TrainingRules.loadTimeLogic(fnTrainingRules);
		System.out.println("Success.");		
		
		System.out.println("\nRandomly initializing matrix E and matrix R");
		this.m_Entity_Factor_MatrixE = new KaleMatrix(this.m_NumEntity, this.m_NumFactor);
		this.m_Relation_Factor_MatrixR = new KaleMatrix(this.m_NumRelation, this.m_NumFactor);
		if (this.isGlove)  {
			loadGloveVectors(fnGloveVectors);
			this.m_Entity_Factor_MatrixE = loadGloveEntityVectors();
			this.m_Relation_Factor_MatrixR = loadGloveRelationVectors();
			/*
			 *
			this.m_Entity_Factor_MatrixE.normalizeByRow();
			this.m_Relation_Factor_MatrixR.normalizeByRow(); 
			 */
		}
		else { 
			this.m_Entity_Factor_MatrixE.setToRandom();
			this.m_Relation_Factor_MatrixR.setToRandom();
			this.m_Entity_Factor_MatrixE.normalizeByRow();
			this.m_Relation_Factor_MatrixR.normalizeByRow();
		}
		System.out.println("Success.");
		
		System.out.println("\nInitializing gradients of matrix E and matrix R");
		this.m_MatrixEGradient = new KaleMatrix(this.m_NumEntity, this.m_NumFactor);
		this.m_MatrixRGradient = new KaleMatrix(this.m_NumRelation, this.m_NumFactor);
		System.out.println("Success.");
	}
	
	public void Initialization(String strNumRelation, String strNumEntity,
			String fnTrainingTriples, String fnValidateTriples, String fnTestingTriples,
			String fnTrainingRules, final String fnGloveVectors, 
			final InMemoryRdfGraph graph, final Configuration config) throws Exception {
		
		m_NumRelation = Integer.parseInt(strNumRelation);
		m_NumEntity = Integer.parseInt(strNumEntity);
		
		Initialization(m_NumRelation, 
				m_NumEntity,
				fnTrainingTriples, 
				fnValidateTriples, 
				fnTestingTriples, 
				fnTrainingRules,
				fnGloveVectors,
				graph,
				config); 
	}
	
	/**
	 * 
	 * @param fnGloveVectors
	 * @throws Exception
	 * @author Euan Westenbroek
	 */
	public void loadGloveVectors(final String fnGloveVectors) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(fnGloveVectors), "UTF-8"));
		// Initialize matrix.
		this.coOccurrenceIdx_I = new ArrayList<Integer>();
		this.coOccurrenceIdx_J = new ArrayList<Integer>();
		this.coOccurrenceValues = new ArrayList<Float>();
		
		/*
		 * FILE FORMAT:
		 * 
		 * line1 <- [NEIGHBORS nodeID1 neighborID1 neighborID2 ... neighborIDn]
		 * line2 <- [VALUES nodeID1 value1 value2 ... value_n]
		 * ...
		 */
		
		String line = "";
		int nodeID, neighborID;
		int vecCounter = 0;
		float value;
		boolean neighborsLine = true;
		while ((line = reader.readLine()) != null) {
			String[] tokens = StringSplitter.RemoveEmptyEntries(StringSplitter
					.split("\t ", line));

			if (tokens[0] == "NEIGHBORS") {
				// Check if correct line order is maintained.
				if (!neighborsLine) throw new Exception("Loading error in KaleModel.loadGloveVectors(): neighborsLine expected.");
				// Parse nodeID and check.
				nodeID = Integer.parseInt(tokens[1]);
				if (nodeID < 0 || nodeID >= this.m_NumGloveVecs) {
					throw new Exception("Loading error in KaleModel.loadGloveVectors(): invalid nodeID.");
				}
				orderedCoOccurrenceIdx_I.add(nodeID);
				// Add current nodeID and each neighbor to matrix.
				for (int col = 2; col < tokens.length; col++) {
					neighborID = Integer.parseInt(tokens[col]);
					if (neighborID < 0 || neighborID >= this.m_NumGloveVecs) {
						throw new Exception("Loading error in KaleModel.loadGloveVectors(): invalid neighborID.");
					}
					this.coOccurrenceIdx_I.add(nodeID);
					this.coOccurrenceIdx_J.add(neighborID);
				}
				// Update expected line type.
				neighborsLine = false;
				
			} else if (tokens[0] == "VALUES") {
				// Check if correct line order is maintained.
				if (neighborsLine) throw new Exception("Loading error in KaleModel.loadGloveVectors(): values line expected (i.e. not neighborsLine expected).");
				// Parse nodeID and check.
				nodeID = Integer.parseInt(tokens[1]);
				if (nodeID != this.coOccurrenceIdx_I.get(this.coOccurrenceIdx_I.size()-1)) {
					throw new Exception("Loading error in KaleModel.loadGloveVectors(): current nodeID does not match expected nodeID.");
				}
				if (nodeID < 0 || nodeID >= this.m_NumGloveVecs) {
					throw new Exception("Loading error in KaleModel.loadGloveVectors(): invalid nodeID.");
				}
				// Add current nodeID and each neighbor to matrix.
				for (int col = 2; col < tokens.length; col++) {
					value = Float.parseFloat(tokens[col]);
					this.coOccurrenceValues.add(value);
				}
				// Update expected line type and increment counter.
				neighborsLine = true;
				vecCounter++;
				
			} else { throw new Exception("Loading error in KaleModel.loadGloveVectors(): invalid row header."); }
		}
		// Check if number of vectors adds up.
		if (vecCounter != this.m_NumGloveVecs) {
			throw new Exception("Loading error in KaleModel.loadGloveVectors(): vecCounter does not match expected number of vectors.");
		}
		
		reader.close();
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 * @author Euan Westenbroek
	 */
	public KaleMatrix loadGloveEntityVectors() throws Exception {
		KaleMatrix kMatrix = new KaleMatrix(this.m_NumEntity, this.m_NumFactor);
		
		int nodeID, neighborID, v = 0, nodeCntr = 0;
		float value;
		// Get matrix values and add to KaleMatrix.
		while (nodeCntr < this.m_NumEntity) {
			nodeID = this.coOccurrenceIdx_I.get(v);
			neighborID = this.coOccurrenceIdx_J.get(v);
			value = this.coOccurrenceValues.get(v);
			
			kMatrix.add(nodeCntr, neighborID, value);
			
			v++;
			// If next node is different than current node:
			// increment node counter.
			if (this.coOccurrenceIdx_I.get(v) != nodeID) nodeCntr++;
		}
		this.iFirstEdge = v;
		return kMatrix;
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 * @author Euan Westenbroek
	 */
	public KaleMatrix loadGloveRelationVectors() throws Exception {
		KaleMatrix kMatrix = new KaleMatrix(this.m_NumEntity, this.m_NumFactor);
		
		int edgeID, neighborID, e = this.iFirstEdge, edgeCntr = 0;
		float value;
		// Get matrix values and add to KaleMatrix.
		while (edgeCntr < this.m_NumRelation) {
			edgeID = this.coOccurrenceIdx_I.get(e);
			neighborID = this.coOccurrenceIdx_J.get(e);
			value = this.coOccurrenceValues.get(e);
			
			kMatrix.add(edgeCntr, neighborID, value);
			
			e++;
			// If next node is different than current node:
			// increment node counter.
			if (!this.coOccurrenceIdx_I.contains(e) || (this.coOccurrenceIdx_I.get(e) != edgeID)) edgeCntr++;
		}
		return kMatrix;
	}
	
	/**
	 * 
	 * @throws Exception
	 * @author Euan Westenbroek
	 */
	public void CochezLearn() throws Exception {
		if (orderedCoOccurrenceIdx_I == null) throw new Exception("Current instatiation of this KaleModel does not contain needed data.");
		
		HashMap<Integer, ArrayList<Triple>> lstPosTriples = new HashMap<Integer, ArrayList<Triple>>();
		HashMap<Integer, ArrayList<Triple>> lstHeadNegTriples = new HashMap<Integer, ArrayList<Triple>>();
		HashMap<Integer, ArrayList<Triple>> lstTailNegTriples = new HashMap<Integer, ArrayList<Triple>>();
		HashMap<Integer, ArrayList<TripleRule>> lstRules = new HashMap<Integer, ArrayList<TripleRule>>();
		HashMap<Integer, ArrayList<TripleRule>> lstSndRelNegRules = new HashMap<Integer, ArrayList<TripleRule>>();
		
		KaleMatrix bestEntityVectors;
		KaleMatrix bestRelationVectors;
		
		// Generate logging file path.
		String PATHLOG = "result-k" + this.m_NumFactor 
				+ "-d" +  this.decimalFormat.format(this.m_Delta)
				+ "-ge" + this.decimalFormat.format(this.m_GammaE) 
				+ "-gr" + this.decimalFormat.format(this.m_GammaR)
				+ "-w" +  this.decimalFormat.format(this.m_Weight) + this.fileExtension;
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(PATHLOG), "UTF-8"));
		
		// Initialize iteration counter and write to file and console.
		int iIterCntr = 0;
		writer.write("Complete iteration #" + iIterCntr + ":\n");
		System.out.println("Complete iteration #" + iIterCntr + ":");
		// Initialize metric monitor and calculate metrics.
		MetricMonitor firstMetric = new MetricMonitor(
				this.m_ValidateTriples,
				this.m_Triples.tripleSet(),
				this.m_Entity_Factor_MatrixE,
				this.m_Relation_Factor_MatrixR,
				this.orderedCoOccurrenceIdx_I,
				this.isGlove);
		firstMetric.calculateMetrics();
		double dCurrentHits = firstMetric.dHits;
		double dCurrentMRR = firstMetric.dMRR;
		bestEntityVectors = this.m_Entity_Factor_MatrixE;
		bestRelationVectors = this.m_Relation_Factor_MatrixR;
		// Write first results to file and save as initial best results.
		writer.write("------Current MRR:"+ dCurrentMRR + "\tCurrent Hits@10:" + dCurrentHits + "\n");
		System.out.print("\n");
		double dBestHits = firstMetric.dHits;
		double dBestMRR = firstMetric.dMRR;
		// Variable to save the best iteration.
		int iBestIter = 0;
		
		// Initialize start of training time and start training process.
		long startTime = System.currentTimeMillis();
		while (iIterCntr < this.m_NumIteration) {
			
			// Loop through training triples and generate negative versions (alterations).
			this.m_TrainingTriples.randomShuffle();
			for (int iTriple = 0; iTriple < this.m_TrainingTriples.nTriples(); iTriple++) {
				// Get triple and generate negative alterations.
				Triple PosTriple = this.m_TrainingTriples.get(iTriple);
				NegativeTripleGenerator negTripGen = new NegativeTripleGenerator(
						PosTriple, this.m_NumEntity, this.m_NumRelation);
				Triple headNegTriple = negTripGen.generateHeadNegTriple();
				Triple tailNegTriple = negTripGen.generateTailNegTriple();
				
				// Determine triple ID within batch.
				int iTripleID = iTriple % this.m_NumMiniBatch;
				// If positive triples list doesn't contain current triple,
				// add new triple to triples lists. Else add newly found triple
				// and its alterations to their respective lists.
				if (!lstPosTriples.containsKey(iTripleID)) {
					ArrayList<Triple> tmpPosLst = new ArrayList<Triple>();
					ArrayList<Triple> tmpHeadNegLst = new ArrayList<Triple>();
					ArrayList<Triple> tmpTailNegLst = new ArrayList<Triple>();
					tmpPosLst.add(PosTriple);
					tmpHeadNegLst.add(headNegTriple);
					tmpTailNegLst.add(tailNegTriple);
					lstPosTriples.put(iTripleID, tmpPosLst);
					lstHeadNegTriples.put(iTripleID, tmpHeadNegLst);
					lstTailNegTriples.put(iTripleID, tmpTailNegLst);
				} else {
					lstPosTriples.get(iTripleID).add(PosTriple);
					lstHeadNegTriples.get(iTripleID).add(headNegTriple);
					lstTailNegTriples.get(iTripleID).add(tailNegTriple);
				}
			}
			
			// Repeat above process for training rules.
			// Loop through training rules and generate negative versions (alterations).
			this.m_TrainingRules.randomShuffle();
			for (int iRule = 0; iRule < this.m_TrainingRules.rules(); iRule++) {
				// Get triple and generate negative alterations.
				TripleRule rule = this.m_TrainingRules.get(iRule);
				NegativeRuleGenerator negRuleGen = new NegativeRuleGenerator(
						rule, this.m_NumRelation);
				TripleRule sndRelNegrule = negRuleGen.generateSndNegRule();			

				// Determine triple ID within batch.
				int iRuleID = iRule % this.m_NumMiniBatch;
				// If positive rules list doesn't contain current rule,
				// add new rule to rules lists. Else add newly found rule
				// and its alterations to their respective lists.
				if (!lstRules.containsKey(iRuleID)) {
					ArrayList<TripleRule> tmpLst = new ArrayList<TripleRule>();
					ArrayList<TripleRule> tmpsndRelNegLst = new ArrayList<TripleRule>();
					tmpLst.add(rule);
					tmpsndRelNegLst.add(sndRelNegrule);
					lstRules.put(iRuleID, tmpLst);
					lstSndRelNegRules.put(iRuleID, tmpsndRelNegLst);
					
				} else {
					lstRules.get(iRuleID).add(rule);
					lstSndRelNegRules.get(iRuleID).add(sndRelNegrule);
				}
			}
			
			// Update stochastically.
			for (int iID = 0; iID < this.m_NumMiniBatch; iID++) {
				StochasticUpdater stochasticUpdate = new StochasticUpdater(
						lstPosTriples.get(iID),
						lstHeadNegTriples.get(iID),
						lstTailNegTriples.get(iID),
						lstRules.get(iID),
						lstSndRelNegRules.get(iID),
						this.m_Entity_Factor_MatrixE,
						this.m_Relation_Factor_MatrixR,
						this.m_MatrixEGradient,
						this.m_MatrixRGradient,
//	###					this.learning rate
						this.m_GammaE,
						this.m_GammaR,
//	###					this.margin
						this.m_Delta,
//	###					this.weight
						this.m_Weight,
						this.isGlove);
				stochasticUpdate.stochasticIterationGlove();
			}
			
			// Reset lists for next iteration.
			lstPosTriples = new HashMap<Integer, ArrayList<Triple>>();
			lstHeadNegTriples = new HashMap<Integer, ArrayList<Triple>>();
			lstTailNegTriples = new HashMap<Integer, ArrayList<Triple>>();

			lstRules = new HashMap<Integer, ArrayList<TripleRule>>();
			lstSndRelNegRules = new HashMap<Integer, ArrayList<TripleRule>>();
			
			// Increment iteration counter and print to console.
			iIterCntr++;
			System.out.println("Complete iteration #" + iIterCntr + ":");
			
			// If current iteration is an nth-fold of this.m_OutputIterSkip
			// write current iteration results to file.
			if (iIterCntr % this.m_OutputIterSkip == 0) {
				writer.write("Complete iteration #" + iIterCntr + ":\n");
				System.out.println("Complete iteration #" + iIterCntr + ":");
				MetricMonitor metric = new MetricMonitor(
						this.m_ValidateTriples,
						this.m_Triples.tripleSet(),
						this.m_Entity_Factor_MatrixE,
						this.m_Relation_Factor_MatrixR,
						this.orderedCoOccurrenceIdx_I,
						this.isGlove);
				metric.calculateMetrics();
				dCurrentHits = metric.dHits;
				dCurrentMRR = metric.dMRR;
				writer.write("------Current MRR:"+ dCurrentMRR + "\tCurrent Hits@10:" + dCurrentHits + "\n");
				// Save current statistics if better than previous best results.
				if (dCurrentMRR > dBestMRR) {
					this.m_Relation_Factor_MatrixR.output(this.m_MatrixR_prefix + ".best");
					this.m_Entity_Factor_MatrixE.output(this.m_MatrixE_prefix + ".best");
					bestEntityVectors = this.m_Entity_Factor_MatrixE;
					bestRelationVectors = this.m_Relation_Factor_MatrixR;
					dBestHits = dCurrentHits;
					dBestMRR = dCurrentMRR;
					iBestIter = iIterCntr;
				}
				writer.write("------Best iteration #" + iBestIter + "\t" + dBestMRR + "\t" + dBestHits+"\n");
				writer.flush();
				System.out.println("------\tBest iteration #" + iBestIter + "\tBest MRR:" + dBestMRR + "Best \tHits@10:" + dBestHits);
				writer.flush();
			}
		}
		this.kaleVectorMatrix = new KaleVectorMatrix(this.graph, this.config, bestEntityVectors, bestRelationVectors);
		// Print end of training time to console and close writer.
		long endTime = System.currentTimeMillis();
		System.out.println("All running time:" + (endTime-startTime)+"ms");
		writer.close();
		
	}
	
	public void TransE_Learn() throws Exception {
		HashMap<Integer, ArrayList<Triple>> lstPosTriples = new HashMap<Integer, ArrayList<Triple>>();
		HashMap<Integer, ArrayList<Triple>> lstHeadNegTriples = new HashMap<Integer, ArrayList<Triple>>();
		HashMap<Integer, ArrayList<Triple>> lstTailNegTriples = new HashMap<Integer, ArrayList<Triple>>();
		HashMap<Integer, ArrayList<TripleRule>> lstRules = new HashMap<Integer, ArrayList<TripleRule>>();
		HashMap<Integer, ArrayList<TripleRule>> lstSndRelNegRules = new HashMap<Integer, ArrayList<TripleRule>>();
		
		
		String PATHLOG = "result-k" + m_NumFactor 
				+ "-d" + decimalFormat.format(m_Delta)
				+ "-ge" + decimalFormat.format(m_GammaE) 
				+ "-gr" + decimalFormat.format(m_GammaR)
				+ "-w" +  decimalFormat.format(m_Weight) + this.fileExtension;
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(PATHLOG), "UTF-8"));
		
		int iIter = 0;
		writer.write("Complete iteration #" + iIter + ":\n");
		System.out.println("Complete iteration #" + iIter + ":");
		MetricMonitor first_metrics = new MetricMonitor(
				m_ValidateTriples,
				m_Triples.tripleSet(),
				m_Entity_Factor_MatrixE,
				m_Relation_Factor_MatrixR,
				this.isGlove);
		first_metrics.calculateMetrics();
		double dCurrentHits = first_metrics.dHits;
		double dCurrentMRR = first_metrics.dMRR;
		writer.write("------Current MRR:"+ dCurrentMRR + "\tCurrent Hits@10:" + dCurrentHits + "\n");
		System.out.print("\n");
		double dBestHits = first_metrics.dHits;
		double dBestMRR = first_metrics.dMRR;
		int iBestIter = 0;
		
		
		long startTime = System.currentTimeMillis();
		while (iIter < m_NumIteration) {
			m_TrainingTriples.randomShuffle();
			for (int iIndex = 0; iIndex < m_TrainingTriples.nTriples(); iIndex++) {
				Triple PosTriple = m_TrainingTriples.get(iIndex);
				NegativeTripleGenerator negTripGen = new NegativeTripleGenerator(
						PosTriple, m_NumEntity, m_NumRelation);
				Triple headNegTriple = negTripGen.generateHeadNegTriple();
				Triple tailNegTriple = negTripGen.generateTailNegTriple();
				
				int iID = iIndex % m_NumMiniBatch;
				if (!lstPosTriples.containsKey(iID)) {
					ArrayList<Triple> tmpPosLst = new ArrayList<Triple>();
					ArrayList<Triple> tmpHeadNegLst = new ArrayList<Triple>();
					ArrayList<Triple> tmpTailNegLst = new ArrayList<Triple>();
					tmpPosLst.add(PosTriple);
					tmpHeadNegLst.add(headNegTriple);
					tmpTailNegLst.add(tailNegTriple);
					lstPosTriples.put(iID, tmpPosLst);
					lstHeadNegTriples.put(iID, tmpHeadNegLst);
					lstTailNegTriples.put(iID, tmpTailNegLst);
				} else {
					lstPosTriples.get(iID).add(PosTriple);
					lstHeadNegTriples.get(iID).add(headNegTriple);
					lstTailNegTriples.get(iID).add(tailNegTriple);
				}
			}
			
			m_TrainingRules.randomShuffle();
			for (int iIndex = 0; iIndex < m_TrainingRules.rules(); iIndex++) {
				TripleRule rule = m_TrainingRules.get(iIndex);
				
				NegativeRuleGenerator negRuleGen = new NegativeRuleGenerator(
						rule,  m_NumRelation);
				TripleRule sndRelNegrule = negRuleGen.generateSndNegRule();			

				int iID = iIndex % m_NumMiniBatch;
				if (!lstRules.containsKey(iID)) {
					ArrayList<TripleRule> tmpLst = new ArrayList<TripleRule>();
					ArrayList<TripleRule> tmpsndRelNegLst = new ArrayList<TripleRule>();
					tmpLst.add(rule);
					tmpsndRelNegLst.add(sndRelNegrule);
					lstRules.put(iID, tmpLst);
					lstSndRelNegRules.put(iID, tmpsndRelNegLst);
					
				} else {
					lstRules.get(iID).add(rule);
					lstSndRelNegRules.get(iID).add(sndRelNegrule);
				}
			}
			
			double m_BatchSize = m_TrainingTriples.nTriples()/(double)m_NumMiniBatch;
			for (int iID = 0; iID < m_NumMiniBatch; iID++) {
				StochasticUpdater stochasticUpdate = new StochasticUpdater(
						lstPosTriples.get(iID),
						lstHeadNegTriples.get(iID),
						lstTailNegTriples.get(iID),
						lstRules.get(iID),
						lstSndRelNegRules.get(iID),
						m_Entity_Factor_MatrixE,
						m_Relation_Factor_MatrixR,
						m_MatrixEGradient,
						m_MatrixRGradient,
//	###					learning rate
						m_GammaE,
						m_GammaR,
//	###					margin
						m_Delta,
//	###					weight
						m_Weight,
						this.isGlove);
				stochasticUpdate.stochasticIteration();
			}

			
			lstPosTriples = new HashMap<Integer, ArrayList<Triple>>();
			lstHeadNegTriples = new HashMap<Integer, ArrayList<Triple>>();
			lstTailNegTriples = new HashMap<Integer, ArrayList<Triple>>();

			lstRules = new HashMap<Integer, ArrayList<TripleRule>>();
			lstSndRelNegRules = new HashMap<Integer, ArrayList<TripleRule>>();
			
			iIter++;
			System.out.println("Complete iteration #" + iIter + ":");
			
			if (iIter % m_OutputIterSkip == 0) {
				writer.write("Complete iteration #" + iIter + ":\n");
				System.out.println("Complete iteration #" + iIter + ":");
				MetricMonitor metric = new MetricMonitor(
						m_ValidateTriples,
						m_Triples.tripleSet(),
						m_Entity_Factor_MatrixE,
						m_Relation_Factor_MatrixR,
						this.isGlove);
				metric.calculateMetrics();
				dCurrentHits = metric.dHits;
				dCurrentMRR = metric.dMRR;
				writer.write("------Current MRR:"+ dCurrentMRR + "\tCurrent Hits@10:" + dCurrentHits + "\n");
				if (dCurrentMRR > dBestMRR) {
					m_Relation_Factor_MatrixR.output(m_MatrixR_prefix + ".best");
					m_Entity_Factor_MatrixE.output(m_MatrixE_prefix + ".best");
					dBestHits = dCurrentHits;
					dBestMRR = dCurrentMRR;
					iBestIter = iIter;
				}
				writer.write("------Best iteration #" + iBestIter + "\t" + dBestMRR + "\t" + dBestHits+"\n");
				writer.flush();
				System.out.println("------\tBest iteration #" + iBestIter + "\tBest MRR:" + dBestMRR + "Best \tHits@10:" + dBestHits);
				writer.flush();
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("All running time:" + (endTime-startTime)+"ms");
		writer.close();
	}
}