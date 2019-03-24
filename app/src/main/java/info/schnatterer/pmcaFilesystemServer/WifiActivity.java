package info.schnatterer.pmcaFilesystemServer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

public class WifiActivity extends BaseActivity {

    public enum WifiState {
        DISABLED("Wifi disabled"),
        ENABLING("Enabling wifi..."),
        ENABLED("Wifi enabled"),
        SCANNING("Scanning..."),
        CONNECTING("Connecting..."),
        CONNECTED("Wifi connected");

        private String label;

        WifiState(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private ConnectivityManager connectivityManager;
    protected WifiManager wifiManager;
    private BroadcastReceiver receiver;
    private boolean keepWifiOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onWifiStateChanged();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        f.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(receiver, f);
        onWifiStateChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        if (!keepWifiOn) {
            setWifiEnabled(false);
        }
        keepWifiOn = false;
    }

    public void onWifiStateChanged() {
        if (getWifiState() != WifiState.CONNECTED) {
            startActivity(new Intent(this, ConnectActivity.class));
        }
    }

    public WifiState getWifiState() {
        switch (wifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
                if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                    return WifiState.CONNECTED;
                } else {
                    switch (WifiInfo.getDetailedStateOf(wifiManager.getConnectionInfo().getSupplicantState())) {
                        case SCANNING:
                            return WifiState.SCANNING;
                        case AUTHENTICATING:
                        case CONNECTING:
                        case OBTAINING_IPADDR:
                            return WifiState.CONNECTING;
                        default:
                            return WifiState.ENABLED;
                    }
                }
            case WifiManager.WIFI_STATE_ENABLING:
                return WifiState.ENABLING;
            default:
                return WifiState.DISABLED;
        }
    }

    public void setWifiEnabled(boolean enabled) {
        wifiManager.setWifiEnabled(enabled);
    }

    public void setKeepWifiOn() {
        keepWifiOn = true;
    }
}
