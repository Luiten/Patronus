package luiten.patronus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by LG on 2017-05-19.
 */

public class SensitivityActivity extends SettingActivity {
    public static Context mContext;

    SeekBar sb;

    @Override
    public void onCreate(Bundle savedIntanceState) {
        super.onCreate(savedIntanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.sensitivity_main);

        mContext = this;

        final TextView tv = (TextView)findViewById(R.id.sensitivity_text_result);
        sb = (SeekBar)findViewById(R.id.sensitivity_seekbar);

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        sb.setProgress(settings.getInt("sensitivity", 4));
        tv.setText("현재 민감도: " + ((sb.getProgress() + 1) * 10));

        // 민감도 테스트
        Button button = (Button)findViewById(R.id.sensitivity_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SensitivityTestActivity.class);
                startActivity(intent);
            }
        });

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            public void onStartTrackingTouch(SeekBar seekBar){

            }
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv.setText("현재 민감도: " + ((progress + 1) * 10));
            }
        });
    }

    @Override
    public void onDestroy() {
        sb = (SeekBar)findViewById(R.id.sensitivity_seekbar);

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("sensitivity", sb.getProgress()); // 데이터 저장
        SetSettings(6, settings.getInt("sensitivity", 4));
        editor.commit(); // 완료한다.

        super.onDestroy();
    }
}
