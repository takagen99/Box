package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hjq.permissions.Permission;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class DefaultConfig {

    public static List<MovieSort.SortData> adjustSort(String sourceKey, List<MovieSort.SortData> list, boolean withMy) {
        List<MovieSort.SortData> data = new ArrayList<>();
        if (sourceKey != null) {
            SourceBean sb = ApiConfig.get().getSource(sourceKey);
            ArrayList<String> categories = sb.getCategories();
            if (!categories.isEmpty()) {
                for (String cate : categories) {
                    for (MovieSort.SortData sortData : list) {
                        if (sortData.name.equals(cate)) {
                            if (sortData.filters == null)
                                sortData.filters = new ArrayList<>();
                            data.add(sortData);
                        }
                    }
                }
            } else {
                for (MovieSort.SortData sortData : list) {
                    if (sortData.filters == null)
                        sortData.filters = new ArrayList<>();
                    data.add(sortData);
                }
            }
        }
        if (withMy)
            data.add(0, new MovieSort.SortData("my0", HomeActivity.getRes().getString(R.string.app_home)));
        Collections.sort(data);
        return data;
    }

    public static int getAppVersionCode(Context mContext) {
        //包管理操作管理类
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void resetApp(Context mContext){
        //使用
        clearPublic(mContext);
        clearPrivate(mContext);
        restartApp();
    }

    public static void restartApp() {
        Activity activity = AppManager.getInstance().getActivity(HomeActivity.class);
        final Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        }
        //杀掉以前进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 清空公有目录
     */
    public static void clearPublic(Context mContext) {
        File dir = new File(App.getInstance().getExternalFilesDir("").getParentFile().getAbsolutePath());
        File[] files = dir.listFiles();
        if (null != files) {
            for (File file : files) {
                FileUtils.recursiveDelete(file);
            }
        }
        String publicFilePath = Environment.getExternalStorageDirectory().getPath() + "/" + getPackageName(mContext);
        dir = new File(publicFilePath);
        files = dir.listFiles();
        if (null != files) {
            for (File file : files) {
                FileUtils.recursiveDelete(file);
            }
        }
    }

    /**
     * 清空私有目录
     */
    public static  void clearPrivate(Context mContext) {
        //清空文件夹
        File dir = new File(Objects.requireNonNull(mContext.getFilesDir().getParent()));
        File[] files = dir.listFiles();
        if (null != files) {
            for (File file : files) {
                if (!file.getName().contains("lib")) {
                    FileUtils.recursiveDelete(file);
                }
            }
        }
    }

    public static String getPackageName(Context mContext) {
        //包管理操作管理类
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), 0);
            return packageInfo.packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }
    public static String getAppVersionName(Context mContext) {
        //包管理操作管理类
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 后缀
     *
     * @param name
     * @return
     */
    public static String getFileSuffix(String name) {
        if (TextUtils.isEmpty(name)) {
            return "";
        }
        int endP = name.lastIndexOf(".");
        return endP > -1 ? name.substring(endP) : "";
    }

    /**
     * 获取文件的前缀
     *
     * @param fileName
     * @return
     */
    public static String getFilePrefixName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return "";
        }
        int start = fileName.lastIndexOf(".");
        return start > -1 ? fileName.substring(0, start) : fileName;
    }

    // takagen99 : 增加对flv|avi|mkv|rm|wmv|mpg等几种视频格式的支持
    private static final Pattern snifferMatch = Pattern.compile(
            "http((?!http).){20,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg)\\?.*|" +
                    "http((?!http).){20,}\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg)|" +
                    "http((?!http).)*?video/tos*|" +
                    "http((?!http).){20,}?/m3u8\\?pt=m3u8.*|" +
                    "http((?!http).)*?default\\.ixigua\\.com/.*|" +
                    "http((?!http).)*?dycdn-tos\\.pstatp[^\\?]*|" +
                    "http.*?/player/m3u8play\\.php\\?url=.*|" +
                    "http.*?/player/.*?[pP]lay\\.php\\?url=.*|" +
                    "http.*?/playlist/m3u8/\\?vid=.*|" +
                    "http.*?\\.php\\?type=m3u8&.*|" +
                    "http.*?/download.aspx\\?.*|" +
                    "http.*?/api/up_api.php\\?.*|" +
                    "https.*?\\.66yk\\.cn.*|" +
                    "http((?!http).)*?netease\\.com/file/.*"
    );
    public static boolean isVideoFormat(String url) {
        if (url.contains("=http")) {
            return false;
        }
        if (snifferMatch.matcher(url).find()) {
            return !url.contains(".js") && !url.contains(".css") && !url.contains(".jpg") && !url.contains(".png") && !url.contains(".gif") && !url.contains(".ico") && !url.contains("rl=") && !url.contains(".html");
        }
        return false;
    }


    public static String safeJsonString(JsonObject obj, String key, String defaultVal) {
        try {
            if (obj.has(key)){
                return obj.get(key).isJsonObject() || obj.get(key).isJsonArray()?obj.get(key).toString().trim():obj.getAsJsonPrimitive(key).getAsString().trim();
            }
            else
                return defaultVal;
        } catch (Throwable th) {
        }
        return defaultVal;
    }

    public static int safeJsonInt(JsonObject obj, String key, int defaultVal) {
        try {
            if (obj.has(key))
                return obj.getAsJsonPrimitive(key).getAsInt();
            else
                return defaultVal;
        } catch (Throwable th) {
        }
        return defaultVal;
    }

    public static ArrayList<String> safeJsonStringList(JsonObject obj, String key) {
        ArrayList<String> result = new ArrayList<>();
        try {
            if (obj.has(key)) {
                if (obj.get(key).isJsonObject()) {
                    result.add(obj.get(key).getAsString());
                } else {
                    for (JsonElement opt : obj.getAsJsonArray(key)) {
                        result.add(opt.getAsString());
                    }
                }
            }
        } catch (Throwable th) {
        }
        return result;
    }

    public static String checkReplaceProxy(String urlOri) {
        if (urlOri.startsWith("proxy://"))
            return urlOri.replace("proxy://", ControlManager.get().getAddress(true) + "proxy?");
        return urlOri;
    }

    public static String[] StoragePermissionGroup() {
        return new String[] {
                Permission.MANAGE_EXTERNAL_STORAGE                
        };
    }

}
