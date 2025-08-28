package com.example.mapdemo.entity;

import static com.example.mapdemo.util.MapTools.convertLatLonStrToDecimal;

import android.util.Log;

import com.example.mapdemo.util.MapTools;
import com.mengbo.vdr.LocationEntity;

/**
 * 定义DR数据类
 */
public class DrData {
    public double latitude = -1;
    public double longitude = -1;
    public double alt;
    public float yaw;
    public float speed;
    public long timestamp;
    public String utcDate;
    public String utcTime;
    public int accValidAxes;
    public int accInterval;
    public double accYawRate;
    public double accPitchRate;
    public double accRollRate;
    public int gyroValidAxes;
    public int gyroInterval;
    public double gyroYawRate;
    public double gyroPitchRate;
    public double gyroRollRate;
    public double temperature;

    private transient boolean isLocationEntityReceived = false;
    private transient boolean isAccReceived = false;
    private transient boolean isGyroReceived = false;
    private transient boolean isTemperatureReceived = false;

    public void setLocationEntity(LocationEntity locationEntity) {
        // 将惯导WGS84字符坐标转换为十进制小数
        double wgs84lat = convertLatLonStrToDecimal(locationEntity.getLat(), "lat");
        double wgs84lon = convertLatLonStrToDecimal(locationEntity.getLon(), "lon");

        // 将惯导WGS84坐标转换为GCJ02坐标
        double[] gcj02 = MapTools.wgs84ToGcj02(wgs84lat, wgs84lon);
        if (gcj02 == null) {
            Log.e("MBVDR", "坐标不在中国境内");
            return;
        }

        this.latitude = gcj02[0];
        this.longitude = gcj02[1];
        this.alt = locationEntity.getAlt();
        this.yaw = locationEntity.getYaw();
        this.speed = locationEntity.getSpeed();
        this.utcDate = locationEntity.getUtcDate();
        this.utcTime = locationEntity.getUtcTime();

        isLocationEntityReceived = true;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setAcc(double x, double y, double z, int validAxes, int interval) {
        this.accYawRate = x;
        this.accPitchRate = y;
        this.accRollRate = z;
        this.accValidAxes = validAxes;
        this.accInterval = interval;

        isAccReceived = true;
    }

    public void setGyro(double x, double y, double z, int validAxes, int interval) {
        this.gyroYawRate = x;
        this.gyroPitchRate = y;
        this.gyroRollRate = z;
        this.gyroValidAxes = validAxes;
        this.gyroInterval = interval;

        isGyroReceived = true;
    }

    public void setTemp(double temp) {
        this.temperature = temp;

        isTemperatureReceived = true;
    }

    public boolean isComplete() {
        return isLocationEntityReceived && isAccReceived && isGyroReceived && isTemperatureReceived;
    }
}
