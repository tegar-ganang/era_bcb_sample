package com.busfm.activity;

import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;
import com.busfm.BusfmApplication;
import com.busfm.R;
import com.busfm.listener.NetResponseListener;
import com.busfm.model.ChannelList;
import com.busfm.net.NetWorkHelper;
import com.busfm.net.NetWorkManager;
import com.busfm.util.Constants;
import com.busfm.util.DialogUtil;
import com.busfm.util.LogUtil;
import com.busfm.util.PrefUtil;
import com.busfm.util.DialogUtil.OnWarningOkCanceDialogListener;
import com.busfm.util.InstrumentUtil;
import com.mobclick.android.MobclickAgent;

/**
 * <p>
 * Title:SplashActivity
 * </p>
 * <p>
 * Description: SplashActivity,Mainly used to initial channel of bus.fm
 * </p>
 * <p>
 * Copyright (c) 2011 www.bus.fm Inc. All rights reserved.
 * </p>
 * <p>
 * Company: bus.fm
 * </p>
 * 
 * 
 * @author jingguo0@gmail.com
 * 
 */
public class SplashActivity extends BaseActivity implements OnWarningOkCanceDialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.splash);
        PrefUtil.init(this);
        initData();
        MobclickAgent.updateOnlineConfig(this);
        MobclickAgent.onError(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initData() {
        NetWorkManager.getChannelList(this);
    }

    @Override
    public void clientDidGetChannelList(NetResponseListener mClientListener, ChannelList channelList) {
        BusfmApplication.getInstance().setChannelList(channelList);
        InstrumentUtil.launchHomeActity(this, channelList);
        if (!NetWorkHelper.isWifiWorking()) {
            Toast.makeText(this, getString(R.string.no_wifi), Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void clientDidFailWithError(NetResponseListener mClientListener, int mOp, int scUnknown, String localizedMessage) {
        try {
            DialogUtil.createInfoDialog(this, 0, getString(R.string.dialog_title_tips), getString(R.string.dialog_net_error), getString(R.string.dialog_set), getString(R.string.dialog_quit), this).show();
        } catch (Exception e) {
            LogUtil.e(Constants.TAG, e.getMessage());
        }
    }

    @Override
    public void onWarningDialogOK(int id) {
        InstrumentUtil.launchNetWorkSetActivity(this);
        finish();
    }

    @Override
    public void onWarningCancel(int id) {
        finish();
    }
}
