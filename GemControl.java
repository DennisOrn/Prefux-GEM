package prefux.controls;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javafx.animation.PauseTransition;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ZoomEvent;
import javafx.util.Duration;

import prefux.Display;
import prefux.FxDisplay;
import prefux.data.Edge;
import prefux.data.Node;
import prefux.data.util.Point2D;
import prefux.util.ColorLib;
import prefux.util.PrefuseLib;
import prefux.visual.EdgeItem;
import prefux.visual.NodeItem;
import prefux.visual.VisualItem;

public class GemControl extends ControlAdapter {
    
    private class ItemInfo {
    	private double size;
    	private double[] coordinates = new double[2];
    	
    	ItemInfo(double size, double[] coordinates) {
    		this.size = size;
    		this.coordinates = coordinates;
    	}
    }
    
    HashMap<VisualItem, ItemInfo> map = new HashMap<>();
    private VisualItem currentItem;
    
    private FxDisplay display;
    private double zoomValue = 1;
    private Point2D anchor;
    
    /**
     * Create a new collapse control.
     */
    public GemControl() {}
    
    public GemControl(FxDisplay display) {
    	this.display = display;
    }
    
	@Override
	public void itemEvent(VisualItem item, Event e)
	{
		if(e.getEventType() == MouseEvent.MOUSE_PRESSED) {
			
			System.out.println(e.getEventType());
			PauseTransition pause = new PauseTransition(Duration.millis(750));
			pause.setOnFinished(event -> {
				System.out.println(e.getEventType());
			});
			pause.play();
			
		}
		else if(e.getEventType() == MouseEvent.MOUSE_RELEASED && item instanceof NodeItem) {
			
			
			item.setVisible(false);
			item.getVisualization().repaint();
			
			
			if(item.isExpanded()) {
				
				/* item.setVisible();
				 * does not work, would have been a lot more simple if it did.
				 */
				
				System.out.println("COLLAPSING");
				currentItem = item;
				
				if(!map.containsKey(item)) {
					double[] coordinates = new double[2];
					coordinates[0] = item.getX();
					coordinates[1] = item.getY();
					ItemInfo itemInfo = new ItemInfo(item.getSize(), coordinates);
					
					map.put(item, itemInfo);
				}
				
				Node n = (Node)item;
				hideChildren(n);
				
				item.setFillColor(ColorLib.rgb(255,165,0)); // INDIGO
				item.setExpanded(false);
				
			} else {
				
				System.out.println("EXPANDING");
				
				Node n = (Node)item;
	    		showChildren(n);
	    		
	    		item.setFillColor(ColorLib.rgb(75, 0, 130)); // ORANGE
				item.setExpanded(true);
			}
    	}
		e.consume();
    }
    
    private void hideChildren(Node n) {
    	
    	Iterator<? extends Node> it = n.outNeighbors();
		while(it.hasNext()) {
			
			Node child = it.next();
			hideChildren(child);
			
			VisualItem vi = (VisualItem)child;
			
			if(!map.containsKey(vi)) {
				double[] coordinates = new double[2];
				coordinates[0] = vi.getX();
				coordinates[1] = vi.getY();
				ItemInfo itemInfo = new ItemInfo(vi.getSize(), coordinates);
				
				map.put(vi, itemInfo);
			}
			
			vi.setSize(0);
			vi.setX(currentItem.getX());
			vi.setY(currentItem.getY());
			
			vi.setExpanded(false);
			vi.setVisible(false);
			
		}
    }
    
    private void showChildren(Node n) {
    	Iterator<? extends Node> it = n.outNeighbors();
		while(it.hasNext()) {
			
			Node child = it.next();
			showChildren(child);
			
			VisualItem vi = (VisualItem)child;
			ItemInfo itemInfo = map.get(vi);
			
			double size = itemInfo.size;
			double[] coordinates = itemInfo.coordinates;
			
			vi.setFillColor(ColorLib.rgb(75, 0, 130)); // ORANGE
			vi.setX(coordinates[0]);
			vi.setY(coordinates[1]);
			vi.setSize(size);
			
			vi.setExpanded(true);
			vi.setVisible(true);
		}
    }
    
} // end of class CollapseControl
