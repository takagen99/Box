// 文件: app/src/python/java/com/github/catvod/crawler/python/PyLoader.java
package com.github.catvod.crawler;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.github.catvod.crawler.python.IPyLoader;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD5;
import com.undcover.freedom.pyramid.PythonLoader;
import com.undcover.freedom.pyramid.PythonSpider;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class pyLoader implements IPyLoader {
    private final PythonLoader pythonLoader;
    private final ConcurrentHashMap<String, Spider> spiders;
    private String lastConfig = null; // 记录上次的配置

    public pyLoader() {
        pythonLoader = PythonLoader.getInstance().setApplication(App.getInstance());
        spiders = new ConcurrentHashMap<>();
    }

    @Override
    public void clear() {
        spiders.clear();
    }

    @Override
    public void setConfig(String jsonStr) {
        if (jsonStr != null && !jsonStr.equals(lastConfig)) {
            Log.i("PyLoader", "echo-setConfig 初始化json ");
            pythonLoader.setConfig(jsonStr);
            lastConfig = jsonStr;
        }
    }

    private String recentPyApi;
    @Override
    public void setRecentPyKey(String pyApi) {
        recentPyApi = pyApi;
    }

    @Override
    public Spider getSpider(String key, String cls, String ext) {
        if (spiders.containsKey(key)) {
            Log.i("PyLoader", "echo-getSpider spider缓存: " + key);
            return spiders.get(key);
        }
        try {
            if (ContextCompat.checkSelfPermission(App.getInstance(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.i("PyLoader", "无存储权限，终止执行");
                return new SpiderNull();
            }
            Log.i("PyLoader", "echo-getSpider url: " + getPyUrl(cls, ext));
            Spider sp = pythonLoader.getSpider(key, getPyUrl(cls, ext));
//            Log.i("PyLoader", "echo-getSpider homeContent: " + sp.homeContent(true));
            spiders.put(key, sp);
            Log.i("PyLoader", "echo-getSpider 加载spider: " + key);
            return sp;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return new SpiderNull();
    }

    @Override
    public Object[] proxyInvoke(Map<String, String> params){
        if(recentPyApi==null)return null;
        LOG.i("echo-recentPyApi" + recentPyApi);
        try {
            PythonSpider originalSpider = (PythonSpider) getSpider(MD5.string2MD5(recentPyApi), recentPyApi,"");
            return originalSpider.proxyLocal(params);
        } catch (Throwable th) {
            LOG.i("echo-proxyInvoke_Throwable:---" + th.getMessage());
            th.printStackTrace();
        }
        return null;
    }

    private String getPyUrl(String api, String ext) throws UnsupportedEncodingException {
        StringBuilder urlBuilder = new StringBuilder(api);
        if (!ext.isEmpty()) {
//            ext= URLEncoder.encode(ext,"utf8");
            urlBuilder.append(api.contains("?") ? "&" : "?").append("extend=").append(ext);
        }
        return urlBuilder.toString();
    }
}
