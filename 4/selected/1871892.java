package jhomenet.server.console.command;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import jhomenet.commons.hw.sensor.Sensor;
import jhomenet.commons.hw.sensor.ValueSensor;
import jhomenet.commons.hw.HomenetHardware;
import jhomenet.commons.hw.RegisteredHardware;
import jhomenet.commons.work.WorkQueue;
import jhomenet.commons.work.unit.WorkUnit;
import jhomenet.server.console.IOUtils;
import jhomenet.server.console.io.SystemInputStream;
import jhomenet.server.console.io.SystemPrintStream;

/**
 * TODO: Class description.
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class ListRegisteredHardwareCommand implements Command {

    /**
     * Define a logging mechanism.
     */
    private static Logger logger = Logger.getLogger(ListHardwareCommand.class.getName());

    /**
     * A work unit for retrieving the registered hardware list
     */
    private final WorkUnit<List<RegisteredHardware>> getRegisteredHardwareListWorkUnit;

    /**
     * The work queue.
     */
    private final WorkQueue workQueue;

    /**
     * Command name.
     */
    private static final String commandName = "list-registered";

    /**
     * Constructor.
     * 
     * @param getRegisteredHardwarerWorkUnit
     * @param workQueue
     */
    public ListRegisteredHardwareCommand(WorkUnit<List<RegisteredHardware>> getRegisteredHardwarerWorkUnit, WorkQueue workQueue) {
        super();
        if (getRegisteredHardwarerWorkUnit == null) throw new IllegalArgumentException("Registered hardware list work unit cannot be null!");
        if (workQueue == null) throw new IllegalArgumentException("Work queue cannot be null!");
        this.getRegisteredHardwareListWorkUnit = getRegisteredHardwarerWorkUnit;
        this.workQueue = workQueue;
    }

    /**
     * @see jhomenet.server.console.command.Command#execute(jhomenet.server.console.io.SystemInputStream, jhomenet.server.console.io.SystemPrintStream, jhomenet.server.console.io.SystemPrintStream, java.lang.String[], java.util.Map)
     */
    public void execute(SystemInputStream in, SystemPrintStream out, SystemPrintStream err, String[] args, Map env) throws Exception {
        boolean detailed = false;
        if (args.length == 1) if (args[0].equals("-D")) detailed = true;
        List<RegisteredHardware> registeredHardwareList = workQueue.addWork(getRegisteredHardwareListWorkUnit).get();
        IOUtils.writeLine("Registered hardware:", out);
        IOUtils.writeLine("Format: [HW address, HW desc, HW class, Setup desc, Polling interval, Preferred unit]", out);
        IOUtils.newLine(out);
        for (RegisteredHardware registeredHardware : registeredHardwareList) {
            IOUtils.twoSpaces(out);
            IOUtils.write(registeredHardware.getHardwareAddr() + ", ", out);
            IOUtils.write(registeredHardware.getAppHardwareDescription() + ", ", out);
            IOUtils.write(registeredHardware.getHardwareClassname() + ", ", out);
            IOUtils.write(registeredHardware.getHardwareSetupDescription(), out);
            if (registeredHardware instanceof Sensor) {
                Sensor sensor = (Sensor) registeredHardware;
                IOUtils.write(", " + sensor.getPollingInterval().toString(), out);
                if (sensor instanceof ValueSensor) {
                    ValueSensor vSensor = (ValueSensor) sensor;
                    IOUtils.write(", " + vSensor.getPreferredDataUnit().toString(), out);
                }
            }
            if (detailed) {
                if (registeredHardware instanceof HomenetHardware) {
                    HomenetHardware hw = (HomenetHardware) registeredHardware;
                    IOUtils.newLine(out);
                    for (int i = 0; i < hw.getNumChannels(); i++) {
                        IOUtils.twoSpaces(out);
                        IOUtils.twoSpaces(out);
                        IOUtils.writeLine("CH-" + i + ": " + hw.getChannelDescription(i), out);
                    }
                }
            } else {
                IOUtils.newLine(out);
            }
        }
        IOUtils.newLine(out);
    }

    /**
     * @see jhomenet.server.console.command.Command#getUsageString()
     */
    public String getUsageString() {
        return "Lists all of the current registered hardware found on the network.\r\nUsage:\r\n  list-registered [arguments]\r\n" + "where arguments include:\r\n  -D 	for detailed information\r\n";
    }

    /**
     * @see jhomenet.server.console.command.Command#getCommandName()
     */
    public String getCommandName() {
        return commandName;
    }
}
