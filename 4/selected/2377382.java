package com.siemens.ct.exi.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import com.siemens.ct.exi.CodingMode;
import com.siemens.ct.exi.Constants;
import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.context.QNameContext;
import com.siemens.ct.exi.core.container.ValueAndDatatype;
import com.siemens.ct.exi.datatype.Datatype;
import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.io.channel.ByteEncoderChannel;
import com.siemens.ct.exi.io.channel.EncoderChannel;
import com.siemens.ct.exi.values.Value;

/**
 * EXI encoder for (pre-)compression streams.
 * 
 * @author Daniel.Peintner.EXT@siemens.com
 * @author Joerg.Heuer@siemens.com
 * 
 * @version 0.8
 */
public class EXIBodyEncoderReordered extends AbstractEXIBodyEncoder {

    protected OutputStream os;

    protected DeflaterOutputStream deflater;

    protected CodingMode codingMode;

    protected int blockValues;

    protected Value lastValue;

    protected Datatype lastDatatype;

    public EXIBodyEncoderReordered(EXIFactory exiFactory) throws EXIException {
        super(exiFactory);
        this.codingMode = exiFactory.getCodingMode();
    }

    @Override
    protected void initForEachRun() throws EXIException, IOException {
        super.initForEachRun();
        blockValues = 0;
    }

    protected void initBlock() {
        blockValues = 0;
        encoderContext.initCompressionBlock();
    }

    public void setOutputStream(OutputStream os) throws EXIException, IOException {
        this.os = os;
        channel = new ByteEncoderChannel(getStream());
    }

    public void setOutputChannel(EncoderChannel encoderChannel) {
        this.channel = encoderChannel;
        this.os = channel.getOutputStream();
    }

    @Override
    protected boolean isTypeValid(Datatype datatype, Value value) {
        lastDatatype = datatype;
        lastValue = value;
        return super.isTypeValid(datatype, value);
    }

    @Override
    protected void writeValue(QNameContext valueContext) throws IOException {
        encoderContext.addValueAndDatatype(valueContext, new ValueAndDatatype(lastValue, lastDatatype));
        if (++blockValues == exiFactory.getBlockSize()) {
            closeBlock();
            initBlock();
            channel = new ByteEncoderChannel(getStream());
        }
    }

    protected OutputStream getStream() {
        if (codingMode == CodingMode.COMPRESSION) {
            deflater = new DeflaterOutputStream(os, new Deflater(codingMode.getDeflateLevel(), true));
            return deflater;
        } else {
            assert (codingMode == CodingMode.PRE_COMPRESSION);
            return os;
        }
    }

    protected void closeBlock() throws IOException {
        if (channel.getLength() == 0) {
        } else if (blockValues <= Constants.MAX_NUMBER_OF_VALUES) {
            for (QNameContext contextOrder : encoderContext.getChannelOrders()) {
                List<ValueAndDatatype> lvd = encoderContext.getValueAndDatatypes(contextOrder);
                for (ValueAndDatatype vd : lvd) {
                    typeEncoder.isValid(vd.datatype, vd.value);
                    typeEncoder.writeValue(encoderContext, contextOrder, channel);
                }
            }
            finalizeStream();
        } else {
            finalizeStream();
            EncoderChannel leq100 = new ByteEncoderChannel(getStream());
            boolean wasThereLeq100 = false;
            for (QNameContext contextOrder : encoderContext.getChannelOrders()) {
                List<ValueAndDatatype> lvd = encoderContext.getValueAndDatatypes(contextOrder);
                if (lvd.size() <= Constants.MAX_NUMBER_OF_VALUES) {
                    for (ValueAndDatatype vd : lvd) {
                        typeEncoder.isValid(vd.datatype, vd.value);
                        typeEncoder.writeValue(encoderContext, contextOrder, leq100);
                    }
                    wasThereLeq100 = true;
                }
            }
            if (wasThereLeq100) {
                finalizeStream();
            }
            for (QNameContext contextOrder : encoderContext.getChannelOrders()) {
                List<ValueAndDatatype> lvd = encoderContext.getValueAndDatatypes(contextOrder);
                if (lvd.size() > Constants.MAX_NUMBER_OF_VALUES) {
                    EncoderChannel gre100 = new ByteEncoderChannel(getStream());
                    for (ValueAndDatatype vd : lvd) {
                        typeEncoder.isValid(vd.datatype, vd.value);
                        typeEncoder.writeValue(encoderContext, contextOrder, gre100);
                    }
                    finalizeStream();
                }
            }
        }
    }

    protected void finalizeStream() throws IOException {
        if (codingMode == CodingMode.COMPRESSION) {
            deflater.finish();
        }
    }

    @Override
    public void flush() throws IOException {
        if (encoderContext != null) {
            closeBlock();
        }
        os.flush();
    }
}
