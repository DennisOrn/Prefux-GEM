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
import prefux.data.Schema;
import prefux.util.PrefuseLib;
import prefux.util.force.DragForce;
import prefux.util.force.ForceItem;
import prefux.util.force.ForceSimulator;
import prefux.util.force.NBodyForce;
import prefux.util.force.SpringForce;
import prefux.visual.EdgeItem;
import prefux.visual.VisualItem;

public class GraphEmbedderLayout extends Layout {
	
	/* -------------------- ADDED -------------------- */
	
	private List<Vertex> nodeList = new ArrayList<>();
	private boolean initialized = false;
	private int nrRounds = 0;
	private int maxRounds;
	private double globalTemp = 2048;
	private double[] sumPos = new double[2];
	
	private final double maxTemp					= 256;
	private final double desiredTemp				= 1;
	private final double desiredEdgeLength			= 128;
	private final double gravitationalConstant		= (double)1 / 16;
	
	private final double oscillationOpeningAngle	= Math.PI / 4;
	private final double rotationOpeningAngle		= Math.PI / 3;
	private final double oscillationSensitivity		= 1.1;
	private double rotationSensitivity;
	
	private class Vertex {
		
		private final VisualItem item;
		private double[] impulse = new double[2];
		private double skew = 0;
		private double temp = 100;
		
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
	
	/* ----------------------------------------------- */

	private ForceSimulator	       m_fsim;
	private boolean	               m_runonce;

	private boolean	               m_enforceBounds;

	protected transient VisualItem	referrer;

	protected String	           m_nodeGroup;
	protected String	           m_edgeGroup;
	
	private static final Logger log = LogManager.getLogger(GraphEmbedderLayout.class);


	/**
	 * Create a new GraphEmbedderLayout. By default, this layout will not
	 * restrict the layout to the layout bounds and will assume it is being run
	 * in animated (rather than run-once) fashion.
	 * 
	 * @param graph
	 *            the data group to layout. Must resolve to a Graph instance.
	 */
	public GraphEmbedderLayout(String graph) {
		this(graph, false, false);
	}

	/**
	 * Create a new GraphEmbedderLayout. The layout will assume it is being run
	 * in animated (rather than run-once) fashion.
	 * 
	 * @param group
	 *            the data group to layout. Must resolve to a Graph instance.
	 * @param enforceBounds
	 *            indicates whether or not the layout should require that all
	 *            node placements stay within the layout bounds.
	 */
	public GraphEmbedderLayout(String group, boolean enforceBounds) {
		this(group, enforceBounds, false);
	}

	/**
	 * Create a new GraphEmbedderLayout.
	 * 
	 * @param group
	 *            the data group to layout. Must resolve to a Graph instance.
	 * @param enforceBounds
	 *            indicates whether or not the layout should require that all
	 *            node placements stay within the layout bounds.
	 * @param runonce
	 *            indicates if the layout will be run in a run-once or animated
	 *            fashion. In run-once mode, the layout will run for a set
	 *            number of iterations when invoked. In animation mode, only one
	 *            iteration of the layout is computed.
	 */
	public GraphEmbedderLayout(String group, boolean enforceBounds,
	        boolean runonce) {
		super(group);
		m_nodeGroup = PrefuseLib.getGroupName(group, Graph.NODES);
		m_edgeGroup = PrefuseLib.getGroupName(group, Graph.EDGES);

		m_enforceBounds = enforceBounds;
		m_runonce = runonce;
		m_fsim = new ForceSimulator();
		m_fsim.addForce(new NBodyForce());
		m_fsim.addForce(new SpringForce());
		m_fsim.addForce(new DragForce());
	}

	/**
	 * Create a new GraphEmbedderLayout. The layout will assume it is being run
	 * in animated (rather than run-once) fashion.
	 * 
	 * @param group
	 *            the data group to layout. Must resolve to a Graph instance.
	 * @param fsim
	 *            the force simulator used to drive the layout computation
	 * @param enforceBounds
	 *            indicates whether or not the layout should require that all
	 *            node placements stay within the layout bounds.
	 */
	public GraphEmbedderLayout(String group, ForceSimulator fsim,
	        boolean enforceBounds) {
		this(group, fsim, enforceBounds, false);
	}

	/**
	 * Create a new GraphEmbedderLayout.
	 * 
	 * @param group
	 *            the data group to layout. Must resolve to a Graph instance.
	 * @param fsim
	 *            the force simulator used to drive the layout computation
	 * @param enforceBounds
	 *            indicates whether or not the layout should require that all
	 *            node placements stay within the layout bounds.
	 * @param runonce
	 *            indicates if the layout will be run in a run-once or animated
	 *            fashion. In run-once mode, the layout will run for a set
	 *            number of iterations when invoked. In animation mode, only one
	 *            iteration of the layout is computed.
	 */
	public GraphEmbedderLayout(String group, ForceSimulator fsim,
	        boolean enforceBounds, boolean runonce) {
		super(group);
		m_nodeGroup = PrefuseLib.getGroupName(group, Graph.NODES);
		m_edgeGroup = PrefuseLib.getGroupName(group, Graph.EDGES);

		m_enforceBounds = enforceBounds;
		m_runonce = runonce;
		m_fsim = fsim;
	}

	// ------------------------------------------------------------------------

	/**
	 * Explicitly sets the node and edge groups to use for this layout,
	 * overriding the group setting passed to the constructor.
	 * 
	 * @param nodeGroup
	 *            the node data group
	 * @param edgeGroup
	 *            the edge data group
	 */
	public void setDataGroups(String nodeGroup, String edgeGroup) {
		m_nodeGroup = nodeGroup;
		m_edgeGroup = edgeGroup;
	}

	// ------------------------------------------------------------------------

	private void init() {
		
		System.out.println("-------------------------------------");
		System.out.println("Initializing algorithm...");
		
		// Place all nodes in random positions
		Iterator<VisualItem> iter = m_vis.visibleItems(m_nodeGroup);
		while (iter.hasNext()) {
			VisualItem item = iter.next();
			
			double newX = (Math.random() * 1280);
			double newY = (Math.random() * 720);
			//newX = 1;
			//newY = 1;
			
			setX(item, referrer, (Math.random() * newX));
			setY(item, referrer, (Math.random() * newY));
			
			//System.out.println(item.getSourceTuple());
			
			sumPos[0] += newX;
			sumPos[1] += newY;
			
			nodeList.add(new Vertex(item));
		}
		System.out.println("Nodes added to list: " + nodeList.size() + ".");
		
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
		
		if(!initialized) {
			init();
		}
		
		if(globalTemp > desiredTemp && nrRounds < maxRounds) {
		//if(nrRounds < 200) {
			
			System.out.println("ROUND " + (nrRounds + 1));
		
			Collections.shuffle(nodeList);
			for(Vertex v : nodeList) {
				/*for(int i = 0; i < 100000; ++i) {
					System.out.print("");
				}*/
				
				// beware of double overflow somewhere, possibly?
				
				double[] imp = calculateImpulse(v);
				
				calculateTemperature(v, imp);
			}
			
			// Update the global temperature
			// This is horrible, change this to something better PLEASE
			// O(n) are you kidding me
			double temp = 0;
			for(Vertex v : nodeList) {
				temp += v.getTemp();
			}
			globalTemp = temp / nodeList.size();
			System.out.println("Global temperature: " + globalTemp);
			//System.out.println("-------------------------------------");
			
			++nrRounds;
		}
	}
	
	private double calculateScalingFactor(Vertex v) {
		double deg = ((Node)v.getItem()).getDegree();
		return 1 + deg / 2;
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
		
		impulse[0] = impulse[0] - v.getItem().getX();
		impulse[1] = impulse[1] - v.getItem().getY();
		
		double scalingFactor = calculateScalingFactor(v);
		impulse[0] = impulse[0] * gravitationalConstant * scalingFactor;
		impulse[1] = impulse[1] * gravitationalConstant * scalingFactor;
		
		// Random disturbance vector; default range: [-32,32] * [-32,32]
		
		impulse[0] += Math.random() * 40 - 20;
		impulse[1] += Math.random() * 40 - 20;
		
		
		
		
		
		/**************************************************/
		// For every node connected to v: calculate repulsive forces
		
		/*Iterator<? extends Edge> iter = ((Node)v.getItem()).edges(); // or maybe every node in the graph?
		while(iter.hasNext()) {
			Edge e = iter.next();
			VisualItem u = (VisualItem)e.getSourceNode();
			
			// Make sure u and v are not the same node
			
			if(u == v.getItem()) {
				u = (VisualItem)e.getTargetNode();
			}
			
			// Calculate repulsive forces
			
			double[] delta = new double[2];
			delta[0] = v.getItem().getX() - u.getX();
			delta[1] = v.getItem().getY() - u.getY();
			
			impulse[0] = impulse[0] + delta[0] * Math.pow(desiredEdgeLength, 2) / Math.pow(delta[0], 2);
			impulse[1] = impulse[1] + delta[1] * Math.pow(desiredEdgeLength, 2) / Math.pow(delta[1], 2);
		}*/
		/**************************************************/
		
		// For every node: calculate repulsive forces
		
		Iterator<VisualItem> iter = m_vis.visibleItems(m_nodeGroup);
		while (iter.hasNext()) {
			VisualItem u = iter.next();
			
			if(u != v.getItem()) {
				double[] delta = new double[2];
				delta[0] = v.getItem().getX() - u.getX();
				delta[1] = v.getItem().getY() - u.getY();
				
				impulse[0] = impulse[0] + delta[0]
						* Math.pow(desiredEdgeLength, 2) / Math.pow(delta[0], 2);
				impulse[1] = impulse[1] + delta[1]
						* Math.pow(desiredEdgeLength, 2) / Math.pow(delta[1], 2);
			}
		}
		
		/*
		 * Try this^ but with nodeList instead, faster?
		 */
		
		
		
		
		
		
		
		// For every node connected to v: calculate attractive forces
		
		Iterator<? extends Edge> it = ((Node)v.getItem()).edges();
		while(it.hasNext()) {
			Edge e = it.next();
			VisualItem u = (VisualItem)e.getSourceNode();
			
			// Make sure u and v are not the same node
			
			if(u == v.getItem()) {
				u = (VisualItem)e.getTargetNode();
			}
			
			// Calculate attractive forces
			
			double[] delta = new double[2];
			delta[0] = v.getItem().getX() - u.getX();
			delta[1] = v.getItem().getY() - u.getY();
			
			impulse[0] = impulse[0] - delta[0] * Math.pow(delta[0], 2)
					/ (Math.pow(desiredEdgeLength, 2) * scalingFactor);
			impulse[1] = impulse[1] - delta[1] * Math.pow(delta[1], 2)
					/ (Math.pow(desiredEdgeLength, 2) * scalingFactor);
		}
		
		return impulse;
	}
	
	public void calculateTemperature(Vertex v, double[] impulse) {
		
		// If the current impulse is not 0 
		if(impulse[0] != 0 || impulse[1] != 0) {
			
			// Scale the impulse with the current temperature
			impulse[0] = v.getTemp() * impulse[0]/Math.abs(impulse[0]);
			impulse[1] = v.getTemp() * impulse[1]/Math.abs(impulse[1]);
			
			double oldX = v.getItem().getX();
			double oldY = v.getItem().getY();
			
			v.getItem().setX(oldX + impulse[0]);
			v.getItem().setY(oldY + impulse[1]);
			
			// Update the sum of all node-coordinates
			sumPos[0] += impulse[0];
			sumPos[1] += impulse[1];
		}
		
		double[] oldImpulse = new double[2];
		oldImpulse[0] = v.getImpulse()[0];
		oldImpulse[1] = v.getImpulse()[1];
		
		// If the last impulse was not 0		
		if(oldImpulse[0] != 0 || oldImpulse[1] != 0) {
			
			// Calculate the angle between the last impulse and the current impulse
			double uLen = Math.sqrt(Math.pow(impulse[0], 2) + Math.pow(impulse[1], 2));
			double vLen = Math.sqrt(Math.pow(oldImpulse[0], 2) + Math.pow(oldImpulse[1], 2));
			double dot = impulse[0] * oldImpulse[0] + impulse[1] * oldImpulse[1];
			double cosAngle = dot / (uLen * vLen);
			double angle = Math.acos(cosAngle);
			
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
			v.setImpulse(impulse);
		} else {
			v.setImpulse(impulse);
		}
	}

	/**
	 * Get the mass value associated with the given node. Subclasses should
	 * override this method to perform custom mass assignment.
	 * 
	 * @param n
	 *            the node for which to compute the mass value
	 * @return the mass value for the node. By default, all items are given a
	 *         mass value of 1.0.
	 */
	protected double getMassValue(VisualItem n) {
		return 1.0f;
	}

	/**
	 * Get the spring length for the given edge. Subclasses should override this
	 * method to perform custom spring length assignment.
	 * 
	 * @param e
	 *            the edge for which to compute the spring length
	 * @return the spring length for the edge. A return value of -1 means to
	 *         ignore this method and use the global default.
	 */
	protected double getSpringLength(EdgeItem e) {
		return -1.;
	}

	/**
	 * Get the spring coefficient for the given edge, which controls the tension
	 * or strength of the spring. Subclasses should override this method to
	 * perform custom spring tension assignment.
	 * 
	 * @param e
	 *            the edge for which to compute the spring coefficient.
	 * @return the spring coefficient for the edge. A return value of -1 means
	 *         to ignore this method and use the global default.
	 */
	protected double getSpringCoefficient(EdgeItem e) {
		return -1.;
	}

	/**
	 * Get the referrer item to use to set x or y coordinates that are
	 * initialized to NaN.
	 * 
	 * @return the referrer item.
	 * @see prefux.util.PrefuseLib#setX(VisualItem, VisualItem, double)
	 * @see prefux.util.PrefuseLib#setY(VisualItem, VisualItem, double)
	 */
	public VisualItem getReferrer() {
		return referrer;
	}

	/**
	 * Set the referrer item to use to set x or y coordinates that are
	 * initialized to NaN.
	 * 
	 * @param referrer
	 *            the referrer item to use.
	 * @see prefux.util.PrefuseLib#setX(VisualItem, VisualItem, double)
	 * @see prefux.util.PrefuseLib#setY(VisualItem, VisualItem, double)
	 */
	public void setReferrer(VisualItem referrer) {
		this.referrer = referrer;
	}

	// ------------------------------------------------------------------------
	// ForceItem Schema Addition

	/**
	 * The data field in which the parameters used by this layout are stored.
	 */
	public static final String	FORCEITEM	     = "_forceItem";
	/**
	 * The schema for the parameters used by this layout.
	 */
	public static final Schema	FORCEITEM_SCHEMA	= new Schema();
	static {
		FORCEITEM_SCHEMA.addColumn(FORCEITEM, ForceItem.class, new ForceItem());
	}

} // end of class GraphEmbedderLayout
