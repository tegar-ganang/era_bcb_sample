package fireteam.interfaces;

import fireteam.fxforms.DocType;
import fireteam.orb.client.ClientMain;
import fireteam.orb.server.stub.StandardException;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.omg.CORBA.Any;

/**
 *
 * @author Tolik1
 */
public abstract class AbstractCardEditor extends FxDialog {

    private Method m_method;

    private Object m_object;

    private String m_sID;

    private String m_sParentID;

    private String m_sVersion;

    private HashMap m_attrs[];

    private HashMap m_signs[];

    private String m_sName;

    private String m_sTypeName;

    private String m_sNumber;

    private String m_sTypeID;

    private String m_sDocFile;

    private ActionListener m_event = null;

    private DocType.ContentType m_contentType = DocType.ContentType.NONE;

    public void setDocFile(String sDocFile) {
        m_sDocFile = sDocFile;
    }

    public DocType.ContentType getContentType() {
        return m_contentType;
    }

    public void setParentID(String sParent) {
        m_sParentID = sParent;
    }

    public void setActionListener(ActionListener ev) {
        m_event = ev;
    }

    public void setNumber(String sNumber) {
        m_sNumber = sNumber;
    }

    public String getNumber() {
        return m_sNumber;
    }

    public String getTypeName() {
        return m_sTypeName;
    }

    public String getDocName() {
        return m_sName;
    }

    public void setDocName(String sName) {
        m_sName = sName;
    }

    public String geVersion() {
        return m_sVersion;
    }

    public void setVersion(String sVersion) {
        m_sVersion = sVersion;
    }

    public HashMap[] getSigns() {
        return m_signs;
    }

    public void setSigns(HashMap[] signs) {
        m_signs = signs;
    }

    public HashMap[] getAttrs() {
        return m_attrs;
    }

    public void setAttrs(HashMap[] attrs) {
        m_attrs = attrs;
    }

    /**
	 * Возвращает информацию о типе документа, по его идентификатору
	 * @param sTypeID	- Тип документа
	 * @param changeAttr	- флаг джля определения аттирутов, если установлен, 
	 *			  то атрибуты установленные в документе и совпадающие 
	 *			  с новыми сохраняют свои значения
	 */
    public void getDocTypeInfo(String sTypeID, boolean changeAttr) {
        try {
            Any param = ClientMain.getInstance().createAny();
            param.insert_string(sTypeID);
            Any ret = ClientMain.getInstance().sessionRequest("getDocTypeInfo", param);
            HashMap map = (HashMap) ret.extract_Value();
            int iTmpType = Integer.valueOf((String) (map.get("TEMPTYPE") != null ? map.get("TEMPTYPE") : "-1"));
            switch(iTmpType) {
                case 1:
                    m_contentType = DocType.ContentType.SVG;
                    break;
                case 2:
                    m_contentType = DocType.ContentType.PDF;
                    break;
                default:
                    m_contentType = DocType.ContentType.NONE;
            }
            m_sTypeName = (String) map.get("CTYPNAME");
            if (!changeAttr) return;
            HashMap attrs[] = (HashMap[]) map.get("ATTR");
            for (HashMap v : attrs) {
                String sName = (String) v.get("NAME");
                if (m_attrs != null) {
                    for (HashMap atr : m_attrs) {
                        v.remove("DESC");
                        v.remove("TYPE");
                        if (sName.equals((String) atr.get("NAME"))) {
                            v.put("VALUE", atr.get("VALUE"));
                        }
                        atr.clear();
                    }
                }
            }
            m_attrs = attrs;
            m_sNumber = (String) map.get("CAUTONUM");
            m_sTypeID = sTypeID;
        } catch (Exception ex) {
            Errors.showError(ex);
        }
    }

    /**
	 * Измнение типа документа - диалог, выдает список типов документа
	 */
    public void changeDocTypes() {
        try {
            Any param = ClientMain.getInstance().createAny();
            param = ClientMain.getInstance().sessionRequest("getDocTypes", param);
            HashMap[] map = (HashMap[]) param.extract_Value();
            if (map.length == 1) getDocTypeInfo((String) map[0].get("ID"), true); else {
                HashMap[] cols = new HashMap[3];
                cols[0] = new HashMap();
                cols[0].put("ID", "ID");
                cols[0].put("NAME", ResourceBundle.getBundle(getClass().getName()).getString("listID"));
                cols[0].put("WIDTH", -40);
                cols[1] = new HashMap();
                cols[1].put("ID", "NAME");
                cols[1].put("NAME", ResourceBundle.getBundle(getClass().getName()).getString("listNAME"));
                cols[1].put("WIDTH", 100);
                cols[2] = new HashMap();
                cols[2].put("ID", "AUTONUM");
                cols[2].put("NAME", ResourceBundle.getBundle(getClass().getName()).getString("listAUTONUM"));
                cols[2].put("WIDTH", -40);
                HashMap ret = InputList.getValueFromList(cols, map);
                if (ret != null) getDocTypeInfo((String) ret.get("ID"), true);
            }
            if (m_event != null) {
                m_event.actionPerformed(new ActionEvent(this, 0, "putObj"));
                ;
            }
        } catch (StandardException ex) {
            Errors.showError(ex);
        }
    }

    /**
	 * Устанавливает новый тип документа для документа
	 * @param sTypeID	- Идентификатор типа документа
	 * @param sTypeName	- Название типа документа
	 * @param sAutoNum	- Автономер
	 */
    public void setDocType(String sTypeID, String sTypeName, String sAutoNum) {
        m_sTypeID = sTypeID;
        m_sTypeName = sTypeName;
        m_sNumber = sAutoNum;
    }

    /**
	 * Данные о карте документа
	 */
    private void getDocCardData() {
        try {
            Any param = ClientMain.getInstance().createAny();
            HashMap map = new HashMap();
            map.put("ID", m_sID);
            map.put("VER", m_sVersion);
            param.insert_Value(map);
            param = ClientMain.getInstance().sessionRequest("getDocAttr", param);
            HashMap ret = (HashMap) param.extract_Value();
            m_sTypeName = (String) ret.get("TYPENAME");
            m_sTypeID = (String) ret.get("TYPEID");
            m_sName = (String) ret.get("NAME");
            m_sNumber = (String) ret.get("NUM");
            m_attrs = (HashMap[]) ret.get("ATTR");
            m_signs = (HashMap[]) ret.get("SIGNS");
            getDocTypeInfo(m_sTypeID, false);
        } catch (Exception e) {
            Errors.showError(e);
        }
    }

    /**
	 * Конструктор абстрактного класса
	 * @param sID		- Идентификатор документа (объекта)
	 * @param sVer		- Версия
	 * @param mt		- Метод вызываемый при сохранении атрибутов
	 * @param obj		- Объект в котором будет вызываться метод для сохранения
	 * @param type		- Тип диалога, модальный немодальный и т. п.
	 */
    public AbstractCardEditor(String sID, String sVer, Method mt, Object obj, Dialog.ModalityType type) {
        super(type);
        m_method = mt;
        m_object = obj;
        m_sID = sID;
        m_sVersion = sVer;
        if (m_sID != null) {
            if (m_sVersion.charAt(0) == 'N') {
                m_sVersion = m_sVersion.replace("N", "");
                getDocCardData();
                m_sVersion = String.valueOf(Integer.valueOf(m_sVersion) + 1);
            } else getDocCardData();
        }
    }

    /**
	 * Обновление данных в БД
	 */
    public void updateFields() {
        try {
            HashMap par = new HashMap();
            par.put("ID", m_sID);
            par.put("PID", m_sParentID);
            par.put("VER", m_sVersion);
            par.put("ATTR", m_attrs);
            par.put("TYPE", m_sTypeID);
            par.put("NAME", m_sName);
            par.put("NUM", m_sNumber);
            if (m_sDocFile != null) {
                FileInputStream fp = new FileInputStream(m_sDocFile);
                byte bFileData[] = new byte[(int) fp.getChannel().size()];
                fp.read(bFileData);
                fp.close();
                par.put("DATA", bFileData);
                par.put("EXT", m_sDocFile.substring(m_sDocFile.lastIndexOf('.'), m_sDocFile.length()));
            }
            Any param = ClientMain.getInstance().createAny();
            param.insert_Value(par);
            ClientMain.getInstance().sessionRequest("setDocAttr", param);
            m_method.invoke(m_object);
            dispose();
        } catch (Exception ex) {
            Errors.showError(ex);
        }
    }

    /**
	 * Функция показывает выбранный диалог
	 */
    public abstract void showCardDialog();
}
