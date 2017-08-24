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
import android.widget.Toast;

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

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.standard_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.imageView = (ImageView)this.findViewById(R.id.standard_image_camera);

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/";
        File file = new File(dirPath + "point.png");

        // 일치하는 파일이 없으면 Toast
        if (!file.exists()) {
            Toast.makeText(getApplicationContext(),"기준점 파일이 존재하지 않습니다.",Toast.LENGTH_SHORT).show();
        }
        else {
            // Load png image
            Bitmap img_bmp = BitmapFactory.decodeFile(dirPath + "point.png");
            imageView.setImageBitmap(img_bmp);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}