package luiten.patronus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.content.pm.ActivityInfo;

import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.imgproc.*;
import org.opencv.core.*;
import android.graphics.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Created by LG on 2017-05-22.
 */

public class StandardActivity extends Activity {
    private ImageView imageView;
    private Mat img_result;
    public static Context mContext;

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    @Override
    public void onCreate(Bundle savedIntanceState) {
        super.onCreate(savedIntanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.standard_main);

        this.imageView = (ImageView)this.findViewById(R.id.standard_image_camera);
        Button photoButton = (Button)this.findViewById(R.id.standard_btn_test);
        photoButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), PointActivity.class);
                startActivity(intent);
            }
        });

        // 취소
        Button button2 = (Button)findViewById(R.id.standard_btn_cancel);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        img_result = new Mat();
        mContext = this;

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/";
        File file = new File(dirPath);

        // 일치하는 폴더가 없으면 생성
        if (!file.exists()) {
            file.mkdirs();
        }

        // Load png image
        Bitmap img_bmp = BitmapFactory.decodeFile(dirPath + "point.png");
        imageView.setImageBitmap(img_bmp);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void SetImage(Mat result)
    {
        result.copyTo(img_result);

        Bitmap img_bmp = Bitmap.createBitmap(img_result.cols(), img_result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_result, img_bmp);
        imageView.setImageBitmap(img_bmp);

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/";
        File file = new File(dirPath);

        // 일치하는 폴더가 없으면 생성
        if (!file.exists()) {
            file.mkdirs();
        }

        // Save png image
        try {
            OutputStream stream = new FileOutputStream(dirPath + "point.png");
            img_bmp.compress(Bitmap.CompressFormat.PNG, 80, stream);
            stream.close();
        }
        catch (IOException e) {
        }
    }
}