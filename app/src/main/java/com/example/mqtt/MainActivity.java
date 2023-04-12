package com.example.mqtt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.BufferedReader;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;


//开一个线程用于日期的更新  yes
//用alert对话框设置报警    yes
//主活动完成mqtt的连接、注册, 利用线程获取消息 yes
//测试数据报警
//主进程传数据（实时时间和传感器数据到第二个活动）
//加入数据库的访问
//传输其他数据进副活动完成chart绘制
public class MainActivity extends AppCompatActivity
        implements View.OnClickListener,DialogInterface.OnClickListener {
    /*************************UI控件*********************************/
    private TextView tx_o2;  //用于溶解氧数据显示
    private TextView tx_ph;  //用于PH的数据显示
    private TextView tx_temp; //用于获取温度数据
    private TextView tx_tds; //获取电导率数据
    private Handler mhandler;//利用handler多线程处理
    private TextView txv_time;//在时间文本框上显示时间
    private String time_line;//时间线程
    public static float temp_val_f, ph_val_f, o2_val_f, tds_val_f;//检测数据,以全局数据传输
    final float max_O2 = 5;  //保存传感器的阈值,用于报警
    public int WARNING_SP=0;                                 //报警标志
    /************************** 设备三元组信息 **************************/
    final private String PRODUCTKEY = "ibbkznChKyz";
    final private String DEVICENAME = "apps";
    final private String DEVICESECRET = "73556a3dab1f6ce4b9fd7725d962c2b8";
    /*************************阿里云Mqtt服务器域名************* */
    final String host = "tcp://" + PRODUCTKEY + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:443";
    private String clientId;
    private String userName;
    private String passWord;

    MqttAndroidClient mqttAndroidClient;
    /* 自动Topic, 用于上报消息 */
    final private String PUB_TOPIC = "/sys/" + PRODUCTKEY + "/" + DEVICENAME + "/thing/event/property/post";
    // final private String PUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/update";
    /* 自动Topic, 用于接受消息 */
    final private String SUB_TOPIC = "/sys/" + PRODUCTKEY + "/" + DEVICENAME + "/thing/service/property/set";
    private String TAG = "Mainactivity";//调试信息
    private Bundle j_bundle;           //Json格式数据
    private Bundle bundle_time;         //实时对象
    private Intent intent;
    public static String rel_time;   //全局变量
    private DateTime date;
    public static String formatTime;
    //final private String SUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/get";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //主线程用于获取实时时间，并传值到其他activity，UI的更新
        //UI初始化
        try {
            initUI();
            Time_updata();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //多线程用于数据的获取
        //连接mqtt
        try {
            mqttInit();
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        mhandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0://mqtt连接成功
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        break;
                    case 1://获取实时时间
                        rel_time = msg.getData().getString("time");
                        //  int int_count = msg.getData().getInt("count");
                        Log.i("MainActivity", rel_time);
                        txv_time.setText(rel_time);
                        //主线程不进行耗时操作，故在多线程传输数据
                        break;
                    case 2: //MQTT连接失败
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        break;

                    case 3://消息到来解析数据
                        String J_data_Json = msg.getData().getString("J_data");
                        try {
                            JSONObject jsonObject = new JSONObject(J_data_Json);      //新建一个Json对象
                            String par = jsonObject.getString("items"); //将items键值转为json对象

                            JSONObject jsonObject_temp = new JSONObject(par);
                            String temper = jsonObject_temp.getString("temperature");//获取温度下键值
                            JSONObject jsonObject2 = new JSONObject(temper);
                            temp_val_f = (float) jsonObject2.getDouble("value");

                            String ph_val_s = jsonObject_temp.getString("pH");      //获取PH下键值
                            JSONObject second_ph_json = new JSONObject(ph_val_s);
                            ph_val_f = (float) second_ph_json.getDouble("value");

                            String o2_val_s = jsonObject_temp.getString("O2");       //获取溶解氧的值
                            JSONObject second_O2_json = new JSONObject(o2_val_s);
                            o2_val_f = (float) second_O2_json.getDouble("value");

                            String tds_val_s = jsonObject_temp.getString("TDS");    //获取电导率的值
                            JSONObject second_tds_json = new JSONObject(tds_val_s);
                            tds_val_f = (float) second_tds_json.getDouble("value");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //数据显示
                        tx_tds.setText(String.valueOf(tds_val_f));
                        tx_o2.setText(String.valueOf(o2_val_f));
                        tx_temp.setText(String.valueOf(temp_val_f));
                        tx_ph.setText(String.valueOf(ph_val_f));
                        //数据报警判断,这里以相等作为判断
                        if(o2_val_f==max_O2)
                            WARNING_SP = 1;
                        switch (WARNING_SP) {
                            case 0:
                                break;
                            case 1:
                                showdialog(0, "警告：溶解氧含量偏低");
                                WARNING_SP = 0;
                                break;
                            default:
                                break;
                        }
                        break;

                    case 4://订阅成功
                        Toast.makeText(MainActivity.this, "订阅成功", Toast.LENGTH_SHORT).show();
                        break;

                    case 5://订阅失败
                        Toast.makeText(MainActivity.this, "订阅失败", Toast.LENGTH_SHORT).show();
                        break;
                    case 6://发布成功
                        Toast.makeText(MainActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                        break;
                    case 7://发布失败
                        Toast.makeText(MainActivity.this, "发布失败", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    //时间更新函数，在多线程中处理耗时操作
    private void Time_updata() {
        new Thread(new Runnable() {



            @Override
            public void run() {  //子线程中获取时间，传输到主进程
                do {
                    Message time_MSG = Message.obtain();
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //通过bundle在线程中传输数据
                    time_line = DateUtil.now();  //获取实时时间保存在字符串中
                    date = DateUtil.parse(time_line); //解析实时时间
                    //时分秒的格式，设定为全局
                    formatTime = DateUtil.formatTime(date);
                    bundle_time = new Bundle();  //通过bundle发送到主线程
                    bundle_time.putString("time", time_line);
                    // bundle_time.putInt("count", count);
                    time_MSG.what = 1;
                    time_MSG.setData(bundle_time);//将实时数据放入消息队列，其他线程可以根据标识符号使用
                    mhandler.sendMessage(time_MSG);

                } while (true);
            }
        }).start();
    }

    //控件初始化，点击事件设置监听
    private void initUI() {
        tx_o2 = findViewById(R.id.tx_O);
        tx_ph = findViewById(R.id.tx_P);
        tx_temp = findViewById(R.id.tx_T);
        tx_tds = findViewById(R.id.tx_Z);
        txv_time = findViewById(R.id.txv_time);
        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
//        AlertDialog.Builder bdr = new AlertDialog.Builder(this);
//        //加入文字信息
//        bdr.setMessage("请连接MQTT \n"+"连接完成后将完成水质检测");
//        bdr.setTitle("欢迎");//加入标题
//        bdr.setIcon(android.R.drawable.btn_star_big_on);
//        bdr.setCancelable(true);
//        bdr.show();
    }

    //对话框用于报警,当数据超出阈值时就会生成报警信息
    public void showdialog(int flag, String msg) {
        //定义一个新的对话框对象
        AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder(this);
        //设置可点击对话框外 对话框是否关闭
        alertdialogbuilder.setCancelable(false);//允许按返回键关闭对话框
        //设置对话框提示内容
        switch (flag) {
            case 0:
                alertdialogbuilder.setMessage(msg);//加入文字信息
                alertdialogbuilder.setPositiveButton("打开增氧机", this);
                alertdialogbuilder.setNegativeButton("忽略", this);
                break;
            case 1:
                alertdialogbuilder.setMessage("警告：溶解氧含量偏低");
                alertdialogbuilder.setNegativeButton("确定", null);
                break;
            default:
                break;
        }
        //创建并显示对话框
        AlertDialog alertdialog1 = alertdialogbuilder.create();
        alertdialog1.show();
    }
    //按钮点击模拟报警事件
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn1://模拟报警模式2
                showdialog(0, "警告：溶解氧含量偏低");
                break;
            case R.id.btn2://模拟报警模式一
                showdialog(1, "Warnning");
                break;
            case R.id.btn3://进入第二个活动，可视化数据
                intent = new Intent(MainActivity.this,chart.class);
                intent.putExtras(bundle_time);//将数据传到下一个活动
                startActivity(intent);
                break;

            default:
                break;
        }
    }

    //对话框监听方法，处理对话框监听方案
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case DialogInterface.BUTTON_POSITIVE:
                Toast.makeText(this, "运行增氧", Toast.LENGTH_SHORT).show();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                Toast.makeText(this, "已忽略", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    //mqtt初始化
    private void mqttInit() throws MqttException {
        /* 获取Mqtt建连信息clientId, username, password */
        AiotMqttOption aiotMqttOption = new AiotMqttOption().getMqttOption(PRODUCTKEY, DEVICENAME, DEVICESECRET);
        if (aiotMqttOption == null) {
            Log.e(TAG, "device info error");
        } else {
            clientId = aiotMqttOption.getClientId();
            userName = aiotMqttOption.getUsername();
            passWord = aiotMqttOption.getPassword();
        }
        /* 创建MqttConnectOptions对象并配置username和password */
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(userName);
        mqttConnectOptions.setPassword(passWord.toCharArray());

        /* 创建MqttAndroidClient对象, 并设置回调接口 */
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), host, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG, "topic: " + topic + ", msg: " + new String(message.getPayload()));
                Message msg = new Message();
                msg.what = 3;//消息到了
                j_bundle = new Bundle();
                j_bundle.putString("J_data",message.toString());
                msg.setData(j_bundle);
                mhandler.sendMessage(msg);
                // Toast.makeText(MainActivity.this, message.toString(), Toast.LENGTH_SHORT).show();
//                try {
//                    String str = message.toString();
//                    JSONObject jsonObject = new JSONObject(str);      //新建一个Json对象
//                    String par = jsonObject.getString("items"); //将items键值转为json对象
//
//                    JSONObject jsonObject_temp = new JSONObject(par);
//
//                    String temper = jsonObject_temp.getString("temperature");//获取温度下键值
//                    JSONObject jsonObject2 = new JSONObject(temper);
//                    temp_val_f = (float) jsonObject2.getDouble("value");
//
//                    String ph_val_s = jsonObject_temp.getString("pH");           //获取PH下键值
//                    JSONObject second_ph_json = new JSONObject(ph_val_s);
//                    ph_val_f = (float) second_ph_json.getDouble("value");
//
//                    String o2_val_s = jsonObject_temp.getString("O2");
//                    JSONObject second_O2_json = new JSONObject(o2_val_s);
//                    o2_val_f = (float) second_O2_json.getDouble("value");
//
//                    String tds_val_s = jsonObject_temp.getString("TDS");
//                    JSONObject second_tds_json = new JSONObject(tds_val_s);
//                    tds_val_f = (float) second_tds_json.getDouble("value");
//
//                    tx_tds.setText(String.valueOf(tds_val_f));
//                    tx_o2.setText(String.valueOf(o2_val_f));
//                    tx_temp.setText(String.valueOf(temp_val_f));
//                    tx_ph.setText(String.valueOf(ph_val_f));
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG, "msg delivered");
            }
        });
        /* Mqtt建连 */
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "connect succeed");
                    Message msg = new Message();
                    msg.what = 0;     //连接成功
                    mhandler.sendMessage(msg);
                  //  Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    subscribeTopic(SUB_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "connect failed");
                    Message msg = new Message();
                    msg.what = 2;   //连接失败
                    mhandler.sendMessage(msg);
                  //  Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    /************************订阅特定的主题********************/
    public void subscribeTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "subscribed succeed");
                    Message msg = new Message();
                    msg.what = 4;   //订阅成功
                    mhandler.sendMessage(msg);
                   // Toast.makeText(MainActivity.this, "订阅成功", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "subscribed failed");
                    Message msg = new Message();
                    msg.what = 5;
                    mhandler.sendMessage(msg);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /***************** 向默认的主题/user/update发布消息*******************/
    public void publishMessage(String payload) {
        try {
            if (mqttAndroidClient.isConnected() == false) {
                mqttAndroidClient.connect();
            }
            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(PUB_TOPIC, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "publish succeed!");
                    Message msg = new Message();
                    msg.what = 6;
                    mhandler.sendMessage(msg);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "publish failed!");
                    Message msg = new Message();
                    msg.what = 7;
                    mhandler.sendMessage(msg);
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }
}
