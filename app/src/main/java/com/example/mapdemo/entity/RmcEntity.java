package com.example.mapdemo.entity;

/**
 * 定义RMC信号的实体类
 */
public class RmcEntity {
    public String time;
    public String status;
    public String latStr;
    public String latDir;
    public String lonStr;
    public String lonDir;
    public double speed;
    public double course;
    public String utcDateAndTime;

    public RmcEntity(String time, String status, String latStr, String latDir, String lonStr, String lonDir, double speed, double course, String utcDateAndTime) {
        this.time = time;
        this.status = status;
        this.latStr = latStr;
        this.latDir = latDir;
        this.lonStr = lonStr;
        this.lonDir = lonDir;
        this.speed = speed;
        this.course = course;
        this.utcDateAndTime = utcDateAndTime;
    }
}
