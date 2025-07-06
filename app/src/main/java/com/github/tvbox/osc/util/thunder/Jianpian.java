package com.github.tvbox.osc.util.thunder;

import android.net.Uri;
import android.text.TextUtils;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.LocalIPAddress;
import com.p2p.P2PClass;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;


public class Jianpian {

    public static String JPUrlDec(String url) {
        if (App.getp2p() != null) {
            try {
                String decode = URLDecoder.decode(url, "UTF-8");
                String[] split = decode.split("\\|");
                String replace = split[0].replace("xg://", "ftp://");
                if (replace.contains("xgplay://")) {
                    replace = split[0].replace("xgplay://", "ftp://");
                }
                if (!TextUtils.isEmpty(App.burl)) {
                    App.getp2p().P2Pdoxpause(App.burl.getBytes("GBK"));
                    App.getp2p().P2Pdoxdel(App.burl.getBytes("GBK"));
                }
                App.burl = replace;
                App.getp2p().P2Pdoxstart(replace.getBytes("GBK"));
                App.getp2p().P2Pdoxadd(replace.getBytes("GBK"));
                return "http://" + LocalIPAddress.getIP(App.getInstance()) + ":" + P2PClass.port + "/" + URLEncoder.encode(Uri.parse(replace).getLastPathSegment(), "GBK");
            } catch (Exception e) {
                return e.getLocalizedMessage();
            }
        } else {
            return "";
        }
    }

    public static void finish() {
        if (!TextUtils.isEmpty(App.burl) && App.getp2p() != null) {
            try {
                App.getp2p().P2Pdoxpause(App.burl.getBytes("GBK"));
                App.getp2p().P2Pdoxdel(App.burl.getBytes("GBK"));
                App.burl = "";
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public static Boolean isJpUrl(String url) {
        return url.startsWith("tvbox-xg:") || (Thunder.isFtp(url) && url.contains("gbl.114s"));
    }
}
