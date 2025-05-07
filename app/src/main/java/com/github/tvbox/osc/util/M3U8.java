package com.github.tvbox.osc.util;

import static com.github.tvbox.osc.util.RegexUtils.getPattern;

import androidx.media3.common.util.UriUtil;

import com.github.tvbox.osc.base.App;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author asdfgh、FongMi
 * 参考 FongMi/TV 的代码
 * https://github.com/FongMi/TV
 */
public class M3U8 {
    private static final String TAG_DISCONTINUITY = "#EXT-X-DISCONTINUITY";
    private static final String TAG_MEDIA_DURATION = "#EXTINF";
    private static final String TAG_ENDLIST = "#EXT-X-ENDLIST";
    private static final String TAG_KEY = "#EXT-X-KEY";

    private static final Pattern REGEX_X_DISCONTINUITY = Pattern.compile("#EXT-X-DISCONTINUITY[\\s\\S]*?(?=#EXT-X-DISCONTINUITY|$)");
    private static final Pattern REGEX_MEDIA_DURATION = Pattern.compile(TAG_MEDIA_DURATION + ":([\\d\\.]+)\\b");
    private static final Pattern REGEX_URI = Pattern.compile("URI=\"(.+?)\"");
    public static int currentAdCount;

    public static boolean isAd(String regex) {
        return regex.contains(TAG_DISCONTINUITY) || regex.contains(TAG_MEDIA_DURATION) || regex.contains(TAG_ENDLIST) || regex.contains(TAG_KEY) || M3U8.isDouble(regex);
    }

    public static String purify(String tsUrlPre, String m3u8content) {
        long start = System.currentTimeMillis();
        currentAdCount = 0;
        if (null == m3u8content || m3u8content.length() == 0) return null;
        if (!m3u8content.startsWith("#EXTM3U")) return null;
        String result = removeMinorityUrl(tsUrlPre, m3u8content);
        if (result != null) return result;
        result = get(tsUrlPre, m3u8content);
        long cost = System.currentTimeMillis() - start;
        LOG.i("echo-fixAdM3u8Ai 耗时：" + cost + "ms");
        return result;
    }

    private static double maxPercent(HashMap<String, Integer> preUrlMap) {
        int maxTimes = 0, totalTimes = 0;
        for (Map.Entry<String, Integer> entry : preUrlMap.entrySet()) {
            if (entry.getValue() > maxTimes) {
                maxTimes = entry.getValue();
            }
            totalTimes += entry.getValue();
        }
        return  maxTimes*1.0 / (totalTimes*1.0);
    }
    /**
     * @author asdfgh
     * <a href="https://github.com/asdfgh"> asdfgh </a>
     */

    private static int timesNoAd = 15;  //出现超过多少次的域名不认为是广告
    private static String removeMinorityUrl(String tsUrlPre, String m3u8content) {
        String linesplit = "\n";
        if (m3u8content.contains("\r\n"))
            linesplit = "\r\n";
        String[] lines = m3u8content.split(linesplit);

        // 第一阶段：按去掉文件后缀后统计各前缀出现次数
        HashMap<String, Integer> preUrlMap = new HashMap<>();
        for (String line : lines) {
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            int ilast = line.lastIndexOf('.');
            if (ilast <= 4) {
                continue;
            }
            String preUrl = line.substring(0, ilast - 4);
            Integer cnt = preUrlMap.get(preUrl);
            if (cnt != null) {
                preUrlMap.put(preUrl, cnt + 1);
            } else {
                preUrlMap.put(preUrl, 1);
            }
        }
        if (preUrlMap.size() <= 1) return null;
        boolean domainFiltering = false;
        if (maxPercent(preUrlMap) < 0.8) {
            //尝试判断域名，取同域名最多的链接，其它域名当作广告去除
            preUrlMap.clear();
            for (String line : lines) {
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                if (!line.startsWith("http://") && !line.startsWith("https://")) {
                    return null;
                }
                int ifirst = line.indexOf('/', 9); // skip http:// 或 https://
                if (ifirst <= 0) {
                    continue;
                }
                String preUrl = line.substring(0, ifirst);
                Integer cnt = preUrlMap.get(preUrl);
                if (cnt != null) {
                    preUrlMap.put(preUrl, cnt + 1);
                } else {
                    preUrlMap.put(preUrl, 1);
                }
            }
            if (preUrlMap.size() <= 1) return null;
            if (maxPercent(preUrlMap) < 0.8) {
                return null; //视频非广告片断占比不够大
            }
            boolean allDomainsExceedThreshold = true;
            for (Integer count : preUrlMap.values()) {
                if (count <= 15) {
                    allDomainsExceedThreshold = false;
                    break;
                }
            }
            if (allDomainsExceedThreshold) return null;
            domainFiltering = true;
        }

        // 找出出现次数最多的 key（文件前缀或域名均适用）
        int maxTimes = 0;
        String maxTimesPreUrl = "";
        for (Map.Entry<String, Integer> entry : preUrlMap.entrySet()) {
            if (entry.getValue() > maxTimes) {
                maxTimesPreUrl = entry.getKey();
                maxTimes = entry.getValue();
            }
        }
        if (maxTimes == 0) return null;

        boolean dealedExtXKey = false;
        for (int i = 0; i < lines.length; ++i) {
            // 处理解密KEY的绝对路径拼接
            if (!dealedExtXKey && lines[i].startsWith("#EXT-X-KEY")) {
                String keyUrl = "";
                int start = lines[i].indexOf("URI=\"");
                if (start != -1) {
                    start += "URI=\"".length();
                    int end = lines[i].indexOf("\"", start);
                    if (end != -1) {
                        keyUrl = lines[i].substring(start, end);
                    }
                    if (!keyUrl.startsWith("http://") && !keyUrl.startsWith("https://")) {
                        String newKeyUrl;
                        if (keyUrl.charAt(0) == '/') {
                            int ifirst = tsUrlPre.indexOf('/', 9);//skip https://, http://
                            newKeyUrl = tsUrlPre.substring(0, ifirst) + keyUrl;
                        } else
                            newKeyUrl = tsUrlPre + keyUrl;
                        lines[i] = lines[i].replace("URI=\"" + keyUrl + "\"", "URI=\"" + newKeyUrl + "\"");
                    }
                    dealedExtXKey = true;
                }
            }
            if (lines[i].length() == 0 || lines[i].charAt(0) == '#') {
                continue;
            }
            // 根据判断方式过滤
            if (!domainFiltering) {
                if (lines[i].startsWith(maxTimesPreUrl)) {
                    if (!lines[i].startsWith("http://") && !lines[i].startsWith("https://")) {
                        if (lines[i].charAt(0) == '/') {
                            int ifirst = tsUrlPre.indexOf('/', 9); //skip https://, http://
                            lines[i] = tsUrlPre.substring(0, ifirst) + lines[i];
                        } else
                            lines[i] = tsUrlPre + lines[i];
                    }
                } else {
                    if (i > 0 && lines[i - 1].length() > 0 && lines[i - 1].charAt(0) == '#') {
                        lines[i - 1] = "";
                    }
                    lines[i] = "";
                    currentAdCount+=1;
                }
            } else {
                // 域名过滤模式：先转换为绝对 URL
                String absoluteUrl = lines[i];
                if (!absoluteUrl.startsWith("http://") && !absoluteUrl.startsWith("https://")) {
                    if (absoluteUrl.charAt(0) == '/') {
                        int ifirst = tsUrlPre.indexOf('/', 9);
                        absoluteUrl = tsUrlPre.substring(0, ifirst) + absoluteUrl;
                    } else {
                        absoluteUrl = tsUrlPre + absoluteUrl;
                    }
                }
                // 提取域名部分（http://xxx或https://xxx）
                int ifirst = absoluteUrl.indexOf('/', 9);
                String domain = (ifirst > 0) ? absoluteUrl.substring(0, ifirst) : absoluteUrl;
                // 保留条件：域名等于出现次数最多的，或者该域名出现次数超过timesNoAd次
                Integer cnt = preUrlMap.get(domain);
                if (domain.equals(maxTimesPreUrl) || (cnt != null && cnt > timesNoAd)) {
                    lines[i] = absoluteUrl;
                } else {
                    if (i > 0 && lines[i - 1].length() > 0 && lines[i - 1].charAt(0) == '#') {
                        lines[i - 1] = "";
                    }
                    lines[i] = "";
                    currentAdCount+=1;
                }
            }
        }
//        ToastHelper.showToast(App.getInstance(), "已移除视频广告");
        return String.join(linesplit, lines);
    }

    private static String get(String tsUrlPre, String m3u8Content) {
        m3u8Content = m3u8Content.replaceAll("\r\n", "\n");
        StringBuilder sb = new StringBuilder();
        for (String line : m3u8Content.split("\n")) sb.append(shouldResolve(line) ? resolve(tsUrlPre, line) : line).append("\n");
        List<String> ads = getRegex(tsUrlPre);
        if (ads == null || ads.isEmpty()) return null;
        return clean(sb.toString(), ads);
    }

    private static List<String> getRegex(String tsUrlPre) {
        HashMap<String, ArrayList<String>> hostsRegex = VideoParseRuler.getHostsRegex();
        List<String> list = new ArrayList<>();
        for (String host : hostsRegex.keySet()) {
            if (!tsUrlPre.contains(host)) continue;
            if (hostsRegex.get(host) == null) continue;
            list = hostsRegex.get(host);
            break;
        }
        return list;
    }

    private static String clean(String line, List<String> ads) {
        boolean scan = false;
        for (String ad : ads) {
            if (ad.contains(TAG_DISCONTINUITY) || ad.contains(TAG_MEDIA_DURATION)) line = scanAd(line,ad);
            else if (isDouble(ad)) scan = true;
        }
        return scan ? scan(line, ads) : line;
    }

    private static String scanAd(String line,String TAG_AD) {
        Matcher m1 = getPattern(TAG_AD).matcher(line);
        List<String> needRemoveAd = new ArrayList<>();
        while (m1.find()) {
            String group = m1.group();
            String groupCleaned = group.replace(TAG_ENDLIST, "");
            Matcher m2 = REGEX_MEDIA_DURATION.matcher(group);
            int tCount = 0;
            while (m2.find()) {
                tCount+=1;
            }
            needRemoveAd.add(groupCleaned);
            currentAdCount+=tCount;
        }
        for (String rem : needRemoveAd) {
            line = line.replace(rem, "");
        }
        return line;
    }
    private static String scan(String line, List<String> ads) {
        Matcher m1 = REGEX_X_DISCONTINUITY.matcher(line);
        List<String> needRemoveAd = new ArrayList<>();
        while (m1.find()) {
            String group = m1.group();
            String groupCleaned = group.replace(TAG_ENDLIST, "");
            Matcher m2 = REGEX_MEDIA_DURATION.matcher(group);
            BigDecimal ft = BigDecimal.ZERO,lt = BigDecimal.ZERO,t = BigDecimal.ZERO;
            int tCount = 0;
            while (m2.find()) {
                if (ft.equals(BigDecimal.ZERO))ft = new BigDecimal(m2.group(1));
                lt = new BigDecimal(m2.group(1));
                t = t.add(lt);
                tCount+=1;
            }

            String ftStr = ft.toString(),ltStr = lt.toString(),tStr = t.toString();
            for (String ad : ads) {
                if (ad.startsWith("-")) {
                    String adClean = ad.substring(1);
                    //匹配最后一条切片
                    if (ltStr.startsWith(adClean)) {
                        needRemoveAd.add(groupCleaned);
                        currentAdCount+=tCount;
                        break;
                    }
                } else {
                    //匹配第一条切片或广告切片总时长
                    if (ftStr.startsWith(ad) || tStr.startsWith(ad)) {
                        needRemoveAd.add(groupCleaned);
                        currentAdCount+=tCount;
                        break;
                    }
                }
            }
        }
        for (String rem : needRemoveAd) {
            line = line.replace(rem, "");
        }
        return line;
    }

    private static boolean isDouble(String ad) {
        try {
            return Double.parseDouble(ad) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean shouldResolve(String line) {
        return (!line.startsWith("#") && !line.startsWith("http")) || line.startsWith(TAG_KEY);
    }

    private static String resolve(String base, String line) {
        if (line.startsWith(TAG_KEY)) {
            Matcher matcher = REGEX_URI.matcher(line);
            String value = matcher.find() ? matcher.group(1) : null;
            return value == null ? line : line.replace(value, UriUtil.resolve(base, value));
        } else {
            return UriUtil.resolve(base, line);
        }
    }
}
