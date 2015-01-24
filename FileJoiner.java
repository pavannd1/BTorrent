import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileJoiner {
	public static void main(String[] args) throws IOException {
		// And for joining file, I put the names of all chunks in a List,
		// then sort it by name and then run the following code:

		FileOutputStream fos;
		FileInputStream fis;
		byte[] fileBytes;
		String fname = "newFile.mp3";
		File ofile = new File(fname);
		fos = new FileOutputStream(ofile, true);
		int bytesRead = 0;
		for (int i = 0; i <= 9; i++) {
			String inFile = "src/file/somefile.mp3.part" + i;
			//String inFile = "src/file/somefile.mp3";
			System.out.println("part "+i+" "+inFile);
			try {
				File ifile = new File(inFile);
				fis = new FileInputStream(inFile);
				//fileBytes = new byte[(int) inFile.length()];
				fileBytes = new byte[(int) ifile.length()];
				bytesRead = fis.read(fileBytes, 0, (int) ifile.length());
				System.out.println("part "+i+" "+fileBytes.length+ " "+ bytesRead+" "+ inFile.length());
				assert (bytesRead == fileBytes.length);
				assert (bytesRead == (int) ifile.length());
				fos.write(fileBytes);
				fos.flush();
				fileBytes = null;
		        fis.close();
		        fis = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		 fos.close();
		 fos = null;

	}
}
