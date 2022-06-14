package tri;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

class RenderLayer {
	final int layerId;
	
	final List<Renderable> objects = new ArrayList<>();
	
	RenderLayer(int layer) {
		layerId = layer;
	}
	
	// I kinda want to change formatting styles & max line lengths towards what are considered "standard"
	// for the purposes of showing potential employers that I am indeed capable of coding using whatever code style they state
	// but this project itself already has a standardized style 
	boolean render(Graphics2D g, Config config, Rectangle2D viewportBounds, boolean antialiasingEnabled) {
		for(int i = 0; i < objects.size(); i++) {
			Renderable object = objects.get(i);
			// render only visible objects that lie within the viewport area
			// if the object's bounds are unknown,
			// it's considered always within the viewport area
			if(object.visible && (object.bounds == null || viewportBounds.intersects(object.bounds))) {
				if(config.antialias && (object.canBeAntialiased() != antialiasingEnabled)) {
					antialiasingEnabled = !antialiasingEnabled;
					
					Object hint = antialiasingEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
				}
				object.render(g, config);
			}
		}
		return antialiasingEnabled;
	}
	
	void clear() {
		objects.clear();
	}
}
