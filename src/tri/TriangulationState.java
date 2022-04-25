package tri;

import tri.types.Line;
import tri.types.Sector;
import tri.types.Vertex;

public final class TriangulationState {
	public Sector sector;
	
	public Vertex vertA, vertB, vertC;
	public Line lineA, lineB, lineC;
	public boolean bGenerated, cGenerated;
	
	public TriangulationState() {}
	
	public TriangulationState(TriangulationState other) {
		sector = other.sector;
		
		vertA = other.vertA;
		vertB = other.vertB;
		vertC = other.vertC;
		
		lineA = other.lineA;
		lineB = other.lineB;
		lineC = other.lineC;
		
		bGenerated = other.bGenerated;
		cGenerated = other.cGenerated;
	}
}