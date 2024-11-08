package org.riverock.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.lang.reflect.Method;
import org.riverock.common.tools.MainTools;
import org.riverock.common.tools.StringTools;
import org.riverock.common.config.JsmithyNamespases;
import org.riverock.common.config.PropertiesProvider;
import org.riverock.generic.config.GenericConfig;
import org.riverock.generic.db.DatabaseAdapter;
import org.riverock.generic.db.DatabaseManager;
import org.riverock.generic.schema.db.structure.*;
import org.riverock.generic.startup.StartupApplication;
import org.riverock.generic.tools.XmlTools;
import org.riverock.schema.gen.*;
import org.riverock.schema.gen.types.ExceptionDefinitionTypeExceptionTypeType;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.Namespaces;
import org.exolab.castor.xml.schema.*;
import org.exolab.castor.xml.schema.util.DatatypeHandler;
import org.exolab.castor.xml.schema.writer.SchemaWriter;
import org.exolab.castor.util.Version;
import org.xml.sax.InputSource;

/**
 * User: Admin
 * Date: Feb 28, 2003
 * Time: 12:02:41 AM
 *
 * $Id: CreateSchemaFromDb.java,v 1.15 2006/06/05 19:18:40 serg_main Exp $
 */
public final class CreateSchemaFromDb {

    private static SchemaGenConfigType config = null;

    private static String packageClass = null;

    private static LoggerType logger = null;

    private static DatabaseType db = null;

    private static boolean isApplModule = false;

    private static Schema schema = new Schema();

    private static DbSchemaType dbSchema = null;

    private static ComplexType baseType = null;

    private static boolean isInsertInterface = false;

    private static boolean isUpdateInterface = false;

    private static boolean isDeleteInterface = false;

    private static String insertInterface = null;

    private static String updateInterface = null;

    private static String deleteInterface = null;

    private static String getAttributeName(DbFieldType field) {
        String s = StringTools.capitalizeString(field.getName());
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private static String getCopyItemMethod(String classNameItem, DbTableType table) throws Exception {
        String s = "     public void copyItem(" + classNameItem + " target)\n" + "     {\n" + "         copyItem(this.item, target);\n" + "     }\n" + "\n" + "     public static void copyItem(" + classNameItem + " source, " + classNameItem + " target)\n" + "     {\n" + "         if (source==null || target==null)\n" + "             return;\n" + "\n";
        for (int i = 0; i < table.getFieldsCount(); i++) {
            DbFieldType field = table.getFields(i);
            if (field.getName().toLowerCase().startsWith("is_")) {
                s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n";
            } else if (isKeyField(field)) {
                s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n";
            } else {
                if (field.getJavaType() != null) switch(field.getJavaType().intValue()) {
                    case Types.DECIMAL:
                        if (field.getDecimalDigit() == null || field.getDecimalDigit() == 0) {
                            if (field.getSize() < 7) s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n"; else s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n";
                        } else s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n";
                        break;
                    case Types.INTEGER:
                        if (field.getSize() < 7) s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n"; else s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n";
                        break;
                    case Types.DOUBLE:
                        s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n";
                        break;
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.LONGVARBINARY:
                        s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n";
                        break;
                    case Types.DATE:
                    case Types.TIMESTAMP:
                        s += "         target.set" + StringTools.capitalizeString(field.getName()) + "( " + "source.get" + StringTools.capitalizeString(field.getName()) + "() );\n";
                        break;
                    default:
                        field.setJavaStringType("unknown field type field - " + field.getName() + " javaType - " + field.getJavaType());
                        System.out.println("unknown field type field - " + field.getName() + " javaType - " + field.getJavaType());
                }
            }
        }
        s += "     }\n";
        return s;
    }

    private static String putCacheAndInstanceAccessor(String className, String classNameItem, boolean isResultCanBeNull) {
        return "     public boolean isFound = false;\n" + "\n" + (config.getIsUseCache() ? "    private static org.riverock.generic.main.CacheFactoryWithDb cache = new org.riverock.generic.main.CacheFactoryWithDb( " + className + ".class.getName() );\n" + "\n" + "    public static void reinit()\n" + "    {\n" + "        cache.reinit();\n" + "    }\n" + "\n" : "") + "    public static " + className + " getInstance(" + db.getFactoryMethod() + " db__, long id__) " + putExceptionDefinition() + "    {\n" + "        return getInstance(db__, new Long(id__) );\n" + "    }\n" + "\n" + putGetInstanceMethod(className, isResultCanBeNull) + "    /**\n" + "     * @deprecated use getInstance() method\n" + "     */\n" + "    public " + classNameItem + " getData(" + db.getFactoryMethod() + " db_, long id) " + putExceptionDefinition() + "    {\n" + "        " + className + " obj = " + className + ".getInstance(db_, id);\n" + "        if (obj!=null)\n" + "            return obj.item;\n" + "\n" + "        return new " + classNameItem + "();\n" + "    }\n" + "\n" + "    public " + className + "(" + db.getFactoryMethod() + " db_, long id) " + putExceptionDefinition() + "    {\n" + "        this(db_, new Long(id));\n" + "    }\n" + "\n";
    }

    private static String putExceptionDefinition() {
        if (config.getPersistenceExceptionName() == null) return " throws Exception ";
        if (config.getPersistenceExceptionName().getExceptionType().getType() == ExceptionDefinitionTypeExceptionTypeType.WRAP_PERSISTENCE_TYPE) {
            if (config.getPersistenceExceptionName().getPersistenceExceptionName() == null) throw new IllegalArgumentException("Not defined PersistebceExceptionName"); else return " throws " + config.getPersistenceExceptionName().getPersistenceExceptionName() + " ";
        }
        return " throws Exception ";
    }

    private static String putImportAndDeclarePart(String classNameItem, String className) {
        return putImportAndDeclarePart(classNameItem, className, null);
    }

    private static String putImportAndDeclarePart(String classNameItem, String className, String addImport) {
        return "package " + packageClass + ";\n" + "\n" + (isApplModule ? "import org.riverock.a3.AuthSession;\n" + "import org.riverock.a3.AccessDeniedException;\n" + "import org.riverock.as.ApplicationInterface;\n" + "import org.riverock.as.server.ApplicationTools;\n" + "import org.riverock.schema.appl_server.ResourceRequestType;\n" + "import org.riverock.schema.appl_server.ResourceRequestParameterType;\n" : "") + (addImport != null ? addImport : "") + "import " + config.getJavaPackageXmlSchema() + '.' + classNameItem + ";\n" + initSqlStatementImport() + "\n" + "import java.sql.PreparedStatement;\n" + "import java.sql.ResultSet;\n" + "\n" + declareClass(className) + "{\n" + initLogging(packageClass + '.' + className) + "\n";
    }

    private static String initSqlStatementImport() {
        return (config.getIsUseCache() ? "import org.riverock.sql.cache.SqlStatement;\n" : "");
    }

    private static String putGetInstanceMethod(String className, boolean isResultCanBeNull) {
        String s = "     public static " + className + " getInstance(" + db.getFactoryMethod() + " db__, Long id__) " + putExceptionDefinition() + "     {\n";
        if (config.getPersistenceExceptionName() != null && config.getPersistenceExceptionName().getExceptionType().getType() == ExceptionDefinitionTypeExceptionTypeType.WRAP_PERSISTENCE_TYPE) {
            if (config.getPersistenceExceptionName().getPersistenceExceptionName() == null) throw new IllegalArgumentException("Not defined PersistebceExceptionName");
            s += "        try\n" + "        {\n";
        }
        if (config.getIsUseCache()) s += "         return (" + className + ") cache.getInstanceNew(db__, id__ );\n"; else {
            s += "         return new " + className + "(db__, id__ );\n";
        }
        if (config.getPersistenceExceptionName() != null && config.getPersistenceExceptionName().getExceptionType().getType() == ExceptionDefinitionTypeExceptionTypeType.WRAP_PERSISTENCE_TYPE) {
            if (config.getPersistenceExceptionName().getPersistenceExceptionName() == null) throw new IllegalArgumentException("Not defined PersistebceExceptionName");
            String exception = null;
            if (config.getIsUseCache()) exception = "Exception"; else exception = config.getPersistenceExceptionName().getPersistenceExceptionName();
            s += "        }\n" + "        catch(" + exception + " exc)\n" + "        {\n" + "            throw new " + config.getPersistenceExceptionName().getPersistenceExceptionName() + "( exc.getMessage(), exc );\n" + "        }\n";
        }
        s += "     }\n" + "\n";
        return s;
    }

    private static String declareClass(String className) {
        return "public class " + className + (isApplModule ? "implements ApplicationInterface" : "") + "\n";
    }

    private static String putAuthCheck(DbTableType table, String accessActionType, String className) {
        if (table == null) return "";
        SchemaGenAuthAccessType authData = null;
        for (int i = 0; i < config.getAuthDataCount(); i++) {
            authData = config.getAuthData(i);
            if (table.getName().equals(authData.getTableName())) {
                return "        boolean isAccessDenied = !authSession.isUserInRole(\"" + authData.getRoleName() + "\");\n" + "        if (isAccessDenied)\n" + "        {\n" + "             String errorString = \"You haven't right to process request in " + className + " class\";\n" + (config.getIsUseLogging() ? "             cat.error( errorString );\n" : "") + "             throw new AccessDeniedException( errorString );\n" + "        }\n";
            }
        }
        return "";
    }

    private static String getEndOfClass(String className, String classNameItem, String debugTable, DbTableType table, String accessAction, boolean isAddParam, boolean isCloseRsPs, boolean isFillBean) throws Exception {
        String s = "        }\n" + "        catch (Exception e)\n" + "        {\n" + (config.getIsUseLogging() ? "            cat.error(\"Exception create object\", e);\n" : "");
        if (config.getPersistenceExceptionName() != null && config.getPersistenceExceptionName().getExceptionType().getType() == ExceptionDefinitionTypeExceptionTypeType.WRAP_PERSISTENCE_TYPE) {
            if (config.getPersistenceExceptionName().getPersistenceExceptionName() == null) throw new IllegalArgumentException("Not defined PersistebceExceptionName");
            s += "            throw new " + config.getPersistenceExceptionName().getPersistenceExceptionName() + "( e.getMessage(), e );\n";
        } else s += "            throw e;\n";
        s += "        }\n" + "        catch (Error err)\n" + "        {\n" + (config.getIsUseLogging() ? "            cat.error(\"Error create object\", err);\n" : "") + "            throw err;\n" + "        }\n" + (isCloseRsPs ? "        finally\n" + "        {\n" + putCloseFactoryMethod() + "            rs = null;\n" + "            ps = null;\n" + "        }\n" : "") + "\n" + "    }\n" + "\n";
        if (isApplModule) {
            s += "     public byte[] processRequest( ResourceRequestType applReq, AuthSession authSession ) " + putExceptionDefinition() + "     {\n";
            if (isAddParam) {
                s += "         if (applReq==null || applReq.getParametersCount()==0)\n" + "             return null;\n" + "\n";
            }
            s += putAuthCheck(table, accessAction, className);
            if (isAddParam) {
                s += "\n" + "         Long id = null;\n" + "         for (int i=0; i<applReq.getParametersCount(); i++)\n" + "         {\n" + "             ResourceRequestParameterType param = applReq.getParameters(i);\n" + "             if (\"mill.id\".equals( param.getNameParameter()))\n" + "             {\n" + "                 String stringParam = ApplicationTools.decodeParameter( param );\n" + "\n" + (config.getIsUseLogging() ? "                 if (cat.isDebugEnabled())\n" + "                     cat.debug(\"Parameter is \"+stringParam);\n" : "") + "\n" + "                 id = new Long( stringParam );\n" + "                 break;\n" + "             }\n" + "         }\n" + "         if (id == null )\n" + "             return null;\n";
            }
            s += "\n" + "         String[][] ns = new String[][]\n" + "         {\n" + "              { \"" + config.getCustomNamespacePrefix() + "\", \"" + config.getCustomNamespace() + "\" }\n" + "         };\n" + "\n" + "         " + db.getFactoryMethod() + " db_ = null;\n" + "         " + classNameItem + " resultItem = null;\n" + "         try\n" + "         {\n" + "             db_ = " + db.getFactoryMethod() + ".getInstance( false );\n" + "             resultItem = " + className + ".getInstance( db_, " + (isAddParam ? " id " : "0") + " ).item;\n" + "         }\n" + "         catch(Exception e)\n" + "         {\n" + (config.getIsUseLogging() ? "             cat.error(\"Exception " + className + ".getInstance()\", e);\n" : "") + "             throw e;\n" + "         }\n" + "         catch(Error e)\n" + "         {\n" + (config.getIsUseLogging() ? "             cat.error(\"Error " + className + ".getInstance()\", e);\n" : "") + "             throw e;\n" + "         }\n" + "         finally\n" + "         {\n" + "             " + db.getFactoryMethod() + ".close( db_ );\n" + "             db_ = null;\n" + "         }\n" + "         return org.riverock.generic.tools.XmlTools.getXml(resultItem, \"" + classNameItem + "\", ns);\n" + "     }\n" + "\n";
        }
        if (isFillBean) s += getBeanFiller(classNameItem, table);
        if (isCloseRsPs) s += getCloseRsPs();
        s += (config.getIsMainMethod() ? "   public static void main(String args[])\n" + "       throws Exception\n" + "   {\n" + "       org.riverock.startup.StartupApplication.init();\n" + "\n" + "       long id = 1;\n" + "       " + classNameItem + " resultItem = " + className + ".getInstance( " + db.getFactoryMethod() + ".getInstance( false ), id ).item;\n" + "\n" + "       String[][] ns = new String[][]\n" + "       {\n" + "           { \"" + config.getCustomNamespacePrefix() + "\", \"" + config.getCustomNamespace() + "\" }\n" + "       };\n" + "\n" + "       org.riverock.tools.XmlTools.writeToFile(\n" + "           resultItem,\n" + "           org.riverock.startup.InitParam.getMillDebugDir()+\"" + debugTable + "\",\n" + "           \"utf-8\",\n" + "           null,\n" + "           ns\n" + "       );\n" + "   }\n" : "") + "}\n";
        return s;
    }

    /**
     * End of update, insert and delete classes
     * @@param className
     * @@param classNameItem
     * @@return
     * @@throws Exception
     */
    private static String getEndOfClassUID(String className, String classNameItem, DbTableType table, String accessAction, String sqlName) throws Exception {
        String s = "         }\n" + "         catch (Exception e) {\n";
        if (config.getIsUseLogging()) {
            for (int i = 0; i < table.getFieldsCount(); i++) {
                DbFieldType field = table.getFields(i);
                String capitalizeName = StringTools.capitalizeString(field.getName()) + "()";
                s += "             cat.error(\"Item get" + capitalizeName + ", value - \"+item.get" + capitalizeName + ");\n";
            }
        }
        if (config.getIsUseLogging()) s += "             cat.error(\"SQL \"+" + sqlName + ");\n" + "             cat.error(\"Exception insert data in db\", e);\n";
        if (config.getPersistenceExceptionName() != null && config.getPersistenceExceptionName().getExceptionType().getType() == ExceptionDefinitionTypeExceptionTypeType.WRAP_PERSISTENCE_TYPE) {
            if (config.getPersistenceExceptionName().getPersistenceExceptionName() == null) throw new IllegalArgumentException("Not defined PersistebceExceptionName");
            s += "            throw new " + config.getPersistenceExceptionName().getPersistenceExceptionName() + "( e.getMessage(), e );\n";
        } else s += "            throw e;\n";
        s += "        }\n" + "        finally {\n" + putCloseFactoryMethod() + "            rs = null;\n" + "            ps = null;\n" + "        }\n" + "\n" + "    }\n" + "\n" + (isApplModule ? "     public byte[] processRequest( ResourceRequestType applReq, AuthSession authSession )\n" + "         throws Exception\n" + "     {\n" + "          if (applReq==null || applReq.getParametersCount()==0)\n" + "              return null;\n" + "\n" + putAuthCheck(table, accessAction, className) + "\n" + "          " + classNameItem + " item = null;\n" + "          for (int i=0; i<applReq.getParametersCount(); i++)\n" + "          {\n" + "              ResourceRequestParameterType param = applReq.getParameters(i);\n" + "              if (\"mill.item\".equals( param.getNameParameter()))\n" + "              {\n" + "                  String stringParam = ApplicationTools.decodeParameter( param );\n" + "\n" + (config.getIsUseLogging() ? "                  if (cat.isDebugEnabled())\n" + "                      cat.debug(\"Parameter is \"+stringParam);\n" : "") + "\n" + "                  org.xml.sax.InputSource inSrc = new org.xml.sax.InputSource( new java.io.StringReader(stringParam) );\n" + "                  item = (" + classNameItem + ") org.exolab.castor.xml.Unmarshaller.unmarshal(" + classNameItem + ".class, inSrc);\n" + "                  break;\n" + "              }\n" + "          }\n" + "          if (item == null )\n" + "              return null;\n" + "\n" + "          " + db.getFactoryMethod() + " db_ = null;\n" + "          try" + "          {\n" + "              db_ = " + db.getFactoryMethod() + ".getInstance( true );\n" + "              db_.getConnection().setAutoCommit(false);\n" + "              long resultLong = " + className + ".process( db_, item );\n" + "              db_.commit();\n" + "              org.riverock.schema.appl_server.LongResultType result = new org.riverock.schema.appl_server.LongResultType();\n" + "              result.setResult(resultLong);\n" + "              return org.riverock.generic.tools.XmlTools.getXml(result, null);\n" + "          }\n" + "          catch(Exception e)\n" + "          {\n" + (config.getIsUseLogging() ? "              cat.error(\"Exception " + className + "process\", e);\n" : "") + "              db_.rollback();\n" + "              throw e;\n" + "          }\n" + "          catch(Error e)\n" + "          {\n" + (config.getIsUseLogging() ? "              cat.error(\"Error " + className + "process\", e);\n" : "") + "              db_.rollback();\n" + "              throw e;\n" + "          }\n" + "          finally\n" + "          {\n" + "              " + db.getFactoryMethod() + ".close( db_ );\n" + "              db_ = null;\n" + "          }\n" + "      }\n" + "\n" : "");
        return s;
    }

    private static String getCloseRsPs() {
        if (config.getCloseFactoryMethod() != null) return "";
        return "    private static void _closeRsPs(ResultSet rs, PreparedStatement ps)\n" + "    {\n" + "        if (rs != null)\n" + "        {\n" + "            try\n" + "            {\n" + "                rs.close();\n" + "                rs = null;\n" + "            }\n" + "            catch (Exception e01)\n" + "            {\n" + "            }\n" + "        }\n" + "        if (ps != null)\n" + "        {\n" + "            try\n" + "            {\n" + "                ps.close();\n" + "                ps = null;\n" + "            }\n" + "            catch (Exception e02)\n" + "            {\n" + "            }\n" + "        }\n" + "    }\n" + "\n";
    }

    private static String getMainMethod(String classNameItem, String className, String debugTable) {
        return getCloseRsPs() + (config.getIsMainMethod() ? "    public static void main(String args[])\n" + "        throws Exception\n" + "    {\n" + "        org.riverock.startup.StartupApplication.init();\n" + "\n" + "        " + classNameItem + " data = new " + classNameItem + "();\n" + "\n" + "//      data.set();\n" + "\n" + "        long resultLong = 0;\n" + "        " + db.getFactoryMethod() + " db_ = null;\n" + "        try\n" + "        {\n" + "            db_ = " + db.getFactoryMethod() + ".getInstance( false );\n" + "            resultLong = " + className + ".process( db_, data );\n" + "        }\n" + "        catch(Exception e)\n" + "        {\n" + (config.getIsUseLogging() ? "            cat.error(\"Exception " + className + ".process()\", e);\n" : "") + "            throw e;\n" + "        }\n" + "        catch(Error e)\n" + "        {\n" + (config.getIsUseLogging() ? "            cat.error(\"Error " + className + ".process()\", e);\n" : "") + "            throw e;\n" + "        }\n" + "        finally\n" + "        {\n" + "            " + db.getFactoryMethod() + ".close( db_ );\n" + "            db_ = null;\n" + "        }\n" + "\n" + "        org.riverock.schema.appl_server.LongResultType result = new org.riverock.schema.appl_server.LongResultType();\n" + "        result.setResult( resultLong );\n" + "\n" + "        org.riverock.tools.XmlTools.writeToFile(\n" + "            result,\n" + "            org.riverock.startup.InitParam.getMillDebugDir()+\"" + debugTable + "\",\n" + "            \"utf-8\",\n" + "            null\n" + "        );\n" + "    }\n" : "") + "}\n";
    }

    private static void createGetClassForFk(DbTableType table) throws Exception {
        DbPrimaryKeyType pk = table.getPrimaryKey();
        if (pk.getColumnsCount() == 0) {
            System.out.println("Table '" + table.getName() + "' can not be processed, primary key not defined");
            return;
        }
        DbPrimaryKeyColumnType column = pk.getColumns(0);
        for (int k = 0; k < table.getImportedKeysCount(); k++) {
            DbImportedPKColumnType key = table.getImportedKeys(k);
            if (DatabaseManager.isMultiColumnFk(table, key)) {
                System.out.println("Create class list for table with multicolumn foreign keys not supported");
                continue;
            }
            String base = StringTools.capitalizeString(table.getName());
            String className = "Get" + base + "With" + StringTools.capitalizeString(key.getFkColumnName()) + "List";
            String classNameItem = StringTools.capitalizeString(table.getName()) + "ListType";
            String s = putImportAndDeclarePart(classNameItem, className) + "     public " + classNameItem + " item = new " + classNameItem + "();\n" + "\n" + "     public " + className + "(){}\n" + "\n" + putCacheAndInstanceAccessor(className, classNameItem, false) + "\n" + initSql(className, base, "        \"select * \"+\n" + "        \"from " + table.getName() + " \"+\n" + "        \"where " + key.getFkColumnName() + "=? \"+\n" + "        \"order by " + column.getColumnName() + " ASC\";\n") + "    public " + className + "(" + db.getFactoryMethod() + " db_, Long id) " + putExceptionDefinition() + "    {\n" + "        this(db_, id, sql_);\n" + "    }\n" + "\n" + "    public " + className + "(" + db.getFactoryMethod() + " db_, Long id, String sqlString) " + putExceptionDefinition() + "    {\n" + "\n" + "        if (id==null)\n" + "            return;\n" + "\n" + "        PreparedStatement ps = null;\n" + "        ResultSet rs = null;\n" + "        try\n" + "        {\n" + "            ps = db_.prepareStatement(sqlString);\n" + "            ps.setLong(1, id.longValue());\n" + "\n" + "            rs = ps.executeQuery();\n" + "            while (rs.next())\n" + "            {\n" + (config.getIsUseCache() ? "                Get" + base + "Item tempItem = " + "Get" + base + "Item.getInstance(db_, rs.getLong(\"" + column.getColumnName() + "\"));\n" + "                if (tempItem!=null && tempItem.item!=null)\n" + "                {\n" + "                    if (item==null)\n" + "                        item = new " + classNameItem + "();\n" + "\n" + "                    this.isFound = true;\n" + "                    item.add" + StringTools.capitalizeString(table.getName()) + "( tempItem.item );\n" + "                }\n" : "                if (item==null)\n" + "                    item = new " + classNameItem + "();\n" + "\n" + "                this.isFound = true;\n" + "                item.add" + StringTools.capitalizeString(table.getName()) + "( Get" + base + "Item.fillBean(rs) );\n") + "            }\n";
            s += getEndOfClass(className, classNameItem, "test-" + table.getName().toLowerCase() + "-list.xml", table, "S", true, true, false);
            writeClass(className, s);
        }
    }

    private static void writeClass(String className, String s) throws Exception {
        String d = preparePath(config.getJavaPackagePath()) + (packageClass.replace('.', File.separatorChar)) + File.separatorChar;
        File file = new File(d);
        if (!file.exists()) {
            System.out.println("Directory " + d + " nore exists. Create dir.");
            file.mkdirs();
        }
        String javaClassFile = d + className + ".java";
        System.out.println("      write class " + className + " to file " + javaClassFile);
        MainTools.writeToFile(javaClassFile, s.getBytes());
    }

    private static String initSql(String className, String base, String sql) {
        if (config.getIsUseCache()) return "    private static String sql_ = null;\n" + "    static\n" + "    {\n" + "         sql_ =\n" + "             " + sql + "\n" + "\n" + (!className.equals("Get" + base + "Item") ? "         try\n" + "         {\n" + "             SqlStatement.registerRelateClass( " + className + ".class, Get" + base + "Item.class);\n" + "\n" + "         }\n" + "         catch(Exception e)\n" + "         {\n" + (config.getIsUseLogging() ? "             cat.error(\"Exception in registerRelateClass, sql\\n\"+sql_, e);\n" : "") + "         }\n\n" : "\n") + "         try\n" + "         {\n" + "             SqlStatement.registerSql( sql_, " + className + ".class );\n" + "         }\n" + "         catch(Exception e)\n" + "         {\n" + (config.getIsUseLogging() ? "             cat.error(\"Exception in registerSql, sql\\n\"+sql_, e);\n" : "") + "         }\n" + "    }\n" + "\n"; else return "    private static String sql_ = \n" + "        " + sql + "\n\n";
    }

    private static void createGetClassFullTable(DbTableType table) throws Exception {
        boolean isNotFound = true;
        for (int i = 0; i < config.getTableToListCount(); i++) {
            if (table.getName().equals(config.getTableToList(i))) {
                isNotFound = false;
                break;
            }
        }
        if (isNotFound) return;
        if (table.getPrimaryKey().getColumnsCount() != 1) {
            System.out.println("Can not create full list of table '" + table.getName() + "' 'cos multicolumn PK not supported");
            return;
        }
        String base = StringTools.capitalizeString(table.getName());
        String className = "Get" + base + "FullList";
        String classNameItem = StringTools.capitalizeString(table.getName()) + "ListType";
        String s = putImportAndDeclarePart(classNameItem, className) + "     public " + classNameItem + " item = new " + classNameItem + "();\n" + "\n" + "     public " + className + "(){}\n" + "\n" + putCacheAndInstanceAccessor(className, classNameItem, false) + "\n" + initSql(className, base, "\"select " + table.getPrimaryKey().getColumns(0).getColumnName() + " " + "from " + table.getName() + " \";\n") + "    public " + className + "(" + db.getFactoryMethod() + " db_, Long id) " + putExceptionDefinition() + "    {\n" + "\n" + "        PreparedStatement ps = null;\n" + "        ResultSet rs = null;\n" + "        try\n" + "        {\n" + "            ps = db_.prepareStatement(sql_);\n" + "\n" + "            rs = ps.executeQuery();\n" + "            while (rs.next())\n" + "            {\n" + "\n" + "                Get" + base + "Item tempItem = " + "Get" + base + "Item" + ".getInstance(db_, rs.getLong( \"" + table.getPrimaryKey().getColumns(0).getColumnName() + "\" ));\n" + "                if (tempItem!=null && tempItem.item!=null)\n" + "                {\n" + "                    this.isFound = true;\n" + "                    item.add" + StringTools.capitalizeString(table.getName()) + "( tempItem.item );\n" + "                }\n" + "            }\n";
        s += getEndOfClass(className, classNameItem, "test-" + table.getName().toLowerCase() + "-list.xml", table, "S", false, true, false);
        writeClass(className, s);
    }

    public static String declareVars = null;

    public static boolean declareIntVars = false;

    public static boolean declareLongVars = false;

    public static boolean declareBooleanVars = false;

    public static boolean declareDoubleVars = false;

    public static boolean declareTimestampVars = false;

    public static boolean declareStringVars = false;

    private static void createGetClassForPk(DbTableType table) throws Exception {
        declareVars = "";
        declareIntVars = false;
        declareLongVars = false;
        declareBooleanVars = false;
        declareDoubleVars = false;
        declareTimestampVars = false;
        declareStringVars = false;
        DbPrimaryKeyType pk = table.getPrimaryKey();
        if (pk.getColumnsCount() == 0) {
            return;
        }
        DbPrimaryKeyColumnType column = pk.getColumns(0);
        String base = StringTools.capitalizeString(table.getName());
        String className = "Get" + base + "Item";
        String classNameItem = base + config.getClassNameSufix();
        String s1 = "package " + packageClass + ";\n" + "\n" + (isApplModule ? "import org.riverock.a3.AuthSession;\n" + "import org.riverock.a3.AccessDeniedException;\n" + "import org.riverock.as.ApplicationInterface;\n" + "import org.riverock.as.server.ApplicationTools;\n" + "import org.riverock.schema.appl_server.ResourceRequestType;\n" + "import org.riverock.schema.appl_server.ResourceRequestParameterType;\n" : "") + "import " + config.getJavaPackageXmlSchema() + '.' + base + config.getClassNameSufix() + ";\n" + initSqlStatementImport() + "\n" + "import java.sql.PreparedStatement;\n" + "import java.sql.ResultSet;\n" + "\n" + declareClass(className) + "{\n" + initLogging(packageClass + '.' + className) + "\n" + (config.getIsUseCache() ? "    private static org.riverock.generic.main.CacheFactoryWithDb cache = new org.riverock.generic.main.CacheFactoryWithDb( " + className + ".class.getName() );\n" + "\n" + "    public static void reinit()\n" + "    {\n" + "        cache.reinit();\n" + "    }\n" + "\n" : "") + (config.getIsUseObjectWrapper() ? "    public " + classNameItem + " item = null;\n" : "    public " + classNameItem + " item = new " + classNameItem + "();\n") + "\n" + "    public boolean isFound = false;\n" + "\n" + "    public " + className + "(){}\n" + "\n" + "    public static " + className + " getInstance(" + db.getFactoryMethod() + " db__, long id__) " + putExceptionDefinition() + "    {\n" + "        return getInstance(db__, new Long(id__) );\n" + "    }\n" + "\n" + putGetInstanceMethod(className, true) + "    /**\n" + "     * @deprecated use getInstance() method\n" + "     */\n" + "    public " + classNameItem + " getData(" + db.getFactoryMethod() + " db_, long id) " + putExceptionDefinition() + "    {\n" + "         " + className + " obj = " + className + ".getInstance(db_, id);\n" + "        if (obj!=null)\n" + "            return obj.item;\n" + "\n" + "        return new " + classNameItem + "();\n" + "    }\n" + "\n" + getCopyItemMethod(classNameItem, table) + "\n" + "    public " + className + "(" + db.getFactoryMethod() + " db_, long id) " + putExceptionDefinition() + "    {\n" + "        this(db_, new Long(id));\n" + "    }\n" + "\n" + initSql(className, base, "        \"select * from " + table.getName() + " where " + column.getColumnName() + "=?\";\n") + "    public " + className + "(" + db.getFactoryMethod() + " db_, Long id) " + putExceptionDefinition() + "    {\n" + "        this(db_, id, sql_);\n" + "    }\n" + "\n" + "    public " + className + "(" + db.getFactoryMethod() + " db_, Long id, String sqlString) " + putExceptionDefinition() + "    {\n" + "\n" + "        if (id==null)\n" + "            return;\n" + "\n" + "        PreparedStatement ps = null;\n" + "        ResultSet rs = null;\n" + "        try\n" + "        {\n" + "            ps = db_.prepareStatement(sqlString);\n" + "            ps.setLong(1, id.longValue());\n" + "\n" + "            rs = ps.executeQuery();\n" + "            if (rs.next())\n" + "            {\n" + "                item = fillBean(rs);\n" + "                isFound = true;\n" + "            }\n" + "\n" + getEndOfClass(className, classNameItem, "test-" + table.getName().toLowerCase() + "-item.xml", table, "S", true, true, true);
        writeClass(className, s1);
    }

    private static String getBeanFiller(String classNameItem, DbTableType table) {
        String s1 = "    public static " + classNameItem + " fillBean(ResultSet rs)\n" + "        throws java.sql.SQLException\n" + "    {\n" + "        " + classNameItem + " item = new " + classNameItem + "();\n" + "\n";
        String s = "";
        for (int i = 0; i < table.getFieldsCount(); i++) {
            DbFieldType field = table.getFields(i);
            if (isLogicField(field)) {
                s += getBooleanField(i, field);
            } else if (isKeyField(field)) {
                s += getLongField(i, field);
            } else {
                switch(field.getJavaType().intValue()) {
                    case Types.DECIMAL:
                        if (field.getDecimalDigit() == null || field.getDecimalDigit() == 0) {
                            if (field.getSize() < 7) s += getIntField(i, field); else s += getLongField(i, field);
                        } else s += getDoubleField(i, field);
                        break;
                    case Types.INTEGER:
                        if (field.getSize() < 7) s += getIntField(i, field); else s += getLongField(i, field);
                        break;
                    case Types.DOUBLE:
                        s += getDoubleField(i, field);
                        break;
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.LONGVARBINARY:
                        s += getStringField(field);
                        break;
                    case Types.DATE:
                    case Types.TIMESTAMP:
                        s += getTimestampField(field);
                        break;
                    default:
                        field.setJavaStringType("unknown field type field - " + field.getName() + " javaType - " + field.getJavaType());
                        System.out.println("unknown field type field - " + field.getName() + " javaType - " + field.getJavaType());
                }
            }
        }
        return s1 + declareVars + s + "        return item;\n" + "    }\n" + "\n";
    }

    private static String getBooleanField(int i, DbFieldType field) {
        if (!declareBooleanVars) {
            declareVars += "                 int tempBoolean;\n";
            declareBooleanVars = true;
        }
        return "                 tempBoolean = rs.getInt( \"" + field.getName() + "\");\n" + "                 if (!rs.wasNull())\n" + (config.getIsUseObjectWrapper() ? "                     item.set" + StringTools.capitalizeString(field.getName()) + "( new Boolean(tempBoolean==1?true:false) );\n" : "                     item.set" + StringTools.capitalizeString(field.getName()) + "( tempBoolean==1?true:false );\n") + "                 else\n" + (config.getIsUseObjectWrapper() ? "                     item.set" + StringTools.capitalizeString(field.getName()) + "( new Boolean(false) );\n" : "                     item.set" + StringTools.capitalizeString(field.getName()) + "( false );\n");
    }

    private static String getLongField(int i, DbFieldType field) {
        if (!declareLongVars) {
            declareVars += "                 long tempLong;\n";
            declareLongVars = true;
        }
        return "                 tempLong = rs.getLong( \"" + field.getName() + "\");\n" + "                 if (!rs.wasNull())\n" + (config.getIsUseObjectWrapper() ? "                     item.set" + StringTools.capitalizeString(field.getName()) + "( new Long(tempLong) );\n" : "                     item.set" + StringTools.capitalizeString(field.getName()) + "( tempLong );\n");
    }

    private static String getIntField(int i, DbFieldType field) {
        if (!declareIntVars) {
            declareVars += "                 int tempInt;\n";
            declareIntVars = true;
        }
        return "                 tempInt = rs.getInt( \"" + field.getName() + "\");\n" + "                 if (!rs.wasNull())\n" + (config.getIsUseObjectWrapper() ? "                     item.set" + StringTools.capitalizeString(field.getName()) + "( new Integer( tempInt ));\n" : "                     item.set" + StringTools.capitalizeString(field.getName()) + "( tempInt );\n");
    }

    private static String getDoubleField(int i, DbFieldType field) {
        if (!declareDoubleVars) {
            declareVars += "                 double tempDouble;\n";
            declareDoubleVars = true;
        }
        return "                 tempDouble = rs.getDouble( \"" + field.getName() + "\");\n" + "                 if (!rs.wasNull())\n" + (config.getIsUseObjectWrapper() ? "                     item.set" + StringTools.capitalizeString(field.getName()) + "( new Double( tempDouble ) );\n" : "                     item.set" + StringTools.capitalizeString(field.getName()) + "( tempDouble );\n");
    }

    private static String getStringField(DbFieldType field) {
        if (!declareStringVars) {
            declareVars += "                 String tempString = null;\n";
            declareStringVars = true;
        }
        return "                 tempString = rs.getString( \"" + field.getName() + "\" );\n" + "                 if (!rs.wasNull())\n" + "                     item.set" + StringTools.capitalizeString(field.getName()) + "( tempString );\n";
    }

    private static String getTimestampField(DbFieldType field) {
        if (!declareTimestampVars) {
            declareVars += "                 java.sql.Timestamp tempTimestamp = null;\n";
            declareTimestampVars = true;
        }
        return "                 tempTimestamp = rs.getTimestamp( \"" + field.getName() + "\" );\n" + "                 if (!rs.wasNull())\n" + "                     item.set" + StringTools.capitalizeString(field.getName()) + "( tempTimestamp );\n";
    }

    private static String initLogging(String className) {
        if (config.getIsUseLogging()) {
            return "    private static " + logger.getLogObjectClassName() + " cat = " + logger.getLogFactoryMethod() + "(\"" + className + "\" );\n";
        }
        return "";
    }

    private static boolean isLogicField(DbFieldType field) {
        if (config.getLogicPrefix() != null && config.getLogicPrefix().length() > 0 && field.getName().toLowerCase().startsWith(config.getLogicPrefix().toLowerCase())) return true;
        if (config.getLogicSuffix() != null && config.getLogicSuffix().length() > 0 && field.getName().toLowerCase().endsWith(config.getLogicSuffix().toLowerCase())) return true;
        return false;
    }

    private static boolean isKeyField(DbFieldType field) {
        if (config.getKeyPrefix() != null && config.getKeyPrefix().length() > 0 && field.getName().toLowerCase().startsWith(config.getKeyPrefix().toLowerCase())) return true;
        if (config.getKeySuffix() != null && config.getKeySuffix().length() > 0 && field.getName().toLowerCase().endsWith(config.getKeySuffix().toLowerCase())) return true;
        return false;
    }

    private static String addNamespace(String type) {
        return addNamespace(type, config.getDefaultNamespacePrefix());
    }

    private static String addNamespace(String type, String ns) {
        return ns + ':' + type;
    }

    private static String convertType(DbFieldType field) {
        if (field == null) return null;
        String type = null;
        if (isKeyField(field) && ((field.getJavaType() == Types.DECIMAL) || (field.getJavaType() == Types.DOUBLE) || (field.getJavaType() == Types.INTEGER))) return addNamespace(DatatypeHandler.LONG_TYPE);
        switch(field.getJavaType().intValue()) {
            case Types.DECIMAL:
                if (field.getDecimalDigit() == null || field.getDecimalDigit() == 0) {
                    if (field.getSize() < 7) type = DatatypeHandler.INTEGER_TYPE; else type = DatatypeHandler.LONG_TYPE;
                } else type = DatatypeHandler.DOUBLE_TYPE;
                break;
            case Types.INTEGER:
                if (field.getSize() < 7) type = DatatypeHandler.INTEGER_TYPE; else type = DatatypeHandler.LONG_TYPE;
                break;
            case Types.DOUBLE:
                type = DatatypeHandler.DOUBLE_TYPE;
                break;
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGVARBINARY:
                type = DatatypeHandler.STRING_TYPE;
                break;
            case Types.DATE:
            case Types.TIMESTAMP:
                type = DatatypeHandler.DATETIME_TYPE;
                break;
            default:
                type = DatatypeHandler.STRING_TYPE;
                System.out.println("unknown field type field - " + field.getName() + " javaType - " + field.getJavaType() + "\nSet to " + config.getDefaultNamespacePrefix() + ':' + "string XMLSchema type");
        }
        return addNamespace(type);
    }

    private static void createComplexTypeList(DbTableType table) throws Exception {
        boolean isNotFound = true;
        for (int i = 0; i < config.getTableToListCount(); i++) {
            if (table.getName().equals(config.getTableToList(i))) {
                isNotFound = false;
                break;
            }
        }
        if (!isNotFound) {
            System.out.println("Create list for table '" + table.getName() + "'");
        }
        if (isNotFound) {
            if (table.getImportedKeysCount() == 0) {
                System.out.println("Table '" + table.getName() + "' not contain foreign keys. This table is skipped");
            } else isNotFound = false;
        }
        if (isNotFound) return;
        String base = StringTools.capitalizeString(table.getName());
        String cTypeName = base + "ListType";
        ComplexType cType = schema.createComplexType(cTypeName);
        String nameElement = base;
        String typeElement = base + config.getClassNameSufix();
        ElementDecl element = new ElementDecl(schema, nameElement);
        element.setType(schema.getComplexType(typeElement));
        element.setMinOccurs(0);
        element.setMaxOccurs(Particle.UNBOUNDED);
        Group group = new Group();
        group.setOrder(Order.seq);
        cType.addGroup(group);
        group.addElementDecl(element);
        schema.addComplexType(cType);
    }

    private static Group createComplexTypeItem(DbTableType table) throws Exception {
        List<AttributeDecl> a = new ArrayList<AttributeDecl>();
        List<ElementDecl> e = new ArrayList<ElementDecl>();
        boolean flag = false;
        for (int i = 0; i < table.getFieldsCount(); i++) {
            DbFieldType field = table.getFields(i);
            if (flag) System.out.println("Field - " + field.getName());
            if (DatabaseManager.isFieldPrimaryKey(table, field) || DatabaseManager.isFieldForeignKey(table, field)) {
                String nameAttr = getAttributeName(field);
                AttributeDecl attr = new AttributeDecl(schema, nameAttr);
                attr.setSimpleTypeReference(convertType(field));
                if (field.getDefaultValue() != null && !"null".equalsIgnoreCase(field.getDefaultValue().trim())) {
                    if (flag) System.out.println("Field - " + field.getName() + ", def value " + field.getDefaultValue());
                    attr.setDefaultValue(field.getDefaultValue());
                }
                a.add(attr);
                continue;
            }
            if (isLogicField(field)) {
                String nameAttr = getAttributeName(field);
                AttributeDecl attr = new AttributeDecl(schema, nameAttr);
                attr.setSimpleTypeReference(addNamespace(DatatypeHandler.BOOLEAN_TYPE));
                if (field.getDefaultValue() != null && !"null".equalsIgnoreCase(field.getDefaultValue().trim())) {
                    if (flag) System.out.println("Field - " + field.getName() + ", def value " + field.getDefaultValue());
                    attr.setDefaultValue(field.getDefaultValue());
                }
                a.add(attr);
                continue;
            }
            if (field.getName().toLowerCase().startsWith("code_")) {
                String nameAttr = getAttributeName(field);
                AttributeDecl attr = new AttributeDecl(schema, nameAttr);
                attr.setSimpleTypeReference(convertType(field));
                if (field.getDefaultValue() != null && !"null".equalsIgnoreCase(field.getDefaultValue().trim())) {
                    if (flag) System.out.println("Field - " + field.getName() + ", def value " + field.getDefaultValue());
                    attr.setDefaultValue(field.getDefaultValue());
                }
                a.add(attr);
                continue;
            }
            if (isKeyField(field)) {
                System.out.println("Table '" + table.getName() + "', " + "field '" + field.getName() + "' " + "starts with 'ID_'m but not have FK");
                String nameAttr = getAttributeName(field);
                AttributeDecl attr = new AttributeDecl(schema, nameAttr);
                attr.setSimpleTypeReference(convertType(field));
                if (field.getDefaultValue() != null && !"null".equalsIgnoreCase(field.getDefaultValue().trim())) {
                    if (flag) System.out.println("Field - " + field.getName() + ", def value " + field.getDefaultValue());
                    attr.setDefaultValue(field.getDefaultValue());
                }
                a.add(attr);
                continue;
            }
            String nameElement = StringTools.capitalizeString(field.getName());
            String typeElement = convertType(field);
            ElementDecl element = new ElementDecl(schema, nameElement);
            if (field.getNullable() == 1 || isOptionalField(table, field)) element.setMinOccurs(0);
            element.setTypeReference(typeElement);
            if (field.getDefaultValue() != null && !"null".equalsIgnoreCase(field.getDefaultValue().trim())) {
                if (flag) System.out.println("Field - " + field.getName() + ", def value " + field.getDefaultValue());
                switch(field.getJavaType().intValue()) {
                    case Types.DECIMAL:
                        if (field.getDecimalDigit() == null || field.getDecimalDigit() == 0) {
                            element.setDefaultValue(field.getDefaultValue());
                        } else {
                            String defVal = field.getDefaultValue().trim().replace(',', '.');
                            element.setDefaultValue(new Double(defVal).toString());
                        }
                        break;
                    case Types.INTEGER:
                        element.setDefaultValue(field.getDefaultValue());
                        break;
                    case Types.DOUBLE:
                        String defVal = field.getDefaultValue().trim().replace(',', '.');
                        element.setDefaultValue(new Double(defVal).toString());
                        break;
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.LONGVARBINARY:
                        element.setDefaultValue("\"" + field.getDefaultValue() + "\"");
                        break;
                    case Types.DATE:
                    case Types.TIMESTAMP:
                        break;
                    default:
                        System.out.println("unknown field type, field - " + field.getName() + ", javaType - " + field.getJavaType() + "\nSet to " + config.getDefaultNamespacePrefix() + ':' + "string XMLSchema type");
                }
            }
            e.add(element);
        }
        ComplexType cType = schema.createComplexType(StringTools.capitalizeString(table.getName()) + config.getClassNameSufix());
        for (int i = 0; i < a.size(); i++) {
            AttributeDecl attr = (AttributeDecl) a.get(i);
            cType.addAttributeDecl(attr);
        }
        Group group = new Group();
        group.setOrder(Order.seq);
        cType.addGroup(group);
        for (int i = 0; i < e.size(); i++) {
            ElementDecl elem = (ElementDecl) e.get(i);
            group.addElementDecl(elem);
        }
        if (baseType != null) {
            cType.setDerivationMethod(SchemaNames.EXTENSION);
            cType.setBaseType(baseType);
        }
        schema.addComplexType(cType);
        return group;
    }

    private static boolean isOptionalField(DbTableType table, DbFieldType field) {
        if (table == null || field == null) return false;
        for (int i = 0; i < config.getOptionalFieldListCount(); i++) {
            OptionalFieldTableType optionalTable = config.getOptionalFieldList(i);
            if (optionalTable.getTableName().equals(table.getName())) {
                for (int j = 0; j < optionalTable.getFieldNameCount(); j++) {
                    String optionalField = optionalTable.getFieldName(j);
                    if (optionalField.equals(field.getName())) return true;
                }
                return false;
            }
        }
        return false;
    }

    private static void createDeleteListForFk(DbTableType table) throws Exception {
        for (int k = 0; k < table.getImportedKeysCount(); k++) {
            DbImportedPKColumnType key = table.getImportedKeys(k);
            if (DatabaseManager.isMultiColumnFk(table, key)) {
                System.out.println("Create class list for table with multicolumn foreign keys not supported");
                continue;
            }
            String base = StringTools.capitalizeString(table.getName());
            String className = "Delete" + base + "With" + StringTools.capitalizeString(key.getFkColumnName());
            String classNameItem = base + config.getClassNameSufix();
            String s = "package " + packageClass + ";\n" + "\n" + (isApplModule ? "import org.riverock.a3.AuthSession;\n" + "import org.riverock.a3.AccessDeniedException;\n" + "import org.riverock.as.ApplicationInterface;\n" + "import org.riverock.schema.appl_server.ResourceRequestType;\n" + "import org.riverock.schema.appl_server.ResourceRequestParameterType;\n" + "import org.riverock.as.server.ApplicationTools;\n" : "") + "import " + config.getJavaPackageXmlSchema() + '.' + base + config.getClassNameSufix() + ";\n" + "\n" + "import java.sql.PreparedStatement;\n" + "import java.sql.ResultSet;\n" + "\n" + "public class " + className + " " + (isApplModule ? "implements ApplicationInterface" : "") + "\n{\n" + initLogging(packageClass + '.' + className) + "\n" + "    public " + className + "(){}\n" + "\n" + "    public static Long processData(" + db.getFactoryMethod() + " db_, long id) " + putExceptionDefinition() + "    {\n" + "        return new Long(process(db_, new Long(id)));\n" + "    }\n" + "\n" + "    public static long process(" + db.getFactoryMethod() + " db_, long id) " + putExceptionDefinition() + "    {\n" + "        return process(db_, new Long(id));\n" + "    }\n" + "\n" + "    public static long process(" + db.getFactoryMethod() + " db_, Long id) " + putExceptionDefinition() + "    {\n" + "\n" + "        if (id==null) \n " + "            throw new IllegalArgumentException(\"Value of id can not be null\");\n" + "\n" + "        String sql_ =\n" + "            \"delete from " + table.getName() + " \"+\n" + "            \"where " + key.getFkColumnName() + "=?\";\n" + "\n" + "        PreparedStatement ps = null;\n" + "        ResultSet rs = null;\n" + "        try\n" + "        {\n" + "            ps = db_.prepareStatement(sql_);\n" + "\n" + "            ps.setLong(1, id.longValue() );\n" + "\n" + "            int countInsertRecord = ps.executeUpdate();\n" + "\n" + (config.getIsUseLogging() ? "            if (cat.isDebugEnabled())\n" + "                 cat.debug(\"Count of deleted records - \"+countInsertRecord);\n" : "") + "\n" + "            return countInsertRecord;\n" + "\n" + "        }\n" + "        catch (Exception e)\n" + "        {\n" + (config.getIsUseLogging() ? "            cat.error(\"Error delete from db\", e);\n" : "");
            if (config.getPersistenceExceptionName() != null && config.getPersistenceExceptionName().getExceptionType().getType() == ExceptionDefinitionTypeExceptionTypeType.WRAP_PERSISTENCE_TYPE) {
                if (config.getPersistenceExceptionName().getPersistenceExceptionName() == null) throw new IllegalArgumentException("Not defined PersistebceExceptionName");
                s += "            throw new " + config.getPersistenceExceptionName().getPersistenceExceptionName() + "( e.getMessage(), e );\n";
            } else s += "            throw e;\n";
            s += "         }\n" + "         finally\n" + "         {\n" + putCloseFactoryMethod() + "             rs = null;\n" + "             ps = null;\n" + "         }\n" + "\n" + "     }\n" + "\n" + (isApplModule ? "     public byte[] processRequest( ResourceRequestType applReq, AuthSession authSession )\n" + "         throws Exception\n" + "     {\n" + "          if (applReq==null || applReq.getParametersCount()==0)\n" + "              return null;\n" + "\n" + putAuthCheck(table, "D", className) + "\n" + "          Long item = null;\n" + "          for (int i=0; i<applReq.getParametersCount(); i++)\n" + "          {\n" + "              ResourceRequestParameterType param = applReq.getParameters(i);\n" + "              if (\"mill.id\".equals( param.getNameParameter()))\n" + "              {\n" + "                  String stringParam = ApplicationTools.decodeParameter( param );\n" + "\n" + (config.getIsUseLogging() ? "                  if (cat.isDebugEnabled())\n" + "                      cat.debug(\"Parameter is \"+stringParam);\n" : "") + "\n" + "                  item = new Long (stringParam );\n" + "                  break;\n" + "              }\n" + "          }\n" + "          if (item == null )\n" + "              return null;\n" + "\n" + "          " + db.getFactoryMethod() + " db_ = null;\n" + "          try" + "          {\n" + "              db_ = " + db.getFactoryMethod() + ".getInstance( true );\n" + "              db_.getConnection().setAutoCommit(false);\n" + "              long resultLong = " + className + ".process( db_, item.longValue() );\n" + "              db_.commit();\n" + "              org.riverock.schema.appl_server.LongResultType result = new org.riverock.schema.appl_server.LongResultType();\n" + "              result.setResult(resultLong);\n" + "              return org.riverock.generic.tools.XmlTools.getXml(result, null);\n" + "          }\n" + "          catch(Exception e)\n" + "          {\n" + "              db_.rollback();\n" + (config.getIsUseLogging() ? "              cat.error(\"Exception in " + className + ".process\", e);\n" : "") + "              throw e;\n" + "          }\n" + "          catch(Error e)\n" + "          {\n" + "              db_.rollback();\n" + (config.getIsUseLogging() ? "              cat.error(\"Error in " + className + ".process\", e);\n" : "") + "              throw e;\n" + "          }\n" + "          finally\n" + "          {\n" + "              " + db.getFactoryMethod() + ".close( db_ );\n" + "              db_ = null;\n" + "          }\n" + "      }\n" + "\n" : "") + getCloseRsPs() + (config.getIsMainMethod() ? "   public static void main(String args[])\n" + "        throws Exception\n" + "   {\n" + "       org.riverock.startup.StartupApplication.init();\n" + "\n" + "       long id = 1;\n" + "\n" + "       long resultLong = " + className + ".process( " + db.getFactoryMethod() + ".getInstance( false ), id );\n" + "\n" + "       org.riverock.schema.appl_server.LongResultType result = new org.riverock.schema.appl_server.LongResultType();\n" + "       result.setResult( resultLong );\n" + "\n" + "       org.riverock.tools.XmlTools.writeToFile(\n" + "           result,\n" + "           org.riverock.startup.InitParam.getMillDebugDir()+\"" + "test-" + table.getName().toLowerCase() + "-item.xml" + "\",\n" + "           \"utf-8\",\n" + "           null\n" + "       );\n" + "   }\n" : "") + "}\n";
            writeClass(className, s);
        }
    }

    private static String putCloseFactoryMethod() {
        return (config.getCloseFactoryMethod() != null ? "            " + config.getCloseFactoryMethod() + "(rs,ps);\n" : "            _closeRsPs(rs, ps);\n");
    }

    private static void createDeleteClassForPk(DbTableType table) throws Exception {
        DbPrimaryKeyType pk = table.getPrimaryKey();
        if (pk.getColumnsCount() == 0) {
            return;
        }
        DbPrimaryKeyColumnType column = pk.getColumns(0);
        String base = StringTools.capitalizeString(table.getName());
        String className = "Delete" + base + "Item";
        String classNameItem = base + config.getClassNameSufix();
        String s = "package " + packageClass + ";\n" + "\n" + (isApplModule ? "import org.riverock.a3.AuthSession;\n" + "import org.riverock.a3.AccessDeniedException;\n" + "import org.riverock.as.ApplicationInterface;\n" + "import org.riverock.schema.appl_server.ResourceRequestType;\n" + "import org.riverock.schema.appl_server.ResourceRequestParameterType;\n" + "import org.riverock.as.server.ApplicationTools;\n" : "") + "import " + config.getJavaPackageXmlSchema() + '.' + base + config.getClassNameSufix() + ";\n" + "\n" + "import java.sql.PreparedStatement;\n" + "import java.sql.ResultSet;\n" + "\n" + "public class " + className + " " + addInterfaceDeclaration(new String[] { isApplModule ? "ApplicationInterface" : "", isDeleteInterface ? deleteInterface : "" }) + "\n{\n" + initLogging(packageClass + '.' + className) + "\n" + "     public " + className + "(){}\n" + "\n" + (isDeleteInterface ? putProcessEntityMethos(classNameItem) : "") + "     public static Long processData(" + db.getFactoryMethod() + " db_, " + classNameItem + " item) " + putExceptionDefinition() + "     {\n" + "         return new Long(process(db_, item));\n" + "     }\n" + "\n" + "     public static long process(" + db.getFactoryMethod() + " db_, " + classNameItem + " item) " + putExceptionDefinition() + "     {\n" + "\n" + "         String sql_ =\n" + "             \"delete from " + table.getName() + " \"+\n" + "             \"where " + column.getColumnName() + "=?\";\n" + "\n" + "         PreparedStatement ps = null;\n" + "         ResultSet rs = null;\n" + "         try\n" + "         {\n" + "             ps = db_.prepareStatement(sql_);\n" + "\n" + (config.getIsUseObjectWrapper() ? "             ps.setObject(1, item.get" + StringTools.capitalizeString(column.getColumnName()) + "() );\n" : "             ps.setLong(1, item.get" + StringTools.capitalizeString(column.getColumnName()) + "() );\n") + "\n" + "             int countInsertRecord = ps.executeUpdate();\n" + "\n" + (config.getIsUseLogging() ? "             if (cat.isDebugEnabled())\n" + "                 cat.debug(\"Count of deleted records - \"+countInsertRecord);\n" : "") + "\n" + "             return countInsertRecord;\n" + "\n";
        s += getEndOfClassUID(className, classNameItem, table, "D", "sql_");
        s += getMainMethod(classNameItem, className, "test-" + table.getName().toLowerCase() + "-item.xml");
        writeClass(className, s);
    }

    private static String putProcessEntityMethos(String classNameItem) {
        return "    public long processEntity( " + db.getFactoryMethod() + " db_, " + config.getBaseInterface().getEntityClassPackage() + '.' + config.getSuperClass().getComplexType() + " item) " + putExceptionDefinition() + " {\n" + "        try {\n" + "            return process(db_, (" + classNameItem + ")item);\n" + "        } catch (ClassCastException e) {\n" + "            cat.error(\"Exception cast to '" + classNameItem + "' type\", e);\n" + "            throw new " + config.getPersistenceExceptionName().getPersistenceExceptionName() + "(\"Exception cast to '" + classNameItem + "' type\",e);\n" + "        }\n" + "    }\n" + "\n";
    }

    private static void createDeleteItemForPk(DbTableType table) throws Exception {
        DbPrimaryKeyType pk = table.getPrimaryKey();
        if (pk.getColumnsCount() != 1) {
            return;
        }
        DbPrimaryKeyColumnType column = pk.getColumns(0);
        String base = StringTools.capitalizeString(table.getName());
        String className = "Delete" + base + "With" + StringTools.capitalizeString(column.getColumnName());
        String classNameItem = base + config.getClassNameSufix();
        String s = "package " + packageClass + ";\n" + "\n" + (isApplModule ? "import org.riverock.a3.AuthSession;\n" + "import org.riverock.a3.AccessDeniedException;\n" + "import org.riverock.as.ApplicationInterface;\n" + "import org.riverock.schema.appl_server.ResourceRequestType;\n" + "import org.riverock.schema.appl_server.ResourceRequestParameterType;\n" + "import org.riverock.as.server.ApplicationTools;\n" : "") + "import " + config.getJavaPackageXmlSchema() + '.' + base + config.getClassNameSufix() + ";\n" + "\n" + "import java.sql.PreparedStatement;\n" + "import java.sql.ResultSet;\n" + "\n" + "public class " + className + " " + (isApplModule ? "implements ApplicationInterface" : "") + "\n{\n" + initLogging(packageClass + '.' + className) + "\n" + "    public " + className + "(){}\n" + "\n" + "    public static Long processData(" + db.getFactoryMethod() + " db_, long id) " + putExceptionDefinition() + "    {\n" + "        return new Long(process(db_, id));\n" + "    }\n" + "\n" + "    public static long process(" + db.getFactoryMethod() + " db_, long id) " + putExceptionDefinition() + "    {\n" + "        return process(db_, new Long(id));\n" + "    }\n" + "\n" + "    public static long process(" + db.getFactoryMethod() + " db_, Long id) " + putExceptionDefinition() + "    {\n" + "\n" + "        if (id==null) \n " + "            throw new IllegalArgumentException(\"Value of id can not be null\");\n" + "\n" + "        String sql_ =\n" + "            \"delete from " + table.getName() + " \"+\n" + "            \"where " + column.getColumnName() + "=?\";\n" + "\n" + "        PreparedStatement ps = null;\n" + "        ResultSet rs = null;\n" + "        try {\n" + "            ps = db_.prepareStatement(sql_);\n" + "\n" + "            ps.setLong(1, id.longValue() );\n" + "\n" + "            int countDeletedtRecord = ps.executeUpdate();\n" + "\n" + (config.getIsUseLogging() ? "            if (cat.isDebugEnabled())\n" + "                 cat.debug(\"Count of deleted records - \"+countDeletedtRecord);\n" : "") + "\n" + "            return countDeletedtRecord;\n" + "\n" + "        }\n" + "        catch (Exception e) {\n" + (config.getIsUseLogging() ? "             cat.error(\"Error delete from db\", e);\n" : "");
        if (config.getPersistenceExceptionName() != null && config.getPersistenceExceptionName().getExceptionType().getType() == ExceptionDefinitionTypeExceptionTypeType.WRAP_PERSISTENCE_TYPE) {
            if (config.getPersistenceExceptionName().getPersistenceExceptionName() == null) throw new IllegalArgumentException("Not defined PersistebceExceptionName");
            s += "            throw new " + config.getPersistenceExceptionName().getPersistenceExceptionName() + "( e.getMessage(), e );\n";
        } else s += "            throw e;\n";
        s += "        }\n" + "        finally {\n" + putCloseFactoryMethod() + "            rs = null;\n" + "            ps = null;\n" + "        }\n" + "\n" + "    }\n" + "\n" + (isApplModule ? "     public byte[] processRequest( ResourceRequestType applReq, AuthSession authSession )\n" + "         throws Exception\n" + "     {\n" + "          if (applReq==null || applReq.getParametersCount()==0)\n" + "              return null;\n" + "\n" + putAuthCheck(table, "D", className) + "\n" + "          Long item = null;\n" + "          for (int i=0; i<applReq.getParametersCount(); i++)\n" + "          {\n" + "              ResourceRequestParameterType param = applReq.getParameters(i);\n" + "              if (\"mill.id\".equals( param.getNameParameter()))\n" + "              {\n" + "                  String stringParam = ApplicationTools.decodeParameter( param );\n" + "\n" + (config.getIsUseLogging() ? "                  if (cat.isDebugEnabled())\n" + "                      cat.debug(\"Parameter is \"+stringParam);\n" : "") + "\n" + "                  item = new Long (stringParam );\n" + "                  break;\n" + "              }\n" + "          }\n" + "          if (item == null )\n" + "              return null;\n" + "\n" + "          " + db.getFactoryMethod() + " db_ = null;\n" + "          try" + "          {\n" + "              db_ = " + db.getFactoryMethod() + ".getInstance( true );\n" + "              db_.getConnection().setAutoCommit(false);\n" + "              long resultLong = " + className + ".process( db_, item.longValue() );\n" + "              db_.commit();\n" + "              org.riverock.schema.appl_server.LongResultType result = new org.riverock.schema.appl_server.LongResultType();\n" + "              result.setResult(resultLong);\n" + "              return org.riverock.generic.tools.XmlTools.getXml(result, null);\n" + "          }\n" + "          catch(Exception e)\n" + "          {\n" + "              db_.rollback();\n" + (config.getIsUseLogging() ? "              cat.error(\"Exception in " + className + ".process()\", e);\n" : "") + "              throw e;\n" + "          }\n" + "          catch(Error e)\n" + "          {\n" + "              db_.rollback();\n" + (config.getIsUseLogging() ? "              cat.error(\"Error in " + className + ".process()\", e);\n" : "") + "              throw e;\n" + "          }\n" + "          finally\n" + "          {\n" + "              " + db.getFactoryMethod() + ".close( db_ );\n" + "              db_ = null;\n" + "          }\n" + "\n" + "      }\n" + "\n" : "") + getCloseRsPs() + (config.getIsMainMethod() ? "   public static void main(String args[])\n" + "       throws Exception\n" + "   {\n" + "       org.riverock.startup.StartupApplication.init();\n" + "\n" + "       long id = 1;\n" + "\n" + "       long resultLong = " + className + ".process( " + db.getFactoryMethod() + ".getInstance( false ), id );\n" + "\n" + "       org.riverock.schema.appl_server.LongResultType result = new org.riverock.schema.appl_server.LongResultType();\n" + "       result.setResult( resultLong );\n" + "\n" + "       org.riverock.tools.XmlTools.writeToFile(\n" + "           result,\n" + "           org.riverock.startup.InitParam.getMillDebugDir()+\"" + "test-" + table.getName().toLowerCase() + "-item.xml" + "\",\n" + "           \"utf-8\",\n" + "           null\n" + "       );\n" + "   }\n" : "") + "}\n";
        writeClass(className, s);
    }

    private static int getDeleteRule(DbTableType table, DbFieldType field) {
        for (int i = 0; i < table.getImportedKeysCount(); i++) {
            DbImportedPKColumnType fk = table.getImportedKeys(i);
            if (fk.getFkColumnName().equals(field.getName())) {
                if (fk.getDeleteRule() != null) return fk.getDeleteRule().getRuleType(); else return java.sql.DatabaseMetaData.importedKeyNoAction;
            }
        }
        return java.sql.DatabaseMetaData.importedKeyNoAction;
    }

    private static void createUpdateClassForPk(DbTableType table) throws Exception {
        DbPrimaryKeyType pk = table.getPrimaryKey();
        if (pk.getColumnsCount() == 0) {
            return;
        }
        if (table.getFieldsCount() < 2) {
            System.out.println("Table '" + table.getName() + "' is skiped - count of fields < 2");
            return;
        }
        DbPrimaryKeyColumnType column = pk.getColumns(0);
        String base = StringTools.capitalizeString(table.getName());
        String className = "Update" + base + "Item";
        String classNameItem = base + config.getClassNameSufix();
        String s = "package " + packageClass + ";\n" + "\n" + (isApplModule ? "import org.riverock.a3.AuthSession;\n" + "import org.riverock.a3.AccessDeniedException;\n" + "import org.riverock.as.ApplicationInterface;\n" + "import org.riverock.schema.appl_server.ResourceRequestType;\n" + "import org.riverock.schema.appl_server.ResourceRequestParameterType;\n" + "import org.riverock.as.server.ApplicationTools;\n" : "") + "import " + config.getJavaPackageXmlSchema() + '.' + base + config.getClassNameSufix() + ";\n" + "\n" + "import java.sql.PreparedStatement;\n" + "import java.sql.ResultSet;\n" + "import java.sql.Types;\n" + "\n" + "public class " + className + " " + addInterfaceDeclaration(new String[] { isApplModule ? "ApplicationInterface" : "", isUpdateInterface ? updateInterface : "" }) + "\n{\n" + initLogging(packageClass + '.' + className) + "\n" + "     public " + className + "(){}\n" + "\n" + (isUpdateInterface ? putProcessEntityMethos(classNameItem) : "") + "     public static Long processData(" + db.getFactoryMethod() + " db_, " + classNameItem + " item) " + putExceptionDefinition() + "     {\n" + "         return new Long(process(db_, item));\n" + "     }\n" + "\n" + "     public static long process(" + db.getFactoryMethod() + " db_, " + classNameItem + " item) " + putExceptionDefinition() + "     {\n" + "\n" + "         String sql_ =\n" + "             \"update " + table.getName() + " \"+\n" + "             \"set\"+\n";
        boolean isNotFirst = false;
        for (int i = 0; i < table.getFieldsCount(); i++) {
            DbFieldType field = table.getFields(i);
            if (field.getJavaType() == 1111) continue;
            if (!field.getName().equals(column.getColumnName())) {
                if (isNotFirst) {
                    s += ", \"+\n";
                } else {
                    isNotFirst = true;
                }
                s += "             \"    " + field.getName() + "=?";
            }
        }
        s += " \"+\n";
        s += "             \"where " + column.getColumnName() + "=?\";\n" + "\n" + "         PreparedStatement ps = null;\n" + "         ResultSet rs = null;\n" + "         try\n" + "         {\n" + "             ps = db_.prepareStatement(sql_);\n" + "\n";
        int numParam = 0;
        for (int i1 = 0; i1 < table.getFieldsCount(); i1++) {
            DbFieldType field = table.getFields(i1);
            if (field.getJavaType() == 1111) continue;
            if (field.getName().equals(column.getColumnName())) continue;
            ++numParam;
            String capitalizeName = StringTools.capitalizeString(field.getName()) + "()";
            if (isLogicField(field)) {
                s += storeBooleanField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls, field.getDefaultValue());
            } else if (isKeyField(field)) {
                s += storeLongField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls && getDeleteRule(table, field) != java.sql.DatabaseMetaData.importedKeyCascade);
            } else {
                switch(field.getJavaType().intValue()) {
                    case Types.DECIMAL:
                        if (field.getDecimalDigit() == null || field.getDecimalDigit() == 0) {
                            if (field.getSize() < 7) {
                                s += storeIntField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                            } else {
                                s += storeLongField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                            }
                        } else {
                            s += storeDoubleField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                        }
                        break;
                    case Types.INTEGER:
                        if (field.getSize() < 7) {
                            s += storeIntField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                        } else {
                            s += storeLongField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                        }
                        break;
                    case Types.DOUBLE:
                        s += storeDoubleField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                        break;
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.LONGVARBINARY:
                        s += storeStringField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                        break;
                    case Types.DATE:
                    case Types.TIMESTAMP:
                        if (field.getNullable() != DatabaseMetaData.columnNoNulls) {
                            s += "             if (item.get" + capitalizeName + "!=null)\n" + "                 ps.setTimestamp(" + numParam + ", new java.sql.Timestamp( item.get" + capitalizeName + ".getTime()) );\n" + "             else\n" + "                 ps.setNull(" + numParam + ", Types.DATE);\n\n";
                        } else {
                            if (field.getNullable() != DatabaseMetaData.columnNoNulls) {
                                s += "             if (item.has" + capitalizeName + ")\n" + "                 ps.setTimestamp(" + numParam + ", item.get" + capitalizeName + " );\n" + "             else\n" + "                 ps.setNull(" + numParam + ", Types.DATE);\n\n";
                            } else s += "             ps.setTimestamp(" + numParam + ", new java.sql.Timestamp( item.get" + capitalizeName + ".getTime()) );\n";
                        }
                        break;
                    default:
                        field.setJavaStringType("unknown field type field - " + field.getName() + " javaType - " + field.getJavaType());
                        System.out.println("unknown field type field - " + field.getName() + " javaType - " + field.getJavaType());
                }
            }
        }
        s += "\n" + "             // prepare PK\n" + storeLongField(StringTools.capitalizeString(column.getColumnName()) + "()", ++numParam, false) + "\n" + "             int countInsertRecord = ps.executeUpdate();\n" + "\n" + (config.getIsUseLogging() ? "             if (cat.isDebugEnabled())\n" + "                 cat.debug(\"Count of inserted records - \"+countInsertRecord);\n" : "") + "\n" + "             return countInsertRecord;\n" + "\n";
        s += getEndOfClassUID(className, classNameItem, table, "U", "sql_");
        s += getMainMethod(classNameItem, className, "test-" + table.getName().toLowerCase() + "-item.xml");
        writeClass(className, s);
    }

    private static void createInsertClassForPk(DbTableType table) throws Exception {
        String base = StringTools.capitalizeString(table.getName());
        String className = "Insert" + base + "Item";
        String classNameItem = base + config.getClassNameSufix();
        String s = "package " + packageClass + ";\n" + "\n" + (isApplModule ? "import org.riverock.a3.AuthSession;\n" + "import org.riverock.a3.AccessDeniedException;\n" + "import org.riverock.as.ApplicationInterface;\n" + "import org.riverock.schema.appl_server.ResourceRequestType;\n" + "import org.riverock.schema.appl_server.ResourceRequestParameterType;\n" + "import org.riverock.as.server.ApplicationTools;\n" : "") + "import " + config.getJavaPackageXmlSchema() + '.' + base + config.getClassNameSufix() + ";\n" + "\n" + "import java.sql.PreparedStatement;\n" + "import java.sql.ResultSet;\n" + "import java.sql.Types;\n" + "\n" + "public class " + className + " " + addInterfaceDeclaration(new String[] { isApplModule ? "ApplicationInterface" : "", isInsertInterface ? insertInterface : "" }) + "\n{\n" + initLogging(packageClass + '.' + className) + "\n" + "    public " + className + "(){}\n" + "\n" + (isInsertInterface ? putProcessEntityMethos(classNameItem) : "") + "    public static Long processData(" + db.getFactoryMethod() + " db_, " + classNameItem + " item) " + putExceptionDefinition() + "    {\n" + "        return new Long(process(db_, item));\n" + "    }\n" + "\n" + "    public static long process(" + db.getFactoryMethod() + " db_, " + classNameItem + " item) " + putExceptionDefinition() + "    {\n" + "        String sql_ =\n" + "            \"insert into " + table.getName() + "\"+\n" + buildInsertFieldList(table, 13) + "            \"values\"+\n" + "            \"(";
        boolean isFirst = true;
        for (int i = 0; i < table.getFieldsCount(); i++) {
            DbFieldType field = table.getFields(i);
            if (field.getJavaType() == 1111) continue;
            if (isFirst) isFirst = false; else s += ", ";
            if (field.getJavaType() != Types.DATE && field.getJavaType() != Types.TIMESTAMP) s += " ?"; else s += " \"+ db_.getNameDateBind()+\"";
        }
        s += ")\";\n" + "\n" + "        return process(db_, item, sql_);\n" + "    }\n" + "\n" + "    public static long process(" + db.getFactoryMethod() + " db_, " + classNameItem + " item, String sql_) " + putExceptionDefinition() + "    {\n" + "\n" + "        PreparedStatement ps = null;\n" + "        ResultSet rs = null;\n" + "        try\n" + "        {\n" + "            ps = db_.prepareStatement(sql_);\n" + "\n";
        int numParam = 0;
        for (int i = 0; i < table.getFieldsCount(); i++) {
            ++numParam;
            DbFieldType field = table.getFields(i);
            String capitalizeName = StringTools.capitalizeString(field.getName()) + "()";
            if (isLogicField(field)) {
                s += storeBooleanField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls, field.getDefaultValue());
            } else {
                if (isKeyField(field)) {
                    s += storeLongField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls && getDeleteRule(table, field) != java.sql.DatabaseMetaData.importedKeyCascade);
                } else {
                    switch(field.getJavaType().intValue()) {
                        case Types.DECIMAL:
                            if (field.getDecimalDigit() == null || field.getDecimalDigit() == 0) {
                                if (field.getSize() < 7) {
                                    s += storeIntField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                                } else {
                                    s += storeLongField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                                }
                            } else {
                                s += storeDoubleField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                            }
                            break;
                        case Types.INTEGER:
                            if (field.getSize() < 7) {
                                s += storeIntField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                            } else {
                                s += storeLongField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                            }
                            break;
                        case Types.DOUBLE:
                            s += storeDoubleField(capitalizeName, numParam, field.getNullable() != DatabaseMetaData.columnNoNulls);
                            break;
                        case Types.VARCHAR:
                        case Types.LONGVARCHAR:
                        case Types.LONGVARBINARY:
                            if (field.getNullable() != DatabaseMetaData.columnNoNulls) {
                                s += "             if (item.get" + capitalizeName + "!=null)\n" + "                 ps.setString(" + numParam + ", item.get" + capitalizeName + " );\n" + "             else\n" + "                 ps.setNull(" + numParam + ", Types.VARCHAR);\n\n";
                            } else s += "             ps.setString(" + numParam + ", item.get" + capitalizeName + " );\n";
                            break;
                        case Types.DATE:
                        case Types.TIMESTAMP:
                            if (field.getNullable() != DatabaseMetaData.columnNoNulls) {
                                s += "             if (item.get" + capitalizeName + "!=null)\n" + "                 db_.bindDate(ps, " + numParam + ", new java.sql.Timestamp( item.get" + capitalizeName + ".getTime()) );\n" + "             else\n" + "                 ps.setNull(" + numParam + ", Types.DATE);\n\n";
                            } else s += "             db_.bindDate(ps, " + numParam + ", new java.sql.Timestamp( item.get" + capitalizeName + ".getTime()) );\n";
                            break;
                        default:
                            field.setJavaStringType("unknown field type field - " + field.getName() + " javaType - " + field.getJavaType());
                            System.out.println("unknown field type field - " + field.getName() + " javaType - " + field.getJavaType());
                    }
                }
            }
        }
        s += "\n" + "             int countInsertRecord = ps.executeUpdate();\n" + "\n" + (config.getIsUseLogging() ? "             if (cat.isDebugEnabled())\n" + "                 cat.debug(\"Count of inserted records - \"+countInsertRecord);\n" : "") + "\n" + "             return countInsertRecord;\n" + "\n";
        s += getEndOfClassUID(className, classNameItem, table, "I", "sql_");
        s += getMainMethod(classNameItem, className, "test-" + table.getName().toLowerCase() + "-item.xml");
        writeClass(className, s);
    }

    private static String addInterfaceDeclaration(String[] in) {
        if (in == null) return "";
        boolean isFirst = true;
        String s = "";
        for (int i = 0; i < in.length; i++) {
            if (in[i] != null && in[i].trim().length() > 0) {
                if (isFirst) {
                    isFirst = false;
                    s += " implements ";
                } else s += ", ";
                s += in[i];
            }
        }
        return s;
    }

    private static String storeLongField(String capitalizeName, int i, boolean isNull) {
        if (isNull) {
            if (!config.getIsUseObjectWrapper()) return "             if (item.has" + capitalizeName + ")\n" + "                 ps.setLong(" + i + ", item.get" + capitalizeName + " );\n" + "             else\n" + "                 ps.setNull(" + i + ", Types.INTEGER);\n\n"; else return "             if (item.get" + capitalizeName + " != null )\n" + "                 ps.setObject(" + i + ", item.get" + capitalizeName + " );\n" + "             else\n" + "                 ps.setNull(" + i + ", Types.INTEGER);\n\n";
        } else {
            if (config.getIsUseObjectWrapper()) return "             ps.setObject(" + i + ", item.get" + capitalizeName + " );\n"; else return "             ps.setLong(" + i + ", item.get" + capitalizeName + " );\n";
        }
    }

    private static String storeBooleanField(String capitalizeName, int i, boolean isNull, String defValue) {
        if (isNull) {
            if (config.getIsUseObjectWrapper()) return "             if (item.get" + capitalizeName + "!=null)\n" + "                 ps.setInt(" + i + ", item.get" + capitalizeName + ".booleanValue()?1:0 );\n" + "             else\n" + ("1".equals(defValue) || "0".equals(defValue) ? "                 ps.setInt(" + i + ", " + ("1".equals(defValue) ? 1 : 0) + " );\n" : "                 ps.setNull(" + i + ", Types.INTEGER);\n\n"); else return "             if (item.has" + capitalizeName + ")\n" + "                 ps.setInt(" + i + ", item.get" + capitalizeName + "?1:0 );\n" + "             else\n" + ("1".equals(defValue) || "0".equals(defValue) ? "                 ps.setInt(" + i + ", " + ("1".equals(defValue) ? 1 : 0) + " );\n" : "                 ps.setInt(" + i + ", 0 );\n\n");
        } else {
            if (config.getIsUseObjectWrapper()) return "             ps.setInt(" + i + ", item.get" + capitalizeName + ".booleanValue()?1:0 );\n"; else return "             ps.setInt(" + i + ", item.get" + capitalizeName + "?1:0 );\n";
        }
    }

    private static String storeIntField(String capitalizeName, int i, boolean isNull) {
        if (isNull) {
            if (!config.getIsUseObjectWrapper()) return "             if (item.has" + capitalizeName + ")\n" + "                 ps.setInt(" + i + ", item.get" + capitalizeName + " );\n" + "             else\n" + "                 ps.setNull(" + i + ", Types.INTEGER);\n\n"; else return "             if (item.get" + capitalizeName + " != null )\n" + "                 ps.setInt(" + i + ", item.get" + capitalizeName + ".intValue() );\n" + "             else\n" + "                 ps.setNull(" + i + ", Types.INTEGER);\n\n";
        } else {
            if (config.getIsUseObjectWrapper()) return "             ps.setInt(" + i + ", item.get" + capitalizeName + ".intValue() );\n"; else return "             ps.setInt(" + i + ", item.get" + capitalizeName + " );\n";
        }
    }

    private static String storeDoubleField(String capitalizeName, int i, boolean isNull) {
        if (isNull) {
            if (!config.getIsUseObjectWrapper()) return "             if (item.has" + capitalizeName + ")\n" + "                 ps.setDouble(" + i + ", item.get" + capitalizeName + " );\n" + "             else\n" + "                 ps.setNull(" + i + ", Types.DECIMAL);\n\n"; else return "             if (item.get" + capitalizeName + " != null )\n" + "                 ps.setDouble(" + i + ", item.get" + capitalizeName + ".doubleValue() );\n" + "             else\n" + "                 ps.setNull(" + i + ", Types.DECIMAL);\n\n";
        } else {
            if (config.getIsUseObjectWrapper()) return "             ps.setDouble(" + i + ", item.get" + capitalizeName + ".longValue() );\n"; else return "             ps.setDouble(" + i + ", item.get" + capitalizeName + " );\n";
        }
    }

    private static String storeTimestampField(String capitalizeName, int i, boolean isNull) {
        if (isNull) {
            return "             if (item.has" + capitalizeName + ")\n" + "                 db_.bindDate(ps, " + i + ", new java.sql.Timestamp( item.get" + capitalizeName + ".getTime()) );\n" + "             else\n" + "                 ps.setNull(" + i + ", Types.DATE);\n\n";
        } else {
            return "             db_.bindDate(ps, " + i + ", new java.sql.Timestamp( item.get" + capitalizeName + ".getTime()) );\n";
        }
    }

    private static String storeStringField(String capitalizeName, int i, boolean isCanBeNull) {
        if (isCanBeNull) {
            return "             if (item.get" + capitalizeName + "!=null)\n" + "                 ps.setString(" + i + ", item.get" + capitalizeName + " );\n" + "             else\n" + "                 ps.setNull(" + i + ", Types.VARCHAR);\n\n";
        } else return "             ps.setString(" + i + ", item.get" + capitalizeName + " );\n";
    }

    private static String buildInsertFieldList(DbTableType table, int indentSize) throws Exception {
        if (table == null) throw new Exception("Table object is null");
        if (table.getFieldsCount() == 0) throw new Exception("Table not containt fields");
        String list = StringTools.addString("\"(", ' ', indentSize, true);
        boolean isNotFirst = false;
        int size = 0;
        for (int i = 0; i < table.getFieldsCount(); i++) {
            DbFieldType field = table.getFields(i);
            if (field.getJavaType() == 1111) continue;
            if (isNotFirst) {
                list += ", ";
                if (size > 50) {
                    list += (StringTools.addString("\"+\n", ' ', indentSize, false) + "\"");
                    size = 0;
                }
            } else {
                isNotFirst = true;
            }
            list += field.getName();
            size += field.getName().length();
        }
        return list + ")\"+\n";
    }

    private static boolean excludeTable(DbTableType table) {
        for (int i = 0; i < config.getDbTableExcludeCount(); i++) {
            if (table.getName().equals(config.getDbTableExclude(i))) return true;
        }
        return false;
    }

    private static boolean isIncludeTable(DbTableType table) {
        if (config.getIsProcessAllTables()) return true;
        for (int i = 0; i < config.getDbTablePrefixCount(); i++) {
            if (table.getName().startsWith(config.getDbTablePrefix(i))) return true;
        }
        return false;
    }

    private static boolean isAuthAccessTable(DbTableType table) {
        for (int i = 0; i < config.getAuthDataCount(); i++) {
            if (table.getName().equals(config.getAuthData(i).getTableName())) return true;
        }
        return false;
    }

    private static String lowerFirstChar(String s) {
        if (s.length() < 2) return s.toLowerCase();
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private static void putExtendComplexType(SchemaGenTableToItemType item, String topTable) throws Exception {
        if (item.getTableToItemCount() != 0) {
            DbTableType t = new DbTableType();
            DbTableType temp = DatabaseManager.getTableFromStructure(dbSchema, item.getTableName());
            t.setPrimaryKey(temp.getPrimaryKey());
            for (int i = 0; i < temp.getFieldsCount(); i++) {
                DbFieldType f = temp.getFields(i);
                if (isFk(item, temp, f, DatabaseManager.getTableFromStructure(dbSchema, topTable)) == null) t.addFields(f);
            }
            t.setName(temp.getName() + "_FOR_" + topTable + "_EXTEND");
            Group group = createComplexTypeItem(t);
            System.out.println("#23 " + t.getName());
            for (int i = 0; i < temp.getFieldsCount(); i++) {
                DbFieldType f = temp.getFields(i);
                String relTableName = isFk(item, temp, f, DatabaseManager.getTableFromStructure(dbSchema, topTable));
                System.out.println("relTableName " + relTableName);
                if (relTableName != null) {
                    ElementDecl element = null;
                    SchemaGenTableToItemType a = getRelatedTable(item, relTableName);
                    System.out.println("#1. a.name " + (a != null ? a.getTableName() : "null"));
                    if (a != null) {
                        String base = StringTools.capitalizeString(relTableName + "_FOR_" + temp.getName() + "_EXTEND");
                        String nameElement = base;
                        String typeElement = base + config.getClassNameSufix();
                        element = new ElementDecl(schema, nameElement);
                        element.setType(schema.getComplexType(typeElement));
                    } else {
                        String base = StringTools.capitalizeString(relTableName);
                        String nameElement = base;
                        String typeElement = base + config.getClassNameSufix();
                        element = new ElementDecl(schema, nameElement);
                        element.setType(schema.getComplexType(typeElement));
                    }
                    element.setMinOccurs(0);
                    element.setMaxOccurs(1);
                    group.addElementDecl(element);
                }
            }
            for (int j = 0; j < item.getTableToItemCount(); j++) {
                SchemaGenTableToItemType ti = item.getTableToItem(j);
                putExtendComplexType(ti, item.getTableName());
            }
        }
    }

    private static SchemaGenTableToItemType getRelatedTable(SchemaGenTableToItemType item, String t) {
        for (int j = 0; j < item.getTableToItemCount(); j++) {
            SchemaGenTableToItemType ti = item.getTableToItem(j);
            if (ti.getTableName().equals(t) && ti.getTableToItemCount() > 0) return ti;
        }
        return null;
    }

    private static void putExtendComplexType(SchemaGenTableToItemType item, DbTableType table1) throws Exception {
        if (item.getTableToItemCount() != 0) {
            DbTableType t = new DbTableType();
            DbTableType temp1 = DatabaseManager.getTableFromStructure(dbSchema, item.getTableName());
            t.setPrimaryKey(temp1.getPrimaryKey());
            for (int i = 0; i < temp1.getFieldsCount(); i++) {
                DbFieldType f = temp1.getFields(i);
                if (isFk(item, temp1, f, null) == null) t.addFields(f);
            }
            t.setName(temp1.getName() + "_EXTEND");
            createComplexTypeItem(t);
            for (int j = 0; j < item.getTableToItemCount(); j++) {
                SchemaGenTableToItemType ti = item.getTableToItem(j);
                DbTableType temp = DatabaseManager.getTableFromStructure(dbSchema, ti.getTableName());
                String nameItemTemp = null;
                String baseTemp = StringTools.capitalizeString(ti.getTableName());
                if (ti.getTableToItemCount() == 0) {
                    nameItemTemp = lowerFirstChar(baseTemp) + SIMPLE_ITEM;
                    extendCode += "            Get" + baseTemp + "Item " + baseTemp + "Object = " + "Get" + baseTemp + "Item.getInstance(adapter, tempItem" + countTempParam + ".item." + findRelateKey(table1, ti.getTableName()) + " );\n" + "            if (" + baseTemp + "Object!=null && " + baseTemp + "Object.item!=null)\n" + "                " + nameItemTemp + " = " + baseTemp + "Object.item;\n";
                } else {
                    String base = StringTools.capitalizeString(table1.getName());
                    String classNameItemTemp = baseTemp + "For" + base + EXTEND_ITEM_TYPE;
                    nameItemTemp = lowerFirstChar(baseTemp) + EXTEND_ITEM;
                    extendCode += "            " + nameItemTemp + " = _get" + baseTemp + EXTEND_ITEM + "(adapter, tempItem" + countTempParam + ".item." + findRelateKey(table1, ti.getTableName()) + " );\n";
                    extendCodeImport += "import " + config.getJavaPackageXmlSchema() + '.' + classNameItemTemp + ";\n";
                    extendCodeProc += "    private " + classNameItemTemp + " _get" + baseTemp + EXTEND_ITEM + "(" + db.getFactoryMethod() + " adapter, Long id) " + putExceptionDefinition() + "    {\n" + "" + "    }\n" + "\n";
                    for (int l = 0; l < temp.getFieldsCount(); l++) {
                        DbFieldType field = temp.getFields(l);
                        boolean isNotFk = true;
                        for (int n = 0; n < temp.getImportedKeysCount(); n++) {
                            DbImportedPKColumnType fk = temp.getImportedKeys(n);
                            if (field.getName().equals(fk.getFkColumnName())) {
                                isNotFk = false;
                                break;
                            }
                        }
                        if (isNotFk) {
                            String pf = StringTools.capitalizeString(field.getName());
                            String nameItem = lowerFirstChar(StringTools.capitalizeString(temp.getName())) + EXTEND_ITEM;
                            extendCode += "            " + nameItem + ".set" + pf + "( tempItem" + countTempParam + ".item.get" + pf + "() );\n";
                        }
                    }
                }
                putExtendComplexType(ti, item.getTableName());
            }
        }
    }

    private static String isFk(SchemaGenTableToItemType item, DbTableType t, DbFieldType f, DbTableType top) throws Exception {
        DbTableType temp = t;
        for (int i = 0; i < temp.getImportedKeysCount(); i++) {
            DbImportedPKColumnType fk = temp.getImportedKeys(i);
            System.out.println("check field " + f.getName() + ", fk.getFkColumnName() " + fk.getFkColumnName() + ", fk.getFkColumnName().equals(f.getName()) " + fk.getFkColumnName().equals(f.getName()));
            if (fk.getFkColumnName().equals(f.getName())) {
                for (int k = 0; k < item.getTableToItemCount(); k++) {
                    SchemaGenTableToItemType ti = item.getTableToItem(k);
                    System.out.println("ti.getTableName() " + ti.getTableName() + ", fk.getPkTableName() " + fk.getPkTableName());
                    if (ti.getTableName().equals(fk.getPkTableName())) return ti.getTableName();
                }
            }
        }
        System.out.println("#34 top " + top + ", top.name " + (top != null ? top.getName() : "null"));
        if (top == null) return null;
        temp = top;
        for (int i = 0; i < temp.getImportedKeysCount(); i++) {
            DbImportedPKColumnType fk = temp.getImportedKeys(i);
            System.out.println("check field " + f.getName() + ", fk.getFkColumnName() " + fk.getFkColumnName() + ", fk.getFkColumnName().equals(f.getName()) " + fk.getFkColumnName().equals(f.getName()));
            if (fk.getFkColumnName().equals(f.getName())) {
                for (int k = 0; k < item.getTableToItemCount(); k++) {
                    SchemaGenTableToItemType ti = item.getTableToItem(k);
                    System.out.println("ti.getTableName() " + ti.getTableName() + ", fk.getPkTableName() " + fk.getPkTableName());
                    if (ti.getTableName().equals(fk.getPkTableName())) return ti.getTableName();
                }
            }
        }
        return null;
    }

    private static String extendCode = "";

    private static String extendCodeProc = "";

    private static String extendCodeImport = "";

    private static int countTempParam = 0;

    private static final String EXTEND_ITEM_TYPE = "ExtendItemType";

    private static final String EXTEND_ITEM = "ExtendItem";

    private static final String SIMPLE_ITEM_TYPE = "ItemType";

    private static final String SIMPLE_ITEM = "Item";

    private static void createGetExtendClass() throws Exception {
        countTempParam = 1;
        for (int i = 0; i < config.getExtendClassesCount(); i++) {
            extendCode = "";
            extendCodeProc = "";
            extendCodeImport = "";
            countTempParam++;
            SchemaGenExtendClassType ec = config.getExtendClasses(i);
            SchemaGenTableToItemType topTable = ec.getTopTable();
            DbTableType table = DatabaseManager.getTableFromStructure(dbSchema, topTable.getTableName());
            putExtendComplexType(topTable, table);
            DbPrimaryKeyType pk = table.getPrimaryKey();
            if (pk.getColumnsCount() == 0) throw new Exception("Error create extend class 'cos table '" + table.getName() + "' not containt PK");
            DbPrimaryKeyColumnType column = pk.getColumns(0);
            for (int k = 0; k < table.getImportedKeysCount(); k++) {
                DbImportedPKColumnType key = table.getImportedKeys(k);
                if (DatabaseManager.isMultiColumnFk(table, key)) {
                    System.out.println("Create class list for table with multicolumn foreign keys not supported");
                    continue;
                }
                String s = "";
                String base = StringTools.capitalizeString(table.getName());
                String className = "Get" + base + EXTEND_ITEM;
                String classNameItem = base + EXTEND_ITEM_TYPE;
                String nameItem = lowerFirstChar(base) + EXTEND_ITEM;
                String addImport = "";
                for (int j = 0; j < topTable.getTableToItemCount(); j++) {
                    SchemaGenTableToItemType ti = topTable.getTableToItem(j);
                    String classNameItemTemp = null;
                    String baseTemp = StringTools.capitalizeString(ti.getTableName());
                    if (ti.getTableToItemCount() == 0) {
                        classNameItemTemp = baseTemp + SIMPLE_ITEM_TYPE;
                    } else {
                        classNameItemTemp = baseTemp + "For" + base + EXTEND_ITEM_TYPE;
                    }
                    addImport += "import " + config.getJavaPackageXmlSchema() + '.' + classNameItemTemp + ";\n";
                }
                s = putImportAndDeclarePart(classNameItem, className, addImport + extendCodeImport) + "    private " + classNameItem + " " + nameItem + " = null;\n" + "    public " + classNameItem + " get" + base + EXTEND_ITEM + "(){ return " + nameItem + "; }\n";
                for (int j = 0; j < topTable.getTableToItemCount(); j++) {
                    SchemaGenTableToItemType ti = topTable.getTableToItem(j);
                    String classNameItemTemp = null;
                    String nameItemTemp = null;
                    String baseTemp = StringTools.capitalizeString(ti.getTableName());
                    String baseType = null;
                    if (ti.getTableToItemCount() == 0) {
                        classNameItemTemp = baseTemp + SIMPLE_ITEM_TYPE;
                        baseType = baseTemp + SIMPLE_ITEM;
                    } else {
                        classNameItemTemp = baseTemp + "For" + base + EXTEND_ITEM_TYPE;
                        baseType = baseTemp + EXTEND_ITEM;
                    }
                    nameItemTemp = lowerFirstChar(baseType);
                    s += "    private " + classNameItemTemp + " " + nameItemTemp + " = null;\n" + "    public " + classNameItemTemp + " get" + baseType + "(){ return " + nameItemTemp + "; }\n";
                }
                s += "    public boolean isFound = false;\n" + "\n" + "    public static " + className + " getInstance(" + db.getFactoryMethod() + " adapter, Long id__) " + putExceptionDefinition() + "    {\n" + (config.getIsUseCache() ? "        return (" + className + ") cache.getInstanceNew(adapter, id__);\n" : "        " + className + " tempItem = new " + className + "(adapter, id__ );\n" + "        return tempItem;\n") + "    }\n" + "\n" + extendCodeProc + "\n" + "    public " + className + "(" + db.getFactoryMethod() + " adapter, Long id) " + putExceptionDefinition() + "    {\n" + "\n" + "        try\n" + "        {\n" + "            Get" + base + "Item tempItem" + countTempParam + " = Get" + base + "Item.getInstance(adapter, id);\n" + "            if (tempItem" + countTempParam + "==null || tempItem" + countTempParam + ".item==null)\n" + "                return;\n" + "            " + nameItem + " = new " + classNameItem + "();\n";
                for (int l = 0; l < table.getFieldsCount(); l++) {
                    DbFieldType field = table.getFields(l);
                    boolean isNotFk = true;
                    for (int n = 0; n < table.getImportedKeysCount(); n++) {
                        DbImportedPKColumnType fk = table.getImportedKeys(n);
                        if (field.getName().equals(fk.getFkColumnName())) {
                            isNotFk = false;
                            break;
                        }
                    }
                    if (isNotFk) {
                        String pf = StringTools.capitalizeString(field.getName());
                        s += "            " + nameItem + ".set" + pf + "( tempItem" + countTempParam + ".item.get" + pf + "() );\n";
                    }
                }
                s += "\n" + extendCode + "\n";
                s += getEndOfClass(className, classNameItem, "test-" + table.getName().toLowerCase() + "-list.xml", table, "S", true, false, false);
                writeClass(className, s);
            }
        }
    }

    private static String findRelateKey(DbTableType table, String tableName) throws Exception {
        DbTableType relateTable = DatabaseManager.getTableFromStructure(dbSchema, tableName);
        if (relateTable == null) throw new Exception("Target relate table '" + tableName + "' not found");
        DbTableType mainTable = table;
        System.out.println("main - " + mainTable.getName() + ", relate - " + relateTable.getName());
        for (int i = 0; i < mainTable.getImportedKeysCount(); i++) {
            DbImportedPKColumnType fk = mainTable.getImportedKeys(i);
            System.out.println("fk " + fk.getFkName() + ", " + fk.getFkTableName() + "." + fk.getFkColumnName() + "->" + fk.getPkTableName() + "." + fk.getPkColumnName());
            if (fk.getPkTableName().equals(relateTable.getName())) return "get" + StringTools.capitalizeString(fk.getFkColumnName()) + "()";
        }
        System.out.println("main - " + relateTable.getName() + ", relate - " + mainTable.getName());
        for (int i = 0; i < relateTable.getImportedKeysCount(); i++) {
            DbImportedPKColumnType fk = relateTable.getImportedKeys(i);
            System.out.println("fk " + fk.getFkName() + ", " + fk.getFkTableName() + "." + fk.getFkColumnName() + "->" + fk.getPkTableName() + "." + fk.getPkColumnName());
            if (fk.getPkTableName().equals(mainTable.getName())) return "get" + StringTools.capitalizeString(fk.getFkColumnName()) + "()";
        }
        throw new Exception("relation for table '" + tableName + "' not found");
    }

    private static void processTables() throws Exception {
        for (int i = 0; i < dbSchema.getTablesCount(); i++) {
            DbTableType table = dbSchema.getTables(i);
            if (DatabaseManager.isSkipTable(table.getName())) {
                continue;
            }
            if (!isIncludeTable(table) || excludeTable(table)) {
                System.out.println("Skip table '" + table.getName() + "'");
                continue;
            }
            System.out.println("Table '" + table.getName());
            isApplModule = false;
            if (isAuthAccessTable(table)) isApplModule = true;
            System.out.println("      'createComplexTypeItem'");
            createComplexTypeItem(table);
            System.out.println("      'createClassForPk'");
            createGetClassForPk(table);
            System.out.println("      'createInsertClassForPk'");
            createInsertClassForPk(table);
            System.out.println("      'createUpdateClassForPk'");
            createUpdateClassForPk(table);
            System.out.println("      'createDeleteClassForPk'");
            createDeleteClassForPk(table);
            System.out.println("      'createDeleteItemForPk'");
            createDeleteItemForPk(table);
            System.out.println("      'createDeleteListForFk'");
            createDeleteListForFk(table);
            System.out.println("      'createComplexTypeList'");
            createComplexTypeList(table);
            System.out.println("      'createClassForFk'");
            createGetClassForFk(table);
            System.out.println("      'createGetClassFullTable'");
            createGetClassFullTable(table);
        }
    }

    public static void main(String[] s) throws Exception {
        if (s == null || s.length == 0) {
            System.out.println("GenSchema config file not specified in command line");
            System.exit(1);
            return;
        }
        File file = new File(s[0]);
        if (!file.exists()) {
            System.out.println("GenSchema config file '" + s[0] + "' not found");
            System.exit(1);
            return;
        }
        StartupApplication.init();
        System.out.println("Current directory " + PropertiesProvider.getApplicationPath());
        InputSource inSrc = new InputSource(new FileInputStream(file));
        config = (SchemaGenConfigType) Unmarshaller.unmarshal(SchemaGenConfigType.class, inSrc);
        db = config.getDatabase();
        if (db == null) throw new IllegalArgumentException("Element 'Database' not initialized");
        System.out.println("Castor build version: " + Version.getBuildVersion());
        Namespaces ns = schema.getNamespaces();
        ns.createNamespaces();
        schema.addNamespace(config.getDefaultNamespacePrefix(), Schema.DEFAULT_SCHEMA_NS);
        schema.addNamespace(config.getCustomNamespacePrefix(), config.getCustomNamespace());
        schema.setTargetNamespace(config.getCustomNamespace());
        Iterator it = config.getNamespaceAsReference().iterator();
        while (it.hasNext()) {
            NamespaceType namespace = (NamespaceType) it.next();
            schema.addNamespace(namespace.getNamespaceAlias(), namespace.getNamespace());
        }
        if (config.getImportSchema() != null) {
            Schema importSchema = new Schema();
            importSchema.setSchemaLocation(config.getImportSchema().getSchemaLocation());
            importSchema.setTargetNamespace(config.getImportSchema().getNamespace());
            if (config.getSuperClass() != null) {
                baseType = new ComplexType(importSchema, config.getSuperClass().getComplexType());
                System.out.println("baseType.getName() = " + baseType.getName());
            }
            schema.addImportedSchema(importSchema);
        }
        packageClass = config.getTargetJavaPackage();
        logger = config.getLogger();
        if (logger == null || logger.getLogFactoryMethod() == null || logger.getLogObjectClassName() == null) config.setIsUseLogging(false);
        if (s.length < 2) {
            System.out.println("Create DbSchema from database");
            DatabaseAdapter db_ = null;
            try {
                db_ = DatabaseAdapter.getInstance();
                dbSchema = DatabaseManager.getDbStructure(db_);
            } finally {
                DatabaseManager.close(db_);
                db_ = null;
            }
            XmlTools.writeToFile(dbSchema, GenericConfig.getGenericTempDir() + "db-schema.xml", "utf-8", null, JsmithyNamespases.namespace);
        } else {
            System.out.println("Create DbSchema from file " + s[1]);
            InputSource inSrcDb = new InputSource(new FileInputStream(s[1]));
            dbSchema = (DbSchemaType) Unmarshaller.unmarshal(DbSchemaType.class, inSrcDb);
        }
        if (config.getBaseInterface() != null) {
            if (config.getBaseInterface().getInsertInterface() != null) {
                isInsertInterface = true;
                insertInterface = config.getBaseInterface().getInsertInterface();
            }
            if (config.getBaseInterface().getUpdateInterface() != null) {
                isUpdateInterface = true;
                updateInterface = config.getBaseInterface().getUpdateInterface();
            }
            if (config.getBaseInterface().getDeleteInterface() != null) {
                isDeleteInterface = true;
                deleteInterface = config.getBaseInterface().getDeleteInterface();
            }
        }
        validate();
        processTables();
        System.out.print("Start write schema to file " + preparePath(config.getXmlSchemaFile()) + " ... ");
        FileOutputStream fos = new FileOutputStream(preparePath(config.getXmlSchemaFile()));
        SchemaWriter writer = new SchemaWriter(new OutputStreamWriter(fos, "utf-8"));
        writer.write(schema);
        System.out.println("done.");
    }

    private static void validate() throws Exception {
        if (config.getBaseInterface() != null && config.getPersistenceExceptionName() == null) throw new Exception("If you define BaseInterface, you must define PersistenceExceptionName too.");
        if (config.getBaseInterface() != null && config.getSuperClass() == null) throw new Exception("If you define BaseInterface, you must define SuperClass for entity bean too.");
    }

    private static String preparePath(String path) {
        if (path == null) return null;
        return path.replace(File.separatorChar == '/' ? '\\' : '/', File.separatorChar);
    }
}
