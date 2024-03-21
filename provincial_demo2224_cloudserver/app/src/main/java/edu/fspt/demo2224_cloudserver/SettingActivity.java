package edu.fspt.demo2224_cloudserver;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class SettingActivity extends AppCompatActivity {
    ListView listView;
    Button save, cancel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        ArrayList<Setting> arrayList = new ArrayList<>();
        arrayList.add(new Setting("串口服务器IP", "newporter_ip", "192.168.14.200"));
        arrayList.add(new Setting("UHF射频读写器端口", "uhf_port", "6001"));
        arrayList.add(new Setting("ADAM端口", "adam_port", "6002"));
        arrayList.add(new Setting("LED显示屏端口","screen_port", "6003"));
        arrayList.add(new Setting("ZigBee端口","zigbee_port","6004"));
        arrayList.add(new Setting("三色灯（绿）", "green_port", "DO1"));
        arrayList.add(new Setting("风扇", "fan_port","DO2"));
        listView = findViewById(R.id.listview);
        save = findViewById(R.id.botton3);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        cancel = findViewById(R.id.button4);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        SettingAdpter adpter = new SettingAdpter(SettingActivity.this, R.layout.setting, arrayList);
        listView.setAdapter(adpter);
    }
}
