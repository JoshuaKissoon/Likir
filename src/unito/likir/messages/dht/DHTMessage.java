package unito.likir.messages.dht;

import java.io.Serializable;

/**
 * Every message exchanged between Nodes is a DHT message. Every message is related to a session,
 * identified by a session ID.
 * @author Cedric
 * @version 1.0b
 */
public interface DHTMessage extends Serializable
{
	public static enum OpCode
    {
        NONCE_REQ(0x01),
        NONCE_RES(0x02),
        RPC_REQ(0x03),
        RPC_RES(0x04);
        
        private final int opcode;
            
        private OpCode(int opcode)
        {
            this.opcode = opcode;
        }
    
        public int toByte()
        {
            return opcode;
        }
        
        public String toString()
        {
            return name() + " (" + toByte() + ")";
        }
        
        private static OpCode[] OPCODES;
        
        static //fills the OpCode[] array checking if there are duplicate opcodes 
        {
            OpCode[] values = values();
            OPCODES = new OpCode[values.length];
            for (OpCode o : values)
            {
                int index = o.opcode % OPCODES.length;
                if (OPCODES[index] != null)
                {
                    // Check the enums for duplicate opcodes!
                    throw new IllegalStateException("OpCode collision: index=" + index 
                            + ", OPCODES=" + OPCODES[index] + ", o=" + o);
                }
                OPCODES[index] = o;
            }
        }
        
        public boolean isRequest()
        {
        	if (opcode == NONCE_REQ.toByte() || opcode == RPC_REQ.toByte())
        		return true;
        	else
        		return false;
        }
        
        public boolean isResponse()
        {
        	if (opcode == NONCE_RES.toByte() || opcode == RPC_RES.toByte())
        		return true;
        	else
        		return false;
        }
        
        public boolean isNonce()
        {
        	if (opcode == NONCE_REQ.toByte() || opcode == NONCE_RES.toByte())
        		return true;
        	else
        		return false;
        }
        
        public boolean isNonceRequest()
        {
        	if (opcode == NONCE_REQ.toByte())
        		return true;
        	else
        		return false;
        }
        
        public boolean isNonceResponse()
        {
        	if (opcode == NONCE_RES.toByte())
        		return true;
        	else
        		return false;
        }
        
        public boolean isRPCMessage()
        {
        	if (opcode == RPC_REQ.toByte() || opcode == RPC_RES.toByte())
        		return true;
           	else
           		return false;
        }
        
        public boolean isRPCMessageRequest()
        {
        	if (opcode == RPC_REQ.toByte())
        		return true;
           	else
           		return false;
        }
        
        public boolean isRPCMessageResponse()
        {
        	if (opcode == RPC_RES.toByte())
        		return true;
           	else
           		return false;
        }
    }
	
	/**
	 * Return the message opcode
	 * @return the opcode
	 */
	public OpCode getMsgOpCode();
	
	/**
	 * Returns the Session ID of this message
	 * @return the session id
	 */
	public long getSid();
}
