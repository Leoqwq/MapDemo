package com.example.mapdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.overlay.MovingPointOverlay;

import com.example.mapdemo.entity.GpsData;
import com.example.mapdemo.entity.DrData;
import com.example.mapdemo.view.DoubleSeekBar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.molihuan.pathselector.PathSelector;
import com.molihuan.pathselector.configs.PathSelectorConfig;
import com.molihuan.pathselector.entity.FileBean;
import com.molihuan.pathselector.entity.FontBean;
import com.molihuan.pathselector.fragment.BasePathSelectFragment;
import com.molihuan.pathselector.fragment.impl.PathSelectFragment;
import com.molihuan.pathselector.listener.FileItemListener;
import com.molihuan.pathselector.utils.MConstants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AmapReplayActivity extends AppCompatActivity{
    /**
     * 地图控件
     */
    private MapView mapView;

    /**
     * 高德地图对象
     */
    private AMap aMap;

    /**
     * 按钮
     */
    Button startReplayButton;
    Button resetReplayButton;
    Button backButton;
    Button saveTrimmedButton;
    Button selectGpsFileButton;
    Button selectDrFileButton;

    /**
     * GPS坐标文本
     */
    private TextView gpsTextView;

    /**
     * 惯导坐标文本
     */
    private TextView drTextView;

    /**
     * 轨迹裁剪滑动条
     */
    private DoubleSeekBar seekBar;

    /**
     * 启用惯导回放按钮
     */
    private SwitchCompat vdrEnableSwitch;

    /**
     * 轨迹裁剪滑动条文本
     */
    private TextView seekBarTxtLeft;
    private TextView seekBarTxtRight;

    /**
     * GPS小车图标
     */
    private Marker gpsMarker;

    /**
     * 惯导小车图标
     */
    private Marker drMarker;

    /**
     * 目的地图标
     */
    private Marker destMarker;

    /**
     * GPS轨迹线
     */
    private Polyline gpsPolyline;

    /**
     * 惯导轨迹线
     */
    private Polyline drPolyline;

    /**
     * GPS小车平滑移动
     */
    private MovingPointOverlay gpsMovingPointOverlay;

    /**
     * 惯导小车平滑移动
     */
    private MovingPointOverlay drMovingPointOverlay;

    /**
     * GPS轨迹数据
     */
    private List<GpsData> gpsDataList = new ArrayList<>();
    private List<GpsData> trimmedGpsDataList = new ArrayList<>();
    private List<LatLng> gpsPath = new ArrayList<>();
    private List<LatLng> currentGpsPath = new ArrayList<>();
    private List<LatLng> trimmedGpsPath = new ArrayList<>();

    /**
     * 惯导轨迹数据
     */
    private List<DrData> drDataList = new ArrayList<>();
    private List<DrData> trimmedDrDataList = new ArrayList<>();
    private List<LatLng> drPath = new ArrayList<>();
    private List<LatLng> currentDrPath = new ArrayList<>();
    private List<LatLng> trimmedDrPath = new ArrayList<>();

    /**
     * GPS轨迹回放的总时长
     */
    private int gpsTotalDuration = 0;

    /**
     * 惯导轨迹回放的总时长
     */
    private int drTotalDuration = 0;

    /**
     * 倍速下拉框
     */
    private Spinner speedSpinner;

    /**
     * 倍速乘数，默认倍速为1.0，即原速播放
     */
    private float speedMultiplier = 1.0f;

    /**
     * 裁剪轨迹片段占完整轨迹长度的比例，默认比例为1，0，即和总轨迹相同
     */
    private float trimmedPathProportion = 1.0f;

    /**
     * 是否启用惯导回放
     */
    private boolean isVdrEnable = false;

    static final int GPS_FILE_SELECTOR_CODE = 1;
    static final int DR_FILE_SELECTOR_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amap_replay);

        mapView = findViewById(R.id.amap_replay_mapView);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();

        // 初始化文本框
        gpsTextView = findViewById(R.id.tv_gps_coordinates);
        drTextView = findViewById(R.id.tv_dr_coordinates);

        // 初始化按钮
        startReplayButton = findViewById(R.id.btn_start);
        resetReplayButton = findViewById(R.id.btn_reset);
        backButton = findViewById(R.id.btn_back);
        saveTrimmedButton = findViewById(R.id.btn_save_trimmed);
        selectGpsFileButton = findViewById(R.id.btn_select_gps_file);
        selectDrFileButton = findViewById(R.id.btn_select_dr_file);

        startReplayButton.setOnClickListener(v -> startReplay());
        resetReplayButton.setOnClickListener(v -> resetReplay());
        backButton.setOnClickListener(v -> {
            this.finish();
            Intent intent = new Intent(AmapReplayActivity.this, MainActivity.class);
            startActivity(intent);
        });
        saveTrimmedButton.setOnClickListener(v ->
        {
            saveTrimmedGpsData();
            saveTrimmedDrData();
        });
        selectGpsFileButton.setOnClickListener(v -> {
            initPathSelector(GPS_FILE_SELECTOR_CODE);
        });
        selectDrFileButton.setOnClickListener(v -> {
            initPathSelector(DR_FILE_SELECTOR_CODE);
        });

        // 加载保存的轨迹数据
        loadGpsData("gps_data.json");

        // 初始化倍速下拉框
        initSpeedSpinner();

        // 初始化启用惯导回放按钮
        initVdrEnableButton();

    }

    private void initPathSelector(int selectorCode) {
        // 关闭自动申请权限弹窗
        PathSelectorConfig.setAutoGetPermission(false);

        //获取PathSelectFragment实例然后在onBackPressed中处理返回按钮点击事件
        PathSelectFragment selector = PathSelector.build(this, MConstants.BUILD_DIALOG)
                //.setBuildType(MConstants.BUILD_DIALOG)//已经在build中已经设置了
                //.setContext(this)//已经在build中已经设置了
                .setRootPath(getApplicationContext().getFilesDir().toString())//初始路径
                .setShowSelectStorageBtn(false)//是否显示内部存储选择按钮
                .setShowTitlebarFragment(true)//是否显示标题栏
                .setShowTabbarFragment(true)//是否显示面包屑
                .setShowFileTypes("", "json")//只显示(后缀为json)的文件
                .setSelectFileTypes("", "json")//只能选择(后缀为json)的文件
                .setMaxCount(1)//最多可以选择1个文件,默认是-1不限制
                .setRadio()//单选(如果需要单选文件夹请使用setMaxCount(0)来替换)
                .setSortType(MConstants.SortRules.SORT_NAME_ASC)//按名称排序
                .setTitlebarMainTitle(new FontBean("请选择JSON轨迹数据文件"))//设置标题栏主标题,还可以设置字体大小,颜色等
                .setTitlebarBG(Color.CYAN)//设置标题栏颜色
                .setPathSelectDialogHeight(mapView.getHeight())
                .setPathSelectDialogWidth(mapView.getWidth())
                .setFileItemListener(//设置文件item点击回调(点击是文件才会回调,如果点击是文件夹则不会)
                        new FileItemListener() {
                            @Override
                            public boolean onClick(View v, FileBean file, String currentPath, BasePathSelectFragment pathSelectFragment) {
                                if (selectorCode == GPS_FILE_SELECTOR_CODE) {
                                    loadGpsData(file.getName());
                                } else if (selectorCode == DR_FILE_SELECTOR_CODE) {
                                    loadDrData(file.getName());
                                }
                                pathSelectFragment.close();
                                return false;
                            }
                        }
                )
                .show();
    }

    /**
     * 初始化倍速下拉框
     */
    private void initSpeedSpinner() {
        speedSpinner = findViewById(R.id.speed_spinner);
        speedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        speedMultiplier = 1.0f;
                        Toast.makeText(view.getContext(), "已重置回放并切换为1倍速，点击'开始'按钮进行回放", Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        speedMultiplier = 0.5f;
                        Toast.makeText(view.getContext(), "已重置回放并切换为2倍速，点击'开始'按钮进行回放", Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        speedMultiplier = 0.25f;
                        Toast.makeText(view.getContext(), "已重置回放并切换为4倍速，点击'开始'按钮进行回放", Toast.LENGTH_LONG).show();
                        break;
                    case 3:
                        speedMultiplier = 0.1f;
                        Toast.makeText(view.getContext(), "已重置回放并切换为10倍速，点击'开始'按钮进行回放", Toast.LENGTH_LONG).show();
                        break;
                }

                resetReplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 初始化启用惯导回放按钮
     */
    private void initVdrEnableButton() {
        vdrEnableSwitch = findViewById(R.id.switch_enable_vdr);
        vdrEnableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    if (!drDataList.isEmpty()) {
                        Toast.makeText(mapView.getContext(),"已启用惯导回放", Toast.LENGTH_SHORT).show();
                        isVdrEnable = true;
                    } else if (loadDrData("dr_data.json")) {
                        Toast.makeText(mapView.getContext(),"已读取默认惯导文件并启用惯导回放", Toast.LENGTH_SHORT).show();
                        isVdrEnable = true;
                    } else {
                        Toast.makeText(mapView.getContext(),"无法读取默认惯导文件，请手动选择后再尝试启用惯导回放", Toast.LENGTH_SHORT).show();
                        vdrEnableSwitch.setChecked(false);
                    }
                }else {
                    Toast.makeText(mapView.getContext(),"已关闭惯导回放", Toast.LENGTH_SHORT).show();
                    isVdrEnable = false;
                }

                resetReplay();
            }
        });
    }

    /**
     * 初始化回放
     */
    private void initReplay() {
        // 初始化GPS和DR坐标文本
        runOnUiThread(() -> {
            gpsTextView.setText("GPS坐标\n经度：null\n纬度：null");
            drTextView.setText("DR坐标\n经度：null\n纬度：null");
        });

        // 初始化Gps和Dr回放
        initGpsReplay();
        if (isVdrEnable) {
            initDrReplay();
        }

        // 初始化终点图标
        if (!trimmedGpsPath.isEmpty()) {
            MarkerOptions destMarkerOptions = new MarkerOptions().position(trimmedGpsPath.get(trimmedGpsPath.size() - 1)).title("终点").icon(BitmapDescriptorFactory.fromAsset("dest.png")).anchor(0.5f, 1f);
            destMarker = aMap.addMarker(destMarkerOptions);
        }
    }

    /**
     * 初始化GPS回放
     */
    private void initGpsReplay() {
        if (trimmedGpsPath.isEmpty() || trimmedGpsDataList.isEmpty()) {
            return;
        }

        // 初始化轨迹线
        currentGpsPath.add(trimmedGpsPath.get(0));
        gpsPolyline = aMap.addPolyline(new PolylineOptions()
                .addAll(trimmedGpsPath)
                .width(10)
                .color(0xAA0000FF) // Blue for GPS
                .transparency(0.3f));

        if (gpsTotalDuration != 0) {
            // 初始化小车图标和平滑移动
            MarkerOptions gpsMarkerOptions =  new MarkerOptions();
            gpsMarkerOptions.position(trimmedGpsPath.get(0));
            gpsMarkerOptions.title("GPS");
            gpsMarkerOptions.icon(BitmapDescriptorFactory.fromAsset("car_blue.png"));
            gpsMarkerOptions.rotateAngle(360.0f - (float) trimmedGpsDataList.get(0).yaw);
            gpsMarker = aMap.addMarker(gpsMarkerOptions);
            gpsMovingPointOverlay = new MovingPointOverlay(aMap, gpsMarker);
            gpsMovingPointOverlay.setPoints(trimmedGpsPath);
            gpsMovingPointOverlay.setTotalDuration((int)(gpsTotalDuration * speedMultiplier * trimmedPathProportion));
            gpsMovingPointOverlay.setMoveListener((distance) -> {
                // 更新坐标文本
                runOnUiThread(() -> {gpsTextView.setText("GPS坐标\n经度：" + gpsMovingPointOverlay.getPosition().latitude + "\n纬度：" + gpsMovingPointOverlay.getPosition().longitude);});

                // 更新轨迹线
                currentGpsPath.add(gpsMovingPointOverlay.getPosition());
                gpsPolyline.setPoints(currentGpsPath);

                // 如果动画完成，则隐藏小车图标
                if (distance == 0) {
                    gpsMarker.setVisible(false);
                }

                // 更新镜头位置
                if (distance == 0) {
                    aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getPathBounds(trimmedGpsPath), 50), 1000, null);
                } else {
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMovingPointOverlay.getPosition(), aMap.getCameraPosition().zoom), (long)(1000 * speedMultiplier), null);
                }
            });
        } else {
            Toast.makeText(this, "GPS回放时长为0", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 初始化惯导回放
     */
    private void initDrReplay() {
        if (trimmedDrPath.isEmpty() || trimmedDrDataList.isEmpty()) {
            return;
        }

        // 初始化轨迹线
        currentDrPath.add(trimmedDrPath.get(0));
        drPolyline = aMap.addPolyline(new PolylineOptions()
                .addAll(trimmedDrPath)
                .width(10)
                .color(0xFFFF0000) // Red for DR
                .transparency(0.3f));

//        drPolyline.setVisible(false);

        if (drTotalDuration != 0) {
            // 初始化小车图标和平滑移动
            MarkerOptions drMarkerOptions =  new MarkerOptions();
            drMarkerOptions.position(trimmedDrPath.get(0));
            drMarkerOptions.title("GPS");
            drMarkerOptions.icon(BitmapDescriptorFactory.fromAsset("car_red.png"));
            drMarkerOptions.rotateAngle(360.0f - (float) trimmedDrDataList.get(0).yaw);
            drMarker = aMap.addMarker(drMarkerOptions);
            drMovingPointOverlay = new MovingPointOverlay(aMap, drMarker);
            drMovingPointOverlay.setPoints(drPath);
            drMovingPointOverlay.setTotalDuration((int)(drTotalDuration * speedMultiplier));
            drMovingPointOverlay.setMoveListener((distance) -> {
                // 更新坐标文本
                runOnUiThread(() -> {drTextView.setText("DR坐标\n经度：" + drMovingPointOverlay.getPosition().longitude + "\n纬度：" + drMovingPointOverlay.getPosition().latitude);});

                // 更新轨迹线
                currentDrPath.add(drMovingPointOverlay.getPosition());
                drPolyline.setPoints(currentDrPath);

                // 如果动画完成，则隐藏小车图标
                if (distance == 0) {
                    drMarker.setVisible(false);
                }
            });

            drMarker.setVisible(false);
        } else {
            Toast.makeText(this, "DR回放时长为0", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 初始化SeekBar
     */
    private void initSeekBar() {
        seekBarTxtLeft = findViewById(R.id.tv_seekBar_left);
        seekBarTxtLeft.setText("index: 0");
        seekBarTxtRight = findViewById(R.id.tv_seekBar_right);
        seekBarTxtRight.setText(String.valueOf(gpsDataList.size() - 1));

        seekBar = findViewById(R.id.seek_bar);
        seekBar.setMaxValue(gpsDataList.size() - 1);
        seekBar.setMinValue(0);
        seekBar.setOnChanged((leftValue, rightValue) -> {
            // 更新文本
            seekBarTxtLeft.setText("index: " + leftValue);
            seekBarTxtRight.setText(String.valueOf(rightValue));


            // 裁剪路线
            trimmedGpsDataList = gpsDataList.subList(leftValue, rightValue + 1);
            trimmedGpsPath = gpsPath.subList(leftValue, rightValue + 1);
            gpsPolyline.setPoints(trimmedGpsPath);

            if (leftValue > drPath.size() - 1) {
                trimmedDrDataList = new ArrayList<>();
                trimmedDrPath = new ArrayList<>();
            } else if (rightValue > drPath.size() - 1) {
                trimmedDrDataList = drDataList.subList(leftValue, drDataList.size());
                trimmedDrPath = drPath.subList(leftValue, drPath.size());
            } else {
                trimmedDrDataList = drDataList.subList(leftValue, rightValue + 1);
                trimmedDrPath = drPath.subList(leftValue, rightValue + 1);
            }

            // 计算裁剪后片段占总路径的比例
            trimmedPathProportion = (float) trimmedGpsPath.size() / (float) gpsPath.size();

            // 更新小车和终点图标
            gpsMarker.setPosition(gpsPath.get(leftValue));
            gpsMarker.setRotateAngle(360.0f - (float)gpsDataList.get(leftValue).yaw);
            destMarker.setPosition(gpsPath.get(rightValue));
        });
    }

    /**
     * 开始回放
     */
    private void startReplay() {
        resetReplay();

        if (gpsMovingPointOverlay != null && gpsPolyline != null) {
            gpsPolyline.setPoints(currentGpsPath);
            gpsMovingPointOverlay.startSmoothMove();
        } else {
            Toast.makeText(this, "此段路径没有可供回放的GPS数据", Toast.LENGTH_SHORT).show();
        }

        if (isVdrEnable) {
            if (drMovingPointOverlay != null && drPolyline != null && drMarker != null) {
                drPolyline.setPoints(currentDrPath);
                drPolyline.setVisible(true);
                drMarker.setVisible(true);
                drMovingPointOverlay.startSmoothMove();
            } else {
                Toast.makeText(this, "此段路经没有可供回放的DR数据", Toast.LENGTH_SHORT).show();
            }
        }

        // 手动初始化第一段镜头移动动画，否则镜头移动会有延迟
        if (!trimmedGpsPath.isEmpty()) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(trimmedGpsPath.get(1), aMap.getCameraPosition().zoom));
        }

        // 隐藏SeekBar
        if (seekBar != null) {
            seekBarTxtLeft.setVisibility(View.INVISIBLE);
            seekBarTxtRight.setVisibility(View.INVISIBLE);
            seekBar.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 重置回放
     */
    private void resetReplay() {
        if (gpsMovingPointOverlay != null) {
            // 重置小车动画
            gpsMovingPointOverlay.destroy();
            gpsMovingPointOverlay = null;
        }
        if (gpsPolyline != null) {
            // 重置轨迹线
            currentGpsPath = new ArrayList<>();
            gpsPolyline.remove();
        }
        if (gpsMarker != null) {
            // 重置小车图标
            gpsMarker.remove();
            gpsMarker = null;
        }
        if (destMarker != null) {
            //重置终点图标
            destMarker.remove();
            destMarker = null;
        }

        if (drMovingPointOverlay != null) {
            // 重置小车动画
            drMovingPointOverlay.destroy();
            drMovingPointOverlay = null;
        }
        if (drPolyline != null) {
            // 重置轨迹线
            currentDrPath = new ArrayList<>();
            drPolyline.remove();
        }
        if (drMarker != null) {
            // 重置小车图标
            drMarker.remove();
            drMarker = null;
        }

        // 重置镜头位置
        if (!gpsPath.isEmpty()) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gpsPath.get(0), 20));
            aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(getPathBounds(trimmedGpsPath), 50));
        }

        // 显示SeekBar
        if (seekBar != null) {
            seekBarTxtLeft.setVisibility(View.VISIBLE);
            seekBarTxtRight.setVisibility(View.VISIBLE);
            seekBar.setVisibility(View.VISIBLE);
        }

        initReplay();
    }

    /**
     * 加载GPS回放数据
     */
    private boolean loadGpsData(String fileName) {
        try {
            if (!gpsDataList.isEmpty() || !trimmedGpsDataList.isEmpty() || !gpsPath.isEmpty() || !currentGpsPath.isEmpty() || !trimmedGpsPath.isEmpty()) {
                gpsDataList = new ArrayList<>();
                trimmedGpsDataList = new ArrayList<>();
                gpsPath = new ArrayList<>();
                currentGpsPath = new ArrayList<>();
                trimmedGpsPath = new ArrayList<>();
            }

            // 获取保存的JSON文件路径
            File dir = getApplicationContext().getFilesDir();
            File file = new File(dir, fileName);
            String jsonFilePath = file.getAbsolutePath();

            // 读取JSON文件
            FileReader reader = new FileReader(jsonFilePath);
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<GpsData>>() {}.getType();
            List<GpsData> dataList = gson.fromJson(reader, listType);

            // 设置轨迹总时长，秒数为dataList的轨迹点总数
            gpsTotalDuration = dataList.size();

            // 提取position并转换为LatLng列表
            if (dataList != null) {
                for (GpsData data : dataList) {
                    if (data != null && data.latitude != -1 && data.longitude != -1) {
                        gpsDataList.add(data);
                        gpsPath.add(new LatLng(data.latitude, data.longitude));
                    }
                }
            }

            if (gpsDataList.isEmpty()) {
                Toast.makeText(this,"无法解析所选GPS文件或数据为空", Toast.LENGTH_SHORT).show();
                return false;
            }

            // 初始化裁剪轨迹
            trimmedGpsDataList = gpsDataList;
            trimmedGpsPath = gpsPath;

            reader.close();

            // 初始化拖动条
            if (seekBar != null) {
                seekBar.setVisibility(View.GONE);
                seekBarTxtLeft.setVisibility(View.GONE);
                seekBarTxtRight.setVisibility(View.GONE);
            }
            initSeekBar();

            // 重置回放
            resetReplay();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"无法解析所选GPS文件或数据为空", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * 加载惯导回放数据
     */
    private boolean loadDrData(String fileName) {
        try {
            if (!drDataList.isEmpty() || !trimmedDrDataList.isEmpty() || !drPath.isEmpty() || !currentDrPath.isEmpty() || !trimmedDrPath.isEmpty()) {
                drDataList = new ArrayList<>();
                trimmedDrDataList = new ArrayList<>();
                drPath = new ArrayList<>();
                currentDrPath = new ArrayList<>();
                trimmedDrPath = new ArrayList<>();
            }

            // 获取保存的JSON文件路径
            File dir = getApplicationContext().getFilesDir();
            File file = new File(dir, fileName);
            String jsonFilePath = file.getAbsolutePath();

            // 读取JSON文件
            FileReader reader = new FileReader(jsonFilePath);
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<DrData>>() {}.getType();
            List<DrData> dataList = gson.fromJson(reader, listType);

            // 设置轨迹总时长，秒数为dataList的轨迹点总数
            drTotalDuration = dataList.size();

            // 提取position并转换为LatLng列表
            if (dataList != null) {
                for (DrData data : dataList) {
                    if (data != null && data.latitude != -1 && data.longitude != -1) {
                        drDataList.add(data);
                        drPath.add(new LatLng(data.latitude, data.longitude));
                    }
                }
            }

            if (drDataList.isEmpty()) {
                Toast.makeText(this,"无法解析所选惯导文件或数据为空", Toast.LENGTH_SHORT).show();
                return false;
            }

            // 初始化裁剪轨迹
            trimmedDrDataList = drDataList;
            trimmedDrPath = drPath;

            reader.close();

            // 重置回放
            resetReplay();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"无法解析所选惯导文件或数据为空", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * 保存裁剪后的GPS轨迹数据
     */
    private void saveTrimmedGpsData() {
        try {
            // 转换为JSON字符串
            Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().create();
            String json = gson.toJson(trimmedGpsDataList);

            // 定义文件保存路径
            File dir = getApplicationContext().getFilesDir();
            File file = new File(dir, "gps_data_trimmed.json");

            // 写入文件
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(json);
            fileWriter.flush();
            fileWriter.close();

            // 打印保存成功的日志
            Toast.makeText(this, "裁剪后的GPS回放数据已保存至：" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Log.d("TENCENT", "Trimmed gps data saved to: " + file.getAbsolutePath());
            Log.d("TENCENT", "JSON content: " + json);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TENCENT", "Failed to save Trimmed gps data", e);
        }
    }

    /**
     * 保存裁剪后的惯导轨迹数据
     */
    private void saveTrimmedDrData() {
        if (trimmedDrDataList == null) {
            Toast.makeText(this, "此段路径没有可保存的惯导数据", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 转换为JSON字符串
            Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().create();
            String json = gson.toJson(trimmedDrDataList);

            // 定义文件保存路径
            File dir = getApplicationContext().getFilesDir();
            File file = new File(dir, "dr_data_trimmed.json");

            // 写入文件
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(json);
            fileWriter.flush();
            fileWriter.close();

            // 打印保存成功的日志
            Toast.makeText(this, "裁剪后的惯导数据已保存至：" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Log.d("DR", "Trimmed DR data saved to: " + file.getAbsolutePath());
            Log.d("DR", "JSON content: " + json);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("DR", "Failed to save Trimmed DR data", e);
        }
    }

    /**
     * 获取轨迹边界
     */
    private LatLngBounds getPathBounds(List<LatLng> path) {
        // 获取最西、最南、最东、最北的坐标点
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;

        for (LatLng point : path) {
            if (point.latitude < minLat) minLat = point.latitude;
            if (point.latitude > maxLat) maxLat = point.latitude;
            if (point.longitude < minLng) minLng = point.longitude;
            if (point.longitude > maxLng) maxLng = point.longitude;
        }

        // 创建 Bounds 对象
        LatLngBounds bounds = new LatLngBounds(
                new LatLng(minLat, minLng),
                new LatLng(maxLat, maxLng)
        );

        return bounds;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (gpsMovingPointOverlay != null) {
            gpsMovingPointOverlay.destroy();
        }
        if (drMovingPointOverlay != null) {
            drMovingPointOverlay.destroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
