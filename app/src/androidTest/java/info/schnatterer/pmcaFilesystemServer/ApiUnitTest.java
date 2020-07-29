package info.schnatterer.pmcaFilesystemServer;

import android.Manifest;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.Response;

@RunWith(AndroidJUnit4.class)
public class ApiUnitTest extends BaseHttpTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void get_meta() {
        String body = apiGet("/api/meta.do");
        Assert.assertNotNull(body);
    }

    @Test
    public void list_empty() {
        String body = apiGet("/api/list.do?type=none");

        JSONArray json = null;
        try {
            json = new JSONArray(body);
        } catch (JSONException ignored) {
        }

        Assert.assertNotNull(json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void list_image() {
        String body = apiGet("/api/list.do?type=image");

        JSONArray json = null;
        try {
            json = new JSONArray(body);
        } catch (JSONException ignored) {
        }

        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 0);
    }

    @Test
    public void list_raw() {
        String body = apiGet("/api/list.do?type=raw");

        JSONArray json = null;
        try {
            json = new JSONArray(body);
        } catch (JSONException ignored) {
        }

        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 0);
    }

    @Test
    public void list_video() {
        String body = apiGet("/api/list.do?type=video");

        JSONArray json = null;
        try {
            json = new JSONArray(body);
        } catch (JSONException ignored) {
        }

        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 0);
    }

    @Test
    public void list_all() {
        String body = apiGet("/api/list.do?type=image,raw,video");

        JSONArray json = null;
        try {
            json = new JSONArray(body);
        } catch (JSONException ignored) {
        }

        Assert.assertNotNull(json);
    }

    @Test
    public void list_missing_query() {
        Response response = apiGetResponse("/api/list.do");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(400, response.code());
    }

    @Test
    public void exif_raw() {
        String body = apiGet("/api/exif.do?f=" + getFileOfType("raw"));

        JSONObject json = null;
        try {
            json = new JSONObject(body);
            Log.i("API", "exif_raw: " + json.toString(2));
        } catch (JSONException ignored) {

        }

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
    public void exif_jpg() {
        String body = apiGet("/api/exif.do?f=" + getFileOfType("image"));

        JSONObject json = null;
        try {
            json = new JSONObject(body);
            Log.i("API", "exif_jpg: " + json.toString(2));
        } catch (JSONException ignored) {

        }

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
    public void exif_fail() {
        Response response = apiGetResponse("/api/exif.do?f=/sdcard/DCIM/invalid-file.JPG");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(404, response.code());
    }

    @Test
    public void exif_missing_query() {
        Response response = apiGetResponse("/api/exif.do");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(400, response.code());
    }

    @Test
    public void exif_bad_file() {
        Response response = apiGetResponse("/api/exif.do?f=/default.prop");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(500, response.code());
    }

    @Test
    public void thumbnail_raw() {
        Response response = apiGetResponse("/thumbnail.do?f=" + getFileOfType("raw"));
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals("image/jpeg", response.header("Content-Type"));
        Assert.assertTrue(Integer.parseInt(response.header("Content-Length")) > 0);
    }

    @Test
    public void thumbnail_jpg() {
        Response response = apiGetResponse("/thumbnail.do?f=" + getFileOfType("image"));
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals("image/jpeg", response.header("Content-Type"));
        Assert.assertTrue(Integer.parseInt(response.header("Content-Length")) > 0);
    }

    @Test
    public void thumbnail_video() {
        Response response = apiGetResponse("/thumbnail.do?f=" + getFileOfType("video"));
        Log.i("API", "thumbnail_video: " + response);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals("image/jpeg", response.header("Content-Type"));
        Assert.assertTrue(Integer.parseInt(response.header("Content-Length")) > 0);
    }

    @Test
    public void thumbnail_missing_query() {
        Response response = apiGetResponse("/thumbnail.do");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(400, response.code());
    }

    @Test
    public void thumbnail_fail() {
        Response response = apiGetResponse("/thumbnail.do?f=/sdcard/DCIM/invalid-file.JPG");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(404, response.code());
    }

    @Test
    public void thumbnail_unsupported_file_type() {
        Response response = apiGetResponse("/thumbnail.do?f=/sdcard/DCIM/other-file.txt");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(404, response.code());
    }
}