package com.easimote.sdk;

//use for YuChuLi
public class Preconditions {
	public Preconditions() {
	}

	public static Object checkNotNull(Object reference, Object errorMessage) {
		if (reference == null)
			throw new NullPointerException(String.valueOf(errorMessage));
		else
			return reference;
	}

	public static Object checkNotNull(Object reference) {
		if (reference == null)
			throw new NullPointerException();
		else
			return reference;
	}

	public static void checkArgument(boolean expression) {
		if (!expression)
			throw new IllegalArgumentException();
		else
			return;
	}

	public static void checkArgument(boolean expression, Object errorMessage) {
		if (!expression)
			throw new IllegalArgumentException(String.valueOf(errorMessage));
		else
			return;
	}

	//not "serialize" mathod 
	public static /*transient*/ void checkArgument(boolean expression, String errorMessageTemplate, Object errorMessageArgs[]) {
		if (!expression)
			throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
		else
			return;
	}

	static /*transient*/ String format(String template, Object args[]) {
		template = String.valueOf(template);
		StringBuilder builder = new StringBuilder(template.length() + 16*args.length);
		int templateStart = 0;
		int i = 0;
		do {
			if (i >= args.length)
				break;
			int placeholderStart = template.indexOf("%s", templateStart);
			if (placeholderStart == -1)
				break;
			builder.append(template.substring(templateStart, placeholderStart));
			builder.append(args[i++]);
			templateStart = placeholderStart + 2;
		} while (true);
		builder.append(template.substring(templateStart));
		if (i < args.length) {
			builder.append(" [");
			builder.append(args[i++]);
			while (i < args.length) {
				builder.append(", ");
				builder.append(args[i++]);
			}
			builder.append(']');
		}
		return builder.toString();
	}

}
