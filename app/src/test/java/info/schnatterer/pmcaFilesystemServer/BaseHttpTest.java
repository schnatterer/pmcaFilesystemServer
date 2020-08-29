package info.schnatterer.pmcaFilesystemServer;

import android.os.Build;
import android.os.Environment;
import androidx.test.core.app.ApplicationProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Before;
import org.robolectric.annotation.Config;

import java.io.*;

@Config(sdk = Build.VERSION_CODES.M)
public class BaseHttpTest {

    private OkHttpClient client = new OkHttpClient();

    protected String apiGet(String url) throws IOException {
        Response response = apiGetResponse(url);
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected status " + response.code() + ": " + response.body().string());
        }
    }

    protected Response apiGetResponse(String url) throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:" + httpServer.getListeningPort() + url)
                .build();
        return client.newCall(request).execute();
    }

    private HttpServer httpServer;

    @Before
    public void startServer() throws IOException {
        httpServer = new HttpServer(ApplicationProvider.getApplicationContext());
        httpServer.start();
    }

    @After
    public void stopServer() {
        httpServer.stop();
        httpServer = null;
    }

    protected String setupFile(TestFile testFile) throws IOException {
        File srcFile = new File( this.getClass().getClassLoader().getResource(testFile.getFilename()).getFile());
        File destFile = new File(Environment.getExternalStorageDirectory(), testFile.getFilename());
        copy(srcFile, destFile);
        return getAbsoluteFilePath(testFile);
    }

    protected String getAbsoluteFilePath(TestFile testFile) {
        return new File(Environment.getExternalStorageDirectory(), testFile.getFilename()).getAbsolutePath();
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
    
    public enum TestFile {
        JPG("test_image.JPG"),
        RAW("test_raw.ARW"),
        VIDEO("test_video.MTS");

        private final String filename;

        TestFile(String filename) {
            this.filename=filename;
        }

        public String getFilename() {
            return filename;
        }
    }
}