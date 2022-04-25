package tri.types;

import java.awt.Color;

import tri.Demo;

public class Line {
	public static Color	defaultColor	= Color.WHITE;
	public static Color generatedColor	= Color.CYAN;
	
	public Vertex		start;
	public Vertex		end;
	public Sector		front;
	public Sector		back;
	public Color		color			= defaultColor;
	public double		angle;
	public double		tagX;
	public double		tagY;
	public float		midX;
	public float		midY;
	public boolean		generated;

	public boolean isOneSided() {
		return (front == null) ^ (back == null);
	}

	public boolean isTwoSided() {
		return (front != null) && (back != null);
	}

	public boolean isUselessForTriangulation() {
		return front == back;
	}

	public boolean isDegenerate() {
		return front == null && back == null;
	}
	
	@Override
	public boolean equals(Object other) {
		return other != null && other instanceof Line && equals((Line) other);
	}

	public boolean equals(Line other) {
		return other != null && equalsExactly(other);
	}

	public boolean equalsExactly(Line other) {
		return other.start == start && other.end == end;
	}

	public boolean equalsMirrored(Line other) {
		return other.end == start && other.start == end;
	}

	public boolean equalsAnyWay(Line other) {
		return equalsExactly(other) || equalsMirrored(other);
	}
	
	public void calculateAngle() {
		generateTag(true);
	}
	
	private void generateTag(boolean withAngle) {
		int dx = end.x - start.x;
		int dy = end.y - start.y;
		
		midX = (end.x + start.x) / 2.f;
		midY = (end.y + start.y) / 2.f;
		
		double mag = Math.sqrt(dx * dx + dy * dy);
		tagX = dy * 4 / mag;
		tagY = dx * -4 / mag;
		
		if(withAngle) {
			angle = Math.atan2(dy, dx);
		}
	}

	public void flip() {
		Vertex vert = start;
		start = end;
		end = vert;

		Sector side = front;
		front = back;
		back = side;
		
		if(angle > 0) angle -= Math.PI;
		else angle += Math.PI;
		
		if(tagX == 0 && tagY == 0) {
			generateTag(false);
		} else {
			tagX *= -1;
			tagY *= -1;
		}
	}

	public void remove(Sector sector) {
		start.removeLine(this, sector);
		end.removeLine(this, sector);
		sector.linesToProcess.remove(this);
		Demo.renderer.lines.remove(this);
	}
	
	public boolean bordersSector(Sector sector) {
		return front == sector || back == sector;
	}
	
	public void resetColor() {
		color = generated ? generatedColor : defaultColor;
	}
	
	public double angleBetween(Line next) {
		// angles and what I want vs what math stuff gives
		// a	b		good		+		a-b			b-a			180-a+b
		// 180	90		90			-90		90			-90			90
		// 135	45		90			180		90			-90			90
		// -135 135		90			0		-270/90		270/-90		90
		// 180 -135		225/-135	-45		315/-45		45			-135
		// 179  1		2			180		178			-178		2
		
		double a = Math.PI - angle + next.angle;
		// addition probably isn't needed, but it doesn't hurt
		a = (a + Math.PI * 2) % (Math.PI * 2);
		return a;
	}
	
	public double distanceTo(Vertex v) {
		double dist;
		if(start.x == end.x) {
			dist = v.x - start.x;
			if(end.y < start.y) dist *= -1;
		} else if(start.y == end.y) {
			dist = v.y - start.y;
			if(end.x > start.x) dist *= -1;
		} else {
			int xLen = end.x - start.x;
			int yLen = end.y - start.y;
			// wikipedia math. I'd remove the abs since I don't think I need it, but I don't trust it entirely
			dist = Math.abs(xLen * (start.y - v.y) - (start.x - v.x) * yLen) / Math.sqrt(xLen * xLen + yLen * yLen);
		}
		return dist;
	}
}