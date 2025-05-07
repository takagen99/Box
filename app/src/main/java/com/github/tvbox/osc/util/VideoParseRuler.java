package com.github.tvbox.osc.util;

import static com.github.tvbox.osc.util.RegexUtils.getPattern;
import android.net.Uri;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class VideoParseRuler {

    private static final HashMap<String, ArrayList<ArrayList<String>>> HOSTS_RULE = new HashMap<>();
    private static final HashMap<String, ArrayList<ArrayList<String>>> HOSTS_FILTER = new HashMap<>();
    private static final HashMap<String, ArrayList<String>> HOSTS_REGEX = new HashMap<>();
    private static final HashMap<String, ArrayList<String>> HOSTS_SCRIPT = new HashMap<>();
    public static void clearRule() {
        HOSTS_RULE.clear();
        HOSTS_FILTER.clear();
        HOSTS_REGEX.clear();
        HOSTS_SCRIPT.clear();   
    }

    public static void addHostRule(String host, ArrayList<String> rule) {
        if (rule == null || rule.size() == 0) return;
        ArrayList<ArrayList<String>> rules = new ArrayList<>();
        if (HOSTS_RULE.get(host) != null && HOSTS_RULE.get(host).size() > 0) {
            rules = HOSTS_RULE.get(host);
        }
        rules.add(rule);
        HOSTS_RULE.put(host, rules);
    }

    public static ArrayList<ArrayList<String>> getHostRules(String host) {
        if (HOSTS_RULE.containsKey(host)) {
            return HOSTS_RULE.get(host);
        }
        return null;
    }

    public static void addHostFilter(String host, ArrayList<String> rule) {
        ArrayList<ArrayList<String>> filters = new ArrayList<>();
        if (HOSTS_FILTER.get(host) != null && HOSTS_FILTER.get(host).size() > 0) {
            filters = HOSTS_FILTER.get(host);
        }
        filters.add(rule);
        HOSTS_FILTER.put(host, filters);
    }

    public static ArrayList<ArrayList<String>> getHostFilters(String host) {
        if (HOSTS_FILTER.containsKey(host)) {
            return HOSTS_FILTER.get(host);
        }
        return null;
    }

    public static void addHostRegex(String host, ArrayList<String> regex) {
        if (regex == null || regex.size() == 0) return;
        ArrayList<String> temp = new ArrayList<>();
        if (HOSTS_REGEX.get(host) != null && HOSTS_REGEX.get(host).size() > 0) temp = HOSTS_REGEX.get(host);
        temp.addAll(regex);
        HOSTS_REGEX.put(host, temp);
    }

    public static HashMap<String, ArrayList<String>> getHostsRegex() {
        return HOSTS_REGEX;
    }

    public static boolean checkIsVideoForParse(String webUrl, String url) {
        try {
            boolean isVideo = DefaultConfig.isVideoFormat(url);
            if (!HOSTS_RULE.isEmpty() && !isVideo && webUrl != null) {
                Uri uri = Uri.parse(webUrl);
                if(getHostRules(uri.getHost()) != null){
                    isVideo = checkVideoForOneHostRules(uri.getHost(), url);
                }else {
                    isVideo = checkVideoForOneHostRules("*", url);
                }
            }
            return isVideo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean checkVideoForOneHostRules(String host, String url) {
        boolean isVideo = false;
        ArrayList<ArrayList<String>> hostRules = getHostRules(host);
        if (hostRules != null && hostRules.size() > 0) {
            boolean isVideoRuleCheck = false;
            for(int i=0; i<hostRules.size(); i++) {
                boolean checkIsVideo = true;
                if (hostRules.get(i) != null && hostRules.get(i).size() > 0) {
                    for(int j=0; j<hostRules.get(i).size(); j++) {
                        Pattern onePattern = getPattern("" + hostRules.get(i).get(j));
                        if (!onePattern.matcher(url).find()) {
                            checkIsVideo = false;
                            break;
                        }
                        LOG.i("VIDEO RULE:" + hostRules.get(i).get(j));
                    }
                } else {
                    checkIsVideo = false;
                }
                if (checkIsVideo) {
                    isVideoRuleCheck = true;
                    break;
                }
            }
            if (isVideoRuleCheck) {
                isVideo = true;
            }
        }
        return isVideo;
    }

    public static boolean isFilter(String webUrl, String url) {
        try {
            boolean isFilter = false;
            if (!HOSTS_FILTER.isEmpty() && webUrl != null) {
                Uri uri = Uri.parse(webUrl);
                if(getHostFilters(uri.getHost()) != null){
                    isFilter = checkIsFilterForOneHostRules(uri.getHost(), url);
                }
            }
            return isFilter;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean checkIsFilterForOneHostRules(String host, String url) {
        boolean isFilter = false;
        ArrayList<ArrayList<String>> hostFilters = getHostFilters(host);
        if (hostFilters != null && hostFilters.size() > 0) {
            boolean isFilterRuleCheck = false;
            for(int i=0; i<hostFilters.size(); i++) {
                boolean checkIsFilter = true;
                if (hostFilters.get(i) != null && hostFilters.get(i).size() > 0) {
                    for(int j=0; j<hostFilters.get(i).size(); j++) {
                        Pattern onePattern = getPattern("" + hostFilters.get(i).get(j));
                        if (!onePattern.matcher(url).find()) {
                            checkIsFilter = false;
                            break;
                        }
                        LOG.i("FILTER RULE:" + hostFilters.get(i).get(j));
                    }
                } else {
                    checkIsFilter = false;
                }
                if (checkIsFilter) {
                    isFilterRuleCheck = true;
                    break;
                }
            }
            if (isFilterRuleCheck) {
                isFilter = true;
            }
        }
        return isFilter;
    }
 
    public static void addHostScript(String host, ArrayList<String> script) {
        if (script == null || script.size() == 0) return;
        ArrayList<String> temp = new ArrayList<>();
        if (HOSTS_SCRIPT.get(host) != null && HOSTS_SCRIPT.get(host).size() > 0) temp = HOSTS_SCRIPT.get(host);
        assert temp != null;
        temp.addAll(script);
        HOSTS_SCRIPT.put(host, temp);
    }

    public static String getHostScript(String url) {
        for (Map.Entry<String, ArrayList<String>> entry : HOSTS_SCRIPT.entrySet()) {
            String host = entry.getKey();
            if (url.contains(host)) {
                List<String> list = entry.getValue();
                if (list != null && !list.isEmpty()) {
                    return list.get(0);
                }
            }
        }
        return "";
    }
}
