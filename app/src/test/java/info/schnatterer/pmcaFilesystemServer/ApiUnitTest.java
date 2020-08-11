package info.schnatterer.pmcaFilesystemServer;

import android.media.ExifInterface;
import junit.framework.Assert;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
public class ApiUnitTest extends BaseHttpTest {

    @Test
    public void get_meta() throws Exception {
        String body = apiGet("/api/meta.do");
        Assert.assertNotNull(body);
    }

    @Test
    public void list_empty() throws Exception {
        String body = apiGet("/api/list.do?type=none");

        JSONArray json = new JSONArray(body);

        Assert.assertNotNull(json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void list_image() throws Exception {
        setupFile(TestFile.JPG);
        String body = apiGet("/api/list.do?type=image");

        JSONArray json = new JSONArray(body);

        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 0);
    }

    @Test
    public void list_raw() throws Exception {
        setupFile(TestFile.RAW);
        String body = apiGet("/api/list.do?type=raw");

        JSONArray json = new JSONArray(body);

        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 0);
    }

    @Test
    public void list_video() throws Exception {
        setupFile(TestFile.VIDEO);
        String body = apiGet("/api/list.do?type=video");

        JSONArray json = new JSONArray(body);

        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 0);
    }

    @Test
    public void list_all() throws Exception {
        String body = apiGet("/api/list.do?type=image,raw,video");

        JSONArray json = new JSONArray(body);

        Assert.assertNotNull(json);
    }

    @Test
    public void list_missing_query() throws Exception {
        Response response = apiGetResponse("/api/list.do");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(400, response.code());
    }

    @Test
    public void exif_raw() throws Exception {
        String absoluteFilePath = setupFile(TestFile.RAW);
        String body = apiGet("/api/exif.do?f=" + absoluteFilePath);

        JSONObject json = new JSONObject(body);

        Assert.assertNotNull(json);

        Assert.assertNotNull(json.optString("Name"));
        Assert.assertNotNull(json.optString("Model"));
        Assert.assertNotSame(0, json.optLong("LastModified"));
        Assert.assertNotSame(0, json.optInt("ImageWidth"));
        Assert.assertNotSame(0, json.optInt("ImageLength"));
        Assert.assertNotNull(json.optString("FNumber"));
        Assert.assertNotSame(-1, json.optInt("FocalLength", -1));
        Assert.assertNotSame(-1, json.optInt("ISOSpeedRatings", -1));
        Assert.assertNotNull(json.optString("ExposureTime"));
    }

    @Test
    public void exif_jpg() throws Exception {
        String absoluteFilePath = setupFile(TestFile.JPG);
        String body = apiGet("/api/exif.do?f=" + absoluteFilePath);

        JSONObject json = new JSONObject(body);

        Assert.assertNotNull(json);

        Assert.assertNotNull(json.optString("Name"));
        Assert.assertNotNull(json.optString("Model"));
        Assert.assertNotSame(0, json.optLong("LastModified"));
        Assert.assertNotSame(0, json.optInt("ImageWidth"));
        Assert.assertNotSame(0, json.optInt("ImageLength"));
        Assert.assertNotNull(json.optString("FNumber"));
        Assert.assertNotSame(-1, json.optInt("FocalLength", -1));
        Assert.assertNotSame(-1, json.optInt("ISOSpeedRatings", -1));
        Assert.assertNotNull(json.optString("ExposureTime"));
    }

    @Test
    public void exif_fail() throws Exception {
        Response response = apiGetResponse("/api/exif.do?f=/sdcard/DCIM/invalid-file.JPG");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(404, response.code());
    }

    @Test
    public void exif_missing_query() throws Exception {
        Response response = apiGetResponse("/api/exif.do");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(400, response.code());
    }

    @Test
    public void exif_bad_file() throws Exception {
        String absoluteFilePath = setupFile(TestFile.VIDEO);
        Response response = apiGetResponse("/api/exif.do?f=" + absoluteFilePath);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());

        JSONObject json = new JSONObject(response.body().string());

        Assert.assertNotNull(json);
        Assert.assertFalse(json.optBoolean("success", true));
    }

    @Test
    public void exif_missing_file() throws Exception {
        Response response = apiGetResponse("/api/exif.do?f=/does-not-exist.txt");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(404, response.code());
    }

    @Test
    public void thumbnail_raw() throws Exception {
        String absoluteFilePath = setupFile(TestFile.RAW);
        Response response = apiGetResponse("/thumbnail.do?f=" + absoluteFilePath);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals("image/jpeg", response.header("Content-Type"));
        Assert.assertTrue(Integer.parseInt(response.header("Content-Length")) > 0);
    }

    @Test
    @Config(shadows = ExifInterfaceShadow.class)
    public void thumbnail_jpg() throws Exception {
        String absoluteFilePath = setupFile(TestFile.JPG);
        Response response = apiGetResponse("/thumbnail.do?f=" + absoluteFilePath);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals("image/jpeg", response.header("Content-Type"));
        Assert.assertTrue(Integer.parseInt(response.header("Content-Length")) > 0);
    }

    @Test
    public void thumbnail_video() throws Exception {
        String absoluteFilePath = setupFile(TestFile.VIDEO);
        Response response = apiGetResponse("/thumbnail.do?f=" + absoluteFilePath);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals("image/jpeg", response.header("Content-Type"));
        Assert.assertTrue(Integer.parseInt(response.header("Content-Length")) > 0);
    }

    @Test
    public void thumbnail_missing_query() throws Exception {
        Response response = apiGetResponse("/thumbnail.do");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(400, response.code());
    }

    @Test
    public void thumbnail_fail() throws Exception {
        Response response = apiGetResponse("/thumbnail.do?f=/sdcard/DCIM/invalid-file.JPG");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(404, response.code());
    }

    @Test
    public void thumbnail_unsupported_file_type() throws Exception {
        Response response = apiGetResponse("/thumbnail.do?f=/sdcard/DCIM/other-file.txt");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(404, response.code());
    }

    /** This class avoids running into an NPE during constructor call. **/
    @Implements(ExifInterface.class)
    @SuppressWarnings("unused") // The methods are weaved in via byte code manipulation
    public static class ExifInterfaceShadow {

        public void __constructor__(String filename) {
        }

        @Implementation
        public byte[] getThumbnail() {
            return "mocked".getBytes();
        }
    }
}