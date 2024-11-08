package it.hakvoort.bdf;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * The <code>BDFHeader</code> class represents a BioSemi Data Format Header.
 * 
 * @author Gido Hakvoort (gido@hakvoort.it)
 * 
 */
public class BDFHeader {

    public byte[] version = new byte[8];

    public byte[] patient = new byte[80];

    public byte[] recording = new byte[80];

    public byte[] startdate = new byte[8];

    public byte[] starttime = new byte[8];

    public byte[] length = new byte[8];

    public byte[] reserved = new byte[44];

    public byte[] numRecords = new byte[8];

    public byte[] duration = new byte[8];

    public byte[] numChannels = new byte[4];

    private List<BDFChannel> channels = new ArrayList<BDFChannel>();

    public BDFHeader() {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy-HH.mm.ss");
        String[] datetime = format.format(Calendar.getInstance().getTime()).split("-");
        setVersion("0");
        setPatient("");
        setRecording("");
        setStartDate(datetime[0]);
        setStartTime(datetime[1]);
        setLength("256");
        setReserved("");
        setNumRecords("-1");
        setDuration("");
        setNumChannels("0");
    }

    /**
	 * Sets the main header part of this object.
	 * 
	 * @param 	main
	 * 			A byte array containing the main header data.
	 * 
	 */
    public void loadMainHeader(byte[] main) throws BDFException {
        if (main.length != 256) {
            throw new BDFException(String.format("Invalid BDF Main Header: %s", main));
        }
        ByteBuffer buffer = ByteBuffer.wrap(main);
        buffer.get(version);
        buffer.get(patient);
        buffer.get(recording);
        buffer.get(startdate);
        buffer.get(starttime);
        buffer.get(length);
        buffer.get(reserved);
        buffer.get(numRecords);
        buffer.get(duration);
        buffer.get(numChannels);
    }

    /**
	 * Sets the channel header part of this object.
	 * 
	 * @param 	channel
	 * 			A byte array containing the header data of the channels.
	 */
    public void loadChannelHeader(byte[] channel) throws BDFException {
        int numChannels = 0;
        try {
            numChannels = Integer.parseInt(getNumChannels());
        } catch (NumberFormatException e) {
            throw new BDFException("Invalid number of channels in main header");
        }
        if (channel.length != 256 * numChannels) {
            throw new BDFException(String.format("Invalid BDF Channel Header: %s", channel));
        }
        ByteBuffer buffer = ByteBuffer.wrap(channel);
        for (int c = 0, offset = numChannels - 1; c < numChannels; c++, offset--) {
            BDFChannel bdfChannel = new BDFChannel();
            buffer.position(c * 16);
            buffer.get(bdfChannel.label);
            buffer.position(buffer.position() + offset * 16 + c * 80);
            buffer.get(bdfChannel.transducerType);
            buffer.position(buffer.position() + offset * 80 + c * 8);
            buffer.get(bdfChannel.physicalDimension);
            buffer.position(buffer.position() + offset * 8 + c * 8);
            buffer.get(bdfChannel.physicalMinimum);
            buffer.position(buffer.position() + offset * 8 + c * 8);
            buffer.get(bdfChannel.physicalMaximum);
            buffer.position(buffer.position() + offset * 8 + c * 8);
            buffer.get(bdfChannel.digitalMinimum);
            buffer.position(buffer.position() + offset * 8 + c * 8);
            buffer.get(bdfChannel.digitalMaximum);
            buffer.position(buffer.position() + offset * 8 + c * 80);
            buffer.get(bdfChannel.prefiltering);
            buffer.position(buffer.position() + offset * 80 + c * 8);
            buffer.get(bdfChannel.numSamples);
            buffer.position(buffer.position() + offset * 8 + c * 32);
            buffer.get(bdfChannel.reserved);
            this.channels.add(bdfChannel);
            buffer.rewind();
        }
    }

    /**
     * Returns a string summarizing this object.
     *
     * @return  A summary string
     */
    public String toString() {
        return new String(getBytes());
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(computeLength());
        buffer.put(version);
        buffer.put(patient);
        buffer.put(recording);
        buffer.put(startdate);
        buffer.put(starttime);
        buffer.put(length);
        buffer.put(reserved);
        buffer.put(numRecords);
        buffer.put(duration);
        buffer.put(numChannels);
        for (int c = 0, offset = channels.size() - 1; c < channels.size(); c++, offset--) {
            BDFChannel channel = channels.get(c);
            buffer.position(256 + c * 16);
            buffer.put(channel.label);
            buffer.position(buffer.position() + offset * 16 + c * 80);
            buffer.put(channel.transducerType);
            buffer.position(buffer.position() + offset * 80 + c * 8);
            buffer.put(channel.physicalDimension);
            buffer.position(buffer.position() + offset * 8 + c * 8);
            buffer.put(channel.physicalMinimum);
            buffer.position(buffer.position() + offset * 8 + c * 8);
            buffer.put(channel.physicalMaximum);
            buffer.position(buffer.position() + offset * 8 + c * 8);
            buffer.put(channel.digitalMinimum);
            buffer.position(buffer.position() + offset * 8 + c * 8);
            buffer.put(channel.digitalMaximum);
            buffer.position(buffer.position() + offset * 8 + c * 80);
            buffer.put(channel.prefiltering);
            buffer.position(buffer.position() + offset * 80 + c * 8);
            buffer.put(channel.numSamples);
            buffer.position(buffer.position() + offset * 8 + c * 32);
            buffer.put(channel.reserved);
            buffer.rewind();
        }
        return buffer.array();
    }

    /**
	 * Returns the version of the object.
	 * 
	 * @return	the version.
	 */
    public String getVersion() {
        return new String(version).trim();
    }

    /**
	 * Sets the version header part of the object.
	 * 
	 * @param 	version
	 * 			A String containing the version data of the object.
	 * 			version is truncating or padding with whitespace so it has the same length as the old version.
	 */
    public void setVersion(String version) {
        Arrays.fill(this.version, (byte) ' ');
        char[] chars = version.toCharArray();
        this.version[0] = (byte) chars[0];
        byte[] bytes = new String(chars, 1, chars.length - 1).getBytes();
        System.arraycopy(bytes, 0, this.version, 1, Math.min(bytes.length, this.version.length));
    }

    /**
	 * Returns the patient information of this object.
	 * 
	 * @return	the patient information.
	 */
    public String getPatient() {
        return new String(patient).trim();
    }

    /**
	 * Sets the patient header part of the object.
	 * 
	 * @param 	patient
	 * 			A String containing the patient data of the object.
	 * 			patient is truncating or padding with whitespace so it has the same length as the old patient.
	 */
    public void setPatient(String patient) {
        Arrays.fill(this.patient, (byte) ' ');
        byte[] bytes = patient.getBytes();
        System.arraycopy(bytes, 0, this.patient, 0, Math.min(bytes.length, this.patient.length));
    }

    /**
	 * Returns the recording information of this object.
	 * 
	 * @return	the recording information.
	 */
    public String getRecording() {
        return new String(recording).trim();
    }

    /**
	 * Sets the recording header part of the object.
	 * 
	 * @param 	recording
	 * 			A String containing the recording data of the object.
	 * 			recording is truncating or padding with whitespace so it has the same length as the old recording.
	 */
    public void setRecording(String recording) {
        Arrays.fill(this.recording, (byte) ' ');
        byte[] bytes = recording.getBytes();
        System.arraycopy(bytes, 0, this.recording, 0, Math.min(bytes.length, this.recording.length));
    }

    /**
	 * Returns the start date of this object.
	 * 
	 * @return	the start date.
	 */
    public String getStartDate() {
        return new String(startdate).trim();
    }

    /**
	 * Sets the startdate header part of the object.
	 * 
	 * @param 	startdate
	 * 			A String containing the startdate of the object.
	 */
    public void setStartDate(String startdate) {
        Arrays.fill(this.startdate, (byte) ' ');
        byte[] bytes = startdate.getBytes();
        System.arraycopy(bytes, 0, this.startdate, 0, Math.min(bytes.length, this.startdate.length));
    }

    /**
	 * Returns the start time of this object.
	 * 
	 * @return	the start time.
	 */
    public String getStartTime() {
        return new String(starttime).trim();
    }

    /**
	 * Sets the starttime header part of the object.
	 * 
	 * @param 	starttime
	 * 			A String containing the starttime of the object.
	 */
    public void setStartTime(String starttime) {
        Arrays.fill(this.starttime, (byte) ' ');
        byte[] bytes = starttime.getBytes();
        System.arraycopy(bytes, 0, this.starttime, 0, Math.min(bytes.length, this.starttime.length));
    }

    /**
	 * Returns the calculated length of this object. {(N + 1) * 256} bytes, where N is the number of channels in this object.
	 * 
	 * @return	the calculated lenght of this object.
	 */
    public int computeLength() {
        return (channels.size() + 1) * 256;
    }

    /**
	 * Returns the length of this object.
	 * 
	 * @return	the lenght of this object.
	 */
    public String getLength() {
        return new String(length).trim();
    }

    /**
	 * Sets the length header part of the object.
	 * 
	 * @param 	length
	 * 			A String containing the length data of the object.
	 * 			length is truncating or padding with whitespace so it has the same length as the old length.
	 */
    public void setLength(String length) {
        Arrays.fill(this.length, (byte) ' ');
        byte[] bytes = length.getBytes();
        System.arraycopy(bytes, 0, this.length, 0, Math.min(bytes.length, this.length.length));
    }

    /**
	 * Returns the reserved header part of this object, which can be used to store aditional information.
	 * 
	 * @return	the reserved part of this object.
	 */
    public String getReserved() {
        return new String(reserved).trim();
    }

    /**
	 * Sets the reserved header part of the object, which can be used to store aditional information.
	 * 
	 * @param 	reserved
	 * 			A String containing the reserved data of the object.
	 * 			reserved is truncating or padding with whitespace so it has the same length as the old reserved.
	 */
    public void setReserved(String reserved) {
        Arrays.fill(this.reserved, (byte) ' ');
        byte[] bytes = reserved.getBytes();
        System.arraycopy(bytes, 0, this.reserved, 0, Math.min(bytes.length, this.reserved.length));
    }

    /**
	 * Returns the number of records in the object.
	 * 
	 * @return	the number of records, or -1 if unknown.
	 */
    public String getNumRecords() {
        return new String(numRecords).trim();
    }

    /**
	 * Sets the number of records header part of the object.
	 * 
	 * @param 	numRecords
	 * 			A String containing the number of records of the object.
	 */
    public void setNumRecords(String numRecords) {
        Arrays.fill(this.numRecords, (byte) ' ');
        byte[] bytes = numRecords.getBytes();
        System.arraycopy(bytes, 0, this.numRecords, 0, Math.min(bytes.length, this.numRecords.length));
    }

    /**
	 * Returns the duration of this object in seconds.
	 * 
	 * @return	the duration of this object.
	 */
    public String getDuration() {
        return new String(duration).trim();
    }

    /**
	 * Sets the duration header part of the object in seconds.
	 * 
	 * @param 	duration
	 * 			A String containing the duration of the object in seconds.
	 */
    public void setDuration(String duration) {
        Arrays.fill(this.duration, (byte) ' ');
        byte[] bytes = duration.getBytes();
        System.arraycopy(bytes, 0, this.duration, 0, Math.min(bytes.length, this.duration.length));
    }

    /**
	 * Returns the calculated number of channels in this object.
	 * 
	 * @return	the calculated number of channels.
	 */
    public int computeNumChannels() {
        return channels.size();
    }

    /**
	 * Returns the number of channels in this object.
	 * 
	 * @return	the number of channels in this object.
	 */
    public String getNumChannels() {
        return new String(numChannels).trim();
    }

    /**
	 * Sets the number of channels header part of the object.
	 * 
	 * @param 	numChannels
	 * 			A String containing the number of channels of the object.
	 */
    public void setNumChannels(String numChannels) {
        Arrays.fill(this.numChannels, (byte) ' ');
        byte[] bytes = numChannels.getBytes();
        System.arraycopy(bytes, 0, this.numChannels, 0, Math.min(bytes.length, this.numChannels.length));
    }

    /**
	 * Returns the requested channel in this object.
	 * 
	 * @param 	index
	 * 			index of the requested channel
	 * @return	the requested channel
	 */
    public BDFChannel getChannel(int index) {
        if (index < 0 || index >= channels.size()) {
            return null;
        }
        return channels.get(index);
    }

    /**
	 * Add a new channel header part to this object.
	 * 
	 * @param 	channel
	 * 			A BDFChannel containing the channel data of the object.
	 * 			Number of channels and the length will be reset to new values
	 */
    public void addChannel(BDFChannel channel) {
        channels.add(channel);
        setNumChannels(Integer.toString(computeNumChannels()));
        setLength(Integer.toString(computeLength()));
    }
}
