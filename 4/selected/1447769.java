package fireteam.orb.server.processors;

import fireteam.orb.util.ObjUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.omg.CORBA.Any;

/**
 * User: msv
 * Date: 13.04.2007
 * Time: 11:47:55
 */
public final class OrderServ {

    /**
	 * Добавление приходного ордера в систему
	 * @param con			- Соединение с БД
	 * @param values		- Параметры тип HashMap
	 *							ORGID	- Идентификатор организации
	 *							DATE	- Дата заведенеи ордера
	 *							NUMBER	- Номер документа
	 *							MEMBER	- Член (человек на которого заводиться приходник)
	 *							ACC		- Номер счета
	 *							SUM		- Сумма
	 *							PURP	- Назначение платежа
	 * @param retValue ничего не возвращает в случае удачи, да и в случае неудачи тоже
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any addOrder(Connection con, Any values, Any retValue) throws SQLException {
        HashMap map = (HashMap) values.extract_Value();
        String sOrderID = (String) map.get("ORDERID");
        String sOrgID = (String) map.get("ORGID");
        Date dDate = (Date) map.get("DATE");
        String sNumber = (String) map.get("NUMBER");
        String sMember = (String) map.get("MEMBER");
        String sAcc = (String) map.get("ACC");
        double sSum = (Double) map.get("SUM");
        String sPurp = (String) map.get("PURP");
        String sSql = ResourceBundle.getBundle(OrderServ.class.getName()).getString("ORDER.ADD");
        PreparedStatement ps = con.prepareStatement(sSql);
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sOrgID);
            ps.setDate(2, dDate);
            ps.setString(3, sNumber);
            ps.setString(4, sMember);
            ps.setString(5, sAcc);
            ps.setDouble(6, sSum);
            ps.setString(7, sPurp);
            if (sOrderID != null) ps.setString(8, sOrderID); else ps.setNull(8, Types.VARCHAR);
            ps.executeUpdate();
            con.commit();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Возвращает список ордеров
	 * @param con			- Соединение с БД
	 * @param values		- Параметры тип HashMap
	 *							DATE	- Дата поиска
	 *							ORGID	- Идентификатор организации
	 * @param retValue		- Возвращаемое значение тип HashMap[]
	 *							ORGID	- Идентификатор организации
	 *							DATE	- Дата проводки
	 *							NUMBER	- Номер проводки
	 *							MEMBER	- Участник
	 *							SUM		- Сумам
	 *							ORDERID	- Идентифкатор ордера
	 *							PURP	- Назанчение платежа
	 *							ACC		- Счет
	 * @return retValue
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 */
    public static Any getOrderList(Connection con, Any values, Any retValue) throws SQLException, IOException {
        HashMap map = (HashMap) values.extract_Value();
        Date dDate = (Date) map.get("DATE");
        String sOrgID = (String) map.get("ORGID");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(OrderServ.class.getName()).getString("ORDER.GETORDER");
        try {
            if (dDate != null) sSql += " AND DDATE = :B";
            ps = con.prepareStatement(sSql);
            ps.setString(1, sOrgID);
            if (dDate != null) ps.setDate(2, dDate);
            ResultSet rSet = ps.executeQuery();
            ArrayList<HashMap> arVal = new ArrayList<HashMap>();
            while (rSet.next()) {
                HashMap val = new HashMap();
                val.put("ORGID", rSet.getString("IORGID"));
                val.put("DATE", rSet.getDate("DDATE"));
                val.put("NUMBER", rSet.getString("ITRNNUM"));
                val.put("MEMBER", rSet.getString("CMEMBER"));
                val.put("SUM", rSet.getDouble("ISUM"));
                val.put("ORDERID", rSet.getString("IORDERID"));
                val.put("PURP", rSet.getString("CPURP"));
                val.put("ACC", rSet.getString("CORGACC"));
                arVal.add(val);
            }
            retValue.insert_Value(arVal.toArray(new HashMap[arVal.size()]));
            rSet.close();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Удаление транзации
	 * @param con			- Соединение с БД
	 * @param values		- Параметры тип HashMap
	 *							ORGID	- Идентификатор ордера
	 *							ORDERID	- Идентификатор ордера
	 * @param retValue
	 * @return ничего не возвращают
	 * @throws java.sql.SQLException
	 */
    public static Any delOrder(Connection con, Any values, Any retValue) throws SQLException {
        HashMap map = (HashMap) values.extract_Value();
        String sOrgID = (String) map.get("ORGID");
        String sOrderID = (String) map.get("ORDERID");
        String sSql = ResourceBundle.getBundle(OrderServ.class.getName()).getString("ORDER.DEL");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sOrgID);
            ps.setString(2, sOrderID);
            ps.executeUpdate();
            con.commit();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Выгрузка в формат putdos
	 * @param con			- Соедиение с БД
	 * @param values		- Параметры тип HashMap
	 *							ORGID	- Идентификатор организации
	 *							DATE	- Дата проводки
	 * @param retValue		- Возвращаемое значение тип HashMap[]
	 *							PUTDOS	- Строка в файл выгрузки
	 * @return retValue
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 */
    public static Any putdosOrder(Connection con, Any values, Any retValue) throws SQLException, IOException {
        HashMap map = (HashMap) values.extract_Value();
        String sOrgID = (String) map.get("ORGID");
        Date dDate = (Date) map.get("DATE");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(OrderServ.class.getName()).getString("ORDER.PUTDOS");
        try {
            if (dDate != null) sSql += " AND DDATE = :B";
            ps = con.prepareStatement(sSql);
            ps.setString(1, sOrgID);
            if (dDate != null) ps.setDate(2, dDate);
            ResultSet rSet = ps.executeQuery();
            ArrayList<HashMap> arVal = new ArrayList<HashMap>();
            while (rSet.next()) {
                HashMap val = new HashMap();
                String sPutdos = rSet.getString("CPUTDOS");
                val.put("PUTDOS", sPutdos);
                arVal.add(val);
            }
            rSet.close();
            retValue.insert_Value(arVal.toArray(new HashMap[arVal.size()]));
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Выгрузка в формат putdos
	 * @param con			- Соедиение с БД
	 * @param values		- Параметры тип HashMap
	 *							ORGID	- Идентификатор организации
	 *							DATE	- Дата проводки
	 * @param retValue		- Возвращаемое значение тип HashMap[]
	 *							MEMDOS	- Строка в файл выгрузки
	 * @return retValue
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 */
    public static Any memdosOrder(Connection con, Any values, Any retValue) throws SQLException, IOException {
        HashMap map = (HashMap) values.extract_Value();
        String sOrgID = (String) map.get("ORGID");
        Date dDate = (Date) map.get("DATE");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(OrderServ.class.getName()).getString("ORDER.MEMDOS");
        try {
            if (dDate != null) sSql += " AND DDATE = :B";
            ps = con.prepareStatement(sSql);
            ps.setString(1, sOrgID);
            if (dDate != null) ps.setDate(2, dDate);
            ResultSet rSet = ps.executeQuery();
            ArrayList<HashMap> arVal = new ArrayList<HashMap>();
            while (rSet.next()) {
                HashMap val = new HashMap();
                String sPutdos = rSet.getString("CMEMDOS");
                val.put("MEMDOS", sPutdos);
                arVal.add(val);
            }
            rSet.close();
            retValue.insert_Value(arVal.toArray(new HashMap[arVal.size()]));
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Функция возвращает список орждеров на печать
	 * @param con			- Соединение с БД
	 * @param values		- Параметры тип HashMap
	 *							ORGID	- Идентификатор организации
	 *							DATE	- Дата проводки
	 * @param retValue		- 
	 * @return
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 */
    public static Any prnOrder(Connection con, Any values, Any retValue) throws SQLException, IOException {
        HashMap map = (HashMap) values.extract_Value();
        String sOrderID = (String) map.get("ORDERID");
        String sOrgID = (String) map.get("ORGID");
        Date dDate = (Date) map.get("DATE");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(OrderServ.class.getName()).getString("ORDER.PRN");
        try {
            if (dDate != null) sSql += " AND DDATE = :B";
            if (sOrderID != null) sSql += " AND IORDERID = :C";
            ps = con.prepareStatement(sSql);
            ps.setString(1, sOrgID);
            int i = 2;
            if (dDate != null) {
                ps.setDate(i, dDate);
                i++;
            }
            if (sOrderID != null) {
                ps.setString(i, sOrderID);
                i++;
            }
            ResultSet rSet = ps.executeQuery();
            ArrayList<HashMap> arVal = new ArrayList<HashMap>();
            while (rSet.next()) {
                HashMap val = new HashMap();
                val.put("ORGID", rSet.getString("IORGID"));
                val.put("NUMBER", rSet.getString("ITRNNUM"));
                val.put("DATE", rSet.getDate("DDATE"));
                val.put("MEMBER", rSet.getString("CMEMBER"));
                val.put("SUM", rSet.getDouble("ISUM"));
                val.put("INFOACC", rSet.getString("CORG_INFOACC"));
                val.put("SVODACC", rSet.getString("CORG_SVODACC"));
                val.put("ACC", rSet.getString("CORGACC"));
                val.put("SSUM", rSet.getString("SSUM"));
                val.put("SDATE", rSet.getString("SDATE"));
                arVal.add(val);
            }
            retValue.insert_Value(arVal.toArray(new HashMap[arVal.size()]));
            arVal.clear();
            rSet.close();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Функция возвращает список работников организации
	 * @param con		- Соеинение с БД
	 * @param values	- Параметры HashMap
	 *						ORGID		- Идентификатор организации
	 *						NAME		- Название пользователя - необязательный параметр
	 *									  используется в качестве шаблона для поиска
	 * @param retValue	- Список пользователей HashMap
	 *						MEMID		- Идентификатор работника
	 *						FNAME		- Имя
	 *						MNAME		- Отчество
	 *						LNAME		- Фамилия
	 *						ORGACC		- Номер счета
	 * @return
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 */
    public static Any getMemberList(Connection con, Any values, Any retValue) throws SQLException, IOException {
        HashMap map = (HashMap) values.extract_Value();
        String sOrgID = (String) map.get("ORGID");
        String cFindName = (String) map.get("NAME");
        PreparedStatement ps = null;
        String sSql = ResourceBundle.getBundle(OrderServ.class.getName()).getString("MEMBER.GETLIST");
        try {
            if (cFindName != null) sSql += " AND (UPPER(CFNAME) like UPPER('%" + cFindName + "%') or UPPER(CMNAME) like UPPER('%" + cFindName + "%') " + "or UPPER(CLNAME) like UPPER('%" + cFindName + "%')" + "or UPPER (CORGACC) like UPPER('%" + cFindName + "%'))";
            ps = con.prepareStatement(sSql);
            ps.setString(1, sOrgID);
            ResultSet rSet = ps.executeQuery();
            ArrayList<HashMap> arVal = new ArrayList<HashMap>();
            while (rSet.next()) {
                HashMap val = new HashMap();
                val.put("MEMID", rSet.getString("IIDMEM"));
                val.put("FNAME", rSet.getString("CFNAME"));
                val.put("MNAME", rSet.getString("CMNAME"));
                val.put("LNAME", rSet.getString("CLNAME"));
                val.put("ORGACC", rSet.getString("CORGACC"));
                arVal.add(val);
            }
            retValue.insert_Value(arVal.toArray(new HashMap[arVal.size()]));
            rSet.close();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Добавление/Изменение работника
	 * @param con			- Соединение с БД
	 * @param values		- Параметры HashMap
	 *							ORGID	- Идентификатор организации
	 *							MEMID	- Идентификатор работника(если есть)
	 *							FNAME	- Имя
	 *							MNAME	- Отчество
	 *							LNAME	- Фамилия
	 *							ORGACC	- Номер счета
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any addMember(Connection con, Any values, Any retValue) throws SQLException {
        HashMap map = (HashMap) values.extract_Value();
        String sOrgID = (String) map.get("ORGID");
        String sMemID = (String) map.get("MEMID");
        String cFName = (String) map.get("FNAME");
        String cMName = (String) map.get("MNAME");
        String cLName = (String) map.get("LNAME");
        String cAcc = (String) map.get("ORGACC");
        String sSql = ResourceBundle.getBundle(OrderServ.class.getName()).getString("MEMBER.ADD");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sSql);
            if (sMemID != null && sMemID.length() > 0) ps.setString(1, sMemID); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, sOrgID);
            ps.setString(3, cFName);
            ps.setString(4, cMName);
            ps.setString(5, cLName);
            ps.setString(6, cAcc);
            ps.executeUpdate();
            con.commit();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Удаление сотрудника
	 * @param con			- Соединение с БД
	 * @param values		- Параметры HashMap
	 *							MEMID	- Иденьтификатор сотрудника
	 * @param retValue
	 * @return
	 * @throws java.sql.SQLException
	 */
    public static Any delMember(Connection con, Any values, Any retValue) throws SQLException {
        String sMemID = values.extract_wstring();
        String sSql = ResourceBundle.getBundle(OrderServ.class.getName()).getString("MEMBER.DEL");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sSql);
            ps.setString(1, sMemID);
            ps.executeUpdate();
            con.commit();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Возвращает печатную форму для приходного ордера, для поиска формы используются параметры
	 *		INFORM и FORMDIR из файла properties
	 * @param con		- не используется
	 * @param values	- не используется
	 * @param retValue возвращаемое значение тип byte[] в сжатом виде
	 * @return
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 */
    public static Any getOrderForm(Connection con, Any values, Any retValue) throws FileNotFoundException, IOException {
        String sForm = ResourceBundle.getBundle(OrderServ.class.getName()).getString("INFORM");
        String sDir = ResourceBundle.getBundle(OrderServ.class.getName()).getString("FORMDIR");
        FileInputStream fi = new FileInputStream(sDir + File.separatorChar + sForm);
        byte[] bData = new byte[(int) fi.getChannel().size()];
        fi.read(bData);
        fi.close();
        retValue.insert_Value(ObjUtil.compress(bData));
        return retValue;
    }

    private OrderServ() {
    }
}
