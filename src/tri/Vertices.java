package tri;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import tri.types.Vertex;

public class Vertices extends Renderable {
	private final Rectangle2D.Float	vertShape	= new Rectangle2D.Float();
	private final List<Vertex>		vertices	= new ArrayList<>();

	@Override
	public void render(Graphics2D g, Config config) {
		double scale = Demo.renderer.getScale();

		float halfSize = (float) (2 * scale);
		vertShape.width = vertShape.height = (float) (4 * scale);
		for(int i = 0; i < vertices.size(); i++) {
			Vertex v = vertices.get(i);
			if(v.color == null) continue;

			vertShape.x = v.x - halfSize;
			vertShape.y = v.y - halfSize;

			g.setColor(v.color);
			g.fill(vertShape);
		}
	}

	public void add(Vertex v) {
		if(!vertices.contains(v)) {
			vertices.add(v);
		}
	}
	
	public void remove(Vertex v) {
		vertices.remove(v);
	}
	
	public void clear() {
		vertices.clear();
	}
}