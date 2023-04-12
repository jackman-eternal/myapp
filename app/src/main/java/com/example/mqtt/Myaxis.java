package com.example.mqtt;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
//
//public class Myaxis extends IAxisValueFormatter {
//
//    public String getXValue(String dateInMillisecons, int index, ViewPortHandler viewPortHandler) {
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM");
//
//            return sdf.format(new Date(Long.parseLong(dateInMillisecons)));
//        } catch (Exception e) {
//            return dateInMillisecons;
//        }
//
//    }
//}
//
//class Myaxis implements IAxisValueFormatter {
//    private String[] mValues;
//
//    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd:hh:mm:ss");
//
//    public Myaxis(String[] values) {
//        this.mValues = values; }
//
//    @Override
//    public String getFormattedValue(float value, AxisBase axis) {
//        return sdf.format(new Date((long) value));
//    }
//}

class Myaxis extends ValueFormatter {
    private String[] mValues ;
    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
    public Myaxis(String[] values) {
        this.mValues = values; }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return sdf.format(new Date((long) value));
    }

}
