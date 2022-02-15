package com.qq.e.union.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.MediaView;
import com.qq.e.ads.nativ.NativeADEventListenerWithClickInfo;
import com.qq.e.ads.nativ.NativeADMediaListener;
import com.qq.e.ads.nativ.NativeADUnifiedListener;
import com.qq.e.ads.nativ.NativeUnifiedAD;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.ads.nativ.widget.NativeAdContainer;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.listeners.NegativeFeedbackListener;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.DownloadConfirmHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.WeakHashMap;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class NativeADUnifiedRecyclerViewActivity extends Activity
    implements NativeADUnifiedListener {

  private static final String TAG = NativeADUnifiedRecyclerViewActivity.class.getSimpleName();
  private AQuery mAQuery;

  private NativeUnifiedAD mAdManager;
  private List<NativeUnifiedADData> mAds = new ArrayList<>();

  private CustomAdapter mAdapter;

  private H mHandler = new H();

  private static final int AD_COUNT = 3;
  private static final int ITEM_COUNT = 30;
  private static final int FIRST_AD_POSITION = 5;
  private static final int AD_DISTANCE = 10;
  private static final int MSG_REFRESH_LIST = 1;

  private static final int TYPE_DATA = 0;
  private static final int TYPE_AD = 1;
  private static final int TYPE_SHOW_SDK_VERSION = 2;

  private boolean mPlayMute = true;
  private boolean mIsLoading = true;

  private WeakHashMap<NativeUnifiedADData, Boolean> mMuteMap = new WeakHashMap<>();
  private boolean mBindToCustomView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_unified_ad_recyclerview);
    initView();

    boolean nonOption = getIntent().getBooleanExtra(Constants.NONE_OPTION, false);
    if(!nonOption){
      mPlayMute = getIntent().getBooleanExtra(Constants.PLAY_MUTE,true);
    }

    mBindToCustomView = getIntent().getBooleanExtra(Constants.BUTTON_BIND_TO_CUSTOM_VIEW, false);

    String token = getToken();
    Log.d(TAG, "onCreate: BiddingToken = " + token);
    if (!TextUtils.isEmpty(token)) {
      mAdManager = new NativeUnifiedAD(this, getPosId(), this, token);
    } else {
      mAdManager = new NativeUnifiedAD(this, getPosId(), this);
    }
    mAdManager.setMinVideoDuration(getMinVideoDuration());
    mAdManager.setMaxVideoDuration(getMaxVideoDuration());
    // 下面设置项为海外流量使用，国内暂不支持
    mAdManager.setVastClassName("com.qq.e.union.demo.adapter.vast.unified.ImaNativeDataAdapter");

    mAdManager.loadData(AD_COUNT);
  }

  private String getPosId() {
    return getIntent().getStringExtra(Constants.POS_ID);
  }

  private String getToken() {
    return getIntent().getStringExtra(Constants.TOKEN);
  }

  private int getMinVideoDuration() {
    return getIntent().getIntExtra(Constants.MIN_VIDEO_DURATION, 0);
  }

  private int getMaxVideoDuration() {
    return getIntent().getIntExtra(Constants.MAX_VIDEO_DURATION, 0);
  }

  private void initView() {
    RecyclerView recyclerView = findViewById(R.id.recycler_view);
    LinearLayoutManager manager = new LinearLayoutManager(this);
    manager.setOrientation(LinearLayoutManager.VERTICAL);
    recyclerView.setLayoutManager(manager);
    List<NormalItem> list = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      list.add(new NormalItem("No." + i + " Init Data"));
    }
    mAdapter = new CustomAdapter(this, list);
    recyclerView.setAdapter(mAdapter);
    mAQuery = new AQuery(this);
    recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
//        super.onScrollStateChanged(recyclerView, newState);

        if(!mIsLoading && newState == SCROLL_STATE_IDLE && !recyclerView.canScrollVertically(1)){
          mIsLoading = true;
          mAdManager.loadData(AD_COUNT);
        }

      }

      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
      }
    });
  }

  @Override
  public void onADLoaded(List<NativeUnifiedADData> ads) {
    mIsLoading = false;
    // 防止在onDestory后网络回包
    if(mAds != null){
      mAds.addAll(ads);
      Message msg = mHandler.obtainMessage(MSG_REFRESH_LIST, ads);
      mHandler.sendMessage(msg);
    }
  }

  @Override
  public void onNoAD(AdError error) {
    mIsLoading = false;
    String msg = "onNoAd error code: " + error.getErrorCode() + ", error msg: " + error.getErrorMsg();
    Log.d(TAG, msg);
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mAds != null) {
      for (NativeUnifiedADData ad : mAds) {
        ad.resume();
      }
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

  class CustomAdapter extends RecyclerView.Adapter<CustomHolder> {

    private List<Object> mData;
    private Context mContext;
    private TreeSet mADSet = new TreeSet();

    public CustomAdapter(Context context, List list) {
      mContext = context;
      mData = list;
    }

    public void addNormalItem(NormalItem item){
      mData.add(item);
    }

    public void addAdToPosition(NativeUnifiedADData nativeUnifiedADData, int position) {
      if (position >= 0 && position < mData.size()) {
        mData.add(position, nativeUnifiedADData);
        mADSet.add(position);
      }
    }

    @Override
    public int getItemViewType(int position) {
      if (mADSet.contains(position)) {
        return TYPE_AD;
      } else if (mADSet.contains(position + 1)) {
        return TYPE_SHOW_SDK_VERSION;
      } else {
        return TYPE_DATA;
      }
    }

    @Override
    public CustomHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view;
      switch (viewType) {
        case TYPE_AD:
          view = LayoutInflater.from(mContext).inflate(R.layout.item_ad_unified, null);
          break;

        case TYPE_DATA:
        case TYPE_SHOW_SDK_VERSION:
          view = LayoutInflater.from(mContext).inflate(R.layout.item_data, null);
          break;

        default:
            view = null;
      }
      return new CustomHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(CustomHolder holder, int position) {
      switch (getItemViewType(position)) {
        case TYPE_AD:
          initItemView(position, holder);
          break;
        case TYPE_DATA:
          holder.title.setText(((NormalItem) mData.get(position))
              .getTitle());
          break;
        case TYPE_SHOW_SDK_VERSION:
          holder.title.setText("show SDK version");
          holder.title.setOnClickListener(v -> startActivity(
              new Intent(NativeADUnifiedRecyclerViewActivity.this, SDKVersionActivity.class)));
          break;
      }
    }

    private void initItemView(int position, final CustomHolder holder) {
      final NativeUnifiedADData ad = (NativeUnifiedADData) mData.get(position);
      AQuery logoAQ = holder.logoAQ;
      String iconUrl = ad.getIconUrl();
      if (TextUtils.isEmpty(iconUrl)) {
        holder.logo.setVisibility(View.GONE);
      } else {
        holder.logo.setVisibility(View.VISIBLE);
        logoAQ.id(R.id.img_logo).image(iconUrl, false, true);
      }

      holder.name.setText(ad.getTitle());
      holder.hash = ad.hashCode();
      holder.desc.setText(ad.getDesc());
      //先隐藏相关view，根据不同广告类型设置可见
      holder.poster.setVisibility(View.GONE);
      holder.mediaView.setVisibility(View.GONE);
      holder.btnsContainer.setVisibility(View.GONE);
      holder.threeImageContainer.setVisibility(View.GONE);

      List<View> clickableViews = new ArrayList<>();
      List<View> customClickableViews = new ArrayList<>();
      if (mBindToCustomView) {
        customClickableViews.add(holder.download);
      } else {
        clickableViews.add(holder.download);
      }
      List<ImageView> imageViews = new ArrayList<>();
      if (ad.getAdPatternType() == AdPatternType.NATIVE_2IMAGE_2TEXT ||
          ad.getAdPatternType() == AdPatternType.NATIVE_1IMAGE_2TEXT) {
        // 双图双文、单图双文：注册mImagePoster的点击事件
        holder.poster.setVisibility(View.VISIBLE);
        clickableViews.add(holder.poster);
        imageViews.add(holder.poster);
      } else if (ad.getAdPatternType() == AdPatternType.NATIVE_3IMAGE) {
        holder.threeImageContainer.setVisibility(View.VISIBLE);
        clickableViews.add(holder.threeImageContainer);
        imageViews.add(holder.container.findViewById(R.id.img_1));
        imageViews.add(holder.container.findViewById(R.id.img_2));
        imageViews.add(holder.container.findViewById(R.id.img_3));
      }
      //作为customClickableViews传入，点击不进入详情页，直接下载或进入落地页，图文、视频广告均生效，
      ad.bindAdToView(NativeADUnifiedRecyclerViewActivity.this, holder.container, null,
          clickableViews, customClickableViews);
      if (!imageViews.isEmpty()) {
        ad.bindImageViews(imageViews, 0);
      }

      setAdListener(holder, ad);

      NativeADUnifiedSampleActivity.updateAdAction(holder.download, ad);

      /**
       * 营销组件
       * 支持项目：智能电话（点击跳转拨号盘），外显表单
       *  bindCTAViews 绑定营销组件监听视图，注意：bindCTAViews的视图不可调用setOnClickListener，否则SDK功能可能受到影响
       *  ad.getCTAText 判断拉取广告是否包含营销组件，如果包含组件，展示组件按钮，否则展示download按钮
       */
      List<View> CTAViews = new ArrayList<>();
      CTAViews.add(holder.ctaButton);
      ad.bindCTAViews(CTAViews);
      String ctaText = ad.getCTAText(); //获取组件文案
      if (!TextUtils.isEmpty(ctaText)) {
        //如果拉取广告包含CTA组件，则渲染该组件
        //当广告中有营销组件时，隐藏下载按钮，仅为demo示例所用，开发者可自行决定mDownloadButton按钮是否显示
        holder.ctaButton.setText(ctaText);
        holder.ctaButton.setVisibility(View.VISIBLE);
        holder.download.setVisibility(View.INVISIBLE);
      } else {
        holder.ctaButton.setVisibility(View.INVISIBLE);
        holder.download.setVisibility(View.VISIBLE);
      }

    }

    private void setAdListener(final CustomHolder holder, final NativeUnifiedADData ad) {
      ad.setNegativeFeedbackListener(new NegativeFeedbackListener(){

        @Override
        public void onComplainSuccess() {
          Log.d(TAG, "onComplainSuccess: ");
        }
      });
      //这里示例NativeADEventListenerWithClickInfo用法，如不需要view信息可继续使用NativeADEventListener
      ad.setNativeAdEventListener(new NativeADEventListenerWithClickInfo() {
        @Override
        public void onADExposed() {
          Log.d(TAG, "onADExposed: " + ad.getTitle());
        }

        @Override
        public void onADClicked(View v) {
          Log.d(TAG, "onADClicked: " + ad.getTitle());
          Log.d(TAG, "onADClicked clicked view: " + v);
        }

        @Override
        public void onADError(AdError error) {
          Log.d(TAG, "onADError error code :" + error.getErrorCode()
              + "  error msg: " + error.getErrorMsg());
        }

        @Override
        public void onADStatusChanged() {
          if (holder.hash == ad.hashCode()) {
            NativeADUnifiedSampleActivity.updateAdAction(holder.download, ad);
          }
        }
      });
      // 视频广告
      if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
        VideoOption videoOption =
            NativeADUnifiedSampleActivity.getVideoOption(getIntent());
        holder.mediaView.setVisibility(View.VISIBLE);
        holder.btnsContainer.setVisibility(View.VISIBLE);
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
                Log.d(TAG, "onVideoReady");
              }

              @Override
              public void onVideoLoaded(int videoDuration) {
                Log.d(TAG, "onVideoLoaded: ");
              }

              @Override
              public void onVideoStart() {
                Log.d(TAG, "onVideoStart");
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

        View.OnClickListener listener = new View.OnClickListener(){
          @Override
          public void onClick(View v) {
            if(v == holder.btnPlay){
              ad.startVideo();
            }else if(v == holder.btnPause){
              ad.pauseVideo();
            }else if(v == holder.btnStop){
              ad.stopVideo();
            }
          }
        };
        holder.btnPlay.setOnClickListener(listener);
        holder.btnPause.setOnClickListener(listener);
        holder.btnStop.setOnClickListener(listener);

        // 静音复选框状态恢复
        boolean mute = mMuteMap.containsKey(ad) ? mMuteMap.get(ad) : mPlayMute;
        holder.btnMute.setChecked(mute);
        mMuteMap.put(ad, mute);

        holder.btnMute.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            boolean check = holder.btnMute.isChecked();
            ad.setVideoMute(check);
            mMuteMap.put(ad, check);
          }
        });
      }
    }

    @Override
    public int getItemCount() {
      return mData.size();
    }
  }

  class CustomHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public int hash;
    public MediaView mediaView;
    public TextView name;
    public TextView desc;
    public ImageView logo;
    public ImageView poster;
    public Button download;
    public Button ctaButton;
    public NativeAdContainer container;
    public AQuery logoAQ;
    private View btnsContainer;
    private Button btnPlay;
    private Button btnPause;
    private Button btnStop;
    public CheckBox btnMute;
    public View threeImageContainer;

    public CustomHolder(View itemView, int adType) {
      super(itemView);
      switch (adType) {
        case TYPE_AD:
          mediaView = itemView.findViewById(R.id.gdt_media_view);
          logo = itemView.findViewById(R.id.img_logo);
          poster = itemView.findViewById(R.id.img_poster);
          name = itemView.findViewById(R.id.text_title);
          desc = itemView.findViewById(R.id.text_desc);
          download = itemView.findViewById(R.id.btn_download);
          ctaButton = itemView.findViewById(R.id.btn_cta);
          container = itemView.findViewById(R.id.native_ad_container);
          btnsContainer = itemView.findViewById(R.id.video_btns_container);
          btnPlay = itemView.findViewById(R.id.btn_play);
          btnPause = itemView.findViewById(R.id.btn_pause);
          btnStop = itemView.findViewById(R.id.btn_stop);
          btnMute = itemView.findViewById(R.id.btn_mute);
          threeImageContainer = itemView.findViewById(R.id.native_3img_ad_container);
          logoAQ = new AQuery(itemView);

        case TYPE_DATA:
        case TYPE_SHOW_SDK_VERSION:
          title = itemView.findViewById(R.id.title);
          break;

      }
    }
  }

  class NormalItem {
    private String mTitle;

    public NormalItem(int index){
      this("No." + index + " Normal Data");
    }

    public NormalItem(String title) {
      this.mTitle = title;
    }

    public String getTitle() {
      return mTitle;
    }

  }

  private class H extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_REFRESH_LIST:

          int count = mAdapter.getItemCount();
          for(int i = 0; i < ITEM_COUNT; i++){
            mAdapter.addNormalItem(new NormalItem(count + i));
          }

          List<NativeUnifiedADData> ads = (List<NativeUnifiedADData>) msg.obj;
          if (ads != null && ads.size() > 0 && mAdapter != null) {
            for (int i = 0; i < ads.size(); i++) {
              if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
                ads.get(i).setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
              }
              mAdapter.addAdToPosition(ads.get(i), count + i * AD_DISTANCE + FIRST_AD_POSITION);
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
}