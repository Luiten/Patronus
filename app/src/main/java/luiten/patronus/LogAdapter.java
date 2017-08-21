package luiten.patronus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by LG on 2017-07-20.
 */

// 출처: http://arabiannight.tistory.com/entry/%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9CAndroid-ExpandableListView-%EB%A7%8C%EB%93%A4%EA%B8%B0
public class LogAdapter extends BaseExpandableListAdapter{

    private ArrayList<String> groupList = null;
    private ArrayList<ArrayList<String>> childTime = null;
    private ArrayList<ArrayList<String>> childDesc = null;
    private LayoutInflater inflater = null;
    private ViewHolder viewHolder = null;

    public LogAdapter(Context c, ArrayList<String> groupList, ArrayList<ArrayList<String>> childTime, ArrayList<ArrayList<String>> childDesc){
        super();
        this.inflater = LayoutInflater.from(c);
        this.groupList = groupList;
        this.childTime = childTime;
        this.childDesc = childDesc;
    }

    // 그룹 포지션을 반환한다.
    @Override
    public String getGroup(int groupPosition) {
        return groupList.get(groupPosition);
    }

    // 그룹 사이즈를 반환한다.
    @Override
    public int getGroupCount() {
        return groupList.size();
    }

    // 그룹 ID를 반환한다.
    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    // 그룹뷰 각각의 ROW
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        View v = convertView;

        if(v == null){
            viewHolder = new ViewHolder();
            v = inflater.inflate(R.layout.record_log_date, parent, false);
            viewHolder.tv_groupName = (TextView) v.findViewById(R.id.record_log_date);
            v.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)v.getTag();
        }

        viewHolder.tv_groupName.setText(getGroup(groupPosition));

        return v;
    }


    // 차일드뷰 중 childDesc를 반환한다.  ** 사용하지 않음
    @Override
    public String getChild(int groupPosition, int childPosition) {
        return childDesc.get(groupPosition).get(childPosition);
    }

    // 차일드뷰 childDesc 사이즈를 반환한다.
    @Override
    public int getChildrenCount(int groupPosition) {
        return childDesc.get(groupPosition).size();
    }

    // 차일드뷰 ID를 반환한다.
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    // 차일드뷰 각각의 ROW
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        View v = convertView;

        if(v == null){
            viewHolder = new ViewHolder();
            v = inflater.inflate(R.layout.record_log_content, null);
            viewHolder.tv_childTime = (TextView) v.findViewById(R.id.record_log_time);
            viewHolder.tv_childDesc = (TextView) v.findViewById(R.id.record_log_content);
            v.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)v.getTag();
        }

        viewHolder.tv_childTime.setText(childTime.get(groupPosition).get(childPosition));
        viewHolder.tv_childDesc.setText(childDesc.get(groupPosition).get(childPosition));

        return v;
    }

    @Override
    public boolean hasStableIds() { return true; }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) { return true; }

    class ViewHolder {
        public TextView tv_groupName;
        public TextView tv_childTime;
        public TextView tv_childDesc;
    }

}
