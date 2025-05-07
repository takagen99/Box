package com.github.tvbox.osc.api;

import static com.github.tvbox.osc.util.RegexUtils.getPattern;
import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import com.github.catvod.crawler.JarLoader;
import com.github.catvod.crawler.JsLoader;
import com.github.catvod.crawler.pyLoader;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.python.IPyLoader;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.util.AES;
import com.github.tvbox.osc.util.AdBlocker;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.M3U8;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.VideoParseRuler;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.tvbox.osc.util.LOG;
/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
public class ApiConfig {
    private static ApiConfig instance;
    private final LinkedHashMap<String, SourceBean> sourceBeanList;
    private SourceBean mHomeSource;
    private ParseBean mDefaultParse;
    private final List<LiveChannelGroup> liveChannelGroupList;
    private final List<ParseBean> parseBeanList;
    private List<String> vipParseFlags;
    private List<IJKCode> ijkCodes;
    private String spider = null;
    public String wallpaper = "";
    public JsonArray livePlayHeaders;
    private final SourceBean emptyHome = new SourceBean();

    private final JarLoader jarLoader = new JarLoader();
    private final JsLoader jsLoader = new JsLoader();
    private final IPyLoader pyLoader =  new pyLoader();
    private final String userAgent = "okhttp/3.15";

    private final String requestAccept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";

    private ApiConfig() {
        clearLoader();
        sourceBeanList = new LinkedHashMap<>();
        liveChannelGroupList = new ArrayList<>();
        parseBeanList = new ArrayList<>();
    }

    public static ApiConfig get() {
        if (instance == null) {
            synchronized (ApiConfig.class) {
                if (instance == null) {
                    instance = new ApiConfig();
                }
            }
        }
        return instance;
    }

    public static String FindResult(String json, String configKey) {
        String content = json;
        try {
            if (AES.isJson(content)) return content;
            Pattern pattern = getPattern("[A-Za-z0]{8}\\*\\*");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                content = content.substring(content.indexOf(matcher.group()) + 10);
                content = new String(Base64.decode(content, Base64.DEFAULT));
            }
            if (content.startsWith("2423")) {
                String data = content.substring(content.indexOf("2324") + 4, content.length() - 26);
                content = new String(AES.toBytes(content)).toLowerCase();
                String key = AES.rightPadding(content.substring(content.indexOf("$#") + 2, content.indexOf("#$")), "0", 16);
                String iv = AES.rightPadding(content.substring(content.length() - 13), "0", 16);
                json = AES.CBC(data, key, iv);
            } else if (configKey != null && !AES.isJson(content)) {
                json = AES.ECB(content, configKey);
            } else {
                json = content;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    private static byte[] getImgJar(String body) {
        Pattern pattern = getPattern("[A-Za-z0]{8}\\*\\*");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            body = body.substring(body.indexOf(matcher.group()) + 10);
            return Base64.decode(body, Base64.DEFAULT);
        }
        return "".getBytes();
    }

    public void loadConfig(boolean useCache, LoadConfigCallback callback, Activity activity) {
        // Embedded Source : Update in Strings.xml if required
        String apiUrl = Hawk.get(HawkConfig.API_URL, HomeActivity.getRes().getString(R.string.app_source));
        if (apiUrl.isEmpty()) {
            callback.error("源地址为空");
            return;
        }
        File cache = new File(App.getInstance().getFilesDir().getAbsolutePath() + "/" + MD5.encode(apiUrl));
        if (useCache && cache.exists()) {
            try {
                parseJson(apiUrl, cache);
                callback.success();
                return;
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        String TempKey = null, configUrl = "", pk = ";pk;";
        if (apiUrl.contains(pk)) {
            String[] a = apiUrl.split(pk);
            TempKey = a[1];
            if (apiUrl.startsWith("clan")) {
                configUrl = clanToAddress(a[0]);
            } else if (apiUrl.startsWith("http")) {
                configUrl = a[0];
            } else {
                configUrl = "http://" + a[0];
            }
        } else if (apiUrl.startsWith("clan")) {
            configUrl = clanToAddress(apiUrl);
        } else if (!apiUrl.startsWith("http")) {
            configUrl = "http://" + configUrl;
        } else {
            configUrl = apiUrl;
        }
        System.out.println("API URL :" + configUrl);
        String configKey = TempKey;
        OkGo.<String>get(configUrl)
                .headers("User-Agent", userAgent)
                .headers("Accept", requestAccept)
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            String json = response.body();
                            parseJson(apiUrl, json);
                            try {
                                File cacheDir = cache.getParentFile();
                                if (!cacheDir.exists())
                                    cacheDir.mkdirs();
                                if (cache.exists())
                                    cache.delete();
                                FileOutputStream fos = new FileOutputStream(cache);
                                fos.write(json.getBytes("UTF-8"));
                                fos.flush();
                                fos.close();
                            } catch (Throwable th) {
                                th.printStackTrace();
                            }
                            callback.success();
                        } catch (Throwable th) {
                            th.printStackTrace();
                            callback.error("解析配置失败");
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        if (cache.exists()) {
                            try {
                                parseJson(apiUrl, cache);
                                callback.success();
                                return;
                            } catch (Throwable th) {
                                th.printStackTrace();
                            }
                        }
                        callback.error("拉取配置失败\n" + (response.getException() != null ? response.getException().getMessage() : ""));
                    }

                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        String result = "";
                        if (response.body() == null) {
                            result = "";
                        } else {
                            result = FindResult(response.body().string(), configKey);
                        }
                        if (apiUrl.startsWith("clan")) {
                            result = clanContentFix(clanToAddress(apiUrl), result);
                        }
                        result = fixContentPath(apiUrl, result);
                        return result;
                    }
                });
    }

    public void loadJar(boolean useCache, String spider, LoadConfigCallback callback) {
        String[] urls = spider.split(";md5;");
        String jarUrl = urls[0];
        String md5 = urls.length > 1 ? urls[1].trim() : "";
        File cache = new File(App.getInstance().getFilesDir().getAbsolutePath() + "/csp/"+MD5.string2MD5(jarUrl)+".jar");
        if (!md5.isEmpty() || useCache) {
            if (cache.exists() && (useCache || MD5.getFileMd5(cache).equalsIgnoreCase(md5))) {
                if (jarLoader.load(cache.getAbsolutePath())) {
                    callback.success();
                } else {
                    callback.error("从缓存加载jar失败");
                }
                return;
            }
        }else {
            if (Boolean.parseBoolean(jarCache) && cache.exists() && !FileUtils.isWeekAgo(cache)) {
                if (jarLoader.load(cache.getAbsolutePath())) {
                    callback.success();
                    return;
                }
            }
        }

        boolean isJarInImg = jarUrl.startsWith("img+");
        jarUrl = jarUrl.replace("img+", "");
        OkGo.<File>get(jarUrl)
                .headers("User-Agent", userAgent)
                .headers("Accept", requestAccept)
                .execute(new AbsCallback<File>() {

                    @Override
                    public File convertResponse(okhttp3.Response response){
                        File cacheDir = cache.getParentFile();
                        assert cacheDir != null;
                        if (!cacheDir.exists()) cacheDir.mkdirs();
                        if (cache.exists()) cache.delete();
                        // 3. 使用 try-with-resources 确保流关闭
                        assert response.body() != null;
                        try (FileOutputStream fos = new FileOutputStream(cache)) {
                            if (isJarInImg) {
                                String respData = response.body().string();
                                LOG.i("echo---jar Response: " + respData);
                                byte[] imgJar = getImgJar(respData);
                                if (imgJar == null || imgJar.length == 0) {
                                    LOG.e("echo---Generated JAR data is empty");
                                    callback.error("JAR data is empty");
                                }
                                fos.write(imgJar);
                            } else {
                                // 使用流式传输避免内存溢出
                                InputStream inputStream = response.body().byteStream();
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }
                            }
                            fos.flush();
                        } catch (IOException e) {
                            return null;
                        }
                        return cache;
                    }

                    @Override
                    public void onSuccess(Response<File> response) {
                        File file = response.body();
                        if (file != null && file.exists()) {
                            try {
                                if (jarLoader.load(file.getAbsolutePath())) {
                                    callback.success();
                                } else {
                                    LOG.e("echo---jar Loader returned false");
                                    callback.error("从网络上加载jar写入缓存后加载失败");
                                }
                            } catch (Exception e) {
                                LOG.e("echo---jar Loader threw exception: " + e.getMessage());
                                callback.error("JAR加载异常: " + e.getMessage());
                            }
                        } else {
                            LOG.e("echo---jar File not found");
                            callback.error("从网络上加载jar地址字节数据为空");
                        }
                    }

                    @Override
                    public void onError(Response<File> response) {
                        Throwable ex = response.getException();
                        if (ex != null) {
                            LOG.i("echo---jar Request failed: " + ex.getMessage());
                        }
                        if(cache.exists())jarLoader.load(cache.getAbsolutePath());
                        callback.error(ex != null ? "从网络上加载jar失败：" + ex.getMessage() : "未知网络错误");
                    }
                });
    }

    private void parseJson(String apiUrl, File f) throws Throwable {
        System.out.println("从本地缓存加载" + f.getAbsolutePath());
        BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String s = "";
        while ((s = bReader.readLine()) != null) {
            sb.append(s + "\n");
        }
        bReader.close();
        parseJson(apiUrl, sb.toString());
    }

    private static  String jarCache ="true";
    private void parseJson(String apiUrl, String jsonStr) {
//        pyLoader.setConfig(jsonStr);
        JsonObject infoJson = new Gson().fromJson(jsonStr, JsonObject.class);
        jarCache = DefaultConfig.safeJsonString(infoJson, "jarCache", "true");
        // spider
        spider = DefaultConfig.safeJsonString(infoJson, "spider", "");
        // wallpaper
        wallpaper = DefaultConfig.safeJsonString(infoJson, "wallpaper", "");
        // 直播播放请求头
        livePlayHeaders = infoJson.getAsJsonArray("livePlayHeaders");
        // 远端站点源
        SourceBean firstSite = null;
        JsonArray sites = infoJson.has("video") ? infoJson.getAsJsonObject("video").getAsJsonArray("sites") : infoJson.get("sites").getAsJsonArray();
        for (JsonElement opt : sites) {
            JsonObject obj = (JsonObject) opt;
            SourceBean sb = new SourceBean();
            String siteKey = obj.get("key").getAsString().trim();
            sb.setKey(siteKey);
            sb.setName(obj.has("name")?obj.get("name").getAsString().trim():siteKey);
            sb.setType(obj.get("type").getAsInt());
            sb.setApi(obj.get("api").getAsString().trim());
            sb.setSearchable(DefaultConfig.safeJsonInt(obj, "searchable", 1));
            sb.setQuickSearch(DefaultConfig.safeJsonInt(obj, "quickSearch", 1));
            if(siteKey.startsWith("py_")){
                sb.setFilterable(1);
            }else {
                sb.setFilterable(DefaultConfig.safeJsonInt(obj, "filterable", 1));
            }
            sb.setHide(DefaultConfig.safeJsonInt(obj, "hide", 0));
            sb.setPlayerUrl(DefaultConfig.safeJsonString(obj, "playUrl", ""));
            sb.setExt(DefaultConfig.safeJsonString(obj, "ext", ""));
            sb.setJar(DefaultConfig.safeJsonString(obj, "jar", ""));
            sb.setPlayerType(DefaultConfig.safeJsonInt(obj, "playerType", -1));
            sb.setCategories(DefaultConfig.safeJsonStringList(obj, "categories"));
            sb.setClickSelector(DefaultConfig.safeJsonString(obj, "click", ""));
            sb.setStyle(DefaultConfig.safeJsonString(obj, "style", ""));
            if (firstSite == null && sb.getHide() == 0)
                firstSite = sb;
            sourceBeanList.put(siteKey, sb);
        }
        if (sourceBeanList != null && sourceBeanList.size() > 0) {
            String home = Hawk.get(HawkConfig.HOME_API, "");
            SourceBean sh = getSource(home);
            if (sh == null || sh.getHide() == 1)
                setSourceBean(firstSite);
            else
                setSourceBean(sh);
        }
        // 需要使用vip解析的flag
        vipParseFlags = DefaultConfig.safeJsonStringList(infoJson, "flags");
        // 解析地址
        parseBeanList.clear();
        if (infoJson.has("parses")) {
            JsonArray parses = infoJson.get("parses").getAsJsonArray();
            for (JsonElement opt : parses) {
                JsonObject obj = (JsonObject) opt;
                ParseBean pb = new ParseBean();
                pb.setName(obj.get("name").getAsString().trim());
                pb.setUrl(obj.get("url").getAsString().trim());
                String ext = obj.has("ext") ? obj.get("ext").getAsJsonObject().toString() : "";
                pb.setExt(ext);
                pb.setType(DefaultConfig.safeJsonInt(obj, "type", 0));
                parseBeanList.add(pb);
            }
            if(!parseBeanList.isEmpty()){
                addSuperParse();
            }
        }
        // 获取默认解析
        if (parseBeanList != null && parseBeanList.size() > 0) {
            String defaultParse = Hawk.get(HawkConfig.DEFAULT_PARSE, "");
            if (!TextUtils.isEmpty(defaultParse))
                for (ParseBean pb : parseBeanList) {
                    if (pb.getName().equals(defaultParse))
                        setDefaultParse(pb);
                }
            if (mDefaultParse == null)
                setDefaultParse(parseBeanList.get(0));
        }

        // takagen99: Check if Live URL is setup in Settings, if no, get from File Config
        liveChannelGroupList.clear();           //修复从后台切换重复加载频道列表
        String liveURL = Hawk.get(HawkConfig.LIVE_URL, "");
        String epgURL  = Hawk.get(HawkConfig.EPG_URL, "");

        String liveURL_final = null;
        try {
            if (infoJson.has("lives") && infoJson.get("lives").getAsJsonArray() != null) {
                JsonObject livesOBJ = infoJson.get("lives").getAsJsonArray().get(0).getAsJsonObject();
                String lives = livesOBJ.toString();
                int index = lives.indexOf("proxy://");
                if (index != -1) {
                    int endIndex = lives.lastIndexOf("\"");
                    String url = lives.substring(index, endIndex);
                    url = DefaultConfig.checkReplaceProxy(url);

                    //clan
                    String extUrl = Uri.parse(url).getQueryParameter("ext");
                    if (extUrl != null && !extUrl.isEmpty()) {
                        String extUrlFix;
                        if (extUrl.startsWith("http") || extUrl.startsWith("clan://")) {
                            extUrlFix = extUrl;
                        } else {
                            extUrlFix = new String(Base64.decode(extUrl, Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP), "UTF-8");
                        }
                        if (extUrlFix.startsWith("clan://")) {
                            extUrlFix = clanContentFix(clanToAddress(apiUrl), extUrlFix);
                        }

                        // takagen99: Capture Live URL into Config
                        System.out.println("Live URL :" + extUrlFix);
                        putLiveHistory(extUrlFix);
                        // Overwrite with Live URL from Settings
                        if (StringUtils.isBlank(liveURL)) {
                            Hawk.put(HawkConfig.LIVE_URL, extUrlFix);
                        } else {
                            extUrlFix = liveURL;
                        }

                        // Final Live URL
                        liveURL_final = extUrlFix;

//                    // Encoding the Live URL
//                    extUrlFix = Base64.encodeToString(extUrlFix.getBytes("UTF-8"), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP);
//                    url = url.replace(extUrl, extUrlFix);
                    }

                    // takagen99 : Getting EPG URL from File Config & put into Settings
                    if (livesOBJ.has("epg")) {
                        String epg = livesOBJ.get("epg").getAsString();
                        System.out.println("EPG URL :" + epg);
                        putEPGHistory(epg);
                        // Overwrite with EPG URL from Settings
                        if (StringUtils.isBlank(epgURL)) {
                            Hawk.put(HawkConfig.EPG_URL, epg);
                        } else {
                            Hawk.put(HawkConfig.EPG_URL, epgURL);
                        }
                    }

//                // Populate Live Channel Listing
//                LiveChannelGroup liveChannelGroup = new LiveChannelGroup();
//                liveChannelGroup.setGroupName(url);
//                liveChannelGroupList.add(liveChannelGroup);

                } else {

                    // if FongMi Live URL Formatting exists
                    if (!lives.contains("type")) {
                        loadLives(infoJson.get("lives").getAsJsonArray());
                    } else {
                        JsonObject fengMiLives = infoJson.get("lives").getAsJsonArray().get(0).getAsJsonObject();
                        Hawk.put(HawkConfig.LIVE_PLAYER_TYPE, DefaultConfig.safeJsonInt(fengMiLives, "playerType", -1));
                        String type = fengMiLives.get("type").getAsString();
                        if (type.equals("0")) {
                            String url = fengMiLives.get("url").getAsString();

                            // takagen99 : Getting EPG URL from File Config & put into Settings
                            if (fengMiLives.has("epg")) {
                                String epg = fengMiLives.get("epg").getAsString();
                                System.out.println("EPG URL :" + epg);
                                putEPGHistory(epg);
                                // Overwrite with EPG URL from Settings
                                if (StringUtils.isBlank(epgURL)) {
                                    Hawk.put(HawkConfig.EPG_URL, epg);
                                } else {
                                    Hawk.put(HawkConfig.EPG_URL, epgURL);
                                }
                            }

                            if (url.startsWith("http")) {
                                // takagen99: Capture Live URL into Settings
                                System.out.println("Live URL :" + url);
                                putLiveHistory(url);
                                // Overwrite with Live URL from Settings
                                if (StringUtils.isBlank(liveURL)) {
                                    Hawk.put(HawkConfig.LIVE_URL, url);
                                } else {
                                    url = liveURL;
                                }

                                // Final Live URL
                                liveURL_final = url;

//                            url = Base64.encodeToString(url.getBytes("UTF-8"), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP);
                            }
                        }
                    }
                }

                // takagen99: Load Live Channel from settings URL (WIP)
                if (StringUtils.isBlank(liveURL_final)) {
                    liveURL_final = liveURL;
                }
                liveURL_final = Base64.encodeToString(liveURL_final.getBytes("UTF-8"), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP);
                liveURL_final = "http://127.0.0.1:9978/proxy?do=live&type=txt&ext=" + liveURL_final;
                LiveChannelGroup liveChannelGroup = new LiveChannelGroup();
                liveChannelGroup.setGroupName(liveURL_final);
                liveChannelGroupList.add(liveChannelGroup);
            }


        } catch (Throwable th) {
            th.printStackTrace();
        }

        // Video parse rule for host
        if (infoJson.has("rules")) {
            VideoParseRuler.clearRule();
            for(JsonElement oneHostRule : infoJson.getAsJsonArray("rules")) {
                JsonObject obj = (JsonObject) oneHostRule;
                //嗅探过滤规则
                if (obj.has("host")) {
                    String host = obj.get("host").getAsString();
                    if (obj.has("rule")) {
                        JsonArray ruleJsonArr = obj.getAsJsonArray("rule");
                        ArrayList<String> rule = new ArrayList<>();
                        for (JsonElement one : ruleJsonArr) {
                            String oneRule = one.getAsString();
                            rule.add(oneRule);
                        }
                        if (rule.size() > 0) {
                            VideoParseRuler.addHostRule(host, rule);
                        }
                    }
                    if (obj.has("filter")) {
                        JsonArray filterJsonArr = obj.getAsJsonArray("filter");
                        ArrayList<String> filter = new ArrayList<>();
                        for (JsonElement one : filterJsonArr) {
                            String oneFilter = one.getAsString();
                            filter.add(oneFilter);
                        }
                        if (filter.size() > 0) {
                            VideoParseRuler.addHostFilter(host, filter);
                        }
                    }
                }
                //广告过滤规则
                if (obj.has("hosts") && obj.has("regex")) {
                    ArrayList<String> rule = new ArrayList<>();
                    ArrayList<String> ads = new ArrayList<>();
                    JsonArray regexArray = obj.getAsJsonArray("regex");
                    for (JsonElement one : regexArray) {
                        String regex = one.getAsString();
                        if (M3U8.isAd(regex)) ads.add(regex);
                        else rule.add(regex);
                    }
                    JsonArray array = obj.getAsJsonArray("hosts");
                    for (JsonElement one : array) {
                        String host = one.getAsString();
                        VideoParseRuler.addHostRule(host, rule);
                        VideoParseRuler.addHostRegex(host, ads);
                    }
                }
                //嗅探脚本规则 如 click
                if (obj.has("hosts") && obj.has("script")) {
                    ArrayList<String> scripts = new ArrayList<>();
                    JsonArray scriptArray = obj.getAsJsonArray("script");
                    for (JsonElement one : scriptArray) {
                        String script = one.getAsString();
                        scripts.add(script);
                    }
                    JsonArray array = obj.getAsJsonArray("hosts");
                    for (JsonElement one : array) {
                        String host = one.getAsString();
                        VideoParseRuler.addHostScript(host, scripts);
                    }
                }
            }
        }

        String defaultIJKADS = "{\"ijk\":[{\"options\":[{\"name\":\"opensles\",\"category\":4,\"value\":\"0\"},{\"name\":\"overlay-format\",\"category\":4,\"value\":\"842225234\"},{\"name\":\"framedrop\",\"category\":4,\"value\":\"0\"},{\"name\":\"soundtouch\",\"category\":4,\"value\":\"1\"},{\"name\":\"start-on-prepared\",\"category\":4,\"value\":\"1\"},{\"name\":\"http-detect-rangeupport\",\"category\":1,\"value\":\"0\"},{\"name\":\"fflags\",\"category\":1,\"value\":\"fastseek\"},{\"name\":\"skip_loop_filter\",\"category\":2,\"value\":\"48\"},{\"name\":\"reconnect\",\"category\":4,\"value\":\"1\"},{\"name\":\"enable-accurate-seek\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-auto-rotate\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-handle-resolution-change\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-hevc\",\"category\":4,\"value\":\"0\"},{\"name\":\"dns_cache_timeout\",\"category\":1,\"value\":\"600000000\"}],\"group\":\"软解码\"},{\"options\":[{\"name\":\"opensles\",\"category\":4,\"value\":\"0\"},{\"name\":\"overlay-format\",\"category\":4,\"value\":\"842225234\"},{\"name\":\"framedrop\",\"category\":4,\"value\":\"0\"},{\"name\":\"soundtouch\",\"category\":4,\"value\":\"1\"},{\"name\":\"start-on-prepared\",\"category\":4,\"value\":\"1\"},{\"name\":\"http-detect-rangeupport\",\"category\":1,\"value\":\"0\"},{\"name\":\"fflags\",\"category\":1,\"value\":\"fastseek\"},{\"name\":\"skip_loop_filter\",\"category\":2,\"value\":\"48\"},{\"name\":\"reconnect\",\"category\":4,\"value\":\"1\"},{\"name\":\"enable-accurate-seek\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-auto-rotate\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-handle-resolution-change\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-hevc\",\"category\":4,\"value\":\"1\"},{\"name\":\"dns_cache_timeout\",\"category\":1,\"value\":\"600000000\"}],\"group\":\"硬解码\"}],\"ads\":[\"mimg.0c1q0l.cn\",\"www.googletagmanager.com\",\"www.google-analytics.com\",\"mc.usihnbcq.cn\",\"mg.g1mm3d.cn\",\"mscs.svaeuzh.cn\",\"cnzz.hhttm.top\",\"tp.vinuxhome.com\",\"cnzz.mmstat.com\",\"www.baihuillq.com\",\"s23.cnzz.com\",\"z3.cnzz.com\",\"c.cnzz.com\",\"stj.v1vo.top\",\"z12.cnzz.com\",\"img.mosflower.cn\",\"tips.gamevvip.com\",\"ehwe.yhdtns.com\",\"xdn.cqqc3.com\",\"www.jixunkyy.cn\",\"sp.chemacid.cn\",\"hm.baidu.com\",\"s9.cnzz.com\",\"z6.cnzz.com\",\"um.cavuc.com\",\"mav.mavuz.com\",\"wofwk.aoidf3.com\",\"z5.cnzz.com\",\"xc.hubeijieshikj.cn\",\"tj.tianwenhu.com\",\"xg.gars57.cn\",\"k.jinxiuzhilv.com\",\"cdn.bootcss.com\",\"ppl.xunzhuo123.com\",\"xomk.jiangjunmh.top\",\"img.xunzhuo123.com\",\"z1.cnzz.com\",\"s13.cnzz.com\",\"xg.huataisangao.cn\",\"z7.cnzz.com\",\"xg.huataisangao.cn\",\"z2.cnzz.com\",\"s96.cnzz.com\",\"q11.cnzz.com\",\"thy.dacedsfa.cn\",\"xg.whsbpw.cn\",\"s19.cnzz.com\",\"z8.cnzz.com\",\"s4.cnzz.com\",\"f5w.as12df.top\",\"ae01.alicdn.com\",\"www.92424.cn\",\"k.wudejia.com\",\"vivovip.mmszxc.top\",\"qiu.xixiqiu.com\",\"cdnjs.hnfenxun.com\",\"cms.qdwght.com\"]}";
        JsonObject defaultJson = new Gson().fromJson(defaultIJKADS, JsonObject.class);
        // 广告地址
        if(AdBlocker.isEmpty()){
            //默认广告拦截
            for (JsonElement host : defaultJson.getAsJsonArray("ads")) {
                AdBlocker.addAdHost(host.getAsString());
            }
            //追加的广告拦截
            if(infoJson.has("ads")){
                for (JsonElement host : infoJson.getAsJsonArray("ads")) {
                    if(!AdBlocker.hasHost(host.getAsString())){
                        AdBlocker.addAdHost(host.getAsString());
                    }
                }
            }
        }
        // IJK解码配置
        if (ijkCodes == null) {
            ijkCodes = new ArrayList<>();
            boolean foundOldSelect = false;
            String ijkCodec = Hawk.get(HawkConfig.IJK_CODEC, "");
            JsonArray ijkJsonArray = infoJson.has("ijk") ? infoJson.get("ijk").getAsJsonArray() : defaultJson.get("ijk").getAsJsonArray();
            for (JsonElement opt : ijkJsonArray) {
                JsonObject obj = (JsonObject) opt;
                String name = obj.get("group").getAsString();
                LinkedHashMap<String, String> baseOpt = new LinkedHashMap<>();
                for (JsonElement cfg : obj.get("options").getAsJsonArray()) {
                    JsonObject cObj = (JsonObject) cfg;
                    String key = cObj.get("category").getAsString() + "|" + cObj.get("name").getAsString();
                    String val = cObj.get("value").getAsString();
                    baseOpt.put(key, val);
                }
                IJKCode codec = new IJKCode();
                codec.setName(name);
                codec.setOption(baseOpt);
                if (name.equals(ijkCodec) || TextUtils.isEmpty(ijkCodec)) {
                    codec.selected(true);
                    ijkCodec = name;
                    foundOldSelect = true;
                } else {
                    codec.selected(false);
                }
                ijkCodes.add(codec);
            }
            if (!foundOldSelect && ijkCodes.size() > 0) {
                ijkCodes.get(0).selected(true);
            }
        }
    }

    private void putLiveHistory(String url) {
        if (!url.isEmpty()) {
            ArrayList<String> liveHistory = Hawk.get(HawkConfig.LIVE_HISTORY, new ArrayList<String>());
            if (!liveHistory.contains(url))
                liveHistory.add(0, url);
            if (liveHistory.size() > 20)
                liveHistory.remove(20);
            Hawk.put(HawkConfig.LIVE_HISTORY, liveHistory);
        }
    }

    public static void putEPGHistory(String url) {
        if (!url.isEmpty()) {
            ArrayList<String> epgHistory = Hawk.get(HawkConfig.EPG_HISTORY, new ArrayList<String>());
            if (!epgHistory.contains(url))
                epgHistory.add(0, url);
            if (epgHistory.size() > 20)
                epgHistory.remove(20);
            Hawk.put(HawkConfig.EPG_HISTORY, epgHistory);
        }
    }

    public void loadLives(JsonArray livesArray) {
        liveChannelGroupList.clear();
        int groupIndex = 0;
        int channelIndex = 0;
        int channelNum = 0;
        for (JsonElement groupElement : livesArray) {
            LiveChannelGroup liveChannelGroup = new LiveChannelGroup();
            liveChannelGroup.setLiveChannels(new ArrayList<LiveChannelItem>());
            liveChannelGroup.setGroupIndex(groupIndex++);
            String groupName = ((JsonObject) groupElement).get("group").getAsString().trim();
            String[] splitGroupName = groupName.split("_", 2);
            liveChannelGroup.setGroupName(splitGroupName[0]);
            if (splitGroupName.length > 1)
                liveChannelGroup.setGroupPassword(splitGroupName[1]);
            else
                liveChannelGroup.setGroupPassword("");
            channelIndex = 0;
            for (JsonElement channelElement : ((JsonObject) groupElement).get("channels").getAsJsonArray()) {
                JsonObject obj = (JsonObject) channelElement;
                LiveChannelItem liveChannelItem = new LiveChannelItem();
                liveChannelItem.setChannelName(obj.get("name").getAsString().trim());
                liveChannelItem.setChannelIndex(channelIndex++);
                liveChannelItem.setChannelNum(++channelNum);
                ArrayList<String> urls = DefaultConfig.safeJsonStringList(obj, "urls");
                ArrayList<String> sourceNames = new ArrayList<>();
                ArrayList<String> sourceUrls = new ArrayList<>();
                int sourceIndex = 1;
                for (String url : urls) {
                    String[] splitText = url.split("\\$", 2);
                    sourceUrls.add(splitText[0]);
                    if (splitText.length > 1)
                        sourceNames.add(splitText[1]);
                    else
                        sourceNames.add("源" + sourceIndex);
                    sourceIndex++;
                }
                liveChannelItem.setChannelSourceNames(sourceNames);
                liveChannelItem.setChannelUrls(sourceUrls);
                liveChannelGroup.getLiveChannels().add(liveChannelItem);
            }
            liveChannelGroupList.add(liveChannelGroup);
        }
    }

    public String getSpider() {
        return spider;
    }

    public Spider getCSP(SourceBean sourceBean) {
        if (sourceBean.getApi().endsWith(".js") || sourceBean.getApi().contains(".js?")){
            return jsLoader.getSpider(sourceBean.getKey(), sourceBean.getApi(), sourceBean.getExt(), sourceBean.getJar());
        }else if (sourceBean.getApi().contains(".py")) {
            return pyLoader.getSpider(sourceBean.getKey(), sourceBean.getApi(), sourceBean.getExt());
        } else {
            return jarLoader.getSpider(sourceBean.getKey(), sourceBean.getApi(), sourceBean.getExt(), sourceBean.getJar());
        }
    }

    public Spider getPyCSP(String url) {
        return pyLoader.getSpider(MD5.string2MD5(url), url, "");
    }
	
    public Object[] proxyLocal(Map<String,String> param) {
        if ("js".equals(param.get("do"))) {
            return jsLoader.proxyInvoke(param);
        }
        SourceBean sourceBean = ApiConfig.get().getHomeSourceBean(); 
        String apiString = sourceBean.getApi();
        return apiString.contains(".py") ? pyLoader.proxyInvoke(param) : jarLoader.proxyInvoke(param);
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) {
        return jarLoader.jsonExt(key, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) {
        return jarLoader.jsonExtMix(flag, key, name, jxs, url);
    }

    public interface LoadConfigCallback {
        void success();

        void retry();

        void error(String msg);
    }

    public interface FastParseCallback {
        void success(boolean parse, String url, Map<String, String> header);

        void fail(int code, String msg);
    }

    public SourceBean getSource(String key) {
        if (!sourceBeanList.containsKey(key))
            return null;
        return sourceBeanList.get(key);
    }

    public void setSourceBean(SourceBean sourceBean) {
        this.mHomeSource = sourceBean;
        Hawk.put(HawkConfig.HOME_API, sourceBean.getKey());
    }

    public void setDefaultParse(ParseBean parseBean) {
        if (this.mDefaultParse != null)
            this.mDefaultParse.setDefault(false);
        this.mDefaultParse = parseBean;
        Hawk.put(HawkConfig.DEFAULT_PARSE, parseBean.getName());
        parseBean.setDefault(true);
    }

    public ParseBean getDefaultParse() {
        return mDefaultParse;
    }

    public List<SourceBean> getSourceBeanList() {
        return new ArrayList<>(sourceBeanList.values());
    }

    public List<ParseBean> getParseBeanList() {
        return parseBeanList;
    }

    public List<String> getVipParseFlags() {
        return vipParseFlags;
    }

    public SourceBean getHomeSourceBean() {
        return mHomeSource == null ? emptyHome : mHomeSource;
    }

    public List<LiveChannelGroup> getChannelGroupList() {
        return liveChannelGroupList;
    }

    public List<IJKCode> getIjkCodes() {
        return ijkCodes;
    }

    public IJKCode getCurrentIJKCode() {
        String codeName = Hawk.get(HawkConfig.IJK_CODEC, "");
        return getIJKCodec(codeName);
    }

    public IJKCode getIJKCodec(String name) {
        for (IJKCode code : ijkCodes) {
            if (code.getName().equals(name))
                return code;
        }
        return ijkCodes.get(0);
    }

    public JsonArray getLivePlayHeaders() {
        return livePlayHeaders;
    }

    String clanToAddress(String lanLink) {
        if (lanLink.startsWith("clan://localhost/")) {
            return lanLink.replace("clan://localhost/", ControlManager.get().getAddress(true) + "file/");
        } else {
            String link = lanLink.substring(7);
            int end = link.indexOf('/');
            return "http://" + link.substring(0, end) + "/file/" + link.substring(end + 1);
        }
    }

    String clanContentFix(String lanLink, String content) {
        String fix = lanLink.substring(0, lanLink.indexOf("/file/") + 6);
        return content.replace("clan://", fix);
    }

    String fixContentPath(String url, String content) {
        if (content.contains("\"./")) {
            url=url.replace("file://","clan://localhost/");
            if (!url.startsWith("http") && !url.startsWith("clan://")) {
                url = "http://" + url;
            }
            if (url.startsWith("clan://")) url = clanToAddress(url);
            content = content.replace("./", url.substring(0, url.lastIndexOf("/") + 1));
        }
        return content;
    }

    public void clearJarLoader()
    {
        jarLoader.clear();
    }
    private void addSuperParse(){
        ParseBean superPb = new ParseBean();
        superPb.setName("超级解析");
        superPb.setUrl("SuperParse");
        superPb.setExt("");
        superPb.setType(4);
        parseBeanList.add(0, superPb);
    }
    public void clearLoader(){
        jarLoader.clear();
        pyLoader.clear();
        jsLoader.clear();
    }
    String miTV(String url) {
        if (url.startsWith("p") || url.startsWith("mitv")) {

        }
        return url;
    }

}
