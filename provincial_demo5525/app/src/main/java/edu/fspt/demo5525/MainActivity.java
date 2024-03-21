/*
* 2024省赛样题5的5-2-5子任务，自动搅拌系统
* 作者：弓长雨辰
* 版本V1.0
* 时间2024.1.6
* 难点：
* 1、旋转动画
* 2、IoT数据采集
* 3、电机数据采集
* 4、推杆、接近开关、行程开关联动
* 5、消息更新主界面UI
* 毒点：
* 1、GenericConnector中的getTCPReadDI函数，没有数据长度检查，数据传输不稳定时会出现数组边界溢出错误。
* 解决办法：
* 直接进入GenericConnector中的getTCPReadDI的源码中，把原生数据提取出来
* */
package edu.fspt.demo5525;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nle.mylibrary.claimer.connector.ConnectorListener;
import com.nle.mylibrary.databus.DataBus;
import com.nle.mylibrary.databus.DataBusFactory;
import com.nle.mylibrary.device.GenericConnector;

public class MainActivity extends AppCompatActivity {

    /*
    * 视图控件：
    * iv_cycle：显示旋转动画的图像
    * tv_cycle_speed：显示转速
    * layout_alarm：警报显示布局
    * */
    ImageView iv_cycle;
    TextView tv_cycle_speed;
    RelativeLayout layout_alarm;
    /*
    * 关键常量
    * maxspeed：高速阈值
    * minspeed：低速阈值
    * iot_ip：iot的IP
    * iot_port：iot的端口
    * newporter_ip：串口服务器的IP
    * com4_port：串口服务器端口
    * speedunit：转速单位
    * do_xx，对应IOT采集器的DO口，推杆前进DO1，推杆后退DO2，红灯DO3，黄灯DO4，绿灯DO5
    * */
    int maxspeed=300, minspeed=150;
    String iot_ip = "172.18.1.18";
    int iot_port = 502;
    String newporter_ip = "172.18.1.15";
    int com4_port = 6004;
    String speedunit = "r/s";
    int do_foward = 1,do_back = 2,do_red =3 ,do_green = 5,do_yellow = 4;
    /*
    * 关键变量
    * iot1：IOT连接器
    * motor：电机调速器连接器
    * iot_databus：iot数据总线类
    * newporter_databus：串口服务器数据总线类
    * iot_di：IOT采集的DI缓存变量
    * keep：推杆动作暂存
    * speed_level：转速等级，1是低速，2是中速，3是高速
    * */
    GenericConnector iot1, motor;
    DataBus iot_databus, newporter_databus;
    int iot_di = 0;
    int keep = 0;
    int speed_level = 2;

    //UI更新的Handler
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //消息类型
            switch (msg.what){
                //动画更新信息
                case 1:
                    //设置旋转360度动画
                    ObjectAnimator animator = ObjectAnimator.ofFloat(iv_cycle,"rotation", 0, 360);
                    //设置完成动画所需要的时间（毫秒）
                    animator.setDuration(msg.getData().getInt("an_speed"));
                    //开始运行动画
                    animator.start();
                    //判断：alarm为true，显示警报信息；否则隐藏警报信息
                    if(msg.getData().getBoolean("alarm")) layout_alarm.setVisibility(View.VISIBLE);
                    else layout_alarm.setVisibility(View.INVISIBLE);
                    break;
                case 2:
                    //显示速度文本
                    tv_cycle_speed.setText(msg.getData().getString("tv_speed"));
                    break;
            }
        }
    };

    //连接器初始化
    void initConnector(){
        //初始化IOT采集器
        iot_databus = DataBusFactory.newSocketDataBus(iot_ip, iot_port);
        iot1 = new GenericConnector(iot_databus, null);
        //初始化串口服务器
        newporter_databus = DataBusFactory.newSocketDataBus(newporter_ip, com4_port);
        motor = new GenericConnector(newporter_databus, null);
    }

    //初始化推杆线程
    void initpusherThread(){
        Thread th_tuigan = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        //获取DI口数据
                        iot1.sendTCPReadDI(1, new ConnectorListener() {
                            @Override
                            public void onSuccess(boolean b) {
                                //毒点，必须采集长度为10的数据才是正确的。
                                byte[] buffer = iot_databus.getReceiveData();
                                if (buffer != null && buffer.length == 10) {
                                    /*
                                    提取DI信息buffer[9]，长度8bit，每一位代表一个DI数据
                                    例如字节是0x33，对应二进制00110011，即DI1,DI2,DI5,DI6为有信号
                                    */
                                    //只有DI1~3，因此与7进行掩码
                                    iot_di = buffer[9] & 7;
                                    //Log.d("debug5525_di", String.valueOf(iot_di));
                                }
                            }
                            @Override
                            public void onFail(Exception e) {
                            }
                        });
                        //推杆运行方向，0是停止，1前进，2后退
                        int dir = 0;
                        //判断DI3（微动开关）是否为1，
                        if((iot_di & 4) != 0){
                            //是打开红灯，推杆停止
                            iot1.sendTCPSetDoVlue(1, do_red, true, null);
                        }
                        else{
                            //微动开关为0，关闭红灯
                            iot1.sendTCPSetDoVlue(1, do_red, false, null);
                            //否则，判断DI2（接近开关）是否为1
                            if((iot_di&2) != 0){
                                //是则打开黄灯，关闭绿灯
                                iot1.sendTCPSetDoVlue(1, do_yellow, true, null);
                                iot1.sendTCPSetDoVlue(1, do_green, false, null);
                            }else{
                                //否则关闭黄灯，打开绿灯
                                iot1.sendTCPSetDoVlue(1, do_green, true, null);
                                iot1.sendTCPSetDoVlue(1, do_yellow, false, null);
                            }
                            //判断是否为低速
                            if(speed_level < 2) {
                                //低速，且DI1（行程开关）与DI2(接近开关)为0，推杆前进
                                if (iot_di == 0) dir = 1;
                                //低速，且DI1（行程开关）与DI2(接近开关)为1，推杆后退
                                if (iot_di == 3) dir = 2;
                                //低速，且DI1为0，接近开关为1，方向为保持
                                if (iot_di == 2) dir = keep;
                            }else{
                                //非低速。并且DI2（接近开关）为1，方向后退。
                                if((iot_di&2)!=0) dir = 3;
                            }
                        }
                        //判断运动方向
                        switch (dir){
                            default:
                                //停止，DO1与DO2为0
                                iot1.sendTCPSetDoVlue(1, do_foward, false, null);
                                iot1.sendTCPSetDoVlue(1, do_back, false, null);
                                break;
                            case 1:
                                //前进，DO1为1，DO2为0，设置保持方向为前进
                                keep = 1;
                                iot1.sendTCPSetDoVlue(1, do_back, false, null);
                                iot1.sendTCPSetDoVlue(1, do_foward, true, null);
                                break;
                            case 2:
                                //初次后退，需2500+500延迟3秒，DO1为0，DO2为1，设置保持方向为保持后退
                                Thread.sleep(2500);
                                keep = 3;
                            case 3:
                                //保持后退，不需要延时
                                iot1.sendTCPSetDoVlue(1, do_foward, false, null);
                                iot1.sendTCPSetDoVlue(1, do_back, true, null);
                                break;
                        }
                        //延时500毫秒，通信过快容易通信失真
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        //启动线程
        th_tuigan.start();
    }

    //电机转速获取
    void initGetSpeedThread(){
        //初始化电机获取连接器
        Thread th_motor = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        //电机速度获取
                        motor.sendEleGetSpeed(1, 1, new ConnectorListener() {
                            @Override
                            public void onSuccess(boolean b) {
                                //获取电机速度int型
                                int curspeed = motor.getEleSetSpeed();
                                //如果获取失败返回-1，curspeed为负数
                                if(curspeed >= 0) {
                                    //电机速度单位是r/min，因此需要除以60
                                    curspeed /= 60;
                                    //电机速度大于高速阈值，等级高速
                                    if (curspeed > maxspeed) speed_level = 3;
                                    else if (curspeed < minspeed) speed_level = 1; //小于低速阈值，等级低速
                                    else speed_level = 2; //在2个阈值之间，中速
                                    //发送消息到handler更新主界面UI
                                    Message message = new Message();
                                    //消息类型2
                                    message.what = 2;
                                    //消息附带的UI更新内容数据
                                    Bundle bundle = new Bundle();
                                    bundle.putString("tv_speed", String.valueOf(curspeed) + speedunit);
                                    message.setData(bundle);
                                    //发送消息
                                    handler.sendMessage(message);
                                }
                            }
                            @Override
                            public void onFail(Exception e) {

                            }
                        });
                        Thread.sleep(4000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        //启动线程
        th_motor.start();
    }

    //旋转动画线程
    void initCycleThread(){
        Thread cycle_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    //警报开关
                    boolean alarm = false;
                    //旋转速度
                    int spe = 2000;
                    //根据转速等级设置旋转速度
                    switch (speed_level){
                        case 1:
                            //4000毫秒
                            spe = 4000;break;
                        case 2:
                            //2000毫秒
                            spe = 2000;break;
                        case 3:
                            //警报开关打开
                            alarm = true;
                            //1000毫秒
                            spe = 1000;break;
                    }
                    //发送消息到handler更新主界面UI
                    Message msg = new Message();
                    //消息类型1
                    msg.what = 1;
                    //消息附带的UI更新内容数据
                    Bundle bundle = new Bundle();
                    bundle.putInt("an_speed", spe);
                    bundle.putBoolean("alarm", alarm);
                    msg.setData(bundle);
                    //发送数据
                    handler.sendMessage(msg);
                    try {
                        Thread.sleep(spe);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        //启动线程
        cycle_thread.start();
    }
    //控件初始化
    void init(){
        iv_cycle = findViewById(R.id.imageView);
        tv_cycle_speed = findViewById(R.id.textView2);
        layout_alarm = findViewById(R.id.relativelayout_alarm);
        layout_alarm.setVisibility(View.INVISIBLE);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化
        init();
        initConnector();
        initGetSpeedThread();
        initCycleThread();
        initpusherThread();
    }
}
