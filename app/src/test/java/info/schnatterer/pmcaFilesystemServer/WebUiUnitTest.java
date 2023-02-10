package info.schnatterer.pmcaFilesystemServer;

import junit.framework.Assert;
import okhttp3.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WebUiUnitTest extends BaseHttpTest{

    @Test
    public void get_index() throws Exception {
        String body = apiGet("/");
        Assert.assertTrue(body.startsWith("<!DOCTYPE html>"));
    }

    @Test
    public void get_404() throws Exception {
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
    public void get_css() throws Exception {
        String body = apiGet("/assets/css/styles.css");
        Assert.assertNotNull(body);
    }

    @Test
    public void get_js() throws Exception {
        String body = apiGet("/assets/js/main.js");
        Assert.assertNotNull(body);
    }

    @Test
    public void get_image() throws Exception {
        String body = apiGet("/assets/img/video-fallback.jpg");
        Assert.assertNotNull(body);
    }
}
