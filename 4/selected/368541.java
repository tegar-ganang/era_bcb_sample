package com.googlecode.legendtv.intf.vlc;

/**
 * VLC audio interface implementation class.
 * 
 * @author Filippo Carone <filippo@carone.org>
 * @author Guy Paddock <guy.paddock@gmail.com>
 */
public class Audio extends VLCInterface {

    /**
	 * Enumeration for the different types of audio channels.
	 * 
	 * @author Guy Paddock
	 */
    public static enum AudioChannel {

        /**
		 * Constant for stereo channel audio 
		 */
        STEREO, /**
		 * Constant for reverse channel audio 
		 */
        REVERSE, /**
		 * Constant for left channel audio.
		 */
        LEFT, /**
		 * Constant for right channel audio.
		 */
        RIGHT, /**
		 * Constant for dolby channel audio 
		 */
        DOLBY;

        /**
		 * Method for obtaining the value of an enumerated value.
		 * 
		 * @return The integer value associated with this enumerated value.
		 */
        public int valueOf() {
            return (this.ordinal() + 1);
        }

        /**
		 * This method returns the enum element that has the specified value.
		 * 
		 * @param value	The value that the desired element is associated with.
		 * @return		The enum element that has the specified value, or null
		 * 				if no such element exists.
		 */
        public static AudioChannel elementForValue(int value) {
            AudioChannel retVal = null;
            AudioChannel[] values = AudioChannel.values();
            if ((value > 0) && (value <= values.length)) retVal = values[value - 1];
            return retVal;
        }
    }

    /**
	 * Constructor for an Audio interface instance.
	 * 
	 * @param	instance	VLC instance that this Audio instance pertains to.
	 */
    Audio(VLCInstance instance) {
        super(instance);
    }

    /**
	 * Accessor for the active audio track.
	 * 
	 * @return					The number of the active audio track.
	 * @throws	VLCException	If an error occurs.
	 */
    public native int getTrackNumber() throws VLCException;

    /**
	 * Mutator for the active audio track.
	 * 
	 * @param	track			The number of the track that should become activated.
	 * @throws	VLCException	If an error occurs.
	 */
    public native void setTrackNumber(int track) throws VLCException;

    /**
	 * Accessor for the active audio channel number.
	 * 
	 * @return					The number of the active audio channel.
	 * @throws	VLCException	If an error occurs.
	 */
    private native int getChannelNumber() throws VLCException;

    /**
	 * Accessor for the active audio channel.
	 * 
	 * @return					The active audio channel, as an element of AudioChannel.
	 * @throws	VLCException	If an error occurs.
	 */
    public AudioChannel getChannel() throws VLCException {
        return (AudioChannel.elementForValue(this.getChannelNumber()));
    }

    /**
	 * Mutator for the active audio channel.
	 * 
	 * @param	channel			The number of the audio channel that should become
	 * 							active.
	 * @throws	VLCException	If an error occurs.
	 */
    private native void setChannelNumber(int channel) throws VLCException;

    /**
	 * Mutator for the active audio channel.
	 * 
	 * @param	channel			The audio channel that should become active.
	 * @throws	VLCException	If an error occurs.
	 */
    public void setChannel(AudioChannel channel) throws VLCException {
        this.setChannelNumber(channel.valueOf());
    }

    /**
     * Accessor for current mute state.
     * 
     * @return					True if output is currently muted; false otherwise.
     * @throws	VLCException	If an error occurs.
     */
    public native boolean isMuted() throws VLCException;

    /**
     * Mutator for mute state of the audio output.
     * 
     * @param	muted			True if audio output should be muted; false otherwise.
     * @throws	VLCException	If an error occurs.
     */
    public native void setMuted(boolean muted) throws VLCException;

    /**
     * Toggles audio output between muted and unmuted state.
     * 
     * @throws	VLCException	If an error occurs.
     */
    public native void toggleMute() throws VLCException;

    /**
     * Accessor for the current volume level.
     * 
     * @return					The current volume level.
     * @throws	VLCException	If an error occurs.
     */
    public native int getVolume() throws VLCException;

    /**
     * Mutator for the current volume level.
     * 
     * @param	volume			The new volume level (0-200) to set.
     * @throws	VLCException	If an error occurs.
     */
    public native void setVolume(int volume) throws VLCException;
}
