package xda.xlafbk.aanotificationforwarder;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger {

    private static final String TAG = "FileLogger";
    private static final String LOG_FILE_NAME = "log.txt";
    private FileWriter writer;
    private boolean enabled;

    public FileLogger(Context context, boolean enable) {
        File[] mediaDirs = context.getExternalMediaDirs();
        // /sdcard/Android/media/xda.xlafbk.aanotificationforwarder/
        File logDirectory = mediaDirs[0];
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
        enabled = enable;
        if (enabled) {
            try {
            writer = new FileWriter(new File(logDirectory, LOG_FILE_NAME), true);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to log file: " + e.getMessage());
            }
        }
    }

    public void log(String message, String... args) {
        if (!enabled) return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String logMessage = String.format("%s: %s\n", sdf.format(new Date()), (args.length == 0 ? message : String.format(message, args)));
        try {
            writer.append(logMessage);
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file: " + e.getMessage());
        }
    }
}