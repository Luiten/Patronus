package luiten.patronus;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by LG on 2017-07-06.
 */

public class Manual_RecordingAdapter extends BaseAdapter {
    private Context mContext = null;
    private ArrayList<AlarmData> mAlarmData = new ArrayList <AlarmData>();
    //알람 아이템들의 정보값들을 배열로 저장 리스트 포지션 하나당 들어갈 것들

    public Manual_RecordingAdapter(Context mContext){
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

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final AlarmViewHolder holder;
        if(convertView == null) {
            holder = new AlarmViewHolder();

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.manual_recording_item, null);

            holder.recording_date = (TextView)convertView.findViewById(R.id.manual_recording_date);
            holder.recording_thumbnail = (ImageView)convertView.findViewById(R.id.manual_recording_thumbnail);

            //홀더에 저장되있는 것들을 이제 view에 뿌려준다
            convertView.setTag(holder);
        } else {
            holder = (AlarmViewHolder)convertView.getTag();
        }

        AlarmData mData = mAlarmData.get(position);

        Bitmap bmThumbnail;
        bmThumbnail = ThumbnailUtils.createVideoThumbnail(mData.minfo, MediaStore.Images.Thumbnails.MINI_KIND);

        holder.recording_thumbnail.setImageBitmap(bmThumbnail);
        holder.recording_date.setText(mData.mDesc);

        return convertView;
    }

    public void addItem(String minfo, String desc) {
        AlarmData addInfo = null;
        addInfo = new AlarmData();

        addInfo.minfo = minfo;
        addInfo.mDesc = desc;

        mAlarmData.add(addInfo);
    }

    private class AlarmViewHolder {
        public TextView recording_date;
        public ImageView recording_thumbnail;
    } //알람 리스트뷰의 아이템 홀더

    public class AlarmData {

        public String minfo; // 체크박스 이름
        public String mDesc; // 체크박스 설명
    }
}
