package info.schnatterer.pmcaFilesystemServer;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 * Bases on https://github.com/Bostwickenator/STGUploader/blob/master/app/src/main/java/org/bostwickenator/googlephotos/FilesystemScanner.java
 * Commit b8ce40d
 */
class FilesystemScanner {

    private static final String[] rawFormats = {".arw"};
    private static final String[] jpegFormats = {".jpg"};
    private static final String[] videoFormats = {".mts", ".mp4"};

    public static List<File> getRawsOnExternalStorage() {
        return getFilteredFileList(Environment.getExternalStorageDirectory(), rawFormats);
    }

    public static List<File> getJpegsOnExternalStorage() {
        return getFilteredFileList(Environment.getExternalStorageDirectory(), jpegFormats);
    }

    public static List<File> getVideosOnExternalStorage() {
        return getFilteredFileList(Environment.getExternalStorageDirectory(), videoFormats);
    }

    private static List<File> getFilteredFileList(File directory, String... extensions) {
        File[] subFiles = directory.listFiles();
        List<File> filtered = new ArrayList<>();
        if (subFiles != null) {
            for (File f : subFiles) {
                String filename = f.getName().toLowerCase();
                if (f.isFile()) {
                    for (String extension : extensions) {
                        if (filename.endsWith(extension)) {
                            filtered.add(f);
                            break;
                        }
                    }
                } else if (f.isDirectory()) {
                    filtered.addAll(getFilteredFileList(f, extensions));
                }
            }
        }
        return filtered;
    }
}
