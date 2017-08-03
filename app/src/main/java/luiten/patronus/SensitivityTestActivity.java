package luiten.patronus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by LG on 2017-05-19.
 */

public class SensitivityTestActivity extends Activity implements SensorEventListener {

    double nowAccel, maxAccel;
    SensorManager mSensorManager = null;
    Sensor accSensor = null;

    @Override
    public void onCreate(Bundle savedIntanceState) {
        super.onCreate(savedIntanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.sensitivity_test);

        // 가속도 초기화
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // 적용
        Button button1 = (Button)findViewById(R.id.sensitivetest_btn_save);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Setting)Setting.mContext).sb_sensi.setProgress((int)(maxAccel / 5.8));
                finish();
            }
        });

        // 취소
        Button button2 = (Button)findViewById(R.id.sensitivetest_btn_cancel);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onResume()
    {
        super.onResume();

        // 주기 설명
        // SENSOR_DELAY_UI 갱신에 필요한 정도 주기
        // SENSOR_DELAY_NORMAL 화면 방향 전환 등의 일상적인  주기
        // SENSOR_DELAY_GAME 게임에 적합한 주기
        mSensorManager.registerListener(this, accSensor, mSensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //정확도에 대한 메소드 호출 (사용안함)
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // 센서값 얻어오기
    // 출처: http://h5bak.tistory.com/271
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        float accelX, accelY, accelZ;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelX = event.values[0];
            accelY = event.values[1];
            accelZ = event.values[2];
            nowAccel = (Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2)) - 9.81);

            if (Math.abs(nowAccel) < 0.3)
            {
                nowAccel = 0.0;
            }

            if (nowAccel > maxAccel)
            {
                maxAccel = nowAccel;
                if (maxAccel > 57)
                {
                    maxAccel = 57;
                }
            }
        }

        TextView tv = (TextView)findViewById(R.id.sensitivetest_text_result);
        tv.setText("" + ((int)(maxAccel / 5.8) + 1) * 10);
    }
}
