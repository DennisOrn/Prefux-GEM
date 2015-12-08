package fx;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import prefux.Constants;
import prefux.FxDisplay;
import prefux.Visualization;
import prefux.action.ActionList;
import prefux.action.RepaintAction;
import prefux.action.assignment.ColorAction;
import prefux.action.assignment.DataColorAction;
import prefux.action.assignment.NodeDegreeSizeAction;
import prefux.action.layout.Layout;
import prefux.action.layout.graph.GraphEmbedderLayout;
import prefux.activity.Activity;
import prefux.controls.DragControl;
import prefux.controls.GemControl;
import prefux.data.Graph;
import prefux.data.Schema;
import prefux.data.Table;
import prefux.data.Tuple;
import prefux.data.expression.Predicate;
import prefux.data.expression.parser.ExpressionParser;
import prefux.data.util.Point2D;
import prefux.data.util.Rectangle2D;
import prefux.render.ArrowRenderer;
import prefux.render.CombinedRenderer;
import prefux.render.DefaultRendererFactory;
import prefux.render.EdgeRenderer;
import prefux.render.LabelRenderer;
import prefux.render.NullRenderer;
import prefux.render.ShapeRenderer;
import prefux.render.StackPaneRenderer;
import prefux.util.ColorLib;
import prefux.util.FontLib;
import prefux.util.PrefuseLib;
import prefux.visual.DecoratorItem;
import prefux.visual.VisualItem;
import prefux.visual.VisualTupleSet;
import prefux.visual.expression.InGroupPredicate;
import prefux.visual.expression.VisiblePredicate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

public class GemTest extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	private static final double WIDTH = 1280;
	private static final double HEIGHT = 720;
	private static final String GROUP = "graph";
	
	Table nodeTable = new Table();
	Table edgeTable = new Table();
	List<OntClass> ontList = new ArrayList<>();
	//double lastX, lastY;
	
	private VisualTupleSet visualTupleSet;
	private List<Label> labels = new ArrayList<>();
	private double labelSize = 16;
	private double labelSizeAdjusted;
	
	private double startScale, startRotate;
    private boolean moveInProgress = false;
    private int touchPointId;
    private Point2D prevPos;
    
    private double zoomValue = 1;
    private double zoomFactor = 1;
    private Point2D anchor;
    private Point2D oldAnchor;
    
	@Override
	public void start(Stage primaryStage) {

		primaryStage.setTitle("GEM");
		Pane root = new Pane();
		
		root.setScaleX(0.05);
        root.setScaleY(0.05);
		//root.setScaleX(0.25);
        //root.setScaleY(0.25);
        
        /*****/
        labelSizeAdjusted = labelSize / root.getScaleX();
        /*****/
		
		root.setStyle("-fx-background-color: white;");
		primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
		root.getStyleClass().add("display");
		primaryStage.show();

		Graph graph = null;
		
		OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		
		try {
			
			//m.read("file:///C:\\Users\\valiv\\Desktop\\EclipseMarsWorkspace\\Prefux-master\\oaei2014_FMA_small_overlapping_nci.owl");
			m.read("file:///Users/dennisornberg/Desktop/datasets2/oaei2014_FMA_small_overlapping_nci.owl");
			//m.read("file:///C:\\Users\\mazze\\Desktop\\datasets2\\oaei2014_FMA_small_overlapping_nci.owl");
			
			/*for(OntClass cls : m.listClasses().toList()) {
				System.out.println(cls);
			}*/
			//System.out.println(m.size());
			
			Iterator<OntClass> it = m.listHierarchyRootClasses().filterDrop(new Filter<OntClass>() {
				public boolean accept(OntClass o) {
					return o.isAnon();
				}
			});

			nodeTable.addColumn("name", String.class);
			
			edgeTable.addColumn("source", int.class);
			edgeTable.addColumn("target", int.class);
			
			while(it.hasNext()) {
				OntClass cls = it.next();
				showClass(cls, new ArrayList<OntClass>(), 0);
			}
			
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
			
			
			System.out.println("DONE");
			System.out.println("ontList.size(): " + ontList.size());
			System.out.println("nodeTable: " + nodeTable);
			System.out.println("edgeTable: " + edgeTable);
			
			
			
			graph = new Graph(nodeTable, edgeTable, false); // TRUE?
			
			
			// graph = new GraphMLReader().readGraph("data/graphml-sample.xml");
			//graph = new GraphMLReader().readGraph("data/socialnet2.xml");
			Visualization vis = new Visualization();
			vis.add(GROUP, graph);

			ShapeRenderer female = new ShapeRenderer();
			female.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
			//LabelRenderer lr = new LabelRenderer("name");
			ShapeRenderer male = new ShapeRenderer();
			male.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
			
			
			
			
			
			
			
			
			
			
			
			

			// create a new default renderer factory
			// return our name label renderer as the default for all
			// non-EdgeItems
			// includes straight line edges for EdgeItems by default
			DefaultRendererFactory rfa = new DefaultRendererFactory();
			//Predicate expMale = ExpressionParser.predicate("gender='M'");// REMOOOOOOOOOOOOOOOOOVE (?)
			//Predicate expFemale = ExpressionParser.predicate("gender='F'");
			//rfa.add(expMale, male);
			//rfa.add(expFemale, female);
			
			/*****/
			//Predicate visible = new VisiblePredicate();
			//rfa.add(visible, lr);
			
			//Predicate pred = (Predicate)ExpressionParser.parse("childcount()==0");
			//rfa.add(pred, lr);
			
			//rfa.add(new InGroupPredicate("labels"), new LabelRenderer("name"));
			/*****/
			
			
			LabelRenderer lr = new LabelRenderer("name");
			//lr.setHorizontalAlignment(Constants.LEFT);
			
			Predicate pVisible		= (Predicate)ExpressionParser.parse("VISIBLE()");
			Predicate pNotVisible	= (Predicate)ExpressionParser.parse("!VISIBLE()");
			
			//rfa.add(pVisible, r);
			//rfa.add(pVisible, new ShapeRenderer());
			ShapeRenderer sr = new ShapeRenderer();
			//sr.setFillMode(ShapeRenderer.SOLID);
			//sr.setRenderType(ShapeRenderer.RENDER_TYPE_DRAW_AND_FILL);
			sr.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
			sr.setBaseSize(30);
			
			//lr.setHorizontalPadding(50);
			
			EdgeRenderer er = new EdgeRenderer();
			CombinedRenderer cr = new CombinedRenderer();
			NullRenderer nr = new NullRenderer();
			cr.add(sr);
			cr.add(lr);
			
			//rfa.add(pVisible, cr);
			//rfa.add(pVisible, er);
			
			
			//rfa.add(pVisible, sr);
			//rfa.add(pVisible, er);
			//vis.setVisible("graph.nodes", null, false);
			//vis.setVisible("graph.edges", null, false);
			
			//vis.addDecorators("labels", "graph.nodes", pVisible, LABEL_SCHEMA);
			
			//rfa.add(pVisible, er);
			rfa.setDefaultRenderer(cr);
			//rfa.setDefaultEdgeRenderer(er);
			ArrowRenderer arrowRenderer = new ArrowRenderer();
			//rfa.setDefaultEdgeRenderer(arrowRenderer);
			rfa.setDefaultEdgeRenderer(er);
			
			//rfa.add(new InGroupPredicate("NODE_DECORATORS"), lr);
			
			//vis.setRendererFactory(rfa);
			vis.setRendererFactory(rfa);
			
			//DECORATOR_SCHEMA.setDefault(VisualItem.TEXTCOLOR, ColorLib.gray(128));
	        //vis.addDecorators("NODE_DECORATORS", "graph.nodes", DECORATOR_SCHEMA);
			

			//ActionList layout = new ActionList(Activity.INFINITY, 30);
			ActionList layout = new ActionList(Activity.INFINITY, 0);
			GraphEmbedderLayout algo = new GraphEmbedderLayout("graph");
			layout.add(algo);
			
			/*****/
			
			//layout.add(new LabelLayout2("NODE_DECORATORS"));
			
			/*****/
			
			layout.add(new RepaintAction());
			
			vis.putAction("layout", layout);
			
			ActionList nodeActions = new ActionList();
			final String NODES = PrefuseLib.getGroupName(GROUP, Graph.NODES);
			NodeDegreeSizeAction size = new NodeDegreeSizeAction(NODES);
			nodeActions.add(size);
			
			//DataColorAction color = new DataColorAction(NODES, "name", Constants.NOMINAL, VisualItem.FILLCOLOR, palette);
			//nodeActions.add(color);
			
			/*****/
			
			ColorAction nodeColor = new ColorAction("graph.nodes", VisualItem.FILLCOLOR, ColorLib.rgb(72, 61, 139));
			nodeActions.add(nodeColor);
			
			ColorAction textColor = new ColorAction("graph.nodes", VisualItem.TEXTCOLOR, ColorLib.rgb(0, 255, 255));
			nodeActions.add(textColor);
			
			/*****/
			
			vis.putAction("nodes", nodeActions);
			
			FxDisplay display = new FxDisplay(vis);
			
			display.addControlListener(new GemControl());
			
			initializeNodes(vis.getVisualGroup("graph"));
			
			root.getChildren().add(display);
			
			vis.run("nodes");
			vis.run("layout");
			
			
			
			/******************** TOUCH-FUNCTIONALITY ********************/
			
			/* Mouse */
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
			/* Mouse */
			
			root.setOnTouchPressed(event -> {
				if(moveInProgress == false) {
					moveInProgress = true;
					touchPointId = event.getTouchPoint().getId();
					prevPos = new Point2D(event.getTouchPoint().getX(), event.getTouchPoint().getY());
				}
				event.consume();
	        });
			
			root.setOnTouchMoved(event -> {
				if(moveInProgress == true && event.getTouchPoint().getId() == touchPointId) {
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
				root.setScaleX(startScale * event.getTotalZoomFactor());
				root.setScaleY(startScale * event.getTotalZoomFactor());
				event.consume();
	        });
			
			root.setOnZoomFinished(event -> {
				// TODO: if zoom factor is [SOMETHING]: show more labels?
				labelSizeAdjusted = labelSize / root.getScaleX();
				for(Label label : labels) {
					label.setFont(new Font(labelSizeAdjusted));
				}
				event.consume();
	        });
	        
			root.setOnRotationStarted(event -> {
				startRotate = root.getRotate();
				event.consume();
	        });
			
			root.setOnRotate(event -> {
				root.setRotate(startRotate + event.getTotalAngle());
				
				/*
					Rotating labels in real-time.
					Move loop to root.setOnRotationFinished() to rotate
					labels only when graph-rotation is finished.
				*/
				/*for(Label label : labels) {
					label.setRotate(0 - root.getRotate());
				}*/
				
				event.consume();
	        });
			
			root.setOnRotationFinished(event -> {
				// TODO: only rotate visible labels?
				for(Label label : labels) {
					label.setRotate(0 - root.getRotate());
				}
				// TODO: rotation offsets the label, NOT GOOD! It rotates around the center of the label (?)
				event.consume();
	        });
			
			/******************** TOUCH-FUNCTIONALITY ********************/
		}
		/*catch (DataIOException e) {
			e.printStackTrace();
			System.err.println("Error loading graph. Exiting...");
			System.exit(1);
		}*/
		catch (com.hp.hpl.jena.shared.WrappedIOException e) {
			if (e.getCause() instanceof java.io.FileNotFoundException) {
				System.err.println("A java.io.FileNotFoundException caught: " 
						+ e.getCause().getMessage());
			}
		}
	}
	
	private void showClass(OntClass cls, List<OntClass> occurs, int depth) {
		//renderClassDescription(cls, depth);
		//System.out.println();
		
		if(!ontList.contains(cls)) {
			ontList.add(cls);
		}
		
		// recurse to the next level down
		if (cls.canAs(OntClass.class) && !occurs.contains(cls)) {
			for (Iterator<OntClass> i = cls.listSubClasses(true); i.hasNext();) {
				OntClass sub = i.next();
				
				// we push this expression on the occurs list before we recurse
				occurs.add(cls);
				showClass(sub, occurs, depth + 1);
				occurs.remove(cls);
			}
		}
	}
	
	private void initializeNodes(VisualTupleSet visualTupleSet) {
		
		Iterator<? extends Tuple> iterator = visualTupleSet.tuples();
		while(iterator.hasNext()) {
			
			VisualItem item = (VisualItem) iterator.next();
			System.out.println(item.getNode());
			if(item.getNode() instanceof Group) {
				
				Group group = (Group) item.getNode();
				ObservableList<javafx.scene.Node> groupList = group.getChildren();
				for(javafx.scene.Node groupChild : groupList) {
					
					System.out.print("    ");
					System.out.println(groupChild);
					if(groupChild instanceof Circle) {
						
						Circle circle = (Circle) groupChild;
						circle.setFill(javafx.scene.paint.Color.LIGHTSEAGREEN);
						circle.setStroke(javafx.scene.paint.Color.BLACK);
						circle.setStrokeWidth(3);
						//circle.setVisible(false);
						//circle.setRadius(80);
						// TODO: set the size of the nodes according to the degree.
						
					} else if(groupChild instanceof StackPane) {
						
						StackPane stack = (StackPane) groupChild;
						ObservableList<javafx.scene.Node> stackList = stack.getChildren();
						for(javafx.scene.Node stackChild : stackList) {
							
							System.out.print("        ");
							System.out.println(stackChild);
							if(stackChild instanceof Label) {
								
								Label label = (Label) stackChild;
								label.setFont(new Font(labelSizeAdjusted));
								if(((prefux.data.Node) item).getDegree() < 10) {
									
									//label.setVisible(false);
									label.setManaged(false);
									//label.setFont(new Font(0));
									// TODO: COLLAPSE AND HIDE CHILDREN AS WELL?
								}
								
								labels.add(label);
							}
						}
					}
				}
			} else if(item.getNode() instanceof Line) {
				// TODO: do something with the lines?
			}
		}
	}
}
