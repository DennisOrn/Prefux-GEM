package fx;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import prefux.Constants;
import prefux.FxDisplay;
import prefux.Visualization;
import prefux.action.ActionList;
import prefux.action.RepaintAction;
import prefux.action.assignment.DataColorAction;
import prefux.action.assignment.NodeDegreeSizeAction;
import prefux.action.layout.graph.GraphEmbedderLayout;
import prefux.activity.Activity;
import prefux.data.Graph;
import prefux.data.Table;
import prefux.data.expression.Predicate;
import prefux.data.expression.parser.ExpressionParser;
import prefux.data.util.Point2D;
import prefux.render.DefaultRendererFactory;
import prefux.render.LabelRenderer;
import prefux.render.ShapeRenderer;
import prefux.util.ColorLib;
import prefux.util.PrefuseLib;
import prefux.visual.VisualItem;

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
	
	private double startScale, startRotate;
    private boolean moveInProgress = false;
    private int touchPointId;
    private Point2D prevPos;

	@Override
	public void start(Stage primaryStage) {

		primaryStage.setTitle("GEM");
		Pane root = new Pane();
		primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
		root.getStyleClass().add("display");
		primaryStage.show();

		Graph graph = null;
		
		OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		
		try {
			
			m.read("file:///C:\\Users\\mazze\\Desktop\\datasets2\\oaei2014_FMA_small_overlapping_nci.owl");
			
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
			
			
			
			graph = new Graph(nodeTable, edgeTable, false);
			
			
			// graph = new GraphMLReader().readGraph("data/graphml-sample.xml");
			//graph = new GraphMLReader().readGraph("data/socialnet2.xml");
			Visualization vis = new Visualization();
			vis.add(GROUP, graph);

			ShapeRenderer female = new ShapeRenderer();
			female.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
			LabelRenderer lr = new LabelRenderer("name");
			ShapeRenderer male = new ShapeRenderer();
			male.setFillMode(ShapeRenderer.GRADIENT_SPHERE);

			// create a new default renderer factory
			// return our name label renderer as the default for all
			// non-EdgeItems
			// includes straight line edges for EdgeItems by default
			DefaultRendererFactory rfa = new DefaultRendererFactory();
			Predicate expMale = ExpressionParser.predicate("gender='M'");// REMOOOOOOOOOOOOOOOOOVE (?)
			Predicate expFemale = ExpressionParser.predicate("gender='F'");
			rfa.add(expMale, male);
			rfa.add(expFemale, female);
			vis.setRendererFactory(rfa);

			ActionList layout = new ActionList(Activity.INFINITY,30);
			GraphEmbedderLayout algo = new GraphEmbedderLayout("graph");
			layout.add(algo);
			layout.add(new RepaintAction());
			vis.putAction("layout", layout);

			ActionList nodeActions = new ActionList();
			final String NODES = PrefuseLib.getGroupName(GROUP, Graph.NODES);
			// DataSizeAction size = new DataSizeAction(NODES, "age");
			// nodeActions.add(size);
			NodeDegreeSizeAction size = new NodeDegreeSizeAction(NODES);
			nodeActions.add(size);
			int[] malePalette = new int[] {
					ColorLib.rgb(150, 0, 0)
			};

			/*DataColorAction colorF = new DataColorAction(NODES, expFemale, "age",
			        Constants.NUMERICAL, VisualItem.FILLCOLOR, femalePalette);
			DataColorAction colorM = new DataColorAction(NODES, expMale, "age",
			        Constants.NUMERICAL, VisualItem.FILLCOLOR, malePalette);
			nodeActions.add(colorF);
			nodeActions.add(colorM);*/
			
			DataColorAction color = new DataColorAction(NODES, "name", Constants.NOMINAL, VisualItem.FILLCOLOR, malePalette);
			nodeActions.add(color);
			
			vis.putAction("nodes", nodeActions);

			FxDisplay display = new FxDisplay(vis);
			//display.addControlListener(new DragControl());
			
			//display.addControlListener(new ZoomControl(display));///////////////////
			
			//root.setCenter(display);
			//root.setContent(display);
			root.getChildren().addAll(display);
			
			vis.run("nodes");
			vis.run("layout");
			
			
			
			/******************** TOUCH-FUNCTIONALITY ********************/
			root.setOnScroll(event -> {
	            double zoomFactor = 1.05;
	            double deltaY = event.getDeltaY();
	            if(deltaY < 0) {
	            	zoomFactor = 2.0 - zoomFactor;
	            }
	            
	            root.setScaleX(root.getScaleX() * zoomFactor);
	            root.setScaleY(root.getScaleY() * zoomFactor);
	            
	            event.consume();
			});
			
			root.setOnTouchPressed(event -> {
				if(moveInProgress == false) {
					moveInProgress = true;
					touchPointId = event.getTouchPoint().getId();
					prevPos = new Point2D(event.getTouchPoint().getSceneX(), event.getTouchPoint().getSceneY());
				}
				event.consume();
	        });
			
			root.setOnTouchMoved(event -> {
				if(moveInProgress == true && event.getTouchPoint().getId() == touchPointId) {
					Point2D currPos = new Point2D(event.getTouchPoint().getSceneX(), event.getTouchPoint().getSceneY());
					double[] translationVector = new double[2];
					
					translationVector[0] = currPos.getX() - prevPos.getX();
					translationVector[1] = currPos.getY() - prevPos.getY();
					
					root.setTranslateX(root.getTranslateX() + translationVector[0]);
					root.setTranslateY(root.getTranslateY() + translationVector[1]);
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
	        
			root.setOnRotationStarted(event -> {
				startRotate = root.getRotate();
				event.consume();
	        });
			
			root.setOnRotate(event -> {
				root.setRotate(startRotate + event.getTotalAngle());
				event.consume();
	        });
			/******************** TOUCH-FUNCTIONALITY ********************/
		}
		/*catch (DataIOException e) {
			e.printStackTrace();
			System.err.println("Error loading graph. Exiting...");
			System.exit(1);
		}*/ catch (com.hp.hpl.jena.shared.WrappedIOException e) {
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
	
	private void renderClassDescription(OntClass c, int depth) {
		indent(depth);
		
		if (!c.isRestriction() && !c.isAnon()) {
			System.out.println("classname : " + c.getLocalName());
			
			/********************************************************************************/
			/*int index = nodeTable.addRow();
			nodeTable.set(index, 0, c.getLocalName());*/
			/********************************************************************************/
			
			// list the instances for this class
			showInstance(c, depth + 2);
		}
	}
	
	private void showInstance(OntClass cls, int depth) {
		
		for(ExtendedIterator<? extends OntResource> iter = cls.listInstances(true); iter.hasNext();){
			indent(depth);
			OntResource instance = iter.next();
			System.out.println("instance : " + instance.getLocalName());
		}
		
	}
	
	private void indent(int depth) {
		for (int i = 0; i < depth; ++i) {
			System.out.print("  ");
		}
	}
	
	
	
	
	
	
	private Node buildControlPanel(FxDisplay display) {
		VBox vbox = new VBox();
		Label txt = new Label("Zoom Factor");
		Slider slider = new Slider(0.0, 10.0, 0.08);
		//slider.setMaxWidth(900);
		display.zoomFactorProperty().bind(slider.valueProperty());
		vbox.getChildren().addAll(txt, slider);
		return vbox;
	}
}
