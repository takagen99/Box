package com.github.tvbox.osc.util;

import android.content.Context;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.player.EXOmPlayer;
import com.github.tvbox.osc.player.IjkmPlayer;
import com.github.tvbox.osc.player.render.SurfaceRenderViewFactory;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import xyz.doikki.videoplayer.aliplayer.AliyunMediaPlayerFactory;
import xyz.doikki.videoplayer.player.AndroidMediaPlayerFactory;
import xyz.doikki.videoplayer.player.PlayerFactory;
import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.render.PlayerViewRenderViewFactory;
import xyz.doikki.videoplayer.render.RenderViewFactory;
import xyz.doikki.videoplayer.render.TextureRenderViewFactory;

public class PlayerHelper {
    public static void updateCfg(VideoView videoView, JSONObject playerCfg) {
        updateCfg(videoView, playerCfg, -1);
    }

    public static void updateCfg(VideoView videoView, JSONObject playerCfg, int forcePlayerType) {
        int playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0);
        int renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0);
        String ijkCode = Hawk.get(HawkConfig.IJK_CODEC, "软解码");
        int scale = Hawk.get(HawkConfig.PLAY_SCALE, 0);
        try {
            playerType = playerCfg.getInt("pl");
            //就我遇到的问题是 Exo 在 TextureView 黑屏 调整设置中的渲染模式无法生效
            //renderType = playerCfg.getInt("pr");//该值无法修改，一旦确认该值后续无法进行修改 就是在设置选的 类型无法应用
            ijkCode = playerCfg.getString("ijk");
            scale = playerCfg.getInt("sc");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        DeviceCapability capability = DeviceCapability.get(videoView.getContext());
        if (forcePlayerType >= 0) {
            playerType = forcePlayerType;
        } else if (capability.isTV() && capability.supportsTunneledPlayback() && capability.hasHevcHwDecoder()) {
            playerType = 2;
        }
        if (playerType == 2 && capability.shouldUseSurfaceView()) {
            renderType = 1;
        }
        IJKCode codec = ApiConfig.get().getIJKCodec(ijkCode);
        PlayerFactory playerFactory;
        if (playerType == 1) {
            playerFactory = new PlayerFactory<IjkmPlayer>() {
                @Override
                public IjkmPlayer createPlayer(Context context) {
                    return new IjkmPlayer(context, codec);
                }
            };
        } else if (playerType == 2) {
            playerFactory = new PlayerFactory<EXOmPlayer>() {
                @Override
                public EXOmPlayer createPlayer(Context context) {
                    return new EXOmPlayer(context);
                }
            };
        } else if (playerType == 3) {
            playerFactory = AliyunMediaPlayerFactory.create();
        } else {
            playerFactory = AndroidMediaPlayerFactory.create();
        }
        RenderViewFactory renderViewFactory = null;
        if (playerType==2){
            renderViewFactory = PlayerViewRenderViewFactory.create(renderType);
        }else{
            switch (renderType) {
                case 0:
                default:
                    renderViewFactory = TextureRenderViewFactory.create();
                    break;
                case 1:
                    renderViewFactory = SurfaceRenderViewFactory.create();
                    break;
            }
        }

        videoView.setPlayerFactory(playerFactory);
        videoView.setRenderViewFactory(renderViewFactory);
        videoView.setScreenScaleType(scale);
    }

    public static void updateCfg(VideoView videoView) {
        int playType = Hawk.get(HawkConfig.PLAY_TYPE, 0);
        DeviceCapability capability = DeviceCapability.get(videoView.getContext());
        if (capability.isTV() && capability.supportsTunneledPlayback() && capability.hasHevcHwDecoder()) {
            playType = 2;
        }
        PlayerFactory playerFactory;
        if (playType == 1) {
            playerFactory = new PlayerFactory<IjkmPlayer>() {
                @Override
                public IjkmPlayer createPlayer(Context context) {
                    return new IjkmPlayer(context, null);
                }
            };

        } else if (playType == 2) {
            playerFactory = new PlayerFactory<EXOmPlayer>() {
                @Override
                public EXOmPlayer createPlayer(Context context) {
                    return new EXOmPlayer(context);
                }
            };
        } else if (playType == 3) {
            playerFactory = AliyunMediaPlayerFactory.create();
        } else {
            playerFactory = AndroidMediaPlayerFactory.create();
        }
        int renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0);
        if (playType == 2 && capability.shouldUseSurfaceView()) {
            renderType = 1;
        }
        RenderViewFactory renderViewFactory = null;
        if (playType == 2) {
            renderViewFactory = PlayerViewRenderViewFactory.create(renderType);
        } else {
            switch (renderType) {
                case 0:
                default:
                    renderViewFactory = TextureRenderViewFactory.create();
                    break;
                case 1:
                    renderViewFactory = SurfaceRenderViewFactory.create();
                    break;
            }
        }
        videoView.setPlayerFactory(playerFactory);
        videoView.setRenderViewFactory(renderViewFactory);
    }

    public static void init() {
        IjkMediaPlayer.loadLibrariesOnce(null);
    }

    public static String getPlayerName(int playType) {
        if (playType == 1) {
            return "IJK";
        } else if (playType == 2) {
            return "Exo";
        } else if (playType == 3) {
            return "阿里";
        } else if (playType == 10) {
            return "MX";
        } else if (playType == 11) {
            return "Reex";
        } else if (playType == 12) {
            return "Kodi";
        } else {
            return "系统";
        }
    }

    public static String getRenderName(int renderType) {
        if (renderType == 1) {
            return "SurfaceView";
        } else {
            return "TextureView";
        }
    }

    public static String getScaleName(int screenScaleType) {
        String scaleText = "默认";
        switch (screenScaleType) {
            case VideoView.SCREEN_SCALE_DEFAULT:
                scaleText = "默认";
                break;
            case VideoView.SCREEN_SCALE_16_9:
                scaleText = "16:9";
                break;
            case VideoView.SCREEN_SCALE_4_3:
                scaleText = "4:3";
                break;
            case VideoView.SCREEN_SCALE_MATCH_PARENT:
                scaleText = "填充";
                break;
            case VideoView.SCREEN_SCALE_ORIGINAL:
                scaleText = "原始";
                break;
            case VideoView.SCREEN_SCALE_CENTER_CROP:
                scaleText = "裁剪";
                break;
        }
        return scaleText;
    }

    public static String getRootCauseMessage(Throwable th) {
        for (int i=0; i<10; i++) {
            if (th.getCause() == null) return th.getLocalizedMessage();
            else th = th.getCause();
        }
        return th.getLocalizedMessage();
    }

}
