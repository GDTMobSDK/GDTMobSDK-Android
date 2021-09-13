package com.qq.e.union.adapter.test.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.qq.e.union.demo.R;
import com.qq.e.union.adapter.test.model.LayerConfig;
import com.qq.e.union.adapter.test.model.NetworkConfig;
import com.qq.e.union.adapter.test.util.ConfigReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class MediationTestActivity extends AppCompatActivity {

  private RecyclerView mConfigsView;
  private ImageView mLoadingIcon;

  private Handler mHandler = new H(Looper.myLooper());

  private List<LayerConfig> mConfigs = new ArrayList<>();

  private static final int MSG_REFRESH_UI = 1;
  private static final int TYPE_LAYER = 1;
  private static final int TYPE_NETWORK = 2;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({TYPE_LAYER, TYPE_NETWORK})
  @interface ViewType {
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_mediation_test);
    mConfigsView = findViewById(R.id.configs_view);
    mLoadingIcon = findViewById(R.id.loading_icon);
    initDataAsync();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mHandler.removeCallbacksAndMessages(null);
  }

  public void initDataAsync() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        getLocalConfigs();
        mHandler.sendEmptyMessage(MSG_REFRESH_UI);
      }
    }).start();
  }

  private void getLocalConfigs() {
    String originalData = ConfigReader.getConfig(this);
    if (TextUtils.isEmpty(originalData)) {
      return;
    }
    try {
      JSONObject originalJson = new JSONObject(originalData);
      JSONArray layerConfigs = originalJson.optJSONArray("layerConfigs");
      for (int i = 0; i < layerConfigs.length(); i++) {
        JSONObject layer = layerConfigs.getJSONObject(i);
        mConfigs.add(new LayerConfig(layer));
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private class H extends Handler {

    H(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      if (msg.what == MSG_REFRESH_UI) {
        mLoadingIcon.setVisibility(View.GONE);
        mConfigsView.setLayoutManager(new LinearLayoutManager(MediationTestActivity.this));
        mConfigsView.setAdapter(new ConfigsAdapter(mConfigs));
        mConfigsView.addItemDecoration(new DividerItemDecoration(MediationTestActivity.this, DividerItemDecoration.VERTICAL));
      }
    }
  }

  private class ConfigsAdapter extends RecyclerView.Adapter<VH> {

    private List<LayerConfig> mLayerConfigs;
    private List<Object> mSortConfigs = new ArrayList<>();

    public ConfigsAdapter(List<LayerConfig> configs) {
      mLayerConfigs = configs;
      for (LayerConfig config : mLayerConfigs) {
        mSortConfigs.add(config);
        List<NetworkConfig> networkConfigs = config.getNetworkConfigs();
        if (networkConfigs != null && networkConfigs.size() > 0) {
          for (NetworkConfig networkConfig : networkConfigs) {
            mSortConfigs.add(networkConfig);
          }
        }
      }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
      View view = null;
      if (i == TYPE_NETWORK) {
        view = LayoutInflater.from(MediationTestActivity.this).inflate(R.layout.item_network, null);
      } else if (i == TYPE_LAYER) {
        view = LayoutInflater.from(MediationTestActivity.this).inflate(R.layout.item_layer, null);
      }
      return new VH(view, i);
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int i) {
      @ViewType int viewType = getItemViewType(i);
      if (viewType == TYPE_LAYER) {
        vh.mLayer.setText(((LayerConfig) (mSortConfigs.get(i))).getPosId());
      } else if (viewType == TYPE_NETWORK) {
        NetworkConfig networkConfig = (NetworkConfig) mSortConfigs.get(i);
        vh.mName.setText("渠道 : " + networkConfig.getName());
        vh.mPosId.setText("PosId : " + networkConfig.getIdentity());
        vh.mClassName.setText("渠道适配器 : " + networkConfig.getClassName());
        vh.mExt.setText("其他信息 : " + networkConfig.getExt());
        vh.mAdapterStatus.setText("适配器状态 : " + networkConfig.getAdapterStatus());
      }
    }

    @Override
    public int getItemCount() {
      return mSortConfigs.size();
    }

    @Override
    public @ViewType int getItemViewType(int i) {
      Object config = mSortConfigs.get(i);
      @ViewType int type = TYPE_LAYER;
      if (config instanceof NetworkConfig) {
        type = TYPE_NETWORK;
      }
      return type;
    }
  }

  private class VH extends RecyclerView.ViewHolder {

    public TextView mLayer;
    public TextView mName;
    public TextView mPosId;
    public TextView mExt;
    public TextView mClassName;
    public TextView mAdapterStatus;

    public VH(@NonNull View itemView, @ViewType int viewType) {
      super(itemView);
      switch (viewType) {
        case TYPE_LAYER:
          mLayer = itemView.findViewById(R.id.layer_title);
          break;
        case TYPE_NETWORK:
          mName = itemView.findViewById(R.id.network_name);
          mPosId = itemView.findViewById(R.id.network_posid);
          mExt = itemView.findViewById(R.id.network_ext);
          mClassName = itemView.findViewById(R.id.network_class_name);
          mAdapterStatus = itemView.findViewById(R.id.network_adapter_status);
          break;
      }
    }
  }
}
