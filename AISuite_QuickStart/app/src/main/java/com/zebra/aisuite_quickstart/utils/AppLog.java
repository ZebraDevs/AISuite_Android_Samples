package com.zebra.aisuite_quickstart.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public final class AppLog {

    private static final String DEFAULT_TAG = "AISuiteQuickStart";
    private static final String LOG_CONFIG_DIRECTORY = "/data/local/tmp";

    private static final String LOG_CONFIG_FILE_NAME = "aisuite_quickstart.json";
    private static final String JSON_LOG_LEVEL = "logLevel";

    private static String appTag = DEFAULT_TAG;

    /**
     * Android log priorities:
     * VERBOSE = 2
     * DEBUG   = 3
     * INFO    = 4
     * WARN    = 5
     * ERROR   = 6
     * ASSERT  = 7
     */
    private static int minLogLevel = Log.INFO;

    private AppLog() {
        // Utility class
    }

    public static void init(Context context) {
        init(DEFAULT_TAG);
    }

    public static void init(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            appTag = tag;
        }
        // default
        resolveLogLevel("INFO");

        // JSON config overrides default.
        loadLogLevelFromJson();
    }

    private static void loadLogLevelFromJson() {
        File configFile = new File(LOG_CONFIG_DIRECTORY, LOG_CONFIG_FILE_NAME);

        Log.i(appTag, "[AppLog] Looking for log config at: " + configFile.getAbsolutePath());

        if (!configFile.exists()) {
            Log.w(appTag, "[AppLog] Log config file not found: " + configFile.getAbsolutePath());
            return;
        }

        if (!configFile.canRead()) {
            Log.w(appTag, "[AppLog] Log config file is not readable: " + configFile.getAbsolutePath());
            return;
        }

        try {
            String json = readFile(configFile);
            JSONObject config = new JSONObject(json);

            if (config.has(JSON_LOG_LEVEL)) {
                String configuredLogLevel = config.optString(JSON_LOG_LEVEL, "INFO");
                minLogLevel = resolveLogLevel(configuredLogLevel);

                Log.i(appTag, "[AppLog] Loaded log level from JSON: " + configuredLogLevel + " / " + minLogLevel);
            } else {
                Log.w(appTag, "[AppLog] JSON does not contain key: " + JSON_LOG_LEVEL);
            }
        } catch (Exception exception) {
            Log.println(Log.WARN, appTag, "[AppLog] Failed to read log config JSON: " + exception.getMessage());
        }
    }


    private static String readFile(File file) throws Exception {
        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }

        return builder.toString();
    }

    public static void setLogLevel(int logLevel) {
        if (logLevel < Log.VERBOSE || logLevel > Log.ASSERT) {
            throw new IllegalArgumentException("Invalid log level: " + logLevel);
        }

        minLogLevel = logLevel;
    }

    public static void setLogLevel(String logLevel) {
        minLogLevel = resolveLogLevel(logLevel);
    }

    public static int getLogLevel() {
        return minLogLevel;
    }

    public static void v(String classTag, String message) {
        log(Log.VERBOSE, classTag, message, null);
    }

    public static void d(String classTag, String message) {
        log(Log.DEBUG, classTag, message, null);
    }

    public static void i(String classTag, String message) {
        log(Log.INFO, classTag, message, null);
    }

    public static void w(String classTag, String message) {
        log(Log.WARN, classTag, message, null);
    }

    public static void e(String classTag, String message) {
        log(Log.ERROR, classTag, message, null);
    }

    public static void e(String classTag, String message, Throwable throwable) {
        log(Log.ERROR, classTag, message, throwable);
    }

    public static void wtf(String classTag, String message) {
        log(Log.ASSERT, classTag, message, null);
    }

    public static void printStackTrace(String classTag, Throwable throwable) {
        if (throwable == null) {
            return;
        }

        log(Log.ERROR, classTag, Log.getStackTraceString(throwable), null);
    }

    private static void log(int priority, String classTag, String message, Throwable throwable) {
        if (priority < minLogLevel) {
            return;
        }

        String safeClassTag = classTag == null ? "Unknown" : classTag;
        String safeMessage = message == null ? "" : message;
        String formattedMessage = "[" + safeClassTag + "] " + safeMessage;

        if (throwable == null) {
            Log.println(priority, appTag, formattedMessage);
        } else {
            Log.println(priority, appTag, formattedMessage + "\n" + Log.getStackTraceString(throwable));
        }
    }

    public static int resolveLogLevel(String logLevelString) {
        if (logLevelString == null || logLevelString.trim().isEmpty()) {
            return Log.INFO;
        }

        switch (logLevelString.trim().toUpperCase()) {
            case "VERBOSE":
                return Log.VERBOSE;
            case "DEBUG":
                return Log.DEBUG;
            case "WARN":
            case "WARNING":
                return Log.WARN;
            case "ERROR":
                return Log.ERROR;
            case "ASSERT":
            case "WTF":
                return Log.ASSERT;
            default:
                return Log.INFO;
        }
    }

    public static String logLevelToString(int logLevel) {
        switch (logLevel) {
            case Log.VERBOSE:
                return "VERBOSE";
            case Log.DEBUG:
                return "DEBUG";
            case Log.WARN:
                return "WARN";
            case Log.ERROR:
                return "ERROR";
            case Log.ASSERT:
                return "ASSERT";
            default:
                return "INFO";
        }
    }
}
