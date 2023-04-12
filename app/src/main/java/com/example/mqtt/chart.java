package com.example.mqtt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.hutool.core.date.DateUtil;

public class chart extends AppCompatActivity implements View.OnClickListener {

    private List<String> timeList = new ArrayList<>();//x轴的时间
    //将当前时间设置进Entry
    private TextView txv_time2;
    private Handler my2handler;
    private Bundle bundle;

    private LineChart lineChart; //生成线性图

    List<Entry> list = new ArrayList<>();

    List<Entry> list2 = new ArrayList<>();
    private float temp_val_f=0, ph_val_f=0, o2_val_f=0, tds_val_f=0;//接收全局数据
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        CT_initUI();
        data_INIT();//显示从主活动传输过来的时间
        my2handler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 11:   //用于时间更新
                     String chart_time = msg.getData().getString("time");
                   txv_time2.setText(MainActivity.formatTime);

                     //   txv_time2.setText(MainActivity.rel_time);//从全局中获取数据
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void data_INIT() {

        new Thread(new Runnable() {
            @Override
            public void run() {
//                do {
//                    try {
//                        Thread.sleep(1000);   //多线程用于时间的更新显示
//                        Message msg = new Message();
//                        msg.what = 11;
//                        msg.setData(bundle);  //多线程发送bundle数据
//                        data_info();
//                        my2handler.sendMessage(msg);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                } while (true);
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = new Message();
                            msg.what = 11;
                            msg.setData(bundle);  //多线程发送bundle数据
                            data_info();
                            my2handler.sendMessage(msg);
//                            if( o2_val_f!= 0f)
//                            {
//                                addEntry();
//                            }
                            addEntry();
                        }
                    });
                }
            }

            private void data_info() {
                //完成数据的初始化
                temp_val_f = MainActivity.temp_val_f;
                ph_val_f = MainActivity.ph_val_f;
                o2_val_f = MainActivity.o2_val_f;
                tds_val_f = MainActivity.tds_val_f;
            }
        }).start();
    }

    private void CT_initUI() {
        txv_time2 = findViewById(R.id.txv_time2);
        findViewById(R.id.btn_re).setOnClickListener(this);
        findViewById(R.id.btn_add).setOnClickListener(this);
        //折线图对象初始化
        lineChart = (LineChart)findViewById(R.id.lineChart);
        Line_INIT();
        bundle = this.getIntent().getExtras(); //主活动传入bundle数据
    }

    private void Line_INIT() { //完成linechart的参数设置
        //lineChart.setDescription("123");
        lineChart.getDescription().setText("数据显示");
        lineChart.setNoDataText("无数据");

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);//可拖拽
        lineChart.setScaleEnabled(true);//可缩放
        lineChart.setDrawGridBackground(false);
        lineChart.setPinchZoom(true);
        //设置背景颜色
        lineChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        //数据显示颜色
        data.setValueTextColor(Color.WHITE);
        //先增加数据，之后动态添加

        //注解
        Legend l = lineChart.getLegend();
        //线性
        l.setForm(Legend.LegendForm.LINE);
        //颜色
        l.setTextColor(Color.WHITE);
        //x_axis
        XAxis x1 = lineChart.getXAxis();
        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(false);
        x1.setAvoidFirstLastClipping(true);
        //5个X坐标轴之间绘制
        x1.setSpaceMin(0f);
        x1.setLabelCount(5);
      //  x1.setValueFormatter(new Myaxis(null));
        x1.setValueFormatter(new ValueFormatter() {
//            @SuppressLint("SimpleDateFormat")
//            SimpleDateFormat mFormat = new SimpleDateFormat("hh:mm:ss");
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                Log.i("Main",String.valueOf(value));
//                return mFormat.format(new Date((long) value));
                return timeList.get((int)value%timeList.size());
            }
        });
        x1.setGranularity(1f);
        x1.setGranularityEnabled(true);
        //x轴可见
        x1.setEnabled(true);
        //将默认顶部的X轴放置于底部
        x1.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis l_Axis = lineChart.getAxisLeft();
        l_Axis.setTextColor(Color.WHITE);
        l_Axis.setAxisMaximum(30f);
        l_Axis.setAxisMinimum(0f);
        l_Axis.setDrawGridLines(true);
        //不显示右边y轴
        YAxis r_Axis = lineChart.getAxisRight();
        r_Axis.setEnabled(false);

        lineChart.setData(data);
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_re:
                finish();//回到主活动
                break;
            case R.id.btn_add:
                addEntry();
                break;
            default:
                break;
        }
    }
    //添加数据
    private void addEntry() {
        timeList.add(MainActivity.formatTime);
        LineData data = lineChart.getData();
        //linsset-一类数据的曲线，从下标0条开始
        LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
        //无数据集，创建一个
        if (set == null) {
            set = creatLineDataSet();
            data.addDataSet(set);
        }
        //添加x坐标轴的值，从0开始

//        float f = (float) ((Math.random())*5+20);
//        Entry entry = new Entry(set.getEntryCount(),f);//获得统计图上所有数据点的值
        Entry entry = new Entry(set.getEntryCount(),o2_val_f);//获得统计图上所有数据点的值
        //添加点
        data.addEntry(entry,0);//一条曲线，下标为0

        lineChart.notifyDataSetChanged();//类似listview更新数据
        //设置图上最多显示数据
        lineChart.setVisibleXRangeMaximum(5);//x轴最多显示五个数据
        //刷新
        lineChart.moveViewToX(data.getEntryCount()-5);
    }
    //添加统计曲线
    private LineDataSet creatLineDataSet() {
        LineDataSet set = new LineDataSet(null,"动态数据");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);//
        //color
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(1f);
        set.setFillAlpha(128);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.GREEN);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);
        set.setDrawValues(true);
        //set.setDrawCircleHole(false);//ture:空心，false:实心
        return set;
    }
}