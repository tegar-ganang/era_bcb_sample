package fireteam.fxforms;

import fireteam.interfaces.AbstractCardEditor;
import fireteam.interfaces.Errors;
import fireteam.orb.beans.ObjPrivs;
import fireteam.orb.client.ClientMain;
import fireteam.orb.server.stub.FTDObject;
import fireteam.orb.util.ObjUtil;
import fireteam.print.SVGPrinter;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import org.omg.CORBA.Any;
import org.xml.sax.SAXException;

/**
 * Класс служит для обработки запросов формы DocsFx - работа с документами системы
 * @author Tolik1
 */
public class Docs {

    private final String WTYPE_PROCESS = "PROCESS", WTYPE_FILE = "FILE";

    private FTDObject m_curObject;

    private ActionListener m_event;

    private final JFrame m_svgFrame = new JFrame();

    private final SVGPrinter m_svgPrinter = new SVGPrinter();

    private final String m_sWaitType = ResourceBundle.getBundle(Docs.class.getName()).getString("WAIT_TYPE");

    private final Boolean m_boolWaitForUpdate;

    private final Boolean m_boolWaitForOpened;

    private final class Running implements Runnable {

        private String sExec;

        private File fp;

        private long modifyDate;

        private String sID;

        private String sVer;

        private String sExt;

        public Running(String sExec, File fp, long modifyDate, String sID, String sVer, String sExt) {
            this.sExec = sExec;
            this.fp = fp;
            this.modifyDate = modifyDate;
            this.sID = sID;
            this.sVer = sVer;
            this.sExt = sExt;
        }

        public void run() {
            Process psFile = null;
            try {
                psFile = Runtime.getRuntime().exec(sExec);
                fp.deleteOnExit();
                waitForUpdate(psFile, fp, modifyDate, sID, sVer, sExt);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void setActionListener(ActionListener lsnr) {
        m_event = lsnr;
    }

    public Docs(String sID) {
        m_curObject = null;
        m_boolWaitForUpdate = Boolean.valueOf(ResourceBundle.getBundle(Docs.class.getName()).getString("WAIT_FOR_UPDATE"));
        m_boolWaitForOpened = Boolean.valueOf(ResourceBundle.getBundle(Docs.class.getName()).getString("WAIT_FOR_OPENED"));
    }

    public Docs(FTDObject obj) {
        m_boolWaitForUpdate = Boolean.valueOf(ResourceBundle.getBundle(Docs.class.getName()).getString("WAIT_FOR_UPDATE"));
        m_boolWaitForOpened = Boolean.valueOf(ResourceBundle.getBundle(Docs.class.getName()).getString("WAIT_FOR_OPENED"));
        m_curObject = obj;
    }

    /**
	 * Вызыватся при смене текущей папки
	 * @param obj - новая папка
	 */
    public void putObj(FTDObject obj) {
        m_curObject = obj;
        if (m_event != null) m_event.actionPerformed(new ActionEvent(this, 0, "putObj"));
    }

    /**
	 * Возвращает список документов в текущей папке
	 * @return список джокументов
	 */
    public HashMap[] getDocList() {
        try {
            Any param = ClientMain.getInstance().createAny();
            param.insert_wstring(m_curObject.ID);
            param = ClientMain.getInstance().sessionRequest("getDocList", param);
            return (HashMap[]) param.extract_Value();
        } catch (Exception e) {
            Errors.showError(e);
        }
        return new HashMap[0];
    }

    /**
	 * Возаращает список версий документа
	 * @param sID	- Идентификатор документа
	 * @return список версий
	 */
    public HashMap[] getDocVersions(String sID) {
        try {
            HashMap val = new HashMap();
            val.put("ID", sID);
            val.put("ISCARD", false);
            Any param = ClientMain.getInstance().createAny();
            param.insert_Value(val);
            param = ClientMain.getInstance().sessionRequest("getDocVer", param);
            return (HashMap[]) param.extract_Value();
        } catch (Exception e) {
            Errors.showError(e);
        }
        return new HashMap[0];
    }

    /**
	 * Возаращает список версий документа
	 * @param sID	- Идентификатор документа
	 * @return список версий
	 */
    public HashMap[] getDocCardVersions(String sID) {
        try {
            HashMap val = new HashMap();
            val.put("ID", sID);
            val.put("ISCARD", true);
            Any param = ClientMain.getInstance().createAny();
            param.insert_Value(val);
            param = ClientMain.getInstance().sessionRequest("getDocVer", param);
            return (HashMap[]) param.extract_Value();
        } catch (Exception e) {
            Errors.showError(e);
        }
        return new HashMap[0];
    }

    private void setupPrinter(byte[] data, HashMap attrs) throws IOException, ParserConfigurationException, ParserConfigurationException, SAXException {
        ByteArrayInputStream is = new ByteArrayInputStream(ObjUtil.deCompress(data));
        m_svgPrinter.getReader().readSVG(is);
        is.close();
        m_svgPrinter.clearVariables();
        m_svgPrinter.addVariable(0, "DUTY", attrs.get("DUTY"));
        m_svgPrinter.addVariable(0, "NAME", attrs.get("USR"));
        HashMap other[] = (HashMap[]) attrs.get("ATTR");
        for (HashMap o : other) {
            m_svgPrinter.addVariable(0, (String) o.get("NAME"), o.get("VALUE"));
        }
        m_svgPrinter.setPage(0);
    }

    /**
	 * Показывает документ 
	 * @param sID		- Идентификатор документа
	 * @param sVersion	- Версия документа
	 */
    public void showDocument(String sID, String sVersion, String sName) {
        try {
            Any param = ClientMain.getInstance().createAny();
            HashMap map = new HashMap();
            map.put("ID", sID);
            map.put("VER", sVersion);
            param.insert_Value(map);
            param = ClientMain.getInstance().sessionRequest("getDocView", param);
            HashMap ret = (HashMap) param.extract_Value();
            int iTempType = (Integer) ret.get("TTYPE");
            boolean bEditable = (Integer) ret.get("EDIT") == 1 ? true : false;
            String sExt = (String) ret.get("EXT");
            switch(iTempType) {
                case 1:
                    setupPrinter((byte[]) ret.get("DATA"), (HashMap) ret.get("ATTR"));
                    m_svgFrame.setContentPane(m_svgPrinter.getReader().getMainPanel());
                    m_svgFrame.setTitle(sName);
                    if (!m_svgFrame.isVisible()) {
                        m_svgFrame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
                        m_svgFrame.setLocation(0, 0);
                        m_svgFrame.setVisible(true);
                    } else {
                        if (m_svgFrame.getState() == JFrame.ICONIFIED) m_svgFrame.setState(JFrame.NORMAL); else m_svgFrame.toFront();
                    }
                    break;
                default:
                    showContent(sID, sVersion, (byte[]) ret.get("DATA"), sExt, sName, bEditable);
                    break;
            }
        } catch (Exception e) {
            Errors.showError(e);
        }
    }

    private String updateFile(String sID, String sVersion, String sExtension, File fp) {
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(fp);
            byte[] bFileData = new byte[(int) fi.getChannel().size()];
            fi.read(bFileData);
            HashMap par = new HashMap();
            par.put("ID", sID);
            par.put("VER", sVersion);
            par.put("EXT", sExtension);
            par.put("DATA", ObjUtil.compress(bFileData));
            Any param = ClientMain.getInstance().createAny();
            param.insert_Value(par);
            param = ClientMain.getInstance().sessionRequest("setDocFile", param);
            return param.extract_wstring();
        } catch (Exception ex) {
            Errors.showError(ex);
        } finally {
            try {
                fi.close();
            } catch (IOException ex) {
                Errors.showError(ex);
            }
        }
        return null;
    }

    private void waitForUpdate(Process psFile, File fp, long modifyDate, String sID, String sVer, String sExt) throws InterruptedException, IOException {
        if (m_sWaitType.equalsIgnoreCase(WTYPE_PROCESS)) psFile.waitFor();
        if (m_sWaitType.equalsIgnoreCase(WTYPE_FILE)) {
            while (true) {
                RandomAccessFile fi = null;
                try {
                    fi = new RandomAccessFile(fp, "rw");
                    FileChannel fc = fi.getChannel();
                    if (fc != null) {
                        FileLock flock = fc.tryLock();
                        if (flock != null) {
                            flock.release();
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fi != null) fi.close();
                }
            }
        }
        long newModifyDate = fp.lastModified();
        if (newModifyDate > modifyDate) {
            updateFile(sID, sVer, sExt, fp);
        }
    }

    private void showContent(String sID, String sVer, byte[] Content, String sExt, String sName, boolean bEditable) {
        try {
            File fp = File.createTempFile(sName, sExt);
            FileOutputStream fo = new FileOutputStream(fp);
            fo.write(Content);
            fo.close();
            long modifyDate = fp.lastModified();
            if (!bEditable) JOptionPane.showMessageDialog(ClientMain.getInstance(), ResourceBundle.getBundle(Docs.class.getName()).getString("READ_ONLY"));
            String sExec = ResourceBundle.getBundle(getClass().getName()).getString("CMD").replace("%S", fp.getPath());
            if (bEditable) {
                if (m_boolWaitForUpdate) {
                    Process psFile = Runtime.getRuntime().exec(sExec);
                    fp.deleteOnExit();
                    waitForUpdate(psFile, fp, modifyDate, sID, sVer, sExt);
                } else {
                    new Thread(new Running(sExec, fp, modifyDate, sID, sVer, sExt)).start();
                }
            } else {
                fp.setReadOnly();
                Process psFile = Runtime.getRuntime().exec(sExec);
                fp.deleteOnExit();
                if (m_boolWaitForOpened) psFile.waitFor();
            }
        } catch (Exception e) {
            Errors.showError(e);
        }
    }

    public void newDocument() {
        try {
            DocCard ae = new DocCard(null, "1", getClass().getMethod("update"), this);
            ae.setParentID(m_curObject.ID);
            ae.showCardDialog();
        } catch (Exception ex) {
            Errors.showError(ex);
        }
    }

    public void showDocumentCard(String sID, String sVersion, String sName) {
        try {
            Any param = ClientMain.getInstance().createAny();
            HashMap map = new HashMap();
            map.put("ID", sID);
            param.insert_Value(map);
            param = ClientMain.getInstance().sessionRequest("getDocForm", param);
            HashMap ret = (HashMap) param.extract_Value();
            String sForm = (String) ret.get("FORM");
            if (sForm != null) {
                Class cl = Class.forName("fireteam.fxdialogs." + sForm);
                if (cl != null) {
                    Constructor ctr = cl.getConstructor(String.class, String.class, Method.class, Object.class);
                    AbstractCardEditor ae = (AbstractCardEditor) ctr.newInstance(sID, sVersion, getClass().getMethod("update"), this);
                    ae.showCardDialog();
                }
            } else {
                DocCard ae = new DocCard(sID, sVersion, getClass().getMethod("update"), this);
                ae.showCardDialog();
            }
        } catch (Exception e) {
            Errors.showError(e);
        }
    }

    public void update() {
        if (m_event != null) m_event.actionPerformed(new ActionEvent(this, 0, "putObj"));
    }

    /**
	 * Функция вызывает форму для редактирования привилегий к типу
	 * @param sID
	 */
    public void editPrivs(String sID) {
        FTDObject obj = new FTDObject();
        obj.ID = sID;
        obj.Type = "DOC";
        ObjPrivs.showObjPrivs(obj);
    }

    /**
	 * Подписание документа
	 * @param sID			- Идентификатор документа
	 * @param sVersion		- Версия документа
	 * @param sName			- Название документа
	 */
    public boolean signDocument(String sID, String sVersion, String sName) {
        try {
            Any param = ClientMain.getInstance().createAny();
            HashMap map = new HashMap();
            map.put("ID", sID);
            map.put("VER", sVersion);
            param.insert_Value(map);
            param = ClientMain.getInstance().sessionRequest("getDocForSign", param);
            HashMap ret = (HashMap) param.extract_Value();
            int iTempType = (Integer) ret.get("TTYPE");
            switch(iTempType) {
                case -1:
                    byte[] dataToSign = (byte[]) ret.get("DATA");
                    byte[] sign = ClientMain.getInstance().signData(dataToSign);
                    Date tstamp = (Date) ret.get("DATE");
                    map.put("DATA", dataToSign);
                    map.put("SIGN", sign);
                    map.put("DATE", tstamp);
                    param.insert_Value(map);
                    break;
                default:
                    HashMap[] attrs = (HashMap[]) ((HashMap) ret.get("ATTR")).get("ATTR");
                    param.insert_Value(attrs);
                    param = ClientMain.getInstance().sessionRequest("getAttributesData", param);
                    dataToSign = (byte[]) param.extract_Value();
                    sign = ClientMain.getInstance().signData(dataToSign);
                    map.put("ATTRS", attrs);
                    map.put("SIGN", sign);
                    param.insert_Value(map);
                    break;
            }
            param = ClientMain.getInstance().sessionRequest("signDoc", param);
            return true;
        } catch (Exception e) {
            Errors.showError(e);
        }
        return false;
    }

    public boolean checDocSigns(String sID, String sVersion) {
        try {
            Any param = ClientMain.getInstance().createAny();
            HashMap map = new HashMap();
            map.put("ID", sID);
            map.put("VER", sVersion);
            param.insert_Value(map);
            param = ClientMain.getInstance().sessionRequest("checkDocSigns", param);
            return true;
        } catch (Exception e) {
            Errors.showError(e);
        }
        return false;
    }

    public int getTemplateType(String sID) {
        try {
            HashMap val = new HashMap();
            val.put("ID", sID);
            Any param = ClientMain.getInstance().createAny();
            param.insert_Value(val);
            param = ClientMain.getInstance().sessionRequest("getTemplateType", param);
            HashMap map = (HashMap) param.extract_Value();
            return (Integer) map.get("TTYPE");
        } catch (Exception e) {
            Errors.showError(e);
        }
        return -1;
    }

    public void newVersion(String sID, String sName) {
        try {
            int iTempType = getTemplateType(sID);
            HashMap[] vers = getDocVersions(sID);
            switch(iTempType) {
                case -1:
                    Any param = ClientMain.getInstance().createAny();
                    HashMap map = new HashMap();
                    map.put("ID", sID);
                    map.put("VER", (String) vers[vers.length - 1].get("VER"));
                    param.insert_Value(map);
                    param = ClientMain.getInstance().sessionRequest("getDocView", param);
                    HashMap ret = (HashMap) param.extract_Value();
                    String sMaxVersion = String.valueOf(Integer.valueOf((String) vers[vers.length - 1].get("VER")) + 1);
                    showContent(sID, sMaxVersion, (byte[]) ret.get("DATA"), (String) ret.get("EXT"), sName, true);
                    break;
                default:
                    showDocumentCard(sID, "N" + (String) vers[vers.length - 1].get("VER"), sName);
                    break;
            }
        } catch (Exception e) {
            Errors.showError(e);
        }
    }
}
