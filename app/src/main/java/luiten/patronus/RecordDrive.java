package luiten.patronus;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

/**
 * Created by power on 2017-06-12.
 */

public class RecordDrive extends SettingActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_drive);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
}
