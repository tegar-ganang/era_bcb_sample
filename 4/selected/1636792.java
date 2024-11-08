package javax.media.ding3d.audioengines;

import javax.media.ding3d.*;

/**
 * The AudioEngine Class defines an audio output device that generates
 * sound 'image' from scene graph.
 * An AudioEngine object encapsulates the AudioDevice's basic information.
 *
 * <p>
 * NOTE: AudioEngine developers should not subclass this class directly.
 * Subclass AudioEngine3DL2 instead.
 */
public abstract class AudioEngine implements AudioDevice {

    int fileDescriptor;

    int audioPlaybackType = HEADPHONES;

    float distanceToSpeaker = 0.0f;

    float angleOffsetToSpeaker = 0.0f;

    int channelsAvailable = 8;

    int totalChannels = 8;

    /**
     * Construct a new AudioEngine with the specified P.E.
     * @param physicalEnvironment the physical environment object where we
     * want access to this device.
     */
    public AudioEngine(PhysicalEnvironment physicalEnvironment) {
        physicalEnvironment.setAudioDevice(this);
    }

    /**
     * Code to initialize the device
     * @return flag: true is initialized sucessfully, false if error
     */
    public abstract boolean initialize();

    /**
     * Code to close the device
     * @return flag: true is closed sucessfully, false if error
     */
    public abstract boolean close();

    /**
     * Set Type of Audio Playback physical transducer(s) sound is output to.
     *     Valid types are HEADPHONE, MONO_SPEAKER, STEREO_SPEAKERS
     * @param type of audio output device
     */
    public void setAudioPlaybackType(int type) {
        audioPlaybackType = type;
    }

    /**
     * Get Type of Audio Playback Output Device
     * returns audio playback type to which sound is currently output
     */
    public int getAudioPlaybackType() {
        return audioPlaybackType;
    }

    /**
     * Set Distance from the Center Ear to a Speaker
     * @param distance from the center ear and to the speaker
     */
    public void setCenterEarToSpeaker(float distance) {
        distanceToSpeaker = distance;
    }

    /**
     * Get Distance from Ear to Speaker
     * returns value set as distance from listener's ear to speaker
     */
    public float getCenterEarToSpeaker() {
        return distanceToSpeaker;
    }

    /**
     * Set Angle Offset To Speaker
     * @param angle in radian between head coordinate Z axis and vector to speaker   */
    public void setAngleOffsetToSpeaker(float angle) {
        angleOffsetToSpeaker = angle;
    }

    /**
     * Get Angle Offset To Speaker
     * returns value set as angle between vector to speaker and Z head axis
     */
    public float getAngleOffsetToSpeaker() {
        return angleOffsetToSpeaker;
    }

    /**
     * Query total number of channels available for sound rendering
     * for this audio device.
     * returns number of maximum sound channels you can run with this
     * library/device-driver.
     */
    public int getTotalChannels() {
        return (totalChannels);
    }

    /**
     * Query number of channels currently available for use by the
     * returns number of sound channels currently available (number
     * not being used by active sounds.
     */
    public int getChannelsAvailable() {
        return (channelsAvailable);
    }

    /**
     * Query number of channels that would be used to render a particular
     * sound node.
     * @param sound refenence to sound node that query to be performed on
     * returns number of sound channels used by a specific Sound node
     * @deprecated This method is now part of the Sound class
     */
    public int getChannelsUsedForSound(Sound sound) {
        if (sound != null) return sound.getNumberOfChannelsUsed(); else return -1;
    }
}
