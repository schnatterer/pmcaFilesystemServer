package info.schnatterer.pmcaFilesystemServer;

import com.github.ma1co.openmemories.framework.DeviceInfo;

import java.io.File;
import java.util.List;

import fi.iki.elonen.SimpleWebServer;

public class HttpServer extends SimpleWebServer {
    static final int PORT = 8080;
    static final String HOST = null; // bind to all interfaces by default
    static final String WWW_ROOT = "/";
    static final boolean QUIET = false;

    public HttpServer() {
        super(HOST, PORT, new File(WWW_ROOT).getAbsoluteFile(), QUIET);
    }

    @Override
    public Response serve(IHTTPSession session) {

        if (session.getUri().equals("/")) {
            return serveRoot();
        } else {
            return super.serve(session);
        }
    }

    private Response serveRoot() {
        String heading =  getDeviceInfo().getBrand() + " - " + getDeviceInfo().getModel();
        StringBuilder response = new StringBuilder("<html><head><title>" + heading
                + "</title><style><!--\n" + "span.dirname { font-weight: bold; }\n"
                + "span.filesize { font-size: 75%; }\n"
                + "// -->\n" + "</style>" + "</head><body><h1>" + heading + "</h1>");

        response.append("<h1>Videos</h1>");
        createFileList(FilesystemScanner.getVideosOnExternalStorage(), response);

        response.append("<h1>JPEGs</h1>");
        createFileList(FilesystemScanner.getJpegsOnExternalStorage(), response);

        response.append("<h1>RAW</h1>");
        createFileList(FilesystemScanner.getRawsOnExternalStorage(), response);

        response.append("<h1>Log File</h1>");
        createLinkToLogFile(response);

        response.append("<h1>File System</h1>");
        response.append(listDirectory("/", new File("/"))
                .replaceFirst("<html>.*<body>", ""));
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, response.toString());
    }

    private void createLinkToLogFile(StringBuilder response) {
        response.append("<a href=\"");
        File logFile = Logger.getFile();
        response.append(logFile.getAbsolutePath());
        response.append("\">");
        response.append(logFile.getName());
        response.append("</a>");
    }

    private DeviceInfo getDeviceInfo() {
        return DeviceInfo.getInstance();
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
}
