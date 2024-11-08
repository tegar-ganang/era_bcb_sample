package com.busfm.net;

import java.util.HashMap;
import com.busfm.listener.NetResponseListener;

/**
 * @Decription Manager the HTTP request.
 * 
 * @author DJ
 * @version 1.0
 * @Date 2011/08/21
 */
public class NetWorkManager {

    private static HashMap<NetResponseListener, NetWorkHandler> handlerMap = new HashMap<NetResponseListener, NetWorkHandler>();

    public static NetWorkHandler myHandler;

    private static long lastLoginTime;

    public static final long TIME_OUT = 30 * 60 * 1000;

    public static void setLastLoginTime() {
        lastLoginTime = System.currentTimeMillis();
    }

    public static long getLastLoginTime() {
        return lastLoginTime;
    }

    private static void init(NetResponseListener listener) {
        if (!handlerMap.containsKey(listener)) {
            myHandler = new NetWorkHandler();
            myHandler.setListener(listener);
            handlerMap.put(listener, myHandler);
        } else {
            myHandler = handlerMap.get(listener);
        }
    }

    private static void redirect(NetResponseListener listener) {
        NetResponseListener otherListener = null;
        if (null != myHandler) {
            otherListener = myHandler.getListener();
        }
        init(listener);
        if (otherListener == listener) {
            myHandler.cancel(otherListener);
            init(listener);
        }
    }

    private static void forward(NetResponseListener listener) {
        NetResponseListener otherListener = null;
        if (null != myHandler) {
            otherListener = myHandler.getListener();
        }
        init(listener);
        if (otherListener != listener) {
            myHandler.setListener(listener);
            handlerMap.put(listener, myHandler);
        }
    }

    public static void onDestroy(NetResponseListener listener) {
        handlerMap.remove(listener);
    }

    public static NetWorkHandler getClient() {
        return myHandler;
    }

    public static void login(NetResponseListener listener, String usermail, String password) {
        forward(listener);
        myHandler.login(usermail, password);
    }

    public static void register(NetResponseListener listener, String usermail, String password, String nickname) {
        forward(listener);
        myHandler.register(usermail, password, nickname);
    }

    public static void resetPassword(NetResponseListener listener, String usermail) {
        forward(listener);
        myHandler.resetPassword(usermail);
    }

    public static void changePassword(NetResponseListener listener, int userid, String oldpwd, String newpwd) {
        forward(listener);
        myHandler.cancel(myHandler.getListener());
        myHandler.changePassword(userid, oldpwd, newpwd);
    }

    public static void checkUserMail(NetResponseListener listener, String usermail) {
        forward(listener);
        myHandler.checkUserMail(usermail);
    }

    public static void checkNickName(NetResponseListener listener, String nickname) {
        forward(listener);
        myHandler.checkNickName(nickname);
    }

    public static void getChannelList(NetResponseListener listener) {
        forward(listener);
        myHandler.getChannelList();
    }

    public static void getListByChannel(NetResponseListener listener, int cid) {
        redirect(listener);
        myHandler.getListByChannel(cid);
    }

    public static void getListByUserId(NetResponseListener listener, int userid) {
        forward(listener);
        myHandler.getListByUserId(userid);
    }

    public static void isFaved(NetResponseListener listener, int userid, String songid) {
        forward(listener);
        myHandler.isFaved(userid + "", songid);
    }

    public static void faveThis(NetResponseListener listener, int userid, String songid) {
        forward(listener);
        myHandler.faveThis(userid + "", songid);
    }
}
