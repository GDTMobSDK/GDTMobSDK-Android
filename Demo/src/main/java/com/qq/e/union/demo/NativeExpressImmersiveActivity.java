package com.qq.e.union.demo;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeExpressAD;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.pi.AdData;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

public class NativeExpressImmersiveActivity extends BaseActivity
    implements NativeExpressAD.NativeExpressADListener {

  private static final String TAG = NativeExpressImmersiveActivity.class.getSimpleName();

  private List<NativeExpressADView> mAds = new ArrayList<>();

  private ItemAdapter mAdapter;

  private final NativeExpressImmersiveActivity.H mHandler = new NativeExpressImmersiveActivity.H();

  private static final int INIT_ITEM_COUNT = 2;
  private static final int ITEM_COUNT = 5;
  private static final int AD_COUNT = 3;
  private static final int MSG_REFRESH_LIST = 1;

  private static final int TYPE_DATA = 0;
  private static final int TYPE_AD = 1;

  private ViewPagerLayoutManager mLayoutManager;
  private RecyclerView mRecyclerView;

  private int mCurrentPage = -1;
  private int mVideoViewCurrentPosition = -1;
  private VideoView mCurrentVideoView;
  private boolean videoIsPaused = false;

  private final int[] mVideoIds = new int[]{R.raw.v1, R.raw.v2, R.raw.v3, R.raw.v4, R.raw.v5};
  private final int[] mImageIds = new int[]{R.raw.p1, R.raw.p2, R.raw.p3, R.raw.p4, R.raw.p5};

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_unified_ad_recyclerview);
    initView();
    initNativeExpressAD();
  }

  protected String getPosId() {
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
        if (mAdapter.getItem(0).type == TYPE_DATA) {
          play();
        }
        mCurrentPage = 0;
      }

      @Override
      public void onPageRelease(boolean isNext, int position) {
        if (mAdapter.getItem(position).type == TYPE_DATA) {
          releaseVideo(isNext ? 0 : 1);
        }
      }

      @Override
      public void onPageSelected(int position, boolean isBottom) {
        if (mAdapter.getItem(position).type == TYPE_DATA) {
          play();
        }
        mCurrentPage = position;
      }
    });

    mRecyclerView.setLayoutManager(mLayoutManager);

    List<NativeExpressImmersiveActivity.Item> list = new ArrayList<>();
    // 初始视频，防止拉取广告网络异常时页面空白
    for (int i = 0; i < INIT_ITEM_COUNT; ++i) {
      list.add(new Item(i));
    }
    mAdapter = new ItemAdapter(this, list);
    mRecyclerView.setAdapter(mAdapter);
  }

  private void initNativeExpressAD() {
    ADSize adSize = new ADSize(ADSize.FULL_WIDTH, ADSize.AUTO_HEIGHT); // 消息流中用AUTO_HEIGHT
    String token = getToken();
    Log.d(TAG, "refreshAd: BiddingToken " + token);
    NativeExpressAD ADManager;
    if (!TextUtils.isEmpty(token)) {
      ADManager = new NativeExpressAD(this, adSize, getPosId(), this, token);
    } else {
      ADManager = new NativeExpressAD(this, adSize, getPosId(), this);
    }
    VideoOption option = NativeExpressADActivity.getVideoOption(getIntent());
    if (option != null) {
      // setVideoOption是可选的，开发者可根据需要选择是否配置
      ADManager.setVideoOption(option);
    }

    ADManager.setMinVideoDuration(getMinVideoDuration());
    ADManager.setMaxVideoDuration(getMaxVideoDuration());
    ADManager.loadAD(AD_COUNT);
  }

  private String getToken() {
    return getIntent().getStringExtra(Constants.TOKEN);
  }

  @Override
  public void onADLoaded(List<NativeExpressADView> ads) {
    // 防止在onDestory后网络回包
    if (mAds != null) {
      ToastUtil.s("拉取到 " + ads.size() + " 条广告");
      mAds.addAll(ads);
      Message msg = mHandler.obtainMessage(MSG_REFRESH_LIST, ads);
      mHandler.sendMessage(msg);
    }
  }

  @Override
  public void onRenderFail(NativeExpressADView adView) {
    Log.i(TAG, "onRenderFail: " + adView.toString());
  }

  @Override
  public void onRenderSuccess(NativeExpressADView adView) {
    Log.i(TAG, "onRenderSuccess: " + adView.toString() + ", adInfo: " + getAdInfo(adView));
    View view = adView.getChildAt(0);
    if (view != null) {
      // NativeExpressADView 里的高度是 WRAP_CONTENT 这里调整为 MATCH_PARENT，保证能充满布局。
      ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
      layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
      view.setLayoutParams(layoutParams);
    }
  }

  private String getAdInfo(NativeExpressADView nativeExpressADView) {
    AdData adData = nativeExpressADView.getBoundData();
    if (adData != null) {
      StringBuilder infoBuilder = new StringBuilder();
      infoBuilder.append("title:").append(adData.getTitle()).append(",").append("desc:")
          .append(adData.getDesc()).append(",").append("patternType:")
          .append(adData.getAdPatternType());
      if (adData.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
        infoBuilder.append(", video info: ")
            .append(getVideoInfo(adData.getProperty(AdData.VideoPlayer.class)));
      }
      return infoBuilder.toString();
    }
    return null;
  }

  private String getVideoInfo(AdData.VideoPlayer videoPlayer) {
    if (videoPlayer != null) {
      return "state:" + videoPlayer.getVideoState() + "," + "duration:" + videoPlayer.getDuration() + "," + "position:" + videoPlayer.getCurrentPosition();
    }
    return null;
  }

  @Override
  public void onADExposure(NativeExpressADView adView) {
    Log.i(TAG, "onADExposure: " + adView.toString());
  }

  @Override
  public void onADClicked(NativeExpressADView adView) {
    Log.i(TAG, "onADClicked: " + adView.toString());
  }

  @Override
  public void onADClosed(NativeExpressADView adView) {
    Log.i(TAG, "onADClosed: " + adView.toString());
  }

  @Override
  public void onADLeftApplication(NativeExpressADView adView) {
    Log.i(TAG, "onADLeftApplication: " + adView.toString());
  }

  @Override
  public void onNoAD(AdError error) {
    ToastUtil.s("没有拉到广告!");
    Log.d(TAG,
        "onNoAd error code: " + error.getErrorCode() + ", error msg: " + error.getErrorMsg());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (videoIsPaused) {
      mCurrentVideoView.seekTo(mVideoViewCurrentPosition);
      mCurrentVideoView.start();
      videoIsPaused = false;
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    Item item = mAdapter.getItem(mCurrentPage);
    if (item.type == TYPE_DATA) {
      if (mLayoutManager.findViewByPosition(mCurrentPage) == null) {
        return;
      }
      mCurrentVideoView = mLayoutManager.findViewByPosition(mCurrentPage)
          .findViewById(R.id.video_view);
      mVideoViewCurrentPosition = mCurrentVideoView.getCurrentPosition();
      mCurrentVideoView.pause();
      videoIsPaused = true;
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mAds != null) {
      for (NativeExpressADView ad : mAds) {
        ad.destroy();
      }
    }
    mAds = null;
  }

  class ItemAdapter extends RecyclerView.Adapter<ItemHolder> {

    private final List<Item> mData;
    private final Context mContext;

    public ItemAdapter(Context context, List<Item> list) {
      mContext = context;
      mData = list;
    }

    public Item getItem(int position) {
      return mData.get(position);
    }

    public void addItem(NativeExpressImmersiveActivity.Item item) {
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

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view;
      switch (viewType) {
        case TYPE_AD:
          view = LayoutInflater.from(mContext)
              .inflate(R.layout.item_immersive_express_ad, parent, false);
          break;

        case TYPE_DATA:
        default:
          view = LayoutInflater.from(mContext)
              .inflate(R.layout.item_full_screen_video_feed, parent, false);
          break;
      }
      return new ItemHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemHolder holder, int position) {
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
      final NativeExpressADView ad = item.ad;
      if (ad.getParent() != null) {
        ((FrameLayout) ad.getParent()).removeAllViews();
      }
      holder.container.addView(ad, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT));
      ad.render();
    }

    @Override
    public int getItemCount() {
      return mData.size();
    }
  }

  private void play() {
    View itemView = mRecyclerView.getChildAt(0);
    final VideoView videoView = itemView.findViewById(R.id.video_view);
    final View coverImage = itemView.findViewById(R.id.cover_image);
    if (videoView != null) {
      videoView.start();
      videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
          Log.d(TAG, "onInfo");
          mp.setLooping(true);
          coverImage.animate().alpha(0).setDuration(200).start();
          return false;
        }
      });
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
      if (coverImage != null) {
        coverImage.animate().alpha(1).start();
      }
    }
  }

  static class ItemHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public VideoView videoView;
    public ImageView coverImage;

    public FrameLayout container;

    public ItemHolder(View itemView, int adType) {
      super(itemView);
      switch (adType) {
        case TYPE_AD:
          container = (FrameLayout) itemView;
          break;

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

    public NativeExpressADView ad;

    public Item(int position) {
      this.type = TYPE_DATA;
      this.title = "第 " + (position + 1) + " 个普通视频";
      this.videoUri = Uri.parse(
          "android.resource://" + getPackageName() + "/" + mVideoIds[position % mVideoIds.length]);
      this.imageUri = Uri.parse(
          "android.resource://" + getPackageName() + "/" + mImageIds[position % mImageIds.length]);
    }

    public Item(NativeExpressADView ad) {
      this.type = TYPE_AD;
      this.ad = ad;
    }

  }

  private class H extends Handler {
    @Override
    public void handleMessage(Message msg) {
      if (msg.what == MSG_REFRESH_LIST) {
        for (int i = INIT_ITEM_COUNT; i < ITEM_COUNT; i++) {
          mAdapter.addItem(new Item(i));
        }

        List<NativeExpressADView> ads = (List<NativeExpressADView>) msg.obj;
        if (ads != null && ads.size() > 0 && mAdapter != null) {
          Random random = new Random();
          for (int i = 0; i < ads.size(); i++) {
            int index = Math.abs(random.nextInt()) % ITEM_COUNT;

            while (index == mCurrentPage) {
              index = Math.abs(random.nextInt()) % ITEM_COUNT;
            }
            if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
              ads.get(i)
                  .setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
            }
            mAdapter.addItemToPosition(new Item(ads.get(i)), index);

            Log.d(TAG, i + ": eCPMLevel = " + ads.get(i).getECPMLevel());
          }
        }
        mAdapter.notifyDataSetChanged();
      }
    }
  }

  private interface OnViewPagerListener {
    void onInitComplete();

    void onPageRelease(boolean isNext, int position);

    void onPageSelected(int position, boolean isBottom);
  }

  private static class ViewPagerLayoutManager extends LinearLayoutManager {
    private final PagerSnapHelper mPagerSnapHelper;
    private OnViewPagerListener mOnViewPagerListener;
    private int mDeltaY;

    private final RecyclerView.OnChildAttachStateChangeListener mChildAttachStateChangeListener =
        new RecyclerView.OnChildAttachStateChangeListener() {
          public void onChildViewAttachedToWindow(@NonNull View view) {
            if (mOnViewPagerListener != null && getChildCount() == 1) {
              mOnViewPagerListener.onInitComplete();
            }
          }

          public void onChildViewDetachedFromWindow(@NonNull View view) {
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
      view.addOnChildAttachStateChangeListener(mChildAttachStateChangeListener);
    }

    public void onScrollStateChanged(int state) {
      if (state == RecyclerView.SCROLL_STATE_IDLE) {
        View curView = mPagerSnapHelper.findSnapView(this);
        int curPos = getPosition(curView);
        if (mOnViewPagerListener != null && getChildCount() == 1) {
          mOnViewPagerListener.onPageSelected(curPos, curPos == getItemCount() - 1);
        }
      }
    }

    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                                  RecyclerView.State state) {
      mDeltaY = dy;
      return super.scrollVerticallyBy(dy, recycler, state);
    }

    public void setOnViewPagerListener(OnViewPagerListener listener) {
      mOnViewPagerListener = listener;
    }
  }

}
