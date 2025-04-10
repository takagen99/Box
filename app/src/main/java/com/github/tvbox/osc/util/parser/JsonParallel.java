package com.github.tvbox.osc.util.parser;
import android.util.Base64;
import com.github.catvod.crawler.SpiderDebug;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 并发解析，直到获得第一个结果
 */
public class JsonParallel {

    private static OkHttpClient client;
    private static ExecutorService executorService;
    private static final List<Future<JSONObject>> futures = new ArrayList<>();
    public static JSONObject parse(LinkedHashMap<String, String> jx, String url) {
        try {
            if (jx != null && jx.size() > 0) {
                client = new OkHttpClient();
                // 使用线程池并发处理任务
                executorService = Executors.newFixedThreadPool(5);
                CompletionService<JSONObject> completionService = new ExecutorCompletionService<>(executorService);
                futures.clear();

                // 遍历所有的解析配置
                for (final String jxName : jx.keySet()) {
                    final String parseUrl = jx.get(jxName);
                    futures.add(completionService.submit(new Callable<JSONObject>() {
                        @Override
                        public JSONObject call() {
                            try {
                                // 获取请求头，并从中取出实际url
                                HashMap<String, String> reqHeaders = JsonParallel.getReqHeader(parseUrl);
                                String realUrl = reqHeaders.get("url");
                                reqHeaders.remove("url");
                                Headers headers = Headers.of(reqHeaders);
                                Request request = new Request.Builder()
                                        .url(realUrl + url)
                                        .headers(headers)
                                        .tag("ParseTag")
                                        .build();

                                Call call = client.newCall(request);
                                Response response = call.execute();
                                String json = response.body().string();

                                JSONObject taskResult = Utils.jsonParse(url, json);
                                taskResult.put("jxFrom", jxName);
                                return taskResult;
                            } catch (Throwable th) {
                                // 输出日志
                                return null;
                            }
                        }
                    }));
                }

                JSONObject pTaskResult = null;
                for (int i = 0; i < futures.size(); ++i) {
                    Future<JSONObject> completed = completionService.take();
                    try {
                        pTaskResult = completed.get();
                        if (pTaskResult != null) {
                            client.dispatcher().cancelAll();
                            for (Future<JSONObject> future : futures) {
                                try {
                                    future.cancel(true);
                                } catch (Throwable t) {
                                    SpiderDebug.log(t);
                                }
                            }
                            futures.clear();
                            break;
                        }
                    } catch (Throwable th) {
                        SpiderDebug.log(th);
                    }
                }
                executorService.shutdownNow();
                if (pTaskResult != null)
                    return pTaskResult;
            }
        } catch (Throwable th) {
            SpiderDebug.log(th);
        }
        return new JSONObject();
    }

    public static void cancelTasks() {
        if (client != null) {
            client.dispatcher().cancelAll();
        }
        if (futures != null) {
            for (Future<JSONObject> future : futures) {
                try {
                    future.cancel(true);
                } catch (Throwable t) {
                }
            }
            futures.clear();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
    public static HashMap<String, String> getReqHeader(String url) {
        HashMap<String, String> reqHeaders = new HashMap<>();
        reqHeaders.put("url", url);
        if (url.contains("cat_ext")) {
            try {
                int start = url.indexOf("cat_ext=");
                int end = url.indexOf("&", start);
                String ext = url.substring(start + 8, end);
                ext = new String(Base64.decode(ext, Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP));
                String newUrl = url.substring(0, start) + url.substring(end + 1);
                JSONObject jsonObject = new JSONObject(ext);
                if (jsonObject.has("header")) {
                    JSONObject headerJson = jsonObject.optJSONObject("header");
                    Iterator<String> keys = headerJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        reqHeaders.put(key, headerJson.optString(key, ""));
                    }
                }
                reqHeaders.put("url", newUrl);
            } catch (Throwable th) {

            }
        }
        return reqHeaders;
    }
}
