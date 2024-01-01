package com.qq.e.union.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.qq.e.union.demo.adapter.PosIdArrayAdapter;
import com.qq.e.union.demo.util.ToastUtil;

import static com.qq.e.union.demo.Constants.VIDEO_DURATION_SETTING_MAX;
import static com.qq.e.union.demo.Constants.VIDEO_DURATION_SETTING_MIN;


public class NativeADUnifiedActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {

  private Spinner mPlayNetworkSpinner;
  private CheckBox mVideoOptionCheckBox;

  private CheckBox mMuteCheckBox;
  private CheckBox mCoverCheckBox;
  private CheckBox mProgressCheckBox;
  private CheckBox mDetailCheckBox;
  private CheckBox mControlCheckBox;
  private CheckBox mDetailPageMutedCheckBox;
  private CheckBox mBindToCustomViewCheckBox;

  private static final String TAG = NativeADUnifiedActivity.class.getSimpleName();

  private boolean mNoneOption = false;

  private EditText posIdEdt;

  private Spinner spinner;
  private PosIdArrayAdapter arrayAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_unified_ad);

    posIdEdt = findViewById(R.id.posId);

    spinner = findViewById(R.id.id_spinner);
    arrayAdapter = new PosIdArrayAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.native_ad_unified_ad));
    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(arrayAdapter);
    spinner.setOnItemSelectedListener(this);

    mVideoOptionCheckBox = findViewById(R.id.cb_none_video_option);
    mVideoOptionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "onCheckedChanged: isChecked:" + isChecked);
        mNoneOption = isChecked;
        boolean enable = !isChecked;
        mPlayNetworkSpinner.setEnabled(enable);
        mMuteCheckBox.setEnabled(enable);
        mCoverCheckBox.setEnabled(enable);
        mProgressCheckBox.setEnabled(enable);
        mDetailCheckBox.setEnabled(enable);
        mControlCheckBox.setEnabled(enable);
      }
    });

    mPlayNetworkSpinner = findViewById(R.id.spinner_network);
    mPlayNetworkSpinner.setSelection(1); // 默认任何网络下都自动播放

    mMuteCheckBox = findViewById(R.id.btn_mute);
    mCoverCheckBox = findViewById(R.id.btn_cover);
    mProgressCheckBox = findViewById(R.id.btn_progress);
    mDetailCheckBox = findViewById(R.id.btn_detail);
    mControlCheckBox = findViewById(R.id.btn_control);
    mDetailPageMutedCheckBox = findViewById(R.id.btn_detail_mute);
    mBindToCustomViewCheckBox = findViewById(R.id.btn_bind_to_custom_view);
  }


  protected String getPosId() {
    String posId = ((EditText) findViewById(R.id.posId)).getText().toString();
    return TextUtils.isEmpty(posId) ? PositionId.NATIVE_UNIFIED_POS_ID : posId;
  }

  private int getMinVideoDuration() {
    if (((CheckBox) findViewById(R.id.cbMinVideoDuration)).isChecked()) {
      try {
        int rst =
            Integer.parseInt(((EditText) findViewById(R.id.etMinVideoDuration)).getText().toString());
        if (rst > 0) {
          return rst;
        } else {
          ToastUtil.l("最小视频时长输入须大于0!");
        }
      } catch (NumberFormatException e) {
        ToastUtil.l("最小视频时长输入不是整数!");
      }
    }
    return 0;
  }

  private int getMaxVideoDuration() {
    if (((CheckBox) findViewById(R.id.cbMaxVideoDuration)).isChecked()) {
      try {
        int rst = Integer.parseInt(((EditText) findViewById(R.id.etMaxVideoDuration)).getText()
            .toString());
        if (rst >= VIDEO_DURATION_SETTING_MIN && rst <= VIDEO_DURATION_SETTING_MAX) {
          return rst;
        } else {
          ToastUtil.l("最大视频时长输入不在有效区间内!");
        }
      } catch (NumberFormatException e) {
        ToastUtil.l("最大视频时长输入不是整数!");
      }
    }
    return 0;
  }

  public void onNormalViewClicked(View view) {
    startActivity(getIntent(NativeADUnifiedSampleActivity.class));
  }

  public void onRecyclerViewClicked(View view) {
    startActivity(getIntent(NativeADUnifiedRecyclerViewActivity.class));
  }

  public void onListViewClick(View view) {
    startActivity(getIntent(NativeADUnifiedListViewActivity.class));
  }

  public void onPreMovieClick(View view){
    startActivity(getIntent(NativeADUnifiedPreMovieActivity.class));
  }

  public void onFullScreenClick(View view) {
    startActivity(getIntent(NativeADUnifiedFullScreenActivity.class));
  }

  public void onFullScreenFeedClick(View view){
    startActivity(getIntent(NativeADUnifiedFullScreenFeedActivity.class));
  }


  private Intent getIntent(Class cls){
    Intent intent = new Intent();
    intent.setClass(this, cls);
    intent.putExtra(Constants.POS_ID, getPosId());
    Log.d(TAG, "getIntent: BiddingToken: " + mS2sBiddingToken);
    intent.putExtra(Constants.TOKEN, mS2sBiddingToken);
    intent.putExtra(Constants.MIN_VIDEO_DURATION, getMinVideoDuration());
    intent.putExtra(Constants.MAX_VIDEO_DURATION, getMaxVideoDuration());
    intent.putExtra(Constants.NONE_OPTION, mNoneOption);
    intent.putExtra(Constants.PLAY_NETWORK, mPlayNetworkSpinner.getSelectedItemPosition());
    intent.putExtra(Constants.PLAY_MUTE, mMuteCheckBox.isChecked());
    intent.putExtra(Constants.NEED_COVER, mCoverCheckBox.isChecked());
    intent.putExtra(Constants.NEED_PROGRESS, mProgressCheckBox.isChecked());
    intent.putExtra(Constants.ENABLE_DETAIL_PAGE, mDetailCheckBox.isChecked());
    intent.putExtra(Constants.ENABLE_USER_CONTROL, mControlCheckBox.isChecked());
    intent.putExtra(Constants.DETAIL_PAGE_MUTED, mDetailPageMutedCheckBox.isChecked());
    intent.putExtra(Constants.BUTTON_BIND_TO_CUSTOM_VIEW, mBindToCustomViewCheckBox.isChecked());
    return intent;
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    arrayAdapter.setSelectedPos(position);
    posIdEdt.setText(getResources().getStringArray(R.array.native_ad_unified_ad_value)[position]);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }
}
