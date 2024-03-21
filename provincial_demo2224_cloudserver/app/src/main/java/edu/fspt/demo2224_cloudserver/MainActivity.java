package edu.fspt.demo2224_cloudserver;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.nle.mylibrary.claimer.connector.ConnectorListener;
import com.nle.mylibrary.databus.DataBus;
import com.nle.mylibrary.databus.DataBusFactory;
import com.nle.mylibrary.databus.ReciveData;
import com.nle.mylibrary.device.GenericConnector;
import com.nle.mylibrary.device.ZigBee;
import com.nle.mylibrary.device.ZigBee3;
import com.nle.mylibrary.enums.zigBee.ZigBeeSensorType;

public class MainActivity extends AppCompatActivity {
    Spinner sensor1,sensor2,op1,op2,actor1,actor2;
    EditText et_value1,et_value2;
    Switch sw_actor1, sw_actor2, sw_logic;
    ImageView iv1, iv2;
    Button btn_setting;
    TextView tv_temp,tv_humi,tv_light,tv_inf;
    GenericConnector iot_conn;
    int val_temp = 0, val_humi= 0, val_light = 0, val_inf = 0;

    DataBus zigbeebus, iotbus;
    boolean isAuto = false;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    tv_temp.setText(String.valueOf(msg.obj)+"℃");
                    break;
                case 2:
                    tv_humi.setText(String.valueOf(msg.obj)+"%");
                    break;
                case 3:
                    tv_light.setText(String.valueOf(msg.obj)+"lx");
                    break;
                case 4:
                    if(((int)msg.obj) == 0){
                        tv_inf.setText("有人");
                    }else{
                        tv_inf.setText("无人");
                    }
                    break;
                case 5:
                    iv1.setBackground(getDrawable((int)msg.obj));
                    break;
                case 6:
                    iv2.setBackground(getDrawable((int)msg.obj));
                    break;
            }
        }
    };

    void ui_update(int what, Object obj){
        Message message = new Message();
        message.what = what;
        message.obj = obj;
        handler.sendMessage(message);
    }

    int getVal(Spinner spinner){
        switch (spinner.getSelectedItemPosition()){
            case 0:
                return val_temp;
            case 1:
                return val_humi;
            case 2:
                return val_light;
            case 3:
                return val_inf;
        }
        return -1;
    }

    void control_actor(int channel, boolean isOpen){
        Spinner spinner = null;
        int DOx = 0;
        int drawable = 0;
        int what = 0;
        switch (channel){
            case 1:
                spinner = actor1;
                what = 5;
                break;
            case 2:
                spinner = actor2;
                what = 6;
                break;
        }
        switch(spinner.getSelectedItemPosition()){
            case 0:
                DOx = Utils.do_green;
                if(isOpen) drawable = R.drawable.green_open;
                else drawable = R.drawable.green_close;
                break;
            case 1:
                DOx = Utils.do_led;
                if(isOpen){
                    drawable = R.drawable.led_open;
                }else{
                    drawable = R.drawable.led_close;
                }
                break;
            case 2:
                DOx = Utils.do_fan;
                if(isOpen){
                    drawable = R.drawable.fan_open;
                }else{
                    drawable = R.drawable.fan_close;
                }
                break;
        }
        try {
            iot_conn.sendTCPSetDoVlue(1, DOx, isOpen, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ui_update(what, drawable);
    }

    boolean judge(EditText et, Spinner op, int val){
        boolean isOpen = false;
        String str = et.getText().toString().trim();
        int thros = Integer.parseInt(str);
        switch (op.getSelectedItemPosition()){
            case 0:
                isOpen = val > thros;
                break;
            case 1:
                isOpen = val < thros;
                break;
            case 2:
                isOpen = val == thros;
                break;
            case 3:
                isOpen = val != thros;
                break;
        }
        return isOpen;
    }

    boolean isOpenActor(int channel){
        Spinner sp_sensor = null;
        Spinner sp_op = null;
        EditText et = null;
        switch (channel){
            case 1:
                sp_sensor = sensor1;
                sp_op = op1;
                et = et_value1;
                break;
            case 2:
                sp_sensor = sensor2;
                sp_op = op2;
                et = et_value2;
                break;
        }
        return judge(et,sp_op,getVal(sp_sensor));
    }

    void initAuto(){
        Thread auto = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean j1,j2;
                while(true){
                    if(isAuto){
                        //channel1:
                        j1 = isOpenActor(1);
                        control_actor(1, j1);
                        //sw_actor1.setChecked(j1);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //channel2:
                        j2 = isOpenActor(2);
                        control_actor(2, j2);
                        //sw_actor1.setChecked(j2);
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        auto.start();
    }

    void initIoT(){
        iotbus = DataBusFactory.newSocketDataBus(Utils.iot_ip, Utils.iot_port);
        iot_conn = new GenericConnector(iotbus, null);
        final Thread th_iot = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        iot_conn.sendTCPgetIOTVirtData(1, 2, new ConnectorListener() {
                            @Override
                            public void onSuccess(boolean b) {
                                val_light = iot_conn.getTCPIOTVirtData();
                                ui_update(3, val_light);
                            }
                            @Override
                            public void onFail(Exception e) {

                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th_iot.start();
    }

    void initZigBee(){
        zigbeebus = DataBusFactory.newSocketDataBus(Utils.newporter_ip, Utils.zigbee_port);
        zigbeebus.setReciveDataListener(new ReciveData() {
            @Override
            public String getReciveData(byte[] bytes) {
                if(bytes.length > 17){
                    int type = bytes[17];
                    switch (type){
                        case 1:
                            if(bytes.length>21){
                                ZigBee3 zigBee3 = new ZigBee3(bytes);
                                val_temp = (int)zigBee3.getVal0();
                                ui_update(1, val_temp);
                                val_humi = (int)zigBee3.getVal1();
                                ui_update(2, val_humi);
                            }
                            break;
                        case 17:
                            if(bytes.length>18){
                                ZigBee3 zigBee3 = new ZigBee3(bytes);
                                val_inf = (int)zigBee3.getVal0();
                                ui_update(4, val_inf);
                            }
                            break;
                    }
                }
                return null;
            }
        });
        zigbeebus.setup(null);
    }


    void settingSpinner(Spinner sp, String [] str){
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, str);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    void initwidget(){
        sensor1 = findViewById(R.id.sp_sensor1);
        settingSpinner(sensor1, Utils.item_sensor);
        sensor2 = findViewById(R.id.sp_sensor2);
        settingSpinner(sensor2,Utils.item_sensor);
        op1 = findViewById(R.id.sp_op1);
        settingSpinner(op1, Utils.item_op);
        op2 = findViewById(R.id.sp_op2);
        settingSpinner(op2, Utils.item_op);
        actor1 = findViewById(R.id.sp_actor1);
        settingSpinner(actor1, Utils.item_actor);
        actor2 = findViewById(R.id.sp_actor2);
        settingSpinner(actor2, Utils.item_actor);
        et_value1 = findViewById(R.id.et_value1);
        et_value2 = findViewById(R.id.et_value2);
        sw_actor1 = findViewById(R.id.sw_actor1);
        sw_actor1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!isAuto) {
                    try {
                        control_actor(1, b);
                        //iot_conn.sendTCPSetDoVlue(1, code, b, null);
                    }catch (Exception e){

                    }
                }
            }
        });
        sw_actor2 = findViewById(R.id.sw_actor2);
        sw_actor2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!isAuto) {
                    try {
                        control_actor(2, b);
                        //iot_conn.sendTCPSetDoVlue(1, code, b, null);
                    }catch (Exception e){

                    }
                }
            }
        });
        sw_logic = findViewById(R.id.sw_logic);
        sw_logic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isAuto = b;
            }
        });
        iv1 = findViewById(R.id.iv1);
        iv2 = findViewById(R.id.iv2);
        btn_setting = findViewById(R.id.btn_setting);
        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });
        tv_temp = findViewById(R.id.tv_temp);
        tv_humi = findViewById(R.id.tv_humi);
        tv_light = findViewById(R.id.tv_light);
        tv_inf = findViewById(R.id.tv_inf);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initwidget();
        initZigBee();
        initIoT();
        initAuto();
    }
}
