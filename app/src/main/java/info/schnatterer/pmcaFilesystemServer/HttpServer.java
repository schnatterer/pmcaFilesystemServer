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
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.github.ma1co.openmemories.framework.DeviceInfo;
import fi.iki.elonen.SimpleWebServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.*;

@SuppressLint("ExifInterface")
public class HttpServer extends SimpleWebServer {

    static final int PORT = 8080;
    static final String HOST = null; // bind to all interfaces by default
    static final String WWW_ROOT = "/";
    static final boolean QUIET = false;

    private static final String MIME_JSON = "application/json";
    private static final String MIME_CSS = "text/css";
    private static final String MIME_JAVASCRIPT = "text/javascript";
    private static final String MIME_JPEG = "image/jpeg";
    public static final String VIDEO_FALLBACK_THUMBNAIL = "assets/img/video-fallback.jpg";

    private final Context context;

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
        Set<String> types;
        if (queryType.contains(",")) {
            types = new HashSet<>(Arrays.asList(queryType.split(",")));
        } else {
            types = new HashSet<>(Collections.singletonList(queryType));
        }

        List<File> fileList = FilesystemScanner.getFileOnExternalStorage(findFileExtensions(types));
        Collections.sort(fileList, new LastModifiedComparator());

        JSONArray list = new JSONArray();
        for (File file : fileList) {
            list.put(toJson(file));
        }

        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, list.toString());
    }

    private Set<String> findFileExtensions(Set<String> types) {
        Set<String> extensions = new HashSet<>();
        if (types.contains("image")) {
            extensions.addAll(FilesystemScanner.jpegFormats);
        }
        if (types.contains("video")) {
            extensions.addAll(FilesystemScanner.videoFormats);
        }
        if (types.contains("raw")) {
            extensions.addAll(FilesystemScanner.rawFormats);
        }
        return extensions;
    }

    private JSONObject toJson(File file) throws JSONException {
        JSONObject e = new JSONObject();
        e.put("name", file.getName());
        e.put("size", file.length());
        e.put("date", file.lastModified());
        e.put("file", file.getPath());
        e.put("preview", file.getPath());
        e.put("type", "image");
        e.put("thumbnail", "/thumbnail.do?f=" + file.getPath());

        String fileExt =  getExtension(file);
        if (FilesystemScanner.rawFormats.contains("." + fileExt.toLowerCase())) {
            e.put("type", "raw");
        } else if (FilesystemScanner.videoFormats.contains("." + fileExt.toLowerCase())) {
            e.put("type", "video");
        }
        return e;
    }


    private Response serveExif(IHTTPSession session) throws JSONException {
        Map<String, List<String>> query = decodeParameters(session.getQueryParameterString());
        if(query.get("f") == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing query parameter \"f\"");
        }
        String imagePath = query.get("f").get(0);
        File imageFile = new File(imagePath);

        JSONObject exifJson = new JSONObject();
        try {
            exifJson.put("Name", new File(imagePath).getName());
            exifJson.put("LastModified", new File(imagePath).lastModified());
            exifJson.put("success", true);

            addPreview(imageFile, exifJson);

            addMetadata(imageFile, exifJson);

        } catch (ImageProcessingException | MetadataException e) {
            exifJson.put("success", false);
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, e.getMessage());
        }

        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, exifJson.toString());
    }

    private void addMetadata(File imageFile, JSONObject exifJson) throws ImageProcessingException, IOException, MetadataException, JSONException {
        Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
        for (Directory directory : collectMetadataDirectories(metadata)) {
            addDescriptionIfExists(ExifDirectoryBase.TAG_FNUMBER, "FNumber", directory, exifJson);
            addIntIfExists(ExifDirectoryBase.TAG_FOCAL_LENGTH, "FocalLength", directory, exifJson);
            addDescriptionIfExists(ExifDirectoryBase.TAG_EXPOSURE_TIME, "ExposureTime", directory, exifJson);
            addIntIfExists(ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH, "ImageWidth", directory, exifJson);
            addIntIfExists(ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT, "ImageLength", directory, exifJson);
            addIntIfExists(ExifDirectoryBase.TAG_ISO_EQUIVALENT, "ISOSpeedRatings", directory, exifJson);
            addDescriptionIfExists(ExifDirectoryBase.TAG_MODEL, "Model", directory, exifJson);
            addDescriptionIfExists(ExifDirectoryBase.TAG_LENS_MODEL, "LensModel", directory, exifJson);
            addDescriptionIfExists(ExifDirectoryBase.TAG_LENS_SPECIFICATION, "LensSpecification", directory, exifJson);
        }
    }

    private void addIntIfExists(int positionInDir, String nameInJson, Directory directory, JSONObject json) throws MetadataException, JSONException {
        if(directory.containsTag(positionInDir)) {
            json.put(nameInJson, directory.getInt(positionInDir));
        }
    }
    
    private void addDescriptionIfExists(int positionInDir, String nameInJson, Directory directory, JSONObject json) throws MetadataException, JSONException {
        if(directory.containsTag(positionInDir)) {
            json.put(nameInJson, directory.getDescription(positionInDir));
        }
    }

    private Collection<Directory> collectMetadataDirectories(Metadata metadata) throws MetadataException {
        Set<Directory> directories = new HashSet<>();
        directories.addAll(metadata.getDirectoriesOfType(ExifIFD0Directory.class));
        directories.addAll(metadata.getDirectoriesOfType(ExifSubIFDDirectory.class));
        if(directories.isEmpty()) {
            throw new MetadataException("No entries for ExifSubIFDDirectory found");
        }
        return directories;
    }

    private void addPreview(File imageFile, JSONObject exifJson) throws JSONException {
        String fileExt = getExtension(imageFile);
        if (FilesystemScanner.rawFormats.contains("." + fileExt.toLowerCase())) {
            // If the file is ARW, we can't display it in the browser
            // so check if we have a jpg with the same file name we can show instead
            // If not, we use the thumbnail so we can show anything
            File jpgFile = changeExtension(imageFile, ".JPG");
            if (jpgFile.exists()) {
                exifJson.put("preview", jpgFile.getPath());
            } else {
                exifJson.put("preview", "/thumbnail.do?f=" + imageFile.getPath());
            }
        } else if (FilesystemScanner.videoFormats.contains("." + fileExt.toLowerCase())) {
            exifJson.put("preview", "/" + VIDEO_FALLBACK_THUMBNAIL);
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

            return createResponseFromAsset(path, findMimeType(path));
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Failed to load " + session.getUri());
        }
    }

    private Response createResponseFromAsset(String assetPath, String mime) throws IOException {
        @SuppressWarnings("java:S2095") // stream is closed by nanohttp after reading
        InputStream is = context.getAssets().open(assetPath);
        return newFixedLengthResponse(Response.Status.OK, mime, is, is.available());
    }

    private String findMimeType(String path) {
        String mime = MIME_HTML;
        if(path.endsWith(".css")) {
            mime = MIME_CSS;
        } else if(path.endsWith(".js")) {
            mime = MIME_JAVASCRIPT;
        } else if(path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            mime = MIME_JPEG;
        }
        return mime;
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
        
        String fileExt = getExtension(imageFile);
        if (FilesystemScanner.rawFormats.contains("." + fileExt.toLowerCase())) {
            return generateRawThumbnail(imageFile);
        } else if (FilesystemScanner.jpegFormats.contains("." + fileExt.toLowerCase())) {
            return generateJpgThumbnail(imagePath);
        } else if (FilesystemScanner.videoFormats.contains("." + fileExt.toLowerCase())) {
            return generateVideoThumbnail();
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Filetype not supported for " + imagePath);
    }

    private Response generateRawThumbnail(File imageFile) {
        try {
            // Try to read the thumbnail with the ImageMetadataReader
            InputStream is = readThumbnailImage(imageFile);
            return newFixedLengthResponse(Response.Status.OK, MIME_JPEG, is, is.available());
        } catch (Exception e) {
            // As a fallback, see if we have a JPEG file with the same name and use that
            File jpgFile = changeExtension(imageFile, ".JPG");
            if (jpgFile.exists()) {
                return generateJpgThumbnail(jpgFile.getPath());
            } else {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, 
                        "Filetype not supported for " + imageFile.getPath());
            }
        }
    }
    
    private Response generateJpgThumbnail(String imagePath) {
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

    private Response generateVideoThumbnail() {
        try {
            return createResponseFromAsset(VIDEO_FALLBACK_THUMBNAIL, MIME_JPEG);
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }

    /**
     * @return file extension without '.'
     */
    private String getExtension(File file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }

    /**
     * @return <code>file</code> with last dot and trailing characters replaced by 
     */
    private File changeExtension(File file, String newExtension) {
        return new File(file.getParentFile(), getFileNameWithoutExtension(file) + newExtension);
    }

    /**
     * @return <code>file</code>'s name with last dot and trailing characters removed.
     *   E.g. <code>/a/b/c/some.jpg</code> becomes <code>some</code>
     */
    private String getFileNameWithoutExtension(File file) {
        return file.getName().substring(0, file.getName().lastIndexOf("."));
    }

    /**
     * @return an open stream to the thumbnail. Must be closed by the caller.
     */
    private InputStream readThumbnailImage(File imageFile) throws IOException {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            ExifThumbnailDirectory thumbnailDirectory = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);
            long offset = thumbnailDirectory.getLong(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET);
            int length = thumbnailDirectory.getInt(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH);

            @SuppressWarnings("java:S2095") // stream is closed by the caller
            RandomAccessFile handle = new RandomAccessFile(imageFile, "r");
            FileChannel channel = handle.getChannel();
            channel.map(FileChannel.MapMode.READ_ONLY, offset, length);
            return Channels.newInputStream(channel);
        } catch (ImageProcessingException | IOException | MetadataException exception) {
            throw new IOException("Reading thumbnail failed", exception);
        } 
    }

    private String getLinkToLogFile() {
        File logFile = Logger.getFile();
        return logFile.getAbsolutePath();
    }

    private DeviceInfo getDeviceInfo() {
        return DeviceInfo.getInstance();
    }

    private static class LastModifiedComparator implements Comparator<File> {
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
    }
}
