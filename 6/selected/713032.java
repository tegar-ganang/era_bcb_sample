package com.beem.project.beem.service;

import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;
import com.beem.project.beem.service.aidl.IXmppConnection;
import com.beem.project.beem.service.aidl.IXmppFacade;

/**
 * This is an asynchronous task that will launch a connection to the XMPP server.
 * 
 * @see android.os.AsyncTask
 * @author Da Risk <da_risk@elyzion.net>
 */
public class LoginAsyncTask extends AsyncTask<IXmppFacade, Integer, Boolean> {

    /**
	 * State of a running connection.
	 */
    public static final int STATE_CONNECTION_RUNNING = 0;

    /**
	 * State of an already connected connection but authentication is running.
	 */
    public static final int STATE_LOGIN_RUNNING = 1;

    /**
	 * State of a connected and authenticated succesfully.
	 */
    public static final int STATE_LOGIN_SUCCESS = 2;

    /**
	 * State of a connected but failed authentication.
	 */
    public static final int STATE_LOGIN_FAILED = 3;

    private static final String TAG = "BeemLoginTask";

    private IXmppConnection mConnection;

    private String mErrorMessage;

    /**
	 * Constructor.
	 */
    public LoginAsyncTask() {
    }

    @Override
    protected Boolean doInBackground(IXmppFacade... params) {
        boolean result = true;
        IXmppFacade facade = params[0];
        try {
            publishProgress(STATE_CONNECTION_RUNNING);
            mConnection = facade.createConnection();
            if (!mConnection.connect()) {
                mErrorMessage = mConnection.getErrorMessage();
                return false;
            }
            publishProgress(STATE_LOGIN_RUNNING);
            if (!mConnection.login()) {
                mErrorMessage = mConnection.getErrorMessage();
                publishProgress(STATE_LOGIN_FAILED);
                return false;
            }
            publishProgress(STATE_LOGIN_SUCCESS);
        } catch (RemoteException e) {
            mErrorMessage = "Exception during connection :" + e;
            result = false;
        }
        return result;
    }

    /**
	 * Make sur to call the parent method when overriding this method.
	 */
    @Override
    protected void onCancelled() {
        try {
            if (mConnection != null && mConnection.isAuthentificated()) {
                mConnection.disconnect();
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Remote exception", e);
        }
    }

    /**
	 * Get the error Message.
	 * 
	 * @return the error message. null if no error
	 */
    public String getErrorMessage() {
        return mErrorMessage;
    }
}
