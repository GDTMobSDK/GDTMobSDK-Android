package com.qq.e.union.demo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;


/**
 * 处理开屏oneshot广告图片,剪裁图片的能力
 * 实现仅供参考
 */
public class OneshotImageView extends ImageView {

  private Paint mPaint;
  private Bitmap mOneshotCoverBitmap;

  private int mOriBitmapWidth;
  private int mOriBitmapHeight;

  private int mViewWidth;
  private int mViewHeight;

  private int mShowWidth;
  private int mShowHeight;

  private int mPortX;
  private int mPortY;

  private Rect mClipRect;
  private BitmapFactory.Options mOptions;
  private BitmapRegionDecoder mDecoder;

  private boolean isOneshot = false;

  public OneshotImageView(Context context) {
    super(context);
  }

  public boolean tryCheckOneshotAndInit(String path){
    boolean result = false;
    if(!TextUtils.isEmpty(path)){
      try{
        mDecoder = BitmapRegionDecoder.newInstance(path, true);
        mClipRect = new Rect();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mOptions = new BitmapFactory.Options();
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, mOptions);
        mOriBitmapWidth = mOptions.outWidth;
        mOriBitmapHeight = mOptions.outHeight;

        isOneshot = true;
        result = true;
      }catch (Exception e){
        e.printStackTrace();
      }
    }
    return result;
  }



  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if(isOneshot){
      /**
       * oneshot广告，SDK提供裁剪能力，
       * 当view的宽高属性变化的时候，对图片的高度裁剪，宽度不剪裁
       * clip的大小对应当前view的高度
       */
      mViewWidth = w;
      mViewHeight = h;

      mShowWidth = mOriBitmapWidth > mViewWidth ? mViewWidth : mOriBitmapWidth;
      mShowHeight = mOriBitmapHeight > mViewHeight ? mViewHeight : mOriBitmapHeight;

      mPortX = 0;
      mPortY = (mOriBitmapHeight - mShowHeight) / 2;

      //宽度不剪，left-right边界为图片的原始宽
      mClipRect.set(mPortX, mPortY, mOriBitmapWidth, mPortY + mShowHeight);
      mOneshotCoverBitmap = mDecoder.decodeRegion(mClipRect, mOptions);
      setImageBitmap(mOneshotCoverBitmap);
    }
  }


  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if(mOneshotCoverBitmap != null && !mOneshotCoverBitmap.isRecycled()){
      mOneshotCoverBitmap.recycle();
      mOneshotCoverBitmap = null;
    }
    mDecoder = null;
    mOptions = null;
  }
}
