package org.uu.nl.embedding.bca.jobs;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.uu.nl.embedding.bca.util.BCAJobStable;
import org.uu.nl.embedding.bca.util.BCV;
import org.uu.nl.embedding.bca.util.PaintedNode;
import org.uu.nl.embedding.util.ArrayUtils;
import org.uu.nl.embedding.util.InMemoryRdfGraph;

import grph.properties.NumericalProperty;

public class KaleUndirectedWeighted extends BCAJobStable {
	/*
	 * 15x15 version.
	 */

    int numVerts;
    
    final TreeMap<Integer, Integer> edgeNodeID = new TreeMap<>();
    final TreeMap<Integer, Integer> edgeCntTree = new TreeMap<>();
    final TreeMap<Integer, Double> edgeTotalWeights = new TreeMap<>();
    final TreeMap<Integer, ArrayList<Integer>> outEdgeOf = new TreeMap<>();

	public KaleUndirectedWeighted(InMemoryRdfGraph graph, int bookmark,
						double alpha, double epsilon,
						int[][] vertexIn, int[][] vertexOut,
						int[][] edgeIn, int[][] edgeOut) {
		super(bookmark, true, alpha, epsilon, graph, vertexOut, vertexIn, edgeOut, edgeIn);
		fillMaps();
	}

	@Override
	protected int[] getIndexes(boolean reverse, int focusNode, int[][] indexIn, int[][] indexOut) {

		int[] index = new int[indexIn[focusNode].length + indexOut[focusNode].length];
		System.arraycopy(indexIn[focusNode], 0, index, 0, indexIn[focusNode].length);
		System.arraycopy(indexOut[focusNode], 0, index, indexIn[focusNode].length, indexOut[focusNode].length);

		return index;
	}
	

    /**
     * Expands the doWork for vertices by adding the
     * edges to the bcv's.
     * @param graph
     * @param reverse
     * @return
     * @author Euan Westenbroek
     */
    public BCV doWork(final InMemoryRdfGraph graph, final boolean reverse) {
        if(reverse) throw new UnsupportedOperationException("No reverse mode in undirected version");
    	
        BCV bcv;
        if (this.bookmark < this.numVerts) bcv = doWorkNodes(reverse);
        else bcv = doWorkEdges(reverse);

        final NumericalProperty edgeWeights = graph.getEdgeWeightProperty();
        final int[] allVerts = graph.getVertices().toIntArray();
        this.numVerts = allVerts.length;

        final TreeMap<Integer, Integer> edgeNodeID = new TreeMap<>();
        final TreeMap<Integer, Integer> edgeCntTree = new TreeMap<>();
        final TreeMap<Integer, Double> edgeTotalWeights = new TreeMap<>();
        final TreeMap<Integer, ArrayList<Integer>> outEdgeOf = new TreeMap<>();
        
        double weight, sumOfWeights = 0d;
        int[] edges;
        int focusNode, edgeID;
        ArrayList<Integer> vertList;

        // Fill all maps for edges.
        for (Map.Entry<Integer, Float> entry : bcv.entrySet()) {
        	int edge;
        	focusNode = entry.getKey();
        	edges = this.edgeOut[focusNode];
        	for (int neighbor = 0; neighbor < edges.length; neighbor++) {
	        	edge = edges[neighbor];
	        	edgeID = this.numVerts + edge;
	        	
	        	if (!edgeNodeID.containsKey(edge)) edgeNodeID.put(edge, edgeID);
	        	
	        	if (!edgeCntTree.containsKey(edgeID)) edgeCntTree.put(edgeID, 1);
	        	else { edgeCntTree.put(edgeID, edgeCntTree.get(edgeID)+1); }
	        	
	        	weight = (double)edgeWeights.getValueAsFloat(this.edgeOut[focusNode][neighbor]);
	        	sumOfWeights += weight;
	        	if (!edgeTotalWeights.containsKey(edgeID)) edgeTotalWeights.put(edgeID, weight);
	        	else { edgeTotalWeights.put(edgeID, edgeTotalWeights.get(edgeID) + weight); }
	        	
	        	if (!outEdgeOf.containsKey(focusNode)) { vertList = new ArrayList<Integer>(); }
	        	else { vertList = outEdgeOf.get(edgeID); }
        		vertList.add(focusNode);
        		outEdgeOf.put(edgeID, vertList);
        	}
        }
        
        PaintedNode pEdge;
        int focusEdge;
        double wetPaint, partialWetPaint;
        final TreeMap<Integer, PaintedNode> edgeTree = new TreeMap<>();
        BCV edgeBCV = new BCV(this.bookmark);
        
        edgeTree.put(this.bookmark, new PaintedNode(this.bookmark, 1));
        while (!edgeTree.isEmpty()) {
            for (Map.Entry<Integer, Float> entry : bcv.entrySet()) {
            	focusNode = entry.getKey();
            	wetPaint = entry.getValue();
            	edges = this.edgeOut[focusNode];

	        	pEdge = edgeTree.pollFirstEntry().getValue();
	            focusEdge = pEdge.nodeID;
	
	            // If there is not enough paint we stop and don't distribute among the neighbors
	            /*
//#######        * CHECK DE WISKUNDE HIER NOG FF GOED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	             * weghalen, want hieronder al skipped?
	             */
	            if ((this.alpha * wetPaint) < this.epsilon) continue;
	
	            // Distribute paint over edges.
	            // Start with outgoing edges.
	            for (int edge = 0; edge < this.edgeOut[focusNode].length; edge++) {
	            	edgeID = edgeNodeID.get(this.edgeOut[focusNode][edge]);
                    weight = edgeTotalWeights.get(edgeID);
	                partialWetPaint = (1 - this.alpha) * wetPaint * (weight / sumOfWeights);

		            // Keep part of the available paint on this node, distribute the rest
		            if (focusEdge != this.bookmark) {
		            	if (!edgeBCV.containsKey(focusEdge)) edgeBCV.add(focusEdge, (this.alpha * wetPaint));
		            	else edgeBCV.add(focusEdge, (edgeBCV.get(focusEdge) + (this.alpha * wetPaint)) );
		            }

	                // We can already tell that the neighbor will not have enough paint to continue
	                if(partialWetPaint < this.epsilon) continue;

	                // Log(n) time lookup
	                if (edgeTree.containsKey(edgeID)) {
	                	if (edgeID != this.bookmark) edgeTree.get(edgeID).addPaint(partialWetPaint);
	                	// Ignore otherwise.
	                	
	                } else {
	                    // Remember which node we came from so we don't go back
	                    // Remember which predicate we used to get here
	                	if(edgeID != this.bookmark)
		                	edgeTree.put(
		                			edgeNodeID.get(edgeID), 
		                			new PaintedNode(edgeID, partialWetPaint));
	                	/*
//#######              	 * GAAT DIT GOED MET CONVERSIE????????????????????????????????????????
	                	 */
	                	else edgeTree.put(
		                			edgeNodeID.get(edgeID), 
		                			new PaintedNode(edgeID, 1));
	                }
	            }
            }
        } // END WHILE
        
        // Add the edges bcv to the bcv and return the result.
        for (Map.Entry<Integer, Float> entry : edgeBCV.entrySet()) {
        	bcv.add(entry.getKey(), entry.getValue());
        }
        return bcv;
    }

	protected BCV doWorkNodes(final boolean reverse) {

		final TreeMap<Integer, PaintedNode> nodeTree = new TreeMap<>();
		final BCV bcv = new BCV(this.bookmark);

		nodeTree.put(this.bookmark, new PaintedNode(this.bookmark, 1));

		int[] neighbors, edges;
		int focusNode;
		double wetPaint, partialWetPaint, totalWeight;
		PaintedNode node;

		while (!nodeTree.isEmpty()) {

			node = nodeTree.pollFirstEntry().getValue();
			focusNode = node.nodeID;
			wetPaint = node.getPaint();

			// Keep part of the available paint on this node, distribute the rest.
			bcv.add(focusNode, (this.alpha * wetPaint));

			neighbors = getNeighbors(reverse, focusNode);
			edges = getEdges(reverse, focusNode);

			totalWeight = getTotalWeight(neighbors, edges);

			for (int i = 0; i < neighbors.length; i++) {

				float weight = this.graph.getEdgeWeightProperty().getValueAsFloat(edges[i]);
				partialWetPaint = (1 - this.alpha) * wetPaint * (weight / totalWeight);

				// Stopping early here increases stability in GloVe.
				if(partialWetPaint < this.epsilon) continue;

				// Log(n) time lookup.
				if (nodeTree.containsKey(neighbors[i])) {
					nodeTree.get(neighbors[i]).addPaint(partialWetPaint);
				} else {
					nodeTree.put(neighbors[i], new PaintedNode(neighbors[i], partialWetPaint));
				}

			}
		}
		return bcv;
	}
	
	protected BCV doWorkEdges(final boolean reverse) {

		final TreeMap<Integer, PaintedNode> nodeTree = new TreeMap<>();
		final BCV bcv = new BCV(this.bookmark);

		nodeTree.put(this.bookmark, new PaintedNode(this.bookmark, 1));

		int[] neighbors, edges;
		int focusNode;
		double wetPaint, partialWetPaint, totalWeight;
		PaintedNode node;

		while (!nodeTree.isEmpty()) {

			node = nodeTree.pollFirstEntry().getValue();
			focusNode = node.nodeID;
			wetPaint = node.getPaint();

			// Keep part of the available paint on this node, distribute the rest.
			bcv.add(focusNode, (this.alpha * wetPaint));

			neighbors = getNeighborsEdge(reverse, focusNode);
			edges = getEdgesEdge(reverse, focusNode);

			totalWeight = getTotalWeight(neighbors, edges);

			for (int i = 0; i < neighbors.length; i++) {

				float weight = this.graph.getEdgeWeightProperty().getValueAsFloat(edges[i]);
				partialWetPaint = (1 - this.alpha) * wetPaint * (weight / totalWeight);

				// Stopping early here increases stability in GloVe.
				if(partialWetPaint < this.epsilon) continue;

				// Log(n) time lookup.
				if (nodeTree.containsKey(neighbors[i])) {
					nodeTree.get(neighbors[i]).addPaint(partialWetPaint);
				} else {
					nodeTree.put(neighbors[i], new PaintedNode(neighbors[i], partialWetPaint));
				}

			}
		}
		return bcv;
	}
	
	private int[] getNeighborsEdge(final boolean reverse, final int focusEdge) {
		final int edgeID = focusEdge - this.numVerts;
        String focusPred = this.graph.getEdgeLabelProperty().getValueAsString(edgeID).toLowerCase();
		ArrayList<Integer> indicesList = new ArrayList<Integer>();
        
		int edge;
		for (int i = 0; i < this.numVerts; i++) {
			for (int j = 0; j < edgeIn[i].length; j++) {
				edge = edgeIn[i][j];
				if (this.graph.getEdgeLabelProperty().getValueAsString(edge).toLowerCase() == focusPred) {
					// Add for both vertexIn and vertexOut.
					indicesList.add(i);
					indicesList.add(j);
				}
			}
		}
		int[] indices = ArrayUtils.toArray(indicesList, 0);
		return indices;
	}
	
	private int[] getEdgesEdge(final boolean reverse, final int focusEdge) {
		final int edgeID = focusEdge - this.numVerts;
        String focusPred = this.graph.getEdgeLabelProperty().getValueAsString(edgeID).toLowerCase();
		ArrayList<Integer> indicesList = new ArrayList<Integer>();
        
		int edge;
		for (int i = 0; i < this.numVerts; i++) {
			for (int j = 0; j < edgeIn[i].length; j++) {
				edge = edgeIn[i][j];
				if (this.graph.getEdgeLabelProperty().getValueAsString(edge).toLowerCase() == focusPred) {
					// Add double for both vertexIn and vertexOut.
					indicesList.add(edge);
					indicesList.add(edge);
				}
			}
		}
		int[] indices = ArrayUtils.toArray(indicesList, 0);
		return indices;
	}
    
    private void fillMaps() {
	    double weight, sumOfWeights = 0d;
	    int[] edges;
	    int edge, edgeID;
	    ArrayList<Integer> vertList;

        final NumericalProperty edgeWeights = this.graph.getEdgeWeightProperty();
	    
	    // Fill all maps for the edges.
	    for (int vert = 0; vert < numVerts; vert++) {
	    	edges = this.edgeOut[vert];
	    	for (int neighbor = 0; neighbor < edges.length; neighbor++) {
	        	edge = edges[neighbor];
	        	edgeID = this.numVerts + edge;
	        	
	        	if (!this.edgeNodeID.containsKey(edge)) this.edgeNodeID.put(edge, edgeID);
	        	
	        	if (!this.edgeCntTree.containsKey(edgeID)) this.edgeCntTree.put(edgeID, 1);
	        	else { this.edgeCntTree.put(edgeID, this.edgeCntTree.get(edgeID)+1); }
	        	
	        	weight = (double)edgeWeights.getValueAsFloat(this.edgeOut[vert][neighbor]);
	        	sumOfWeights += weight;
	        	if (!this.edgeTotalWeights.containsKey(edgeID)) this.edgeTotalWeights.put(edgeID, weight);
	        	else { this.edgeTotalWeights.put(edgeID, this.edgeTotalWeights.get(edgeID) + weight); }
	        	
	        	if (!this.outEdgeOf.containsKey(vert)) { vertList = new ArrayList<Integer>(); }
	        	else { vertList = this.outEdgeOf.get(edgeID); }
	    		vertList.add(vert);
	    		this.outEdgeOf.put(edgeID, vertList);
	    	}
	    }
    }
		
}
