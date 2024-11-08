package gov.sns.apps.tripviewer;

import gov.sns.services.tripmonitor.*;
import gov.sns.application.*;
import gov.sns.tools.bricks.WindowReference;
import gov.sns.tools.services.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.Timer;
import java.util.*;

/** Controller for the client that monitors the trip monitor services */
public class ServicesController {

    /** reference to the main window */
    protected final WindowReference MAIN_WINDOW_REFERENCE;

    /** list of service handlers */
    protected final List<ServiceHandler> SERVICE_HANDLERS;

    /** table of service handlers keyed by ID */
    protected final Map<String, ServiceHandler> SERVICE_HANDLER_MAP;

    /** table model of services */
    protected final ServicesTableModel SERVICES_TABLE_MODEL;

    /** list model of channel references for the selected monitor */
    protected final ChannelListModel CHANNEL_LIST_MODEL;

    /** table model of trip records */
    protected final TripsTableModel TRIPS_TABLE_MODEL;

    /** timer to synchronize the client with the selected service */
    protected final Timer SYNC_TIMER;

    /** handler of synchronization events between client and service */
    protected final ActionListener SYNC_HANDLER;

    /** selected service handler */
    protected volatile ServiceHandler _selectedServiceHandler;

    /** selected trip monitor name */
    protected volatile String _selectedTripMonitorID;

    /** Constructor */
    public ServicesController(final WindowReference mainWindowReference) {
        MAIN_WINDOW_REFERENCE = mainWindowReference;
        SERVICE_HANDLERS = new ArrayList<ServiceHandler>();
        SERVICE_HANDLER_MAP = new Hashtable<String, ServiceHandler>();
        SERVICES_TABLE_MODEL = new ServicesTableModel();
        CHANNEL_LIST_MODEL = new ChannelListModel();
        TRIPS_TABLE_MODEL = new TripsTableModel();
        SYNC_HANDLER = new SynchronizationHandler();
        SYNC_TIMER = new Timer(15000, SYNC_HANDLER);
        SYNC_TIMER.setRepeats(true);
        initializeViews();
        monitorTripService();
        SYNC_TIMER.start();
    }

    /** initialize views */
    protected void initializeViews() {
        final JTable servicesTable = (JTable) MAIN_WINDOW_REFERENCE.getView("ServicesTable");
        servicesTable.setModel(SERVICES_TABLE_MODEL);
        servicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        servicesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    final int selectedRow = servicesTable.getSelectedRow();
                    synchronized (SERVICE_HANDLERS) {
                        _selectedServiceHandler = selectedRow >= 0 && selectedRow < SERVICE_HANDLERS.size() ? SERVICE_HANDLERS.get(selectedRow) : null;
                    }
                    refreshForServiceHandlerSelection();
                }
            }
        });
        final JList monitorsList = (JList) MAIN_WINDOW_REFERENCE.getView("MonitorList");
        monitorsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        monitorsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    handleMonitorSelection(monitorsList);
                }
            }
        });
        final JList channelList = (JList) MAIN_WINDOW_REFERENCE.getView("ChannelList");
        channelList.setModel(CHANNEL_LIST_MODEL);
        final JTable tripsTable = (JTable) MAIN_WINDOW_REFERENCE.getView("TripRecordsBufferTable");
        tripsTable.setModel(TRIPS_TABLE_MODEL);
        final JButton publishButton = (JButton) MAIN_WINDOW_REFERENCE.getView("PublishButton");
        publishButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                publishBuffer();
            }
        });
    }

    /** handle monitor selection */
    protected void handleMonitorSelection(final JList monitorsList) {
        final Object selection = monitorsList.getSelectedValue();
        _selectedTripMonitorID = selection != null ? selection.toString() : null;
        refreshForMonitorSelection();
    }

    /** Setup service discovery so we can monitor Trip Monitor services on the local network that are either new or have quit. */
    public void monitorTripService() {
        ServiceDirectory.defaultDirectory().addServiceListener(TripMonitorPortal.class, new ServiceListener() {

            /**
			 * Handle a new service being added
			 * @param directory The service directory.
			 * @param serviceRef A reference to the new service.
			 */
            public void serviceAdded(final ServiceDirectory directory, final ServiceRef serviceRef) {
                final TripMonitorPortal proxy = directory.getProxy(TripMonitorPortal.class, serviceRef);
                final String ID = serviceRef.getRawName();
                System.out.println("\nhost:  " + proxy.getHostName() + ", launched:  " + proxy.getLaunchTime());
                synchronized (SERVICE_HANDLERS) {
                    final ServiceHandler serviceHandler = new ServiceHandler(proxy, ID);
                    SERVICE_HANDLERS.add(serviceHandler);
                    SERVICE_HANDLER_MAP.put(ID, serviceHandler);
                    SERVICES_TABLE_MODEL.setServiceHandlers(SERVICE_HANDLERS);
                    final JTable servicesTable = (JTable) MAIN_WINDOW_REFERENCE.getView("ServicesTable");
                    if (servicesTable.getSelectedRow() < 0 && SERVICE_HANDLERS.size() == 1) {
                        servicesTable.setRowSelectionInterval(0, 0);
                    }
                }
            }

            /**
			 * Handle a service being removed
			 * @param directory The service directory.
			 * @param name The unique name of the service.
			 */
            public void serviceRemoved(final ServiceDirectory directory, final String type, final String name) {
                synchronized (SERVICE_HANDLERS) {
                    final ServiceHandler serviceHandler = SERVICE_HANDLER_MAP.get(name);
                    if (serviceHandler != null) {
                        SERVICE_HANDLERS.remove(serviceHandler);
                        SERVICE_HANDLER_MAP.remove(name);
                        SERVICES_TABLE_MODEL.setServiceHandlers(SERVICE_HANDLERS);
                    }
                }
            }
        });
    }

    /** dump the services info to standard output */
    private void printServicesInfo(final TripMonitorPortal service) {
        System.out.println("Trip Monitors:  " + service.getTripMonitorNames());
        final Vector monitors = service.getTripMonitorNames();
        for (Object monitor : monitors) {
            System.out.println("Monitor: " + monitor + ", Enabled:  " + service.isEnabled(monitor.toString()));
            System.out.println("Monitor: " + monitor + ", Enabled:  " + "Trip Records:  " + service.getTripRecords(monitor.toString()));
            System.out.println("Monitor: " + monitor + ", Enabled:  " + "Channel Info:  " + service.getChannelInfo(monitor.toString()));
        }
    }

    /** get the main window */
    protected DefaultXalWindow getMainWindow() {
        return (DefaultXalWindow) MAIN_WINDOW_REFERENCE.getWindow();
    }

    /** publish the buffer to the database */
    protected void publishBuffer() {
        synchronized (SERVICE_HANDLERS) {
            final ServiceHandler serviceHandler = _selectedServiceHandler;
            if (serviceHandler != null) {
                try {
                    serviceHandler.publishBuffer();
                    refreshForMonitorSelection();
                } catch (Exception exception) {
                    getMainWindow().displayError("Publish Error", "Exception attempting to publish records to the database!", exception);
                }
            }
        }
    }

    /** refresh the views to account for the newly selected service handler */
    protected void refreshForServiceHandlerSelection() {
        final ServiceHandler serviceHandler = _selectedServiceHandler;
        final JList monitorsList = (JList) MAIN_WINDOW_REFERENCE.getView("MonitorList");
        final JButton publishButton = (JButton) MAIN_WINDOW_REFERENCE.getView("PublishButton");
        if (serviceHandler != null) {
            synchronizeWithSelectedServiceIfNeeded();
            final Vector monitorNames = serviceHandler.getTripMonitorNameVector();
            monitorsList.setListData(monitorNames);
        } else {
            monitorsList.setListData(new Vector());
        }
        publishButton.setEnabled(false);
        CHANNEL_LIST_MODEL.setChannelRefs(new ArrayList<ChannelRef>());
        TRIPS_TABLE_MODEL.setTripRecords(new ArrayList<TripRecord>());
    }

    /** refresh for the monitor selection */
    protected void refreshForMonitorSelection() {
        final ServiceHandler serviceHandler = _selectedServiceHandler;
        final String monitorID = _selectedTripMonitorID;
        final JButton publishButton = (JButton) MAIN_WINDOW_REFERENCE.getView("PublishButton");
        if (serviceHandler != null && monitorID != null) {
            CHANNEL_LIST_MODEL.setChannelRefs(serviceHandler.getChannelRefs(monitorID));
            TRIPS_TABLE_MODEL.setTripRecords(serviceHandler.getTripRecords(monitorID));
            publishButton.setEnabled(true);
        } else if (monitorID == null) {
            CHANNEL_LIST_MODEL.setChannelRefs(new ArrayList<ChannelRef>());
            TRIPS_TABLE_MODEL.setTripRecords(new ArrayList<TripRecord>());
            publishButton.setEnabled(false);
        }
    }

    /** synchronize the client from the selected service */
    protected void synchronizeWithSelectedServiceIfNeeded() {
        synchronized (SYNC_TIMER) {
            final ServiceHandler serviceHandler = _selectedServiceHandler;
            if (serviceHandler != null) {
                final double period = SYNC_TIMER.getDelay() / 1000.0;
                if (!serviceHandler.hasRefreshedInPeriod(period)) {
                    try {
                        serviceHandler.refresh();
                        refreshForMonitorSelection();
                    } catch (Exception exception) {
                        getMainWindow().displayError("Synchronization Error", "Exception attempting to synchronize with the remote service!", exception);
                    }
                }
            } else {
            }
        }
    }

    /** handle the client-server synchronization events */
    protected class SynchronizationHandler implements ActionListener {

        /** handle the synchronization event */
        public void actionPerformed(final ActionEvent event) {
            synchronizeWithSelectedServiceIfNeeded();
        }
    }
}
