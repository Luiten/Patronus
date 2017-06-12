package luiten.patronus;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
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

/**
 * Created by LG on 2017-05-31.
 */


public class RecordActivity extends SettingActivity {

    private ListView listView;
    private ItemArrayAdapter itemArrayAdapter;

    private String LogType[] = { "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함",
            "표지판", "졸음 운전", "충돌", "운전 점수" };

    List<String[]> csvLogs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        listView = (ListView)findViewById(R.id.record_listview);
        itemArrayAdapter = new ItemArrayAdapter(getApplicationContext(), R.layout.record_log);

        Parcelable state = listView.onSaveInstanceState();
        listView.setAdapter(itemArrayAdapter);
        listView.onRestoreInstanceState(state);

        // 경로 설정
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/";
        File file = new File(dirPath);

        // 일치하는 폴더가 없으면 생성
        if (!file.exists()) {
            file.mkdirs();
        }

        String csvName = "logs.csv";

        try {
            InputStream inputStream = new FileInputStream(dirPath + csvName);
            CSVReader csv = new CSVReader(inputStream);

            csvLogs = csv.read();

            for(String[] ListData : csvLogs) {
                String[] temp = Arrays.copyOf(ListData, ListData.length);
                temp[2] = LogType[Integer.valueOf(ListData[2]) - 1];
                itemArrayAdapter.add(temp);
            }
        }
        catch (IOException e) {
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

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

                    // 운전 점수
                    case 9:
                        intent = new Intent(getApplicationContext(), RecordDrive.class);
                        startActivity(intent);
                        break;

                }
            }

        });

    }

}
