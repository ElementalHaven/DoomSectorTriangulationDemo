package tri.types;

import java.util.List;

import tri.Demo;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Vertex {
	public static Color	defaultColor	= Color.WHITE;

	// giving increased precision since it shouldn't break anything
	// and we can ensure points we create for the purpose of fixing sectors is in range
	public int			x;
	public int			y;
	
	public int			id;

	public List<Line>	lines			= new ArrayList<>();
	public List<Line>	linesToProcess	= new ArrayList<>();
	public Color		color			= defaultColor;
	private Sector		preppedSector	= null;

	public Vertex() {}

	public Vertex(ByteBuffer buf) {
		x = buf.getShort();
		y = buf.getShort();
	}

	public boolean isDegenerate() {
		return lines.isEmpty();
	}

	public boolean isUselessForTriangulation() {
		return lines.size() < 2;
	}
	
	public int distSquared(Vertex other) {
		int dx = other.x - x;
		int dy = other.y - y;
		return dx * dx + dy * dy;
	}

	public void addLine(Line line) {
		lines.add(line);
	}

	public Line getAnyLineStartingHere(List<Line> discard) {
		for(Line line : linesToProcess) {
			if(discard.contains(line)) continue;

			if(line.start == this) return line;
		}

		return null;
	}
	
	public Line getAnyLineEndingHere(List<Line> discard) {
		for(Line line : linesToProcess) {
			if(line.isUselessForTriangulation() && !line.generated) {
				System.err.println("HOW DID THIS GET IN HERE");
			}
			if(discard.contains(line)) continue;

			if(line.end == this) return line;
		}

		return null;
	}
	
	public void addLine(Line line, Sector sector) {
		linesToProcess.add(line);
		//System.out.printf("    Attached line to %s(%d total)\n", this, linesToProcess.size());
	}
	
	public void removeLine(Line line, Sector sector) {
		linesToProcess.remove(line);
		//System.out.printf("    Removed line from %s(%d total)\n", this, linesToProcess.size());
		if(linesToProcess.isEmpty()) {
			Demo.renderer.removeVertex(this);
			sector.vertsToProcess.remove(this);
		}
	}
	
	public void prepForSector(Sector sector) {
		if(preppedSector == sector) return;

		color = defaultColor;
		linesToProcess.clear();
		for(Line line : lines) {
			if(line.bordersSector(sector) && !line.isUselessForTriangulation()) {
				addLine(line, sector);
			}
		}
		
		preppedSector = sector;
	}
	
	public String toString() {
		return String.format("v%d(%d, %d)", id, x, y);
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof Vertex && this.equals((Vertex) obj);
	}
	
	public boolean equals(Vertex v) {
		return v != null && x == v.x && y == v.y;
	}
}