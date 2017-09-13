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

public class Capture_lengthAdapter extends BaseAdapter {
    private Context mContext = null;
    private ArrayList<Cap_lengthData> mCap_length = new ArrayList<>();
    //알람 아이템들의 정보값들을 배열로 저장 리스트 포지션 하나당 들어갈 것들

    public Capture_lengthAdapter(Context mContext){
        super();
        this.mContext = mContext;
    }//생성자

    @Override
    public int getCount() {
        return mCap_length.size();
    } //리스트뷰 포지션의 개수가 배열의 수만큼 나오게한다

    @Override
    public Object getItem(int position) {
        return mCap_length.get(position);
    } //각 리스트 포지션마다 배열의 저장된 데이터 반환

    @Override
    public long getItemId(int position) {
        return position;
    } //해당 포지션 반환

    public boolean isChecked(int position) {
        return mCap_length.get(position).mChecked;
    } //리스트뷰 포지션의 개수가 배열의 수만큼 나오게한다

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Cap_lengthViewHolder holder;
        if(convertView == null) {
            holder = new Cap_lengthViewHolder();

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.capture_item, null);

            holder.captureText = (TextView)convertView.findViewById(R.id.capture_text_item);
            holder.captureCheck = (CheckBox)convertView.findViewById(R.id.capture_chk_item);

            //홀더에 저장되있는 것들을 이제 view에 뿌려준다

            convertView.setTag(holder);
        } else {
            holder = (Cap_lengthViewHolder) convertView.getTag();
        }

        holder.captureCheck.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Cap_lengthData mData = mCap_length.get(position);
                mData.mChecked = holder.captureCheck.isChecked();
                holder.captureText.setEnabled(mData.mEnabled);
                holder.captureCheck.setEnabled(mData.mEnabled);
            }
        });

        Cap_lengthData mData = mCap_length.get(position);

        holder.captureText.setText(mData.minfo);
        holder.captureCheck.setVisibility(View.VISIBLE);
        holder.captureText.setEnabled(mData.mEnabled);
        holder.captureCheck.setEnabled(mData.mEnabled);
        holder.captureCheck.setChecked(mData.mChecked);

        return convertView;
    }

    public void addItem(String minfo, boolean checked, boolean enable) {
        Cap_lengthData addInfo = null;
        addInfo = new Cap_lengthData();
        addInfo.minfo = minfo;
        addInfo.mChecked = checked;
        addInfo.mEnabled = enable;

        mCap_length.add(addInfo);
    }

    public void setEnable(int position, boolean enable) {
        mCap_length.get(position).mEnabled = enable;
    }

    private class Cap_lengthViewHolder {
        public TextView captureText;
        public CheckBox captureCheck;
    } //알람 리스트뷰의 아이템 홀더

    public class Cap_lengthData {

        public String minfo; // 체크박스 이름
        public boolean mChecked; // 체크 여부
        public boolean mEnabled; // 사용 여부
    }
}