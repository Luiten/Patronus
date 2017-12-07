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
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

                ShowLogActivity(groupPosition, childPosition);
                return false;
            }
        });

        registerForContextMenu(log_listview);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

        int type = ExpandableListView.getPackedPositionType(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            menu.add(0, 0, 1, "삭제");
            //그룹 날짜있는 곳을 눌렀을때 나오는곳
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            menu.add(0, 0, 1, "삭제");
            menu.add(0, 1, 1, "보기");
            //자식 있는곳을 눌렀을때 나오는곳
        }
    } //ContextMenu 항목 추가 란!!~~~~~~

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();

        int type = ExpandableListView.getPackedPositionType(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            switch (item.getItemId()) {
                case 0:
                    AlertDialog.Builder alert_confirm = new AlertDialog.Builder(RecordActivity.this);
                    alert_confirm.setMessage("해당 날짜의 로그를 삭제 하시겠습니까?").setCancelable(false).setPositiveButton("확인",
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
                    break;

            } //그룹 포지션일때 0번 포지션에 있는 아이템을 눌렀을경우
        }
        else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            switch(item.getItemId()) {
                case 0:
                    AlertDialog.Builder alert_confirm = new AlertDialog.Builder(RecordActivity.this);
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
                    ShowLogActivity(item.getGroupId(), item.getItemId());
                    break; //자식 포지션일때 1번 포지션에 있는 아이템을 눌렀을 경우
            }
        }
        return super.onContextItemSelected(item);
    }// 해당 ContextMenu에 아이템들을 각각 클릭햇을때 나오는 함수설정

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

    private void ShowLogActivity(int GroupID, int ChildID) {
        ArrayList<Integer> arrTemp = arrIndex.get(GroupID);
        int position = arrTemp.get(ChildID);

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
    }
}
