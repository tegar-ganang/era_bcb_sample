package android.media;

import java.lang.ref.WeakReference;
import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.media.AudioManager;
import android.util.Log;

/**
 * The AudioTrack class manages and plays a single audio resource for Java applications.
 * It allows to stream PCM audio buffers to the audio hardware for playback. This is
 * achieved by "pushing" the data to the AudioTrack object using one of the
 *  {@link #write(byte[], int, int)} and {@link #write(short[], int, int)} methods.
 *  
 * <p>An AudioTrack instance can operate under two modes: static or streaming.<br>
 * In Streaming mode, the application writes a continuous stream of data to the AudioTrack, using
 * one of the write() methods. These are blocking and return when the data has been transferred
 * from the Java layer to the native layer and queued for playback. The streaming mode
 *  is most useful when playing blocks of audio data that for instance are:
 * <ul>
 *   <li>too big to fit in memory because of the duration of the sound to play,</li>
 *   <li>too big to fit in memory because of the characteristics of the audio data
 *         (high sampling rate, bits per sample ...)</li>
 *   <li>received or generated while previously queued audio is playing.</li>
 * </ul>
 * The static mode is to be chosen when dealing with short sounds that fit in memory and
 * that need to be played with the smallest latency possible. AudioTrack instances in static mode
 * can play the sound without the need to transfer the audio data from Java to native layer
 * each time the sound is to be played. The static mode will therefore be preferred for UI and
 * game sounds that are played often, and with the smallest overhead possible.
 * 
 * <p>Upon creation, an AudioTrack object initializes its associated audio buffer.
 * The size of this buffer, specified during the construction, determines how long an AudioTrack
 * can play before running out of data.<br>
 * For an AudioTrack using the static mode, this size is the maximum size of the sound that can
 * be played from it.<br>
 * For the streaming mode, data will be written to the hardware in chunks of
 * sizes inferior to the total buffer size.
 */
public class AudioTrack {

    /** Minimum value for a channel volume */
    private static final float VOLUME_MIN = 0.0f;

    /** Maximum value for a channel volume */
    private static final float VOLUME_MAX = 1.0f;

    /** indicates AudioTrack state is stopped */
    public static final int PLAYSTATE_STOPPED = 1;

    /** indicates AudioTrack state is paused */
    public static final int PLAYSTATE_PAUSED = 2;

    /** indicates AudioTrack state is playing */
    public static final int PLAYSTATE_PLAYING = 3;

    /**
     * Creation mode where audio data is transferred from Java to the native layer
     * only once before the audio starts playing.
     */
    public static final int MODE_STATIC = 0;

    /**
     * Creation mode where audio data is streamed from Java to the native layer
     * as the audio is playing.
     */
    public static final int MODE_STREAM = 1;

    /**
     * State of an AudioTrack that was not successfully initialized upon creation.
     */
    public static final int STATE_UNINITIALIZED = 0;

    /**
     * State of an AudioTrack that is ready to be used.
     */
    public static final int STATE_INITIALIZED = 1;

    /**
     * State of a successfully initialized AudioTrack that uses static data,
     * but that hasn't received that data yet.
     */
    public static final int STATE_NO_STATIC_DATA = 2;

    /**
     * Denotes a successful operation.
     */
    public static final int SUCCESS = 0;

    /**
     * Denotes a generic operation failure.
     */
    public static final int ERROR = -1;

    /**
     * Denotes a failure due to the use of an invalid value.
     */
    public static final int ERROR_BAD_VALUE = -2;

    /**
     * Denotes a failure due to the improper use of a method.
     */
    public static final int ERROR_INVALID_OPERATION = -3;

    private static final int ERROR_NATIVESETUP_AUDIOSYSTEM = -16;

    private static final int ERROR_NATIVESETUP_INVALIDCHANNELMASK = -17;

    private static final int ERROR_NATIVESETUP_INVALIDFORMAT = -18;

    private static final int ERROR_NATIVESETUP_INVALIDSTREAMTYPE = -19;

    private static final int ERROR_NATIVESETUP_NATIVEINITFAILED = -20;

    /**
     * Event id denotes when playback head has reached a previously set marker.
     */
    private static final int NATIVE_EVENT_MARKER = 3;

    /**
     * Event id denotes when previously set update period has elapsed during playback.
     */
    private static final int NATIVE_EVENT_NEW_POS = 4;

    private static final String TAG = "AudioTrack-Java";

    /**
     * Indicates the state of the AudioTrack instance.
     */
    private int mState = STATE_UNINITIALIZED;

    /**
     * Indicates the play state of the AudioTrack instance.
     */
    private int mPlayState = PLAYSTATE_STOPPED;

    /**
     * Lock to make sure mPlayState updates are reflecting the actual state of the object.
     */
    private final Object mPlayStateLock = new Object();

    /**
     * The listener the AudioTrack notifies when the playback position reaches a marker
     * or for periodic updates during the progression of the playback head.
     *  @see #setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener)
     */
    private OnPlaybackPositionUpdateListener mPositionListener = null;

    /**
     * Lock to protect event listener updates against event notifications.
     */
    private final Object mPositionListenerLock = new Object();

    /**
     * Size of the native audio buffer.
     */
    private int mNativeBufferSizeInBytes = 0;

    /**
     * Handler for marker events coming from the native code.
     */
    private NativeEventHandlerDelegate mEventHandlerDelegate = null;

    /**
     * Looper associated with the thread that creates the AudioTrack instance.
     */
    private Looper mInitializationLooper = null;

    /**
     * The audio data sampling rate in Hz.
     */
    private int mSampleRate = 22050;

    /**
     * The number of audio output channels (1 is mono, 2 is stereo).
     */
    private int mChannelCount = 1;

    /**
     * The audio channel mask.
     */
    private int mChannels = AudioFormat.CHANNEL_OUT_MONO;

    /**
     * The type of the audio stream to play. See
     *   {@link AudioManager#STREAM_VOICE_CALL}, {@link AudioManager#STREAM_SYSTEM},
     *   {@link AudioManager#STREAM_RING}, {@link AudioManager#STREAM_MUSIC} and
     *   {@link AudioManager#STREAM_ALARM}
     */
    private int mStreamType = AudioManager.STREAM_MUSIC;

    /**
     * The way audio is consumed by the hardware, streaming or static.
     */
    private int mDataLoadMode = MODE_STREAM;

    /**
     * The current audio channel configuration.
     */
    private int mChannelConfiguration = AudioFormat.CHANNEL_OUT_MONO;

    /**
     * The encoding of the audio samples.
     * @see AudioFormat#ENCODING_PCM_8BIT
     * @see AudioFormat#ENCODING_PCM_16BIT
     */
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Accessed by native methods: provides access to C++ AudioTrack object.
     */
    @SuppressWarnings("unused")
    private int mNativeTrackInJavaObj;

    /**
     * Accessed by native methods: provides access to the JNI data (i.e. resources used by
     * the native AudioTrack object, but not stored in it).
     */
    @SuppressWarnings("unused")
    private int mJniData;

    /**
     * Class constructor.
     * @param streamType the type of the audio stream. See
     *   {@link AudioManager#STREAM_VOICE_CALL}, {@link AudioManager#STREAM_SYSTEM},
     *   {@link AudioManager#STREAM_RING}, {@link AudioManager#STREAM_MUSIC} and
     *   {@link AudioManager#STREAM_ALARM}
     * @param sampleRateInHz the sample rate expressed in Hertz. Examples of rates are (but
     *   not limited to) 44100, 22050 and 11025.
     * @param channelConfig describes the configuration of the audio channels.
     *   See {@link AudioFormat#CHANNEL_OUT_MONO} and
     *   {@link AudioFormat#CHANNEL_OUT_STEREO}
     * @param audioFormat the format in which the audio data is represented.
     *   See {@link AudioFormat#ENCODING_PCM_16BIT} and
     *   {@link AudioFormat#ENCODING_PCM_8BIT}
     * @param bufferSizeInBytes the total size (in bytes) of the buffer where audio data is read
     *   from for playback. If using the AudioTrack in streaming mode, you can write data into
     *   this buffer in smaller chunks than this size. If using the AudioTrack in static mode,
     *   this is the maximum size of the sound that will be played for this instance.
     *   See {@link #getMinBufferSize(int, int, int)} to determine the minimum required buffer size
     *   for the successful creation of an AudioTrack instance in streaming mode. Using values
     *   smaller than getMinBufferSize() will result in an initialization failure.
     * @param mode streaming or static buffer. See {@link #MODE_STATIC} and {@link #MODE_STREAM}
     * @throws java.lang.IllegalArgumentException
     */
    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode) throws IllegalArgumentException {
        mState = STATE_UNINITIALIZED;
        if ((mInitializationLooper = Looper.myLooper()) == null) {
            mInitializationLooper = Looper.getMainLooper();
        }
        audioParamCheck(streamType, sampleRateInHz, channelConfig, audioFormat, mode);
        audioBuffSizeCheck(bufferSizeInBytes);
        int initResult = native_setup(new WeakReference<AudioTrack>(this), mStreamType, mSampleRate, mChannels, mAudioFormat, mNativeBufferSizeInBytes, mDataLoadMode);
        if (initResult != SUCCESS) {
            loge("Error code " + initResult + " when initializing AudioTrack.");
            return;
        }
        if (mDataLoadMode == MODE_STATIC) {
            mState = STATE_NO_STATIC_DATA;
        } else {
            mState = STATE_INITIALIZED;
        }
    }

    private void audioParamCheck(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int mode) {
        if ((streamType != AudioManager.STREAM_ALARM) && (streamType != AudioManager.STREAM_MUSIC) && (streamType != AudioManager.STREAM_RING) && (streamType != AudioManager.STREAM_SYSTEM) && (streamType != AudioManager.STREAM_VOICE_CALL) && (streamType != AudioManager.STREAM_NOTIFICATION) && (streamType != AudioManager.STREAM_BLUETOOTH_SCO) && (streamType != AudioManager.STREAM_DTMF)) {
            throw (new IllegalArgumentException("Invalid stream type."));
        } else {
            mStreamType = streamType;
        }
        if ((sampleRateInHz < 4000) || (sampleRateInHz > 48000)) {
            throw (new IllegalArgumentException(sampleRateInHz + "Hz is not a supported sample rate."));
        } else {
            mSampleRate = sampleRateInHz;
        }
        mChannelConfiguration = channelConfig;
        switch(channelConfig) {
            case AudioFormat.CHANNEL_OUT_DEFAULT:
            case AudioFormat.CHANNEL_OUT_MONO:
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                mChannelCount = 1;
                mChannels = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case AudioFormat.CHANNEL_OUT_STEREO:
            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
                mChannelCount = 2;
                mChannels = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            default:
                mChannelCount = 0;
                mChannels = AudioFormat.CHANNEL_INVALID;
                mChannelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_INVALID;
                throw (new IllegalArgumentException("Unsupported channel configuration."));
        }
        switch(audioFormat) {
            case AudioFormat.ENCODING_DEFAULT:
                mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
            case AudioFormat.ENCODING_PCM_8BIT:
                mAudioFormat = audioFormat;
                break;
            default:
                mAudioFormat = AudioFormat.ENCODING_INVALID;
                throw (new IllegalArgumentException("Unsupported sample encoding." + " Should be ENCODING_PCM_8BIT or ENCODING_PCM_16BIT."));
        }
        if ((mode != MODE_STREAM) && (mode != MODE_STATIC)) {
            throw (new IllegalArgumentException("Invalid mode."));
        } else {
            mDataLoadMode = mode;
        }
    }

    private void audioBuffSizeCheck(int audioBufferSize) {
        int frameSizeInBytes = mChannelCount * (mAudioFormat == AudioFormat.ENCODING_PCM_8BIT ? 1 : 2);
        if ((audioBufferSize % frameSizeInBytes != 0) || (audioBufferSize < 1)) {
            throw (new IllegalArgumentException("Invalid audio buffer size."));
        }
        mNativeBufferSizeInBytes = audioBufferSize;
    }

    /**
     * Releases the native AudioTrack resources.
     */
    public void release() {
        try {
            stop();
        } catch (IllegalStateException ise) {
        }
        native_release();
        mState = STATE_UNINITIALIZED;
    }

    @Override
    protected void finalize() {
        native_finalize();
    }

    /**
     * Returns the minimum valid volume value. Volume values set under this one will
     * be clamped at this value.
     * @return the minimum volume expressed as a linear attenuation.
     */
    public static float getMinVolume() {
        return AudioTrack.VOLUME_MIN;
    }

    /**
     * Returns the maximum valid volume value. Volume values set above this one will
     * be clamped at this value.
     * @return the maximum volume expressed as a linear attenuation.
     */
    public static float getMaxVolume() {
        return AudioTrack.VOLUME_MAX;
    }

    /**
     * Returns the configured audio data sample rate in Hz
     */
    public int getSampleRate() {
        return mSampleRate;
    }

    /**
     * Returns the current playback rate in Hz.
     */
    public int getPlaybackRate() {
        return native_get_playback_rate();
    }

    /**
     * Returns the configured audio data format. See {@link AudioFormat#ENCODING_PCM_16BIT}
     * and {@link AudioFormat#ENCODING_PCM_8BIT}.
     */
    public int getAudioFormat() {
        return mAudioFormat;
    }

    /**
     * Returns the type of audio stream this AudioTrack is configured for.
     * Compare the result against {@link AudioManager#STREAM_VOICE_CALL},
     * {@link AudioManager#STREAM_SYSTEM}, {@link AudioManager#STREAM_RING},
     * {@link AudioManager#STREAM_MUSIC} or {@link AudioManager#STREAM_ALARM}
     */
    public int getStreamType() {
        return mStreamType;
    }

    /**
     * Returns the configured channel configuration.

     * See {@link AudioFormat#CHANNEL_OUT_MONO}
     * and {@link AudioFormat#CHANNEL_OUT_STEREO}.
     */
    public int getChannelConfiguration() {
        return mChannelConfiguration;
    }

    /**
     * Returns the configured number of channels.
     */
    public int getChannelCount() {
        return mChannelCount;
    }

    /**
     * Returns the state of the AudioTrack instance. This is useful after the
     * AudioTrack instance has been created to check if it was initialized
     * properly. This ensures that the appropriate hardware resources have been
     * acquired.
     * @see #STATE_INITIALIZED
     * @see #STATE_NO_STATIC_DATA
     * @see #STATE_UNINITIALIZED
     */
    public int getState() {
        return mState;
    }

    /**
     * Returns the playback state of the AudioTrack instance.
     * @see #PLAYSTATE_STOPPED
     * @see #PLAYSTATE_PAUSED
     * @see #PLAYSTATE_PLAYING
     */
    public int getPlayState() {
        return mPlayState;
    }

    /**
     *  Returns the native frame count used by the hardware.
     */
    protected int getNativeFrameCount() {
        return native_get_native_frame_count();
    }

    /**
     * Returns marker position expressed in frames.
     */
    public int getNotificationMarkerPosition() {
        return native_get_marker_pos();
    }

    /**
     * Returns the notification update period expressed in frames.
     */
    public int getPositionNotificationPeriod() {
        return native_get_pos_update_period();
    }

    /**
     * Returns the playback head position expressed in frames
     */
    public int getPlaybackHeadPosition() {
        return native_get_position();
    }

    /**
     *  Returns the hardware output sample rate
     */
    public static int getNativeOutputSampleRate(int streamType) {
        return native_get_output_sample_rate(streamType);
    }

    /**
     * Returns the minimum buffer size required for the successful creation of an AudioTrack
     * object to be created in the {@link #MODE_STREAM} mode. Note that this size doesn't
     * guarantee a smooth playback under load, and higher values should be chosen according to
     * the expected frequency at which the buffer will be refilled with additional data to play. 
     * @param sampleRateInHz the sample rate expressed in Hertz.
     * @param channelConfig describes the configuration of the audio channels. 
     *   See {@link AudioFormat#CHANNEL_OUT_MONO} and
     *   {@link AudioFormat#CHANNEL_OUT_STEREO}
     * @param audioFormat the format in which the audio data is represented. 
     *   See {@link AudioFormat#ENCODING_PCM_16BIT} and 
     *   {@link AudioFormat#ENCODING_PCM_8BIT}
     * @return {@link #ERROR_BAD_VALUE} if an invalid parameter was passed,
     *   or {@link #ERROR} if the implementation was unable to query the hardware for its output 
     *     properties, 
     *   or the minimum buffer size expressed in bytes.
     */
    public static int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat) {
        int channelCount = 0;
        switch(channelConfig) {
            case AudioFormat.CHANNEL_OUT_MONO:
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                channelCount = 1;
                break;
            case AudioFormat.CHANNEL_OUT_STEREO:
            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
                channelCount = 2;
                break;
            default:
                loge("getMinBufferSize(): Invalid channel configuration.");
                return AudioTrack.ERROR_BAD_VALUE;
        }
        if ((audioFormat != AudioFormat.ENCODING_PCM_16BIT) && (audioFormat != AudioFormat.ENCODING_PCM_8BIT)) {
            loge("getMinBufferSize(): Invalid audio format.");
            return AudioTrack.ERROR_BAD_VALUE;
        }
        if ((sampleRateInHz < 4000) || (sampleRateInHz > 48000)) {
            loge("getMinBufferSize(): " + sampleRateInHz + "Hz is not a supported sample rate.");
            return AudioTrack.ERROR_BAD_VALUE;
        }
        int size = native_get_min_buff_size(sampleRateInHz, channelCount, audioFormat);
        if ((size == -1) || (size == 0)) {
            loge("getMinBufferSize(): error querying hardware");
            return AudioTrack.ERROR;
        } else {
            return size;
        }
    }

    /**
     * Sets the listener the AudioTrack notifies when a previously set marker is reached or
     * for each periodic playback head position update.
     * Notifications will be received in the same thread as the one in which the AudioTrack
     * instance was created.
     * @param listener
     */
    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener) {
        setPlaybackPositionUpdateListener(listener, null);
    }

    /**
     * Sets the listener the AudioTrack notifies when a previously set marker is reached or
     * for each periodic playback head position update.
     * Use this method to receive AudioTrack events in the Handler associated with another
     * thread than the one in which you created the AudioTrack instance.
     * @param listener
     * @param handler the Handler that will receive the event notification messages.
     */
    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener, Handler handler) {
        synchronized (mPositionListenerLock) {
            mPositionListener = listener;
        }
        if (listener != null) {
            mEventHandlerDelegate = new NativeEventHandlerDelegate(this, handler);
        }
    }

    /**
     * Sets the specified left/right output volume values on the AudioTrack. Values are clamped
     * to the ({@link #getMinVolume()}, {@link #getMaxVolume()}) interval if outside this range.
     * @param leftVolume output attenuation for the left channel. A value of 0.0f is silence,
     *      a value of 1.0f is no attenuation.
     * @param rightVolume output attenuation for the right channel
     * @return error code or success, see {@link #SUCCESS},
     *    {@link #ERROR_INVALID_OPERATION}
     */
    public int setStereoVolume(float leftVolume, float rightVolume) {
        if (mState != STATE_INITIALIZED) {
            return ERROR_INVALID_OPERATION;
        }
        if (leftVolume < getMinVolume()) {
            leftVolume = getMinVolume();
        }
        if (leftVolume > getMaxVolume()) {
            leftVolume = getMaxVolume();
        }
        if (rightVolume < getMinVolume()) {
            rightVolume = getMinVolume();
        }
        if (rightVolume > getMaxVolume()) {
            rightVolume = getMaxVolume();
        }
        native_setVolume(leftVolume, rightVolume);
        return SUCCESS;
    }

    /**
     * Sets the playback sample rate for this track. This sets the sampling rate at which
     * the audio data will be consumed and played back, not the original sampling rate of the
     * content. Setting it to half the sample rate of the content will cause the playback to
     * last twice as long, but will also result in a negative pitch shift.
     * The valid sample rate range if from 1Hz to twice the value returned by
     * {@link #getNativeOutputSampleRate(int)}.
     * @param sampleRateInHz the sample rate expressed in Hz
     * @return error code or success, see {@link #SUCCESS}, {@link #ERROR_BAD_VALUE},
     *    {@link #ERROR_INVALID_OPERATION}
     */
    public int setPlaybackRate(int sampleRateInHz) {
        if (mState != STATE_INITIALIZED) {
            return ERROR_INVALID_OPERATION;
        }
        if (sampleRateInHz <= 0) {
            return ERROR_BAD_VALUE;
        }
        return native_set_playback_rate(sampleRateInHz);
    }

    /**
     * Sets the position of the notification marker.
     * @param markerInFrames marker in frames
     * @return error code or success, see {@link #SUCCESS}, {@link #ERROR_BAD_VALUE},
     *  {@link #ERROR_INVALID_OPERATION}
     */
    public int setNotificationMarkerPosition(int markerInFrames) {
        if (mState != STATE_INITIALIZED) {
            return ERROR_INVALID_OPERATION;
        }
        return native_set_marker_pos(markerInFrames);
    }

    /**
     * Sets the period for the periodic notification event.
     * @param periodInFrames update period expressed in frames
     * @return error code or success, see {@link #SUCCESS}, {@link #ERROR_INVALID_OPERATION}
     */
    public int setPositionNotificationPeriod(int periodInFrames) {
        if (mState != STATE_INITIALIZED) {
            return ERROR_INVALID_OPERATION;
        }
        return native_set_pos_update_period(periodInFrames);
    }

    /**
     * Sets the playback head position. The track must be stopped for the position to be changed.
     * @param positionInFrames playback head position expressed in frames
     * @return error code or success, see {@link #SUCCESS}, {@link #ERROR_BAD_VALUE},
     *    {@link #ERROR_INVALID_OPERATION}
     */
    public int setPlaybackHeadPosition(int positionInFrames) {
        synchronized (mPlayStateLock) {
            if ((mPlayState == PLAYSTATE_STOPPED) || (mPlayState == PLAYSTATE_PAUSED)) {
                return native_set_position(positionInFrames);
            } else {
                return ERROR_INVALID_OPERATION;
            }
        }
    }

    /**
     * Sets the loop points and the loop count. The loop can be infinite.
     * @param startInFrames loop start marker expressed in frames
     * @param endInFrames loop end marker expressed in frames
     * @param loopCount the number of times the loop is looped.
     *    A value of -1 means infinite looping.
     * @return error code or success, see {@link #SUCCESS}, {@link #ERROR_BAD_VALUE},
     *    {@link #ERROR_INVALID_OPERATION}
     */
    public int setLoopPoints(int startInFrames, int endInFrames, int loopCount) {
        if (mDataLoadMode == MODE_STREAM) {
            return ERROR_INVALID_OPERATION;
        }
        return native_set_loop(startInFrames, endInFrames, loopCount);
    }

    /**
     * Sets the initialization state of the instance. To be used in an AudioTrack subclass
     * constructor to set a subclass-specific post-initialization state.
     * @param state the state of the AudioTrack instance
     */
    protected void setState(int state) {
        mState = state;
    }

    /**
     * Starts playing an AudioTrack.
     * @throws IllegalStateException
     */
    public void play() throws IllegalStateException {
        if (mState != STATE_INITIALIZED) {
            throw (new IllegalStateException("play() called on uninitialized AudioTrack."));
        }
        synchronized (mPlayStateLock) {
            native_start();
            mPlayState = PLAYSTATE_PLAYING;
        }
    }

    /**
     * Stops playing the audio data.
     * @throws IllegalStateException
     */
    public void stop() throws IllegalStateException {
        if (mState != STATE_INITIALIZED) {
            throw (new IllegalStateException("stop() called on uninitialized AudioTrack."));
        }
        synchronized (mPlayStateLock) {
            native_stop();
            mPlayState = PLAYSTATE_STOPPED;
        }
    }

    /**
     * Pauses the playback of the audio data.
     * @throws IllegalStateException
     */
    public void pause() throws IllegalStateException {
        if (mState != STATE_INITIALIZED) {
            throw (new IllegalStateException("pause() called on uninitialized AudioTrack."));
        }
        synchronized (mPlayStateLock) {
            native_pause();
            mPlayState = PLAYSTATE_PAUSED;
        }
    }

    /**
     * Flushes the audio data currently queued for playback.
     */
    public void flush() {
        if (mState == STATE_INITIALIZED) {
            native_flush();
        }
    }

    /**
     * Writes the audio data to the audio hardware for playback.
     * @param audioData the array that holds the data to play.
     * @param offsetInBytes the offset expressed in bytes in audioData where the data to play 
     *    starts.
     * @param sizeInBytes the number of bytes to read in audioData after the offset.
     * @return the number of bytes that were written or {@link #ERROR_INVALID_OPERATION}
     *    if the object wasn't properly initialized, or {@link #ERROR_BAD_VALUE} if
     *    the parameters don't resolve to valid data and indexes.
     */
    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        if ((mDataLoadMode == MODE_STATIC) && (mState == STATE_NO_STATIC_DATA) && (sizeInBytes > 0)) {
            mState = STATE_INITIALIZED;
        }
        if (mState != STATE_INITIALIZED) {
            return ERROR_INVALID_OPERATION;
        }
        if ((audioData == null) || (offsetInBytes < 0) || (sizeInBytes < 0) || (offsetInBytes + sizeInBytes > audioData.length)) {
            return ERROR_BAD_VALUE;
        }
        return native_write_byte(audioData, offsetInBytes, sizeInBytes, mAudioFormat);
    }

    /**
     * Writes the audio data to the audio hardware for playback.
     * @param audioData the array that holds the data to play.
     * @param offsetInShorts the offset expressed in shorts in audioData where the data to play
     *     starts.
     * @param sizeInShorts the number of bytes to read in audioData after the offset.
     * @return the number of shorts that were written or {@link #ERROR_INVALID_OPERATION}
      *    if the object wasn't properly initialized, or {@link #ERROR_BAD_VALUE} if
      *    the parameters don't resolve to valid data and indexes.
     */
    public int write(short[] audioData, int offsetInShorts, int sizeInShorts) {
        if ((mDataLoadMode == MODE_STATIC) && (mState == STATE_NO_STATIC_DATA) && (sizeInShorts > 0)) {
            mState = STATE_INITIALIZED;
        }
        if (mState != STATE_INITIALIZED) {
            return ERROR_INVALID_OPERATION;
        }
        if ((audioData == null) || (offsetInShorts < 0) || (sizeInShorts < 0) || (offsetInShorts + sizeInShorts > audioData.length)) {
            return ERROR_BAD_VALUE;
        }
        return native_write_short(audioData, offsetInShorts, sizeInShorts, mAudioFormat);
    }

    /**
     * Notifies the native resource to reuse the audio data already loaded in the native
     * layer. This call is only valid with AudioTrack instances that don't use the streaming
     * model.
     * @return error code or success, see {@link #SUCCESS}, {@link #ERROR_BAD_VALUE},
     *  {@link #ERROR_INVALID_OPERATION}
     */
    public int reloadStaticData() {
        if (mDataLoadMode == MODE_STREAM) {
            return ERROR_INVALID_OPERATION;
        }
        return native_reload_static();
    }

    /**
     * Interface definition for a callback to be invoked when the playback head position of
     * an AudioTrack has reached a notification marker or has increased by a certain period.
     */
    public interface OnPlaybackPositionUpdateListener {

        /**
         * Called on the listener to notify it that the previously set marker has been reached
         * by the playback head.
         */
        void onMarkerReached(AudioTrack track);

        /**
         * Called on the listener to periodically notify it that the playback head has reached
         * a multiple of the notification period.
         */
        void onPeriodicNotification(AudioTrack track);
    }

    /**
     * Helper class to handle the forwarding of native events to the appropriate listener
     * (potentially) handled in a different thread
     */
    private class NativeEventHandlerDelegate {

        private final AudioTrack mAudioTrack;

        private final Handler mHandler;

        NativeEventHandlerDelegate(AudioTrack track, Handler handler) {
            mAudioTrack = track;
            Looper looper;
            if (handler != null) {
                looper = handler.getLooper();
            } else {
                looper = mInitializationLooper;
            }
            if (looper != null) {
                mHandler = new Handler(looper) {

                    @Override
                    public void handleMessage(Message msg) {
                        if (mAudioTrack == null) {
                            return;
                        }
                        OnPlaybackPositionUpdateListener listener = null;
                        synchronized (mPositionListenerLock) {
                            listener = mAudioTrack.mPositionListener;
                        }
                        switch(msg.what) {
                            case NATIVE_EVENT_MARKER:
                                if (listener != null) {
                                    listener.onMarkerReached(mAudioTrack);
                                }
                                break;
                            case NATIVE_EVENT_NEW_POS:
                                if (listener != null) {
                                    listener.onPeriodicNotification(mAudioTrack);
                                }
                                break;
                            default:
                                Log.e(TAG, "[ android.media.AudioTrack.NativeEventHandler ] " + "Unknown event type: " + msg.what);
                                break;
                        }
                    }
                };
            } else {
                mHandler = null;
            }
        }

        Handler getHandler() {
            return mHandler;
        }
    }

    @SuppressWarnings("unused")
    private static void postEventFromNative(Object audiotrack_ref, int what, int arg1, int arg2, Object obj) {
        AudioTrack track = (AudioTrack) ((WeakReference) audiotrack_ref).get();
        if (track == null) {
            return;
        }
        if (track.mEventHandlerDelegate != null) {
            Message m = track.mEventHandlerDelegate.getHandler().obtainMessage(what, arg1, arg2, obj);
            track.mEventHandlerDelegate.getHandler().sendMessage(m);
        }
    }

    private final native int native_setup(Object audiotrack_this, int streamType, int sampleRate, int nbChannels, int audioFormat, int buffSizeInBytes, int mode);

    private final native void native_finalize();

    private final native void native_release();

    private final native void native_start();

    private final native void native_stop();

    private final native void native_pause();

    private final native void native_flush();

    private final native int native_write_byte(byte[] audioData, int offsetInBytes, int sizeInBytes, int format);

    private final native int native_write_short(short[] audioData, int offsetInShorts, int sizeInShorts, int format);

    private final native int native_reload_static();

    private final native int native_get_native_frame_count();

    private final native void native_setVolume(float leftVolume, float rightVolume);

    private final native int native_set_playback_rate(int sampleRateInHz);

    private final native int native_get_playback_rate();

    private final native int native_set_marker_pos(int marker);

    private final native int native_get_marker_pos();

    private final native int native_set_pos_update_period(int updatePeriod);

    private final native int native_get_pos_update_period();

    private final native int native_set_position(int position);

    private final native int native_get_position();

    private final native int native_set_loop(int start, int end, int loopCount);

    private static final native int native_get_output_sample_rate(int streamType);

    private static final native int native_get_min_buff_size(int sampleRateInHz, int channelConfig, int audioFormat);

    private static void logd(String msg) {
        Log.d(TAG, "[ android.media.AudioTrack ] " + msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, "[ android.media.AudioTrack ] " + msg);
    }
}
