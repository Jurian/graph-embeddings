package org.uu.nl.embedding.bca.jobs;

import grph.properties.NumericalProperty;
import grph.properties.Property;
import grph.properties.StringProperty;
import org.uu.nl.embedding.bca.util.BCAJob;
import org.uu.nl.embedding.bca.util.BCAJobStable;
import org.uu.nl.embedding.bca.util.BCV;
import org.uu.nl.embedding.bca.util.PaintedNode;
import org.uu.nl.embedding.util.InMemoryRdfGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


/**
 * @author Jurian Baas
 */
public class UndirectedWeighted extends BCAJobStable {

    private int[][] vertexOut, vertexIn, edgeOut, edgeIn;


	public UndirectedWeighted(InMemoryRdfGraph graph, int bookmark,
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

    
    protected BCV doWork(final InMemoryRdfGraph graph, final boolean reverse) {

        if(reverse) throw new UnsupportedOperationException("No reverse mode in undirected version");

        final NumericalProperty edgeWeights = graph.getEdgeWeightProperty();
        final NumericalProperty edgeTypes = graph.getEdgeTypeProperty();
        final TreeMap<Integer, PaintedNode> nodeTree = new TreeMap<>();
        final BCV bcv = new BCV(bookmark);

        nodeTree.put(bookmark, new PaintedNode(bookmark, 1));

        int focusNode;
        double wetPaint, partialWetPaint;
        PaintedNode node;

        while (!nodeTree.isEmpty()) {

            node = nodeTree.pollFirstEntry().getValue();
            focusNode = node.nodeID;
            wetPaint = node.getPaint();

            // Keep part of the available paint on this node, distribute the rest
            bcv.add(focusNode, (alpha * wetPaint));

            // If there is not enough paint we stop and don't distribute among the neighbors
            if (wetPaint < epsilon)
                continue;
            
            double totalWeight = 0;

            for (int i = 0; i < vertexOut[focusNode].length; i++) {
                // Skip any edges we don't want to follow
                //if(neighbors[i] == node.prevNodeID) continue;
                totalWeight += edgeWeights.getValueAsFloat(edgeOut[focusNode][i]);
            }
            
            for (int i = 0; i < vertexIn[focusNode].length; i++) {
                // Skip any edges we don't want to follow
                //if(neighbors[i] == node.prevNodeID) continue;
                totalWeight += edgeWeights.getValueAsFloat(edgeIn[focusNode][i]);
            }

            for (int i = 0; i < vertexOut[focusNode].length; i++) {

                float weight = edgeWeights.getValueAsFloat(edgeOut[focusNode][i]);
                partialWetPaint = (1 - alpha) * wetPaint * (weight / totalWeight);

                // We can already tell that the neighbor will not have enough paint to continue
                if(partialWetPaint < epsilon)
                    continue;

                // Log(n) time lookup
                if (nodeTree.containsKey(vertexOut[focusNode][i])) {
                    nodeTree.get(vertexOut[focusNode][i]).addPaint(partialWetPaint);
                } else {

                    // Remember which node we came from so we don't go back
                    // Remember which predicate we used to get here
                    nodeTree.put(vertexOut[focusNode][i], new PaintedNode(vertexOut[focusNode][i], partialWetPaint));
                }
            }

            for (int i = 0; i < vertexIn[focusNode].length; i++) {

                float weight = edgeWeights.getValueAsFloat(edgeIn[focusNode][i]);
                partialWetPaint = (1 - alpha) * wetPaint * (weight / totalWeight);

                // We can already tell that the neighbor will not have enough paint to continue
                if(partialWetPaint < epsilon)
                    continue;

                // Log(n) time lookup
                if (nodeTree.containsKey(vertexIn[focusNode][i])) {
                    nodeTree.get(vertexIn[focusNode][i]).addPaint(partialWetPaint);
                } else {

                    // Remember which node we came from so we don't go back
                    // Remember which predicate we used to get here
                    nodeTree.put(vertexIn[focusNode][i], new PaintedNode(vertexIn[focusNode][i], partialWetPaint));
                }
            }
        }
        return bcv;
    }
    
    /**
     * Expands the doWork for vertices by adding the
     * edges to the bcv's.
     * @param graph
     * @param reverse
     * @return
     * @author Euan Westenbroek
     */
    public BCV doWorkInclEdges(final InMemoryRdfGraph graph, final boolean reverse) {
        if(reverse) throw new UnsupportedOperationException("No reverse mode in undirected version");
    	
        BCV bcv = doWork(graph, reverse);

        final NumericalProperty edgeWeights = graph.getEdgeWeightProperty();
        final int[] allVerts = graph.getVertices().toIntArray();
        final int numVerts = allVerts.length;

        final TreeMap<Integer, Integer> edgeNodeID = new TreeMap<>();
        final TreeMap<Integer, Integer> edgeCntTree = new TreeMap<>();
        final TreeMap<Integer, Double> edgeTotalWeights = new TreeMap<>();
        final TreeMap<Integer, ArrayList<Integer>> outEdgeOf = new TreeMap<>();
        
        double weight, sumOfWeights = 0d;
        int[] edges;
        int focusNode, edgeID;
        ArrayList<Integer> vertList;

        // Fill all maps for the edges.
        for (Map.Entry<Integer, Float> entry : bcv.entrySet()) {
        	int edge;
        	focusNode = entry.getKey();
        	edges = this.edgeOut[focusNode];
        	for (int neighbor = 0; neighbor < edges.length; neighbor++) {
	        	edge = edges[neighbor];
	        	edgeID = numVerts + edge;
	        	
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
	             */
	            if ( (this.alpha * wetPaint) < this.epsilon)
	                continue;
	
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
	                if(partialWetPaint < this.epsilon)
	                    continue;

	                // Log(n) time lookup
	                if (edgeTree.containsKey(edgeID)) {
	                	edgeTree.get(edgeID).addPaint(partialWetPaint);
	                	
	                } else {

	                    // Remember which node we came from so we don't go back
	                    // Remember which predicate we used to get here
	                	edgeTree.put(
	                			edgeNodeID.get(edgeID), 
	                			new PaintedNode(edgeID, partialWetPaint));
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
    
    /**
     * Creates separate BCV for edges.
     * @param graph
     * @param reverse
     * @return
     * @author Euan Westenbroek
     */
    public BCV doWorkEdges(final InMemoryRdfGraph graph, final boolean reverse) {
        if(reverse) throw new UnsupportedOperationException("No reverse mode in undirected version");
        
        final BCV bcv = new BCV(this.bookmark);

        final NumericalProperty edgeWeights = graph.getEdgeWeightProperty();
        final int[] allVerts = graph.getVertices().toIntArray();
        final int numVerts = allVerts.length;
        
        final TreeMap<Integer, Integer> edgeNodeID = new TreeMap<>();
        final TreeMap<Integer, Integer> edgeCntTree = new TreeMap<>();
        final TreeMap<Integer, Double> edgeTotalWeights = new TreeMap<>();
        final TreeMap<Integer, ArrayList<Integer>> outEdgeOf = new TreeMap<>();
        
        double weight, sumOfWeights = 0d;
        int[] edges;
        int edge, edgeID;
        ArrayList<Integer> vertList;
        
        // Fill all maps for the edges.
        for (int vert = 0; vert < numVerts; vert++) {
        	edges = this.edgeOut[vert];
        	for (int neighbor = 0; neighbor < edges.length; neighbor++) {
	        	edge = edges[neighbor];
	        	edgeID = numVerts + edge;
	        	
	        	if (!edgeNodeID.containsKey(edge)) edgeNodeID.put(edge, edgeID);
	        	
	        	if (!edgeCntTree.containsKey(edgeID)) edgeCntTree.put(edgeID, 1);
	        	else { edgeCntTree.put(edgeID, edgeCntTree.get(edgeID)+1); }
	        	
	        	weight = (double)edgeWeights.getValueAsFloat(this.edgeOut[vert][neighbor]);
	        	sumOfWeights += weight;
	        	if (!edgeTotalWeights.containsKey(edgeID)) edgeTotalWeights.put(edgeID, weight);
	        	else { edgeTotalWeights.put(edgeID, edgeTotalWeights.get(edgeID) + weight); }
	        	
	        	if (!outEdgeOf.containsKey(vert)) { vertList = new ArrayList<Integer>(); }
	        	else { vertList = outEdgeOf.get(edgeID); }
        		vertList.add(vert);
        		outEdgeOf.put(edgeID, vertList);
        	}
        }

        PaintedNode pEdge;
        int focusEdge, focusNode;
        ArrayList<Integer> edgeVertices;
        double totalWeight, wetPaint, partialWetPaint;
        final TreeMap<Integer, PaintedNode> edgeTree = new TreeMap<>();
        
        edgeTree.put(this.bookmark, new PaintedNode(this.bookmark, 1));
        while (!edgeTree.isEmpty()) {

        	pEdge = edgeTree.pollFirstEntry().getValue();
            focusEdge = pEdge.nodeID;
            wetPaint = pEdge.getPaint();

            // Keep part of the available paint on this node, distribute the rest
            bcv.add(focusEdge, (this.alpha * wetPaint));

            // If there is not enough paint we stop and don't distribute among the neighbors
            if (wetPaint < this.epsilon)
                continue;

            // Distribute paint over edges.
            // Start with outgoing edges.
            edgeVertices = outEdgeOf.get(focusEdge);
            for (int vert = 0; vert < edgeVertices.size(); vert++) {
            	for (int neighbor = 0; neighbor < this.vertexOut[vert].length; neighbor++) {
                    weight = edgeTotalWeights.get(edgeNodeID.get(this.edgeOut[vert][neighbor]));
                    partialWetPaint = (1 - this.alpha) * wetPaint * (weight / sumOfWeights);

                    // We can already tell that the neighbor will not have enough paint to continue
                    /*
//###########        * IS EPSILON TE KLEIN ALS DE PAINT VAN DE EDGES ZO HOOG GETAL IS????????????
                     * ?????????????????
                     */
                    if(partialWetPaint < this.epsilon) continue;

                    // Log(n) time lookup
                    if (edgeTree.containsKey(edgeNodeID.get(this.edgeOut[vert][neighbor]))) {
                    	edgeTree.get(edgeNodeID.get(this.edgeOut[vert][neighbor])).addPaint(partialWetPaint);
                    } else {

                        // Remember which node we came from so we don't go back
                        // Remember which predicate we used to get here
                    	edgeTree.put(
                    			edgeNodeID.get(this.edgeOut[vert][neighbor]),
                    			new PaintedNode(edgeNodeID.get(this.edgeOut[vert][neighbor]), partialWetPaint));
                    }
            }} 
        } // END WHILE
        return bcv;
    }

    /**
     * 
     * @param graph
     * @param reverse
     * @param origBcv
     * @param orderedNodes
     * @param removedInOutMaps
     * @return
     * @author Euan Westenbroek
     */
    public BCV doWorkWithIgnore(final InMemoryRdfGraph graph, final boolean reverse,
    							final BCV origBcv, final int[] orderedNodes,
    							final List<Map<Integer, int[]>> removedInOutMaps) {

        if(reverse) throw new UnsupportedOperationException("No reverse mode in undirected version");

        final NumericalProperty edgeWeights = graph.getEdgeWeightProperty();
        final TreeMap<Integer, PaintedNode> nodeTree = new TreeMap<>();
        final BCV bcv = new BCV(bookmark);
        int[][][] inOutVerts = new int[][][] {vertexIn, vertexOut};
        int[][][] inOutEdges = new int[][][] {edgeIn, edgeOut};
        
        // Start with bookmark.
        nodeTree.put(this.bookmark, new PaintedNode(this.bookmark, 1));

        int focusNode;
        double wetPaint, partialWetPaint;
        PaintedNode node;

        while (!nodeTree.isEmpty()) {
        	// Initialize variabels for current loop.
            node = nodeTree.pollFirstEntry().getValue();
            focusNode = node.nodeID;
            wetPaint = node.getPaint();

            // Keep part of the available paint on this node,
            // distribute the rest
            bcv.add(focusNode, (this.alpha * wetPaint));

            // If there is not enough paint we stop and don't 
            // distribute among the neighbors
            if (wetPaint < this.epsilon) continue;
            
            double totalWeight = 0;
            for(int direction = 0; direction < removedInOutMaps.size(); direction++) {
                // get array of removed neighbors (== neighbor of focusNode)
                int[] removedNeighbors = removedInOutMaps.get(direction).get(focusNode);
                
                /*
                 * Calculate totalWeight using edgeWeights
                 */
                for (int i = 0; i < inOutVerts[direction][focusNode].length; i++) {
                    // Check for any removed neighbors of focusNode.
                    if(removedNeighbors.length == 0) {
                        totalWeight += edgeWeights.getValueAsFloat(inOutEdges[direction][focusNode][i]);
                            
                    } // If not empty: ignore each removed vertex.
                    else if(removedNeighbors.length > 0) {

                        for(int removedEdge : removedNeighbors) {
                        	// Ignore removed edges.
                            if(inOutEdges[direction][focusNode][i] == removedEdge)  continue;
                            // Include if not removed.
                            else totalWeight += edgeWeights.getValueAsFloat(inOutEdges[direction][focusNode][i]);
                        }
                    } // else: removedVerts < 0 --> throw Exception
                    else { 
                    	// Include clarification in exception.
                        String sDir;
                        if(direction == 0)  sDir = "INCOMING";
                        else if(direction == 1) sDir = "OUTGOING";
                        else sDir = "(BUG WARNING: direction == " + Integer.toString(direction) + ")";
                        // Throw exception.
                        throw new IllegalArgumentException("Length of removed " + sDir + "verts array of " +
                                Integer.toString(focusNode) + " is smaller than 0, namely: " + Integer.toString(removedNeighbors.length));
                    }
                }
            }
            
            
            /*
             * Calculate new paint values
             * and put neighboring vertices in TreeMap
             */
            // Loop over directions as integer.
            for(int direction = 0; direction < removedInOutMaps.size(); direction++) {
                // Get array of removed neighbors (== neighbor of focusNode).
                int[] removedNeighbors = removedInOutMaps.get(direction).get(focusNode);

                // Check if there are any removed neighbors of the focusNode.
                for (int i = 0; i < inOutVerts[direction][focusNode].length; i++) {
                    if(removedNeighbors.length == 0) {
                                
                        float weight = edgeWeights.getValueAsFloat(inOutEdges[direction][focusNode][i]);
                        partialWetPaint = (1 - this.alpha) * wetPaint * (weight / totalWeight);
        
                        // We can already tell that the neighbor will not 
                        // have enough paint to continue.
                        if(partialWetPaint < this.epsilon) continue;
        
                        // Log(n) time lookup.
                        if (nodeTree.containsKey(inOutVerts[direction][focusNode][i])) {
                            nodeTree.get(inOutVerts[direction][focusNode][i]).addPaint(partialWetPaint);
                        } else {
        
                            // Remember which node we came from so we don't go back.
                            // Remember which predicate we used to get here.
                            nodeTree.put(inOutVerts[direction][focusNode][i],
                            				new PaintedNode(inOutVerts[direction][focusNode][i], partialWetPaint));
                        }
                            
                    } // If array not empty then ignore each removed vertex.
                    else if(removedNeighbors.length > 0) {
                    	// Act accordingly by reducing number of neighbors and 
                    	// ignore the removed vertex (== neighbor of focusNode).
                        for(int removedEdge : removedNeighbors) {
                        	
                            // Don't update values, and ignore this edge.
                            if(inOutEdges[direction][focusNode][i] == removedEdge) continue;
                            // Else: update values.
                            else {
                                float weight = edgeWeights.getValueAsFloat(inOutEdges[direction][focusNode][i]);
                                partialWetPaint = (1 - this.alpha) * wetPaint * (weight / totalWeight);
                
                                // We can already tell that the neighbor will not
                                // have enough paint to continue.
                                if(partialWetPaint < this.epsilon) continue;
                
                                // Log(n) time lookup.
                                if (nodeTree.containsKey(inOutVerts[direction][focusNode][i])) {
                                    nodeTree.get(inOutVerts[direction][focusNode][i]).addPaint(partialWetPaint);
                                } else {
                
                                    // Remember which node we came from so we don't go back.
                                    // Remember which predicate we used to get here.
                                    nodeTree.put(inOutVerts[direction][focusNode][i],
                                    				new PaintedNode(inOutVerts[direction][focusNode][i], partialWetPaint));
                                }
                            }
                        }
                    } // Else: removedVerts < 0 --> throw Exception
	                else { 
	                	// Include clarification in exception.
                        String sDir;
                        if(direction == 0) sDir = "INCOMING";
                        else if(direction == 1) sDir = "OUTGOING";
                        else sDir = "(BUG WARNING: direction == " + Integer.toString(direction) + ")";
                        // Throw exception.
                        throw new IllegalArgumentException("Length of removed " + sDir + "verts array of " +
                                Integer.toString(focusNode) + " is smaller than 0, namely: " + Integer.toString(removedNeighbors.length));
                    }
                }
            }
        }
        return bcv;
    }
    
}


