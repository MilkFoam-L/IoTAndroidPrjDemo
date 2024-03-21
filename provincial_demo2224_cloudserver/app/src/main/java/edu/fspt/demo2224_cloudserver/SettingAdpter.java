package edu.fspt.demo2224_cloudserver;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class SettingAdpter extends ArrayAdapter<Setting> {
    public SettingAdpter(@NonNull Context context, int resource, @NonNull List<Setting> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup parent){
        Setting setting = getItem(pos);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.setting,parent,false);
        TextView name = view.findViewById(R.id.textView_name);
        name.setText(setting.getName());
        EditText editText = view.findViewById(R.id.editText_setting);
        editText.setText(setting.val);
//        SharedPreferences sp = getContext().getSharedPreferences("setting", Context.MODE_PRIVATE);
//        editText.setText(sp.getString(setting.sptag, "192.168.14.200"));
        return view;
    }
}
