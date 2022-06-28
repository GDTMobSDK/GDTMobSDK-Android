package com.qq.e.union.demo;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.MediaView;
import com.qq.e.ads.nativ.NativeADEventListener;
import com.qq.e.ads.nativ.NativeADMediaListener;
import com.qq.e.ads.nativ.NativeADUnifiedListener;
import com.qq.e.ads.nativ.NativeUnifiedAD;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.ads.nativ.widget.NativeAdContainer;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.util.PxUtils;
import com.qq.e.union.demo.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NativeADUnifiedFullScreenFeedActivity extends BaseActivity implements NativeADUnifiedListener {

  private static final String TAG = NativeADUnifiedFullScreenFeedActivity.class.getSimpleName();

  private NativeUnifiedAD mAdManager;
  private List<NativeUnifiedADData> mAds = new ArrayList<>();

  private ItemAdapter mAdapter;

  private NativeADUnifiedFullScreenFeedActivity.H mHandler = new NativeADUnifiedFullScreenFeedActivity.H();

  private static final int INIT_ITEM_COUNT = 2;
  private static final int ITEM_COUNT = 5;
  private static final int AD_COUNT = 3;
  private static final int MSG_REFRESH_LIST = 1;

  private static final int TYPE_DATA = 0;
  private static final int TYPE_AD = 1;

  private ViewPagerLayoutManager mLayoutManager;
  private RecyclerView mRecyclerView;

  private int mCurrentPage = -1;
  private int mVideoViewCurrentPosition=-1;
  private VideoView mCurrentVideoView;
  private boolean videoIsPaused=false;

  private int[] mVideoIds = new int[]{R.raw.v1, R.raw.v2, R.raw.v3, R.raw.v4, R.raw.v5};
  private int[] mImageIds = new int[]{R.raw.p1, R.raw.p2, R.raw.p3, R.raw.p4, R.raw.p5};
  private boolean mBindToCustomView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_unified_ad_recyclerview);
    initView();

    mBindToCustomView = getIntent().getBooleanExtra(Constants.BUTTON_BIND_TO_CUSTOM_VIEW, false);

    mAdManager = new NativeUnifiedAD(this, getPosId(), this);
    mAdManager.setMinVideoDuration(getMinVideoDuration());
    mAdManager.setMaxVideoDuration(getMaxVideoDuration());

    mAdManager.loadData(AD_COUNT);
  }

  private String getPosId() {
    return getIntent().getStringExtra(Constants.POS_ID);
  }

  private int getMinVideoDuration() {
    return getIntent().getIntExtra(Constants.MIN_VIDEO_DURATION, 0);
  }

  private int getMaxVideoDuration() {
    return getIntent().getIntExtra(Constants.MAX_VIDEO_DURATION, 0);
  }

  private void initView() {
    mRecyclerView = findViewById(R.id.recycler_view);
    mLayoutManager = new ViewPagerLayoutManager(this, LinearLayoutManager.VERTICAL);
    mLayoutManager.setOnViewPagerListener(new OnViewPagerListener() {
      @Override
      public void onInitComplete() {
        if(mAdapter.getItem(0).type == TYPE_DATA){
          play();
        }
        mCurrentPage = 0;
      }

      @Override
      public void onPageRelease(boolean isNext, int position) {
        if(mAdapter.getItem(position).type == TYPE_DATA){
          releaseVideo(isNext ? 0 : 1);
        }
      }

      @Override
      public void onPageSelected(int position, boolean isBottom) {
        if(mAdapter.getItem(position).type == TYPE_DATA){
          play();
        }
        mCurrentPage = position;
      }
    });

    mRecyclerView.setLayoutManager(mLayoutManager);

    List<NativeADUnifiedFullScreenFeedActivity.Item> list = new ArrayList<>();
    // 初始视频，防止拉取广告网络异常时页面空白
    for (int i = 0; i < INIT_ITEM_COUNT; ++i) {
      list.add(new Item(i));
    }
    mAdapter = new ItemAdapter(this, list);
    mRecyclerView.setAdapter(mAdapter);
  }

  @Override
  public void onADLoaded(List<NativeUnifiedADData> ads) {
    // 防止在onDestory后网络回包
    if(mAds != null){
      ToastUtil.s("拉取到 " + ads.size() + " 条广告");
      mAds.addAll(ads);
      Message msg = mHandler.obtainMessage(MSG_REFRESH_LIST, ads);
      mHandler.sendMessage(msg);
    }
  }

  @Override
  public void onNoAD(AdError error) {
    ToastUtil.s("没有拉到广告!");
    Log.d(TAG, "onNoAd error code: " + error.getErrorCode()
        + ", error msg: " + error.getErrorMsg());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if(videoIsPaused){
      mCurrentVideoView.seekTo(mVideoViewCurrentPosition);
      mCurrentVideoView.start();
      videoIsPaused=false;
    }
  }

  @Override
  protected void onPause(){
    super.onPause();
    Item item =mAdapter.getItem(mCurrentPage);
    if(item.type==TYPE_DATA){
      if (mLayoutManager.findViewByPosition(mCurrentPage) == null) {
        return;
      }
      mCurrentVideoView = mLayoutManager.findViewByPosition(mCurrentPage)
              .findViewById(R.id.video_view);
      mVideoViewCurrentPosition=mCurrentVideoView.getCurrentPosition();
      mCurrentVideoView.pause();
      videoIsPaused=true;
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mAds != null) {
      for (NativeUnifiedADData ad : mAds) {
        ad.destroy();
      }
    }
    mAds = null;
  }

  class ItemAdapter extends RecyclerView.Adapter<ItemHolder> {

    private List<Item> mData;
    private Context mContext;

    public ItemAdapter(Context context, List list) {
      mContext = context;
      mData = list;
    }

    public Item getItem(int position){
      return mData.get(position);
    }

    public void addItem(NativeADUnifiedFullScreenFeedActivity.Item item){
      mData.add(item);
    }

    public void addItemToPosition(Item item, int position) {
      if (position >= 0 && position < mData.size()) {
        mData.add(position, item);
      }
    }

    @Override
    public int getItemViewType(int position) {
      return mData.get(position).type;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view;
      switch (viewType) {
        case TYPE_AD:
          view = LayoutInflater.from(mContext).inflate(R.layout.activity_native_unified_ad_full_screen, parent, false);
          break;

        case TYPE_DATA:
          view = LayoutInflater.from(mContext).inflate(R.layout.item_full_screen_video_feed, parent, false);
          break;

        default:
          view = null;
      }
      return new ItemHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(final ItemHolder holder, int position) {
      switch (getItemViewType(position)) {
        case TYPE_AD:
          initADItemView(position, holder);
          break;
        case TYPE_DATA:
          holder.title.setText(mData.get(position).title);
          holder.videoView.setVideoURI(mData.get(position).videoUri);
          holder.coverImage.setImageURI(mData.get(position).imageUri);
          break;
      }
    }

    private void initADItemView(int position, final ItemHolder holder) {
      Item item = mData.get(position);
      final NativeUnifiedADData ad = item.ad;
      holder.adInfoView.setAdInfo(ad);
      // 视频广告
      if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
        holder.poster.setVisibility(View.INVISIBLE);
        holder.mediaView.setVisibility(View.VISIBLE);
      } else {
        holder.poster.setVisibility(View.VISIBLE);
        holder.mediaView.setVisibility(View.INVISIBLE);
      }
      holder.adInfoContainer.setVisibility(View.VISIBLE);
      List<View> clickableViews = new ArrayList<>();
      List<View> customClickableViews = new ArrayList<>();
      if (mBindToCustomView) {
        customClickableViews.addAll(holder.adInfoView.getClickableViews());
      } else {
        clickableViews.addAll(holder.adInfoView.getClickableViews());
      }
      ArrayList<ImageView>imageViews = new ArrayList<>();
      if(ad.getAdPatternType() == AdPatternType.NATIVE_2IMAGE_2TEXT ||
          ad.getAdPatternType() == AdPatternType.NATIVE_1IMAGE_2TEXT){
        // 双图双文、单图双文：注册mImagePoster的点击事件
        clickableViews.add(holder.poster);
        imageViews.add(holder.poster);
      }
      FrameLayout.LayoutParams adLogoParams = new FrameLayout.LayoutParams(PxUtils.dpToPx(mContext
          , 46),
          PxUtils.dpToPx(mContext, 14));
      adLogoParams.gravity = Gravity.END | Gravity.BOTTOM;
      adLogoParams.rightMargin = PxUtils.dpToPx(mContext, 10);
      adLogoParams.bottomMargin = PxUtils.dpToPx(mContext, 10);
      //作为customClickableViews传入，点击不进入详情页，直接下载或进入落地页，图文、视频广告均生效，
      ad.bindAdToView(NativeADUnifiedFullScreenFeedActivity.this, holder.container, adLogoParams,
          clickableViews, customClickableViews);

      if (!imageViews.isEmpty()) {
        ad.bindImageViews(imageViews, 0);
      }
      setAdListener(holder, ad);
      holder.adInfoView.updateAdAction(ad);
    }

    private void setAdListener(final ItemHolder holder, final NativeUnifiedADData ad) {
      //如果需要获得点击view的信息使用NativeADEventListenerWithClickInfo代替NativeADEventListener
      ad.setNativeAdEventListener(new NativeADEventListener() {
        @Override
        public void onADExposed() {
          Log.d(TAG, "onADExposed: " + ad.getTitle());
        }

        @Override
        public void onADClicked() {
          Log.d(TAG, "onADClicked: " + ad.getTitle());
        }

        @Override
        public void onADError(AdError error) {
          Log.d(TAG, "onADError error code :" + error.getErrorCode()
              + "  error msg: " + error.getErrorMsg());
        }

        @Override
        public void onADStatusChanged() {
          holder.adInfoView.updateAdAction(ad);
        }
      });
      // 视频广告
      if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
        VideoOption videoOption = NativeADUnifiedSampleActivity.getVideoOption(getIntent());
        ad.bindMediaView(holder.mediaView, videoOption, new NativeADMediaListener() {
          @Override
          public void onVideoInit() {
            Log.d(TAG, "onVideoInit: ");
          }

          @Override
          public void onVideoLoading() {
            Log.d(TAG, "onVideoLoading: ");
          }

          @Override
          public void onVideoReady() {
            Log.d(TAG, "onVideoReady ");
          }

          @Override
          public void onVideoLoaded(int videoDuration) {
            Log.d(TAG, "onVideoLoaded: ");
          }

          @Override
          public void onVideoStart() {
            Log.d(TAG, "onVideoStart ");
            holder.adInfoContainer.setVisibility(View.VISIBLE);
            holder.adInfoView.playAnim();
          }

          @Override
          public void onVideoPause() {
            Log.d(TAG, "onVideoPause: ");
          }

          @Override
          public void onVideoResume() {
            Log.d(TAG, "onVideoResume: ");
          }

          @Override
          public void onVideoCompleted() {
            Log.d(TAG, "onVideoCompleted: ");
            holder.adInfoContainer.setVisibility(View.GONE);
            holder.adInfoView.resetUI();
          }

          @Override
          public void onVideoError(AdError error) {
            Log.d(TAG, "onVideoError: ");
          }

          @Override
          public void onVideoStop() {
            Log.d(TAG, "onVideoStop");
          }

          @Override
          public void onVideoClicked() {
            Log.d(TAG, "onVideoClicked");
          }
        });
      }
    }

    @Override
    public int getItemCount() {
      return mData.size();
    }
  }

  private void play(){
    View itemView = mRecyclerView.getChildAt(0);
    final VideoView videoView = itemView.findViewById(R.id.video_view);
    final View coverImage = itemView.findViewById(R.id.cover_image);
    if (videoView != null) {
      videoView.start();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
          @Override
          public boolean onInfo(MediaPlayer mp, int what, int extra) {
            Log.d(TAG, "onInfo");
            mp.setLooping(true);
            coverImage.animate().alpha(0).setDuration(200).start();
            return false;
          }
        });
      }
      videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
          Log.d(TAG, "onPrepared");
        }
      });
    }
  }

  private void releaseVideo(int index) {
    View itemView = mRecyclerView.getChildAt(index);
    if (itemView != null) {
      final View coverImage = itemView.findViewById(R.id.cover_image);
      final VideoView videoView = itemView.findViewById(R.id.video_view);
      if (videoView != null) {
        videoView.stopPlayback();
      }
      coverImage.animate().alpha(1).start();
    }
  }

  class ItemHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public VideoView videoView;
    public ImageView coverImage;
    public MediaView mediaView;
    public RelativeLayout adInfoContainer;
    public ImageView poster;
    public NativeADUnifiedAdInfoView adInfoView;
    public NativeAdContainer container;
    public CheckBox btnMute;

    public ItemHolder(View itemView, int adType) {
      super(itemView);
      switch (adType) {
        case TYPE_AD:
          mediaView = itemView.findViewById(R.id.gdt_media_view);
          adInfoContainer = itemView.findViewById(R.id.ad_info_container);
          poster = itemView.findViewById(R.id.img_poster);
          container = itemView.findViewById(R.id.native_ad_container);
          adInfoView = itemView.findViewById(R.id.ad_info_view);
          btnMute = itemView.findViewById(R.id.btn_mute);

        case TYPE_DATA:
          title = itemView.findViewById(R.id.title);
          videoView = itemView.findViewById(R.id.video_view);
          coverImage = itemView.findViewById(R.id.cover_image);
          break;

      }
    }
  }

  private class Item {

    public int type;
    public int position;

    public Uri imageUri;
    public Uri videoUri;
    public String title;

    public NativeUnifiedADData ad;

    public Item(int position){
      this.type = TYPE_DATA;
      this.title = "第 " + (position + 1) + " 个普通视频";
      this.videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + mVideoIds[position % mVideoIds.length]);
      this.imageUri = Uri.parse("android.resource://" + getPackageName() + "/" + mImageIds[position % mImageIds.length]);
    }

    public Item(NativeUnifiedADData ad){
      this.type = TYPE_AD;
      this.ad = ad;
    }

  }

  private class H extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_REFRESH_LIST:

          for(int i = INIT_ITEM_COUNT; i < ITEM_COUNT; i++){
            mAdapter.addItem(new Item(i));
          }

          List<NativeUnifiedADData> ads = (List<NativeUnifiedADData>) msg.obj;
          if (ads != null && ads.size() > 0 && mAdapter != null) {
            Random random = new Random();
            for (int i = 0; i < ads.size(); i++) {
              int index = Math.abs(random.nextInt()) % ITEM_COUNT;

              while(index == mCurrentPage){
                index = Math.abs(random.nextInt()) % ITEM_COUNT;
              }
              if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
                ads.get(i).setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
              }
              mAdapter.addItemToPosition(new Item(ads.get(i)), index);

              Log.d(TAG,
                  i + ": eCPMLevel = " + ads.get(i).getECPMLevel() + " , videoDuration = " + ads.get(i).getVideoDuration());
            }
          }
          mAdapter.notifyDataSetChanged();
          break;

        default:
      }
    }
  }

  private interface OnViewPagerListener {
    void onInitComplete();

    void onPageRelease(boolean isNext, int position);

    void onPageSelected(int position, boolean isBottom);
  }

  private class ViewPagerLayoutManager extends LinearLayoutManager {
    private PagerSnapHelper mPagerSnapHelper;
    private OnViewPagerListener mOnViewPagerListener;
    private RecyclerView mRecyclerView;
    private int mDeltaY;

    private RecyclerView.OnChildAttachStateChangeListener mChildAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {
      public void onChildViewAttachedToWindow(View view) {
        if (mOnViewPagerListener != null && getChildCount() == 1) {
          mOnViewPagerListener.onInitComplete();
        }
      }

      public void onChildViewDetachedFromWindow(View view) {
        if (mDeltaY >= 0) {
          if (mOnViewPagerListener != null) {
            mOnViewPagerListener.onPageRelease(true, getPosition(view));
          }
        } else if (mOnViewPagerListener != null) {
          mOnViewPagerListener.onPageRelease(false, getPosition(view));
        }
      }
    };

    public ViewPagerLayoutManager(Context context, int orientation) {
      super(context, orientation, false);
      mPagerSnapHelper = new PagerSnapHelper();
    }

    public void onAttachedToWindow(RecyclerView view) {
      super.onAttachedToWindow(view);
      mPagerSnapHelper.attachToRecyclerView(view);
      mRecyclerView = view;
      mRecyclerView.addOnChildAttachStateChangeListener(mChildAttachStateChangeListener);
    }

    public void onScrollStateChanged(int state) {
      if(state == RecyclerView.SCROLL_STATE_IDLE){
        View curView = mPagerSnapHelper.findSnapView(this);
        int curPos = getPosition(curView);
        if (mOnViewPagerListener != null && getChildCount() == 1) {
          mOnViewPagerListener.onPageSelected(curPos, curPos == getItemCount() - 1);
        }
      }
    }

    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
      mDeltaY = dy;
      return super.scrollVerticallyBy(dy, recycler, state);
    }

    public void setOnViewPagerListener(OnViewPagerListener listener) {
      mOnViewPagerListener = listener;
    }
  }

}
