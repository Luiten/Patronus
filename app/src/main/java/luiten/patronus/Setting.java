package luiten.patronus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LG on 2017-07-06.
 */

public class Setting extends AppCompatActivity {

    private ListView alarmlistview = null;
    private AlarmAdapter alarmadapter = null;
    private ListView warninglistview = null;
    private WarningAdapter warningadapter = null;
    private ListView capture_lengthview = null;
    private AlarmAdapter capture_lengthadapter = null;
    private ListView resolutionlistview = null;
    private ResolutionAdapter resolutionadapter = null;
    private ListView sensitivitylistview = null;
    private SensitivityAdapter sensitivityadapter = null;
    private ListView standardlistview = null;
    private StandardAdapter standardadapter = null;
    private ListView recordlistview = null;
    private RecordAdapter recordadapter = null;

    ArrayList<int[]> strResolution = new ArrayList<int[]>();

    public static Context mContext;
    SeekBar sb_sensi, sb_cap;

    public native int SetSettings(int type, double value);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("설정");
        setContentView(R.layout.setting_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mContext = this;

        // 세팅 읽기
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);

        // 캡처 길이 슬라이드 바
        final TextView tv_cap = (TextView)findViewById(R.id.set_text_capscore);
        sb_cap = (SeekBar)findViewById(R.id.set_seekbar_caplength);

        sb_cap.setProgress(settings.getInt("capture", 5));
        tv_cap.setText("" + ((sb_cap.getProgress() + 1) * 30) + "초");

        sb_cap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            public void onStartTrackingTouch(SeekBar seekBar){

            }
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_cap.setText("" + ((progress + 1) * 30) + "초");
            }
        });

        // 민감도 슬라이드 바
        final TextView tv_sensi = (TextView)findViewById(R.id.set_text_sensiscore);
        sb_sensi = (SeekBar)findViewById(R.id.set_seekbar_sensitivity);

        sb_sensi.setProgress(settings.getInt("sensitivity", 4));
        tv_sensi.setText("" + ((sb_sensi.getProgress() + 1) * 10) + "%");

        sb_sensi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            public void onStartTrackingTouch(SeekBar seekBar){

            }
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_sensi.setText("" + ((progress + 1) * 10) + "%");
            }
        });

        alarmlistview = (ListView)findViewById(R.id.set_listview_alarm);
        alarmadapter = new AlarmAdapter(this);
        alarmlistview.setAdapter(alarmadapter);

        warninglistview = (ListView)findViewById(R.id.set_listview_warning);
        warningadapter = new WarningAdapter(this);
        warninglistview.setAdapter(warningadapter);
        capture_lengthview = (ListView)findViewById(R.id.set_listview_caplength);
        capture_lengthadapter = new AlarmAdapter(this);
        capture_lengthview.setAdapter(capture_lengthadapter);

        resolutionlistview = (ListView)findViewById(R.id.set_listview_resolution);
        resolutionadapter = new ResolutionAdapter(this);
        resolutionlistview.setAdapter(resolutionadapter);

        sensitivitylistview = (ListView)findViewById(R.id.set_listview_sensitivity);
        sensitivityadapter = new SensitivityAdapter(this);
        sensitivitylistview.setAdapter(sensitivityadapter);

        standardlistview = (ListView)findViewById(R.id.set_listview_standard);
        standardadapter = new StandardAdapter(this);
        standardlistview.setAdapter(standardadapter);

        recordlistview = (ListView)findViewById(R.id.set_listview_record);
        recordadapter = new RecordAdapter(this);
        recordlistview.setAdapter(recordadapter);

        alarmadapter.addItem("차선", "주행 중 차선을 밟고 있을 경우 경고합니다.", settings.getBoolean("lane" , true), true);
        alarmadapter.addItem("차간 거리", "앞 차와의 거리가 너무 가깝거나 옆 차량이 내 차선에 끼어들 경우 경고합니다.", settings.getBoolean("distance", true), true);
        alarmadapter.addItem("신호등", "신호 위반을 하거나 신호 대기 후 주행 신호에 출발하지 않을 경우 경고합니다.", settings.getBoolean("signal", true), true);
        alarmadapter.addItem("졸음 운전", "주행 중 졸음 운전을 하거나 운전에 집중하지 않을 경우 경고합니다.", settings.getBoolean("sleep", true), true);
        //alarmadapter.addItem("표지판", "표지판 내용을 알려줍니다.", settings.getBoolean("sign", true), true);

        warningadapter.addItem("소리 (TTS)", "음성과 이미지를 사용하여 경고합니다.");
        warningadapter.addItem("진동", "진동과 이미지를 사용하여 경고합니다.");
        warningadapter.addItem("무음", "이미지만 사용하여 경고합니다.");
        warningadapter.SetSelectNumber(settings.getInt("sound", 0));

        //--------------------------------------------------------------------------//
        // 듀얼 카메라 지원 확인
        //--------------------------------------------------------------------------//
        SharedPreferences.Editor editor = settings.edit();
        int nDualcameraSupport = settings.getInt("dualcamera", 0); // 0 = 초기화 안됨, 1 = 사용 불가, 2 = 사용 가능

        if (nDualcameraSupport == 0) {
            Camera mBackCamera = getCameraInstance(0);
            Camera mFrontCamera = getCameraInstance(1);

            if (mBackCamera == null || mFrontCamera == null) {
                //Toast.makeText(getApplicationContext(), "듀얼 카메라를 지원하지 않습니다.", Toast.LENGTH_LONG).show();

                nDualcameraSupport = 1;
            } else {
                nDualcameraSupport = 2;
            }

            editor.putInt("dualcamera", nDualcameraSupport);
            editor.apply(); // 완료한다.

            if (mBackCamera != null) mBackCamera.release();
            if (mFrontCamera != null) mFrontCamera.release();
        }

        if (nDualcameraSupport == 2) {
            capture_lengthadapter.addItem("전면 카메라", "운전자 방향의 카메라를 동작시킵니다.", settings.getBoolean("frontcamera", false), true);
        } else {
            //Toast.makeText(getApplicationContext(), "듀얼 카메라를 지원하지 않습니다.", Toast.LENGTH_LONG).show();

            capture_lengthadapter.addItem("전면 카메라", "운전자 방향의 카메라를 동작시킵니다.\n듀얼 카메라를 지원하지 않아 기능을 사용할 수 없습니다.", false, false);
        }

        capture_lengthadapter.addItem("후면 카메라", "도로 방향의 카메라를 동작시킵니다.", settings.getBoolean("backcamera", true), true);

        // 지원 해상도 알아내기
        Camera camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> SupporetdSizes =  parameters.getSupportedPreviewSizes();

        for(Camera.Size camSize : SupporetdSizes) {
            float raito = (float) camSize.width / camSize.height;
            // 비율(16:9 +-20%)과 일정 해상도(400)이상 만족시 표시 + 끝자리가 0일 경우 표시
            if (raito >= 1.77 * 0.8 && raito <= 1.77 * 1.2 && camSize.width > 400 && camSize.width % 10 == 0)
            {
                resolutionadapter.addItem(camSize.width + "×" + camSize.height);
                int temp[] = { camSize.width, camSize.height };
                strResolution.add(temp);
            }
        }
        camera.release();

        /*resolutionadapter.addItem("1920 × 1080");
        resolutionadapter.addItem("1280 × 720");
        resolutionadapter.addItem("800 × 600");
        resolutionadapter.addItem("640 × 480");*/
        //resolutionadapter.SetSelectNumber(settings.getInt("resolution", 1));
        resolutionadapter.SetSelectNumber(settings.getInt("resolution", 0));

        sensitivityadapter.addItem("테스트 하기");

        standardadapter.addItem("현재 기준점 보기");
        standardadapter.addItem("다시 기준점 찍기");

        recordadapter.addItem("운전 점수");
        recordadapter.addItem("로그");
        recordadapter.addItem("수동 동영상");

        setListViewHeightBasedOnChildren(alarmlistview);
        setListViewHeightBasedOnChildren(warninglistview);
        setListViewHeightBasedOnChildren(capture_lengthview);
        setListViewHeightBasedOnChildren(resolutionlistview);
        setListViewHeightBasedOnChildren(sensitivitylistview);
        setListViewHeightBasedOnChildren(standardlistview);
        setListViewHeightBasedOnChildren(recordlistview);

        sensitivitylistview.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0 :
                        Intent intent = new Intent(getApplicationContext(), SensitivityTestActivity.class);
                        startActivity(intent);
                        break;
                }
            }
        });

        standardlistview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0 :
                        Intent intent = new Intent(getApplicationContext(), StandardActivity.class);
                        startActivity(intent);
                        break;
                    case 1 :
                        intent = new Intent(getApplicationContext(), PointActivity.class);
                        intent.putExtra("resolutionwidth", strResolution.get(resolutionadapter.GetChecked())[0]);
                        intent.putExtra("resolutionheight", strResolution.get(resolutionadapter.GetChecked())[1]);
                        startActivity(intent);
                        break;
                }
            }

        }); //기준점 리스트뷰에서 액티비티 전환하는 함수

        recordlistview.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0 :
                        Intent intent = new Intent(getApplicationContext(), RecordDrive
                                .class);
                        startActivity(intent);
                        break;
                    case 1 :
                        intent = new Intent(getApplicationContext(), RecordActivity.class);
                        startActivity(intent);
                        break;
                    case 2 :
                        intent = new Intent(getApplicationContext(), Manual_Recording.class);
                        startActivity(intent);
                        break;
                }
            }
        }); //기록 리스트뷰에서 액티비티 전환하는 함수
    }

    public static void setListViewHeightBasedOnChildren(ListView listView){
        ListAdapter listAdapter = listView.getAdapter();
        if(listAdapter == null){
            return;
        }

        int totalHeight = 0;
        for(int i = 0; i<listAdapter.getCount(); i++){
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
    //리스트 뷰의 초기화 및 아이템을 갱신한 후 리스트뷰의 height을 계산하여 layoutparms에 맞춰 설정해주는 함수
    //스크롤뷰안에 리스트뷰가 여러개 있으면 이 함수를 써줘야 함

    @Override
    public void onDestroy() {
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        sb_cap = (SeekBar)findViewById(R.id.set_seekbar_caplength);
        editor.putInt("capture", sb_cap.getProgress());

        sb_sensi = (SeekBar)findViewById(R.id.set_seekbar_sensitivity);
        editor.putInt("sensitivity", sb_sensi.getProgress());

        editor.putBoolean("lane", alarmadapter.isChecked(0));
        editor.putBoolean("distance", alarmadapter.isChecked(1));
        editor.putBoolean("signal", alarmadapter.isChecked(2));
        editor.putBoolean("sleep", alarmadapter.isChecked(3));
        //editor.putBoolean("sign", alarmadapter.isChecked(4));

        editor.putInt("sound", warningadapter.GetChecked()); // 데이터 저장

        editor.putBoolean("frontcamera", capture_lengthadapter.isChecked(0));
        editor.putBoolean("backcamera", capture_lengthadapter.isChecked(1));

        editor.putInt("resolution", resolutionadapter.GetChecked()); // 데이터 저장
        editor.putInt("resolutionwidth", strResolution.get(resolutionadapter.GetChecked())[0]);
        editor.putInt("resolutionheight", strResolution.get(resolutionadapter.GetChecked())[1]);
        editor.apply(); // 완료한다.

        // Manager 클래스에 적용
        SetSettings(1, alarmadapter.isChecked(0) ? 1 : 0); // Lane
        SetSettings(2, alarmadapter.isChecked(1) ? 1 : 0); // Distance
        SetSettings(3, alarmadapter.isChecked(2) ? 1 : 0); // Signal Light
        SetSettings(4, alarmadapter.isChecked(3) ? 1 : 0); // Sleep
        //SetSettings(5, alarmadapter.isChecked(4) ? 1 : 0); // Sign
        SetSettings(6, sb_sensi.getProgress());
        SetSettings(7, resolutionadapter.GetChecked()); // Video Size
        SetSettings(8, strResolution.get(resolutionadapter.GetChecked())[0]); // Video Width
        SetSettings(9, strResolution.get(resolutionadapter.GetChecked())[1]); // Video Height
        SetSettings(10, (sb_cap.getProgress() + 1) * 30); // Record Length

        super.onDestroy();
    }

    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
            Log.d("Splash", "Got camera " + cameraId);
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e("Splash", "Camera " + cameraId + " not available! " + e.toString());
        }
        return c; // returns null if camera is unavailable
    }
}
