package org.uu.nl.embedding.kale.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.uu.nl.embedding.convert.util.InEdgeNeighborhoodAlgorithm;
import org.uu.nl.embedding.convert.util.NodeInfo;
import org.uu.nl.embedding.convert.util.OutEdgeNeighborhoodAlgorithm;
import org.uu.nl.embedding.kale.struct.KaleMatrix;
import org.uu.nl.embedding.util.CoOccurrenceMatrix;
import org.uu.nl.embedding.util.InMemoryRdfGraph;
import org.uu.nl.embedding.util.config.Configuration;
import org.uu.nl.embedding.util.rnd.Permutation;

public class KaleVectorMatrix implements CoOccurrenceMatrix {

	private ArrayList<Integer> coOccurrenceIdx_I;
	private ArrayList<Integer> coOccurrenceIdx_J;
	private ArrayList<Float> coOccurrenceValues;
	private KaleMatrix entityMatrix;
	private KaleMatrix relationMatrix;
	
	private Map<String, Double> bcvMaxVals;
	private final int vocabSize;
	private double max;
	private int focusVectors, contextVectors;
	private int coOccurrenceCount;
	private Permutation permutation;
	private final InMemoryRdfGraph graph;
	private final Configuration graphConfig;

	private final int[][] inVertex;
	private final int[][] outVertex;
	private final int[][] inEdge;
	private final int[][] outEdge;
	private final Map<Integer, Integer> context2focus;
	private final Map<Integer, Integer> focus2context;
	
	public KaleVectorMatrix(final InMemoryRdfGraph graph, final Configuration config, 
						final KaleMatrix entityMatrix, final KaleMatrix relationMatrix) throws Exception {

		final double alpha = config.getBca().getAlpha();
		final double epsilon = config.getBca().getEpsilon();
		final int[] verts = graph.getVertices().toIntArray();
		final boolean[] performBCA = new boolean[verts.length];

		this.graph = graph;
		this.vocabSize = verts.length;
		
		this.entityMatrix = entityMatrix;
		this.relationMatrix = relationMatrix;
		
		// Initialization standard co-occurrence matrix
		this.coOccurrenceIdx_I = new ArrayList<>();
		this.coOccurrenceIdx_J = new ArrayList<>();
		this.coOccurrenceValues = new ArrayList<>();
		readKaleMatrices();
		
		this.bcvMaxVals = new HashMap<String, Double>();
		final Configuration.Output output = config.getOutput();

		this.context2focus = new HashMap<>();
		this.focus2context = new HashMap<>();
		
		this.contextVectors = verts.length;

		this.graphConfig = config;

		this.inVertex = graph.getInNeighborhoods();
		this.outVertex = graph.getOutNeighborhoods();
		this.inEdge = new InEdgeNeighborhoodAlgorithm(config).compute(graph);
		this.outEdge = new OutEdgeNeighborhoodAlgorithm(config).compute(graph);
	}
	
	private void readKaleMatrices() throws Exception {
		for (int r = 0; r < this.entityMatrix.rows(); r++)
			for (int c = 0; c < this.entityMatrix.columns(); c++) {
				this.coOccurrenceIdx_I.add(r);
				this.coOccurrenceIdx_J.add(c);
				this.coOccurrenceValues.add((float)this.entityMatrix.get(r, c));
			}

		for (int r = 0; r < this.relationMatrix.rows(); r++)
			for (int c = 0; c < this.relationMatrix.columns(); c++) {
				this.coOccurrenceIdx_I.add(r + this.vocabSize);
				this.coOccurrenceIdx_J.add(c + this.vocabSize);
				this.coOccurrenceValues.add((float)this.relationMatrix.get(r, c));
			}
	}

	@Override
	public void shuffle() {
		permutation.shuffle();
	}
	
	public int cIdx_I(int i) {
		return contextIndex2Focus(coOccurrenceIdx_I.get(permutation.randomAccess(i)));
	}
	
	public int cIdx_J(int j) {
		return this.coOccurrenceIdx_J.get(permutation.randomAccess(j));
	}
	
	public float cIdx_C(int i) {
		return this.coOccurrenceValues.get(permutation.randomAccess(i));
	}
	
	public byte getType(int index) {
		return (byte) this.graph.getVertexTypeProperty().getValueAsInt(focusIndex2Context(index));
	}
	
	public int coOccurrenceCount() {
		return this.coOccurrenceCount;
	}


	@Override
	public InMemoryRdfGraph getGraph() {
		return graph;
	}

	@Override
	public int contextIndex2Focus(int i) {
		return context2focus.get(i);
	}

	@Override
	public int focusIndex2Context(int i) {
		return focus2context.get(i);
	}

	@Override
	public int nrOfContextVectors() {
		return contextVectors;
	}

	@Override
	public int nrOfFocusVectors() {
		return focusVectors;
	}

	@Override
	public double max() {
		return this.bcvMaxVals.get("standard");
	}
	
	public double max(String bcvName) {
		return this.bcvMaxVals.get(bcvName);
	}
	
	@Override
	public String getKey(int index) {
		return this.graph.getVertexLabelProperty().getValueAsString(focusIndex2Context(index));
	}
	

	private void setMax(double newMax) {
		this.bcvMaxVals.put("standard", Math.max(this.bcvMaxVals.get("standard"), newMax));
	}
	
	private void setMax(String bcvName, double newMax) {
		this.bcvMaxVals.put(bcvName, Math.max(this.bcvMaxVals.get(bcvName), newMax));
	}
	
	/**
	 * 
	 * @return
	 */
	public int[][] getInVertices() {
		return this.inVertex;
	}
	
	/**
	 * 
	 * @return
	 */
	public int[][] getOutVertices() {
		return this.outVertex;
	}

	/**
	 * 
	 * @return
	 */
	public int[][] getInEdges() {
		return this.inEdge;
	}

	/**
	 * 
	 * @return
	 */
	public int[][] getOutEdges() {
		return this.outEdge;
	}
}
