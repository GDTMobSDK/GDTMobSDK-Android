package com.qq.e.union.demo.util;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import com.qq.e.union.demo.R;


public class IDCollectDialog extends Dialog {

  private final Callback mCallback;

  public IDCollectDialog(@NonNull Context context, @NonNull Callback callback) {
    super(context);
    mCallback = callback;
    initView();
  }

  private void initView() {
    setContentView(R.layout.dialog_id_collect);
    findViewById(R.id.btn_submit).setOnClickListener(this::submit);
  }

  public void submit(View view) {
    Editable name = ((EditText) findViewById(R.id.et_name)).getText();
    Editable number = ((EditText) findViewById(R.id.et_number)).getText();
    mCallback.onSubmitting(name == null ? "" : name.toString().trim(),
        number == null ? "" : number.toString().trim());
    dismiss();
  }

  public interface Callback {
    void onSubmitting(String name, String number);
  }

}
