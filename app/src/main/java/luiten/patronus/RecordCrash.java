package luiten.patronus;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;

/**
 * Created by LG on 2017-06-05.
 */

public class RecordCrash extends SettingActivity {

    private String LogType[] = { "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함",
            "표지판", "졸음 운전", "충돌", "운전 점수" };


    private String LogDescType[] = { "", "에서 자동차가 끼어들었습니다.", "을 침범했습니다.", "일 때 신호를 위반했습니다.",
            " 중일 때 전방을 주시하지 않았습니다.", "입니다.", "초간 눈을 감았습니다.", "%의 충돌이 감지됐습니다.", "" };

    private String[] Logs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_crash);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent intent = getIntent();
        Logs = intent.getStringArrayExtra("Logs");

        VideoView videoView = (VideoView)findViewById(R.id.recordcrash_videoView);
        MediaController mediaController = new MediaController(this);

        mediaController.setAnchorView(videoView);

        // 경로 설정
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/video/";
        File file = new File(dirPath);

        // 일치하는 폴더가 없으면 생성
        if (!file.exists()) {
            file.mkdirs();
        }

        dirPath += Logs[4];

        Uri video = Uri.parse(dirPath);

        videoView.setMediaController(mediaController);
        videoView.setVideoURI(video);
        videoView.requestFocus();

        //videoView.start();

        final TextView tvTitle = (TextView)findViewById(R.id.recordcrash_text_title);
        tvTitle.setText(LogType[Integer.valueOf(Logs[2]) - 1]);

        final TextView tvContent = (TextView)findViewById(R.id.recordcrash_text_content);
        tvContent.setText("날짜: " + Logs[0] + "\n시간: " + Logs[1] + "\n종류: " + LogType[Integer.valueOf(Logs[2]) - 1] + "\n영상: " +
                Logs[4] + "\n\n내용\n" + Logs[3] + LogDescType[Integer.valueOf(Logs[2]) - 1]);
    }
}
