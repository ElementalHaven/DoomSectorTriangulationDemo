package tri;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.swing.KeyStroke;

public class Config {
	public static int valueAsInt(String value, String friendlyName, int fallback) {
		return valueAsInt(value, friendlyName, Integer.MIN_VALUE, fallback);
	}
	
	public static int valueAsInt(String value, String friendlyName, int minValue, int fallback) {
		try {
			int val = Integer.parseInt(value);
			if(val >= minValue) {
				return val;
			} else {
				System.err.println(friendlyName + " must be >= " + minValue);
			}
		} catch(NumberFormatException nfe) {
			System.err.println(friendlyName + " is not a valid number");
		}
		return fallback;
	}
	
	public static KeyStroke valueAsKeystroke(String value, String friendlyName, KeyStroke fallback) {
		KeyStroke keystroke = KeyStroke.getKeyStroke(value);
		if(keystroke == null) {
			System.err.println(friendlyName + " is not a valid keystroke");
			keystroke = fallback;
		}
		return keystroke;
	}
	
	/**
	 * Converts the given string to a boolean value
	 * Positive integers and "true", ignoring case are treated as true
	 * Everything else is considered false
	 * @param value The string to derive a boolean value from
	 * @return The resulting boolean value
	 */
	public static boolean valueAsBoolean(String value) {
		try {
			int val = Integer.parseInt(value);
			return val > 0;
		} catch(NumberFormatException nfe) {
			return Boolean.parseBoolean(value);
		}
	}
	
	public static Color valueAsColor(String value, String friendlyName, Color fallback) {
		int len = value.length();
		boolean isShortenedForm = len == 4;
		boolean ok = (len == 7 || isShortenedForm) && value.charAt(0) == '#';
		if(ok) {
			if(isShortenedForm) {
				// some math after being converted to an int would probably be more efficient
				// but this is probably simpler and more readable code
				char r = value.charAt(1);
				char g = value.charAt(2);
				char b = value.charAt(3);
				value = "#" + r + r + g + g + b + b;
			}
			try {
				int rgb = Integer.parseInt(value.substring(1), 16);
				return new Color(rgb);
			} catch(NumberFormatException e) {}
		}
		System.err.println(friendlyName + " is not a valid color");
		return fallback;
	}

	public final File				file;

	public boolean					antialias		= false;
	public boolean					drawGrid		= true;

	public Color					backgroundColor	= Color.BLACK;
	/** color of the X and Y axes */
	public Color					axisColor		= Color.ORANGE;
	/** color of a large grid square */
	public Color					gridColorMajor	= Color.LIGHT_GRAY;
	/** color of a small grid square */
	public Color					gridColorMinor	= Color.DARK_GRAY;

	/** size of a grid square */
	public int						gridSizeMinor	= 64;
	/** number of small grid squares in a large grid square */
	public int						gridSizeMajor	= 8;

	protected transient Renderer	renderer;

	private KeyBinding				bindingConfig	= new KeyBinding("C", "DEFAULT:CONFIG", () -> load());
	private KeyBinding				bindingEmpty	= new KeyBinding("E", "DEFAULT:CLEAR", () -> {
		renderer.clear();
		renderer.repaint();
	});

	public Config() {
		this(new File("config.ini"));
	}
	
	public Config(File file) {
		if(file == null) {
			throw new IllegalArgumentException("File can not be null");
		}
		this.file = file;
	}
	
	public void init(Renderer renderer) {
		this.renderer = renderer;
		renderer.addKeyBinding(bindingConfig);
		renderer.addKeyBinding(bindingEmpty);
	}
	
	public final void load() {
		try {
			List<String> lines = Files.readAllLines(file.toPath());
			for(String line : lines) {
				line = line.trim();
				if(line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

				int idx = line.indexOf('=');
				if(idx == -1) continue;

				String key = line.substring(0, idx).trim().toLowerCase().replaceAll("[- _]", "");
				String value = line.substring(idx + 1).trim();
				
				boolean handled = handleStandardConfigLine(key, value);
				if(!handled) {
					handleConfigLine(key, value);
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private final boolean handleStandardConfigLine(String key, String value) {
		switch(key) {
			case "antialias":
				antialias = valueAsBoolean(value);
				break;
			case "drawgrid":
				drawGrid = valueAsBoolean(value);
				break;
			case "backgroundcolor":
				backgroundColor = valueAsColor(value, "Background color", backgroundColor);
				break;
			case "axiscolor":
				axisColor = valueAsColor(value, "Axis color", axisColor);
				break;
			case "majorgridcolor":
				gridColorMajor = valueAsColor(value, "Major grid color", gridColorMajor);
				break;
			case "minorgridcolor":
				gridColorMinor = valueAsColor(value, "Minor grid color", gridColorMinor);
				break;
			case "majorgridsize":
				gridSizeMajor = valueAsInt(value, "Major grid size", 1, gridSizeMajor);
				break;
			case "minorgridsize":
				gridSizeMinor = valueAsInt(value, "Minor grid size", 1, gridSizeMinor);
				break;
			case "keyclear":
				updateKeyBinding(bindingEmpty, "Clear renderer key", value);
				break;
			case "keyconfig":
				updateKeyBinding(bindingConfig, "Config update key", value);
				break;
			default:
				return false;
		}
		return true;
	}
	
	/**
	 * This method should be overridden in a subclass to handle program-specific config lines
	 * @param key The name of the config option.
	 *	This value is assured to be all lowercase with all dashes, underscores, spaces, and leading and trailing whitespace removed.
	 * @param value The config options value as a string. Any leading or trailing whitespace is removed
	 * @return true if the config option was handled. Otherwise false.
	 */
	public boolean handleConfigLine(String key, String value) {
		return false;
	}
	
	final Color gridLineColor(int val) {
		if(val == 0) return axisColor;
		if((val / gridSizeMinor) % gridSizeMajor == 0) return gridColorMajor;
		return gridColorMinor;
	}
	
	/**
	 * Creates a copy of this configuration.
	 * The main intention of this method is so that when the config is updated,
	 * the changes aren't being applied mid-frame by using a copy of this config for rendering during the update.
	 * @return
	 */
	public Config createCopy() {
		Config config = new Config(file);
		copyTo(config);
		return config;
	}
	
	/**
	 * Copy all config settings from this config to the specified one
	 * <br>
	 * Subclasses shoud be sure to call {@code super.copyTo(config)}
	 * so that no settings are left out
	 * @param config
	 */
	public void copyTo(Config config) {
		config.antialias = antialias;
		config.drawGrid = drawGrid;
		config.backgroundColor = backgroundColor;
		config.axisColor = axisColor;
		config.gridColorMajor = gridColorMajor;
		config.gridColorMinor = gridColorMinor;
		config.gridSizeMajor = gridSizeMajor;
		config.gridSizeMinor = gridSizeMinor;
		config.bindingConfig = bindingConfig;
		config.bindingEmpty = bindingEmpty;
		config.renderer = renderer;
	}
	
	public void updateKeyBinding(KeyBinding binding, String friendlyName, String newValue) {
		KeyStroke oldKey = binding.keystroke;
		KeyStroke newKey = valueAsKeystroke(newValue, friendlyName, oldKey);
		if(newKey != oldKey) {
			binding.keystroke = newKey;
			if(renderer == null) {
				System.err.println("Can't set key binding as config doesn't have a copy of the renderer to update");
			} else {
				renderer.addKeyBinding(binding);
			}
		}
	}
}
