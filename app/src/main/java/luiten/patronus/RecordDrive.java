package luiten.patronus;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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

import java.util.ArrayList;

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
    private TextView attention_score;
    private TextView sign_score;
    int crash, signal, line, sleep, attention, sign; //충돌, 신호위반, 차선침범, 졸음운전, 부주의, 표지판 횟수
    int tot_score; //100점에서 뺀 총 점수

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("운전 점수");
        setContentView(R.layout.record_drive);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        tot_score = 100-(crash*30+signal*10+line*10+sleep*10+sign+5+attention*5);// 토탈값을 구한후 밑에 barchart에 삽입

        crash_score = (TextView)findViewById(R.id.recorddrive_text_crashscore);
        signal_score = (TextView)findViewById(R.id.recorddrive_text_signalscore);
        line_score = (TextView)findViewById(R.id.recorddrive_text_linescore);
        sleep_score = (TextView)findViewById(R.id.recorddrive_text_sleepscore);
        attention_score = (TextView)findViewById(R.id.recorddrive_text_attentionscore);
        sign_score = (TextView)findViewById(R.id.recorddrive_text_signscore);

//        score_layout.setVisibility(View.INVISIBLE);

        drive_chart = (BarChart)findViewById(R.id.recorddrive_barchart);

        ArrayList<String> mDate = new ArrayList<>();
        ArrayList<IBarDataSet> dataSets = null;
        ArrayList<BarEntry> valueSet = new ArrayList<>();

        mDate.add("6.10");
        mDate.add("6.11");
        mDate.add("6.12");
        mDate.add("6.13");
        mDate.add("6.14");
        mDate.add("6.15");
        mDate.add("6.16");
        mDate.add("6.17");
        mDate.add("6.18");


        valueSet.add(new BarEntry(25,0));
        valueSet.add(new BarEntry(79,1));
        valueSet.add(new BarEntry(60,2));
        valueSet.add(new BarEntry(21,3));
        valueSet.add(new BarEntry(30,4));
        valueSet.add(new BarEntry(45,5));
        valueSet.add(new BarEntry(25,6));
        valueSet.add(new BarEntry(79,7));
        valueSet.add(new BarEntry(21,8));


        BarDataSet barDataSet = new BarDataSet(valueSet,"최근 운전 점수");

        barDataSet.setColor(R.color.colorAccent);
        barDataSet.setValueTextSize(15);

        dataSets = new ArrayList<>();
        dataSets.add(barDataSet);

        YAxis yAxisRight = drive_chart.getAxisRight();
        yAxisRight.setEnabled(false);


        XAxis xAxis = drive_chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        BarData data = new BarData(mDate,dataSets);
        data.setValueFormatter(new CustomValueFormatter());

        Legend legend = drive_chart.getLegend();
        legend.setEnabled(false);

//        drive_chart.setExtraOffsets(0, 0, 0, 20);
        drive_chart.setVisibleXRangeMaximum(7);
        drive_chart.setData(data);
        drive_chart.setScaleXEnabled(true);
        drive_chart.setScaleYEnabled(true);
        drive_chart.setDescription("");
        drive_chart.animateXY(2000,2000);
        drive_chart.invalidate();
        drive_chart.setDragEnabled(true);
        drive_chart.setDrawBorders(false);

        drive_chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
        @Override
        public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
            if(e==null)
            return;

            switch (e.getXIndex()){
        case 0 :
            crash_score.setText("aaaa");
            break;
        case 1 :
            crash_score.setText("bbbb");
            break;
        case 2 :
            crash_score.setText("cccc");
            break;
        case 3 :
            crash_score.setText("dddd");
            break;
        case 4 :
            crash_score.setText("eeee");
            break;
        case 5 :
            crash_score.setText("ffff");
            break;
    }
}

@Override
public void onNothingSelected() {
        Toast.makeText(getApplicationContext(), "다시 눌러주세요", Toast.LENGTH_LONG).show();
        }
    });

        }
}
