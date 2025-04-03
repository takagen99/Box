package com.undcover.freedom.pyramid;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.github.catvod.crawler.Spider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PythonSpider extends Spider {
    PyObject app;
    PyObject pySpider;
    boolean loadSuccess = false;
    private String cachePath;
    private String name;

    public PythonSpider() {
        this("/storage/emulated/0/plugin/");
    }

    public PythonSpider(String cache) {
        this("", cache);
    }

    public PythonSpider(String name, String cache) {
        this.cachePath = cache;
        this.name = name;
    }

    @Override
    public void init(Context context) {
        app.callAttr("init", pySpider);
    }

    public void init(Context context, String url) {
        app = PythonLoader.getInstance().pyApp;
        PyObject retValue = app.callAttr("downloadPlugin", cachePath, url);
        Uri uri = Uri.parse(url);
        String extInfo = uri.getQueryParameter("extend");
        if (null == extInfo) extInfo = "";
        String path = retValue.toString();
        Log.i("PyLoader", "echo-init path: " +path);
        File file = new File(path);
        if (file.exists()) {
            pySpider = app.callAttr("loadFromDisk", path);

            List<PyObject> poList = app.callAttr("getDependence", pySpider).asList();
            for (PyObject po : poList) {
                String api = po.toString();
                Log.i("PyLoader", "echo-init api: " +api);
                String depUrl = PythonLoader.getInstance().getUrlByApi(api);
                if (!depUrl.isEmpty()) {
                    Log.i("PyLoader", "echo-init depUrl: " +depUrl);
                    String tmpPath = app.callAttr("downloadPlugin", cachePath, depUrl).toString();
                    if (!new File(tmpPath).exists()) {
                        PyToast.showCancelableToast(api + "加载失败!");
                        return;
                    } else {
                        PyLog.d(api + ": 加载插件依赖成功！");
                    }
                }
            }
            app.callAttr("init", pySpider, extInfo);
            loadSuccess = true;
            Log.i("PyLoader", "echo-init extInfo: " +url+ extInfo);
            PyLog.d(name + ": 下載插件成功！");
        } else {
            PyToast.showCancelableToast(name + "下载插件失败");
        }
    }

    public String getName() {
        if (name.isEmpty()) {
            PyObject po = app.callAttr("getName", pySpider);
            return po.toString();
        } else {
            return name;
        }
    }

    public JSONObject map2json(HashMap<String, String> extend) {
        JSONObject jo = new JSONObject();
        try {
            if (extend != null) {
                for (String key : extend.keySet()) {
                    jo.put(key, extend.get(key));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    public JSONObject map2json(Map extend) {
        JSONObject jo = new JSONObject();
        try {
            if (extend != null) {
                for (Object key : extend.keySet()) {
                    jo.put(key.toString(), extend.get(key));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    public JSONArray list2json(List<String> array) {
        JSONArray ja = new JSONArray();
        if (array != null) {
            for (String str : array) {
                ja.put(str);
            }
        }
        return ja;
    }

    public String paramLog(Object... obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("request params:[");
        for (Object o : obj) {
            sb.append(o).append("-");
        }
        sb.append("]");
        return sb.toString();
    }

    public Object[] proxyLocal(Map<String,String> params) {
        Log.i("PyLoader","echo-proxyLocal:param"+params.toString());
        List<PyObject> list = app.callAttr("localProxy", pySpider, map2json(params).toString()).asList();
        boolean base64 = list.size() > 4 && list.get(4).toInt() == 1;
        boolean headerAvailable = list.size() > 3 && list.get(3) != null;
        Object[] result = new Object[4];
        result[0] = list.get(0).toInt();
        result[1] = list.get(1).toString();
        result[2] = getStream(list.get(2), base64);
        result[3] = headerAvailable ? getHeader(list.get(3)) : null;
//        result[3] = null;
        return result;
    }


    private Map<String, String> getHeader(PyObject headerObj) {
        if (headerObj == null) {
            return null;
        }
        // 处理 headerObj
        Map<String, String> headerMap = new HashMap<>();
        for (PyObject key : headerObj.asMap().keySet()) {
            headerMap.put(key.toString(), Objects.requireNonNull(headerObj.asMap().get(key)).toString());
        }
        return headerMap;
    }

    private ByteArrayInputStream getStream(PyObject o, boolean base64) {
        if (o == null) return new ByteArrayInputStream(new byte[0]);
        String typeStr = o.type().toString();
        if (typeStr.contains("bytes")) return new ByteArrayInputStream(o.toJava(byte[].class));
        String content = o.toString();
        if (base64 && content.contains("base64,")) {
            content = content.split("base64,")[1];
        }
        return new ByteArrayInputStream(base64 ? decode(content) : content.getBytes());
    }

    public String replaceLocalUrl(String content) {
        return content.replace("http://127.0.0.1:UndCover/proxy", PythonLoader.getInstance().localProxyUrl());
    }

    /**
     * 首页数据内容
     *
     * @param filter 是否开启筛选
     * @return
     */
    public String homeContent(boolean filter) {
        PyLog.nw("homeContent" + "-" + name, paramLog(filter));
        PyObject po = app.callAttr("homeContent", pySpider, filter);
        String rsp = po.toString();
        PyLog.nw("homeContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 首页最近更新数据 如果上面的homeContent中不包含首页最近更新视频的数据 可以使用这个接口返回
     *
     * @return
     */
    public String homeVideoContent() {
        PyLog.nw("homeVideoContent" + "-" + name, "");
        PyObject po = app.callAttr("homeVideoContent", pySpider);
        String rsp = po.toString();
        PyLog.nw("homeVideoContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 分类数据
     *
     * @param tid
     * @param pg
     * @param filter
     * @param extend
     * @return
     */
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        PyLog.nw("categoryContent" + "-" + name, paramLog(tid, pg, filter, map2json(extend).toString()));
        PyObject po = app.callAttr("categoryContent", pySpider, tid, pg, filter, map2json(extend).toString());
        String rsp = po.toString();
        PyLog.nw("categoryContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 详情数据
     *
     * @param ids
     * @return
     */
    public String detailContent(List<String> ids) {
        PyLog.nw("detailContent" + "-" + name, paramLog(list2json(ids).toString()));
        PyObject po = app.callAttr("detailContent", pySpider, list2json(ids).toString());
        String rsp = po.toString();
        PyLog.nw("detailContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 搜索数据内容
     *
     * @param key
     * @param quick
     * @return
     */
    public String searchContent(String key, boolean quick) {
        PyLog.nw("searchContent" + "-" + name, paramLog(key, quick));
        PyObject po = app.callAttr("searchContent", pySpider, key, quick);
        String rsp = po.toString();
        PyLog.nw("searchContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 播放信息
     *
     * @param flag
     * @param id
     * @return
     */
    public String playerContent(String flag, String id, List<String> vipFlags) {
        PyLog.nw("playerContent" + "-" + name, paramLog(flag, id, list2json(vipFlags).toString()));
        PyObject po = app.callAttr("playerContent", pySpider, flag, id, list2json(vipFlags).toString());
        String rsp = replaceLocalUrl(po.toString());
        PyLog.nw("playerContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 直播列表数据
     * @return
     */
    public String liveContent(String url) {
        PyLog.nw("liveContent" + "-" + name, "");
        PyObject po = app.callAttr("liveContent", pySpider,url);
        String rsp = po.toString();
        PyLog.nw("liveContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * webview解析时使用 可自定义判断当前加载的 url 是否是视频
     *
     * @param url
     * @return
     */
    public boolean isVideoFormat(String url) {
        return false;
    }

    /**
     * 是否手动检测webview中加载的url
     *
     * @return
     */
    public boolean manualVideoCheck() {
        return false;
    }

    public static byte[] decode(String s) {
        return decode(s, Base64.DEFAULT | Base64.NO_WRAP);
    }

    public static byte[] decode(String s, int flags) {
        return Base64.decode(s, flags);
    }
}
