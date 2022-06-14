package tri;

import tri.types.Line;
import tri.types.Vertex;

public class DoomTriRenderer extends Renderer {
	private final Vertices	vertices	= new Vertices();
	private final Lines		lines		= new Lines(vertices);

	public void addLine(Line line) {
		lines.add(line);
	}

	public void removeLine(Line line) {
		lines.remove(line);
	}

	public void addVertex(Vertex v) {
		vertices.add(v);
	}

	public void removeVertex(Vertex v) {
		vertices.remove(v);
	}

	@Override
	protected void addDefaultItems() {
		super.addDefaultItems();
		add(lines, 0);
		add(vertices, 1);
	}

	@Override
	public void clear() {
		vertices.clear();
		lines.clear();
		super.clear();
	}
}
