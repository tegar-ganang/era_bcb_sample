package org.carp.engine.event;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.util.List;
import org.apache.log4j.Logger;
import org.carp.annotation.CarpAnnotation.Generate;
import org.carp.beans.ColumnsMetadata;
import org.carp.beans.PrimarysMetadata;
import org.carp.engine.ParametersProcessor;
import org.carp.engine.cascade.SaveCascade;
import org.carp.exception.CarpException;
import org.carp.id.AutoGenerator;
import org.carp.id.Generator;
import org.carp.id.SequenceGenerator;
import org.carp.impl.AbstractCarpSession;
import org.carp.sql.OracleCarpSql;

/**
 * �־û�������
 * @author zhou
 * @since 0.1
 */
public class SaveEvent extends Event {

    private static final Logger logger = Logger.getLogger(SaveEvent.class);

    private java.io.Serializable id;

    public SaveEvent(AbstractCarpSession session, Object entity) throws CarpException {
        super(session, entity, "insert");
    }

    /**
	 * ����Statement�����������ֵ
	 */
    @Override
    public void processStatmentParameters(ParametersProcessor psProcess) throws Exception {
        this.processFieldValues(psProcess);
        generateKeyBefore();
        this.processPrimaryValues(psProcess);
    }

    /**
	 * ����ʵ���id��ֵ
	 * @param psProcess
	 * @throws Exception
	 */
    protected void processPrimaryValues(ParametersProcessor psProcess) throws Exception {
        List<PrimarysMetadata> pms = this.getBean().getPrimarys();
        for (int i = 0, count = pms.size(); i < count; ++i) {
            PrimarysMetadata pk = pms.get(i);
            if (pk.getBuild() != Generate.auto) {
                Class<?> ft = pk.getFieldType();
                Object value = pk.getValue(this.getEntity());
                int _index = this.getNextIndex();
                if (logger.isDebugEnabled()) {
                    logger.debug("��������" + _index + " , ��������:" + pk.getColName() + " , FieldName:" + pk.getFieldName() + " , FieldType:" + pk.getFieldType().getName() + " ,FieldValue: " + value);
                    if (value != null) logger.debug(value.getClass().getName());
                }
                id = (Serializable) value;
                psProcess.setStatementParameters(value, ft, _index);
            }
        }
    }

    public Serializable getPrimaryValue() {
        return id;
    }

    /**
	 * ����������ʽΪcustom��sequenceʱ����ɵ�����ֵ
	 * @throws Exception
	 */
    private void generateKeyBefore() throws Exception {
        List<PrimarysMetadata> pms = getBean().getPrimarys();
        for (int i = 0, count = pms.size(); i < count; ++i) {
            PrimarysMetadata pk = pms.get(i);
            if (pk.getBuild() == Generate.sequences) {
                Generator generate = new SequenceGenerator();
                id = generate.generate(this.getSession(), this.getEntity(), pk);
            } else if (pk.getBuild() == Generate.custom) {
                Generator build = (Generator) pk.getBuilder().newInstance();
                id = build.generate(this.getSession(), this.getEntity(), pk);
            }
        }
    }

    /**
	 * ����������ʽΪautoʱ����ɵ�����ֵ
	 * @throws Exception
	 */
    private void generateKeyAfter() throws Exception {
        List<PrimarysMetadata> pms = getBean().getPrimarys();
        for (int i = 0, count = pms.size(); i < count; ++i) {
            PrimarysMetadata pk = pms.get(i);
            if (pk.getBuild() == Generate.auto) {
                Generator generate = new AutoGenerator();
                id = generate.generate(this.getSession(), this.getEntity(), pk);
            }
        }
    }

    /**
	 * ��������
	 */
    @Override
    public void cascadeAfterOperator() throws Exception {
        SaveCascade cascade = new SaveCascade(this.getSession(), this.getBean(), this.getEntity(), id);
        cascade.cascadeOTOOperator().cascadeOTMOperator();
    }

    @Override
    public void cascadeBeforeOperator() throws Exception {
    }

    @Override
    public void executeBefore() throws Exception {
        if (this.getSession().getInterceptor() != null) this.getSession().getInterceptor().onBeforeSave(getEntity(), getSession());
    }

    @Override
    public void executeAfter() throws Exception {
        generateKeyAfter();
        this.processBlob();
        if (this.getSession().getInterceptor() != null) this.getSession().getInterceptor().onAfterSave(getEntity(), getSession());
    }

    /**
	 * �����ݿ�Ϊoracle������£���������blob��clob���͵��ֶ�
	 * @throws Exception
	 */
    @SuppressWarnings("deprecation")
    private void processBlob() throws Exception {
        String updateSql = "select ";
        if (this.getSession().getJdbcContext().getContext().getCarpSqlClass().equals(OracleCarpSql.class)) {
            List<ColumnsMetadata> cols = this.getBean().getColumns();
            for (int i = 0; i < cols.size(); ++i) {
                ColumnsMetadata col = cols.get(i);
                if (col.getFieldType().equals(Blob.class) || col.getFieldType().equals(Clob.class)) {
                    updateSql += col.getColName() + " ,";
                }
            }
            if (updateSql.equals("select ")) return;
            updateSql = updateSql.substring(0, updateSql.length() - 2) + " from " + this.getBean().getTable() + " where ";
            PrimarysMetadata pm = this.getBean().getPrimarys().get(0);
            if (pm.getFieldType().equals(String.class)) {
                updateSql += pm.getColName() + " = '" + id + "' for update";
            } else {
                updateSql += pm.getColName() + " = " + id + " for update";
            }
            ResultSet rs = this.getSession().creatDataSetQuery(updateSql).resultSet();
            while (rs != null && rs.next()) {
                for (int i = 0, index = 0; i < cols.size(); ++i) {
                    ColumnsMetadata col = cols.get(i);
                    if (col.getValue(getEntity()) == null) continue;
                    if (col.getFieldType().equals(Blob.class)) {
                        oracle.sql.BLOB blob = (oracle.sql.BLOB) rs.getBlob(++index);
                        this.setBlobValue(blob.getBinaryOutputStream(), (java.sql.Blob) col.getValue(getEntity()));
                    }
                    if (col.getFieldType().equals(Clob.class)) {
                        oracle.sql.CLOB clob = (oracle.sql.CLOB) rs.getClob(++index);
                        setClobValue(clob.getCharacterOutputStream(), (java.sql.Clob) col.getValue(getEntity()));
                    }
                }
            }
            rs.close();
        }
    }

    private void setBlobValue(OutputStream src, java.sql.Blob blob) throws Exception {
        InputStream stream = blob.getBinaryStream();
        byte[] b = new byte[4096];
        for (int len = -1; (len = stream.read(b, 0, 4096)) != -1; ) src.write(b, 0, len);
        src.flush();
        src.close();
        stream.close();
    }

    private void setClobValue(Writer src, java.sql.Clob clob) throws Exception {
        Reader stream = clob.getCharacterStream();
        char[] b = new char[4096];
        for (int len = -1; (len = stream.read(b, 0, 4096)) != -1; ) src.write(b, 0, len);
        src.flush();
        src.close();
        stream.close();
    }
}
