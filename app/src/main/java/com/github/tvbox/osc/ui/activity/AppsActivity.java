package com.github.tvbox.osc.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AppInfo;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.AppsAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class AppsActivity extends BaseActivity {
    private TextView tvDelTip;
    private ImageView tvDelete;
    private TvRecyclerView mGridViewApps;
    private AppsAdapter appsAdapter;
    private boolean delMode = false;
    private String packageName = "";
    private boolean isUnInstallClicked;
    private int appPosition;

    private final AsyncTask<Void, Void, AppInfo[]> mApplicationLoader = new AsyncTask<Void, Void, AppInfo[]>() {
        List<AppInfo> items = new ArrayList<>();

        @Override
        protected AppInfo[] doInBackground(Void... params) {
            items = getInstallApps(getApplicationContext());
            return items.toArray(new AppInfo[0]);
        }

        @Override
        protected void onPostExecute(AppInfo[] apps) {
            AppInfo.Sorter.sort(items);
            appsAdapter.setNewData(items);
            appsAdapter.notifyDataSetChanged();
        }
    };


    @Override
    protected int getLayoutResID() {
        return R.layout.activity_apps;
    }

    @Override
    protected void init() {
        initView();
        initData();
    }

    private void toggleDelMode() {
        // takagen99: Toggle Delete Mode
        HawkConfig.hotVodDelete = !HawkConfig.hotVodDelete;
        appsAdapter.notifyDataSetChanged();

        delMode = !delMode;
        tvDelTip.setVisibility(delMode ? View.VISIBLE : View.GONE);

        // takagen99: Added Theme Color
//        tvDel.setTextColor(delMode ? getThemeColor() : Color.WHITE);
    }

    private void initView() {
        EventBus.getDefault().register(this);
        tvDelTip = findViewById(R.id.tvDelTip);
        tvDelete = findViewById(R.id.tvDelete);
        mGridViewApps = findViewById(R.id.mGridViewApps);
        mGridViewApps.setHasFixedSize(true);
        mGridViewApps.setLayoutManager(new V7GridLayoutManager(this.mContext, isBaseOnWidth() ? 6 : 7));
        appsAdapter = new AppsAdapter();
        mGridViewApps.setAdapter(appsAdapter);
        tvDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDelMode();
            }
        });
        mGridViewApps.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            @Override
            public boolean onInBorderKeyEvent(int direction, View focused) {
                if (direction == View.FOCUS_UP) {
                    tvDelete.setFocusable(true);
                    tvDelete.requestFocus();
                }
                return false;
            }
        });
        mGridViewApps.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
			//修改放大参数
                itemView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {

            }
        });
        appsAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                AppInfo appInfo = appsAdapter.getData().get(position);
                if (delMode) {
                    // Trigger to uninstall
                    Uri packageURI = Uri.parse("package:" + appInfo.getPack());
                    Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                    uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(uninstallIntent);

                    // Storing Package Info
                    packageName = appInfo.getPack();
                    isUnInstallClicked = true;
                    appPosition = position;
                } else {
                    // Trigger to start activity
                    try {
                        startActivity(getPackageManager().getLaunchIntentForPackage(appInfo.getPack()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        appsAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                tvDelete.setFocusable(true);
                toggleDelMode();
                return true;
            }
        });
    }

    private void initData() {

        // Method 1 (Direct)
//        List<AppInfo> items = new ArrayList<>();
//        PackageManager pm = getPackageManager();
//        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
//
//        for (ApplicationInfo app : apps) {
//            if(pm.getLaunchIntentForPackage(app.packageName) != null) {
//                // apps with launcher intent
//                if((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
//                    // updated system apps
//                } else if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
//                    // system apps
//                } else {
//                    // user installed apps & not equal to self
//                    if (!app.packageName.equals(App.getInstance().getPackageName())) {
//                        items.add(AppInfo.get(app));
//                    }
//                }
//            }
//            AppInfo.Sorter.sort(items);
//            appsAdapter.setNewData(items);
//        }

        // Method 2 (Test)
        List<AppInfo> appInfos = getInstallApps(getApplicationContext());
        if (appInfos == null) {
            return;
        }
        AppInfo.Sorter.sort(appInfos);
        appsAdapter.setNewData(appInfos);

        // Method 3 (via Async)
//        mApplicationLoader.execute();

    }

    public List<AppInfo> getInstallApps(Context context) {
        List<AppInfo> items = new ArrayList<>();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                // apps with launcher intent
                if ((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    // updated system apps
                } else if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    // system apps
                } else {
                    // user installed apps & not equal to self
                    if (!app.packageName.equals(App.getInstance().getPackageName())) {
                        items.add(AppInfo.get(app));
                    }
                }
            }
        }
        return items;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_APP_REFRESH) {
            initData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if (delMode) {
            toggleDelMode();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // if clicked and App is already uninstalled
        if (isUnInstallClicked && !appInstalledOrNot(packageName)) {
            appsAdapter.remove(appPosition);
//            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_APP_REFRESH));
        }
    }

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

}