package com.busfm.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import com.busfm.BusfmApplication;
import com.busfm.R;
import com.busfm.listener.NetResponseListener;
import com.busfm.media.PlayControllerListener;
import com.busfm.model.ChannelEntity;
import com.busfm.model.ChannelList;
import com.busfm.model.PlayList;
import com.busfm.model.ResultEntity;
import com.busfm.model.SongEntity;
import com.busfm.model.UserEntity;
import com.busfm.net.NetWorkManager;
import com.busfm.provider.URLProvider;
import com.busfm.provider.UserAccountManager;
import com.busfm.service.IplayServiceStub;
import com.busfm.service.PlayerService;
import com.busfm.util.AsyncImageLoader;
import com.busfm.util.Constants;
import com.busfm.util.DialogUtil;
import com.busfm.util.AsyncImageLoader.onImageDownloadedListener;
import com.busfm.util.DialogUtil.OnWarningDialogListener;
import com.busfm.util.DialogUtil.OnWarningOkCanceDialogListener;
import com.busfm.util.InstrumentUtil;
import com.busfm.util.LogUtil;
import com.busfm.util.PrefUtil;
import com.busfm.util.Utilities;
import com.busfm.widget.FlingTab;
import com.busfm.widget.FlingTab.OnItemClickListener;
import com.feedback.NotificationType;
import com.feedback.UMFeedbackService;
import com.mobclick.android.MobclickAgent;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

/**
 * <p>
 * Title:HomeActivity
 * </p>
 * <p>
 * Description: HomeActivity
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
@SuppressWarnings("deprecation")
public class HomeActivity extends BaseActivity implements OnClickListener, OnItemClickListener, OnWarningOkCanceDialogListener, onImageDownloadedListener, OnWarningDialogListener, OnTouchListener, SensorListener {

    private static final int UnknowErrorDialog = 1;

    private static final int LoginDialog = UnknowErrorDialog + 1;

    private static final int NetWorkErrorDailog = LoginDialog + 1;

    private static final int TimerPickerDialog = NetWorkErrorDailog + 1;

    private int mYear;

    private int mMonth;

    private int mDay;

    private int mHour;

    private int mMinute;

    FlingTab channelGallery;

    ImageView coverImageView;

    ImageView weiboImageView;

    ProgressBar mProgressBar;

    ProgressBar mLoadingBar;

    ViewAnimator viewAnimator;

    RadioGroup rbChannel;

    TextView tvPrivateTitle;

    TextView tvAuthor;

    TextView tvTitle;

    TextView tvTime;

    TextView tvNoLoginFavoriate;

    TextView tvFavoriateSuccess;

    LinearLayout loadingView;

    ImageView ivIntroduction;

    LinearLayout weiboView;

    ProgressBar mFavoriateProgressBar;

    ImageButton btnNext;

    private SensorManager sensorMgr;

    private Animation mSlideInAnimation;

    private Animation mSlideOutAnimation;

    private Animation mFadeInAnimation;

    private Animation mFadeOutAnimation;

    private PlayList mPlayList;

    private IplayServiceStub mServiceStub;

    private ChannelList mChannelList;

    private ChannelEntity mPriaveChannelEntity;

    private int mCurrentID;

    private ChannelAdapter mChannelAdapter;

    private NotificationManager notificationManager;

    private VelocityTracker mVelocityTracker;

    private float startX;

    private long downTime;

    private boolean isCheck = false;

    private boolean isPrivate = false;

    private boolean isFirst = true;

    private boolean isDouble = false;

    private boolean isBackground = false;

    private boolean isFavoriate = false;

    private boolean isGetListFailed = false;

    private boolean isRecycled = false;

    private static final int SHAKE_THRESHOLD = 4000;

    private float x, y, z, last_x, last_y, last_z;

    private long lastUpdate;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        PrefUtil.init(this);
        LogUtil.i(Constants.TAG, "onCreate()");
        MobclickAgent.update(this);
        UMFeedbackService.enableNewReplyNotification(this, NotificationType.NotificationBar);
        sensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        initCalendar();
        initView();
        setupView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        isBackground = false;
        if (null == mChannelList) {
            notificationManager.cancel(Constants.NotifyID);
            Toast.makeText(this, getString(R.string.dialog_unknow_error), Toast.LENGTH_SHORT).show();
            android.os.Process.killProcess(android.os.Process.myPid());
            return;
        }
        LogUtil.i(Constants.TAG, "OnResume");
        Intent intent = new Intent(this, PlayerService.class);
        getApplicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        BusfmApplication.getInstance().setPlayerListener(mPlayControllerListener);
    }

    @Override
    protected void onStop() {
        try {
            getApplicationContext().unbindService(mServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.i(Constants.TAG, "onDestroy");
        BusfmApplication.getInstance().setPlayerListener(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusfmApplication.getInstance().setCurrentID(mCurrentID);
        isBackground = true;
        MobclickAgent.onPause(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initView() {
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);
        mFavoriateProgressBar = (ProgressBar) findViewById(R.id.favoriate_loading_progress);
        ivIntroduction = (ImageView) findViewById(R.id.introduction);
        ivIntroduction.setOnClickListener(this);
        if (PrefUtil.getFirstEnter()) {
            PrefUtil.setFirstEnter(false);
            ivIntroduction.setVisibility(View.VISIBLE);
        } else {
            ivIntroduction.setVisibility(View.GONE);
        }
        tvTime = (TextView) findViewById(R.id.time);
        tvAuthor = (TextView) findViewById(R.id.tv_author);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        channelGallery = (FlingTab) findViewById(R.id.chanell);
        coverImageView = (ImageView) findViewById(R.id.cover);
        coverImageView.setOnTouchListener(this);
        weiboImageView = (ImageView) findViewById(R.id.weibo);
        mProgressBar = (ProgressBar) findViewById(R.id.ProgressBar);
        viewAnimator = (ViewAnimator) findViewById(R.id.viewAnimate);
        mLoadingBar = (ProgressBar) findViewById(R.id.pb_progress);
        rbChannel = (RadioGroup) findViewById(R.id.rg_channel);
        tvPrivateTitle = (TextView) findViewById(R.id.channel_private);
        tvNoLoginFavoriate = (TextView) findViewById(R.id.no_login_favoriate);
        tvFavoriateSuccess = (TextView) findViewById(R.id.favoriate_success);
        loadingView = (LinearLayout) findViewById(R.id.loading);
        weiboView = (LinearLayout) findViewById(R.id.ll_weibo);
        mSlideInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_up);
        mSlideInAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (isFavoriate) {
                    tvFavoriateSuccess.setVisibility(View.VISIBLE);
                    return;
                }
                tvNoLoginFavoriate.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isFavoriate) {
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            if (mSlideInAnimation.hasEnded()) {
                                tvFavoriateSuccess.startAnimation(mSlideOutAnimation);
                            }
                        }
                    }, 1000);
                }
            }
        });
        mSlideOutAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_down);
        mSlideOutAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (isFavoriate) {
                    tvFavoriateSuccess.startAnimation(mSlideOutAnimation);
                    return;
                }
                tvNoLoginFavoriate.startAnimation(mSlideOutAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isFavoriate) {
                    tvFavoriateSuccess.setVisibility(View.GONE);
                    return;
                }
                tvNoLoginFavoriate.setVisibility(View.GONE);
            }
        });
        mFadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mFadeInAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                weiboView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (mFadeInAnimation.hasEnded()) weiboView.startAnimation(mFadeOutAnimation);
                    }
                }, 7500);
            }
        });
        mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        mFadeOutAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                weiboView.setAnimation(mFadeOutAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                weiboView.setVisibility(View.GONE);
            }
        });
    }

    private void initCalendar() {
        Calendar calendar = Calendar.getInstance();
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
    }

    private void setupView() {
        refreshChannel(0);
        mLoadingBar.setVisibility(View.VISIBLE);
        findViewById(R.id.weibo).setOnClickListener(this);
        findViewById(R.id.btnFavorate).setOnClickListener(this);
        findViewById(R.id.btnPause).setOnClickListener(this);
        tvNoLoginFavoriate.setOnClickListener(this);
        channelGallery.setOnItemClickListener(this);
        rbChannel.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (isCheck) {
                    isCheck = false;
                    return;
                }
                if (isDouble) {
                    isDouble = false;
                    return;
                }
                btnNext.setEnabled(true);
                switch(checkedId) {
                    case R.id.rb_public:
                        {
                            LogUtil.i(Constants.TAG, "Public is Clicked");
                            viewAnimator.setDisplayedChild(0);
                            if (null != channelGallery) channelGallery.setDefaultTab(mCurrentID - 1);
                            break;
                        }
                    case R.id.rb_private:
                        {
                            LogUtil.i(Constants.TAG, "Private is Clicked");
                            if (!UserAccountManager.isSignIn(HomeActivity.this)) {
                                showDialog(LoginDialog);
                                return;
                            }
                            initPrivate();
                            break;
                        }
                    default:
                        break;
                }
            }
        });
    }

    /**
     * initial private channel
     */
    private void initPrivate() {
        MobclickAgent.onEvent(this, Constants.CHANNEL_CLICK_RATE, "99");
        LogUtil.i(Constants.TAG, "InitPrivate()");
        viewAnimator.setDisplayedChild(1);
        mLoadingBar.setVisibility(View.VISIBLE);
        UserEntity userEntity = UserAccountManager.getUserData(HomeActivity.this);
        tvPrivateTitle.setText(userEntity.getMemberNickName());
        if (mPriaveChannelEntity != null) {
            mPlayList = null;
            NetWorkManager.getListByUserId(HomeActivity.this, Integer.valueOf(userEntity.getMemberID()));
        }
    }

    private void initData() {
        BusfmApplication.getInstance().setPlayerListener(mPlayControllerListener);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * Get Current SongEntity
     * 
     * @return
     */
    private SongEntity getCurrentSongEntity() {
        if (null != mPlayList) {
            return mPlayList.getSelectedEntity();
        }
        return null;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceStub = null;
            LogUtil.i(Constants.TAG, "onServiceDisconnected");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.i(Constants.TAG, "onServiceConnected");
            mServiceStub = IplayServiceStub.Stub.asInterface(service);
        }
    };

    private PlayControllerListener mPlayControllerListener = new PlayControllerListener() {

        @Override
        public void onTrackStreamError() {
            mLoadingBar.setVisibility(View.GONE);
            showDialog(NetWorkErrorDailog);
        }

        @Override
        public void onTrackStop() {
        }

        @Override
        public boolean onTrackStart() {
            return true;
        }

        @Override
        public void onTrackProgress(int seconds) {
            tvTime.setText(Utilities.secondsToString(seconds));
            mProgressBar.setMax(BusfmApplication.getInstance().getDuration());
            mProgressBar.setProgress(seconds);
        }

        @Override
        public void onTrackPause() {
            findViewById(R.id.btnPause).setBackgroundResource(R.drawable.btn_play_selector);
        }

        @Override
        public void onTrackChanged(SongEntity songEntity) {
            btnNext.setEnabled(true);
            if (null == songEntity) {
                return;
            }
            if (tvNoLoginFavoriate.getVisibility() == View.VISIBLE) {
                tvNoLoginFavoriate.startAnimation(mSlideOutAnimation);
            }
            if (songEntity.getIsFavoriate()) {
                findViewById(R.id.btnFavorate).setBackgroundResource(R.drawable.favorite_selected);
            } else {
                findViewById(R.id.btnFavorate).setBackgroundResource(R.drawable.btn_favorite_selector);
            }
            findViewById(R.id.btnPause).setBackgroundResource(R.drawable.btn_pasue_selector);
            mFavoriateProgressBar.setVisibility(View.GONE);
            mLoadingBar.setVisibility(View.VISIBLE);
            notifyUpdateCover(songEntity.thumb);
            if (isGetListFailed) {
                onUpdatePlayList();
            }
            tvTitle.setText(songEntity.getTitle());
            tvAuthor.setText(songEntity.getArtist());
            tvFavoriateSuccess.setVisibility(View.GONE);
            tvNoLoginFavoriate.setVisibility(View.GONE);
        }

        @Override
        public void onTrackBuffering(int percent) {
        }

        @Override
        public void onUpdatePlayList() {
            NetWorkManager.getListByChannel(HomeActivity.this, getCurrentCID());
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.i(Constants.TAG, "onActivityResult Private : " + isPrivate);
        if (requestCode == Constants.LOGINREQUESTCODE) {
            LogUtil.i(Constants.TAG, resultCode);
            if (resultCode == RESULT_OK) {
                initPrivate();
                isDouble = false;
            } else if (resultCode == RESULT_CANCELED) {
                LogUtil.i(Constants.TAG, "onActivityResult RBPUBLIC is Click");
                rbChannel.check(R.id.rb_public);
            }
        }
    }

    /**
     * 刷新歌曲封面
     */
    private void notifyUpdateCover(String url) {
        AsyncImageLoader.getInstance().loadImage(url, coverImageView, R.drawable.cover, this);
    }

    /**
     * 收藏歌曲
     */
    private void doWithFavoriate() {
        if (null == mPlayList || mPlayList.size() == 0) {
            Toast.makeText(this, getString(R.string.loading_playlist), Toast.LENGTH_SHORT).show();
            return;
        }
        mFavoriateProgressBar.setVisibility(View.VISIBLE);
        NetWorkManager.faveThis(this, UserAccountManager.getUid(this), mPlayList.getSelectedEntity().getSongId());
    }

    /**
     * 刷新频道列表
     */
    private void refreshChannel(int defaultId) {
        ChannelList channelList = (ChannelList) getIntent().getSerializableExtra(Constants.EXTRA_KEY_CHANNELLIST_ENTITY);
        if (null == channelList) {
            channelList = BusfmApplication.getInstance().getChannelList();
        }
        if (null == channelList) {
            return;
        }
        mChannelList = channelList;
        LogUtil.i(Constants.TAG, mChannelList.toString());
        viewAnimator.setDisplayedChild(0);
        mPriaveChannelEntity = mChannelList.removePrivateList();
        BusfmApplication.getInstance().setPrivateEntitiy(mPriaveChannelEntity);
        mChannelAdapter = new ChannelAdapter(mChannelList.getchannelList());
        channelGallery.setAdapter(mChannelAdapter);
        channelGallery.setDefaultTab(defaultId);
    }

    private int getCurrentCID() {
        int mCurrentID = -1;
        if (null != channelGallery) {
            mCurrentID = (mChannelAdapter.getChannelEntity().get(channelGallery.getFocus()).getCID());
        }
        return mCurrentID;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        if (UserAccountManager.isSignIn(this)) {
            inflater.inflate(R.menu.main_menu_out, menu);
        } else {
            inflater.inflate(R.menu.main_menu_in, menu);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_login:
                Intent intent = new Intent();
                intent.setClass(this, LoginActivity.class);
                startActivity(intent);
                break;
            case R.id.menu_logout:
                UserAccountManager.removeAccount(this);
                isFavoriate = false;
                break;
            case R.id.menu_out:
                try {
                    mServiceStub.stop();
                    exit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.menu_feedback:
                UMFeedbackService.openUmengFeedbackSDK(this);
                break;
            case R.id.menu_about:
                InstrumentUtil.launchAboutActivity(this);
                break;
            case R.id.menu_stop:
                showDialog(TimerPickerDialog);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        try {
            switch(v.getId()) {
                case R.id.weibo:
                    doShared();
                    break;
                case R.id.btnPause:
                    if (mServiceStub.isPlaying()) {
                        mServiceStub.pause();
                        findViewById(R.id.btnPause).setBackgroundResource(R.drawable.btn_play_selector);
                    } else {
                        mServiceStub.play();
                        findViewById(R.id.btnPause).setBackgroundResource(R.drawable.btn_pasue_selector);
                    }
                    break;
                case R.id.btnNext:
                    doWithNextButton();
                    break;
                case R.id.btnFavorate:
                    if (UserAccountManager.isSignIn(this)) {
                        doWithFavoriate();
                    } else {
                        tvNoLoginFavoriate.setVisibility(View.VISIBLE);
                        tvNoLoginFavoriate.startAnimation(mSlideInAnimation);
                    }
                    break;
                case R.id.no_login_favoriate:
                    tvNoLoginFavoriate.startAnimation(mSlideOutAnimation);
                    break;
                case R.id.introduction:
                    ivIntroduction.setVisibility(View.GONE);
                default:
                    break;
            }
        } catch (Exception e) {
            LogUtil.e(Constants.TAG, e.getMessage());
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch(id) {
            case UnknowErrorDialog:
                return DialogUtil.CreateInfoDialog(this, UnknowErrorDialog, getString(R.string.dialog_title_tips), getString(R.string.dialog_unknow_error), this);
            case LoginDialog:
                return DialogUtil.createInfoDialog(this, LoginDialog, getString(R.string.no_login_title), getString(R.string.no_login), getString(R.string.menu_login), getString(R.string.dialog_btn_cancel), this);
            case NetWorkErrorDailog:
                return DialogUtil.createInfoDialog(this, NetWorkErrorDailog, getString(R.string.dialog_title_tips), getString(R.string.dialog_net_error), getString(R.string.dialog_set), getString(R.string.dialog_quit), this);
            case TimerPickerDialog:
                return new TimePickerDialog(HomeActivity.this, onTimeSetListener, mHour, mMinute, true);
            default:
                break;
        }
        return super.onCreateDialog(id, args);
    }

    @Override
    public void clientDidFailWithError(NetResponseListener mClientListener, int mOp, int scUnknown, String localizedMessage) {
        switch(mOp) {
            case URLProvider.OP_GET_LIST_BY_CHANNEL:
                mLoadingBar.setVisibility(View.GONE);
                if (null != mPlayList && mPlayList.getPlayList().size() > 0) {
                    isGetListFailed = true;
                    return;
                }
                try {
                    showDialog(NetWorkErrorDailog);
                } catch (IllegalArgumentException e) {
                    LogUtil.e(Constants.TAG, "clientDidFailWithError:" + e.getMessage());
                }
                break;
            case URLProvider.OP_FAVE_THIS:
                mFavoriateProgressBar.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    @Override
    public void clientDidGetPlayListByChannel(NetResponseListener mClientListener, PlayList playList, int cid) {
        LogUtil.i(Constants.TAG, "clientDidGetPlayListByChannel");
        isGetListFailed = false;
        if (null != mPlayList && cid == getCurrentCID()) {
            BusfmApplication.getInstance().addPlayList(playList);
            mPlayList.addList(playList.getPlayList());
            LogUtil.i(Constants.TAG, mPlayList.getPlayList().size());
            return;
        }
        mPlayList = playList;
        mPlayList.select(0);
        BusfmApplication.getInstance().setPlayList(playList);
        try {
            mServiceStub.play();
        } catch (RemoteException e) {
            LogUtil.e(Constants.TAG, e.getMessage());
        }
    }

    @Override
    public void clientDidGetPlayListByUserId(NetResponseListener mClientListener, PlayList playList) {
        super.clientDidGetPlayListByUserId(mClientListener, playList);
        LogUtil.i(Constants.TAG, "clientDidGetPlayListByUserId");
        if ((null == playList || playList.size() == 0) && null == mPlayList) {
            mLoadingBar.setVisibility(View.GONE);
            isCheck = true;
            rbChannel.check(R.id.rb_public);
            Toast.makeText(this, getString(R.string.no_favoriate_songs), Toast.LENGTH_SHORT).show();
            return;
        }
        if (null != mPlayList) {
            BusfmApplication.getInstance().addPlayList(playList);
            mPlayList.addList(playList.getPlayList());
            LogUtil.i(Constants.TAG, mPlayList.getPlayList().size());
            return;
        }
        mPlayList = playList;
        mPlayList.select(0);
        BusfmApplication.getInstance().setPlayList(playList);
        try {
            mServiceStub.play();
        } catch (RemoteException e) {
            LogUtil.e(Constants.TAG, e.getMessage());
        }
    }

    @Override
    public void clientDidFaveThis(NetResponseListener mClientListener, ResultEntity resultEntity, String songId) {
        mFavoriateProgressBar.setVisibility(View.GONE);
        if (resultEntity.getResult() == Constants.SUCCESS && songId.equals(mPlayList.getSelectedEntity().getSongId())) {
            if (resultEntity.getErrorMsg().equals(Constants.FavoriateSuccess)) {
                isFavoriate = true;
                tvFavoriateSuccess.setVisibility(View.VISIBLE);
                findViewById(R.id.btnFavorate).setBackgroundResource(R.drawable.favorite_selected);
                tvFavoriateSuccess.startAnimation(mSlideInAnimation);
            } else {
                findViewById(R.id.btnFavorate).setBackgroundResource(R.drawable.favorite);
                Toast.makeText(this, resultEntity.getErrorMsg(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, resultEntity.getErrorMsg(), Toast.LENGTH_SHORT).show();
        }
    }

    public class ChannelAdapter {

        private List<ChannelEntity> mChannelList = new ArrayList<ChannelEntity>();

        public ChannelAdapter(List<ChannelEntity> channelList) {
            mChannelList = channelList;
        }

        public List<ChannelEntity> getChannelEntity() {
            return mChannelList;
        }
    }

    private void exit() {
        try {
            mServiceStub.quit();
            notificationManager.cancel(Constants.NotifyID);
            PrefUtil.clear();
            NetWorkManager.myHandler.cancel(this);
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            LogUtil.e(Constants.TAG, e.getMessage());
        }
    }

    private TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            Toast.makeText(HomeActivity.this, getString(R.string.set_stop_time, hourOfDay, minute), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, PlayerService.class);
            intent.setAction(PlayerService.ACTION_PAUSE);
            PendingIntent pi = PendingIntent.getService(HomeActivity.this, 0, intent, 0);
            AlarmManager am = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        }
    };

    @Override
    public void onWarningDialogOK(int id) {
        switch(id) {
            case UnknowErrorDialog:
                try {
                    exit();
                } catch (Exception e) {
                    LogUtil.e(Constants.TAG, e.getMessage());
                }
                break;
            case LoginDialog:
                isPrivate = true;
                isDouble = true;
                InstrumentUtil.launchLogon(this);
                break;
            case NetWorkErrorDailog:
                InstrumentUtil.launchNetWorkSetActivity(this);
                break;
            default:
                break;
        }
    }

    @Override
    public void onWarningCancel(int id) {
        switch(id) {
            case LoginDialog:
                isCheck = true;
                rbChannel.check(R.id.rb_public);
                break;
            case UnknowErrorDialog:
                exit();
                break;
            case NetWorkErrorDailog:
                exit();
                break;
            default:
                break;
        }
    }

    @Override
    public void onImageDownloadedListener() {
        mLoadingBar.setVisibility(View.GONE);
    }

    @Override
    public void onImageDownloadedFailedListener() {
        mLoadingBar.setVisibility(View.GONE);
    }

    @Override
    public void onItemClickListener(View v, int position) {
        btnNext.setEnabled(true);
        if (isRecycled = true) {
            if (null != mPlayControllerListener) {
                mPlayControllerListener.onTrackChanged(getCurrentSongEntity());
            }
        }
        if (isFirst) {
            isFirst = false;
            return;
        }
        if (isBackground) {
            return;
        }
        LogUtil.i(Constants.TAG, "onItemClickListener");
        mLoadingBar.setVisibility(View.VISIBLE);
        mPlayList = null;
        mCurrentID = mChannelAdapter.getChannelEntity().get(position).getCID();
        NetWorkManager.getListByChannel(this, mCurrentID);
        MobclickAgent.onEvent(this, Constants.CHANNEL_CLICK_RATE, String.valueOf(mCurrentID));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float currentX = event.getX();
        if (v.getId() == R.id.cover) {
            if (null == mVelocityTracker) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(event);
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = currentX;
                    downTime = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    LogUtil.i(Constants.TAG, "MotionEvent.ACTION_UP");
                    if (System.currentTimeMillis() - downTime > 50 && Math.abs(currentX - startX) > 20) {
                        mVelocityTracker.computeCurrentVelocity(1000);
                        boolean left;
                        if (Math.abs(mVelocityTracker.getXVelocity()) > 200) {
                            left = mVelocityTracker.getXVelocity() > 0;
                        } else {
                            left = currentX - startX > 0;
                        }
                        doWithFling(left);
                    } else {
                        if (weiboView.getVisibility() == View.VISIBLE && mFadeInAnimation.hasEnded()) {
                            weiboView.startAnimation(mFadeOutAnimation);
                        } else {
                            weiboView.setVisibility(View.VISIBLE);
                            weiboView.startAnimation(mFadeInAnimation);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    private void doWithFling(boolean left) {
        if (left) {
        } else {
            doWithNextButton();
        }
    }

    /**
     * 下一首
     */
    private void doWithNextButton() {
        if (null == mPlayList || null == mPlayList.getPlayList()) {
            return;
        }
        if (tvNoLoginFavoriate.getVisibility() == View.VISIBLE) {
            tvNoLoginFavoriate.startAnimation(mSlideOutAnimation);
        }
        findViewById(R.id.btnFavorate).setBackgroundResource(R.drawable.favorite);
        mProgressBar.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);
        try {
            mServiceStub.next();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void doShared() {
        MobclickAgent.onEvent(this, Constants.WEIBO_SHARED_TIMES);
        SongEntity songEntity = getCurrentSongEntity();
        if (songEntity == null) {
            Toast.makeText(HomeActivity.this, getString(R.string.no_music_info), Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.weibo_subject));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.weibo_content, "@" + getString(R.string.weibo_account), (songEntity.title + "-" + songEntity.artist + "  " + Constants.mUrlPrefix + Utilities.getShareLinked(songEntity.songId))));
            startActivity(Intent.createChooser(intent, getString(R.string.weibo_title)));
        }
    }

    @Override
    public void onAccuracyChanged(int sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(int sensor, float[] values) {
        if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                x = values[SensorManager.DATA_X];
                y = values[SensorManager.DATA_Y];
                z = values[SensorManager.DATA_Z];
                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
                if (speed > SHAKE_THRESHOLD) {
                    doWithNextButton();
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }
}
