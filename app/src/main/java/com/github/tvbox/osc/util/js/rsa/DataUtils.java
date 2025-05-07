package com.github.tvbox.osc.util.js.rsa;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 数据工具类
 */
public class DataUtils {

    /**
     * 将 Base64 字符串 解码成 字节数组
     */
    public static byte[] base64Decode(String data) {
        return Base64.decode(data.getBytes(), Base64.NO_WRAP);
    }

   /**
     * 将 字节数组 转换成 Base64 编码
     */
    public static String base64Encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    /**
     * 将字节数组转换成 int 类型
     */
    public static int byte2Int(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer.getInt();
    }

    /**
     * 将 int 转换成 byte 数组
     */
    public static byte[] int2byte(int data) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(data);
        return buffer.array();
    }
}