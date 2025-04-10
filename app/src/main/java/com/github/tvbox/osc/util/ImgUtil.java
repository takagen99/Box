package com.github.tvbox.osc.util;

import static com.bumptech.glide.load.resource.bitmap.VideoDecoder.FRAME_OPTION;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.widget.ImageView;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

public class ImgUtil {
    public static int defaultWidth = 244;
    public static int defaultHeight = 320;

    /**
     * style 数据结构：ratio 指定宽高比（宽 / 高），type 表示风格（例如 rect、list）
     */
    public static class Style {
        public float ratio;
        public String type;

        public Style(float ratio, String type) {
            this.ratio = ratio;
            this.type = type;
        }
    }

    public static Style initStyle() {
        String bStyle = ApiConfig.get().getHomeSourceBean().getStyle();
        if(!bStyle.isEmpty()){
            try {
                JSONObject jsonObject = new JSONObject(bStyle);
                float ratio = (float) jsonObject.getDouble("ratio");
                String type = jsonObject.getString("type");
                return new Style(ratio, type);
            }catch (JSONException e){

            }
        }
        return null;
    }

    public static int spanCountByStyle(Style style,int defaultCount){
        int spanCount=defaultCount;
        if ("rect".equals(style.type)) {
            if (style.ratio >= 1.7) {
                spanCount = 3; // 横图
            } else if (style.ratio >= 1.3) {
                spanCount = 4; // 4:3
            }
        } else if ("list".equals(style.type)) {
            spanCount = 1;
        }
        return spanCount;
    }

    public static int getStyleDefaultWidth(Style style){
        int styleDefaultWidth = 280;
        if(style.ratio<1)styleDefaultWidth=214;
        if(style.ratio>1.7)styleDefaultWidth=380;
        return styleDefaultWidth;
    }
//    public static void load(String url, ImageView view) {
//        load(url, view, 10);
//    }
//
//    public static void load(String url, ImageView view, ImageView.ScaleType scaleType) {
//        load(url, view, 10, scaleType);
//    }
//
//    public static void load(String url, ImageView view, int roundingRadius, ImageView.ScaleType scaleType) {
//        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
//        if (TextUtils.isEmpty(url)) {
//            view.setImageResource(R.drawable.img_loading_placeholder);
//        } else {
//            if (roundingRadius == 0) roundingRadius = 1;
//            RequestOptions requestOptions = new RequestOptions()
//                .format(DecodeFormat.PREFER_RGB_565)
//                .diskCacheStrategy(getDiskCacheStrategy(4))
//                .dontAnimate()
//                .transform(
//            new RoundedCorners(roundingRadius));
//            Glide.with(App.getInstance())
//                .asBitmap()
//                .load(getUrl(url))
//                .error(R.drawable.img_loading_placeholder)
//                .placeholder(R.drawable.img_loading_placeholder)
//                .listener(getListener(view, scaleType))
//                .apply(requestOptions)
//                .into(view);
//        }
//    }
    public static void load(String url, ImageView view, int roundingRadius) {
        load(url, view, roundingRadius,0,0);
    }

    public static void load(String url, ImageView view, int roundingRadius, int newWidth, int newHeight) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (TextUtils.isEmpty(url)) {
            view.setImageResource(R.drawable.img_loading_placeholder);
        } else {
            if (roundingRadius == 0) roundingRadius = 1;
            RequestOptions requestOptions = new RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(getDiskCacheStrategy(4))
                .dontAnimate()
                .transform(new CenterCrop(), new RoundedCorners(roundingRadius));
            if (newWidth > 0 && newHeight > 0) {
                requestOptions = requestOptions.override(newWidth, newHeight);
            }
            Glide.with(App.getInstance())
                .asBitmap()
                .load(getUrl(url))
                .error(R.drawable.img_loading_placeholder)
                .placeholder(R.drawable.img_loading_placeholder)
                .listener(getListener(view, ImageView.ScaleType.FIT_XY))
                .apply(requestOptions)
                .into(view);
        }
    }

    /*
     * 使用Glide方式获取视频某一帧
     * @param uri 视频地址
     * @param imageView 设置image
     * @param frameTimeMicros 获取某一时间帧.
     */
    public static void loadVideoScreenshot(String uri, ImageView imageView, long frameTimeMicros) {
        RequestOptions requestOptions = RequestOptions.frameOf(frameTimeMicros * 1000)
            .set(FRAME_OPTION, MediaMetadataRetriever.OPTION_CLOSEST)
            .transform(
        new CenterCrop(),
        new RoundedCorners(10));
        Glide.with(App.getInstance())
            .load(uri)
            .skipMemoryCache(true)
            .apply(requestOptions)
            .into(imageView);
    }

    public static String getDiskCacheStrategyName(int index) {
        String[] names = new String[] {
            "[NONE] 关闭", "[RESOURCE] 转换图片", "[DATA] 原始图片 ", "[ALL] 原始图片和转换图片", "[AUTOMATIC] 自动"
        };
        return names[index];
    }

    public static DiskCacheStrategy getDiskCacheStrategy(int index) {
        switch (index) {
            case 1:
                return DiskCacheStrategy.RESOURCE;
            case 2:
                return DiskCacheStrategy.DATA;
            case 3:
                return DiskCacheStrategy.ALL;
            case 4:
                return DiskCacheStrategy.AUTOMATIC;
            default:
                return DiskCacheStrategy.NONE;
        }
    }

    private static Object getUrl(String url) {
        if (url.startsWith("data:")) return url;
        url = DefaultConfig.checkReplaceProxy(url);

        String header = null;
        String referer = null;
        String ua = UA.random();
        String cookie = null;

        if (url.contains("doubanio.com") && !url.contains("@Referer=") && !url.contains("@User-Agent=")) {
            url += "@Referer=https://api.douban.com/@User-Agent=" + ua;
        }

        //检查链接里面是否有自定义header
        if (url.contains("@Headers=")) {
            header = url.split("@Headers=")[1].split("@")[0];
            try {
                header = URLDecoder.decode(header, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (url.contains("@Cookie=")) cookie = url.split("@Cookie=")[1].split("@")[0];
        if (url.contains("@User-Agent=")) ua = url.split("@User-Agent=")[1].split("@")[0];
        if (url.contains("@Referer=")) referer = url.split("@Referer=")[1].split("@")[0];
        url = url.split("@")[0];
        if(TextUtils.isEmpty(url)) return null;
        /*   AuthInfo authInfo = new AuthInfo(url);
        url = authInfo.url; */

        LazyHeaders.Builder builder = new LazyHeaders.Builder();

        /*     if (!authInfo.auth.isEmpty()) {
            builder.addHeader(HttpHeaders.AUTHORIZATION, authInfo.auth);
        }*/

        if (!TextUtils.isEmpty(header)) {
            JsonObject jsonInfo = new Gson()
                .fromJson(header, JsonObject.class);
            for (String key: jsonInfo.keySet()) {
                String val = jsonInfo.get(key)
                    .getAsString();
                builder.addHeader(key, val);
            }
        } else {
            if (!TextUtils.isEmpty(cookie)) {
                builder.addHeader(HttpHeaders.COOKIE, cookie);
            }
            if (!TextUtils.isEmpty(ua)) {
                builder.addHeader(HttpHeaders.USER_AGENT, ua);
            } else {
                String mobile_UA = "Dalvik/2.1.0 (Linux; U; Android 13; M2102J2SC Build/TKQ1.220829.002)";
                builder.addHeader(HttpHeaders.USER_AGENT, mobile_UA);
            }
            if (!TextUtils.isEmpty(referer)) builder.addHeader(HttpHeaders.REFERER, referer);
        }

        try {
            URL imgUrl = new URL(url);
            String host = imgUrl.getHost();
            builder.addHeader(HttpHeaders.HOST, host);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return new GlideUrl(url, builder.build());
    }

    private static RequestListener < Bitmap > getListener(ImageView view, ImageView.ScaleType scaleType) {
        return new RequestListener < Bitmap > () {@Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target < Bitmap > target, boolean isFirstResource) {
                view.setScaleType(scaleType);
                view.setImageResource(R.drawable.img_loading_placeholder);
                return true;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target < Bitmap > target, DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(scaleType);
                return false;
            }
        };
    }
}
