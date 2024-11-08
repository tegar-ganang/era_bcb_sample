package com.softwoehr.pigiron.functions;

import java.io.IOException;
import com.softwoehr.pigiron.access.*;

/**
 * <tt>Image_Disk_Share</tt> VSMAPI Function
 */
public class ImageDiskShare extends VSMCall {

    /**
     * The transmitted name of the function.
     */
    public static final String FUNCTION_NAME = "Image_Disk_Share";

    /** Read-only (R/O) access */
    public static final String READ_WRITE_MODE_R = "R";

    /** Read-only (R/O) access is desired even if the owner or another user has a link to the minidisk in write status */
    public static final String READ_WRITE_MODE_RR = "RR";

    /** Write access */
    public static final String READ_WRITE_MODE_W = "W";

    /** Write access is desired. Only R/O access allowed if the owner or any other user has a link to the minidisk in read or write status. */
    public static final String READ_WRITE_MODE_WR = "WR";

    /** Multiple access is desired' */
    public static final String READ_WRITE_MODE_M = "M";

    /** Write or any exclusive access is allowed to the minidisk unless another user already has write access to it. */
    public static final String READ_WRITE_MODE_MR = "MR";

    /** Write access is allowed to the disk unconditionally except for existing stable or exclusive links */
    public static final String READ_WRITE_MODE_MW = "MW";

    /**
     *  Create an instance of the function call with important fields not instanced.
     */
    public ImageDiskShare() {
    }

    /**
     * Create an instance with the variables filled in.
     * @param hostname  VSMAPI Host DNS name
     * @param port port VSMAPI Host is listening on
     * @param userid userid executing the function
     * @param password the password
     * @param target_identifier the target of the VSMAPI function
     * @param image_disk_number instances {@code imageDiskNumber}
     * @param target_image_name instances {@code targetImageName}
     * @param target_image_disk_number instances {@code targetImageDiskNumber}
     * @param read_write_mode instances {@code readWriteMode}
     * @param optional_password instances {@code optionalPassword}
     */
    public ImageDiskShare(String hostname, int port, String userid, String password, String target_identifier, String image_disk_number, String target_image_name, String target_image_disk_number, String read_write_mode, String optional_password) {
        this();
        setHostname(hostname);
        setPort(port);
        setUserid(userid);
        setPassword(password);
        setTarget_identifier(target_identifier);
        set_imageDiskNumber(image_disk_number);
        set_targetImageName(target_image_name);
        set_targetImageDiskNumber(target_image_disk_number);
        set_readWriteMode(read_write_mode);
        set_optionalPassword(optional_password);
    }

    /** The target_image_names virtual device address of the disk to be shared' */
    private String imageDiskNumber = "";

    /** The name of the virtual image that owns the image disk being shared */
    private String targetImageName = "";

    /** The virtual device number to assign to the shared disk for target_identifier */
    private String targetImageDiskNumber = "";

    /** The access mode requested for the disk as seen by the owner when the virtual image is logged on */
    private String readWriteMode = READ_WRITE_MODE_RR;

    /** The password that may be required to share the disk */
    private String optionalPassword = "";

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

    /** Set the value of {@code  targetImageName }.
     * @param val The value to set {@code  targetImageName }.
     */
    public void set_targetImageName(String val) {
        targetImageName = val;
    }

    /** Get the value of {@code  targetImageName }.
     * @return The value of {@code  targetImageName }.
     */
    public String get_targetImageName() {
        return targetImageName;
    }

    /** Set the value of {@code  targetImageDiskNumber }.
     * @param val The value to set {@code  targetImageDiskNumber }.
     */
    public void set_targetImageDiskNumber(String val) {
        targetImageDiskNumber = val;
    }

    /** Get the value of {@code  targetImageDiskNumber }.
     * @return The value of {@code  targetImageDiskNumber }.
     */
    public String get_targetImageDiskNumber() {
        return targetImageDiskNumber;
    }

    /** Set the value of {@code  readWriteMode }.
     * @param val The value to set {@code  readWriteMode }.
     */
    public void set_readWriteMode(String val) {
        readWriteMode = val;
    }

    /** Get the value of {@code  readWriteMode }.
     * @return The value of {@code  readWriteMode }.
     */
    public String get_readWriteMode() {
        return readWriteMode;
    }

    /** Set the value of {@code  optionalPassword }.
     * @param val The value to set {@code  optionalPassword }.
     */
    public void set_optionalPassword(String val) {
        optionalPassword = val;
    }

    /** Get the value of {@code  optionalPassword }.
     * @return The value of {@code  optionalPassword }.
     */
    public String get_optionalPassword() {
        return optionalPassword;
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
        tempString = new VSMString(get_targetImageName(), "target_image_name");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "target_image_name_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_targetImageDiskNumber(), "target_image_disk_number");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "target_image_disk_number_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_readWriteMode(), "read_write_mode");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "read_write_mode_length"));
        parameterArray.add(tempString);
        tempString = new VSMString(get_optionalPassword(), "optional_password");
        parameterArray.add(new VSMInt4(tempString.paramLength(), "optional_password_length"));
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
        ImageDiskShare instance = null;
        if (argv.length != 10) {
            System.out.println("usage: args are:\ninetaddr port user pw target image_disk_number target_image_name target_image_disk_number read_write_mode optional_password");
            System.exit(1);
        }
        System.out.println("Args are: " + argv[0] + " " + argv[1] + " " + argv[2] + " " + argv[3] + " " + argv[4] + " " + argv[5] + " " + argv[6] + " " + argv[7] + " " + argv[8] + " " + argv[9]);
        instance = new ImageDiskShare(argv[0], Integer.valueOf(argv[1]).intValue(), argv[2], argv[3], argv[4], argv[5], argv[6], argv[7], argv[8], argv[9]);
        ParameterArray pA = instance.doIt();
        System.out.println("Returns from call to " + instance.getFunctionName() + ":");
        System.out.println(pA.prettyPrintAll());
    }
}
