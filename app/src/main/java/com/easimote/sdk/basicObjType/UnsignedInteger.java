package com.easimote.sdk.basicObjType;

import java.math.BigInteger;

import com.easimote.sdk.Preconditions;

public final class UnsignedInteger extends Number {


	private static final long serialVersionUID = -5019267302929611081L;
	
	public static final UnsignedInteger ZERO = fromIntBits(0);
	public static final UnsignedInteger ONE = fromIntBits(1);
	public static final UnsignedInteger MAX_VALUE = fromIntBits(-1);
	private final int value;
	
	private UnsignedInteger(int value) {
		this.value = value & -1;
	}

	public static UnsignedInteger fromIntBits(int bits) {
		return new UnsignedInteger(bits);
	}

	public static UnsignedInteger valueOf(long value) {
		Preconditions.checkArgument((value & 0xffffffff) == value,
						"value (%s) is outside the range for an unsigned integer value",
						new Object[] { Long.valueOf(value) });
		return fromIntBits((int) value);
	}

	public static UnsignedInteger valueOf(BigInteger value) {
		Preconditions.checkNotNull(value);
		Preconditions.checkArgument(value.signum() >= 0 && value.bitLength() <= 32,
						"value (%s) is outside the range for an unsigned integer value",
						new Object[] { value });
		return fromIntBits(value.intValue());
	}

	public static UnsignedInteger valueOf(String string) {
		return valueOf(string, 10);
	}

	public static UnsignedInteger valueOf(String string, int radix) {
		return fromIntBits(UnsignedInts.parseUnsignedInt(string, radix));
	}

	public UnsignedInteger plus(UnsignedInteger val) {
		return fromIntBits(value + ((UnsignedInteger) Preconditions.checkNotNull(val)).value);
	}

	public UnsignedInteger minus(UnsignedInteger val) {
		return fromIntBits(value - ((UnsignedInteger) Preconditions.checkNotNull(val)).value);
	}

	public UnsignedInteger times(UnsignedInteger val) {
		return fromIntBits(value * ((UnsignedInteger) Preconditions.checkNotNull(val)).value);
	}

	public UnsignedInteger dividedBy(UnsignedInteger val) {
		return fromIntBits(UnsignedInts.divide(value, ((UnsignedInteger) Preconditions.checkNotNull(val)).value));
	}

	public UnsignedInteger mod(UnsignedInteger val) {
		return fromIntBits(UnsignedInts.remainder(value, ((UnsignedInteger) Preconditions.checkNotNull(val)).value));
	}

	public int intValue() {
		return value;
	}

	public long longValue() {
		return UnsignedInts.toLong(value);
	}

	public float floatValue() {
		return (float) longValue();
	}

	public double doubleValue() {
		return (double) longValue();
	}

	public BigInteger bigIntegerValue() {
		return BigInteger.valueOf(longValue());
	}

	public int hashCode() {
		return value;
	}

	public boolean equals(Object obj) {
		if (obj instanceof UnsignedInteger) {
			UnsignedInteger other = (UnsignedInteger) obj;
			return value == other.value;
		} else {
			return false;
		}
	}

	public String toString() {
		return toString(10);
	}

	public String toString(int radix) {
		return UnsignedInts.toString(value, radix);
	}

}
