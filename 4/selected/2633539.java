package basys.client.commands;

import basys.LocaleResourceBundle;
import basys.client.ListEntry;
import basys.client.Project;
import basys.client.eib.EIBDeviceConfigurator;
import basys.client.ui.dialogs.ListSelectorDialog;
import basys.client.ui.dialogs.NewEndDeviceDialog;
import basys.datamodels.installation.InstallationModel;
import basys.datamodels.architectural.ArchitecturalDataModel;
import basys.datamodels.eib.EIBDevicesDataModel;
import basys.eib.EIBPhaddress;
import basys.eib.exceptions.EIBAddressFormatException;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

/**
 * AddSensorCommand.java
 * 
 * 
 * @author	oalt
 * @version $Id: AddSensorCommand.java,v 1.1 2004/01/14 21:38:40 oalt Exp $
 * 
 */
public class AddSensorCommand extends Command {

    private static ResourceBundle locale = LocaleResourceBundle.getLocale();

    private static Logger logger = Logger.getLogger(AddSensorCommand.class);

    public static final int LAMP = 1;

    public static final int DIMMABLE_LAMP = 2;

    public static final int VALVE = 3;

    public static final int JALOUSIE = 4;

    public static final int SENSOR = 100;

    private static final boolean DEBUG = true;

    private Project project;

    private String roomID;

    private Point location;

    /**
	 * @param c
	 */
    public AddSensorCommand(Component c, Project project, String roomID, Point location) {
        super(c);
        this.project = project;
        this.roomID = roomID;
        this.location = location;
    }

    /**
	 * @see basys.client.commands.Command#execute()
	 */
    public boolean execute() {
        EIBDevicesDataModel devmodel = (EIBDevicesDataModel) this.project.getApplication().getBusDeviceDataModel(this.project.getPrefferedBusSystem());
        ArchitecturalDataModel amodel = this.project.getArchitecturalDataModel();
        InstallationModel imodel = this.project.getInstallationModel();
        Vector passiveIDs = (Vector) imodel.getInstallationDeviceIDs();
        Vector passiveList = new Vector();
        for (int i = 0; i < passiveIDs.size(); i++) {
            String id = (String) passiveIDs.get(i);
            ListEntry entry = new ListEntry(imodel.getName(id), id);
            passiveList.add(entry);
        }
        if (passiveList.isEmpty()) {
            JOptionPane.showMessageDialog(null, locale.getString("mess.noDevForSensor"));
            return false;
        }
        ListSelectorDialog lsd0 = new ListSelectorDialog(new Frame(), locale.getString("tit.selectInstallationDevice"), passiveList);
        lsd0.setVisible(true);
        if (lsd0.getSelection() == null) {
            return false;
        }
        String enddeviceID = ((ListEntry) lsd0.getSelection()).getValue();
        String enddeviceName = ((ListEntry) lsd0.getSelection()).getDisplayedName();
        String bussystem = imodel.getPropertyByName(enddeviceID, "bussystem");
        if (bussystem.equals("EIB")) {
            NewEndDeviceDialog dialog = new NewEndDeviceDialog(new Frame(), project.getPrefferedBusSystem(), SENSOR);
            dialog.setName("Sensor " + enddeviceName);
            int exitValue = dialog.showDialog();
            if (exitValue == 0) {
                return false;
            }
            EIBDeviceConfigurator finder;
            String installationLocationID = this.roomID;
            if (dialog.getInstallationLocation().equals("REG")) {
                Vector jboxids = amodel.getJunctionBoxList();
                if (jboxids.isEmpty()) {
                    JOptionPane.showMessageDialog(null, locale.getString("mess.noJunctionBoxesFonund"));
                    return false;
                } else {
                    if (jboxids.size() > 1 && jboxids.size() != 0) {
                        ListSelectorDialog lsd = new ListSelectorDialog(new Frame(), locale.getString("tit.selection"), jboxids);
                        lsd.setVisible(true);
                        if (lsd.getSelection() == null) {
                            return false;
                        } else {
                            installationLocationID = ((ListEntry) lsd.getSelection()).getValue();
                        }
                    } else {
                        installationLocationID = ((ListEntry) jboxids.firstElement()).getValue();
                    }
                }
            }
            finder = new EIBDeviceConfigurator(SENSOR, installationLocationID, dialog.getInstallationLocation(), this.project);
            boolean takeNew = true;
            Vector alreadyActors = finder.findSensorsInProject(imodel.getFunctionsForEnddevice(enddeviceID));
            if (!alreadyActors.isEmpty()) {
                int option = JOptionPane.showConfirmDialog(null, locale.getString("mess.devInProjectFound"), "", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    takeNew = false;
                } else {
                    takeNew = true;
                }
            }
            String sensorID = "";
            if (takeNew) {
                sensorID = this.addNewDevice(finder, dialog, enddeviceID);
            } else {
                sensorID = this.selectAlreadyInstalledSensor(alreadyActors);
            }
            if (sensorID == null) {
                return false;
            }
            logger.debug("Sensor ID: " + sensorID);
            Vector functionList = (Vector) imodel.getFunctionGroupList(sensorID);
            ListEntry leFuncGroup;
            if (functionList.size() > 1 && functionList.size() != 0) {
                ListSelectorDialog lsd3 = new ListSelectorDialog(new Frame(), locale.getString("tit.selDevice"), functionList);
                lsd3.setVisible(true);
                if (lsd3.getSelection() == null) {
                    return false;
                }
                leFuncGroup = (ListEntry) lsd3.getSelection();
            } else {
                leFuncGroup = (ListEntry) functionList.firstElement();
            }
            String devID = imodel.addPassiveComponent();
            imodel.setName(devID, dialog.getName());
            imodel.setProperty(devID, "bussystem", bussystem);
            imodel.setProperty(devID, "sensor-installation-location", dialog.getInstallationLocation());
            imodel.setProperty(devID, "type", dialog.getDeviceType() + "");
            imodel.setName(leFuncGroup.getValue(), leFuncGroup.getDisplayedName());
            String afuncGrpID = ((String) ((Vector) imodel.getConnections(enddeviceID)).firstElement());
            imodel.setSensorGroupAddresses(afuncGrpID, leFuncGroup.getValue());
            imodel.addConnection(devID, leFuncGroup.getValue());
            amodel.addEndDevice(this.roomID, devID, this.location, dialog.getDeviceType());
            String state = imodel.getPropertyByName(sensorID, "device-state");
            if (state.equals("ready")) {
                imodel.setProperty(sensorID, "device-state", "addressed");
            }
            return true;
        } else {
            JOptionPane.showMessageDialog(null, locale.getString("mess.busNotSupported"));
            return false;
        }
    }

    /**
	 * 
	 * @param actuatorIDs
	 * @return
	 */
    private String selectAlreadyInstalledSensor(Vector sensorIDs) {
        InstallationModel imodel = this.project.getInstallationModel();
        Vector sensororlist = new Vector();
        for (Enumeration e = sensorIDs.elements(); e.hasMoreElements(); ) {
            String id = (String) e.nextElement();
            if (DEBUG) {
            }
            String text = imodel.getName(id) + " - " + imodel.getPropertyByName(id, "device-name");
            ListEntry le = new ListEntry(text, id);
            sensororlist.addElement(le);
        }
        if (sensororlist.size() > 1 && sensororlist.size() != 0) {
            ListSelectorDialog lsd2 = new ListSelectorDialog(new Frame(), locale.getString("tit.selDevice"), sensororlist);
            lsd2.setVisible(true);
            if (lsd2.getSelection() == null) {
                return null;
            } else {
                return ((ListEntry) lsd2.getSelection()).getValue();
            }
        } else {
            return ((ListEntry) sensororlist.firstElement()).getValue();
        }
    }

    /**
	 * Add new device to installation model.
	 * @param finder
	 * @param dialog
	 * @return the actuator id in the installation model for the new actuator.
	 */
    private String addNewDevice(EIBDeviceConfigurator finder, NewEndDeviceDialog dialog, String enddeviceID) {
        EIBDevicesDataModel devmodel = (EIBDevicesDataModel) this.project.getApplication().getBusDeviceDataModel("EIB");
        InstallationModel imodel = this.project.getInstallationModel();
        Vector sensors = new Vector();
        sensors = finder.findNewSensors(enddeviceID);
        if (sensors.isEmpty()) {
            JOptionPane.showMessageDialog(null, locale.getString("mess.noDeviceFound"));
            return null;
        }
        Vector sensorlist = new Vector();
        for (Enumeration e = sensors.elements(); e.hasMoreElements(); ) {
            String id = (String) e.nextElement();
            if (DEBUG) {
                logger.debug(devmodel.getName(id));
            }
            String text = devmodel.getManufacturerName(id) + " - " + devmodel.getName(id);
            ListEntry le = new ListEntry(text, id);
            sensorlist.addElement(le);
        }
        ListSelectorDialog lsd2 = new ListSelectorDialog(new Frame(), locale.getString("tit.selDevice"), sensorlist);
        lsd2.setVisible(true);
        if (lsd2.getSelection() == null) {
            return null;
        }
        String name = JOptionPane.showInputDialog(null, locale.getString("mess.inputname"), locale.getString("tit.input"), JOptionPane.WARNING_MESSAGE);
        if (name == null) {
            return null;
        }
        if (name.equals("")) {
            name = locale.getString("noname");
        }
        String pastring = "";
        EIBPhaddress pad;
        while (true) {
            pastring = JOptionPane.showInputDialog(null, locale.getString("mess.inputPhAddr"));
            if (pastring == null) {
                return null;
            }
            try {
                pad = new EIBPhaddress(pastring);
                if (imodel.isEIBPhAddressInUse(pad)) {
                    JOptionPane.showMessageDialog(null, locale.getString("mess.phAddrInUse"));
                } else {
                    break;
                }
            } catch (EIBAddressFormatException afe) {
                JOptionPane.showMessageDialog(null, locale.getString("mess.wrongPhAddrFormat"));
            }
        }
        String dID = ((ListEntry) lsd2.getSelection()).getValue();
        String sensorID = imodel.addSensor(name);
        imodel.setProperty(sensorID, "device-name", devmodel.getName(dID));
        imodel.setProperty(sensorID, "device-id", dID);
        imodel.setProperty(sensorID, "device-state", "unprogrammed");
        imodel.setProperty(sensorID, "manufacturer", devmodel.getManufacturerName(dID));
        imodel.setProperty(sensorID, "bussystem", "EIB");
        imodel.setProperty(sensorID, "installation-location", dialog.getInstallationLocation());
        imodel.setProperty(sensorID, "eib-physical-address", pad.toString());
        logger.debug("eib device id: " + dID);
        Vector fgroups = (Vector) devmodel.getFunctionGroupIDs(dID);
        for (Enumeration e = fgroups.elements(); e.hasMoreElements(); ) {
            String devfuncgroupID = (String) e.nextElement();
            String fgid = imodel.addFunctionGroup(sensorID);
            Vector funcs = (Vector) devmodel.getFunctionIDs(devfuncgroupID);
            for (Enumeration f = funcs.elements(); f.hasMoreElements(); ) {
                String devfuncID = (String) f.nextElement();
                String fid = imodel.addFunction(fgid);
                imodel.setName(fid, devmodel.getName(devfuncID));
                Node source = devmodel.getDataRootNode(devfuncID);
                Node dest = imodel.getDataRootNode(fid);
                imodel.writeDOMNodeValue(dest, new StringTokenizer("type", "/"), devmodel.readDOMNodeValue(source, new StringTokenizer("type", "/")));
                imodel.writeDOMNodeValue(dest, new StringTokenizer("eis-type", "/"), devmodel.readDOMNodeValue(source, new StringTokenizer("eis-type", "/")));
                imodel.writeDOMNodeValue(dest, new StringTokenizer("com-object", "/"), devmodel.readDOMNodeValue(source, new StringTokenizer("com-object", "/")));
                imodel.writeDOMNodeValue(dest, new StringTokenizer("devmodelid", "/"), devfuncID);
                imodel.writeDOMNodeValue(dest, new StringTokenizer("state", "/"), "unused");
            }
        }
        ArchitecturalDataModel amodel = this.project.getArchitecturalDataModel();
        amodel.addBusDevice(finder.getInstallationLocationID(), sensorID);
        logger.debug("SensorID: " + sensorID);
        return sensorID;
    }

    /**
	 * @see basys.client.commands.Command#unexecute()
	 */
    public void unexecute() {
    }

    /**
	 * @see java.lang.Object#clone()
	 */
    public Object clone() {
        return null;
    }
}
