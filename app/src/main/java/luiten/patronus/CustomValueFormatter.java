package luiten.patronus;

import android.icu.text.DecimalFormat;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Created by LG on 2017-08-10.
 */

public class CustomValueFormatter implements ValueFormatter{

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        int value2 = (int) value;

        return value2 + "";
    }
}
