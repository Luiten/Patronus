package luiten.patronus;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.SeekBar;
import android.widget.TextView;

import android.content.SharedPreferences;


/**
 * Created by LG on 2017-05-22.
 */

public class AlarmActivity extends SettingActivity {

    @Override
    public void onCreate(Bundle savedIntanceState) {
        super.onCreate(savedIntanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.alarm_main);

        CheckBox chkLane = (CheckBox) findViewById(R.id.alarm_chk_lane);
        CheckBox chkDistance = (CheckBox) findViewById(R.id.alarm_chk_distance);
        CheckBox chkSignal = (CheckBox) findViewById(R.id.alarm_chk_signal);
        CheckBox chkSleep = (CheckBox) findViewById(R.id.alarm_chk_sleep);
        CheckBox chkSign = (CheckBox) findViewById(R.id.alarm_chk_sign);

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        chkLane.setChecked(settings.getBoolean("lane", true));
        chkDistance.setChecked(settings.getBoolean("distance", true));
        chkSignal.setChecked(settings.getBoolean("signal", true));
        chkSleep.setChecked(settings.getBoolean("sleep", true));
        chkSign.setChecked(settings.getBoolean("sign", true));
    }

    @Override
    public void onDestroy() {
        CheckBox chkLane = (CheckBox) findViewById(R.id.alarm_chk_lane);
        CheckBox chkDistance = (CheckBox) findViewById(R.id.alarm_chk_distance);
        CheckBox chkSignal = (CheckBox) findViewById(R.id.alarm_chk_signal);
        CheckBox chkSleep = (CheckBox) findViewById(R.id.alarm_chk_sleep);
        CheckBox chkSign = (CheckBox) findViewById(R.id.alarm_chk_sign);

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("lane", chkLane.isChecked()); // 데이터 저장
        editor.putBoolean("distance", chkDistance.isChecked()); // 데이터 저장
        editor.putBoolean("signal", chkSignal.isChecked()); // 데이터 저장
        editor.putBoolean("sleep", chkSleep.isChecked()); // 데이터 저장
        editor.putBoolean("sign", chkSign.isChecked()); // 데이터 저장
        editor.commit(); // 완료한다.

        // Manager 클래스에 적용
        SetSettings(1, chkLane.isChecked() ? 1 : 0);
        SetSettings(2, chkDistance.isChecked() ? 1 : 0);
        SetSettings(3, chkSignal.isChecked() ? 1 : 0);
        SetSettings(4, chkSleep.isChecked() ? 1 : 0);
        SetSettings(5, chkSign.isChecked() ? 1 : 0);

        super.onDestroy();
    }
}

