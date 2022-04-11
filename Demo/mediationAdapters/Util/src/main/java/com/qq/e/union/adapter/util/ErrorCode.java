package com.qq.e.union.adapter.util;

public class ErrorCode {

  /**
   * 由于网络连接问题，广告请求失败
   */
  public static final int ERROR_CODE_NETWORK_ERROR = 3001;

  /**
   * 开屏广告容器尺寸错误
   */
  public static final int SPLASH_CONTAINER_SIZE = 4005;

  /**
   * 请求超时错误
   */
  public static final int TIME_OUT = 4011;

  /**
   * 应用未在前台运行时，广告无法展示
   */
  public static final int ERROR_CODE_APP_NOT_FOREGROUND = 4014;

  /**
   * 视频素材下载错误
   */
  public static final int VIDEO_DOWNLOAD_FAIL = 5002;

  /**
   * 视频素材播放错误
   */
  public static final int VIDEO_PLAY_ERROR = 5003;

  /**
   * 广告请求成功，但由于缺少广告资源，未返回广告
   */
  public static final int NO_AD_FILL = 5004;

  /**
   * 广告请求无效
   */
  public static final int ERROR_CODE_INVALID_REQUEST = 5010;

  /**
   * 其他错误，SDK版本不受支持或过期等
   */
  public static final int ERROR_CODE_OTHER = 6000;

  /**
   * 第三方ADN不返回错误码时上报的默认错误码
   */
  public static final int DEFAULT_ERROR_CODE = -1;

  /**
   * 第三方ADN不返回错误信息时上报的默认错误信息
   */
  public static final String DEFAULT_ERROR_MESSAGE = "no_reason";

}
