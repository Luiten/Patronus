package luiten.patronus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by LG on 2017-07-06.
 */

public class StandardAdapter extends BaseAdapter {
    private Context mContext = null;
    private ArrayList<StandData> mStandData = new ArrayList <StandData>();
    //알람 아이템들의 정보값들을 배열로 저장 리스트 포지션 하나당 들어갈 것들

    public StandardAdapter(Context mContext){
        super();
        this.mContext = mContext;
    }//생성자

    @Override
    public int getCount() {
        return mStandData.size();
    } //리스트뷰 포지션의 개수가 배열의 수만큼 나오게한다

    @Override
    public Object getItem(int position) {
        return mStandData.get(position);
    } //각 리스트 포지션마다 배열의 저장된 데이터 반환

    @Override
    public long getItemId(int position) {
        return position;
    } //해당 포지션 반환

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StandViewHolder holder;
        if(convertView == null){
            holder = new StandViewHolder();

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.sensitivity_item, null);

            holder.sensiText = (TextView)convertView.findViewById(R.id.sensitivity_text_item);
            holder.sensiNext = (TextView)convertView.findViewById(R.id.sensitivity_text_next);

            //홀더에 저장되있는 것들을 이제 view에 뿌려준다

            convertView.setTag(holder);
        } else {
            holder = (StandViewHolder)convertView.getTag();
        }

        StandData mData = mStandData.get(position);

        holder.sensiText.setText(mData.minfo);
        holder.sensiNext.setVisibility(View.VISIBLE);

        return convertView;
    }

    public void addItem(String minfo){
        StandData addInfo = null;
        addInfo = new StandData();
        addInfo.minfo = minfo;

        mStandData.add(addInfo);
    }

    private class StandViewHolder {
        public TextView sensiText;
        public TextView sensiNext;
    } //알람 리스트뷰의 아이템 홀더

    public class StandData {

        public String minfo;
    }
}
