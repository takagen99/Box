package com.github.tvbox.osc.util.js;

import android.text.TextUtils;
import com.github.tvbox.osc.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlParser {
    private static String pdfh_html = "";
    private static String pdfa_html = "";
    private static final Pattern p = Pattern.compile("url\\((.*?)\\)", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern NOADD_INDEX = Pattern.compile(":eq|:lt|:gt|:first|:last|^body$|^#"); // 不自动加eq下标索引
    private static final Pattern URLJOIN_ATTR = Pattern.compile("(url|src|href|-original|-src|-play|-url|style)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE); // 需要自动urljoin的属性
    private static final Pattern SPECIAL_URL = Pattern.compile("^(ftp|magnet|thunder|ws):", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE); // 过滤特殊链接,不走urlJoin
    private static Document pdfh_doc = null;
    private static Document pdfa_doc = null;

    public static String joinUrl(String parent, String child) {
        if (StringUtils.isEmpty(parent)) {
            return child;
        }

        URL url;
        String q = parent;
        try {
            url = new URL(new URL(parent), child);
            q = url.toExternalForm();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //        if (q.contains("#")) {
        //            q = q.replaceAll("^(.+?)#.*?$", "$1");
        //        }
        return q;
    }

    public static class Painfo {
        public String nparse_rule;
        public int nparse_index;
        public List<String> excludes;
    }

    private static Painfo getParseInfo(String nparse) {
        /*
         根据传入的单规则获取 parse规则，索引位置,排除列表  -- 可以用于剔除元素,支持多个，按标签剔除，按id剔除等操作
         :param nparse:
         :return:*/
        Painfo painfo = new Painfo();
        //List<String> excludes = new ArrayList<>();  //定义排除列表默认值为空
        //int nparse_index;  //定义位置索引默认值为0
        painfo.nparse_rule = nparse; //定义规则默认值为本身
        if (nparse.contains(":eq")) {
            painfo.nparse_rule = nparse.split(":")[0];
            String nparse_pos = nparse.split(":")[1];

            if (painfo.nparse_rule.contains("--")) {
                String[] rules = painfo.nparse_rule.split("--");
                painfo.excludes = new ArrayList<>(Arrays.asList(rules));
                painfo.excludes.remove(0);
                painfo.nparse_rule = rules[0];
            } else if (nparse_pos.contains("--")) {
                String[] rules = nparse_pos.split("--");
                painfo.excludes = new ArrayList<>(Arrays.asList(rules));
                painfo.excludes.remove(0);
                nparse_pos = rules[0];
            }

            try {
                painfo.nparse_index = Integer.parseInt(nparse_pos.replace("eq(", "")
				.replace(")", ""));
            } catch (Exception e1) {
                painfo.nparse_index = 0;
            }
        } else {
            if (nparse.contains("--")) {
                String[] rules = painfo.nparse_rule.split("--");
                painfo.excludes = new ArrayList<>(Arrays.asList(rules));
                painfo.excludes.remove(0);
                painfo.nparse_rule = rules[0];
            }
        }
        return painfo;
    }

    public static boolean isIndex(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        for (String str2 : new String[]{
            ":eq", ":lt", ":gt", ":first", ":last", "body", "#"
        }) {
            if (str.contains(str2)) {
                if (str2.equals("body") || str2.equals("#")) {
                    return str.startsWith(str2);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isUrl(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        for (String str2 : new String[]{
            "url", "src", "href", "-original", "-play"
		}) {

            if (str.contains(str2)) {
                return true;
            }
        }
        return false;
    }

    private static String parseHikerToJq(String parse, boolean first) {
        /*
         海阔解析表达式转原生表达式,自动补eq,如果传了first就最后一个也取eq(0)
        :param parse:
        :param first:
        :return:
        */
        // 不自动加eq下标索引
        if (parse.contains("&&")) {
            String[] parses = parse.split("&&"); //带&&的重新拼接
            List < String > new_parses = new ArrayList < > (); //构造新的解析表达式列表
            for (int i = 0; i < parses.length; i++) {
                String[] pss = parses[i].split(" ");
                String ps = pss[pss.length - 1]; //如果分割&&后带空格就取最后一个元素
                Matcher m = NOADD_INDEX.matcher(ps);
                //if (!isIndex(ps)) {
                if (!m.find()) {
                    if (!first && i >= parses.length - 1) { //不传first且遇到最后一个,不用补eq(0)
                        new_parses.add(parses[i]);
                    } else {
                        new_parses.add(parses[i] + ":eq(0)");
                    }
                } else {
                    new_parses.add(parses[i]);
                }
            }
            parse = TextUtils.join(" ", new_parses);
        } else {
            String[] pss = parse.split(" ");
            String ps = pss[pss.length - 1]; //如果分割&&后带空格就取最后一个元素
            Matcher m = NOADD_INDEX.matcher(ps);
            //if (!isIndex(ps) && first) {
            if (!m.find() && first) {
                parse = parse + ":eq(0)";
            }
        }
        return parse;
    }

    public static String parseDomForUrl(String html, String rule, String add_url) {
        if (!pdfh_html.equals(html)) {
            pdfh_html = html;
            pdfh_doc = Jsoup.parse(html);
        }
        Document doc = pdfh_doc;
        if (rule.equals("body&&Text") || rule.equals("Text")) {
            return doc.text();
        } else if (rule.equals("body&&Html") || rule.equals("Html")) {
            return doc.html();
        }
        String option = "";
        if (rule.contains("&&")) {
            String[] rs = rule.split("&&");
            option = rs[rs.length - 1];
            List<String> excludes = new ArrayList<>(Arrays.asList(rs));
            excludes.remove(rs.length - 1);
            rule = TextUtils.join("&&", excludes);
        }
        rule = parseHikerToJq(rule, true);
        String[] parses = rule.split(" ");
        Elements ret = new Elements();
        for (String nparse : parses) {
            ret = parseOneRule(doc, nparse, ret);
            if (ret.isEmpty()) {
                return "";
            }
        }
        String result;
        if (StringUtils.isNotEmpty(option)) {
            if (option.equals("Text")) {
                result = ret.text();
            } else if (option.equals("Html")) {
                result = ret.html();
            } else {
                result = ret.attr(option);
                if (option.toLowerCase()
                    .contains("style") && result.contains("url(")) {
                    Matcher m = p.matcher(result);
                    if (m.find()) {
                        result = m.group(1);
                    }
                    if (StringUtils.isNotEmpty(result)) {
                        result = result.replaceAll("^['|\"](.*)['|\"]$", "$1");
                    }
                }
                if (StringUtils.isNotEmpty(result) && StringUtils.isNotEmpty(add_url)) {
                    // 需要自动urljoin的属性
                    Matcher m = URLJOIN_ATTR.matcher(option);
                    Matcher n = SPECIAL_URL.matcher(result);
                    //if (isUrl(option)) {
                    if (m.find() && !n.find()) {
                        if (result.contains("http")) {
                            result = result.substring(result.indexOf("http"));
                        } else {
                            result = joinUrl(add_url, result);
                        }
                    }
                }
            }
        } else {
            result = ret.outerHtml();
        }
        return result;

    }

    public static List < String > parseDomForArray(String html, String rule) {
        if (!pdfa_html.equals(html)) {
            pdfa_html = html;
            pdfa_doc = Jsoup.parse(html);
        }
        Document doc = pdfa_doc;
        rule = parseHikerToJq(rule, false);
        String[] parses = rule.split(" ");
        Elements ret = new Elements();
        for (String pars: parses) {
            ret = parseOneRule(doc, pars, ret);
            if (ret.isEmpty()) {
                return new ArrayList < > ();
            }
        }

        List < String > eleHtml = new ArrayList < > ();
        for (int i = 0; i < ret.size(); i++) {
            Element element1 = ret.get(i);
            eleHtml.add(element1.outerHtml());
        }
        return eleHtml;
    }

    private static Elements parseOneRule(Document doc, String nparse, Elements ret) {
        Painfo painfo = getParseInfo(nparse);
        if (ret.isEmpty()) {
            ret = doc.select(painfo.nparse_rule);
        } else {
            ret = ret.select(painfo.nparse_rule);
        }

        if (nparse.contains(":eq")) {
            if (painfo.nparse_index < 0) {
                ret = ret.eq(ret.size() + painfo.nparse_index);
            } else {
                ret = ret.eq(painfo.nparse_index);
            }
        }

        if (painfo.excludes != null && !ret.isEmpty()) {
            ret = ret.clone(); //克隆一个, 免得直接remove会影响doc的缓存
            for (int i = 0; i < painfo.excludes.size(); i++) {
                ret.select(painfo.excludes.get(i))
                    .remove();
            }
        }
        return ret;
    }

    public static List < String > parseDomForList(String html, String p1, String list_text, String list_url, String add_url) {
        if (!pdfa_html.equals(html)) {
            pdfa_html = html;
            pdfa_doc = Jsoup.parse(html);
        }
        Document doc = pdfa_doc;
        p1 = parseHikerToJq(p1, false);
        String[] parses = p1.split(" ");
        Elements ret = new Elements();
        for (String pars: parses) {
            ret = parseOneRule(doc, pars, ret);
            if (ret.isEmpty()) {
                return new ArrayList < > ();
            }
        }
        List < String > new_vod_list = new ArrayList < > ();
        for (int i = 0; i < ret.size(); i++) {
            String it = ret.get(i)
                .outerHtml();
            new_vod_list.add(parseDomForUrl(it, list_text, "")
                .trim() + '$' + parseDomForUrl(it, list_url, add_url));
        }
        return new_vod_list;
    }
}
