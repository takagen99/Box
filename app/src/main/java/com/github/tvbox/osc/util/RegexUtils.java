package com.github.tvbox.osc.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexUtils {

    private static final Map<String, Pattern> patternCache = new HashMap<>();
    public static Pattern getPattern(String regex) {
        Pattern pattern = patternCache.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            patternCache.put(regex, pattern);
        }
        return pattern;
    }

    public static Pattern getPattern(String regex,int flag) {
        Pattern pattern = patternCache.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex,flag);
            patternCache.put(regex, pattern);
        }
        return pattern;
    }
}
