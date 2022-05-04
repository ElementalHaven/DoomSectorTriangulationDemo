package tri;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import tri.types.WADFile;

public class Demo {
	public static final String			WINDOW_TITLE	= "Sector Triangulation Demo";
	
	public static Renderer				renderer;
	private static Map<String, WADFile>	wads			= new HashMap<>();
	public static WADFile				topWad;
	private static TriangulateThread	thread;
	public static String				mapName;

	/* FIXME certain maps still break for unknown reasons
	 * DOOM.WAD
	 *	E3M2 (a single line that isn't a part of the rest of the sector)
	 *		fix doesnt work because theres a line thats part of its own sector
	 *		 when it should be a part of another in what is effectively a tiny, 6 line box 
	 *	E4M1 (2 linedefs bordering sector 61 aren't double sided)
	 *		fix doesnt work because theres even more wrong in a complex way
	 *	E4M3 (sector 54 only has a single linedef)
	 *		fix doesnt work because sector 81 is completely fucked in a way the game logic perfectly understands
	 *	...
	 * DOOM2.WAD
	 * ...
	 *	MAP16 (sector 8 has only a single line)
	 * MAP29
	 * 		v1: it gets the whole blood pit and then dies to some whiterock
	 * 		v2: there seems to be a vertex insanely close to a generated line that isn't considered in
	 */

	public static void main(String[] args) {
		renderer = new Renderer();
		
		loadConfig();
		
		JFrame frame = new JFrame();
		frame.setTitle(WINDOW_TITLE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(renderer);

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public static void loadConfig() {
		File f = new File("config.ini");
		try {
			List<String> lines = Files.readAllLines(f.toPath());
			for(String line : lines) {
				line = line.trim();
				if(line.startsWith("#") || line.isEmpty()) continue;

				int idx = line.indexOf('=');
				if(idx == -1) continue;

				String key = line.substring(0, idx).trim().toLowerCase().replaceAll("[- _]", "");
				String value = line.substring(idx + 1).trim();
				switch(key) {
					case "wads":
						loadWads(value.split(";"));
						break;
					case "map":
					case "mapname":
						mapName = value;
						break;
					case "antialias":
						renderer.antialias = parseAsBoolean(value);
						break;
					case "renderlit":
						TriangulateThread.renderLit = parseAsBoolean(value);
						break;
					case "vertextime":
						TriangulateThread.vertexSleepTime = parseAsInt(value, "Vertex sleep time", 0, TriangulateThread.vertexSleepTime);
						break;
					case "linetime":
						TriangulateThread.lineSleepTime = parseAsInt(value, "Line sleep time", 0, TriangulateThread.lineSleepTime);
						break;
					case "pausetime":
					case "sleeptime":
						TriangulateThread.sleepTime = parseAsInt(value, "Sleep time", 1, TriangulateThread.sleepTime);
						break;
					case "searchtime":
						TriangulateThread.searchTime = parseAsInt(value, "Search time", 0, TriangulateThread.searchTime);
						break;
					case "fixtime":
						TriangulateThread.fixTime = parseAsInt(value, "Fix time", 0, TriangulateThread.fixTime);
						break;
					case "subverttime":
						TriangulateThread.subvertTime = parseAsInt(value, "Subvert time", 1, TriangulateThread.subvertTime);
						break;
					case "startsector":
						TriangulateThread.startSector = parseAsInt(value, "Start sector", 0, TriangulateThread.startSector);
						break;
					case "endsector":
						TriangulateThread.endSector = parseAsInt(value, "End sector", -1, TriangulateThread.endSector);
						break;
					case "drawgrid":
						renderer.drawGrid = parseAsBoolean(value);
						break;
					case "backgroundcolor":
						renderer.backgroundColor = parseAsColor(value, "Background color", renderer.backgroundColor);
						break;
					case "axiscolor":
						renderer.axisColor = parseAsColor(value, "Axis color", renderer.axisColor);
						break;
					case "majorgridcolor":
						renderer.gridColorMajor = parseAsColor(value, "Major grid color", renderer.gridColorMajor);
						break;
					case "minorgridcolor":
						renderer.gridColorMinor = parseAsColor(value, "Minor grid color", renderer.gridColorMinor);
						break;
					case "majorgridsize":
						renderer.gridSizeMajor = parseAsInt(value, "Major grid size", 1, renderer.gridSizeMajor);
						break;
					case "minorgridsize":
						renderer.gridSizeMinor = parseAsInt(value, "Minor grid size", 1, renderer.gridSizeMinor);
						break;
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private static int parseAsInt(String value, String friendlyName, int minValue, int oldValue) {
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
		return oldValue;
	}
	
	private static boolean parseAsBoolean(String value) {
		try {
			int val = Integer.parseInt(value);
			return val > 0;
		} catch(NumberFormatException nfe) {
			return Boolean.parseBoolean(value);
		}
	}
	
	private static Color parseAsColor(String value, String friendlyName, Color oldValue) {
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
		return oldValue;
	}

	private static void loadWads(String[] wadPaths) {
		wads.clear();
		topWad = null;

		for(String path : wadPaths) {
			path = path.trim();
			try {
				WADFile wad = new WADFile(path);
				wads.put(path, wad);
				if(topWad != null) wad.parent = topWad;
				topWad = wad;
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void forceStop() {
		if(thread != null) {
			thread.toggleSuspend();
		}
	}

	public static void triangulate() {
		if(thread != null) {
			thread.shouldStop = true;
			thread.interrupt();
			try {
				Thread.sleep(TriangulateThread.sleepTime);
			} catch(InterruptedException ie) {};
		}
		thread = new TriangulateThread();
		thread.start();
	}
}
