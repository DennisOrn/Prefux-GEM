/*  
 * Copyright (c) 2004-2013 Regents of the University of California.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of the University nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * Copyright (c) 2014 Martin Stockhammer
 */
package prefux.action.layout.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import prefux.action.layout.Layout;
import prefux.data.Edge;
import prefux.data.Graph;
import prefux.data.Node;
import prefux.util.PrefuseLib;
import prefux.visual.VisualItem;

public class GraphEmbedderLayout extends Layout {
	
	// List containing all the nodes.
	private List<Vertex> nodeList = new ArrayList<>();
	
	// Indicates if the algorithm has been initialized.
	private boolean initialized = false;
	
	// Current number of rounds.
	private int nrRounds = 0;
	
	// Maximum number of rounds allowed.
	private int maxRounds;
	
	// How often we want the visualization to update.
	// If set to 1: visualization will be updated after every round.
	// The visualization will always be updated when algorithm finishes,
	// no matter what this constant is set to.
	// WARNING: do not set this to zero!
	private final int updateFrequency = 9999;
	
	// The method used to calculate the distance between nodes.
	// If set to true: Euclidean distance is used.
	// If set to false: Manhattan distance is used.
	private final boolean euclideanDistance = true;
	
	// The global temperature.
	private double globalTemp;
	
	// The sum of the coordinates for all the nodes. This is
	// used to calculate the location of the barycenter.
	private double[] sumPos = new double[2];
	
	// The maximal temperature a node is allowed to have.
	private final double maxTemp = 256;
	
	// The desired global temperature.
	private final double desiredTemp = 3;
	
	// The desired length of the edges.
	private final double desiredEdgeLength = 128;
	
	// The gravitational constant.
	private final double gravitationalConstant = (double) 1 / 32;
	
	// The opening angles for oscillation- and rotation-detection.
	private final double oscillationOpeningAngle = Math.PI / 2;
	private final double rotationOpeningAngle = Math.PI;
	
	// The sensitivity for correcting oscillations and rotations.
	private final double oscillationSensitivity = 1.1;
	private double rotationSensitivity; // will be set in init()
	
	private class Vertex {
		
		// The corresponding visual item.
		private final VisualItem item;
		
		// The impulse vector.
		private double[] impulse = new double[2];
		
		// The temperature.
		private double temp = 256;
		
		// The skew.
		private double skew = 0;
		
		// The current coordinates.
		private double[] coordinates = new double[2];
		
		// List containing the neighbors.
		private List<Vertex> neighbors = new ArrayList<>();
		
		private Vertex(VisualItem item) {
			this.item = item;
			impulse[0] = 0;
			impulse[1] = 0;
		}
	}

	protected transient VisualItem referrer;
	protected String m_nodeGroup;
	protected String m_edgeGroup;
	private static final Logger log = LogManager.getLogger(GraphEmbedderLayout.class);
	
	/**
	 * Create a new GraphEmbedderLayout.
	 * 
	 * @param graph the data group to layout. Must resolve to a Graph instance.
	 */
	public GraphEmbedderLayout(String graph) {
		super(graph);
		m_nodeGroup = PrefuseLib.getGroupName(graph, Graph.NODES);
		m_edgeGroup = PrefuseLib.getGroupName(graph, Graph.EDGES);
	}
	
    /**
     * Initializes the algorithm and all the variables that are needed.
     */
	private void init() {
		
		System.out.println("Initializing algorithm...");
		
		// Place all the nodes in random positions and add them to nodeList.
		Iterator<VisualItem> iter = m_vis.visibleItems(m_nodeGroup);
		while(iter.hasNext()) {
			
			VisualItem item = iter.next();
			
			double newX = (Math.random() * 2048) - 1024;
			double newY = (Math.random() * 2048) - 1024;
			
			item.setX(newX);
			item.setY(newY);
			
			item.setFixed(false);
			
			sumPos[0] += newX;
			sumPos[1] += newY;
			
			Vertex v = new Vertex(item);
			v.coordinates[0] = item.getX();
			v.coordinates[1] = item.getY();
			
			nodeList.add(v);
		}
		
		System.out.println("Nodes added to list: " + nodeList.size() + ".");
		
		// Make sure the neighbors are added to every node.
		for(Vertex v : nodeList) {
			
			Iterator<? extends Edge> it = ((Node) v.item).edges();
			while(it.hasNext()) {
				
				Edge e = it.next();
				VisualItem u = (VisualItem) e.getSourceNode();
				
				// Make sure u and v are not the same node.
				if(u == v.item) {
					u = (VisualItem) e.getTargetNode();
				}
				
				for(Vertex v2 : nodeList) {
					if(u == v2.item) {
						v.neighbors.add(v2);
					}
				}
			}
		}
		
		maxRounds = nodeList.size() * 4;
		System.out.println("maxRounds set to: " + maxRounds + ".");
		
		rotationSensitivity = (double) 1 / (2 * nodeList.size());
		System.out.println("rotationSensitivity set to: " + rotationSensitivity + ".");
		
		initialized = true;
		
		System.out.println("Initialization done.");
	}
	
	/**
	 * @see prefux.action.Action#run(double)
	 */
	public void run(double frac) {
		
		System.out.println("-------------------------------------");
		System.out.println("Algorithm started.");
		
		long startTime = System.nanoTime();
		
		if(!initialized) {
			init();
		} // TODO: this if-statement might be unnecessary at the moment.
		
		do {
			
			System.out.println("-------------------------------------");
			System.out.println("ROUND " + (++nrRounds));
			
			// Reset the global temperature at the start of every round.
			globalTemp = 0;
			
			// Shuffle the list before every iteration.
			Collections.shuffle(nodeList);
			for(Vertex v : nodeList) {
				
				// Calculate the impulse. 
				double[] imp = calculateImpulse(v);
				
				// Use the impulse to calculate the temperature and move the node.
				calculateTemperature(v, imp);
			}
			
			// Calculate the average temperature.
			globalTemp = globalTemp / nodeList.size();
			
			System.out.println("Global temperature: " + globalTemp);
			System.out.println("Time elapsed: " + (System.nanoTime() - startTime) / 1000000000 + "s");
			
			// Update the visualization, or not.
			if(nrRounds % updateFrequency == 0 || globalTemp <= desiredTemp) {
				System.out.println("Updating visualization...");
				for(Vertex v : nodeList) {
					v.item.setX(v.coordinates[0]);
					v.item.setY(v.coordinates[1]);
				}
			}
			
		} while(globalTemp >= desiredTemp && nrRounds < maxRounds);
		
		// When the algorithm has finished, set fixed to true to enable
		// the touch-functionality in prefux.controls.GemControl.
		for(Vertex v : nodeList) {
			v.item.setFixed(true);
		}
		
		System.out.println("Algorithm finished.");
	}
	
    /**
     * Calculates the scaling factor.
     * @param v the vertex for which we want the scaling factor
     * @return the scaling factor
     */
	private double calculateScalingFactor(Vertex v) {
		return 1 + v.neighbors.size() / 2;
	}
	
    /**
     * Calculates the barycenter, or the center of mass.
     * @return the coordinates for the barycenter
     */
	private double[] calculateBarycenter() {
		double[] center = new double[2];
		center[0] = sumPos[0] / nodeList.size();
		center[1] = sumPos[1] / nodeList.size();
		return center;
	}
	
    /**
     * Calculates the impulse, which is the direction the
     * node wants to move towards.
     * @param v the vertex for which we want the impulse
     * @return an impulse vector
     */
	private double[] calculateImpulse(Vertex v) {
		
		// Attraction to the barycenter.
		double[] impulse = new double[2];
		impulse = calculateBarycenter();
		
		impulse[0] = impulse[0] - v.coordinates[0];
		impulse[1] = impulse[1] - v.coordinates[1];
		
		// Apply scaling factor.
		double scalingFactor = calculateScalingFactor(v);
		impulse[0] = impulse[0] * gravitationalConstant * scalingFactor;
		impulse[1] = impulse[1] * gravitationalConstant * scalingFactor;
		
		// Random disturbance vector; default range: [-32,32] * [-32,32].
		impulse[0] = impulse[0] + Math.random() * 40 - 20;
		impulse[1] = impulse[1] + Math.random() * 40 - 20;
		
		double desSquared = desiredEdgeLength * desiredEdgeLength;
		
		// For every node in the graph: calculate the repulsive forces.
		// NOTE: this is the most time-critical part of the algorithm.
		for(Vertex u : nodeList) {
			
			// If u and v are the same item: skip the iteration.
			if(u.item == v.item) {
				continue;
			}
			
			double[] delta = new double[2];
			delta[0] = v.coordinates[0] - u.coordinates[0];
			delta[1] = v.coordinates[1] - u.coordinates[1];
			
			double distance;
			
			if(euclideanDistance) {
				distance = Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1]);
			} else {
				distance = Math.abs(delta[0]) + Math.abs(delta[1]);
			}
			
			if(distance != 0) {
				double scale = desSquared / (distance * distance);
				impulse[0] = impulse[0] + delta[0] * scale;
				impulse[1] = impulse[1] + delta[1] * scale;
			}
		}
		
		double desSquaredScaled = desSquared * scalingFactor;
		
		// For every node connected to v: calculate the attractive forces.
		for(Vertex u : v.neighbors) {
			
			double[] delta = new double[2];
			delta[0] = v.coordinates[0] - u.coordinates[0];
			delta[1] = v.coordinates[1] - u.coordinates[1];
			
			double distance;
			
			if(euclideanDistance) {
				distance = Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1]);
			} else {
				distance = Math.abs(delta[0]) + Math.abs(delta[1]);
			}
			
			double scale = (distance * distance) / desSquaredScaled;
			
			impulse[0] = impulse[0] - delta[0] * scale;
			impulse[1] = impulse[1] - delta[1] * scale;
		}
		
		return impulse;
	}
	
    /**
     * Calculates the temperature, which is the distance the
     * node is going to move. The node is then moved in the 
     * @param v the vertex that is going to be moved
     * @param impulse the vector that is returned from {@link #calculateImpulse(Vertex)}
     */
	private void calculateTemperature(Vertex v, double[] impulse) {
		
		// If the current impulse is not 0.
		if(impulse[0] != 0 || impulse[1] != 0) {
			
			// Scale the impulse with the current temperature.
			double length = Math.sqrt(impulse[0] * impulse[0] + impulse[1] * impulse[1]);
			impulse[0] = v.temp * impulse[0] / length;
			impulse[1] = v.temp * impulse[1] / length;
			
			// Update v's coordinates.
			v.coordinates[0] += impulse[0];
			v.coordinates[1] += impulse[1];
			
			// Update the sum of all node-coordinates (used for calculating the barycenter).
			sumPos[0] += impulse[0];
			sumPos[1] += impulse[1];
		}
		
		double[] oldImpulse = v.impulse;
		
		// If the last impulse was not 0.
		if(oldImpulse[0] != 0 || oldImpulse[1] != 0) {
			
			// Calculate the angle between the last impulse and the current impulse.
			double uLen = Math.sqrt(impulse[0] * impulse[0] + impulse[1] * impulse[1]);
			double vLen = Math.sqrt(oldImpulse[0] * oldImpulse[0] + oldImpulse[1] * oldImpulse[1]);
			double dot = impulse[0] * oldImpulse[0] + impulse[1] * oldImpulse[1];
			double cosAngle = dot / (uLen * vLen);
			double angle = Math.acos(cosAngle);
			
			// Check for rotation.
			if(Math.sin(angle) >= Math.sin((Math.PI / 2) + (rotationOpeningAngle / 2))) {
				v.skew = v.skew + rotationSensitivity * Math.signum(Math.sin(angle));
			}
			
			// Check for oscillation or move in the right direction.
			if(Math.abs(Math.cos(angle)) >= Math.cos(oscillationOpeningAngle / 2)) {
				if(Math.cos(angle) > 0) { // Move in the right direction detected: increase temperature.
					v.temp = v.temp * oscillationSensitivity;
				} else { // Oscillation detected: decrease temperature.
					v.temp = v.temp / oscillationSensitivity;
				}
			}
			
			v.temp = v.temp * (1 - Math.abs(v.skew));
			v.temp = Math.min(v.temp, maxTemp);
		}
		
		v.impulse = impulse;
		
		// Add the node's temperature to the global temperature.
		globalTemp += v.temp;
	}
}
