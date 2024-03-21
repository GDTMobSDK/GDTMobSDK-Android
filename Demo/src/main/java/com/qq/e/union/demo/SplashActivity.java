package com.qq.e.union.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qq.e.ads.splash.SplashAD;
import com.qq.e.ads.splash.SplashADListener;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.ToastUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 这是demo工程的入口Activity，在这里会首次调用优量汇的SDK。
 * 在调用SDK之前，如果您的App的targetSDKVersion >= 23，那么建议动态申请相关权限。
 */
public class SplashActivity extends BaseActivity implements SplashADListener,
    View.OnClickListener {

  private static final String TAG = "SplashActivity";

  private SplashAD splashAD;
  private ViewGroup container;

  public boolean canJump = false;
  private boolean needStartDemoList = true;

  private boolean loadAdOnly = false;
  private boolean showingAd = false;
  private boolean isFullScreen = false;
  private Integer fetchDelay;
  private LinearLayout loadAdOnlyView;
  private Button loadAdOnlyCloseButton;
  private Button loadAdOnlyDisplayButton;
  private Button loadAdOnlyRefreshButton;
  private TextView loadAdOnlyStatusTextView;

  /**
   * 为防止无广告时造成视觉上类似于"闪退"的情况，设定无广告时页面跳转根据需要延迟一定时间，demo
   * 给出的延时逻辑是从拉取广告开始算开屏最少持续多久，仅供参考，开发者可自定义延时逻辑，如果开发者采用demo
   * 中给出的延时逻辑，也建议开发者考虑自定义minSplashTimeWhenNoAD的值（单位ms）
   **/
  private int minSplashTimeWhenNoAD = 2000;
  /**
   * 记录拉取广告的时间
   */
  private long fetchSplashADTime = 0;
  private Handler handler = new Handler(Looper.getMainLooper());

  // 是否适配全面屏，默认是适配全面屏，即使用顶部状态栏和底部导航栏
  private boolean isNotchAdaptation = true;
  private boolean mLoadSuccess;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // 获取开屏配置（是否适配全面屏等）
    getSplashAdSettings();

    // 如需适配刘海屏水滴屏，必须在onCreate方法中设置全屏显示
    if (isNotchAdaptation) {
      hideSystemUI();
    }

    setContentView(R.layout.activity_splash);
    container = this.findViewById(R.id.splash_container);
    Intent intent = getIntent();

    boolean needLogo = false;
    try {
      needLogo = intent.getBooleanExtra("need_logo", true);
      needStartDemoList = intent.getBooleanExtra("need_start_demo_list", true);
      loadAdOnly = intent.getBooleanExtra("load_ad_only", false);
      isFullScreen = intent.getBooleanExtra("is_full_screen", false);
      fetchDelay = (Integer) intent.getSerializableExtra("fetch_delay");
    } catch (Exception e) {
      e.printStackTrace();
    }

    loadAdOnlyView = findViewById(R.id.splash_load_ad_only);
    loadAdOnlyCloseButton = findViewById(R.id.splash_load_ad_close);
    loadAdOnlyCloseButton.setOnClickListener(this);
    loadAdOnlyDisplayButton = findViewById(R.id.splash_load_ad_display);
    loadAdOnlyDisplayButton.setOnClickListener(this);
    loadAdOnlyRefreshButton = findViewById(R.id.splash_load_ad_refresh);
    loadAdOnlyRefreshButton.setOnClickListener(this);
    loadAdOnlyStatusTextView = findViewById(R.id.splash_load_ad_status);
    findViewById(R.id.is_ad_valid_button).setOnClickListener(this);

    if(loadAdOnly){
      loadAdOnlyView.setVisibility(View.VISIBLE);
      loadAdOnlyStatusTextView.setText(R.string.splash_loading);
      loadAdOnlyDisplayButton.setEnabled(false);
    }
    if (!needLogo) {
      findViewById(R.id.app_logo).setVisibility(View.GONE);
    }
    if (Build.VERSION.SDK_INT >= 23) {
      checkAndRequestPermission();
    } else {
      // 如果是Android6.0以下的机器，建议在manifest中配置相关权限，这里可以直接调用SDK
      fetchSplashAD(this, container, getPosId(), this);
    }
  }

  protected String getPosId() {
    String posId = getIntent().getStringExtra("pos_id");
    return TextUtils.isEmpty(posId) ? PositionId.SPLASH_POS_ID : posId;
  }

  private String getToken() {
    return getIntent().getStringExtra(Constants.TOKEN);
  }

  /**
   *
   * ----------非常重要----------
   *
   * Android6.0以上的权限适配简单示例：
   *
   * 如果targetSDKVersion >= 23，那么建议动态申请相关权限，再调用优量汇SDK
   *
   * SDK不强制校验下列权限（即:无下面权限sdk也可正常工作），但建议开发者申请下面权限，尤其是READ_PHONE_STATE权限
   *
   * READ_PHONE_STATE权限用于允许SDK获取用户标识,
   * 针对单媒体的用户，允许获取权限的，投放定向广告；不允许获取权限的用户，投放通投广告，媒体可以选择是否把用户标识数据提供给优量汇，并承担相应广告填充和eCPM单价下降损失的结果。
   *
   * Demo代码里是一个基本的权限申请示例，请开发者根据自己的场景合理地编写这部分代码来实现权限申请。
   * 注意：下面的`checkSelfPermission`和`requestPermissions`方法都是在Android6.0的SDK中增加的API，如果您的App还没有适配到Android6.0以上，则不需要调用这些方法，直接调用优量汇SDK即可。
   */
  @TargetApi(Build.VERSION_CODES.M)
  private void checkAndRequestPermission() {
    List<String> lackedPermission = new ArrayList<String>();
    //TODO 先注释掉，方便测试关闭READ_PHONE_STATE进行兼容性测试验证
//    if (!(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)) {
//      lackedPermission.add(Manifest.permission.READ_PHONE_STATE);
//    }
    List<String> permissions = getPermissionsInManifest();
    // 检查读写存储权限开始
    // 在android 14上读写外部存储空间权限被拆分，也获取不到外部存储的文件了，这里直接略过
    if (Build.VERSION.SDK_INT < 31) {
      // 快手SDK所需相关权限，存储权限，此处配置作用于流量分配功能，关于流量分配，详情请咨询运营;如果您的APP不需要快手SDK的流量分配功能，则无需申请SD卡权限
      if (!(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
        if (permissions != null && permissions.size() > 0 && permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
          lackedPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
      }
      if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
        if (permissions != null && permissions.size() > 0 && permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
      }
    }
    if (!(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
      lackedPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION);
    }
    // 检查读写存储权限结束
    // 如果需要的权限都已经有了，那么直接调用SDK
    if (lackedPermission.size() == 0) {
      fetchSplashAD(this, container, getPosId(), this);
    } else {
      // 否则，建议请求所缺少的权限，在onRequestPermissionsResult中再看是否获得权限
      String[] requestPermissions = new String[lackedPermission.size()];
      lackedPermission.toArray(requestPermissions);
      requestPermissions(requestPermissions, 1024);
    }
  }

  private List<String> getPermissionsInManifest() {
    String[] requestedPermissions = null;
    try {
      PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
      requestedPermissions = packageInfo.requestedPermissions;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    return Arrays.asList(requestedPermissions);
  }

  private boolean hasAllPermissionsGranted(int[] grantResults) {
    for (int grantResult : grantResults) {
      if (grantResult == PackageManager.PERMISSION_DENIED) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 1024 && hasAllPermissionsGranted(grantResults)) {
      fetchSplashAD(this, container, getPosId(), this);
    } else {
      ToastUtil.l("应用缺少必要的权限！请点击\"权限\"，打开所需要的权限。");
      try {
      Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
      } catch (Exception e) {
      }
      finish();
    }
  }

  /**
   * 拉取开屏广告，开屏广告的构造方法有3种，详细说明请参考开发者文档。
   *
   * @param activity        展示广告的activity
   * @param adContainer     展示广告的大容器
   * @param posId           广告位ID
   * @param adListener      广告状态监听器
   */
  private void fetchSplashAD(Activity activity, ViewGroup adContainer,
                             String posId, SplashADListener adListener) {
    fetchSplashADTime = System.currentTimeMillis();
    splashAD = getSplashAd(activity, posId, adListener, fetchDelay, getToken());
    // 设置是否全屏显示
    setSystemUi();
    if (isFullScreen) {
      splashAD.fetchFullScreenAdOnly();
    } else {
      splashAD.fetchAdOnly();
    }
  }

  protected SplashAD getSplashAd(Activity activity, String posId,
                                 SplashADListener adListener, Integer fetchDelay, String token) {
    SplashAD splashAD;
    Log.d(TAG, "getSplashAd: BiddingToken " + token);
    if (!TextUtils.isEmpty(token)) {
      splashAD = new SplashAD(activity, posId, adListener, fetchDelay == null ? 0 : fetchDelay, token);
    } else {
      splashAD = new SplashAD(activity, posId, adListener, fetchDelay == null ? 0 : fetchDelay);
    }
    if (isFullScreen) {
      splashAD.setDeveloperLogo(getIntent().getIntExtra("developer_logo", 0));
    }
    splashAD.setLoadAdParams(DemoUtil.getLoadAdParams("splash"));
    return splashAD;
  }

  @Override
  public void onADPresent() {
    Log.i("AD_DEMO", "SplashADPresent");
  }

  @Override
  public void onADClicked() {
    Log.i("AD_DEMO", "SplashADClicked");
  }

  /**
   * 倒计时回调，返回广告还将被展示的剩余时间。
   * 通过这个接口，开发者可以自行决定是否显示倒计时提示，或者还剩几秒的时候显示倒计时
   *
   * @param millisUntilFinished 剩余毫秒数
   */
  @Override
  public void onADTick(long millisUntilFinished) {
    Log.i("AD_DEMO", "SplashADTick " + millisUntilFinished + "ms");
  }

  @Override
  public void onADExposure() {
    Log.i("AD_DEMO", "SplashADExposure");
  }

  @Override
  public void onADLoaded(long expireTimestamp) {
    mLoadSuccess = true;
    Log.i("AD_DEMO", "SplashADFetch expireTimestamp: " + expireTimestamp
        + ", eCPMLevel = " + splashAD.getECPMLevel()+ ", ECPM: " + splashAD.getECPM()
        + ", testExtraInfo:" + splashAD.getExtraInfo().get("mp")
        + ", request_id:" + splashAD.getExtraInfo().get("request_id"));
    if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
      splashAD.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
    }
    reportBiddingResult(splashAD);
    if (loadAdOnly) {
      loadAdOnlyDisplayButton.setEnabled(true);
      long timeIntervalSec = (expireTimestamp - SystemClock.elapsedRealtime()) / 1000;
      long min = timeIntervalSec / 60;
      long second = timeIntervalSec - (min * 60);
      loadAdOnlyStatusTextView.setText("加载成功,广告将在:" + min + "分" + second + "秒后过期，请在此之前展示(showAd)");
    } else {
      if (isFullScreen) {
        splashAD.showFullScreenAd(container);
      } else {
        splashAD.showAd(container);
      }
    }
  }

  /**
   * 上报给优量汇服务端在开发者客户端竞价中优量汇的竞价结果，以便于优量汇服务端调整策略提供给开发者更合理的报价
   *
   * 优量汇竞价失败调用 sendLossNotification，并填入优量汇竞败原因（必填）、竞胜ADN ID（选填）、竞胜ADN报价（选填）
   * 优量汇竞价胜出调用 sendWinNotification
   * 请开发者如实上报相关参数，以保证优量汇服务端能根据相关参数调整策略，使开发者收益最大化
   */
  private void reportBiddingResult(SplashAD splashAD) {
    DemoBiddingC2SUtils.reportBiddingWinLoss(splashAD);
    if (DemoUtil.isNeedSetBidECPM()) {
      splashAD.setBidECPM(300);
    }
  }

  @Override
  public void onADDismissed() {
    Log.i("AD_DEMO", "SplashADDismissed");
    next();
  }

  @Override
  public void onNoAD(AdError error) {
    String str = String.format("LoadSplashADFail, eCode=%d, errorMsg=%s", error.getErrorCode(),
        error.getErrorMsg());
    Log.i("AD_DEMO",str);
    handler.post(new Runnable() {
      @Override
      public void run() {
        ToastUtil.s(str);
      }
    });
    if(loadAdOnly && !showingAd) {
      loadAdOnlyStatusTextView.setText(str);
      return;//只拉取广告时，不终止activity
    }    /**
     * 为防止无广告时造成视觉上类似于"闪退"的情况，设定无广告时页面跳转根据需要延迟一定时间，demo
     * 给出的延时逻辑是从拉取广告开始算开屏最少持续多久，仅供参考，开发者可自定义延时逻辑，如果开发者采用demo
     * 中给出的延时逻辑，也建议开发者考虑自定义minSplashTimeWhenNoAD的值
     **/
    long alreadyDelayMills = System.currentTimeMillis() - fetchSplashADTime;//从拉广告开始到onNoAD已经消耗了多少时间
    long shouldDelayMills = alreadyDelayMills > minSplashTimeWhenNoAD ? 0 : minSplashTimeWhenNoAD
        - alreadyDelayMills;//为防止加载广告失败后立刻跳离开屏可能造成的视觉上类似于"闪退"的情况，根据设置的minSplashTimeWhenNoAD
    // 计算出还需要延时多久
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (needStartDemoList) {
          try {
            SplashActivity.this.startActivity(new Intent(SplashActivity.this, BuildConfig.demolist));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        SplashActivity.this.finish();
      }
    }, shouldDelayMills);
  }

  /**
   * 设置一个变量来控制当前开屏页面是否可以跳转，当开屏广告为普链类广告时，点击会打开一个广告落地页，此时开发者还不能打开自己的App主页。当从广告落地页返回以后，
   * 才可以跳转到开发者自己的App主页；当开屏广告是App类广告时只会下载App。
   */
  private void next() {
    if (canJump) {
      if (needStartDemoList) {
        try {
          this.startActivity(new Intent(this, BuildConfig.demolist));
        } catch (Exception e) {
        }
      }
      this.finish();
    } else {
      canJump = true;
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    canJump = false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    // 获取开屏配置（是否适配全面屏等）
    getSplashAdSettings();
    if (canJump) {
      next();
    }
    canJump = true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    handler.removeCallbacksAndMessages(null);
  }

  /** 开屏页一定要禁止用户对返回按钮的控制，否则将可能导致用户手动退出了App而广告无法正常曝光和计费 */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
      if(keyCode == KeyEvent.KEYCODE_BACK && loadAdOnlyView.getVisibility() == View.VISIBLE){
        return super.onKeyDown(keyCode, event);
      }
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.splash_load_ad_close:
        mLoadSuccess = false;
        this.finish();
        break;
      case R.id.is_ad_valid_button:
        DemoUtil.isAdValid(mLoadSuccess, splashAD != null && splashAD.isValid(), false);
        break;
      case R.id.splash_load_ad_refresh:
        mLoadSuccess = false;
        showingAd = false;
        if (isFullScreen) {
          splashAD.fetchFullScreenAdOnly();
        } else {
          splashAD.fetchAdOnly();
        }
        this.loadAdOnlyStatusTextView.setText(R.string.splash_loading);
        loadAdOnlyDisplayButton.setEnabled(false);
        break;
      case R.id.splash_load_ad_display:
        loadAdOnly = false;
        // 设置是否全屏显示
        setSystemUi();
        loadAdOnlyView.setVisibility(View.GONE);
        showingAd = true;
        if (isFullScreen) {
          splashAD.showFullScreenAd(container);
        } else {
          splashAD.showAd(container);
        }
        break;
      default:
    }
  }

  private void hideSystemUI() {
    int systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
        View.SYSTEM_UI_FLAG_FULLSCREEN |
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      systemUiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    } else {
      systemUiVisibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
    }
    Window window = this.getWindow();
    window.getDecorView().setSystemUiVisibility(systemUiVisibility);
    // 五要素隐私详情页或五要素弹窗关闭回到开屏广告时，再次设置SystemUi
    window.getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> setSystemUi());

    // Android P 官方方法
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      WindowManager.LayoutParams params = window.getAttributes();
      params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      window.setAttributes(params);
    }
  }

  private void showSystemUI() {
    int systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    Window window = this.getWindow();
    window.getDecorView().setSystemUiVisibility(systemUiVisibility);
    // 五要素隐私详情页或五要素弹窗关闭回到开屏广告时，再次设置SystemUi
    window.getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> setSystemUi());
  }

  private void setSystemUi() {
    if (loadAdOnly || !isNotchAdaptation) {
      showSystemUI();
    } else {
      hideSystemUI();
    }
  }

  private void getSplashAdSettings() {
    SharedPreferences sp = this.getSharedPreferences("com.qq.e.union.demo.debug", Context.MODE_PRIVATE);
    String splashAdNotchSetting = sp.getString("splashAdNotchAdaptation", "true");
    isNotchAdaptation = Boolean.parseBoolean(splashAdNotchSetting);
  }
}
