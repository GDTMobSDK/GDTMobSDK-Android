package com.qq.e.union.demo.util;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.qq.e.union.demo.R;

import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class CustomParamsDialog extends Dialog {

  private Map<String, String> mDataSrc;
  private CustomAdapter mCustomAdapter;

  public CustomParamsDialog(@NonNull Context context, Map<String, String> map) {
    super(context);
    mDataSrc = map;
    initView();
  }


  private void initView() {
    setContentView(R.layout.dialog_custom_params);
    RecyclerView recyclerView = findViewById(R.id.recycler_view_params);
    mCustomAdapter = new CustomAdapter(mDataSrc);
    recyclerView.setAdapter(mCustomAdapter);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
    linearLayoutManager.setOrientation(OrientationHelper.VERTICAL);
    recyclerView.setLayoutManager(linearLayoutManager);
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    findViewById(R.id.btn_add_param).setOnClickListener(this::addParams);
    findViewById(R.id.btn_finish).setOnClickListener(this::onFinish);
  }

  public void onFinish(View view) {
    mDataSrc.clear();
    mDataSrc.putAll(mCustomAdapter.getData());
    dismiss();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void addParams(View view) {
    TextView etValue = (EditText) findViewById(R.id.et_value);
    CharSequence valueText = etValue.getText();
    String value = valueText == null ? "" : valueText.toString();
    TextView etKey = (EditText) findViewById(R.id.et_key);
    CharSequence keyText = etKey.getText();
    String key = keyText == null ? "" : keyText.toString();
    etKey.setText("");
    etValue.setText("");
    boolean hasExited = mCustomAdapter.getData().containsKey(key);
    mCustomAdapter.getData().put(key, value);
    int index = mCustomAdapter.getData().indexOfKey(key);
    if (!hasExited) {
      mCustomAdapter.notifyItemInserted(index);
    } else {
      mCustomAdapter.notifyItemChanged(index);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  private static class CustomAdapter extends RecyclerView.Adapter<VH> {
    private final ArrayMap<String, String> mData;

    public CustomAdapter(Map<String, String> map) {
      mData = new ArrayMap<>();
      mData.putAll(map);
    }

    public ArrayMap<String, String> getData() {
      return mData;
    }

    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
      View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_custom_params
          , viewGroup, false);
      return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int i) {
      String key = mData.keyAt(i);
      vh.tvValue.setText(mData.get(key));
      vh.tvKey.setText(key);
      vh.btnDelete.setOnClickListener((view) -> {
        mData.removeAt(i);
        CustomAdapter.this.notifyItemRemoved(i);
      });
    }

    @Override
    public int getItemCount() {
      return mData.size();
    }
  }

  private static class VH extends RecyclerView.ViewHolder {
    TextView tvKey;
    TextView tvValue;
    Button btnDelete;

    VH(@NonNull View itemView) {
      super(itemView);
      tvKey = itemView.findViewById(R.id.item_key);
      tvValue = itemView.findViewById(R.id.item_value);
      btnDelete = itemView.findViewById(R.id.btn_delete_param);
    }
  }

}
