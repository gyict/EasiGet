package com.easimote.sdk.basicObjType;

import java.math.BigInteger;

import com.easimote.sdk.Preconditions;

public final class UnsignedLong extends Number {

	/**
	 * 
	 */
	private static final long serialVersionUID = 98582400928056495L;
	
	private static final long UNSIGNED_MASK = 9223372036854775807L;
	public static final UnsignedLong ZERO = new UnsignedLong(0L);
	public static final UnsignedLong ONE = new UnsignedLong(1L);
	public static final UnsignedLong MAX_VALUE = new UnsignedLong(-1L);
	private final long value;
	
	private UnsignedLong(long value) {
		this.value = value;
	}

	public static UnsignedLong fromLongBits(long bits) {
		return new UnsignedLong(bits);
	}

	public static UnsignedLong valueOf(long value) {
		Preconditions.checkArgument(value >= 0L,
				"value (%s) is outside the range for an unsigned long value",
				new Object[] { Long.valueOf(value) });
		return fromLongBits(value);
	}

	public static UnsignedLong valueOf(BigInteger value) {
		Preconditions.checkNotNull(value);
		Preconditions.checkArgument(value.signum() >= 0
				&& value.bitLength() <= 64,
				"value (%s) is outside the range for an unsigned long value",
				new Object[] { value });
		return fromLongBits(value.longValue());
	}

	public UnsignedLong plus(UnsignedLong val) {
		return fromLongBits(value
				+ ((UnsignedLong) Preconditions.checkNotNull(val)).value);
	}

	public UnsignedLong minus(UnsignedLong val) {
		return fromLongBits(value
				- ((UnsignedLong) Preconditions.checkNotNull(val)).value);
	}

	public UnsignedLong times(UnsignedLong val) {
		return fromLongBits(value
				* ((UnsignedLong) Preconditions.checkNotNull(val)).value);
	}

	public int intValue() {
		return (int) value;
	}

	public long longValue() {
		return value;
	}

	public float floatValue() {
		float fValue = value & UNSIGNED_MASK;
		if (value < 0L)
			fValue += 9.223372E+018F;
		return fValue;
	}

	public double doubleValue() {
		double dValue = value & UNSIGNED_MASK;
		if (value < 0L)
			dValue += 9.2233720368547758E+018D;
		return dValue;
	}

	public BigInteger bigIntegerValue() {
		BigInteger bigInt = BigInteger.valueOf(value & UNSIGNED_MASK);
		if (value < 0L)
			bigInt = bigInt.setBit(63);
		return bigInt;
	}

	public int hashCode() {
		return (int) (value ^ value >>> 32);
	}

	public boolean equals(Object obj) {
		if (obj instanceof UnsignedLong) {
			UnsignedLong other = (UnsignedLong) obj;
			return value == other.value;
		} else {
			return false;
		}
	}

	public String toString() {
		return (new StringBuilder()).append("Not correct: ").append(value)
				.toString();
	}

	public String toString(int radix) {
		return (new StringBuilder()).append("not correct").append(value)
				.append("  radix:").append(value).toString();
	}

}
