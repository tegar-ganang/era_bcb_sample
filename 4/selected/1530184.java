package org.mmtk.vm.gcspy;

import org.mmtk.plan.Plan;
import org.mmtk.utility.Log;
import org.mmtk.utility.Options;
import com.ibm.JikesRVM.VM_Uninterruptible;
import com.ibm.JikesRVM.VM_PragmaInterruptible;

/** 
 * This class implements collector-independent GCSpy functionality to start
 * the GCSpy server.
 *
 * author <a href="http://www.cs.ukc.ac.uk/people/staff/rej">Richard Jones</a>
 * @version $Revision: 7030 $
 * @date $Date: 2004-04-09 13:07:29 -0400 (Fri, 09 Apr 2004) $
 */
public class GCSpy implements VM_Uninterruptible {

    public static final String Id = "$Id: GCSpy.java 7030 2004-04-09 17:07:29Z perry-oss $";

    private static int gcspyPort_ = 0;

    private static boolean gcspyWait_ = false;

    /**
   * The boot method is called by the runtime immediately after
   * command-line arguments are available.  Note that allocation must
   * be supported prior to this point because the runtime
   * infrastructure may require allocation in order to parse the
   * command line arguments.  
   */
    public static void postBoot() {
    }

    /**
   * Get the number of the port that GCSpy communicates on
   *
   * @return the GCSpy port number
   */
    public static int getGCSpyPort() {
        return Options.gcspyPort;
    }

    /**
   * Should the JVM wait for GCSpy to connect?
   *
   * @return whether the JVM should wait for the visualiser to connect
   */
    public static boolean getGCSpyWait() {
        return Options.gcspyWait;
    }

    /**
   * Start the GCSpy server
   * WARNING: allocates memory indirectly
   */
    public static void startGCSpyServer() throws VM_PragmaInterruptible {
        int port = getGCSpyPort();
        Log.write("GCSpy.startGCSpyServer, port=", port);
        Log.write(", wait=");
        Log.writeln(getGCSpyWait());
        if (port > 0) {
            Plan.startGCSpyServer(port, getGCSpyWait());
            Log.writeln("gcspy thread booted");
        }
    }

    public static int getGCSpyPort() {
        return 0;
    }

    public static boolean getGCSpyWait() {
        return false;
    }

    public static void startGCSpyServer() {
    }
}
