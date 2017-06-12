package luiten.patronus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LG on 2017-06-09.
 */

public class ItemArrayAdapter extends ArrayAdapter {

    private List<String[]> logList = new ArrayList<String[]>();

    static class ItemViewHolder {
        TextView date;
        TextView time;
        TextView type;
    }

    public ItemArrayAdapter (Context context, int resource) {
        super(context, resource);
    }

    public void add(String[] object) {
        logList.add(object);
        super.add(object);
    }

    @Override
    public int getCount() {
        return  this.logList.size();
    }

    @Override
    public String[] getItem(int position) {
        return this.logList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View row = convertView;
        ItemViewHolder viewHolder;
        if(row == null){
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService((Context.LAYOUT_INFLATER_SERVICE));
            row = inflater.inflate(R.layout.record_log, parent, false);
            viewHolder = new ItemViewHolder();
            viewHolder.date = (TextView)row.findViewById(R.id.log_date);
            viewHolder.time = (TextView)row.findViewById(R.id.log_time);
            viewHolder.type = (TextView)row.findViewById(R.id.log_type);
            row.setTag(viewHolder);
        } else {
            viewHolder = (ItemViewHolder)row.getTag();
        }
        String[] stat = getItem(position);
        viewHolder.date.setText(stat[0]);
        viewHolder.time.setText(stat[1]);
        viewHolder.type.setText(stat[2]);
        return row;
    }
}
