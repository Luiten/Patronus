package luiten.patronus;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.widget.ListView;

/**
 * Created by LG on 2017-06-05.
 */

public class Manual_Recording extends AppCompatActivity {

    private ListView manual_recording;
    private Manual_RecordingAdapter manual_recordingAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().setTitle("수동 동영상");
        setContentView(R.layout.manual_recording);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        manual_recording = (ListView)findViewById(R.id.manual_recording);
        manual_recordingAdapter = new Manual_RecordingAdapter(this);
        manual_recording.setAdapter(manual_recordingAdapter);

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/video/20171104_101547.mp4";
        String filename = "20171104_101547.mp4";

        manual_recordingAdapter.addItem(dirPath,filename); //파일경로와 파일이름 add


    }
}
