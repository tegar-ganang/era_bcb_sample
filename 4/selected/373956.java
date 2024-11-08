package com.softwoehr.pigiron.webobj.topview.functions;

import com.softwoehr.pigiron.webobj.topview.*;
import com.softwoehr.pigiron.webobj.topview.functions.FunctionProxy;

/**
 * Proxy function class to bridge JSON to PigIron
 *
 */
public class ImageDiskCreateDM extends FunctionProxy {

    /**
     * Create an instance of the function proxy with requestor and response instanced.
     * It will consume the requestor in execution and return the response suitably modified.
     * @param requestor the requestor spawning the instance execution
     * @param response the response to be modified and returned in the execution
     */
    public ImageDiskCreateDM(Requestor requestor, Response response) throws org.json.JSONException {
        super(requestor, response);
    }

    /**
     * Execute the PigIron VSMAPI call we have set up in this instance.
     *
     * @return                             the response from the call
     * @exception  org.json.JSONException  on JSON err
     *
     * The PigIron/VSMAPI parameters fed to the instancing within execute() are as follows:
     *   --  hostname  VSMAPI Host DNS name
     *   --  port port VSMAPI Host is listening on
     *   --  userid userid executing the function
     *   --  password the password
     *   --  target_identifier the target of the VSMAPI function
     *   --  image_disk_number instances {@code String}
     *   --  image_disk_device_type instances {@code String}
     *   --  image_disk_allocation_type instances {@code String}
     *   --  allocation_area_name_or_volser instances {@code String}
     *   --  allocation_unit_size instances {@code int}
     *   --  image_disk_size instances {@code int}
     *   --  image_disk_mode instances {@code String}
     *   --  image_disk_formatting instances {@code int}
     *   --  image_disk_label instances {@code String}
     *   --  read_password instances {@code String}
     *   --  write_password instances {@code String}
     *   --  multi_password instances {@code String}
     */
    public Response execute() throws org.json.JSONException {
        com.softwoehr.pigiron.functions.ImageDiskCreateDM pigfunc = new com.softwoehr.pigiron.functions.ImageDiskCreateDM(getHostSpecifier(), host.getPortNumber(), user.getUid(), user.getPassword(), getTargetIdentifier(), getInputArgumentString("image_disk_number"), getInputArgumentString("image_disk_device_type"), getInputArgumentString("image_disk_allocation_type"), getInputArgumentString("allocation_area_name_or_volser"), (int) getInputArgumentLong("allocation_unit_size"), (int) getInputArgumentLong("image_disk_size"), getInputArgumentString("image_disk_mode"), (int) getInputArgumentLong("image_disk_formatting"), getInputArgumentString("image_disk_label"), getInputArgumentString("read_password"), getInputArgumentString("write_password"), getInputArgumentString("multi_password"));
        execute(pigfunc, requestor, response);
        return response;
    }
}