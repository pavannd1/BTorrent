import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
	int serverPort;
	Socket connectionSocket;
	ServerSocket welcomeSocket;

	public Server(int port) {
		serverPort = port;
	}

	@Override
	public void run() {
		try {
			welcomeSocket = new ServerSocket(serverPort);
			System.out.println("Server Running...");
			while (true) {
				connectionSocket = welcomeSocket.accept();
				InputStream inFromRemote = connectionSocket.getInputStream();
				int bytesRead;
				byte[] rx = new byte[10000];
				bytesRead = inFromRemote.read(rx);
				System.out.println("log:Total Bytes read> " + bytesRead);
				FileShare.rxPacket(rx);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				welcomeSocket.close();
				connectionSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
