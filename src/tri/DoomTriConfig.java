package tri;

public class DoomTriConfig extends Config {
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
			default:
				return false;
		}
		return true;
	}
	
	
}
