package com.busfm.net;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import com.busfm.listener.NetResponseListener;
import com.busfm.model.ChannelList;
import com.busfm.model.PlayList;
import com.busfm.model.ResultEntity;
import com.busfm.model.UserEntity;
import com.busfm.provider.URLProvider;
import com.busfm.util.Constants;
import com.busfm.util.LogUtil;
import com.busfm.util.NetUtil;
import com.busfm.util.Utilities;
import android.R.integer;
import android.os.AsyncTask;
import android.text.TextUtils;

/**
 * @Description Send the HTTP request and get the response.
 * 
 * @author DJ
 * @version 1.0
 * @Date 2011/08/21
 */
public class NetWorkHandler {

    public class ClientTask extends AsyncTask<Void, Void, HttpResponse> {

        NetResponseListener mClientListener;

        private DefaultHttpClient mClient;

        private String url;

        int mOp;

        int statusCode;

        ClientTask(int op, NetResponseListener listener) {
            mOp = op;
            mClientListener = listener;
            url = "";
        }

        protected boolean shouldAddSessionId() {
            return true;
        }

        protected void onFinished(HttpResponse response) throws Exception {
        }

        protected String getRequestBody() {
            return null;
        }

        @Override
        protected HttpResponse doInBackground(Void... params) {
            mClient = NetWorkHelper.createDefaultHttpClient();
            try {
                setRequestURL();
                final String body = getRequestBody();
                LogUtil.i(Constants.TAG, "Url: " + url);
                final boolean post = !TextUtils.isEmpty(body);
                mLastOp = post ? new HttpPost(url) : new HttpGet(url);
                if (post) {
                    final HttpPost httpPost = (HttpPost) mLastOp;
                    StringEntity entity = new StringEntity(body, "UTF-8");
                    entity.setContentType("text/xml; charset=UTF-8");
                    httpPost.setEntity(entity);
                }
                return mClient.execute(mLastOp);
            } catch (Exception e) {
                LogUtil.i(Constants.TAG, "doInBackground:" + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            clientDidStart(mClientListener, mOp);
        }

        @Override
        protected void onPostExecute(HttpResponse response) {
            try {
                if (response == null) {
                    clientDidFailWithError(mClientListener, mOp, NetUtil.SC_UNKNOWN, "Unknown error");
                    LogUtil.i(Constants.TAG, "clientDidFailWithError: Unknown error");
                } else {
                    this.statusCode = response.getStatusLine().getStatusCode();
                    LogUtil.i(Constants.TAG, "statusCode: " + statusCode);
                    if (statusCode == 401) {
                        clientDidRequireAuthentication(mClientListener);
                    } else if (statusCode >= 300) {
                        clientDidFailWithError(mClientListener, mOp, statusCode, getHttpError(statusCode));
                    } else if (statusCode == 207) {
                        clientNoEnoughCredit(mClientListener, mOp);
                    } else if (statusCode == 204) {
                        clientNoNeedUpdate(mClientListener, mOp);
                    } else {
                        try {
                            onFinished(response);
                        } catch (Exception e) {
                            e.printStackTrace();
                            clientDidFailWithError(mClientListener, mOp, NetUtil.SC_UNKNOWN, e.getLocalizedMessage());
                        }
                    }
                }
            } finally {
                mClient.getConnectionManager().shutdown();
            }
        }

        byte[] getResponseBody(HttpEntity entity) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[2048];
                InputStream is = entity.getContent();
                for (int i = 0; i != -1; i = is.read(buf)) baos.write(buf, 0, i);
                return baos.toByteArray();
            } catch (Exception e) {
                return null;
            }
        }

        String getResponseBodyAsString(HttpEntity entity) {
            return Utilities.getUTF8String(getResponseBody(entity));
        }

        void setRequestURL() {
            url = URLProvider.getURL(mOp);
        }

        void setURL(String url) {
            this.url = url;
        }

        int getStatusCode() {
            return this.statusCode;
        }
    }

    public static final int TYPE_THEME = 0;

    public static final int TYPE_WIDGET = 1;

    public static final int SORT_BY_TIME = 0;

    public static final int SORT_BY_NAME = 1;

    public static final int SORT_BY_RATING = 2;

    public static final int SORT_BY_PRICE = 3;

    public static final int SORT_BY_DOWNLOAD = 4;

    private static final String[] TYPE_STRINGS = { "theme", "widget" };

    private HttpUriRequest mLastOp;

    private NetResponseListener mListener;

    private ClientTask mThread;

    void login(final String usermail, final String password) {
        mThread = new ClientTask(URLProvider.OP_LOGIN, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "Login response: " + body);
                final UserEntity userEnity = JsonParser.parserUserData(body);
                clientDidLogin(mClientListener, userEnity);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, usermail, password));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    void register(final String usermail, final String password, final String nickname) {
        mThread = new ClientTask(URLProvider.OP_REGISTER, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "Register response: " + body);
                ResultEntity resultEntity = StringParser.parserResult(body);
                clientDidRegister(mClientListener, resultEntity);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, usermail, password, nickname));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    void resetPassword(final String usermail) {
        mThread = new ClientTask(URLProvider.OP_RESET_PASSWORD, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "ResetPwd response: " + body);
                ResultEntity resultEntity = StringParser.parserResult(body);
                clientDidResetPwd(mClientListener, resultEntity);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, usermail));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    void changePassword(final int userid, final String oldpwd, final String newpwd) {
        mThread = new ClientTask(URLProvider.OP_CHANGE_PASSWORD, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "ChangePwd response: " + body);
                ResultEntity resultEntity = StringParser.parserResult(body);
                clientDidChangePwd(mClientListener, resultEntity);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, userid + "", oldpwd, newpwd));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    void checkUserMail(final String usermail) {
        mThread = new ClientTask(URLProvider.OP_CHECK_USER_EMAIL, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "CheckMail response: " + body);
                ResultEntity resultEntity = StringParser.parserResult(body);
                clientDidCheckUserMail(mClientListener, resultEntity);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, usermail));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    void checkNickName(final String nickname) {
        mThread = new ClientTask(URLProvider.OP_CHECK_NICKNAME, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "CheckNickName response: " + body);
                ResultEntity resultEntity = StringParser.parserResult(body);
                clientDidCheckNickName(mClientListener, resultEntity);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, nickname));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    void getChannelList() {
        mThread = new ClientTask(URLProvider.OP_GET_CHANNEL_LIST, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "ChannelList response: " + body);
                final ChannelList channelList = JsonParser.parserChannels(body);
                clientDidGetChannelList(mClientListener, channelList);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    public void getListByChannel(final int cid) {
        mThread = new ClientTask(URLProvider.OP_GET_LIST_BY_CHANNEL, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "SongList(Channel) response: " + body);
                final PlayList playList = JsonParser.parserSongs(body, false);
                clientDidGetPlayListByChannel(mClientListener, playList, cid);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, cid + ""));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    void getListByUserId(final int userid) {
        mThread = new ClientTask(URLProvider.OP_GET_LIST_USERID, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "SongList(UserID) response: " + body);
                final PlayList playList = JsonParser.parserSongs(body, true);
                clientDidGetPlayListByUserId(mClientListener, playList);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, userid + ""));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    void isFaved(final String userid, final String songid) {
        mThread = new ClientTask(URLProvider.OP_IS_FAVED, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "IsFaved response: " + body);
                ResultEntity resultEntity = StringParser.parserResult(body);
                clientDidSongIsFaved(mClientListener, resultEntity);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, userid, songid));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    void faveThis(final String userid, final String songid) {
        mThread = new ClientTask(URLProvider.OP_FAVE_THIS, mListener) {

            @Override
            protected void onFinished(HttpResponse response) throws Exception {
                final String body = getResponseBodyAsString(response.getEntity());
                LogUtil.i(Constants.TAG, "FaveThis response: " + body);
                ResultEntity resultEntity = StringParser.parserResult(body);
                clientDidFaveThis(mClientListener, resultEntity, songid);
            }

            @Override
            void setRequestURL() {
                setURL(URLProvider.getURL(mOp, userid, songid));
            }

            @Override
            protected boolean shouldAddSessionId() {
                return false;
            }
        };
        mThread.execute();
    }

    public void cancel(NetResponseListener listener) {
        if (mThread != null && mThread.mClientListener == listener) {
            mThread.mClientListener = null;
            try {
                mThread.cancel(true);
            } catch (Exception e) {
            }
            mThread = null;
        }
    }

    public void forceCancel() {
        if (mThread != null) {
            mThread.mClientListener = null;
            try {
                mThread.cancel(true);
            } catch (Exception e) {
            }
            mThread = null;
        }
    }

    public static int resolveType(String typeString) {
        for (int i = 0; i < TYPE_STRINGS.length; i++) {
            if (TYPE_STRINGS[i].equalsIgnoreCase(typeString)) return i;
        }
        return 0;
    }

    private void clientDidLogin(NetResponseListener mClientListener, UserEntity userEnity) {
        if (mClientListener != null) {
            mClientListener.clientDidLogin(mClientListener, userEnity);
        }
    }

    private void clientDidRegister(NetResponseListener mClientListener, ResultEntity resultEntity) {
        if (mClientListener != null) {
            mClientListener.clientDidRegister(mClientListener, resultEntity);
        }
    }

    private void clientDidResetPwd(NetResponseListener mClientListener, ResultEntity resultEntity) {
        if (mClientListener != null) {
            mClientListener.clientDidResetPwd(mClientListener, resultEntity);
        }
    }

    private void clientDidChangePwd(NetResponseListener mClientListener, ResultEntity resultEntity) {
        if (mClientListener != null) {
            mClientListener.clientDidChangePwd(mClientListener, resultEntity);
        }
    }

    private void clientDidCheckUserMail(NetResponseListener mClientListener, ResultEntity resultEntity) {
        if (mClientListener != null) {
            mClientListener.clientDidCheckUserMail(mClientListener, resultEntity);
        }
    }

    private void clientDidCheckNickName(NetResponseListener mClientListener, ResultEntity resultEntity) {
        if (mClientListener != null) {
            mClientListener.clientDidCheckNickName(mClientListener, resultEntity);
        }
    }

    private void clientDidGetChannelList(NetResponseListener mClientListener, ChannelList channelList) {
        if (mClientListener != null) {
            mClientListener.clientDidGetChannelList(mClientListener, channelList);
        }
    }

    private void clientDidGetPlayListByChannel(NetResponseListener mClientListener, PlayList playList, int cid) {
        if (mClientListener != null) {
            mClientListener.clientDidGetPlayListByChannel(mClientListener, playList, cid);
        }
    }

    private void clientDidGetPlayListByUserId(NetResponseListener mClientListener, PlayList playList) {
        if (mClientListener != null) {
            mClientListener.clientDidGetPlayListByUserId(mClientListener, playList);
        }
    }

    private void clientDidSongIsFaved(NetResponseListener mClientListener, ResultEntity resultEntity) {
        if (mClientListener != null) {
            mClientListener.clientDidSongIsFaved(mClientListener, resultEntity);
        }
    }

    private void clientDidFaveThis(NetResponseListener mClientListener, ResultEntity resultEntity, String songId) {
        if (mClientListener != null) {
            mClientListener.clientDidFaveThis(mClientListener, resultEntity, songId);
        }
    }

    public void clientDidStart(NetResponseListener mClientListener, int mOp) {
    }

    public void clientDidRequireAuthentication(NetResponseListener mClientListener) {
        if (mClientListener != null) {
            mClientListener.clientDidRequireAuthentication(mClientListener);
        }
    }

    public void clientNoEnoughCredit(NetResponseListener mClientListener, int mOp) {
        if (mClientListener != null) {
            mClientListener.clientNoEnoughCredit(mClientListener);
        }
    }

    public void clientNoNeedUpdate(NetResponseListener mClientListener, int mOp) {
        if (mClientListener != null) {
            mClientListener.clientNoNeedUpdate(mClientListener);
        }
    }

    public void clientDidFailWithError(NetResponseListener mClientListener, int mOp, int scUnknown, String localizedMessage) {
        if (mClientListener != null) {
            mClientListener.clientDidFailWithError(mClientListener, mOp, scUnknown, localizedMessage);
        }
    }

    private String getHttpError(int statusCode) {
        return null;
    }

    void setListener(NetResponseListener listener) {
        mListener = listener;
    }

    public NetResponseListener getListener() {
        return mListener;
    }
}
