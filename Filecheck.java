import java.io.*;

public class Filecheck {
	public static int[] getChunkArray() { 
		String fname = "src/file/somefile.mp3";
		int[] egarray = new int[10];// need to get number of chunks the file has
		// been split into and give that size
		String filename;
		for (int i = 0; i < 10; i++) {
			filename = fname + ".part" + i;
			File f = new File(filename);
			if (f.exists() && !f.isDirectory()) {
				egarray[i] = 1;
			} else {
				egarray[i] = 0;
			}
		}
		return egarray;
	}
	public static void main(String[] args) {
		String fname = "src/file/somefile.mp3";
		// int num=new File(ifile.getParentFile().getAbsolutePath()).listFiles().length;
		int[] egarray = new int[10];// need to get number of chunks the file has
									// been split into and give that size
		String filename;
		/***
		 * need to get number of chunks the file has been split into and loop
		 * till that number
		 */
		for (int i = 0; i < 10; i++) {
			filename = fname + ".part" + i;
			File f = new File(filename);
			if (f.exists() && !f.isDirectory())
				egarray[i] = 1;
			else
				egarray[i] = 0;
			System.out.println(egarray[i] + "\t" + filename);
		}
	}
}
