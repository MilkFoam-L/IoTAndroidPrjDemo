/**
 * 2024样题6的Android样例
 * 作者：弓长雨辰
 * 时间2024.1.17
 * 版本V1.0
 * 难点：
 * 1、摄像头的开关
 * 2、摄像头方向调整
 * 3、云平台控制LED
 * 4、时间范围判断
 * 5、时间范围策略开关灯
 *
 */
package edu.fspt.demo6_camera;

import android.icu.text.SimpleDateFormat;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.tplink.applibs.util.TPByteArrayJNI;
import com.tplink.sdk.tpopensdk.TPOpenSDK;
import com.tplink.sdk.tpopensdk.TPPlayer;
import com.tplink.sdk.tpopensdk.TPSDKContext;
import com.tplink.sdk.tpopensdk.common.TPSDKCommon;
import com.tplink.sdk.tpopensdk.openctx.IPCDevice;
import com.tplink.sdk.tpopensdk.openctx.IPCDeviceContext;
import com.tplink.sdk.tpopensdk.openctx.IPCReqListener;
import com.tplink.sdk.tpopensdk.openctx.IPCReqResponse;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Date;


import cn.com.newland.nle_sdk.requestEntity.SignIn;
import cn.com.newland.nle_sdk.responseEntity.User;
import cn.com.newland.nle_sdk.responseEntity.base.BaseResponseEntity;
import cn.com.newland.nle_sdk.util.NCallBack;
import cn.com.newland.nle_sdk.util.NetWorkBusiness;

public class MainActivity extends AppCompatActivity {
    /**
     * 关键常亮定义。
     */
    //云平台地址，比赛时用的是IP地址
    String url = "http://api.nlecloud.com/";
    //设备ID
    String deviceid = "910044";
    //传感器/执行器标识名
    String apitag = "m_led";
    //开灯和关灯的字符串
    String led_open = "开灯";
    String led_close = "关灯";
    /**
     * 关键变量定义
     */
    //云平台访问对象
    NetWorkBusiness netWorkBusiness;
    //摄像头状态标识符
    boolean isCamOpen =false;
    //led状态标识符
    boolean isLedOpen = false;
    //开灯与关灯的时间区间的边界值,st_date是左边界,ed_date是右边界
    Date st_date,ed_date;
    //时间只用到时和分，因此时间格式化设置为"hh:mm"
    SimpleDateFormat format = new SimpleDateFormat("hh:mm");
    /**
     * 控件定义
     */
    FrameLayout cameraLayout;
    ImageButton shang,xia,zuo,you,setting;
    //sw_cam是摄像头开关按钮，sw_led是手动开关灯按钮
    Switch sw_cam,sw_led;
    //灯光状态图片
    ImageView iv_led;
    //灯光状态文本
    TextView tv_led;
    //用于显示设置控件的布局
    RelativeLayout relativeLayout;
    //设置时间区间的左右边界的EditText
    EditText starttime, endtime;
    /**
     * 摄像头关键对象定义
     */
    //TPLINK的SDK上下文
    TPSDKContext tpsdkContext;
    //摄像头设备上下文
    IPCDeviceContext devctx;
    //摄像头设备对象
    IPCDevice dev;
    //摄像头播放器
    TPPlayer tpPlayer;
    //摄像头镜头坐标，x是的负是左，正是右，y的负是下，正是上
    int x = 0,y = 0;

    //界面更新handler，用于界面更新
    Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //判断接收的消息类型，1是开关灯的消息，2是开关摄像头的消息，3是提示信息
            switch (msg.what){
                case 1:
                    isLedOpen= (boolean)msg.obj;
                    //如果是开灯
                    if (isLedOpen) {
                        //led的状态图片和文本都设置为打开
                        tv_led.setText(led_open);
                        iv_led.setBackground(getDrawable(R.drawable.led_open));
                        //发送开灯指令到云平台，4个参数分别为：1、设备ID；2、执行器标识名；3、开关数据（1为打开，0为关闭）；4、回调函数（构造函数需要参数MainActivity.this）
                        netWorkBusiness.control(deviceid, apitag, 1, new NCallBack<BaseResponseEntity>(MainActivity.this) {
                            @Override
                            protected void onResponse(BaseResponseEntity response) {
                            }
                        });
                    }else{
                        //否则，led的状态图片和文本都设置为关闭
                        tv_led.setText(led_close);
                        iv_led.setBackground(getDrawable(R.drawable.led_close));
                        //发送关灯指令到云平台
                        netWorkBusiness.control(deviceid, apitag, 0, new NCallBack<BaseResponseEntity>(MainActivity.this) {
                            @Override
                            protected void onResponse(BaseResponseEntity response) {
                            }
                        });
                    }
                    break;
                case 2:
                    isCamOpen = (boolean)msg.obj;
                    //如果打开摄像头
                    if (isCamOpen) {
                        //显示设置布局，隐藏手动操作按钮
                        relativeLayout.setVisibility(View.VISIBLE);
                        sw_led.setVisibility(View.INVISIBLE);
                        //打开摄像头
                        initPlayer();
                    }else{
                        //否则，隐藏设置布局，显示手动操作按钮
                        relativeLayout.setVisibility(View.INVISIBLE);
                        sw_led.setVisibility(View.VISIBLE);
                        //关闭摄像头
                        release();
                    }
                    break;
                case 3:
                    //用toast显示文字，可以用于调试or报错
                    //这里主要是用于提示输入的时间格式不正确
                    String text = (String) msg.obj;
                    Toast.makeText(MainActivity.this,text, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    //消息发送函数，用于快速发送消息
    void send_ui_update_message(int what, Object obj){
        //实例化消息对象
        Message message = new Message();
        //设置消息类型
        message.what = what;
        //设置消息内容
        message.obj = obj;
        //发送消息
        handler.sendMessage(message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //控件定义
        init();
        //云平台定义
        initcloud();
        //摄像头定义
        initConn();
        //线程定义
        initThread();
    }

    void init(){
        /**
         * 控件定义
         */
        //用于摄像头显示的FrameLayout布局
        cameraLayout = findViewById(R.id.camera);
        //按钮，镜头上下左右按钮;
        shang = findViewById(R.id.shang);
        shang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //y轴正方向移动10
                y+=10;
                move();
            }
        });
        //按钮，镜头上下左右按钮;
        xia = findViewById(R.id.xia);
        xia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //y轴负方向移动10
                y-=10;
                move();
            }
        });
        //按钮，镜头上下左右按钮;
        zuo = findViewById(R.id.zuo);
        zuo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //x轴负方向移动10
                x-=10;
                move();
            }
        });
        //按钮，镜头上下左右按钮;
        you = findViewById(R.id.you);
        you.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //x轴正方向移动10
                x+=10;
                move();
            }
        });
        //摄像头打开按钮
        sw_cam = findViewById(R.id.sw_cam);
        sw_cam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //如果按钮为打开，b为ture，否则b为false，则发送消息2到handler处，内容是b
                send_ui_update_message(2, b);
            }
        });
        //自动设置布局
        relativeLayout = findViewById(R.id.auto_led);
        //默认状态下隐藏自动设置布局
        relativeLayout.setVisibility(View.INVISIBLE);
        //时间区间设置按钮
        setting = findViewById(R.id.ibtn_setting);
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //点击此按钮后，获取输入的时间，开灯时间设置为st，关灯时间设置为ed
                String st = starttime.getText().toString();
                String ed = endtime.getText().toString();
                try {
                    //将输入的字符串解析为Date类
                    st_date = format.parse(st);
                    ed_date = format.parse(ed);
                } catch (ParseException e) {
                    //解析失败则发送报错格式信息
                    send_ui_update_message(3,"输入时间格式不正确");
                }
            }
        });
        //手动开关
        sw_led = findViewById(R.id.sw_led);
        sw_led.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //如果按钮为打开，b为ture，否则b为false，则发送消息1到handler处，内容是b
                send_ui_update_message(1, b);
            }
        });
        //初始化LED状态显示控件，包括图片和文本
        iv_led = findViewById(R.id.iv_led);
        tv_led = findViewById(R.id.tv_led);
        //开灯时间编辑框
        starttime = findViewById(R.id.tv_open_setting);
        //关灯时间编辑框
        endtime = findViewById(R.id.tv_close_setting);
    }

    void initThread(){
        //自动开关灯线程
        Thread autoled = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //设置默认时间区间为18；00-6：00
                    st_date = format.parse("18:00");
                    ed_date = format.parse("6:00");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                while(true){
                    try {
                        //如果打开摄像头，则打开自动开关灯按钮
                        if (isCamOpen) {
                            //提取当前时间戳的时和分
                            long lnow = System.currentTimeMillis() % (1000 * 60 * 60 * 24);
                            //创建当前时分的Date类
                            Date now = new Date(lnow);
                            //如果当前时间是在开灯时间前并且在关灯时间后，关灯；否则，开灯
                            if (now.before(st_date) && now.after(ed_date)) {
                                //消息1，内容false为关灯
                                send_ui_update_message(1, false);
                            } else {
                                //消息1，内容false为开灯
                                send_ui_update_message(1,true);
                            }
                        }

                        //休眠3秒
                        Thread.sleep(3000);
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        //启动线程
        autoled.start();
    }

    //云平台定义
    void initcloud(){
        //创建一个用于登录的networkbusiness对象，参数1为accesstoken，但是初次登录没有，所以用""空字符串即可，参数2的url即为云平台的url，比赛用IP地址
        NetWorkBusiness login= new NetWorkBusiness("", url);
        //调用登录函数，参数1为登录对象，新建一个登录对象，对象构造参数1为账号名，参数2为密码名；回调函数为NCallBack
        login.signIn(new SignIn("13600232031", "1qaz2wsx"), new NCallBack<BaseResponseEntity<User>>(MainActivity.this) {
            @Override
            protected void onResponse(BaseResponseEntity<User> response) {
                //登录连接后，会调用此回调函数获得返回信息response，从返回信息中获得accesstoken
                String a = response.getResultObj().getAccessToken();
                //创建一个新的netWorkBusiness对象，带有accesstoken与url的，表示后面的netWorkBusiness对象用的都是此账号密码
                netWorkBusiness = new NetWorkBusiness(a, url);
            }
        });
    }

    //定义摄像头
    void initConn(){
        //获取TP-LINK的SDK上下文
        tpsdkContext = TPOpenSDK.getInstance().getSDKContext();
        //启动底层模块，参数true为同步启动
        tpsdkContext.appReqStart(true, null);
        //获取摄像头设备模块上下文对象
        devctx = tpsdkContext.getDevCtx();
        //初始化设备，这里采用的是http协议，因此是IP+80端口，并实例化摄像头设备对象
        dev = new IPCDevice(devctx.initDev("192.168.1.253", 80));
        //登录设备，用的admin账号名以及密码
        devctx.reqLogin(dev, "123456", new IPCReqListener() {
            @Override
            public int callBack(IPCReqResponse ipcReqResponse) {
                //请求连接
                devctx.reqConnectDev(dev, new IPCReqListener() {
                    @Override
                    public int callBack(IPCReqResponse ipcReqResponse) {
                        return 0;
                    }
                });
                return 0;
            }
        });
    }

    //打开摄像头
    void initPlayer(){
        //请求获取视频接口
        devctx.reqGetVideoPort(dev, new IPCReqListener() {
            @Override
            public int callBack(IPCReqResponse ipcReqResponse) {
                //请求后回调函数
                /**
                 * 设置初始化播放器，需要创建播放器，并且设置framelaoyout作为摄像头画面的播放器
                 */
                tpPlayer = TPOpenSDK.getInstance().createPlayer(MainActivity.this).setViewHolder(cameraLayout);
                //设置解码模式为只用软解码
                tpPlayer.setDecodeMode(TPSDKCommon.PlayerDecodeMode.TPPLAYER_DECODE_MODE_SOFTWARE_ONLY);
                //设置播放回调
                tpPlayer.setPlayerCallback(new TPPlayer.PlayerCallback() {
                    //播放状态改变时回调此函数
                    @Override
                    public int onPlayStatusChange(int i, int i1) { return 0; }
                    //录像状态改变时回调此函数
                    @Override
                    public int onRecordStatusChange(int i, int i1, String s) {
                        return 0;
                    }
                    //拍照时回调此函数
                    @Override
                    public int onSnapshot(int i, String s) {
                        return 0;
                    }
                    //播放录像（时间）改变时调用此函数
                    @Override
                    public int onPlayTimeUpdate(long l) {
                        return 0;
                    }
                    //录像时长的回调
                    @Override
                    public int onRecordDurationUpdate(long l) {
                        return 0;
                    }
                    //数据统计信息回调
                    @Override
                    public int onDataStatistics(long l, double v) {
                        return 0;
                    }
                    //切换清晰度回调
                    @Override
                    public int onChangeQuality(int i) {
                        return 0;
                    }
                    //实时码流数据回调
                    @Override
                    public int onDataRecv(TPByteArrayJNI tpByteArrayJNI) {
                        return 0;
                    }
                    //YUV数据回调
                    @Override
                    public int onYUVDataRecv(ByteBuffer[] byteBuffers, int[] ints, int i, int i1) {
                        return 0;
                    }
                });
                //开始预览，参数1是摄像头对象实例，
                tpPlayer.startRealPlay(dev, TPSDKCommon.Quality.QUALITY_FLUENCY);
                //把摄像头位置移动到初始点
                move();
                return 0;
            }
        });
    }

    //摄像头镜头移动函数
    void move(){
        //摄像头移动，参数1是摄像头实例对象，参数2是回调函数，参数3是移动到x轴的坐标，参数4是移动到的y轴坐标，参数5是通道号(-1为IPC)
        devctx.reqMotorMoveTo(dev, new IPCReqListener(){
            @Override
            public int callBack(IPCReqResponse ipcReqResponse) {
                return 0;
            }
        }, x, y, -1);
    }
    //关闭摄像头
    void release(){
        //停止播放
        tpPlayer.stop();
        //释放播放实例
        tpPlayer.release();
    }
}
