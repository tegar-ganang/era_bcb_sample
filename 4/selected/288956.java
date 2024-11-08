package netposa.network;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import netposa.entity.AbstractBaseEntity;
import netposa.entity.SDKVersionEntity;
import netposa.npm.UserBaseInfo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import android.util.Log;

public class BaseNetOperation implements INetOperation {

    public static final String TAG = "NPMPlayer";

    public AbstractBaseEntity start(final NetOperationParam param) {
        final int mNOCode = param.getmNOCode();
        ArrayList<String> params = param.getParams();
        AbstractBaseEntity tmp = null;
        switch(mNOCode) {
            case LOGIN:
                tmp = login(params.get(0), params.get(1));
                break;
            case START_TRANSFER:
                tmp = startTransfer(params.get(0), params.get(1), params.get(2));
                break;
            case GET_USER_FROM:
                tmp = getUserFrom();
                break;
            case GET_CHANNEL:
                tmp = getChannel(params.get(0));
                break;
            case GET_DOWN_ADDR:
                tmp = getDownAddr(params.get(0));
                break;
            case GET_UP_ADDR:
                tmp = getUpAddr(params.get(0));
                break;
            case SEND_PTZ_CONTROL:
                tmp = sendPTZControl(params.get(0), params.get(1), params.get(2));
                break;
            case GET_HISTORY_GROUP:
                tmp = getHistoryGroup();
                break;
            case GET_HISTORY_FILE:
                tmp = getHistoryFile(params.get(0));
                break;
            case GET_ALL_POINT:
                tmp = getAllPoint();
                break;
            case GET_USER_GROUP:
                tmp = getUserGroup();
                break;
            case GET_PERMISSION_ID:
                tmp = getPermissionId(params.get(0));
                break;
            case GET_SDK_VERSION:
                tmp = getSDKVersion();
                break;
            case LOGOUT:
                tmp = logout();
                break;
            case GET_AHSS:
                tmp = getAhss();
                break;
            case GET_RTSP:
                tmp = getRtsp();
                break;
            case GET_TCS:
                tmp = getTcs();
                break;
            case GET_PLAN_GROUP:
                tmp = getPlanGroup();
                break;
            case GET_PLAN_CHANNEL:
                tmp = getPlanChannel(params.get(0));
                break;
            case GET_SUDDEN_CHANNEL:
                tmp = getSuddenChannel();
                break;
            case GET_PLAN_ENABLE:
                tmp = getPlanEnable();
                break;
            case GO_PLAYER:
                tmp = goPlayer(params.get(0));
                break;
            case GET_PREVIEW:
                tmp = getPreview(params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), params.get(5));
                break;
            default:
                break;
        }
        return tmp;
    }

    protected byte[] sendRequest(String uri) {
        byte[] strRet;
        try {
            HttpParams httpParameters = new BasicHttpParams();
            HttpClientParams.setRedirecting(httpParameters, true);
            HttpClientParams.setCookiePolicy(httpParameters, CookiePolicy.BROWSER_COMPATIBILITY);
            DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
            if (UserBaseInfo.cookieStore != null) {
                httpclient.setCookieStore(UserBaseInfo.cookieStore);
            }
            HttpResponse response;
            HttpGet httpget = new HttpGet(uri);
            response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                strRet = EntityUtils.toByteArray(entity);
                Log.d(TAG, uri + ":" + response.getStatusLine().toString());
                Log.d(TAG, "length:" + strRet.length);
                UserBaseInfo.cookieStore = httpclient.getCookieStore();
                return strRet;
            } else {
                Log.d(TAG, uri + ":" + response.getStatusLine().toString());
                strRet = "null".getBytes();
                return strRet;
            }
        } catch (Exception e) {
            strRet = "null".getBytes();
            e.printStackTrace();
        }
        return "null".getBytes();
    }

    @Override
    public AbstractBaseEntity getSDKVersion() {
        String u = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetVersion.php";
        SDKVersionEntity entity = new SDKVersionEntity();
        try {
            entity.parse(new String(sendRequest(u), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity login(String username, String password) {
        return null;
    }

    @Override
    public AbstractBaseEntity logout() {
        return null;
    }

    @Override
    public AbstractBaseEntity getChannel(String addr) {
        return null;
    }

    @Override
    public AbstractBaseEntity getAhss() {
        return null;
    }

    @Override
    public AbstractBaseEntity getRtsp() {
        return null;
    }

    @Override
    public AbstractBaseEntity getTcs() {
        return null;
    }

    @Override
    public AbstractBaseEntity startTransfer(String channelcode, String type, String networktype) {
        return null;
    }

    @Override
    public AbstractBaseEntity getUserFrom() {
        return null;
    }

    @Override
    public AbstractBaseEntity getDownAddr(String addr) {
        return null;
    }

    @Override
    public AbstractBaseEntity getUpAddr(String addr) {
        return null;
    }

    @Override
    public AbstractBaseEntity sendPTZControl(String code, String pmd, String par) {
        return null;
    }

    @Override
    public AbstractBaseEntity getAllPoint() {
        return null;
    }

    @Override
    public AbstractBaseEntity getHistoryGroup() {
        return null;
    }

    @Override
    public AbstractBaseEntity getHistoryFile(String historygroup) {
        return null;
    }

    @Override
    public AbstractBaseEntity getUserGroup() {
        return null;
    }

    @Override
    public AbstractBaseEntity getPermissionId(String usergroup) {
        return null;
    }

    @Override
    public AbstractBaseEntity getSuddenChannel() {
        return null;
    }

    @Override
    public AbstractBaseEntity getPlanGroup() {
        return null;
    }

    @Override
    public AbstractBaseEntity getPlanChannel(String plangroup) {
        return null;
    }

    @Override
    public AbstractBaseEntity goPlayer(String url) {
        return null;
    }

    public BaseNetOperation creatNew() {
        return new BaseNetOperation();
    }

    @Override
    public AbstractBaseEntity getPlanEnable() {
        return null;
    }

    @Override
    public AbstractBaseEntity getPreview(String code, String width, String height, String scale, String viewtime, String isopen) {
        return null;
    }
}
