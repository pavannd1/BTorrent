public class BitField {
	static int messageLength;
	static int messageType = 5;
	int[] bitFieldArray;

	public static byte[] arrayToByte(int[] bitArray) {
		// TODO add a method in Filecheck to return the array
		bitArray = Filecheck.getChunkArray();
		messageLength = bitArray.length;
		int i = 0;
		String bitStr = ""+messageLength+messageType;
		while (i < messageLength) {
			bitStr += bitArray[i];
			i++;
		}
		byte[] byteChunk = bitStr.getBytes();
		System.out.println(new String(byteChunk));
		return byteChunk;
	}

	public static void sendByteBitField() throws Exception {
		int[] bitArray = new int[10];
		byte[] bitFieldByte = arrayToByte(bitArray);
		FileShare.sendByte(bitFieldByte);
	}
}
