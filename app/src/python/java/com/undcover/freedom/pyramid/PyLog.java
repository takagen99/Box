package com.undcover.freedom.pyramid;
import android.util.Log;

/**
 * Created by UndCover on 16/12/15.
 */

public class PyLog {
    public final static int LEVEL_V = 5;
    public final static int LEVEL_D = 4;
    public final static int LEVEL_I = 3;
    public final static int LEVEL_W = 2;
    public final static int LEVEL_E = 1;
    public final static int LEVEL_RELEASE = 0;
    private static int logLevel = LEVEL_RELEASE;
    private static PyLog mLog;

    public synchronized static PyLog getInstance() {
        mLog = new PyLog();
        return mLog;
    }

    public PyLog setLogLevel(int logLevel) {
        try
        {
            checkInit();
        }
        catch (Exception e){
            return mLog;
        }
        this.logLevel = logLevel;
        return mLog;
    }

    /**
     * 用于生命周期
     */
    public final static int FILTER_LC = 0x01;
    /**
     * 用于网络请求 默认为 LEVEL_I
     */
    public final static int FILTER_NW = 0x02;
    /**
     * ActivityManager内置Log
     */
    public final static int FILTER_AM = 0x04;
    /**
     * 用于FrameWork内置log
     */
    public final static int FILTER_FW = 0x08;

    private static boolean isLifeCycleEnable = false;
    private static boolean isNetWorkEnable = false;
    private static boolean isFrameWorkEnable = false;
    private static boolean isAtyManagerEnable = false;

    /**
     * 设置内设Log的过滤，添加需要输出的Log<br/>
     * FILTER_LC 生命周期<br/>
     * FILTER_NW 网络请求<br/>
     * FILTER_AM AtyManager<br/>
     * FILTER_FW 框架<br/>
     *
     * @param filter
     * @return
     */
    public PyLog setFilter(int filter) {
        try
        {
            checkInit();
        }
        catch (Exception e){
            return mLog;
        }
        isLifeCycleEnable = (filter & FILTER_LC) / FILTER_LC == 1 ? true : false;
        isNetWorkEnable = (filter & FILTER_NW) / FILTER_NW == 1 ? true : false;
        isFrameWorkEnable = (filter & FILTER_FW) / FILTER_FW == 1 ? true : false;
        isAtyManagerEnable = (filter & FILTER_AM) / FILTER_AM == 1 ? true : false;
        return mLog;
    }

    public static void V(String tag, String msg) {
        if (logLevel < LEVEL_V)
            return;
        Log.v(tag, msg);
    }

    public static void D(String tag, String msg) {
        if (logLevel < LEVEL_D)
            return;
        Log.d(tag, msg);
    }

    public static void I(String tag, String msg) {
        if (logLevel < LEVEL_I)
            return;
        Log.i(tag, msg);
    }

    public static void W(String tag, String msg) {
        if (logLevel < LEVEL_W)
            return;
        Log.w(tag, msg);
    }

    public static void E(String tag, String msg) {
        if (logLevel < LEVEL_E)
            return;
        Log.e(tag, msg);
    }

    private static int segmentSize = 3 * 1024;

    private static void longV(String tag, String msg) {
        if (logLevel < LEVEL_V)
            return;
        while (msg.length() > segmentSize) {// 循环分段打印日志
            String logContent = msg.substring(0, segmentSize);
            msg = msg.replace(logContent, "\t\t");
            Log.v(tag, logContent);
        }
        Log.v(tag, msg);// 打印剩余日志
    }

    private static void longD(String tag, String msg) {
        if (logLevel < LEVEL_D)
            return;
        while (msg.length() > segmentSize) {// 循环分段打印日志
            String logContent = msg.substring(0, segmentSize);
            msg = msg.replace(logContent, "\t\t");
            Log.d(tag, logContent);
        }
        Log.d(tag, msg);// 打印剩余日志
    }

    private static void longI(String tag, String msg) {
        if (logLevel < LEVEL_I)
            return;
        while (msg.length() > segmentSize) {// 循环分段打印日志
            String logContent = msg.substring(0, segmentSize);
            msg = msg.replace(logContent, "\t\t");
            Log.i(tag, logContent);
        }
        Log.i(tag, msg);// 打印剩余日志
    }

    private static void longW(String tag, String msg) {
        if (logLevel < LEVEL_W)
            return;

        while (msg.length() > segmentSize) {// 循环分段打印日志
            String logContent = msg.substring(0, segmentSize);
            msg = msg.replace(logContent, "\t\t");
            Log.w(tag, logContent);
        }
        Log.w(tag, msg);// 打印剩余日志
    }

    private static void longE(String tag, String msg) {
        if (logLevel < LEVEL_E)
            return;

        while (msg.length() > segmentSize) {// 循环分段打印日志
            String logContent = msg.substring(0, segmentSize);
            msg = msg.replace(logContent, "\t\t");
            Log.e(tag, logContent);
        }
        Log.e(tag, msg);// 打印剩余日志

//        if (tag == null || tag.length() == 0
//                || msg == null || msg.length() == 0)
//            return;

//        int segmentSize = 3 * 1024;
//        long length = msg.length();
//        if (length <= segmentSize) {// 长度小于等于限制直接打印
//            Log.e(tag, msg);
//        } else {
//            while (msg.length() > segmentSize) {// 循环分段打印日志
//                String logContent = msg.substring(0, segmentSize);
//                msg = msg.replace(logContent, "");
//                Log.e(tag, logContent);
//            }
//            Log.e(tag, msg);// 打印剩余日志
//        }
    }

    /**
     * 默认Tag
     *
     * @param msg
     */
    public static void v(String msg) {
        v(TagConstant.TAG_DEF, msg);
    }

    /**
     * 默认Tag
     *
     * @param msg
     */
    public static void d(String msg) {
        d(TagConstant.TAG_DEF, msg);
    }

    /**
     * 默认Tag
     *
     * @param msg
     */
    public static void i(String msg) {
        i(TagConstant.TAG_DEF, msg);
    }

    /**
     * 默认Tag
     *
     * @param msg
     */
    public static void w(String msg) {
        w(TagConstant.TAG_DEF, msg);
    }

    /**
     * 默认Tag
     *
     * @param msg
     */
    public static void e(String msg) {
        e(TagConstant.TAG_DEF, msg);
    }

    /**
     * 添加AppTag
     *
     * @param tag
     * @param msg
     */
    public static void v(String tag, String msg) {
        String msgStr = tag + " " + msg;
        if (msgStr.length() > segmentSize) {
            longV(TagConstant.TAG_APP, msgStr);
        } else {
            V(TagConstant.TAG_APP, msgStr);
        }
    }

    /**
     * 添加AppTag
     *
     * @param tag
     * @param msg
     */
    public static void d(String tag, String msg) {
        String msgStr = tag + " " + msg;
        if (msgStr.length() > segmentSize) {
            longD(TagConstant.TAG_APP, msgStr);
        } else {
            D(TagConstant.TAG_APP, msgStr);
        }
    }

    /**
     * 添加AppTag
     *
     * @param tag
     * @param msg
     */
    public static void i(String tag, String msg) {
        String msgStr = tag + " " + msg;
        if (msgStr.length() > segmentSize) {
            longI(TagConstant.TAG_APP, msgStr);
        } else {
            I(TagConstant.TAG_APP, msgStr);
        }
    }

    /**
     * 添加AppTag
     *
     * @param tag
     * @param msg
     */
    public static void w(String tag, String msg) {
        String msgStr = tag + " " + msg;
        if (msgStr.length() > segmentSize) {
            longW(TagConstant.TAG_APP, msgStr);
        } else {
            W(TagConstant.TAG_APP, msgStr);
        }
    }

    /**
     * 添加AppTag
     *
     * @param tag
     * @param msg
     */
    public static void e(String tag, String msg) {
        String msgStr = tag + " " + msg;
        if (msgStr.length() > segmentSize) {
            longE(TagConstant.TAG_APP, msgStr);
        } else {
            E(TagConstant.TAG_APP, msgStr);
        }
    }

    /**
     * 多参数,使用默认Tag
     *
     * @param args
     */
    public static void v(String... args) {
        String msg = getArgsStr(args);
        v(TagConstant.TAG_DEF, msg);
    }

    /**
     * 多参数,使用默认Tag
     *
     * @param args
     */
    public static void d(String... args) {
        String msg = getArgsStr(args);
        d(msg);
    }

    /**
     * 多参数,使用默认Tag
     *
     * @param args
     */
    public static void i(String... args) {
        String msg = getArgsStr(args);
        i(msg);
    }

    /**
     * 多参数,使用默认Tag
     *
     * @param args
     */
    public static void w(String... args) {
        String msg = getArgsStr(args);
        w(msg);
    }

    /**
     * 多参数,使用默认Tag
     *
     * @param args
     */
    public static void e(String... args) {
        String msg = getArgsStr(args);
        e(msg);
    }

    /**
     * 打印生命周期
     *
     * @param tag
     * @param msg
     */
    public static void lc(String tag, String msg) {
        if (isLifeCycleEnable) {
            d(tag, TagConstant.TAG_LC, msg);
        }
    }

    /**
     * 打印ActivityManager管理
     *
     * @param tag
     * @param msg
     */
    public static void am(String tag, String msg) {
        if (isAtyManagerEnable) {
            d(tag, TagConstant.TAG_AM, msg);
        }
    }

    /**
     * 打印框架信息
     *
     * @param tag
     * @param msg
     */
    public static void fw(String tag, String msg) {
        if (isFrameWorkEnable) {
            d(tag, TagConstant.TAG_FW, msg);
        }
    }

    /**
     * 打印网络请求
     *
     * @param tag
     * @param msg
     */
    public static void nw(String tag, String msg) {
        if (isNetWorkEnable) {
            nw(tag, msg, false);
        }
    }

    public static void nw(String tag, String msg, boolean isError) {
        if (isNetWorkEnable) {
            if (isError) {
                e(tag, TagConstant.TAG_NW, msg);
            } else {
                i(tag, TagConstant.TAG_NW, msg);
            }
        }
    }

    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    private static String getArgsStr(String... args) {
        String ret = "";
        if (args != null && args.length > 0) {
            for (String str : args) {
                ret += str + " ";
            }
        }
        return ret;
    }

    private static void checkInit() throws Exception {
        if (mLog == null) {
            throw new Exception("SDK未初始化");
        }
    }

    public static class TagConstant {
        public static String TAG_APP = "SmartSdk";
        public static String TAG_LC = "-----LifeCycle-----";
        public static String TAG_AM = "-----AtyManager-----";
        public static String TAG_NW = "-----NetWork-----";
        public static String TAG_FW = "-----FrameWork-----";
        public static String TAG_DEF = "";
        public static String TAG_REQ = "Request\n";
        public static String TAG_RSP = "Response\n";
    }
}