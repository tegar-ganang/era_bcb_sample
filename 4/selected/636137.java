package android.test;

import com.google.android.collect.Lists;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.accounts.Account;
import android.content.ContextWrapper;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import java.util.List;
import java.io.File;

/**
     * A mock context which prevents its users from talking to the rest of the device while
 * stubbing enough methods to satify code that tries to talk to other packages.
 */
public class IsolatedContext extends ContextWrapper {

    private ContentResolver mResolver;

    private final MockAccountManager mMockAccountManager;

    private List<Intent> mBroadcastIntents = Lists.newArrayList();

    public IsolatedContext(ContentResolver resolver, Context targetContext) {
        super(targetContext);
        mResolver = resolver;
        mMockAccountManager = new MockAccountManager();
    }

    /** Returns the list of intents that were broadcast since the last call to this method. */
    public List<Intent> getAndClearBroadcastIntents() {
        List<Intent> intents = mBroadcastIntents;
        mBroadcastIntents = Lists.newArrayList();
        return intents;
    }

    @Override
    public ContentResolver getContentResolver() {
        return mResolver;
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return false;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return null;
    }

    @Override
    public void sendBroadcast(Intent intent) {
        mBroadcastIntents.add(intent);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        mBroadcastIntents.add(intent);
    }

    @Override
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
        return PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        return PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public Object getSystemService(String name) {
        if (Context.ACCOUNT_SERVICE.equals(name)) {
            return mMockAccountManager;
        }
        return null;
    }

    private class MockAccountManager extends AccountManager {

        public MockAccountManager() {
            super(IsolatedContext.this, null, null);
        }

        public void addOnAccountsUpdatedListener(OnAccountsUpdateListener listener, Handler handler, boolean updateImmediately) {
        }

        public Account[] getAccounts() {
            return new Account[] {};
        }
    }

    @Override
    public File getFilesDir() {
        return new File("/dev/null");
    }
}
