package info.schnatterer.pmcaFilesystemServer;

import android.os.Environment;
import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public static File getFile() {
        // e.g. /storage/sdcard0/pmcaFilesystemServer/LOG.TXT
        return new File(Environment.getExternalStorageDirectory(), "pmcaFilesystemServer/LOG.TXT");
    }

    protected static void log(String msg) {
        try {
            getFile().getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(getFile(), true));
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
            writer.append(sdf.format(new Date()) + " ");
            writer.append(msg);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            Log.e("pmcaFilesystemServer", "Error writing log", e);
        }
    }
    protected static void log(String type, String msg) { log("[" + type + "] " + msg); }

    public static void info(String msg) { log("INFO", msg); }
    public static void error(String msg) { log("ERROR", msg); }
}
