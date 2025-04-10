package com.github.tvbox.osc.ui.activity;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.blankj.utilcode.util.ServiceUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.bean.VodSeriesGroup;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.player.controller.VodController;
import com.github.tvbox.osc.server.PlayService;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesFlagAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesGroupAdapter;
import com.github.tvbox.osc.ui.dialog.DescDialog;
import com.github.tvbox.osc.ui.dialog.PushDialog;
import com.github.tvbox.osc.ui.dialog.QuickSearchDialog;
import com.github.tvbox.osc.ui.fragment.PlayFragment;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.SearchHelper;
import com.github.tvbox.osc.util.StringUtils;
import com.github.tvbox.osc.util.SubtitleHelper;
import com.github.tvbox.osc.util.thunder.Thunder;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
public class DetailActivity extends BaseActivity {
    private LinearLayout llLayout;
    private FragmentContainerView llPlayerFragmentContainer;
    private View llPlayerFragmentContainerBlock;
    private View llPlayerPlace;
    private PlayFragment playFragment = null;
    private ImageView ivThumb;
    private TextView tvName;
    private TextView tvYear;
    private TextView tvSite;
    private TextView tvArea;
    private TextView tvLang;
    private TextView tvType;
    private TextView tvActor;
    private TextView tvDirector;
    private TextView tvDes;
    private TextView tvDesc;
    private TextView tvPlay;
    private TextView tvSort;
    private TextView tvPush;
    private TextView tvQuickSearch;
    private TextView tvCollect;
    private TvRecyclerView mGridViewFlag;
    private TvRecyclerView mGridView;
    private TvRecyclerView mSeriesGroupView;
    private LinearLayout mEmptyPlayList;
    private SourceViewModel sourceViewModel;
    private Movie.Video mVideo;
    private VodInfo vodInfo;
    private SeriesFlagAdapter seriesFlagAdapter;
    private SeriesAdapter seriesAdapter;
    public String vodId;
    public String sourceKey;
    public String firstsourceKey;
    boolean seriesSelect = false;
    private View seriesFlagFocus = null;
    private HashMap<String, String> mCheckSources = null;
    private V7GridLayoutManager mGridViewLayoutMgr = null;
    private String preFlag = "";
    private List<Runnable> pauseRunnable = null;
    private String searchTitle = "";
    private boolean hadQuickStart = false;
    private final List<String> quickSearchWord = new ArrayList<>();
    private ExecutorService searchExecutorService = null;
    private SeriesGroupAdapter seriesGroupAdapter;
    private List<List<VodInfo.VodSeries>> uu;
    private int GroupCount;
    private int GroupIndex = 0;

    // preview : true 开启 false 关闭
    VodInfo previewVodInfo = null;
    boolean showPreview = Hawk.get(HawkConfig.SHOW_PREVIEW, true);
    public boolean fullWindows = false;
    ViewGroup.LayoutParams windowsPreview = null;
    ViewGroup.LayoutParams windowsFull = null;

    private final List<Movie.Video> quickSearchData = new ArrayList<>();
    private BroadcastReceiver pipActionReceiver;
    public static final String BROADCAST_ACTION = "VOD_CONTROL";
    public static final int BROADCAST_ACTION_PREV = 0;
    public static final int BROADCAST_ACTION_PLAYPAUSE = 1;
    public static final int BROADCAST_ACTION_NEXT = 2;

    private ImageView tvPlayUrl;
    /**
     * Home键广播,用于触发后台服务
     */
    private BroadcastReceiver mHomeKeyReceiver;
    /**
     * 是否开启后台播放标记,不在广播开启,onPause根据标记开启
     */
    boolean openBackgroundPlay;

    public static int getNum(String str) {
        try {
            Matcher matcher = Pattern.compile("\\d+")
                    .matcher(str);
            if (!matcher.find()) {
                return 0;
            }
            String group = matcher.group(0);
            if (TextUtils.isEmpty(group)) {
                return 0;
            }
            return Integer.parseInt(group);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_detail;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        initReceiver();
        initView();
        initViewModel();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundPlay = false;
        playServerSwitch(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (openBackgroundPlay) {
            playServerSwitch(true);
        }
    }

    private void initReceiver() {
        // 注册广播接收器
        if (mHomeKeyReceiver == null) {
            mHomeKeyReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action != null && action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                        openBackgroundPlay = Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0) == 1 && playFragment.getPlayer() != null && playFragment.getPlayer().isPlaying();
                    }
                }
            };
            registerReceiver(mHomeKeyReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }
    }

    private void initView() {
        llLayout = findViewById(R.id.llLayout);
        llPlayerPlace = findViewById(R.id.previewPlayerPlace);
        llPlayerFragmentContainer = findViewById(R.id.previewPlayer);
        llPlayerFragmentContainerBlock = findViewById(R.id.previewPlayerBlock);
        ivThumb = findViewById(R.id.ivThumb);
        llPlayerPlace.setVisibility(showPreview ? View.VISIBLE : View.GONE);
        ivThumb.setVisibility(!showPreview ? View.VISIBLE : View.GONE);
        tvName = findViewById(R.id.tvName);
        tvYear = findViewById(R.id.tvYear);
        tvSite = findViewById(R.id.tvSite);
        tvArea = findViewById(R.id.tvArea);
        tvLang = findViewById(R.id.tvLang);
        tvType = findViewById(R.id.tvType);
        tvActor = findViewById(R.id.tvActor);
        tvDirector = findViewById(R.id.tvDirector);
        tvDes = findViewById(R.id.tvDes);
        tvDesc = findViewById(R.id.tvDesc);
        tvPlay = findViewById(R.id.tvPlay);
        tvSort = findViewById(R.id.tvSort);
        tvPush = findViewById(R.id.tvPush);
        tvCollect = findViewById(R.id.tvCollect);
        tvQuickSearch = findViewById(R.id.tvQuickSearch);
        tvPlayUrl = findViewById(R.id.tvPlayUrl);
        mEmptyPlayList = findViewById(R.id.mEmptyPlaylist);
        mGridView = findViewById(R.id.mGridView);
        mGridView.setHasFixedSize(false);
        mGridViewLayoutMgr = new V7GridLayoutManager(this.mContext, 6);
        mGridView.setLayoutManager(mGridViewLayoutMgr);
//        mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, isBaseOnWidth() ? 6 : 7));
        seriesAdapter = new SeriesAdapter();
        mGridView.setAdapter(seriesAdapter);
        mGridViewFlag = findViewById(R.id.mGridViewFlag);
        mGridViewFlag.setHasFixedSize(true);
        mGridViewFlag.setLayoutManager(new V7LinearLayoutManager(this.mContext, 0, false));
        seriesFlagAdapter = new SeriesFlagAdapter();
        mGridViewFlag.setAdapter(seriesFlagAdapter);
        if (showPreview) {
            playFragment = new PlayFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.previewPlayer, playFragment).commit();
            getSupportFragmentManager().beginTransaction().show(playFragment).commitAllowingStateLoss();
            tvPlay.setText(getString(R.string.det_expand));
            tvPlay.setVisibility(View.GONE);
        } else {
            tvPlay.setVisibility(View.VISIBLE);
            tvPlay.requestFocus();
        }

        mSeriesGroupView = findViewById(R.id.mSeriesGroupView);
        mSeriesGroupView.setHasFixedSize(true);
        mSeriesGroupView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 0, false));
        seriesGroupAdapter = new SeriesGroupAdapter();
        mSeriesGroupView.setAdapter(seriesGroupAdapter);

        tvSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vodInfo != null && vodInfo.seriesMap.size() > 0) {
                    vodInfo.reverseSort = !vodInfo.reverseSort;
                    preFlag = "";
                    if (vodInfo.seriesMap.get(vodInfo.playFlag).size() > vodInfo.getplayIndex()) {
                        vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.getplayIndex()).selected = false;
                    }
                    vodInfo.reverse();
                    if (vodInfo.seriesMap.get(vodInfo.playFlag).size() > vodInfo.getplayIndex()) {
                        vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.getplayIndex()).selected = true;
                    }
                    refreshList();
                    insertVod(firstsourceKey, vodInfo);
                    //seriesAdapter.notifyDataSetChanged();
                }
            }
        });

        tvPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PushDialog pushDialog = new PushDialog(mContext);
                pushDialog.show();
            }
        });
        tvPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                if (showPreview) {
                    toggleFullPreview();
                } else {
                    jumpToPlay();
                }
            }
        });
        // takagen99 : Added click Image Thummb or Preview Window to play video
        ivThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                jumpToPlay();
            }
        });
        llPlayerFragmentContainerBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                toggleFullPreview();
            }
        });
        tvQuickSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQuickSearch();
                QuickSearchDialog quickSearchDialog = new QuickSearchDialog(DetailActivity.this);
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, quickSearchData));
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, quickSearchWord));
                quickSearchDialog.show();
                if (pauseRunnable != null && pauseRunnable.size() > 0) {
                    searchExecutorService = Executors.newFixedThreadPool(5);
                    for (Runnable runnable : pauseRunnable) {
                        searchExecutorService.execute(runnable);
                    }
                    pauseRunnable.clear();
                    pauseRunnable = null;
                }
                quickSearchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        try {
                            if (searchExecutorService != null) {
                                pauseRunnable = searchExecutorService.shutdownNow();
                                searchExecutorService = null;
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                });
            }
        });
        tvCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = tvCollect.getText().toString();
                if (getString(R.string.det_fav_unstar).equals(text)) {
                    RoomDataManger.insertVodCollect(sourceKey, vodInfo);
                    Toast.makeText(DetailActivity.this, getString(R.string.det_fav_add), Toast.LENGTH_SHORT).show();
                    tvCollect.setText(getString(R.string.det_fav_star));
                } else {
                    RoomDataManger.deleteVodCollect(sourceKey, vodInfo);
                    Toast.makeText(DetailActivity.this, getString(R.string.det_fav_del), Toast.LENGTH_SHORT).show();
                    tvCollect.setText(getString(R.string.det_fav_unstar));
                }
            }
        });
        tvDesc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FastClickCheckUtil.check(v);
                        DescDialog dialog = new DescDialog(mContext);
                        //  dialog.setTip("内容简介");
                        dialog.setDescribe(removeHtmlTag(mVideo.des));
                        dialog.show();
                    }
                });
            }
        });

        tvDesc.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FastClickCheckUtil.check(v);
                        ClipboardManager clipprofile = (ClipboardManager) DetailActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                        String cpContent = removeHtmlTag(mVideo.des);
                        ClipData clipData = ClipData.newPlainText(null, cpContent);
                        clipprofile.setPrimaryClip(clipData);
                        Toast.makeText(DetailActivity.this, "已复制：" + cpContent, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }
        });

        tvPlayUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取剪切板管理器
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                //设置内容到剪切板
                cm.setPrimaryClip(ClipData.newPlainText(null, vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.getplayIndex()).url));
                Toast.makeText(DetailActivity.this, getString(R.string.det_url), Toast.LENGTH_SHORT).show();
            }
        });
        mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                seriesSelect = false;
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                seriesSelect = true;
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
            }
        });
        mGridViewFlag.setOnItemListener(new TvRecyclerView.OnItemListener() {
            private void refresh(View itemView, int position) {
                if (position == -1) {
                    return;
                }
                String newFlag = seriesFlagAdapter.getData().get(position).name;
                if (vodInfo != null && !vodInfo.playFlag.equals(newFlag)) {
                    for (int i = 0; i < vodInfo.seriesFlags.size(); i++) {
                        VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(i);
                        if (flag.name.equals(vodInfo.playFlag)) {
                            flag.selected = false;
                            seriesFlagAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(position);
                    flag.selected = true;

                    // clean pre flag select status
                    if (vodInfo.seriesMap.get(vodInfo.playFlag).size() > vodInfo.getplayIndex()) {
                        vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).selected = false;
                    }
                    vodInfo.playFlag = newFlag;
                    seriesFlagAdapter.notifyItemChanged(position);
                    refreshList();
                }
                seriesFlagFocus = itemView;
            }

            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
            }
        });
        seriesAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                if (vodInfo != null && vodInfo.seriesMap.get(vodInfo.playFlag).size() > 0) {
                    boolean reload = false;
                    if (vodInfo.getplayIndex() != GroupIndex * GroupCount + position) {
                        for (int i = 0; i < seriesAdapter.getData().size(); i++) {
                            VodInfo.VodSeries Series = seriesAdapter.getData().get(i);
                            Series.selected = false;
                            seriesAdapter.notifyItemChanged(i);
                        }
                        seriesAdapter.getData().get(position).selected = true;
                        seriesAdapter.notifyItemChanged(position);
                        vodInfo.playIndex = position;
                        vodInfo.playGroup = GroupIndex;
                        reload = true;
                    }
                    //解决当前集不刷新的BUG
                    if (!vodInfo.playFlag.equals(preFlag)) {
                        reload = true;
                    }
                    //选集全屏 想选集不全屏的注释下面一行
                    if (showPreview && !fullWindows && playFragment.getPlayer().isPlaying())
                        toggleFullPreview();
                    if (reload || !showPreview) jumpToPlay();
                }
            }
        });

        mSeriesGroupView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            public void refresh(View itemView, int position) {

                if (GroupIndex != position) {
                    seriesGroupAdapter.getData().get(GroupIndex).selected = false;
                    seriesGroupAdapter.notifyItemChanged(GroupIndex);
                    seriesGroupAdapter.getData().get(position).selected = true;
                    seriesGroupAdapter.notifyItemChanged(position);
                    GroupIndex = position;
                    seriesAdapter.setNewData(uu.get(position));
                }
            }

            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {

            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
            }
        });

        setLoadSir(llLayout);

        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) DetailActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                String cpContent = "视频ID：" + vodId + "，图片地址：" + (mVideo == null ? "" : mVideo.pic);
                ClipData clipData = ClipData.newPlainText(null, cpContent);
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(DetailActivity.this, "已复制" + cpContent, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void jumpToPlay() {
        if (vodInfo != null && vodInfo.seriesMap.get(vodInfo.playFlag).size() > 0) {
            preFlag = vodInfo.playFlag;
            Bundle bundle = new Bundle();
            //保存历史
            insertVod(firstsourceKey, vodInfo);
            bundle.putString("sourceKey", sourceKey);
            bundle.putSerializable("VodInfo", vodInfo);
            if (showPreview) {
                if (previewVodInfo == null) {
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(vodInfo);
                        oos.flush();
                        oos.close();
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
                        previewVodInfo = (VodInfo) ois.readObject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (previewVodInfo != null) {
                    previewVodInfo.playerCfg = vodInfo.playerCfg;
                    previewVodInfo.playFlag = vodInfo.playFlag;
                    previewVodInfo.playIndex = vodInfo.playIndex;
                    previewVodInfo.playGroup = vodInfo.playGroup;
                    previewVodInfo.reverseSort = vodInfo.reverseSort;
                    previewVodInfo.playGroupCount = vodInfo.playGroupCount;
                    previewVodInfo.seriesMap = vodInfo.seriesMap;
                    bundle.putSerializable("VodInfo", previewVodInfo);
                }
                playFragment.setData(bundle);
            } else {
                jumpActivity(PlayActivity.class, bundle);
            }
        }
    }

    private void refreshList() {
        try {
            if (vodInfo.seriesMap.get(vodInfo.playFlag).size() <= vodInfo.getplayIndex()) {
                vodInfo.playIndex = 0;
            }
            if (vodInfo.seriesMap.get(vodInfo.playFlag) != null) {
                int playIndex = this.vodInfo.getplayIndex();
                if (vodInfo.seriesMap.get(vodInfo.playFlag).size() >= playIndex) {
                    vodInfo.seriesMap.get(vodInfo.playFlag).get(playIndex).selected = true;
                } else {
                    // 到了这里说明当前选中的播放源总播放集数 小于 上次选中的播放源的总集数
                    vodInfo.playGroup = 0;
                }
            }

            List<VodInfo.VodSeries> list = vodInfo.seriesMap.get(vodInfo.playFlag);
            int index = 0;
            for (VodInfo.VodSeries vodSeries : list) {
                index++;
                if (TextUtils.isEmpty(vodSeries.name)) {
                    if (list.size() == 1) {
                        vodSeries.name = vodInfo.name;
                    }
                    if (TextUtils.isEmpty(vodSeries.name)) {
                        vodSeries.name = "" + index;
                    }
                }
            }

            // Dynamic series list width
            Paint pFont = new Paint();
            //List<VodInfo.VodSeries> list = vodInfo.seriesMap.get(vodInfo.playFlag);
            int listSize = list.size();
            int w = 1;
            for (int i = 0; i < listSize; ++i) {
                String name = list.get(i).name;
                if (w < (int) pFont.measureText(name)) {
                    w = (int) pFont.measureText(name);
                }
            }
            w += 32;
            int screenWidth = getWindowManager().getDefaultDisplay().getWidth() / 3;
            int offset = screenWidth / w;
            if (offset <= 2) offset = 2;
            if (offset > 6) offset = 6;
            mGridViewLayoutMgr.setSpanCount(offset);

            List<VodSeriesGroup> seriesGroupList = getSeriesGroupList();
            seriesGroupList.get(vodInfo.playGroup).selected = true;
            seriesGroupAdapter.setNewData(seriesGroupList);
            seriesAdapter.setNewData(uu.get(vodInfo.playGroup));

            mGridView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mGridView.scrollToPosition(vodInfo.playGroup);
                    mSeriesGroupView.scrollToPosition(vodInfo.playGroup);
                }
            }, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<VodSeriesGroup> getSeriesGroupList() {
        List<VodSeriesGroup> arrayList = new ArrayList<>();
        if (uu != null) {
            uu.clear();
        } else {
            uu = new ArrayList<>();
        }
        try {
            List<VodInfo.VodSeries> vodSeries = vodInfo.seriesMap.get(vodInfo.playFlag);
            int size = vodSeries.size();
            GroupCount = size > 2500.0d ? 300 : size > 1500.0d ? 200 : size > 1000.0d ? 150 : size > 500.0d ? 100 : size > 300.0d ? 50 : size > 100.0d ? 30 : 20;
            vodInfo.playGroupCount = GroupCount;
            GroupIndex = (int) Math.floor(vodInfo.getplayIndex() / (GroupCount + 0.0f));
            if (GroupIndex < 0) {
                GroupIndex = 0;
            }
            int Groups = (int) Math.ceil(size / (GroupCount + 0.0f));
            for (int i = 0; i < Groups; i++) {
                mSeriesGroupView.setVisibility(View.VISIBLE);
                int s = (i * GroupCount) + 1;
                int e = (i + 1) * GroupCount;
                int name_s = s;
                int name_e = e;
                if (vodInfo.reverseSort) {
                    name_s = size - i * GroupCount;
                    name_e = size - (i + 1) * GroupCount;
                }
                List<VodInfo.VodSeries> info = new ArrayList<>();
                if (e < size) {
                    for (int j = s - 1; j < e; j++) {
                        info.add(vodSeries.get(j));
                    }
                    arrayList.add(new VodSeriesGroup(name_s + "-" + name_e));
                } else {
                    for (int j = s - 1; j < size; j++) {
                        info.add(vodSeries.get(j));
                    }
                    if (vodInfo.reverseSort) {
                        arrayList.add(new VodSeriesGroup(name_s + "-" + 1));
                    } else {
                        arrayList.add(new VodSeriesGroup(name_s + "-" + size));
                    }
                }
                uu.add(info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    private void setTextShow(TextView view, String tag, String info) {
        if (info == null || info.trim().isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setText(Html.fromHtml(getHtml(tag, info)));
    }

    private String removeHtmlTag(String info) {
        if (info == null)
            return "";
        return info.replaceAll("\\<.*?\\>", "").replaceAll("\\s", "");
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.detailResult.observe(this, new Observer<AbsXml>() {
            @Override
            public void onChanged(AbsXml absXml) {
                if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
                    showSuccess();
                    if (!TextUtils.isEmpty(absXml.msg) && !absXml.msg.equals("数据列表")) {
                        Toast.makeText(DetailActivity.this, absXml.msg, Toast.LENGTH_SHORT).show();
                        showEmpty();
                        return;
                    }
                    mVideo = absXml.movie.videoList.get(0);
                    mVideo.id = vodId;
                    if (TextUtils.isEmpty(mVideo.name)) mVideo.name = "片名";
                    vodInfo = new VodInfo();
                    vodInfo.setVideo(mVideo);
                    vodInfo.sourceKey = mVideo.sourceKey;
                    sourceKey = mVideo.sourceKey;

                    tvName.setText(mVideo.name);
                    setTextShow(tvSite, getString(R.string.det_source), ApiConfig.get().getSource(firstsourceKey).getName());
                    setTextShow(tvYear, getString(R.string.det_year), mVideo.year == 0 ? "" : String.valueOf(mVideo.year));
                    setTextShow(tvArea, getString(R.string.det_area), mVideo.area);
                    setTextShow(tvLang, getString(R.string.det_lang), mVideo.lang);
                    if (!firstsourceKey.equals(sourceKey)) {
                        setTextShow(tvType, getString(R.string.det_type), "[" + ApiConfig.get().getSource(sourceKey).getName() + "] 解析");
                    } else {
                        setTextShow(tvType, getString(R.string.det_type), mVideo.type);
                    }
                    setTextShow(tvActor, getString(R.string.det_actor), mVideo.actor);
                    setTextShow(tvDirector, getString(R.string.det_dir), mVideo.director);
                    setTextShow(tvDes, getString(R.string.det_des), removeHtmlTag(mVideo.des));
                    if (!TextUtils.isEmpty(mVideo.pic)) {
                        // takagen99 : Use Glide instead : Rounding Radius is in pixel
                        ImgUtil.load(mVideo.pic, ivThumb, 14);
                    } else {
                        ivThumb.setImageResource(R.drawable.img_loading_placeholder);
                    }

                    if (vodInfo.seriesMap != null && vodInfo.seriesMap.size() > 0) {
                        mGridViewFlag.setVisibility(View.VISIBLE);
                        mGridView.setVisibility(View.VISIBLE);
                        mEmptyPlayList.setVisibility(View.GONE);

                        VodInfo vodInfoRecord = RoomDataManger.getVodInfo(sourceKey, vodId);
                        // 读取历史记录
                        if (vodInfoRecord != null) {
                            vodInfo.playIndex = Math.max(vodInfoRecord.playIndex, 0);
                            vodInfo.playGroup = Math.max(vodInfoRecord.playGroup, 0);
                            GroupIndex = vodInfo.playGroup;
                            vodInfo.playGroupCount = Math.max(vodInfoRecord.playGroupCount, 0);
                            GroupCount = vodInfo.playGroupCount;
                            vodInfo.playFlag = vodInfoRecord.playFlag;
                            vodInfo.playerCfg = vodInfoRecord.playerCfg;
                            vodInfo.reverseSort = vodInfoRecord.reverseSort;
                        } else {
                            vodInfo.playIndex = 0;
                            vodInfo.playGroup = 0;
                            vodInfo.playFlag = null;
                            vodInfo.playerCfg = "";
                            vodInfo.reverseSort = false;
                        }

                        if (vodInfo.reverseSort) {
                            vodInfo.reverse();
                        }

                        if (vodInfo.playFlag == null || !vodInfo.seriesMap.containsKey(vodInfo.playFlag))
                            vodInfo.playFlag = (String) vodInfo.seriesMap.keySet().toArray()[0];

                        int flagScrollTo = 0;
                        for (int j = 0; j < vodInfo.seriesFlags.size(); j++) {
                            VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(j);
                            if (flag.name.equals(vodInfo.playFlag)) {
                                flagScrollTo = j;
                                flag.selected = true;
                            } else
                                flag.selected = false;
                        }

                        seriesFlagAdapter.setNewData(vodInfo.seriesFlags);
                        mGridViewFlag.scrollToPosition(flagScrollTo);

                        refreshList();
                        if (showPreview) {
                            jumpToPlay();
                            llPlayerFragmentContainer.setVisibility(View.VISIBLE);
                            llPlayerFragmentContainerBlock.setVisibility(View.VISIBLE);
                            llPlayerFragmentContainerBlock.requestFocus();
                            toggleSubtitleTextSize();
                        }
                    } else {
                        mGridViewFlag.setVisibility(View.GONE);
                        mGridView.setVisibility(View.GONE);
                        tvPlay.setVisibility(View.GONE);
                        tvSort.setVisibility(View.GONE);
                        mEmptyPlayList.setVisibility(View.VISIBLE);
                    }
                } else {
                    showEmpty();
                    llPlayerFragmentContainer.setVisibility(View.GONE);
                    llPlayerFragmentContainerBlock.setVisibility(View.GONE);
                }
            }
        });
    }

    private String getHtml(String label, String content) {
        if (content == null) {
            content = "";
        }
        if (label.length() > 0) {
            label = label + ": ";
        }
        return label + "<font color=\"#FFFFFF\">" + content + "</font>";
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            loadDetail(bundle.getString("id", null), bundle.getString("sourceKey", ""));
        }
    }

    private void loadDetail(String vid, String key) {
        if (vid != null) {
            vodId = vid;
            sourceKey = key;
            firstsourceKey = key;
            showLoading();
            sourceViewModel.getDetail(sourceKey, vodId);

            boolean isVodCollect = RoomDataManger.isVodCollect(sourceKey, vodId);
            if (isVodCollect) {
                tvCollect.setText(getString(R.string.det_fav_star));
            } else {
                tvCollect.setText(getString(R.string.det_fav_unstar));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_REFRESH) {
            if (event.obj != null) {
                if (event.obj instanceof Integer) {
                    int index = (int) event.obj;
                    int mGroupIndex = (int) Math.floor(index / (GroupCount + 0.0f));
                    boolean changeGroup = false;
                    if (mGroupIndex != GroupIndex) {
                        changeGroup = true;
                        seriesAdapter.getData().get(vodInfo.playIndex).selected = false;
                        seriesAdapter.notifyItemChanged(vodInfo.playIndex);
                        seriesGroupAdapter.getData().get(GroupIndex).selected = false;
                        seriesGroupAdapter.notifyItemChanged(GroupIndex);
                        seriesGroupAdapter.getData().get(mGroupIndex).selected = true;
                        seriesGroupAdapter.notifyItemChanged(mGroupIndex);
                        seriesAdapter.setNewData(uu.get(mGroupIndex));
                        GroupIndex = mGroupIndex;
                        mSeriesGroupView.scrollToPosition(mGroupIndex);
                    }
                    if (index != vodInfo.getplayIndex()) {
                        if (!changeGroup) {
                            seriesAdapter.getData().get(vodInfo.playIndex).selected = false;
                            seriesAdapter.notifyItemChanged(vodInfo.playIndex);
                        }
                        vodInfo.playIndex = index % GroupCount;
                        vodInfo.playGroup = index / GroupCount;
                        seriesAdapter.getData().get(vodInfo.playIndex).selected = true;
                        seriesAdapter.notifyItemChanged(vodInfo.playIndex);
                        mGridView.scrollToPosition(vodInfo.playIndex);
                        //保存历史
                        insertVod(firstsourceKey, vodInfo);
                    }
                } else if (event.obj instanceof JSONObject) {
                    vodInfo.playerCfg = ((JSONObject) event.obj).toString();
                    //保存历史
                    insertVod(firstsourceKey, vodInfo);
                }
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_SELECT) {
            if (event.obj != null) {
                Movie.Video video = (Movie.Video) event.obj;
                loadDetail(video.id, video.sourceKey);
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_WORD_CHANGE) {
            if (event.obj != null) {
                String word = (String) event.obj;
                switchSearchWord(word);
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void pushVod(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_PUSH_VOD) {
            if (event.obj != null) {
                List<String> data = (List<String>) event.obj;
                OkGo.getInstance().cancelTag("pushVod");
                OkGo.<String>post("http://" + data.get(0) + ":" + data.get(1) + "/action")
                        .tag("pushVod")
                        .params("id", vodId)
                        .params("sourceKey", sourceKey)
                        .params("do", "mirror")
                        .execute(new AbsCallback<String>() {
                            @Override
                            public String convertResponse(okhttp3.Response response) throws Throwable {
                                if (response.body() != null) {
                                    return response.body().string();
                                } else {
                                    Toast.makeText(DetailActivity.this, "推送失败，填的地址可能不对", Toast.LENGTH_SHORT).show();
                                    throw new IllegalStateException("网络请求错误");
                                }
                            }

                            @Override
                            public void onSuccess(Response<String> response) {
                                String r = response.body();
                                if ("mirrored".equals(r))
                                    Toast.makeText(DetailActivity.this, "推送成功", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(DetailActivity.this, "推送失败，远端tvbox版本不支持", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Response<String> response) {
                                super.onError(response);
                                Toast.makeText(DetailActivity.this, "推送失败，填的地址可能不对", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    private void switchSearchWord(String word) {
        OkGo.getInstance().cancelTag("quick_search");
        quickSearchData.clear();
        searchTitle = word;
        searchResult();
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    private void startQuickSearch() {
        initCheckedSourcesForSearch();
        if (hadQuickStart)
            return;
        hadQuickStart = true;
        OkGo.getInstance().cancelTag("quick_search");
        quickSearchWord.clear();
        searchTitle = mVideo.name;
        quickSearchData.clear();
        quickSearchWord.add(searchTitle);
        // 分词
        OkGo.<String>get("https://api.yesapi.cn/?service=App.Scws.GetWords&text=" + searchTitle + "&app_key=CEE4B8A091578B252AC4C92FB4E893C3&sign=CB7602F3AC922808AF5D475D8DA33302")
                .tag("fenci")
                .execute(new AbsCallback<String>() {
                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        if (response.body() != null) {
                            return response.body().string();
                        } else {
                            throw new IllegalStateException("网络请求错误");
                        }
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        String json = response.body();
                        quickSearchWord.clear();
                        try {
                            JsonObject resJson = JsonParser.parseString(json).getAsJsonObject();
                            JsonElement wordsJson = resJson.get("data").getAsJsonObject().get("words");

                            for (JsonElement je : wordsJson.getAsJsonArray()) {
                                quickSearchWord.add(je.getAsJsonObject().get("word").getAsString());
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                        quickSearchWord.add(searchTitle);
                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, quickSearchWord));
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }
                });

        searchResult();
    }

    private void searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        searchExecutorService = Executors.newFixedThreadPool(5);
        List<SourceBean> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);

        ArrayList<String> siteKey = new ArrayList<>();
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable() || !bean.isQuickSearch()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
        }
        for (String key : siteKey) {
            searchExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    sourceViewModel.getQuickSearch(key, searchTitle);
                }
            });
        }
    }

    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                // 去除当前相同的影片
                if (video.sourceKey.equals(sourceKey) && video.id.equals(vodId))
                    continue;
                data.add(video);
            }
            quickSearchData.addAll(data);
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, data));
        }
    }

    private void insertVod(String sourceKey, VodInfo vodInfo) {
        try {
            vodInfo.playNote = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.getplayIndex()).name;
        } catch (Throwable th) {
            vodInfo.playNote = "";
        }
        RoomDataManger.insertVodRecord(sourceKey, vodInfo);
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        if (mHomeKeyReceiver != null) {
            unregisterReceiver(mHomeKeyReceiver);
            mHomeKeyReceiver = null;
        }
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        OkGo.getInstance().cancelTag("fenci");
        OkGo.getInstance().cancelTag("detail");
        OkGo.getInstance().cancelTag("quick_search");
        OkGo.getInstance().cancelTag("pushVod");
        EventBus.getDefault().unregister(this);
        if (!showPreview) Thunder.stop(true);
    }

    @Override
    public void onUserLeaveHint() {
        // takagen99 : Additional check for external player
        if (supportsPiPMode() && showPreview && !playFragment.extPlay && Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0) == 2) {
            // 创建一个Intent对象，模拟按下Home键
            try { //部分电视使用该方法启动首页闪退比如小米的澎湃OS
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            } catch (SecurityException e) {
                e.printStackTrace();
                ToastUtils.showShort("画中画 开启失败!");
                return;
            }
            // Calculate Video Resolution
            int vWidth = playFragment.mVideoView.getVideoSize()[0];
            int vHeight = playFragment.mVideoView.getVideoSize()[1];
            Rational ratio = null;
            if (vWidth != 0) {
                if ((((double) vWidth) / ((double) vHeight)) > 2.39) {
                    vHeight = (int) (((double) vWidth) / 2.35);
                }
                ratio = new Rational(vWidth, vHeight);
            } else {
                ratio = new Rational(16, 9);
            }
            List<RemoteAction> actions = new ArrayList<>();
            actions.add(generateRemoteAction(android.R.drawable.ic_media_previous, BROADCAST_ACTION_PREV, "Prev", "Play Previous"));
            actions.add(generateRemoteAction(android.R.drawable.ic_media_play, BROADCAST_ACTION_PLAYPAUSE, "Play", "Play/Pause"));
            actions.add(generateRemoteAction(android.R.drawable.ic_media_next, BROADCAST_ACTION_NEXT, "Next", "Play Next"));
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(ratio)
                    .setActions(actions).build();
            if (!fullWindows) {
                toggleFullPreview();
            }
            enterPictureInPictureMode(params);
            playFragment.getVodController().hideBottom();
            playFragment.getPlayer().postDelayed(() -> {
                if (!playFragment.getPlayer().isPlaying()) {
                    playFragment.getVodController().togglePlay();
                }
            }, 400);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private RemoteAction generateRemoteAction(int iconResId, int actionCode, String title, String desc) {
        final PendingIntent intent =
                PendingIntent.getBroadcast(
                        DetailActivity.this,
                        actionCode,
                        new Intent(BROADCAST_ACTION).putExtra("action", actionCode),
                        0);
        final Icon icon = Icon.createWithResource(DetailActivity.this, iconResId);
        return (new RemoteAction(icon, title, desc, intent));
    }

    /**
     * 事件接收广播(画中画/后台播放点击事件)
     *
     * @param isRegister 注册/注销
     */
    private void registerActionReceiver(boolean isRegister) {
        if (isRegister) {
            pipActionReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || !intent.getAction().equals(BROADCAST_ACTION) || playFragment.getVodController() == null) {
                        return;
                    }

                    int currentStatus = intent.getIntExtra("action", 1);
                    if (currentStatus == BROADCAST_ACTION_PREV) {
                        playFragment.playPrevious();
                    } else if (currentStatus == BROADCAST_ACTION_PLAYPAUSE) {
                        playFragment.getVodController().togglePlay();
                    } else if (currentStatus == BROADCAST_ACTION_NEXT) {
                        playFragment.playNext(false);
                    }
                }
            };
            registerReceiver(pipActionReceiver, new IntentFilter(BROADCAST_ACTION));

        } else {
            if (pipActionReceiver != null) {
                unregisterReceiver(pipActionReceiver);
                pipActionReceiver = null;
            }
            if (playFragment.getPlayer().isPlaying()) {
                playFragment.getVodController().togglePlay();
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        registerActionReceiver(supportsPiPMode() && isInPictureInPictureMode);
    }

    /**
     * 后台播放服务开关,开启时注册操作广播,关闭时注销
     */
    private void playServerSwitch(boolean open) {
        if (open) {
            VodInfo.VodSeries vod = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex);
            PlayService.start(playFragment.getPlayer(), vodInfo.name + "&&" + vod.name);
            registerActionReceiver(true);
        } else {
            if (ServiceUtils.isServiceRunning(PlayService.class)) {
                PlayService.stop();
                registerActionReceiver(false);
            }
        }
    }

    @Override
    public void onBackPressed() {
        boolean showPreview = Hawk.get(HawkConfig.SHOW_PREVIEW, true);
        if (fullWindows) {
            if (playFragment.onBackPressed())
                return;
            playFragment.getVodController().mProgressTop.setVisibility(View.INVISIBLE);
            toggleFullPreview();
            mGridView.requestFocus();
            return;
        } else if (seriesSelect) {
            if (seriesFlagFocus != null && !seriesFlagFocus.isFocused()) {
                seriesFlagFocus.requestFocus();
                return;
            }
        } else if (showPreview && playFragment != null) {
            playFragment.setPlayTitle(false);
            playFragment.mVideoView.release();
        }
        super.onBackPressed();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && playFragment != null && fullWindows) {
            if (playFragment.dispatchKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    // takagen99 : Commented out to allow monitor Click Event
    //@Override
    //public boolean dispatchTouchEvent(MotionEvent ev) {
    //    if (showPreview && !fullWindows) {
    //        Rect editTextRect = new Rect();
    //        llPlayerFragmentContainerBlock.getHitRect(editTextRect);
    //        if (editTextRect.contains((int) ev.getX(), (int) ev.getY())) {
    //            return true;
    //        }
    //    }
    //    return super.dispatchTouchEvent(ev);
    //}


    public void toggleFullPreview() {
        if (windowsPreview == null) {
            windowsPreview = llPlayerFragmentContainer.getLayoutParams();
        }
        if (windowsFull == null) {
            windowsFull = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // Full Window flag
        fullWindows = !fullWindows;
        llPlayerFragmentContainer.setLayoutParams(fullWindows ? windowsFull : windowsPreview);
        llPlayerFragmentContainerBlock.setVisibility(fullWindows ? View.GONE : View.VISIBLE);
        mGridView.setVisibility(fullWindows ? View.GONE : View.VISIBLE);
        mGridViewFlag.setVisibility(fullWindows ? View.GONE : View.VISIBLE);

        // 全屏下禁用详情页几个按键的焦点 防止上键跑过来 : Disable buttons when full window
        tvPlay.setFocusable(!fullWindows);
        tvSort.setFocusable(!fullWindows);
        tvPush.setFocusable(!fullWindows);
        tvDesc.setFocusable(!fullWindows);
        tvCollect.setFocusable(!fullWindows);
        tvQuickSearch.setFocusable(!fullWindows);
        toggleSubtitleTextSize();

        // Hide navbar only when video playing on full window, else show navbar
        if (fullWindows) {
            hideSystemUI(false);
        } else {
            showSystemUI();
        }
    }

    void toggleSubtitleTextSize() {
        int subtitleTextSize = SubtitleHelper.getTextSize(this);
        if (!fullWindows) {
            subtitleTextSize *= 0.5;
        }
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE, subtitleTextSize));
    }
}
