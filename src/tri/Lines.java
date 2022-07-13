package tri;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

import java.util.ArrayList;
import java.util.List;

import tdf.Config;
import tdf.Renderable;

import tri.types.Line;

public class Lines extends Renderable {
	private final Line2D.Float	lineShape	= new Line2D.Float();
	private final List<Line>	lines		= new ArrayList<>();
	private final Vertices		vertices;
	
	Lines(Vertices vertices) {
		this.vertices = vertices;
	}

	@Override
	public void render(Graphics2D g, Config config) {
		double scale = Demo.renderer.getScale();

		g.setStroke(new BasicStroke((float) (2 * scale)));
		for(int i = 0; i < lines.size(); i++) {
			Line line = lines.get(i);
			if(line.color == null) continue;

			lineShape.setLine(line.start.x, line.start.y, line.end.x, line.end.y);

			g.setColor(line.color);
			g.draw(lineShape);
			lineShape.setLine(line.midX, line.midY, line.midX + line.tagX * scale, line.midY + line.tagY * scale);
			g.draw(lineShape);
		}
	}

	public void add(Line line) {
		if(!lines.contains(line)) {
			lines.add(line);
			vertices.add(line.start);
			vertices.add(line.end);
		}
	}
	
	public void remove(Line line) {
		lines.remove(line);
	}

	public void clear() {
		lines.clear();
	}
}
