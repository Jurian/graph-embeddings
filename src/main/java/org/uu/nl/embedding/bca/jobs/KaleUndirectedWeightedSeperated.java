package org.uu.nl.embedding.bca.jobs;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.uu.nl.embedding.bca.util.BCAJobStable;
import org.uu.nl.embedding.bca.util.BCV;
import org.uu.nl.embedding.bca.util.PaintedNode;
import org.uu.nl.embedding.util.ArrayUtils;
import org.uu.nl.embedding.util.InMemoryRdfGraph;

public class KaleUndirectedWeightedSeperated extends BCAJobStable {
	/*
	 * 10x10 + 5x5 version
	 */

    int numVerts;

	public KaleUndirectedWeightedSeperated(InMemoryRdfGraph graph, int bookmark,
									double alpha, double epsilon,
									int[][] vertexIn, int[][] vertexOut,
									int[][] edgeIn, int[][] edgeOut) {
		super(bookmark, true, alpha, epsilon, graph, vertexOut, vertexIn, edgeOut, edgeIn);
	}

	@Override
	protected int[] getIndexes(boolean reverse, int focusNode, int[][] indexIn, int[][] indexOut) {

		int[] index = new int[indexIn[focusNode].length + indexOut[focusNode].length];
		System.arraycopy(indexIn[focusNode], 0, index, 0, indexIn[focusNode].length);
		System.arraycopy(indexOut[focusNode], 0, index, indexIn[focusNode].length, indexOut[focusNode].length);

		return index;
	}
	
	@Override
	protected BCV doWork(final boolean revers) {
		if (this.bookmark < this.numVerts) return doWorkNodes(revers);
		else return doWorkEdges(revers);
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

			// Keep part of the available paint on this node, distribute the rest
			bcv.add(focusNode, (this.alpha * wetPaint));

			neighbors = getNeighbors(reverse, focusNode);
			edges = getEdges(reverse, focusNode);

			totalWeight = getTotalWeight(neighbors, edges);

			for (int i = 0; i < neighbors.length; i++) {

				float weight = this.graph.getEdgeWeightProperty().getValueAsFloat(edges[i]);
				partialWetPaint = (1 - this.alpha) * wetPaint * (weight / totalWeight);

				// Stopping early here increases stability in GloVe
				if(partialWetPaint < this.epsilon) continue;

				// Log(n) time lookup
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

		final TreeMap<Integer, PaintedNode> edgeTree = new TreeMap<>();
		final TreeMap<Integer, PaintedNode> memoryTree = new TreeMap<>();
		final BCV bcv = new BCV(this.bookmark);

		edgeTree.put(this.bookmark, new PaintedNode(this.bookmark, 1));

		int[] edges;
		int focusEdge, edgeID;
		double wetPaint, partialWetPaint, totalWeight;
		PaintedNode node;

		while (!edgeTree.isEmpty()) {

			node = edgeTree.pollFirstEntry().getValue();
			focusEdge = node.nodeID;
			wetPaint = node.getPaint();

			// Keep part of the available paint on this node, distribute the rest
			memoryTree.put(focusEdge, new PaintedNode(focusEdge, (this.alpha * wetPaint)));

			edges = getEdgesEdge(reverse, focusEdge);

			totalWeight = getTotalWeight(edges);

			for (int i = 0; i < edges.length; i++) {
				
				edgeID = edges[i] + this.numVerts;
				
				float weight = this.graph.getEdgeWeightProperty().getValueAsFloat(edges[i]);
				partialWetPaint = (1 - this.alpha) * wetPaint * (weight / totalWeight);

				// Stopping early here increases stability in GloVe
				if(partialWetPaint < this.epsilon) continue;

				// Log(n) time lookup
				if (edgeTree.containsKey(edgeID)) {
					edgeTree.get(edgeID).addPaint(partialWetPaint);
				} else {
					edgeTree.put(edgeID, new PaintedNode(edgeID, partialWetPaint));
				}

			}
		}
		for (Map.Entry<Integer, PaintedNode> entry : memoryTree.entrySet()) {
			bcv.add(entry.getKey(), entry.getValue().getPaint());
		}
		return bcv;
	}
	
	private int[] getEdgesEdge(final boolean reverse, final int focusEdge) {

		final int edgeID = focusEdge - this.numVerts;
        String focusPred = this.graph.getEdgeLabelProperty().getValueAsString(edgeID).toLowerCase();
		ArrayList<Integer> indicesList = new ArrayList<Integer>();
        
		int edge;
		ArrayList<Integer> nodesWithEdge = new ArrayList<Integer>();
		for (int i = 0; i < this.numVerts; i++) {
			for (int j = 0; j < this.edgeOut[i].length; j++) {
				
				edge = this.edgeOut[i][j];
				if (this.graph.getEdgeLabelProperty().getValueAsString(edge).toLowerCase() == focusPred) {
					// Add for both vertexIn and vertexOut.
					if (!nodesWithEdge.contains(i)) {
						nodesWithEdge.add(i);
						for (int e = 0; e < this.edgeOut[i].length; e++)
							indicesList.add(this.edgeOut[i][e]);
					}
					if (!nodesWithEdge.contains(this.vertexOut[i][j])) {
						nodesWithEdge.add(this.vertexOut[i][j]);
						for (int e = 0; e < this.edgeOut[this.vertexOut[i][j]].length; e++)
							indicesList.add(this.edgeOut[this.vertexOut[i][j]][e]);
					}
				}
				
			}
		}
		int[] indices = ArrayUtils.toArray(indicesList, 0);
		return indices;
	}
	
	protected double getTotalWeight(final int[] edges) {
		double totalWeight = 0f;
		for (int i = 0; i < edges.length; i++)
			totalWeight += graph.getEdgeWeightProperty().getValueAsFloat(edges[i]);
		return totalWeight;
	}
}
