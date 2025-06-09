package com.github.tvbox.osc.player.controller;

import static xyz.doikki.videoplayer.util.PlayerUtils.stringForTimeVod;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.transition.TransitionManager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.player.thirdparty.Kodi;
import com.github.tvbox.osc.player.thirdparty.MXPlayer;
import com.github.tvbox.osc.player.thirdparty.ReexPlayer;
import com.github.tvbox.osc.subtitle.widget.SimpleSubtitleView;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.adapter.ParseAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.ScreenUtils;
import com.github.tvbox.osc.util.SubtitleHelper;
import com.github.tvbox.osc.util.VideoParseRuler;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.util.PlayerUtils;

public class VodController extends BaseController {
    public VodController(@NonNull @NotNull Context context) {
        super(context);
        mHandlerCallback = new HandlerCallback() {
            @Override
            public void callback(Message msg) {
                switch (msg.what) {
                    case 1000: { // seek 刷新
                        mProgressRoot.setVisibility(VISIBLE);
                        if (isPaused) {
                            mProgressTop.setVisibility(GONE);
                        }
                        break;
                    }
                    case 1001: { // seek 关闭
                        mProgressRoot.setVisibility(GONE);
                        if (isPaused) {
                            mProgressTop.setVisibility(VISIBLE);
                        }
                        break;
                    }
                    case 1002: { // 显示底部菜单
//                        mTopHide.setVisibility(GONE);
//                        mTopRoot.setVisibility(VISIBLE);
//                        TranslateAnimation animateT = new TranslateAnimation(
//                                0,                // fromXDelta
//                                0,                  // toXDelta
//                                -mTopRoot.getHeight(),       // fromYDelta
//                                0);                 // toYDelta
//                        animateT.setDuration(400);
//                        animateT.setFillAfter(true);
//                        mTopRoot.startAnimation(animateT);
//
//                        mBottomRoot.setVisibility(VISIBLE);
//                        TranslateAnimation animateB = new TranslateAnimation(
//                                0,                // fromXDelta
//                                0,                  // toXDelta
//                                mBottomRoot.getHeight(),    // fromYDelta
//                                0);                 // toYDelta
//                        animateB.setDuration(400);
//                        animateB.setFillAfter(true);
//                        mBottomRoot.startAnimation(animateB);
//                        mBottomRoot.requestFocus();

                        // takagen99 : Revamp Show & Hide Logic with alpha
//                        mTopHide.setVisibility(GONE);
                        mTopRoot.setVisibility(VISIBLE);
                        mTopRoot.setAlpha(0.0f);
                        mTopRoot.setTranslationY(-mTopRoot.getHeight() / 2);
                        mTopRoot.animate()
                                .translationY(0)
                                .alpha(1.0f)
                                .setDuration(250)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(null);

                        mBottomRoot.setVisibility(VISIBLE);
                        mBottomRoot.setAlpha(0.0f);
                        mBottomRoot.setTranslationY(mBottomRoot.getHeight() / 2);
                        mBottomRoot.animate()
                                .translationY(0)
                                .alpha(1.0f)
                                .setDuration(250)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(null);
                        mBottomRoot.requestFocus();
                        mHandler.postDelayed(mUpdateLayout, 255);   // Workaround Fix : SurfaceView

                        // takagen99: Check if Touch Screen, show back button
                        if (((BaseActivity) mActivity).supportsTouch()) {
                            mBack.setVisibility(VISIBLE);
                        }
                        updateDanmuBtn();
                        showLockView();

                        if (isKeyUp) {
                            mPlayerTimeStartBtn.requestFocus();
                            isKeyUp = false;
                        } else {
                            mPauseBtn.requestFocus();
                        }
                        break;
                    }
                    case 1003: { // 隐藏底部菜单
//                        TranslateAnimation animateT = new TranslateAnimation(
//                                0,                 // fromXDelta
//                                0,                   // toXDelta
//                                0,                 // fromYDelta
//                                -mTopRoot.getHeight());
//                        animateT.setDuration(400);
//                        animateT.setFillAfter(true);
//                        mTopRoot.startAnimation(animateT);
//                        mTopRoot.setVisibility(GONE);
//
//                        TranslateAnimation animateB = new TranslateAnimation(
//                                0,                 // fromXDelta
//                                0,                   // toXDelta
//                                0,                 // fromYDelta
//                                //mBottomRoot.getHeight());  // toYDelta
//                                // takagen99: Quick fix VOD controller shows after PIP
//                                mBottomRoot.getHeight());
//                        animateB.setDuration(400);
//                        animateB.setFillAfter(true);
//                        mBottomRoot.startAnimation(animateB);
//                        mBottomRoot.setVisibility(GONE);
//
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                mBottomRoot.clearAnimation();
//                            }
//                        }, 450);

                        // takagen99 : Revamp Show & Hide Logic with alpha
                        mTopRoot.animate()
                                .translationY(-mTopRoot.getHeight() / 2)
                                .alpha(0.0f)
                                .setDuration(250)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        mTopRoot.setVisibility(View.GONE);
                                        mTopRoot.clearAnimation();
                                    }
                                });

                        mBottomRoot.animate()
                                .translationY(mBottomRoot.getHeight() / 2)
                                .alpha(0.0f)
                                .setDuration(250)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        mBottomRoot.setVisibility(View.GONE);
                                        mBottomRoot.clearAnimation();
                                    }
                                });
                        mDanmuSetting.setVisibility(GONE);
                        mBack.setVisibility(GONE);
                        mLockView.setVisibility(GONE);
                        break;
                    }
                    case 1004: { // 设置速度
                        if (isInPlaybackState()) {
                            try {
                                float speed = (float) mPlayerConfig.getDouble("sp");
                                mControlWrapper.setSpeed(speed);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else
                            mHandler.sendEmptyMessageDelayed(1004, 100);
                        break;
                    }
                }
            }
        };
    }

    // top container
    LinearLayout mTopHide;
    LinearLayout mTopRoot;
    TextView mPlayTitle;
    TextView mPlayerResolution;
    LinearLayout mSpeedHidell;
    LinearLayout mSpeedll;

    // pause container
    public FrameLayout mProgressTop;
    ImageView mPauseIcon;
    LinearLayout mTapSeek;

    // progress container
    LinearLayout mProgressRoot;
    ImageView mProgressIcon;
    TextView mProgressText;
    ProgressBar mDialogVideoProgressBar;
    ProgressBar mDialogVideoPauseBar;

    // center BACK button
    LinearLayout mBack;

    LinearLayout mDanmuSetting;

    private boolean hasDanmu = false;

    //center LOCK button    
    private boolean isLock = false;
    ImageView mLockView;
    LockRunnable lockRunnable = new LockRunnable();

    // screen_display
    TextView mPlayPauseTime;
    TextView mPlayLoadNetSpeedRightTop;
    LinearLayout mTopRoot2;
    TextView seekTime; //右上角进度时间显示
    LinearLayout mScreendisplay; //增加屏显开关

    // bottom container
    LinearLayout mBottomRoot;
    TextView mTime;
    TextView mTimeEnd;
    TextView mCurrentTime;
    SeekBar mSeekBar;
    TextView mTotalTime;
    boolean mIsDragging;

    // 1. media control
    LinearLayout mPreBtn;
    LinearLayout mPauseBtn;
    ImageView mPauseImg;
    LinearLayout mNextBtn;
    float mSpeed;
    LinearLayout mPlayerRetry;

    // Fast Forward Buttons
    LinearLayout mFFwdBtn;
    ImageView mFFwdImg;
    TextView mFFwdTxt;

    // Scale Buttons
    LinearLayout mPlayerScaleBtn;
    ImageView mPlayerScaleImg;
    TextView mPlayerScaleTxt;

    // Player Buttons
    LinearLayout mPlayerBtn;
    ImageView mPlayerImg;
    TextView mPlayerTxt;
    TextView mPlayerIJKBtn;
    LinearLayout mSubtitleBtn;

    public SimpleSubtitleView mSubtitleView;
    LinearLayout mAudioTrackBtn;
    TextView mPlayerTimeStartBtn;
    TextView mPlayerTimeSkipBtn;
    TextView mPlayerTimeStepBtn;
    public TextView mPlayerTimeResetBtn;
    public ImageView mLvPortraitBtn;
    public LinearLayout mLandscapePortraitBtn;

    // parse container
    LinearLayout mParseRoot;
    TvRecyclerView mGridView;

    // takagen99 : To get system time
    private final Runnable mTimeRunnable = new Runnable() {
        @Override
        public void run() {
            Date date = new Date();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
            mPlayPauseTime.setText(timeFormat.format(date));
            mTime.setText(timeFormat.format(date));
            mHandler.postDelayed(this, 1000);
        }
    };

    private final Runnable mUpdateLayout = new Runnable() {
        @Override
        public void run() {
            mBottomRoot.requestLayout();
        }
    };

    private void showLockView() {
        mLockView.setVisibility(ScreenUtils.isTv(getContext()) ? INVISIBLE : VISIBLE);
        mHandler.removeCallbacks(lockRunnable);
        mHandler.postDelayed(lockRunnable, 3000);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mTimeRunnable);
    }

    @Override
    protected void initView() {
        super.initView();

        // top container
        mTopHide = findViewById(R.id.top_container_hide);
        mTopRoot = findViewById(R.id.top_container);
        mPlayTitle = findViewById(R.id.tv_title_top);
        mPlayerResolution = findViewById(R.id.tv_resolution);
        mSpeedHidell = findViewById(R.id.tv_speed_top_hide);
        mSpeedll = findViewById(R.id.tv_speed_top);

        // pause container
        mProgressTop = findViewById(R.id.tv_pause_container);
        mPauseIcon = findViewById(R.id.tv_pause_icon);
        mTapSeek = findViewById(R.id.ll_ddtap);

        // progress container
        mProgressRoot = findViewById(R.id.tv_progress_container);
        mProgressIcon = findViewById(R.id.tv_progress_icon);
        mProgressText = findViewById(R.id.tv_progress_text);
        mDialogVideoProgressBar = findViewWithTag("progressbar_video");
        mDialogVideoPauseBar = findViewWithTag("pausebar_video");

        // center back button
        mBack = findViewById(R.id.tvBackButton);
        mDanmuSetting = findViewById(R.id.ll_danmu_setting);

        // center lock button
        mLockView = findViewById(R.id.tv_lock);

        // bottom container
        mBottomRoot = findViewById(R.id.bottom_container);
        mTime = findViewById(R.id.tv_sys_time);
        mTimeEnd = findViewById(R.id.tv_time_end);
        mCurrentTime = findViewById(R.id.curr_time);
        mSeekBar = findViewById(R.id.seekBar);
        mTotalTime = findViewById(R.id.total_time);

        // 1. Media Control Buttons
        mPreBtn = findViewById(R.id.play_prev);
        mPauseBtn = findViewById(R.id.play_pause);
        mPauseImg = findViewById(R.id.play_pauseImg);
        mNextBtn = findViewById(R.id.play_next);
        mPlayerRetry = findViewById(R.id.play_retry);

        // Fast Forward Buttons
        mFFwdBtn = findViewById(R.id.play_speed);
        mFFwdImg = findViewById(R.id.play_speed_img);
        mFFwdTxt = findViewById(R.id.play_speed_txt);

        // Scale Buttons
        mPlayerScaleBtn = findViewById(R.id.play_scale);
        mPlayerScaleImg = findViewById(R.id.play_scale_img);
        mPlayerScaleTxt = findViewById(R.id.play_scale_txt);

        // Player Buttons
        mPlayerBtn = findViewById(R.id.play_player);
        mPlayerImg = findViewById(R.id.play_player_img);
        mPlayerTxt = findViewById(R.id.play_player_txt);
        mPlayerIJKBtn = findViewById(R.id.play_ijk);

        mSubtitleBtn = findViewById(R.id.play_subtitle);
        mSubtitleView = findViewById(R.id.subtitle_view);
        mAudioTrackBtn = findViewById(R.id.play_audio);
        mPlayerTimeStartBtn = findViewById(R.id.play_time_start);
        mPlayerTimeSkipBtn = findViewById(R.id.play_time_end);
        mPlayerTimeStepBtn = findViewById(R.id.play_time_step);
        mPlayerTimeResetBtn = findViewById(R.id.play_time_reset);
        mLandscapePortraitBtn = findViewById(R.id.landscape_portrait);
        mLvPortraitBtn = findViewById(R.id.lv_portrait);

        // parse container
        mParseRoot = findViewById(R.id.parse_root);
        mGridView = findViewById(R.id.mGridView);

        // initialize view
        mTopRoot.setVisibility(INVISIBLE);
        mBottomRoot.setVisibility(INVISIBLE);
        mBack.setVisibility(INVISIBLE);
        mDanmuSetting.setVisibility(INVISIBLE);

        // initialize subtitle
        initSubtitleInfo();

        // screen_display
        mPlayPauseTime = findViewById(R.id.tv_system_time);
        mPlayLoadNetSpeedRightTop = findViewById(R.id.tv_play_load_net_speed_right_top);
        mTopRoot2 = findViewById(R.id.tv_top_r_container);
        seekTime = findViewById(R.id.tv_seek_time); //右上角进度时间显示
        mScreendisplay = findViewById(R.id.screen_display); //增加屏显开关

        mLockView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                isLock = !isLock;
                mLockView.setImageResource(isLock ? R.drawable.icon_lock : R.drawable.icon_unlock);
                if (isLock) {
                    Message obtain = Message.obtain();
                    obtain.what = 1003; //隐藏底部菜单
                    mHandler.sendMessage(obtain);
                }
                showLockView();
            }
        });

        View rootView = findViewById(R.id.rootView);
        rootView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isLock) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (mLockView.getVisibility() == View.VISIBLE) {
                            mLockView.setVisibility(GONE);
                        } else {
                            showLockView();
                        }
                    }
                }
                return isLock;
            }
        });

        mPlayPauseTime.post(new Runnable() {
            @Override
            public void run() {
                mHandler.post(mTimeRunnable);
            }
        });

        mGridView.setLayoutManager(new V7LinearLayoutManager(getContext(), 0, false));
        ParseAdapter parseAdapter = new ParseAdapter();
        parseAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                ParseBean parseBean = parseAdapter.getItem(position);
                // 当前默认解析需要刷新
                int currentDefault = parseAdapter.getData().indexOf(ApiConfig.get().getDefaultParse());
                parseAdapter.notifyItemChanged(currentDefault);
                ApiConfig.get().setDefaultParse(parseBean);
                parseAdapter.notifyItemChanged(position);
                listener.changeParse(parseBean);
                hideBottom();
            }
        });
        mGridView.setAdapter(parseAdapter);
        parseAdapter.setNewData(ApiConfig.get().getParseBeanList());

        mParseRoot.setVisibility(VISIBLE);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * progress) / seekBar.getMax();
                if (mCurrentTime != null)
                    mCurrentTime.setText(stringForTimeVod((int) newPosition));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragging = true;
                mControlWrapper.stopProgress();
                mControlWrapper.stopFadeOut();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * seekBar.getProgress()) / seekBar.getMax();
                mControlWrapper.seekTo((int) newPosition);
                mIsDragging = false;
                mControlWrapper.startProgress();
                mControlWrapper.startFadeOut();
            }
        });
        // Text : Share to other App -------------------------------------
        mPlayTitle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                listener.openVideo();
            }
        });
        // Button : Play PREV --------------------------------------------
        mPreBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.playPre();
                hideBottom();
            }
        });
        // Button : Play PAUSE --------------------------------------------
        mPauseBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlay();
                if (!isPaused) {
                    hideBottom();
                }
            }
        });
        // Button : Play NEXT --------------------------------------------
        mNextBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.playNext(false);
                hideBottom();
            }
        });
        // Button : SPEED of video --------------------------------------
        mFFwdBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mHideBottomRunnable);
                mHandler.postDelayed(mHideBottomRunnable, 8000);
                try {
                    float speed = (float) mPlayerConfig.getDouble("sp");
                    increasePlaySpeed(speed);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // takagen99: Add long press to reset speed
        mFFwdBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                float currentSpeed = mControlWrapper.getSpeed();
                if (currentSpeed == 1.0f) {
                    setPlaySpeed(5.0f);
                } else {
                    setPlaySpeed(1.0f);
                }
                return true;
            }
        });
//        mFFwdBtn.setOnFocusChangeListener(new OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean isFocus) {
//                if (isFocus) {
//                    mFFwdImg.setVisibility(GONE);
//                    mFFwdTxt.setVisibility(VISIBLE);
//                } else {
//                    mFFwdImg.setVisibility(VISIBLE);
//                    mFFwdTxt.setVisibility(GONE);
//                }
//            }
//        });
//        mFFwdBtn.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    mFFwdImg.setVisibility(GONE);
//                    mFFwdTxt.setVisibility(VISIBLE);
//                    return true;
//                } else if (event.getAction() == MotionEvent.ACTION_UP) {
//                    mFFwdImg.setVisibility(VISIBLE);
//                    mFFwdTxt.setVisibility(GONE);
//                    return true;
//                }
//                return false;
//            }
//        });
        // Button : REPLAY from start ------------------------------------
        mPlayerRetry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.replay(false);
                hideBottom();
            }
        });
        // takagen99: Add long press to refresh from same position (not from start)
        mPlayerRetry.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                listener.replay(true);
                hideBottom();
                return true;
            }
        });
        // Button : SCALE video size ------------------------------------
        mPlayerScaleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mHideBottomRunnable);
                mHandler.postDelayed(mHideBottomRunnable, 8000);
                try {
                    int scaleType = mPlayerConfig.getInt("sc");
                    scaleType++;
                    if (scaleType > 5)
                        scaleType = 0;
                    mPlayerConfig.put("sc", scaleType);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    mControlWrapper.setScreenScaleType(scaleType);
//                    Toast.makeText(getContext(), PlayerHelper.getScaleName(mPlayerConfig.getInt("sc")), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // takagen99 : Long Press to change orientation
        mPlayerScaleBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int checkOrientation = mActivity.getRequestedOrientation();
                if (checkOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                } else if (checkOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
                return true;
            }
        });
        // Button : CHANGE player type ------------------------------------
//        mPlayerBtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                try {
//                    int playerType = mPlayerConfig.getInt("pl");
//                    boolean playerVail = false;
//                    do {
//                        playerType++;
//                        if (playerType <= 2) {
//                            playerVail = true;
//                        } else if (playerType == 10) {
//                            playerVail = mxPlayerExist;
//                        } else if (playerType == 11) {
//                            playerVail = reexPlayerExist;
//                        } else if (playerType == 12) {
//                            playerVail = KodiExist;
//                        } else if (playerType > 12) {
//                            playerType = 0;
//                            playerVail = true;
//                        }
//                    } while (!playerVail);
//                    mPlayerConfig.put("pl", playerType);
//                    updatePlayerCfgView();
//                    listener.updatePlayerCfg();
//                    listener.replay(false);
//                    // hideBottom();
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                mPlayerBtn.requestFocus();
//            }
//        });
        mPlayerBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                try {
                    int defaultPos = mPlayerConfig.getInt("pl");
                    ArrayList<Integer> players = new ArrayList<>();
                    players.add(0);  // System
                    players.add(1);  // IJK
                    players.add(2);  // Exo
                    players.add(3);  // Ali
                    if (mxPlayerExist) {
                        players.add(10);
                    }
                    if (reexPlayerExist) {
                        players.add(11);
                    }
                    if (KodiExist) {
                        players.add(12);
                    }
                    SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                    dialog.setTip(HomeActivity.getRes().getString(R.string.dia_player));
                    dialog.setAdapter(null, new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                        @Override
                        public void click(Integer value, int pos) {
                            try {
                                dialog.cancel();
                                int thisPlayType = players.get(pos);
                                mPlayerConfig.put("pl", thisPlayType);
                                updatePlayerCfgView();
                                listener.updatePlayerCfg();
                                listener.replay(false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public String getDisplay(Integer val) {
                            return PlayerHelper.getPlayerName(val);
                        }
                    }, new DiffUtil.ItemCallback<Integer>() {
                        @Override
                        public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                            return oldItem.intValue() == newItem.intValue();
                        }

                        @Override
                        public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                            return oldItem.intValue() == newItem.intValue();
                        }
                    }, players, defaultPos);
                    dialog.show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // Button : IJK select software or hardware decoding --------------------
        mPlayerIJKBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String ijk = mPlayerConfig.getString("ijk");
                    List<IJKCode> codecs = ApiConfig.get().getIjkCodes();
                    for (int i = 0; i < codecs.size(); i++) {
                        if (ijk.equals(codecs.get(i).getName())) {
                            if (i >= codecs.size() - 1)
                                ijk = codecs.get(0).getName();
                            else {
                                ijk = codecs.get(i + 1).getName();
                            }
                            break;
                        }
                    }
                    mPlayerConfig.put("ijk", ijk);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    listener.replay(false);
                    // hideBottom();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mPlayerIJKBtn.requestFocus();
            }
        });
        // Button : Subtitle selection ----------------------------------------
        mSubtitleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                listener.selectSubtitle();
            }
        });
        mSubtitleBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mSubtitleView.getVisibility() == View.GONE) {
                    mSubtitleView.setVisibility(VISIBLE);
                    hideBottom();
                    Toast.makeText(getContext(), HomeActivity.getRes().getString(R.string.vod_sub_on), Toast.LENGTH_SHORT).show();
                } else {
                    mSubtitleView.setVisibility(View.GONE);
                    // mSubtitleView.destroy();
                    // mSubtitleView.clearSubtitleCache();
                    // mSubtitleView.isInternal = false;
                    hideBottom();
                    Toast.makeText(getContext(), HomeActivity.getRes().getString(R.string.vod_sub_off), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        // Button : AUDIO track selection --------------------------------------
        mAudioTrackBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                listener.selectAudioTrack();
            }
        });
        //        增加播放页面片头片尾时间重置
        mPlayerTimeResetBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(mHideBottomRunnable);
                mHandler.postDelayed(mHideBottomRunnable, 8000);
                try {
                    mPlayerConfig.put("et", 0);
                    mPlayerConfig.put("st", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerTimeResetBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerConfig.put("st", 110);
                    mPlayerConfig.put("et", 150);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getContext(), "已预设片头片尾", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        // Button : SKIP time start -----------------------------------------
        mPlayerTimeStartBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mHideBottomRunnable);
                mHandler.postDelayed(mHideBottomRunnable, 8000);
                try {
//                    int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
//                    int st = mPlayerConfig.getInt("st");
//                    st += step;
//                    if (st > 60 * 10)
//                        st = 0;          600 = 10 mins

                    // takagen99: Reference FongMi to get exact opening skip time
                    int current = (int) mControlWrapper.getCurrentPosition();
                    int duration = (int) mControlWrapper.getDuration();
                    if (current > duration / 2) return;
                    mPlayerConfig.put("st", current / 1000);

                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // takagen99: Add long press to reset counter
        mPlayerTimeStartBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerConfig.put("st", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        // Button : SKIP time end -------------------------------------------
        mPlayerTimeSkipBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mHideBottomRunnable);
                mHandler.postDelayed(mHideBottomRunnable, 8000);
                try {
//                    int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
//                    int et = mPlayerConfig.getInt("et");
//                    et += step;
//                    if (et > 60 * 10)
//                        et = 0;

                    // takagen99: Reference FongMi to get exact ending skip time
                    int current = (int) mControlWrapper.getCurrentPosition();
                    int duration = (int) mControlWrapper.getDuration();
                    if (current < duration / 2) return;
                    mPlayerConfig.put("et", (duration - current) / 1000);

                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // takagen99: Add long press to reset counter
        mPlayerTimeSkipBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerConfig.put("et", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        // Button : SKIP time step -----------------------------------------
        mPlayerTimeStepBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
                step += 5;
                if (step > 30) {
                    step = 5;
                }
                Hawk.put(HawkConfig.PLAY_TIME_STEP, step);
                updatePlayerCfgView();
            }
        });
        // takagen99: Add long press to reset counter
        mPlayerTimeStepBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Hawk.put(HawkConfig.PLAY_TIME_STEP, 5);
                updatePlayerCfgView();
                return true;
            }
        });
        mLandscapePortraitBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                setLandscapePortrait();
                hideBottom();
            }
        });
        // Button: BACK click to go back to previous page -------------------
        mBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean showPreview = Hawk.get(HawkConfig.SHOW_PREVIEW, true);
                if (showPreview) {
                    mTopRoot.setVisibility(GONE);
                    mBottomRoot.setVisibility(GONE);
                    mBack.setVisibility(GONE);
                    mLockView.setVisibility(GONE);
                    mProgressTop.setVisibility(GONE);
                    mDanmuSetting.setVisibility(GONE);
                    mHandler.removeCallbacks(mHideBottomRunnable);
                    if (mActivity != null) {
                        if (mActivity.getClass().getSimpleName().equals("DetailActivity")) {
                            ((DetailActivity) mActivity).toggleFullPreview();
                        } else {
                            mActivity.finish();
                        }
                    }
                } else {
                    if (mActivity != null) {
                        mActivity.finish();
                    }
                }
            }
        });

        //屏显开关
        mTopRoot2.setVisibility(Hawk.get(HawkConfig.SCREEN_DISPLAY, GONE));
        mScreendisplay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mTopRoot2.setVisibility(mTopRoot2.getVisibility() == VISIBLE ? GONE : VISIBLE);
                Hawk.put(HawkConfig.SCREEN_DISPLAY, mTopRoot2.getVisibility());
                hideBottom();
                //Toast.makeText(getContext(), "点击显示网速 播放进度 时间", Toast.LENGTH_SHORT).show();
            }
        });

        mDanmuSetting.setOnClickListener(view -> {
            listener.showDanmuSetting();
        });

    }

    public void initLandscapePortraitBtnInfo() {
        if (mControlWrapper != null) {
            int width = mControlWrapper.getVideoSize()[0];
            int height = mControlWrapper.getVideoSize()[1];
            if (width < height) {
                mLandscapePortraitBtn.setVisibility(View.VISIBLE);
                mLvPortraitBtn.setImageResource(R.drawable.htov);
            }
        }
    }

    void setLandscapePortrait() {
        int requestedOrientation = mActivity.getRequestedOrientation();
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            mLvPortraitBtn.setImageResource(R.drawable.vtoh);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            mLvPortraitBtn.setImageResource(R.drawable.htov);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    void initSubtitleInfo() {
        int subtitleTextSize = SubtitleHelper.getTextSize(mActivity);
        mSubtitleView.setTextSize(subtitleTextSize);
        SubtitleHelper.upTextStyle(mSubtitleView);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.player_vod_control_view;
    }

    public void showParse(boolean userJxList) {
        mParseRoot.setVisibility(userJxList ? VISIBLE : GONE);
    }

    private JSONObject mPlayerConfig = null;

    private boolean mxPlayerExist = false;
    private boolean reexPlayerExist = false;
    private boolean KodiExist = false;

    public void setPlayerConfig(JSONObject playerCfg) {
        this.mPlayerConfig = playerCfg;
        updatePlayerCfgView();
        mxPlayerExist = MXPlayer.getPackageInfo() != null;
        reexPlayerExist = ReexPlayer.getPackageInfo() != null;
        KodiExist = Kodi.getPackageInfo() != null;
    }

    void updatePlayerCfgView() {
        try {
            int playerType = mPlayerConfig.getInt("pl");
            mPlayerTxt.setText(PlayerHelper.getPlayerName(playerType));
            mPlayerScaleTxt.setText(PlayerHelper.getScaleName(mPlayerConfig.getInt("sc")));
            mPlayerIJKBtn.setText(mPlayerConfig.getString("ijk"));
            mPlayerIJKBtn.setVisibility(playerType == 1 ? VISIBLE : GONE);
            mFFwdTxt.setText("x" + mPlayerConfig.getDouble("sp"));
            mPlayerTimeStartBtn.setText(PlayerUtils.stringForTime(mPlayerConfig.getInt("st") * 1000));
            mPlayerTimeSkipBtn.setText(PlayerUtils.stringForTime(mPlayerConfig.getInt("et") * 1000));
            mPlayerTimeStepBtn.setText(Hawk.get(HawkConfig.PLAY_TIME_STEP, 5) + "s");
//            mSubtitleBtn.setVisibility(playerType == 1 ? VISIBLE : GONE);
//            mAudioTrackBtn.setVisibility(playerType == 1 ? VISIBLE : GONE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setTitle(String playTitleInfo) {
        mPlayTitle.setText(playTitleInfo);
    }

    public void resetSpeed() {
        skipEnd = true;
        mHandler.removeMessages(1004);
        mHandler.sendEmptyMessageDelayed(1004, 100);
    }

    public interface VodControlListener {
        void playNext(boolean rmProgress);

        void playPre();

        void prepared();

        void changeParse(ParseBean pb);

        void updatePlayerCfg();

        void replay(boolean replay);

        void errReplay();

        void selectSubtitle();

        void selectAudioTrack();

        void openVideo();

        void showDanmuSetting();

    }

    public void setListener(VodControlListener listener) {
        this.listener = listener;
    }

    private VodControlListener listener;

    private boolean skipEnd = true;

    @SuppressLint("SetTextI18n")
    @Override
    protected void setProgress(int duration, int position) {
        if (mIsDragging) {
            return;
        }
        super.setProgress(duration, position);
        if (skipEnd && position != 0 && duration != 0) {
            int et = 0;
            try {
                et = mPlayerConfig.getInt("et");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (et > 0 && position + (et * 1000) >= duration) {
                skipEnd = false;
                listener.playNext(true);
            }
        }
        // takagen99 : Calculate finish time
        long TimeRemaining = mControlWrapper.getDuration() - mControlWrapper.getCurrentPosition();
        Calendar date = Calendar.getInstance();
        long t = date.getTimeInMillis();
        Date afterAdd = new Date(t + TimeRemaining);
        SimpleDateFormat timeEnd = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        if (isPaused) {
            mTimeEnd.setText(getContext().getString(R.string.vod_remaining_time) + " " + PlayerUtils.stringForTime((int) TimeRemaining) + " | " + getContext().getString(R.string.vod_ends_at) + " " + timeEnd.format(afterAdd));
        } else {
            mTimeEnd.setText(getContext().getString(R.string.vod_ends_at) + " " + timeEnd.format(afterAdd));
        }
        mCurrentTime.setText(PlayerUtils.stringForTimeVod(position));
        mTotalTime.setText(PlayerUtils.stringForTimeVod(duration));
        seekTime.setText((PlayerUtils.stringForTime(position)) + " | " + (PlayerUtils.stringForTime(duration))); //右上角进度条时间显示
        if (duration > 0) {
            mSeekBar.setEnabled(true);
            int pos = (int) (position * 1.0 / duration * mSeekBar.getMax());
            mSeekBar.setProgress(pos);
        } else {
            mSeekBar.setEnabled(false);
        }
        int percent = mControlWrapper.getBufferedPercentage();
        if (percent >= 95) {
            mSeekBar.setSecondaryProgress(mSeekBar.getMax());
        } else {
            mSeekBar.setSecondaryProgress(percent * 10);
        }
    }

    private boolean simSlideStart = false;
    private int simSeekPosition = 0;
    private long simSlideOffset = 0;
    private int tapDirection;
    private long lastSlideTime = 0;

    public void tvSlideStop() {
        if (!simSlideStart)
            return;
        mControlWrapper.seekTo(simSeekPosition);
        if (!mControlWrapper.isPlaying())
            mControlWrapper.start();
        simSlideStart = false;
        simSeekPosition = 0;
        simSlideOffset = 0;
    }

    public void tvSlideStart(int dir) {
        int duration = (int) mControlWrapper.getDuration();
        if (duration <= 0)
            return;

        long currentTime = System.currentTimeMillis();
        final int baseSkip = 10000; // 基础跳转10秒
        final float accelerationFactor = 1.5f; // 连续操作时的加速因子
        final long threshold = 500; // 操作间隔阈值500ms

        if (!simSlideStart) {
            simSlideStart = true;
            simSlideOffset = (long) baseSkip * dir;
        } else {
            if (currentTime - lastSlideTime <= threshold) {
                simSlideOffset += (baseSkip * accelerationFactor * dir);
            } else {
                simSlideOffset = (long) baseSkip * dir;
            }
        }
        lastSlideTime = currentTime;
        int currentPosition = (int) mControlWrapper.getCurrentPosition();
        int position = (int) (currentPosition + simSlideOffset);
        if (position > duration) position = duration;
        if (position < 0) position = 0;
        updateSeekUI(currentPosition, position, duration);
        simSeekPosition = position;
    }

    @Override
    protected void updateSeekUI(int curr, int seekTo, int duration) {
        super.updateSeekUI(curr, seekTo, duration);
        if (seekTo > curr) {
            mProgressIcon.setImageResource(R.drawable.play_ffwd);
        } else {
            mProgressIcon.setImageResource(R.drawable.play_rewind);
        }
        mProgressText.setText(PlayerUtils.stringForTime(seekTo) + " / " + PlayerUtils.stringForTime(duration));

        // takagen99: Update Minibar
        int percent = (int) (((double) seekTo / (double) duration) * 100);
        mDialogVideoPauseBar.setProgress(percent);
        mDialogVideoProgressBar.setProgress(percent);

        mHandler.sendEmptyMessage(1000);
        mHandler.removeMessages(1001);
        mHandler.sendEmptyMessageDelayed(1001, 1000);
    }

    // takagen99: (Optional) Hide Bottom Control if trigger Brightness / Volume Slider
//    @Override
//    protected void slideToChangeBrightness(float deltaY) {
//        if (isBottomVisible()) {
//            hideBottom();
//        }
//        super.slideToChangeBrightness(deltaY);
//    }
//    @Override
//    protected void slideToChangeVolume(float deltaY) {
//        if (isBottomVisible()) {
//            hideBottom();
//        }
//        super.slideToChangeVolume(deltaY);
//    }

    @Override
    protected void onPlayStateChanged(int playState) {
        super.onPlayStateChanged(playState);
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_REFRESH_NOTIFY, null));
        switch (playState) {
            case VideoView.STATE_IDLE:
                break;
            case VideoView.STATE_PLAYING:
                isPaused = false;
                mPauseImg.setImageDrawable(getResources().getDrawable(R.drawable.v_pause));
                startProgress();
                break;
            case VideoView.STATE_PAUSED:
                isPaused = true;
                mPauseImg.setImageDrawable(getResources().getDrawable(R.drawable.v_play));
                break;
            case VideoView.STATE_ERROR:
                listener.errReplay();
                break;
            case VideoView.STATE_PREPARED:
                listener.prepared();
                // takagen99 : Add Video Resolution
                if (mControlWrapper.getVideoSize().length >= 2) {
                    mPlayerResolution.setText(mControlWrapper.getVideoSize()[0] + " x " + mControlWrapper.getVideoSize()[1]);
                    initLandscapePortraitBtnInfo();
                }
            case VideoView.STATE_BUFFERED:
                break;
            case VideoView.STATE_PREPARING:
            case VideoView.STATE_BUFFERING:
                break;
            case VideoView.STATE_PLAYBACK_COMPLETED:
                listener.playNext(true);
                break;
        }
    }

    boolean isBottomVisible() {
        return mBottomRoot.getVisibility() == VISIBLE;
    }

    void showBottom() {
        mHandler.removeMessages(1003);
        mHandler.sendEmptyMessage(1002);
        mHandler.post(mTimeRunnable);
        mHandler.postDelayed(mHideBottomRunnable, 8000);
    }

    Runnable mHideBottomRunnable = new Runnable() {
        @Override
        public void run() {
            hideBottom();
        }
    };

    public void hideBottom() {
        mHandler.removeMessages(1002);
        mHandler.sendEmptyMessage(1003);
        mHandler.removeCallbacks(mHideBottomRunnable);
    }

    void increasePlaySpeed(float speed) {
        if (speed == 5) {
            speed = 0.25f;
        } else if (speed >= 2 & speed < 3) {
            speed += 0.5f;
        } else if (speed >= 3) {
            speed += 1.0f;
        } else {
            speed += 0.25f;
        }
        setPlaySpeed(speed);
    }

    void decreasePlaySpeed(float speed) {
        if (speed == 0.25f) {
            speed = 5.0f;
        } else if (speed > 3) {
            speed -= 1.0f;
        } else if (speed > 2 & speed <= 3) {
            speed -= 0.5f;
        } else {
            speed -= 0.25f;
        }
        setPlaySpeed(speed);
    }

    void setPlaySpeed(float value) {
        try {
            mPlayerConfig.put("sp", value);
            updatePlayerCfgView();
            listener.updatePlayerCfg();
            mControlWrapper.setSpeed(value);
        } catch (JSONException err) {
            err.printStackTrace();
        }
    }

    void increaseTime(String type) {
        try {
            int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
            int time = mPlayerConfig.getInt(type);
            time += step;
            if (time > 30 * 10)
                time = 0;          // 600 = 10 mins
            mPlayerConfig.put(type, time);

//            // takagen99: Reference FongMi to get exact opening skip time
//            int current = (int) mControlWrapper.getCurrentPosition();
//            int duration = (int) mControlWrapper.getDuration();
//            if (current > duration / 2) return;
//            mPlayerConfig.put("st", current / 1000);

            updatePlayerCfgView();
            listener.updatePlayerCfg();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void decreaseTime(String type) {
        try {
            int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
            int time = mPlayerConfig.getInt(type);
            time -= step;
            if (time < 0)
                time = (30 * 10);
            mPlayerConfig.put(type, time);

//            // takagen99: Reference FongMi to get exact ending skip time
//            int current = (int) mControlWrapper.getCurrentPosition();
//            int duration = (int) mControlWrapper.getDuration();
//            if (current < duration / 2) return;
//            mPlayerConfig.put("et", (duration - current) / 1000);

            updatePlayerCfgView();
            listener.updatePlayerCfg();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // takagen99 : Check Pause
    private boolean isPaused = false;
    private boolean isKeyUp = false;

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        boolean isInPlayback = isInPlaybackState();

        if (super.onKeyEvent(event)) {
            return true;
        }
        if (isBottomVisible()) {
            mHandler.removeCallbacks(mHideBottomRunnable);
            mHandler.postDelayed(mHideBottomRunnable, 8000);
            return super.dispatchKeyEvent(event);
        }
        if (action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (isInPlayback) {
                    tvSlideStart(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (isInPlayback) {
                    togglePlay();
                    if (!isBottomVisible() && isPaused) {
                        showBottom();
                    }
                    return true;
                }
                // takagen99 : Key Up to focus Start Time Skip
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (!isBottomVisible()) {
                    showBottom();
                    isKeyUp = true;
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!isBottomVisible()) {
                    showBottom();
                    return true;
                }
            }
        } else if (action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (isInPlayback) {
                    tvSlideStop();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (isBottomVisible() && mFFwdBtn.isFocused()) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    try {
                        float speed = (float) mPlayerConfig.getDouble("sp");
                        increasePlaySpeed(speed);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    try {
                        float speed = (float) mPlayerConfig.getDouble("sp");
                        decreasePlaySpeed(speed);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (isBottomVisible() && mPlayerTimeStartBtn.isFocused()) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    increaseTime("st");
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    decreaseTime("st");
                }
            }
        } else if (isBottomVisible() && mPlayerTimeSkipBtn.isFocused()) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    increaseTime("et");
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    decreaseTime("et");
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (!isBottomVisible()) {
            showBottom();
        } else {
            hideBottom();
        }
        return true;
    }

    // takagen99 : Add long press to fast forward x3 speed
    private boolean fromLongPress;
    private float currentSpeed;

    @Override
    public void onLongPress(MotionEvent e) {
        if (!isPaused) {
            fromLongPress = true;
            try {
                currentSpeed = (float) mPlayerConfig.getDouble("sp");
                circularReveal(mTapSeek, 1);
                // Set Fast Forward Icon
                mProgressTop.setVisibility(VISIBLE);
                mPauseIcon.setImageResource(R.drawable.play_ffwd);
                // Set x3 Speed only if less than x3
                if (currentSpeed < 3.0f) {
                    mSpeed = 3.0f;
                } else {
                    mSpeed = currentSpeed;
                }
                setPlaySpeed(mSpeed);
            } catch (JSONException f) {
                f.printStackTrace();
            }
        }
    }

    // takagen99 : On release long press, resume previous speed
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            if (fromLongPress) {
                // Set back to Pause Icon
                mProgressTop.setVisibility(GONE);
                mPauseIcon.setImageResource(R.drawable.play_pause);
                // Set back to current speed
                mSpeed = currentSpeed;
                setPlaySpeed(mSpeed);
                fromLongPress = false;
            }
        }
        return super.onTouchEvent(e);
    }

    // takagen99 : Added double tap to rewind or fast forward with animation
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        int threeScreen = PlayerUtils.getScreenWidth(getContext(), true) / 3;

        if (e.getX() > 0 && e.getX() < threeScreen) {
            // left side <<<<<
            tapDirection = -1;
        } else if ((e.getX() > threeScreen) && (e.getX() < (threeScreen * 2))) {
            // middle screen
            tapDirection = 0;
        } else if (e.getX() > (threeScreen * 2)) {
            // right side >>>>>
            tapDirection = 1;
        }
        if (tapDirection == 0 || isPaused) {
            togglePlay();
        } else {
            circularReveal(mTapSeek, tapDirection);
            int duration = (int) mControlWrapper.getDuration();
            int currentPosition = (int) mControlWrapper.getCurrentPosition();
            // Fast Forward or Backward by 10 seconds
            int position = (int) (10000.0f * tapDirection) + currentPosition;
            if (position > duration) position = duration;
            if (position < 0) position = 0;
            updateSeekUI(currentPosition, position, duration);
            mControlWrapper.seekTo(position);
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void circularReveal(View v, int direction) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int radius = Math.max(v.getWidth(), v.getHeight()) / 2;
            int width = 0;
            if (direction == 1) {
                width = v.getWidth();
            }
            TransitionManager.beginDelayedTransition((ViewGroup) v);
            Animator anim = ViewAnimationUtils.createCircularReveal(v, width, v.getHeight() / 2, 0, radius);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    v.setVisibility(VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    v.setVisibility(GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            anim.setDuration(600);
            anim.start();
        }
    }

    private class LockRunnable implements Runnable {
        @Override
        public void run() {
            mLockView.setVisibility(GONE);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (super.onBackPressed()) {
            return true;
        }
        if (isBottomVisible()) {
            hideBottom();
            return true;
        }
        int checkOrientation = mActivity.getRequestedOrientation();
        if (checkOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        return false;
    }

    public void updateDanmuBtn(){
        if(hasDanmu){
            mDanmuSetting.setVisibility(VISIBLE);
        }else{
            mDanmuSetting.setVisibility(GONE);
        }
    }

    public void setHasDanmu(boolean hasDanmu){
        this.hasDanmu = hasDanmu;
    }

    public void evaluateScript(SourceBean sourceBean,String url, WebView web_view, XWalkView xWalk_view){
        String clickSelector = sourceBean.getClickSelector().trim();
        clickSelector=clickSelector.isEmpty()?VideoParseRuler.getHostScript(url):clickSelector;
        if (!clickSelector.isEmpty()) {
            String selector;
            if (clickSelector.contains(";") && !clickSelector.endsWith(";")) {
                String[] parts = clickSelector.split(";", 2);
                if (!url.contains(parts[0])) {
                    return;
                }
                selector = parts[1].trim();
            } else {
                selector = clickSelector.trim();
            }
            // 构造点击的 JS 代码
            String js = selector;
//            if(!selector.contains("click()"))js+=".click();";
            if(web_view!=null){
                //4.4以上才支持这种写法
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    web_view.evaluateJavascript(js, null);
                } else {
                    web_view.loadUrl("javascript:" + js);
                }
            }
            if(xWalk_view!=null){
                //4.0+开始全部支持这种写法
                xWalk_view.evaluateJavascript(js, null);
            }
        }
    }	    
}
