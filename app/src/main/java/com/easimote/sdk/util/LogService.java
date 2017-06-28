package com.easimote.sdk.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import android.util.Log;

public class LogService {

	private static final String TAG = "EasinetSDK";
	private static boolean ENABLE_DEBUG_LOGGING = false;
	private static boolean ENABLE_CRASHLYTICS_LOGGING = false;
	private static Method CRASHLYTICS_LOG_METHOD;

	public LogService() {
	}

	public static void enableDebugLogging(boolean enableDebugLogging) {
		ENABLE_DEBUG_LOGGING = enableDebugLogging;
	}

	public static void enableCrashlyticsLogging(boolean enableCrashlytics) {
		if (enableCrashlytics)
			try {
				Class<?> crashlytics = Class.forName("com.crashlytics.android.Crashlytics");
				CRASHLYTICS_LOG_METHOD = crashlytics.getMethod("log",new Class[] { String.class });
				ENABLE_CRASHLYTICS_LOGGING = true;
			} catch (ClassNotFoundException e) {
			} catch (NoSuchMethodException e) {
			}
		else
			ENABLE_CRASHLYTICS_LOGGING = false;
	}

	public static void v(String msg) {
		if (ENABLE_DEBUG_LOGGING) {
			String logMsg = (new StringBuilder()).append(debugInfo()).append(msg).toString();
			Log.v(TAG, logMsg);
			logCrashlytics(logMsg);
		}
	}

	public static void d(String msg) {
		if (ENABLE_DEBUG_LOGGING) {
			String logMsg = (new StringBuilder()).append(debugInfo()).append(msg).toString();
			Log.d(TAG, logMsg);
			logCrashlytics(logMsg);
		}
	}

	public static void i(String msg) {
		String logMsg = (new StringBuilder()).append(debugInfo()).append(msg).toString();
		Log.i(TAG, logMsg);
		logCrashlytics(logMsg);
	}

	public static void w(String msg) {
		String logMsg = (new StringBuilder()).append(debugInfo()).append(msg).toString();
		Log.w(TAG, logMsg);
		logCrashlytics(logMsg);
	}

	public static void e(String msg) {
		String logMsg = (new StringBuilder()).append(debugInfo()).append(msg).toString();
		Log.e(TAG, logMsg);
		logCrashlytics(msg);
	}

	public static void e(String msg, Throwable e) {
		String logMsg = (new StringBuilder()).append(debugInfo()).append(msg).toString();
		Log.e(TAG, logMsg, e);
		logCrashlytics((new StringBuilder()).append(msg).append(" ").append(throwableAsString(e)).toString());
	}

	public static void wtf(String msg) {
		String logMsg = (new StringBuilder()).append(debugInfo()).append(msg).toString();
		Log.wtf(TAG, logMsg);
		logCrashlytics(logMsg);
	}

	public static void wtf(String msg, Exception exception) {
		String logMsg = (new StringBuilder()).append(debugInfo()).append(msg).toString();
		Log.wtf(TAG, logMsg, exception);
		logCrashlytics((new StringBuilder()).append(logMsg).append(" ").append(throwableAsString(exception)).toString());
	}

	private static String debugInfo() {
		StackTraceElement stackTrace[] = Thread.currentThread().getStackTrace();
		String className = stackTrace[4].getClassName();
		String methodName = Thread.currentThread().getStackTrace()[4].getMethodName();
		int lineNumber = stackTrace[4].getLineNumber();
		return (new StringBuilder()).append(className).append(".")
				.append(methodName).append(":").append(lineNumber).append(" ")
				.toString();
	}

	private static void logCrashlytics(String msg) {
		if (ENABLE_CRASHLYTICS_LOGGING)
			try {
				CRASHLYTICS_LOG_METHOD.invoke(null, new Object[] { (new StringBuilder()).append(debugInfo()).append(msg).toString() });
			} catch (Exception e) {
			}
	}

	private static String throwableAsString(Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

}