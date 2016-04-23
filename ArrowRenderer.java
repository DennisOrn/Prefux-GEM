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
package prefux.render;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import prefux.data.util.Point2D;
import prefux.visual.EdgeItem;
import prefux.visual.VisualItem;

/*
 * Renderer that draws edges as lines with arrows. The raw shape is
 * a Group that consists of one Line and one Polygon. When a node
 * changes location, the arrowheads of all the edges that are connected
 * to it will be adjusted.
 */
public class ArrowRenderer extends AbstractShapeRenderer implements Renderer {

	private static final Logger log = LogManager.getLogger(ArrowRenderer.class);

	public static final String DEFAULT_STYLE_CLASS = "prefux-edge";

	@Override
	public boolean locatePoint(Point2D p, VisualItem item) {
		log.debug("locatePoint " + p + " " + item);
		return false;
	}
	
	private static void adjustArrowRotation(EdgeItem edge, Polygon polygon) {
		
		double startX = edge.getSourceItem().xProperty().get();
		double startY = edge.getSourceItem().yProperty().get();
		double endX = edge.getTargetItem().xProperty().get();
		double endY = edge.getTargetItem().yProperty().get();
		
		double x = endX - startX;
		double y = endY - startY;
		
		double angle = Math.toDegrees(Math.atan2(y, x));
		
		angle += 90;
		
		Rotate rotation = (Rotate) polygon.getTransforms().get(0);
		rotation.setAngle(angle);
	}

	@Override
	protected Node getRawShape(VisualItem item, boolean bind) {
		
		EdgeItem edge = (EdgeItem) item;
		Line line = new Line();
		line.setStrokeWidth(3);
		
		Polygon polygon = new Polygon(
				0.0, 0.0,
                -20.0, 80.0,
                20.0, 80.0,
                0.0, 0.0
        );
		
		polygon.getTransforms().add(new Rotate(0, 0, 0));
		polygon.setStroke(javafx.scene.paint.Color.BLACK);
		Group group = new Group(line, polygon);
		
		if (bind) {
			Platform.runLater(() -> {
				line.startXProperty().bind(edge.getSourceItem().xProperty());
				line.startYProperty().bind(edge.getSourceItem().yProperty());
				line.endXProperty().bind(edge.getTargetItem().xProperty());
				line.endYProperty().bind(edge.getTargetItem().yProperty());
				
				polygon.layoutXProperty().bind(line.endXProperty());
				polygon.layoutYProperty().bind(line.endYProperty());
				
				edge.getTargetItem().xProperty().addListener((observable, oldValue, newValue) -> {
					adjustArrowRotation(edge, polygon);
				});
				
				edge.getTargetItem().yProperty().addListener((observable, oldValue, newValue) -> {
					adjustArrowRotation(edge, polygon);
				});
				
				edge.getSourceItem().xProperty().addListener((observable, oldValue, newValue) -> {
					adjustArrowRotation(edge, polygon);
				});
				
				edge.getSourceItem().yProperty().addListener((observable, oldValue, newValue) -> {
					adjustArrowRotation(edge, polygon);
				});
			});
		}
		
		return group;
	}

	@Override
	public String getDefaultStyle() {
		return DEFAULT_STYLE_CLASS;
	}

}
