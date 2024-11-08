package com.softwoehr.pigiron.functions;

import java.io.IOException;
import com.softwoehr.pigiron.access.*;

/**
 * <tt>Image_Disk_Create_DM</tt> VSMAPI Function
 */
public class ImageDiskCreateDM extends VSMCall {

    /**
     * The transmitted name of the function.
     */
    public static final String FUNCTION_NAME = "Image_Disk_Create_DM";

    /** CYLINDERS */
    public static final int ALLOCATION_UNIT_SIZE_CYLINDERS = 1;

    /** BLK0512 */
    public static final int ALLOCATION_UNIT_SIZE_BLK0512 = 2;

    /** BLK1024 */
    public static final int ALLOCATION_UNIT_SIZE_BLK1024 = 3;

    /** BLK2048 */
    public static final int ALLOCATION_UNIT_SIZE_BLK2048 = 4;

    /** BLK4096 */
    public static final int ALLOCATION_UNIT_SIZE_BLK4096 = 5;

    /** Read-only (R/O) access */
    public static final String IMAGE_DISK_MODE_R = "R";

    /** Read-only (R/O) access is desired even if the owner or another user has a link to the minidisk in write status */
    public static final String IMAGE_DISK_MODE_RR = "RR";

    /** Write access */
    public static final String IMAGE_DISK_MODE_W = "W";

    /** Write access is desired. Only R/O access allowed if the owner or any other user has a link to the minidisk in read or write status. */
    public static final String IMAGE_DISK_MODE_WR = "WR";

    /** Multiple access is desired' */
    public static final String IMAGE_DISK_MODE_M = "M";

    /** Write or any exclusive access is allowed to the minidisk unless another user already has write access to it. */
    public static final String IMAGE_DISK_MODE_MR = "MR";

    /** Write access is allowed to the disk unconditionally except for existing stable or exclusive links */
    public static final String IMAGE_DISK_MODE_MW = "MW";

    /** Unspecified */
    public static final int IMAGE_DISK_FORMATTING_UNSPECIFIED = 0;

    /** Unformatted */
    public static final int IMAGE_DISK_FORMATTING_NONE = 1;

    /** CMS formatted with 512 bytes per block */
    public static final int IMAGE_DISK_FORMATTING_CMS0512 = 2;

    /** CMS formatted with 1024 bytes per block */
    public static final int IMAGE_DISK_FORMATTING_CMS1024 = 3;

    /** CMS formatted with 2048 bytes per block */
    public static final int IMAGE_DISK_FORMATTING_CMS2048 = 4;

    /** CMS formatted with 4096 bytes per block */
    public static final int IMAGE_DISK_FORMATTING_CMS4096 = 5;

    /** CMS formatted with the default block size for the allocated device type */
    public static final int IMAGE_DISK_FORMATTING_CMS = 6;

    /**
     *  Create an instance of the function call with important fields not instanced.
     */
    public ImageDiskCreateDM() {
    }

    /**
     * Create an instance with the variables filled in.
     * @param hostname  VSMAPI Host DNS name
     * @param port port VSMAPI Host is listening on
     * @param userid userid executing the function
     * @param password the password
     * @param target_identifier the target of the VSMAPI function
     * @param image_disk_number instances {@code imageDiskNumber}
     * @param image_disk_device_type instances {@code imageDiskDeviceType}
     * @param image_disk_allocation_type instances {@code imageDiskAllocationType}
     * @param allocation_area_name_or_volser instances {@code allocationAreaNameOrVolser}
     * @param allocation_unit_size instances {@code allocationUnitSize}
     * @param image_disk_size instances {@code imageDiskSize}
     * @param image_disk_mode instances {@code imageDiskMode}
     * @param image_disk_formatting instances {@code imageDiskFormatting}
     * @param image_disk_label instances {@code imageDiskLabel}
     * @param read_password instances {@code readPassword}
     * @param write_password instances {@code writePassword}
     * @param multi_password instances {@code multiPassword}
     */
    public ImageDiskCreateDM(String hostname, int port, String userid, String password, String target_identifier, String image_disk_number, String image_disk_device_type, String image_disk_allocation_type, String allocation_area_name_or_volser, int allocation_unit_size, int image_disk_size, String image_disk_mode, int image_disk_formatting, String image_disk_label, String read_password, String write_password, String multi_password) {
        this();
        setHostname(hostname);
        setPort(port);
        setUserid(userid);
        setPassword(password);
        setTarget_identifier(target_identifier);
        set_imageDiskNumber(image_disk_number);
        set_imageDiskDeviceType(image_disk_device_type);
        set_imageDiskAllocationType(image_disk_allocation_type);
        set_allocationAreaNameOrVolser(allocation_area_name_or_volser);
        set_allocationUnitSize(allocation_unit_size);
        set_imageDiskSize(image_disk_size);
        set_imageDiskMode(image_disk_mode);
        set_imageDiskFormatting(image_disk_formatting);
        set_imageDiskLabel(image_disk_label);
        set_readPassword(read_password);
        set_writePassword(write_password);
        set_multiPassword(multi_password);
    }

    /** The virtual device address of the disk to be copied */
    private String imageDiskNumber = "";

    /** The device type of the volume to which the disk is assigned */
    private String imageDiskDeviceType = "";

    /** Location or allocation */
    private String imageDiskAllocationType = "";

    /** Like it says */
    private String allocationAreaNameOrVolser = "";

    /** Like it says */
    private int allocationUnitSize = 0;

    /** Like it says */
    private int imageDiskSize = 0;

    /** The access mode requested for the disk */
    private String imageDiskMode = "";

    /** Like it says */
    private int imageDiskFormatting = 0;

    /** The disk label to use when formatting the new extent */
    private String imageDiskLabel = "";

    /** Defines the read password that will be used for accessing the disk */
    private String readPassword = "";

    /** Defines the write password that will be used for accessing the disk */
    private String writePassword = "";

    /** Defines the multi password that will be used for accessing the disk */
    private String multiPassword = "";

    /** Set the value of {@code  imageDiskNumber }.
     * @param val The value to set {@code  imageDiskNumber }.
     */
    public void set_imageDiskNumber(String val) {
        imageDiskNumber = val;
    }

    /** Get the value of {@code  imageDiskNumber }.
     * @return The value of {@code  imageDiskNumber }.
     */
    public String get_imageDiskNumber() {
        return imageDiskNumber;
    }

    /** Set the value of {@code  imageDiskDeviceType }.
     * @param val The value to set {@code  imageDiskDeviceType }.
     */
    public void set_imageDiskDeviceType(String val) {
        imageDiskDeviceType = val;
    }

    /** Get the value of {@code  imageDiskDeviceType }.
     * @return The value of {@code  imageDiskDeviceType }.
     */
    public String get_imageDiskDeviceType() {
        return imageDiskDeviceType;
    }

    /** Set the value of {@code  imageDiskAllocationType }.
     * @param val The value to set {@code  imageDiskAllocationType }.
     */
    public void set_imageDiskAllocationType(String val) {
        imageDiskAllocationType = val;
    }

    /** Get the value of {@code  imageDiskAllocationType }.
     * @return The value of {@code  imageDiskAllocationType }.
     */
    public String get_imageDiskAllocationType() {
        return imageDiskAllocationType;
    }

    /** Set the value of {@code  allocationAreaNameOrVolser }.
     * @param val The value to set {@code  allocationAreaNameOrVolser }.
     */
    public void set_allocationAreaNameOrVolser(String val) {
        allocationAreaNameOrVolser = val;
    }

    /** Get the value of {@code  allocationAreaNameOrVolser }.
     * @return The value of {@code  allocationAreaNameOrVolser }.
     */
    public String get_allocationAreaNameOrVolser() {
        return allocationAreaNameOrVolser;
    }

    /** Set the value of {@code  allocationUnitSize }.
     * @param val The value to set {@code  allocationUnitSize }.
     */
    public void set_allocationUnitSize(int val) {
        allocationUnitSize = val;
    }

    /** Get the value of {@code  allocationUnitSize }.
     * @return The value of {@code  allocationUnitSize }.
     */
    public int get_allocationUnitSize() {
        return allocationUnitSize;
    }

    /** Set the value of {@code  imageDiskSize }.
     * @param val The value to set {@code  imageDiskSize }.
     */
    public void set_imageDiskSize(int val) {
        imageDiskSize = val;
    }

    /** Get the value of {@code  imageDiskSize }.
     * @return The value of {@code  imageDiskSize }.
     */
    public int get_imageDiskSize() {
        return imageDiskSize;
    }

    /** Set the value of {@code  imageDiskMode }.
     * @param val The value to set {@code  imageDiskMode }.
     */
    public void set_imageDiskMode(String val) {
        imageDiskMode = val;
    }

    /** Get the value of {@code  imageDiskMode }.
     * @return The value of {@code  imageDiskMode }.
     */
    public String get_imageDiskMode() {
        return imageDiskMode;
    }

    /** Set the value of {@code  imageDiskFormatting }.
     * @param val The value to set {@code  imageDiskFormatting }.
     */
    public void set_imageDiskFormatting(int val) {
        imageDiskFormatting = val;
    }

    /** Get the value of {@code  imageDiskFormatting }.
     * @return The value of {@code  imageDiskFormatting }.
     */
    public int get_imageDiskFormatting() {
        return imageDiskFormatting;
    }

    /** Set the value of {@code  imageDiskLabel }.
     * @param val The value to set {@code  imageDiskLabel }.
     */
    public void set_imageDiskLabel(String val) {
        imageDiskLabel = val;
    }

    /** Get the value of {@code  imageDiskLabel }.
     * @return The value of {@code  imageDiskLabel }.
     */
    public String get_imageDiskLabel() {
        return imageDiskLabel;
    }

    /** Set the value of {@code  readPassword }.
     * @param val The value to set {@code  readPassword }.
     */
    public void set_readPassword(String val) {
        readPassword = val;
    }

    /** Get the value of {@code  readPassword }.
     * @return The value of {@code  readPassword }.
     */
    public String get_readPassword() {
        return readPassword;
    }

    /** Set the value of {@code  writePassword }.
     * @param val The value to set {@code  writePassword }.
     */
    public void set_writePassword(String val) {
        writePassword = val;
    }

    /** Get the value of {@code  writePassword }.
     * @return The value of {@code  writePassword }.
     */
    public String get_writePassword() {
        return writePassword;
    }

    /** Set the value of {@code  multiPassword }.
     * @param val The value to set {@code  multiPassword }.
     */
    public void set_multiPassword(String val) {
        multiPassword = val;
    }

    /** Get the value of {@code  multiPassword }.
     * @return The value of {@code  multiPassword }.
     */
    public String get_multiPassword() {
        return multiPassword;
    }

    /**
     * Marshall parameters for the VSMAPI function call.
     * "Input" as in "input to VSMAPI".
     * @return the composed input ParameterArray
     * @see #composeOutputArray()
     * @see com.softwoehr.pigiron.access.ParameterArray
     */
    protected ParameterArray composeInputArray() {
        VSMString tempString = null;
        ParameterArray parameterArray = new ParameterArray(this);
        tempString = new VSMString(getFunctionName(), getFunctionName());
        parameterArray.add(new VSMInt4(tempString.paramLength(), "function_name_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(getUserid(), "authenticated_userid");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "authenticated_userid_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(getPassword(), "password");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "password_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(getTarget_identifier(), "target_identifier");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "target_identifier_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_imageDiskNumber(), "image_disk_number");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "image_disk_number_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_imageDiskDeviceType(), "image_disk_device_type");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "image_disk_device_type_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_imageDiskAllocationType(), "image_disk_allocation_type");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "image_disk_allocation_type_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_allocationAreaNameOrVolser(), "allocation_area_name_or_volser");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "allocation_area_name_or_volser_length"));
        parameterArray.add(tempString);
        parameterArray.add(new VSMInt1(get_allocationUnitSize(), "allocation_unit_size"));
        parameterArray.add(new VSMInt4(get_imageDiskSize(), "image_disk_size"));
        tempString = new VSMString(get_imageDiskMode(), "image_disk_mode");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "image_disk_mode_length"));
        parameterArray.add(tempString);
        parameterArray.add(new VSMInt1(get_imageDiskFormatting(), "image_disk_formatting"));
        tempString = new VSMString(get_imageDiskLabel(), "image_disk_label");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "image_disk_label_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_readPassword(), "read_password");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "read_password_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_writePassword(), "write_password");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "write_password_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_multiPassword(), "multi_password");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "multi_password_length"));
        parameterArray.add(tempString);
        VSMInt4 outputLength = new VSMInt4(new Long(parameterArray.totalParameterLength()).intValue(), "output_length");
        parameterArray.insertElementAt(outputLength, 0);
        setInParams(parameterArray);
        return parameterArray;
    }

    /**
     * Marshall parameters for the return of the VSMAPI function call.
     * "output" as in "output from VSMAPI"
     * @return the composed output ParameterArray
     * @see #composeInputArray()
     * @see com.softwoehr.pigiron.access.ParameterArray
     */
    protected ParameterArray composeOutputArray() {
        ParameterArray parameterArray = new ParameterArray(this);
        parameterArray.add(new VSMInt4(-1, "request_id_immediate"));
        parameterArray.add(new VSMInt4(-1, "output_length"));
        parameterArray.add(new VSMInt4(-1, "request_id"));
        parameterArray.add(new VSMInt4(-1, "return_code"));
        parameterArray.add(new VSMInt4(-1, "reason_code"));
        setOutParams(parameterArray);
        return parameterArray;
    }

    /**
     * Get the formal name of the VSMAPI function.
     * @return the formal name of the VSMAPI function.
     */
    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    /**
     * You can execute the VSMAPI call from <tt>main()</tt>, try it
     * with no args to see the usage message.
     * @param argv array of commandline args
     * @throws IOException on comm error
     * @throws VSMException on internal Pigiron param marshalling error
     */
    public static void main(String[] argv) throws IOException, VSMException {
        ImageDiskCreateDM instance = null;
        if (argv.length != 17) {
            System.out.println("usage: args are:\ninetaddr port user pw target image_disk_number image_disk_device_type image_disk_allocation_type allocation_area_name_or_volser allocation_unit_size image_disk_size image_disk_mode image_disk_formatting image_disk_label read_password write_password multi_password");
            System.exit(1);
        }
        System.out.println("Args are: " + argv[0] + " " + argv[1] + " " + argv[2] + " " + argv[3] + " " + argv[4] + " " + argv[5] + " " + argv[6] + " " + argv[7] + " " + argv[8] + " " + argv[9] + " " + argv[10] + " " + argv[11] + " " + argv[12] + " " + argv[13] + " " + argv[14] + " " + argv[15] + " " + argv[16]);
        instance = new ImageDiskCreateDM(argv[0], Integer.valueOf(argv[1]).intValue(), argv[2], argv[3], argv[4], argv[5], argv[6], argv[7], argv[8], Integer.valueOf(argv[9]).intValue(), Integer.valueOf(argv[10]).intValue(), argv[11], Integer.valueOf(argv[12]).intValue(), argv[13], argv[14], argv[15], argv[16]);
        ParameterArray pA = instance.doIt();
        System.out.println("Returns from call to " + instance.getFunctionName() + ":");
        System.out.println(pA.prettyPrintAll());
    }
}
