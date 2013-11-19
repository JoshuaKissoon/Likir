package unito.likir.messages.ca;

import java.io.Serializable;

public interface CAMessage extends Serializable
{
	public String getUserId();
	public long getMessageId();
	
	public OpCode getOpCode();
	
	public static enum OpCode
    {
        INITIALIZATION_REQUEST(0x01),
        INITIALIZATION_RESPONSE(0x02),
        
        BOOTLIST_REQUEST(0x03),
        BOOTLIST_RESPONSE(0x04),
        
        CA_KEY_REQUEST(0x05),
        CA_KEY_RESPONSE(0x06),
        
        ERROR(0x00);
        
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
        	if (opcode == INITIALIZATION_REQUEST.toByte() ||
        		opcode == BOOTLIST_REQUEST.toByte() ||
        		opcode == CA_KEY_REQUEST.toByte()
        		)
        	return true;
        	else
        		return false;
        }
        
        public boolean isResponse()
        {
        	if (opcode == INITIALIZATION_RESPONSE.toByte() ||
            	opcode == BOOTLIST_RESPONSE.toByte() ||
            	opcode == CA_KEY_RESPONSE.toByte()
           		)
            return true;
           	else
           		return false;
        }
    }
}