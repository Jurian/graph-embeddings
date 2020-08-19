package org.uu.nl.embedding.bca.jobs;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.uu.nl.embedding.bca.util.BCAJobStable;
import org.uu.nl.embedding.bca.util.BCV;
import org.uu.nl.embedding.bca.util.PaintedNode;
import org.uu.nl.embedding.util.ArrayUtils;
import org.uu.nl.embedding.util.InMemoryRdfGraph;

public class KaleUndirectedWeightedNodeBased extends BCAJobStable {
	/*
	 * 15x10 version
	 */

    int numVerts;

	public KaleUndirectedWeightedNodeBased(InMemoryRdfGraph graph, int bookmark,
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
	protected BCV doWork(final boolean reverse) {
		if (this.bookmark < this.numVerts) return doWorkNodes(reverse);
		else return doWorkEdges(reverse);
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

		final TreeMap<Integer, PaintedNode> nodeTree = new TreeMap<>();
		final BCV bcv = new BCV(this.bookmark);
        String bookmarkPred = this.graph.getEdgeLabelProperty().getValueAsString(this.bookmark).toLowerCase();


		int[] neighbors, edges;
		int focusNode;
		double wetPaint, partialWetPaint, totalWeight;
		PaintedNode node;
		
		// Firstly, add all nodes with bookmark edge.
		for (int i = 0; i < this.vertexIn.length; i++)
			for (int j = 0; j < this.vertexIn[i].length; j++) {
				if (this.graph.getEdgeLabelProperty().getValueAsString(edgeIn[i][j]).toLowerCase() == bookmarkPred) {
					
					neighbors = getNeighbors(reverse, i);
					edges = getEdges(reverse, i);
					totalWeight = getTotalWeight(neighbors, edges);
					float weight = this.graph.getEdgeWeightProperty().getValueAsFloat(this.edgeIn[i][j]);
					
					if (!nodeTree.containsKey(i)) {
						nodeTree.put(i, new PaintedNode(i, (1 - this.alpha) * 1 * (weight / totalWeight)));
					} else {
						nodeTree.get(i).addPaint((1 - this.alpha) * 1 * (weight / totalWeight));
					}
				}
			}

		// Then resume regular method to add to bcv.
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
}
