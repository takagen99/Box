package com.github.tvbox.osc.util;

import android.app.ActivityManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DeviceCapability {
    private static final String TAG = "DeviceCapability";

    public static final int MEMORY_LOW = 0;
    public static final int MEMORY_MEDIUM = 1;
    public static final int MEMORY_HIGH = 2;

    private static volatile DeviceCapability sInstance;
    private final boolean mIsTV;
    private final int mMemoryClass;
    private final boolean mHasHevcHwDecoder;
    private final boolean mSupportsTunneledPlayback;
    private final boolean mIsMtkTv;

    private DeviceCapability(Context context) {
        Context appCtx = context.getApplicationContext();
        mIsTV = detectTV(appCtx);
        mMemoryClass = detectMemoryClass(appCtx);
        mHasHevcHwDecoder = hasHardwareDecoder("video/hevc");
        mSupportsTunneledPlayback = detectTunneledPlayback("video/hevc");
        mIsMtkTv = detectMtkTv();
    }

    public static DeviceCapability get(Context context) {
        if (sInstance == null) {
            synchronized (DeviceCapability.class) {
                if (sInstance == null) {
                    sInstance = new DeviceCapability(context);
                }
            }
        }
        return sInstance;
    }

    private static boolean detectTV(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }
        if (context.getPackageManager().hasSystemFeature("android.software.leanback")) {
            return true;
        }
        String characteristics = Build.UNKNOWN;
        try {
            characteristics = (String) Build.class.getField("CHARACTERISTICS").get(null);
        } catch (Exception ignored) {}
        return characteristics != null && characteristics.contains("tv");
    }

    public static boolean hasHardwareDecoder(String mimeType) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
            if (codecInfo.isEncoder()) continue;
            if (!isHardwareCodec(codecInfo)) continue;
            for (String type : codecInfo.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isHardwareCodec(MediaCodecInfo codecInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return codecInfo.isHardwareAccelerated();
        }
        String name = codecInfo.getName().toLowerCase();
        return !name.startsWith("omx.google.") && !name.startsWith("c2.android.")
                && !name.contains("sw") && !name.contains("ffmpeg");
    }

    private static boolean detectTunneledPlayback(String mimeType) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
            if (codecInfo.isEncoder()) continue;
            if (!isHardwareCodec(codecInfo)) continue;
            for (String type : codecInfo.getSupportedTypes()) {
                if (!type.equalsIgnoreCase(mimeType)) continue;
                try {
                    MediaCodecInfo.CodecCapabilities caps = codecInfo.getCapabilitiesForType(type);
                    if (caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback)) {
                        Log.d(TAG, "Tunneled playback supported by: " + codecInfo.getName());
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private static boolean detectMtkTv() {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
            if (codecInfo.isEncoder()) continue;
            if (codecInfo.getName().startsWith("c2.mtk.")) {
                return true;
            }
        }
        return false;
    }

    private static int detectMemoryClass(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return MEMORY_MEDIUM;
        int memoryClass = am.getMemoryClass();
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);
        long totalMB = memInfo.totalMem / (1024 * 1024);
        if (memoryClass <= 128 || totalMB <= 1536) {
            return MEMORY_LOW;
        } else if (memoryClass <= 256 || totalMB <= 3072) {
            return MEMORY_MEDIUM;
        }
        return MEMORY_HIGH;
    }

    public boolean isTV() { return mIsTV; }
    public int getMemoryClass() { return mMemoryClass; }
    public boolean hasHevcHwDecoder() { return mHasHevcHwDecoder; }
    public boolean supportsTunneledPlayback() { return mSupportsTunneledPlayback; }
    public boolean isMtkTv() { return mIsMtkTv; }

    public boolean shouldUseSurfaceView() {
        return mIsTV && mSupportsTunneledPlayback;
    }

    public long getRecommendedCacheSize() {
        switch (mMemoryClass) {
            case MEMORY_LOW:    return 64 * 1024 * 1024L;
            case MEMORY_MEDIUM: return 128 * 1024 * 1024L;
            default:            return 512 * 1024 * 1024L;
        }
    }

    public static List<String> listHardwareDecoders() {
        List<String> result = new ArrayList<>();
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
            if (codecInfo.isEncoder()) continue;
            if (!isHardwareCodec(codecInfo)) continue;
            StringBuilder sb = new StringBuilder(codecInfo.getName()).append(" [");
            String[] types = codecInfo.getSupportedTypes();
            for (int i = 0; i < types.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(types[i]);
            }
            sb.append("]");
            result.add(sb.toString());
        }
        return result;
    }
}
