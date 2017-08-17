package luiten.patronus;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class RecordSignal extends Activity implements OnMapReadyCallback{

    private String LogType[] = { "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함",
            "표지판", "졸음 운전", "충돌", "운전 점수" };

    private String LogDescType[] = { "", "에서 자동차가 끼어들었습니다.", "을 침범했습니다.", "일 때 신호를 위반했습니다.",
            " 중일 때 전방을 주시하지 않았습니다.", "입니다.", "초간 눈을 감았습니다.", "의 충돌이 감지됐습니다.", "" };

    private String[] Logs;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.record_signal);
        Intent intent = getIntent();
        Logs = intent.getStringArrayExtra("Logs");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        FragmentManager fragmentManager = getFragmentManager();
        MapFragment mapFragment = (MapFragment)fragmentManager.findFragmentById(R.id.recordsignal_map);
        mapFragment.getMapAsync(this);

/*        final TextView tvTitle = (TextView)findViewById(R.id.recordsignal_text_title);
        tvTitle.setText(LogType[Integer.valueOf(Logs[2]) - 1]);*/

        final TextView tvContent = (TextView)findViewById(R.id.recordsignal_text_content);
        tvContent.setText("날짜: " + Logs[0] + "\n시간: " + Logs[1] + "\n종류: " + LogType[Integer.valueOf(Logs[2]) - 1] + "\n\n" +
                Logs[3] + LogDescType[Integer.valueOf(Logs[2]) - 1]);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {

        LatLng LocPos = new LatLng(Float.valueOf(Logs[5]), Float.valueOf(Logs[6]));

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(LocPos);
        markerOptions.title(LogType[Integer.valueOf(Logs[2]) - 1]);
        googleMap.addMarker(markerOptions);

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(LocPos));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

    }
}
