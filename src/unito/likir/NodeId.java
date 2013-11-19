package unito.likir;

import java.util.Arrays;
import java.math.BigInteger;
import java.io.Serializable;
import unito.likir.security.MTRandom;
import unito.likir.util.ArrayUtils;
import unito.likir.util.PatriciaTrie.KeyAnalyzer;

/**
 * Kademlia node 160 bit identifier
 * Adapted from:
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 * @author Luca Maria Aiello
 */

public final class NodeId implements Serializable, Comparable<NodeId>, Cloneable
{
	private static final long serialVersionUID = -4845343716582644164L;
	
	private byte[] id; //The node identifier
	public static final int LENGTH = 20;
	public static final int BITLENGTH = LENGTH * 8;
    public static final NodeId MINIMUM; //All 160 bits are 0
    public static final NodeId MAXIMUM; //All 160 bits are 1
    
    public static final MTRandom randGen = new MTRandom(System.currentTimeMillis()); 
    
    private final int hashCode; //the hashcode of this object
    static
    {
        byte[] min = new byte[LENGTH];
        //byte minByte = 0;
        Arrays.fill(min, (byte)0x00);
        byte[] max = new byte[LENGTH];
        Arrays.fill(max, (byte)0xFF);
        
        MINIMUM = new NodeId(min);
        MAXIMUM = new NodeId(max);
    }
    
    // Bytes with one bit set, from most significant bit to less significant bit 
    private static final int[] BITS =
    {
        0x80, //10000000
        0x40, //01000000
        0x20, //00100000
        0x10, //00010000
        0x8,  //00001000
        0x4,  //00000100
        0x2,  //00000010
        0x1   //00000001
    };
	
    /**
     * Constructs a NodeId object from a raw byte array.
     * The specified array length <g>must</g> be equals to NodeId.LENGTH
     * @param nodeId a byte array
     */
	public NodeId(byte[] nodeId)
	{
		if (nodeId == null)
			throw new NullPointerException("The NodeId must not be NULL");
		if (nodeId.length != LENGTH)
			throw new IllegalArgumentException("The NodeId must be"+ LENGTH +"byte ("+ BITLENGTH +" bit) long");
		this.id = nodeId;
		this.hashCode = Arrays.hashCode(nodeId);
	}
	
	public static NodeId createWithBytes(byte[] id)
	{
		byte[] dst = new byte[id.length];
        System.arraycopy(id, 0, dst, 0, id.length);
        return new NodeId(dst);
	}
	
	public static NodeId createWithHexString(String id)
	{
	    return new NodeId(ArrayUtils.parseHexString(id));
	}
	
	public static NodeId createWithPrefix(NodeId prefix, int depth)
	{
        byte[] random = new byte[LENGTH];
        randGen.nextBytes(random);        
        
        depth++;
        int length = depth/8;
        System.arraycopy(prefix.id, 0, random, 0, length);
        
        int bitsToCopy = depth % 8;
        if (bitsToCopy != 0) {
            // Mask has the low-order (8-bits) bits set
            int mask = (1 << (8-bitsToCopy)) - 1;
            int prefixByte = prefix.id[length];
            int randByte   = random[length];
            random[length] = (byte) ((prefixByte & ~mask) | (randByte & mask));
        }
        
        return new NodeId(random);
    }
	
	public static NodeId createRandom()
	{
		byte[] random = new byte[LENGTH];
        randGen.nextBytes(random);
        return new NodeId(random);
	}
	
	/**
	 * Returns an array of byte representing this NodeId
	 * @return the raw byte id
	 */
	public byte[] getId()
	{
		return id;
	}

	public void setId(byte[] id) throws IllegalArgumentException
	{
		if (id.length != LENGTH)
			throw new IllegalArgumentException("Illegal id size");
		this.id = id;
	}
	
	public int hashCode()
	{
		return hashCode;
	}
	
    /**
     * Returns whether or not the 'bitIndex'th bit is set to 1
     * @return the result of the check
     */
    public boolean isBitSet(int bitIndex)
    {
        int index = (bitIndex / BITS.length); //selects the byte containing the 'bitIndex'th bit
        int bit = (bitIndex - index * BITS.length); //selects the index of the bit in the byte
        return (id[index] & BITS[bit]) != 0; //AND between the byte and a mask with only the 'bit'th bit set
    }
    
    /**
     * Returns the raw bytes of the current NodeId. The
     * returned byte[] array is a copy and modifications
     * are not reflected to this NodeId
     */
    public byte[] getBytes()
    {
        return getBytes(0, new byte[id.length], 0, id.length);
    }
    
    /**
     * Returns the raw bytes of the current NodeId from the specified interval
     */
    public byte[] getBytes(int srcPos, byte[] dest, int destPos, int length)
    {
        System.arraycopy(id, srcPos, dest, destPos, length);
        return dest;
    }
    
    /**
     * Create a new NodeId setting or unsetting the 'bitIndex'th bit of this NodeId
     * @param bitIndex the index of the bit to be set/unset
     * @param set true to set, false to unset 
    */
    private NodeId set(int bitIndex, boolean set)
    {
        int index = (bitIndex / BITS.length);
        int bit = (bitIndex - index * BITS.length);
        boolean isSet = (id[index] & BITS[bit]) != 0;
        
        byte[] modifiedId = getBytes();
        if (isSet != set)
            modifiedId[index] ^= BITS[bit];
        
        return new NodeId(modifiedId);
    }
    
    /**
     * Sets the specified bit to 1 and returns a new NodeId instance
     * @param the index of the bit to be set
     * @return the new NodeId
     */
    public NodeId set(int bitIndex)
    {
        return set(bitIndex, true);
    }
    
    /**
     * Sets the specified bit to 0 and returns a new NodeId instance
     * @param the index of the bit to be unset
     * @return the new NodeId
     */
    public NodeId unset(int bitIndex)
    {
        return set(bitIndex, false);
    }
    
    /**
     * Flips the specified bit from 0 to 1 or vice versa and returns a new NodeId instance
     * @param the index of the bit to be flipped
     * @return the new NodeId
     */
    public NodeId flip(int bitIndex)
    {
        return set(bitIndex, !isBitSet(bitIndex));
    }
    
    /**
     * Inverts all bits of this NodeId and returns a new NodeId instance
     * @return the new NodeId
     */
    public NodeId invert()
    {
        byte[] result = new byte[id.length];
        for(int i = 0; i < result.length; i++)
            result[i] = (byte)~id[i];
        
        return new NodeId(result);
    }
    
    /**
     * Returns the XOR distance between this and given NodeId.
     * @param nodeId a NodeId
     * @return the XOR between this and nodeId
     */
    public NodeId xor(NodeId nodeId)
    {
        byte[] result = new byte[id.length];
        for(int i = 0; i < result.length; i++)
            result[i] = (byte)(id[i] ^ nodeId.id[i]);
        
        return new NodeId(result);
    }
	
	/**
	 * Returns true if the specified NodeId is equal to this NodeId
	 * @param nodeId the NodeId to be tested for equality
	 * @return the result of the comparison
	 */
	/*public boolean equals(NodeId nodeId)
	{
		return Arrays.equals(id,nodeId.getId());
	}*/
	
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }
        try
        {
        	NodeId castedObject = (NodeId) o;
        	return Arrays.equals(id, castedObject.id);
        }
        catch (Exception e)
        {
        	return false;
        }
    }
	
	/**
	 * Compares this NodeId with the specified NodeId for order.
	 * Returns a negative integer, zero, or a positive integer as this NodeId is less than,
	 * equal to, or greater than the specified NodeId.
	 * @param nodeId the NodeId to be compared with this NodeId
	 * @return the result of the comparison
	 */
	public int compareTo(NodeId nodeId)
	{
        int difference = 0;
        for(int i = 0; i < id.length; i++)
        {
            difference = (id[i] & 0xFF) - (nodeId.getId()[i] & 0xFF);
            if (difference < 0)
                return -1;
            else if (difference > 0)
                return 1;
        }
        return 0;
	}
	
	/**
	 * Returns the number of bits in this NodeId different from the corresponding bits in the
	 * NodeId specified as parameter.
	 * @param nodeId the NodeId to be compared with this NodeId
	 * @return the number of different bits
	 */
	public int differentBitNumber(NodeId nodeId)
	{
		int distance = 0;
		for (int i=0; i<BITLENGTH; i++)
		{
			if (isBitSet(i) != nodeId.isBitSet(i))
				distance++;
		}
		return distance;
	}
	
	/**
     * Returns the current NodeId as hex String
     */
    public String toHexString()
    {
        return ArrayUtils.toHexString(id);
    }
    
    /**
     * Returns the current NodeId as bin String
     */
    public String toBinString()
    {
        return ArrayUtils.toBinString(id);
    }
    
    /**
     * Returns the current NodeId as BigInteger
     */
    public BigInteger toBigInteger()
    {    
        return new BigInteger(1 /* unsigned! */, id);
    }
    
    /**
     * Makes a deep copy of this NodeId
     */
    public Object clone() throws CloneNotSupportedException
    {
    	NodeId cloned = (NodeId) super.clone();
    	cloned.setId(this.getId().clone());
    	return cloned;
    }
    
	/**
	 * Returns the String representation of this NodeId
	 * @return the String representation of this object
	 */
	public String toString()
	{
		return toBinString().substring(0,17);
	}
	
	//PATRICIA TRIE UTILITIES
	
    /**
     * Returns the first bit that differs in this KUID
     * and the given KUID or KeyAnalyzer.NULL_BIT_KEY
     * if all 160 bits are zero or KeyAnalyzer.EQUAL_BIT_KEY
     * if both KUIDs are equal
     * @param nodeId the NodeId
     * @return the index of the bit
     */
    public int bitIndex(NodeId nodeId)
    {
        boolean allNull = true;
        
        int bitIndex = 0;
        for(int i = 0; i < id.length; i++)
        {
            if (allNull && id[i] != 0)
                allNull = false;
            
            if (id[i] != nodeId.id[i])
            {
                for(int j = 0; j < BITS.length; j++)
                {
                    if ((id[i] & BITS[j]) != (nodeId.id[i] & BITS[j]))
                    {
                        break;
                    }
                    bitIndex++;
                }
                break;
            }
            bitIndex += BITS.length;
        }
        
        if (allNull)
            return KeyAnalyzer.NULL_BIT_KEY;
        
        if (bitIndex == BITLENGTH)
            return KeyAnalyzer.EQUAL_BIT_KEY;
        
        return bitIndex;
    }
	
    /**
     * The default KeyAnalyzer for NodeIds
     */
    public static final KeyAnalyzer<NodeId> KEY_ANALYZER = new NodeIdKeyAnalyzer();
    
    /**
     * A PATRICIA Trie KeyAnalyzer for NodeIds
     */
    private static class NodeIdKeyAnalyzer implements KeyAnalyzer<NodeId>
    {
        private static final long serialVersionUID = 6412279289438108492L;

        public int bitIndex(NodeId key, int keyStart, int keyLength, NodeId found, int foundStart, int foundLength)
        {
            if (found == null)
                found = NodeId.MINIMUM;
            
            return key.bitIndex(found);
        }

        public int bitsPerElement()
        {
            return 1;
        }

        public boolean isBitSet(NodeId key, int keyLength, int bitIndex)
        {
            return key.isBitSet(bitIndex);
        }

        public boolean isPrefix(NodeId prefix, int offset, int length, NodeId key)
        {
            int end = offset + length;
            for (int i = offset; i < end; i++)
            {
                if (prefix.isBitSet(i) != key.isBitSet(i))
                {
                    return false;
                }
            }
            
            return true;
        }

        public int length(NodeId key)
        {
            return NodeId.LENGTH;
        }

        public int compare(NodeId o1, NodeId o2)
        {
            return o1.compareTo(o2);
        }
    }

}
