package com.example.mapdemo.entity;

/**
 * 定义GGA信号的实体类
 */
public class GgaEntity {
    String time;
    double lat; // WGS84
    String latDir;
    double lon; // WGS84
    String lonDir;
    String quality;
    String satellites;
    String hdop;
    double alt; // WGS84
    String geoid;

    public GgaEntity(String time, double lat, String latDir, double lon, String lonDir, String quality, String satellites, String hdop, double alt, String geoid) {
        this.time = time;
        this.lat = lat;
        this.latDir = latDir;
        this.lon = lon;
        this.lonDir = lonDir;
        this.quality = quality;
        this.satellites = satellites;
        this.hdop = hdop;
        this.alt = alt;
        this.geoid = geoid;
    }
}
