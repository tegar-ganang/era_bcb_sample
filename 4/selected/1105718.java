package org.opennfc.service;

import org.opennfc.ConnectionProperty;
import org.opennfc.ConvertInformationNfc;
import org.opennfc.HelperForNfc;
import org.opennfc.NfcErrorCode;
import org.opennfc.NfcException;
import org.opennfc.NfcManager;
import org.opennfc.NfcPriority;
import org.opennfc.cardlistener.CardDetectionEventHandler;
import org.opennfc.cardlistener.Connection;
import org.opennfc.hardware.CollisionEventHandler;
import org.opennfc.hardware.NfcControllerExceptionEventHandler;
import org.opennfc.nfctag.NdefMessage;
import org.opennfc.nfctag.NdefTypeNameFormat;
import org.opennfc.nfctag.NfcTagConnection;
import org.opennfc.nfctag.NfcTagDetectionEventHandler;
import org.opennfc.nfctag.NfcTagManager;
import org.opennfc.service.communication.ClientServer;
import org.opennfc.service.communication.ClientServerCache;
import org.opennfc.tests.TestUtil;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.nfc.ErrorCodes;
import android.nfc.FormatException;
import android.nfc.INfcTag;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TransceiveResult;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import java.nio.charset.Charsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Open NFC background service.<br>
 * It listen tag events and send an {@link Intent}
 */
public class OpenNFCBackgoundService extends Service implements NfcTagDetectionEventHandler {

    /**
     * Listener on controller issue.<br>
     * Actually does nothing but log
     */
    class ControllerListener implements CollisionEventHandler, NfcControllerExceptionEventHandler {

        /**
         * Call when collision is detected <br>
         * <br>
         * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
         * 
         * @see org.opennfc.hardware.CollisionEventHandler#onCollisionDetected()
         */
        @Override
        public void onCollisionDetected() {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "ControllerListener ::::: onCollisionDetected");
            }
        }

        /**
         * Call when an exception append <br>
         * <br>
         * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
         * 
         * @see org.opennfc.hardware.NfcControllerExceptionEventHandler#onExceptionOccured()
         */
        @Override
        public void onExceptionOccured() {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "ControllerListener ::::: onExceptionOccured");
            }
        }
    }

    /** Enable/disable debug */
    private static final boolean DEBUG = true;

    /**
     * Time out by default on transceive transaction in millisecond (Actually :
     * 1 second)
     */
    private static final int DEFAULT_TRANSCEIVE_TIMEOUT = 1000;

    /** Key for the message sent inside the sent {@link Intent} */
    public static final String EXTRA_NDEF_MESSAGES = "android.nfc.extra.NDEF_MESSAGES";

    /** Tag extra for NFC inside the {@link Intent} sent */
    public static final String EXTRA_TAG = "android.nfc.extra.TAG";

    /** Filters define by user */
    static IntentFilter[] mDispatchOverrideFilters;

    /** Intent define by user */
    static PendingIntent mDispatchOverrideIntent;

    /** Technologies define by user */
    static String[][] mDispatchOverrideTechLists;

    /** Answer used when tag is lost */
    private static final TransceiveResult RESULT_TAG_LOST = new TransceiveResult(false, true, null);

    /** Client/server URI use for P2P */
    public static final String SERVER_COM_ANDROID_NPP_NAME = "com.android.npp";

    /** Open NFC service background name */
    public static final String SERVICE_NAME = "OpenNFCBackgoundService";

    /** Instance of actual background service */
    public static OpenNFCBackgoundService sOpenNFCBackgoundService;

    /** Tag use in debug */
    private static final String TAG = OpenNFCBackgoundService.class.getSimpleName();

    /**
     * Obtain the NFC manager
     * 
     * @return NFC Manager
     */
    public static NfcManager getNfcManager() {
        return OpenNFCBackgoundService.sOpenNFCBackgoundService.mNfcManager;
    }

    /** For communicate with the background service */
    private final IOpenNFCBackgroundService.Stub iOpenNFCBackgroundService = new IOpenNFCBackgroundService.Stub() {

        /**
         * Ask the background to register
         */
        @Override
        public void passOnManualManagement(final IBinder binder) {
            if (binder == null) {
                Log.e(OpenNFCBackgoundService.TAG, "The binder MUSN'T be null !");
                return;
            }
            final IOpenNFCBackgroundService tmp = IOpenNFCBackgroundService.Stub.asInterface(binder);
            if (tmp == null) {
                Log.e(OpenNFCBackgoundService.TAG, "The binder MUST be the binder gives by onServiceConnected when binding with 'org.opennfc.service.OpenNFCBackgoundService.ACTION'");
                return;
            }
            try {
                binder.linkToDeath(OpenNFCBackgoundService.this.mDeathRecipient, 0);
            } catch (final RemoteException exception) {
                Log.e(OpenNFCBackgoundService.TAG, "The passOnManualManagement failed !", exception);
                return;
            }
            OpenNFCBackgoundService.this.unregister();
        }
    };

    /** Indicates if the background service is registered */
    private boolean isRegistered = false;

    /** Card detection event handler */
    private final CardDetectionEventHandler mCardDetectionEventHandler = new CardDetectionEventHandler() {

        /**
         * Call when card is detected <br>
         * <br>
         * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
         * 
         * @param connection Connection link with the detected card
         * @see org.opennfc.cardlistener.CardDetectionEventHandler#onCardDetected(org.opennfc.cardlistener.Connection)
         */
        @Override
        public void onCardDetected(final Connection connection) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Card detected ! " + connection);
            }
            if (connection instanceof NfcTagConnection) {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "Usal tag treament");
                }
                OpenNFCBackgoundService.this.onTagDetected((NfcTagConnection) connection);
                return;
            }
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Need special treament .... ");
            }
            for (final ConnectionProperty connectionProperty : connection.getProperties()) {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "> " + connectionProperty);
                }
            }
            ThreadedTaskManager.MANAGER.doInSeparateThread(OpenNFCBackgoundService.this.threadedSpecialTreatment, connection);
        }

        /**
         * Call when card detection is on error <br>
         * <br>
         * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
         * 
         * @param what Error information
         * @see org.opennfc.cardlistener.CardDetectionEventHandler#onCardDetectedError(org.opennfc.NfcErrorCode)
         */
        @Override
        public void onCardDetectedError(final NfcErrorCode what) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Error in CardDetectionEventHandler = " + what);
            }
        }
    };

    /** Controller listener */
    final ControllerListener mControllerListener = new ControllerListener();

    final DeathRecipient mDeathRecipient = new DeathRecipient() {

        @Override
        public void binderDied() {
            Log.d(OpenNFCBackgoundService.TAG, "   |");
            Log.d(OpenNFCBackgoundService.TAG, "---+---");
            Log.d(OpenNFCBackgoundService.TAG, "   |");
            Log.d(OpenNFCBackgoundService.TAG, "   |");
            Log.d(OpenNFCBackgoundService.TAG, "Rest in peace !");
            OpenNFCBackgoundService.this.register();
        }
    };

    /** Link with tag and Android API */
    INfcTag.Stub mINfcTag = new INfcTag.Stub() {

        /** Last append error code */
        private int lastErrorCode;

        /** Last append exception */
        private NfcException lastException;

        /** Actual transceive timeout */
        private int timeout = OpenNFCBackgoundService.DEFAULT_TRANSCEIVE_TIMEOUT;

        /**
         * Close the connection<br>
         * <br>
         * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
         * 
         * @param nativeHandle Tag handle
         * @return Operation result : {@link ErrorCodes#SUCCESS} on success
         * @throws RemoteException If link is broken
         * @see android.nfc.INfcTag#close(int)
         */
        @Override
        public int close(final int nativeHandle) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null) {
                Log.v(OpenNFCBackgoundService.TAG, "Not found : " + nativeHandle);
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return this.lastErrorCode;
            }
            if (OpenNFCBackgoundService.DEBUG) {
                Log.v(OpenNFCBackgoundService.TAG, "WARNING CLOSE : " + nativeHandle);
            }
            nfcInformation.finishTime();
            this.lastErrorCode = ErrorCodes.SUCCESS;
            return ErrorCodes.SUCCESS;
        }

        /**
         * Connect the tag <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Tag handle
         * @param technology Technology to connect
         * @return Operation result : {@link ErrorCodes#SUCCESS} on success
         * @throws RemoteException On link broken
         * @see android.nfc.INfcTag#connect(int, int)
         */
        @Override
        public int connect(final int nativeHandle, final int technology) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "connect : " + nativeHandle + " notFound !");
                }
                return this.lastErrorCode;
            }
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "technology search : " + technology);
            }
            for (final int tech : nfcInformation.techList) {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "tech possible : " + tech);
                }
                if (tech == technology) {
                    nfcInformation.connectedStatus = NfcInformation.STATUS_CONNECTED;
                    nfcInformation.intializeTime();
                    return ErrorCodes.SUCCESS;
                }
            }
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "technology : " + technology + " notFound !");
            }
            this.lastErrorCode = ErrorCodes.ERROR_INVALID_PARAM;
            return this.lastErrorCode;
        }

        /**
         * Format a tag <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Tag connection handle
         * @param key Data used for format
         * @return Operation result : {@link ErrorCodes#SUCCESS} on success
         * @throws RemoteException On link broken
         * @see android.nfc.INfcTag#formatNdef(int, byte[])
         */
        @Override
        public int formatNdef(final int nativeHandle, final byte[] key) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "formatNdef : " + nativeHandle);
            }
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "Not found !");
                }
                return this.lastErrorCode;
            }
            nfcInformation.intializeTime();
            final NfcTagConnection connection = nfcInformation.mNfcTagConnection;
            try {
                nfcInformation.intializeTime();
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "Will test read only");
                }
                if (connection.isReadOnly() == true) {
                    nfcInformation.intializeTime();
                    if (OpenNFCBackgoundService.DEBUG) {
                        Log.d(OpenNFCBackgoundService.TAG, "Is read only");
                    }
                    return this.lastErrorCode = ErrorCodes.ERROR_WRITE;
                }
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "read actual content");
                }
                final org.opennfc.nfctag.NdefRecord ndefRecord = new org.opennfc.nfctag.NdefRecord(NdefTypeNameFormat.EMPTY, (String) null, (byte[]) null);
                final NdefMessage ndefMessage = new NdefMessage(ndefRecord.getContent());
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "will write and format");
                }
                nfcInformation.intializeTime();
                connection.writeMessage(ndefMessage, NfcTagManager.ACTION_BIT_CHECK_WRITE | NfcTagManager.ACTION_BIT_ERASE | NfcTagManager.ACTION_BIT_FORMAT_BLANK_TAG);
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "Write and format done!");
                }
            } catch (final NfcException exception) {
                this.lastException = exception;
                this.lastErrorCode = HelperForNfc.obtainErrorCode(this.lastException, ErrorCodes.ERROR_WRITE);
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "Issue while write and format", exception);
                }
            } catch (final Exception exception) {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.e(OpenNFCBackgoundService.TAG, "Oulalalalalalalalala", exception);
                }
            } catch (final Error error) {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.e(OpenNFCBackgoundService.TAG, "Aie aie aie aie", error);
                }
            }
            nfcInformation.intializeTime();
            return this.lastErrorCode;
        }

        /**
         * Last error append information <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Handle to the link
         * @return Last error append code
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#getLastError(int)
         */
        @Override
        public int getLastError(final int nativeHandle) throws RemoteException {
            return HelperForNfc.obtainErrorCode(this.lastException, this.lastErrorCode);
        }

        /**
         * Obtain technologies list of a tag <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Link handle
         * @return Technologies list or {@code null} if can 't connect
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#getTechList(int)
         */
        @Override
        public int[] getTechList(final int nativeHandle) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return null;
            }
            nfcInformation.intializeTime();
            return nfcInformation.techList;
        }

        /**
         * Obtain the UID <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Handle
         * @return UID or {@code null} on error
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#getUid(int)
         */
        @Override
        public byte[] getUid(final int nativeHandle) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return null;
            }
            nfcInformation.intializeTime();
            final NfcTagConnection connection = nfcInformation.mNfcTagConnection;
            try {
                final byte[] result = nfcInformation.overideID != null ? nfcInformation.overideID : connection.getIdentifier();
                nfcInformation.intializeTime();
                return result;
            } catch (final NfcException exception) {
                this.lastException = exception;
                this.lastErrorCode = HelperForNfc.obtainErrorCode(this.lastException, ErrorCodes.ERROR_READ);
            }
            nfcInformation.intializeTime();
            return null;
        }

        /**
         * Indicates if tag is Ndef <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Link handle
         * @return {@code true} if it is Ndef
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#isNdef(int)
         */
        @Override
        public boolean isNdef(final int nativeHandle) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return false;
            }
            if (OpenNFCBackgoundService.DEBUG) {
                Log.v(OpenNFCBackgoundService.TAG, "May be do better thing to know the Ndef status");
            }
            nfcInformation.intializeTime();
            return true;
        }

        /**
         * Indicates if tag is present <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Tag handle
         * @return {@code true} if tag is present
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#isPresent(int)
         */
        @Override
        public boolean isPresent(final int nativeHandle) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return false;
            }
            if (OpenNFCBackgoundService.DEBUG) {
                Log.v(OpenNFCBackgoundService.TAG, "May be do better thing to know the presence");
            }
            nfcInformation.intializeTime();
            return true;
        }

        /**
         * indicates if it is a writable tag <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Tag handle
         * @return {@code true} if it is writable
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#ndefIsWritable(int)
         */
        @Override
        public boolean ndefIsWritable(final int nativeHandle) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return false;
            }
            nfcInformation.intializeTime();
            final NfcTagConnection connection = nfcInformation.mNfcTagConnection;
            try {
                final boolean result = !connection.isReadOnly();
                nfcInformation.intializeTime();
                return result;
            } catch (final NfcException exception) {
                this.lastException = exception;
                this.lastErrorCode = HelperForNfc.obtainErrorCode(this.lastException, ErrorCodes.ERROR_WRITE);
            }
            nfcInformation.intializeTime();
            return false;
        }

        /**
         * Make the tag on ready only mode , after that it will be locked <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Tag link handle
         * @return Operation result : {@link ErrorCodes#SUCCESS} on success
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#ndefMakeReadOnly(int)
         */
        @Override
        public int ndefMakeReadOnly(final int nativeHandle) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return this.lastErrorCode;
            }
            nfcInformation.intializeTime();
            final NfcTagConnection connection = nfcInformation.mNfcTagConnection;
            try {
                nfcInformation.intializeTime();
                if (connection.isReadOnly() == true) {
                    nfcInformation.intializeTime();
                    return this.lastErrorCode = ErrorCodes.SUCCESS;
                }
                nfcInformation.intializeTime();
                if (connection.isLockable() == false) {
                    nfcInformation.intializeTime();
                    return this.lastErrorCode = ErrorCodes.ERROR_WRITE;
                }
                nfcInformation.intializeTime();
                connection.writeMessage(connection.readMessage(), NfcTagManager.ACTION_BIT_CHECK_WRITE | NfcTagManager.ACTION_BIT_ERASE | NfcTagManager.ACTION_BIT_LOCK);
            } catch (final NfcException exception) {
                this.lastException = exception;
                this.lastErrorCode = HelperForNfc.obtainErrorCode(this.lastException, ErrorCodes.ERROR_WRITE);
            }
            nfcInformation.intializeTime();
            return this.lastErrorCode;
        }

        /**
         * Read message on tag <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Tag link handle
         * @return Read message or {@code null} on read issue or not message
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#ndefRead(int)
         */
        @Override
        public android.nfc.NdefMessage ndefRead(final int nativeHandle) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return null;
            }
            nfcInformation.intializeTime();
            final NfcTagConnection connection = nfcInformation.mNfcTagConnection;
            try {
                final android.nfc.NdefMessage message = new android.nfc.NdefMessage(connection.readMessage().getContent());
                nfcInformation.intializeTime();
                return message;
            } catch (final NfcException exception) {
                this.lastException = exception;
                this.lastErrorCode = HelperForNfc.obtainErrorCode(this.lastException, ErrorCodes.ERROR_IO);
            } catch (final FormatException exception) {
                this.lastErrorCode = HelperForNfc.obtainErrorCode(this.lastException, ErrorCodes.ERROR_INVALID_PARAM);
            }
            nfcInformation.intializeTime();
            return null;
        }

        /**
         * Write a message <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Tag link handle
         * @param msg Message to write
         * @return Operation result : {@link ErrorCodes#SUCCESS} on success
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#ndefWrite(int, android.nfc.NdefMessage)
         */
        @Override
        public int ndefWrite(final int nativeHandle, final android.nfc.NdefMessage msg) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return this.lastErrorCode;
            }
            nfcInformation.intializeTime();
            final NfcTagConnection connection = nfcInformation.mNfcTagConnection;
            try {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "WILL write ! WILL write ! WILL write ! WILL write ! WILL write ! WILL write ! ");
                }
                connection.writeMessage(new NdefMessage(msg.toByteArray()), NfcTagManager.ACTION_BIT_CHECK_WRITE | NfcTagManager.ACTION_BIT_ERASE);
            } catch (final NfcException exception) {
                this.lastException = exception;
                this.lastErrorCode = HelperForNfc.obtainErrorCode(this.lastException, ErrorCodes.ERROR_WRITE);
                nfcInformation.intializeTime();
                return this.lastErrorCode;
            } catch (final Exception exception) {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.e(OpenNFCBackgoundService.TAG, "Oulalalalalalalalala 2", exception);
                }
            } catch (final Error error) {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.e(OpenNFCBackgoundService.TAG, "Aie aie aie aie 2", error);
                }
            }
            nfcInformation.intializeTime();
            this.lastErrorCode = ErrorCodes.SUCCESS;
            return ErrorCodes.SUCCESS;
        }

        /**
         * Try to reconnect a tag <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Tag link handle
         * @return Operation result : {@link ErrorCodes#SUCCESS} on success
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#reconnect(int)
         */
        @Override
        public int reconnect(final int nativeHandle) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return this.lastErrorCode;
            }
            nfcInformation.intializeTime();
            this.lastErrorCode = ErrorCodes.SUCCESS;
            return this.close(nativeHandle);
        }

        /**
         * Reset the transceive timeout <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#resetIsoDepTimeout()
         */
        @Override
        public void resetIsoDepTimeout() throws RemoteException {
            this.lastException = null;
            this.timeout = OpenNFCBackgoundService.DEFAULT_TRANSCEIVE_TIMEOUT;
            this.lastErrorCode = ErrorCodes.SUCCESS;
        }

        /**
         * Modify the transceive timeout <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param timeout New timeout in milliseconds
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#setIsoDepTimeout(int)
         */
        @Override
        public void setIsoDepTimeout(final int timeout) throws RemoteException {
            this.lastException = null;
            this.timeout = timeout;
            this.lastErrorCode = ErrorCodes.SUCCESS;
        }

        /**
         * Send data to the tag <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param nativeHandle Tag link handle
         * @param data Data to send
         * @param raw Indicates if data are in raw mode
         * @return The tag answer or {@code null} if failed or no answer
         * @throws RemoteException On broken link
         * @see android.nfc.INfcTag#transceive(int, byte[], boolean)
         */
        @Override
        public TransceiveResult transceive(final int nativeHandle, final byte[] data, final boolean raw) throws RemoteException {
            this.lastException = null;
            this.lastErrorCode = ErrorCodes.SUCCESS;
            final NfcInformation nfcInformation = NfcInformationCache.getCache().get(nativeHandle);
            if (nfcInformation == null || nfcInformation.connectedStatus != NfcInformation.STATUS_CONNECTED) {
                this.lastErrorCode = ErrorCodes.ERROR_DISCONNECT;
                return OpenNFCBackgoundService.RESULT_TAG_LOST;
            }
            nfcInformation.intializeTime();
            final NfcTagConnection connection = nfcInformation.mNfcTagConnection;
            boolean sucess = false;
            byte[] answer = null;
            try {
                answer = HelperForNfc.exchange(connection, data, this.timeout);
                nfcInformation.intializeTime();
                sucess = true;
                this.lastErrorCode = ErrorCodes.SUCCESS;
            } catch (final NfcException exception) {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "Error append in exchange", exception);
                }
                this.lastException = exception;
            }
            nfcInformation.intializeTime();
            return new TransceiveResult(sucess, false, answer);
        }
    };

    /** NFC manager linked */
    private NfcManager mNfcManager;

    /** NFC TAG manager linked */
    private NfcTagManager mNfcTagManager;

    /**
     * Stop NFC manager
     */
    private final ThreadedTask<Object> mStopNfcManager = new ThreadedTask<Object>() {

        /**
         * Cancel the task <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @see org.opennfc.service.ThreadedTask#cancel()
         */
        @Override
        public void cancel() {
        }

        /**
         * Stop NFC manager <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param taskID Unused
         * @param parameters Unused
         * @see ThreadedTask#excuteTask(int, Object...)
         */
        @Override
        public void excuteTask(final int taskID, final Object... parameters) {
            try {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "stop NFC manager");
                }
                if (OpenNFCBackgoundService.this.mNfcManager != null) {
                    final Intent intent = new Intent(NfcAdapter.ACTION_ADAPTER_STATE_CHANGE);
                    intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                    intent.putExtra(NfcAdapter.EXTRA_NEW_BOOLEAN_STATE, false);
                    OpenNFCBackgoundService.this.sendBroadcast(intent);
                    OpenNFCBackgoundService.this.mNfcManager.stop();
                }
            } catch (final IllegalStateException e) {
                e.printStackTrace();
            } catch (final NfcException e) {
                e.printStackTrace();
            }
        }
    };

    /** For sensitive operations */
    private WakeLock mWakeLock;

    /** Task call when tag is detected */
    private final ThreadedTask<NfcTagConnection> onTagDetectedTask = new ThreadedTask<NfcTagConnection>() {

        /**
         * Cancel the task <br>
         * <br>
         * <u> <b> Documentation from parent : </b></u> <br> {@inheritDoc}
         * 
         * @see org.opennfc.service.ThreadedTask#cancel()
         */
        @Override
        public void cancel() {
        }

        /**
         * Parse the received tag and send the good intent <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @param taskID Task ID : unused
         * @param parameters The tag connection
         * @see ThreadedTask#excuteTask(int, Object...)
         */
        @Override
        public void excuteTask(final int taskID, final NfcTagConnection... parameters) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.v(OpenNFCBackgoundService.TAG, "I see a TAG !");
            }
            final NfcTagConnection connection = parameters[0];
            try {
                NdefMessage message = null;
                try {
                    message = connection.readMessage();
                } catch (final Exception exception) {
                }
                android.nfc.NdefMessage[] msgs = null;
                Log.d(OpenNFCBackgoundService.TAG, "onTagDetected");
                final byte[] content = message == null ? null : message.getContent();
                if (content != null) {
                    msgs = new android.nfc.NdefMessage[] { new android.nfc.NdefMessage(content) };
                }
                final ConvertInformationNfc convertInformationNfc = ConvertInformationNfc.create(connection, msgs == null ? null : msgs[0], content == null ? 0 : content.length);
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.v(OpenNFCBackgoundService.TAG, "Send tag : " + convertInformationNfc.handle);
                }
                final Tag tag = new Tag(connection.getIdentifier(), convertInformationNfc.technologies, convertInformationNfc.bundles, convertInformationNfc.handle, OpenNFCBackgoundService.this.mINfcTag);
                OpenNFCBackgoundService.this.mINfcTag.resetIsoDepTimeout();
                NfcInformationCache.getCache().add(connection, convertInformationNfc);
                if (OpenNFCBackgoundService.this.sendTag(tag, msgs) == true) {
                    if (OpenNFCBackgoundService.DEBUG) {
                        Log.d(OpenNFCBackgoundService.TAG, "SUCCES to SEND !");
                    }
                } else {
                    NfcInformationCache.getCache().remove(convertInformationNfc.handle);
                    if (OpenNFCBackgoundService.DEBUG) {
                        Log.d(OpenNFCBackgoundService.TAG, "REMOVE !");
                    }
                }
            } catch (final Exception exception) {
                if (OpenNFCBackgoundService.DEBUG) {
                    Log.d(OpenNFCBackgoundService.TAG, "Tag detection issue", exception);
                }
            }
        }
    };

    /** Server handle ID */
    int server_com_android_npp;

    /** Separate thread for send a message */
    private final ThreadedTask<byte[]> threadedSendMessage = new ThreadedTask<byte[]>() {

        /**
         * Call when task is cancel <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @see org.opennfc.service.ThreadedTask#cancel()
         */
        @Override
        public void cancel() {
        }

        /**
         * Send the message
         * 
         * @param taskID Task ID
         * @param parameters First argument : message to send
         */
        @Override
        public void excuteTask(final int taskID, final byte[]... parameters) {
            OpenNFCBackgoundService.this.sendTag(Tag.createMockTag(null, new int[] { TagTechnology.NDEF }, new Bundle[] { new Bundle() }), new android.nfc.NdefMessage[] { new android.nfc.NdefMessage(new NdefRecord[] { new NdefRecord(NdefRecord.TNF_UNKNOWN, new byte[0], new byte[0], parameters[0]) }) });
        }
    };

    /** Threaded special treatment */
    final ThreadedTask<Connection> threadedSpecialTreatment = new ThreadedTask<Connection>() {

        /**
         * Call when task is cancel <br>
         * <br>
         * < u > < b > Documentation from parent : < / b > < / u > <br>
         * {@inheritDoc}
         * 
         * @see org.opennfc.service.ThreadedTask#cancel()
         */
        @Override
        public void cancel() {
        }

        /**
         * Do the treatment
         * 
         * @param taskID Task ID
         * @param parameters First argument : connection to use
         */
        @Override
        public void excuteTask(final int taskID, final Connection... parameters) {
            final Connection connection = parameters[0];
            final ConvertInformationNfc convertInformationNfc = ConvertInformationNfc.createConnection(connection);
            if (convertInformationNfc != null) {
                try {
                    final Tag tag = Tag.createMockTag(convertInformationNfc.overrideID, convertInformationNfc.technologies, convertInformationNfc.bundles);
                    if (OpenNFCBackgoundService.this.sendTag(tag, null) == true) {
                        if (OpenNFCBackgoundService.DEBUG) {
                            Log.d(OpenNFCBackgoundService.TAG, "SUCCES to SEND  ---MOCK--- !");
                        }
                    } else {
                        if (OpenNFCBackgoundService.DEBUG) {
                            Log.d(OpenNFCBackgoundService.TAG, "Can't send ---MOCK---");
                        }
                    }
                } catch (final Exception exception) {
                    if (OpenNFCBackgoundService.DEBUG) {
                        Log.d(OpenNFCBackgoundService.TAG, "Error while try send mock", exception);
                    }
                }
            }
            connection.close();
        }
    };

    /**
     * Build intent for sending
     * 
     * @param tag Tag to sent
     * @param msgs Messages to sent
     * @param action Intent action
     * @return The created intent
     */
    private Intent buildTagIntent(final Tag tag, final android.nfc.NdefMessage[] msgs, final String action) {
        final Intent intent = new Intent(action);
        intent.putExtra(NfcAdapter.EXTRA_TAG, tag);
        intent.putExtra(NfcAdapter.EXTRA_ID, tag.getId());
        if (msgs != null) {
            intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, msgs);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Call when someone bind to the service<br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param intent Intent used for bind
     * @return Link to communicate with the service
     * @see Service#onBind(Intent)
     */
    @Override
    public IBinder onBind(final Intent intent) {
        Log.d(OpenNFCBackgoundService.TAG, "onBind");
        return this.iOpenNFCBackgroundService;
    }

    /**
     * Call when service created<br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @see Service#onCreate()
     */
    @Override
    public void onCreate() {
        this.isRegistered = false;
        this.server_com_android_npp = -1;
        OpenNFCBackgoundService.sOpenNFCBackgoundService = this;
        super.onCreate();
        this.mWakeLock = ((PowerManager) this.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, OpenNFCBackgoundService.TAG);
        NfcInformationCache.getCache().clear();
        Log.i(OpenNFCBackgoundService.TAG, "onCreate");
        this.showToast("Create the background service for NFC");
        if (OpenNFCBackgoundService.DEBUG) {
            Log.d(OpenNFCBackgoundService.TAG, "get a single instance of the NFC manager");
        }
        this.mNfcManager = NfcManager.getInstance(this);
        if (OpenNFCBackgoundService.DEBUG) {
            Log.d(OpenNFCBackgoundService.TAG, "$$$$$$$$$$$$$$$$$$$$$$$>>>>>>>>>>>>>> mNfcManager=" + this.mNfcManager);
        }
        if (OpenNFCBackgoundService.DEBUG) {
            Log.d(OpenNFCBackgoundService.TAG, "get a single instance of the NFC Tag manager");
        }
        this.mNfcTagManager = this.mNfcManager.getNfcTagManager();
        try {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Start NFC Manager!");
            }
            this.mNfcManager.start();
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "NFC Manager started!");
            }
        } catch (final IllegalStateException exception) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Start failed !", exception);
            }
        } catch (final NfcException exception) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Start failed !", exception);
            }
        }
        try {
            this.mNfcManager.getCardListenerRegistry().registerCardListener(NfcPriority.MAXIMUM, this.mCardDetectionEventHandler);
            this.isRegistered = true;
            this.mNfcManager.getNfcController().registerCardCollisionHandler(this.mControllerListener);
            this.mNfcManager.getNfcController().registerExceptionEventHandler(this.mControllerListener);
        } catch (final Exception exception) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Issue when register as listener", exception);
            }
        }
        try {
            this.mNfcManager.getP2PManager().setLocalLto((short) 1230);
        } catch (final Exception exception) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Cant change the P2P timeout", exception);
            }
        }
        ClientServer clientServer = ClientServerCache.CACHE.get(OpenNFCBackgoundService.this.server_com_android_npp);
        if (clientServer == null) {
            OpenNFCBackgoundService.this.server_com_android_npp = ClientServerCache.CACHE.createClientServer(OpenNFCService.SERVER_COM_ANDROID_NPP_NAME, OpenNFCService.SERVER_SAP, true);
            clientServer = ClientServerCache.CACHE.get(OpenNFCBackgoundService.this.server_com_android_npp);
        }
        clientServer.connect();
        if (TestUtil.TEST_MODE == true) {
            TestUtil.startTests(this, this.mCardDetectionEventHandler);
        }
    }

    /**
     * Call when service is destroy<br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @see Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        final ClientServer clientServer = ClientServerCache.CACHE.get(OpenNFCBackgoundService.this.server_com_android_npp);
        if (clientServer != null) {
            clientServer.disconnect();
        }
        Log.e(OpenNFCBackgoundService.TAG, "onDestroy");
        this.showToast("Terminate the background service for NFC");
        ThreadedTaskManager.MANAGER.doInSeparateThread(this.mStopNfcManager);
        this.mNfcManager.getCardListenerRegistry().unregisterCardListener(this.mCardDetectionEventHandler);
        this.mNfcManager.getNfcController().unregisterCardCollisionHandler(this.mControllerListener);
        this.mNfcManager.getNfcController().unregisterExceptionEventHandler(this.mControllerListener);
        super.onDestroy();
    }

    /**
     * Call when someone rebind to the service<br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param intent Intent used for rebind
     * @see Service#onRebind(Intent)
     */
    @Override
    public void onRebind(final Intent intent) {
        if (OpenNFCBackgoundService.DEBUG) {
            Log.d(OpenNFCBackgoundService.TAG, "onRebind");
        }
    }

    /**
     * Call when some call startService<br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param intent Intent the launch the service
     * @param flags Flags used for launch
     * @param startId ID used on start
     * @return The way of close the service. here we want close it manually
     * @see Service#onStartCommand(Intent, int, int)
     */
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.i(OpenNFCBackgoundService.TAG, "onStartCommand:" + "Received start id " + startId + ": " + intent);
        return Service.START_STICKY;
    }

    /**
     * Call when a tag read<br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param connection The tag connection
     * @see NfcTagDetectionEventHandler#onTagDetected(NfcTagConnection)
     */
    @Override
    public void onTagDetected(final NfcTagConnection connection) {
        ThreadedTaskManager.MANAGER.doInSeparateThread(this.onTagDetectedTask, connection);
    }

    /**
     * Call when error append in NFC reading<br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param what Error description
     * @see NfcTagDetectionEventHandler#onTagDetectedError(NfcErrorCode)
     */
    @Override
    public void onTagDetectedError(final NfcErrorCode what) {
        Log.e(OpenNFCBackgoundService.TAG, "/!\\ onReadError : " + what);
        this.showToast("Read error on the background service for NFC : " + what.toString());
    }

    /**
     * Call when someone unbind to the service<br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param intent Intent used for rebind
     * @return {@code true} to indicates that rebind are allowed
     * @see Service#onUnbind(Intent)
     */
    @Override
    public boolean onUnbind(final Intent intent) {
        if (OpenNFCBackgoundService.DEBUG) {
            Log.d(OpenNFCBackgoundService.TAG, "onUnbind");
        }
        if (OpenNFCBackgoundService.DEBUG) {
            Log.d(OpenNFCBackgoundService.TAG, "I register again !!!");
        }
        this.register();
        return true;
    }

    /**
     * Register the background service for listen new incoming tag/card/phone
     */
    void register() {
        if (this.isRegistered == true) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Already regitered !");
            }
            return;
        }
        try {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "REGISTER");
            }
            this.mNfcManager.getCardListenerRegistry().registerCardListener(NfcPriority.MAXIMUM, this.mCardDetectionEventHandler);
            this.isRegistered = true;
        } catch (final Exception exception) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Register failed !", exception);
            }
        }
    }

    /**
     * Try to send an intent
     * 
     * @param intent Intent to send
     * @param overrideFilters User filters
     * @param overrideIntent User intent
     * @param overrideTechLists User technologies
     * @return {@code true} if intent is sent
     */
    private boolean sendIntent(final Intent intent, final IntentFilter[] overrideFilters, final PendingIntent overrideIntent, final String[][] overrideTechLists) {
        if (overrideIntent != null) {
            boolean found = false;
            if (overrideFilters == null && overrideTechLists == null) {
                found = true;
            } else if (overrideFilters != null) {
                for (final IntentFilter filter : overrideFilters) {
                    if (filter.match(OpenNFCBackgoundService.this.getContentResolver(), intent, false, OpenNFCBackgoundService.TAG) >= 0) {
                        found = true;
                        break;
                    }
                }
            }
            if (found == true) {
                Log.i(OpenNFCBackgoundService.TAG, "Dispatching to override intent " + overrideIntent);
                try {
                    overrideIntent.send(OpenNFCBackgoundService.this, Activity.RESULT_OK, intent);
                } catch (final CanceledException exception) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        } else {
            try {
                OpenNFCBackgoundService.this.startActivity(intent);
                return true;
            } catch (final ActivityNotFoundException e) {
                return false;
            }
        }
    }

    /**
     * Send a message
     * 
     * @param temp Message to send
     * @param length Message length
     */
    public void sendMessage(final byte[] temp, final int length) {
        ThreadedTaskManager.MANAGER.doInSeparateThread(this.threadedSendMessage, Arrays.copyOf(temp, length));
    }

    /**
     * Send Ndef discovered tag
     * 
     * @param tag Tag to send
     * @param msgs Messages to send
     * @param overrideFilters User filters
     * @param overrideIntent User intent
     * @param overrideTechLists User technologies
     * @return {@code true} if sent
     */
    private boolean sendNdefDiscovered(final Tag tag, final android.nfc.NdefMessage[] msgs, final IntentFilter[] overrideFilters, final PendingIntent overrideIntent, final String[][] overrideTechLists) {
        if (msgs != null && msgs.length > 0) {
            final android.nfc.NdefMessage msg = msgs[0];
            final android.nfc.NdefRecord[] records = msg.getRecords();
            if (records.length > 0) {
                final android.nfc.NdefRecord record = records[0];
                final Intent intent = this.buildTagIntent(tag, msgs, NfcAdapter.ACTION_NDEF_DISCOVERED);
                if (this.setTypeOrDataFromNdef(intent, record) == true) {
                    if (this.sendIntent(intent, overrideFilters, overrideIntent, overrideTechLists) == true) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Send a tag throw intent
     * 
     * @param tag Tag to send
     * @param msgs Messages to send
     * @return {@code true} if at least one intent sent
     */
    private boolean sendTag(final Tag tag, final android.nfc.NdefMessage[] msgs) {
        IntentFilter[] overrideFilters;
        PendingIntent overrideIntent;
        String[][] overrideTechLists;
        synchronized (OpenNFCService.sOpenNFCService.mNfcAdapter) {
            overrideFilters = OpenNFCBackgoundService.mDispatchOverrideFilters;
            overrideIntent = OpenNFCBackgoundService.mDispatchOverrideIntent;
            overrideTechLists = OpenNFCBackgoundService.mDispatchOverrideTechLists;
        }
        if (this.sendNdefDiscovered(tag, msgs, overrideFilters, overrideIntent, overrideTechLists) == true) {
            return true;
        }
        if (OpenNFCBackgoundService.DEBUG) {
            Log.v(OpenNFCBackgoundService.TAG, "sendNdefDiscovered failed");
        }
        if (this.sendTechDiscovered(tag, msgs, overrideFilters, overrideIntent, overrideTechLists) == true) {
            return true;
        }
        if (OpenNFCBackgoundService.DEBUG) {
            Log.v(OpenNFCBackgoundService.TAG, "sendTechDiscovered failed");
        }
        return this.sendTagDiscovered(tag, msgs, overrideFilters, overrideIntent, overrideTechLists);
    }

    /**
     * Send tag discovered intent
     * 
     * @param tag Tag to send
     * @param msgs Messages to send
     * @param overrideFilters User filters
     * @param overrideIntent User intent
     * @param overrideTechLists User technologies
     * @return {@code true} if intent sent
     */
    private boolean sendTagDiscovered(final Tag tag, final android.nfc.NdefMessage[] msgs, final IntentFilter[] overrideFilters, final PendingIntent overrideIntent, final String[][] overrideTechLists) {
        final Intent intent = this.buildTagIntent(tag, msgs, NfcAdapter.ACTION_TAG_DISCOVERED);
        final boolean result = this.sendIntent(intent, overrideFilters, overrideIntent, overrideTechLists);
        if (result == false) {
            Log.w(OpenNFCBackgoundService.TAG, "Tag discovered not sent");
        }
        return result;
    }

    public void sendTagLost() {
        this.sendIntent(new Intent(NfcAdapter.ACTION_TAG_LEFT_FIELD), null, null, null);
    }

    /**
     * Send technology discovered intent
     * 
     * @param tag Tag to send
     * @param msgs Messages to send
     * @param overrideFilters User filters
     * @param overrideIntent User intent
     * @param overrideTechLists User technologies
     * @return {@code true} if intent sent
     */
    private boolean sendTechDiscovered(final Tag tag, final android.nfc.NdefMessage[] msgs, final IntentFilter[] overrideFilters, final PendingIntent overrideIntent, final String[][] overrideTechLists) {
        final String[] tagTechs = tag.getTechList();
        final ArrayList<ResolveInfo> matches = RegisteredPackageList.registeredPacakgeList.listOfActivities(NfcAdapter.ACTION_TECH_DISCOVERED, NfcAdapter.ACTION_TECH_DISCOVERED, tagTechs);
        final int size = matches == null ? 0 : matches.size();
        if (size == 1) {
            final Intent intent = this.buildTagIntent(tag, msgs, NfcAdapter.ACTION_TECH_DISCOVERED);
            final ResolveInfo info = matches.get(0);
            intent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            return this.sendIntent(intent, overrideFilters, overrideIntent, overrideTechLists);
        } else if (size > 1) {
            final Intent intent = new Intent(this, TechListChooserActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_INTENT, this.buildTagIntent(tag, msgs, NfcAdapter.ACTION_TECH_DISCOVERED));
            intent.putParcelableArrayListExtra(TechListChooserActivity.EXTRA_RESOLVE_INFOS, matches);
            try {
                this.startActivity(intent);
                return true;
            } catch (final ActivityNotFoundException e) {
                if (OpenNFCBackgoundService.DEBUG == true) {
                    Log.w(OpenNFCBackgoundService.TAG, "No activities for technology handling of " + intent);
                }
            }
        }
        return false;
    }

    /**
     * Add type or data if record if ndef message
     * 
     * @param intent Intent where add info
     * @param record Record to parse
     * @return {@code true} if it is a ndef message and correctly filled
     */
    private boolean setTypeOrDataFromNdef(final Intent intent, final android.nfc.NdefRecord record) {
        final short tnf = record.getTnf();
        final byte[] type = record.getType();
        try {
            switch(tnf) {
                case android.nfc.NdefRecord.TNF_MIME_MEDIA:
                    {
                        intent.setType(new String(type, Charsets.US_ASCII));
                        return true;
                    }
                case android.nfc.NdefRecord.TNF_ABSOLUTE_URI:
                    {
                        intent.setData(Uri.parse(new String(record.getPayload(), Charsets.UTF_8)));
                        return true;
                    }
                case android.nfc.NdefRecord.TNF_WELL_KNOWN:
                    {
                        final byte[] payload = record.getPayload();
                        if (payload == null || payload.length == 0) {
                            return false;
                        }
                        if (Arrays.equals(type, android.nfc.NdefRecord.RTD_TEXT)) {
                            intent.setType("text/plain");
                            return true;
                        } else if (Arrays.equals(type, android.nfc.NdefRecord.RTD_SMART_POSTER)) {
                            try {
                                final android.nfc.NdefMessage msg = new android.nfc.NdefMessage(record.getPayload());
                                for (final android.nfc.NdefRecord subRecord : msg.getRecords()) {
                                    final short subTnf = subRecord.getTnf();
                                    if (subTnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && Arrays.equals(subRecord.getType(), android.nfc.NdefRecord.RTD_URI)) {
                                        intent.setData(HelperForNfc.parseWellKnownUriRecord(subRecord));
                                        return true;
                                    } else if (subTnf == android.nfc.NdefRecord.TNF_ABSOLUTE_URI) {
                                        intent.setData(Uri.parse(new String(subRecord.getPayload(), Charsets.UTF_8)));
                                        return true;
                                    }
                                }
                            } catch (final FormatException e) {
                                return false;
                            }
                        } else if (Arrays.equals(type, android.nfc.NdefRecord.RTD_URI)) {
                            intent.setData(HelperForNfc.parseWellKnownUriRecord(record));
                            return true;
                        }
                        return false;
                    }
            }
            return false;
        } catch (final Exception e) {
            Log.e(OpenNFCBackgoundService.TAG, "failed to parse record", e);
            return false;
        }
    }

    /**
     * Show a toast
     * 
     * @param text Text to show
     */
    public void showToast(final String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * Unregister background service, so it not alert any more about new
     * tag/card/phone
     */
    void unregister() {
        if (this.isRegistered == false) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Not regitered !");
            }
            return;
        }
        try {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "UNREGISTER");
            }
            this.mNfcManager.getCardListenerRegistry().unregisterCardListener(this.mCardDetectionEventHandler);
            this.isRegistered = false;
        } catch (final Exception exception) {
            if (OpenNFCBackgoundService.DEBUG) {
                Log.d(OpenNFCBackgoundService.TAG, "Unregister failed !", exception);
            }
        }
    }
}
