package com.example.mapdemo.util;

/**
 * 地图工具Helpers
 */
public class MapTools {
    private static final double PI = 3.1415926535897932384626;
    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;

    /**
     * WGS84转GCJ02
     *
     * @param lat WGS84纬度
     * @param lon WGS84经度
     * @return GCJ02坐标
     */
    public static double[] wgs84ToGcj02(double lat, double lon) {
        if (outOfChina(lat, lon)) {
            return null;
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLat, mgLon};
    }

    /**
     * 判断是否在国内
     *
     * @param lat 纬度
     * @param lon 经度
     * @return 是否在国内
     */
    private static boolean outOfChina(double lat, double lon) {
        return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    /**
     * 计算纬度偏移量
     *
     * @param x x坐标
     * @param y y坐标
     * @return 纬度偏移量
     */
    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 计算经度偏移量
     *
     * @param x x坐标
     * @param y y坐标
     * @return 经度偏移量
     */
    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 将VDR格式的经纬度字符串转换为十进制小数
     * @param coordinate 坐标字符串
     * @param latOrLon 经纬度识别字符串
     * @return
     */
    public static double convertLatLonStrToDecimal(String coordinate, String latOrLon) {
        int degreeLength;
        if (latOrLon.equals("lat")) {
            degreeLength = 2;
        } else if(latOrLon.equals("lon")) {
            degreeLength = 3;
        } else {
            throw new IllegalArgumentException("Invalid latOrLon parameter");
        }

        // 检查输入是否为空
        if (coordinate == null || coordinate.isEmpty()) {
            throw new IllegalArgumentException("Coordinate string cannot be null or empty");
        }

        // 提取度部分（前两位或三位）
        String degreeStr = coordinate.substring(0, degreeLength);
        int degree = Integer.parseInt(degreeStr);

        // 提取分部分（剩下的部分）
        String minuteStr = coordinate.substring(degreeLength);
        double minuteDecimal = Double.parseDouble(minuteStr);

        // 转换为十进制度
        return degree + (minuteDecimal / 60.0);
    }

    /**
     * 计算两点间的距离
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 使用Haversine公式计算两点间距离（单位：米）
        double R = 6371e3; // 地球半径，单位：米
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // 距离，单位：米
    }

    /**
     * 计算UTC时间
     * @param timeStr
     * @param dateStr
     * @return
     */
    public static String calculateUTCTime(String timeStr, String dateStr) {
        try {
            String hh = timeStr.substring(0, 2);
            String mm = timeStr.substring(2, 4);
            String ss = timeStr.substring(4, 6);
            String dd = dateStr.substring(0, 2);
            String mmMonth = dateStr.substring(2, 4);
            String yy = dateStr.substring(4, 6);

            return String.format("20%s-%s-%s %s:%s:%s", yy, mmMonth, dd, hh, mm, ss);
        } catch (Exception e) {
            return "";
        }
    }
}
