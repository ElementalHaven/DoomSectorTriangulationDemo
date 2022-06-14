package tri;

import java.awt.Graphics2D;
import java.awt.Rectangle;

public abstract class Renderable {
	public boolean		visible = true;
	public Rectangle	bounds;
	
	public abstract void render(Graphics2D g, Config config);
	
	/**
	 * When overriden, allows objects to opt out of any antialiasing.
	 * This is useful for when antialiasing can cause rendering artifacts,
	 * such as bleeding between the edges of filled polygons
	 * @return Whether or not antialiasing is allowed to be done for this object
	 */
	public boolean canBeAntialiased() {
		return true;
	}
}
