package prefux.controls;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.input.ScrollEvent;
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

public class CollapseControl extends ControlAdapter {
    
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
    
    /**
     * Create a new collapse control.
     */
    public CollapseControl() {}
    
	@Override
	public void itemEvent(VisualItem item, Event e)
	{
		if(e.getEventType() == MouseEvent.MOUSE_RELEASED && item instanceof NodeItem) {
			
			
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
				
				item.setFillColor(ColorLib.rgb(238, 130, 238));
				item.setExpanded(false);
				
			} else {
				
				System.out.println("EXPANDING");
				
				Node n = (Node)item;
	    		showChildren(n);
	    		//item.setFillColor(ColorLib.rgb(76, 153, 0));
	    		item.setFillColor(ColorLib.rgb(0, 0, 0));
				item.setExpanded(true);
			}
    	}
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
			
			//vi.setFillColor(ColorLib.rgb(76, 153, 0));
			vi.setFillColor(ColorLib.rgb(0, 0, 0));
			vi.setX(coordinates[0]);
			vi.setY(coordinates[1]);
			vi.setSize(size);
			
			vi.setExpanded(true);
			vi.setVisible(true);
		}
    }
    
} // end of class CollapseControl
