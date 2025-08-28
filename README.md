# MapDemo（地图打点及轨迹回放工具）

## 📖 项目简介
MapDemo 是一个基于 **Android** 的地图打点与轨迹回放工具，支持 **高德地图** 与 **腾讯地图** 双平台。项目集成了 **GPS 定位** 与 **MBVDR 惯导数据**，可实现车辆行驶过程中的实时打点记录，以及轨迹的回放、裁剪与保存。适用于车载导航、轨迹可视化、测试验证等场景。

---

## ✨ 功能特性
- **地图打点**
  - 支持 GPS（蓝点）与 DR 惯导（红点）实时打点
  - 自定义打点稀释参数（最小时间间隔 / 最小移动距离）
  - 打点数据保存为 JSON 文件（gps_data.json / dr_data.json）

- **轨迹回放**
  - 支持高德 / 腾讯地图回放
  - 轨迹线 + 小车动画展示
  - 倍速控制（1x / 2x / 4x / 10x 等）
  - 支持 GPS 与 DR 轨迹叠加回放
  - 裁剪轨迹并保存为新的 JSON 文件

- **文件管理**
  - 使用 [mlhfileselector](https://github.com/molihuan/mlhfileselectorlib) 选择 JSON 数据文件
  - 支持保存与加载轨迹文件

---

## 🗂 项目结构
- `MainActivity.java` —— 地图打点工具实现（基于高德地图）
- `TencentReplayActivity.java` —— 腾讯地图轨迹回放
- `AmapReplayActivity.java` —— 高德地图轨迹回放
- `entity/` —— 数据类（GpsData、DrData、GgaEntity、RmcEntity 等）
- `util/MapTools.java` —— 经纬度转换、NMEA 解析、距离计算等工具方法
- `res/layout/` —— UI 界面布局（地图打点 / 高德回放 / 腾讯回放）

---

## 🔧 依赖配置
主要依赖：
- [高德地图 SDK](https://lbs.amap.com/api/android-sdk/summary)
- [腾讯地图 SDK](https://lbs.qq.com/mobile/androidMapSDK/developerGuide/androidSummary)
- MBClient 惯导 SDK（libs/mbClientLib-1.0.0.aar）
- Gson（数据序列化）
- mlhfileselector（文件选择器）

在 `app/build.gradle` 中需添加依赖，并在 `AndroidManifest.xml` 中配置 API Key：

```xml
<!-- 高德地图 Key -->
<meta-data
    android:name="com.amap.api.v2.apikey"
    android:value="YOUR_AMAP_KEY"/>

<!-- 腾讯地图 Key -->
<meta-data
    android:name="TencentMapSDK"
    android:value="YOUR_TENCENT_KEY"/>

