package ces.platform.infoplat.core.dao;

import java.util.*;
import java.text.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ListIterator;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.*;
import ces.coral.file.*;
import ces.platform.infoplat.core.base.*;
import ces.platform.infoplat.utils.Function;

public class ChannelScheduleDao extends BaseDAO {

    public ChannelScheduleDao() {
    }

    /**
	* ��add��update��ʱ����Ҫд�����ļ� 
	* @param strTime	ʱ����� begin1:end1-begin2:end2
	* @param channelPath	Ƶ��Path
	*/
    public void writeFile(String strTime, String channelPath) throws Exception {
        org.jdom.Document doc = null;
        Element element = null;
        SAXBuilder builder = new SAXBuilder();
        doc = builder.build(CesGlobals.getCesHome() + "/platform.xml");
        element = doc.getRootElement();
        element = element.getChild("platform");
        element = element.getChild("infoplat");
        element = element.getChild("channel-schedule");
        ListIterator lIte = element.getChildren().listIterator();
        boolean Found = false;
        for (int i = 0; lIte.hasNext(); i++) {
            Element eltTemp = (Element) lIte.next();
            if (eltTemp.getAttribute("channel").getValue().trim().equals(channelPath)) {
                eltTemp.setAttribute("time", strTime);
                eltTemp.setAttribute("channel", channelPath);
                Found = true;
                break;
            }
        }
        if (!Found) {
            Element eltChannel = new Element("schedule");
            eltChannel.setAttribute("time", strTime);
            eltChannel.setAttribute("channel", channelPath);
            element.addContent(eltChannel);
        }
        File outfile = new File(CesGlobals.getCesHome() + "/platform.xml");
        FileOutputStream outStream = new FileOutputStream(outfile);
        XMLOutputter fmt = new XMLOutputter();
        fmt.setEncoding("gb2312");
        fmt.output(doc, outStream);
        outStream.close();
    }

    /**
	* ��add��update��ʱ����Ҫд�����ļ� 
	* @param channelPath	Ƶ��Path
	*/
    public String[][] getChannelScheduleTime(String channelPath) throws Exception {
        readFile();
        String[][] aReturn = (String[][]) htChanTimes.get(channelPath);
        return aReturn;
    }

    /**
	* �ж�Ƶ����ʱ�Ƿ���� 
	* @param channelPath	Ƶ��Path
	* @return true���� false������
	*/
    public boolean isAvilible(String channelPath) {
        boolean avilible = true;
        try {
            String nowTime = Function.getSysTime().toString().substring(11, 16);
            SimpleDateFormat formatter1 = new SimpleDateFormat("H':'m");
            Date nowDate = formatter1.parse(nowTime);
            Date begin, end;
            int i;
            String[][] times = getChannelScheduleTime(channelPath);
            if (null != times && times.length > 0) {
                for (i = 0; i < times.length; i++) {
                    begin = null;
                    end = null;
                    if (!"".equals(times[i][0])) {
                        if (times[i][0].indexOf(":") == -1) times[i][0] = times[i][0] + ":00";
                        begin = formatter1.parse(times[i][0]);
                    }
                    if (!"".equals(times[i][1])) {
                        if (times[i][1].indexOf(":") == -1) times[i][1] = times[i][1] + ":00";
                        end = formatter1.parse(times[i][1]);
                    }
                    if ((null == begin && null == end) || (nowDate.after(begin) && null == end) || (nowDate.after(begin) && nowDate.before(end)) || (null == begin && nowDate.before(end))) {
                        avilible = false;
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            return avilible;
        }
    }

    /**
	* ��add��update��ʱ����Ҫд�����ļ� 
	* @param channelPath	Ƶ��Path
	*/
    private void readFile() throws Exception {
        if (null != htChanTimes) return;
        htChanTimes = new Hashtable();
        Hashtable ht = ConfigInfo.getInstance().getChannelSchedule();
        String[] aTimes, aTime;
        String chanPath, times;
        ArrayList al;
        int i;
        if (null != ht && !ht.isEmpty()) {
            Iterator iter = ht.keySet().iterator();
            if (null != iter) {
                while (iter.hasNext()) {
                    chanPath = (String) iter.next();
                    times = (String) ht.get(chanPath);
                    al = new ArrayList();
                    aTimes = Function.stringToArray(times, ";");
                    if (null != aTimes && aTimes.length > 0) {
                        for (i = 0; i < aTimes.length; i++) {
                            if (aTimes[i].length() > 1) {
                                aTime = Function.stringToArray(aTimes[i], "-");
                                if (null != aTime && aTime.length == 2) {
                                    al.add(aTime);
                                }
                            }
                        }
                    }
                    if (al.size() > 0) {
                        htChanTimes.put(chanPath, (String[][]) al.toArray(new String[0][0]));
                    }
                }
            }
        }
    }

    private Hashtable htChanTimes = null;

    public static void main(String args[]) {
        try {
            ChannelScheduleDao v = new ChannelScheduleDao();
            String[][] aTemp = v.getChannelScheduleTime("00000007020180200902");
            if (aTemp != null && aTemp.length > 0) {
                for (int i = 0; i < aTemp.length; i++) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print(e);
        }
    }
}
