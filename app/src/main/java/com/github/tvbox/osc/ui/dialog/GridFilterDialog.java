package com.github.tvbox.osc.ui.dialog;

import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.ui.adapter.GridFilterKVAdapter;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class GridFilterDialog extends BaseDialog {
    public LinearLayout filterRoot;

    public GridFilterDialog(@NonNull @NotNull Context context) {
        super(context);
        setCanceledOnTouchOutside(false);
        setCancelable(true);
        setContentView(R.layout.dialog_grid_filter);
        filterRoot = findViewById(R.id.filterRoot);
        if(!isTvOrBox(context)){
            View rootView = findViewById(R.id.root);
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }

    public interface Callback {
        void change();
    }

    public void setOnDismiss(Callback callback) {
        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (selectChange) {
                    callback.change();
                }
            }
        });
    }

    public void setData(MovieSort.SortData sortData) {
        ArrayList<MovieSort.SortFilter> filters = sortData.filters;
        for (MovieSort.SortFilter filter : filters) {
            View line = LayoutInflater.from(getContext()).inflate(R.layout.item_grid_filter, null);
            ((TextView) line.findViewById(R.id.filterName)).setText(filter.name);
            TvRecyclerView gridView = line.findViewById(R.id.mFilterKv);
            gridView.setHasFixedSize(true);
            gridView.setLayoutManager(new V7LinearLayoutManager(getContext(), 0, false));
            GridFilterKVAdapter filterKVAdapter = new GridFilterKVAdapter();
            gridView.setAdapter(filterKVAdapter);
            String key = filter.key;
            ArrayList<String> values = new ArrayList<>(filter.values.keySet());
            ArrayList<String> keys = new ArrayList<>(filter.values.values());
            filterKVAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                View pre = null;

                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                    selectChange = true;
                    String filterSelect = sortData.filterSelect.get(key);
                    if (filterSelect == null || !filterSelect.equals(keys.get(position))) {
                        sortData.filterSelect.put(key, keys.get(position));
                        if (pre != null) {
                            TextView val = pre.findViewById(R.id.filterValue);
                            val.getPaint().setFakeBoldText(false);
                            val.setTextColor(getContext().getResources().getColor(R.color.color_FFFFFF));
                        }
                        TextView val = view.findViewById(R.id.filterValue);
                        val.getPaint().setFakeBoldText(true);
                        // takagen99: Added Theme Color
//                        val.setTextColor(getContext().getResources().getColor(R.color.color_theme));
                        TypedArray a = getContext().obtainStyledAttributes(R.styleable.themeColor);
                        int themeColor = a.getColor(R.styleable.themeColor_color_theme, 0);
                        val.setTextColor(themeColor);
                        pre = view;
                    } else {
                        sortData.filterSelect.remove(key);
                        TextView val = pre.findViewById(R.id.filterValue);
                        val.getPaint().setFakeBoldText(false);
                        val.setTextColor(getContext().getResources().getColor(R.color.color_FFFFFF));
                        pre = null;
                    }
                }
            });
            filterKVAdapter.setNewData(values);
            filterRoot.addView(line);
        }
    }

    private boolean selectChange = false;

    public void show() {
        selectChange = false;
        super.show();
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        // layoutParams.dimAmount = 0f;
        getWindow().getDecorView().setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);
    }
    public static boolean isTvOrBox(Context context) {
        // 1. UiMode 判断
        UiModeManager uiMode = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (uiMode.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }
        // 2. Android TV / Leanback 特性判断
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || pm.hasSystemFeature("android.software.leanback_only")   // Strict leanback
                || pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                // Amazon Fire TV
                || pm.hasSystemFeature("amazon.hardware.fire_tv")
                // Google TV (part of Android TV 家族)
                || pm.hasSystemFeature("com.google.android.tv")) {
            return true;
        }
        // 3. 没有触摸屏：大多数机顶盒、电视不带触摸
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
            return true;
        }
        // 4. 物理遥控器 / D‑pad 键存在判断 兼容一些既有触摸也支持遥控的设备
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_UP)
                && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_DOWN)
                && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_LEFT)
                && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_RIGHT)
                && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_CENTER)) {
            return true;
        }
        // 5. 输入设备源中有 DPAD
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int id : deviceIds) {
            InputDevice dev = InputDevice.getDevice(id);
            if (dev == null) continue;
            int sources = dev.getSources();
            if ((sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) {
                return true;
            }
        }
        return false;
    }   
}
