package fireteam.orb.server.processors;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import fireteam.orb.server.processors.types.FTSign;
import fireteam.orb.server.processors.types.FTSigns;
import fireteam.orb.server.processors.types.ObjAttr;
import fireteam.orb.server.processors.types.ObjAttrs;
import fireteam.orb.server.stub.StandardException;
import fireteam.orb.util.ObjUtil;
import fireteam.security.RSA;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleTypes;
import oracle.sql.BLOB;
import org.omg.CORBA.Any;
import org.omg.CORBA.Any;

/**
 *
 * @author Tolik1
 */
public class Documents {

    private Documents() {
    }

    /**
	 * Взвращает список типов документов
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any getDocTypes(Connection con, Any values, Any retValue) throws SQLException {
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocTypes");
        PreparedStatement stmt = con.prepareStatement(sSql);
        ResultSet rSet = stmt.executeQuery();
        ArrayList<HashMap> arMaps = new ArrayList<HashMap>();
        while (rSet.next()) {
            HashMap map = new HashMap();
            map.put("ID", rSet.getString("IDOCTID"));
            map.put("NAME", rSet.getString("CTYPNAME"));
            map.put("AUTONUM", rSet.getString("CAUTONUM"));
            arMaps.add(map);
        }
        rSet.close();
        stmt.close();
        retValue.insert_Value(arMaps.toArray(new HashMap[arMaps.size()]));
        return retValue;
    }

    /**
	 * Фозвращает информацию по типу документа
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any getDocTypeInfo(Connection con, Any values, Any retValue) throws SQLException {
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocTypeInfo");
        PreparedStatement stmt = con.prepareStatement(sSql);
        String sTypeID = values.extract_string();
        stmt.setString(1, sTypeID);
        OracleResultSet rSet = (OracleResultSet) stmt.executeQuery();
        HashMap map = new HashMap();
        if (rSet.next()) {
            BLOB blob = (BLOB) rSet.getBlob("BXMLTEMP");
            byte cData[] = new byte[0];
            if (blob != null) {
                cData = blob.getBytes(1, (int) blob.length());
                map.put("BXMLTEMP", cData);
            }
            ObjAttrs arAttrs = (ObjAttrs) rSet.getORAData("ATTRS", new ObjAttrs());
            ObjAttr attrs[] = arAttrs.getArray();
            ArrayList<HashMap> arAttr = new ArrayList<HashMap>();
            for (ObjAttr atr : attrs) {
                HashMap mapAttr = new HashMap();
                mapAttr.put("NAME", atr.getCname());
                mapAttr.put("DESC", atr.getCdesc());
                mapAttr.put("TYPE", atr.getCtype());
                arAttr.add(mapAttr);
            }
            map.put("ATTR", arAttr.toArray(new HashMap[arAttr.size()]));
            map.put("TEMPTYPE", rSet.getString("ITEMPTYPE"));
            map.put("CDOCFORM", rSet.getString("CDOCFORM"));
            map.put("CAUTONUM", rSet.getString("CAUTONUM"));
            map.put("CTYPNAME", rSet.getString("CTYPNAME"));
        }
        rSet.close();
        stmt.close();
        retValue.insert_Value(map);
        return retValue;
    }

    /**
	 * Возвращает список для справочника автономеров
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any getNMR(Connection con, Any values, Any retValue) throws SQLException {
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getNMR");
        PreparedStatement stmt = con.prepareStatement(sSql);
        ResultSet rSet = stmt.executeQuery();
        ArrayList<HashMap> arMaps = new ArrayList<HashMap>();
        while (rSet.next()) {
            HashMap map = new HashMap();
            map.put("1", rSet.getString("CAUTONUM"));
            map.put("2", rSet.getString("CNUMMASK"));
            map.put("3", rSet.getString("ILASTNUM"));
            arMaps.add(map);
        }
        rSet.close();
        stmt.close();
        retValue.insert_Value(arMaps.toArray(new HashMap[arMaps.size()]));
        return retValue;
    }

    /**
	 * Создает запись с справочнике автономеров
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any putNMR(Connection con, Any values, Any retValue) throws SQLException {
        HashMap arVals = (HashMap) values.extract_Value();
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("putNMR");
        PreparedStatement stmt = con.prepareStatement(sSql);
        stmt.setString(1, (String) arVals.get("1"));
        stmt.setString(2, (String) arVals.get("2"));
        stmt.setString(3, (String) arVals.get("3"));
        stmt.executeUpdate();
        stmt.close();
        con.commit();
        return retValue;
    }

    /**
	 * Удаляет запись из справочника автономеров
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any delNMR(Connection con, Any values, Any retValue) throws SQLException {
        String sID = values.extract_wstring();
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("delNMR");
        PreparedStatement stmt = con.prepareStatement(sSql);
        stmt.setString(1, sID);
        stmt.executeUpdate();
        stmt.close();
        con.commit();
        return retValue;
    }

    /**
	 * Добавляет новый тип документа или изменяет старый в системе докфлоу
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any addDocType(Connection con, Any values, Any retValue) throws SQLException {
        HashMap mapPar = (HashMap) values.extract_Value();
        String sName = (String) mapPar.get("NAME");
        String sAutoNum = (String) mapPar.get("AUTONUM");
        String sID = (String) mapPar.get("ID");
        int iTempType = (Integer) mapPar.get("TEMPTYPE");
        String sDocForm = (String) mapPar.get("DOCFORM");
        HashMap[] attrs = (HashMap[]) mapPar.get("ATTR");
        byte[] tempData = (byte[]) mapPar.get("TEMPDATA");
        OracleCallableStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("addDocType");
        try {
            ArrayList<ObjAttr> arObjAttr = new ArrayList<ObjAttr>();
            for (HashMap attr : attrs) {
                arObjAttr.add(new ObjAttr((String) attr.get("NAME"), (String) attr.get("TYPE"), (String) attr.get("DESC")));
            }
            ObjAttrs arAttrs = new ObjAttrs(arObjAttr.toArray(new ObjAttr[arObjAttr.size()]));
            ps = (OracleCallableStatement) con.prepareCall(sSql);
            BLOB blb = null;
            ps.setString(1, sName);
            ps.setString(2, sAutoNum);
            ps.setString(3, sDocForm);
            switch(iTempType) {
                case 1:
                case 2:
                    blb = BLOB.createTemporary(con, true, BLOB.DURATION_SESSION);
                    blb.setBytes(1, tempData);
                    ps.setBlob(4, blb);
                    break;
                default:
                    ps.setNull(4, Types.BLOB);
                    break;
            }
            ps.setORAData(5, arAttrs);
            if (sID != null) ps.setString(6, sID); else ps.setNull(6, Types.INTEGER);
            ps.setInt(7, iTempType);
            ps.executeUpdate();
            con.commit();
            ps.close();
            if (blb != null) BLOB.freeTemporary(blb);
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Возвращает список документов в определенной папке
	 * @param con
	 * @param values	- входящий параметр - идентификатор родителького элемента (wstring)
	 * @param retValue	- Массив типа HashMap
	 *						ID		- Идентификатор документа
	 *						NAME	- Название документа
	 *						NUM		- Номер документа
	 *						USRNAME	- Имя создателдя
	 *						LOGNAME	- Логин создателя
	 *						DUTY	- Должность создателя
	 *						DCREATE	- Дата создания
	 *						EXT		- Расширение файла
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any getDocList(Connection con, Any values, Any retValue) throws SQLException {
        String sParentID = (String) values.extract_wstring();
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocList");
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sParentID);
            ResultSet rSet = ps.executeQuery();
            ArrayList<HashMap> arVal = new ArrayList<HashMap>();
            while (rSet.next()) {
                HashMap value = new HashMap();
                value.put("ID", rSet.getString("IOBJID"));
                value.put("NAME", rSet.getString("COBJNAME"));
                value.put("NUM", rSet.getString("COBJNUM"));
                value.put("USRNAME", rSet.getString("CUSRNAME"));
                value.put("LOGNAME", rSet.getString("CLOGNAME"));
                value.put("DUTY", rSet.getString("CUSRDUTY"));
                value.put("DCREATE", rSet.getString("DCREATE"));
                String sExt = rSet.getString("EXTENSION");
                if (sExt != null) sExt = sExt.replace(".", "");
                value.put("EXT", sExt);
                arVal.add(value);
            }
            retValue.insert_Value(arVal.toArray(new HashMap[arVal.size()]));
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Функция возвращает список версий документа
	 * @param con		- Соединение
	 * @param values	- параметры
	 * @param retValue	- возвращаемое значение
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any getDocVer(Connection con, Any values, Any retValue) throws SQLException {
        HashMap v = (HashMap) values.extract_Value();
        String sID = (String) v.get("ID");
        boolean bCard = (Boolean) v.get("ISCARD");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocVer");
        if (bCard) sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocCardVer");
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sID);
            ResultSet rSet = ps.executeQuery();
            ArrayList<HashMap> arRet = new ArrayList<HashMap>();
            while (rSet.next()) {
                HashMap val = new HashMap();
                val.put("VER", rSet.getString("IOBJVER"));
                arRet.add(val);
            }
            rSet.close();
            retValue.insert_Value(arRet.toArray(new HashMap[arRet.size()]));
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    private static byte[] writePDF(byte[] compressPDFData, fireteam.orb.server.processors.types.Attribute attrs[], FTSign signs[], String sDuty, String sUsrName) {
        byte retArray[] = new byte[0];
        try {
            String sFontPath = ResourceBundle.getBundle(Documents.class.getName()).getString("FONT_PATH");
            BaseFont fn = BaseFont.createFont(sFontPath, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            PdfReader reader = new PdfReader(ObjUtil.deCompress(compressPDFData));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            PdfStamper stamp = new PdfStamper(reader, bo);
            AcroFields form = stamp.getAcroFields();
            form.addSubstitutionFont(fn);
            for (Object obj : form.getFields().entrySet()) {
                Map.Entry item = (Map.Entry) obj;
                String key = item.getKey().toString();
                int iIndex1 = key.lastIndexOf('.');
                int iIndex2 = key.lastIndexOf('[');
                String sKey = key.substring(iIndex1 + 1, iIndex2);
                if (sKey.equals("DUTY")) form.setField(key, sDuty); else if (sKey.equals("NAME")) form.setField(key, sUsrName); else for (fireteam.orb.server.processors.types.Attribute sField : attrs) if (sKey.equals(sField.getName())) form.setField(key, sField.getValue());
            }
            reader.close();
            Font fnT = new Font(fn, 8);
            Font fnTB = new Font(fn, 9);
            PdfPTable pTable = new PdfPTable(3);
            pTable.setWidths(new int[] { 20, 30, 50 });
            PdfPCell cell;
            cell = new PdfPCell(new Phrase(2, "Дата подписания", fnTB));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            pTable.addCell(cell);
            cell = new PdfPCell(new Phrase(2, "Должность", fnTB));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            pTable.addCell(cell);
            cell = new PdfPCell(new Phrase(2, "Ф.И.О.", fnTB));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            pTable.addCell(cell);
            for (int i = 0; i < 20; i++) {
                for (FTSign Sign : signs) {
                    cell = new PdfPCell(new Phrase(2, DateFormat.getDateTimeInstance().format(Sign.getDateCreate()), fnT));
                    pTable.addCell(cell);
                    cell = new PdfPCell(new Phrase(2, Sign.getUsrDuty(), fnT));
                    pTable.addCell(cell);
                    cell = new PdfPCell(new Phrase(2, Sign.getUsrName(), fnT));
                    pTable.addCell(cell);
                }
            }
            int iPages = stamp.getReader().getNumberOfPages();
            PdfContentByte cb = stamp.getOverContent(iPages);
            pTable.setSplitLate(true);
            pTable.setSplitRows(true);
            pTable.setHeaderRows(1);
            pTable.setTotalWidth(stamp.getReader().getPageSizeWithRotation(iPages).getWidth() - 140);
            float iOfset = stamp.getReader().getPageSizeWithRotation(iPages).getHeight() - stamp.getWriter().getVerticalPosition(true);
            pTable.writeSelectedRows(0, -1, 80, iOfset, cb);
            stamp.close();
            bo.close();
            retArray = bo.toByteArray();
        } catch (Exception e) {
            ObjUtil.throwStandardException(e);
        }
        return retArray;
    }

    /**
	 * Возвращает представление документа - если это шаблон, то заполняет поля и возвращает в формате либо PDF, 
	 * либо SVG, если это вложенный файл, то возвращает вложенный файл
	 * @param con
	 * @param values		- Параметры HashMap
	 *							ID		- Идентификатор докмента
	 *							VER		- Версия документа
	 * @param retValue		- HashMap
	 *							TTYPE	- Тип Шаблона
	 *							DATA	- Данные для отображения в сжатом виде (byte[])
	 *							EXT		- Расширение файла
	 *							EDIT	- Флаг для возможности редактирования (1 можно, 0 - нельзя) 
	 *							ATTR	- Доп атрибуты, используются для формата SVG, т.к. 
	 *									  он формируется и отображается на клиенте HashMap
	 *										USR - Имя пользователя
	 *										DUTY - Должность пользователя 
	 *										DATE - Дата создания
	 *										ATTR - Доп. атрибуты HashMap[]: (NAME, VALUE)
	 * @return retValue
	 * @throws java.sql.SQLException
	 */
    public static Any getDocView(Connection con, Any values, Any retValue) throws SQLException {
        HashMap val = (HashMap) values.extract_Value();
        String sID = (String) val.get("ID");
        String sVer = (String) val.get("VER");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocInfo");
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sID);
            ps.setString(2, sVer);
            OracleResultSet rSet = (OracleResultSet) ps.executeQuery();
            if (rSet.next()) {
                int iTempType = rSet.getInt("ITEMPTYPE");
                Timestamp dCh = rSet.getTimestamp("DCHANGE");
                HashMap retVal = new HashMap();
                retVal.put("TTYPE", iTempType);
                switch(iTempType) {
                    case 1:
                    case 2:
                        BLOB blobTempl = (BLOB) rSet.getBlob("BXMLTEMP");
                        String sUsrName = rSet.getString("CUSRNAME");
                        String sDuty = rSet.getString("CUSRDUTY");
                        retVal.put("EDIT", 0);
                        fireteam.orb.server.processors.types.AttributeList arAttrs = (fireteam.orb.server.processors.types.AttributeList) rSet.getORAData("ATTRS", new fireteam.orb.server.processors.types.AttributeList());
                        fireteam.orb.server.processors.types.Attribute attrs[] = arAttrs.getArray();
                        if (iTempType == 2) {
                            retVal.put("EXT", ".PDF");
                            FTSigns arSign = (FTSigns) rSet.getORAData("SIGNS", new FTSigns());
                            FTSign signs[] = arSign.getArray();
                            retVal.put("DATA", writePDF(blobTempl.getBytes(1, (int) blobTempl.length()), attrs, signs, sDuty, sUsrName));
                        } else {
                            retVal.put("EXT", ".SVG");
                            retVal.put("DATA", blobTempl.getBytes(1, (int) blobTempl.length()));
                            HashMap attr = new HashMap();
                            attr.put("USR", sUsrName);
                            attr.put("DUTY", sDuty);
                            attr.put("DATE", dCh);
                            ArrayList<HashMap> arAtr = new ArrayList<HashMap>();
                            for (fireteam.orb.server.processors.types.Attribute a : attrs) {
                                HashMap a1 = new HashMap();
                                a1.put("NAME", a.getName());
                                a1.put("VALUE", a.getValue());
                                arAtr.add(a1);
                            }
                            attr.put("ATTR", arAtr.toArray(new HashMap[arAtr.size()]));
                            retVal.put("ATTR", attr);
                        }
                        break;
                    default:
                        String sExt = rSet.getString("EXTENSION");
                        BLOB blobDoc = (BLOB) rSet.getBlob("BOBJDATA");
                        retVal.put("EDIT", rSet.getInt("EDIT_FLAG"));
                        retVal.put("TTYPE", -1);
                        retVal.put("EXT", sExt);
                        retVal.put("DATA", blobDoc.getBytes(1, (int) blobDoc.length()));
                        retVal.put("DATE", dCh);
                        break;
                }
                retValue.insert_Value(retVal);
            }
            rSet.close();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * 
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any setDocFile(Connection con, Any values, Any retValue) throws SQLException, IOException {
        HashMap val = (HashMap) values.extract_Value();
        String sID = (String) val.get("ID");
        String sVer = (String) val.get("VER");
        byte bFileData[] = ObjUtil.deCompress((byte[]) val.get("DATA"));
        String sExt = (String) val.get("EXT");
        OracleCallableStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("setDocAttr");
        try {
            ps = (OracleCallableStatement) con.prepareCall(sSql);
            ps.setString(1, sID);
            ps.setString(2, sVer);
            ps.setNull(3, Types.VARCHAR);
            ps.setNull(4, Types.VARCHAR);
            ps.setNull(5, OracleTypes.ARRAY, "BC.ATTRIBUTE_LIST");
            BLOB blb = BLOB.createTemporary(con, false, BLOB.DURATION_SESSION);
            blb.setBytes(1, bFileData);
            ps.setBlob(6, blb);
            ps.setString(7, sExt);
            ps.executeUpdate();
            con.commit();
            ps.close();
            if (blb != null) BLOB.freeTemporary(blb);
            retValue.insert_wstring(ResourceBundle.getBundle(Documents.class.getName()).getString("OK"));
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Функция возвращает информация по конкретному документу
	 * @param con			- Соедиение
	 * @param values		- Параметры HashMap
	 *							ID		- Идентификатор докмента
	 *							VER		- Версия документа
	 * @param retValue		- Возвращаемые значения HashMap
	 *							TYPE	- Название типа документа
	 *							TYPEID	- Идентификатор типа документа
	 *							NAME	- Название документа
	 *							NUM		- Номер документа
	 *							ATTR	- HashMap[] атрибуты (NAME, VALUE)
	 *							SIGNS	- Список подписей HashMap[]
	 *										LOGNAME	- Логин пользователя
	 *										NAME	- Имя пользователя
	 *										DUTY	- Должность пользователя
	 *										DATE	- Дата подписания
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any getDocAttr(Connection con, Any values, Any retValue) throws SQLException {
        HashMap val = (HashMap) values.extract_Value();
        String sID = (String) val.get("ID");
        String sVer = (String) val.get("VER");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocCard");
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sID);
            ps.setString(2, sVer);
            OracleResultSet rSet = (OracleResultSet) ps.executeQuery();
            while (rSet.next()) {
                HashMap rv = new HashMap();
                rv.put("TYPENAME", rSet.getString("CTYPNAME"));
                rv.put("NAME", rSet.getString("COBJNAME"));
                rv.put("NUM", rSet.getString("COBJNUM"));
                rv.put("TYPEID", rSet.getString("IDOCTID"));
                fireteam.orb.server.processors.types.AttributeList arAttrs = (fireteam.orb.server.processors.types.AttributeList) rSet.getORAData("ATTRS", new fireteam.orb.server.processors.types.AttributeList());
                fireteam.orb.server.processors.types.Attribute attrs[] = arAttrs.getArray();
                FTSigns arSign = (FTSigns) rSet.getORAData("SIGNS", new FTSigns());
                FTSign signs[] = arSign.getArray();
                ArrayList<HashMap> arAtr = new ArrayList<HashMap>();
                for (fireteam.orb.server.processors.types.Attribute atr : attrs) {
                    HashMap hm = new HashMap();
                    hm.put("NAME", atr.getName());
                    hm.put("VALUE", atr.getValue());
                    arAtr.add(hm);
                }
                rv.put("ATTR", arAtr.toArray(new HashMap[arAtr.size()]));
                arAtr.clear();
                for (FTSign sign : signs) {
                    HashMap hm = new HashMap();
                    hm.put("LOGNAME", sign.getLogName());
                    hm.put("NAME", sign.getUsrName());
                    hm.put("DUTY", sign.getUsrDuty());
                    hm.put("DATE", sign.getDateCreate());
                    arAtr.add(hm);
                }
                rv.put("SIGNS", arAtr.toArray(new HashMap[arAtr.size()]));
                retValue.insert_Value(rv);
            }
            rSet.close();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    public static Any getDocForm(Connection con, Any values, Any retValue) throws SQLException {
        HashMap val = (HashMap) values.extract_Value();
        String sID = (String) val.get("ID");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocForm");
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sID);
            OracleResultSet rSet = (OracleResultSet) ps.executeQuery();
            while (rSet.next()) {
                HashMap rv = new HashMap();
                rv.put("FORM", rSet.getString("CDOCFORM"));
                retValue.insert_Value(rv);
            }
            rSet.close();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Изменяет атрибуты документа
	 * @param con			- Соединение с БД
	 * @param values		- Параметры тип HashMap
	 *							ID		- Идентификатор документа
	 *							VER		- Номер версии
	 *							TYPE	- Тип документа
	 *							NAME	- Название документа
	 *							NUM		- Номер документа
	 *							ATTR	- Список заполненных атрибутов
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any setDocAttr(Connection con, Any values, Any retValue) throws SQLException {
        HashMap val = (HashMap) values.extract_Value();
        String sID = (String) val.get("ID");
        String sVer = (String) val.get("VER");
        String sParentID = (String) val.get("PID");
        String sTypeID = (String) val.get("TYPE");
        String sName = (String) val.get("NAME");
        String sNum = (String) val.get("NUM");
        HashMap attr[] = (HashMap[]) val.get("ATTR");
        byte[] bFileData = (byte[]) val.get("DATA");
        String sExt = (String) val.get("EXT");
        if (sID == null) return addDoc(con, values, retValue);
        OracleCallableStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("setDocAttr");
        try {
            ps = (OracleCallableStatement) con.prepareCall(sSql);
            ArrayList<fireteam.orb.server.processors.types.Attribute> arAttr = new ArrayList<fireteam.orb.server.processors.types.Attribute>();
            for (HashMap a : attr) {
                arAttr.add(new fireteam.orb.server.processors.types.Attribute((String) a.get("NAME"), (String) a.get("VALUE")));
            }
            fireteam.orb.server.processors.types.AttributeList arAttrs = new fireteam.orb.server.processors.types.AttributeList(arAttr.toArray(new fireteam.orb.server.processors.types.Attribute[arAttr.size()]));
            ps.setString(1, sID);
            ps.setString(2, sVer);
            ps.setString(3, sTypeID);
            ps.setString(4, sName);
            ps.setORAData(5, arAttrs);
            BLOB blb = null;
            if (bFileData != null) {
                blb = BLOB.createTemporary(con, false, BLOB.DURATION_SESSION);
                blb.setBytes(1, bFileData);
                ps.setBlob(6, blb);
                ps.setString(7, sExt);
            } else {
                ps.setNull(6, Types.BLOB);
                ps.setNull(7, Types.VARCHAR);
            }
            ps.executeUpdate();
            con.commit();
            if (blb != null) BLOB.freeTemporary(blb);
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Добавляет документ в систему
	 * @param con			- Соединение с БД
	 * @param values		- Параметры те же что и у предыдущей функции
	 * @param retValue		- возвращаемое значение
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any addDoc(Connection con, Any values, Any retValue) throws SQLException {
        HashMap val = (HashMap) values.extract_Value();
        String sParentID = (String) val.get("PID");
        String sTypeID = (String) val.get("TYPE");
        String sID = (String) val.get("ID");
        String sVer = (String) val.get("VER");
        String sName = (String) val.get("NAME");
        String sNum = (String) val.get("NUM");
        HashMap attr[] = (HashMap[]) val.get("ATTR");
        byte[] bFileData = (byte[]) val.get("DATA");
        String sExt = (String) val.get("EXT");
        OracleCallableStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("addDoc");
        try {
            ArrayList<fireteam.orb.server.processors.types.Attribute> arAttr = new ArrayList<fireteam.orb.server.processors.types.Attribute>();
            for (HashMap a : attr) {
                arAttr.add(new fireteam.orb.server.processors.types.Attribute((String) a.get("NAME"), (String) a.get("VALUE")));
            }
            fireteam.orb.server.processors.types.AttributeList arAttrs = new fireteam.orb.server.processors.types.AttributeList(arAttr.toArray(new fireteam.orb.server.processors.types.Attribute[arAttr.size()]));
            ps = (OracleCallableStatement) con.prepareCall(sSql);
            ps.setString(1, sParentID);
            ps.setString(2, sTypeID);
            ps.setString(3, sName);
            ps.setORAData(4, arAttrs);
            BLOB blb = null;
            if (bFileData != null) {
                blb = BLOB.createTemporary(con, false, BLOB.DURATION_SESSION);
                blb.setBytes(1, bFileData);
                ps.setBlob(5, blb);
                ps.setString(6, sExt);
            } else {
                ps.setNull(5, Types.BLOB);
                ps.setNull(6, Types.VARCHAR);
            }
            ps.executeUpdate();
            con.commit();
            if (blb != null) BLOB.freeTemporary(blb);
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Функция возвращает данные документа, только для вложенных файлов
	 * @param con		- Соединение с БД
	 * @param sID		- Идентификатор документа
	 * @param sVer		- Версия документа
	 * @return данные документа
	 */
    private static byte[] getDocumentBLOB(Connection con, String sID, String sVer) throws SQLException {
        byte[] retData = null;
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocInfo");
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sID);
            ps.setString(2, sVer);
            OracleResultSet rSet = (OracleResultSet) ps.executeQuery();
            if (rSet.next()) {
                BLOB blobDoc = (BLOB) rSet.getBlob("BOBJDATA");
                retData = blobDoc.getBytes(1, (int) blobDoc.length());
            }
            rSet.close();
        } finally {
            if (ps != null) ps.close();
        }
        return retData;
    }

    /**
	 * Функция возвращает значение текущего открытого ключа клиента
	 * @param con	- Содинение с БД
	 * @return
	 */
    private static PublicKey getCurrentPublicKey(Connection con) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        PublicKey pKey = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getPubKey");
        Statement stmt = con.createStatement();
        ResultSet rSet = stmt.executeQuery(sSql);
        if (rSet.next()) {
            BLOB blob = (BLOB) rSet.getBlob(1);
            pKey = RSA.getPubkey(blob.getBytes(1, (int) blob.length()));
        }
        return pKey;
    }

    /**
	 * Функция возвращает бинароное представление атрибутов. Используется при подписании
	 * документов
	 * @param con		- Соединение с БД
	 * @param values	- Значения атрибутов в формате HashMap[]
	 *						NAME	- Имя 
	 *						VALUE	- Значение
	 * @param retValue - возвращает бинарное представление тип byte[]
	 * @return
	 */
    public static Any getAttributesData(Connection con, Any values, Any retValue) throws UnsupportedEncodingException, IOException {
        HashMap attrs[] = (HashMap[]) values.extract_Value();
        retValue.insert_Value(getAttributesData(attrs));
        return retValue;
    }

    /**
	 * Функция превращает атрибуты в бинарные данные по определенному формату
	 * @param attrs		- Список атрибутов
	 * @return
	 * @throws java.io.UnsupportedEncodingException
	 * @throws java.io.IOException
	 */
    private static byte[] getAttributesData(HashMap[] attrs) throws UnsupportedEncodingException, IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        for (HashMap a : attrs) {
            bo.write(((String) a.get("NAME")).getBytes("UTF8"));
            bo.write(ResourceBundle.getBundle(Documents.class.getName()).getString("COL_SEP").getBytes("UTF8"));
            bo.write(((String) a.get("VALUE")).getBytes("UTF8"));
            bo.write(ResourceBundle.getBundle(Documents.class.getName()).getString("ROW_SEP").getBytes("UTF8"));
        }
        bo.close();
        return bo.toByteArray();
    }

    /**
	 * Функция подписывает документ
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any signDoc(Connection con, Any values, Any retValue) throws SQLException, StandardException, NoSuchAlgorithmException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeyException, SignatureException, InvalidKeySpecException, IOException {
        HashMap val = (HashMap) values.extract_Value();
        String sID = (String) val.get("ID");
        String sVersion = (String) val.get("VER");
        HashMap[] attrs = (HashMap[]) val.get("ATTRS");
        byte[] data = (byte[]) val.get("DATA");
        java.util.Date tstamp = (java.util.Date) val.get("DATE");
        byte[] sign = (byte[]) val.get("SIGN");
        PublicKey pKey = getCurrentPublicKey(con);
        if (data != null) {
            byte oldData[] = getDocumentBLOB(con, sID, sVersion);
            if (!Arrays.equals(oldData, data)) {
                ObjUtil.throwStandardException(new StandardException(ResourceBundle.getBundle(Documents.class.getName()).getString("SIGN_ERROR")));
            }
            if (!RSA.signVerify(data, sign, pKey)) {
                ObjUtil.throwStandardException(new StandardException(ResourceBundle.getBundle(Documents.class.getName()).getString("SIGN_ERROR")));
            }
        }
        if (attrs != null) {
            if (!RSA.signVerify(getAttributesData(attrs), sign, pKey)) {
                ObjUtil.throwStandardException(new StandardException(ResourceBundle.getBundle(Documents.class.getName()).getString("SIGN_ERROR")));
            }
        }
        OracleCallableStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("signDoc");
        try {
            BLOB blb = null;
            ps = (OracleCallableStatement) con.prepareCall(sSql);
            ps.registerOutParameter(1, Types.NUMERIC);
            ps.setString(2, sID);
            ps.setString(3, sVersion);
            if (attrs != null) {
                ArrayList<fireteam.orb.server.processors.types.Attribute> arAttr = new ArrayList<fireteam.orb.server.processors.types.Attribute>();
                for (HashMap Attr : attrs) {
                    arAttr.add(new fireteam.orb.server.processors.types.Attribute((String) Attr.get("NAME"), (String) Attr.get("VALUE")));
                }
                fireteam.orb.server.processors.types.AttributeList arAttrs = new fireteam.orb.server.processors.types.AttributeList(arAttr.toArray(new fireteam.orb.server.processors.types.Attribute[arAttr.size()]));
                ps.setORAData(4, arAttrs);
                blb = BLOB.createTemporary(con, false, BLOB.DURATION_SESSION);
                blb.setBytes(1, data);
                ps.setBlob(5, blb);
                ps.setNull(6, Types.DATE);
            } else {
                ps.setNull(4, OracleTypes.ARRAY, "BC.ATTRIBUTE_LIST");
                blb = BLOB.createTemporary(con, false, BLOB.DURATION_SESSION);
                blb.setBytes(1, data);
                ps.setBlob(5, blb);
                ps.setTimestamp(6, new Timestamp(tstamp.getTime()));
            }
            ps.executeUpdate();
            long ret = ps.getLong(1);
            if (ret == 1) {
                con.commit();
            } else {
                con.rollback();
            }
            if (blb != null) BLOB.freeTemporary(blb);
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Возвращает представление документа - если это шаблон, то заполняет поля и возвращает в формате либо PDF, 
	 * либо SVG, если это вложенный файл, то возвращает вложенный файл
	 * @param con
	 * @param values		- Параметры HashMap
	 *							ID		- Идентификатор докмента
	 *							VER		- Версия документа
	 * @param retValue		- HashMap
	 *							TTYPE	- Тип Шаблона
	 *							DATA	- Данные для отображения в сжатом виде (byte[])
	 *							EXT		- Расширение файла
	 *							EDIT	- Флаг для возможности редактирования (1 можно, 0 - нельзя) 
	 *							ATTR	- Доп атрибуты, используются для формата SVG, т.к. 
	 *									  он формируется и отображается на клиенте HashMap
	 *										USR - Имя пользователя
	 *										DUTY - Должность пользователя 
	 *										DATE - Дата создания
	 *										ATTR - Доп. атрибуты HashMap[]: (NAME, VALUE)
	 * @return retValue
	 * @throws java.sql.SQLException
	 */
    public static Any getDocForSign(Connection con, Any values, Any retValue) throws SQLException {
        HashMap val = (HashMap) values.extract_Value();
        String sID = (String) val.get("ID");
        String sVer = (String) val.get("VER");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocInfo");
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sID);
            ps.setString(2, sVer);
            OracleResultSet rSet = (OracleResultSet) ps.executeQuery();
            if (rSet.next()) {
                int iTempType = rSet.getInt("ITEMPTYPE");
                HashMap retVal = new HashMap();
                retVal.put("TTYPE", iTempType);
                Timestamp dCh = rSet.getTimestamp("DCHANGE");
                switch(iTempType) {
                    case 1:
                    case 2:
                        fireteam.orb.server.processors.types.AttributeList arAttrs = (fireteam.orb.server.processors.types.AttributeList) rSet.getORAData("ATTRS", new fireteam.orb.server.processors.types.AttributeList());
                        fireteam.orb.server.processors.types.Attribute attrs[] = arAttrs.getArray();
                        HashMap attr = new HashMap();
                        ArrayList<HashMap> arAtr = new ArrayList<HashMap>();
                        for (fireteam.orb.server.processors.types.Attribute a : attrs) {
                            HashMap a1 = new HashMap();
                            a1.put("NAME", a.getName());
                            a1.put("VALUE", a.getValue());
                            arAtr.add(a1);
                        }
                        attr.put("ATTR", arAtr.toArray(new HashMap[arAtr.size()]));
                        retVal.put("ATTR", attr);
                        break;
                    default:
                        BLOB blobDoc = (BLOB) rSet.getBlob("BOBJDATA");
                        retVal.put("TTYPE", -1);
                        retVal.put("DATA", blobDoc.getBytes(1, (int) blobDoc.length()));
                        retVal.put("DATE", dCh);
                        break;
                }
                retValue.insert_Value(retVal);
            }
            rSet.close();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Функция проверяет соответствие подписей к документу
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 * @throws java.io.UnsupportedEncodingException
	 * @throws java.io.IOException
	 */
    public static Any checkDocumentSigns(Connection con, Any values, Any retValue) throws SQLException, UnsupportedEncodingException, IOException {
        HashMap val = (HashMap) values.extract_Value();
        String sID = (String) val.get("ID");
        String sVer = (String) val.get("VER");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocInfo");
        byte dataToVerify[] = new byte[0];
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sID);
            ps.setString(2, sVer);
            OracleResultSet rSet = (OracleResultSet) ps.executeQuery();
            if (rSet.next()) {
                int iTempType = rSet.getInt("ITEMPTYPE");
                HashMap retVal = new HashMap();
                retVal.put("TTYPE", iTempType);
                switch(iTempType) {
                    case 1:
                    case 2:
                        fireteam.orb.server.processors.types.AttributeList arAttrs = (fireteam.orb.server.processors.types.AttributeList) rSet.getORAData("ATTRS", new fireteam.orb.server.processors.types.AttributeList());
                        fireteam.orb.server.processors.types.Attribute attrs[] = arAttrs.getArray();
                        HashMap attr = new HashMap();
                        ArrayList<HashMap> arAtr = new ArrayList<HashMap>();
                        for (fireteam.orb.server.processors.types.Attribute a : attrs) {
                            HashMap a1 = new HashMap();
                            a1.put("NAME", a.getName());
                            a1.put("VALUE", a.getValue());
                            arAtr.add(a1);
                        }
                        dataToVerify = getAttributesData(arAtr.toArray(new HashMap[arAtr.size()]));
                        break;
                    default:
                        BLOB blobDoc = (BLOB) rSet.getBlob("BOBJDATA");
                        dataToVerify = blobDoc.getBytes(1, (int) blobDoc.length());
                        break;
                }
            }
            rSet.close();
            sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getDocSignsForVerify");
            String sSqlDel = ResourceBundle.getBundle(Documents.class.getName()).getString("delDocSign");
            ps = con.prepareStatement(sSql);
            ps.setString(1, sID);
            ps.setString(2, sVer);
            rSet = (OracleResultSet) ps.executeQuery();
            while (rSet.next()) {
                String sPkeyID = rSet.getString("IPKEYID");
                BLOB Sign = rSet.getBLOB("BSIGN");
                BLOB bKey = rSet.getBLOB("BPKEY");
                try {
                    byte sign[] = Sign.getBytes(1, (int) Sign.length());
                    PublicKey pKey = RSA.getPubkey(bKey.getBytes(1, (int) bKey.length()));
                    if (!RSA.signVerify(dataToVerify, sign, pKey)) {
                        PreparedStatement psDel = con.prepareStatement(sSqlDel);
                        psDel.setString(1, sID);
                        psDel.setString(2, sVer);
                        psDel.setString(3, sPkeyID);
                        psDel.executeUpdate();
                        psDel.close();
                        con.commit();
                    }
                } catch (Exception e) {
                    Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
                }
            }
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Возвращает тип шаблона для документа
	 * @param con
	 * @param values
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any getTemplateType(Connection con, Any values, Any retValue) throws SQLException {
        HashMap val = (HashMap) values.extract_Value();
        String sID = (String) val.get("ID");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(Documents.class.getName()).getString("getTemplateType");
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sID);
            OracleResultSet rSet = (OracleResultSet) ps.executeQuery();
            if (rSet.next()) {
                int iTempType = rSet.getInt("ITEMPTYPE");
                HashMap ret = new HashMap();
                ret.put("TTYPE", iTempType);
                retValue.insert_Value(ret);
            }
            rSet.close();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }
}
