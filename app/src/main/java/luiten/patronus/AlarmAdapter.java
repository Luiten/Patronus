package luiten.patronus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by LG on 2017-07-06.
 */

public class AlarmAdapter extends BaseAdapter {
    private Context mContext = null;
    private ArrayList<AlarmData> mAlarmData = new ArrayList <AlarmData>();
    //알람 아이템들의 정보값들을 배열로 저장 리스트 포지션 하나당 들어갈 것들

    public AlarmAdapter(Context mContext){
        super();
        this.mContext = mContext;
    }//생성자

    @Override
    public int getCount() {
        return mAlarmData.size();
    } //리스트뷰 포지션의 개수가 배열의 수만큼 나오게한다

    @Override
    public Object getItem(int position) {
        return mAlarmData.get(position);
    } //각 리스트 포지션마다 배열의 저장된 데이터 반환

    @Override
    public long getItemId(int position) {
        return position;
    } //해당 포지션 반환

    public boolean isChecked(int position) {
        return mAlarmData.get(position).mChecked;
    } //리스트뷰 포지션의 개수가 배열의 수만큼 나오게한다

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final AlarmViewHolder holder;
        if(convertView == null) {
            holder = new AlarmViewHolder();

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.alarm_item, null);

            holder.alarmText = (TextView)convertView.findViewById(R.id.alarm_text_item);
            holder.alarmTextDesc = (TextView)convertView.findViewById(R.id.alarm_text_itemdesc);
            holder.alarmCheck = (CheckBox)convertView.findViewById(R.id.alarm_chk_item);

            //홀더에 저장되있는 것들을 이제 view에 뿌려준다

            convertView.setTag(holder);
        } else {
            holder = (AlarmViewHolder)convertView.getTag();
        }

        holder.alarmCheck.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                AlarmData mData = mAlarmData.get(position);
                mData.mChecked = holder.alarmCheck.isChecked();
            }
        });

        AlarmData mData = mAlarmData.get(position);

        holder.alarmText.setText(mData.minfo);
        holder.alarmTextDesc.setText(mData.mDesc);
        holder.alarmCheck.setVisibility(View.VISIBLE);
        holder.alarmCheck.setChecked(mData.mChecked);

        return convertView;
    }

    public void addItem(String minfo, String desc, boolean checked) {
        AlarmData addInfo = null;
        addInfo = new AlarmData();

        addInfo.minfo = minfo;
        addInfo.mDesc = desc;
        addInfo.mChecked = checked;

        mAlarmData.add(addInfo);
    }

    private class AlarmViewHolder {
        public TextView alarmText;
        public TextView alarmTextDesc;
        public CheckBox alarmCheck;
    } //알람 리스트뷰의 아이템 홀더

    public class AlarmData {

        public String minfo; // 체크박스 이름
        public String mDesc; // 체크박스 설명
        public boolean mChecked; // 체크 여부
    }
}