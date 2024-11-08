package jhomenet.server.console.command;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.hw.RegisteredHardware;
import jhomenet.commons.hw.sensor.Sensor;
import jhomenet.commons.hw.mngt.NoSuchHardwareException;
import jhomenet.commons.hw.data.HardwareData;
import jhomenet.commons.work.unit.WorkUnit;
import jhomenet.server.console.IOUtils;
import jhomenet.server.console.io.SystemInputStream;
import jhomenet.server.console.io.SystemPrintStream;
import jhomenet.server.work.units.CaptureSensorDataWorkUnit;

/**
 * TODO: Class description.
 * <p>
 * Id: $Id: $
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class CaptureDataCommand extends AbstractCommand {

    /**
	 * Define a logging mechanism.
	 */
    private static Logger logger = Logger.getLogger(ListHardwareCommand.class.getName());

    /**
	 * Command.
	 */
    private static final String command = "capture-data";

    /**
	 * Constructor.
	 * 
	 * @param hardwareManager
	 * @param workQueue
	 */
    public CaptureDataCommand(GeneralApplicationContext serverContext) {
        super(serverContext);
    }

    /**
	 * @see jhomenet.server.console.command.Command#execute(jhomenet.server.console.io.SystemInputStream, jhomenet.server.console.io.SystemPrintStream, jhomenet.server.console.io.SystemPrintStream, java.lang.String[], java.util.Map)
	 */
    public void execute(SystemInputStream in, SystemPrintStream out, SystemPrintStream err, String[] args, Map env) throws Exception {
        if (args.length == 0) {
            captureData(IOUtils.promptForHardwareAddr(serverContext.getHardwareManager(), serverContext.getWorkQueue(), in, out, err), out, err);
        } else {
            if (args[0].equalsIgnoreCase("-A")) {
                captureData(args[1], out, err);
            } else {
                IOUtils.writeLine("Unknown command line parameter: " + args[0], out);
                IOUtils.writeLine(getUsageString(), out);
            }
        }
    }

    /**
	 * 
	 *
	 * @param hardwareAddr
	 * @param out
	 */
    private void captureData(String hardwareAddr, SystemPrintStream out, SystemPrintStream err) {
        if (hardwareAddr == null) {
            IOUtils.writeLine("ERROR: Invalid hardware address: " + hardwareAddr, out);
            IOUtils.newLine(out);
        }
        try {
            RegisteredHardware registeredHardware = serverContext.getHardwareManager().getRegisteredHardware(hardwareAddr);
            if (registeredHardware instanceof Sensor) {
                WorkUnit<List<HardwareData>> captureDataWorkUnit = new CaptureSensorDataWorkUnit((Sensor) registeredHardware);
                try {
                    List<HardwareData> hardwareDataList = serverContext.getWorkQueue().addWork(captureDataWorkUnit).get();
                    IOUtils.twoSpaces(out);
                    IOUtils.writeLine("Hardware address: " + hardwareAddr, out);
                    for (HardwareData data : hardwareDataList) {
                        IOUtils.twoSpaces(out);
                        IOUtils.twoSpaces(out);
                        IOUtils.writeLine("CH-" + data.getChannel() + ", data: " + data.getDataString() + ", timestamp: " + data.getTimestamp().toString(), out);
                    }
                    IOUtils.newLine(out);
                } catch (InterruptedException ie) {
                    IOUtils.writeLine("ERROR: Error while capturing data: " + ie.getMessage(), out);
                    ie.printStackTrace(err);
                    IOUtils.newLine(out);
                } catch (ExecutionException ee) {
                    IOUtils.writeLine("ERROR: Error while capturing data: " + ee.getMessage(), out);
                    ee.printStackTrace(err);
                    IOUtils.newLine(out);
                }
            } else {
                IOUtils.writeLine("Hardware must be a sensor!", out);
                IOUtils.newLine(out);
            }
        } catch (NoSuchHardwareException nshe) {
            IOUtils.write("No such hardware exists", out);
            IOUtils.newLine(out);
        }
    }

    /**
	 * @see jhomenet.server.console.command.Command#getUsageString()
	 */
    public String getUsageString() {
        return "Capture hardware data for a given hardware.\r\nUsage:\r\n  capture-data [arguments]\r\n" + "where arguments include:\r\n  -A <hardware address>\r\n";
    }

    /**
	 * @see jhomenet.server.console.command.Command#getCommandName()
	 */
    public String getCommandName() {
        return command;
    }
}
