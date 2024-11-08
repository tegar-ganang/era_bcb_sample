package com.intel.bluetooth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import com.intel.bluetooth.BluetoothConsts.DeviceClassConsts;

class ServiceRecordImpl implements ServiceRecord {

    private BluetoothStack bluetoothStack;

    private RemoteDevice device;

    private long handle;

    Hashtable attributes;

    protected boolean attributeUpdated;

    int deviceServiceClasses;

    int deviceServiceClassesRegistered;

    ServiceRecordImpl(BluetoothStack bluetoothStack, RemoteDevice device, long handle) {
        this.bluetoothStack = bluetoothStack;
        this.device = device;
        this.handle = handle;
        this.deviceServiceClassesRegistered = 0;
        this.attributes = new Hashtable();
    }

    byte[] toByteArray() throws IOException {
        DataElement element = new DataElement(DataElement.DATSEQ);
        final boolean sort = true;
        if (sort) {
            int[] sortIDs = new int[attributes.size()];
            int k = 0;
            for (Enumeration e = attributes.keys(); e.hasMoreElements(); ) {
                Integer key = (Integer) e.nextElement();
                sortIDs[k] = key.intValue();
                k++;
            }
            for (int i = 0; i < sortIDs.length; i++) {
                for (int j = 0; j < sortIDs.length - i - 1; j++) {
                    if (sortIDs[j] > sortIDs[j + 1]) {
                        int temp = sortIDs[j];
                        sortIDs[j] = sortIDs[j + 1];
                        sortIDs[j + 1] = temp;
                    }
                }
            }
            for (int i = 0; i < sortIDs.length; i++) {
                element.addElement(new DataElement(DataElement.U_INT_2, sortIDs[i]));
                element.addElement(getAttributeValue(sortIDs[i]));
            }
        } else {
            for (Enumeration e = attributes.keys(); e.hasMoreElements(); ) {
                Integer key = (Integer) e.nextElement();
                element.addElement(new DataElement(DataElement.U_INT_2, key.intValue()));
                element.addElement((DataElement) attributes.get(key));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        (new SDPOutputStream(out)).writeElement(element);
        return out.toByteArray();
    }

    void loadByteArray(byte data[]) throws IOException {
        DataElement element = (new SDPInputStream(new ByteArrayInputStream(data))).readElement();
        if (element.getDataType() != DataElement.DATSEQ) {
            throw new IOException("DATSEQ expected instead of " + element.getDataType());
        }
        Enumeration en = (Enumeration) element.getValue();
        while (en.hasMoreElements()) {
            DataElement id = (DataElement) en.nextElement();
            if (id.getDataType() != DataElement.U_INT_2) {
                throw new IOException("U_INT_2 expected instead of " + id.getDataType());
            }
            DataElement value = (DataElement) en.nextElement();
            this.populateAttributeValue((int) id.getLong(), value);
        }
    }

    public DataElement getAttributeValue(int attrID) {
        if (attrID < 0x0000 || attrID > 0xffff) {
            throw new IllegalArgumentException();
        }
        return (DataElement) attributes.get(new Integer(attrID));
    }

    public RemoteDevice getHostDevice() {
        return device;
    }

    public int[] getAttributeIDs() {
        int[] attrIDs = new int[attributes.size()];
        int i = 0;
        for (Enumeration e = attributes.keys(); e.hasMoreElements(); ) {
            attrIDs[i++] = ((Integer) e.nextElement()).intValue();
        }
        return attrIDs;
    }

    public boolean populateRecord(int[] attrIDs) throws IOException {
        if (device == null) {
            throw new RuntimeException("This is local device service record");
        }
        if (attrIDs == null) {
            throw new NullPointerException("attrIDs is null");
        }
        if (attrIDs.length == 0) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < attrIDs.length; i++) {
            if (attrIDs[i] < 0x0000 || attrIDs[i] > 0xffff) {
                throw new IllegalArgumentException();
            }
        }
        int[] sortIDs = new int[attrIDs.length];
        System.arraycopy(attrIDs, 0, sortIDs, 0, attrIDs.length);
        for (int i = 0; i < sortIDs.length; i++) {
            for (int j = 0; j < sortIDs.length - i - 1; j++) {
                if (sortIDs[j] > sortIDs[j + 1]) {
                    int temp = sortIDs[j];
                    sortIDs[j] = sortIDs[j + 1];
                    sortIDs[j + 1] = temp;
                }
            }
        }
        for (int i = 0; i < sortIDs.length - 1; i++) {
            if (sortIDs[i] == sortIDs[i + 1]) {
                throw new IllegalArgumentException();
            }
            DebugLog.debug0x("query for ", sortIDs[i]);
        }
        DebugLog.debug0x("query for ", sortIDs[sortIDs.length - 1]);
        return this.bluetoothStack.populateServicesRecordAttributeValues(this, sortIDs);
    }

    public String getConnectionURL(int requiredSecurity, boolean mustBeMaster) {
        int commChannel = -1;
        DataElement protocolDescriptor = getAttributeValue(BluetoothConsts.ProtocolDescriptorList);
        if ((protocolDescriptor == null) || (protocolDescriptor.getDataType() != DataElement.DATSEQ)) {
            return null;
        }
        boolean isL2CAP = false;
        boolean isRFCOMM = false;
        boolean isOBEX = false;
        for (Enumeration protocolsSeqEnum = (Enumeration) protocolDescriptor.getValue(); protocolsSeqEnum.hasMoreElements(); ) {
            DataElement elementSeq = (DataElement) protocolsSeqEnum.nextElement();
            if (elementSeq.getDataType() == DataElement.DATSEQ) {
                Enumeration elementSeqEnum = (Enumeration) elementSeq.getValue();
                if (elementSeqEnum.hasMoreElements()) {
                    DataElement protocolElement = (DataElement) elementSeqEnum.nextElement();
                    if (protocolElement.getDataType() != DataElement.UUID) {
                        continue;
                    }
                    Object uuid = protocolElement.getValue();
                    if (BluetoothConsts.OBEX_PROTOCOL_UUID.equals(uuid)) {
                        isOBEX = true;
                        isRFCOMM = false;
                        isL2CAP = false;
                    } else if (elementSeqEnum.hasMoreElements() && (BluetoothConsts.RFCOMM_PROTOCOL_UUID.equals(uuid))) {
                        DataElement protocolPSMElement = (DataElement) elementSeqEnum.nextElement();
                        switch(protocolPSMElement.getDataType()) {
                            case DataElement.U_INT_1:
                            case DataElement.U_INT_2:
                            case DataElement.U_INT_4:
                            case DataElement.INT_1:
                            case DataElement.INT_2:
                            case DataElement.INT_4:
                            case DataElement.INT_8:
                                long val = protocolPSMElement.getLong();
                                if ((val >= BluetoothConsts.RFCOMM_CHANNEL_MIN) && (val <= BluetoothConsts.RFCOMM_CHANNEL_MAX)) {
                                    commChannel = (int) val;
                                    isRFCOMM = true;
                                    isL2CAP = false;
                                }
                                break;
                        }
                    } else if (elementSeqEnum.hasMoreElements() && (BluetoothConsts.L2CAP_PROTOCOL_UUID.equals(uuid))) {
                        DataElement protocolPSMElement = (DataElement) elementSeqEnum.nextElement();
                        switch(protocolPSMElement.getDataType()) {
                            case DataElement.U_INT_1:
                            case DataElement.U_INT_2:
                            case DataElement.U_INT_4:
                            case DataElement.INT_1:
                            case DataElement.INT_2:
                            case DataElement.INT_4:
                            case DataElement.INT_8:
                                long pcm = protocolPSMElement.getLong();
                                if ((pcm >= BluetoothConsts.L2CAP_PSM_MIN) && (pcm <= BluetoothConsts.L2CAP_PSM_MAX)) {
                                    commChannel = (int) pcm;
                                    isL2CAP = true;
                                }
                                break;
                        }
                    }
                }
            }
        }
        if (commChannel == -1) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        if (isOBEX) {
            buf.append(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX);
        } else if (isRFCOMM) {
            buf.append(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM);
        } else if (isL2CAP) {
            buf.append(BluetoothConsts.PROTOCOL_SCHEME_L2CAP);
        } else {
            return null;
        }
        buf.append("://");
        if (device == null) {
            try {
                Object saveID = BlueCoveImpl.getCurrentThreadBluetoothStackID();
                try {
                    BlueCoveImpl.setThreadBluetoothStack(bluetoothStack);
                    buf.append(LocalDevice.getLocalDevice().getBluetoothAddress());
                } finally {
                    if (saveID != null) {
                        BlueCoveImpl.setThreadBluetoothStackID(saveID);
                    }
                }
            } catch (BluetoothStateException bse) {
                DebugLog.error("can't read LocalAddress", bse);
                buf.append("localhost");
            }
        } else {
            buf.append(getHostDevice().getBluetoothAddress());
        }
        buf.append(":");
        if (isL2CAP) {
            String hex = Integer.toHexString(commChannel);
            for (int i = hex.length(); i < 4; i++) {
                buf.append('0');
            }
            buf.append(hex);
        } else {
            buf.append(commChannel);
        }
        switch(requiredSecurity) {
            case NOAUTHENTICATE_NOENCRYPT:
                buf.append(";authenticate=false;encrypt=false");
                break;
            case AUTHENTICATE_NOENCRYPT:
                buf.append(";authenticate=true;encrypt=false");
                break;
            case AUTHENTICATE_ENCRYPT:
                buf.append(";authenticate=true;encrypt=true");
                break;
            default:
                throw new IllegalArgumentException();
        }
        if (mustBeMaster) {
            buf.append(";master=true");
        } else {
            buf.append(";master=false");
        }
        return buf.toString();
    }

    int getChannel(UUID protocolUUID) {
        int channel = -1;
        DataElement protocolDescriptor = getAttributeValue(BluetoothConsts.ProtocolDescriptorList);
        if ((protocolDescriptor == null) || (protocolDescriptor.getDataType() != DataElement.DATSEQ)) {
            return -1;
        }
        for (Enumeration protocolsSeqEnum = (Enumeration) protocolDescriptor.getValue(); protocolsSeqEnum.hasMoreElements(); ) {
            DataElement elementSeq = (DataElement) protocolsSeqEnum.nextElement();
            if (elementSeq.getDataType() == DataElement.DATSEQ) {
                Enumeration elementSeqEnum = (Enumeration) elementSeq.getValue();
                if (elementSeqEnum.hasMoreElements()) {
                    DataElement protocolElement = (DataElement) elementSeqEnum.nextElement();
                    if (protocolElement.getDataType() != DataElement.UUID) {
                        continue;
                    }
                    Object uuid = protocolElement.getValue();
                    if (elementSeqEnum.hasMoreElements() && (protocolUUID.equals(uuid))) {
                        DataElement protocolPSMElement = (DataElement) elementSeqEnum.nextElement();
                        switch(protocolPSMElement.getDataType()) {
                            case DataElement.U_INT_1:
                            case DataElement.U_INT_2:
                            case DataElement.U_INT_4:
                            case DataElement.INT_1:
                            case DataElement.INT_2:
                            case DataElement.INT_4:
                            case DataElement.INT_8:
                                channel = (int) protocolPSMElement.getLong();
                                break;
                        }
                    }
                }
            }
        }
        return channel;
    }

    public void setDeviceServiceClasses(int classes) {
        if (device != null) {
            throw new RuntimeException("Service record obtained from a remote device");
        }
        if ((classes & (0xff000000 | DeviceClassConsts.LIMITED_DISCOVERY_SERVICE | DeviceClassConsts.FORMAT_VERSION_MASK)) != 0) {
            throw new IllegalArgumentException();
        }
        if ((classes & (DeviceClassConsts.MAJOR_MASK | DeviceClassConsts.MINOR_MASK)) != 0) {
            throw new IllegalArgumentException();
        }
        if ((bluetoothStack.getFeatureSet() & BluetoothStack.FEATURE_SET_DEVICE_SERVICE_CLASSES) == 0) {
            throw new NotSupportedRuntimeException(bluetoothStack.getStackID());
        }
        this.deviceServiceClasses = classes;
    }

    public boolean setAttributeValue(int attrID, DataElement attrValue) {
        if (device != null) {
            throw new IllegalArgumentException();
        }
        if (attrID < 0x0000 || attrID > 0xffff) {
            throw new IllegalArgumentException();
        }
        if (attrID == BluetoothConsts.ServiceRecordHandle) {
            throw new IllegalArgumentException();
        }
        attributeUpdated = true;
        if (attrValue == null) {
            return (attributes.remove(new Integer(attrID)) != null);
        } else {
            attributes.put(new Integer(attrID), attrValue);
            return true;
        }
    }

    /**
	 * Internal implementation function
	 */
    void populateAttributeValue(int attrID, DataElement attrValue) {
        if (attrID < 0x0000 || attrID > 0xffff) {
            throw new IllegalArgumentException();
        }
        if (attrValue == null) {
            attributes.remove(new Integer(attrID));
        } else {
            attributes.put(new Integer(attrID), attrValue);
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("{\n");
        for (Enumeration e = attributes.keys(); e.hasMoreElements(); ) {
            Integer i = (Integer) e.nextElement();
            buf.append("0x");
            buf.append(Integer.toHexString(i.intValue()));
            buf.append(":\n\t");
            DataElement d = (DataElement) attributes.get(i);
            buf.append(d);
            buf.append("\n");
        }
        buf.append("}");
        return buf.toString();
    }

    /**
	 * Internal implementation function
	 */
    long getHandle() {
        return this.handle;
    }

    /**
	 * Internal implementation function
	 */
    void setHandle(long handle) {
        this.handle = handle;
    }

    /**
	 * Internal implementation function
	 */
    boolean hasServiceClassUUID(UUID uuid) {
        DataElement attrDataElement = getAttributeValue(BluetoothConsts.ServiceClassIDList);
        if ((attrDataElement == null) || (attrDataElement.getDataType() != DataElement.DATSEQ) || attrDataElement.getSize() == 0) {
            return false;
        }
        Object value = attrDataElement.getValue();
        if ((value == null) || (!(value instanceof Enumeration))) {
            DebugLog.debug("Bogus Value in DATSEQ");
            if (value != null) {
                DebugLog.error("DATSEQ class " + value.getClass().getName());
            }
            return false;
        }
        for (Enumeration e = (Enumeration) value; e.hasMoreElements(); ) {
            Object element = e.nextElement();
            if (!(element instanceof DataElement)) {
                DebugLog.debug("Bogus element in DATSEQ, " + value.getClass().getName());
                continue;
            }
            DataElement dataElement = (DataElement) element;
            if ((dataElement.getDataType() == DataElement.UUID) && (uuid.equals(dataElement.getValue()))) {
                return true;
            }
        }
        return false;
    }

    boolean hasProtocolClassUUID(UUID uuid) {
        DataElement protocolDescriptor = getAttributeValue(BluetoothConsts.ProtocolDescriptorList);
        if ((protocolDescriptor == null) || (protocolDescriptor.getDataType() != DataElement.DATSEQ)) {
            return false;
        }
        for (Enumeration protocolsSeqEnum = (Enumeration) protocolDescriptor.getValue(); protocolsSeqEnum.hasMoreElements(); ) {
            DataElement elementSeq = (DataElement) protocolsSeqEnum.nextElement();
            if (elementSeq.getDataType() == DataElement.DATSEQ) {
                Enumeration elementSeqEnum = (Enumeration) elementSeq.getValue();
                if (elementSeqEnum.hasMoreElements()) {
                    DataElement protocolElement = (DataElement) elementSeqEnum.nextElement();
                    if (protocolElement.getDataType() != DataElement.UUID) {
                        continue;
                    }
                    if (uuid.equals(protocolElement.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    DataElement clone(DataElement de) {
        DataElement c = null;
        switch(de.getDataType()) {
            case DataElement.U_INT_1:
            case DataElement.U_INT_2:
            case DataElement.U_INT_4:
            case DataElement.INT_1:
            case DataElement.INT_2:
            case DataElement.INT_4:
                c = new DataElement(de.getDataType(), de.getLong());
                break;
            case DataElement.URL:
            case DataElement.STRING:
            case DataElement.UUID:
            case DataElement.INT_16:
            case DataElement.INT_8:
            case DataElement.U_INT_16:
                c = new DataElement(de.getDataType(), de.getValue());
                break;
            case DataElement.NULL:
                c = new DataElement(de.getDataType());
                break;
            case DataElement.BOOL:
                c = new DataElement(de.getBoolean());
                break;
            case DataElement.DATSEQ:
            case DataElement.DATALT:
                c = new DataElement(de.getDataType());
                for (Enumeration en = (Enumeration) de.getValue(); en.hasMoreElements(); ) {
                    DataElement dataElement = (DataElement) en.nextElement();
                    c.addElement(clone(dataElement));
                }
        }
        return c;
    }

    /**
	 * Internal implementation function
	 */
    void populateRFCOMMAttributes(long handle, int channel, UUID uuid, String name, boolean obex) {
        this.populateAttributeValue(BluetoothConsts.ServiceRecordHandle, new DataElement(DataElement.U_INT_4, handle));
        DataElement serviceClassIDList = new DataElement(DataElement.DATSEQ);
        serviceClassIDList.addElement(new DataElement(DataElement.UUID, uuid));
        if (!obex) {
            serviceClassIDList.addElement(new DataElement(DataElement.UUID, BluetoothConsts.SERIAL_PORT_UUID));
        }
        this.populateAttributeValue(BluetoothConsts.ServiceClassIDList, serviceClassIDList);
        DataElement protocolDescriptorList = new DataElement(DataElement.DATSEQ);
        DataElement L2CAPDescriptor = new DataElement(DataElement.DATSEQ);
        L2CAPDescriptor.addElement(new DataElement(DataElement.UUID, BluetoothConsts.L2CAP_PROTOCOL_UUID));
        protocolDescriptorList.addElement(L2CAPDescriptor);
        DataElement RFCOMMDescriptor = new DataElement(DataElement.DATSEQ);
        RFCOMMDescriptor.addElement(new DataElement(DataElement.UUID, BluetoothConsts.RFCOMM_PROTOCOL_UUID));
        RFCOMMDescriptor.addElement(new DataElement(DataElement.U_INT_1, channel));
        protocolDescriptorList.addElement(RFCOMMDescriptor);
        if (obex) {
            DataElement OBEXDescriptor = new DataElement(DataElement.DATSEQ);
            OBEXDescriptor.addElement(new DataElement(DataElement.UUID, BluetoothConsts.OBEX_PROTOCOL_UUID));
            protocolDescriptorList.addElement(OBEXDescriptor);
        }
        this.populateAttributeValue(BluetoothConsts.ProtocolDescriptorList, protocolDescriptorList);
        if (name != null) {
            this.populateAttributeValue(BluetoothConsts.AttributeIDServiceName, new DataElement(DataElement.STRING, name));
        }
    }

    void populateL2CAPAttributes(int handle, int channel, UUID uuid, String name) {
        this.populateAttributeValue(BluetoothConsts.ServiceRecordHandle, new DataElement(DataElement.U_INT_4, handle));
        DataElement serviceClassIDList = new DataElement(DataElement.DATSEQ);
        serviceClassIDList.addElement(new DataElement(DataElement.UUID, uuid));
        this.populateAttributeValue(BluetoothConsts.ServiceClassIDList, serviceClassIDList);
        DataElement protocolDescriptorList = new DataElement(DataElement.DATSEQ);
        DataElement L2CAPDescriptor = new DataElement(DataElement.DATSEQ);
        L2CAPDescriptor.addElement(new DataElement(DataElement.UUID, BluetoothConsts.L2CAP_PROTOCOL_UUID));
        L2CAPDescriptor.addElement(new DataElement(DataElement.U_INT_2, channel));
        protocolDescriptorList.addElement(L2CAPDescriptor);
        this.populateAttributeValue(BluetoothConsts.ProtocolDescriptorList, protocolDescriptorList);
        if (name != null) {
            this.populateAttributeValue(BluetoothConsts.AttributeIDServiceName, new DataElement(DataElement.STRING, name));
        }
    }
}
