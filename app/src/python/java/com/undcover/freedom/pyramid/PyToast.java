package com.undcover.freedom.pyramid;

import android.content.Context;
import android.widget.Toast;


/**
 * Created by UndCover on 16/9/7.
 */
public class PyToast {
    private static Toast innerToast;
    private static Context mContext;
    private static PyToast sInstance;


    public static void init(Context context) {
        mContext = context;
    }

    /**
     * 在Application中 用于初始化
     *
     * @return
     */
    public static PyToast getInstance() {
        if (sInstance == null) {
            synchronized (PyToast.class) {
                if (sInstance == null) {
                    sInstance = new PyToast();
                }
            }
        }
        return sInstance;
    }

    public static void showCancelableToast(String msg) {
        showCancelableToast(msg, Toast.LENGTH_SHORT);
    }

    /**
     * 快速显示Toast,无需排队等待
     *
     * @param msg
     * @param duration
     */
    public static void showCancelableToast(String msg, int duration) {
        if (innerToast != null) {
            innerToast.cancel();
        }

        innerToast = Toast.makeText(mContext, msg, duration);
        innerToast.show();
    }

    public static void showMessage(String msg, int duration) {
        Toast.makeText(mContext, msg, duration).show();
    }
}