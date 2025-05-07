package com.github.catvod.crawler;


import android.util.Log;

import com.github.tvbox.osc.base.App;

import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD5;

import com.github.tvbox.osc.util.js.JsSpider;
import com.lzy.okgo.OkGo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;
import okhttp3.Response;

public class JsLoader {
    private static final ConcurrentHashMap<String, Spider> spiders = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Class<?>> classes = new ConcurrentHashMap<>();
    //当前的Js爬虫key
    private volatile String recentKey = "";

    public static void destroy() {
        for (Spider spider : spiders.values()){
            spider.cancelByTag();
            spider.destroy();
        }
    }
    public void clear() {
        spiders.clear();
        classes.clear();
    }

    public static void stopAll() {
        for (Spider spider : spiders.values()){
            spider.cancelByTag();
        }
    }

    private boolean loadClassLoader(String jar, String key) {
        boolean success = false;
        Class<?> classInit = null;
        try {
            File cacheDir = new File(App.getInstance().getCacheDir().getAbsolutePath() + "/catvod_jsapi");
            if (!cacheDir.exists())
                cacheDir.mkdirs();
            DexClassLoader classLoader = new DexClassLoader(jar, cacheDir.getAbsolutePath(), null, App.getInstance().getClassLoader());
            int count = 0;
            do {
                try {
                    classInit = classLoader.loadClass("com.github.catvod.js.Method");
                    if (classInit != null) {
                        Log.i("JSLoader", "echo-自定义jsapi代码加载成功!");
                        success = true;
                        break;
                    }
                    Thread.sleep(200);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
                count++;
            } while (count < 5);

            if (success) {
                classes.put(key, classInit);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return success;
    }

    private Class<?> loadJarInternal(String jar, String md5, String key) {
        if (classes.containsKey(key)){
            Log.i("JSLoader", "echo-loadJarInternal cached");
            return classes.get(key);
        }
        File cache = new File(App.getInstance().getFilesDir().getAbsolutePath() + "/csp/" + key + ".jar");
        if (!md5.isEmpty()) {
            if (cache.exists() && MD5.getFileMd5(cache).equalsIgnoreCase(md5)) {
                loadClassLoader(cache.getAbsolutePath(), key);
                return classes.get(key);
            }
        }else {
            if (cache.exists() && !FileUtils.isWeekAgo(cache)) {
                if(loadClassLoader(cache.getAbsolutePath(), key)){
                    return classes.get(key);
                }
            }
        }
        try {
            Response response = OkGo.<File>get(jar).execute();
            InputStream is = response.body().byteStream();
            OutputStream os = new FileOutputStream(cache);
            try {
                byte[] buffer = new byte[2048];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } finally {
                try {
                    is.close();
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            loadClassLoader(cache.getAbsolutePath(), key);
            return classes.get(key);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        if (spiders.containsKey(key)){
            Log.i("JSLoader", "echo-getSpider cached");
            return spiders.get(key);
        }
        Class<?> classLoader = null;
        if (!jar.isEmpty()) {
            String[] urls = jar.split(";md5;");
            String jarUrl = urls[0];
            String jarKey = MD5.string2MD5(jarUrl);
            String jarMd5 = urls.length > 1 ? urls[1].trim() : "";
            classLoader = loadJarInternal(jarUrl, jarMd5, jarKey);
        }
        recentKey = key;
        try {
            Log.i("JSLoader", "echo-getSpider load");
            Spider sp = new JsSpider(key, api, classLoader);
            sp.init(App.getInstance(), ext);
            spiders.put(key, sp);
            return sp;
        } catch (Throwable th) {
            LOG.i("echo-getSpider-error "+th.getMessage());
        }
        return new SpiderNull();
    }

    public Object[] proxyInvoke(Map<String, String> params) {
        try {
            Spider proxyFun = spiders.get(recentKey);
            if (proxyFun != null) {
                return proxyFun.proxyLocal(params);
            }
        } catch (Throwable th) {
        }
        return null;
    }
}
