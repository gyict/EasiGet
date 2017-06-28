package com.easimote.sdk.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES encryption and decryption algorithms
 * 
 * @author PK
 *
 */
public class AESMath {

	 /**
     * Encryption
     * notice : we use AEC/ECB/NoPadding Algorithm, so we can get a 16-16 Encryption-decryption
     * 
     * @param content- the content to be  encrypted
     * @param password- the encryption password
     * @return
     */
	public static byte[] encrypt(byte[] content, byte[] key) {
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES/ECB/NoPadding");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SecretKeySpec securekey = new SecretKeySpec(key, "AES");
		try {
			cipher.init(Cipher.ENCRYPT_MODE, securekey);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			return cipher.doFinal(content);
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;
	}

//	public static byte[] encrypt(byte[] content, String key) {
//		Cipher cipher = null;
//		try {
//			cipher = Cipher.getInstance("AES/ECB/NoPadding");
//		} catch (NoSuchAlgorithmException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		SecretKeySpec securekey = new SecretKeySpec(key.getBytes(), "AES");
//		try {
//			cipher.init(Cipher.ENCRYPT_MODE, securekey);
//		} catch (InvalidKeyException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		try {
//			return cipher.doFinal(content);
//		} catch (IllegalBlockSizeException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (BadPaddingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return content;
//	}
    
    /**
     * Decryption 
     * 
     * @param content- the content to be decoded
     * @param password- you know what it is
     * @return 
     */
	public static byte[] decrypt(byte[] content, String key) {
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES/ECB/NoPadding");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SecretKeySpec securekey = new SecretKeySpec(key.getBytes(), "AES");
		try {
			cipher.init(Cipher.DECRYPT_MODE, securekey);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			return cipher.doFinal(content);
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;
	}

//	public static byte[] decrypt(byte[] content, byte[] key) {
//		Cipher cipher = null;
//		try {
//			cipher = Cipher.getInstance("AES/ECB/NoPadding");
//		} catch (NoSuchAlgorithmException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		SecretKeySpec securekey = new SecretKeySpec(key, "AES");
//		try {
//			cipher.init(Cipher.DECRYPT_MODE, securekey);
//		} catch (InvalidKeyException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		try {
//			return cipher.doFinal(content);
//		} catch (IllegalBlockSizeException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (BadPaddingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return content;
//	}
}
