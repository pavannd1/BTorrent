public class Interested {
	static int messageLength = 0;
	static int messageType = 2;
	public static void shouldSendInterested() throws Exception {
		int[] piecesPresent = Filecheck.getChunkArray();
		int[] neighborPieces = FileShare.rcvBitArray;
		int flag = 0;
		for (int i = 0; i < piecesPresent.length; i++) 
		{
			if (piecesPresent[i] == 0 && neighborPieces[i] == 1) {
				sendInterestedMessage();
				flag = 1;
				break;
			}
		}
		if (flag == 0) {
			notInterested.sendNotInterestedMessage();
		}

	}
	public static void sendInterestedMessage() throws Exception {
		String bitStr = "0"+messageLength+messageType;
		byte[] byteChunk = bitStr.getBytes();
		FileShare.sendByte(byteChunk);
		//System.out.println("Interested Message "+new String(byteChunk));
	}
}

class notInterested {
	static int messageLength = 0;
	static int messageType = 3;
	public static void sendNotInterestedMessage() throws Exception {
		String bitStr = "0"+messageLength+messageType;
		byte[] byteChunk = bitStr.getBytes();
		FileShare.sendByte(byteChunk);
	}
}
