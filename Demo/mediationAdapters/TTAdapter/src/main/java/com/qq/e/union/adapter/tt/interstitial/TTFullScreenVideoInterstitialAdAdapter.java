package com.qq.e.union.adapter.tt.interstitial;

import android.app.Activity;

public class TTFullScreenVideoInterstitialAdAdapter extends TTExpressInterstitialAdAdapter {

  public TTFullScreenVideoInterstitialAdAdapter(Activity context, String appId, String posId,
                                                String ext) {
    super(context, appId, posId, ext);
  }

  @Override
  public void loadAd() {
    super.loadFullScreenAD();
  }

  @Override
  public void show() {
    this.show(mContext);
  }

  @Override
  public void show(Activity activity) {
    super.showFullScreenAD(activity);
  }

  @Override
  public void showAsPopupWindow() {
    this.show();
  }

  @Override
  public void showAsPopupWindow(Activity act) {
    this.show(act);
  }

}
