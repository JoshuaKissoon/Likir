package unito.likir.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class SecurityAgent implements Serializable
{
	private static final long serialVersionUID = 3372196637413971724L;

	//the RSA key pair of this instance
	private KeyPair keyPair;
	
	//a secure random function
	private transient SecureRandom random;
	
	//RSA
	private final int KEYSIZE = 1024;
	private transient KeyPairGenerator keyPairGen;
	private transient Cipher RSAcipher;
	private transient Signature RSAsign;
	
	//DES
	private transient Cipher DEScipher;
	private transient KeyGenerator keyGen;
	
	//Hashing
	private transient MessageDigest sha1md;
	
	public SecurityAgent()
	{
		try
		{
			Security.addProvider(new BouncyCastleProvider());
			keyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
			random = SecureRandom.getInstance("SHA1PRNG", "SUN");
			DEScipher = Cipher.getInstance("DES");
			RSAcipher = Cipher.getInstance("RSA/NONE/PKCS1Padding", "BC");
			sha1md = MessageDigest.getInstance("SHA-1");
			RSAsign = Signature.getInstance("SHA1withRSA", "BC");
			keyGen = KeyGenerator.getInstance("DES");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public SecurityAgent(KeyPair keyPair)
	{
		this();
		this.keyPair = keyPair;
	}
	
	public KeyPair getKeyPair()
	{
		return keyPair;
	}
	
	public PublicKey getPublicKey()
	{
		return keyPair.getPublic();
	}
	
	public void setKeyPair(KeyPair keyPair)
	{
		this.keyPair = keyPair;
	}
	
	public synchronized void initKeyPair()
	{
		try
		{
			keyPair = genKeyPair();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public synchronized KeyPair genKeyPair()
	{
		try
		{
			keyPairGen.initialize(KEYSIZE, random);
			return keyPairGen.generateKeyPair();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public synchronized SecretKey genSecretKey()
	{
		return keyGen.generateKey();
	}
	
	public byte[] hash(byte[] object)
	{
		byte[] hash;
		sha1md.update(object, 0, object.length);
		hash = sha1md.digest();
		return hash;
	}
	
	public boolean verifyHash(Serializable obj, byte[] hash)
	{
		try
		{
			byte[] h = hash(toBytes(obj));
			return (Arrays.equals(h,hash));
		}
		catch(IOException ioe)
		{
			return false;
		}
	}
	
	public byte[] encrypt(Serializable obj, SecretKey key) throws IOException
	{
		try
		{
			byte[] bytes = toBytes(obj);
			DEScipher.init(Cipher.ENCRYPT_MODE, key);
			return DEScipher.doFinal( bytes );
		}
		catch (InvalidKeyException e)
		{
			e.printStackTrace();
			throw new IOException("Invalid key");
		}
		catch (IllegalBlockSizeException e)
		{
			e.printStackTrace();
			throw new IOException("Illegal block size");
		}
		catch (BadPaddingException e)
		{
			e.printStackTrace();
			throw new IOException("Bad padding");
		}
	}
	
	public byte[] decrypt(byte[] encData, SecretKey key) throws IOException
	{
		try
		{
			DEScipher.init(Cipher.DECRYPT_MODE, key);
			return DEScipher.doFinal( encData );
		}
		catch (InvalidKeyException e)
		{
			e.printStackTrace();
			throw new IOException("Invalid key");
		}
		catch (IllegalBlockSizeException e)
		{
			e.printStackTrace();
			throw new IOException("Illegal block size");
		}
		catch (BadPaddingException e)
		{
			e.printStackTrace();
			throw new IOException("Bad padding");
		}
	}
	
	public byte[] encrypt(Serializable obj, PublicKey publicKey) throws IOException
	{
		try
		{
			byte[] bytes = toBytes(obj);
			RSAcipher.init(Cipher.ENCRYPT_MODE, publicKey);
			
			ByteArrayInputStream bufin = new ByteArrayInputStream(bytes);
			byte[] buffer = new byte[16];
			int len;
			while (bufin.available() != 0)
			{
				len = bufin.read(buffer);
				RSAcipher.update( buffer, 0, len );
			}
			return RSAcipher.doFinal();
		}
		catch (InvalidKeyException e)
		{
			e.printStackTrace();
			throw new IOException("Invalid key");
		}
		catch (IllegalBlockSizeException e)
		{
			e.printStackTrace();
			throw new IOException("Illegal block size");
		}
		catch (BadPaddingException e)
		{
			e.printStackTrace();
			throw new IOException("Bad padding");
		}
	}
	
	public byte[] decrypt(byte[] encData) throws IOException
	{
		try
		{
			RSAcipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
			return RSAcipher.doFinal( encData );
		}
		catch (InvalidKeyException e)
		{
			e.printStackTrace();
			throw new IOException("Invalid key");
		}
		catch (IllegalBlockSizeException e)
		{
			e.printStackTrace();
			throw new IOException("Illegal block size");
		}
		catch (BadPaddingException e)
		{
			e.printStackTrace();
			throw new IOException("Bad padding");
		}
	}
	
	public byte[] sign(Serializable obj) throws SignatureException
	{
		try
		{
			//Signature algorithm initialization
			RSAsign.initSign(keyPair.getPrivate());

			//Transform the object to be signed to a byte[]
			byte[] bytes = toBytes(obj);
			
			//Signature
			ByteArrayInputStream bufin = new ByteArrayInputStream(bytes);
			byte[] buffer = new byte[1024];
			int len;
			while (bufin.available() != 0)
			{
			    len = bufin.read(buffer);
			    RSAsign.update(buffer, 0, len);
			}
			bufin.close();
			byte[] signature = RSAsign.sign();
			return signature;
		}
		catch (InvalidKeyException ike)
		{
			//ike.printStackTrace();
			throw new SignatureException("Invalid key");
		}
		catch (IOException ioe)
		{
			//ioe.printStackTrace();
			throw new SignatureException("IO error");
		}
		catch (SignatureException se)
		{
			//se.printStackTrace();
			throw new SignatureException("Signature error");
		}
	}
	
	public boolean verifySignature(Serializable obj, byte[] signature, PublicKey pubKey) throws SignatureException
	{
		try
		{
			//Signature algorithm initialization
			RSAsign.initVerify(pubKey);
		
			//Transform the object to be signed to a byte[]
			byte[] bytes = toBytes(obj);
		
			//Verify
			ByteArrayInputStream bufin = new ByteArrayInputStream(bytes);
			
			byte[] buffer = new byte[1024];
			int len;
			while (bufin.available() != 0)
			{
			    len = bufin.read(buffer);
			    RSAsign.update(buffer, 0, len);
			}
	
			bufin.close();
			
			return RSAsign.verify(signature);
		}
		catch (InvalidKeyException ike)
		{
			//ike.printStackTrace();
			throw new SignatureException("Invalid key");
		}
		catch (IOException ioe)
		{
			//ioe.printStackTrace();
			throw new SignatureException("IO error");
		}
		catch (SignatureException se)
		{
			//se.printStackTrace();
			throw new SignatureException("Signature error");
		}

	}
	
	public byte[] toBytes(Serializable obj) throws IOException
	{
		//Transform the object to be signed to a byte[]
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);
		byte[] bytes = baos.toByteArray();
		return bytes;
	}
}
