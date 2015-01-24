import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class peerProcess {

	// Contains the peerID,port,bitField of the current machine
	public static String myPeerId;
	int portNumber;
	public static BitSet mybitmap;
	private ServerSocket serverSock;
	private BufferedReader inp;

	// Configuration Parameters
	private static int numberOfPreferredNeighbors;
	private static int unchokingInterval;
	private static int optimisticUnchokingInterval;
	private static String fileName;
	private static int fileSize;
	private static int pieceSize;
	public static int totalPieces;

	// Maps
	public static Map<String, Socket> socketsMap = new HashMap<String, Socket>();
	public static Map<String, Connection> connMap = new ConcurrentHashMap<String, Connection>();
	public static HashMap<String, Integer> downloadRate = new HashMap<String, Integer>();
	public static Map<String, Vector<Integer>> requestedMap = new ConcurrentHashMap<String, Vector<Integer>>();

	// Vectors
	public static volatile Vector<String> validRequestList = new Vector<String>();
	public static Vector<Connection> peersInterestedVec = new Vector<Connection>();
	public static volatile Vector<String> unchokedVec = new Vector<String>();
	public static Vector<String> prefNbr = new Vector<String>();

	// Neighbors files
	public static Map<String, BitSet> bitMap = new ConcurrentHashMap<String, BitSet>();

	// Flags
	public static volatile Boolean iFile = false;
	public static boolean theEnd = false;
	public static volatile Boolean iComp = false;

	// Explicit lock object
	public static Object lock = new Object();

	// Counters
	public static int myPieceCount = 0;

	// Connection Objects
	public static volatile Connection currOptNbr = null;
	public static Vector<Thread> ConnectionThread = new Vector<Thread>();

	// Temporary variables
	String st;
	public static Logger logObj = null;

	public static void main(String[] args) {
		final peerProcess peerObj = new peerProcess();
		peerObj.talkToNeighbours(args);
	}

	public void talkToNeighbours(String args[]) {

		// LoadConfiguration
		try {
			readCommon();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// startTimer that schedules tasks
		scheduleTasks();

		// set my peerID from command line args
		// myPeerId = args[0];
		myPeerId = new String("1001");
		// Create a new directory for storing the files
		new File("peer_" + myPeerId).mkdir();
		// get the port number of the current peer
		portNumber = getPortNumber();
		logObj = new Logger(myPeerId);

		try {
			// Create a server socket at the portNumber of the peer
			serverSock = new ServerSocket(portNumber);

			inp = new BufferedReader(new FileReader("src/PeerInfo.cfg"));
			// Loop to read all peers from the peerConfigFile
			while ((st = inp.readLine()) != null) {

				final String[] tokens = st.split("\\s+");
				System.out.println(" Token " + tokens[1]);
				// read the peer id of others from the peer configuration file
				// and compare them with this peer ID
				int otherPeer = tokens[0].compareTo(myPeerId);
				System.out.println(" otherPeer " + otherPeer);
				// if the read peer id is the same as the current peer id
				if (otherPeer == 0) {
					// Check if this peer has the full file
					if (tokens[3].equals("1")) {
						mybitmap.set(0, totalPieces, true);
						iFile = true;
						fileSplitter();
						fileJoiner();
					}
					bitMap.put(myPeerId, mybitmap);
					continue;

				} else if (otherPeer > 0) {
					System.out.println(" here pavan!!");
					// Waiting until other peers are
					// ready
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
					Thread t1 = new Thread(new Runnable() {
						public void run() {
							System.out.println(" here pavan 12!!");
							Socket bindingSocket = null;
							try {
								// Create new binding socket for the other peer
								// SERVER
								bindingSocket = serverSock.accept();
								System.out.println(" here 2");
								System.out.println("Server to " + tokens[0]
										+ "  token1 " + tokens[1] + " token 2 "
										+ tokens[2]);
								synchronized (this) {
									// Set the sockets map with the current
									// socket
									socketsMap.put(tokens[0], bindingSocket);
								}

								// Save the connection parameters
								final Connection c = new Connection(tokens[0],
										tokens[1], Integer.parseInt(tokens[2]));
								c.setPeerSocket(bindingSocket);
								c.setOutputStream(new ObjectOutputStream(
										bindingSocket.getOutputStream()));
								c.setInputStream(new ObjectInputStream(
										bindingSocket.getInputStream()));

								logObj.logFrom(tokens[0]);
								/* Send HandShake */
								String sentence = "HELLO";
								for (int i = 0; i < 32; i++) {
									sentence += "0";
								}
								sentence += myPeerId;
								byte[] buff = sentence.getBytes();

								// c.getOutputStream().write(buff);
								c.getOutputStream().writeObject(
										new MsgContainer(buff));
								c.getOutputStream().flush();

								boolean handShake = false;
								MsgContainer recievedHandShake = null;
								/* Receiving HandShake */
								while (recievedHandShake == null) {
									try {
										recievedHandShake = (MsgContainer) c
												.getInputStream().readObject();
									} catch (Exception ex) {
										System.out
												.println("Something went wrong while rx handshake");
										ex.printStackTrace();
									}

								}
								// byte[] rx = new byte[10000];
								byte[] rx;
								rx = recievedHandShake.getBytes();

								String pktType = "";
								for (int i = 0; i < 5; i++) {
									pktType += (char) rx[i];
								}

								if (pktType.equals("HELLO")) {
									System.out.println("Received Packet type:"
											+ pktType + "from :" + tokens[0]);
									String t = "";
									for (int i = 37; i < 41; i++) {
										t += (char) rx[i];
									}
									int hisPeerId = Integer.parseInt(t);
									System.out
											.println(" his peer " + hisPeerId);
									if (tokens[0].equals(t)) {
										handShake = true;
										System.out.println("Shook hands with "
												+ hisPeerId);
									}
								}

								/*
								 * Send bit field message
								 */
								byte[] buffBF = constructBitFieldMsg(mybitmap);
								// c.getOutputStream().write(buffBF);
								c.getOutputStream().writeObject(
										new MsgContainer(buffBF));
								c.getOutputStream().flush();
								/*
								 * Update connection object to connection map
								 */
								synchronized (this) {
									connMap.put(tokens[0], c);
									downloadRate.put(tokens[0], 0);
								}

								/*
								 * Listen to messages indefinitely
								 */
								final Connection SafeConn = c;
								Thread Listen = new Thread(new Runnable() {
									public void run() {
										try {
											while (!theEnd) {
												try {
													/*
													 * Read Bytes and interpret
													 * message(i.e. Send it to
													 * processor)
													 */
													System.out
															.println("Here 1A");
													// byte[] msg = new
													// byte[fileSize + 5];

													System.out
															.println("Here 1B");
													MsgContainer m = (MsgContainer) SafeConn
															.getInputStream()
															.readObject();
													if (m == null)
														continue;
													byte[] msg = m.getBytes();
													System.out
															.println("Here 1C");
													String pktSize = "";
													for (int i = 0; i < 4; i++) {
														pktSize += (char) msg[i];
													}
													System.out
															.println("Here 1D");
													System.out
															.println("Here 1D1 "
																	+ pktSize);
													int payLoadSize = Integer
															.parseInt(pktSize,
																	16);
													System.out
															.println("Here 1E");
													String pktType = ""
															+ (char) msg[4];
													int pktTp = Integer
															.parseInt(pktType);
													System.out
															.println("Here 1E type "
																	+ pktTp);
													System.out
															.println("Here 1F "
																	+ payLoadSize);
													byte[] payLoad = new byte[1];
													if (payLoadSize > 0) {
														System.out
														.println("Here G where payloadsize is considerable");
														int j = 0;
														payLoad = new byte[payLoadSize];
														for (int i = 5; i < 5 + payLoadSize; i++) {
															System.out
																	.println("loop "
																			+ i
																			+ " "
																			+ msg[i]);
															payLoad[j] = msg[i];
															j++;

														}
													}
													System.out
															.println("Here 1H");
													HandleMsg hm = new HandleMsg(
															pktTp,
															payLoad,
															SafeConn.getPeerID());
													Thread msgProcess = new Thread(
															hm);
													msgProcess.start();
												} catch (Exception ex) {
													System.out
															.println("1 Exception thrown while listening messages for peer "
																	+ tokens[0]);
												}
											}

										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								});
								Listen.start();

							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
					t1.start();
					ConnectionThread.add(t1);
				}

				else {
					// Waiting until other peers are
					// ready
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
					Thread t2 = new Thread(new Runnable() {
						public void run() {
							Socket connSocket = null;
							try {

								System.out.println("CLIENT Connecting to "
										+ tokens[0] + "  token1 " + tokens[1]
										+ " token 2 " + tokens[2]);
								// Create new connection socket for the other
								// peer CLIENT
								while (true) {
									try {
										System.out.println("CLIENT trying to "
												+ tokens[0]);
										connSocket = new Socket(tokens[1],
												portNumber);
										System.out.println("out of  CLIENT "
												+ tokens[0]);
										if (connSocket != null) {
											System.out.println("done CLIENT "
													+ tokens[0]);
											break;
										}
									} catch (IOException e) {
										Thread.sleep(1000);
									}
								}
								synchronized (this) {
									// Set the sockets map with the current
									// socket
									socketsMap.put(tokens[0], connSocket);
								}

								// Save the connection parameters
								final Connection c = new Connection(tokens[0],
										tokens[1], Integer.parseInt(tokens[2]));
								logObj.logTo(tokens[0]);
								c.setPeerSocket(connSocket);
								c.setOutputStream(new ObjectOutputStream(
										connSocket.getOutputStream()));
								c.setInputStream(new ObjectInputStream(
										connSocket.getInputStream()));

								// ---
								/* Send HandShake */
								String sentence = "HELLO";
								for (int i = 0; i < 32; i++) {
									sentence += "0";
								}
								sentence += myPeerId;
								byte[] buff = sentence.getBytes();

								// c.getOutputStream().write(buff);
								c.getOutputStream().writeObject(
										new MsgContainer(buff));
								c.getOutputStream().flush();

								boolean handShake = false;
								MsgContainer recievedHandShake = null;
								/* Receiving HandShake */
								while (recievedHandShake == null) {
									try {
										recievedHandShake = (MsgContainer) c
												.getInputStream().readObject();
									} catch (Exception ex) {
										System.out
												.println("Something went wrong while rx handshake");
										ex.printStackTrace();
									}

								}
								// byte[] rx = new byte[10000];
								byte[] rx;
								System.out.println("the size is "
										+ recievedHandShake.getBytes());
								rx = recievedHandShake.getBytes();

								String pktType = "";
								for (int i = 0; i < 5; i++) {
									pktType += (char) rx[i];
								}
								if (pktType.equals("HELLO")) {
									System.out.println("Received Packet type:"
											+ pktType + "from :" + tokens[0]);
									String t = "";
									for (int i = 37; i < 41; i++) {
										t += (char) rx[i];
									}
									int hisPeerId = Integer.parseInt(t);
									System.out
											.println(" his peer " + hisPeerId);
									if (tokens[0].equals(t)) {
										handShake = true;
										System.out.println("Shook hands with "
												+ hisPeerId);
									}
								}

								/*
								 * Send bit field message
								 */
								byte[] buffBF = constructBitFieldMsg(mybitmap);
								c.getOutputStream().writeObject(
										new MsgContainer(buffBF));
								c.getOutputStream().flush();

								/*
								 * Update connection object to connection map
								 */
								synchronized (this) {
									connMap.put(tokens[0], c);
									downloadRate.put(tokens[0], 0);
								}

								/*
								 * Listen to messages indefinitely
								 */
								final Connection SafeConn = c;
								Thread Listen = new Thread(new Runnable() {
									public void run() {
										try {
											while (!theEnd) {
												try {
													/*
													 * Read Bytes and interpret
													 * message(i.e. Send it to
													 * processor)
													 */
													MsgContainer m = (MsgContainer) SafeConn
															.getInputStream()
															.readObject();
													if (m == null)
														continue;
													System.out.println("Read something!");
													byte[] msg = m.getBytes();
													String pktSize = "";
													for (int i = 0; i < 4; i++) {
														pktSize += (char) msg[i];
													}
													int payLoadSize = Integer
															.parseInt(pktSize,
																	16);
													System.out.println("payLoadSize is "+payLoadSize);
													String pktType = ""
															+ (char) msg[4];
													int pktTp = Integer
															.parseInt(pktType);

													byte[] payLoad = new byte[1];
													if (payLoadSize > 0) {
														int j = 0;
														payLoad = new byte[payLoadSize];
														for (int i = 5; i < 5 + payLoadSize; i++) {
															payLoad[j] = msg[i];
															j++;
														}
													}
													HandleMsg hm = new HandleMsg(
															pktTp,
															payLoad,
															SafeConn.getPeerID());
													Thread msgProcess = new Thread(
															hm);
													msgProcess.start();
												} catch (Exception ex) {
													System.out
															.println("2 Exception thrown while listening messages for peer "
																	+ tokens[0]);
												}
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								});
								Listen.start();

							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
					t2.start();
					ConnectionThread.add(t2);
				}
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}// end of talkToNeighbours

	/*
	 * Function that schedules the tasks
	 */
	public void scheduleTasks() {
		Thread taskScheduler = new Thread(new Runnable() {
			public void run() {
				while (connMap.size() == 0) {
					try {
						System.out.println("Connection object empty!");
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println("Connection object filled ***!");
				final Timer optNbrTimer = new Timer();
				optNbrTimer.schedule(new TimerTask() {
					public void run() {
						System.out.println("Running timer task! "
								+ optimisticUnchokingInterval);
						send_chokes();
						System.out.println("Chokes");
						downloadRateCalc();
						send_unchokes();
						getOptNbr();
					}
				}, 0, optimisticUnchokingInterval * 1000);

				final Timer finishTimer = new Timer();
				finishTimer.schedule(new TimerTask() {
					public void run() {
						if (completionChecker() == true) {
							System.out.println("File sharing complete");
							optNbrTimer.cancel();
							theEnd = true;
							finishTimer.cancel();
						}
					}
				}, 0, 10 * 1000);
			}
		});
		taskScheduler.start();
	}

	/*
	 * Function to get the Port Number of the current peer
	 */
	public int getPortNumber() {
		try {
			BufferedReader inp2 = new BufferedReader(new FileReader(
					"src/PeerInfo.cfg"));
			String st2 = inp2.readLine();
			String[] tokens2 = st2.split("\\s+");
			int portN = Integer.parseInt(tokens2[2]);
			inp2.close();
			return portN;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return 0;
	}// end of getPortNumber

	/*
	 * Read from Common.cfg file
	 */
	public void readCommon() throws Exception {
		String line;
		String fname = "src/Common.cfg";
		FileReader fileReader = new FileReader(fname);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		while ((line = bufferedReader.readLine()) != null) {
			String[] splits = line.split(" ");
			if (splits[0].equals("NumberOfPreferredNeighbors")) {
				numberOfPreferredNeighbors = Integer.parseInt(splits[1]);
			} else if (splits[0].equals("UnchokingInterval")) {
				unchokingInterval = Integer.parseInt(splits[1]);
			} else if (splits[0].equals("OptimisticUnchokingInterval")) {
				optimisticUnchokingInterval = Integer.parseInt(splits[1]);
			} else if (splits[0].equals("FileName")) {
				fileName = (splits[1]);
			} else if (splits[0].equals("FileSize")) {
				fileSize = Integer.parseInt(splits[1]);
			} else if (splits[0].equals("PieceSize")) {
				pieceSize = Integer.parseInt(splits[1]);
			}
		}
		bufferedReader.close();
		// Calculate the number of bits for bit field
		if (fileSize % pieceSize == 0) {
			totalPieces = fileSize / pieceSize;
		} else {
			totalPieces = (fileSize / pieceSize) + 1;
		}
		mybitmap = new BitSet(totalPieces);
	}// end of readCommon()

	/*
	 * File Splitter
	 */
	public static void fileSplitter() {
		try {
			int SPLIT_SIZE = pieceSize;
			// Place where the whole file is located
			// FileInputStream fis = new FileInputStream("./myDir/" + fileName);
			FileInputStream fis = new FileInputStream("src/file/" + fileName);
			byte buffer[] = new byte[SPLIT_SIZE];
			int count = 10000;
			while (true) {
				int i = fis.read(buffer, 0, SPLIT_SIZE);

				if (i == -1)
					break;
				String filename = "peer_" + myPeerId + "/" + count;
				FileOutputStream fos = new FileOutputStream(filename);
				fos.write(buffer, 0, i);
				fos.flush();
				fos.close();
				++count;
			}
			fis.close();
			// System.out.println("Done Splitting");
		} catch (Exception e) {
			System.out.println("Exception caught while splitting file");
			e.printStackTrace();
		}
	}// end of fileSplitter()

	/*
	 * File Joiner
	 */
	public synchronized static void fileJoiner() throws FileNotFoundException {
		// File f = new File("./" + "peer_" + myPeerId + "/" + fileName);
		File f = new File("src/file/peer_" + myPeerId + "/" + fileName);
		System.out.println("Joining 1");
		if (f.exists())
			return;
		int SPLIT_SIZE = pieceSize;
		File file = new File("peer_" + myPeerId);
		File[] files = file.listFiles();
		Arrays.sort(files);
		FileOutputStream fout = new FileOutputStream("./" + "peer_" + myPeerId
				+ "/" + fileName);
		FileInputStream segment;
		int length;
		try {
			for (int i = 0; i < files.length; i++) {
				System.out.println("Joining " + i);
				segment = new FileInputStream(files[i].getPath());
				byte[] buff = new byte[SPLIT_SIZE];
				while ((length = segment.read(buff)) > 0) {
					fout.write(buff, 0, length);

				}
				segment.close();
			}
			System.out.println("Done Joining");
			fout.close();
		} catch (Exception e) {
			System.out.println("Exception in file Splitter");
			e.printStackTrace();
		}
	}// end of fileJoiner()

	/*
	 * Construct BitField message
	 */
	public static byte[] constructBitFieldMsg(BitSet btmp) {
		int messageLength = totalPieces;
		int messageType = 5;
		int i = 0;
		System.out.println("Bitmap length " + messageLength);
		String ml = Integer.toHexString(messageLength);
		String bitStr = "";
		if (ml.length() <= 1) {
			bitStr += "000";
		} else if (ml.length() <= 2) {
			bitStr += "00";
		} else if (ml.length() <= 3) {
			bitStr += "0";
		}
		bitStr += ml + Integer.toString(messageType);
		while (i < totalPieces) {
			if (btmp.get(i)) {
				bitStr += "1";
			} else {
				bitStr += "0";
			}
			i++;
		}
		System.out.println("Bitmap is " + bitStr);
		byte[] byteChunk = bitStr.getBytes();
		return byteChunk;
	}

	/*
	 * Method to compare two given bitmaps
	 */
	public static synchronized int compareBitmap(BitSet BitMapIn) {
		ArrayList<Integer> missingChunks = new ArrayList<Integer>();
		if (iFile) {
			return -1;
		}
		int index = 0;
		while (index < totalPieces) {
			index = mybitmap.nextClearBit(index);
			if (BitMapIn.get(index)) {
				missingChunks.add(index);
			}
			index++;
		}
		if (missingChunks.size() == 0) {
			return -1;
		} else {
			return missingChunks
					.get(new Random().nextInt(missingChunks.size()));
		}
	}

	/*
	 * Method to check if a piece id was requested
	 */
	public static synchronized boolean isRequested(int PieceID) {
		Collection<Vector<Integer>> pieceIDs = requestedMap.values();
		Iterator<Vector<Integer>> valueIterator = pieceIDs.iterator();
		while (valueIterator.hasNext()) {
			Vector<Integer> requested = (Vector<Integer>) valueIterator.next();
			if (requested.contains(PieceID)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Method that sends REQUEST MESSAGE
	 */
	public static synchronized void sendRequest(
			ObjectOutputStream outputStream, int pieceid) throws IOException {
		String prep = "0004";
		prep += "6";
		if (pieceid < 10) {
			prep += "000";
		} else if (pieceid < 100) {
			prep += "00";
		} else if (pieceid < 1000) {
			prep += "0";
		}
		prep += "" + Integer.toString(pieceid);
		byte[] buff = prep.getBytes();
		outputStream.writeObject(new MsgContainer(buff));
		outputStream.flush();
	}

	/*
	 * Method that sends INTERESTED MESSAGE
	 */
	public static synchronized void sendInterested(
			ObjectOutputStream outputStream) throws IOException {
		String prep = "0000";
		prep += "2";
		byte[] buff = prep.getBytes();
		outputStream.writeObject(new MsgContainer(buff));
		outputStream.flush();
	}

	/*
	 * Method that sends NOT INTERESTED MESSAGE
	 */
	public static synchronized void sendNotInterested(
			ObjectOutputStream outputStream) throws IOException {
		String prep = "0000";
		prep += "3";
		byte[] buff = prep.getBytes();
		outputStream.writeObject(new MsgContainer(buff));
		outputStream.flush();
	}

	/*
	 * Get Bitset from raw bytes
	 */
	public static synchronized BitSet getBitSet(byte[] raw) {
		BitSet bt = new BitSet(totalPieces);
		String tmp = "";
		for (int i = 0; i < totalPieces; i++) {
			tmp = "" + (char) raw[i];
			if (tmp.equals("0")) {
				bt.set(i, false);
			} else {
				bt.set(i, true);
			}
		}
		return bt;
	}

	/*
	 * Get piece given its index
	 */
	public static synchronized byte[] getPiece(int pieceNumber) {
		pieceNumber += 10000;
		File pieceFile = new File("peer_" + myPeerId + "/" + pieceNumber);
		if (!pieceFile.exists()) {
			System.out.println("Piece requested does not exist. PieceNumber = "
					+ pieceNumber);
			return null;
		}
		int pieceFileSize = (int) pieceFile.length();
		System.out.println("Piece Length = "+ pieceFileSize);
		byte[] pieceFileBytes = new byte[(int) pieceFileSize];
		int bytesRead = 0;
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new FileInputStream(pieceFile));
			bytesRead = dis.read(pieceFileBytes, 0, pieceFileSize);
		} catch (FileNotFoundException e) {
			System.out.println("FNOF Execp");
		} catch (IOException e) {
			System.out.println("IOE Execp");
		} 
		if (bytesRead < pieceFileSize) {
			System.out.println("Could not read piece" + pieceNumber
					+ " Completely");
			return null;
		}
		return pieceFileBytes;
	}// end of getPiece

	/*
	 * Method to send piece message
	 */
	public static synchronized void sendPiece(ObjectOutputStream outputStream,
			byte[] raw, int pid) throws IOException {
		String prep = "";
//		byte[] buff = prep.getBytes();
		byte[] header = new byte[9];
		int pktLength = raw.length + 4;
		System.out.println("raw length="+raw.length);
		String pktLengthS = Integer.toHexString(pktLength);
		if (pktLengthS.length() <= 1) {
			prep += "000";
		} else if (pktLengthS.length() <= 2) {
			prep += "00";
		} else if (pktLengthS.length() <= 3) {
			prep += "0";
		}
		prep += "" + pktLengthS;
		prep += "7";
		if (pid < 10) {
			prep += "000";
		} else if (pid < 100) {
			prep += "00";
		} else if (pid < 1000) {
			prep += "0";
		}
		prep += "" + Integer.toString(pid);
		header = prep.getBytes();
		byte[] outPkt = concatBytes(header, raw);
		System.out.println("Sending pc");
		outputStream.writeObject(new MsgContainer(outPkt));
		outputStream.flush();
		System.out.println("pc sent");
	}

	/*
	 * Concatenate two byte objects
	 */
	static byte[] concatBytes(byte a[], byte b[]) {
		byte[] result = new byte[((int) a.length) + ((int) b.length)];
		int i = 0;
		while (i < ((int) a.length)) {
			result[i] = a[i];
			i++;
		}
		int j = 0;
		while (j < ((int) b.length)) {
			result[i] = b[j];
			i++;
			j++;
		}
		return result;
	}// end of concatenate two bytes

	/*
	 * Save a piece
	 */
	public static synchronized void putPiece(byte[] BytesIn, int pieceNumber) {
		try {
			pieceNumber += 10000;
			OutputStream out = new FileOutputStream("peer_" + myPeerId + "/"
					+ pieceNumber);
			// out.write(BytesIn, 0, BytesIn.length);
			out.write(BytesIn);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}// end of put piece

	/*
	 * Count all the pieces i have
	 */
	public static synchronized void set_currentPieceCount() {
		myPieceCount++;
	}

	public static synchronized int get_currentPieceCount() {
		return myPieceCount;
	}

	/*
	 * Send Have message
	 */
	public static synchronized void sendHave(ObjectOutputStream outputStream,
			int pieceid) throws IOException {
		String prep = "0004";
		prep += "4";
		if (pieceid < 10) {
			prep += "000";
		} else if (pieceid < 100) {
			prep += "00";
		} else if (pieceid < 1000) {
			prep += "0";
		}
		prep += "" + pieceid;
		byte[] buff = prep.getBytes();
		outputStream.writeObject(new MsgContainer(buff));
	}

	/*
	 * To Check is file sharing is complete
	 */
	public boolean completionChecker() {
		/* this guy returns if the program is complete */
		// bitMap ..check all bit sets are set
		// for all the peers in the list
		int count_peers = 0;
		if (mybitmap.nextClearBit(0) < totalPieces) {
			return false;
		}
		Iterator itr = this.connMap.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry) itr.next();
			String p = (String) entry.getKey();
			BitSet tempBS = this.bitMap.get(p);
			if (tempBS == null) {
				return false;
			}
			if (tempBS.nextClearBit(0) >= totalPieces) {
				count_peers++;
			} else {

			}
		}
		if (count_peers == connMap.size()) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			Iterator vecItr = this.ConnectionThread.iterator();
			try {
				for (Connection c : peerProcess.connMap.values()) {
					(c.getOutputStream()).close();
					(c.getInputStream()).close();
				}
			} catch (Exception e) {
			}
			while (vecItr.hasNext()) {
				Thread temp = (Thread) vecItr.next();
				temp.stop();
			}
			return true;
		}
		return false;
	}// end of completion checker

	/*
	 * Get the randomly selected random neighbor
	 */
	public void getOptNbr() {
		System.out.println("in get Nbr");
		if (peersInterestedVec.size() == 0) {
			return;
		}
		if (currOptNbr != null) {
			try {
				sendChoke(currOptNbr.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			unchokedVec.remove(currOptNbr.getPeerID());
		}

		Connection newOptNbr = peersInterestedVec.get(new Random()
				.nextInt(peersInterestedVec.size()));

		currOptNbr = newOptNbr;
		logObj.logUnchoked(newOptNbr.getPeerID());
		// send unchoke message
		try {
			sendChoke(newOptNbr.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		unchokedVec.add(newOptNbr.getPeerID());
		System.out.println("out of Nbr");
	}

	/*
	 * send Choke messages to preferred neighbous
	 */
	public void send_chokes() {
		System.out.println("Ccame to sendChokes..!");
		for (int i = 0; i < prefNbr.size(); i++) {
			String temp = prefNbr.elementAt(i);
			System.out.println("prefNbr : " + temp);
			Connection c = this.connMap.get(temp);
			// Choke message
			try {
				sendChoke(c.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			prefNbr.remove(temp);
		}
		prefNbr.clear();
	}

	/*
	 * send un Choke messages to preferred neighbors
	 */
	@SuppressWarnings("static-access")
	public void send_unchokes() {
		System.out.println("Unchoking " + prefNbr.size());
		for (int i = 0; i < prefNbr.size(); i++) {
			String temp = prefNbr.elementAt(i);
			System.out.println("Unchoking2 " + temp);
			Connection c = this.connMap.get(temp);
			System.out.println("Sending unchoke " + temp);
			try {
				sendUnChoke(c.getOutputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Sent unchoke " + temp + " i:" + i);
		}
		System.out.println("out of unchokes");
	}

	/*
	 * Create and send choke message
	 */
	public static synchronized void sendChoke(ObjectOutputStream outputStream)
			throws IOException {
		System.out.println("actually sending a choke message...");
		String prep = "0000";
		prep += "0";
		byte[] buff = prep.getBytes();
		System.out.println("choke raw byte size =" + buff.length);
		outputStream.writeObject(new MsgContainer(buff));
		outputStream.flush();
	}

	/*
	 * Create and send unchoke message
	 */
	public static synchronized void sendUnChoke(ObjectOutputStream outputStream)
			throws IOException {
		System.out.println("Unchoke<<<<<");
		String prep = "0000";
		prep += "1";
		byte[] buff = prep.getBytes();
		System.out.println("1111Unchoke<<<<<");
		outputStream.writeObject(new MsgContainer(buff));
		outputStream.flush();
		System.out.println("2222Unchoke<<<<<");
	}

	/*
	 * Calculate Download rate of neighbors and choose preferred neighbors
	 */
	public static void downloadRateCalc() {
		System.out.println("download rate calc");
		System.out.println("DRC : " + numberOfPreferredNeighbors + " : size : "
				+ downloadRate.size());
		sortByComparator(peerProcess.downloadRate);
		int temp = numberOfPreferredNeighbors;
		for (String key : downloadRate.keySet()) {
			System.out.println("DRC 2 : " + key);
			if (temp == 0)
				break;
			temp--;
			peerProcess.prefNbr.add(key);
		}
		peerProcess.logObj.logPrefNeighbor(" " + prefNbr);
	}

	/*
	 * Method to sort Maps based on given attribute
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map sortByComparator(Map unsortMap) {

		@SuppressWarnings("unchecked")
		List list = new LinkedList(unsortMap.entrySet());

		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		// put sorted list into map again
		// LinkedHashMap make sure order in which keys were inserted
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

}
