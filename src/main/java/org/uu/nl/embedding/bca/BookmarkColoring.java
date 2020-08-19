package org.uu.nl.embedding.bca;

import me.tongfei.progressbar.ProgressBar;

import org.apache.commons.lang3.ArrayUtils;
import org.uu.nl.embedding.bca.jobs.ContextWinnowedUndirectedWeighted;
import org.uu.nl.embedding.bca.jobs.DirectedWeighted;
import org.uu.nl.embedding.bca.jobs.HybridWeighted;
import org.uu.nl.embedding.bca.jobs.KaleUndirectedWeighted;
import org.uu.nl.embedding.bca.jobs.KaleUndirectedWeightedNodeBased;
import org.uu.nl.embedding.bca.jobs.KaleUndirectedWeightedSeperated;
import org.uu.nl.embedding.bca.jobs.UndirectedWeighted;
import org.uu.nl.embedding.bca.util.BCV;
import org.uu.nl.embedding.bca.util.PaintedNode;
import org.uu.nl.embedding.convert.util.InEdgeNeighborhoodAlgorithm;
import org.uu.nl.embedding.convert.util.NodeInfo;
import org.uu.nl.embedding.convert.util.OutEdgeNeighborhoodAlgorithm;
import org.uu.nl.embedding.logic.DateCompareLogic;
import org.uu.nl.embedding.logic.ExactSameDateLogic;
import org.uu.nl.embedding.logic.util.SimpleDate;
import org.uu.nl.embedding.util.CoOccurrenceMatrix;
import org.uu.nl.embedding.util.InMemoryRdfGraph;
import org.uu.nl.embedding.util.config.Configuration;
import org.uu.nl.embedding.util.rnd.Permutation;

import grph.properties.NumericalProperty;
import grph.properties.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.*;

/**
 * @author Jurian Baas
 */
public class BookmarkColoring implements CoOccurrenceMatrix {

	private ArrayList<Integer> coOccurrenceIdx_I;
	private ArrayList<Integer> coOccurrenceIdx_J;
	private ArrayList<Float> coOccurrenceValues;
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

	public BookmarkColoring(final InMemoryRdfGraph graph, final Configuration config) {

		final double alpha = config.getBca().getAlpha();
		final double epsilon = config.getBca().getEpsilon();
		final int[] verts = graph.getVertices().toIntArray();
		final boolean[] performBCA = new boolean[verts.length];

		this.graph = graph;
		this.vocabSize = verts.length;
		
		// Initialization standard co-occurrence matrix
		this.coOccurrenceIdx_I = new ArrayList<>(vocabSize);
		this.coOccurrenceIdx_J = new ArrayList<>(vocabSize);
		this.coOccurrenceValues = new ArrayList<>(vocabSize);
		
		this.bcvMaxVals = new HashMap<String, Double>();
		final Configuration.Output output = config.getOutput();

		this.context2focus = new HashMap<>();
		this.focus2context = new HashMap<>();

		int notSkipped = 0;

		for(int i = 0; i < verts.length; i++) {

			final int vert = verts[i];
			final byte type = (byte) graph.getVertexTypeProperty().getValueAsInt(vert);
			final String key = graph.getVertexLabelProperty().getValueAsString(vert);
			final NodeInfo nodeInfo = NodeInfo.fromByte(type);

			switch (nodeInfo) {
			case URI:
				if(output.outputUriNodes() && (output.getUri().isEmpty() || output.getUri().stream().anyMatch(key::startsWith))) {
					performBCA[i] = true;
					notSkipped++;
				}
				break;
			case BLANK:
				if(output.outputBlankNodes()) {
					performBCA[i] = true;
					notSkipped++;
				}
				break;
			case LITERAL:
				if(output.outputLiteralNodes() && (output.getLiteral().isEmpty() || output.getLiteral().stream().anyMatch(key::startsWith))) {
					performBCA[i] = true;
					notSkipped++;
				}
				break;
			}
		}
		
		this.focusVectors = notSkipped;
		this.contextVectors = verts.length;
		this.coOccurrenceIdx_I = new ArrayList<>(notSkipped);
		this.coOccurrenceIdx_J = new ArrayList<>(notSkipped);
		this.coOccurrenceValues = new ArrayList<>(notSkipped);

		final int numThreads = config.getThreads();
		this.graphConfig = config;

		final ExecutorService es = Executors.newWorkStealingPool(numThreads);

		this.inVertex = graph.getInNeighborhoods();
		this.outVertex = graph.getOutNeighborhoods();
		this.inEdge = new InEdgeNeighborhoodAlgorithm(config).compute(graph);
		this.outEdge = new OutEdgeNeighborhoodAlgorithm(config).compute(graph);

		CompletionService<BCV> completionService = new ExecutorCompletionService<>(es);

		for(int i = 0, j = 0; i < verts.length; i++) {

			if(!performBCA[i]) continue;

			final int bookmark = verts[i];
			context2focus.put(bookmark, j);
			focus2context.put(j, bookmark);
			j++;

			// Choose a graph neighborhood algorithm
			switch (config.getBca().getTypeEnum()){

				case DIRECTED:
					completionService.submit(new DirectedWeighted(
							graph, bookmark,
							alpha, epsilon,
							this.inVertex, this.outVertex, this.inEdge, this.outEdge));
					break;
				case UNDIRECTED:
					completionService.submit(new UndirectedWeighted(
							graph, bookmark,
							alpha, epsilon,
							inVertex, outVertex, inEdge, outEdge));
					break;
				case HYBRID:
					completionService.submit(new HybridWeighted(
							graph, bookmark,
							alpha, epsilon,
							inVertex, outVertex, inEdge, outEdge));
					break;
			}
		}

		try(ProgressBar pb = Configuration.progressBar("BCA", notSkipped, "nodes")) {

			//now retrieve the futures after computation (auto wait for it)
			int received = 0;

			while(received < notSkipped) {

				try {

					final BCV bcv = completionService.take().get();

					switch (config.getBca().getNormalizeEnum()) {
						case UNITY:
							bcv.toUnity();
							break;
						case COUNTS:
							bcv.toCounts();
							break;
						default:
						case NONE:
							break;
					}
					
					//System.out.println(bcv);

					// It is possible to use this maximum value in GloVe, although in the
					// literature they set this value to 100 and leave it at that
					setMax(bcv.max());
					
					// Create co-occurrence matrix for standard bcv
					for (Entry<Integer, Float> bcr : bcv.entrySet()) {
						coOccurrenceIdx_I.add(bcv.getRootNode());
						coOccurrenceIdx_J.add(bcr.getKey());
						coOccurrenceValues.add(bcr.getValue());
					}
					coOccurrenceCount += bcv.size();

				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				} finally {
					received++;
					pb.step();
				}
			}

		} finally {
			es.shutdown();
		}

		permutation = new Permutation(coOccurrenceCount);
	}
	
	/**
	 * 
	 * @param graph
	 * @param config
	 * @param isKale
	 * @author Euan Westenbroek
	 */
	public BookmarkColoring(final InMemoryRdfGraph graph, final Configuration config, final boolean nonDefault) {
		
		final double alpha = config.getBca().getAlpha();
		final double epsilon = config.getBca().getEpsilon();
		final int[] verts = graph.getVertices().toIntArray();
		final boolean[] performBCA = new boolean[verts.length];

		this.graph = graph;
		this.vocabSize = verts.length;
		
		this.bcvMaxVals = new HashMap<String, Double>();
		final Configuration.Output output = config.getOutput();

		this.inVertex = graph.getInNeighborhoods();
		this.outVertex = graph.getOutNeighborhoods();
		this.inEdge = new InEdgeNeighborhoodAlgorithm(config).compute(graph);
		this.outEdge = new OutEdgeNeighborhoodAlgorithm(config).compute(graph);

		int notSkipped = 0;

		for(int i = 0; i < this.vocabSize; i++) {

			final int vert = verts[i];
			final byte type = (byte) graph.getVertexTypeProperty().getValueAsInt(vert);
			final String key = graph.getVertexLabelProperty().getValueAsString(vert);
			final NodeInfo nodeInfo = NodeInfo.fromByte(type);

			switch (nodeInfo) {
			case URI:
				if(output.outputUriNodes() && (output.getUri().isEmpty() || output.getUri().stream().anyMatch(key::startsWith))) {
					performBCA[i] = true;
					notSkipped++;
				}
				break;
			case BLANK:
				if(output.outputBlankNodes()) {
					performBCA[i] = true;
					notSkipped++;
				}
				break;
			case LITERAL:
				if(output.outputLiteralNodes() && (output.getLiteral().isEmpty() || output.getLiteral().stream().anyMatch(key::startsWith))) {
					performBCA[i] = true;
					notSkipped++;
				}
				break;
			}
		}
		
		TreeMap<Integer, Integer> edgeIdMap = generateEdgeIdMap(graph);
		// Initialization standard co-occurrence matrix
		this.focusVectors = notSkipped;
		final int nVectors = notSkipped + edgeIdMap.size();
		this.coOccurrenceIdx_I = new ArrayList<>(nVectors);
		this.coOccurrenceIdx_J = new ArrayList<>(nVectors);
		this.coOccurrenceValues = new ArrayList<>(nVectors);

		final int numThreads = config.getThreads();
		this.graphConfig = config;

		final ExecutorService es = Executors.newWorkStealingPool(numThreads);

		CompletionService<BCV> completionService = new ExecutorCompletionService<>(es);

		this.context2focus = new HashMap<>();
		this.focus2context = new HashMap<>();
		
		// Create subgraphs according to config-file.
		for(int i = 0, j = 0; i < this.vocabSize; i++) {
			
			// Skip unnecessary nodes.
			if(!performBCA[i]) continue;
			
			final int bookmark = verts[i];
			context2focus.put(bookmark, j);
			focus2context.put(j, bookmark);
			j++;

			// Choose a graph neighborhood algorithm
			switch (config.getBca().getTypeEnum()){
				case DIRECTED:
					completionService.submit(new DirectedWeighted(
							graph, bookmark,
							alpha, epsilon,
							this.inVertex, this.outVertex, this.inEdge, this.outEdge));
					break;
				case UNDIRECTED:
					completionService.submit(new UndirectedWeighted(
							graph, bookmark,
							alpha, epsilon,
							inVertex, outVertex, inEdge, outEdge));
					break;
				case KALEUNDIRECTED:
					completionService.submit(new KaleUndirectedWeighted(
							graph, bookmark,
							alpha, epsilon,
							inVertex, outVertex, inEdge, outEdge));
					break;
				case KALESEPERATED:
					completionService.submit(new KaleUndirectedWeightedSeperated(
							graph, bookmark,
							alpha, epsilon,
							inVertex, outVertex, inEdge, outEdge));
					break;
				case KALENODEBASED:
					completionService.submit(new KaleUndirectedWeightedNodeBased(
							graph, bookmark,
							alpha, epsilon,
							inVertex, outVertex, inEdge, outEdge));
					break;
				case CONTEXTWINNOWED:
					/*
					 * Days difference fiksen vanuit config
					 */
					int daysDiff = 0;
					completionService.submit(new ContextWinnowedUndirectedWeighted(
							graph, bookmark, daysDiff,
							alpha, epsilon,
							inVertex, outVertex, inEdge, outEdge));
					break;
				case HYBRID:
					completionService.submit(new HybridWeighted(
							graph, bookmark,
							alpha, epsilon,
							inVertex, outVertex, inEdge, outEdge));
					break;
			}
		}
		
		// Concurrent BCV generation.
		try(ProgressBar pb = Configuration.progressBar("BCA", notSkipped, "nodes")) {

			//now retrieve the futures after computation (auto wait for it)
			int received = 0;

			while(received < notSkipped) {
				try {

					final BCV bcv = completionService.take().get();

					switch (config.getBca().getNormalizeEnum()) {
						case UNITY:
							bcv.toUnity();
							break;
						case COUNTS:
							bcv.toCounts();
							break;
						default:
						case NONE:
							break;
					}
					
					//System.out.println(bcv);

					// It is possible to use this maximum value in GloVe, although in the
					// literature they set this value to 100 and leave it at that
					setMax(bcv.max());
					
					// Create co-occurrence matrix for standard bcv
					for (Entry<Integer, Float> bcr : bcv.entrySet()) {
						coOccurrenceIdx_I.add(bcv.getRootNode());
						coOccurrenceIdx_J.add(bcr.getKey());
						coOccurrenceValues.add(bcr.getValue());
					}
					coOccurrenceCount += bcv.size();

				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				} finally {
					received++;
					pb.step();
				}
			}

		} finally {
			es.shutdown();
		}

		this.permutation = new Permutation(coOccurrenceCount);
	}
	

    /**
     * 
     * @param graph
     * @return
     * @author Euan Westenbroek
     */
    public TreeMap<Integer, Integer> generateEdgeIdMap(final InMemoryRdfGraph graph) {

        final int numVerts = graph.getVertices().toIntArray().length;
        
        final TreeMap<Integer, Integer> edgeNodeID = new TreeMap<>();
        
        int[] edges;
        int edge, edgeID;
        for (int v = 0; v < this.outEdge.length; v++) {
        	edges = this.outEdge[v];
        	for (int e = 0; e < edges.length; e++) {
        		edge = edges[e];
	        	edgeID = numVerts + edge;
	        	
	        	if (!edgeNodeID.containsKey(edge)) edgeNodeID.put(edge, edgeID);
        }}
        return edgeNodeID;
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
