package tri;

import java.awt.*;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import tri.types.Line;
import tri.types.Sector;
import tri.types.Triangle;
import tri.types.Vertex;

public class Renderer extends JComponent {
	private static final int	DEFAULT_WIDTH	= 960;
	private static final int	DEFAULT_HEIGHT	= 720;
	
	public boolean				antialias		= false;

	public double				offsetX			= 0;
	public double				offsetY			= 0;
	public double				scale			= 1;

	private double				virtualWidth	= DEFAULT_WIDTH;
	private double				virtualHeight	= DEFAULT_HEIGHT;
	private double				virtualTop		= virtualHeight / 2;
	private double				virtualRight	= virtualWidth / 2;
	private double				virtualBottom	= virtualTop - virtualHeight;
	private double				virtualLeft		= virtualRight - virtualWidth;

	private int					gridSize		= 64;

	public final List<Vertex>	vertices		= new ArrayList<>();
	public final List<Line>		lines			= new ArrayList<>();
	public final List<Triangle>	triangles		= new ArrayList<>();

	private Line2D.Float		lineShape		= new Line2D.Float(); 
	private Polygon				triShape		= new Polygon();
	private Rectangle2D.Float	vertShape		= new Rectangle2D.Float();
	private Rectangle2D			triAnchor		= new Rectangle2D.Float(0, 0, Sector.FLAT_SIZE, -Sector.FLAT_SIZE);
	
	private boolean				showCoords		= false;
	private int					mouseX, mouseY;

	Renderer() {
		setPreferredSize(new Dimension(960, 720));
		MouseAdapter adapter = new MouseAdapter() {
			private int x, y;

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int rot = e.getWheelRotation();
				if(rot > 0) scale *= 1.1;
				else scale /= 1.1;
				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int x2 = e.getX();
				int y2 = e.getY();
				int dx = x - x2;
				int dy = y2 - y;
				x = x2;
				y = y2;

				offsetX += dx * scale;
				offsetY += dy * scale;

				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				x = e.getX();
				y = e.getY();
			}
			
			@Override
			public void mouseMoved(MouseEvent e) {
				showCoords = true;

				mouseX = (int) ((e.getX() - getWidth() / 2) * scale + offsetX);
				mouseY = (int) ((getHeight() / 2 - e.getY()) * scale + offsetY);
				repaint();
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				showCoords = false;
				repaint();
			}
		};
		addMouseListener(adapter);
		addMouseWheelListener(adapter);
		addMouseMotionListener(adapter);
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch(e.getKeyCode()) {
					case KeyEvent.VK_R:
						Demo.triangulate();
						break;
					case KeyEvent.VK_C:
						Demo.loadConfig();
						break;
					case KeyEvent.VK_E:
						resetMap();
						repaint();
						break;
					case KeyEvent.VK_S:
						Demo.forceStop();
						break;
				}
			}
		});
		setFocusable(true);
		enableEvents(KeyEvent.KEY_EVENT_MASK);
	}

	public void resetMap() {
		vertices.clear();
		lines.clear();
		triangles.clear();
	}

	public void positionAndScaleToFitContent(Rectangle contentBounds) {
		// TODO get bounding box of all vertices and make sure viewport can view all of them comfortably.
		// should also be paired with a config option of whether this should be done or not
	}

	private void calculateVirtualBounds() {
		virtualWidth = getWidth() * scale;
		virtualHeight = getHeight() * scale;
		virtualTop = offsetY + virtualHeight / 2.0;
		virtualRight = offsetX + virtualWidth / 2.0;
		virtualBottom = virtualTop - virtualHeight;
		virtualLeft = virtualRight - virtualWidth;
	}

	@Override
	public void paint(Graphics g) {
		calculateVirtualBounds();

		drawBackground(g);

		Graphics2D g2 = (Graphics2D) g;
		AffineTransform trans = g2.getTransform();
		g2.translate(getWidth() / 2.0, getHeight() / 2.0);
		g2.scale(1 / scale, -1 / scale);
		g2.translate(-offsetX, -offsetY);

		drawTriangles(g2);
		
		Object hint = antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
		
		drawGridlines(g2);
		drawLines(g2);
		drawVertices(g2);

		g2.setTransform(trans);
		
		if(showCoords) {			
			String str = String.format("(%d, %d)", mouseX, mouseY);
			int width = g.getFontMetrics().stringWidth(str);
			int x = getWidth() - width - 5;
			int y = getHeight() - 5;
			g.setColor(Color.BLACK);
			g.drawString(str, x + 2, y + 2);
			g.drawString(str, x - 2, y - 2);
			g.setColor(Color.WHITE);
			g.drawString(str, x, y);
		}
	}

	private void drawBackground(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	private Color gridLineColor(int val) {
		if(val == 0) return Color.ORANGE;
		if((val / gridSize) % 8 == 0) return Color.LIGHT_GRAY;
		return Color.DARK_GRAY;
	}

	private void drawGridlines(Graphics2D g) {
		g.setColor(Color.DARK_GRAY);
		g.setStroke(new BasicStroke((float) scale));

		int gridStart = (int) virtualLeft;
		gridStart -= (gridStart % gridSize);
		Line2D.Float line = new Line2D.Float();
		line.y1 = (float) virtualTop;
		line.y2 = (float) virtualBottom;
		for(int x = gridStart; x <= virtualRight; x += gridSize) {
			line.x1 = line.x2 = x;
			g.setColor(gridLineColor(x));
			g.draw(line);
		}

		gridStart = (int) virtualBottom;
		gridStart -= (gridStart % gridSize);
		line.x1 = (float) virtualLeft;
		line.x2 = (float) virtualRight;
		for(int y = gridStart; y <= virtualTop; y += gridSize) {
			line.y1 = line.y2 = y;
			g.setColor(gridLineColor(y));
			g.draw(line);
		}
	}

	private void drawTriangles(Graphics2D g) {
		// Not using a foreach so we don't have the console spammed with concurrent modification issues
		for(int i = 0; i < triangles.size(); i++) {
			Triangle tri = triangles.get(i);
			// can't recycle TexturePaint
			TexturePaint tp = new TexturePaint(tri.texture, triAnchor);
			g.setPaint(tp);
			
			triShape.reset();
			triShape.addPoint(tri.a.x, tri.a.y);
			triShape.addPoint(tri.b.x, tri.b.y);
			triShape.addPoint(tri.c.x, tri.c.y);
			
			g.fill(triShape);
		}
	}

	private void drawLines(Graphics2D g) {
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

	private void drawVertices(Graphics2D g) {
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
	
	public void addLine(Line line) {
		if(!lines.contains(line)) {
			lines.add(line);
			addVertex(line.start);
			addVertex(line.end);
		}
	}
	
	public void addVertex(Vertex v) {
		if(!vertices.contains(v)) {
			vertices.add(v);
		}
	}
}