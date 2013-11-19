package unito.likir.messages.dht;

import unito.likir.NodeId;
import unito.likir.security.MTRandom;
import unito.likir.util.ArrayUtils;

public class Nonce implements DHTMessage
{
	private static final long serialVersionUID = 5885305137426672836L;
	public static final int REQ_TYPE = 0;
	public static final int RES_TYPE = 1;
	
	private static MTRandom rand = new MTRandom();
	private OpCode opcode;
	private long sid;
	private NodeId sender;
	private byte[] nonce;

	public Nonce(NodeId sender, byte[] nonce, long sid, boolean request)
	{
		if (nonce == null || nonce.length != 16) throw new IllegalArgumentException("Nonce must be 128 bits long");
		this.sender = sender;
		this.nonce = nonce;
		this.sid = sid;
		if (request)
			this.opcode = DHTMessage.OpCode.NONCE_REQ;
		else
			this.opcode = DHTMessage.OpCode.NONCE_RES;
	}
	
	public Nonce(NodeId sender, byte[] nonce, boolean request)
	{
		if (nonce == null || nonce.length != 16) throw new IllegalArgumentException("Nonce must be 128 bits long");
		this.sender = sender;
		this.nonce = nonce;
		this.sid = rand.nextLong();
		if (request)
			this.opcode = DHTMessage.OpCode.NONCE_REQ;
		else
			this.opcode = DHTMessage.OpCode.NONCE_RES;
	}
	
	/**
	 * Create a new nonce, with a random nonce
	 * @param sender
	 * @param sid
	 */
	public Nonce(NodeId sender, long sid, boolean request)
	{
		this.sender = sender;
		this.nonce = new byte[16];
		rand.nextBytes(nonce);
		this.sid = sid;
		if (request)
			this.opcode = DHTMessage.OpCode.NONCE_REQ;
		else
			this.opcode = DHTMessage.OpCode.NONCE_RES;
	}
	
	/**
	 * Create a new nonce, with random nonce and session ID
	 * @param sender
	 */
	public Nonce(NodeId sender, boolean request)
	{		
		this.sender = sender;
		this.nonce = new byte[16];
		rand.nextBytes(nonce);
		this.sid = rand.nextLong();
		if (request)
			this.opcode = DHTMessage.OpCode.NONCE_REQ;
		else
			this.opcode = DHTMessage.OpCode.NONCE_RES;
	}

	public NodeId getSender()
	{
		return sender;
	}

	public void setSender(NodeId sender)
	{
		this.sender = sender;
	}

	public byte[] getNonce()
	{
		return nonce;
	}

	public void setNonce(byte[] nonce)
	{
		if (nonce == null || nonce.length != 16) throw new IllegalArgumentException("Nonce must be 128 bits long");
		this.nonce = nonce;
	}
	
	public long getSid()
	{
		return sid;
	}

	public void setSid(long sid)
	{
		this.sid = sid;
	}
	
	public OpCode getMsgOpCode()
	{
		return opcode;
	}
	
	public String toString()
	{
		return "Nonce - " + sender + ", " + sid + " nonce = " + ArrayUtils.toHexString(nonce);
	}
}