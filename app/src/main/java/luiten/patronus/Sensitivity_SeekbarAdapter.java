package luiten.patronus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by LG on 2017-07-06.
 */

public class Sensitivity_SeekbarAdapter extends BaseAdapter {
    private Context mContext = null;
    private ArrayList<Sensi_SeekData> mSensi_SeekData = new ArrayList <Sensi_SeekData>();
    //알람 아이템들의 정보값들을 배열로 저장 리스트 포지션 하나당 들어갈 것들

    public Sensitivity_SeekbarAdapter(Context mContext){
        super();
        this.mContext = mContext;
    }//생성자

    @Override
    public int getCount() {
        return mSensi_SeekData.size();
    } //리스트뷰 포지션의 개수가 배열의 수만큼 나오게한다

    @Override
    public Object getItem(int position) {
        return mSensi_SeekData.get(position);
    } //각 리스트 포지션마다 배열의 저장된 데이터 반환

    @Override
    public long getItemId(int position) {
        return position;
    } //해당 포지션 반환

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Sensi_SeekViewHolder holder;
        if(convertView == null){
            holder = new Sensi_SeekViewHolder();

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.sensitivity_item, null);

            holder.seek_scoreText = (TextView)convertView.findViewById(R.id.sensitivity_text_score);
            holder.sensi_seekbar = (SeekBar)convertView.findViewById(R.id.sensitivity_seekbar_item);

            //홀더에 저장되있는 것들을 이제 view에 뿌려준다

            convertView.setTag(holder);
        } else {
            holder = (Sensi_SeekViewHolder)convertView.getTag();
        }

        holder.sensi_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                holder.seek_scoreText.setText(((progress + 1) * 10));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return convertView;
    }

    public void addItem(String minfo){
        Sensi_SeekData addInfo = null;
        addInfo = new Sensi_SeekData();
        addInfo.minfo = minfo;

        mSensi_SeekData.add(addInfo);
    }

    private class Sensi_SeekViewHolder {
        public TextView seek_scoreText;
        public SeekBar sensi_seekbar;
    } //알람 리스트뷰의 아이템 홀더

    public class Sensi_SeekData {

        public String minfo;
    }
}
