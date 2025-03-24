package com.github.tvbox.osc.util.parser;

import android.util.Base64;

import com.github.catvod.crawler.SpiderDebug;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SuperParse {
    public static HashMap<String, ArrayList<String>> flagWebJx = new HashMap<>();
    static HashMap<String, ArrayList<String>> configs = null;

    public static JSONObject parse(LinkedHashMap<String, HashMap<String, String>> jx, String flag, String url) {
        try {
            // 初始化全局配置（configs）一次
            if (configs == null) {
                configs = new HashMap<>();
                for (Map.Entry<String, HashMap<String, String>> entry : jx.entrySet()) {
                    String key = entry.getKey();
                    HashMap<String, String> parseBean = entry.getValue();
                    if (parseBean == null) {
                        continue;
                    }
                    String type = parseBean.get("type");
                    if (type == null) {
                        continue;
                    }
                    if ("1".equals(type) || "0".equals(type)) {
                        try {
                            String ext = parseBean.get("ext");
                            if (ext == null) {
                                continue;
                            }
                            JSONArray flagsArray = new JSONObject(ext).getJSONArray("flag");
                            for (int j = 0; j < flagsArray.length(); j++) {
                                String flagKey = flagsArray.getString(j);
                                ArrayList<String> flagJx = configs.get(flagKey);
                                if (flagJx == null) {
                                    flagJx = new ArrayList<>();
                                    configs.put(flagKey, flagJx);
                                }
                                flagJx.add(key);
                            }
                        } catch (Exception e) {
                            SpiderDebug.log(e);
                        }
                    }
                }
            }

            // 根据配置构建 jsonJx 和 webJx
            LinkedHashMap<String, String> jsonJx = new LinkedHashMap<>();
            ArrayList<String> webJx = new ArrayList<>();
            List<String> targetKeys = configs.get(flag);

            if (targetKeys != null && !targetKeys.isEmpty()) {
                for (String key : targetKeys) {
                    HashMap<String, String> parseBean = jx.get(key);
                    if (parseBean == null) {
                        continue;
                    }
                    String type = parseBean.get("type");
                    if (type == null) {
                        continue;
                    }
                    if ("1".equals(type)) {
                        String urlValue = parseBean.get("url");
                        String ext = parseBean.get("ext");
                        if (urlValue != null && ext != null) {
                            jsonJx.put(key, mixUrl(urlValue, ext));
                        }
                    } else if ("0".equals(type)) {
                        String urlValue = parseBean.get("url");
                        if (urlValue != null) {
                            webJx.add(urlValue);
                        }
                    }
                }
            } else {
                for (Map.Entry<String, HashMap<String, String>> entry : jx.entrySet()) {
                    String key = entry.getKey();
                    HashMap<String, String> parseBean = entry.getValue();
                    if (parseBean == null) {
                        continue;
                    }
                    String type = parseBean.get("type");
                    if (type == null) {
                        continue;
                    }
                    if ("1".equals(type)) {
                        String urlValue = parseBean.get("url");
                        String ext = parseBean.get("ext");
                        if (urlValue != null && ext != null) {
                            jsonJx.put(key, mixUrl(urlValue, ext));
                        }
                    } else if ("0".equals(type)) {
                        String urlValue = parseBean.get("url");
                        if (urlValue != null) {
                            webJx.add(urlValue);
                        }
                    }
                }
            }
            if (!webJx.isEmpty()) {
                flagWebJx.put(flag, webJx);
            }
            //同时进行json和web
            ExecutorService exec = Executors.newFixedThreadPool(2);
            CompletionService<JSONObject> cs = new ExecutorCompletionService<>(exec);
            List<Future<JSONObject>> tasks = new ArrayList<>();

            tasks.add(cs.submit(new Callable<JSONObject>() {
                @Override
                public JSONObject call() {
                    return JsonParallel.parse(jsonJx, url);
                }
            }));
            if (!webJx.isEmpty()) {
                tasks.add(cs.submit(new Callable<JSONObject>() {
                    @Override
                    public JSONObject call() {
                        JSONObject webResult = new JSONObject();
                        String encodedUrl = Base64.encodeToString(url.getBytes(),
                                Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP);
                        try {
                            webResult.put("url", "proxy://go=SuperParse&flag=" + flag + "&url=" + encodedUrl);
                            webResult.put("parse", 1);
                            webResult.put("ua", Utils.UaWinChrome);
                        } catch (Exception e) {
                            SpiderDebug.log(e);
                        }
                        return webResult;
                    }
                }));
            }
            JSONObject result = null;
            for (int i = 0, n = tasks.size(); i < n; i++) {
                try {
                    Future<JSONObject> future = cs.take();
                    JSONObject res = future.get();
                    if (res != null && res.has("url")) {
                        result = res;
                        break;
                    }
                } catch (Exception e) {
                    SpiderDebug.log(e);
                }
            }
            for (Future<JSONObject> future : tasks) {
                future.cancel(true);
            }
            exec.shutdownNow();
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return new JSONObject();
    }

    private static String mixUrl(String url, String ext) {
        if (ext.trim().length() > 0) {
            int idx = url.indexOf("?");
            if (idx > 0) {
                return url.substring(0, idx + 1) + "cat_ext=" + Base64.encodeToString(ext.getBytes(), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP) + "&" + url.substring(idx + 1);
            }
        }
        return url;
    }

    public static Object[] loadHtml(String flag, String url) {
        try {
            url = new String(Base64.decode(url, Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP), "UTF-8");
            String html = "\n" +
                    "<!doctype html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<title>解析</title>\n" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" +
                    "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=EmulateIE10\" />\n" +
                    "<meta name=\"renderer\" content=\"webkit|ie-comp|ie-stand\">\n" +
                    "<meta name=\"viewport\" content=\"width=device-width\">\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<script>\n" +
                    "var apiArray=[#jxs#];\n" +
                    "var urlPs=\"#url#\";\n" +
                    "var iframeHtml=\"\";\n" +
                    "for(var i=0;i<apiArray.length;i++){\n" +
                    "var URL=apiArray[i]+urlPs;\n" +
                    "iframeHtml=iframeHtml+\"<iframe sandbox='allow-scripts allow-same-origin allow-forms' frameborder='0' allowfullscreen='true' webkitallowfullscreen='true' mozallowfullscreen='true' src=\"+URL+\"></iframe>\";\n" +
                    "}\n" +
                    "document.write(iframeHtml);\n" +
                    "</script>\n" +
                    "</body>\n" +
                    "</html>";

            StringBuilder jxs = new StringBuilder();
            if (flagWebJx.containsKey(flag)) {
                ArrayList<String> jxUrls = flagWebJx.get(flag);
                for (int i = 0; i < jxUrls.size(); i++) {
                    jxs.append("\"");
                    jxs.append(jxUrls.get(i));
                    jxs.append("\"");
                    if (i < jxUrls.size() - 1) {
                        jxs.append(",");
                    }
                }
            }
            html = html.replace("#url#", url).replace("#jxs#", jxs.toString());
            Object[] result = new Object[3];
            result[0] = 200;
            result[1] = "text/html; charset=\"UTF-8\"";
            ByteArrayInputStream baos = new ByteArrayInputStream(html.toString().getBytes("UTF-8"));
            result[2] = baos;
            return result;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return null;
    }
}
