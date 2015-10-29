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

import javafx.geometry.Rectangle2D;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import prefux.action.layout.Layout;
import prefux.data.Graph;
import prefux.data.Node;
import prefux.data.Schema;
import prefux.data.Tuple;
import prefux.data.tuple.TupleSet;
import prefux.data.util.Point2D;
import prefux.util.PrefuseLib;
import prefux.util.force.DragForce;
import prefux.util.force.ForceItem;
import prefux.util.force.ForceSimulator;
import prefux.util.force.NBodyForce;
import prefux.util.force.SpringForce;
import prefux.visual.EdgeItem;
import prefux.visual.NodeItem;
import prefux.visual.VisualItem;

public class GraphEmbedderLayout extends Layout {
	
	/* -------------------- ADDED -------------------- */
	
	private List<Vertex> nodeList = new ArrayList<>();
	private boolean initialized = false;
	private int nrRounds;
	private int maxRounds;
	private double globalTemp = 2048;
	
	private final double upperBoundLocalTemp	= 256;
	private final double desiredMinTemp			= 3;
	private final double desiredEdgeLength		= 128;
	private final double gravitationalConstant	= 0.0625; // = 1 / 16
	
	private class Vertex {
		
		private final VisualItem item;
		private double impulse = 0;
		private double skew = 0;
		private double temp = 10;
		
		Vertex(VisualItem item) {
			this.item = item;
		}
		
		public VisualItem getItem() { return item; }
		public double getImpulse() { return impulse; }
		public double getSkew() { return skew; }
		public double getTemp() { return temp; }
	}
	
	/* ----------------------------------------------- */

	private ForceSimulator	       m_fsim;
	private long	               m_lasttime	= -1L;
	private long	               m_maxstep	= 50L;
	private boolean	               m_runonce;
	private int	                   m_iterations	= 100;
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
	 * Get the maximum timestep allowed for integrating node settings between
	 * runs of this layout. When computation times are longer than desired, and
	 * node positions are changing dramatically between animated frames, the max
	 * step time can be lowered to suppress node movement.
	 * 
	 * @return the maximum timestep allowed for integrating between two layout
	 *         steps.
	 */
	public long getMaxTimeStep() {
		return m_maxstep;
	}

	/**
	 * Set the maximum timestep allowed for integrating node settings between
	 * runs of this layout. When computation times are longer than desired, and
	 * node positions are changing dramatically between animated frames, the max
	 * step time can be lowered to suppress node movement.
	 * 
	 * @param maxstep
	 *            the maximum timestep allowed for integrating between two
	 *            layout steps
	 */
	public void setMaxTimeStep(long maxstep) {
		this.m_maxstep = maxstep;
	}

	/**
	 * Get the force simulator driving this layout.
	 * 
	 * @return the force simulator
	 */
	public ForceSimulator getForceSimulator() {
		return m_fsim;
	}

	/**
	 * Set the force simulator driving this layout.
	 * 
	 * @param fsim
	 *            the force simulator
	 */
	public void setForceSimulator(ForceSimulator fsim) {
		m_fsim = fsim;
	}

	/**
	 * Get the number of iterations to use when computing a layout in run-once
	 * mode.
	 * 
	 * @return the number of layout iterations to run
	 */
	public int getIterations() {
		return m_iterations;
	}

	/**
	 * Set the number of iterations to use when computing a layout in run-once
	 * mode.
	 * 
	 * @param iter
	 *            the number of layout iterations to run
	 */
	public void setIterations(int iter) {
		if (iter < 1)
			throw new IllegalArgumentException(
			        "Iterations must be a positive number!");
		m_iterations = iter;
	}

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
		System.out.println("Initializing algorithm...");
		
		// Place all nodes in random positions
		Iterator<VisualItem> iter = m_vis.visibleItems(m_nodeGroup);
		while (iter.hasNext()) {
			VisualItem item = iter.next();
			setX(item, referrer, (Math.random() * 1280));
			setY(item, referrer, (Math.random() * 720));
			
			//System.out.println(item.getSourceTuple());
			
			// Save all the nodes in a list for later use
			nodeList.add(new Vertex(item));
		}
		System.out.println("Nodes added to list: " + nodeList.size() + ".");
		
		maxRounds = nodeList.size() * 4;
		initialized = true;
		
		System.out.println("Initialization done.");
	}
	
	//private int iterCount = 0;
	/**
	 * @see prefux.action.Action#run(double)
	 */
	public void run(double frac) {
		
		//System.out.println(++iterCount);
		
		if(!initialized) {
			init();
		}
		
		// perform different actions if this is a run-once or
		// run-continuously layout
		
		// m_runonce is typically FALSE
		if (m_runonce) {
			//
		} else {
			//
		}
		
		//if(globalTemp > desiredMinTemp && nrRounds < maxRounds) {
		if(nrRounds < 1) {
		
			Collections.shuffle(nodeList);
			for(Vertex v : nodeList) {
				setX(v.getItem(), referrer, (Math.random() * 1280));
				setY(v.getItem(), referrer, (Math.random() * 720));
				/*for(int i = 0; i < 100000; ++i) {
					System.out.print("");
				}*/
				
				
				calculateImpulse(v);
			}
			double[] baryCenter = calculateBarycenter();
			System.out.println(baryCenter[0] + "," + baryCenter[1]);
			++nrRounds;
		}
	}
	
	private double calculateScalingFactor(Vertex v) {
		double deg = ((Node)v.getItem()).getDegree();
		return 1 + deg / 2;
	}
	
	private double[] calculateBarycenter() {
		double[] center = new double[2];
		center[0] = 0;
		center[1] = 0;
		for(Vertex v : nodeList) {
			center[0] += v.getItem().getX();
			center[1] += v.getItem().getY();
		}
		center[0] = center[0] / nodeList.size();
		center[1] = center[1] / nodeList.size();
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
		
		
		
		
		
		return new double[2];
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
