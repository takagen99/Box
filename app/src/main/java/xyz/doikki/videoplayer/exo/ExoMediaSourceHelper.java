package xyz.doikki.videoplayer.exo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Util;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.datasource.rtmp.RtmpDataSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;
import androidx.media3.extractor.ts.TsExtractor;

import com.github.tvbox.osc.util.DeviceCapability;
import com.github.tvbox.osc.util.FileUtils;
import com.google.androidx.media3.exoplayer.ext.okhttp.OkHttpDataSource;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;

import okhttp3.OkHttpClient;

public final class ExoMediaSourceHelper {

    private static volatile ExoMediaSourceHelper sInstance;

    private final String mUserAgent;
    private final Context mAppContext;
    private OkHttpDataSource.Factory mHttpDataSourceFactory;
    private OkHttpDataSource.Factory mHttpDataSourceFactoryNoProxy;
    private OkHttpClient mOkClient = null;
    private Cache mCache;

    @SuppressLint("UnsafeOptInUsageError")
    private ExoMediaSourceHelper(Context context) {
        mAppContext = context.getApplicationContext();
        mUserAgent = Util.getUserAgent(mAppContext, mAppContext.getApplicationInfo().name);
    }

    public static ExoMediaSourceHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (ExoMediaSourceHelper.class) {
                if (sInstance == null) {
                    sInstance = new ExoMediaSourceHelper(context);
                }
            }
        }
        return sInstance;
    }

    private static MediaItem getMediaItem(String uri, int errorCode) {
        MediaItem.Builder builder = new MediaItem.Builder().setUri(Uri.parse(uri.trim().replace("\\", "")));
        if (errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)
            builder.setMimeType(MimeTypes.APPLICATION_M3U8);
        return builder.build();
    }

    @SuppressLint("UnsafeOptInUsageError")
    private static synchronized ExtractorsFactory getExtractorsFactory() {
        return new DefaultExtractorsFactory().setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS).setTsExtractorTimestampSearchBytes(TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES * 3);

    }

    public void setOkClient(OkHttpClient client) {
        mOkClient = client;
    }

    public MediaSource getMediaSource(String uri) {
        return getMediaSource(uri, null, false);
    }

    public MediaSource getMediaSource(String uri, Map<String, String> headers) {
        return getMediaSource(uri, headers, false);
    }

    public MediaSource getMediaSource(String uri, boolean isCache) {
        return getMediaSource(uri, null, isCache);
    }

    public MediaSource getMediaSource(String uri, Map<String, String> headers, boolean isCache) {
        return getMediaSource(uri, headers, isCache, -1);
    }

    @SuppressLint("UnsafeOptInUsageError")
    public MediaSource getMediaSource(String uri, Map<String, String> headers, boolean isCache, int errorCode) {
        Uri contentUri = Uri.parse(uri);
        if ("rtmp".equals(contentUri.getScheme())) {
            return new ProgressiveMediaSource.Factory(new RtmpDataSource.Factory())
                    .createMediaSource(MediaItem.fromUri(contentUri));
        } else if ("rtsp".equals(contentUri.getScheme())) {
            return new RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri(contentUri));
        }
        int contentType = inferContentType(uri);
        DataSource.Factory factory;
        if (isCache) {
            factory = getCacheDataSourceFactory();
        } else {
            factory = getDataSourceFactory();
        }
        if (mHttpDataSourceFactory != null) {
            setHeaders(headers);
        }
        if (errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED) {
            MediaItem.Builder builder = new MediaItem.Builder().setUri(uri);
            builder.setMimeType(MimeTypes.APPLICATION_M3U8);
            return new DefaultMediaSourceFactory(getDataSourceFactory(), getExtractorsFactory()).createMediaSource(getMediaItem(uri, errorCode));
        }
        switch (contentType) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(contentUri));
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mHttpDataSourceFactory)
                        .setAllowChunklessPreparation(true)
                        .setExtractorFactory(new MyHlsExtractorFactory())
                        .createMediaSource(MediaItem.fromUri(contentUri));
            //return new HlsMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(contentUri));
            default:
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(contentUri));
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private int inferContentType(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.contains(".mpd") || fileName.contains("type=mpd")) {
            return C.TYPE_DASH;
        } else if (fileName.contains("m3u8")) {
            return C.TYPE_HLS;
        } else {
            return C.TYPE_OTHER;
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private DataSource.Factory getCacheDataSourceFactory() {
        if (mCache == null) {
            mCache = newCache();
        }
        return new CacheDataSource.Factory()
                .setCache(mCache)
                .setUpstreamDataSourceFactory(getDataSourceFactory())
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    @SuppressLint("UnsafeOptInUsageError")
    private Cache newCache() {
        return new SimpleCache(
                new File(FileUtils.getExternalCachePath(), "exo-video-cache"),//缓存目录
                new LeastRecentlyUsedCacheEvictor(DeviceCapability.get(mAppContext).getRecommendedCacheSize()),
                new StandaloneDatabaseProvider(mAppContext));
    }

    /**
     * Returns a new DataSource factory.
     *
     * @return A new DataSource factory.
     */
    private DataSource.Factory getDataSourceFactory() {
        return new DefaultDataSource.Factory(mAppContext, getHttpDataSourceFactory());
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @return A new HttpDataSource factory.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private DataSource.Factory getHttpDataSourceFactory() {
        if (mHttpDataSourceFactory == null) {
            mHttpDataSourceFactory = new OkHttpDataSource.Factory(mOkClient)
                    .setUserAgent(mUserAgent)/*
                    .setAllowCrossProtocolRedirects(true)*/;
            mHttpDataSourceFactoryNoProxy = mHttpDataSourceFactory;
        }
        return mHttpDataSourceFactory;
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void setHeaders(Map<String, String> headers) {
        if (headers != null && headers.size() > 0) {
            //如果发现用户通过header传递了UA，则强行将HttpDataSourceFactory里面的userAgent字段替换成用户的
            if (headers.containsKey("User-Agent")) {
                String value = headers.remove("User-Agent");
                if (!TextUtils.isEmpty(value)) {
                    try {
                        Field userAgentField = mHttpDataSourceFactory.getClass().getDeclaredField("userAgent");
                        userAgentField.setAccessible(true);
                        userAgentField.set(mHttpDataSourceFactory, value.trim());
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
            for (String k : headers.keySet()) {
                String v = headers.get(k);
                if (v != null)
                    headers.put(k, v.trim());
            }
            mHttpDataSourceFactory.setDefaultRequestProperties(headers);
        }
    }

    public void setCache(Cache cache) {
        this.mCache = cache;
    }

    public void setSocksProxy(String server, int port) {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(server, port));
        mHttpDataSourceFactory = new OkHttpDataSource.Factory(mOkClient.newBuilder().proxy(proxy).build())
                .setUserAgent(mUserAgent);
    }
    public void clearSocksProxy() {
        mHttpDataSourceFactory = mHttpDataSourceFactoryNoProxy;
    }
}