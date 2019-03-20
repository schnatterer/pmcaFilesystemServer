package info.schnatterer.pmcaFilesystemServer;

import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {
    public static final int PORT = 8080;

    public HttpServer() {
        super(PORT);
    }

    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {

        if (session.getUri().equals("/")) {
            return serveRoot();
        } else {
            /* TODO right now we serve the whole file system. This might be a security risk.
               Validation might make sense here
             */
            return serveFiles(session);
        }
    }
    private Response serveRoot() {
        StringBuilder response = new StringBuilder();
        response.append("<h1>Videos</h1>");
        createFileList(FilesystemScanner.getVideosOnExternalStorage(), response);

        response.append("<h1>JPEGs</h1>");
        createFileList(FilesystemScanner.getJpegsOnExternalStorage(), response);

        response.append("<h1>RAW</h1>");
        createFileList(FilesystemScanner.getRawsOnExternalStorage(), response);

        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, response.toString());
    }

    private void createFileList(List<File> files, StringBuilder response) {
        response.append("<ul>");
        for (File file : files) {
            response.append("<li><a href=\"");
            response.append(file.getAbsolutePath());
            response.append("\">");
            response.append(file.getName());
            response.append("</a></li>");
        }
        response.append("</ul>");
    }

    private Response serveFiles(IHTTPSession session) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(session.getUri());
            return newFixedLengthResponse(Response.Status.OK, getMimeType(session.getUri()), fis,
                    new File(session.getUri()).length());
        } catch (FileNotFoundException e) {
            Logger.info("File not found");
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, "File Not Found");
        }
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}
