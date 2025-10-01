package app.revanced.extension.shared.utils;

import static app.revanced.extension.shared.settings.BaseSettings.DEBUG;
import static app.revanced.extension.shared.settings.BaseSettings.DEBUG_TOAST_ON_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.preference.LogBufferManager;

/**
 * ReVanced specific logger.  Logging is done to standard device log (accessible thru ADB),
 * and additionally accessible thru {@link LogBufferManager}.
 * <p>
 * All methods are thread safe, and are safe to call even
 * if {@link Utils#getContext()} is not available.
 */
@SuppressWarnings("unused")
public class Logger {

    /**
     * Log messages using lambdas.
     */
    public interface LogMessage {
        /**
         * @return Logger string message. This method is only called if logging is enabled.
         */
        @NonNull
        String buildMessageString();
    }

    private enum LogLevel {
        DEBUG,
        INFO,
        ERROR,
        WARN,
        WTF,
    }

    /**
     * Log tag prefix. Only used for system logging.
     */
    private static final String REVANCED_LOG_TAG_PREFIX = "Extended: ";

    /**
     * @return For outer classes, this returns {@link Class#getSimpleName()}.
     * For static, inner, or anonymous classes, this returns the simple name of the enclosing class.
     * <br>
     * For example, each of these classes returns 'SomethingView':
     * <code>
     * com.company.SomethingView
     * com.company.SomethingView$StaticClass
     * com.company.SomethingView$1
     * </code>
     */
    private static String getOuterClassSimpleName(Object obj) {
        Class<?> logClass = obj.getClass();
        String fullClassName = logClass.getName();
        final int dollarSignIndex = fullClassName.indexOf('$');
        if (dollarSignIndex < 0) {
            return logClass.getSimpleName(); // Already an outer class.
        }

        // Class is inner, static, or anonymous.
        // Parse the simple name full name.
        // A class with no package returns index of -1, but incrementing gives index zero which is correct.
        final int simpleClassNameStartIndex = fullClassName.lastIndexOf('.') + 1;
        return fullClassName.substring(simpleClassNameStartIndex, dollarSignIndex);
    }

    /**
     * Internal method to handle logging to Android Log and {@link LogBufferManager}.
     * Appends the log message, stack trace (if enabled), and exception (if present) to logBuffer
     * with class name but without 'revanced:' prefix.
     *
     * @param logLevel  The log level.
     * @param message   Log message object.
     * @param ex        Optional exception.
     * @param showToast If a toast is to be shown.
     */
    private static void logInternal(LogLevel logLevel, LogMessage message, @Nullable Throwable ex, boolean showToast) {
        // It's very important that no Settings are used in this method,
        // as this code is used when a context is not set and thus referencing
        // a setting will crash the app.
        String messageString = message.buildMessageString();
        String className = getOuterClassSimpleName(message);

        String logText = messageString;

        // Append exception message if present.
        if (ex != null) {
            var exceptionMessage = ex.getMessage();
            if (exceptionMessage != null) {
                logText += "\nException: " + exceptionMessage;
            }
        }

        // Do not include "revanced:" prefix in clipboard logs.
        String managerToastString = className + ": " + logText;
        LogBufferManager.appendToLogBuffer(managerToastString);

        String logTag = REVANCED_LOG_TAG_PREFIX + className;
        switch (logLevel) {
            case DEBUG:
                if (ex == null) Log.d(logTag, logText);
                else Log.d(logTag, logText, ex);
                break;
            case INFO:
                if (ex == null) Log.i(logTag, logText);
                else Log.i(logTag, logText, ex);
                break;
            case WARN:
                if (ex == null) Log.w(logTag, logText);
                else Log.w(logTag, logText, ex);
                break;
            case ERROR:
                if (ex == null) Log.e(logTag, logText);
                else Log.e(logTag, logText, ex);
                break;
            case WTF:
                if (ex == null) Log.wtf(logTag, logText);
                else Log.wtf(logTag, logText, ex);
                break;
        }

        if (showToast) {
            Utils.showToastLong(managerToastString);
        }
    }

    private static boolean shouldLogDebug() {
        // If the app is still starting up and the context is not yet set,
        // then allow debug logging regardless what the debug setting actually is.
        return Utils.context == null || DEBUG.get();
    }

    private static boolean shouldShowErrorToast() {
        return Utils.context != null && DEBUG_TOAST_ON_ERROR.get();
    }

    /**
     * Logs debug messages under the outer class name of the code calling this method.
     * <p>
     * Whenever possible, the log string should be constructed entirely inside
     * {@link LogMessage#buildMessageString()} so the performance cost of
     * building strings is paid only if {@link BaseSettings#DEBUG} is enabled.
     */
    public static void printDebug(LogMessage message) {
        printDebug(message, null);
    }

    /**
     * Logs debug messages under the outer class name of the code calling this method.
     * <p>
     * Whenever possible, the log string should be constructed entirely inside
     * {@link LogMessage#buildMessageString()} so the performance cost of
     * building strings is paid only if {@link BaseSettings#DEBUG} is enabled.
     */
    public static void printDebug(LogMessage message, @Nullable Exception ex) {
        if (shouldLogDebug()) {
            logInternal(LogLevel.DEBUG, message, ex, false);
        }
    }

    /**
     * Logs information messages using the outer class name of the code calling this method.
     */
    public static void printInfo(LogMessage message) {
        printInfo(message, null);
    }

    /**
     * Logs information messages using the outer class name of the code calling this method.
     */
    public static void printInfo(LogMessage message, @Nullable Exception ex) {
        logInternal(LogLevel.INFO, message, ex, false);
    }

    /**
     * Logs warning messages using the outer class name of the code calling this method.
     */
    public static void printWarn(LogMessage message) {
        printWarn(message, null);
    }

    /**
     * Logs warning messages using the outer class name of the code calling this method.
     */
    public static void printWarn(LogMessage message, @Nullable Exception ex) {
        logInternal(LogLevel.WARN, message, ex, false);
    }

    /**
     * Logs exceptions under the outer class name of the code calling this method.
     * Appends the log message, exception (if present), and toast message (if enabled) to logBuffer.
     */
    public static void printException(LogMessage message) {
        printException(message, null);
    }

    /**
     * Logs exceptions under the outer class name of the code calling this method.
     * <p>
     * If the calling code is showing it's own error toast,
     * instead use {@link #printInfo(LogMessage, Exception)}
     *
     * @param message log message
     * @param ex      exception (optional)
     */
    public static void printException(LogMessage message, @Nullable Throwable ex) {
        logInternal(LogLevel.ERROR, message, ex, shouldShowErrorToast());
    }

    /**
     * Logs WTF (What a Terrible Failure) messages using the outer class name of the code calling this method.
     */
    public static void printWTF(LogMessage message) {
        printWTF(message, null);
    }

    /**
     * Logs WTF (What a Terrible Failure) messages using the outer class name of the code calling this method.
     */
    public static void printWTF(LogMessage message, @Nullable Exception ex) {
        logInternal(LogLevel.WTF, message, ex, false);
    }
}