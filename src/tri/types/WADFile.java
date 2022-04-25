package tri.types;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class WADFile {
	public static String readString(ByteBuffer buf, int maxChars) {
		byte[] tmp = new byte[maxChars];
		buf.get(tmp);
		int len;
		for(len = 0; len < maxChars; len++) {
			if(tmp[len] == '\0') break;
		}
		return new String(tmp, 0, len);
	}
	
	public static ByteBuffer skip(ByteBuffer buf, int count) {
		buf.position(buf.position() + count);
		return buf;
	}
	
	public class Lump {
		public final int index;
		public final String name;
		public final ByteBuffer data;
		
		private Lump(int index, ByteBuffer buf) {
			this.index = index;
			
			
			int offset = buf.getInt();
			int size = buf.getInt();
			name = readString(buf, 8);
			
			if(size <= 0) {
				data = null;
			} else {
				int end = buf.position();
				
				buf.position(offset).limit(offset + size);
				data = buf.slice();
				data.order(ByteOrder.LITTLE_ENDIAN);
				buf.limit(buf.capacity());
				
				buf.position(end);
			}
		}
		
		public WADFile getWad() {
			return WADFile.this;
		}
		
		public String toString() {
			return String.format("%s[len=0x%04X]", name, data == null ? 0 : data.capacity());
		}
	}
	
	// memory is plentiful enough nowadays that it should be fine to store entire wads
	private ByteBuffer data;
	private Lump[] lumps;
	public final String path;
	
	public WADFile parent;
	
	public WADFile(String path) throws IOException {
		this.path = path;
		File f = new File(path);
		int length = (int) f.length();
		data = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
		try(FileChannel channel = FileChannel.open(f.toPath(), StandardOpenOption.READ)) {
			while(data.hasRemaining()) {
				channel.read(data);
			}
		}
		data.flip();
		
		data.position(4); // skip magic. it's not really useful to us unless we're validating stuff
		int lumpCount = data.getInt();
		int lumpsOffset = data.getInt();
		
		data.position(lumpsOffset);
		lumps = new Lump[lumpCount];
		for(int i = 0; i < lumpCount; i++) {
			lumps[i] = new Lump(i, data);
		}
	}
	
	public Lump getLump(int index) {
		if(index < 0 || index >= lumps.length) {
			throw new ArrayIndexOutOfBoundsException("Offset must be between 0 and " + lumps.length + " exclusive");
		}
		return lumps[index];
	}
	
	public Lump getLump(String name) {
		Lump lump = getLump(name, 0);
		if(lump == null && parent != null) {
			lump = parent.getLump(name);
		}
		return lump;
	}
	
	public Lump getLump(String name, int offset) {
		if(offset < 0 || offset >= lumps.length) {
			throw new ArrayIndexOutOfBoundsException("Offset must be between 0 and " + lumps.length + " exclusive");
		}
		if(name == null) {
			throw new IllegalArgumentException("Lump name must not be null");
		}
		for(int i = offset; i < lumps.length; i++) {
			Lump l = lumps[i];
			if(name.equals(l.name)) return l;
		}
		return null;
	}
	
	public int getLumpCount() {
		return lumps.length;
	}
}
