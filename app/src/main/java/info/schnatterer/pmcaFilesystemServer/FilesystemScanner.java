package info.schnatterer.pmcaFilesystemServer;

import android.os.Environment;

import java.io.File;
import java.util.*;

/*
 * Bases on https://github.com/Bostwickenator/STGUploader/blob/master/app/src/main/java/org/bostwickenator/googlephotos/FilesystemScanner.java
 * Commit b8ce40d
 */
class FilesystemScanner {

    protected static final Set<String> rawFormats = Collections.unmodifiableSet(Collections.singleton(".arw"));
    protected static final Set<String> jpegFormats = Collections.unmodifiableSet(Collections.singleton(".jpg"));
    protected static final Set<String> videoFormats = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(".mts", ".mp4")));

    public static List<File> getFileOnExternalStorage(Collection<String> extensions) {
        return getFilteredFileList(Environment.getExternalStorageDirectory(), extensions);
    }

    private static List<File> getFilteredFileList(File directory, Collection<String> extensions) {
        File[] subFiles = directory.listFiles();
        List<File> filtered = new LinkedList<>();
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
