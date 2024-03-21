package edu.fspt.demo2224_cloudserver;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    static public int zigbee_port = 6003;
    static public String newporter_ip = "172.18.1.15";
    static public String iot_ip = "172.18.1.19";
    static public int iot_port = 502;
    static public String item_actor[] = {"绿灯","LED灯","风扇"};
    static public String item_sensor[] = {"温度","湿度","光照","人体"};
    static public String item_op[] = {"大于","小于","等于","不等于"};

    static public int do_green = 2;
    static public int do_fan = 1;
    static public int do_led = 3;

//    static List<Setting> settings = new ArrayList() {
//            new Setting("串口服务器", "ip_newporter"),
//            new Setting("IoT采集器", "ip_iot"),
//            new Setting("IoT采集器端口", "port_iot"),
//            new Setting("ZigBee端口", "zigbeeport_newporter"),
//            new Setting("三色灯（绿）", "do_green"),
//            new Setting("风扇", "do_fan")
//    };
}
