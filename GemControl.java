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

/*
 * This is used to select and move nodes and collapsing / expanding.
 */

public class GemControl extends ControlAdapter {
	
	public static Paint NORMAL_COLOR			= Color.DEEPSKYBLUE;
	public static Paint COLLAPSED_COLOR			= Color.ORANGE;
	public static double NORMAL_STROKE_WIDTH	= 3;
	public static double SELECTED_STROKE_WIDTH	= 15; 
    
	// Used to calculate how long a node has been pressed
    private long startTime = 0;
    
    // The item that is currently touched.
    // Will be set when the user press and hold on the item.
    private VisualItem touchedItem;
    
    // Used to determine if the currently touched item is selected or not.
    private boolean selected;
    
    // A list of all the currently selected items.
    private List<VisualItem> selectedItems = new ArrayList<>();
    
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
					
					touchedItem = item; // To prevent the if-statement above to be true over and over again.
					if(!selected) { // Select the item.
						
						selectedItems.add(item);
						item.setHighlighted(true);
						
						// Change the stroke-width to indicate that the item is selected.
						Circle circle = getCircle((Node) item);
						if(circle != null) {
							circle.setStrokeWidth(SELECTED_STROKE_WIDTH);
						}
						
					} else { // Deselect the item.
						
						selectedItems.remove(item);
						item.setHighlighted(false);
						
						// Change the stroke-width to indicate that the item no longer selected.
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
						
						// Hide the "outgoing" edges.
						List<Line> lines = getLines(node);
						for(Line line : lines) {
							line.setVisible(false);
						}
						
						// Change the color to indicate that the item is collapsed.
						Circle circle = getCircle(node);
						if(circle != null) {
							circle.setFill(COLLAPSED_COLOR);
						}
						
						item.setExpanded(false);
						
					} else { // Expand
						
						Node node = (Node) item;
			    		showChildren(node);
			    		
			    		// Show the "outgoing" edges.
			    		List<Line> lines = getLines(node);
						for(Line line : lines) {
							line.setVisible(true);
						}
			    		
			    		// Change the color to indicate that the item is expanded.
			    		Circle circle = getCircle(node);
			    		if(circle != null) {
			    			circle.setFill(NORMAL_COLOR);
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
					
					// Move all the selected items.
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
	
    /**
     * Recursive method that hides all the children of the node
     * that is passed to the method.
     */
    private void hideChildren(Node node) {
    	Iterator<? extends Node> it = node.outNeighbors();
		while(it.hasNext()) {
			
			Node child = it.next();
			
			// Recursive method-call
			hideChildren(child);
			
			// Hide the circle
			Circle circle = getCircle(child);
			if(circle != null) {
				circle.setVisible(false);
			}
			
			// Hide the label
			Label label = getLabel(child);
			if(label != null) {
				label.setVisible(false);
			}
			
			// Hide the lines
			List<Line> lines = getLines(child);
			for(Line line : lines) {
				line.setVisible(false);
			}
			
			VisualItem item = (VisualItem) child;
			item.setVisible(false);
		}
    }
    
    /**
     * Recursive method that shows all the children of the node
     * that is passed to the method.
     */
    private void showChildren(Node node) {
    	Iterator<? extends Node> it = node.outNeighbors();
		while(it.hasNext()) {
			
			Node child = it.next();
			
			// Recursive method-call
			showChildren(child);
			
			// Show the circle
			Circle circle = getCircle(child);
			if(circle != null) {
				circle.setVisible(true);
				circle.setFill(NORMAL_COLOR);
			}
			
			// Show label
			/*Label label = getLabel(child);
			if(label != null) {
				//if(label.isVisible())//////////////////////////// TODO: WHAT
				label.setVisible(true);
			}*/
			
			// Show the lines
			List<Line> lines = getLines(child);
			for(Line line : lines) {
				line.setVisible(true);
			}
			
			VisualItem item = (VisualItem) child;
			
			// All the items will be expanded.
			item.setExpanded(true);
			item.setVisible(true);
		}
    }
    
    /**
     * Returns the circle that is associated with the node
     * that is passed to the method.
     */
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
    
    /**
     * Returns the label that is associated with the node
     * that is passed to the method.
     */
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
    
    /**
     * Returns a list with all the lines that are associated
     * with the node that is passed to the method.
     */
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
