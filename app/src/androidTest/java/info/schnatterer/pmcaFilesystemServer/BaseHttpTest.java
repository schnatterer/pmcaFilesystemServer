package info.schnatterer.pmcaFilesystemServer;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BaseHttpTest {

    private OkHttpClient client = new OkHttpClient();

    protected String apiGet(String url) {
        Request request = new Request.Builder()
                .url("http://localhost:" + httpServer.getListeningPort() + url)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if(response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new IOException("Unexpected status " + response.code() + ": " + response.body().string());
            }
        } catch (Exception e) {
            Log.e("API", "GET " + url + " FAILED!\n" + response, e);
        }
        return null;
    }

    protected Response apiGetResponse(String url) {
        Request request = new Request.Builder()
                .url("http://localhost:" + httpServer.getListeningPort() + url)
                .build();
        try {
            return client.newCall(request).execute();
        } catch (Exception ignored) {
        }
        return null;
    }

    protected String getFileOfType(String type) {
        String body = apiGet("/api/list.do?type=" + type);

        try {
            JSONArray json = new JSONArray(body);
            return json.getJSONObject(0).getString("file");
        } catch (JSONException ignored) {
        }

        return null;
    }

    private HttpServer httpServer;

    @Before
    public void startServer() throws IOException {
        Context context = InstrumentationRegistry.getTargetContext();
        httpServer = new HttpServer(context);
        httpServer.start();
    }

    @After
    public void stopServer() {
        httpServer.stop();
        httpServer = null;
    }
}