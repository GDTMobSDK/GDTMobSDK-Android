package com.qq.e.union.adapter.tt.util;

public class LoadAdUtil {

  public static void load(TTAdManagerHolder.InitCallBack initCallBack) {
    TTAdManagerHolder.InitStatus initStatus = TTAdManagerHolder.getSdkInitStatus();
    if (initStatus.equals(TTAdManagerHolder.InitStatus.UN_INIT)) {
      TTAdManagerHolder.registerInitCallback(initCallBack);
    } else if (initStatus.equals(TTAdManagerHolder.InitStatus.INIT_SUCCESS)) {
      initCallBack.onInitSuccess();
    } else {
      initCallBack.onInitFail();
    }
  }
}
