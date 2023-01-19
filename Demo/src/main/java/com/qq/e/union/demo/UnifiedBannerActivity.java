package com.qq.e.union.demo;

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;

import com.qq.e.ads.banner2.UnifiedBannerADListener;
import com.qq.e.ads.banner2.UnifiedBannerView;
import com.qq.e.comm.listeners.NegativeFeedbackListener;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.adapter.PosIdArrayAdapter;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.util.PxUtils;
import com.qq.e.union.demo.util.ToastUtil;
import com.qq.e.union.demo.view.S2SBiddingDemoUtils;

import java.util.Locale;


public class UnifiedBannerActivity extends BaseActivity implements OnClickListener,
    UnifiedBannerADListener, AdapterView.OnItemSelectedListener {

  private static final String TAG = UnifiedBannerActivity.class.getSimpleName();
  ViewGroup mBannerContainer;
  UnifiedBannerView mBannerView;
  String mCurrentPosId;
  String mS2SBiddingToken;
  private boolean mLoadSuccess;
  private PosIdArrayAdapter mArrayAdapter;
  private EditText mPosIdEdit;
  private CheckBox cbCustomWidth;
  private EditText etCustomWidth;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_unified_banner);
    cbCustomWidth = findViewById(R.id.cbCustomWidth);
    etCustomWidth = findViewById(R.id.etCustomWidth);
    mBannerContainer = (ViewGroup) this.findViewById(R.id.bannerContainer);
    mPosIdEdit = findViewById(R.id.posId);
    mPosIdEdit.setText(PositionId.UNIFIED_BANNER_POS_ID);
    this.findViewById(R.id.refreshBanner).setOnClickListener(this);
    this.findViewById(R.id.closeBanner).setOnClickListener(this);
    this.findViewById(R.id.isAdValid).setOnClickListener(this);
    Spinner spinner = findViewById(R.id.id_spinner);
    mArrayAdapter = new PosIdArrayAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.unified_banner));
    mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(mArrayAdapter);
    spinner.setOnItemSelectedListener(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mBannerView != null) {
      mBannerView.destroy();
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (mBannerView != null) {
      mBannerView.setLayoutParams(getUnifiedBannerLayoutParams());
    }
  }

  protected UnifiedBannerView getBanner() {
    String editPosId = getPosID();
    if (mBannerView == null || !editPosId.equals(mCurrentPosId) || !TextUtils.isEmpty(mS2SBiddingToken)) {
      if(this.mBannerView != null){
        mBannerView.destroy();
      }
      Log.d(TAG, "getBanner: BiddingToken " + mS2SBiddingToken);
      if (!TextUtils.isEmpty(mS2SBiddingToken)) {
        mBannerView = new UnifiedBannerView(this, editPosId, this, null, mS2SBiddingToken);
      } else {
        mBannerView = new UnifiedBannerView(this, editPosId, this);
      }
      mBannerView.setLoadAdParams(DemoUtil.getLoadAdParams("banner"));
      mCurrentPosId = editPosId;
      mBannerContainer.removeAllViews();
      mBannerContainer.addView(mBannerView, getUnifiedBannerLayoutParams());
    } else {
      mBannerView.setLayoutParams(getUnifiedBannerLayoutParams());
    }
    if (((CheckBox) findViewById(R.id.cbRefreshInterval)).isChecked()) {
      try {
        int refreshInterval = Integer.parseInt(((EditText) findViewById(R.id.etRefreshInterval))
            .getText().toString());
        this.mBannerView.setRefresh(refreshInterval);
      } catch (NumberFormatException e) {
        ToastUtil.l("请输入合法的轮播时间间隔!");
      }
    } else {
      // 默认 30 秒轮播，可以不设置
      this.mBannerView.setRefresh(30);
    }
    mBannerView.setNegativeFeedbackListener(new NegativeFeedbackListener() {
      @Override
      public void onComplainSuccess() {
        Log.d(TAG, "onComplainSuccess");
      }
    });
    return this.mBannerView;
  }

  /**
   * banner2.0规定banner宽高比应该为6.4:1 , 开发者可自行设置符合规定宽高比的具体宽度和高度值
   *
   * @return
   */
  private FrameLayout.LayoutParams getUnifiedBannerLayoutParams() {
    CheckBox checkBox = findViewById(R.id.cbCustomScale);
    float scale = 6.4F;
    if (checkBox.isChecked()) {
      try {
        scale = Float.parseFloat(((EditText) findViewById(R.id.etCustomScale)).getText().toString());
      } catch (Exception e) {
      }
    }
    String customWidth;
    if (cbCustomWidth.isChecked() && !TextUtils.isEmpty(customWidth = etCustomWidth.getText().toString())) {
      int width = PxUtils.dpToPx(this, Integer.parseInt(customWidth));
      return new FrameLayout.LayoutParams(width, Math.round(width / scale), Gravity.CENTER_HORIZONTAL);
    }
    Point screenSize = new Point();
    getWindowManager().getDefaultDisplay().getSize(screenSize);
    return new FrameLayout.LayoutParams(screenSize.x,  Math.round(screenSize.x / scale));
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.refreshBanner:
        mLoadSuccess = false;
        doRefreshBanner();
        break;
      case R.id.closeBanner:
        mLoadSuccess = false;
        doCloseBanner();
        break;
      case R.id.isAdValid:
        DemoUtil.isAdValid(mLoadSuccess, mBannerView != null && mBannerView.isValid(), false);
        break;
      default:
        break;
    }
  }

  public void requestS2SBiddingToken(View view) {
    S2SBiddingDemoUtils.requestBiddingToken(getPosID(), token -> mS2SBiddingToken = token);
  }

  private void doRefreshBanner() {
    DemoUtil.hideSoftInput(this);
    getBanner().loadAD();
  }

  private void doCloseBanner() {
    mBannerContainer.removeAllViews();
    if (mBannerView != null) {
      mBannerView.destroy();
      mBannerView = null;
    }
  }

  private String getPosID() {
    String posId = mPosIdEdit.getText().toString();
    return TextUtils.isEmpty(posId) ? PositionId.BANNER_POS_ID : posId;
  }

  @Override
  public void onNoAD(AdError adError) {
    String msg = String.format(Locale.getDefault(), "onNoAD, error code: %d, error msg: %s",
        adError.getErrorCode(), adError.getErrorMsg());
    ToastUtil.l(msg);
  }

  @Override
  public void onADReceive() {
    if (mBannerView != null) {
      mLoadSuccess = true;
      Log.i(TAG, "onADReceive" + ", ECPM: " + mBannerView.getECPM() + ", ECPMLevel: "
          + mBannerView.getECPMLevel() + ", adNetWorkName: " + mBannerView.getAdNetWorkName()
          + ", testExtraInfo:" + mBannerView.getExtraInfo().get("mp")
          + ", request_id:" + mBannerView.getExtraInfo().get("request_id"));
      if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
        mBannerView.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
      }
      reportBiddingResult(mBannerView);
    }
  }

  /**
   * 上报给优量汇服务端在开发者客户端竞价中优量汇的竞价结果，以便于优量汇服务端调整策略提供给开发者更合理的报价
   *
   * 优量汇竞价失败调用 sendLossNotification，并填入优量汇竞败原因（必填）、竞胜ADN ID（选填）、竞胜ADN报价（选填）
   * 优量汇竞价胜出调用 sendWinNotification
   * 请开发者如实上报相关参数，以保证优量汇服务端能根据相关参数调整策略，使开发者收益最大化
   */
  private void reportBiddingResult(UnifiedBannerView unifiedBannerView) {
    DemoBiddingC2SUtils.reportBiddingWinLoss(unifiedBannerView);
    if (DemoUtil.isNeedSetBidECPM()) {
      unifiedBannerView.setBidECPM(300);
    }
  }

  @Override
  public void onADExposure() {
    Log.i(TAG, "onADExposure");
  }

  @Override
  public void onADClosed() {
    Log.i(TAG, "onADClosed");
  }

  @Override
  public void onADClicked() {
    Log.i(TAG, "onADClicked : ");
  }

  @Override
  public void onADLeftApplication() {
    Log.i(TAG, "onADLeftApplication");
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    mArrayAdapter.setSelectedPos(position);
    mPosIdEdit.setText(getResources().getStringArray(R.array.unified_banner_value)[position]);
    getBanner().loadAD();
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }
}
