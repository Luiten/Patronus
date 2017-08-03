package luiten.patronus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by LG on 2017-07-06.
 */

public class ResolutionAdapter extends BaseAdapter {
    private Context mContext = null;
    private ArrayList<ResolData> mResolData = new ArrayList <ResolData>();
    //알람 아이템들의 정보값들을 배열로 저장 리스트 포지션 하나당 들어갈 것들

    public ResolutionAdapter(Context mContext){
        super();
        this.mContext = mContext;
    }//생성자

    @Override
    public int getCount() {
        return mResolData.size();
    } //리스트뷰 포지션의 개수가 배열의 수만큼 나오게한다

    @Override
    public Object getItem(int position) {
        return mResolData.get(position);
    } //각 리스트 포지션마다 배열의 저장된 데이터 반환

    @Override
    public long getItemId(int position) {
        return position;
    } //해당 포지션 반환

    // 체크된 라디오버튼 인덱스 반환
    public int GetChecked() {
        return mSelectedRadioPosition;
    }

    private int mSelectedRadioPosition;
    private RadioButton mLastSelectedRadioButton;

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final ResolViewHolder holder;
        if(convertView == null){
            holder = new ResolViewHolder();

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.resolution_item, null);

            holder.resolText = (TextView)convertView.findViewById(R.id.resol_text_item);
            holder.resolRadio = (RadioButton)convertView.findViewById(R.id.resol_radio_item);

            //홀더에 저장되있는 것들을 이제 view에 뿌려준다

            convertView.setTag(holder);
        } else {
            holder = (ResolViewHolder)convertView.getTag();
        }

        if(mSelectedRadioPosition == position) {
            holder.resolRadio.setChecked(true);
        } else {
            holder.resolRadio.setChecked(false);
        } //라디오 버튼이 해당 포지션을 눌렀을 경우 체크되고 다른 라디오버튼은 다 체크 x

        final ResolData mData = mResolData.get(position);

        holder.resolRadio.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(mSelectedRadioPosition==position) {
                    return;
                }

                mSelectedRadioPosition = position;

                if(mLastSelectedRadioButton != null) {
                    mLastSelectedRadioButton.setChecked(false);
                }

                mLastSelectedRadioButton = (RadioButton) v;
                notifyDataSetChanged();
            }
        });

        holder.resolText.setText(mData.minfo);

        return convertView;
    }

    public void addItem(String minfo) {
        ResolData addInfo = null;
        addInfo = new ResolData();
        addInfo.minfo = minfo;

        mResolData.add(addInfo);
    }

    public void SetSelectNumber(int idx) {
        mSelectedRadioPosition = idx;
    }

    private class ResolViewHolder {
        public TextView resolText;
        public RadioButton resolRadio;
    } //알람 리스트뷰의 아이템 홀더

    public class ResolData {
        public String minfo;
    }
}
