import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileSplitter {
	public static void main(String[] args) {
		String fname = "src/file/somefile.mp3";
		int Chunk_Size = 1000000;
		File ifile = new File(fname);
		FileInputStream fis;
		String newName;
		FileOutputStream chunk;
		int fileSize = (int) ifile.length();
		int nChunks = 0, read = 0, readLength = Chunk_Size;
		byte[] byteChunk;
		try {
			fis = new FileInputStream(ifile);
			// StupidTest.size = (int)ifile.length();
			while (fileSize > 0) {
				if (fileSize <= Chunk_Size) {
					readLength = fileSize;
				}
				byteChunk = new byte[readLength];
				read = fis.read(byteChunk, 0, readLength);
				fileSize -= read;
				assert (read == byteChunk.length);
				nChunks++;
				newName = fname + ".part" + Integer.toString(nChunks - 1);
				chunk = new FileOutputStream(new File(newName));
				chunk.write(byteChunk);
				chunk.flush();
				chunk.close();
				byteChunk = null;
				chunk = null;
			}
			fis.close();
			fis = null;
			System.out.println("done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
