package com.qq.e.union.demo;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.union.demo.util.PxUtil;

import java.util.ArrayList;
import java.util.List;

public class NativeADUnifiedAdInfoView extends ConstraintLayout {

  private CardView mDownloadButton;
  private ImageView mImgLogo;
  private ImageView mImgLogoDownload;
  private TextView mDownloadButtonText;
  private TextView mAdTitle;
  private TextView mAdDesc;
  private AnimatorSet mAnimatorSet;
  private ValueAnimator mEndAnimator;
  private Boolean isVideo = null;
  private AQuery aQuery;
  private BitmapAjaxCallback callback;


  public NativeADUnifiedAdInfoView(Context context) {
    super(context);
    initView(context);
  }

  public NativeADUnifiedAdInfoView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initView(context);
  }

  public NativeADUnifiedAdInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initView(context);
  }

  private void initView(Context context) {
    inflate(context, R.layout.layout_native_unified_ad_info, this);
    mDownloadButton = findViewById(R.id.btn_download);
    mImgLogo = findViewById(R.id.img_logo);
    mImgLogoDownload = findViewById(R.id.img_logo_download);
    mDownloadButtonText = findViewById(R.id.btn_download_text);
    mAdTitle = findViewById(R.id.text_title);
    mAdDesc = findViewById(R.id.text_desc);
    resetUI();
  }

  public List<View> getClickableViews() {
    List<View> views = new ArrayList<>();
    views.add(mImgLogo);
    views.add(mImgLogoDownload);
    views.add(mDownloadButton);
    return views;
  }

  public void setAdInfo(NativeUnifiedADData ad) {
    if (!TextUtils.isEmpty(ad.getIconUrl())) {
      if (aQuery == null) {
        aQuery = new AQuery(mImgLogo);
      }
      if (callback == null) {
        callback = new BitmapAjaxCallback() {
          @Override
          protected void callback(String url, ImageView iv, Bitmap bm, AjaxStatus status) {
            super.callback(url, iv, bm, status);
            if (bm != null) {
              mImgLogo.setImageBitmap(bm);
              mImgLogoDownload.setVisibility(View.VISIBLE);
            }
          }
        };
      }
      aQuery.image(ad.getIconUrl(), false, true, 0, 0, callback);
    }
    mAdTitle.setText(ad.getTitle());
    mAdDesc.setText(ad.getDesc());
    isVideo = ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO;
  }

  public void updateAdAction(NativeUnifiedADData ad) {
    // 更新 Ad 按钮文案
    NativeADUnifiedSampleActivity.updateAdAction(mDownloadButtonText, ad);
    // 更新 Ad 文案侧图标
    updateAdButtonIcon(ad);
  }

  public void resetUI() {
    hideProgressBar();
    mDownloadButton.setCardBackgroundColor(Color.parseColor("#19ffffff"));
    mDownloadButton.setVisibility(View.GONE);
    mDownloadButtonText.setVisibility(View.GONE);
    mAdTitle.setTranslationY(0);
    mAdDesc.setTranslationY(0);
  }

  public void playAnim() {
    // 视频播放第4秒 or 图片出现后
    // 文案组：上移94px，离底238px，300ms
    // 按钮背景：出现，#FFFFFF，透明度0%-10%，300ms
    // 按钮图标&文字：出现，透明度从0%-100%，300ms
    // 2s后
    // 按钮背景颜色：从 #FFFFFF-10% 变为 #3185FC，300ms
    if (mAnimatorSet == null) {
      int yValue = PxUtil.dpToPx(getContext(), 42);
      ObjectAnimator titleAnim =
          ObjectAnimator.ofFloat(mAdTitle, "translationY", 0, -yValue).setDuration(300);
      ObjectAnimator descAnim =
          ObjectAnimator.ofFloat(mAdDesc, "translationY", 0, -yValue).setDuration(300);
      ObjectAnimator buttonBGAnim =
          ObjectAnimator.ofFloat(mDownloadButton, "alpha", 0, 1).setDuration(300);
      ObjectAnimator buttonTextAnim =
          ObjectAnimator.ofFloat(mDownloadButtonText, "alpha", 0, 1).setDuration(300);
      mAnimatorSet = new AnimatorSet();
      mAnimatorSet.addListener(new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
          mDownloadButton.setVisibility(View.VISIBLE);
          mDownloadButtonText.setVisibility(View.VISIBLE);
          if (mEndAnimator == null) {
            mEndAnimator = ObjectAnimator.ofInt(mDownloadButton, "cardBackgroundColor",
                Color.parseColor("#19ffffff"),
                Color.parseColor("#3185FC")).setDuration(300);
            mEndAnimator.setEvaluator(new ArgbEvaluator());
            mEndAnimator.setStartDelay(1700);
          }
          mEndAnimator.start();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
      });
      mAnimatorSet.setInterpolator(new LinearInterpolator());
      mAnimatorSet.play(titleAnim).with(descAnim).with(buttonBGAnim).with(buttonTextAnim);
    }
    if (isVideo != null && isVideo) {
      mAnimatorSet.setStartDelay(4000);
      showProgressBar();
    } else {
      mAnimatorSet.setStartDelay(0);
    }
    mAnimatorSet.start();
  }

  private void pauseAnim() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      if (mAnimatorSet != null && mAnimatorSet.isStarted()) {
        mAnimatorSet.pause();
      }
      if (mEndAnimator != null && mEndAnimator.isStarted()) {
        mEndAnimator.pause();
      }
    }
  }

  private void resumeAnim() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      if (mAnimatorSet != null && mAnimatorSet.isPaused()) {
        mAnimatorSet.resume();
      }
      if (mEndAnimator != null && mEndAnimator.isPaused()) {
        mEndAnimator.resume();
      }
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    pauseAnim();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (isVideo == null) return;
    if (isVideo) {
      resumeAnim();
    } else {
      // 图文动画比较短，每次显示在屏幕上时，重新播放一次
      resetUI();
      playAnim();
    }
  }

  private void updateAdButtonIcon(NativeUnifiedADData ad) {
    if (ad.isWeChatCanvasAd() || !ad.isAppAd()) {
      mImgLogoDownload.setImageResource(R.drawable.icon_link);
      mDownloadButtonText.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getContext(),
          R.drawable.icon_to_link), null, null, null);
      return;
    }
    mImgLogoDownload.setImageResource(R.drawable.icon_download_gray);
    mDownloadButtonText.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getContext(),
        R.drawable.icon_download_gray), null, null, null);
  }

  private void showProgressBar() {
    // 留出2dp的间距显示progressbar
    // progressbar的高度为2dp，且在下一视图层级
    // 视频需要显示progressbar，图文不需要
    setPadding(0, 0, 0, PxUtil.dpToPx(getContext(), 2));
  }

  private void hideProgressBar() {
    setPadding(0, 0, 0, 0);
  }
}
