package info.schnatterer.pmcaFilesystemServer;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.Response;

@RunWith(AndroidJUnit4.class)
public class WebUiUnitTest extends BaseHttpTest{

    @Test
    public void get_index() {
        String body = apiGet("/");
        Assert.assertTrue(body.startsWith("<!DOCTYPE html>"));
    }

    @Test
    public void get_404() {
        Response response = apiGetResponse("/error.html");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(404, response.code());

        response = apiGetResponse("/assets/error.html");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(404, response.code());
    }

    @Test
    public void get_css() {
        String body = apiGet("/assets/css/styles.css");
        Assert.assertNotNull(body);
    }

    @Test
    public void get_js() {
        String body = apiGet("/assets/js/loader.js");
        Assert.assertNotNull(body);
    }

    @Test
    public void get_image() {
        String body = apiGet("/assets/img/video-fallback.jpg");
        Assert.assertNotNull(body);
    }
}