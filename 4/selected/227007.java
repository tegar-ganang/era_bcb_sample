package br.com.mcampos.controller.anoto.util.icr;

import br.com.mcampos.sysutils.SysUtils;
import java.util.Map;
import org.jawin.COMException;

public class A2iaRequest extends A2iaBase {

    private A2iaDocument document;

    private A2iaChannel channel;

    private A2iaTemplate template;

    private Integer timeout = 60000;

    private A2iaResult result;

    private Map<String, IcrField> fields;

    public A2iaRequest(A2ia a2ia, A2iaChannel channel, A2iaTemplate template) throws COMException {
        super(a2ia);
        setChannel(channel);
        setTemplate(template);
        open();
    }

    protected void setDocument(A2iaDocument document) {
        this.document = document;
    }

    protected A2iaDocument getDocument() {
        return document;
    }

    protected void setChannel(A2iaChannel channel) {
        this.channel = channel;
    }

    protected A2iaChannel getChannel() {
        return channel;
    }

    protected void open() throws COMException {
        setId(getIcrObj().openRequest(this));
    }

    public void process() throws COMException {
        A2iaResult result = getIcrObj().processRequest(this);
        setResult(result);
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getTimeout() {
        return timeout;
    }

    protected void setResult(A2iaResult result) {
        this.result = result;
    }

    public A2iaResult getResult() throws COMException {
        return result;
    }

    public void close() throws COMException {
        if (SysUtils.isZero(getId()) == false) {
            getIcrObj().close(this);
            setId(null);
            System.out.println("Request is finalized!");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
        System.out.println("Request is finalized!");
    }

    private void setTemplate(A2iaTemplate template) {
        this.template = template;
        fields = template.getFields();
        setDocument(template.getDefaultDocument());
    }

    public A2iaTemplate getTemplate() {
        return template;
    }

    public Map<String, IcrField> getFields() {
        return fields;
    }

    public void setFieldValue(Integer fieldIndex, String value, Integer score) throws COMException {
        IcrField field = getFields().get(template.getFieldName(fieldIndex));
        if (field != null) {
            field.setValue(value);
            field.setScore(score);
        }
    }
}
