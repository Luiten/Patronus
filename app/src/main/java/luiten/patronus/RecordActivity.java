package luiten.patronus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;

/**
 * Created by LG on 2017-05-31.
 */


public class RecordActivity extends AppCompatActivity {

    private ExpandableListView log_listview;
    private ArrayList<String> log_date = new ArrayList<String>();
    private ArrayList<ArrayList<Integer>> arrIndex = new ArrayList<ArrayList<Integer>>();
    private ArrayList<ArrayList<String>> mChildTime =  new ArrayList<ArrayList<String>>();
    private ArrayList<ArrayList<String>> mChildDesc =  new ArrayList<ArrayList<String>>();
    //private HashMap<String, ArrayList<String>> log_content = new HashMap<String, ArrayList<String>>();
    private String LogType[] = { "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함",
            "표지판", "졸음 운전", "충돌", "운전 점수", "수동 녹화" };

    List<String[]> csvLogs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("로그");
        setContentView(R.layout.record_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        log_listview = (ExpandableListView)findViewById(R.id.record_exlistview);
        setArrayData();

        log_listview.setAdapter(new LogAdapter(this, log_date, mChildTime, mChildDesc));

        // Listview Group click listener
        log_listview.setOnGroupClickListener(new OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                // Toast.makeText(getApplicationContext(),
                // "Group Clicked " + listDataHeader.get(groupPosition),
                // Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        // Listview Group expanded listener
        log_listview.setOnGroupExpandListener(new OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {

            }
        });

        // Listview Group collasped listener
        log_listview.setOnGroupCollapseListener(new OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {

            }
        });

        // 차일드 클릭 했을 경우 이벤트
        // 출처: http://arabiannight.tistory.com/entry/360
        log_listview.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                ArrayList<Integer> arrTemp = arrIndex.get(groupPosition);
                int position = arrTemp.get(childPosition);

                String[] temp = csvLogs.get(position);
                Intent intent = null;

                switch (Integer.valueOf(temp[2])) {
                    // 자동차간 거리
                    case 1:

                        break;

                    // 끼어들기
                    // 차선 침범
                    // 신호 위반
                    // 신호 주시 안함
                    // 표지판
                    // 졸음 운전
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        intent = new Intent(getApplicationContext(), RecordSignal.class);
                        intent.putExtra("Logs", temp);
                        startActivity(intent);
                        break;

                    // 충돌
                    case 8:
                        intent = new Intent(getApplicationContext(), RecordCrash.class);
                        intent.putExtra("Logs", temp);
                        startActivity(intent);
                        break;

                    // 운전 점수 - 현재는 운전 끝을 의미
                    case 9:
                        intent = new Intent(getApplicationContext(), RecordDrive.class);
                        startActivity(intent);
                        break;

                    // 수동녹화
                    case 10:
                        intent = new Intent(getApplicationContext(), RecordCrash.class);
                        intent.putExtra("Logs", temp);
                        startActivity(intent);
                        break;

                }

                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //------------------------------------------------------------------------------------------------//
    // 로그를 읽어서 뷰에 표시
    //------------------------------------------------------------------------------------------------//
    private void setArrayData() {
        ArrayList<String> arrChildTime = new ArrayList<String>();
        ArrayList<String> arrChildDesc = new ArrayList<String>();

        // 경로 설정
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/";
        File file = new File(dirPath);

        // 일치하는 폴더가 없으면 생성
        if (!file.exists()) {
            file.mkdirs();
        }

        String csvName = "logs.csv";
        String strTemp = "";
        ArrayList<Integer> arrTemp = new ArrayList<Integer>();
        int idx = 0;

        try {
            InputStream inputStream = new FileInputStream(dirPath + csvName);
            CSVReader csv = new CSVReader(inputStream);

            csvLogs = csv.read();

            for(String[] ListData : csvLogs) {
                String[] strCSV = Arrays.copyOf(ListData, ListData.length);
                strCSV[2] = LogType[Integer.valueOf(ListData[2]) - 1];

                if (strTemp == "") {
                    strTemp = strCSV[0];
                }

                // ** 계속 첫번째 문자열이 11개가 나오고 두번째부터 10개 나오는 이유: 텍스트 파일 맨 앞은 텍스트 인코딩 정보 때문에.
                // 날짜가 달라지면 새로운 확장 리스트 추가
                if (!strTemp.equals(strCSV[0])) {
                    log_date.add(strTemp);
                    mChildTime.add(arrChildTime);
                    mChildDesc.add(arrChildDesc);
                    arrIndex.add(arrTemp);
                    arrChildTime = new ArrayList<String>();
                    arrChildDesc = new ArrayList<String>();
                    arrTemp = new ArrayList<Integer>();
                    strTemp = strCSV[0];
                }

                arrTemp.add(idx++);
                arrChildTime.add(strCSV[1]);
                arrChildDesc.add(strCSV[2]);
            }

            log_date.add(strTemp);
            mChildTime.add(arrChildTime);
            mChildDesc.add(arrChildDesc);
            arrIndex.add(arrTemp);
        }
        catch (IOException e) {
        }

    }
}
