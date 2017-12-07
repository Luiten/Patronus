package luiten.patronus;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by LG on 2017-06-05.
 */

public class Manual_Recording extends AppCompatActivity {

    private String LogType[] = { "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함",
            "표지판", "졸음 운전", "충돌", "운전 점수", "수동 녹화" };

    private ListView RecordingListView;
    private Manual_RecordingAdapter manual_recordingAdapter;

    private ArrayList<ArrayList<String>> mLogData = new ArrayList<ArrayList<String>>();
    private ArrayList<Integer> arrIndex = new ArrayList<Integer>();
    List<String[]> csvLogs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().setTitle("동영상");
        setContentView(R.layout.manual_recording);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        RecordingListView = (ListView)findViewById(R.id.manual_recording);
        manual_recordingAdapter = new Manual_RecordingAdapter(this);
        RecordingListView.setAdapter(manual_recordingAdapter);

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/video/";

        // csv 읽기
        setArrayData();

        for(ArrayList<String> ListData : mLogData) {
            // 파일경로와 파일이름 추가
            manual_recordingAdapter.addItem(dirPath + ListData.get(1), ListData.get(1), LogType[Integer.valueOf(ListData.get(0)) - 1]);
        }

        RecordingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), RecordCrash.class);
                intent.putExtra("Logs", csvLogs.get(arrIndex.get(position)));
                startActivity(intent);
            }
        });

        registerForContextMenu(RecordingListView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, 0, 1, "삭제");
        menu.add(0, 1, 1, "보기");
    } //ContextMenu 항목 추가 란!!~~~~~~

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListView.AdapterContextMenuInfo info = (ListView.AdapterContextMenuInfo) item.getMenuInfo();

        switch(item.getItemId()) {
            case 0:
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(Manual_Recording.this);
                alert_confirm.setMessage("해당 로그를 삭제 하시겠습니까?").setCancelable(false).setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getApplicationContext(), "로그가 삭제되었습니다.", Toast.LENGTH_LONG).show();
                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });
                AlertDialog alert = alert_confirm.create();
                alert.show();
                break; //자식 포지션일때 0번 포지션에 있는 아이템을 눌렀을 경우

            case 1:
                Intent intent = new Intent(getApplicationContext(), RecordCrash.class);
                intent.putExtra("Logs", csvLogs.get(arrIndex.get(info.position)));
                startActivity(intent);
                break; //자식 포지션일때 1번 포지션에 있는 아이템을 눌렀을 경우
            }

        return super.onContextItemSelected(item);
    }// 해당 ContextMenu에 아이템들을 각각 클릭햇을때 나오는 함수설정

    //------------------------------------------------------------------------------------------------//
    // 로그 읽어오기
    //------------------------------------------------------------------------------------------------//
    private void setArrayData() {
        ArrayList<String> arrLogLine = new ArrayList<String>();

        // 경로 설정
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/";
        File file = new File(dirPath);

        // 일치하는 폴더가 없으면 생성
        if (!file.exists()) {
            file.mkdirs();
        }

        String csvName = "logs.csv";
        int idx = 0;

        try {
            InputStream inputStream = new FileInputStream(dirPath + csvName);
            CSVReader csv = new CSVReader(inputStream);

            csvLogs = csv.read();

            for(String[] ListData : csvLogs) {
                String[] strCSV = Arrays.copyOf(ListData, ListData.length);

                // 충돌과 수동 녹화 로그만 읽고 ""가 아닌 경우
                if ((Integer.valueOf(ListData[2]) == 8 || Integer.valueOf(ListData[2]) == 10) && !strCSV[4].equals("")) {
                    arrLogLine.add(strCSV[2]);
                    arrLogLine.add(strCSV[4]);
                    arrIndex.add(idx);
                    mLogData.add(arrLogLine);
                    arrLogLine = new ArrayList<String>();
                }
                idx++;
            }
        }
        catch (IOException e) {
        }
    }
}
