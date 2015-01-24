import java.io.Serializable;


public class MsgContainer implements Serializable{
	public byte[] msgB;
	
	MsgContainer(byte[] b)
	{
		msgB=b;	
		System.out.println("size of byte packet is "+b.length);
	}
	
	public byte[] getBytes()
	{
		return msgB;
	}

}
