/*
* 2024省赛样题5，子任务5-2-4，RGB灯带控制
* 作者：弓长雨辰
* 2024.1.6
* 难点：
* 1.SeekBar控件的使用
* 2.RGB灯带的控制
* 3.RGB的三色原理
* */
package edu.fspt.demo5524_rgbled;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.nle.mylibrary.databus.DataBus;
import com.nle.mylibrary.databus.DataBusFactory;
import com.nle.mylibrary.device.RGBLed;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener{
    /*
    控件初定义：
    iv_led：灯光颜色显示
    sb_red：RGB的红色拖动条
    sb_green：RGB的绿色拖动条
    sb_blue：RGB的蓝色拖动条
    tv_al：RGB通道值
     */
    ImageView iv_led;
    SeekBar sb_red,sb_green,sb_blue;
    TextView tv_al;
    Switch sw_open;
    /*
    关键常量定义
     */
    String tv_str = "RGB通道值：";
    int color=0;
    String ip = "172.18.1.15";
    int port =6006;
    //RGB串口连接器
    RGBLed rgbLed;
    //RGB的ModBus设备地址
    int rgbaddr = 2;
    //存储RGB值的三个变量
    int red,green,blue = 0;

    //RGB连接器初始化
    void initRGB(){
        //TCP串口设置，ip+端口，设置TCP连接时记得要去AndroidManifest.xml中添加2个网络连接权限
        DataBus dataBus = DataBusFactory.newSocketDataBus(ip, port);
        //RGB连接器加载串口
        rgbLed = new RGBLed(dataBus, null);
        try {
            //初始化灯带颜色
            rgbLed.controlRGB(blue,green,red,rgbaddr,null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //控件初始化
    void init(){
        sb_red = findViewById(R.id.seekBar);
        //加载接口OnSeekBarChangeListener
        sb_red.setOnSeekBarChangeListener(this);
        sb_green = findViewById(R.id.seekBar2);
        //加载接口OnSeekBarChangeListener
        sb_green.setOnSeekBarChangeListener(this);
        sb_blue = findViewById(R.id.seekBar3);
        //加载接口OnSeekBarChangeListener
        sb_blue.setOnSeekBarChangeListener(this);
        tv_al = findViewById(R.id.textView);
        //通道值初始化
        tv_al.setText(tv_str+String.format("(%d,%d,%d)",red,green,blue));
        iv_led = findViewById(R.id.imageView);
        //灯光颜色初始化
        iv_led.setBackgroundColor(Color.rgb(red,green,blue));
        sw_open = findViewById(R.id.switch1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化
        init();
        initRGB();
    }

    /*
    seekbar的监听事件OnSeekBarChangeListener的接口，此接口一共3个方法。
    1、拖动条的进度值改变时，回调onProgressChanged，seekBar是被拖动的控件，i是被拖动后的值，b是的ture是人为拖动，false是代码自动拖动
    2、当触摸拖动条，回调onStartTrackingTouch，seekBar是被点击的控件
    3、当离开拖动条，回调onStopTrackingTouch, seekBar是被离开的控件
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        //获取seekbar的ID
        int id = seekBar.getId();
        /*
        判断是否红色，是则改变变量red
        否则再判断是否绿色，是则改变green
        都不是，则是蓝色，改变blue
        */
        if(id == sb_red.getId()) red = i;
        else if (id == sb_green.getId()) green = i;
        else blue = i;
        //设置通道值
        tv_al.setText(tv_str+String.format("(%d,%d,%d)",red,green,blue));
        //设置灯显示颜色
        iv_led.setBackgroundColor(Color.rgb(red,green,blue));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        try {
            //当松手完成设置时，判断开关是否打开，是则发送命令控制灯带颜色
            if(sw_open.isChecked()) {
                //连接器发送命令控制灯带颜色
                rgbLed.controlRGB(blue, green, red, rgbaddr, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
