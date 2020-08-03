package info.schnatterer.pmcaFilesystemServer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.ExifInterface;
import android.text.TextUtils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
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
        try {
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
            if (BuildConfig.DEBUG) {
                response.addHeader("Access-Control-Allow-Origin", "*");
            }
            return response;
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }

    private Response serveMeta() throws JSONException {
        JSONObject meta = new JSONObject();
        meta.put("brand", getDeviceInfo().getBrand());
        meta.put("model", getDeviceInfo().getModel());
        meta.put("log", getLinkToLogFile());
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, meta.toString());
    }

    private Response serveList(IHTTPSession session) throws JSONException {
        Map<String, List<String>> query = decodeParameters(session.getQueryParameterString());
        if(query.get("type") == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing query parameter \"type\"");
        }
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
        for (File file : fileList) {
            JSONObject e = new JSONObject();
            e.put("name", file.getName());
            e.put("size", file.length());
            e.put("date", file.lastModified());
            e.put("file", file.getPath());
            e.put("preview", file.getPath());
            e.put("type", "image");
            e.put("thumbnail", "/thumbnail.do?f=" + file.getPath());

            String fileExt = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            if (fileExt.equalsIgnoreCase("ARW")) {
                e.put("type", "raw");
            } else if (Arrays.asList(FilesystemScanner.videoFormats).contains("." + fileExt.toLowerCase())) {
                e.put("type", "video");
            }

            list.put(e);
        }

        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, list.toString());
    }

    private Response serveExif(IHTTPSession session) {
        Map<String, List<String>> query = decodeParameters(session.getQueryParameterString());
        if(query.get("f") == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing query parameter \"f\"");
        }
        String imagePath = query.get("f").get(0);
        File imageFile = new File(imagePath);

        try {
            JSONObject exif = new JSONObject();
            exif.put("Name", new File(imagePath).getName());
            exif.put("LastModified", new File(imagePath).lastModified());

            // If the file is ARW, we can't display it in the browser
            // so check if we have a jpg with the same file name we can show instead
            // If not, we use the thumbnail so we can show anything
            String fileExt = imageFile.getName().substring(imageFile.getName().lastIndexOf(".") + 1);
            if (fileExt.equalsIgnoreCase("ARW")) {
                File jpgFile = new File(imageFile.getParentFile(), imageFile.getName().substring(0, imageFile.getName().lastIndexOf(".")) + ".JPG");
                if (jpgFile.exists()) {
                    exif.put("preview", jpgFile.getPath());
                } else {
                    exif.put("preview", "/thumbnail.do?f=" + imageFile.getPath());
                }
            }

            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

            ArrayList<Directory> directories = new ArrayList<>();
            directories.addAll(metadata.getDirectoriesOfType(ExifIFD0Directory.class));
            directories.addAll(metadata.getDirectoriesOfType(ExifSubIFDDirectory.class));
            if(directories.size() == 0) {
                throw new MetadataException("No entries for ExifSubIFDDirectory found");
            }
            for (Directory directory : directories) {
                if(directory.containsTag(ExifSubIFDDirectory.TAG_FNUMBER)) {
                    exif.put("FNumber", directory.getDescription(ExifSubIFDDirectory.TAG_FNUMBER));
                }
                if(directory.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                    exif.put("FocalLength", directory.getInt(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                }
                if(directory.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) {
                    exif.put("ExposureTime", directory.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                }
                if(directory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH)) {
                    exif.put("ImageWidth", directory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH));
                }
                if(directory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)) {
                    exif.put("ImageLength", directory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT));
                }
                if(directory.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) {
                    exif.put("ISOSpeedRatings", directory.getInt(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                }
                if(directory.containsTag(ExifIFD0Directory.TAG_MODEL)) {
                    exif.put("Model", directory.getDescription(ExifSubIFDDirectory.TAG_MODEL));
                }
                if(directory.containsTag(ExifSubIFDDirectory.TAG_LENS_MODEL)) {
                    exif.put("LensModel", directory.getDescription(ExifSubIFDDirectory.TAG_LENS_MODEL));
                }
                if(directory.containsTag(ExifSubIFDDirectory.TAG_LENS_SPECIFICATION)) {
                    exif.put("LensSpecification", directory.getDescription(ExifSubIFDDirectory.TAG_LENS_SPECIFICATION));
                }
            }

            return newFixedLengthResponse(Response.Status.OK, MIME_JSON, exif.toString());
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, e.getMessage());
        } catch (JSONException | ImageProcessingException | MetadataException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
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
            } else if(path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                mime = MIME_JPEG;
            }

            InputStream is = context.getAssets().open(path);
            return newFixedLengthResponse(Response.Status.OK, mime, is, is.available());
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Failed to load " + session.getUri());
        }
    }

    private Response generateThumbnail(IHTTPSession session) {
        Map<String, List<String>> query = decodeParameters(session.getQueryParameterString());
        if(query.get("f") == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing query parameter \"f\"");
        }

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
            } catch (Exception ignored) {

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
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
            }
        }

        if (Arrays.asList(FilesystemScanner.videoFormats).contains("." + ext.toLowerCase())) {
            try {
                InputStream is = context.getAssets().open("assets/img/video-fallback.jpg");
                return newFixedLengthResponse(Response.Status.OK, MIME_JPEG, is, is.available());
            } catch (IOException e) {
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
        } catch (Exception ignored) {
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
