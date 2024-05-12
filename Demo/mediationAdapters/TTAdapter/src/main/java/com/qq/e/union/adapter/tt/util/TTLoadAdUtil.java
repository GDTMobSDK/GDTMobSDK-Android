package com.qq.e.union.adapter.tt.util;

public class TTLoadAdUtil {
  public static void load(TTAdManagerHolder.InitCallBack initCallBack) {
    TTAdManagerHolder.InitStatus initStatus = TTAdManagerHolder.getSdkInitStatus();
    if (initStatus.equals(TTAdManagerHolder.InitStatus.UN_INIT) || initStatus.equals(TTAdManagerHolder.InitStatus.INITIALIZING)) {
      TTAdManagerHolder.registerInitCallback(initCallBack);
    } else if (initStatus.equals(TTAdManagerHolder.InitStatus.INIT_SUCCESS)) {
      initCallBack.onInitSuccess();
    } else {
      initCallBack.onInitFail();
    }
  }
}
