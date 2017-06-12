package luiten.patronus;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import android.content.SharedPreferences;

/**
 * Created by LG on 2017-05-22.
 */

public class CaptureActivity extends SettingActivity {
    @Override
    public void onCreate(Bundle savedIntanceState) {
        super.onCreate(savedIntanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.capture_main);

        final TextView tv = (TextView)findViewById(R.id.cap_text_result);
        SeekBar sb = (SeekBar)findViewById(R.id.cap_seekbar);
        CheckBox chkFront = (CheckBox) findViewById(R.id.cap_chk_frontcamera);
        CheckBox chkBack = (CheckBox) findViewById(R.id.cap_chk_backcamera);

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        sb.setProgress(settings.getInt("capture", 5));
        tv.setText("캡처 길이: " + (sb.getProgress() + 1) * 30 + "초");

        chkFront.setChecked(settings.getBoolean("frontcamera", false));
        chkBack.setChecked(settings.getBoolean("backcamera", true));

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            public void onStartTrackingTouch(SeekBar seekBar){

            }
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv.setText("캡처 길이: " + ((progress + 1) * 30) + "초");
            }
        });
    }


    @Override
    public void onDestroy() {
        SeekBar sb = (SeekBar)findViewById(R.id.cap_seekbar);
        CheckBox chkFront = (CheckBox) findViewById(R.id.cap_chk_frontcamera);
        CheckBox chkBack = (CheckBox) findViewById(R.id.cap_chk_backcamera);

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("capture", sb.getProgress()); // 데이터 저장
        editor.putBoolean("frontcamera", chkFront.isChecked());
        editor.putBoolean("backcamera", chkBack.isChecked());
        editor.commit(); // 완료한다.

        super.onDestroy();
    }
}