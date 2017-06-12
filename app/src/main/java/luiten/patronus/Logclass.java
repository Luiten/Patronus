package luiten.patronus;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by power on 2017-06-04.
 */

public class Logclass  {

    String csvName = "logs.csv";

    public void Write(int nType, String strMsg, String strDesc, double x, double y)
    {
        String strLog = "logs.csv";

        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
        strLog = sdfNow.format(new Date(System.currentTimeMillis()));

        strLog += "," + nType + "," + strMsg + "," + strDesc + "," + x + "," + y + "\n";

        // 파일 저장
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(dirPath);

        // 일치하는 폴더가 없으면 생성
        if (!file.exists()) {
            file.mkdirs();
        }

        // Save csv file
        try {
            OutputStream stream = new FileOutputStream(dirPath + "/Patronus/" + csvName);
            stream.write(strLog.getBytes());
            stream.close();
        }
        catch (IOException e) {
        }
    }

    int Read(int idx, int pType, String pstrMsg, String pstrDesc, double px, double py)
    {
        return 0;
    }
}
