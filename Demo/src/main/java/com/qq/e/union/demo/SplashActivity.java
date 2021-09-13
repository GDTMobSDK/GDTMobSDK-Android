package com.qq.e.union.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qq.e.ads.splash.SplashAD;
import com.qq.e.ads.splash.SplashADListener;
import com.qq.e.ads.splash.SplashADZoomOutListener;
import com.qq.e.comm.constants.BiddingLossReason;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.util.SplashZoomOutManager;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.view.ViewUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 这是demo工程的入口Activity，在这里会首次调用优量汇的SDK。
 *
 * 在调用SDK之前，如果您的App的targetSDKVersion >= 23，那么建议动态申请相关权限。
 *
 * 这里为了示例开屏V+广告使用了SplashADZoomOutListener，如不需要使用开屏V+广告可以继续使用之前的SplashADListener
 */
public class SplashActivity extends Activity implements SplashADZoomOutListener,View.OnClickListener {

  private static final String TAG = "SplashActivity";

  private SplashAD splashAD;
  private ViewGroup container;
  private ViewGroup zoomOutView;
  private ImageView splashHolder;

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

  private boolean isZoomOut = false;
  private boolean isSupportZoomOut = true;
  private boolean isZoomOutInAnother = false;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);
    container = (ViewGroup) this.findViewById(R.id.splash_container);
    Intent intent = getIntent();

    splashHolder = (ImageView) findViewById(R.id.splash_holder);
    boolean needLogo = intent.getBooleanExtra("need_logo", true);
    needStartDemoList = intent.getBooleanExtra("need_start_demo_list", true);
    loadAdOnly = intent.getBooleanExtra("load_ad_only", false);
    isSupportZoomOut = intent.getBooleanExtra("support_zoom_out", false);
    isZoomOutInAnother = intent.getBooleanExtra("zoom_out_in_another", false);
    isFullScreen = intent.getBooleanExtra("is_full_screen", false);
    fetchDelay = (Integer) intent.getSerializableExtra("fetch_delay");

    loadAdOnlyView = findViewById(R.id.splash_load_ad_only);
    loadAdOnlyCloseButton = findViewById(R.id.splash_load_ad_close);
    loadAdOnlyCloseButton.setOnClickListener(this);
    loadAdOnlyDisplayButton = findViewById(R.id.splash_load_ad_display);
    loadAdOnlyDisplayButton.setOnClickListener(this);
    loadAdOnlyRefreshButton = findViewById(R.id.splash_load_ad_refresh);
    loadAdOnlyRefreshButton.setOnClickListener(this);
    loadAdOnlyStatusTextView = findViewById(R.id.splash_load_ad_status);

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

  private String getPosId() {
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
    if (!(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)) {
      lackedPermission.add(Manifest.permission.READ_PHONE_STATE);
    }

    if (!(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
      lackedPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    // 快手SDK所需相关权限，存储权限，此处配置作用于流量分配功能，关于流量分配，详情请咨询运营;如果您的APP不需要快手SDK的流量分配功能，则无需申请SD卡权限
    if (!(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )){
      lackedPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
    }
    if (!(checkSelfPermission( Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
      lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

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
      Toast.makeText(this, "应用缺少必要的权限！请点击\"权限\"，打开所需要的权限。", Toast.LENGTH_LONG).show();
      Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      intent.setData(Uri.parse("package:" + getPackageName()));
      startActivity(intent);
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
    if(loadAdOnly) {
      if (isFullScreen) {
        splashAD.fetchFullScreenAdOnly();
      } else {
        splashAD.fetchAdOnly();
      }
    }else{
      if (isFullScreen) {
        splashAD.fetchFullScreenAndShowIn(adContainer);
      } else {
        splashAD.fetchAndShowIn(adContainer);
      }
    }
  }

  protected SplashAD getSplashAd(Activity activity, String posId,
                                 SplashADListener adListener, Integer fetchDelay, String token) {
    SplashAD splashAD;
    Log.d(TAG, "getSplashAd: BiddingToken " + token);
    if (!TextUtils.isEmpty(token)) {
      splashAD = new SplashAD(activity, posId, adListener, fetchDelay == null ? 0 : fetchDelay, null,
          null, token);
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
    Log.i("AD_DEMO", "SplashADClicked clickUrl: "
        + (splashAD.getExt() != null ? splashAD.getExt().get("clickUrl") : ""));
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
    Log.i("AD_DEMO", "SplashADFetch expireTimestamp: " + expireTimestamp
        + ", eCPMLevel = " + splashAD.getECPMLevel()+ ", ECPM: " + splashAD.getECPM()
        + ", testExtraInfo:" + splashAD.getExtraInfo().get("mp"));
    if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
      splashAD.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
    }
    if(loadAdOnly) {
      loadAdOnlyDisplayButton.setEnabled(true);
      long timeIntervalSec = (expireTimestamp-SystemClock.elapsedRealtime())/1000;
      long min = timeIntervalSec/60;
      long second = timeIntervalSec-(min*60);
      loadAdOnlyStatusTextView.setText("加载成功,广告将在:"+min+"分"+second+"秒后过期，请在此之前展示(showAd)");
    }
    reportBiddingResult(splashAD);
  }

  /**
   * 上报给优量汇服务端在开发者客户端竞价中优量汇的竞价结果，以便于优量汇服务端调整策略提供给开发者更合理的报价
   *
   * 优量汇竞价失败调用 sendLossNotification，并填入优量汇竞败原因（必填）、竞胜ADN ID（选填）、竞胜ADN报价（选填）
   * 优量汇竞价胜出调用 sendWinNotification，并填入开发者期望扣费价格（单位分）
   * 请开发者如实上报相关参数，以保证优量汇服务端能根据相关参数调整策略，使开发者收益最大化
   */
  private void reportBiddingResult(SplashAD splashAD) {
    if (DemoUtil.isReportBiddingLoss() == DemoUtil.REPORT_BIDDING_LOSS) {
      splashAD.sendLossNotification(100, BiddingLossReason.LOW_PRICE, "WinAdnID");
    } else if (DemoUtil.isReportBiddingLoss() == DemoUtil.REPORT_BIDDING_WIN) {
      splashAD.sendWinNotification(200);
    }
    if (DemoUtil.isNeedSetBidECPM()) {
      splashAD.setBidECPM(300);
    }
  }

  @Override
  public void onADDismissed() {
    Log.i("AD_DEMO", "SplashADDismissed");
    if (zoomOutView != null) {
      ViewUtils.removeFromParent(zoomOutView);
    }
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
        Toast.makeText(SplashActivity.this.getApplicationContext(), str, Toast.LENGTH_SHORT).show();
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
          SplashActivity.this.startActivity(new Intent(SplashActivity.this, DemoListActivity.class));
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
        this.startActivity(new Intent(this, DemoListActivity.class));
      }
      if (isZoomOut && isZoomOutInAnother) {
        //防止移除view后显示底图导致屏幕闪烁
        Bitmap b = splashAD.getZoomOutBitmap();
        if (b != null) {
          splashHolder.setScaleType(ImageView.ScaleType.CENTER_CROP);
          splashHolder.setImageBitmap(b);
        }
        SplashZoomOutManager zoomOutManager = SplashZoomOutManager.getInstance();
        zoomOutManager.setSplashInfo(splashAD, container.getChildAt(0),
                getWindow().getDecorView());
        this.setResult(RESULT_OK);
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
        this.finish();
        break;
      case R.id.splash_load_ad_refresh:
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

  @Override
  public void onZoomOut() {
    isZoomOut = true;
    Log.d("AD_DEMO", "onZoomOut");
    if (isZoomOutInAnother) {
      next();
    } else {
      SplashZoomOutManager splashZoomOutManager = SplashZoomOutManager.getInstance();
      ViewGroup content = findViewById(android.R.id.content);
      zoomOutView = splashZoomOutManager.startZoomOut(container.getChildAt(0), content, content,
              new SplashZoomOutManager.AnimationCallBack() {
                @Override
                public void animationStart(int animationTime) {
                  Log.d("AD_DEMO", "animationStart:" + animationTime);
                }

                @Override
                public void animationEnd() {
                  Log.d("AD_DEMO", "animationEnd");
                  splashAD.zoomOutAnimationFinish();
                }
              });
      findViewById(R.id.splash_main).setVisibility(View.GONE);
    }
  }

  @Override
  public void onZoomOutPlayFinish() {
    Log.d("AD_DEMO", "onZoomOutPlayFinish");
  }

  @Override
  public boolean isSupportZoomOut() {
    return isSupportZoomOut;
  }
}
