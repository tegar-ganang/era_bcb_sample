package com.busfm.provider;

public class URLProvider {

    private static final String host = "http://api.bus.fm/pt/";

    public static final int OP_LOGIN = 0;

    public static final int OP_REGISTER = OP_LOGIN + 1;

    public static final int OP_RESET_PASSWORD = OP_REGISTER + 1;

    public static final int OP_CHANGE_PASSWORD = OP_RESET_PASSWORD + 1;

    public static final int OP_CHECK_USER_EMAIL = OP_CHANGE_PASSWORD + 1;

    public static final int OP_CHECK_NICKNAME = OP_CHECK_USER_EMAIL + 1;

    public static final int OP_GET_CHANNEL_LIST = OP_CHECK_NICKNAME + 1;

    public static final int OP_GET_LIST_BY_CHANNEL = OP_GET_CHANNEL_LIST + 1;

    public static final int OP_GET_LIST_USERID = OP_GET_LIST_BY_CHANNEL + 1;

    public static final int OP_IS_FAVED = OP_GET_LIST_USERID + 1;

    public static final int OP_FAVE_THIS = OP_IS_FAVED + 1;

    public static final String Key = "e95fe882-0898-4789-8394-c879c8b8994f";

    private static final String[] URLS = { host + "login", host + "reg", host + "resetpwd", host + "changepwd", host + "checusermail", host + "checknickname", host + "getchannellist", host + "getlistbychannel", host + "getlistbyuserid", host + "isfaved", host + "favethis" };

    private static String getBaseURL(int opCode) {
        if (opCode < 0 || opCode > 10) {
            return "";
        } else {
            return URLS[opCode];
        }
    }

    public static String getURL(int opCode) {
        switch(opCode) {
            case OP_GET_CHANNEL_LIST:
                return getChannelListURL();
            default:
                return "";
        }
    }

    public static String getURL(int opCode, String id) {
        switch(opCode) {
            case OP_RESET_PASSWORD:
                return getResetPwdURL(id);
            case OP_CHECK_USER_EMAIL:
                return getCheckUserMailURL(id);
            case OP_CHECK_NICKNAME:
                return getCheckNickNameURL(id);
            case OP_GET_LIST_BY_CHANNEL:
                return getListByChannelURL(id);
            case OP_GET_LIST_USERID:
                return getListByUserIdURL(id);
            default:
                return "";
        }
    }

    public static String getURL(int opCode, String fp, String sp) {
        switch(opCode) {
            case OP_LOGIN:
                return getLoginURL(fp, sp);
            case OP_IS_FAVED:
                return getIsFavedURL(fp, sp);
            case OP_FAVE_THIS:
                return getFaveThisURL(fp, sp);
            default:
                return "";
        }
    }

    public static String getURL(int opCode, String fp, String sp, String tp) {
        switch(opCode) {
            case OP_REGISTER:
                return getRegisterURL(fp, sp, tp);
            case OP_CHANGE_PASSWORD:
                return getChangePwdURL(fp, sp, tp);
            default:
                return "";
        }
    }

    private static String getLoginURL(String usermail, String password) {
        return getBaseURL(OP_LOGIN) + "?usermail=" + usermail + "&userpwd=" + password;
    }

    private static String getRegisterURL(String usermail, String password, String nickname) {
        return getBaseURL(OP_REGISTER) + "?usermail=" + usermail + "&userpwd=" + password + "&nickname=" + nickname;
    }

    private static String getResetPwdURL(String username) {
        return getBaseURL(OP_RESET_PASSWORD) + "?usermail=" + username;
    }

    private static String getChangePwdURL(String userid, String oldpwd, String newpwd) {
        return getBaseURL(OP_CHANGE_PASSWORD) + "?userid=" + userid + "&oldpwd=" + oldpwd + "&newpwd=" + newpwd;
    }

    private static String getCheckUserMailURL(String usermail) {
        return getBaseURL(OP_CHECK_USER_EMAIL) + "?usermail=" + usermail;
    }

    private static String getCheckNickNameURL(String nickname) {
        return getBaseURL(OP_CHECK_NICKNAME) + "?nickname=" + nickname;
    }

    private static String getChannelListURL() {
        return getBaseURL(OP_GET_CHANNEL_LIST);
    }

    private static String getListByChannelURL(String channelId) {
        return getBaseURL(OP_GET_LIST_BY_CHANNEL) + "?channelid=" + channelId + "&appkey=" + Key;
    }

    private static String getListByUserIdURL(String userid) {
        return getBaseURL(OP_GET_LIST_USERID) + "?userid=" + userid + "&appkey=" + Key;
    }

    private static String getIsFavedURL(String userid, String songid) {
        return getBaseURL(OP_IS_FAVED) + "?userid=" + userid + "&songid=" + songid;
    }

    private static String getFaveThisURL(String userid, String songid) {
        return getBaseURL(OP_FAVE_THIS) + "?userid=" + userid + "&songid=" + songid;
    }
}
