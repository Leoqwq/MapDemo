package com.example.mapdemo;

import static com.example.mapdemo.util.MapTools.convertLatLonStrToDecimal;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.app.AlertDialog;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import com.example.mapdemo.entity.DrData;
import com.example.mapdemo.entity.GgaEntity;
import com.example.mapdemo.entity.RmcEntity;
import com.example.mapdemo.entity.GpsData;
import com.example.mapdemo.util.MapTools;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mengbo.mbclient.MBServiceInstance;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    MapView mapView;
    AMap aMap;
    long startTime;

    /**
     * 打点抽稀相关变量
     */
    private static final long UPDATE_INTERVAL = 3000; // 打点最小时间间隔（毫秒）
    private static final double MIN_DISTANCE_DELTA = 2.0; // 最小移动距离（米）
    private long lastVdrUpdateTime = 0;
    double lastAmapLat = -1;
    double lastAmapLon = -1;
    double lastVdrLat = -1;
    double lastVdrLon = -1;

    /**
     * 是否需要检测后台定位权限，设置为true时，如果用户没有给予后台定位权限会弹窗提示
     */
    private boolean needCheckBackLocation = false;

    /**
     * 如果设置了target > 28，需要增加这个权限，否则不会弹出"始终允许"这个选择框
     */
    private static final String BACK_LOCATION_PERMISSION = "android.permission.ACCESS_BACKGROUND_LOCATION";

    /**
     * 原生GPS监听
     */
    private LocationManager locationManager;
    private LocationListener locationListener;

    /**
     * GNSS信号监听
     */
    private OnNmeaMessageListener nmeaListener;

    /**
     * 传感器监听
     */
    private SensorManager sensorManager;
    private long lastUpdateAcc = 0;
    private long lastUpdateGyr = 0;
    private long lastUpdateTemp = 0;
    private static final int SENSOR_UPDATE_INTERVAL = 100; // 100毫秒更新一次

    /**
     * 当前时间的GPS数据
     */
    private static GpsData currGpsData = new GpsData();

    /**
     * 保存所有GPS数据
     */
    private List<GpsData> gpsDataList = new ArrayList<>();

    /**
     * 当前时间的DR相关传感器数据
     */
    private DrData currDrData = new DrData();

    /**
     * 保存所有DR相关传感器数据
     */
    private List<DrData> drDataList = new ArrayList<>();

    /**
     * 数据计数器
     */
    private int numOfDrData = 0;
    private int numOfGpsData = 0;

    /**
     * 惯导是否初始化成功
     */
    private boolean isVdrEnable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startTime = System.currentTimeMillis();

        // 进行高德地图的隐私政策合规检查
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);

        // 设置需要请求的权限
        if (Build.VERSION.SDK_INT > 28
                && getApplicationContext().getApplicationInfo().targetSdkVersion > 28) {
            needPermissions = new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    BACK_LOCATION_PERMISSION
            };
            needCheckBackLocation = true;
        }

        setContentView(R.layout.activity_main);

        // 初始化按钮
        Button saveButton = findViewById(R.id.btn_save_data);
        Button amapReplayButton = findViewById(R.id.btn_amap_replay);
        Button tencentReplayButton = findViewById(R.id.btn_tencent_replay);
        Button clearMapButton = findViewById(R.id.btn_clear_map);
        Button exitButton = findViewById(R.id.btn_exit);
        saveButton.setOnClickListener(v -> {
            if (isVdrEnable) {
                saveGpsData();
                saveDrData();
            } else {
                saveGpsData();
            }
        });
        amapReplayButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AmapReplayActivity.class));
            this.finish();
        });
        tencentReplayButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TencentReplayActivity.class));
            this.finish();
        });
        clearMapButton.setOnClickListener(v -> {
            clearMap();
        });
        exitButton.setOnClickListener(v -> {
            this.finish();
        });

        // 初始化高德地图
        amapInit(savedInstanceState);

        // 初始化盟博VDR
        MBServiceInstance.init(this, (i, s) -> {
            if (i == 0) {
                addOnVdrListener();
                isVdrEnable = true;

                // 重置GPS数据集来保证GPS和惯导的数据同步
                gpsDataList = new ArrayList<>();
                numOfGpsData = 0;

                Toast toast = Toast.makeText(MainActivity.this, "MBVDR初始化成功", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                isVdrEnable = false;

                Toast toast = Toast.makeText(MainActivity.this, "MBVDR初始化失败: " + s, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
                toast.show();
            }
        });

        // 初始化GPS监听
        initGpsListener();

        // 初始化NMEA信号监听
        initNmeaListener();

        // 初始化传感器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        sensorManager.registerListener(sensorEventListener, accelerometer, SENSOR_UPDATE_INTERVAL);
        sensorManager.registerListener(sensorEventListener, gyroscope, SENSOR_UPDATE_INTERVAL);
        sensorManager.registerListener(sensorEventListener, temperature, SENSOR_UPDATE_INTERVAL);
    }

    /***************************************服务初始化******************************************************/
    private void initNmeaListener() {
        nmeaListener = (nmea, timestamp) -> {
            Log.d("NMEA SIGNAL", nmea);

            // 提取 GGA、GSV 和 RMC 信号
            if (nmea.startsWith("$GNGGA")) {
                GgaEntity gga = parseGGA(nmea);
                currGpsData.setGga(gga);
            } else if (nmea.startsWith("$GPGSV")) {

            } else if (nmea.startsWith("$GNRMC")) {
                RmcEntity rmc = parseRMC(nmea);
                currGpsData.setRmc(rmc);
            }

            if (isVdrEnable) {
                checkAndAddDataSync();
            } else {
                checkAndAddGpsData();
            }
        };

        // 注册 NMEA 监听器
        try {
            locationManager.addNmeaListener(nmeaListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void initGpsListener() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // 记录原生GPS坐标
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                long timestamp = System.currentTimeMillis() - startTime;

                // 打印日志（可选）
                Log.d("NATIVE_GPS", "Recorded native GPS: Lat=" + latitude + ", Lon=" + longitude);
                Log.d("TIME_STAMP", "Recorded timestamp: " + timestamp);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        // 请求GPS位置更新
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3, locationListener);
        }
    }

    private void amapInit(Bundle savedInstanceState) {
        // 初始化地图
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        aMap = mapView.getMap();

        amapMyLocatoinInit();
    }

    private void amapMyLocatoinInit() {
        if (aMap == null) {
            return;
        }

        // 初始化视角跟随开关
        SwitchCompat centerSwitch = findViewById(R.id.switch_center);

        centerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 初始化定位蓝点
            MyLocationStyle myLocationStyle;
            myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
            if (!isChecked) {
                myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，地图依照设备方向旋转，并且蓝点会跟随设备移动。
                Toast.makeText(this, "关闭视角跟随", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "开启视角跟随", Toast.LENGTH_LONG).show();
            }
            myLocationStyle.interval(UPDATE_INTERVAL); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
            myLocationStyle.showMyLocation(true);
            aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
            aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
            aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        });

        // 首次初始化时默认为跟随视角
        centerSwitch.setChecked(true);

        aMap.addOnMyLocationChangeListener(location -> {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            LatLng latLng = new LatLng(lat, lon);

            // 打点抽稀
            double distanceDelta = MapTools.calculateDistance(lastAmapLat, lastAmapLon, lat, lon);

            if (lastAmapLat != -1 && lastAmapLon != -1) {
                if (distanceDelta < MIN_DISTANCE_DELTA) {
                    Log.d("AMAP_LOCATION", "Skipped point due to distance delta: " + distanceDelta);
                    return;
                }
            }

            // 高德实时打点
            MarkerOptions markerOption = new MarkerOptions();
            markerOption.position(latLng);
            markerOption.title("Amap");
            // markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_amap));
            aMap.addMarker(markerOption);
            Log.d("AMAP_LOCATION", "Added point: Lat=" + lat + ", Lon=" + lon);

            // 更新最后坐标
            lastAmapLat = lat;
            lastAmapLon = lon;
        });
    }

    private void addOnVdrListener() {
        MBServiceInstance.getInstance().addOnVdrListener(locationEntity -> {
            double currentTime = System.currentTimeMillis();
            Log.d("zqh", "Lat: " + locationEntity.getLat() + ", Lon: " + locationEntity.getLon());

            if (locationEntity.getLat() == null || locationEntity.getLon() == null) {
                Log.d("MBVDR", "Vdr回调经纬度为null");
                return;
            }

            // 保存惯导数据
            currDrData.setLocationEntity(locationEntity);

            checkAndAddDataSync();

            // 将惯导WGS84字符坐标转换为十进制小数
            double wgs84lat = convertLatLonStrToDecimal(locationEntity.getLat(), "lat");
            double wgs84lon = convertLatLonStrToDecimal(locationEntity.getLon(), "lon");

            // 将惯导WGS84坐标转换为GCJ02坐标
            double[] gcj02 = MapTools.wgs84ToGcj02(wgs84lat, wgs84lon);
            if (gcj02 == null) {
                Log.e("MBVDR", "坐标不在中国境内");
                return;
            }

            // Log.d("MBVDR", "Added point: Lat=" + gcj02[0] + ", Lon=" + gcj02[1]);

            // 打点抽稀
            double distanceDelta = MapTools.calculateDistance(lastAmapLat, lastAmapLon, gcj02[0], gcj02[1]);

            if (lastVdrLat != -1 && lastVdrLon != -1) {
                if (currentTime - lastVdrUpdateTime < UPDATE_INTERVAL || distanceDelta < MIN_DISTANCE_DELTA) {
                    Log.d("MBVDR", "Skipped point due to time delta: " + (currentTime - lastVdrUpdateTime) + ", distance delta: " + distanceDelta);
                    return;
                }
            }


            // 惯导实时打点
            MarkerOptions markerOption = new MarkerOptions();
            markerOption.position(new LatLng(gcj02[0], gcj02[1]));
            markerOption.title("MBVdr");
            markerOption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            aMap.addMarker(markerOption);

            // 更新最后打点时间
            lastVdrUpdateTime = System.currentTimeMillis();

            // 更新最后坐标
            lastVdrLat = gcj02[0];
            lastVdrLon = gcj02[1];
        });
    }

    /********************************************数据保存******************************************************/

    /**
     * 保存GPS数据
     */
    private void saveGpsData() {
        // 重新赋值防止写入过程中数组被修改
        List<GpsData> tempArr = gpsDataList;

        try {
            // 转换为JSON字符串
            Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().create();
            String json = gson.toJson(tempArr);

            // 定义文件保存路径
            File dir = getApplicationContext().getFilesDir();
            File file = new File(dir, "gps_data.json");

            // 写入文件
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(json);
            fileWriter.flush();
            fileWriter.close();

            // 打印保存成功的日志
            Toast.makeText(this, "GPS回放数据已保存至：" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Log.d("GPS", "Gps data saved to: " + file.getAbsolutePath());
            Log.d("GPS", "JSON content: " + json);
            Log.d("Test", "Num of Dr data: " + numOfDrData);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("GPS", "Failed to save gps data", e);
        }
    }

    /**
     * 保存惯导数据
     */
    private void saveDrData() {
        // 重新赋值防止写入过程中数组被修改
        List<DrData> tempArr = drDataList;

        try {
            // 转换为JSON字符串
            Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().create();
            String json = gson.toJson(tempArr);

            // 定义文件保存路径
            File dir = getApplicationContext().getFilesDir();
            File file = new File(dir, "dr_data.json");

            // 写入文件
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(json);
            fileWriter.flush();
            fileWriter.close();

            // 打印保存成功的日志
            Toast.makeText(this, "惯导数据已保存至：" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Log.d("DR", "DR data saved to: " + file.getAbsolutePath());
            Log.d("DR", "JSON content: " + json);
            Log.d("Test", "Num of GPS data: " + numOfGpsData);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("DR", "Failed to save DR data", e);
        }
    }

    private void clearMap() {
        aMap.clear();
        gpsDataList = new ArrayList<>();
        drDataList = new ArrayList<>();
    }

    /***************************************Activity状态更新回调*************************************************/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mapView.onDestroy();
        MBServiceInstance.unregisterInstance();

        // 移除GPS监听
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }

        // 移除NMEA监听
        if (locationManager != null && locationListener != null) {
            locationManager.removeNmeaListener(nmeaListener);
        }

        // 移除传感器监听
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        System.exit(0);
    }


    /***************************************权限检查******************************************************/

    /**
     * 需要进行检测的权限数组
     */
    protected String[] needPermissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            BACK_LOCATION_PERMISSION
    };

    private static final int PERMISSON_REQUESTCODE = 0;

    /**
     * 判断是否需要检测，防止不停的弹框
     */
    private boolean isNeedCheck = true;

    @Override
    protected void onResume() {
        try {
            super.onResume();
            mapView.onResume();

            if (Build.VERSION.SDK_INT >= 23) {
                if (isNeedCheck) {
                    checkPermissions(needPermissions);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * @param
     * @since 2.5.0
     */
    @TargetApi(23)
    private void checkPermissions(String... permissions) {
        try {
            if (Build.VERSION.SDK_INT >= 23 && getApplicationInfo().targetSdkVersion >= 23) {
                List<String> needRequestPermissonList = findDeniedPermissions(permissions);
                if (null != needRequestPermissonList
                        && needRequestPermissonList.size() > 0) {
                    try {
                        String[] array = needRequestPermissonList.toArray(new String[needRequestPermissonList.size()]);
                        Method method = getClass().getMethod("requestPermissions", new Class[]{String[].class, int.class});
                        method.invoke(this, array, 0);
                    } catch (Throwable e) {

                    }
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取权限集中需要申请权限的列表
     *
     * @param permissions
     * @return
     * @since 2.5.0
     */
    @TargetApi(23)
    private List<String> findDeniedPermissions(String[] permissions) {
        try {
            List<String> needRequestPermissonList = new ArrayList<String>();
            if (Build.VERSION.SDK_INT >= 23 && getApplicationInfo().targetSdkVersion >= 23) {
                for (String perm : permissions) {
                    if (checkMySelfPermission(perm) != PackageManager.PERMISSION_GRANTED
                            || shouldShowMyRequestPermissionRationale(perm)) {
                        if (!needCheckBackLocation
                                && BACK_LOCATION_PERMISSION.equals(perm)) {
                            continue;
                        }
                        needRequestPermissonList.add(perm);
                    }
                }
            }
            return needRequestPermissonList;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private int checkMySelfPermission(String perm) {
        try {
            Method method = getClass().getMethod("checkSelfPermission", new Class[]{String.class});
            Integer permissionInt = (Integer) method.invoke(this, perm);
            return permissionInt;
        } catch (Throwable e) {
        }
        return -1;
    }

    private boolean shouldShowMyRequestPermissionRationale(String perm) {
        try {
            Method method = getClass().getMethod("shouldShowRequestPermissionRationale", new Class[]{String.class});
            Boolean permissionInt = (Boolean) method.invoke(this, perm);
            return permissionInt;
        } catch (Throwable e) {
        }
        return false;
    }

    /**
     * 检测是否说有的权限都已经授权
     *
     * @param grantResults
     * @return
     * @since 2.5.0
     */
    private boolean verifyPermissions(int[] grantResults) {
        try {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }

    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] paramArrayOfInt) {
        super.onRequestPermissionsResult(requestCode, permissions, paramArrayOfInt);
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                if (requestCode == PERMISSON_REQUESTCODE) {
                    if (!verifyPermissions(paramArrayOfInt)) {
                        showMissingPermissionDialog();
                        isNeedCheck = false;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示提示信息
     *
     * @since 2.5.0
     */
    private void showMissingPermissionDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage("当前应用缺少必要权限。\n\n请点击\"设置\"-\"权限\"-打开所需权限");

            // 拒绝, 退出应用
            builder.setNegativeButton("取消",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                finish();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    });

            builder.setPositiveButton("设置",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                startAppSettings();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    });

            builder.setCancelable(false);

            builder.show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动应用的设置
     *
     * @since 2.5.0
     */
    private void startAppSettings() {
        try {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /***************************************Helpers******************************************************/
    private void checkAndAddDataSync() {
        if (currGpsData.isComplete() && currDrData.isComplete()) {
            // 添加当前GPS数据
            currGpsData.setTimestamp(System.currentTimeMillis() - startTime);
            gpsDataList.add(currGpsData);

            // 重置当前GPS数据
            currGpsData = new GpsData();

            // 重置当前DR数据
            currDrData = new DrData();

            numOfGpsData++;
            numOfDrData++;
        }
    }

    private void checkAndAddGpsData() {
        if (currGpsData.isComplete()) {
            // 添加当前GPS数据
            currGpsData.setTimestamp(System.currentTimeMillis() - startTime);
            gpsDataList.add(currGpsData);

            // 重置当前GPS数据
            currGpsData = new GpsData();

            numOfGpsData++;
        }
    }

    /**
     * 解析GGA数据
     *
     * @param gpgga
     * @return
     */
    private GgaEntity parseGGA(String gpgga) {
        String[] parts = gpgga.split(",");

        String time = parts[1];
        String latStr = parts[2];
        String latDir = parts[3];
        String lonStr = parts[4];
        String lonDir = parts[5];
        String quality = parts[6];
        String satellites = parts[7];
        String hdop = parts[8];
        String altStr = parts[9];
        String geoid = parts[11];

        // 计算十进制度数
        double lat = MapTools.convertLatLonStrToDecimal(latStr, "lat");
        double lon = MapTools.convertLatLonStrToDecimal(lonStr, "lon");
        double alt = Double.parseDouble(altStr);

        return new GgaEntity(time, lat, latDir, lon, lonDir, quality, satellites, hdop, alt, geoid);
    }

    /**
     * 解析RMC数据
     *
     * @param gprmc
     * @return
     */
    private RmcEntity parseRMC(String gprmc) {
        String[] parts = gprmc.split(",");

        String time = parts[1];
        String status = parts[2];
        String latStr = parts[3];
        String latDir = parts[4];
        String lonStr = parts[5];
        String lonDir = parts[6];
        String speedStr = parts[7];
        String courseStr = parts[8];
        String date = parts[9];

        double speedKnots = -1;
        if (!speedStr.isEmpty()) {
            speedKnots = Double.parseDouble(speedStr);
        }

        // 将速度转换为千米/小时
        double speedKph = speedKnots * 1.852;

        double course = -1;
        if (!courseStr.isEmpty()) {
            course = Double.parseDouble(courseStr);
        }


        // 计算UTC日期时间
        String utcDateAndTime = MapTools.calculateUTCTime(time, date);

        return new RmcEntity(time, status, latStr, latDir, lonStr, lonDir, speedKph, course, utcDateAndTime);
    }

    /**
     * 检查GPS和DR数据文件是否有效
     *
     * @return
     */
    private boolean isGpsAndDrDatalistValid() {
        if (!isJsonFileValid("gps_data.json", new TypeToken<ArrayList<GpsData>>() {
        }.getType())) {
            Toast.makeText(this, "GPS回放数据文件不存在或数据为空", Toast.LENGTH_LONG).show();
            return false;
        } else if (!isJsonFileValid("dr_data.json", new TypeToken<ArrayList<DrData>>() {
        }.getType())) {
            Toast.makeText(this, "DR数据文件不存在或数据为空", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    /**
     * 检查JSON文件是否有效
     *
     * @param fileName
     * @param listType
     * @return
     */
    private boolean isJsonFileValid(String fileName, Type listType) {
        try {
            File dir = getApplicationContext().getFilesDir();
            File file = new File(dir, fileName);
            String jsonFilePath = file.getAbsolutePath();

            // 读取JSON文件
            FileReader reader = new FileReader(jsonFilePath);
            Gson gson = new Gson();
            List dataList = gson.fromJson(reader, listType);

            if (dataList == null || dataList.isEmpty()) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //  传感器监听器
    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long currentTimestamp = System.currentTimeMillis();

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (currentTimestamp - lastUpdateAcc > SENSOR_UPDATE_INTERVAL) {
                    lastUpdateAcc = currentTimestamp;

                    // 获取加速度数据
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    // 判断有效轴
                    int validAxes = 0;

                    if (Math.abs(y) > 0) {
                        // 仅YAW轴有效
                        validAxes = 1;
                        if (Math.abs(x) > 0 && Math.abs(z) > 0) {
                            // 三轴均有效
                            validAxes = 7;
                        } else if (Math.abs(x) > 0) {
                            // YAW+PITCH轴有效
                            validAxes = 2;
                        } else if (Math.abs(z) > 0) {
                            // YAW+ROLL轴有效
                            validAxes = 3;
                        }
                    }

                    // 为currDrData设置Acc
                    currDrData.setAcc(x, y, z, validAxes, SENSOR_UPDATE_INTERVAL);

                    // Log.d("SENSOR", "Accelerometer Received");
                }
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                if (currentTimestamp - lastUpdateGyr > SENSOR_UPDATE_INTERVAL) {
                    lastUpdateGyr = currentTimestamp;

                    // 获取陀螺仪数据
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    // 判断有效轴
                    int validAxes = 0;

                    if (Math.abs(y) > 0) {
                        // 仅YAW轴有效
                        validAxes = 1;
                        if (Math.abs(x) > 0 && Math.abs(z) > 0) {
                            // 三轴均有效
                            validAxes = 7;
                        } else if (Math.abs(x) > 0) {
                            // YAW+PITCH轴有效
                            validAxes = 2;
                        } else if (Math.abs(z) > 0) {
                            // YAW+ROLL轴有效
                            validAxes = 3;
                        }
                    }

                    // 为currDrData设置Gyro
                    currDrData.setGyro(x, y, z, validAxes, SENSOR_UPDATE_INTERVAL);

                    // Log.d("SENSOR", "Gyroscope Received");
                }
            } else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                if (currentTimestamp - lastUpdateTemp > SENSOR_UPDATE_INTERVAL) {
                    lastUpdateTemp = currentTimestamp;

                    // 获取温度传感器数据
                    double temperature = event.values[0];

                    // 为CurrDrData设置Temperature
                    currDrData.setTemp(temperature);

                    // Log.d("SENSOR", "Temperature Received");
                }
            }

            checkAndAddDataSync();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };
}