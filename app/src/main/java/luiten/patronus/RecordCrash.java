package luiten.patronus;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

/**
 * Created by LG on 2017-06-05.
 */

public class RecordCrash extends Activity {

    private String LogType[] = { "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함",
            "표지판", "졸음 운전", "충돌", "운전 점수" };

    private String LogDescType[] = { "", "에서 자동차가 끼어들었습니다.", "을 침범했습니다.", "일 때 신호를 위반했습니다.",
            " 중일 때 전방을 주시하지 않았습니다.", "입니다.", "초간 눈을 감았습니다.", "%의 충돌이 감지됐습니다.", "" };

    private String[] Logs;

    VideoView videoView;
    MediaController mediaController;
    Button play, stop;
    SeekBar playtime;
    int pos;
    boolean isPlaying = false;
    boolean bstart = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.record_crash);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent intent = getIntent();
        Logs = intent.getStringArrayExtra("Logs");

        // *** https://github.com/brightec/ExampleMediaController 참조해서 MediaController 대신 커스텀 SeekBar로 만들기
        videoView = (VideoView)findViewById(R.id.recordcrash_videoView);
        play = (Button)findViewById(R.id.button_play);
        stop = (Button)findViewById(R.id.button_stop);
        playtime = (SeekBar)findViewById(R.id.video_seekbar);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                playtime.setMax(videoView.getDuration());
                playtime.postDelayed(onEverySecond, 1000);
            }
        });

        playtime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    videoView.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isPlaying = false;
                //videoView.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isPlaying = true;
                int user_stop = seekBar.getProgress();
                videoView.seekTo(user_stop);
                //videoView.start();
            }
        });

        // 경로 설정
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/video/";
        dirPath += Logs[4];
        File file = new File(dirPath);

        // 일치하는 파일이 없으면 오류 메시지 출력
        if (!file.exists() || Logs[4].equals("")) {
            Toast.makeText(getApplicationContext(), "동영상 파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
        }
        else {
            Uri video = Uri.parse(dirPath);
            videoView.setVideoURI(video);
            videoView.requestFocus();
        }

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 재생중 일 떄
                if(bstart) {
                    pos = videoView.getCurrentPosition();
                    videoView.pause();
                    isPlaying = false;
                    play.setText("Play");
                }
                else {
                    videoView.start();
                    play.setText("Pause");
                }

                bstart = !bstart;
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlaying = false;
                pos = 0;
                playtime.setProgress(0);
                videoView.seekTo(0);
                videoView.pause();
                play.setText("Play");
                bstart = false;
            }
        });

        final TextView tvContent = (TextView)findViewById(R.id.recordcrash_text_content);
        tvContent.setText("날짜: " + Logs[0] + "\n시간: " + Logs[1] + "\n종류: " + LogType[Integer.valueOf(Logs[2]) - 1] + "\n영상: " +
                Logs[4] + "\n\n" + Logs[3] + LogDescType[Integer.valueOf(Logs[2]) - 1]);
    }

    private Runnable onEverySecond = new Runnable() {
        @Override
        public void run() {
            if(playtime != null){
                playtime.setProgress(videoView.getCurrentPosition());
            }

            if(videoView.isPlaying()){
                playtime.postDelayed(onEverySecond, 500);
            }
        }
    };
}
