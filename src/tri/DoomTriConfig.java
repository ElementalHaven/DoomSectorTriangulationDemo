package tri;

public class DoomTriConfig extends Config {
	private KeyBinding	bindingRun		= new KeyBinding("R", "DOOMTRI:RUN", () -> Demo.triangulate());
	private KeyBinding	bindingSuspend	= new KeyBinding("S", "DOOMTRI:PAUSE", () -> Demo.suspend());
	
	@Override
	public void init(Renderer renderer) {
		super.init(renderer);
		
		renderer.addKeyBinding(bindingRun);
		renderer.addKeyBinding(bindingSuspend);
	}
	
	@Override
	public boolean handleConfigLine(String key, String value) {
		switch(key) {
			case "wads":
				Demo.loadWads(value.split(";"));
				break;
			case "map":
			case "mapname":
				Demo.mapName = value;
				break;
			case "renderlit":
				TriangulateThread.renderLit = valueAsBoolean(value);
				break;
			case "vertextime":
				TriangulateThread.vertexSleepTime = valueAsInt(value, "Vertex sleep time", 0, TriangulateThread.vertexSleepTime);
				break;
			case "linetime":
				TriangulateThread.lineSleepTime = valueAsInt(value, "Line sleep time", 0, TriangulateThread.lineSleepTime);
				break;
			case "pausetime":
			case "sleeptime":
				TriangulateThread.sleepTime = valueAsInt(value, "Sleep time", 1, TriangulateThread.sleepTime);
				break;
			case "searchtime":
				TriangulateThread.searchTime = valueAsInt(value, "Search time", 0, TriangulateThread.searchTime);
				break;
			case "fixtime":
				TriangulateThread.fixTime = valueAsInt(value, "Fix time", 0, TriangulateThread.fixTime);
				break;
			case "subverttime":
				TriangulateThread.subvertTime = valueAsInt(value, "Subvert time", 1, TriangulateThread.subvertTime);
				break;
			case "startsector":
				TriangulateThread.startSector = valueAsInt(value, "Start sector", 0, TriangulateThread.startSector);
				break;
			case "endsector":
				TriangulateThread.endSector = valueAsInt(value, "End sector", -1, TriangulateThread.endSector);
				break;
			case "keypause":
				updateKeyBinding(bindingSuspend, "Pause key", value);
				break;
			case "keyrun":
				updateKeyBinding(bindingRun, "Run key", value);
				break;
			default:
				return false;
		}
		return true;
	}
	
	@Override
	public Config createCopy() {
		DoomTriConfig config = new DoomTriConfig();
		copyTo(config);
		return config;
	}
	
	public void copyTo(DoomTriConfig config) {
		super.copyTo(config);
		
		config.bindingRun = bindingRun;
		config.bindingSuspend = bindingSuspend;
	}
}
