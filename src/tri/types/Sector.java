package tri.types;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import tri.Demo;

public class Sector {
	public static final int	FLAT_SIZE		= 64;

	public int				id;
	public BufferedImage	floorTex;
	public RenderableSector	renderable;
	public short			lightLevel;
	public String			floorTexName;
	public List<Line>		lines			= new ArrayList<>();

	public List<Vertex>		vertsToProcess	= new ArrayList<>();
	public List<Line>		linesToProcess	= new ArrayList<>();

	public void addLine(Line line) {
		if(!lines.contains(line)) {
			lines.add(line);
			linesToProcess.add(line);
		}
	}

	public Line findOrCreateLine(Vertex a, Vertex b) {
		Line line = new Line();
		line.generated = true;
		line.start = a;
		line.end = b;
		line.front = line.back = this; // newly added. hopefully this works as I think it should -Liz (4/13/22)
		int idx = linesToProcess.indexOf(line);
		boolean generated = idx == -1;
		if(generated) {
			//System.out.println("  Creating new line for triangulation");
			line.calculateAngle();
			Demo.renderer.lines.add(line);
			linesToProcess.add(line);
			a.addLine(line, this);
			b.addLine(line, this);
			//System.out.printf("  Creating new line from %s(%d cons) to %s(%d cons)\n", a, a.linesToProcess.size(), b, b.linesToProcess.size());
		} else {
			line = linesToProcess.get(idx);
		}
		return line;
	}

	public void removeAllLines() {
		for(int lineIdx = linesToProcess.size() - 1; lineIdx >= 0; lineIdx--) {
			linesToProcess.get(lineIdx).remove(this);
		}
	}

	/**
	 * Get the vertex furthest north of all the vertices on the leftmost edge of this sector that have yet to be processed
	 */
	public Vertex getTopLeftVertex() {
		Vertex best = null;
		for(Vertex v : vertsToProcess) {
			if(best == null || v.x < best.x) best = v;
			else if(v.x == best.x && v.y > best.y) best = v;
		}
		return best;
	}

	public void prepForTriangulation() {
		//System.out.println("Prepping sector for triangulation");
		vertsToProcess.clear();
		// make sure all lines in the sector have the front side facing this sector
		for(Line line : linesToProcess) {
			line.color = Line.defaultColor;
			if(line.back == this) {
				line.flip();
			}
			if(!vertsToProcess.contains(line.start)) vertsToProcess.add(line.start);
			if(!vertsToProcess.contains(line.end)) vertsToProcess.add(line.end);
			Demo.renderer.addLine(line);
		}
		for(Vertex v : vertsToProcess) {
			v.prepForSector(this);
			Demo.renderer.addVertex(v);
		}
	}
}