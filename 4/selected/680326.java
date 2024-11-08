package com.softwoehr.pigiron.functions;

import java.io.IOException;
import com.softwoehr.pigiron.access.*;

/**
 * <tt>Image_Disk_Copy_DM</tt> VSMAPI Function
 */
public class ImageDiskCopyDM extends VSMCall {

    /**
     * The transmitted name of the function.
     */
    public static final String FUNCTION_NAME = "Image_Disk_Copy_DM";

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

    /**
     *  Create an instance of the function call with important fields not instanced.
     */
    public ImageDiskCopyDM() {
    }

    /**
     * Create an instance with the variables filled in.
     * @param hostname  VSMAPI Host DNS name
     * @param port port VSMAPI Host is listening on
     * @param userid userid executing the function
     * @param password the password
     * @param target_identifier the target of the VSMAPI function
     * @param image_disk_number instances {@code imageDiskNumber}
     * @param source_image_name instances {@code sourceImageName}
     * @param source_image_disk_number instances {@code sourceImageDiskNumber}
     * @param image_disk_allocation_type instances {@code imageDiskAllocationType}
     * @param allocation_area_name_or_volser instances {@code allocationAreaNameOrVolser}
     * @param image_disk_mode instances {@code imageDiskMode}
     * @param read_password instances {@code readPassword}
     * @param write_password instances {@code writePassword}
     * @param multi_password instances {@code multiPassword}
     */
    public ImageDiskCopyDM(String hostname, int port, String userid, String password, String target_identifier, String image_disk_number, String source_image_name, String source_image_disk_number, String image_disk_allocation_type, String allocation_area_name_or_volser, String image_disk_mode, String read_password, String write_password, String multi_password) {
        this();
        setHostname(hostname);
        setPort(port);
        setUserid(userid);
        setPassword(password);
        setTarget_identifier(target_identifier);
        set_imageDiskNumber(image_disk_number);
        set_sourceImageName(source_image_name);
        set_sourceImageDiskNumber(source_image_disk_number);
        set_imageDiskAllocationType(image_disk_allocation_type);
        set_allocationAreaNameOrVolser(allocation_area_name_or_volser);
        set_imageDiskMode(image_disk_mode);
        set_readPassword(read_password);
        set_writePassword(write_password);
        set_multiPassword(multi_password);
    }

    /** The virtual device address of the disk to be copied */
    private String imageDiskNumber = "";

    /** The name of the virtual image that owns the image disk being copied */
    private String sourceImageName = "";

    /** The image disk number of the virtual image that owns the disk being copied */
    private String sourceImageDiskNumber = "";

    /** Location or allocation */
    private String imageDiskAllocationType = "";

    /** Like it says */
    private String allocationAreaNameOrVolser = "";

    /** The access mode requested for the disk */
    private String imageDiskMode = "";

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

    /** Set the value of {@code  sourceImageName }.
     * @param val The value to set {@code  sourceImageName }.
     */
    public void set_sourceImageName(String val) {
        sourceImageName = val;
    }

    /** Get the value of {@code  sourceImageName }.
     * @return The value of {@code  sourceImageName }.
     */
    public String get_sourceImageName() {
        return sourceImageName;
    }

    /** Set the value of {@code  sourceImageDiskNumber }.
     * @param val The value to set {@code  sourceImageDiskNumber }.
     */
    public void set_sourceImageDiskNumber(String val) {
        sourceImageDiskNumber = val;
    }

    /** Get the value of {@code  sourceImageDiskNumber }.
     * @return The value of {@code  sourceImageDiskNumber }.
     */
    public String get_sourceImageDiskNumber() {
        return sourceImageDiskNumber;
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
        tempString = new VSMString(get_sourceImageName(), "source_image_name");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "source_image_name_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_sourceImageDiskNumber(), "source_image_disk_number");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "source_image_disk_number_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_imageDiskAllocationType(), "image_disk_allocation_type");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "image_disk_allocation_type_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_allocationAreaNameOrVolser(), "allocation_area_name_or_volser");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "allocation_area_name_or_volser_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_imageDiskMode(), "image_disk_mode");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "image_disk_mode_length"));
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
        ImageDiskCopyDM instance = null;
        if (argv.length != 14) {
            System.out.println("usage: args are:\ninetaddr port user pw target image_disk_number");
            System.exit(1);
        }
        System.out.println("Args are: " + argv[0] + " " + argv[1] + " " + argv[2] + " " + argv[3] + " " + argv[4] + " " + argv[5] + " " + argv[6] + " " + argv[7] + " " + argv[8] + " " + argv[9] + " " + argv[10] + " " + argv[11] + " " + argv[12] + " " + argv[13]);
        instance = new ImageDiskCopyDM(argv[0], Integer.valueOf(argv[1]).intValue(), argv[2], argv[3], argv[4], argv[5], argv[6], argv[7], argv[8], argv[9], argv[10], argv[11], argv[12], argv[13]);
        ParameterArray pA = instance.doIt();
        System.out.println("Returns from call to " + instance.getFunctionName() + ":");
        System.out.println(pA.prettyPrintAll());
    }
}
