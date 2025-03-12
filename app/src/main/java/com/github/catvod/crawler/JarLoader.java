package com.github.catvod.crawler;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.util.Log;


import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.MD5;
import com.lzy.okgo.OkGo;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import dalvik.system.DexClassLoader;
import okhttp3.Response;

public class JarLoader {
    private ConcurrentHashMap<String, DexClassLoader> classLoaders = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Method> proxyMethods = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Spider> spiders = new ConcurrentHashMap<>();
    private volatile String recentJarKey = "";

    /**
     * 不要在主线程调用我
     *
     * @param cache
     */
    public boolean load(String cache) {
        spiders.clear();
        recentJarKey = "main";
        proxyMethods.clear();
        classLoaders.clear();
        return loadClassLoader(cache, "main");
    }

    /*private boolean loadClassLoader(String jar, String key) {
        boolean success = false;
        try {
            File cacheDir = new File(App.getInstance().getCacheDir().getAbsolutePath() + "/catvod_csp");
            if (!cacheDir.exists())
                cacheDir.mkdirs();
            DexClassLoader classLoader = new DexClassLoader(jar, cacheDir.getAbsolutePath(), null, App.getInstance().getClassLoader());
            // make force wait here, some device async dex load
            int count = 0;
            do {
                try {
                    Class classInit = classLoader.loadClass("com.github.catvod.spider.Init");
                    if (classInit != null) {
                        Method method = classInit.getMethod("init", Context.class);
                        method.invoke(null, App.getInstance());
                        System.out.println("自定义爬虫代码加载成功!");
                        success = true;
                        try {
                            Class proxy = classLoader.loadClass("com.github.catvod.spider.Proxy");
                            Method mth = proxy.getMethod("proxy", Map.class);
                            proxyMethods.put(key, mth);
                        } catch (Throwable th) {

                        }
                        break;
                    }
                    Thread.sleep(200);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
                count++;
            } while (count < 5);

            if (success) {
                classLoaders.put(key, classLoader);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return success;
    }*/

    private boolean loadClassLoader(String jar, String key) {
        if (classLoaders.containsKey(key)) {
            return true;
        }
        final String TAG = "JarLoader";
        final File jarFile = new File(jar);
        final AtomicBoolean success = new AtomicBoolean(false);
        DexClassLoader classLoader = null;
        // 1. 前置校验
        if (!validateJarFile(jarFile, TAG)) return false;
        // 2. 准备缓存目录
        File cacheDir = prepareCacheDir(TAG);
        if (cacheDir == null) return false;
        classLoader = createDexClassLoader(jarFile, cacheDir, TAG);
        if (classLoader == null) return false;
        int retryCount = 0;
        final int maxRetries = 2; // 减少重试次数，增加超时检测
        final long retryInterval = 200; // 增加重试间隔
        while (retryCount < maxRetries && !success.get()) {
            try {
                Class<?> initClass = classLoader.loadClass("com.github.catvod.spider.Init");
                Method initMethod = initClass.getMethod("init", Context.class);
                // 4.2 异步执行初始化（解决主线程网络问题）
                executeInitInBackground(initMethod, success, TAG);
                // 4.3 处理初始化结果
                if (success.get()) {
                    handlePostInit(classLoader, key, TAG);
                    classLoaders.put(key, classLoader);
                    Log.i(TAG, "JAR加载成功: " + jar);
                    return true;
                }
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "Init类未找到，重试: " + (++retryCount) + "/" + maxRetries);
                sleep(retryInterval);
            } catch (Exception e) {
                Log.w(TAG, "Init类 加载失败");
                break;
            }
        }

        // 5. 清理资源
        cleanupResources(classLoader, TAG);
        return false;
 }
    // ------------------- 辅助方法 -------------------
    private boolean validateJarFile(File jarFile, String tag) {
        if (!jarFile.exists() || !jarFile.isFile() || jarFile.length() == 0) {
            Log.e(tag, "JAR文件无效: " + jarFile);
            return false;
        }
        return true;
    }

    private File prepareCacheDir(String tag) {
        File cacheDir = new File(App.getInstance().getCacheDir(), "catvod_csp");
        try {
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                Log.e(tag, "目录创建失败: " + cacheDir);
                return null;
            }
            return cacheDir;
        } catch (SecurityException e) {
            Log.e(tag, "目录访问拒绝: " + e.getMessage());
            return null;
        }
    }

    private DexClassLoader createDexClassLoader(File jarFile, File cacheDir, String tag) {
        try {
            return new DexClassLoader(
                    jarFile.getAbsolutePath(),
                    cacheDir.getAbsolutePath(),
                    null,
                    App.getInstance().getClassLoader()
            );
        } catch (Exception e) {
            Log.e(tag, "类加载器创建失败", e);
            return null;
        }
    }

    private void executeInitInBackground(Method initMethod, AtomicBoolean successFlag, String tag) {
        final CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] exceptionHolder = {null};

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                initMethod.invoke(null, App.getInstance());
                successFlag.set(true);
            } catch (InvocationTargetException e) {
                exceptionHolder[0] = e.getTargetException();
            } catch (Exception e) {
                exceptionHolder[0] = e;
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(6, TimeUnit.SECONDS)) {
                Log.e(tag, "初始化超时");
                throw new TimeoutException("初始化未在6秒内完成");
            }
        } catch (InterruptedException e) {
            Log.e(tag, "线程中断", e);
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }

        if (exceptionHolder[0] != null) {
            handleInitException(exceptionHolder[0], tag);
        }
    }

    private void handlePostInit(ClassLoader loader, String key, String tag) {
        // 主线程处理后续操作
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Class<?> proxyClass = loader.loadClass("com.github.catvod.spider.Proxy");
                Method method = proxyClass.getMethod("proxy", Map.class);
                proxyMethods.put(key, method);
                Log.d(tag, "代理方法加载成功");
            } catch (Exception e) {
                Log.w(tag, "代理功能未启用: " + e.getMessage());
            }
        });
    }

    private void handleInitException(Throwable t, String tag) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));

        Log.e(tag, "初始化失败: \n" +
                "类型: " + t.getClass().getName() + "\n" +
                "信息: " + (t.getMessage() != null ? t.getMessage() : "无详细消息") + "\n" +
                "堆栈: \n" + sw.toString());

        if (t instanceof NetworkOnMainThreadException) {
            Log.w(tag, "建议: 第三方JAR包含主线程网络操作，请更新实现或联系开发者");
        }
    }

    private void cleanupResources(DexClassLoader loader, String tag) {
        if (loader != null) {
            try {
                Field pathList = loader.getClass().getSuperclass().getDeclaredField("pathList");
                pathList.setAccessible(true);
                Object dexPathList = pathList.get(loader);
                Field dexElements = dexPathList.getClass().getDeclaredField("dexElements");
                dexElements.setAccessible(true);
                dexElements.set(dexPathList, new Object[0]);
            } catch (Exception e) {
                Log.w(tag, "资源清理失败", e);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
    private DexClassLoader loadJarInternal(String jar, String md5, String key) {

        File cache = new File(App.getInstance().getFilesDir().getAbsolutePath() + "/" + key + ".jar");
        if (!md5.isEmpty()) {
            if (cache.exists() && MD5.getFileMd5(cache).equalsIgnoreCase(md5)) {
                loadClassLoader(cache.getAbsolutePath(), key);
                return classLoaders.get(key);
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
            return classLoaders.get(key);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public Spider getSpider(String key, String cls, String ext, String jar) {
        String clsKey = cls.replace("csp_", "");
        String jarUrl = "";
        String jarMd5 = "";
        String jarKey = "";
        if (jar.isEmpty()) {
            jarKey = "main";
        } else {
            String[] urls = jar.split(";md5;");
            jarUrl = urls[0];
            jarKey = MD5.string2MD5(jarUrl);
            jarMd5 = urls.length > 1 ? urls[1].trim() : "";
        }
        recentJarKey = jarKey;
        if (spiders.containsKey(key))
            return spiders.get(key);
        DexClassLoader classLoader = null;
        if (jarKey.equals("main"))
            classLoader = classLoaders.get("main");
        else {
            classLoader = loadJarInternal(jarUrl, jarMd5, jarKey);
        }
        if (classLoader == null)
            return new SpiderNull();
        try {
            Spider sp = (Spider) classLoader.loadClass("com.github.catvod.spider." + clsKey).newInstance();
            sp.init(App.getInstance(), ext);
            if (!jar.isEmpty()) {
                sp.homeContent(false); // 增加此行 应该可以解决部分写的有问题源的历史记录问题 但会增加这个源的首次加载时间 不需要可以已删掉
            }
            spiders.put(key, sp);
            return sp;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return new SpiderNull();
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) {
        try {
            DexClassLoader classLoader = classLoaders.get("main");
            String clsKey = "Json" + key;
            String hotClass = "com.github.catvod.parser." + clsKey;
            Class jsonParserCls = classLoader.loadClass(hotClass);
            Method mth = jsonParserCls.getMethod("parse", LinkedHashMap.class, String.class);
            return (JSONObject) mth.invoke(null, jxs, url);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return null;
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) {
        try {
            DexClassLoader classLoader = classLoaders.get("main");
            String clsKey = "Mix" + key;
            String hotClass = "com.github.catvod.parser." + clsKey;
            Class jsonParserCls = classLoader.loadClass(hotClass);
            Method mth = jsonParserCls.getMethod("parse", LinkedHashMap.class, String.class, String.class, String.class);
            return (JSONObject) mth.invoke(null, jxs, name, flag, url);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return null;
    }

    public Object[] proxyInvoke(Map params) {
        try {
            Method proxyFun = proxyMethods.get(recentJarKey);
            if (proxyFun != null) {
                return (Object[]) proxyFun.invoke(null, params);
            }
        } catch (Throwable th) {

        }
        return null;
    }
}
