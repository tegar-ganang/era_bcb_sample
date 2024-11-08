package com.dcivision.user.core;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.dcivision.framework.SessionContainer;
import com.dcivision.framework.SystemParameterConstant;
import com.dcivision.framework.SystemParameterFactory;
import com.dcivision.framework.Utility;
import com.dcivision.framework.taglib.channel.AjaxChannelPreferenceComparator;
import com.dcivision.user.bean.PersonalHomePreference;

public class PreferenceManager {

    public static final String[][] THEME_PREFERENCE_MAP = { { "user.label.preference_1", "1" }, { "user.label.preference_2", "2" } };

    public static final String[][] LANG_MAP = { { "user.label.locale_en_US", "en_US" }, { "user.label.locale_zh_HK", "zh_HK" }, { "user.label.locale_zh_CN", "zh_CN" } };

    private SessionContainer sessionContainer = null;

    private Connection conn = null;

    public PreferenceManager(SessionContainer sessionContainer, Connection conn) {
        this.sessionContainer = sessionContainer;
        this.conn = conn;
    }

    public List getChannelSequence(String channelStr) {
        String[] channels = channelStr.split("\\|");
        List ls = null;
        ls = Arrays.asList(channels);
        Collections.sort(ls, new AjaxChannelPreferenceComparator());
        return ls;
    }

    public List getSpecialChannelSequence(String channelStr) {
        List ls = getChannelSequence(channelStr);
        List result = new ArrayList();
        String shortcut = null;
        String recentUpdate = null;
        for (int i = 0; i < ls.size(); i++) {
            String str = (String) ls.get(i);
            if (str.indexOf(PersonalHomePreference.TOOLS) >= 0 && i < (ls.size() - 2)) {
                shortcut = (String) ls.get(i);
            } else if (str.indexOf(PersonalHomePreference.RECENTLY_ACCESSED_DOC) >= 0 && i < (ls.size() - 1)) {
                recentUpdate = (String) ls.get(i);
            } else {
                result.add(str);
            }
        }
        if (!Utility.isEmpty(shortcut)) {
            result.add(shortcut);
        }
        if (!Utility.isEmpty(recentUpdate)) {
            result.add(recentUpdate);
        }
        return result;
    }

    public String fitTheOldChannelConfig(String channeldisableViewAndViewStr) {
        StringBuffer bstr = new StringBuffer();
        String[] channels = new String[] { PersonalHomePreference.CALENDAR_MEETING_LIST, PersonalHomePreference.CALENDAR_TODO_LIST, PersonalHomePreference.PENDING_WORKFLOW_TASK, PersonalHomePreference.RECENTLY_ACCESSED_DOC, PersonalHomePreference.SYSTEM_LOGS, PersonalHomePreference.TOOLS, PersonalHomePreference.WORKFLOW_TRACKING_LIST };
        Pattern pattern = Pattern.compile("(\\D{2}_[YN]_\\d+_\\d+(\\|?)){" + channels.length + "}");
        Matcher matcher = pattern.matcher(channeldisableViewAndViewStr);
        if (matcher.matches()) {
            bstr.append(channeldisableViewAndViewStr);
        } else {
            String[] channelItems = channeldisableViewAndViewStr.split("\\|");
            for (int y = 0; y < channels.length; y++) {
                boolean isFound = false;
                for (int i = 0; i < channelItems.length; i++) {
                    if (channelItems[i].indexOf(channels[y]) >= 0) {
                        isFound = true;
                        if (y == channels.length - 1) bstr.append(fitOldChannelItemConfig(channelItems[i], false, y)); else bstr.append(fitOldChannelItemConfig(channelItems[i], false, y) + "|");
                        break;
                    }
                }
                if (!isFound) {
                    if (y == channels.length - 1) bstr.append(fitOldChannelItemConfig(channels[y], true, y)); else bstr.append(fitOldChannelItemConfig(channels[y], true, y) + "|");
                }
            }
        }
        return bstr.toString();
    }

    private String fitOldChannelItemConfig(String channelItem, boolean isDisplay, int seq) {
        Pattern patternItem = Pattern.compile("(\\D{2}_[YN]_\\d+_\\d+){1}");
        Matcher matcherItem = patternItem.matcher(channelItem);
        if (matcherItem.matches()) {
            return channelItem;
        } else {
            if (!isDisplay) return (channelItem + "_N_5_" + seq); else return (channelItem + "_Y_5_1" + seq);
        }
    }

    public String[] getChannelPreference(String channelPreferenceStr) {
        String[] preferences = new String[] { "", "" };
        if (channelPreferenceStr == null) return preferences;
        String[] tempChannelViewSytles = channelPreferenceStr.split("\\|");
        String disAbleViewStr = "";
        String ableViewStr = "";
        for (int i = 0; i < tempChannelViewSytles.length; i++) {
            if (tempChannelViewSytles[i].indexOf("_N_") == -1) ableViewStr = ableViewStr.equals("") ? tempChannelViewSytles[i] : ableViewStr + "|" + tempChannelViewSytles[i];
            if (tempChannelViewSytles[i].indexOf("_Y_") == -1) disAbleViewStr = disAbleViewStr.equals("") ? tempChannelViewSytles[i] : disAbleViewStr + "|" + tempChannelViewSytles[i];
        }
        preferences[0] = disAbleViewStr;
        preferences[1] = ableViewStr;
        return preferences;
    }

    public static boolean isValidPreference(Integer preference) {
        if (Utility.isEmpty(preference)) {
            return false;
        }
        if (preference.intValue() < 1 && preference.intValue() > 2) {
            return false;
        }
        return true;
    }

    public static Integer getValidPreference(Integer preference) {
        if (isValidPreference(preference)) {
            return preference;
        } else {
            String systemDefaultPreference = SystemParameterFactory.getSystemParameter(SystemParameterConstant.PREFERENCE);
            if (Utility.isEmpty(systemDefaultPreference)) {
                return new Integer(1);
            } else {
                preference = Integer.valueOf(systemDefaultPreference);
                if (isValidPreference(preference)) {
                    return preference;
                } else {
                    return new Integer(1);
                }
            }
        }
    }

    public static Integer getValidPreference(String preference) {
        Integer IntPreference = null;
        if (!Utility.isEmpty(preference)) {
            IntPreference = Integer.valueOf(preference);
        }
        return getValidPreference(IntPreference);
    }
}
