package prefux.controls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import prefux.data.Edge;
import prefux.data.Node;
import prefux.visual.NodeItem;
import prefux.visual.VisualItem;

/*
 * GemControl handles the selection, collapsing/expanding and movements of nodes
 */

public class GemControl extends ControlAdapter {
	
	public static Paint NORMAL_COLOR			= Color.DEEPSKYBLUE;
	public static Paint COLLAPSED_COLOR			= Color.ORANGE;
	public static double NORMAL_STROKE_WIDTH	= 3;
	public static double SELECTED_STROKE_WIDTH	= 15; 
    
	// Used to calculate how long a node has been pressed
    private long startTime = 0;
    
    // The item that is currently touched
    // if null: nothing is touched or the touched item is not selected
    private VisualItem touchedItem;
    
    // Used to determine if the currently touched item is selected or not.
    private boolean selected;
    
    // A list of all the currently selected items.
    private List<VisualItem> selectedItems = new ArrayList<>();
    
    /**
     * Create a new collapse control.
     */
    public GemControl() {}
    
    /*public GemControl(FxDisplay display) {
    	this.display = display;
    }*/
    
	@Override
	public void itemEvent(VisualItem item, Event e)
	{
		// Handle event only if algorithm has finished.
		if(item.isFixed()) {
			
			if(e.getEventType() == TouchEvent.TOUCH_PRESSED && item instanceof NodeItem) {
				startTime = System.nanoTime();
				selected = item.isHighlighted() ? true : false;
			}
			
			else if(e.getEventType() == TouchEvent.TOUCH_STATIONARY && item instanceof NodeItem) {
				long elapsedTime = (System.nanoTime() - startTime) / 1000000; // Milliseconds
				if(elapsedTime > 500 && touchedItem != item ) {
					
					touchedItem = item;
					if(!selected) { // Select the item
						
						selectedItems.add(item);
						item.setHighlighted(true);
						
						// Change the stroke-width of the circle
						Circle circle = getCircle((Node) item);
						if(circle != null) {
							circle.setStrokeWidth(SELECTED_STROKE_WIDTH);
						}
						
					} else { // Deselect the item
						
						selectedItems.remove(item);
						item.setHighlighted(false);
						
						// Change the stroke-width of the circle
						Circle circle = getCircle((Node) item);
						if(circle != null) {
							circle.setStrokeWidth(NORMAL_STROKE_WIDTH);
						}
					}
					
					System.out.println("selected items: " + selectedItems.size());
				}
			}
			
			// TODO: DESELECT EVEYTHING WHEN DOUBLE TAPPING???
			
			else if(e.getEventType() == TouchEvent.TOUCH_RELEASED && item instanceof NodeItem) {
				
				long elapsedTime = (System.nanoTime() - startTime) / 1000000; // Milliseconds
				if(elapsedTime < 500) {
					
					if(item.isExpanded()) { // Collapse
						
						Node node = (Node) item;
						hideChildren(node);
						
						// Change the color of the circle
						Circle circle = getCircle(node);
						if(circle != null) {
							circle.setFill(COLLAPSED_COLOR);
						}
						
						// Hide lines
						List<Line> lines = getLines(node);
						for(Line line : lines) {
							line.setVisible(false);
						}
						
						item.setExpanded(false);
						
					} else { // Expand
						
						Node node = (Node) item;
			    		showChildren(node);
			    		
			    		// Change the color of the circle
			    		Circle circle = getCircle(node);
			    		if(circle != null) {
			    			circle.setFill(NORMAL_COLOR);
			    		}
			    		
			    		// Show lines
			    		List<Line> lines = getLines(node);
						for(Line line : lines) {
							line.setVisible(true);
						}
			    		
						item.setExpanded(true);
					}
				}
				
				touchedItem = null;
			}
			
			else if(e.getEventType() == TouchEvent.TOUCH_MOVED && item instanceof NodeItem) {
				
				long elapsedTime = (System.nanoTime() - startTime) / 1000000; // Milliseconds
				if(!selected && elapsedTime > 500 && item.isExpanded()) {
					
					TouchEvent ev = (TouchEvent) e;
					TouchPoint point = ev.getTouchPoint();
					
					// Move all selected items
					for(VisualItem vi : selectedItems) {
						if(vi.isExpanded() && vi.isVisible()) {
							vi.setX(vi.getX() + point.getX());
							vi.setY(vi.getY() + point.getY());
						}
					}
				}
			}
		}
		
		e.consume();
    }
    
    private void hideChildren(Node node) {
    	Iterator<? extends Node> it = node.outNeighbors();
		while(it.hasNext()) {
			
			Node child = it.next();
			hideChildren(child);
			
			// Hide circle
			Circle circle = getCircle(child);
			if(circle != null) {
				circle.setVisible(false);
			}
			
			// Hide label
			Label label = getLabel(child);
			if(label != null) {
				label.setVisible(false);
			}
			
			// Hide lines
			List<Line> lines = getLines(child);
			for(Line line : lines) {
				line.setVisible(false);
			}
			
			VisualItem item = (VisualItem) child;
			item.setVisible(false);
		}
    }
    
    private void showChildren(Node node) {
    	Iterator<? extends Node> it = node.outNeighbors();
		while(it.hasNext()) {
			
			Node child = it.next();
			showChildren(child);
			
			// Show circle
			Circle circle = getCircle(child);
			if(circle != null) {
				circle.setVisible(true);
				circle.setFill(NORMAL_COLOR);
			}
			
			// Show label
			/*Label label = getLabel(child);
			if(label != null) {
				//if(label.isVisible())////////////////////////////
				label.setVisible(true);
			}*/
			
			// Show lines
			List<Line> lines = getLines(child);
			for(Line line : lines) {
				line.setVisible(true);
			}
			
			VisualItem item = (VisualItem) child;
			item.setExpanded(true);
			item.setVisible(true);
		}
    }
    
    private Circle getCircle(Node node) {
    	
    	VisualItem item = (VisualItem) node;
    	if(item.getNode() instanceof Group) {
    		
    		Group group = (Group) item.getNode();
			ObservableList<javafx.scene.Node> groupList = group.getChildren();
			for(javafx.scene.Node groupChild : groupList) {
				
				if(groupChild instanceof Circle) {
					return (Circle) groupChild;
				}
			}
    	}
    	
    	return null;
    }
    
    private Label getLabel(Node node) {
    	
	    VisualItem item = (VisualItem) node;
		if(item.getNode() instanceof Group) {
			Group group = (Group) item.getNode();
			ObservableList<javafx.scene.Node> groupList = group.getChildren();
			for(javafx.scene.Node groupChild : groupList) {
				
				if(groupChild instanceof StackPane) {
					
					StackPane stack = (StackPane) groupChild;
					ObservableList<javafx.scene.Node> stackList = stack.getChildren();
					for(javafx.scene.Node stackChild : stackList) {
						
						if(stackChild instanceof Label) {
							return (Label) stackChild;
						}
					}
				}
			}
		}
	
		return null;
    }
    
    private List<Line> getLines(Node node) {
    	
    	List<Line> lines = new ArrayList<>();
    	Iterator<? extends Edge> it = node.outEdges();
    	while(it.hasNext()) {
    		
    		Edge edge = it.next();
    		VisualItem item = (VisualItem) edge;
    		if(item.getNode() instanceof Line) {
    			Line line = (Line) item.getNode();
				lines.add(line);
    		}
    	}
    	
    	return lines;
    }
}
