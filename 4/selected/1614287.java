package cn.jsprun.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import cn.jsprun.dao.DataBaseDao;

public final class Cache {

    private String tablepre = "jrun_";

    private String path = null;

    private String HTTP_HOST = null;

    private DataBaseDao dataBaseDao = (DataBaseDao) BeanFactory.getBean("dataBaseDao");

    private DataParse dataParse = (DataParse) BeanFactory.getBean("dataParse");

    public Cache(String rootPath) {
        path = rootPath;
    }

    public Cache(String rootPath, HttpServletRequest request) {
        path = rootPath;
        int port = request.getServerPort();
        String http_host = request.getServerName();
        if (port > 0) {
            http_host = http_host.concat(":" + port);
        }
        HTTP_HOST = http_host;
    }

    static String JSPRUN_KERNEL_VERSION = "6.0.0";

    static String JSPRUN_KERNEL_RELEASE = "200806028";

    static Map<String, String> cachescript = new HashMap<String, String>();

    static {
        cachescript.put("settings", "settings");
        cachescript.put("admingroup", "admingroups");
        cachescript.put("usergroup", "usergroups");
        cachescript.put("archiver", "advs_archiver");
        cachescript.put("register", "advs_register");
        cachescript.put("forums", "forums");
        cachescript.put("plugins", "plugins");
        cachescript.put("style", "styles");
        cachescript.put("faqs", "faqs");
        cachescript.put("icons", "icons");
        cachescript.put("secqaa", "secqaa");
        cachescript.put("medals", "medals");
        cachescript.put("ranks", "ranks");
        cachescript.put("censor", "censor");
        cachescript.put("google", "google");
        cachescript.put("baidu", "baidu");
        cachescript.put("index", "announcements,forumlinks,advs_index,onlinelist,tags_index,birthdays_index");
        cachescript.put("forumdisplay", "announcements,announcements_forum,globalstick,onlinelist,advs_forumdisplay");
        cachescript.put("viewthread", "announcements,ranks,bbcodes,advs_viewthread,tags_viewthread");
        cachescript.put("post", "bbcodes_display,smilies_display,smilies");
        cachescript.put("profilefields", "profilefields");
        cachescript.put("threadtypes", "threadtypes");
        cachescript.put("smilies_var", "smilies_var");
    }

    public boolean updatecache() throws Exception {
        return updatecache(null);
    }

    public boolean updatecache(String cachename) throws Exception {
        Map<String, String> settings = ForumInit.settings;
        if (cachename != null) {
            String cname = cachescript.get(cachename);
            if (cname != null && !cname.equals("")) {
                setcache(cname, cachename, null, settings);
            }
        } else {
            Set<String> keys = cachescript.keySet();
            for (String key : keys) {
                if (!key.equals("google") || !key.equals("baidu")) {
                    String cname = cachescript.get(key);
                    setcache(cname, key, null, settings);
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void setcache(String cname, String cachename, String prefix, Map<String, String> settings) throws Exception {
        prefix = prefix != null ? prefix : "cache_";
        boolean flag = false;
        String[] cnames = cname.split(",");
        for (String obj : cnames) {
            String cols = null;
            String table = null;
            String sql = null;
            String conditions = null;
            if ("settings".equals(obj)) {
                conditions = "WHERE variable NOT IN ('siteuniqueid', 'mastermobile', 'closedreason', 'creditsnotify', 'backupdir', 'custombackup','maxonlines', 'newsletter')";
            } else if ("forumlinks".equals(obj) || "onlinelist".equals(obj)) {
                conditions = "ORDER BY displayorder";
            } else if ("forums".equals(obj)) {
                Map<String, Map<String, String>> datas = new HashMap<String, Map<String, String>>();
                List<Map<String, String>> forumList = dataBaseDao.executeQuery("SELECT f.fid, f.type, f.name, f.fup, ff.viewperm FROM " + tablepre + "forums f LEFT JOIN " + tablepre + "forumfields ff ON ff.fid=f.fid LEFT JOIN " + tablepre + "access a ON a.fid=f.fid AND a.allowview='1' WHERE f.status>0 AND f.type<>'group' ORDER BY f.type, f.displayorder");
                if (forumList != null && forumList.size() > 0) {
                    Map<String, Map<String, String>> forums = new HashMap<String, Map<String, String>>();
                    Map<String, Map<String, String>> subs = new HashMap<String, Map<String, String>>();
                    for (Map<String, String> forum : forumList) {
                        forum.put("name", Common.strip_tags(forum.get("name")));
                        String type = forum.get("type");
                        if (type.equals("forum")) {
                            forums.put(forum.get("fid"), forum);
                        } else if (type.equals("sub")) {
                            Map<String, String> upforum = forums.get(forum.get("fup"));
                            if (upforum != null) {
                                if (upforum.get("hasSub") == null) {
                                    upforum.put("hasSub", "true");
                                }
                            }
                            subs.put(forum.get("fid"), forum);
                        }
                    }
                    if (forums != null && forums.size() > 0) {
                        Set<String> forumids = forums.keySet();
                        for (String forumid : forumids) {
                            Map<String, String> forum = forums.get(forumid);
                            boolean hasSub = "true".equals(forum.get("hasSub"));
                            forum.remove("hasSub");
                            datas.put(forumid, forum);
                            if (hasSub) {
                                Set<String> subids = subs.keySet();
                                for (String subid : subids) {
                                    Map<String, String> sub = subs.get(subid);
                                    if (sub.get("fup").equals(forumid)) {
                                        datas.put(subid, sub);
                                    }
                                }
                            }
                        }
                    }
                    forums = null;
                    subs = null;
                }
                forumList = null;
                Map<String, String> data = new HashMap<String, String>();
                data.put("forums", dataParse.combinationChar(datas));
                datas = null;
                writeToCacheFile(prefix + cachename, arrayeval(cachename, data), "", flag);
                data = null;
                break;
            } else if ("plugins".equals(obj)) {
                List<Map<String, String>> plugins = dataBaseDao.executeQuery("SELECT pluginid, available, adminid, name, identifier, datatables, directory, copyright, modules FROM " + tablepre + "plugins");
                if (plugins != null && plugins.size() > 0) {
                    for (Map<String, String> plugin : plugins) {
                        List<Map<String, String>> queryvars = dataBaseDao.executeQuery("SELECT variable, value FROM " + tablepre + "pluginvars WHERE pluginid='" + plugin.get("pluginid") + "'");
                        if (queryvars != null && queryvars.size() > 0) {
                            Map<String, String> vars = new HashMap<String, String>();
                            for (Map<String, String> var : queryvars) {
                                vars.put(var.get("variable"), var.get("value"));
                            }
                            plugin.put("vars", dataParse.combinationChar(vars));
                        }
                        writeToCacheFile("plugin_" + plugin.get("identifier"), arrayeval(plugin.get("identifier"), plugin), "", flag);
                    }
                }
                break;
            } else if (obj.startsWith("tags_")) {
                table = "tags";
                int taglimit = Common.toDigit("viewthread".equals(obj.substring(5)) ? settings.get("viewthreadtags") : settings.get("hottags"));
                cols = "tagname, total";
                conditions = "WHERE closed=0 ORDER BY total DESC LIMIT " + taglimit;
            } else if ("announcements".equals(obj)) {
                int timestamp = Common.time();
                conditions = "WHERE starttime<='" + timestamp + "' AND (endtime>='" + timestamp + "' OR endtime='0') ORDER BY displayorder, starttime DESC, id DESC";
            } else if ("google".equals(obj)) {
                table = "settings";
                conditions = "WHERE variable = 'google'";
            } else if ("baidu".equals(obj)) {
                table = "settings";
                conditions = "WHERE variable = 'baidu'";
            } else if ("birthdays_index".equals(obj)) {
                table = "members";
                int timeoffset = (int) (Float.valueOf(settings.get("timeoffset")) * 3600);
                conditions = "WHERE RIGHT(bday, 5)='" + Common.gmdate("MM-dd", Common.time() + timeoffset) + "' ORDER BY bday LIMIT " + Common.toDigit(settings.get("maxbdays"));
            } else if ("styles".equals(obj)) {
                table = "stylevars";
                sql = "SELECT sv.* FROM " + tablepre + "stylevars sv LEFT JOIN " + tablepre + "styles s ON s.styleid = sv.styleid AND (s.available=1 OR s.styleid='0')";
            } else if ("icons".equals(obj)) {
                table = "smilies";
                conditions = "WHERE type='icon' ORDER BY displayorder";
            } else if ("secqaa".equals(obj)) {
                Random rand = new Random();
                int secqaanum = Integer.valueOf(dataBaseDao.executeQuery("SELECT COUNT(*) count FROM jrun_itempool").get(0).get("count"));
                int start_limit = secqaanum <= 10 ? 0 : rand.nextInt(secqaanum - 10);
                List<Map<String, String>> secqaas = dataBaseDao.executeQuery("SELECT question, answer FROM jrun_itempool LIMIT " + start_limit + ", 10");
                Map<String, String> datas = new HashMap<String, String>();
                int size = secqaas.size();
                if (size > 0) {
                    for (int i = 0; i < size; i++) {
                        Map<String, String> secqaa = secqaas.get(i);
                        datas.put(String.valueOf(i), dataParse.combinationChar(secqaa));
                    }
                } else {
                    datas.put("0", null);
                }
                while ((size = datas.size()) < 10) {
                    datas.put(size + "", datas.get(rand.nextInt(size) + ""));
                }
                writeToCacheFile(prefix + cachename, arrayeval(obj, datas), "", flag);
                datas = null;
                break;
            } else if ("medals".equals(obj)) {
                table = "medals";
                cols = "medalid, name, image";
                conditions = "WHERE available='1'";
            } else if ("censor".equals(obj)) {
                table = "words";
            } else if ("faqs".equals(obj)) {
                conditions = "WHERE identifier!='' AND keyword!=''";
            } else if ("announcements_forum".equals(obj)) {
                int timestamp = Common.time();
                table = "announcements";
                conditions = "WHERE type!=2 AND groups = '' AND starttime<='" + timestamp + "' ORDER BY displayorder, starttime DESC, id DESC LIMIT 1";
            } else if ("globalstick".equals(obj)) {
                table = "forums";
                conditions = "WHERE status>0 AND type IN ('forum', 'sub') ORDER BY type";
            } else if ("bbcodes".equals(obj)) {
                table = "bbcodes";
                conditions = "WHERE available='1' AND icon!=''";
            } else if ("ranks".equals(obj)) {
                table = "ranks";
                cols = "ranktitle, postshigher, stars, color";
                conditions = "ORDER BY postshigher DESC";
            } else if ("bbcodes_display".equals(obj)) {
                table = "bbcodes";
                conditions = "WHERE available='1' AND icon!=''";
            } else if ("smilies_display".equals(obj)) {
                table = "imagetypes";
                conditions = "WHERE type='smiley' ORDER BY displayorder";
            } else if ("smilies".equals(obj)) {
                table = "smilies";
                sql = "SELECT s.* FROM " + tablepre + "smilies s LEFT JOIN " + tablepre + "imagetypes t ON t.typeid=s.typeid WHERE s.type='smiley' AND s.code<>'' AND t.typeid IS NOT NULL ORDER BY LENGTH(s.code) DESC";
            } else if ("profilefields".equals(obj)) {
                table = "profilefields";
                cols = "fieldid, invisible, title, description, required, unchangeable, selective, choices";
                conditions = "WHERE available='1' ORDER BY displayorder";
            } else if (obj.length() > 5 && obj.substring(0, 5).equals("advs_")) {
                Map<String, String> datas = new HashMap<String, String>();
                Map advs = advertisement(obj.substring(5));
                datas.put(obj.substring(0, 4), advs != null && advs.size() > 0 ? dataParse.combinationChar(advs) : "");
                advs = null;
                writeToCacheFile(prefix + cachename, arrayeval(obj.substring(0, 4), datas), "", flag);
                datas = null;
                flag = true;
                continue;
            } else if ("smilies_var".equals(cachename)) {
                String smrows = settings.get("smrows");
                String smcols = settings.get("smcols");
                int spp = Common.toDigit(smcols) * Common.toDigit(smrows);
                List<Map<String, String>> imagetypes = dataBaseDao.executeQuery("select typeid,name,directory from " + tablepre + "imagetypes order by displayorder");
                if (imagetypes != null && imagetypes.size() > 0) {
                    StringBuffer return_type = new StringBuffer("var smthumb = '20';var smilies_type = new Array();");
                    StringBuffer return_datakey = new StringBuffer("var smilies_array = new Array();");
                    for (Map<String, String> stypes : imagetypes) {
                        List<Map<String, String>> smileslist = dataBaseDao.executeQuery("SELECT id, code, url FROM " + tablepre + "smilies WHERE type='smiley' AND code<>'' AND typeid='" + stypes.get("typeid") + "' ORDER BY displayorder");
                        if (smileslist != null && smileslist.size() > 0) {
                            int j = 1;
                            int i = 0;
                            return_type.append("smilies_type[" + stypes.get("typeid") + "] = ['" + stypes.get("name").replace("'", "\\'") + "','" + stypes.get("directory").replace("'", "\\'") + "'];");
                            return_datakey.append("smilies_array[" + stypes.get("typeid") + "] = new Array();");
                            return_datakey.append("smilies_array[" + stypes.get("typeid") + "][" + j + "]=[");
                            for (Map<String, String> smiles : smileslist) {
                                if (i >= spp) {
                                    return_datakey.deleteCharAt(return_datakey.length() - 1);
                                    return_datakey.append("];");
                                    j++;
                                    return_datakey.append("smilies_array[" + stypes.get("typeid") + "][" + j + "]=[");
                                    i = 0;
                                }
                                i++;
                                String smileycode = smiles.get("code").replace("'", "\\'");
                                String url = smiles.get("url").replace("'", "\\'");
                                int windth = 20;
                                URL imgurl = Thread.currentThread().getContextClassLoader().getResource("../../images/smilies/" + stypes.get("directory") + "/" + url);
                                if (imgurl != null) {
                                    String path = Common.decode(imgurl.getPath());
                                    File file = new File(path);
                                    if (file.exists()) {
                                        windth = ImageIO.read(file).getWidth();
                                    }
                                }
                                return_datakey.append("['" + smiles.get("id") + "','" + smileycode + "','" + url + "','20','20','" + windth + "'],");
                            }
                            return_datakey.deleteCharAt(return_datakey.length() - 1);
                            return_datakey.append("];");
                        }
                        smileslist = null;
                    }
                    imagetypes = null;
                    writeToJsCacheFile("smilies", return_type.toString() + return_datakey.toString(), "_var");
                }
                break;
            } else if ("usergroups".equals(obj)) {
                sql = "SELECT * FROM " + tablepre + "usergroups u LEFT JOIN " + tablepre + "admingroups a ON u.groupid=a.admingid";
            }
            this.getDataList(sql, table != null ? table : obj, cols, conditions, cachename, obj, prefix, flag);
            flag = true;
        }
        if ("threadtypes".equals(cachename)) {
            Map<String, String> datas = new HashMap<String, String>();
            List<Map<String, String>> dataList = dataBaseDao.executeQuery("SELECT t.typeid, tt.optionid, tt.title, tt.type, tt.rules, tt.identifier, tt.description, tv.required, tv.unchangeable, tv.search FROM " + tablepre + "threadtypes t LEFT JOIN " + tablepre + "typevars tv ON t.typeid=tv.typeid	LEFT JOIN " + tablepre + "typeoptions tt ON tv.optionid=tt.optionid WHERE t.special='1' AND tv.available='1' ORDER BY tv.displayorder");
            Map<Integer, Map<Integer, Map<String, String>>> typelists = new HashMap<Integer, Map<Integer, Map<String, String>>>();
            Map<Integer, String> templatedata = new HashMap<Integer, String>();
            if (dataList != null && dataList.size() > 0) {
                Map<Integer, Map<String, String>> typelist = null;
                for (Map<String, String> data : dataList) {
                    Map<String, String> rules = dataParse.characterParse(data.get("rules"), false);
                    Integer typeid = Integer.valueOf(data.get("typeid"));
                    String type = data.get("type");
                    typelist = typelists.get(typeid);
                    if (typelist == null) {
                        typelist = new HashMap<Integer, Map<String, String>>();
                    }
                    if (rules != null && rules.size() > 0) {
                        if ("select".equals(type) || "checkbox".equals(type) || "radio".equals(type)) {
                            String[] choices = rules.get("choices").split("(\r\n|\n|\r)");
                            StringBuffer temp = new StringBuffer();
                            for (String choice : choices) {
                                String[] items = choice.split("=");
                                if (items.length == 2) {
                                    temp.append("," + items[1].trim());
                                }
                            }
                            data.put("choices", temp.length() > 0 ? temp.substring(1).replaceAll(",", "\\\\n") : "");
                        } else if ("text".equals(type) || "textarea".equals(type)) {
                            data.put("maxlength", rules.get("maxlength"));
                        } else if ("image".equals(type)) {
                            data.put("maxwidth", rules.get("maxwidth"));
                            data.put("maxheight", rules.get("maxheight"));
                        } else if ("number".equals(type)) {
                            data.put("maxnum", rules.get("maxnum"));
                            data.put("minnum", rules.get("minnum"));
                        }
                    }
                    data.remove("rules");
                    typelist.put(Integer.valueOf(data.get("optionid")), data);
                    typelists.put(typeid, typelist);
                }
            }
            dataList = dataBaseDao.executeQuery("SELECT typeid, template FROM " + tablepre + "threadtypes WHERE special='1'");
            if (dataList != null && dataList.size() > 0) {
                for (Map<String, String> data : dataList) {
                    templatedata.put(Integer.valueOf(data.get("typeid")), data.get("template"));
                }
            }
            dataList = null;
            Set<Integer> typeids = typelists.keySet();
            for (Integer typeid : typeids) {
                datas.put("dtype", dataParse.combinationChar(typelists.get(typeid)));
                datas.put("dtypeTemplate", templatedata.get(typeid));
                this.writeToCacheFile(String.valueOf(typeid), arrayeval("threadtype", datas), "threadtype_", false);
            }
            datas = null;
            typeids = null;
            templatedata = null;
            typelists = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void getDataList(String sql, String table, String cols, String conditions, String cachename, String cname, String prefix, boolean append) throws Exception {
        Map<String, String> datas = new HashMap<String, String>();
        List<Map<String, String>> dataList = new ArrayList<Map<String, String>>();
        dataList = dataBaseDao.executeQuery(sql != null ? sql : "SELECT " + (cols == null ? "*" : cols) + " FROM " + tablepre + table + (conditions != null ? " " + conditions : ""));
        if ("settings".equals(cname)) {
            Map<String, String> map = new HashMap<String, String>();
            for (Map<String, String> data : dataList) {
                datas.put(data.get("variable"), data.get("value"));
            }
            Map<Integer, Map> extcredits = dataParse.characterParse(datas.get("extcredits"), false);
            if (extcredits != null && extcredits.size() > 0) {
                int creditstrans = Integer.valueOf(datas.get("creditstrans"));
                Map<Integer, Map> exchcredits = new HashMap<Integer, Map>();
                Set<Integer> extcreditids = extcredits.keySet();
                boolean allowexchangein = false;
                boolean allowexchangeout = false;
                for (Integer extcreditid : extcreditids) {
                    Map extcredit = extcredits.get(extcreditid);
                    if ("1".equals(extcredit.get("available"))) {
                        extcredit.remove("available");
                        exchcredits.put(extcreditid, extcredit);
                        Object obj = extcredit.get("ratio");
                        if (obj == null) {
                            obj = 0;
                        }
                        double ratio = Double.valueOf(obj.toString());
                        if (ratio > 0) {
                            if ("1".equals(extcredit.get("allowexchangein"))) {
                                allowexchangein = true;
                            }
                            if ("1".equals(extcredit.get("allowexchangeout"))) {
                                allowexchangeout = true;
                            }
                        }
                    }
                }
                datas.put("exchangestatus", (allowexchangein && allowexchangeout ? "1" : "0"));
                datas.put("transferstatus", (exchcredits.get(creditstrans) != null ? "1" : "0"));
                datas.put("extcredits", dataParse.combinationChar(exchcredits));
            }
            extcredits = null;
            int jsmenustatus = Integer.valueOf(datas.get("jsmenustatus"));
            datas.put("jsmenu_1", String.valueOf(jsmenustatus & 1));
            datas.put("jsmenu_2", String.valueOf(jsmenustatus & 2));
            datas.put("jsmenu_3", String.valueOf(jsmenustatus & 4));
            datas.put("jsmenu_4", String.valueOf(jsmenustatus & 8));
            datas.put("timeformat", datas.get("timeformat").equals("1") ? "hh:mm a" : "HH:mm");
            datas.put("onlinehold", String.valueOf(Integer.valueOf(datas.get("onlinehold")) * 60));
            datas.put("version", JSPRUN_KERNEL_VERSION);
            map = dataBaseDao.executeQuery("SELECT COUNT(*) count FROM " + tablepre + "members").get(0);
            datas.put("totalmembers", map != null ? map.get("count") : "0");
            map = dataBaseDao.executeQuery("SELECT COUNT(*) count FROM " + tablepre + "forums WHERE status>0 AND threadcaches>0").get(0);
            datas.put("cachethreadon", map != null && Integer.valueOf(map.get("count")) > 0 ? "1" : "0");
            List<Map<String, String>> lastMember = dataBaseDao.executeQuery("SELECT username FROM " + tablepre + "members ORDER BY uid DESC LIMIT 1");
            map = lastMember != null && lastMember.size() > 0 ? lastMember.get(0) : null;
            lastMember = null;
            datas.put("lastmember", map != null ? map.get("username").replace("\\", "\\\\") : "");
            List<Map<String, String>> crons = dataBaseDao.executeQuery("SELECT nextrun FROM " + tablepre + "crons WHERE available>'0' AND nextrun>'0' ORDER BY nextrun LIMIT 1");
            datas.put("cronnextrun", crons != null && crons.size() > 0 ? crons.get(0).get("nextrun") : "0");
            Map<String, String> google = dataParse.characterParse(datas.get("google"), false);
            datas.put("google_status", google.get("status"));
            datas.put("google_searchbox", google.get("searchbox"));
            google = null;
            datas.remove("google");
            Map<String, String> baidu = dataParse.characterParse(datas.get("baidu"), false);
            datas.put("baidu_status", baidu.get("status"));
            datas.put("baidu_searchbox", baidu.get("searchbox"));
            baidu = null;
            datas.remove("baidu");
            datas.put("stylejumpstatus", datas.get("stylejump"));
            List<Map<String, String>> styleList = dataBaseDao.executeQuery("SELECT styleid, name FROM " + tablepre + "styles WHERE available='1'");
            if (styleList != null && styleList.size() > 0) {
                Map<Integer, String> styles = new HashMap<Integer, String>();
                for (Map<String, String> style : styleList) {
                    styles.put(Integer.valueOf(style.get("styleid")), style.get("name"));
                }
                datas.put("stylejump", styles != null ? dataParse.combinationChar(styles) : "");
            }
            styleList = null;
            Map globaladvs = advertisement("all");
            datas.put("globaladvs", globaladvs.get("all") != null ? dataParse.combinationChar((Map) globaladvs.get("all")) : "");
            datas.put("redirectadvs", globaladvs.get("redirect") != null ? dataParse.combinationChar((Map) globaladvs.get("redirect")) : "");
            globaladvs = null;
            List<Map<String, String>> plugins = dataBaseDao.executeQuery("SELECT available, name, identifier, directory, datatables, modules FROM " + tablepre + "plugins where available='1'");
            if (plugins != null && plugins.size() > 0) {
                Map<String, Map<String, Map<String, String>>> pluginlinks = new HashMap<String, Map<String, Map<String, String>>>();
                Map<Integer, Map<String, String>> links = new TreeMap<Integer, Map<String, String>>();
                Map<Integer, Map<String, String>> includes = new TreeMap<Integer, Map<String, String>>();
                Map<Integer, Map<String, String>> jsmenus = new TreeMap<Integer, Map<String, String>>();
                for (Map<String, String> plugin : plugins) {
                    Map<Integer, Map> modules = dataParse.characterParse(plugin.get("modules"), false);
                    if (modules != null && modules.size() > 0) {
                        Set<Integer> keys = modules.keySet();
                        for (Integer key : keys) {
                            Map module = modules.get(key);
                            int type = Common.toDigit((String) module.get("type"));
                            String identifier = plugin.get("identifier");
                            if (type == 1) {
                                Map<String, String> link = new HashMap<String, String>();
                                link.put("adminid", String.valueOf(module.get("adminid")));
                                link.put("url", "<a href=\"" + module.get("url") + "\">" + module.get("menu") + "</a>");
                                links.put(links.size(), link);
                            } else if (type == 2) {
                                String name = String.valueOf(module.get("name"));
                                String adminid = String.valueOf(module.get("adminid"));
                                Map<String, String> link = new HashMap<String, String>();
                                link.put("adminid", adminid);
                                link.put("url", "<a href=\"plugin.jsp?identifier=" + identifier + "&module=" + name + "\">" + name + "</a>");
                                links.put(links.size(), link);
                                Map<String, Map<String, String>> pluginlink = pluginlinks.get(identifier);
                                if (pluginlink == null) {
                                    pluginlink = new HashMap<String, Map<String, String>>();
                                    pluginlinks.put(identifier, pluginlink);
                                }
                                Map<String, String> templink = new HashMap<String, String>();
                                templink.put("adminid", adminid);
                                templink.put("directory", String.valueOf(plugin.get("directory")));
                                pluginlink.put(name, templink);
                            } else if (type == 4) {
                                Map<String, String> include = new HashMap<String, String>();
                                include.put("adminid", String.valueOf(module.get("adminid")));
                                include.put("script", plugin.get("directory") + module.get("name"));
                                includes.put(includes.size(), include);
                            } else if (type == 5) {
                                Map<String, String> jsmenu = new HashMap<String, String>();
                                jsmenu.put("adminid", String.valueOf(module.get("adminid")));
                                jsmenu.put("url", "<a href=\"" + module.get("url") + "\">" + module.get("menu") + "</a>");
                                jsmenus.put(jsmenus.size(), jsmenu);
                            } else if (type == 6) {
                                String name = String.valueOf(module.get("name"));
                                String adminid = String.valueOf(module.get("adminid"));
                                Map<String, String> jsmenu = new HashMap<String, String>();
                                jsmenu.put("adminid", String.valueOf(module.get("adminid")));
                                jsmenu.put("url", "<a href=\"plugin.jsp?identifier=" + identifier + "&module=" + name + "\">" + name + "</a>");
                                jsmenus.put(jsmenus.size(), jsmenu);
                                Map<String, Map<String, String>> pluginlink = pluginlinks.get(identifier);
                                if (pluginlink == null) {
                                    pluginlink = new HashMap<String, Map<String, String>>();
                                    pluginlinks.put(identifier, pluginlink);
                                }
                                Map<String, String> templink = new HashMap<String, String>();
                                templink.put("adminid", adminid);
                                templink.put("directory", String.valueOf(plugin.get("directory")));
                                pluginlink.put(name, templink);
                            }
                        }
                    }
                }
                Map<String, Map> pluginstemp = new HashMap<String, Map>();
                if (links.size() > 0) {
                    pluginstemp.put("links", links);
                }
                if (includes.size() > 0) {
                    pluginstemp.put("includes", includes);
                }
                if (jsmenus.size() > 0) {
                    pluginstemp.put("jsmenus", jsmenus);
                }
                datas.put("plugins", pluginstemp.size() > 0 ? dataParse.combinationChar(pluginstemp) : "");
                datas.put("pluginlinks", pluginlinks.size() > 0 ? dataParse.combinationChar(pluginlinks) : "");
            }
            List<Map<String, String>> pluginhooks = dataBaseDao.executeQuery("SELECT ph.title, ph.code, p.identifier FROM jrun_plugins p LEFT JOIN jrun_pluginhooks ph ON ph.pluginid=p.pluginid AND ph.available='1' WHERE p.available='1' ORDER BY p.identifier");
            if (pluginhooks != null && pluginhooks.size() > 0) {
                Map<String, String> hooks = new HashMap<String, String>();
                for (Map<String, String> pluginhook : pluginhooks) {
                    String title = pluginhook.get("title");
                    String code = pluginhook.get("code");
                    if (title != null && code != null) {
                        hooks.put(pluginhook.get("identifier") + "_" + pluginhook.get("title"), pluginhook.get("code"));
                    }
                }
                datas.put("hooks", hooks.size() > 0 ? dataParse.combinationChar(hooks) : "");
            }
            List<Map<String, String>> forumList = dataBaseDao.executeQuery("SELECT f.fid, f.type, f.name, f.fup, ff.viewperm FROM jrun_forums f LEFT JOIN jrun_forumfields ff ON ff.fid=f.fid LEFT JOIN jrun_access a ON a.fid=f.fid AND a.allowview='1' WHERE f.status>0 ORDER BY f.type, f.displayorder");
            Common.setForums(forumList);
            String forums = dataParse.combinationChar(forumList);
            datas.put("forums", forums);
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
        } else if ("admingroups".equals(cname)) {
            for (Map<String, String> data : dataList) {
                writeToCacheFile(cachename + "_" + data.get("admingid"), arrayeval(cname, data), "", append);
            }
        } else if ("usergroups".equals(cname)) {
            for (Map<String, String> data : dataList) {
                Set<String> keys = data.keySet();
                for (String key : keys) {
                    if (data.get(key) == null) {
                        data.put(key, "0");
                    }
                }
                writeToCacheFile(cachename + "_" + data.get("groupid"), arrayeval(cname, data), "", append);
            }
        } else if ("styles".equals(cname)) {
            List<Map<String, String>> styles = dataBaseDao.executeQuery("SELECT s.styleid,s.name,s.available,s.templateid, t.directory as tpldir FROM " + tablepre + "styles s LEFT JOIN " + tablepre + "templates t ON s.templateid=t.templateid WHERE s.available=1 OR s.styleid='0'");
            List<Map<String, String>> maxavatarpixel = dataBaseDao.executeQuery("select value from jrun_settings where variable='maxavatarpixel'");
            List<Map<String, String>> customauthorinfo = dataBaseDao.executeQuery("select value from jrun_settings where variable='customauthorinfo'");
            int maxavatarpic = maxavatarpixel.size() > 0 ? Integer.valueOf(maxavatarpixel.get(0).get("value")) : 0;
            Map<String, String> maxsigrows = dataBaseDao.executeQuery("select value from jrun_settings where variable='maxsigrows'").get(0);
            Map custommap = dataParse.characterParse(customauthorinfo.get(0).get("value"), false);
            customauthorinfo = null;
            int left = 0;
            if (custommap != null && custommap.get(0) != null) {
                Map customreMap = (Map) custommap.get(0);
                Iterator its = customreMap.keySet().iterator();
                while (its.hasNext()) {
                    Object key = its.next();
                    Map dismap = (Map) customreMap.get(key);
                    if (dismap.get("left") != null) {
                        left++;
                    }
                }
            }
            for (Map<String, String> style : styles) {
                Set<String> keys = style.keySet();
                for (String key : keys) {
                    datas.put(key.toUpperCase(), style.get(key));
                }
                String styleid = style.get("styleid");
                for (Map<String, String> data : dataList) {
                    if (styleid.equals(data.get("styleid"))) {
                        datas.put(data.get("variable").toUpperCase(), data.get("substitute"));
                    }
                }
                datas.put("BGCODE", setcssbackground(datas, "BGCOLOR"));
                datas.put("CATBGCODE", setcssbackground(datas, "CATCOLOR"));
                datas.put("HEADERBGCODE", setcssbackground(datas, "HEADERCOLOR"));
                datas.put("HEADERMENUBGCODE", setcssbackground(datas, "HEADERMENU"));
                datas.put("PORTALBOXBGCODE", setcssbackground(datas, "PORTALBOXBGCODE"));
                String boardimg = datas.get("BOARDIMG");
                if (boardimg.indexOf(",") > -1) {
                    String[] flash = boardimg.split(",");
                    flash[0] = flash[0].trim();
                    if (!Common.matches(flash[0], "^http:\\/\\/")) {
                        flash[0] = datas.get("IMGDIR") + "/" + flash[0];
                    }
                    datas.put("BOARDLOGO", "<embed src=\"" + flash[0] + "\" width=\"" + flash[1].trim() + "\" height=\"" + flash[2].trim() + "\" type=\"application/x-shockwave-flash\"></embed>");
                } else {
                    if (!Common.matches(boardimg, "^http:\\/\\/")) {
                        boardimg = datas.get("IMGDIR") + "/" + boardimg;
                    }
                    datas.put("BOARDIMG", boardimg);
                    datas.put("BOARDLOGO", "<img src=\"" + boardimg + "\" alt=\"JspRun\" border=\"0\" />");
                }
                datas.put("BOLD", datas.get("NOBOLD") != null && !datas.get("NOBOLD").equals("") ? "normal" : "bold");
                datas.put("POSTMINHEIGHT", (maxavatarpic > 300 ? 300 : maxavatarpic + left * 20) + "");
                datas.put("MAXSIGROWS", maxsigrows.get("value"));
                writeToCssCache(datas);
                writeToCacheFile(cachename + "_" + styleid, arrayeval(cname, datas), "", append);
                datas.clear();
            }
            maxavatarpixel = null;
            maxsigrows = null;
        } else if ("icons".equals(cname)) {
            for (Map<String, String> data : dataList) {
                datas.put(data.get("id"), data.get("url"));
            }
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
        } else if ("medals".equals(cname)) {
            Map<Integer, Map<String, String>> map = new HashMap<Integer, Map<String, String>>();
            int size = dataList.size();
            for (Integer i = 0; i < size; i++) {
                map.put(i, dataList.get(i));
            }
            datas.put(cname, dataParse.combinationChar(map.size() > 0 ? map : null));
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
        } else if ("censor".equals(cname)) {
            StringBuffer banned = new StringBuffer();
            StringBuffer mod = new StringBuffer();
            Map<String, Map<String, String>> filters = new HashMap<String, Map<String, String>>();
            Map<String, String> finds = new HashMap<String, String>();
            Map<String, String> replaces = new HashMap<String, String>();
            for (Map<String, String> data : dataList) {
                String id = data.get("id");
                String find = data.get("find");
                String replacement = data.get("replacement");
                find = find.replaceAll("\\{(\\d+)\\}", ".{0,\\1}");
                if ("{BANNED}".equals(replacement)) {
                    banned.append(find);
                } else if ("{MOD}".equals(replacement)) {
                    mod.append(find);
                } else {
                    finds.put(id, find);
                    replaces.put(id, replacement);
                }
            }
            if (finds.size() > 0) {
                filters.put("find", finds);
                filters.put("replace", replaces);
            }
            datas.put("filter", dataParse.combinationChar(filters.size() > 0 ? filters : null));
            datas.put("banned", banned.length() > 0 ? "(" + banned + ")" : "");
            datas.put("mod", mod.length() > 0 ? "(" + mod + ")" : "");
            finds = null;
            replaces = null;
            filters = null;
            mod = null;
            banned = null;
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
        } else if ("faqs".equals(cname)) {
            Map<String, Map<String, String>> faqsmap = new HashMap<String, Map<String, String>>();
            for (Map<String, String> data : dataList) {
                if (!"".equals(data.get("identifier")) && !"".equals(data.get("keyword"))) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("id", data.get("id"));
                    map.put("keyword", data.get("keyword"));
                    faqsmap.put(data.get("identifier"), map);
                }
            }
            datas.put(cname, dataParse.combinationChar(faqsmap.size() > 0 ? faqsmap : null));
            faqsmap = null;
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
        } else if ("index".equals(cachename)) {
            Map<Integer, Map<String, String>> map = new HashMap<Integer, Map<String, String>>();
            if ("forumlinks".equals(cname)) {
                Map<String, String> settings = ForumInit.settings;
                int forumlinkstatus = Common.toDigit(settings.get("forumlinkstatus"));
                if (forumlinkstatus > 0) {
                    Map<String, String> forumlink = null;
                    StringBuffer tightlink_text = new StringBuffer();
                    StringBuffer tightlink_logo = new StringBuffer();
                    for (Map<String, String> flink : dataList) {
                        int id = Integer.valueOf(flink.get("id"));
                        String name = flink.get("name");
                        String url = flink.get("url");
                        forumlink = new HashMap<String, String>();
                        if (!"".equals(flink.get("description"))) {
                            forumlink.put("content", "<h5><a href='" + url + "' target='_blank'>" + name + "</a></h5><p>" + flink.get("description") + "</p>");
                            if (!"".equals(flink.get("logo"))) {
                                forumlink.put("type", "1");
                                forumlink.put("logo", flink.get("logo"));
                            } else {
                                forumlink.put("type", "2");
                            }
                            map.put(id, forumlink);
                        } else {
                            if (!"".equals(flink.get("logo"))) {
                                tightlink_logo.append("<a href='" + url + "' target='_blank'><img src='" + flink.get("logo") + "' border='0' alt='" + name + "' /></a> ");
                            } else {
                                tightlink_text.append("<a href='" + url + "' target='_blank'>[" + name + "]]</a> ");
                            }
                        }
                    }
                    if (tightlink_logo.length() > 0 || tightlink_text.length() > 0) {
                        forumlink = new HashMap<String, String>();
                        if (!"".equals(tightlink_logo)) {
                            tightlink_logo.append("<br />");
                        }
                        forumlink.put("type", "3");
                        forumlink.put("content", tightlink_logo.append(tightlink_text).toString());
                        map.put(0, forumlink);
                    }
                    tightlink_text = null;
                    tightlink_logo = null;
                    forumlink = null;
                }
                datas.put(cname, dataParse.combinationChar(map.size() > 0 ? map : null));
            } else if ("onlinelist".equals(cname)) {
                StringBuffer legend = new StringBuffer();
                for (Map<String, String> list : dataList) {
                    datas.put(list.get("groupid"), list.get("url"));
                    legend.append("<img src=\"images/common/" + list.get("url") + "\" alt=\"\" /> " + list.get("title") + " &nbsp; &nbsp; &nbsp; ");
                    if (Integer.valueOf(list.get("groupid")) == 7) {
                        datas.put("guest", list.get("title"));
                    }
                }
                datas.put("legend", legend.toString());
                legend = null;
            } else if ("birthdays_index".equals(cname)) {
                StringBuffer todaysbdays = new StringBuffer();
                for (Map<String, String> bdaymember : dataList) {
                    todaysbdays.append("<a href=\"space.jsp?uid=" + bdaymember.get("uid") + "\" target=\"_blank\" title=\"" + bdaymember.get("bday") + "\">" + bdaymember.get("username") + "</a>, ");
                }
                int length = todaysbdays.length();
                datas.put("todaysbdays", length >= 2 ? todaysbdays.substring(0, length - 2) : "");
            } else if ("announcements".equals(cname)) {
                int size = dataList.size();
                for (int i = 1; i <= size; i++) {
                    Map<String, String> data = dataList.get(i - 1);
                    if (!"1".equals(data.get("type"))) {
                        data.remove("message");
                    }
                    map.put(i, data);
                }
                if (map.size() > 0) {
                    datas.put(cname, dataParse.combinationChar(map));
                }
            } else if ("tags_index".equals(cname)) {
                cname = "tags";
                Map<String, String> settings = ForumInit.settings;
                int tagstatus = Common.toDigit(settings.get("tagstatus"));
                int rewritestatus = Common.toDigit(settings.get("rewritestatus"));
                boolean tagsurl = (rewritestatus & 8) > 0;
                if (tagstatus > 0) {
                    int hottags = Common.toDigit(settings.get("hottags"));
                    StringBuffer tags = new StringBuffer();
                    if (hottags > 0 && dataList != null && dataList.size() > 0) {
                        for (Map<String, String> tag : dataList) {
                            if (tagsurl) {
                                tags.append(" <a href='tag-" + Common.encode(tag.get("tagname")) + ".html' target='_blank'>" + tag.get("tagname") + "<em>(" + tag.get("total") + ")</em></a>");
                            } else {
                                tags.append(" <a href='tag.jsp?name=" + Common.encode(tag.get("tagname")) + "' target='_blank'>" + tag.get("tagname") + "<em>(" + tag.get("total") + ")</em></a>");
                            }
                        }
                    }
                    if (tags.length() > 0) {
                        datas.put(cname, tags.substring(1));
                    }
                }
            }
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
            map = null;
        } else if ("forumdisplay".equals(cachename)) {
            Map<Integer, Map<String, String>> map = new HashMap<Integer, Map<String, String>>();
            if ("announcements_forum".equals(cname)) {
                cname = "announcement";
                if (dataList != null && dataList.size() > 0) {
                    Map<String, String> data = dataList.get(0);
                    Set<String> keys = data.keySet();
                    if (!"1".equals(data.get("type"))) {
                        keys.remove("message");
                    }
                    for (String key : keys) {
                        datas.put(key, data.get(key));
                    }
                }
            } else if ("announcements".equals(cname)) {
                int size = dataList.size();
                for (int i = 1; i <= size; i++) {
                    Map<String, String> data = dataList.get(i - 1);
                    data.remove("message");
                    map.put(i, data);
                }
                datas.put(cname, dataParse.combinationChar(map.size() > 0 ? map : null));
            } else if ("onlinelist".equals(cname)) {
                StringBuffer legend = new StringBuffer();
                for (Map<String, String> list : dataList) {
                    datas.put(list.get("groupid"), list.get("url"));
                    legend.append("<img src=\"images/common/" + list.get("url") + "\" alt=\"\" /> " + list.get("title") + " &nbsp; &nbsp; &nbsp; ");
                    if (Integer.valueOf(list.get("groupid")) == 7) {
                        datas.put("guest", list.get("title"));
                    }
                }
                datas.put("legend", legend.toString());
            } else if ("globalstick".equals(cname)) {
                Map<String, Map<String, String>> globalstick = new HashMap<String, Map<String, String>>();
                Map<String, String> fupMap = new HashMap<String, String>();
                Map<String, String> threadMap = new HashMap<String, String>();
                for (Map<String, String> list : dataList) {
                    if (list.get("type").equals("forum")) {
                        fupMap.put(list.get("fid"), list.get("fup"));
                    } else {
                        fupMap.put(list.get("fid"), fupMap.get(list.get("fup")));
                    }
                }
                List<Map<String, String>> threads = dataBaseDao.executeQuery("SELECT tid,fid,displayorder FROM jrun_threads WHERE displayorder IN (2, 3)");
                if (threads != null && threads.size() > 0) {
                    for (Map<String, String> thread : threads) {
                        if (thread.get("displayorder").equals("2")) {
                            StringBuffer tids = null;
                            if (threadMap.get(fupMap.get(thread.get("fid"))) == null) {
                                tids = new StringBuffer(thread.get("tid"));
                            } else {
                                tids = new StringBuffer(threadMap.get(fupMap.get(thread.get("fid"))) + "," + thread.get("tid"));
                            }
                            threadMap.put(fupMap.get(thread.get("fid")), tids.toString());
                        } else {
                            StringBuffer tids = null;
                            if (threadMap.get("global") == null) {
                                tids = new StringBuffer(thread.get("tid"));
                            } else {
                                tids = new StringBuffer(threadMap.get("global") + "," + thread.get("tid"));
                            }
                            threadMap.put("global", tids.toString());
                        }
                    }
                }
                Set<String> keys = threadMap.keySet();
                for (String key : keys) {
                    String tids = threadMap.get(key);
                    if (tids != null) {
                        Map<String, String> categories = new HashMap<String, String>();
                        categories.put("tids", tids);
                        categories.put("count", String.valueOf(tids.split(",").length));
                        globalstick.put(key, categories);
                    }
                }
                datas.put(cname, dataParse.combinationChar(globalstick.size() > 0 ? globalstick : null));
                globalstick = null;
                fupMap = null;
                threadMap = null;
            } else {
                int size = dataList.size();
                for (Integer i = 1; i <= size; i++) {
                    map.put(i, dataList.get(i - 1));
                }
                datas.put(cname, dataParse.combinationChar(map.size() > 0 ? map : null));
            }
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
            map = null;
        } else if ("viewthread".equals(cachename)) {
            if ("bbcodes".equals(cname)) {
                for (Map<String, String> list : dataList) {
                    datas.put(list.get("tag"), list.get("replacement"));
                }
            } else if ("announcements".equals(cname)) {
                int size = dataList.size();
                Map<Integer, Map<String, String>> map = new HashMap<Integer, Map<String, String>>();
                for (int i = 1; i <= size; i++) {
                    Map<String, String> data = dataList.get(i - 1);
                    data.remove("message");
                    map.put(i, data);
                }
                datas.put(cname, dataParse.combinationChar(map.size() > 0 ? map : null));
                map = null;
            } else {
                Map<Integer, Map<String, String>> map = new HashMap<Integer, Map<String, String>>();
                int size = dataList.size();
                for (Integer i = 1; i <= size; i++) {
                    map.put(i, dataList.get(i - 1));
                }
                datas.put(cname, dataParse.combinationChar(map.size() > 0 ? map : null));
                map = null;
            }
            String currdata = arrayeval(cname, datas);
            writeToCacheFile(cachename, currdata, prefix, append);
        } else if ("ranks".equals(cachename)) {
            Map<Integer, Map<String, String>> map = new HashMap<Integer, Map<String, String>>();
            int size = dataList.size();
            for (Integer i = 1; i <= size; i++) {
                map.put(i, dataList.get(i - 1));
            }
            datas.put(cname, dataParse.combinationChar(map.size() > 0 ? map : null));
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
        } else if ("post".equals(cachename)) {
            Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
            if ("bbcodes_display".equals(cname)) {
                for (Map<String, String> bbcode : dataList) {
                    map.put(bbcode.get("tag"), bbcode);
                }
                datas.put(cname, dataParse.combinationChar(map.size() > 0 ? map : null));
                map = null;
            } else if ("smilies_display".equals(cname)) {
                Map<String, String> smileytypes = new HashMap<String, String>();
                for (Map<String, String> imagetype : dataList) {
                    List<Map<String, String>> smileyList = dataBaseDao.executeQuery("SELECT id, code, url FROM " + tablepre + "smilies WHERE type='smiley' AND code<>'' AND typeid='" + imagetype.get("typeid") + "' ORDER BY displayorder");
                    if (smileyList != null && smileyList.size() > 0) {
                        imagetype.remove("displayorder");
                        imagetype.remove("type");
                        smileytypes.put(imagetype.get("typeid"), dataParse.combinationChar(imagetype.size() > 0 ? imagetype : null));
                        Map<String, Map<String, String>> smilies = new HashMap<String, Map<String, String>>();
                        for (Map<String, String> smiley : smileyList) {
                            smilies.put(smiley.get("id"), smiley);
                        }
                        datas.put(imagetype.get("typeid"), dataParse.combinationChar(smilies.size() > 0 ? smilies : null));
                        ;
                    }
                }
                writeToCacheFile(cachename, arrayeval("smileytypes", smileytypes), prefix, append);
                smileytypes = null;
            } else if ("smilies".equals(cname)) {
                Map<String, String> searcharray = new HashMap<String, String>();
                Map<String, String> replacearray = new HashMap<String, String>();
                Map<String, String> typearray = new HashMap<String, String>();
                for (Map<String, String> smiley : dataList) {
                    searcharray.put(smiley.get("id"), smiley.get("code"));
                    replacearray.put(smiley.get("id"), smiley.get("url"));
                    typearray.put(smiley.get("id"), smiley.get("typeid"));
                }
                datas.put("searcharray", dataParse.combinationChar(searcharray.size() > 0 ? searcharray : null));
                datas.put("replacearray", dataParse.combinationChar(replacearray.size() > 0 ? replacearray : null));
                datas.put("typearray", dataParse.combinationChar(typearray.size() > 0 ? typearray : null));
                searcharray = null;
                replacearray = null;
                typearray = null;
            }
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
        } else if ("google".equals(cachename)) {
            if (dataList != null && dataList.size() > 0) {
                Map<String, String> googleInfo = dataList.get(0);
                Map<String, String> google = dataParse.characterParse(googleInfo.get("value"), false);
                writeToJsCacheFile(cachename, "var google_host=\"" + HTTP_HOST + "\";var google_charset=\"" + JspRunConfig.charset + "\";var google_hl=\"" + google.get("lang") + "\";var google_lr=\"" + (google.get("lang") != null ? "lang_" + google.get("lang") : "") + "\";", "_var");
                googleInfo = null;
                google = null;
            }
        } else if ("baidu".equals(cachename)) {
            if (dataList != null && dataList.size() > 0) {
                Map<String, String> baiduInfo = dataList.get(0);
                Map<String, String> baidu = dataParse.characterParse(baiduInfo.get("value"), false);
                writeToJsCacheFile(cachename, "var baidu_host=\"" + HTTP_HOST + "\";var baidu_charset=\"" + JspRunConfig.charset + "\";var baidu_hl=\"" + baidu.get("lang") + "\";var baidu_lr=\"" + (baidu.get("lang") != null ? "lang_" + baidu.get("lang") : "") + "\";", "_var");
                baiduInfo = null;
                baidu = null;
            }
        } else if ("profilefields".equals(cachename)) {
            if (dataList != null && dataList.size() > 0) {
                Map<String, Map<String, String>> fields_required = new TreeMap<String, Map<String, String>>();
                Map<String, Map<String, String>> fields_optional = new TreeMap<String, Map<String, String>>();
                for (Map<String, String> field : dataList) {
                    if ("1".equals(field.get("selective"))) {
                        String[] choices = field.get("choices").split("(\r\n|\r|\n)");
                        StringBuffer temp = new StringBuffer();
                        for (String choice : choices) {
                            choice = choice.trim();
                            String[] options = choice.split("=");
                            if (options.length == 2) {
                                temp.append("," + options[1]);
                            }
                        }
                        field.put("choices", temp.length() > 0 ? temp.substring(1) : "");
                    } else {
                        field.remove("choices");
                    }
                    if ("1".equals(field.get("required"))) {
                        fields_required.put("field_" + field.get("fieldid"), field);
                    } else {
                        fields_optional.put("field_" + field.get("fieldid"), field);
                    }
                }
                datas.put("fields_required", dataParse.combinationChar(fields_required));
                datas.put("fields_optional", dataParse.combinationChar(fields_optional));
            }
            writeToCacheFile(cachename, arrayeval(cname, datas), prefix, append);
        }
        datas = null;
        dataList = null;
    }

    private String arrayeval(String cachename, Map<String, String> map) {
        StringBuffer mapName = new StringBuffer("_DCACHE_");
        mapName.append(cachename);
        StringBuffer curdata = new StringBuffer();
        curdata.append("<%\n");
        curdata.append("Map<String,String> " + mapName + "= new HashMap<String,String>();\n");
        if (map != null) {
            Set<String> keys = map.keySet();
            for (String key : keys) {
                String value = map.get(key);
                if (value != null) {
                    value = Common.addslashes(value);
                    value = value.replaceAll("\r\n", "\\\\n");
                }
                curdata.append(mapName + ".put(\"" + key + "\",\"" + value + "\");\n");
            }
        }
        curdata.append("request.setAttribute(\"" + cachename + "\"," + mapName + ");\n");
        curdata.append("%>\n");
        return curdata.toString();
    }

    private boolean writeToCssCache(Map<String, String> datas) throws Exception {
        String[] csstemplates = { "css", "css_append" };
        String styleid = datas.get("STYLEID");
        String cachedir = path + "./forumdata/cache/";
        for (String templateName : csstemplates) {
            File file = new File(path + datas.get("TPLDIR") + "/" + templateName + ".jsp");
            if (!file.exists()) {
                file = new File(path + "./templates/default/" + templateName + ".jsp");
            }
            if (file.exists()) {
                StringBuffer cssdata = new StringBuffer();
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                while (br.ready()) {
                    cssdata.append(br.readLine() + "\n");
                }
                br.close();
                fr.close();
                Set<String> keys = datas.keySet();
                String content = cssdata.toString();
                for (String key : keys) {
                    content = content.replaceAll("\\{" + key + "\\}", datas.get(key));
                }
                content = content.replaceAll("<\\?.+?\\?>\\s*", "");
                String imgdir = datas.get("IMGDIR");
                content = !Common.matches(imgdir, "^http:\\/\\/") ? content.replace("url(\"" + imgdir, "url(\"../../" + imgdir) : content;
                content = !Common.matches(imgdir, "^http:\\/\\/") ? content.replace("url(" + imgdir, "url(../../" + imgdir) : content;
                String extra = "";
                if (templateName.length() > 3) {
                    extra = templateName.substring(3);
                }
                FileOutputStream fos = new FileOutputStream(cachedir + "style_" + styleid + extra + ".css");
                OutputStreamWriter os = new OutputStreamWriter(fos, JspRunConfig.charset);
                BufferedWriter bw = new BufferedWriter(os);
                bw.write(content);
                bw.flush();
                os.flush();
                fos.flush();
                bw.close();
                os.close();
                fos.close();
            } else {
                throw new Exception("Can not find the csstemplates files, please check directory " + datas.get("TPLDIR") + "/css.jsp and " + datas.get("TPLDIR") + "/css_append.jsp  ");
            }
            file = null;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean writeToCacheFile(String script, String cachedata, String prefix, boolean append) throws Exception {
        StringBuffer dir = new StringBuffer();
        dir.append(path);
        dir.append("./forumdata/cache/");
        File dirFile = new File(dir.toString());
        if (!dirFile.isDirectory()) {
            if (!dirFile.mkdir()) {
                throw new Exception("Can not write to cache files, please check directory ./forumdata/ and ./forumdata/cache/ .");
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(dir + prefix + script + ".jsp", append);
            OutputStreamWriter os = new OutputStreamWriter(fos, JspRunConfig.charset);
            BufferedWriter bw = new BufferedWriter(os);
            if (!append) {
                bw.write("<%--\n");
                bw.write("JspRun! cache file, DO NOT modify me!\n");
                bw.write("Created: " + new Date().toGMTString() + "\n");
                bw.write("Identify: " + Md5Token.getInstance().getLongToken(prefix + prefix + cachedata) + "\n");
                bw.write("--%>\n");
                bw.write("<%@ page language=\"java\" import=\"java.util.*\" pageEncoding=\"" + JspRunConfig.charset + "\"%>\n");
            }
            bw.write(cachedata);
            bw.flush();
            os.flush();
            fos.flush();
            bw.close();
            os.close();
            fos.close();
            bw = null;
        } catch (IOException e) {
            throw new Exception("Can not write to cache files, please check directory ./forumdata/ and ./forumdata/cache/ .");
        }
        dir = null;
        return true;
    }

    private boolean writeToJsCacheFile(String script, String cachedata, String postfix) throws Exception {
        StringBuffer dir = new StringBuffer();
        dir.append(path);
        dir.append("./forumdata/cache/");
        File dirFile = new File(dir.toString());
        if (!dirFile.isDirectory()) {
            if (!dirFile.mkdir()) {
                throw new Exception("Can not write to cache files, please check directory ./forumdata/ and ./forumdata/cache/ .");
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(dir + script + postfix + ".js");
            OutputStreamWriter os = new OutputStreamWriter(fos, JspRunConfig.charset);
            BufferedWriter bw = new BufferedWriter(os);
            bw.write(cachedata);
            bw.flush();
            os.flush();
            fos.flush();
            bw.close();
            os.close();
            fos.close();
        } catch (IOException e) {
            throw new Exception("Can not write to cache files, please check directory ./forumdata/ and ./forumdata/cache/ .");
        }
        dir = null;
        return true;
    }

    private String setcssbackground(Map<String, String> datas, String code) {
        String content = datas.get(code);
        if (content != null) {
            String[] codes = datas.get(code).split(" ");
            StringBuffer css = new StringBuffer("background: ");
            StringBuffer codevalue = new StringBuffer();
            int length = codes.length;
            for (int i = 0; i < length; i++) {
                if (!codes[i].equals("")) {
                    if (codes[i].charAt(0) == '#') {
                        css.append(codes[i].toUpperCase() + " ");
                        codevalue.append(codes[i].toUpperCase());
                    } else if (Common.matches(codes[i], "^http:\\/\\/")) {
                        css.append("url(\"" + codes[i] + "\") ");
                    } else {
                        css.append("url(\"" + datas.get("IMGDIR") + "/" + codes[i] + "\") ");
                    }
                }
            }
            datas.put(code, codevalue.toString());
            return css.toString().trim();
        } else {
            return "";
        }
    }

    public static void main(String[] arg) {
        Cache cache = new Cache("c:\\");
        try {
            cache.updatecache();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({ "unchecked", "static-access" })
    private Map advertisement(String range) {
        Map advs = new HashMap();
        int timestamp = Common.time();
        List<Map<String, String>> advertisements = dataBaseDao.executeQuery("SELECT advid,type,targets,parameters,code FROM " + tablepre + "advertisements WHERE available>'0' AND starttime<='" + timestamp + "' and (endtime ='0' or endtime >='" + timestamp + "') ORDER BY displayorder");
        if (advertisements != null && advertisements.size() > 0) {
            Map<String, String> itemsMap = new HashMap<String, String>();
            Map<String, Map<String, String>> typesMap = new HashMap<String, Map<String, String>>();
            for (Map<String, String> adv : advertisements) {
                String type = adv.get("type");
                String advid = adv.get("advid");
                String code = adv.get("code").replaceAll("\r\n", " ");
                code = code.replace("\\", "\\\\");
                Map<String, String> parameters = new HashMap<String, String>();
                if ("footerbanner".equals(type) || "thread".equals(type)) {
                    parameters = dataParse.characterParse(adv.get("parameters"), false);
                    type += (parameters.get("position") != null && parameters.get("position").matches("^(2|3)$") ? parameters.get("position") : "1");
                }
                adv.put("targets", (adv.get("targets").equals("") || adv.get("targets").equals("all")) ? (type.equals("text") ? "forum" : (type.length() > 6 && type.substring(0, 6).equals("thread") ? "forum" : "all")) : adv.get("targets"));
                String[] targets = adv.get("targets").split("\t");
                if (targets != null && targets.length > 0) {
                    for (String target : targets) {
                        target = ("0".equals(target) ? "index" : ("all".equals(target) || "index".equals(target) || "forumdisplay".equals(target) || "viewthread".equals(target) || "register".equals(target) || "redirect".equals(target) || "archiver".equals(target) ? target : ("forum".equals(target) ? "forum_all" : "forum_" + target)));
                        if ((("forumdisplay".equals(range) && !("thread".equals(adv.get("type")) || "interthread".equals(adv.get("type")))) || "viewthread".equals(range)) && (target.length() > 6 && target.substring(0, 6).equals("forum_"))) {
                            if ("thread".equals(adv.get("type"))) {
                                String displayorder = parameters.get("displayorder");
                                String[] displayorders = displayorder != null && !displayorder.trim().equals("") ? displayorder.split("\t") : new String[] { "0" };
                                for (String postcount : displayorders) {
                                    postcount = postcount.trim();
                                    Map<String, String> targetMap = typesMap.get(type + "_" + postcount);
                                    if (targetMap == null) {
                                        targetMap = new HashMap<String, String>();
                                    }
                                    targetMap.put(target, targetMap.get(target) != null ? targetMap.get(target) + "," + advid : advid);
                                    typesMap.put(type + "_" + postcount, targetMap);
                                }
                            } else {
                                Map<String, String> targetMap = typesMap.get(type);
                                if (targetMap == null) {
                                    targetMap = new HashMap<String, String>();
                                }
                                targetMap.put(target, targetMap.get(target) != null ? targetMap.get(target) + "," + advid : advid);
                                typesMap.put(type, targetMap);
                            }
                            itemsMap.put(advid, code);
                        } else if ("all".equals(range) && ("all".equals(target) || "redirect".equals(target))) {
                            Map targetMap = (Map) advs.get(target);
                            if (targetMap == null) {
                                targetMap = new HashMap();
                            }
                            Map<String, Map<String, String>> typeMap = (Map<String, Map<String, String>>) targetMap.get("type");
                            Map<String, String> itemMap = (Map<String, String>) targetMap.get("items");
                            if (typeMap == null) {
                                typeMap = new HashMap<String, Map<String, String>>();
                                itemMap = new HashMap<String, String>();
                            }
                            Map<String, String> typeitems = typeMap.get(type);
                            if (typeitems == null) {
                                typeitems = new HashMap<String, String>();
                            }
                            typeitems.put("all", typeitems.get("all") != null ? typeitems.get("all") + "," + advid : advid);
                            typeMap.put(type, typeitems);
                            itemMap.put(advid, code);
                            if (typeMap.size() > 0 && itemMap.size() > 0) {
                                targetMap.put("type", typeMap);
                                targetMap.put("items", itemMap);
                            }
                            if (targetMap.size() > 0) {
                                advs.put(target, targetMap);
                            }
                            typeMap = null;
                        } else if ("index".equals(range) && "intercat".equals(type)) {
                            parameters = dataParse.characterParse(adv.get("parameters"), false);
                            String position = parameters.get("position");
                            if (position == null || position.equals("")) {
                                position = "0";
                            }
                            String[] positions = position.trim().split(",");
                            Map<String, String> positionMap = (Map<String, String>) typesMap.get(type);
                            if (positionMap == null) {
                                positionMap = new HashMap<String, String>();
                            }
                            for (String obj : positions) {
                                positionMap.put(obj.trim(), positionMap.get(obj.trim()) != null ? positionMap.get(obj.trim()) + "," + advid : advid);
                                itemsMap.put(advid, code);
                            }
                            typesMap.put(type, positionMap);
                        } else if (target.equals(range) || ("index".equals(range) && "forum_all".equals(target))) {
                            Map<String, String> advtypeMap = (Map<String, String>) typesMap.get(type);
                            if (advtypeMap == null) {
                                advtypeMap = new HashMap<String, String>();
                            }
                            advtypeMap.put("0", advtypeMap.get("0") != null ? advtypeMap.get("0") + "," + advid : advid);
                            itemsMap.put(advid, code);
                            typesMap.put(type, advtypeMap);
                        }
                    }
                }
                if (itemsMap.size() > 0 && typesMap.size() > 0) {
                    advs.put("items", itemsMap);
                    advs.put("type", typesMap);
                }
            }
            itemsMap = null;
            typesMap = null;
        }
        advertisements = null;
        return advs;
    }
}
