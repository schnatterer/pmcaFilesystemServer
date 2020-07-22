package info.schnatterer.pmcaFilesystemServer;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
 * Bases on https://github.com/Bostwickenator/STGUploader/blob/master/app/src/main/java/org/bostwickenator/googlephotos/FilesystemScanner.java
 * Commit b8ce40d
 */
class FilesystemScanner {

    public static final String[] rawFormats = {".arw"};
    public static final String[] jpegFormats = {".jpg"};
    public static final String[] videoFormats = {".mts", ".mp4"};

    public static List<File> getFileOnExternalStorage(String... extensions) {
        return getFilteredFileList(Environment.getExternalStorageDirectory(), extensions);
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
