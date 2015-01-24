

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ByteTest {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		// String remoteIP = args[0];
		// int remotePort = Integer.parseInt(args[1]);
		System.out.println("Hi...");
		bytechatServerbyte cs = new bytechatServerbyte();
		Thread t = new Thread(cs);
		t.setName("Service");
		t.start();
		System.out.println("Hello...");
		String remoteIP = "localhost";
		int remotePort = 6792;
		String sentence;
		Thread.sleep(2000);

		System.out.println("Client Running...");
		while (true) {
			Socket clientSocket = new Socket(remoteIP, remotePort);
			BufferedReader inFromUser = new BufferedReader(
					new InputStreamReader(System.in));
			sentence = inFromUser.readLine();
			byte[] buff = sentence.getBytes();
			System.out.println("you /> " + sentence + "\n");
			OutputStream outToServer = clientSocket.getOutputStream();
			outToServer.write(buff);
		}
	}

	static byte[] concatBytes(byte a[], byte b[]) {
		byte[] result = new byte[((int) a.length) + ((int) b.length)];
		int i=0;
		while(i<((int)a.length))
		{
			result[i]=a[i];
			i++;
		}
		int j=0;
		while(j<((int)b.length))
		{
			result[i]=b[j];
			i++;
			j++;
		}
		return result;
	}
}

class bytechatServerbyte implements Runnable {
	String clientSentence;

	@Override
	public void run() {
		try {
			@SuppressWarnings("resource")
			ServerSocket welcomeSocket = new ServerSocket(6792);
			System.out.println("Server Running...");
			while (true) {
				Socket connectionSocket = welcomeSocket.accept();
				InputStream inFromRemote = connectionSocket.getInputStream();
				int bytesRead;
				byte[] msg = new byte[32773];
				bytesRead = inFromRemote.read(msg);
				String pktSize="";
				for (int i = 0; i < 4 ; i++) {
					pktSize += (char) msg[i];
				}
				int payLoadSize=Integer.parseInt(pktSize);
				String pktType= ""+ (char) msg[4];
				String payLoad = "";
				for (int i = 5; i < 5+payLoadSize; i++) {
					payLoad += (char) msg[i];
				}
				System.out.println("Received Message Length " + payLoadSize);
				System.out.println("Received Message Type " + pktType);
				System.out.println("Received Message Payload " + payLoad);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
