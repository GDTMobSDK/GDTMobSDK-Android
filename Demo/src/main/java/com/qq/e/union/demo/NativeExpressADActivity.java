package com.qq.e.union.demo;

import android.app.Activity;
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
import android.widget.Toast;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.union.demo.adapter.PosIdArrayAdapter;
import com.qq.e.union.demo.view.S2SBiddingDemoUtils;

import static com.qq.e.union.demo.Constants.VIDEO_DURATION_SETTING_MAX;
import static com.qq.e.union.demo.Constants.VIDEO_DURATION_SETTING_MIN;

/**
 * Created by hechao on 2018/2/8.
 */

public class NativeExpressADActivity extends Activity implements CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener {

  private static final String TAG = "NativeExpressADActivity";

  private CheckBox btnNoOption;
  private CheckBox btnMute;
  private CheckBox btnDetailMute;
  private Spinner networkSpinner;
  private EditText posIdEdt;

  private Spinner spinner;
  private PosIdArrayAdapter arrayAdapter;
  private String mS2SBiddingToken;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_express_ad);
    /**
     * 如果选择支持视频的模板样式，请使用{@link PositionId#NATIVE_EXPRESS_SUPPORT_VIDEO_POS_ID}
     */
    posIdEdt = findViewById(R.id.posId);

    spinner = findViewById(R.id.id_spinner);
    arrayAdapter = new PosIdArrayAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.native_express_video_ad));
    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(arrayAdapter);
    spinner.setOnItemSelectedListener(this);

    btnNoOption = findViewById(R.id.cb_none_video_option);
    btnNoOption.setOnCheckedChangeListener(this);
    btnMute = findViewById(R.id.btn_mute);
    btnDetailMute = findViewById(R.id.btn_detail_mute);
    networkSpinner = findViewById(R.id.spinner_network);
  }

  /**
   * 如果选择支持视频的模板样式，请使用{@link PositionId#NATIVE_EXPRESS_SUPPORT_VIDEO_POS_ID}
   */
  private String getPosID() {
    String posId = ((EditText) findViewById(R.id.posId)).getText().toString();
    return TextUtils.isEmpty(posId) ? PositionId.NATIVE_EXPRESS_POS_ID : posId;
  }

  public void requestS2SBiddingToken(View view) {
    S2SBiddingDemoUtils.requestBiddingToken(this, getPosID(), token -> mS2SBiddingToken = token);
  }

  private int getMinVideoDuration() {
    if (((CheckBox) findViewById(R.id.cbMinVideoDuration)).isChecked()) {
      try {
        int rst =
            Integer.parseInt(((EditText) findViewById(R.id.etMinVideoDuration)).getText().toString());
        if (rst > 0) {
          return rst;
        } else {
          Toast.makeText(getApplicationContext(), "最小视频时长输入须大于0!", Toast.LENGTH_LONG).show();
        }
      } catch (NumberFormatException e) {
        Toast.makeText(getApplicationContext(), "最小视频时长输入不是整数!", Toast.LENGTH_LONG).show();
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
          Toast.makeText(getApplicationContext(), "最大视频时长输入不在有效区间内!", Toast.LENGTH_LONG).show();
        }
      } catch (NumberFormatException e) {
        Toast.makeText(getApplicationContext(), "最大视频时长输入不是整数!", Toast.LENGTH_LONG).show();
      }
    }
    return 0;
  }

  public void onNormalViewClicked(View view) {
    Intent intent = new Intent();
    intent.setClass(this, NativeExpressDemoActivity.class);
    putExtraToIntent(intent);
    startActivity(intent);
  }

  public void onRecyclerViewClicked(View view) {
    Intent intent = new Intent();
    intent.setClass(this, NativeExpressRecyclerViewActivity.class);
    putExtraToIntent(intent);
    startActivity(intent);
  }

  private void putExtraToIntent(Intent intent){
    intent.putExtra(Constants.POS_ID, getPosID());
    Log.d(TAG, "BiddingToken: " + mS2SBiddingToken);
    intent.putExtra(Constants.TOKEN, mS2SBiddingToken);
    intent.putExtra(Constants.MIN_VIDEO_DURATION, getMinVideoDuration());
    intent.putExtra(Constants.MAX_VIDEO_DURATION, getMaxVideoDuration());
    if(btnNoOption.isChecked()){
      intent.putExtra(Constants.NONE_OPTION, true);
    }else{
      intent.putExtra(Constants.PLAY_MUTE, btnMute.isChecked());
      intent.putExtra(Constants.PLAY_NETWORK, networkSpinner.getSelectedItemPosition());
      intent.putExtra(Constants.DETAIL_PAGE_MUTED, btnDetailMute.isChecked());
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (buttonView == btnNoOption) {
      networkSpinner.setEnabled(!isChecked);
      btnMute.setEnabled(!isChecked);
      btnDetailMute.setEnabled(!isChecked);
    }
  }

  public static VideoOption getVideoOption(Intent intent) {
    if(intent == null){
      return null;
    }

    VideoOption videoOption = null;
    boolean noneOption = intent.getBooleanExtra(Constants.NONE_OPTION, false);
    if (!noneOption) {
      VideoOption.Builder builder = new VideoOption.Builder();

      builder.setAutoPlayPolicy(intent.getIntExtra(Constants.PLAY_NETWORK, VideoOption.AutoPlayPolicy.ALWAYS));
      builder.setAutoPlayMuted(intent.getBooleanExtra(Constants.PLAY_MUTE, true));
      builder.setDetailPageMuted(intent.getBooleanExtra(Constants.DETAIL_PAGE_MUTED,false));

      videoOption = builder.build();
    }
    return videoOption;
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    arrayAdapter.setSelectedPos(position);
    posIdEdt.setText(getResources().getStringArray(R.array.native_express_video_ad_value)[position]);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }
}
