package com.qq.e.union.adapter.util;

import android.graphics.Bitmap;
import android.widget.ImageView;

public interface IImageLoader {
  void displayImage(ImageView imageView, String url);

  void displayImage(ImageView imageView, Bitmap bitmap);
}
