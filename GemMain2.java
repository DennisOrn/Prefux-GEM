package fx;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.stage.Stage;
import prefux.FxDisplay;
import prefux.Visualization;
import prefux.action.ActionList;
import prefux.action.RepaintAction;
import prefux.action.layout.graph.GraphEmbedderLayout2;
import prefux.controls.GemControl2;
import prefux.data.Graph;
import prefux.data.Node;
import prefux.data.Table;
import prefux.data.Tuple;
import prefux.data.util.Point2D;
import prefux.render.ArrowRenderer;
import prefux.render.CombinedRenderer;
import prefux.render.DefaultRendererFactory;
import prefux.render.LabelRenderer;
import prefux.render.ShapeRenderer;
import prefux.visual.VisualItem;
import prefux.visual.VisualTupleSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.Filter;

public class GemMain2 extends Application {
	public static void main(String[] args) {
		launch(args);
	}
	
	private static final javafx.scene.paint.Color FILL_COLOR	= javafx.scene.paint.Color.DEEPSKYBLUE;
	private static final javafx.scene.paint.Color STROKE_COLOR	= javafx.scene.paint.Color.BLACK;
	private static final int STROKE_WIDTH						= 3;
	private static final String LABEL_FONT						= "Verdana";
	private static final FontPosture LABEL_FONT_POSTURE			= FontPosture.ITALIC;
	
	private static final double WIDTH = 1280;
	private static final double HEIGHT = 720;
	private static final String GROUP = "graph";
	
	// Table containing all the nodes.
	Table nodeTable = new Table();
	
	// Table containing all the edges.
	Table edgeTable = new Table();
	
	// List containing the OntClasses read from the ontology-file.
	List<OntClass> ontList = new ArrayList<>();
	
	// Map containing items and their labels.
	// Used to display the correct labels after zooming.
	private HashMap<VisualItem, Label> itemLabelMap = new HashMap<>();
	
	// The size of the labels.
	private double labelSize = 12;
	
	// The size of the labels when scaled to the zoom factor.
	private double labelSizeAdjusted;
	
	// The number of levels that are shown initially
	private int level = 5;
	
	// Variables used for the touch-functionality, zooming and rotating.
	private double startScale, startRotate;
    private boolean moveInProgress = false;
    private int touchPointId;
    private Point2D prevPos;
    
	@Override
	public void start(Stage primaryStage) {
		
		System.out.println("JavaFX version: " + com.sun.javafx.runtime.VersionInfo.getRuntimeVersion());
		
		primaryStage.setTitle("GEM-2");
		Pane root = new Pane();
		
		// Zoom out to fit the whole initial graph on the screen.
		root.setScaleX(0.05);
        root.setScaleY(0.05);
		
		//root.setScaleX(0.15);
        //root.setScaleY(0.15);
        
        labelSizeAdjusted = labelSize / root.getScaleX();
		
		root.setStyle("-fx-background-color: white;");
		primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
		root.getStyleClass().add("display");
		primaryStage.show();

		Graph graph = null;
		OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		
		try {
			
			// Read a specified ontology-file.
			//m.read("file:///C:\\Users\\valiv\\Desktop\\EclipseMarsWorkspace\\Prefux-master\\oaei2014_FMA_small_overlapping_nci.owl");
			
			m.read("file:///Users/dennisornberg/Desktop/oaei2014_FMA_small_overlapping_nci.owl");
			//m.read("file:///Users/dennisornberg/Desktop/oaei2014_NCI_small_overlapping_fma.owl");
			//m.read("file:///Users/dennisornberg/Desktop/oaei2014_FMA_small_overlapping_snomed.owl");
			//m.read("file:///Users/dennisornberg/Desktop/oaei2014_SNOMED_small_overlapping_fma.owl");
			//m.read("file:///Users/dennisornberg/Desktop/oaei2014_NCI_small_overlapping_snomed.owl");
			
			//m.read("file:///C:\\Users\\mazze\\Desktop\\datasets2\\oaei2014_FMA_small_overlapping_nci.owl");
			
			// Create an iterator for iterating over the ontology.
			Iterator<OntClass> it = m.listHierarchyRootClasses().filterDrop(new Filter<OntClass>() {
				public boolean accept(OntClass o) {
					return o.isAnon();
				}
			});

			// Add a column to nodeTable containing the name.
			nodeTable.addColumn("name", String.class);
			
			// Add two columns to edgeTable containing the source and target for all the edges.
			edgeTable.addColumn("source", int.class);
			edgeTable.addColumn("target", int.class);
			
			// Add all the ontology-entries to ontList.
			while(it.hasNext()) {
				OntClass cls = it.next();
				showClass(cls, new ArrayList<OntClass>(), 0);
			}
			
			// Add all the entries in ontList to the tables.
			for(int i = 0; i < ontList.size(); ++i) {
				
				OntClass cls = ontList.get(i);
				int index = nodeTable.addRow();
				nodeTable.set(index, 0, cls.getLocalName());
				
				for(it = cls.listSubClasses(true); it.hasNext();) {
					
					OntClass sub = it.next();
					index = edgeTable.addRow();
					edgeTable.set(index, 0, i);
					edgeTable.set(index, 1, ontList.indexOf(sub));
				}
			}
			
			System.out.println("Total number of nodes: " + nodeTable.getRowCount());
			System.out.println("Total number of edges: " + edgeTable.getRowCount());
			
			// Create a new graph using the two tables.
			graph = new Graph(nodeTable, edgeTable, true);
			
			// Create the visualization and add the graph.
			Visualization vis = new Visualization();
			vis.add(GROUP, graph);
			
			// Create the renderers.
			DefaultRendererFactory rfa = new DefaultRendererFactory();
			LabelRenderer lr = new LabelRenderer("name");
			ShapeRenderer sr = new ShapeRenderer();
			sr.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
			sr.setBaseSize(30);
			
			CombinedRenderer cr = new CombinedRenderer();
			cr.add(sr);
			cr.add(lr);
			
			rfa.setDefaultRenderer(cr);
			
			ArrowRenderer arrowRenderer = new ArrowRenderer();
			rfa.setDefaultEdgeRenderer(arrowRenderer);
			
			// The line below uses the original edge-renderer instead of the custom arrow-renderer.
			// The interaction is smoother with the edge-renderer since it is simpler.
			//rfa.setDefaultEdgeRenderer(new EdgeRenderer());
			
			vis.setRendererFactory(rfa);
			
			// Create a list of actions for the layout.
			ActionList layout = new ActionList(0, 0);
			
			// Add the algorithm.
			GraphEmbedderLayout2 algo = new GraphEmbedderLayout2("graph");
			layout.add(algo);
			layout.add(new RepaintAction());
			
			vis.putAction("layout", layout);
			
			// Create the display and add the visualization.
			FxDisplay display = new FxDisplay(vis);
			
			// Add the touch-functionality from GemControl to the display.
			display.addControlListener(new GemControl2());
			
			// Initialize all the nodes to set their colors and visibilities.
			initializeNodes(vis.getVisualGroup("graph"));
			
			// Find all the root nodes and then hide everything below a certain level.
			List<Node> rootNodes = new ArrayList<>();
			
			VisualTupleSet vts = vis.getVisualGroup("graph");
			Iterator<? extends Tuple> iterator = vts.tuples();
			
			while(iterator.hasNext()) {
				VisualItem item = (VisualItem) iterator.next();
				if(item instanceof Node && ((Node) item).getInDegree() == 0) {
					rootNodes.add((Node) item);
				}
			}
			
			for(Node n : rootNodes) {
				// Recursive method that hides nodes.
				hideNodes(n, 0);
			}
			
			// Add the display to the root-pane.
			root.getChildren().add(display);
			
			// Run the list of actions.
			vis.run("layout");
			
			
			
			/******************** TOUCH-FUNCTIONALITY ********************/
			
			/*
			 * This is used to move, rotate and zoom in and out on the graph.
			 * The functionality for selecting and moving nodes and collapsing
			 * / expanding is located in prefux.controls.GemControl.
			 */
			
			/* MOUSE */
			/*class Delta { double x, y; }
			final Delta dragDelta = new Delta();
			
			root.setOnMouseMoved(event -> {
				dragDelta.x = display.getLayoutX() - event.getX();
			    dragDelta.y = display.getLayoutY() - event.getY();
				event.consume();
			});
			
			root.setOnMouseDragged(event -> {
				display.setLayoutX(event.getX() + dragDelta.x);
				display.setLayoutY(event.getY() + dragDelta.y);
				event.consume();
			});*/
			/* END OF MOUSE */
			
			root.setOnTouchPressed(event -> {
				if(!moveInProgress) {
					moveInProgress = true;
					touchPointId = event.getTouchPoint().getId();
					prevPos = new Point2D(event.getTouchPoint().getX(), event.getTouchPoint().getY());
				}
				event.consume();
	        });
			
			root.setOnTouchMoved(event -> {
				if(moveInProgress && event.getTouchPoint().getId() == touchPointId) {
					Point2D currPos = new Point2D(event.getTouchPoint().getX(), event.getTouchPoint().getY());
					double[] translationVector = new double[2];
					
					translationVector[0] = currPos.getX() - prevPos.getX();
					translationVector[1] = currPos.getY() - prevPos.getY();
					
					display.setTranslateX(display.getTranslateX() + translationVector[0]);
					display.setTranslateY(display.getTranslateY() + translationVector[1]);
					prevPos = currPos;
				}
				event.consume();
	        });
			
			root.setOnTouchReleased(event -> {
				if(event.getTouchPoint().getId() == touchPointId) {
					moveInProgress = false;
				}
				event.consume();
	        });
			
			root.setOnZoomStarted(event -> {
				startScale = root.getScaleX();
				event.consume();
	        });
			
			root.setOnZoom(event -> {
				double totalZoomFactor = event.getTotalZoomFactor();
				root.setScaleX(startScale * totalZoomFactor);
				root.setScaleY(startScale * totalZoomFactor);
				event.consume();
	        });
			
			root.setOnZoomFinished(event -> {
				
				labelSizeAdjusted = labelSize / root.getScaleX();
				
				int degree;
				double scale = root.getScaleX();
				
				// Sets the degree of visibility based on the scale of root.
				if(scale <= 0.05) {
					degree = 10;
				} else if(scale > 0.05 && scale <= 0.1) {
					degree = 7;
				} else if(scale > 0.1 && scale <= 0.2) {
					degree = 5;
				} else if(scale > 0.2 && scale <= 0.3) {
					degree = 3;
				} else {
					degree = 0;
				}
				
				for(Map.Entry<VisualItem, Label> entry : itemLabelMap.entrySet()) {
			    
					// If a node has an equal or higher degree, its label will be visible.
					Node node = (Node) entry.getKey();
					if(node.getDegree() >= degree) {
						entry.getValue().setManaged(true);
						entry.getValue().setVisible(true);
						entry.getValue().setFont(Font.font(LABEL_FONT, LABEL_FONT_POSTURE, labelSizeAdjusted));
					} else {
						entry.getValue().setManaged(false);
						entry.getValue().setVisible(false);
					}
			    }
				
				event.consume();
	        });
	        
			// NOTE: the rotation-functionality is very slow with big graphs, it is commented below.
			
			/*root.setOnRotationStarted(event -> {
				startRotate = root.getRotate();
				event.consume();
	        });
			
			root.setOnRotate(event -> {
				root.setRotate(startRotate + event.getTotalAngle());
				event.consume();
	        });
			
			root.setOnRotationFinished(event -> {
				
				// Move the loop below to root.setOnRotate() to make the text
				// rotate in real time as the graph rotates.
				// NOTE: The bigger the graph, the slower this becomes.
				for(Map.Entry<VisualItem, Label> entry : itemLabelMap.entrySet()) {
					
					Label label = (Label) entry.getValue();
					label.setRotate(0 - root.getRotate());
				}
				
				event.consume();
	        });*/
			
			/***************** END OF TOUCH-FUNCTIONALITY ****************/
		}
		catch(org.apache.jena.shared.WrappedIOException e) {
			if(e.getCause() instanceof java.io.FileNotFoundException) {
				System.err.println("A java.io.FileNotFoundException caught: " + e.getCause().getMessage());
			}
		}
	}
	
	private void showClass(OntClass cls, List<OntClass> occurs, int depth) {
		
		if(!ontList.contains(cls)) {
			ontList.add(cls);
		}
		
		// Recurse to the next level.
		if(cls.canAs(OntClass.class) && !occurs.contains(cls)) {
			
			for(Iterator<OntClass> i = cls.listSubClasses(true); i.hasNext();) {
				
				OntClass sub = i.next();
				
				// This expression is added to 'occurs' before recursion.
				occurs.add(cls);
				
				// Recursive method-call
				showClass(sub, occurs, depth + 1);
				occurs.remove(cls);
			}
		}
	}
	
    /**
     * Initializes and modifies the looks of the nodes based on some criteria.
     * @param visualTupleSet the tuple set containing the visual items to be modified
     */
	private void initializeNodes(VisualTupleSet visualTupleSet) {
		
		Iterator<? extends Tuple> iterator = visualTupleSet.tuples();
		
		// Iterate over all the items.
		while(iterator.hasNext()) {
			
			VisualItem item = (VisualItem) iterator.next();
			Group group = (Group) item.getNode();
			ObservableList<javafx.scene.Node> groupList = group.getChildren();
			
			// Iterate over the group's children.
			for(javafx.scene.Node groupChild : groupList) {
				
				// If the child is a circle, modify it.
				if(groupChild instanceof Circle) {
					
					Circle circle = (Circle) groupChild;
					circle.setFill(FILL_COLOR);
					circle.setStroke(STROKE_COLOR);
					circle.setStrokeWidth(STROKE_WIDTH);
					
				} 
				
				// If the child is a StackPane, one of its children is the label.
				else if(groupChild instanceof StackPane) {
					
					StackPane stack = (StackPane) groupChild;
					ObservableList<javafx.scene.Node> stackList = stack.getChildren();
					for(javafx.scene.Node stackChild : stackList) {
						
						// If the child is a label, modify it.
						if(stackChild instanceof Label) {
							
							Label label = (Label) stackChild;
							label.setFont(Font.font(LABEL_FONT, LABEL_FONT_POSTURE, labelSizeAdjusted));
							
							// Set the labels visibility based on its degree.
							if(((prefux.data.Node) item).getDegree() < 10) {
								label.setManaged(false);
								label.setVisible(false);
							}
							
							itemLabelMap.put(item, label);
						}
					}
				}
			}
		}
	}
	
    /**
     * Recursive method that hides all the nodes that are below a certain level.
     * @param node the root node
     * @param depth the current depth (or level)
     */
	private void hideNodes(Node node, int depth) {
		
		Iterator<? extends Node> it = node.outNeighbors();
		while(it.hasNext()) {
			
			Node child = it.next();
			
			// Recursive method-call
			hideNodes(child, depth + 1);
			
			// If the current depth is as far down as we are going.
			if(depth == level) {
				
				// Hide the outgoing edges from this child.
				List<javafx.scene.Node> lines = GemControl2.getOutLines(child);
				for(javafx.scene.Node line : lines) {
					line.setVisible(false);
				}
				
				// If the child has children.
				Iterator<? extends Node> itChild = child.outNeighbors();
				if(itChild.hasNext()) {
					
					// Indicate that the child is collapsed.
					Circle circle = GemControl2.getCircle(child);
					if(circle != null) {
						circle.setFill(GemControl2.COLLAPSED_COLOR);
					}
					
					VisualItem item = (VisualItem) child;
					item.setExpanded(false);
				}
			}
			
			// If we are further down than 'level'.
			else if(depth > level) {
				
				// Set the visibility of everything to false.
				VisualItem item = (VisualItem) child;
				item.setVisible(false);
				
				Circle circle = GemControl2.getCircle(child);
				if(circle != null) {
					circle.setVisible(false);
				}
				
				Label label = GemControl2.getLabel(child);
				if(label != null) {
					label.setVisible(false);
				}
				
				List<javafx.scene.Node> lines = GemControl2.getOutLines(child);
				for(javafx.scene.Node line : lines) {
					line.setVisible(false);
				}
			}
		}
	}
}
