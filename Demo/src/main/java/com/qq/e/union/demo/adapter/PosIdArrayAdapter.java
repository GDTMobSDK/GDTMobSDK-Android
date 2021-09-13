package com.qq.e.union.demo.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PosIdArrayAdapter extends ArrayAdapter {

    private int selectedPos;

    public void setSelectedPos(int selectedPos) {
        this.selectedPos = selectedPos;
    }

    public PosIdArrayAdapter(@NonNull Context context, int resource, @NonNull Object[] objects) {
        super(context, resource, objects);
    }

    public PosIdArrayAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView textView = (TextView) view;
        if (this.selectedPos == position) {
            textView.setTextColor(0xff373741);
            textView.getPaint().setFakeBoldText(true);
        } else {
            textView.setTextColor(0xff6d6d6d);
            textView.getPaint().setFakeBoldText(false);
        }
        return view;
    }
}
