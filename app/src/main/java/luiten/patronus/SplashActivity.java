package luiten.patronus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by power on 2017-07-26.
 */

// 출처: http://dudmy.net/android/2017/04/09/improved-loading-screen/
public class SplashActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

    public native int SetSettings(int type, double value);

    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS  = {"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.READ_PHONE_STATE",
            "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION", "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK", "android.permission.RECORD_AUDIO" };

    String[] strFileLists = {"cars.xml", "checkcas.xml", "lbpcascade_frontalface.xml", "haarcascade_frontalface_alt.xml", "haarcascade_eye.xml" };
    ArrayList<String> strDownloadLists = new ArrayList<String>();

    private ProgressDialog progressBar;

    private boolean hasPermissions(String[] permissions) {
        int ret = 0;
        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){
            ret = checkCallingOrSelfPermission(perms);
            if (!(ret == PackageManager.PERMISSION_GRANTED)) {
                //퍼미션 허가 안된 경우
                return false;
            }

        }
        //모든 퍼미션이 허가된 경우
        return true;
    }

    private void requestNecessaryPermissions(String[] permissions) {
        //마시멜로( API 23 )이상에서 런타임 퍼미션(Runtime Permission) 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){

            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean camreaAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!camreaAccepted)
                        {
                            showDialogforPermission("앱을 실행하려면 퍼미션 허가가 필요합니다.");
                            return;
                        }else
                        {
                            //이미 사용자에게 퍼미션 허가를 받음.
                        }
                    }
                }
                break;
        }
    }

    private void showDialogforPermission(String msg) {

        final AlertDialog.Builder myDialog = new AlertDialog.Builder(SplashActivity.this);
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
                }

            }
        });
        myDialog.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        myDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.drawable.splash);

        //--------------------------------------------------------------------------//
        // 스플래시 이미지
        //--------------------------------------------------------------------------//
        final ImageView imageView = (ImageView) findViewById(R.id.setting_splashimg);
        // dpi와 density 구하는 방법
        DisplayMetrics outMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);

        // 변경하고 싶은 레이아웃의 파라미터 값을 가져 옴
        RelativeLayout.LayoutParams ImageParams = (RelativeLayout.LayoutParams) imageView.getLayoutParams();

        ImageParams.width = outMetrics.widthPixels / 5;
        ImageParams.height = outMetrics.heightPixels / 5;

        // 변경된 값의 파라미터를 해당 레이아웃 파라미터 값에 셋팅
        imageView.setLayoutParams(ImageParams);


        if (!hasPermissions(PERMISSIONS)) { //퍼미션 허가를 했었는지 여부를 확인
            requestNecessaryPermissions(PERMISSIONS); //퍼미션 허가안되어 있다면 사용자에게 요청
        } else {
            //이미 사용자에게 퍼미션 허가를 받음.
        }

        // 세팅 값 읽기
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);

        // Manager 클래스에 적용
        SetSettings(1, settings.getBoolean("lane", true) ? 1 : 0);
        SetSettings(2, settings.getBoolean("distance", true) ? 1 : 0);
        SetSettings(3, settings.getBoolean("signal", true) ? 1 : 0);
        SetSettings(4, settings.getBoolean("sleep", true) ? 1 : 0);
        SetSettings(5, settings.getBoolean("sign", true) ? 1 : 0);
        SetSettings(6, settings.getInt("sensitivity", 4));
        //SetSettings(7, settings.getInt("resolution", 1)); // Video Size
        SetSettings(8, settings.getInt("resolutionwidth", 0)); // Video Width
        SetSettings(9, settings.getInt("resolutionheight", 0)); // Video Height
        SetSettings(10, (settings.getInt("sensitivity", 4) + 1) * 30); // Record Length
        SetSettings(400, settings.getInt("standardlanex0", 0));
        SetSettings(401, settings.getInt("standardlaney0", 0));
        SetSettings(402, settings.getInt("standardlanex1", 0));
        SetSettings(403, settings.getInt("standardlaney1", 0));

        // 파일이 하나라도 없으면 다운로드
        for (int i = 0; i < strFileLists.length; i++)
        {
            File file = new File("/sdcard/Patronus/" + strFileLists[i]);

            // 파일이 없으면 다운로드할 파일 리스트에 추가
            if (!file.exists()) {
                strDownloadLists.add(strFileLists[i]);
            }
        }

        final Handler handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                this.removeMessages(0);

                // 다운로드를 받아야할 게 하나라도 있으면 다운로드 팝업창 띄우기
                if (strDownloadLists.size() > 0)
                {
                    // 파일 다운로드 확인 창
                    AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this);
                    //builder.setTitle("파일 다운로드");
                    builder.setMessage("앱 실행에 필요한 파일을 다운로드합니다.");
                    builder.setCancelable(false); // 버튼을 누르기 전 창을 끌 수 없다

                    builder.setNegativeButton("아니오",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // 다음 액티비티 실행
                                    StartNextActivity();
                                }
                            });

                    builder.setPositiveButton("예",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    progressBar = new ProgressDialog(SplashActivity.this);
                                    progressBar.setMessage("다운로드 중");
                                    progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    progressBar.setIndeterminate(true);
                                    progressBar.setCancelable(true);

                                    final DownloadFilesTask downloadTask = new DownloadFilesTask(SplashActivity.this);
                                    downloadTask.execute(strDownloadLists);

                                    progressBar.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            downloadTask.cancel(true);
                                        }
                                    });
                                }
                            });
                    builder.show();
                }
                else {
                    // 다음 액티비티 실행
                    StartNextActivity();
                }

            }
        };

        handler.sendEmptyMessageDelayed(0, 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        strDownloadLists.clear();

        if (progressBar != null)
            progressBar.dismiss();

        final ImageView imageView = (ImageView) findViewById(R.id.setting_splashimg);
        //((BitmapDrawable)imageView.getDrawable()).getBitmap().recycle();
        imageView.setImageDrawable(null);
    }

    private void StartNextActivity()
    {
        // 처음 실행 확인: point.png 파일 유무 확인
        File file = new File("/sdcard/Patronus/point.png");

        // 일치하는 세팅 파일이 있으면 메인 액티비티
        if (file.exists()) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
        // 없으면 세팅 액티비티
        else {
            Toast.makeText(getApplicationContext(),"기준점 파일이 존재하지 않습니다.\n기준점을 촬영해 주세요.",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            intent = new Intent(getApplicationContext(), Setting.class);
            startActivity(intent);
        }

        finish();
    }

    // 다운로드 작업   출처: http://webnautes.tistory.com/1085
    private class DownloadFilesTask extends AsyncTask<ArrayList<String>, String, String> {
        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadFilesTask(Context context) {
            this.context = context;
        }

        //파일 다운로드를 시작하기 전에 프로그레스바를 화면에 보여줍니다.
        @Override
        protected void onPreExecute() { //2
            super.onPreExecute();

            //사용자가 다운로드 중 파워 버튼을 누르더라도 CPU가 잠들지 않도록 해서
            //다시 파워버튼 누르면 그동안 다운로드가 진행되고 있게 됩니다.
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();

            progressBar.show();
        }


        //파일 다운로드를 진행합니다.
        @Override
        protected String doInBackground(ArrayList<String>... strDownloadLists) { //3
            int count;
            long nFileSize = -1;
            URLConnection connection = null;
            InputStream input = null;
            OutputStream output = null;
            String strServerURL = "http://luiten.iptime.org/";
            File outputFile; //파일명까지 포함한 경로
            File path; //디렉토리경로
            String filename = "";

            String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/";
            File file = new File(dirPath);

            // 일치하는 폴더가 없으면 생성
            if (!file.exists()) {
                file.mkdirs();
            }

            for (int i = 0; i < strDownloadLists[0].size(); i++) {
                try {
                    URL url = new URL(strServerURL + strDownloadLists[0].get(i));
                    connection = url.openConnection();
                    connection.setRequestProperty("Accept-Encoding", "identity");
                    connection.connect();

                    filename = strServerURL + strDownloadLists[0].get(i);

                    //파일 크기를 가져옴
                    nFileSize = connection.getContentLength();

                    //URL 주소로부터 파일다운로드하기 위한 input stream
                    input = connection.getInputStream();

                    path = new File(dirPath);
                    outputFile = new File(path, strDownloadLists[0].get(i)); //파일명까지 포함함 경로의 File 객체 생성

                    // SD카드에 저장하기 위한 Output stream
                    output = new FileOutputStream(outputFile);

                    byte data[] = new byte[1024];
                    long downloadedSize = 0;
                    while ((count = input.read(data)) != -1) {
                        //사용자가 BACK 버튼 누르면 취소가능
                        if (isCancelled()) {
                            input.close();
                            return "";
                            //return Long.valueOf(-1);
                        }

                        downloadedSize += count;

                        if (nFileSize > 0) {
                            float per = ((float) downloadedSize / nFileSize) * 100;
                            String str = strDownloadLists[0].get(i) + ": " + (downloadedSize / 1024) + "KB / " + (nFileSize / 1024) + "KB";
                            publishProgress("" + (int) per, str);
                            //Thread.sleep(1);
                        }

                        //파일에 데이터를 기록합니다.
                        output.write(data, 0, count);
                    }

                    // Flush output
                    output.flush();

                    // Close streams
                    output.close();
                    input.close();

                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                } finally {
                    try {
                        if (output != null)
                            output.close();
                        if (input != null)
                            input.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            mWakeLock.release();

            return filename;
        }


        //다운로드 중 프로그레스바 업데이트
        @Override
        protected void onProgressUpdate(String... progress) { //4
            super.onProgressUpdate(progress);

            // if we get here, length is known, now set indeterminate to false
            progressBar.setIndeterminate(false);
            progressBar.setMax(100);
            progressBar.setProgress(Integer.parseInt(progress[0]));
            progressBar.setMessage(progress[1]);
        }

        //파일 다운로드 완료 후
        @Override
        protected void onPostExecute(String filename) { //5
            super.onPostExecute(filename);

            progressBar.dismiss();

            Toast.makeText(getApplicationContext(), "다운로드가 완료되었습니다.", Toast.LENGTH_LONG).show();

            StartNextActivity();
            /*if ( size > 0) {
                Toast.makeText(getApplicationContext(), "다운로드가 완료되었습니다. 파일 크기: " + size.toString(), Toast.LENGTH_LONG).show();
            }
            else
                Toast.makeText(getApplicationContext(), "다운로드 에러", Toast.LENGTH_LONG).show();*/
        }

    }
}