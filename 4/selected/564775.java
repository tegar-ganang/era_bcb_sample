package fireteam.fxforms;

import fireteam.interfaces.Errors;
import fireteam.orb.beans.ObjPrivs;
import fireteam.orb.client.ClientMain;
import fireteam.orb.server.stub.FTDObject;
import fireteam.orb.server.stub.StandardException;
import fireteam.orb.util.ObjUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.omg.CORBA.Any;

/**
 * Класс для обработки запросов формы DocTypeFx - работа с типами документов
 * @author Tolik1
 */
public class DocType {

    /**
	 * Типы шаблонов
	 */
    public static enum ContentType {

        NONE, SVG, PDF
    }

    ;

    private ByteArrayInputStream m_fileData;

    private byte[] m_byteFileData;

    private ContentType m_contentType = ContentType.NONE;

    private HashMap m_typeAttributes[];

    private String m_docForm;

    private String m_sAutoNum;

    private String m_sTypeName;

    private String m_sID = null;

    /**
	 * Возвращает список доступных типов документов
	 * @return
	 */
    public HashMap[] getDocTypes() {
        try {
            Any param = ClientMain.getInstance().createAny();
            param = ClientMain.getInstance().sessionRequest("getDocTypes", param);
            return (HashMap[]) param.extract_Value();
        } catch (Exception e) {
            Errors.showError(e);
        }
        return new HashMap[0];
    }

    public InputStream getContentData() {
        return m_fileData;
    }

    public ContentType getDataType() {
        return m_contentType;
    }

    public HashMap[] getAttributes() {
        return m_typeAttributes;
    }

    public String getAutoNum() {
        return m_sAutoNum;
    }

    public String getTypeName() {
        return m_sTypeName;
    }

    public String getDocForm() {
        return m_docForm;
    }

    public String getTemplateName() {
        return m_contentType.name();
    }

    /**
	 * Фукнция вызывает внешнюю программу для показа PDF-шаблона
	 */
    public void openPDF() {
        try {
            File fp = File.createTempFile("ftd_", "_tmp.PDF");
            fp.deleteOnExit();
            FileOutputStream fo = new FileOutputStream(fp);
            fo.write(m_byteFileData);
            fo.close();
            Runtime.getRuntime().exec("cmd.exe /C \"" + fp.getPath() + "\"");
        } catch (IOException ex) {
            Errors.showError(ex);
        }
    }

    /**
	 * Функция получает все данные по идентификатору типа
	 * @param sID - Идентификатор типа
	 */
    public void loadData(String sID) {
        try {
            if (m_fileData != null) {
                m_fileData.close();
                m_fileData = null;
                m_byteFileData = null;
            }
            if (sID != null) {
                Any param = ClientMain.getInstance().createAny();
                param.insert_string(sID);
                Any ret = ClientMain.getInstance().sessionRequest("getDocTypeInfo", param);
                HashMap map = (HashMap) ret.extract_Value();
                int iTmpType = Integer.valueOf((String) (map.get("TEMPTYPE") != null ? map.get("TEMPTYPE") : "-1"));
                switch(iTmpType) {
                    case 1:
                        m_contentType = ContentType.SVG;
                        m_byteFileData = ObjUtil.deCompress((byte[]) map.get("BXMLTEMP"));
                        m_fileData = new ByteArrayInputStream(m_byteFileData);
                        break;
                    case 2:
                        m_contentType = ContentType.PDF;
                        m_byteFileData = ObjUtil.deCompress((byte[]) map.get("BXMLTEMP"));
                        m_fileData = new ByteArrayInputStream(m_byteFileData);
                        break;
                    default:
                        m_contentType = ContentType.NONE;
                }
                m_typeAttributes = (HashMap[]) map.get("ATTR");
                m_docForm = (String) map.get("CDOCFORM");
                m_sAutoNum = (String) map.get("CAUTONUM");
                m_sTypeName = (String) map.get("CTYPNAME");
            } else {
                m_typeAttributes = new HashMap[0];
                m_docForm = "";
                m_sAutoNum = "";
                m_sTypeName = "";
                m_contentType = ContentType.NONE;
            }
            m_sID = sID;
        } catch (Exception ex) {
            Errors.showError(ex);
        }
    }

    /**
	 * Функция загружает файл как шаблон к типу
	 * @param sFolder	- папка для поиска файлов
	 * @return
	 */
    public String getTemplateFile(String sFolder) {
        String sName = null;
        FileNameExtensionFilter fltr = new FileNameExtensionFilter("Файлы шаблонов", "svg", "pdf");
        JFileChooser jf = new JFileChooser(sFolder);
        jf.setAcceptAllFileFilterUsed(false);
        jf.setFileFilter(fltr);
        if (jf.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            FileInputStream fi = null;
            try {
                if (m_fileData != null) {
                    m_fileData.close();
                    m_fileData = null;
                    m_byteFileData = null;
                }
                sName = jf.getSelectedFile().getPath();
                fi = new FileInputStream(sName);
                m_byteFileData = new byte[(int) fi.getChannel().size()];
                fi.read(m_byteFileData);
                m_fileData = new ByteArrayInputStream(m_byteFileData);
                String sExt = sName.substring(sName.lastIndexOf('.') + 1, sName.length());
                if (sExt.equalsIgnoreCase("SVG")) {
                    m_contentType = ContentType.SVG;
                } else if (sExt.equalsIgnoreCase("PDF")) {
                    m_contentType = ContentType.PDF;
                }
            } catch (Exception ex) {
                Errors.showError(ex);
            } finally {
                try {
                    fi.close();
                } catch (IOException ex) {
                    Errors.showError(ex);
                }
            }
        }
        return sName;
    }

    /**
	 * Функция вызывает форму для редактирования привилегий к типу
	 * @param sID
	 */
    public void editPrivs(String sID) {
        FTDObject obj = new FTDObject();
        obj.ID = sID;
        obj.Type = "DTYP";
        ObjPrivs.showObjPrivs(obj);
    }

    /**
	 * Фукнция сохраняет тип документа
	 * @param sName		- Название
	 * @param sAutoNum	- Автономер
	 * @param sDocForm	- Форма для отображения доп. атрибутов
	 * @param attrs		- Атрибуты
	 * @return успешность сохранения
	 */
    public boolean saveDocType(String sName, String sAutoNum, String sDocForm, HashMap[] attrs) {
        try {
            HashMap map = new HashMap();
            map.put("NAME", sName);
            map.put("AUTONUM", sAutoNum);
            map.put("DOCFORM", sDocForm);
            map.put("ATTR", attrs);
            map.put("ID", m_sID);
            switch(m_contentType) {
                case SVG:
                    map.put("TEMPTYPE", 1);
                    map.put("TEMPDATA", ObjUtil.compress(m_byteFileData));
                    break;
                case PDF:
                    map.put("TEMPTYPE", 2);
                    map.put("TEMPDATA", ObjUtil.compress(m_byteFileData));
                    break;
                case NONE:
                    map.put("TEMPTYPE", -1);
                    break;
            }
            Any param = ClientMain.getInstance().createAny();
            param.insert_Value(map);
            param = ClientMain.getInstance().sessionRequest("addDocType", param);
            return true;
        } catch (Exception e) {
            Errors.showError(e);
        }
        return false;
    }
}
