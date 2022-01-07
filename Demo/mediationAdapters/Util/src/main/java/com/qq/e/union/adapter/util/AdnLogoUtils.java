package com.qq.e.union.adapter.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.qq.e.ads.nativ.widget.NativeAdContainer;

public class AdnLogoUtils {
  public static void initAdLogo(Context context, IImageLoader imageLoader,
                                FrameLayout.LayoutParams adLogoParams,
                                int defaultWidth, int defaultHeight,
                                NativeAdContainer nativeAdContainer, Bitmap bitmap) {
    LogoImageView adLogoView = displayLogoView(context, adLogoParams, defaultWidth, defaultHeight,
        nativeAdContainer);
    imageLoader.displayImage(adLogoView, bitmap);
  }

  public static void initAdLogo(Context context, IImageLoader imageLoader,
                                FrameLayout.LayoutParams adLogoParams,
                                int defaultWidth, int defaultHeight,
                                NativeAdContainer nativeAdContainer, String url) {
    LogoImageView adLogoView = displayLogoView(context, adLogoParams, defaultWidth, defaultHeight,
        nativeAdContainer);
    imageLoader.displayImage(adLogoView, url);
  }

  @NonNull
  private static LogoImageView displayLogoView(Context context,
                                               FrameLayout.LayoutParams adLogoParams,
                                               int defaultWidth, int defaultHeight,
                                               NativeAdContainer nativeAdContainer) {
    if (adLogoParams == null) {
      int width = defaultWidth < 0 ? defaultWidth : PxUtils.dpToPx(context, defaultWidth);
      int height = defaultHeight < 0 ? defaultHeight : PxUtils.dpToPx(context, defaultHeight);
      adLogoParams = new FrameLayout.LayoutParams(width, height);
      adLogoParams.gravity = Gravity.END | Gravity.BOTTOM;
    }
    LogoImageView adLogoView = getAddedLogo(nativeAdContainer);
    if (adLogoView == null) {
      adLogoView = new LogoImageView(context);
      nativeAdContainer.addView(adLogoView, adLogoParams);
    } else { // 如果已经添加过则不需要重复添加
      adLogoView.setLayoutParams(adLogoParams);
      adLogoView.bringToFront();
    }
    return adLogoView;
  }

  private static LogoImageView getAddedLogo(NativeAdContainer container) {
    for (int i = 0; i < container.getChildCount(); i++) {
      View child = container.getChildAt(i);
      if (child instanceof LogoImageView) {
        return (LogoImageView) child;
      }
    }
    return null;
  }
}
