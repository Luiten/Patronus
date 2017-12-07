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
    private ArrayList<RecordingData> mRecordingData = new ArrayList <RecordingData>();
    //알람 아이템들의 정보값들을 배열로 저장 리스트 포지션 하나당 들어갈 것들

    public Manual_RecordingAdapter(Context mContext){
        super();
        this.mContext = mContext;
    }//생성자

    @Override
    public int getCount() {
        return mRecordingData.size();
    } //리스트뷰 포지션의 개수가 배열의 수만큼 나오게한다

    @Override
    public Object getItem(int position) {
        return mRecordingData.get(position);
    } //각 리스트 포지션마다 배열의 저장된 데이터 반환

    @Override
    public long getItemId(int position) {
        return position;
    } //해당 포지션 반환

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final RecordingViewHolder holder;
        if(convertView == null) {
            holder = new RecordingViewHolder();

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.manual_recording_item, null);

            holder.recording_thumbnail = (ImageView)convertView.findViewById(R.id.manual_recording_thumbnail);
            holder.recording_date = (TextView)convertView.findViewById(R.id.manual_recording_date);
            holder.recording_desc = (TextView)convertView.findViewById(R.id.manual_recording_desc);

            //홀더에 저장되있는 것들을 이제 view에 뿌려준다
            convertView.setTag(holder);
        } else {
            holder = (RecordingViewHolder)convertView.getTag();
        }

        RecordingData mData = mRecordingData.get(position);

        Bitmap bmThumbnail;
        bmThumbnail = ThumbnailUtils.createVideoThumbnail(mData.mPath, MediaStore.Images.Thumbnails.MINI_KIND);

        holder.recording_thumbnail.setImageBitmap(bmThumbnail);
        holder.recording_date.setText(mData.mName);
        holder.recording_desc.setText(mData.mDesc);

        return convertView;
    }

    public void addItem(String path, String name, String desc) {
        RecordingData addInfo = new RecordingData();

        addInfo.mPath = path;
        addInfo.mName = name;
        addInfo.mDesc = desc;

        mRecordingData.add(addInfo);
    }

    private class RecordingViewHolder {
        public ImageView recording_thumbnail;
        public TextView recording_date;
        public TextView recording_desc;
    } //알람 리스트뷰의 아이템 홀더

    public class RecordingData {

        public String mPath; // 비디오 경로
        public String mName; // 비디오 이름
        public String mDesc; // 비디오 설명
    }
}
