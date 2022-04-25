package tri.types;

import java.awt.image.BufferedImage;
import java.util.List;

import tri.TriangulationState;

public class Triangle {
	private static int determinePointSide(Vertex a, Vertex b, Vertex c) {
		// I don't understand the math, but I want to say this is computing the cross product
		return (a.x - c.x) * (b.y - c.y) - (b.x - c.x) * (a.y - c.y);  
	}
	
	public Vertex			a;
	public Vertex			b;
	public Vertex			c;
	public BufferedImage	texture;
	
	public Triangle(TriangulationState state) {
		texture = state.sector.floorTex;
		a = state.vertA;
		b = state.vertB;
		c = state.vertC;
	}
	
	// this is more or less code I found online and apparently uses something called barycentric coordinates
	// I know nothing about them, but I did recognize the first 2 results were trying to compare floats with ==, which can only end badly
	// looking at that code even longer makes me feel like its some sort of noob trap
	// https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle/2049593#2049593
	public boolean isPointInside(Vertex v) {
		int r1 = determinePointSide(v, a, b);
		int r2 = determinePointSide(v, b, c);
		int r3 = determinePointSide(v, c, a);
		
		// I'm only ever doing clockwise winding, so if I understood this better, I could remove a lot of these checks
		boolean neg = (r1 < 0) || (r2 < 0) || (r3 < 0);
		boolean pos = (r1 > 0) || (r2 > 0) || (r3 > 0);
		
		return !(pos && neg);
	}
	
	public int getPointsInside(List<Vertex> in, List<Vertex> out) {
		out.clear();
		
		int count = 0;
		for(Vertex v : in) {
			if(v == a || v == b || v == c) continue;
			
			if(isPointInside(v)) {
				out.add(v);
				count++;
			}
		}
		return count;
	}
}