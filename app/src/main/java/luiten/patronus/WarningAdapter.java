package luiten.patronus;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by LG on 2017-07-06.
 */

public class WarningAdapter extends BaseAdapter {
    private Context mContext = null;
    private ArrayList<WarningData> mWarningData = new ArrayList <WarningData>();
    //알람 아이템들의 정보값들을 배열로 저장 리스트 포지션 하나당 들어갈 것들

    public WarningAdapter(Context mContext){
        super();
        this.mContext = mContext;
    }//생성자

    @Override
    public int getCount() {
        return mWarningData.size();
    } //리스트뷰 포지션의 개수가 배열의 수만큼 나오게한다

    @Override
    public Object getItem(int position) {
        return mWarningData.get(position);
    } //각 리스트 포지션마다 배열의 저장된 데이터 반환

    @Override
    public long getItemId(int position) {
        return position;
    } //해당 포지션 반환

    public int GetChecked() {
        return mSelectedRadioPosition;
    }

    private int mSelectedRadioPosition;
    private RadioButton mLastSelectedRadioButton;

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final WarningViewHolder holder;
        if(convertView == null) {
            holder = new WarningViewHolder();

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.warning_item, null);

            holder.WarningText = (TextView)convertView.findViewById(R.id.warning_text_item);
            holder.WarningTextDesc = (TextView)convertView.findViewById(R.id.warning_text_itemdesc);
            holder.WarningRadio = (RadioButton) convertView.findViewById(R.id.warning_radio_item);

            //----------------------------------------------------//
            // 텍스트 너비 화면에 맞게 재설정
            // 출처: http://developer88.tistory.com/71
            //----------------------------------------------------//
            // dpi와 density 구하는 방법
            DisplayMetrics outMetrics = new DisplayMetrics();
            ((Setting)mContext).getWindowManager().getDefaultDisplay().getMetrics(outMetrics);

            // 변경하고 싶은 레이아웃의 파라미터 값을 가져 옴
            LinearLayout.LayoutParams TextDescParams = (LinearLayout.LayoutParams) holder.WarningTextDesc.getLayoutParams();

            TextDescParams.width = outMetrics.widthPixels * 2 / 3;

            // 변경된 값의 파라미터를 해당 레이아웃 파라미터 값에 셋팅
            holder.WarningTextDesc.setLayoutParams(TextDescParams);

            //홀더에 저장되있는 것들을 이제 view에 뿌려준다
            convertView.setTag(holder);
        } else {
            holder = (WarningViewHolder)convertView.getTag();
        }

        if(mSelectedRadioPosition == position) {
            holder.WarningRadio.setChecked(true);
        } else {
            holder.WarningRadio.setChecked(false);
        } //라디오 버튼이 해당 포지션을 눌렀을 경우 체크되고 다른 라디오버튼은 다 체크 x

        WarningData mData = mWarningData.get(position);

        holder.WarningRadio.setOnClickListener(new View.OnClickListener(){

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

        holder.WarningText.setText(mData.minfo);
        holder.WarningTextDesc.setText(mData.mDesc);

        return convertView;
    }

    public void addItem(String minfo, String desc) {
        WarningData addInfo = null;
        addInfo = new WarningData();

        addInfo.minfo = minfo;
        addInfo.mDesc = desc;

        mWarningData.add(addInfo);
    }

    public void SetSelectNumber(int idx) {
        mSelectedRadioPosition = idx;
    }

    private class WarningViewHolder {
        public TextView WarningText;
        public TextView WarningTextDesc;
        public RadioButton WarningRadio;
    } //알람 리스트뷰의 아이템 홀더

    public class WarningData {

        public String minfo; // 체크박스 이름
        public String mDesc; // 체크박스 설명
    }
}
