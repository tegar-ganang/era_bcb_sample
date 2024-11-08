package playground.ou.intersectiongroup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;
import playground.ou.signallight.SignalLight;

public class GreentimeTXTtoXML {

    static final int PERIOD = 30 * 60 - 1;

    private BufferedWriter out;

    private BufferedReader infile;

    private Map<String, SignalLight> linksgreen = new TreeMap<String, SignalLight>();

    private Map<String, String> links = new TreeMap<String, String>();

    public GreentimeTXTtoXML(Map<String, String> links) {
        this.links = links;
    }

    public void convertTXTtoXML(String txtfilename, String xmlfilename, int simu_endtime) {
        initialReader(txtfilename);
        initialWriter(xmlfilename);
        readandwriteFile(txtfilename, simu_endtime);
        endReader();
        endWriter();
    }

    protected void readandwriteFile(String txtfilename, int simu_endtime) {
        readFiletogreentime();
        for (String linkId : this.links.keySet()) {
            writexmlbody(linkId, simu_endtime);
        }
    }

    private void writexmlbody(String linkId, int simu_endtime) {
        Map<Integer, String[]> onelink = new TreeMap<Integer, String[]>(this.linksgreen.get(linkId).getGreentime());
        String starttime = null;
        for (int x : onelink.keySet()) {
            starttime = onelink.get(x)[1];
            break;
        }
        onelink = calcu_PERIOD_avgvalue(onelink, simu_endtime);
        writelink(onelink, linkId, starttime, simu_endtime);
    }

    private Map<Integer, String[]> calcu_PERIOD_avgvalue(Map<Integer, String[]> originlink, int simu_endtime) {
        Map<Integer, String[]> des_link = new TreeMap<Integer, String[]>();
        String[] tempgreen = new String[4];
        boolean periodfirst = true;
        double totalvalue = 0.0;
        double totalduration = 0.0;
        for (int x : originlink.keySet()) {
            String[] greens = originlink.get(x);
            if (periodfirst) tempgreen = greens;
            if ((Integer.parseInt(greens[2]) - Integer.parseInt(tempgreen[1]) > PERIOD) || (Integer.parseInt(greens[2]) >= simu_endtime)) {
                if ((Integer.parseInt(greens[2]) - Integer.parseInt(greens[1]) < PERIOD) || periodfirst) {
                    tempgreen[2] = greens[2];
                    if (Integer.parseInt(tempgreen[2]) >= simu_endtime) tempgreen[2] = Integer.toString(simu_endtime);
                    des_link.put(x, tempgreen);
                } else {
                    des_link.put((x - 1), tempgreen);
                    if (Integer.parseInt(greens[2]) >= simu_endtime) greens[2] = Integer.toString(simu_endtime);
                    des_link.put(x, greens);
                }
                periodfirst = true;
                totalvalue = 0;
                totalduration = 0;
            } else {
                periodfirst = false;
                totalvalue += (Integer.parseInt(greens[2]) - Integer.parseInt(greens[1])) * Double.parseDouble(greens[3]);
                totalduration += Integer.parseInt(greens[2]) - Integer.parseInt(greens[1]);
                tempgreen[2] = greens[2];
                tempgreen[3] = Double.toString(totalvalue / totalduration);
            }
        }
        return des_link;
    }

    private Map<Integer, String[]> combinecontinue_samevalue(Map<Integer, String[]> originlink, int simu_endtime) {
        Map<Integer, String[]> des_link = new TreeMap<Integer, String[]>();
        String[] tempgreen = new String[4];
        boolean first = true;
        String starttime = null;
        int index = 0;
        for (int x : originlink.keySet()) {
            index = x;
            String[] greens = originlink.get(x);
            if (first) {
                tempgreen = greens;
                first = false;
                starttime = tempgreen[1];
            } else {
                if ((tempgreen[3].equalsIgnoreCase(greens[3])) && (Integer.parseInt(greens[1]) - Integer.parseInt(tempgreen[2])) == 1) {
                    tempgreen = greens;
                } else {
                    tempgreen[1] = starttime;
                    starttime = greens[1];
                    if (Integer.parseInt(tempgreen[2]) > simu_endtime - 2) tempgreen[2] = Integer.toString(simu_endtime);
                    des_link.put(x, tempgreen);
                    tempgreen = greens;
                }
            }
        }
        tempgreen[1] = starttime;
        if (Integer.parseInt(tempgreen[2]) > simu_endtime - 2) tempgreen[2] = Integer.toString(simu_endtime);
        des_link.put(index, tempgreen);
        return des_link;
    }

    private void writelink(Map<Integer, String[]> onelink, String linkId, String starttime, int simu_endtime) {
        writelinkhead(linkId);
        for (int x : onelink.keySet()) {
            writeline(onelink.get(x));
        }
        writelinktail();
    }

    private void writelinkhead(String linkId) {
        try {
            out.write("\t<linkgtfs id=\"" + linkId + "\" time_period=\"24:00:00\">\n");
        } catch (IOException e) {
            Gbl.errorMsg(e);
        }
    }

    private void writelinktail() {
        try {
            out.write("\t</linkgtfs>\n");
        } catch (IOException e) {
            Gbl.errorMsg(e);
        }
    }

    private void writeline(String[] greentime) {
        try {
            out.write("\t\t<gtf time=\"" + Time.writeTime(Integer.parseInt(greentime[1])) + "\" val=\"" + greentime[3] + "\"/>\n");
            out.write("\t\t<gtf time=\"" + Time.writeTime(Integer.parseInt(greentime[2])) + "\" val=\"" + greentime[3] + "\"/>\n");
        } catch (IOException e) {
            Gbl.errorMsg(e);
        }
    }

    protected void readFiletogreentime() {
        try {
            String line = infile.readLine();
            int index = 0;
            if (line != null && line.charAt(0) >= '0' && line.charAt(0) <= '9') {
                index = parseLinetogreentime(index, line);
            }
            while ((line = infile.readLine()) != null) {
                index = parseLinetogreentime(index, line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected int parseLinetogreentime(int id, final String line) {
        int index = id;
        String[] result = StringUtils.explode(line, '\t', 4);
        SignalLight signallight;
        String[] temp = new String[4];
        if (result.length == 4 && (links.containsKey(result[0]))) {
            if (linksgreen.get(result[0]) == null) {
                signallight = new SignalLight();
                index = 0;
                signallight.getGreentime().put(index, result);
            } else {
                signallight = linksgreen.get(result[0]);
                while (signallight.getGreentime().get(index) == null) index--;
                temp = signallight.getGreentime().get(index);
                if (temp[3].equalsIgnoreCase(result[3])) {
                    temp[2] = result[2];
                    signallight.getGreentime().put(index, temp);
                } else {
                    index++;
                    temp = result;
                    signallight.getGreentime().put(index, temp);
                }
            }
            linksgreen.put(result[0], signallight);
        }
        return index;
    }

    protected void initialReader(String txtfilename) {
        try {
            this.infile = IOUtils.getBufferedReader(txtfilename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void initialWriter(String xmlfilename) {
        try {
            out = new BufferedWriter(new FileWriter(xmlfilename));
        } catch (IOException e) {
            Gbl.errorMsg(e);
        }
        try {
            out.write("<greentimefractions desc=\"ou light signal optimisation\">\n");
        } catch (IOException e) {
            Gbl.errorMsg(e);
        }
    }

    protected void endReader() {
        try {
            this.infile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void endWriter() {
        try {
            out.write("</greentimefractions>\n");
        } catch (IOException e) {
            Gbl.errorMsg(e);
        }
        try {
            out.close();
        } catch (IOException e) {
            Gbl.errorMsg(e);
        }
    }
}
