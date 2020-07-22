package info.schnatterer.pmcaFilesystemServer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.ExifInterface;
import android.text.TextUtils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.github.ma1co.openmemories.framework.DeviceInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.SimpleWebServer;

@SuppressLint("ExifInterface")
public class HttpServer extends SimpleWebServer {
    private static final String MIME_JSON = "application/json";
    private static final String MIME_CSS = "text/css";
    private static final String MIME_JAVASCRIPT = "text/javascript";
    private static final String MIME_JPEG = "image/jpeg";

    private static final String[] EXIF_TAGS = new String[] {
            "Make",
            "Model",
            "DateTime",
            "FNumber",
            "ExposureTime",
            "FocalLength",
            "ISOSpeedRatings",
            "Orientation",
            "Flash",
            "ImageWidth",
            "ImageLength",
    };

    static final int PORT = 8080;
    static final String HOST = null; // bind to all interfaces by default
    static final String WWW_ROOT = "/";
    static final boolean QUIET = false;

    private Context context;

    public HttpServer(Context context) {
        super(HOST, PORT, new File(WWW_ROOT).getAbsoluteFile(), QUIET);
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response;
        if (session.getUri().equals("/") || session.getUri().startsWith("/assets/")) {
            response = serveAssets(session);
        } else if (session.getUri().equals("/thumbnail.do")) {
            response = generateThumbnail(session);
        } else if (session.getUri().equals("/api/list.do")) {
            response = serveList(session);
        } else if (session.getUri().equals("/api/meta.do")) {
            response = serveMeta();
        } else if (session.getUri().equals("/api/exif.do")) {
            response = serveExif(session);
        } else {
            response = super.serve(session);
        }
        if(BuildConfig.DEBUG) {
            if(response.getMimeType().equals(MIME_JSON)) {
                response.addHeader("Access-Control-Allow-Origin", "*");
            }
        }
        return response;
    }

    private Response serveMeta() {
        JSONObject meta = new JSONObject();
        try {
            meta.put("brand", getDeviceInfo().getBrand());
            meta.put("model", getDeviceInfo().getModel());
            meta.put("log", getLinkToLogFile());
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, meta.toString());
    }

    private Response serveList(IHTTPSession session) {
        Map<String, List<String>> query = decodeParameters(session.getQueryParameterString());
        String queryType = query.get("type").get(0);
        List<String> types = new ArrayList<>();
        if (queryType.contains(",")) {
            types = Arrays.asList(queryType.split(","));
        } else {
            types.add(queryType);
        }

        List<String> ext = new ArrayList<>();
        if (types.contains("image")) {
            ext.addAll(Arrays.asList(FilesystemScanner.jpegFormats));
        }
        if (types.contains("video")) {
            ext.addAll(Arrays.asList(FilesystemScanner.videoFormats));
        }
        if (types.contains("raw")) {
            ext.addAll(Arrays.asList(FilesystemScanner.rawFormats));
        }

        List<File> fileList = FilesystemScanner.getFileOnExternalStorage(ext.toArray(new String[0]));
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                long k = file1.lastModified() - file2.lastModified();
                if (k < 0) {
                    return 1;
                } else if (k == 0) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });

        JSONArray list = new JSONArray();
        try {
            for (File file : fileList) {
                JSONObject e = new JSONObject();
                e.put("name", file.getName());
                e.put("size", file.length());
                e.put("date", file.lastModified());
                e.put("file", file.getPath());
                e.put("thumbnail", "/thumbnail.do?f=" + file.getPath());

                list.put(e);
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, list.toString());
    }

    private Response serveExif(IHTTPSession session) {
        Map<String, List<String>> query = decodeParameters(session.getQueryParameterString());
        String imagePath = query.get("f").get(0);

        // TODO Use metadata-extractor to read EXIF for RAW/ARW files

        ExifInterface exif;
        try {
            exif = new ExifInterface(imagePath);
        } catch (IOException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }

        JSONObject e = new JSONObject();
        try {
            e.put("Name", new File(imagePath).getName());
            e.put("LastModified", new File(imagePath).lastModified());
            for(String key : EXIF_TAGS) {
                e.put(key, exif.getAttribute(key));
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, e.toString());
    }

    private Response serveAssets(IHTTPSession session) {
        try {
            String path = session.getUri();
            if(TextUtils.isEmpty(path) || "/".equals(path)) {
                path = "index.html";
            }
            while (path.startsWith("/")) {
                path = path.substring(1);
            }

            String mime = MIME_HTML;
            if(path.endsWith(".css")) {
                mime = MIME_CSS;
            } else if(path.endsWith(".js")) {
                mime = MIME_JAVASCRIPT;
            }

            InputStream is = context.getAssets().open(path);
            return newFixedLengthResponse(Response.Status.OK, mime, is, is.available());
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Failed to load " + session.getUri());
        }
    }

    private Response generateThumbnail(IHTTPSession session) {
        Map<String, List<String>> query = decodeParameters(session.getQueryParameterString());
        String imagePath = query.get("f").get(0);
        File imageFile = new File(imagePath);

        if(!imageFile.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, imagePath + " not found");
        }

        String ext = imageFile.getName().substring(imageFile.getName().lastIndexOf(".") + 1);

        if (ext.equalsIgnoreCase("ARW")) {
            // Try to read the thumbnail with the ImageMetadataReader
            try {
                byte[] buffer = getThumbnailImage(imageFile);
                if (buffer == null || buffer.length == 0) {
                    throw new IOException("Failed to read thumbnail from EXIF in " + imagePath);
                }
                ByteArrayInputStream bs = new ByteArrayInputStream(buffer);
                return newFixedLengthResponse(Response.Status.OK, MIME_JPEG, bs, buffer.length);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // As a fallback, see if we have a JPEG file with the same name and use that
            File jpgFile = new File(imageFile.getParentFile(), imageFile.getName().substring(0, imageFile.getName().lastIndexOf(".")) + ".JPG");
            if (jpgFile.exists()) {
                imagePath = jpgFile.getPath();
                ext = "JPG";
            }
        }

        if (ext.equalsIgnoreCase("JPG")) {
            try {
                ExifInterface exif = new ExifInterface(imagePath);
                byte[] thumbnail = exif.getThumbnail();
                if (thumbnail == null || thumbnail.length == 0) {
                    throw new IOException("Failed to read thumbnail from EXIF in " + imagePath);
                }

                ByteArrayInputStream bs = new ByteArrayInputStream(thumbnail);
                Response res = newFixedLengthResponse(Response.Status.OK, MIME_JPEG, bs, thumbnail.length);
                res.addHeader("Accept-Ranges", "bytes");
                return res;
            } catch (IOException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Filetype not supported for " + imagePath);
    }

    private byte[] getThumbnailImage(File imageFile) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            ExifThumbnailDirectory thumbnailDirectory = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);
            long offset = thumbnailDirectory.getLong(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET);
            int length = thumbnailDirectory.getInt(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH);

            RandomAccessFile handle = new RandomAccessFile(imageFile, "r");
            handle.seek(offset);
            byte[] buffer = new byte[length];
            handle.readFully(buffer);

            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getLinkToLogFile() {
        File logFile = Logger.getFile();
        return logFile.getAbsolutePath();
    }

    private DeviceInfo getDeviceInfo() {
        return DeviceInfo.getInstance();
    }
}
