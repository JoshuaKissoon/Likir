package unito.likir.messages.dht;

import java.io.Serializable;

public interface RPC extends Serializable
{
    public static enum OpCode
    {
        PING_REQUEST(0x01),
        PING_RESPONSE(0x02),
        
        STORE_REQUEST(0x03),
        STORE_RESPONSE(0x04),
        
        FIND_NODE_REQUEST(0x05),
        FIND_NODE_RESPONSE(0x06),
        
        FIND_VALUE_REQUEST(0x07),
        FIND_VALUE_RESPONSE(0x08);
        
        //PROBE_REQUEST(0x09),
        //PROBE_RESPONSE(0x0A);
        
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
        	if (opcode == PING_REQUEST.toByte() ||
        		opcode == STORE_REQUEST.toByte() ||
        		opcode == FIND_NODE_REQUEST.toByte() ||
        		opcode == FIND_VALUE_REQUEST.toByte()
        		)
        	return true;
        	else
        		return false;
        }
        
        public boolean isResponse()
        {
        	if (opcode == PING_RESPONSE.toByte() ||
            	opcode == STORE_RESPONSE.toByte() ||
            	opcode == FIND_NODE_RESPONSE.toByte() ||
           		opcode == FIND_VALUE_RESPONSE.toByte()
           		)
            return true;
           	else
           		return false;
        }
    }
    
    /** Returns the Message ID of the Message */
    public long getMessageId();
    
    /** Returns the opcode (type) of the Message */
    public OpCode getRPCOpCode();
    

}
