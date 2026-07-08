package xyz.doikki.videoplayer.render;

import static androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL;
import static androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;
import static androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;

import com.github.tvbox.osc.player.EXOmPlayer;
import com.lzy.okgo.utils.OkLogger;

import xyz.doikki.videoplayer.player.AbstractPlayer;


@SuppressLint("ViewConstructor")
public class PlayerViewRenderView extends PlayerView implements IRenderView {

    // private final MeasureHelper mMeasureHelper;

    public PlayerViewRenderView(Context context) {
        this(context, null);
    }

    public PlayerViewRenderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public @androidx.annotation.OptIn(markerClass = UnstableApi.class)
    PlayerViewRenderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // mMeasureHelper = new MeasureHelper();
        setUseController(false);
        setKeepContentOnPlayerReset(true);
        SubtitleView subtitleView = getSubtitleView();
        if (subtitleView != null) {//隐藏PlayerView自带的字幕
            subtitleView.setAlpha(0);
        }

    }

    @UnstableApi
    @Override
    public void attachToPlayer(AbstractPlayer player) {
        if (player instanceof EXOmPlayer) {
            ((EXOmPlayer) player).setPlayerView(this);
        }
    }

    @Override
    public void setVideoSize(int videoWidth, int videoHeight) {
    }

    @Override
    public void setVideoRotation(int degree) {
    }

    @UnstableApi
    @Override
    public void setScaleType(int scaleType) {
        try {
            switch (scaleType) {
                case 0:
                default:
                    setResizeMode(RESIZE_MODE_FIT);
                    break;
                case 3:
                    setResizeMode(RESIZE_MODE_ZOOM);
                    break;
                case 4:
                    setResizeMode(RESIZE_MODE_FILL);
                    break;
                case 1: {
                    View surfaceView = getVideoSurfaceView();
                    if (surfaceView != null) {
                        ViewParent parent = surfaceView.getParent();
                        if (parent instanceof AspectRatioFrameLayout) {
                            onContentAspectRatioChanged((AspectRatioFrameLayout) parent, (float) (16.0 / 9.0));
                            setResizeMode(RESIZE_MODE_FIT);
                        }
                    }
                    break;
                }

                case 2: {
                    View surfaceView = getVideoSurfaceView();
                    if (surfaceView != null) {
                        ViewParent parent = surfaceView.getParent();
                        if (parent instanceof AspectRatioFrameLayout) {
                            onContentAspectRatioChanged((AspectRatioFrameLayout) parent, (float) (4.0 / 3.0));
                            setResizeMode(RESIZE_MODE_FIT);
                        }
                    }
                    break;
                }
                case 5:
                    View surfaceView = getVideoSurfaceView();
                    if (surfaceView != null) {
                        ViewParent parent = surfaceView.getParent();
                        if (parent instanceof AspectRatioFrameLayout) {
                            onContentAspectRatioChanged((AspectRatioFrameLayout) parent, (float) ((float) getWidth() / getHeight()));
                            setResizeMode(RESIZE_MODE_ZOOM);
                        }
                    }
                    break;
            }
        } catch (Throwable t) {
            OkLogger.d("Throwable >>> " + Log.getStackTraceString(t));
        }
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public Bitmap doScreenShot() {
        return null;
    }

    @Override
    public void release() {
        setPlayer(null);
    }
}