import java.io.BufferedReader;
import java.io.FileReader;

public class Config {
	/**
	 * All time intervals are in seconds
	 */
	int NumberOfPreferredNeighbors, UnchokingInterval,
			OptimisticUnchokingInterval, fileSize, pieceSize;
	String fileName;
	/**
	 * peerList array :
	 * [peer ID] [host name] [listening port] [has file or not] 

	 */
	String[][] peerList=new String[6][4];

	public static void main(String[] args) throws Exception {
		Config c = new Config();
		c.readCommon();
		c.printCommon();
	}

	public void readCommon() throws Exception {

		String line;
		String fname = "src/configFiles/Common.cfg";
		FileReader fileReader = new FileReader(fname);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		while ((line = bufferedReader.readLine()) != null) {
			String[] splits = line.split(" ");
			if (splits[0].equals("NumberOfPreferredNeighbors")) {
				NumberOfPreferredNeighbors = Integer.parseInt(splits[1]);
			} else if (splits[0].equals("UnchokingInterval")) {
				UnchokingInterval = Integer.parseInt(splits[1]);
			} else if (splits[0].equals("OptimisticUnchokingInterval")) {
				OptimisticUnchokingInterval = Integer.parseInt(splits[1]);
			} else if (splits[0].equals("FileName")) {
				fileName = (splits[1]);
			} else if (splits[0].equals("FileSize")) {
				fileSize = Integer.parseInt(splits[1]);
			} else if (splits[0].equals("PieceSize")) {
				pieceSize = Integer.parseInt(splits[1]);
			}
		}
		bufferedReader.close();
	}

	public void printCommon() {
		System.out.println(NumberOfPreferredNeighbors + " " + UnchokingInterval
				+ " " + OptimisticUnchokingInterval + " " + fileSize + " "
				+ pieceSize);
		System.out.println(fileName);
	}

	public void readPeerInfo() throws Exception {

		String line;
		String fname = "src/configFiles/PeerInfo.cfg";
		FileReader fileReader = new FileReader(fname);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		int i=0;
		while ((line = bufferedReader.readLine()) != null) {
			String[] splits = line.split(" ");
			peerList[i][0]=splits[0];
			peerList[i][1]=splits[1];
			peerList[i][2]=splits[2];
			peerList[i][3]=splits[3];
			i++;
		}
	}
}
