package tri;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import tri.types.WADFile;

public class Demo {
	public static final String			WINDOW_TITLE	= "Sector Triangulation Demo";
	
	private static Config				config			= new DoomTriConfig();
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
		
		config.load();
		
		JFrame frame = new JFrame();
		frame.setTitle(WINDOW_TITLE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(renderer);

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	public static Config getConfig() {
		return config;
	}
	
	public static void loadConfig() {
		Config parsingConfig = new DoomTriConfig();
		config.copyTo(parsingConfig);
		parsingConfig.load();
		config = parsingConfig;
	}

	static void loadWads(String[] wadPaths) {
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
