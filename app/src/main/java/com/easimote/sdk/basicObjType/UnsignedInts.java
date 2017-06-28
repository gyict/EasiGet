package com.easimote.sdk.basicObjType;

import com.easimote.sdk.Preconditions;

public final class UnsignedInts {

	static int flip(int value) {
		return value ^ -2147483648;
	}

	public static long toLong(int value) {
		return (long) value & 4294967295L;
	}

	public static /*transient*/ String join(String separator, int array[]) {
		Preconditions.checkNotNull(separator);
		if (array.length == 0)
			return "";
		StringBuilder builder = new StringBuilder(array.length * 5);
		builder.append(toString(array[0]));
		for (int i = 1; i < array.length; i++)
			builder.append(separator).append(toString(array[i]));

		return builder.toString();
	}

	public static int divide(int dividend, int divisor) {
		return (int) (toLong(dividend) / toLong(divisor));
	}

	public static int remainder(int dividend, int divisor) {
		return (int) (toLong(dividend) % toLong(divisor));
	}

	public static int parseUnsignedInt(String s) {
		return parseUnsignedInt(s, 10);
	}

	public static int parseUnsignedInt(String string, int radix) {
		Preconditions.checkNotNull(string);
		long result = Long.parseLong(string, radix);
		if ((result & 4294967295L) != result)
			throw new NumberFormatException((new StringBuilder())
					.append("Input ").append(string).append(" in base ")
					.append(radix)
					.append(" is not in the range of an unsigned integer")
					.toString());
		else
			return (int) result;
	}

	public static String toString(int x) {
		return toString(x, 10);
	}

	public static String toString(int x, int radix) {
		long asLong = (long) x & 4294967295L;
		return Long.toString(asLong, radix);
	}

	static final long INT_MASK = 4294967295L;
}