package com.qq.e.union.demo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.widget.VideoView;

public class FullScreenVideoView extends VideoView {

  public FullScreenVideoView(Context context) {
    super(context);
    init();
  }

  public FullScreenVideoView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public FullScreenVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  /**
   * 解决在android 12 video播放过程中会缩小的问题
   */
  private void init() {
    SurfaceHolder holder = getHolder();
    if (holder == null) {
      return;
    }
    holder.addCallback(new SurfaceHolder.Callback() {
      @Override
      public void surfaceCreated(SurfaceHolder holder) {

      }

      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        holder.setFixedSize(getMeasuredWidth(), getMeasuredHeight());
      }

      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {

      }
    });
  }


  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = getDefaultSize(0, widthMeasureSpec);
    int height = getDefaultSize(0, heightMeasureSpec);
    setMeasuredDimension(width, height);
  }
}
