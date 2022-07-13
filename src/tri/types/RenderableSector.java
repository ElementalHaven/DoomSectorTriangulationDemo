package tri.types;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TexturePaint;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.List;

import tdf.Config;
import tdf.Renderable;

public class RenderableSector extends Renderable {
	private static final Rectangle2D	ANCHOR		= new Rectangle2D.Float(0, 0, Sector.FLAT_SIZE, -Sector.FLAT_SIZE);
	private static final Polygon		SHAPE		= new Polygon();

	private final List<Triangle>		triangles	= new ArrayList<>();
	private final TexturePaint			paint;
	
	public RenderableSector(BufferedImage flat) {
		paint = new TexturePaint(flat, ANCHOR);
		bounds = new Rectangle(0, 0, -1, -1);
	}

	@Override
	public void render(Graphics2D g, Config config) {
		g.setPaint(paint);
		
		for(int i = 0; i < triangles.size(); i++) {
			Triangle tri = triangles.get(i);

			SHAPE.reset();
			SHAPE.addPoint(tri.a.x, tri.a.y);
			SHAPE.addPoint(tri.b.x, tri.b.y);
			SHAPE.addPoint(tri.c.x, tri.c.y);

			g.fill(SHAPE);
		}
	}
	
	public void addTriangle(Triangle tri) {
		triangles.add(tri);
		bounds.add(tri.a.x, tri.a.y);
		bounds.add(tri.b.x, tri.b.y);
		bounds.add(tri.c.x, tri.c.y);
	}
	
	@Override
	public boolean canBeAntialiased() {
		return false;
	}
}