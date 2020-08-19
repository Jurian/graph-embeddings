package org.uu.nl.embedding.bca.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.uu.nl.embedding.bca.util.BCAJobStable;
import org.uu.nl.embedding.bca.util.BCV;
import org.uu.nl.embedding.bca.util.PaintedNode;
import org.uu.nl.embedding.convert.util.NodeInfo;
import org.uu.nl.embedding.logic.DateCompareLogic;
import org.uu.nl.embedding.logic.util.SimpleDate;
import org.uu.nl.embedding.util.InMemoryRdfGraph;

import grph.properties.NumericalProperty;
import grph.properties.Property;

public class ContextWinnowedUndirectedWeighted extends BCAJobStable {

    int numVerts;
    double daysDifference;

	public ContextWinnowedUndirectedWeighted(InMemoryRdfGraph graph,
											int bookmark, double daysDifference,
											double alpha, double epsilon, 
											int[][] vertexIn, int[][] vertexOut,
											int[][] edgeIn, int[][] edgeOut) {
		
		super(bookmark, true, alpha, epsilon, graph, vertexOut, vertexIn, edgeOut, edgeIn);
		this.daysDifference = daysDifference;
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
		return doWorkContextWinnow(reverse, this.daysDifference);
	}

    
    /**
     * Creates separate BCV for edges.
     * @param graph
     * @param reverse
     * @return
     * @author Euan Westenbroek
     */
	protected BCV addDateAwareWinnow(final boolean reverse,
									/*final BCV origBcv,*/ final int[] orderedNodes,
									final List<Map<Integer, int[]>> removedInOutMaps) {


        if(reverse) throw new UnsupportedOperationException("No reverse mode in undirected version");

        final NumericalProperty edgeWeights = this.graph.getEdgeWeightProperty();
        final TreeMap<Integer, PaintedNode> nodeTree = new TreeMap<>();
        final BCV bcv = new BCV(this.bookmark);
        int[][][] inOutVerts = new int[][][] {this.vertexIn, this.vertexOut};
        int[][][] inOutEdges = new int[][][] {this.edgeIn, this.edgeOut};
        
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
                    else if (removedNeighbors.length > 0) {
                    	// Act accordingly by reducing number of neighbors and 
                    	// ignore the removed vertex (== neighbor of focusNode).
                        	
                            // Don't update values, and ignore this edge.
                            if (Arrays.asList(removedNeighbors).contains(inOutEdges[direction][focusNode][i])) continue;
                            // Else: update values.
                            else {
                                float weight = edgeWeights.getValueAsFloat(inOutEdges[direction][focusNode][i]);
                                partialWetPaint = (1 - this.alpha) * wetPaint * (weight / totalWeight);
                
                                // We can already tell that the neighbor will not
                                // have enough paint to continue.
                                if (partialWetPaint < this.epsilon) continue;
                
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
	

	/**
	 * 
	 * 
	 * @author Euan Westenbroek
	 * @version 1.0
	 * @since 25-05-2020
	 */
	protected BCV doWorkContextWinnow(final boolean reverse, final double daysDifference) {
		
		BCV bcv = doWorkDefault(this.reverse);
		final int root = bcv.getRootNode();
	
		//nodeTree.put(root, new PaintedNode(root, 1)); // nodig om bcv opnieuw te maken als neighbouring node wordt weggehaald

//###--------------------------------------------------
		int[] dateNeighbors = new int[] {}; // OMSCHRIJVEN NAAR ARRAYLIST?
		int[] dateEdges = new int[] {};
		
		// Initialize for Out-edges.
		int[][] allDateVertsOut = new int[bcv.size()][];
		int[][] allDateEdgesOut = new int[bcv.size()][];
		int[][] allProxyDateVertsOut = new int[bcv.size()][];
		int[][] allProxyDateEdgesOut = new int[bcv.size()][];
		// Initialize for In-edges
		int[][] allDateVertsIn = new int[bcv.size()][];
		int[][] allDateEdgesIn = new int[bcv.size()][];
		int[][] allProxyDateVertsIn = new int[bcv.size()][];
		int[][] allProxyDateEdgesIn= new int[bcv.size()][];
		// Combine arrays in array for clean looping code.
		int[][][] allDateVerts = new int[][][] {allDateVertsIn, allDateVertsOut};
		int[][][] allDateEdges = new int[][][] {allDateEdgesIn, allDateEdgesOut};
		int[][][] allProxyDateVerts = new int[][][] {allProxyDateVertsIn, allProxyDateVertsOut};
		int[][][] allProxyDateEdges= new int[][][] {allProxyDateEdgesIn, allProxyDateEdgesOut};
    	List<Map<Integer, int[]>> removedMaps = new ArrayList<>();
	
		final NumericalProperty nodeTypes = graph.getVertexTypeProperty();
		final NumericalProperty edgeWeights = graph.getEdgeWeightProperty();
		final NumericalProperty edgeTypes = graph.getEdgeTypeProperty(); // Added edges have type == 0
		final Property vertexLabels = graph.getVertexLabelProperty();
		final Property edgeLabels = graph.getEdgeLabelProperty();
		
	
		NodeInfo rootInfo = NodeInfo.fromByte((byte) nodeTypes.getValueAsInt(root));
		String rootLabel = vertexLabels.getValueAsString(root);
		
		// Keep track of the order in which the Date entries were put in.
		int[] focusNodeOrder = new int[bcv.entrySet().size()];
		focusNodeOrder[0] = root;
		
		int[][] rootNeighbors = new int[][] {};
		int[][] rootEdges = new int[][] {};
		
		final int[][][] inOutVerts = new int[][][] {this.vertexIn, this.vertexOut};
		final int[][][] inOutEdges = new int[][][] {this.edgeIn, this.edgeOut};
		int[] rootProxyDateVerts;
		int[] rootProxyDateEdges;
		String neighborLabel;
		
		for(int direction = 0; direction < inOutVerts.length; direction++) {
			
			rootNeighbors[direction] = inOutVerts[direction][root];
			rootEdges[direction] = inOutEdges[direction][root];
			rootProxyDateVerts = new int[] {};
			rootProxyDateEdges = new int[] {};
			
			// Check for edges of root not in original graph.
			for(int i = 0; i < rootEdges[direction].length; i++) {
				
				// Get vertex label to check if it is a date.
			    neighborLabel = vertexLabels.getValueAsString(rootNeighbors[direction][i]);
		    	if(SimpleDate.isDateFormat(neighborLabel)) {
		    		// Add the found date vertex and edge to their respective arrays.
					dateNeighbors = ArrayUtils.addAll(dateNeighbors,
										new int[] {rootNeighbors[direction][i]});
					dateEdges = ArrayUtils.addAll(dateEdges,
										new int[] {rootEdges[direction][i]});
		    		
					// edgeType == 0 means edge was not in original graph.
		    		if(edgeTypes.getValueAsInt(rootEdges[direction][i]) == 0) { 
						// Expand the added vertex list with all vertices the proxy
			    		// edges flow to an Date Literal node.
						rootProxyDateVerts = ArrayUtils.addAll(rootProxyDateVerts,
												new int[] {rootNeighbors[direction][i]});
						rootProxyDateEdges = ArrayUtils.addAll(rootProxyDateEdges,
												new int[] {rootEdges[direction][i]});
			    	}
				}
			}
			// Add the found proxyDate arrays to their respective arrays.
			allDateVerts[direction][0] = dateNeighbors;
			allDateEdges[direction][0] = dateEdges;
			allProxyDateVerts[direction][0] = rootProxyDateVerts;
			allProxyDateEdges[direction][0] = rootProxyDateEdges;
		}
		
		
		int counter = 0;
		int focusNode;
		int[] neighborEdges;
		int[] neighborVerts;
		int[] neighborProxyDateVerts;
		int[] neighborProxyDateEdges;

		// Loop through BCV and check for context.
		for(Map.Entry<Integer, Float> entry : bcv.entrySet()) { //START foreach-loop
			focusNode = entry.getKey();
			
			// Skip rest of loop if focusNode is root
			// counter increment is also skipped.
			if(focusNode == root)  continue;
			
	    	counter++;
	    	
	    	// Get node info to check type.
		    NodeInfo neighborInfo = NodeInfo.fromByte((byte) nodeTypes.getValueAsInt(focusNode));
		    neighborLabel = vertexLabels.getValueAsString(focusNode);
		    
	    	// Skip literal nodes.
			if(neighborInfo == NodeInfo.LITERAL) continue;
			
			// Check URI nodes for similar properties as root node above.
		    else if(neighborInfo == NodeInfo.URI) {
				for(int direction = 0; direction < inOutVerts.length; direction++) { // START for-loop
			    	// Initialize variables.
			    	dateNeighbors = new int[] {};
			    	dateEdges = new int[] {};
			    	neighborProxyDateVerts = new int[] {};
			    	neighborProxyDateEdges = new int[] {};
			    	// Initialize focusNode's out-edges and out-nodes.
			    	neighborVerts = inOutVerts[direction][focusNode];
			    	neighborEdges = inOutEdges[direction][focusNode];

					// Check for edges of focusNode not in root graph.
			    	for(int i = 0; i < neighborEdges.length; i++) {
						// Get vertex label to check for date.
					    neighborLabel = vertexLabels.getValueAsString(neighborVerts[i]);
				    	if(SimpleDate.isDateFormat(neighborLabel)) {
				    		// Add found date vertex and edge to their respective arrays.
							dateNeighbors = ArrayUtils.addAll(dateNeighbors,
												new int[] {neighborVerts[i]});
							dateEdges = ArrayUtils.addAll(dateEdges,
												new int[] {neighborEdges[i]});
				    		
							// edgeType == 0 means edge was not in original graph.
				    		if(edgeTypes.getValueAsInt(neighborEdges[i]) == 0) {
								// Expand added vertex list with all vertices the proxy
					    		// edges flow to an Date Literal node.
								neighborProxyDateVerts = ArrayUtils.addAll(neighborProxyDateVerts,
															new int[] {neighborVerts[i]});
								neighborProxyDateEdges = ArrayUtils.addAll(neighborProxyDateEdges,
															new int[] {neighborEdges[i]});
					    	}
			    		}
			    	}
					// Add found proxyDate arrays to their respective arrays.
					allDateVerts[direction][counter] = dateNeighbors;
					allDateEdges[direction][counter] = dateEdges;
					allProxyDateVerts[direction][counter] = neighborProxyDateVerts;
					allProxyDateEdges[direction][counter] = neighborProxyDateEdges;
				    
				} //END for-loop

				// Keep track of the order in which the Date entries were put in.
		    	focusNodeOrder[counter] = focusNode;
		    	
		    } // END if-statement NodeInfo.URI
		    
			/* 
			 * else { NodeInfo.BLANK node }
			 * So skip this kind of nodes
			 */
		    
		} //END foreach-loop
    	
    	/* 
    	 * Now check if there is both a proxy date vertex and proxy date edge
    	 * of either the root or focusNode, that are part of the other vertex'
    	 * neighbors.
    	 */
		removedMaps = createRemovedMaps(allProxyDateVerts, allProxyDateEdges,
										allDateVerts, allDateEdges,
										vertexLabels, edgeLabels,
										daysDifference,
										focusNodeOrder, counter);
    	// Create date aware BCV.
		BCV awareBcv = new BCV(root);
    	awareBcv = addDateAwareWinnow(this.reverse, focusNodeOrder, removedMaps);
		return awareBcv;
	}
	
	protected BCV doWorkDefault(final boolean reverse) {

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
	
	/**
	 * 
	 * 
	 * 
	 * @param allProxyDateVerts
	 * @param allProxyDateEdges
	 * @param allDateVerts
	 * @param allDateEdges
	 * @param vertexLabels
	 * @param edgeLabels
	 * @param dateRule
	 * @param daysDifference
	 * @param focusNodeOrder
	 * @param counter
	 * @param inOutSize
	 * @return removedMaps, a list of HashMap<Integer, int[]>
	 * 
	 * @author Euan Westenbroek
	 * @version 1.0
	 * @since 25-05-2020
	 */
	/*
	 * Deze methode als aanroep methode gebruiken en die hierboven juist als private?
	 * d.w.z. in deze methode wordt bovenstaande methode aangeroepen om de juiste 
	 * data terug te krijgen. --> Realistisch m.b.t. de hoeveelheid arrays etc die 
	 * aangemaakt moeten worden?
	 * 
	 */
	private List<Map<Integer, int[]>> createRemovedMaps(final int[][][] allProxyDateVerts,
									final int[][][] allProxyDateEdges, final int[][][] allDateVerts,
									final int[][][] allDateEdges, final Property vertexLabels,
									final Property edgeLabels, final double daysDifference,
									final int[] focusNodeOrder,final int counter) {

		// List of HashMaps that will be returned.
    	List<Map<Integer, int[]>> removedMaps = new ArrayList<>();
    	// Map of nodes with their incoming or outgoing neighboring
    	// nodes removed from original BCV.
		Map<Integer, int[]> removedMapIn = new HashMap<Integer, int[]>();
		Map<Integer, int[]> removedMapOut = new HashMap<Integer, int[]>();
    	
    	// Initialize empty new integer arrays.
    	int[] dateNeighbors;
    	int[] dateEdges;
		int[] neighborProxyDateVerts;
		int[] neighborProxyDateEdges;
		int[] removedVerts;

    	String proxyLabel, dateLabel;
		boolean validComparison;
		
    	for(int iProxy = 0; iProxy < counter; iProxy++) {
    		for(int jDate = 0; jDate < counter; jDate++) { //BEGIN outer double for-loop
    			
    			// Skip comparison with itself.
    			if(iProxy == jDate) continue;
    			
				for(int direction = 0; direction < allProxyDateVerts.length; direction++) {
    				// Reset arrays arrays.
    				neighborProxyDateVerts = allProxyDateVerts[direction][iProxy]; 
    				neighborProxyDateEdges = allProxyDateEdges[direction][iProxy];
    				dateNeighbors = allDateVerts[direction][jDate];
    				dateEdges = allDateEdges[direction][jDate];
    				removedVerts = new int[] {};
    				
    				// Check first for proxyArray size to skip unnecessary steps.
    				for(int i = 0; i < neighborProxyDateVerts.length; i++) {
    					for(int j = 0; j < dateNeighbors.length; j++) { //BEGIN inner double for-loop
    						
    						// Compare added date node to original date node but
    						// only if they have the same label.
    						if(neighborProxyDateVerts[i] == dateNeighbors[j] &&
    								neighborProxyDateEdges[i] == dateEdges[j]) {

    						    proxyLabel = vertexLabels.getValueAsString(neighborProxyDateVerts[i]);
    						    dateLabel = vertexLabels.getValueAsString(dateNeighbors[j]);
    						    
    						    // Get boolean value for comparing the two dates
    						    System.out.println("------------ ------------ ------- ------------ ------------");
    						    System.out.println("------------ ADD ALL TYPES OF DATE COMPARISONS ------------");
    						    System.out.println("-- ContextWinnowedUndirectedWeighted.createRemovedMaps() --");
    						    System.out.println("------------ ------------ ------- ------------ ------------");
    						    // OR IN DATECOMPARELOGIC CLASS
    							validComparison = DateCompareLogic.compareTwoDates(proxyLabel, dateLabel, (int)daysDifference);
    							if(!validComparison) { // validComparison == false
    								// Add of which a neighbor is removed and the removed vertices themselves
    								removedVerts = ArrayUtils.addAll(removedVerts,
    													new int[] {neighborProxyDateVerts[i]});
    							}
    						}
    					}
	    			// Add array with removed neighbors to its focusNodes place
    				if(direction == 0) 		removedMapIn.put(focusNodeOrder[iProxy], removedVerts);
    				else if(direction == 1) removedMapOut.put(focusNodeOrder[iProxy], removedVerts);
    				else { // direction > 1, throw Exception
    		        	throw new IllegalArgumentException("More than 2 directions for neighboring vertices."); }
    				} //END inner double for-loop
				}

    		}
    	} //END outer double for-loop
    	
    	// Put both the generated maps in the list and return the list
    	removedMaps.set(0, removedMapIn);
    	removedMaps.set(1, removedMapOut);
    	return removedMaps;
	}
	
}
