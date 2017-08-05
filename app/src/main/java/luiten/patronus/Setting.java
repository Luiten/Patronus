package luiten.patronus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompatBase;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LG on 2017-07-06.
 */

public class Setting extends AppCompatActivity {

    private ListView alarmlistview = null;
    private AlarmAdapter alarmadapter = null;
    private ListView capture_lengthview = null;
    private Capture_lengthAdapter capture_lengthadapter = null;
    private ListView resolutionlistview = null;
    private ResolutionAdapter resolutionadapter = null;
    private ListView sensitivitylistview = null;
    private SensitivityAdapter sensitivityadapter = null;
    private ListView standardlistview = null;
    private StandardAdapter standardadapter = null;
    private ListView recordlistview = null;
    private RecordAdapter recordadapter = null;

    public static Context mContext;
    SeekBar sb_sensi, sb_cap;

    public native int SetSettings(int type, double value);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("설정");
        setContentView(R.layout.setting);

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

        capture_lengthview = (ListView)findViewById(R.id.set_listview_caplength);
        capture_lengthadapter = new Capture_lengthAdapter(this);
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

        alarmadapter.addItem("차선", "차선을 넘었을 경우", settings.getBoolean("lane" , true));
        alarmadapter.addItem("앞차간 거리", "앞차간 거리가 가까울 경우", settings.getBoolean("distance", true));
        alarmadapter.addItem("신호 위반", "신호 위반일 경우", settings.getBoolean("signal", true));
        alarmadapter.addItem("졸음 운전", "운전자가 졸음을 감지할 경우", settings.getBoolean("sleep", true));
        alarmadapter.addItem("표지판", "표지판을 발견했을 경우", settings.getBoolean("sign", true));

        capture_lengthadapter.addItem("전면 카메라", settings.getBoolean("frontcamera", false));
        capture_lengthadapter.addItem("후면 카메라", settings.getBoolean("backcamera", true));

        resolutionadapter.addItem("1920×1080");
        resolutionadapter.addItem("1280×720");
        resolutionadapter.addItem("800×600");
        resolutionadapter.addItem("640×480");
        resolutionadapter.SetSelectNumber(settings.getInt("resolution", 1));

        sensitivityadapter.addItem("테스트 하기");

        standardadapter.addItem("현재 기준점 보기");
        standardadapter.addItem("다시 기준점 찍기");

        recordadapter.addItem("운전 결과");
        recordadapter.addItem("로그");

        setListViewHeightBasedOnChildren(alarmlistview);
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
                        intent.putExtra("resolution", resolutionadapter.GetChecked());
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
        editor.putBoolean("sign", alarmadapter.isChecked(4));

        editor.putBoolean("frontcamera", capture_lengthadapter.isChecked(0));
        editor.putBoolean("backcamera", capture_lengthadapter.isChecked(1));

        editor.putInt("resolution", resolutionadapter.GetChecked()); // 데이터 저장
        editor.commit(); // 완료한다.

        // Manager 클래스에 적용
        SetSettings(1, alarmadapter.isChecked(0) ? 1 : 0); // Lane
        SetSettings(2, alarmadapter.isChecked(1) ? 1 : 0); // Distance
        SetSettings(3, alarmadapter.isChecked(2) ? 1 : 0); // Signal Light
        SetSettings(4, alarmadapter.isChecked(3) ? 1 : 0); // Sleep
        SetSettings(5, alarmadapter.isChecked(4) ? 1 : 0); // Sign
        SetSettings(6, sb_sensi.getProgress());
        SetSettings(7, resolutionadapter.GetChecked()); // Video Size

        super.onDestroy();
    }
}
