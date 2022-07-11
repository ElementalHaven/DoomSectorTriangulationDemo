package tri;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class Renderer extends JComponent {
	private static final int			DEFAULT_WIDTH	= 960;
	private static final int			DEFAULT_HEIGHT	= 720;
	
	private class GridLines extends Renderable {

		@Override
		public void render(Graphics2D g, Config config) {
			g.setStroke(new BasicStroke((float) scale));

			int gridStart = (int) virtualLeft;
			gridStart -= (gridStart % config.gridSizeMinor);
			Line2D.Float line = new Line2D.Float();
			line.y1 = (float) virtualTop;
			line.y2 = (float) virtualBottom;
			for(int x = gridStart; x <= virtualRight; x += config.gridSizeMinor) {
				line.x1 = line.x2 = x;
				g.setColor(config.gridLineColor(x));
				g.draw(line);
			}

			gridStart = (int) virtualBottom;
			gridStart -= (gridStart % config.gridSizeMinor);
			line.x1 = (float) virtualLeft;
			line.x2 = (float) virtualRight;
			for(int y = gridStart; y <= virtualTop; y += config.gridSizeMinor) {
				line.y1 = line.y2 = y;
				g.setColor(config.gridLineColor(y));
				g.draw(line);
			}
		}
		
	}
	
	protected Config					config;

	private double						offsetX			= 0;
	private double						offsetY			= 0;
	private double						scale			= 1;

	private double						virtualWidth	= DEFAULT_WIDTH;
	private double						virtualHeight	= DEFAULT_HEIGHT;
	private double						virtualTop		= virtualHeight / 2;
	private double						virtualRight	= virtualWidth / 2;
	private double						virtualBottom	= virtualTop - virtualHeight;
	private double						virtualLeft		= virtualRight - virtualWidth;
	private Rectangle2D					virtualBounds	= new Rectangle2D.Double(virtualLeft, virtualBottom, virtualWidth, virtualHeight);

	private final List<RenderLayer>		layers			= new ArrayList<>();
	private final RenderLayer			baseLayer		= new RenderLayer(0);
	private final GridLines				gridLines		= new GridLines();
	private final int					yMirror;

	private boolean						directRendering	= false;
	private BufferedImage				image;
	private int							width			= -1;
	private int							height			= -1;
	private boolean						doFullRebuild	= true;
	private boolean						showCoords		= false;
	private int							mouseX, mouseY;

	public Renderer() {
		setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
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

				mouseX = (int) ((e.getX() - width / 2) * scale + offsetX);
				mouseY = (int) ((e.getY() - height / 2) * yMirror * scale + offsetY);
				repaint(true);
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
		setFocusable(true);
		enableEvents(KeyEvent.KEY_EVENT_MASK);
		
		yMirror = isPositiveYUp() ? -1 : 1;
		
		// can't call directly because subclass variables aren't initialized yet
		// not sure if there's a better way than this or asking the user to do it themself
		SwingUtilities.invokeLater(() -> addDefaultItems());
	}
	
	public void addKeyBinding(KeyBinding binding) {
		setKeyBinding(binding.keystroke, binding.id, binding.action);
	}
	
	private void setKeyBinding(KeyStroke key, String id, Action action) {
		ActionMap actions = getActionMap();
		// looking at the implementation, it seems there's a 1 to 1 mapping
		actions.put(id, action);
		
		InputMap inputs = getInputMap();
		Object existing = inputs.get(key);
		// no need to update if the binding hasnt changed
		if(existing == null || !existing.equals(id)) {
			// remove existing binding if a different one existed
			KeyStroke[] keys = inputs.keys();
			if(keys != null) {
				for(KeyStroke stroke : keys) {
					Object obj = inputs.get(stroke);
					if(id.equals(obj)) {
						inputs.remove(stroke);
					}
				}
			}
			inputs.put(key, id);
		}
	}
	
	/**
	 * Adds items that should always be present to the renderer.
	 * This is done during initialization and after clearing.
	 * Subclassed renderers that override this method
	 * should make sure to call {@code super.addDefaultItems()}
	 */
	protected void addDefaultItems() {
		layers.add(baseLayer);
		baseLayer.objects.add(gridLines);
	}

	/**
	 * Removes all existing renderable items from this renderer.
	 * Afterwards, always present items are readded automatically.
	 * Subclassed renderers that override this method
	 * should make sure to call {@code super.clear()}
	 */
	public void clear() {
		layers.clear();
		baseLayer.clear();
		addDefaultItems();
	}
	
	/**
	 * Gets the scale that the viewport is being rendered with
	 * for purposes of scaling objects whose size should remain constant in the viewport.
	 */
	public final double getScale() {
		return scale;
	}
	
	/**
	 * Returns a value indicating if units at the top of the viewport show have
	 * a greater Y value than units at the bottom of the viewport.<br>
	 * This is used for initial viewport scaling,
	 * but it should also be used for scaling text and textures if the value is not known ahead of time
	 * @return
	 */
	public boolean isPositiveYUp() {
		return true;
	}

	public void positionAndScaleToFitContent(Rectangle contentBounds) {
		// TODO get bounding box of all vertices and make sure viewport can view all of them comfortably.
		// should also be paired with a config option of whether this should be done or not
	}
	
	private void handlePotentialResize() {
		int width = getWidth();
		int height = getHeight();

		if(this.width != width || this.height != height) {
			this.width = width;
			this.height = height;
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			doFullRebuild = true;
		}
	}

	private void calculateVirtualBounds() {	
		virtualWidth = width * scale;
		virtualHeight = height * scale;
		virtualTop = offsetY + virtualHeight / 2.0;
		virtualRight = offsetX + virtualWidth / 2.0;
		virtualBottom = virtualTop - virtualHeight;
		virtualLeft = virtualRight - virtualWidth;
		virtualBounds.setRect(virtualLeft, virtualBottom, virtualWidth, virtualHeight);
	}
	
	@Override
	public void repaint() {
		repaint(false);
	}
	
	public void repaint(boolean uiUpdateOnly) {
		doFullRebuild |= !uiUpdateOnly;
		super.repaint();
	}
	
	private void drawScene(Graphics g) {
		if(directRendering) {
			drawSceneToWindow((Graphics2D) g);
		} else {
			if(doFullRebuild) {
				drawSceneToImage();
				doFullRebuild = false;
			}
			
			// XXX this could be optimized so that only part of the image is rendered if a full rebuild wasn't done.
			// just the part under the text would be redrawn
			g.drawImage(image, 0, 0, null);
		}
	}
	
	private void drawSceneToImage() {
		Graphics2D g2 = image.createGraphics();
		drawSceneImpl(g2);
	}
	
	private void drawSceneToWindow(Graphics2D g) {
		AffineTransform transform = g.getTransform();
		drawSceneImpl(g);
		g.setTransform(transform);
	}
	
	private void drawSceneImpl(Graphics2D g) {
		drawBackground(g);
		
		// scale and positioning
		g.translate(width / 2.0, height / 2.0);
		g.scale(1 / scale, yMirror / scale);
		g.translate(-offsetX, -offsetY);
		
		boolean antialiased = config.antialias;
		Object hint = antialiased ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
		
		for(int i = 0; i < layers.size(); i++) {
			RenderLayer layer = layers.get(i);
			antialiased = layer.render(g, config, virtualBounds, antialiased);
		}
	}
	
	private void drawCoords(Graphics2D g) {
		Object hint = config.antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
		
		String str = String.format("(%d, %d)", mouseX, mouseY);
		int width = g.getFontMetrics().stringWidth(str);
		int x = getWidth() - width - 5;
		int y = getHeight() - 5;
		g.setColor(config.backgroundColor);
		g.drawString(str, x + 2, y + 2);
		g.drawString(str, x - 2, y - 2);
		g.setColor(Color.WHITE);
		g.drawString(str, x, y);
	}

	@Override
	public void paint(Graphics g) {
		// save the active config each frame so we don't potentially draw a frame with 2 different config states
		config = Demo.getConfig();
		gridLines.visible = config.drawGrid;
		
		handlePotentialResize();
		calculateVirtualBounds();

		drawScene(g);

		if(showCoords) {
			drawCoords((Graphics2D) g);
		}
	}

	private void drawBackground(Graphics g) {
		g.setColor(config.backgroundColor);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
	
	private int searchLayers(int layer) {
		// layers should be small enough that we won't see that much gain out of binary searching
		int i;
		for(i = 0; i < layers.size(); i++) {
			RenderLayer rlayer = layers.get(i);
			if(rlayer.layerId == layer) {
				return i;
			}
			if(layer < rlayer.layerId) {
				break;
			}
		}
		return -(i + 1);
	}
	
	/**
	 * Adds a renderable item to the default layer(0)
	 * @param r The object to be added
	 */
	public void add(Renderable r) {
		add(r, 0);
	}
	
	/**
	 * Adds a new item to be rendered to the given layer.
	 * Grid lines are drawn immediately before layer 0.
	 * Items should therefore be added to layers around that fact.
	 * @param r The object to be added
	 * @param layer The layer to add the object to
	 */
	public void add(Renderable r, int layer) {
		if(r == null) {
			throw new IllegalArgumentException("Object can not be null");
		}
		int idx = searchLayers(layer);
		RenderLayer rlayer;
		if(idx < 0) {
			rlayer = new RenderLayer(layer);
			idx = -(idx + 1);
			layers.add(idx, rlayer);
		} else {
			rlayer = layers.get(idx);
		}
		// assuming that the user is making sure not to add items twice
		rlayer.objects.add(r);
	}
}