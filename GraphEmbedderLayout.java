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
import prefux.util.ColorLib;
import prefux.util.PrefuseLib;
import prefux.visual.VisualItem;

public class GraphEmbedderLayout extends Layout {
	
	private List<Vertex> nodeList = new ArrayList<>();
	private boolean initialized = false;
	private int nrRounds = 0;
	private int maxRounds;
	private double globalTemp = 1024;
	private double[] sumPos = new double[2];
	
	private final double maxTemp					= 256;
	private final double desiredTemp				= 50;
	private final double desiredEdgeLength			= 256;
	//private final double gravitationalConstant		= (double)1 / 16;
	private final double gravitationalConstant		= (double)1 / 16;
	
	private final double oscillationOpeningAngle	= Math.PI / 4;
	private final double rotationOpeningAngle		= Math.PI / 3;
	private final double oscillationSensitivity		= 1.1;
	private double rotationSensitivity;
	
	private class Vertex {
		
		private final VisualItem item;
		private double[] impulse = new double[2];
		private double skew = 0;
		private double temp = 256;
		
		public double x;
		public double y;
		public List<Vertex> neighbors = new ArrayList<>();
		
		Vertex(VisualItem item) {
			this.item = item;
			impulse[0] = 0;
			impulse[1] = 0;
		}
		
		public VisualItem getItem() { return item; }
		
		public void setImpulse(double[] impulse) {
			this.impulse[0] = impulse[0];
			this.impulse[1] = impulse[1];
		}
		
		public void setSkew(double skew) { this.skew = skew; }
		public void setTemp(double temp) { this.temp = temp; }
		
		public double[] getImpulse() { return impulse; }
		public double getSkew() { return skew; }
		public double getTemp() { return temp; }
	}

	protected transient VisualItem referrer;
	protected String m_nodeGroup;
	protected String m_edgeGroup;
	private static final Logger log = LogManager.getLogger(GraphEmbedderLayout.class);
	
	/**
	 * Create a new GraphEmbedderLayout.
	 * 
	 * @param graph
	 *            the data group to layout. Must resolve to a Graph instance.
	 */
	public GraphEmbedderLayout(String graph) {
		super(graph);
		m_nodeGroup = PrefuseLib.getGroupName(graph, Graph.NODES);
		m_edgeGroup = PrefuseLib.getGroupName(graph, Graph.EDGES);
	}
	
	private void init() {
		
		System.out.println("-------------------------------------");
		System.out.println("Initializing algorithm...");
		
		// Place all nodes in random positions and add them to nodeList
		Iterator<VisualItem> iter = m_vis.visibleItems(m_nodeGroup);
		while (iter.hasNext()) {
			VisualItem item = iter.next();
			
			double newX = (Math.random() * 2048) - 1024;
			double newY = (Math.random() * 2048) - 1024;
			
			item.setX(newX);
			item.setY(newY);
			
			sumPos[0] += newX;
			sumPos[1] += newY;
			
			Vertex v = new Vertex(item);
			v.x = item.getX();
			v.y = item.getY();
			nodeList.add(v);
			
			item.setSize(item.getSize() * 3);
		}
		
		System.out.println("Nodes added to list: " + nodeList.size() + ".");
		
		// Make sure the neighbors are added to every node
		for(Vertex v : nodeList) {
			Iterator<? extends Edge> it = ((Node)v.getItem()).edges();
			while(it.hasNext()) {
				Edge e = it.next();
				VisualItem u = (VisualItem)e.getSourceNode();
				
				// Make sure u and v are not the same node
				
				if(u == v.getItem()) {
					u = (VisualItem)e.getTargetNode();
				}
				
				for(Vertex v2 : nodeList) {
					if(u == v2.getItem()) {
						v.neighbors.add(v2);
					}
				}
			}
		}
		
		/*for(Vertex v : nodeList) {
			System.out.println(v.neighbors.size());
		}*/
		
		System.out.println("Edges added to vertices.");
		
		
		
		
		
		maxRounds = nodeList.size() * 4;
		System.out.println("maxRounds set to: " + maxRounds + ".");
		
		rotationSensitivity = (double)1 / (2 * nodeList.size());
		System.out.println("rotationSensitivity set to: " + rotationSensitivity + ".");
		
		initialized = true;
		
		System.out.println("Initialization done.");
		System.out.println("-------------------------------------");
	}
	
	/**
	 * @see prefux.action.Action#run(double)
	 */
	public void run(double frac) {
		
		long startTime = System.nanoTime();
		
		if(!initialized) {
			init();
		}
		
		//FxDisplay display = (FxDisplay)getVisualization().getDisplay(0);
		
		//if(globalTemp > desiredTemp && nrRounds < maxRounds) {
		while(globalTemp > desiredTemp && nrRounds < maxRounds) {
		//while(nrRounds < 1) {
			
			System.out.println("ROUND " + (nrRounds + 1));
			
			// reset the global temperature
			globalTemp = 0;
		
			Collections.shuffle(nodeList);
			for(Vertex v : nodeList) {
				double[] imp = calculateImpulse(v);
				calculateTemperature(v, imp);
			}
			
			globalTemp = globalTemp / nodeList.size();
			System.out.println("Global temperature: " + globalTemp);
			
			++nrRounds;
			
			System.out.println("Time elapsed: " + (System.nanoTime() - startTime) / 1000000000 + "s");
			
			// Update the graph every n rounds
			int n = maxRounds;
			if(nrRounds % n == 0 || globalTemp < desiredTemp) {
				for(Vertex v : nodeList) {
					v.getItem().setX(v.x);
					v.getItem().setY(v.y);
					if(globalTemp < desiredTemp) {
						v.getItem().setFillColor(ColorLib.rgb(0, 150, 0));
					}
				}
			}
		}
	}
	
	private double calculateScalingFactor(Vertex v) {
		return 1 + v.neighbors.size() / 2;
	}
	
	private double[] calculateBarycenter() {
		double[] center = new double[2];
		center[0] = sumPos[0] / nodeList.size();
		center[1] = sumPos[1] / nodeList.size();
		return center;
	}
	
	private double[] calculateImpulse(Vertex v) {
		
		// Attraction to center of gravity
		double[] impulse = new double[2];
		impulse = calculateBarycenter();
		
		impulse[0] = impulse[0] - v.x;
		impulse[1] = impulse[1] - v.y;
		
		double scalingFactor = calculateScalingFactor(v);
		impulse[0] = impulse[0] * gravitationalConstant * scalingFactor;
		impulse[1] = impulse[1] * gravitationalConstant * scalingFactor;
		
		// Random disturbance vector; default range: [-32,32] * [-32,32]
		impulse[0] += Math.random() * 40 - 20;
		impulse[1] += Math.random() * 40 - 20;
		
		double desSquared = Math.pow(desiredEdgeLength, 2);
		
		// For every node in the graph: calculate repulsive forces
		for(Vertex u : nodeList) {
			
			// If u and v are the same: skip iteration
			if(u.getItem() == v.getItem()) {
				continue;
			}
			
			double[] delta = new double[2];
			delta[0] = v.x - u.x;
			delta[1] = v.y - u.y;
			
			double length = Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1]);
			
			if(length != 0) {
				double lengthSquared = Math.pow(length, 2);
				impulse[0] = impulse[0] + delta[0] * desSquared / lengthSquared;
				impulse[1] = impulse[1] + delta[1] * desSquared / lengthSquared;
			}
		}
		
		// For every node connected to v: calculate attractive forces
		for(Vertex u : v.neighbors) {
			double[] delta = new double[2];
			
			delta[0] = v.x - u.x;
			delta[1] = v.y - u.y;
			
			double length = Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1]);
			double lengthSquared = Math.pow(length, 2);
			
			impulse[0] = impulse[0] - delta[0] * lengthSquared / (desSquared * scalingFactor);
			impulse[1] = impulse[1] - delta[1] * lengthSquared / (desSquared * scalingFactor);
		}
		
		return impulse;
	}
	
	private void calculateTemperature(Vertex v, double[] impulse) {
		
		// If the current impulse is not 0 
		if(impulse[0] != 0 || impulse[1] != 0) {
			
			// Scale the impulse with the current temperature
			double length = Math.sqrt(impulse[0] * impulse[0] + impulse[1] * impulse[1]);
			impulse[0] = v.getTemp() * impulse[0] / length;
			impulse[1] = v.getTemp() * impulse[1] / length;
			
			// Update v's coordinates
			v.x += impulse[0];
			v.y += impulse[1];
			
			// Update the sum of all node-coordinates
			sumPos[0] += impulse[0];
			sumPos[1] += impulse[1];
		}
		
		double[] oldImpulse = v.getImpulse();
		
		// If the last impulse was not 0		
		if(oldImpulse[0] != 0 || oldImpulse[1] != 0) {
			
			// Calculate the angle between the last impulse and the current impulse
			double uLen = Math.sqrt(impulse[0] * impulse[0] + impulse[1] * impulse[1]);
			double vLen = Math.sqrt(oldImpulse[0] * oldImpulse[0] + oldImpulse[1] * oldImpulse[1]);
			double dot = impulse[0] * oldImpulse[0] + impulse[1] * oldImpulse[1];
			double cosAngle = dot / (uLen * vLen);
			double angle = Math.acos(cosAngle);
			
			/*
			 * angle = NaN?
			 */
			
			// Check for rotation
			if(Math.sin(angle) >= Math.sin((Math.PI / 2) + (rotationOpeningAngle / 2))) {
				v.setSkew(v.getSkew() + rotationSensitivity * Math.signum(Math.sin(angle)));
			}
			
			// Check for oscillation OR move in the right direction
			if(Math.abs(Math.cos(angle)) >= Math.cos(oscillationOpeningAngle / 2)) {
				if(Math.cos(angle) > 0) { // move in the right direction
					v.setTemp(v.getTemp() * oscillationSensitivity);
				} else { // oscillation
					v.setTemp(v.getTemp() / oscillationSensitivity);
				}
			}
			
			v.setTemp(v.getTemp() * (1 - Math.abs(v.getSkew())));
			v.setTemp(Math.min(v.getTemp(), maxTemp));
		}
		v.setImpulse(impulse);
		globalTemp += v.getTemp();
	}

} // end of class GraphEmbedderLayout
