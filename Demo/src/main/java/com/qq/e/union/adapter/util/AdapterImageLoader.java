package com.qq.e.union.adapter.util;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ImageView;
import com.androidquery.AQuery;

/**
 * 此类是Demo实现，开发者需要根据实际接入的图片加载库进行修改
 */
public class AdapterImageLoader implements IImageLoader {
  private final AQuery mAQuery;
  public AdapterImageLoader(Context context){
    mAQuery = new AQuery(context);
  }

  @Override
  public void displayImage(ImageView imageView, String url) {
    if (imageView == null || TextUtils.isEmpty(url)) {
      return;
    }
    mAQuery.id(imageView).image(url, false, true, 0, 0);
  }
}
