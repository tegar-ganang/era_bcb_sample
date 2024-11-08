package alesis.fusion.objects;

import alesis.fusion.MuteGroupTypeEnum;
import alesis.fusion.Constant;
import alesis.fusion.Utility;
import alesis.fusion.chunks.Chunk;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

/**
 *
 * @author jam
 */
public class Multisample extends FusionObject {

    /**
     *
     */
    public MultisampleHdr header;

    private Vector<Zone> zones;

    /**
     *
     */
    public class MultisampleHdr extends Header {

        private MultisampleHdr() {
            buffer = ByteBuffer.allocate(length);
            setSignature(Constant.MULTISAMPLE_SIGNATURE);
        }
    }

    /**
     *
     * @param zone
     */
    public void addZone(Zone zone) {
        zones.add(zone);
    }

    /**
     *
     * @param zoneNo
     * @return
     */
    public Zone getZone(int zoneNo) {
        return zones.get(Utility.adjustRange(zoneNo, 0, zones.size() - 1));
    }

    /**
     *
     * @param zoneNo
     */
    public void removeZone(int zoneNo) {
        zones.remove(Utility.adjustRange(zoneNo, 0, zones.size() - 1));
    }

    /**
     *
     * @return
     */
    public int getZonesNumber() {
        return zones.size();
    }

    /**
     *
     * @return
     */
    public ByteBuffer getBytes() {
        return ByteBuffer.allocate(0);
    }

    /**
     *
     * @param fileName
     * @param bankName
     * @param volumeName
     * @param numberOfZones
     */
    public Multisample(String fileName, String bankName, String volumeName, int numberOfZones) {
        setFileName(fileName);
        setBankName(bankName);
        setVolumeName(volumeName);
        header = new MultisampleHdr();
        zones = new Vector();
        zones.ensureCapacity(Constant.MAX_ZONES);
        for (int i = 0; i < Utility.adjustRange(numberOfZones, 0, Constant.MAX_ZONES); i++) {
            zones.add(new Zone());
        }
    }

    /**
     *
     * @param fileName
     * @param bankName
     * @param volumeName
     */
    public Multisample(String fileName, String bankName, String volumeName) {
        this(fileName, bankName, volumeName, 1);
    }

    /**
     *
     * @param pathFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Multisample createFromFile(String pathFile) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(pathFile);
        FileChannel fc = fis.getChannel();
        Multisample multi = new Multisample("", "", "");
        multi.header.readFromFileChannel(fc);
        ByteBuffer bb = ByteBuffer.allocate(0x0c);
        fc.read(bb);
        for (int i = 0; i < bb.get(0x09); i++) {
            multi.zones.add((Zone) new Zone().readFromFileChannel(fc));
        }
        fc.close();
        fis.close();
        return multi;
    }

    /**
     *
     * @param pathFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public Multisample writeToFile(String pathFile) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(pathFile);
        FileChannel fc = fos.getChannel();
        header.writeToFileChannel(fc);
        ByteBuffer bb = ByteBuffer.allocate(0x0c);
        bb.put(0x09, (byte) zones.size());
        bb.rewind();
        fc.write(bb);
        for (int i = 0; i < zones.size(); i++) {
            zones.get(i).writeToFileChannel(fc);
        }
        fc.close();
        fos.close();
        return this;
    }

    /**
     *
     */
    public static class Zone extends Chunk {

        /**
         *
         * @param key
         */
        public void setHighKey(int key) {
            buffer.put(0, (byte) Utility.adjustRange(key, 0, 127));
        }

        /**
         *
         * @return
         */
        public int getHighKey() {
            return buffer.get(0);
        }

        /**
         *
         * @param key
         */
        public void setLowKey(int key) {
            buffer.put(1, (byte) Utility.adjustRange(key, 0, 127));
        }

        /**
         *
         * @return
         */
        public int getLowKey() {
            return buffer.get(1);
        }

        /**
         *
         * @param velocity
         */
        public void setVelocityHigh(int velocity) {
            buffer.put(2, (byte) Utility.adjustRange(velocity, 0, 127));
        }

        /**
         *
         * @return
         */
        public int getVelocityHigh() {
            return buffer.get(2);
        }

        /**
         *
         * @param velocity
         */
        public void setVelocityLow(int velocity) {
            buffer.put(3, (byte) Utility.adjustRange(velocity, 0, 127));
        }

        /**
         *
         * @return
         */
        public int getVelocityLow() {
            return buffer.get(3);
        }

        /**
         *
         * @param group
         */
        public void setMuteGroup(MuteGroupTypeEnum group) {
            buffer.put(5, (byte) group.ordinal());
        }

        /**
         *
         * @param group
         */
        public void setMuteGroup(int group) {
            buffer.put(5, (byte) Utility.adjustRange(group, 0, MuteGroupTypeEnum.values().length - 1));
        }

        /**
         *
         * @return
         */
        public MuteGroupTypeEnum getMuteGroup() {
            return MuteGroupTypeEnum.values()[buffer.get(5)];
        }

        /**
         *
         * @param volumeName
         */
        public void setSampleVolumeName(String volumeName) {
            System.arraycopy(Utility.swapStringEndianness(volumeName).getBytes(), 0, buffer.array(), 0x08, Constant.VOLUME_NAME_LENGTH);
        }

        /**
         *
         * @return
         */
        public String getSampleVolumeName() {
            return new String(buffer.array(), 0x08, Constant.VOLUME_NAME_LENGTH);
        }

        /**
         *
         * @param bankName
         */
        public void setSampleBankName(String bankName) {
            System.arraycopy(Utility.swapStringEndianness(bankName).getBytes(), 0, buffer.array(), 0x08 + Constant.VOLUME_NAME_LENGTH, Constant.BANK_NAME_LENGTH);
        }

        /**
         *
         * @return
         */
        public String getSampleBankName() {
            return new String(buffer.array(), 0x08 + Constant.VOLUME_NAME_LENGTH, Constant.BANK_NAME_LENGTH);
        }

        /**
         *
         * @param tableName
         */
        public void setSampleName(String tableName) {
            System.arraycopy(Utility.swapStringEndianness(tableName).getBytes(), 0, buffer.array(), 0x08 + Constant.VOLUME_NAME_LENGTH + Constant.BANK_NAME_LENGTH, Constant.FILE_NAME_LENGTH);
        }

        /**
         *
         * @return
         */
        public String getSampleName() {
            return new String(buffer.array(), 0x08 + Constant.VOLUME_NAME_LENGTH + Constant.BANK_NAME_LENGTH, Constant.FILE_NAME_LENGTH);
        }

        /**
         *
         * @param cents
         */
        public void setFineTune(int cents) {
            buffer.put(0x48, (byte) cents);
        }

        /**
         *
         * @return
         */
        public int getFineTune() {
            return buffer.get(0x48);
        }

        /**
         *
         * @param note
         */
        public void setRootKey(int note) {
            buffer.put(0x49, (byte) note);
        }

        /**
         *
         * @return
         */
        public int getRootKey() {
            return buffer.get(0x49);
        }

        /**
         *
         * @param enabled
         */
        public void setMute(boolean enabled) {
            buffer.put(0x4b, (byte) (enabled ? 1 : 0));
        }

        /**
         *
         * @param enabled
         */
        public void setMute(int enabled) {
            buffer.put(0x4b, (byte) (enabled == 0 ? 0 : 1));
        }

        /**
         *
         * @return
         */
        public boolean getMute() {
            return buffer.get(0x4b) == 0 ? false : true;
        }

        /**
         *
         * @param volume
         */
        public void setVolume(float volume) {
            buffer.putInt(0x4c, (byte) Utility.swapIntOrder(Float.floatToIntBits(volume / 100)));
        }

        /**
         *
         * @return
         */
        public float getVolume() {
            return Float.intBitsToFloat(Utility.swapIntOrder(buffer.getInt(0x4c))) * 100;
        }

        /**
         *
         * @param pan
         */
        public void setPan(float pan) {
            buffer.putInt(0x50, (byte) Utility.swapIntOrder(Float.floatToIntBits((pan + 100) / 200)));
        }

        /**
         *
         * @return
         */
        public float getPan() {
            return (Float.intBitsToFloat(Utility.swapIntOrder(buffer.getInt(0x50))) * 200) - 100;
        }

        /**
         *
         * @param start
         */
        public void setSampleStart(int start) {
            buffer.putInt(0x54, (byte) Utility.swapIntOrder(start));
        }

        /**
         *
         * @return
         */
        public int getSampleStart() {
            return Utility.swapIntOrder(buffer.getInt(0x54));
        }

        /**
         *
         * @param start
         */
        public void setLoopStart(int start) {
            buffer.putInt(0x58, (byte) Utility.swapIntOrder(start));
        }

        /**
         *
         * @return
         */
        public int getLoopStart() {
            return Utility.swapIntOrder(buffer.getInt(0x58));
        }

        /**
         *
         * @param end
         */
        public void setLoopEnd(int end) {
            buffer.putInt(0x5c, Utility.swapIntOrder(end));
        }

        /**
         *
         * @return
         */
        public int getLoopEnd() {
            return Utility.swapIntOrder(buffer.getInt(0x5c));
        }

        /**
         *
         * @param cents
         */
        public void setLoopFineTune(int cents) {
            buffer.put(0x61, (byte) cents);
        }

        /**
         *
         * @return
         */
        public int getLoopFineTune() {
            return buffer.get(0x61);
        }

        /**
         *
         */
        public Zone() {
            buffer = ByteBuffer.allocate(Constant.ZONE_LENGTH);
        }
    }
}
