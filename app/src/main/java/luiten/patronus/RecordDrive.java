package luiten.patronus;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by power on 2017-06-12.
 */

public class RecordDrive extends AppCompatActivity {

    private BarChart drive_chart;
    private LinearLayout score_layout;
    private TextView crash_score;
    private TextView signal_score;
    private TextView line_score;
    private TextView sleep_score;
    private TextView distance_score;
    private TextView attention_score;
    private TextView cutin_score;
    private TextView total_score;

    private int minusValue = 0;

    private String LogType[] = { "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함",
            "표지판", "졸음 운전", "충돌", "운전 점수", "수동 녹화" };
    private Integer LogTypeScore[] = { 2, 5, 1, 5, 3,
            0, 20, 30, 100, 0 };
    private String MedalDesc[] = { "아주 완벽해요!", "잘했어요!", "괜찮았어요!", "조금 더 노력해봐요!", "무슨 일이 있었나요?", "면허를 따신지 얼마 안되셨나요?" };

    private ArrayList<String> mLogDate = new ArrayList<String>();
    private ArrayList<ArrayList<Integer>> arrIndex = new ArrayList<ArrayList<Integer>>();
    private ArrayList<ArrayList<String>> mChildTime =  new ArrayList<ArrayList<String>>();
    //private ArrayList<ArrayList<String>> mChildDesc =  new ArrayList<ArrayList<String>>();
    private ArrayList<ArrayList<Integer>> mChildType =  new ArrayList<ArrayList<Integer>>();
    List<String[]> csvLogs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("운전 점수");
        setContentView(R.layout.record_drive);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ArrayList<String> mDate = new ArrayList<>();

        crash_score = (TextView)findViewById(R.id.recorddrive_text_crashscore);
        signal_score = (TextView)findViewById(R.id.recorddrive_text_signalscore);
        line_score = (TextView)findViewById(R.id.recorddrive_text_linescore);
        sleep_score = (TextView)findViewById(R.id.recorddrive_text_sleepscore);
        distance_score = (TextView)findViewById(R.id.recorddrive_text_distancescore);
        attention_score = (TextView)findViewById(R.id.recorddrive_text_attentionscore);
        cutin_score = (TextView)findViewById(R.id.recorddrive_text_cutinscore);
        total_score = (TextView)findViewById(R.id.recorddrive_text_totalscore);

//        score_layout.setVisibility(View.INVISIBLE);

        drive_chart = (BarChart)findViewById(R.id.recorddrive_barchart);

        ArrayList<IBarDataSet> dataSets = null;
        ArrayList<BarEntry> valueSet = new ArrayList<>();

        // csv 읽기
        setArrayData();

        int n = 0;
        if (7 - mLogDate.size() > 0) {
            minusValue = mLogDate.size();
        } else {
            minusValue = 7;
        }

        for (int i = mLogDate.size() - minusValue; i < mLogDate.size(); i++, n++) {
            mDate.add(mLogDate.get(i).substring(5)); // 연도를 지우고 월-일만 표시한다
            valueSet.add(new BarEntry(CalculateScore(i), n)); // 총 점수 계산값 적용
        }

        BarDataSet barDataSet = new BarDataSet(valueSet,"최근 운전 점수");

        barDataSet.setColor(getResources().getColor(R.color.colorAccent), 200);
        barDataSet.setValueTextSize(15);

        dataSets = new ArrayList<>();
        dataSets.add(barDataSet);

        YAxis yAxisRight = drive_chart.getAxisRight();
        yAxisRight.setEnabled(false);

        XAxis xAxis = drive_chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        BarData data = new BarData(mDate, dataSets);
        data.setValueFormatter(new CustomValueFormatter());

        Legend legend = drive_chart.getLegend();
        legend.setEnabled(false);

//        drive_chart.setExtraOffsets(0, 0, 0, 20);
        drive_chart.setVisibleXRangeMaximum(7);
        drive_chart.setData(data);
        drive_chart.setScaleXEnabled(false);
        drive_chart.setScaleYEnabled(false);
        drive_chart.setDescription("");
        drive_chart.animateXY(2000, 2000);
        drive_chart.invalidate();
        drive_chart.setDragEnabled(true);
        drive_chart.setDrawBorders(false);
        drive_chart.getAxisLeft().setAxisMaxValue(100);
        drive_chart.getAxisLeft().setAxisMinValue(0);
        //drive_chart.setBackgroundColor(getResources().getColor(R.color.colorAccent));

        drive_chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                if (e == null)
                    return;
                CalculateScore(mLogDate.size() - minusValue + e.getXIndex());
            }

            @Override
            public void onNothingSelected() {
                //Toast.makeText(getApplicationContext(), "다시 눌러주세요", Toast.LENGTH_LONG).show();
            }
        });
    }

    //------------------------------------------------------------------------------------------------//
    // 로그 읽어오기
    //------------------------------------------------------------------------------------------------//
    private void setArrayData() {
        ArrayList<String> arrChildTime = new ArrayList<String>();
        //ArrayList<String> arrChildDesc = new ArrayList<String>();
        ArrayList<Integer> arrChildType = new ArrayList<Integer>();

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
                    mLogDate.add(strTemp);
                    mChildTime.add(arrChildTime);
                    mChildType.add(arrChildType);
                    arrIndex.add(arrTemp);
                    arrChildTime = new ArrayList<String>();
                    arrChildType = new ArrayList<Integer>();
                    arrTemp = new ArrayList<Integer>();
                    strTemp = strCSV[0];
                }

                arrTemp.add(idx++);
                arrChildTime.add(strCSV[1]);
                arrChildType.add(Integer.valueOf(ListData[2]) - 1);
            }

            mLogDate.add(strTemp);
            mChildTime.add(arrChildTime);
            mChildType.add(arrChildType);
            arrIndex.add(arrTemp);
        }
        catch (IOException e) {
        }

    }

    //------------------------------------------------------------------------------------------------//
    // 로그 읽어오기
    //------------------------------------------------------------------------------------------------//
    private int CalculateScore(int idx)
    {
        int resultScore = 100;
        // 충돌, 신호위반, 차선침범, 졸음운전, 부주의, 차량 거리, 끼어들기
        int crash = 0, signal = 0, line = 0, sleep = 0, attention = 0, distance = 0,  cutin = 0;

        for (int i = 0; i < mChildType.get(idx).size(); i++) {
            resultScore -= LogTypeScore[mChildType.get(idx).get(i)];

            switch (mChildType.get(idx).get(i)) {
                case 0: // 자동차간 거리
                    distance++;
                    break;
                case 1: // 끼어들기
                    cutin++;
                   break;
                case 2: // 차선 침범
                    line++;
                    break;
                case 3: // 신호 위반
                    signal++;
                    break;
                case 4: // 신호 주시 안함
                    attention++;
                    break;
                case 6: // 졸음 운전
                    sleep++;
                    break;
                case 7: // 충돌
                    crash++;
                    break;
            }
        }

        if (resultScore < 0)
            resultScore = 0;

        distance_score.setText("차간 거리: " + distance + "회");
        cutin_score.setText("끼어들기: " + cutin + "회");
        line_score.setText("차선침범: " + line + "회");
        signal_score.setText("신호위반: " + signal + "회");
        attention_score.setText("신호 부주의: " + attention + "회");
        crash_score.setText("충돌: " + crash + "회");
        sleep_score.setText("졸음운전: " + sleep + "회");
        total_score.setText("총 점수: " + resultScore + "점");

        // 날짜
        TextView textDate = (TextView)findViewById(R.id.recorddrive_text_date);
        textDate.setText(mLogDate.get(idx));

        // 날짜 설명
        TextView textDesc = (TextView)findViewById(R.id.recorddrive_text_medaldesc);

        // 이미지 뷰 메달 설정
        final ImageView imageMedal = (ImageView) findViewById(R.id.recorddrive_image_medal);
        if (resultScore >= 90) {
            imageMedal.setImageResource(R.drawable.trophy);
            textDesc.setText(MedalDesc[0]);
        }
        else if (resultScore >= 80) {
            imageMedal.setImageResource(R.drawable.medal1);
            textDesc.setText(MedalDesc[1]);
        }
        else if (resultScore >= 60) {
            imageMedal.setImageResource(R.drawable.medal2);
            textDesc.setText(MedalDesc[2]);
        }
        else if (resultScore >= 40) {
            imageMedal.setImageResource(R.drawable.medal3);
            textDesc.setText(MedalDesc[3]);
        }
        else  {
            imageMedal.setImageResource(R.drawable.logo);
            if (resultScore >= 10) textDesc.setText(MedalDesc[4]);
            else textDesc.setText(MedalDesc[5]);
        }

        return resultScore;
    }
}
