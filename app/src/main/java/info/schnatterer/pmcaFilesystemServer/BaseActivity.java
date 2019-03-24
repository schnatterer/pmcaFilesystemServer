package info.schnatterer.pmcaFilesystemServer;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;

import com.sony.scalar.sysutil.ScalarInput;

public class BaseActivity extends Activity {

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (event.getScanCode()) {
            case ScalarInput.ISV_KEY_DELETE:
            case ScalarInput.ISV_KEY_SK2:
            case ScalarInput.ISV_KEY_MENU:
                return onDeleteKeyUp();
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    protected boolean onDeleteKeyUp() {
        onBackPressed();
        return true;
    }

    protected void setAutoPowerOffMode(boolean enable) {
        String mode = enable ? "APO/NORMAL" : "APO/NO";// or "APO/SPECIAL" ?
        Intent intent = new Intent();
        intent.setAction("com.android.server.DAConnectionManagerService.apo");
        intent.putExtra("apo_info", mode);
        sendBroadcast(intent);
    }
}
