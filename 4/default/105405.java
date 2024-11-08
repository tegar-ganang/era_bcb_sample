import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.component.*;
import net.rim.blackberry.api.browser.*;
import java.util.Calendar;
import java.util.Date;

public class ociScreen extends MainScreen {

    private RadioStatusListener radioStatusListener = null;

    private GPS doGPS = null;

    private LabelField lf = new LabelField("Click on the Start GPS menu item to start logging locations");

    public LabelField latlon = new LabelField("");

    private LabelField signal = new LabelField("");

    private LabelField update = new LabelField("");

    private LabelField lf2 = new LabelField("");

    public ociScreen() {
        super();
        LabelField title = new LabelField("OpenCellID 4 BlackBerry", LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(title);
        doGPS = new GPS();
        radioStatusListener = new RadioStatusListener() {

            public void signalLevel(int level) {
                onRadioStatusChanged();
            }

            public void networkStarted(int networkId, int service) {
                onRadioStatusChanged();
            }

            public void baseStationChange() {
                onRadioStatusChanged();
            }

            public void radioTurnedOff() {
                onRadioStatusChanged();
            }

            public void pdpStateChange(int apn, int state, int cause) {
                onRadioStatusChanged();
            }

            public void networkStateChange(int state) {
                onRadioStatusChanged();
            }

            public void networkScanComplete(boolean success) {
                onRadioStatusChanged();
            }

            public void networkServiceChange(int networkId, int service) {
                onRadioStatusChanged();
            }
        };
        Application.getApplication().addRadioListener(radioStatusListener);
        add(lf);
        add(latlon);
        add(signal);
        add(update);
        add(lf2);
    }

    public boolean onClose() {
        System.exit(0);
        return (true);
    }

    MenuItem gpsRunMenu = new MenuItem("Start GPS", 30, 30) {

        public void run() {
            if (gpsRunMenu.toString().equals("Start GPS")) {
                doGPS.startLocationUpdate();
                gpsRunMenu.setText("Stop GPS");
                lf.setText("Starting GPS, please wait while a lock is achieved.");
            } else {
                doGPS.stopLocationUpdate();
                gpsRunMenu.setText("Start GPS");
                doGPS.latitude = -999.0;
                doGPS.longitude = -999.0;
                lf.setText("Click on the Start GPS menu item to start logging locations");
            }
        }
    };

    MenuItem websiteMenu = new MenuItem("OCI4BB Homepage", 40, 40) {

        public void run() {
            Browser.getDefaultSession().displayPage("http://oci4bb.sourceforge.net");
        }
    };

    MenuItem aboutMenu = new MenuItem("About", 50, 50) {

        public void run() {
            Browser.getDefaultSession().displayPage("http://www.opencellid.org");
        }
    };

    MenuItem quitMenu = new MenuItem("Quit", 60, 60) {

        public void run() {
            onClose();
        }
    };

    protected void makeMenu(Menu menu, int instance) {
        menu.add(gpsRunMenu);
        menu.add(websiteMenu);
        menu.add(aboutMenu);
        menu.add(quitMenu);
    }

    protected void onRadioStatusChanged() {
        signal.setText("Signal: " + RadioInfo.getSignalLevel());
        if (doGPS.latitude < -90 || doGPS.latitude > 90 || doGPS.longitude < -180 || doGPS.longitude > 180 || doGPS.lastupdate < ((System.currentTimeMillis() / 1000) - 120) || !ServerCommunication.isConnectionAvailable()) {
            lf2.setText("Still waiting for lock...");
            return;
        }
        String string = "http://www.opencellid.org/measure/add";
        string += "?key=9c46be0fe0d6b163f75b22a4acbc6a50";
        string += "&userid=" + ociApp.getkey();
        string += "&lat=" + doGPS.latitude;
        string += "&lon=" + doGPS.longitude;
        string += "&signal=" + RadioInfo.getSignalLevel();
        long timenow = System.currentTimeMillis();
        string += "&measured_at=" + dateToString(timenow) + "%20" + timeToString(timenow);
        try {
            if (isNetworkType(RadioInfo.NETWORK_CDMA)) {
                string += "&mcc=" + Integer.toHexString(CDMAInfo.getCellInfo().getSID());
                string += "&mnc=" + Integer.toHexString(CDMAInfo.getCellInfo().getNID());
                string += "&lac=";
                string += "&cellid=" + CDMAInfo.getCellInfo().getBID();
                string += "&extraInfo=" + networktypes() + "-" + CDMAInfo.getChannelNumber();
                lf.setText("C=" + Integer.toHexString(CDMAInfo.getCellInfo().getSID()) + "," + Integer.toHexString(CDMAInfo.getCellInfo().getNID()) + "," + Integer.toHexString(CDMAInfo.getCellInfo().getBID()) + "," + "N/A" + ",S=" + RadioInfo.getSignalLevel() + ",T=" + networktypes() + ",C=" + CDMAInfo.getChannelNumber());
            } else if (isNetworkType(RadioInfo.NETWORK_GPRS)) {
                string += "&mcc=" + Integer.toHexString(GPRSInfo.getCellInfo().getMCC());
                string += "&mnc=" + RadioInfo.getMNC(RadioInfo.getCurrentNetworkIndex());
                string += "&lac=" + GPRSInfo.getCellInfo().getLAC();
                string += "&cellid=" + GPRSInfo.getCellInfo().getCellId();
                string += "&extraInfo=" + networktypes() + "-" + GPRSInfo.getCellInfo().getARFCN();
                lf.setText("C=" + Integer.toHexString(GPRSInfo.getCellInfo().getMCC()) + "," + Integer.toHexString(GPRSInfo.getCellInfo().getMCC()) + "," + GPRSInfo.getCellInfo().getCellId() + "," + GPRSInfo.getCellInfo().getLAC() + ",S=" + RadioInfo.getSignalLevel() + ",T=" + networktypes() + ",C=" + GPRSInfo.getCellInfo().getARFCN());
            }
        } catch (Exception e) {
        }
        update.setText(dateToString(timenow) + " " + timeToString(timenow));
        System.out.println("url: " + string);
        if (!DeviceInfo.isSimulator()) {
            String reply = ServerCommunication.http_get(string);
            lf2.setText(reply);
        } else {
            lf2.setText("Simulator Session");
        }
    }

    public static String timeToString(long date) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(date));
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);
        String t = (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
        return t;
    }

    public static String dateToString(long date) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(date));
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DATE);
        String t = (y < 10 ? "0" : "") + y + "-" + (m < 10 ? "0" : "") + m + "-" + (d < 10 ? "0" : "") + d;
        return t;
    }

    private boolean isNetworkType(final int type) {
        return (RadioInfo.getNetworkType() & type) == type;
    }

    private String networktypes() {
        if (isNetworkType(RadioInfo.NETWORK_UMTS)) return "3G";
        if (isNetworkType(RadioInfo.NETWORK_CDMA)) return "CDMA";
        if (isNetworkType(RadioInfo.NETWORK_GPRS)) return "GPRS";
        if (isNetworkType(RadioInfo.NETWORK_IDEN)) return "IDEN";
        if (isNetworkType(RadioInfo.NETWORK_802_11)) return "WiFi";
        return "None";
    }
}
