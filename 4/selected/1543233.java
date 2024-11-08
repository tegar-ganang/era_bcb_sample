package net.sourceforge.jcoupling2.stresstest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.yawlfoundation.yawl.util.JDOMUtil;
import net.sourceforge.jcoupling2.persistence.DataMapper;
import net.sourceforge.jcoupling2.persistence.Message;

public class MessageData {

    DataMapper dMapper = null;

    private ArrayList<String> buffer = new ArrayList<String>();

    public MessageData() {
        dMapper = new DataMapper();
    }

    public ArrayList<String> getBuffer() {
        return buffer;
    }

    public String[] getChannels() {
        String[] channels = { "Random", "Channel1", "Channel2", "Channel3" };
        return channels;
    }

    public String getTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        GregorianCalendar cal = new GregorianCalendar();
        return sdf.format(new Date(cal.getTimeInMillis()));
    }

    public String[] getLocations() {
        String[] locations = { "Random", "PreOpHolding", "PACU", "ICU", "OR", "ER" };
        return locations;
    }

    public String[] getPatients() {
        String[] patients = { "Random", "PatientA", "PatientB", "PatientC", "PatientD" };
        return patients;
    }

    public String[] getStaffNames() {
        String[] staffnames = { "Random", "DoctorA", "DoctorB", "DoctorC", "DoctorD" };
        return staffnames;
    }

    public String[] getDevices() {
        String[] devices = { "Random", "HLM", "Cellsaver", "Microscope" };
        return devices;
    }

    public String[] getInformation() {
        String[] infos = { "Random", "infoA", "infoB", "infoC" };
        return infos;
    }

    public String[] getStaff() {
        String[] staff = { "Random", "nurse", "surgeon", "anesthetist" };
        return staff;
    }

    public String getRandomEntry(String[] array) {
        Random generator = new Random();
        int randomInt = 0;
        while (randomInt == 0) {
            randomInt = generator.nextInt(array.length);
        }
        return array[randomInt];
    }

    public String formatString(String text) {
        Element msgElement = JDOMUtil.stringToElement(text);
        XMLOutputter fmt = new XMLOutputter();
        fmt.setFormat(Format.getPrettyFormat());
        return fmt.outputString(msgElement);
    }

    public void unlearnMessage(String text) {
        for (int i = 0; i < buffer.size(); i++) {
            String msg = buffer.get(i);
            if (formatString(msg).equals(text)) {
                buffer.remove(i);
            }
        }
    }
}
