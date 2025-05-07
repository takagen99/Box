package com.github.tvbox.osc.util.js;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.js.rsa.RSAEncrypt;
import com.whl.quickjs.wrapper.ContextSetter;
import com.whl.quickjs.wrapper.Function;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.JSUtils;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Global {
    private QuickJSContext runtime;
    public ExecutorService executor;
    private final Timer timer;

    public Global(ExecutorService executor) {
        this.executor = executor;
        this.timer = new Timer();
    }

    @Keep
    @Function
    public String getProxy(boolean local) {
        return ControlManager.get().getAddress(local) + "proxy?do=js";
    }

    @Keep
    @Function
    public String js2Proxy(Boolean dynamic, Integer siteType, String siteKey, String url, JSObject headers) {
        return getProxy(true) + "&from=catvod" + "&siteType=" + siteType + "&siteKey=" + siteKey + "&header=" + URLEncoder.encode(headers.toJsonString()) + "&url=" + URLEncoder.encode(url);
    }

    @Keep
    @Function
    public String joinUrl(String parent, String child) {
        return HtmlParser.joinUrl(parent, child);
    }

    @Keep
    @Function
    public String pd(String html, String rule, String add_url) {
        return HtmlParser.parseDomForUrl(html, rule, add_url);
    }

    @Keep
    @Function
    public String pdfh(String html, String rule) {
        return HtmlParser.parseDomForUrl(html, rule, "");
    }

    @Keep
    @Function
    public JSArray pdfa(String html, String rule) {

        return new JSUtils<String>().toArray(runtime, HtmlParser.parseDomForArray(html, rule));
    }

    @Keep
    @Function
    public JSArray pdfla(String html, String p1, String list_text, String list_url, String add_url) {
        return new JSUtils<String>().toArray(runtime, HtmlParser.parseDomForList(html, p1, list_text, list_url, add_url));
    }

    @Keep
    @Function
    public String s2t(String text) {
        try {
            return Trans.s2t(false, text);
        } catch (Exception e) {
            return "";
        }
    }

    @Keep
    @Function
    public String t2s(String text) {
        try {
            return Trans.t2s(false, text);
        } catch (Exception e) {
            return "";
        }
    }

    @Keep
    @Function
    public String aesX(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        String result = Crypto.aes(mode, encrypt, input, inBase64, key, iv, outBase64);
        //LOG.e("aesX",String.format("mode:%s\nencrypt:%s\ninBase64:%s\noutBase64:%s\nkey:%s\niv:%s\ninput:\n%s\nresult:\n%s", mode, encrypt, inBase64, outBase64, key, iv, input, result));
        return result;
    }

    @Keep
    @Function
    public String rsaX(String mode, boolean pub, boolean encrypt, String input, boolean inBase64, String key, boolean outBase64) {
        String result = Crypto.rsa(pub, encrypt, input, inBase64, key, outBase64);
        //LOG.e("aesX",String.format("mode:%s\npub:%s\nencrypt:%s\ninBase64:%s\noutBase64:%s\nkey:\n%s\ninput:\n%s\nresult:\n%s", mode, pub, encrypt, inBase64, outBase64, key, input, result));
        return result;
    }

    @Keep
    @Function
    public String rsaEncrypt(String data, String key) {
        return  rsaEncrypt(data, key, null);
    }
    /**
     * RSA 加密
     *
     * @param data    要加密的数据
     * @param key     密钥，type 为 1 则公钥，type 为 2 则私钥
     * @param options 加密的选项，包含加密配置和类型：{ config: "RSA/ECB/PKCS1Padding", type: 1, long: 1 }
     *                config 加密的配置，默认 RSA/ECB/PKCS1Padding （可选）
     *                type 加密类型，1 公钥加密 私钥解密，2 私钥加密 公钥解密（可选，默认 1）
     *                long 加密方式，1 普通，2 分段（可选，默认 1）
     *                block 分段长度，false 固定117，true 自动（可选，默认 true ）
     * @return 返回加密结果
     */

    @Keep
    @Function
    public String rsaEncrypt(String data, String key, JSObject options) {
        int mLong = 1;
        int mType = 1;
        boolean mBlock = true;
        String mConfig = null;
        if (options != null) {
            JSONObject op = options.toJsonObject();
            if (op.has("config")) {
                try {
                    mConfig = (String) op.get("config");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.has("type")) {
                try {
                    mType = ((Double) op.get("type")).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.has("long")) {
                try {
                    mLong = ((Double) op.get("long")).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.has("block")) {
                try {
                    mBlock = (Boolean) op.get("block");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            switch (mType) {
                case 1:
                    if (mConfig != null) {
                        return RSAEncrypt.encryptByPublicKey(data, key, mConfig, mLong, mBlock);
                    } else {
                        return RSAEncrypt.encryptByPublicKey(data, key, mLong, mBlock);
                    }
                case 2:
                    if (mConfig != null) {
                        return RSAEncrypt.encryptByPrivateKey(data, key, mConfig, mLong, mBlock);
                    } else {
                        return RSAEncrypt.encryptByPrivateKey(data, key, mLong, mBlock);
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    @Keep
    @Function
    public String rsaDecrypt(String encryptBase64Data, String key) {
        return  rsaDecrypt(encryptBase64Data, key, null);
    }

    /**
     * RSA 解密
     *
     * @param encryptBase64Data 加密后的 Base64 字符串
     * @param key               密钥，type 为 1 则私钥，type 为 2 则公钥
     * @param options           解密的选项，包含解密配置和类型：{ config: "RSA/ECB/PKCS1Padding", type: 1, long: 1 }
     *                          config 解密的配置，默认 RSA/ECB/PKCS1Padding （可选）
     *                          type 解密类型，1 公钥加密 私钥解密，2 私钥加密 公钥解密（可选，默认 1）
     *                          long 解密方式，1 普通，2 分段（可选，默认 1）
     *                          block 分段长度，false 固定128，true 自动（可选，默认 true ）
     * @return 返回解密结果
     */
    @Keep
    @Function
    public String rsaDecrypt(String encryptBase64Data, String key, JSObject options) {
        int mLong = 1;
        int mType = 1;
        boolean mBlock = true;
        String mConfig = null;
        if (options != null) {
            JSONObject op = options.toJsonObject();
            if (op.has("config")) {
                try {
                    mConfig = (String) op.get("config");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.has("type")) {
                try {
                    mType = ((Double) op.get("type")).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.has("long")) {
                try {
                    mLong = ((Double) op.get("long")).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.has("block")) {
                try {
                    mBlock = (Boolean) op.get("block");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            switch (mType) {
                case 1:
                    if (mConfig != null) {
                        return RSAEncrypt.decryptByPrivateKey(encryptBase64Data, key, mConfig, mLong, mBlock);
                    } else {
                        return RSAEncrypt.decryptByPrivateKey(encryptBase64Data, key, mLong, mBlock);
                    }
                case 2:
                    if (mConfig != null) {
                        return RSAEncrypt.decryptByPublicKey(encryptBase64Data, key, mConfig, mLong, mBlock);
                    } else {
                        return RSAEncrypt.decryptByPublicKey(encryptBase64Data, key, mLong, mBlock);
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private JSObject req(String url, JSObject options) {
        try {
            Req req = Req.objectFrom(options.toJsonObject().toString());
            Response res = Connect.to(url, req).execute();
            return Connect.success(runtime, req, res);
        } catch (Exception e) {
            return Connect.error(runtime);
        }
    }

    @Keep
    @Function
    public JSObject _http(String url, JSObject options) {
        JSFunction complete = options.getJSFunction("complete");
        if (complete == null) return req(url, options);
        Req req = Req.objectFrom(options.toJsonObject().toString());
        Connect.to(url, req).enqueue(getCallback(complete, req));
        return null;
    }

    @Keep
    @Function
    public void setTimeout(JSFunction func, Integer delay) {
        func.hold();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!executor.isShutdown()) executor.submit(() -> {func.call();});
            }
        }, delay);
    }

    private Callback getCallback(JSFunction complete, Req req) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response res) {
                executor.submit(() -> {
                    complete.call(Connect.success(runtime, req, res));
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                executor.submit(() -> {
                    complete.call(Connect.error(runtime));
                });
            }
        };
    }
    @Keep
    // 声明用于依赖注入的 QuickJSContext
    @ContextSetter
    public void setJSContext(QuickJSContext runtime) {
        this.runtime = runtime;
    }

}
