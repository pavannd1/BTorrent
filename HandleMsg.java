import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.BitSet;
import java.util.Vector;

public class HandleMsg implements Runnable {

	int msgType;
	byte[] payLoad;
	String peerID;

	HandleMsg(int mT, byte[] pL, String pid) {
		payLoad = pL;
		msgType = mT;
		peerID = pid;
	}

	@Override
	public void run() {
		switch (msgType) {
		case 0:
			rx_choke();
			break;
		case 1:
			rx_unchoke();
			break;
		case 2:
			rx_interested();
			break;
		case 3:
			rx_notInterested();
			break;
		case 4:
			rx_have();
			break;
		case 5:
			rx_bitfield();
			break;
		case 6:
			rx_request();
			break;
		case 7:
			rx_piece();
			break;
		default:
			return;
		}
	}

	private void rx_choke() {
		System.out.println("Received choke msg from " + peerID);
		if (peerProcess.validRequestList == null) {
			return;
		}
		peerProcess.validRequestList.remove(peerID);
		Vector<Integer> requestedList = peerProcess.requestedMap.get(peerID);
		if (requestedList == null) {
			return;
		}
		requestedList.clear();
		peerProcess.logObj.logChoked(peerID); 
	}// end of choke

	private void rx_unchoke() {
		System.out.println("Received unchoke msg from " + peerID);
		if (peerProcess.theEnd) {
			return;
		}
		if (!peerProcess.validRequestList.contains(peerID)) {
			peerProcess.validRequestList.add(peerID);
		}

		BitSet connectionBitSet = peerProcess.bitMap.get(peerID);
		while (peerProcess.validRequestList.contains(peerID)) {
			int pieceId = peerProcess.compareBitmap(connectionBitSet);
			if (pieceId == -1) {
				continue;
			}
			if (!peerProcess.isRequested(pieceId)) {
				if (peerProcess.requestedMap.get(peerID) == null) {
					Vector<Integer> pieceRequested = new Vector<Integer>();
					pieceRequested.add(pieceId);
					peerProcess.requestedMap.put(peerID, pieceRequested);
				} else {
					Vector<Integer> pieceRequested = peerProcess.requestedMap
							.get(peerID);
					pieceRequested.add(pieceId);
				}
				// Send a request message
				Connection intrConn = peerProcess.connMap.get(peerID);
				try {
					peerProcess
							.sendRequest(intrConn.getOutputStream(), pieceId);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		peerProcess.logObj.logUnchokedBy(peerID);
	}// end of unchoke

	private void rx_interested() {
		peerProcess.logObj.logInterested(this.peerID);
		System.out.println("Received interested msg from " + peerID);
		// Get the connection
		Connection tempConn = peerProcess.connMap.get(this.peerID);
		tempConn.setInterested(true);
		// If not included in the interested list, then include it
		if (!peerProcess.peersInterestedVec.contains(tempConn)) {
			peerProcess.peersInterestedVec.add(tempConn);
		}
	}// end of rx interested

	private void rx_notInterested() {
		peerProcess.logObj.logNotInterested(this.peerID);
		System.out.println("Received not interested msg from " + peerID);
		Connection tempConn = peerProcess.connMap.get(this.peerID);
		tempConn.setInterested(false);
		// If included in the interested list, then remove it
		if (peerProcess.peersInterestedVec.contains(tempConn)) {
			peerProcess.peersInterestedVec.remove(tempConn);
		}
	}// end of not interested

	private void rx_have() {
		String pi = "";
		for (int i = 0; i < payLoad.length; i++) {
			pi += (char) payLoad[i];
		}
		int pieceIndex = Integer.parseInt(pi);
		peerProcess.logObj.logHave(peerID,pieceIndex);
		System.out.println("Received have("+ pieceIndex +") msg from " + peerID);
		// place a Lock on bit field so that other threads cannot access it
		synchronized (peerProcess.lock) {
			BitSet connectionBitSet = peerProcess.bitMap.get(peerID);
			if (connectionBitSet == null) {
				return;
			}

			connectionBitSet.set(pieceIndex, true);

			if (peerProcess.mybitmap.get(pieceIndex) == false) {
				Connection intrConn = peerProcess.connMap.get(peerID);
				// Send Interested message
				try {
					peerProcess.sendInterested(intrConn.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				Connection notIntrConn = peerProcess.connMap.get(peerID);
				// Send not Interested message
				try {
					peerProcess
							.sendNotInterested(notIntrConn.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

	}// end of have message

	private void rx_bitfield() {
		System.out.println("Received bitfield msg from " + peerID);
		BitSet payloadBM = peerProcess.getBitSet(payLoad);

		synchronized (peerProcess.lock) {
			BitSet connectionBitSet = peerProcess.bitMap.get(peerID);
			if (connectionBitSet == null) {
				peerProcess.bitMap.put(peerID, payloadBM);
				System.out.println("Putting BitfieldIn");
			} else {
				connectionBitSet = payloadBM;
			}

			int missing = peerProcess.compareBitmap(payloadBM);

			if (missing != -1) {
				Connection intrConn = peerProcess.connMap.get(peerID);
				try {
					peerProcess.sendInterested(intrConn.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				// Don't need anything
			}
		}
	}

	private void rx_request() {

		String pi = "";
		for (int i = 0; i < payLoad.length; i++) {
			pi += (char) payLoad[i];
		}
		int pieceIndex = Integer.parseInt(pi);
		System.out.println("Received request("+ pieceIndex +") msg from " + peerID);
		BitSet connectionBitSet = peerProcess.bitMap.get(peerID);
		if (connectionBitSet != null) {
			if (connectionBitSet.get(pieceIndex) == true) {
				return;
			}
		}
		byte[] pieceByte = null;
		synchronized (peerProcess.lock) {
			// get piece given piece Index
			pieceByte = peerProcess.getPiece(pieceIndex);
		}
		System.out.println(" pc length = "+pieceByte.length);
		Connection pcConn = peerProcess.connMap.get(peerID);
		// Send piece
		try {
			peerProcess.sendPiece(pcConn.getOutputStream(), pieceByte,
					pieceIndex);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void rx_piece() {
		String pi = "";
		for (int i = 0; i < 4; i++) {
			pi += (char) payLoad[i];
		}
		int pieceIndex = Integer.parseInt(pi);
		byte[] pieceBytes = new byte[payLoad.length - 4];
		int j=0;
		for (int i = 4; i < payLoad.length; i++) {
			pieceBytes[j++] = payLoad[i];
		}
		System.out.println("Received piece("+ pieceIndex +")  from " + peerID);
		int totalDown = new File("peer_" + peerProcess.myPeerId).list().length;
		if (peerProcess.iComp) {
			return;
		}
		if (peerProcess.mybitmap.get(pieceIndex) == true) {
			peerProcess.logObj.stuff("dup request return from  handle_piece & piece index is "+ pieceIndex);
			return;
		}
		synchronized (peerProcess.lock) {
			peerProcess.logObj.stuff("current piece count: " +peerProcess.get_currentPieceCount()+ "Total piece count "+peerProcess.totalPieces);
			peerProcess.putPiece(pieceBytes, pieceIndex);
			peerProcess.mybitmap.set(pieceIndex, true);
			peerProcess.bitMap.put(peerProcess.myPeerId, peerProcess.mybitmap);

			totalDown = new File("peer_" + peerProcess.myPeerId).list().length;
			if (peerProcess.iComp != true) {
				if (totalDown == peerProcess.totalPieces) {
					peerProcess.iComp = true;
					try {
						peerProcess.fileJoiner();
					} catch (FileNotFoundException ex) {
					}
					peerProcess.logObj.logFinished();
				}
			}
			try {
				if (peerProcess.downloadRate.get(peerID) == null)
					peerProcess.downloadRate.put(peerID, 0);
				else {
					int temp = peerProcess.downloadRate.get(peerID);
					temp++;
					peerProcess.downloadRate.put(peerID, temp);
				}
			} catch (NullPointerException ex) {
				System.out.println("Download rate thingy");
			}
			peerProcess.set_currentPieceCount();
		}
		for (Connection c : peerProcess.connMap.values()) {
			// send have message to everybody
			try {
				peerProcess.sendHave(c.getOutputStream(), pieceIndex);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		peerProcess.logObj.logDownloaded(peerID, pieceIndex, totalDown);
	}
}
