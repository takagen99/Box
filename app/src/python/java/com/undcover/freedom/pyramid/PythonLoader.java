package com.undcover.freedom.pyramid;

import android.app.Application;
import android.content.Context;
import android.util.Base64;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.urlhttp.OKCallBack;
import com.github.tvbox.osc.util.urlhttp.OkHttpUtil;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Response;

public class PythonLoader {
    private ConcurrentHashMap<String, Spider> spiders = new ConcurrentHashMap<>();
    private static PythonLoader sInstance;
    private Application app;
    private HashMap<String, JSONObject> siteMap;
    Python pyInstance;
    PyObject pyApp;
    Python.Platform androidPlatform;

    public PythonLoader() {
        siteMap = new HashMap<>();
    }

    public static PythonLoader getInstance() {
        if (sInstance == null) {
            synchronized (PyToast.class) {
                if (sInstance == null) {
                    sInstance = new PythonLoader();
                }
            }
        }
        return sInstance;
    }

    private void setSdk(Context context) {
        int logLevel = PyLog.LEVEL_V;
        PyLog.getInstance().setLogLevel(logLevel).setFilter(PyLog.FILTER_NW | PyLog.FILTER_LC);
        PyLog.TagConstant.TAG_APP = "PythonLoader";

        PyToast.init(context);
    }

    public void setConfig(String config) {
        try {
            JSONObject configJo = new JSONObject(config);
            JSONArray siteList = configJo.getJSONArray("sites");
            for (int i = 0; i < siteList.length(); i++) {
                JSONObject jo = siteList.getJSONObject(i);
                String key = jo.optString("api");
                siteMap.put(key, jo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public PythonLoader setApplication(Application app) {
        this.app = app;
        setSdk(this.app);
        if (pyInstance == null) {
            if (!Python.isStarted()) {
                androidPlatform = new AndroidPlatform(app);
                Python.start(androidPlatform);
            }
            pyInstance = Python.getInstance();
            pyApp = pyInstance.getModule("app");
        }
        return this;
    }

    String cache = "/storage/emulated/0/plugin/";

    public PythonLoader setPluginConfig(String config) {
        this.cache = config;
        return this;
    }

    public String getUrlByApi(String api) {
        String key = "";
        String url = "";
        if (siteMap.containsKey(api)) {
            JSONObject jo = siteMap.get(api);
            key = jo.optString("key");
            url = jo.optString("ext");
        }
        if (!key.isEmpty() && !url.isEmpty()) {
            if (spiders.containsKey(key)) {
                return "";
            } else {
                return url;
            }
        }
        return "";
    }

    public Spider getSpider(String key, String url) throws Exception {
        if (app == null) throw new Exception("set application first");
        if (spiders.containsKey(key)) {
            PyLog.d(key + " :缓存加载成功！");
            return spiders.get(key);
        }
        try {
            PythonSpider sp = new PythonSpider(key, cache);
            Thread initThread = new Thread(() -> {
                try {
                    sp.init(app, url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            initThread.start();
            initThread.join(12_000);
            if (initThread.isAlive()) {
                PyLog.e("echo-init方法执行超时超时");
                initThread.interrupt();
                return new SpiderNull();
            }
            spiders.put(key, sp);
            return sp;
        } catch (Throwable th) {
            PyLog.e(th.toString());
        }
        return new SpiderNull();
    }

    int port = -1;

    public void getPort() {
        if (port <= 0) {
            for (int i = 9978; i < 10000; i++) {
                if (OkHttpUtil.string("http://127.0.0.1:" + i + "/proxy?do=ck&api=python", null).equals("ok")) {
                    port = i;
                    return;
                }
            }
        }
    }

    public String localProxyUrl() {
        getPort();
        return "http://127.0.0.1:" + port + "/proxy";
    }

    public Map<String, String> str2map(String header) {
        Map<String, String> map = new HashMap<>();
        if (header == null || header.isEmpty())
            return map;
        try {
            JSONObject jo = new JSONObject(header);
            for (Iterator<String> it = jo.keys(); it.hasNext(); ) {
                String key = it.next();
                String value = jo.optString(key);
                map.put(key, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    public InputStream getFileStream(String url, String param, String header) {
        if (streamCallback != null) {
            return streamCallback.get(url, str2map(param), str2map(header));
        } else {
            OKCallBack.OKCallBackDefault callBack = new OKCallBack.OKCallBackDefault() {
                @Override
                protected void onFailure(Call call, Exception e) {

                }

                @Override
                protected void onResponse(Response response) {

                }
            };
            OkHttpUtil.get(OkGoHelper.getDefaultClient(), url, str2map(param), str2map(header), callBack);
            return callBack.getResult().body().byteStream();
        }
    }

    public String getFileString(String url, String header) {
        if (stringCallback != null) {
            return stringCallback.get(url, str2map(header));
        } else {
            return OkHttpUtil.string(url, str2map(header));
        }
    }

    FileStreamCallback streamCallback;
    FileStringCallback stringCallback;

    public PythonLoader setFileStreamCallback(FileStreamCallback callback) {
        streamCallback = callback;
        return this;
    }

    public PythonLoader setFileStringCallback(FileStringCallback callback) {
        stringCallback = callback;
        return this;
    }

    public interface FileStreamCallback {
        InputStream get(String url, Map<String, String> paramsMap, Map<String, String> headerMap);
    }

    public interface FileStringCallback {
        String get(String url, Map<String, String> headerMap);
    }
}
