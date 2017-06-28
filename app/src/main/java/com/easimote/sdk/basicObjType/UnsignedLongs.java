package com.easimote.sdk.basicObjType;

public final class UnsignedLongs {

	public static final long MAX_VALUE = -1L;
	
	private static long flip(long a) {
		return a ^ -9223372036854775808L;
	}

	public static int compare(long a, long b) {
		return compareInternal(flip(a), flip(b));
	}

	private static int compareInternal(long a, long b) {
		return a >= b ? ((int) (a <= b ? 0 : 1)) : -1;
	}

	public static long remainder(long dividend, long divisor) {
		if (divisor < 0L)
			if (compare(dividend, divisor) < 0)
				return dividend;
			else
				return dividend - divisor;
		if (dividend >= 0L) {
			return dividend % divisor;
		} else {
			long quotient = (dividend >>> 1) / divisor << 1;
			long rem = dividend - quotient * divisor;
			return rem - (compare(rem, divisor) < 0 ? 0L : divisor);
		}
	}

}
