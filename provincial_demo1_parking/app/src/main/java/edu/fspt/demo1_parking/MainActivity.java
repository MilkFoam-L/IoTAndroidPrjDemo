/**
 * 题目样题1停车场计费系统
 * 作者：弓长雨辰
 * 时间：2024.1.19
 * 版本：V1.0
 * 难点：
 * 1、UHF卡的EPC与车牌绑定
 * 2、车辆出场入场的逻辑
 * 3、语音
 * 4、推杆控制
 */
package edu.fspt.demo1_parking;

import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nle.mylibrary.claimer.connector.ConnectorListener;
import com.nle.mylibrary.claimer.rfid.SingleEpcListener;
import com.nle.mylibrary.databus.DataBus;
import com.nle.mylibrary.databus.DataBusFactory;
import com.nle.mylibrary.device.GenericConnector;
import com.nle.mylibrary.device.RFID;
import com.nle.mylibrary.device.listener.ConnectResultListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    /**
     * 常用常量
     * */
    //UHF的IP+接口和IOT的IP+接口
    String newport_ip = "192.168.1.200";
    String iot_ip = "192.168.1.254";
    int rfid_port = 6004;
    int iot_port = 502;

    /**
     * 常用变量
     */
    //行程开关DI量
    int DI_xingcheng = 0;
    //接近开关DI量
    int DI_jiejin = 0;
    //推杆前进DO口
    int DO_tuigan_qian = 1;
    //推杆后退DO口
    int DO_tuigan_hou = 2;
    //绿灯DO口
    int DO_green = 3;
    //红灯DO口
    int DO_red = 4;
    //RFID与IoT的连接bus
    DataBus rfidbus, iotbus;
    //rfid连接器
    RFID rfid;
    //IoT连接器
    GenericConnector iot;
    //用于存储入库时间的MAP
    Map<String, Long> parking = new HashMap<>();
    //用于查找EPC与车牌的MAP
    Map<String, String> carData = new HashMap<>();
    //语音类
    TextToSpeech textToSpeech;

    /**
     * 常用控件
     */
    //显示框
    TextView tv_carid, tv_intime, tv_outtime, tv_parktime, tv_money;
    //按钮
    Button btn_leave;

    //用于UI更新的handler
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //接收到消息后，就开始更新UI
            String [] info = msg.getData().getStringArray("info");
            tv_carid.setText(info[0]);
            tv_intime.setText("入场时间："+info[1]);
            tv_outtime.setText("出场时间："+info[2]);
            tv_parktime.setText("停车时长："+info[3]);
            tv_money.setText("应缴费用："+info[4]);
        }
    };

    /**
     * 用于UI更新发送的方法
     * @param info 显示的信息字符串
     */
    void ui_update(String [] info){
        //用于存储要显示的信息的bundle类
        Bundle bundle = new Bundle();
        //存储要显示的字符串
        bundle.putStringArray("info", info);
        //创建消息
        Message msg = new Message();
        //装载数据
        msg.setData(bundle);
        //发送消息
        handler.sendMessage(msg);
    }

    //将记录的EPC绑定车牌
    void initcar(){
        //清空车辆绑定map
        carData.clear();
        //绑定EPC与车牌，注意车牌不能有空格，否则播报时会把车牌号念成XX万XX千的数字
        carData.put("E3 2D 77 FC A1 20 15 09 25 00 D8 4F", "京A123456");
        carData.put("E3 2D 77 FC A1 20 15 09 25 00 D8 4D", "粤E987456");
        carData.put("E3 2D 77 FC A1 20 15 09 25 00 D8 4E", "粤A654321");
        //清空停车场MAP
        parking.clear();
    }

    //初始化控件
    void initwiget(){
        //初始化显示控件
        tv_carid = findViewById(R.id.tv_carid);
        tv_intime = findViewById(R.id.tv_intime);
        tv_outtime = findViewById(R.id.tv_outtime);
        tv_parktime = findViewById(R.id.tv_parktime);
        tv_money = findViewById(R.id.tv_money);
        //初始化按钮
        btn_leave = findViewById(R.id.btn_leave);
        btn_leave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //每次点击，创建一个线程完成推杆伸缩
                Thread th_iot = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //锁定按钮，在推杆完成前不能点击
                        btn_leave.setClickable(false);
                        //Log.d("debug_btn","in th_iot");
                        //初始化行程开关的DI值
                        DI_jiejin = 0;
                        try {
                            //推杆前进的DO设置为0
                            iot.sendTCPSetDoVlue(1, DO_tuigan_qian, false, null);
                            //推杆后退的DO设置为1
                            iot.sendTCPSetDoVlue(1, DO_tuigan_hou, true, null);
                            //循环检测接近开关是否为1，如果是1，则表示推杆还没有推到最短，保持推杆后退
                            while(DI_jiejin == 1){
                                //发送获取接近开关的DI值的请求
                                iot.sendTCPReadDI(1, new ConnectorListener() {
                                    @Override
                                    public void onSuccess(boolean b) {
                                        //请求成功，回调函数中获取接近开关的DI值
                                        DI_jiejin = iot.getTCPReadDI().getDI0();
                                    }

                                    @Override
                                    public void onFail(Exception e) {

                                    }
                                });
                                //Log.d("debug_tuigan","qianjin");
                                //休眠50毫秒，避免信号发送太多导致丢包
                                Thread.sleep(50);
                            }
                            //退出循环后，表示到达了最近端
                            //语音播报
                            textToSpeech.speak("一路平安", TextToSpeech.QUEUE_FLUSH, null, "");
                            //推杆后退DO设置为0
                            iot.sendTCPSetDoVlue(1, DO_tuigan_hou, false, null);
                            //绿灯DO设置为1
                            iot.sendTCPSetDoVlue(1, DO_green , true, null);
                            //红灯DO设置为0
                            iot.sendTCPSetDoVlue(1, DO_red, false, null);
                            //推杆在最短端休眠5秒
                            Thread.sleep(5000);
                            //推杆前进设置为1
                            iot.sendTCPSetDoVlue(1, DO_tuigan_qian, true, null);
                            //绿灯关
                            iot.sendTCPSetDoVlue(1, DO_green , false, null);
                            //红灯关
                            iot.sendTCPSetDoVlue(1, DO_red, true, null);
                            //判断行程开关是否为0，是则表示推杆没有到达最远，保存前进
                            while(DI_jiejin == 0){
                                //发送行程DI的请求
                                iot.sendRtuReadDI(1, new ConnectorListener() {
                                    @Override
                                    public void onSuccess(boolean b) {
                                        //获取行程DI
                                        DI_jiejin = iot.getTCPReadDI().getDI1();
                                    }

                                    @Override
                                    public void onFail(Exception e) {

                                    }
                                });
                                //休眠，防数据请求过快
                                Thread.sleep(50);
                                //Log.d("debug_tuigan","houtui");
                            }
                            //停止推杆的后退
                            iot.sendTCPSetDoVlue(1, DO_tuigan_hou, false, null);
                            //停止推杆的前进
                            iot.sendTCPSetDoVlue(1, DO_tuigan_qian, false, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d("debug_btn","out th_iot");
                        //恢复按钮可以点击
                        btn_leave.setClickable(true);
                    }
                });
                //启动线程
                th_iot.start();
            }
        });
    }

    /**
     * RFID初始化
     */
    void initRFID(){
        //串口初始化
        rfidbus = DataBusFactory.newSocketDataBus(newport_ip, rfid_port);
        //连机器初始化
        rfid = new RFID(rfidbus, null);
        //创建一个线程读取RFID卡
        Thread th_rfid = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        //读取卡ID
                        rfid.readSingleEpc(new SingleEpcListener() {
                            @Override
                            public void onVal(String s) {
                                //以下语句可用于记录EPC卡的卡号
                                //Log.d("debug_rfid", s);
                                //读取成功，判断是入场还是出场
                                //如果停车map为空，或者卡号不存在于停车map，则是入场。否则else出场
                                if (parking.isEmpty() || (!parking.containsKey(s))) {
                                    //记录入场的卡EPC以及入场的时间戳
                                    parking.put(s, System.currentTimeMillis());
                                    //Log.d("debug_rfid", s + " 入场");
                                    //播报绑定EPC的车牌号
                                    textToSpeech.speak("欢迎入场，"+carData.get(s), TextToSpeech.QUEUE_FLUSH, null, "");
                                }else{
                                    //日期显示格式化
                                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    //字符串信息,0是车牌，1是入场，2是出场，3是时长，4是费用，
                                    String [] info = new String[5];
                                    //保存车牌信息
                                    info[0] = carData.get(s);
                                    //获取车辆入场时保存的时间戳
                                    long l_in = parking.get(s);
                                    //存储格式化时间
                                    info[1] = format.format(new Date(l_in));
                                    //获取当前出场时间
                                    long l_out = System.currentTimeMillis();
                                    //存储格式化时间
                                    info[2] = format.format(new Date(l_out));
                                    //Log.d("debug_rfid", s + " 出场");
                                    //计算时间，单位为分钟，而时间戳的单位是毫秒。1000毫秒=1秒，60秒=1分钟，因此要除以1000*60
                                    long mins = (l_out - l_in)/(1000*60);
                                    //如果不足一分钟，按照一分钟算
                                    if(mins<1) mins++;
                                    //转为停车时长的字符串，小时数=分钟数/60，而剩下的分钟则是分钟%60
                                    info[3] = String.valueOf(mins/60) + "小时"+String.valueOf(mins%60)+"分";
                                    //每分钟10元
                                    info[4] = mins*10+"元";
                                    //发送消息更新UI
                                    ui_update(info);
                                    //出场的车辆信息移除出停车map
                                    parking.remove(s);
                                }
                            }
                            @Override
                            public void onFail(Exception e) {
                                //如果没有卡在读会包超时信息，而且卡不用一直放在UHF，容易读取失败
                                Log.d("debug_rfid", e.toString());
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        //每10秒读一次卡
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        //启动线程
        th_rfid.start();
    }

    //初始化IoT
    void initIoT(){
        //初始化串口
        iotbus = DataBusFactory.newSocketDataBus(iot_ip, iot_port);
        //初始化iot连接器
        iot = new GenericConnector(iotbus, new ConnectResultListener() {
            @Override
            public void onConnectResult(boolean b) {
                //如果连接成功，初始化DO口
                if(b){
                    Log.d("debug_iot", "success");
                    try {
                        //打开红灯
                        iot.sendTCPSetDoVlue(1, DO_red , true, null);
                        //打开绿灯
                        iot.sendTCPSetDoVlue(1, DO_green, false, null);
                        //推杆前进关闭
                        iot.sendTCPSetDoVlue(1, DO_tuigan_qian, false, null);
                        //推杆后退关闭
                        iot.sendTCPSetDoVlue(1, DO_tuigan_hou,false, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else Log.d("debug_iot", "failed");
            }
        });
    }

    //语言模块初始化
    void initTts(){
        //语音模块初始化
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                //如果初始化成功
                if(i == TextToSpeech.SUCCESS){
                    //设置中文语音包
                    textToSpeech.setLanguage(Locale.CHINESE);
                    textToSpeech.speak("语音模块初始化成功", TextToSpeech.QUEUE_FLUSH, null, "");
                    //textToSpeech.speak("欢迎入场，"+ carData.get("E3 2D 77 FC A1 20 15 09 25 00 D8 4D"), TextToSpeech.QUEUE_FLUSH, null, "");
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化
        initcar();
        initwiget();
        initTts();
        //初始化连接器前一定要注意开启网络权限
        /**
         *     <uses-permission android:name="android.permission.INTERNET"/>
         *     <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
         */
        //RFID波特率115200
        initRFID();
        initIoT();
    }
}
