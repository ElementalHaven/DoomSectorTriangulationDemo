package tri;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tri.types.*;
import tri.types.WADFile.Lump;

public class TriangulateThread extends Thread {
	private static final int	PALETTE_COLORS	= 256;
	private static final int	ELEMS_PER_COLOR	= 3;
	private static final int	PALETTE_SIZE	= PALETTE_COLORS * ELEMS_PER_COLOR;
	
	private static Color[]		colors = {
		Color.GREEN,
		new Color(0, 127, 0),
		new Color(255, 127, 0),
		Color.RED,
		new Color(127, 63, 0)
	};
	
	public static int			sleepTime		= 256;
	public static int			vertexSleepTime	= 32;
	public static int			lineSleepTime	= 64;
	public static int			fixTime			= 0;
	public static int			searchTime		= 128;
	public static int			subvertTime		= 512;
	public static int			startSector		= 0;
	public static int			endSector		= -1;
	public static boolean		renderLit		= false;

	private static int toColor(byte[] pal, int off) {
		int r = pal[off] & 0xFF;
		int g = pal[off + 1] & 0xFF;
		int b = pal[off + 2] & 0xFF;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	// this may be redundant with interrupts now. I'm not sure -Liz (4/17/22)
	public boolean			shouldStop;
	private boolean			suspend = false;
	private WADFile			wad;

	private List<Vertex>	points	= new ArrayList<>();
	private List<Line>		lines	= new ArrayList<>();
	private List<Sector>	sectors	= new ArrayList<>();

	@Override
	public void run() {
		wad = Demo.topWad;
		
		try {
			loadMap();
			fixVanillaQuirks();
			mergeLineSegments();
			removeDegenerates();
			loadTextures();
			triangulate();
		} catch(InterruptedException e) {}
	}
	
	public void toggleSuspend() {
		suspend = !suspend;
	}
	
	private void update() throws InterruptedException {
		update(sleepTime);
	}
	
	private void update(int delay) throws InterruptedException {
		if(delay != 0) {
			Demo.renderer.repaint();
			Thread.sleep(delay);
		}
		while(suspend && !shouldStop) {
			Thread.sleep(16);
		}
		if(shouldStop) throw new InterruptedException();
	}

	private int[] getPalette() {
		Lump playpal = wad.getLump("PLAYPAL");
		int[] palette = new int[PALETTE_COLORS];
		if(playpal != null && playpal.data != null && playpal.data.capacity() >= PALETTE_SIZE) {
			byte[] tmp = new byte[PALETTE_SIZE];
			playpal.data.clear();
			playpal.data.get(tmp);
			for(int i = 0; i < PALETTE_COLORS; i++) {
				palette[i] = toColor(tmp, i * ELEMS_PER_COLOR);
			}
		} else {
			// A temp tan as a placeholder
			int col = toColor(new byte[] { -1, (byte) 211, (byte) 187 }, 0);
			Arrays.fill(palette, col);
		}
		
		// applying the colormap isn't explicitly needed for iwad resources, but it's probably a good idea regardless -Liz (4/15/22)
		return palette;
	}
	
	private int[][] getColormaps(int count) {
		int[][] maps = new int[count][PALETTE_COLORS];
		
		Lump lump = wad.getLump("COLORMAP");
		ByteBuffer data = lump == null ? null : lump.data;
		if(data != null) {
			data.clear();
		}
					
		for(int mapIdx = 0; mapIdx < count; mapIdx++) {
			if(data != null && data.remaining() >= PALETTE_COLORS) {
				for(int palIdx = 0; palIdx < PALETTE_COLORS; palIdx++) {
					maps[mapIdx][palIdx] = data.get() & 0xFF;
				}
			} else {
				// use dummy values if the colormap doesn't exist
				for(int palIdx = 0; palIdx < PALETTE_COLORS; palIdx++) {
					maps[mapIdx][palIdx] = palIdx;
				}
			}
		}
		return maps;
	}

	private void loadTextures() {
		// storing the count now both lets us get an arbitrary number of colormaps(only what we need)
		// and also makes sure that a config update at the current time wont break anything
		int cmapCount = renderLit ? 32 : 1;
		Map<String, BufferedImage> textures = new HashMap<>();
		int[] palette = getPalette();
		int[][] colormaps = getColormaps(cmapCount);

		for(Sector sector : sectors) {
			String texName = sector.floorTexName;
			// convert brightness to colormap by clamping, inverting, and dividing by total number of brightness colormaps
			int cmapIdx = (255 - Math.max(0, Math.min(sector.lightLevel, 255))) * cmapCount / 256;
			int[] colormap = colormaps[cmapIdx];
			// use fact that texture names are ASCII to make sure our delimiter can't possibly exist in a texture name
			String cacheName = texName + '\u0100' + cmapIdx;
			BufferedImage tex = textures.get(cacheName);
			if(tex == null) {
				final int size = Sector.FLAT_SIZE;
				tex = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
				int[] colors = new int[size * size];
				byte[] indices = new byte[size * size];
				Lump lump = wad.getLump(texName);
				if(lump != null && lump.data != null && lump.data.capacity() >= indices.length) {
					lump.data.clear();
					lump.data.get(indices);
				}
				for(int i = 0; i < colors.length; i++) {
					colors[i] = palette[colormap[indices[i] & 0xFF]];
				}
				tex.setRGB(0, 0, size, size, colors, 0, size);
				textures.put(cacheName, tex);
			}
			sector.renderable = new RenderableSector(tex);
		}
	}

	private ByteBuffer getMapLumpData(Lump mapLump, String name) {
		Lump lump = mapLump.getWad().getLump(name, mapLump.index);
		return lump == null ? null : lump.data;
	}

	private void loadMap() throws InterruptedException {
		String mapName = Demo.mapName;
		Lump map = null;
		if(mapName != null) {
			map = wad.getLump(mapName);
			if(map == null) {
				System.err.println("Lump for map " + mapName + " not found in the wad hierarchy.");
			}
		} else {
			System.out.println("A map name was not specified.");
		}
		if(map == null) {
			System.out.println("Attempting to load the first map that can be found.");
			Lump things = wad.getLump("THINGS");
			if(things != null) {
				map = things.getWad().getLump(things.index - 1);
			}
		}
		if(map == null) {
			System.err.println("No maps could be found to load");
			shouldStop = true;
			return;
		}

		Component comp = Demo.renderer.getParent();
		while(!(comp instanceof Frame)) {
			comp = comp.getParent();
		}
		String name = wad.path;
		int slashIdx = name.replace('\\', '/').lastIndexOf('/') + 1;
		((Frame) comp).setTitle(Demo.WINDOW_TITLE + " - " + name.substring(slashIdx) + " - " + map.name);
		
		System.out.println("Loading map \"" + map.name + '"');
		ByteBuffer bufSectors = getMapLumpData(map, "SECTORS");
		ByteBuffer bufVerts = getMapLumpData(map, "VERTEXES");
		ByteBuffer bufLines = getMapLumpData(map, "LINEDEFS");
		ByteBuffer bufSides = getMapLumpData(map, "SIDEDEFS");

		Demo.renderer.clear();
		update();
		Thread.sleep(3 * sleepTime);
		

		bufVerts.clear();
		System.out.println("Map contains " + (bufVerts.capacity() / 4) + " vertices");
		while(bufVerts.hasRemaining()) {
			Vertex v = new Vertex(bufVerts);
			v.id = points.size();
			int idx = points.indexOf(v);
			if(idx == -1) {
				points.add(v);
				Demo.renderer.addVertex(v);
			} else {
				System.err.println("Found duplicate vertex @ " + v + ". Merging with previous instance");
				points.add(points.get(idx));
			}
			update(vertexSleepTime);
		}
		// ensure we've done an update at least once for all the vertices
		if(vertexSleepTime == 0) update();

		bufSectors.clear();
		System.out.println("Map contains " + (bufSides.capacity() / 26) + " sectors");
		while(bufSectors.hasRemaining()) {
			Sector sector = new Sector();
			sector.id = sectors.size();
			sectors.add(sector);
			WADFile.skip(bufSectors, 4); // skip heights
			sector.floorTexName = WADFile.readString(bufSectors, 8);
			WADFile.skip(bufSectors, 8); // skip ceiling texture name
			sector.lightLevel = bufSectors.getShort();
			WADFile.skip(bufSectors, 4); // skip tag & type
		}

		bufSides.clear();
		System.out.println("Map contains " + (bufSides.capacity() / 30) + " sidedefs");
		List<Sector> sides = new ArrayList<>();
		while(bufSides.hasRemaining()) {
			WADFile.skip(bufSides, 28);
			Sector s = sectors.get(bufSides.getShort() & 0xFFFF);
			sides.add(s);
		}

		bufLines.clear();
		System.out.println("Map contains " + (bufLines.capacity() / 14) + " linedefs");
		while(bufLines.hasRemaining()) {
			Line line = new Line();

			line.start = points.get(bufLines.getShort() & 0xFFFF);
			line.start.addLine(line);
			line.end = points.get(bufLines.getShort() & 0xFFFF);
			line.end.addLine(line);
			
			line.calculateAngle();

			WADFile.skip(bufLines, 6);

			int side = bufLines.getShort() & 0xFFFF;
			line.front = side == WADFile.NO_SIDEDEF ? null : sides.get(side);
			if(line.front != null) line.front.addLine(line);

			side = bufLines.getShort() & 0xFFFF;
			line.back = side == WADFile.NO_SIDEDEF ? null : sides.get(side);
			if(line.back != null) line.back.addLine(line);

			lines.add(line);
			Demo.renderer.addLine(line);
			update(lineSleepTime);
		}
		// ensure we've done an update at least once for all the lines
		if(lineSleepTime == 0) update();
	}
	
	private void fixVanillaQuirks() throws InterruptedException {
		System.out.println("Fixing sector issues");
		
		// this actually works for E1M3 under the last resort rule
		for(Sector sector : sectors) {
			tryCloseSector(sector);
		}
	}
	
	/**
	 * Makes sure the specified sector is closed, pulling in and altering adjacent one-sided lines if something seems wrong
	 */
	private void tryCloseSector(Sector sector) throws InterruptedException {
		int lineCount = sector.lines.size();
		if(lineCount < 3) {
			System.err.println("  Sector " + sector.id + " is guaranteed to be unclosed. It only has " + lineCount + " lines");
		}
		
		sector.prepForTriangulation();
		
		final Vertex start = sector.getTopLeftVertex();
		
		Vertex next = new Vertex();
		next.x = start.x;
		next.y = start.y + 50;
		
		Line currentLine = new Line();
		currentLine.end = start;
		currentLine.start = next;
		currentLine.calculateAngle();
		
		// XXX lines checked is a hack because sometimes the iterator gets stuck, probably from two sided lines
		int linesChecked = 0;
		
		// go in a circle using angle logic on adjacent lines using largest angle rather than smallest until we get back to start
		do {
			next = currentLine.end;
			
			double bestAngle = 0;
			Line bestLine = null;
			for(Line line : next.lines) {
				if(line.start == next && line.front == sector && line.back != sector) {
					double angle = currentLine.angleBetween(line);
					if(angle > bestAngle) {
						bestAngle = angle;
						bestLine = line;
					}
				}
			}
			
			// if we can't make another connection, use smallest angle on one-sided lines that are connected to create a new line
			if(bestLine == null) {
				bestAngle = Double.MAX_VALUE;
				
				for(Line line : next.lines) {
					if(line.end == next && line.back == null && line.front != sector) {
						line.flip();
						double angle = currentLine.angleBetween(line);
						line.flip();
						
						if(angle < bestAngle) {
							bestAngle = angle;
							bestLine = line;
						}
					}
				}
				
				if(bestLine != null) {
					System.out.println("  Making line from " + bestLine.end + " to " + bestLine.start + " double sided to be a part of sector " + sector.id);
					bestLine.back = sector;
					// have to flip it so the angle is right for the next segment
					bestLine.flip();
				} else {
					// if that didn't work, do a last resort generation of a linedef to the nearest vertex not a part of the current line and is also disconnected
					// this should fix generation on E3M8 and possibly E1M3
					int bestDistSq = Integer.MAX_VALUE;
					Vertex nearest = null;
					for(Vertex vert : sector.vertsToProcess) {
						if(vert != currentLine.end && vert != currentLine.start && vert.linesToProcess.size() == 1) {
							int distSq = vert.distSquared(next);
							if(distSq < bestDistSq) {
								bestDistSq = distSq;
								nearest = vert;
							}
						}
					}
					
					if(nearest != null) {
						System.out.println("  Last resort generation of linedef from " + next + " to " + nearest + " for sector " + sector.id + ". This may be buggy");
						
						bestLine = new Line();
						bestLine.start = next;
						bestLine.end = nearest;
						bestLine.front = sector;
						bestLine.calculateAngle();
						
						next.addLine(bestLine);
						nearest.addLine(bestLine);
					} else {
						System.err.println("  Failed to bridge lines for sector " + sector.id + " at " + next + ". Triangulation will fail when it reaches this sector");
						currentLine.color = Line.defaultColor;
						return;
					}
				}
				
				sector.addLine(bestLine);
				Demo.renderer.addLine(bestLine);
				update();
			}
			
			currentLine.color = Line.defaultColor;
			currentLine = bestLine;
			currentLine.color = Color.CYAN;
			update(fixTime);
			
			//linesChecked++;
		} while((currentLine.end != start) && (linesChecked < lineCount * 2));
		
		currentLine.color = Line.defaultColor;
		// we're using the normal sleep time, but we don't want to delay if the user wanted to skip rendering the fixing process
		update(fixTime == 0 ? 0 : sleepTime);
	}
	
	private void mergeLineSegments() {
		System.out.println("Merging redundant lines");
		// TODO back to back lines that have the same sectors and angle(or mirrored) should be merged into a single line
		System.out.println("  Unimplemented");
	}

	private void removeDegenerates() throws InterruptedException {
		System.out.println("Removing degenerate and useless lines");
		for(Sector sector : sectors) {
			for(Vertex v : sector.vertsToProcess) {
				v.prepForSector(sector);
			}
			
			int removed;
			do {
				removed = 0;
				for(int i = sector.linesToProcess.size() - 1; i >= 0; i--) {
					Line line = sector.linesToProcess.get(i);
					if(line.isUselessForTriangulation()) {
						System.out.printf("  Removing useless line from %s to %s\n", line.start, line.end);
						line.remove(sector);
						removed++;
					}
				}
				if(removed > 0) {
					System.out.printf("  Removed %d lines from sector %d\n", removed, sector.id);
					update();
				}
			} while(removed > 0);
		}
		for(Vertex v : points) {
			if(v.lines.isEmpty()) {
				Demo.renderer.removeVertex(v);
			}
		}
		update();
	}
	
	private void triangulate() throws InterruptedException {
		System.out.println("Beginning triangulation");
		List<Line> badLines = new ArrayList<>();
		List<Vertex> insidePoints = new ArrayList<>();
		
		lines.forEach(line -> line.color = colors[4]);
		update(subvertTime);
		
		final int secCount = sectors.size();
		int startSec = startSector < 0 ? 0 : startSector;
		int endSec = endSector < 0 ? secCount : endSector;
		if(endSec > secCount) endSec = secCount;
		
		// exclude sectors that we said not to care about 
		for(int sectorIdx = 0; sectorIdx < startSec; sectorIdx++) {
			sectors.get(sectorIdx).removeAllLines();
			
		}
		for(int sectorIdx = endSec; sectorIdx < secCount; sectorIdx++) {
			sectors.get(sectorIdx).removeAllLines();
		}
		
		for(int sectorIdx = startSec; sectorIdx < endSec; sectorIdx++) {
			//System.out.println("Sector " + sectorIdx);
			Sector sector = sectors.get(sectorIdx);
			Demo.renderer.add(sector.renderable, -1);
			//Demo.renderer.sectors.add(sector.renderable);
			
			//System.out.println("  Prepping for triangulation");
			sector.prepForTriangulation();
			
			while(!sector.vertsToProcess.isEmpty()) {
				TriangulationState state = new TriangulationState();
				state.sector = sector;
				
				// find leftmost vertex since it is guaranteed to be on the outside of a sector
				state.vertB = sector.getTopLeftVertex();
				
				badLines.clear();
				while(true) {
					//System.out.println("Checking for line ending at " + vb);
					state.lineA = state.vertB.getAnyLineEndingHere(badLines);
					
					update(searchTime);
					
					if(state.lineA == null) {
						System.err.println("TRIANGULATION FAILURE: AN ERROR IN MAP DATA OR THE TRIANGULATION CODE CAUSED A FAILURE OBTAINING NEXT LINE TO PROCESS");
						System.err.printf("\tSector: %d (textured with %s)\n", sectorIdx, sector.floorTexName);
						System.err.println("\tVertex: " + state.vertB);
						state.vertB.color = Color.RED;
						update(1);
						return;
					}
					
					state.lineA.color = colors[0];
					state.vertA = state.lineA.start;
					//System.out.println("  Found line starts at " + va);
					state.vertA.color = colors[0];
					state.vertB.color = colors[1];
					update(searchTime);
					
					double bAngle = Float.MAX_VALUE;
					//System.out.println("  Checking for next line from " + vb.linesToProcess.size() + " lines");
					for(Line line : state.vertB.linesToProcess) {
						// don't use the first line as second line and require line be next in a potential sequence
						if(line == state.lineA || line.start != state.vertB) continue;
						
						double angle = state.lineA.angleBetween(line);
						//System.out.printf("    Angle to %s = %d\n", line.end, (int) Math.toDegrees(angle));
						// we don't want angles >= 180deg
						if(angle >= Math.PI) continue;
						//if(angle <= 0 || angle >= Math.PI) continue;
						if(angle < bAngle) {
							bAngle = angle;
							state.lineB = line;
						}
					}
					
					//System.out.printf("Chose v%d (%d deg)\n", lineB.end.id, (int) Math.toDegrees(bAngle));
					
					if(state.lineB == null) {
						badLines.add(state.lineA);
						state.lineA.resetColor();
						state.vertA.color = state.vertB.color = Vertex.defaultColor;
						
						state.vertB = state.vertA;
						state.vertA = null;
						continue;
					}
					
					state.lineB.color = colors[1];
					state.vertC = state.lineB.end;
					state.vertC.color = colors[2];
					update();
					
					int lineCount = sector.linesToProcess.size();
					state.lineC = sector.findOrCreateLine(state.vertC, state.vertA);
					state.cGenerated = sector.linesToProcess.size() != lineCount;
					state.lineC.color = colors[2];
					update();
					
					Triangle triangle = new Triangle(state);
					
					// we can't make a triangle if there are points inside
					while(triangle.getPointsInside(sector.vertsToProcess, insidePoints) > 0) {
						insidePoints.forEach((Vertex v) -> v.color = colors[3]);
						update();
						
						state.vertC.color = Vertex.defaultColor;
						
						if(insidePoints.size() == 1) {
							// skip math if theres only 1 point
							state.vertC = insidePoints.get(0);
						} else {
							// the cheat test: if one of the inside points has a line connecting it
							// to the first point of the triangle, it's likely a valid triangle
							// we do an angle test just to be safe
							Vertex best = null;
							bAngle = Double.MAX_VALUE;
							for(Line line : state.vertA.linesToProcess) {
								if(line.end == state.vertA && insidePoints.contains(line.start)) {
									double angle = line.angleBetween(state.lineA);
									if(angle < bAngle) {
										best = line.start;
										bAngle = angle;
										// we don't change lineC here because the old line might need removed if it was generated
									}
								}
							}
							
							if(best == null) {
								// we're checking distance to the 2nd line as well in case there's two or more equally distant lines from A
								// yes, we're using == on float point values, but hopefully it should be consistent enough 
								double closestDist = Double.MAX_VALUE;
								double closestAlt = Double.MAX_VALUE;
								for(Vertex v : insidePoints) {
									double dist = state.lineA.distanceTo(v);
									double alt = state.lineB.distanceTo(v);
									//System.out.println("  Line distance is " + dist);
									if((dist < closestDist) || (dist == closestDist && alt < closestAlt)) {
										best = v;
										closestDist = dist;
										closestAlt = alt;
									}
								}
							}
							state.vertC = best;
						}
						
						state.lineB.resetColor();
						if(state.bGenerated) {
							state.lineB.remove(sector);
						}
						triangle.c = state.vertC;
						if(state.cGenerated) {
							state.lineC.remove(sector);
						}
						
						lineCount = sector.linesToProcess.size();
						state.lineB = sector.findOrCreateLine(state.vertB, state.vertC);
						state.bGenerated = sector.linesToProcess.size() != lineCount;
						
						lineCount = sector.linesToProcess.size();
						state.lineC = sector.findOrCreateLine(state.vertC, state.vertA);
						state.cGenerated = sector.linesToProcess.size() != lineCount;

						insidePoints.forEach((Vertex v) -> v.color = Vertex.defaultColor);
						state.lineB.color = colors[1];
						state.vertC.color = colors[2];
						state.lineC.color = colors[2];
						update();
					}
					
					sector.renderable.addTriangle(triangle);
					resetColors(state);
					update();
					
					//System.out.println("  Removing primary line");
					state.lineA.remove(sector);
					if(state.bGenerated) {
						//System.out.println("  Flipping secondary line");
						state.lineB.flip();
					} else {
						//System.out.println("  Removing secondary line");
						state.lineB.remove(sector);
					}
					if(state.cGenerated) {
						//System.out.println("  Flipping tertiary line");
						state.lineC.flip();
					} else {
						//System.out.println("  Removing tertiary line");
						state.lineC.remove(sector);
					}
					
					break;
				}
			}
			update();
		}
		System.out.println("Triangulation Complete");
	}
	
	private void resetColors(TriangulationState state) {
		state.vertA.color = state.vertB.color = state.vertC.color = Color.WHITE;
		state.lineA.resetColor();
		state.lineC.resetColor();
		state.lineB.resetColor();
	}
}