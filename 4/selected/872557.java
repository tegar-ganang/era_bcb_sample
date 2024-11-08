package cn.chengdu.in.android.app;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import cn.chengdu.in.android.App;
import cn.chengdu.in.android.util.AndroidUtil;
import cn.chengdu.in.api.ApiManager;
import cn.chengdu.in.type.ConnectResult;

/**
 * 获取用户状态
 * @author Declan.Z(declan.zhang@gmail.com)
 * @date 2011-6-28
 */
public class UserStatusTask extends AsyncTask<Void, Void, ConnectResult> {

    private OnUserStatusListener mOnUserStatusListener;

    private Service mContext;

    private App mApp;

    public UserStatusTask(Service context, OnUserStatusListener l) {
        mOnUserStatusListener = l;
        mContext = context;
        mApp = (App) mContext.getApplicationContext();
    }

    @Override
    protected ConnectResult doInBackground(Void... params) {
        try {
            ApiManager api = mApp.getApiManager();
            return api.connect(AndroidUtil.getDeviceId(mContext), AndroidUtil.getChannelId(mContext) + "");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(ConnectResult result) {
        if (result != null) {
            mApp.updateUserStatus(result);
            mOnUserStatusListener.onUserStatusUpdateFinish();
            if (result.getVersion() != null) {
                mContext.sendBroadcast(new Intent(App.INTENT_ACTION_VERSION_UPDATE).putExtra("version", result.getVersion()));
            }
        }
    }

    public interface OnUserStatusListener {

        public void onUserStatusUpdateFinish();
    }
}
