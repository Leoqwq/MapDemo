package com.example.mapdemo.entity;

import com.example.mapdemo.util.MapTools;

/**
 * 定义腾讯GPS模式回放数据类
 */
public class GpsData {
    public double latitude = -1; // GCJ02
    public double longitude = -1; // GCJ02
    public double alt;
    public double yaw;
    public double speed;
    public String utcDateAndTime;
    public long timestamp;

    private transient boolean isGgaReceived = false;
    private transient boolean isRmcReceived = false;

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setGga(GgaEntity gga) {
        // 转换GPS的WGS84经纬度坐标为GCJ02坐标
        double[] gcj02 = MapTools.wgs84ToGcj02(gga.lat, gga.lon);
        this.latitude = gcj02[0];
        this.longitude = gcj02[1];

        this.alt = gga.alt;

        isGgaReceived = true;
    }

    public void setRmc(RmcEntity rmc) {
        this.speed = rmc.speed;
        this.yaw = rmc.course;
        this.utcDateAndTime = rmc.utcDateAndTime;

        isRmcReceived = true;
    }

    public boolean isComplete() {
        return isGgaReceived && isRmcReceived;
    }
}
