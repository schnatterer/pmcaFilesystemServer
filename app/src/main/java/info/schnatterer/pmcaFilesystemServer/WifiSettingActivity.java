package info.schnatterer.pmcaFilesystemServer;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;

public class WifiSettingActivity extends BaseActivity {
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startActivityForResult(new Intent("com.sony.scalar.app.wifisettings.WifiSettings"), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wifiManager.setWifiEnabled(false);
    }
}
