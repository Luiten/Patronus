package luiten.patronus;

import android.content.Context;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import android.annotation.TargetApi;

import java.util.Locale;

/**
 * Created by power on 2017-06-12.
 */

public class Warning extends MainActivity {
    //TODO : 추가 :: TTS, 진동
    TextToSpeech tts;
    Vibrator vibrator;

    // "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함", "표지판", "졸음 운전", "충돌"
    private int WarningTime[] = { 500, 2000, 500, 300, 1000, 0, 500, 0 };
    private int WarningPeroid[] = { 1, 1, 3, 2, 3, 0, 5, 0 };
    private String WarningDesc[] = { "앞차와 너무 가깝습니다", " 자동차를 주의하세요", "을 침범 중입니다", " 신호를 위반했습니다",
            " 신호를 확인하세요", "입니다", "졸음운전 중 경고입니다", "충돌이 감지됐습니다", "" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO : TTS
        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN); // 한국어
                }
            }
        });

        //TODO : 진동
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    //TODO : TTS
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId=this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    public void TTS(int nType, String strAddMsg)
    {
        ttsGreater21(strAddMsg + WarningDesc[nType]);
    }

    public void Vibrate(int nType)
    {
        for (int i = 0; i < WarningPeroid[nType]; i++) {
            // ms 단위
            vibrator.vibrate(WarningTime[nType]);
        }
    }
}
