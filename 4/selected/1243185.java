package com.android.email.activity;

import com.android.email.Controller;
import com.android.email.Email;
import com.android.email.R;
import com.android.email.Utility;
import com.android.email.mail.Address;
import com.android.email.mail.MessagingException;
import com.android.email.mail.internet.EmailHtmlUtil;
import com.android.email.mail.internet.MimeUtility;
import com.android.email.provider.AttachmentProvider;
import com.android.email.provider.EmailContent;
import com.android.email.provider.EmailContent.Attachment;
import com.android.email.provider.EmailContent.Body;
import com.android.email.provider.EmailContent.BodyColumns;
import com.android.email.provider.EmailContent.Message;
import org.apache.commons.io.IOUtils;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsContract.StatusUpdates;
import android.text.TextUtils;
import android.text.util.Regex;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageView extends Activity implements OnClickListener {

    private static final String EXTRA_MESSAGE_ID = "com.android.email.MessageView_message_id";

    private static final String EXTRA_MAILBOX_ID = "com.android.email.MessageView_mailbox_id";

    private static final String STATE_MESSAGE_ID = "messageId";

    private static final Pattern IMG_TAG_START_REGEX = Pattern.compile("<(?i)img\\s+");

    private static final Pattern WEB_URL_PROTOCOL = Pattern.compile("(?i)http|https://");

    private static final String[] BODY_CONTENT_PROJECTION = new String[] { Body.RECORD_ID, BodyColumns.MESSAGE_KEY, BodyColumns.HTML_CONTENT, BodyColumns.TEXT_CONTENT };

    private static final String[] PRESENCE_STATUS_PROJECTION = new String[] { Contacts.CONTACT_PRESENCE };

    private static final int BODY_CONTENT_COLUMN_RECORD_ID = 0;

    private static final int BODY_CONTENT_COLUMN_MESSAGE_KEY = 1;

    private static final int BODY_CONTENT_COLUMN_HTML_CONTENT = 2;

    private static final int BODY_CONTENT_COLUMN_TEXT_CONTENT = 3;

    private TextView mSubjectView;

    private TextView mFromView;

    private TextView mDateView;

    private TextView mTimeView;

    private TextView mToView;

    private TextView mCcView;

    private View mCcContainerView;

    private WebView mMessageContentView;

    private LinearLayout mAttachments;

    private ImageView mAttachmentIcon;

    private ImageView mFavoriteIcon;

    private View mShowPicturesSection;

    private ImageView mSenderPresenceView;

    private ProgressDialog mProgressDialog;

    private View mScrollView;

    private long mAccountId;

    private long mMessageId;

    private long mMailboxId;

    private Message mMessage;

    private long mWaitForLoadMessageId;

    private LoadMessageTask mLoadMessageTask;

    private LoadBodyTask mLoadBodyTask;

    private LoadAttachmentsTask mLoadAttachmentsTask;

    private PresenceCheckTask mPresenceCheckTask;

    private long mLoadAttachmentId;

    private boolean mLoadAttachmentSave;

    private String mLoadAttachmentName;

    private java.text.DateFormat mDateFormat;

    private java.text.DateFormat mTimeFormat;

    private Drawable mFavoriteIconOn;

    private Drawable mFavoriteIconOff;

    private MessageViewHandler mHandler = new MessageViewHandler();

    private Controller mController;

    private ControllerResults mControllerCallback = new ControllerResults();

    private View mPrevious;

    private View mNext;

    private LoadPrevNextTask mLoadPrevNextTask;

    private Cursor mPrevNextCursor;

    private ContentObserver mNextPrevObserver;

    private String mHtmlText;

    class MessageViewHandler extends Handler {

        private static final int MSG_PROGRESS = 1;

        private static final int MSG_ATTACHMENT_PROGRESS = 2;

        private static final int MSG_LOAD_CONTENT_URI = 3;

        private static final int MSG_SET_ATTACHMENTS_ENABLED = 4;

        private static final int MSG_LOAD_BODY_ERROR = 5;

        private static final int MSG_NETWORK_ERROR = 6;

        private static final int MSG_FETCHING_ATTACHMENT = 10;

        private static final int MSG_VIEW_ATTACHMENT_ERROR = 12;

        private static final int MSG_UPDATE_ATTACHMENT_ICON = 18;

        private static final int MSG_FINISH_LOAD_ATTACHMENT = 19;

        @Override
        public void handleMessage(android.os.Message msg) {
            switch(msg.what) {
                case MSG_PROGRESS:
                    setProgressBarIndeterminateVisibility(msg.arg1 != 0);
                    break;
                case MSG_ATTACHMENT_PROGRESS:
                    boolean progress = (msg.arg1 != 0);
                    if (progress) {
                        mProgressDialog.setMessage(getString(R.string.message_view_fetching_attachment_progress, mLoadAttachmentName));
                        mProgressDialog.show();
                    } else {
                        mProgressDialog.dismiss();
                    }
                    setProgressBarIndeterminateVisibility(progress);
                    break;
                case MSG_LOAD_CONTENT_URI:
                    String uriString = (String) msg.obj;
                    if (mMessageContentView != null) {
                        mMessageContentView.loadUrl(uriString);
                    }
                    break;
                case MSG_SET_ATTACHMENTS_ENABLED:
                    for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
                        AttachmentInfo attachment = (AttachmentInfo) mAttachments.getChildAt(i).getTag();
                        attachment.viewButton.setEnabled(msg.arg1 == 1);
                        attachment.downloadButton.setEnabled(msg.arg1 == 1);
                    }
                    break;
                case MSG_LOAD_BODY_ERROR:
                    Toast.makeText(MessageView.this, R.string.error_loading_message_body, Toast.LENGTH_LONG).show();
                    break;
                case MSG_NETWORK_ERROR:
                    Toast.makeText(MessageView.this, R.string.status_network_error, Toast.LENGTH_LONG).show();
                    break;
                case MSG_FETCHING_ATTACHMENT:
                    Toast.makeText(MessageView.this, getString(R.string.message_view_fetching_attachment_toast), Toast.LENGTH_SHORT).show();
                    break;
                case MSG_VIEW_ATTACHMENT_ERROR:
                    Toast.makeText(MessageView.this, getString(R.string.message_view_display_attachment_toast), Toast.LENGTH_SHORT).show();
                    break;
                case MSG_UPDATE_ATTACHMENT_ICON:
                    ((AttachmentInfo) mAttachments.getChildAt(msg.arg1).getTag()).iconView.setImageBitmap((Bitmap) msg.obj);
                    break;
                case MSG_FINISH_LOAD_ATTACHMENT:
                    long attachmentId = (Long) msg.obj;
                    doFinishLoadAttachment(attachmentId);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        public void attachmentProgress(boolean progress) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_ATTACHMENT_PROGRESS);
            msg.arg1 = progress ? 1 : 0;
            sendMessage(msg);
        }

        public void progress(boolean progress) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_PROGRESS);
            msg.arg1 = progress ? 1 : 0;
            sendMessage(msg);
        }

        public void loadContentUri(String uriString) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_LOAD_CONTENT_URI);
            msg.obj = uriString;
            sendMessage(msg);
        }

        public void setAttachmentsEnabled(boolean enabled) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_SET_ATTACHMENTS_ENABLED);
            msg.arg1 = enabled ? 1 : 0;
            sendMessage(msg);
        }

        public void loadBodyError() {
            sendEmptyMessage(MSG_LOAD_BODY_ERROR);
        }

        public void networkError() {
            sendEmptyMessage(MSG_NETWORK_ERROR);
        }

        public void fetchingAttachment() {
            sendEmptyMessage(MSG_FETCHING_ATTACHMENT);
        }

        public void attachmentViewError() {
            sendEmptyMessage(MSG_VIEW_ATTACHMENT_ERROR);
        }

        public void updateAttachmentIcon(int pos, Bitmap icon) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_UPDATE_ATTACHMENT_ICON);
            msg.arg1 = pos;
            msg.obj = icon;
            sendMessage(msg);
        }

        public void finishLoadAttachment(long attachmentId) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_FINISH_LOAD_ATTACHMENT);
            msg.obj = Long.valueOf(attachmentId);
            sendMessage(msg);
        }
    }

    /**
     * Encapsulates known information about a single attachment.
     */
    private static class AttachmentInfo {

        public String name;

        public String contentType;

        public long size;

        public long attachmentId;

        public Button viewButton;

        public Button downloadButton;

        public ImageView iconView;
    }

    /**
     * View a specific message found in the Email provider.
     * @param messageId the message to view.
     * @param mailboxId identifies the sequence of messages used for prev/next navigation.
     */
    public static void actionView(Context context, long messageId, long mailboxId) {
        Intent i = new Intent(context, MessageView.class);
        i.putExtra(EXTRA_MESSAGE_ID, messageId);
        i.putExtra(EXTRA_MAILBOX_ID, mailboxId);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.message_view);
        mSubjectView = (TextView) findViewById(R.id.subject);
        mFromView = (TextView) findViewById(R.id.from);
        mToView = (TextView) findViewById(R.id.to);
        mCcView = (TextView) findViewById(R.id.cc);
        mCcContainerView = findViewById(R.id.cc_container);
        mDateView = (TextView) findViewById(R.id.date);
        mTimeView = (TextView) findViewById(R.id.time);
        mMessageContentView = (WebView) findViewById(R.id.message_content);
        mAttachments = (LinearLayout) findViewById(R.id.attachments);
        mAttachmentIcon = (ImageView) findViewById(R.id.attachment);
        mFavoriteIcon = (ImageView) findViewById(R.id.favorite);
        mShowPicturesSection = findViewById(R.id.show_pictures_section);
        mSenderPresenceView = (ImageView) findViewById(R.id.presence);
        mNext = findViewById(R.id.next);
        mPrevious = findViewById(R.id.previous);
        mScrollView = findViewById(R.id.scrollview);
        mNext.setOnClickListener(this);
        mPrevious.setOnClickListener(this);
        mFromView.setOnClickListener(this);
        mSenderPresenceView.setOnClickListener(this);
        mFavoriteIcon.setOnClickListener(this);
        findViewById(R.id.reply).setOnClickListener(this);
        findViewById(R.id.reply_all).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);
        findViewById(R.id.show_pictures).setOnClickListener(this);
        mMessageContentView.setVerticalScrollBarEnabled(false);
        mMessageContentView.getSettings().setBlockNetworkImage(true);
        mMessageContentView.getSettings().setSupportZoom(false);
        mMessageContentView.setWebViewClient(new CustomWebViewClient());
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDateFormat = android.text.format.DateFormat.getDateFormat(this);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(this);
        mFavoriteIconOn = getResources().getDrawable(R.drawable.btn_star_big_buttonless_on);
        mFavoriteIconOff = getResources().getDrawable(R.drawable.btn_star_big_buttonless_off);
        Intent intent = getIntent();
        mMessageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
        if (icicle != null) {
            mMessageId = icicle.getLong(STATE_MESSAGE_ID, mMessageId);
        }
        mMailboxId = intent.getLongExtra(EXTRA_MAILBOX_ID, -1);
        mController = Controller.getInstance(getApplication());
        mNextPrevObserver = new NextPrevObserver(mHandler);
        messageChanged();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mMessageId != -1) {
            state.putLong(STATE_MESSAGE_ID, mMessageId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mWaitForLoadMessageId = -1;
        mController.addResultCallback(mControllerCallback);
        if (mMessage != null) {
            startPresenceCheck();
            if (mLoadPrevNextTask == null && mMailboxId != -1) {
                mLoadPrevNextTask = new LoadPrevNextTask(mMailboxId);
                mLoadPrevNextTask.execute();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mController.removeResultCallback(mControllerCallback);
        closePrevNextCursor();
    }

    private void closePrevNextCursor() {
        if (mPrevNextCursor != null) {
            mPrevNextCursor.unregisterContentObserver(mNextPrevObserver);
            mPrevNextCursor.close();
            mPrevNextCursor = null;
        }
    }

    private static void cancelTask(AsyncTask<?, ?, ?> task) {
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            task.cancel(true);
        }
    }

    private void cancelAllTasks() {
        cancelTask(mLoadMessageTask);
        mLoadMessageTask = null;
        cancelTask(mLoadBodyTask);
        mLoadBodyTask = null;
        cancelTask(mLoadAttachmentsTask);
        mLoadAttachmentsTask = null;
        cancelTask(mLoadPrevNextTask);
        mLoadPrevNextTask = null;
        cancelTask(mPresenceCheckTask);
        mPresenceCheckTask = null;
    }

    /**
     * We override onDestroy to make sure that the WebView gets explicitly destroyed.
     * Otherwise it can leak native references.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAllTasks();
        synchronized (this) {
            mMessageContentView.destroy();
            mMessageContentView = null;
        }
    }

    private void onDelete() {
        if (mMessage != null) {
            long messageIdToDelete = mMessageId;
            boolean moved = onPrevious() || onNext();
            mController.deleteMessage(messageIdToDelete, mMessage.mAccountKey);
            Toast.makeText(this, R.string.message_deleted_toast, Toast.LENGTH_SHORT).show();
            if (!moved) {
                finish();
            }
        }
    }

    /**
     * Overrides for various WebView behaviors.
     */
    private class CustomWebViewClient extends WebViewClient {

        /**
         * This is intended to mirror the operation of the original
         * (see android.webkit.CallbackProxy) with one addition of intent flags
         * "FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET".  This improves behavior when sublaunching
         * other apps via embedded URI's.
         *
         * We also use this hook to catch "mailto:" links and handle them locally.
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url != null && url.toLowerCase().startsWith("mailto:")) {
                return MessageCompose.actionCompose(MessageView.this, url, mAccountId);
            }
            boolean result = false;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            try {
                startActivity(intent);
                result = true;
            } catch (ActivityNotFoundException ex) {
            }
            return result;
        }
    }

    /**
     * Handle clicks on sender, which shows {@link QuickContact} or prompts to add
     * the sender as a contact.
     */
    private void onClickSender() {
        if (mMessage == null) return;
        final Address senderEmail = Address.unpackFirst(mMessage.mFrom);
        if (senderEmail == null) return;
        final ContentResolver resolver = getContentResolver();
        final String address = senderEmail.getAddress();
        final Uri dataUri = Uri.withAppendedPath(CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(address));
        final Uri lookupUri = ContactsContract.Data.getContactLookupUri(resolver, dataUri);
        if (lookupUri != null) {
            QuickContact.showQuickContact(this, mSenderPresenceView, lookupUri, QuickContact.MODE_LARGE, null);
        } else {
            final Uri mailUri = Uri.fromParts("mailto", address, null);
            final Intent intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, mailUri);
            intent.putExtra(ContactsContract.Intents.EXTRA_CREATE_DESCRIPTION, senderEmail.toString());
            final String senderPersonal = senderEmail.getPersonal();
            if (!TextUtils.isEmpty(senderPersonal)) {
                intent.putExtra(ContactsContract.Intents.Insert.NAME, senderPersonal);
            }
            startActivity(intent);
        }
    }

    /**
     * Toggle favorite status and write back to provider
     */
    private void onClickFavorite() {
        if (mMessage != null) {
            boolean newFavorite = !mMessage.mFlagFavorite;
            mFavoriteIcon.setImageDrawable(newFavorite ? mFavoriteIconOn : mFavoriteIconOff);
            mMessage.mFlagFavorite = newFavorite;
            mController.setMessageFavorite(mMessageId, newFavorite);
        }
    }

    private void onReply() {
        if (mMessage != null) {
            MessageCompose.actionReply(this, mMessage.mId, false);
            finish();
        }
    }

    private void onReplyAll() {
        if (mMessage != null) {
            MessageCompose.actionReply(this, mMessage.mId, true);
            finish();
        }
    }

    private void onForward() {
        if (mMessage != null) {
            MessageCompose.actionForward(this, mMessage.mId);
            finish();
        }
    }

    private boolean onNext() {
        if (mPrevNextCursor != null && mPrevNextCursor.moveToNext()) {
            mMessageId = mPrevNextCursor.getLong(0);
            messageChanged();
            return true;
        }
        return false;
    }

    private boolean onPrevious() {
        if (mPrevNextCursor != null && mPrevNextCursor.getPosition() > 0) {
            mPrevNextCursor.moveToPrevious();
            mMessageId = mPrevNextCursor.getLong(0);
            messageChanged();
            return true;
        }
        return false;
    }

    private void onMarkAsRead(boolean isRead) {
        if (mMessage != null && mMessage.mFlagRead != isRead) {
            mMessage.mFlagRead = isRead;
            mController.setMessageRead(mMessageId, isRead);
        }
    }

    static File createUniqueFile(File directory, String filename) {
        File file = new File(directory, filename);
        if (!file.exists()) {
            return file;
        }
        int index = filename.lastIndexOf('.');
        String format;
        if (index != -1) {
            String name = filename.substring(0, index);
            String extension = filename.substring(index);
            format = name + "-%d" + extension;
        } else {
            format = filename + "-%d";
        }
        for (int i = 2; i < Integer.MAX_VALUE; i++) {
            file = new File(directory, String.format(format, i));
            if (!file.exists()) {
                return file;
            }
        }
        return null;
    }

    private void onDownloadAttachment(AttachmentInfo attachment) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, getString(R.string.message_view_status_attachment_not_saved), Toast.LENGTH_SHORT).show();
            return;
        }
        mLoadAttachmentId = attachment.attachmentId;
        mLoadAttachmentSave = true;
        mLoadAttachmentName = attachment.name;
        mController.loadAttachment(attachment.attachmentId, mMessageId, mMessage.mMailboxKey, mAccountId, mControllerCallback);
    }

    private void onViewAttachment(AttachmentInfo attachment) {
        mLoadAttachmentId = attachment.attachmentId;
        mLoadAttachmentSave = false;
        mLoadAttachmentName = attachment.name;
        mController.loadAttachment(attachment.attachmentId, mMessageId, mMessage.mMailboxKey, mAccountId, mControllerCallback);
    }

    private void onShowPictures() {
        if (mMessage != null) {
            if (mMessageContentView != null) {
                mMessageContentView.getSettings().setBlockNetworkImage(false);
            }
            mShowPicturesSection.setVisibility(View.GONE);
        }
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.from:
            case R.id.presence:
                onClickSender();
                break;
            case R.id.favorite:
                onClickFavorite();
                break;
            case R.id.reply:
                onReply();
                break;
            case R.id.reply_all:
                onReplyAll();
                break;
            case R.id.delete:
                onDelete();
                break;
            case R.id.next:
                onNext();
                break;
            case R.id.previous:
                onPrevious();
                break;
            case R.id.download:
                onDownloadAttachment((AttachmentInfo) view.getTag());
                break;
            case R.id.view:
                onViewAttachment((AttachmentInfo) view.getTag());
                break;
            case R.id.show_pictures:
                onShowPictures();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = handleMenuItem(item.getItemId());
        if (!handled) {
            handled = super.onOptionsItemSelected(item);
        }
        return handled;
    }

    boolean handleMenuItem(int menuItemId) {
        switch(menuItemId) {
            case R.id.delete:
                onDelete();
                break;
            case R.id.reply:
                onReply();
                break;
            case R.id.reply_all:
                onReplyAll();
                break;
            case R.id.forward:
                onForward();
                break;
            case R.id.mark_as_unread:
                onMarkAsRead(false);
                finish();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_view_option, menu);
        return true;
    }

    /**
     * Re-init everything needed for changing message.
     */
    private void messageChanged() {
        if (Email.DEBUG) {
            Email.log("MessageView: messageChanged to id=" + mMessageId);
        }
        cancelAllTasks();
        setTitle("");
        if (mMessageContentView != null) {
            mMessageContentView.scrollTo(0, 0);
            mMessageContentView.loadUrl("file:///android_asset/empty.html");
        }
        mScrollView.scrollTo(0, 0);
        mAttachments.removeAllViews();
        mAttachments.setVisibility(View.GONE);
        mAttachmentIcon.setVisibility(View.GONE);
        mLoadMessageTask = new LoadMessageTask(mMessageId, true);
        mLoadMessageTask.execute();
        updatePrevNextArrows(mPrevNextCursor);
    }

    /**
     * Reposition the next/prev cursor.  Finish() the activity if we are no longer
     * in the list.  Update the UI arrows as appropriate.
     */
    private void repositionPrevNextCursor() {
        if (Email.DEBUG) {
            Email.log("MessageView: reposition to id=" + mMessageId);
        }
        mPrevNextCursor.moveToPosition(-1);
        while (mPrevNextCursor.moveToNext() && mPrevNextCursor.getLong(0) != mMessageId) {
        }
        if (mPrevNextCursor.isAfterLast()) {
            finish();
        }
        updatePrevNextArrows(mPrevNextCursor);
    }

    /**
     * Based on the current position of the prev/next cursor, update the prev / next arrows.
     */
    private void updatePrevNextArrows(Cursor cursor) {
        if (cursor != null) {
            boolean hasPrev, hasNext;
            if (cursor.isAfterLast() || cursor.isBeforeFirst()) {
                hasPrev = hasNext = false;
            } else {
                hasPrev = !cursor.isFirst();
                hasNext = !cursor.isLast();
            }
            mPrevious.setVisibility(hasPrev ? View.VISIBLE : View.GONE);
            mNext.setVisibility(hasNext ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * This observer is used to watch for external changes to the next/prev list
     */
    private class NextPrevObserver extends ContentObserver {

        public NextPrevObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mLoadPrevNextTask == null && mPrevNextCursor != null) {
                mLoadPrevNextTask = new LoadPrevNextTask(mMailboxId);
                mLoadPrevNextTask.execute();
            }
        }
    }

    private Bitmap getPreviewIcon(AttachmentInfo attachment) {
        try {
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(AttachmentProvider.getAttachmentThumbnailUri(mAccountId, attachment.attachmentId, 62, 62)));
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatSize(float size) {
        long kb = 1024;
        long mb = (kb * 1024);
        long gb = (mb * 1024);
        if (size < kb) {
            return String.format("%d bytes", (int) size);
        } else if (size < mb) {
            return String.format("%.1f kB", size / kb);
        } else if (size < gb) {
            return String.format("%.1f MB", size / mb);
        } else {
            return String.format("%.1f GB", size / gb);
        }
    }

    private void updateAttachmentThumbnail(long attachmentId) {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            AttachmentInfo attachment = (AttachmentInfo) mAttachments.getChildAt(i).getTag();
            if (attachment.attachmentId == attachmentId) {
                Bitmap previewIcon = getPreviewIcon(attachment);
                if (previewIcon != null) {
                    mHandler.updateAttachmentIcon(i, previewIcon);
                }
                return;
            }
        }
    }

    /**
     * Copy data from a cursor-refreshed attachment into the UI.  Called from UI thread.
     *
     * @param attachment A single attachment loaded from the provider
     */
    private void addAttachment(Attachment attachment) {
        AttachmentInfo attachmentInfo = new AttachmentInfo();
        attachmentInfo.size = attachment.mSize;
        attachmentInfo.contentType = attachment.mMimeType;
        attachmentInfo.name = attachment.mFileName;
        attachmentInfo.attachmentId = attachment.mId;
        if (attachmentInfo.contentType == null || attachmentInfo.contentType.length() == 0) {
            attachmentInfo.contentType = "application/octet-stream";
        }
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.message_view_attachment, null);
        TextView attachmentName = (TextView) view.findViewById(R.id.attachment_name);
        TextView attachmentInfoView = (TextView) view.findViewById(R.id.attachment_info);
        ImageView attachmentIcon = (ImageView) view.findViewById(R.id.attachment_icon);
        Button attachmentView = (Button) view.findViewById(R.id.view);
        Button attachmentDownload = (Button) view.findViewById(R.id.download);
        if ((!MimeUtility.mimeTypeMatches(attachmentInfo.contentType, Email.ACCEPTABLE_ATTACHMENT_VIEW_TYPES)) || (MimeUtility.mimeTypeMatches(attachmentInfo.contentType, Email.UNACCEPTABLE_ATTACHMENT_VIEW_TYPES))) {
            attachmentView.setVisibility(View.GONE);
        }
        if ((!MimeUtility.mimeTypeMatches(attachmentInfo.contentType, Email.ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES)) || (MimeUtility.mimeTypeMatches(attachmentInfo.contentType, Email.UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))) {
            attachmentDownload.setVisibility(View.GONE);
        }
        if (attachmentInfo.size > Email.MAX_ATTACHMENT_DOWNLOAD_SIZE) {
            attachmentView.setVisibility(View.GONE);
            attachmentDownload.setVisibility(View.GONE);
        }
        attachmentInfo.viewButton = attachmentView;
        attachmentInfo.downloadButton = attachmentDownload;
        attachmentInfo.iconView = attachmentIcon;
        view.setTag(attachmentInfo);
        attachmentView.setOnClickListener(this);
        attachmentView.setTag(attachmentInfo);
        attachmentDownload.setOnClickListener(this);
        attachmentDownload.setTag(attachmentInfo);
        attachmentName.setText(attachmentInfo.name);
        attachmentInfoView.setText(formatSize(attachmentInfo.size));
        Bitmap previewIcon = getPreviewIcon(attachmentInfo);
        if (previewIcon != null) {
            attachmentIcon.setImageBitmap(previewIcon);
        }
        mAttachments.addView(view);
        mAttachments.setVisibility(View.VISIBLE);
    }

    private class PresenceCheckTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... emails) {
            Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, PRESENCE_STATUS_PROJECTION, CommonDataKinds.Email.DATA + "=?", emails, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int status = cursor.getInt(0);
                        int icon = StatusUpdates.getPresenceIconResourceId(status);
                        return icon;
                    }
                } finally {
                    cursor.close();
                }
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer icon) {
            if (icon == null) {
                return;
            }
            updateSenderPresence(icon);
        }
    }

    /**
     * Launch a thread (because of cross-process DB lookup) to check presence of the sender of the
     * message.  When that thread completes, update the UI.
     *
     * This must only be called when mMessage is null (it will hide presence indications) or when
     * mMessage has already seen its headers loaded.
     *
     * Note:  This is just a polling operation.  A more advanced solution would be to keep the
     * cursor open and respond to presence status updates (in the form of content change
     * notifications).  However, because presence changes fairly slowly compared to the duration
     * of viewing a single message, a simple poll at message load (and onResume) should be
     * sufficient.
     */
    private void startPresenceCheck() {
        if (mMessage != null) {
            Address sender = Address.unpackFirst(mMessage.mFrom);
            if (sender != null) {
                String email = sender.getAddress();
                if (email != null) {
                    mPresenceCheckTask = new PresenceCheckTask();
                    mPresenceCheckTask.execute(email);
                    return;
                }
            }
        }
        updateSenderPresence(0);
    }

    /**
     * Update the actual UI.  Must be called from main thread (or handler)
     * @param presenceIconId the presence of the sender, 0 for "unknown"
     */
    private void updateSenderPresence(int presenceIconId) {
        if (presenceIconId == 0) {
            presenceIconId = R.drawable.presence_inactive;
        }
        mSenderPresenceView.setImageResource(presenceIconId);
    }

    /**
     * This task finds out the messageId for the previous and next message
     * in the order given by mailboxId as used in MessageList.
     *
     * It generates the same cursor as the one used in MessageList (but with an id-only projection),
     * scans through it until finds the current messageId, and takes the previous and next ids.
     */
    private class LoadPrevNextTask extends AsyncTask<Void, Void, Cursor> {

        private long mLocalMailboxId;

        public LoadPrevNextTask(long mailboxId) {
            mLocalMailboxId = mailboxId;
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            String selection = Utility.buildMailboxIdSelection(getContentResolver(), mLocalMailboxId);
            Cursor c = getContentResolver().query(EmailContent.Message.CONTENT_URI, EmailContent.ID_PROJECTION, selection, null, EmailContent.MessageColumns.TIMESTAMP + " DESC");
            return c;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if (cursor == null) {
                return;
            }
            MessageView.this.mLoadPrevNextTask = null;
            if (cursor.isClosed()) {
                return;
            }
            closePrevNextCursor();
            mPrevNextCursor = cursor;
            mPrevNextCursor.registerContentObserver(MessageView.this.mNextPrevObserver);
            repositionPrevNextCursor();
        }
    }

    /**
     * Async task for loading a single message outside of the UI thread
     * Note:  To support unit testing, a sentinel messageId of Long.MIN_VALUE prevents
     * loading the message but leaves the activity open.
     */
    private class LoadMessageTask extends AsyncTask<Void, Void, Message> {

        private long mId;

        private boolean mOkToFetch;

        /**
         * Special constructor to cache some local info
         */
        public LoadMessageTask(long messageId, boolean okToFetch) {
            mId = messageId;
            mOkToFetch = okToFetch;
        }

        @Override
        protected Message doInBackground(Void... params) {
            if (mId == Long.MIN_VALUE) {
                return null;
            }
            return Message.restoreMessageWithId(MessageView.this, mId);
        }

        @Override
        protected void onPostExecute(Message message) {
            if (isCancelled()) {
                return;
            }
            if (message == null) {
                if (mId != Long.MIN_VALUE) {
                    finish();
                }
                return;
            }
            reloadUiFromMessage(message, mOkToFetch);
            startPresenceCheck();
        }
    }

    /**
     * Async task for loading a single message body outside of the UI thread
     */
    private class LoadBodyTask extends AsyncTask<Void, Void, String[]> {

        private long mId;

        /**
         * Special constructor to cache some local info
         */
        public LoadBodyTask(long messageId) {
            mId = messageId;
        }

        @Override
        protected String[] doInBackground(Void... params) {
            try {
                String text = null;
                String html = Body.restoreBodyHtmlWithMessageId(MessageView.this, mId);
                if (html == null) {
                    text = Body.restoreBodyTextWithMessageId(MessageView.this, mId);
                }
                return new String[] { text, html };
            } catch (RuntimeException re) {
                Log.d(Email.LOG_TAG, "Exception while loading message body: " + re.toString());
                mHandler.loadBodyError();
                return new String[] { null, null };
            }
        }

        @Override
        protected void onPostExecute(String[] results) {
            if (results == null) {
                return;
            }
            reloadUiFromBody(results[0], results[1]);
            onMarkAsRead(true);
        }
    }

    /**
     * Async task for loading attachments
     *
     * Note:  This really should only be called when the message load is complete - or, we should
     * leave open a listener so the attachments can fill in as they are discovered.  In either case,
     * this implementation is incomplete, as it will fail to refresh properly if the message is
     * partially loaded at this time.
     */
    private class LoadAttachmentsTask extends AsyncTask<Long, Void, Attachment[]> {

        @Override
        protected Attachment[] doInBackground(Long... messageIds) {
            return Attachment.restoreAttachmentsWithMessageId(MessageView.this, messageIds[0]);
        }

        @Override
        protected void onPostExecute(Attachment[] attachments) {
            if (attachments == null) {
                return;
            }
            boolean htmlChanged = false;
            for (Attachment attachment : attachments) {
                if (mHtmlText != null && attachment.mContentId != null && attachment.mContentUri != null) {
                    String contentIdRe = "\\s+(?i)src=\"cid(?-i):\\Q" + attachment.mContentId + "\\E\"";
                    String srcContentUri = " src=\"" + attachment.mContentUri + "\"";
                    mHtmlText = mHtmlText.replaceAll(contentIdRe, srcContentUri);
                    htmlChanged = true;
                } else {
                    addAttachment(attachment);
                }
            }
            if (htmlChanged && mMessageContentView != null) {
                mMessageContentView.loadDataWithBaseURL("email://", mHtmlText, "text/html", "utf-8", null);
            }
            mHtmlText = null;
        }
    }

    /**
     * Reload the UI from a provider cursor.  This must only be called from the UI thread.
     *
     * @param message A copy of the message loaded from the database
     * @param okToFetch If true, and message is not fully loaded, it's OK to fetch from
     * the network.  Use false to prevent looping here.
     *
     * TODO: trigger presence check
     */
    private void reloadUiFromMessage(Message message, boolean okToFetch) {
        mMessage = message;
        mAccountId = message.mAccountKey;
        if (mMailboxId == -1) {
            mMailboxId = message.mMailboxKey;
        }
        if (mPrevNextCursor == null) {
            mLoadPrevNextTask = new LoadPrevNextTask(mMailboxId);
            mLoadPrevNextTask.execute();
        }
        mSubjectView.setText(message.mSubject);
        mFromView.setText(Address.toFriendly(Address.unpack(message.mFrom)));
        Date date = new Date(message.mTimeStamp);
        mTimeView.setText(mTimeFormat.format(date));
        mDateView.setText(Utility.isDateToday(date) ? null : mDateFormat.format(date));
        mToView.setText(Address.toFriendly(Address.unpack(message.mTo)));
        String friendlyCc = Address.toFriendly(Address.unpack(message.mCc));
        mCcView.setText(friendlyCc);
        mCcContainerView.setVisibility((friendlyCc != null) ? View.VISIBLE : View.GONE);
        mAttachmentIcon.setVisibility(message.mAttachments != null ? View.VISIBLE : View.GONE);
        mFavoriteIcon.setImageDrawable(message.mFlagFavorite ? mFavoriteIconOn : mFavoriteIconOff);
        if (okToFetch && message.mFlagLoaded != Message.FLAG_LOADED_COMPLETE) {
            mWaitForLoadMessageId = message.mId;
            mController.loadMessageForView(message.mId, mControllerCallback);
        } else {
            mWaitForLoadMessageId = -1;
            mLoadBodyTask = new LoadBodyTask(message.mId);
            mLoadBodyTask.execute();
        }
    }

    /**
     * Reload the body from the provider cursor.  This must only be called from the UI thread.
     *
     * @param bodyText text part
     * @param bodyHtml html part
     *
     * TODO deal with html vs text and many other issues
     */
    private void reloadUiFromBody(String bodyText, String bodyHtml) {
        String text = null;
        mHtmlText = null;
        boolean hasImages = false;
        if (bodyHtml == null) {
            text = bodyText;
            StringBuffer sb = new StringBuffer("<html><body>");
            if (text != null) {
                text = EmailHtmlUtil.escapeCharacterToDisplay(text);
                Matcher m = Regex.WEB_URL_PATTERN.matcher(text);
                while (m.find()) {
                    int start = m.start();
                    if (start == 0 || text.charAt(start - 1) != '@') {
                        String url = m.group();
                        Matcher proto = WEB_URL_PROTOCOL.matcher(url);
                        String link;
                        if (proto.find()) {
                            link = proto.group().toLowerCase() + url.substring(proto.end());
                        } else {
                            link = "http://" + url;
                        }
                        String href = String.format("<a href=\"%s\">%s</a>", link, url);
                        m.appendReplacement(sb, href);
                    } else {
                        m.appendReplacement(sb, "$0");
                    }
                }
                m.appendTail(sb);
            }
            sb.append("</body></html>");
            text = sb.toString();
        } else {
            text = bodyHtml;
            mHtmlText = bodyHtml;
            hasImages = IMG_TAG_START_REGEX.matcher(text).find();
        }
        mShowPicturesSection.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        if (mMessageContentView != null) {
            mMessageContentView.loadDataWithBaseURL("email://", text, "text/html", "utf-8", null);
        }
        mLoadAttachmentsTask = new LoadAttachmentsTask();
        mLoadAttachmentsTask.execute(mMessage.mId);
    }

    /**
     * Controller results listener.  This completely replaces MessagingListener
     */
    class ControllerResults implements Controller.Result {

        public void loadMessageForViewCallback(MessagingException result, long messageId, int progress) {
            if (messageId != MessageView.this.mMessageId || messageId != MessageView.this.mWaitForLoadMessageId) {
                return;
            }
            if (result == null) {
                switch(progress) {
                    case 0:
                        mHandler.progress(true);
                        mHandler.loadContentUri("file:///android_asset/loading.html");
                        break;
                    case 100:
                        mWaitForLoadMessageId = -1;
                        mHandler.progress(false);
                        cancelAllTasks();
                        mLoadMessageTask = new LoadMessageTask(mMessageId, false);
                        mLoadMessageTask.execute();
                        break;
                    default:
                        break;
                }
            } else {
                mWaitForLoadMessageId = -1;
                mHandler.progress(false);
                mHandler.networkError();
                mHandler.loadContentUri("file:///android_asset/empty.html");
            }
        }

        public void loadAttachmentCallback(MessagingException result, long messageId, long attachmentId, int progress) {
            if (messageId == MessageView.this.mMessageId) {
                if (result == null) {
                    switch(progress) {
                        case 0:
                            mHandler.setAttachmentsEnabled(false);
                            mHandler.attachmentProgress(true);
                            mHandler.fetchingAttachment();
                            break;
                        case 100:
                            mHandler.setAttachmentsEnabled(true);
                            mHandler.attachmentProgress(false);
                            updateAttachmentThumbnail(attachmentId);
                            mHandler.finishLoadAttachment(attachmentId);
                            break;
                        default:
                            break;
                    }
                } else {
                    mHandler.setAttachmentsEnabled(true);
                    mHandler.attachmentProgress(false);
                    mHandler.networkError();
                }
            }
        }

        public void updateMailboxCallback(MessagingException result, long accountId, long mailboxId, int progress, int numNewMessages) {
            if (result != null || progress == 100) {
                Email.updateMailboxRefreshTime(mailboxId);
            }
        }

        public void updateMailboxListCallback(MessagingException result, long accountId, int progress) {
        }

        public void serviceCheckMailCallback(MessagingException result, long accountId, long mailboxId, int progress, long tag) {
        }

        public void sendMailCallback(MessagingException result, long accountId, long messageId, int progress) {
        }
    }

    /**
     * Back in the UI thread, handle the final steps of downloading an attachment (view or save).
     *
     * @param attachmentId the attachment that was just downloaded
     */
    private void doFinishLoadAttachment(long attachmentId) {
        if (attachmentId != mLoadAttachmentId) {
            return;
        }
        Attachment attachment = Attachment.restoreAttachmentWithId(MessageView.this, attachmentId);
        Uri attachmentUri = AttachmentProvider.getAttachmentUri(mAccountId, attachment.mId);
        Uri contentUri = AttachmentProvider.resolveAttachmentIdToContentUri(getContentResolver(), attachmentUri);
        if (mLoadAttachmentSave) {
            try {
                File file = createUniqueFile(Environment.getExternalStorageDirectory(), attachment.mFileName);
                InputStream in = getContentResolver().openInputStream(contentUri);
                OutputStream out = new FileOutputStream(file);
                IOUtils.copy(in, out);
                out.flush();
                out.close();
                in.close();
                Toast.makeText(MessageView.this, String.format(getString(R.string.message_view_status_attachment_saved), file.getName()), Toast.LENGTH_LONG).show();
                new MediaScannerNotifier(this, file, mHandler);
            } catch (IOException ioe) {
                Toast.makeText(MessageView.this, getString(R.string.message_view_status_attachment_not_saved), Toast.LENGTH_LONG).show();
            }
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(contentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                mHandler.attachmentViewError();
            }
        }
    }

    /**
     * This notifier is created after an attachment completes downloaded.  It attaches to the
     * media scanner and waits to handle the completion of the scan.  At that point it tries
     * to start an ACTION_VIEW activity for the attachment.
    */
    private static class MediaScannerNotifier implements MediaScannerConnectionClient {

        private Context mContext;

        private MediaScannerConnection mConnection;

        private File mFile;

        MessageViewHandler mHandler;

        public MediaScannerNotifier(Context context, File file, MessageViewHandler handler) {
            mContext = context;
            mFile = file;
            mHandler = handler;
            mConnection = new MediaScannerConnection(context, this);
            mConnection.connect();
        }

        public void onMediaScannerConnected() {
            mConnection.scanFile(mFile.getAbsolutePath(), null);
        }

        public void onScanCompleted(String path, Uri uri) {
            try {
                if (uri != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    mContext.startActivity(intent);
                }
            } catch (ActivityNotFoundException e) {
                mHandler.attachmentViewError();
            } finally {
                mConnection.disconnect();
                mContext = null;
                mHandler = null;
            }
        }
    }
}
