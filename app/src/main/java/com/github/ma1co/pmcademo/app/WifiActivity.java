package com.github.ma1co.pmcademo.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.TextView;
import info.schnatterer.pmcaFilesystemServer.R;
import java.io.IOException;

public class WifiActivity extends BaseActivity {
    private TextView textView;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiStateReceiver;
    private BroadcastReceiver supplicantStateReceiver;
    private BroadcastReceiver networkStateReceiver;
    private HttpServer httpServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);

        textView = (TextView) findViewById(R.id.logView);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                wifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
            }
        };

        supplicantStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                networkStateChanged(WifiInfo.getDetailedStateOf((SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
            }
        };

        networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                networkStateChanged(((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState());
            }
        };

        httpServer = new HttpServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        registerReceiver(supplicantStateReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
        registerReceiver(networkStateReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        wifiManager.setWifiEnabled(true);
        try {
            httpServer.start();
        } catch (IOException e) {
            Logger.error("Failed to start HTTP Server: " + e.getMessage());
        }
        setAutoPowerOffMode(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiStateReceiver);
        unregisterReceiver(supplicantStateReceiver);
        unregisterReceiver(networkStateReceiver);
        wifiManager.setWifiEnabled(false);
        httpServer.stop();
        setAutoPowerOffMode(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void wifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                log("Enabling wifi");
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                log("Wifi enabled");
                break;
        }
    }

    protected void networkStateChanged(NetworkInfo.DetailedState state) {
        String ssid = wifiManager.getConnectionInfo().getSSID();
        switch (state) {
            case CONNECTING:
                if (ssid != null)
                    log(ssid + ": Connecting");
                break;
            case AUTHENTICATING:
                log(ssid + ": Authenticating");
                break;
            case OBTAINING_IPADDR:
                log(ssid + ": Obtaining IP");
                break;
            case CONNECTED:
                wifiConnected();
                break;
            case DISCONNECTED:
                log("Disconnected");
                break;
            case FAILED:
                log("Connection failed");
                break;
        }
    }

    protected void wifiConnected() {
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = info.getSSID();
        String ip = Formatter.formatIpAddress(info.getIpAddress());
        log(ssid + ": Connected. Server URL: http://" + ip + ":" + HttpServer.PORT + "/");
    }

    protected void log(String msg) {
        textView.setText(msg);
    }
}
