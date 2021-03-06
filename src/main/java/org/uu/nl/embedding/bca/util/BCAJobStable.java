package org.uu.nl.embedding.bca.util;

import org.uu.nl.embedding.util.InMemoryRdfGraph;

import java.util.TreeMap;

/**
 * One BCAJob represents performing the bookmark coloring algorithm for a single node. This version does early stopping,
 * thereby preventing any paint less than alpha * epsilon to be distributed. This improves stability in GloVe later on.
 * @author Jurian Baas
 *
 */
public abstract class BCAJobStable extends BCAJob {

	protected BCAJobStable(int bookmark, double alpha, double epsilon, InMemoryRdfGraph graph, int[][] vertexNeighborhood, int[][] edgeNeighborhood) {
		super(bookmark, alpha, epsilon, graph, vertexNeighborhood, edgeNeighborhood);
	}

	protected BCV doWork() {

		final TreeMap<Integer, PaintedNode> nodeTree = new TreeMap<>();
		final BCV bcv = new BCV(bookmark);

		nodeTree.put(bookmark, new PaintedNode(bookmark, 1));

		int[] neighbors, edges;
		int focusNode;
		double wetPaint, partialWetPaint, totalWeight;
		PaintedNode node;

		while (!nodeTree.isEmpty()) {

			node = nodeTree.pollFirstEntry().getValue();
			focusNode = node.nodeID;
			wetPaint = node.getPaint();


			// Keep part of the available paint on this node, distribute the rest
			bcv.add(focusNode, (alpha * wetPaint));

			neighbors = vertexNeighborhood[focusNode];
			edges = edgeNeighborhood[focusNode];

			totalWeight = getTotalWeight(neighbors, edges, -1);

			for (int i = 0; i < neighbors.length; i++) {

				float weight = graph.getEdgeWeightProperty().getValueAsFloat(edges[i]);
				partialWetPaint = (1 - alpha) * wetPaint * (weight / totalWeight);

				// Stopping early here increases stability in GloVe
				if(partialWetPaint < epsilon) continue;

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
