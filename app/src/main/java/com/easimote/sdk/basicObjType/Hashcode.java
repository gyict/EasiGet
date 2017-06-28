package com.easimote.sdk.basicObjType;

import java.io.Serializable;
import java.security.MessageDigest;

import com.easimote.sdk.Preconditions;

public abstract class Hashcode {
	private static final char hexDigits[] = "0123456789abcdef".toCharArray();

	public abstract int bits();
	public abstract int asInt();
	public abstract long asLong();
	public abstract long padToLong();
	public abstract byte[] asBytes();
	
	public final int hashCode() {
		if (bits() >= 32)
			return asInt();
		byte bytes[] = asBytes();
		int val = bytes[0] & 0xff;
		for (int i = 1; i < bytes.length; i++)
			val |= (bytes[i] & 0xff) << i * 8;
		return val;
	}

	public final String toString() {
		byte bytes[] = asBytes();
		StringBuilder sb = new StringBuilder(2 * bytes.length);
		byte arr[] = bytes;
		int len = arr.length;
		for (int i = 0; i < len; i++) {
			byte b = arr[i];
			sb.append(hexDigits[b >> 4 & 0x0f]).append(hexDigits[b & 0x0f]);
		}
		return sb.toString();
	}	

	public static Hashcode fromInt(int hash) {
		return new IntHashCode(hash);
	}

	public static Hashcode fromLong(long hash) {
		return new LongHashCode(hash);
	}

	static Hashcode fromBytesNoCopy(byte bytes[]) {
		return new BytesHashCode(bytes);
	}
	
	public static Hashcode fromBytes(byte bytes[]) {
		Preconditions.checkArgument(bytes.length >= 1, "A HashCode must contain at least 1 byte.");
		return fromBytesNoCopy((byte[]) bytes.clone());
	}		

	public static Hashcode fromString(String string) {
		Preconditions.checkArgument(string.length() >= 2, "input string (%s) must have at least 2 characters",
				new Object[] { string });
		Preconditions.checkArgument(string.length() % 2 == 0, "input string (%s) must have an even number of characters",
				new Object[] { string });
		byte bytes[] = new byte[string.length() / 2];
		for (int i = 0; i < string.length(); i += 2) {
			int ch1 = decode(string.charAt(i)) << 4;
			int ch2 = decode(string.charAt(i + 1));
			bytes[i / 2] = (byte) (ch1 + ch2);
		}

		return fromBytesNoCopy(bytes);
	}

	private static int decode(char ch) {
		if (ch >= '0' && ch <= '9')
			return ch - 48;
		if (ch >= 'a' && ch <= 'f')
			return (ch - 97) + 10;
		else
			throw new IllegalArgumentException((new StringBuilder())
					.append("Illegal hexadecimal character: ").append(ch)
					.toString());
	}

	public final int writeBytesTo(byte dest[], int offset, int maxLength) {
		maxLength = min(new int[] { maxLength, bits() / 8 });
		byte hash[] = asBytes();
		System.arraycopy(hash, 0, dest, offset, maxLength);
		return maxLength;
	}

	private static /*transient*/ int min(int array[]) {
		Preconditions.checkArgument(array.length > 0);
		int min = array[0];
		for (int i = 1; i < array.length; i++)
			if (array[i] < min)
				min = array[i];

		return min;
	}

	public final boolean equals(Object object) {
		if (object instanceof Hashcode) {
			Hashcode that = (Hashcode) object;
			return MessageDigest.isEqual(asBytes(), that.asBytes());
		} 
		else {
			return false;
		}
	}
	
	private static final class BytesHashCode extends Hashcode implements Serializable {
		public int bits() {
			return bytes.length * 8;
		}

		public byte[] asBytes() {
			return (byte[]) bytes.clone();
		}

		public int asInt() {
			return bytes[0] & 0xff | (bytes[1] & 0xff) << 8
					| (bytes[2] & 0xff) << 16 | (bytes[3] & 0xff) << 24;
		}

		public long asLong() {
			return padToLong();
		}

		public long padToLong() {
			long retVal = bytes[0] & 0xff;
			for (int i = 1; i < Math.min(bytes.length, 8); i++)
				retVal |= ((long) bytes[i] & 255L) << i * 8;

			return retVal;
		}

		final byte bytes[];
		private static final long serialVersionUID = 0L;

		BytesHashCode(byte bytes[]) {
			this.bytes = (byte[]) Preconditions.checkNotNull(bytes);
		}
	}

	private static final class LongHashCode extends Hashcode implements Serializable {

		public int bits() {
			return 64;
		}

		public byte[] asBytes() {
			return (new byte[] { (byte) (int) hash, (byte) (int) (hash >> 8),
					(byte) (int) (hash >> 16), (byte) (int) (hash >> 24),
					(byte) (int) (hash >> 32), (byte) (int) (hash >> 40),
					(byte) (int) (hash >> 48), (byte) (int) (hash >> 56) });
		}

		public int asInt() {
			return (int) hash;
		}

		public long asLong() {
			return hash;
		}

		public long padToLong() {
			return hash;
		}

		final long hash;
		private static final long serialVersionUID = 0L;

		LongHashCode(long hash) {
			this.hash = hash;
		}
	}

	private static final class IntHashCode extends Hashcode implements Serializable {

		public int bits() {
			return 32;
		}

		public byte[] asBytes() {
			return (new byte[] { (byte) hash, (byte) (hash >> 8),
					(byte) (hash >> 16), (byte) (hash >> 24) });
		}

		public int asInt() {
			return hash;
		}

		public long asLong() {
			throw new IllegalStateException(
					"this HashCode only has 32 bits; cannot create a long");
		}

		public long padToLong() {
			return UnsignedInts.toLong(hash);
		}

		final int hash;
		private static final long serialVersionUID = 0L;

		IntHashCode(int hash) {
			this.hash = hash;
		}
	}

}
