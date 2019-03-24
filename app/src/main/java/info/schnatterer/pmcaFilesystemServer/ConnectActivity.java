package info.schnatterer.pmcaFilesystemServer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ConnectActivity extends MainActivity {
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_activity);
        textView = (TextView) findViewById(R.id.text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setWifiEnabled(true);
    }

    @Override
    public void onWifiStateChanged() {
        WifiState state =  getWifiState();
        textView.setText(state.toString());
        if (state == WifiState.CONNECTED) {
            setKeepWifiOn();
            finish();
        }
    }

    public void onSettingsButtonClicked(View view) {
        String action = "com.sony.scalar.app.wifisettings.WifiSettings";
        startActivity(new Intent(action));
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        finish();
    }
}
