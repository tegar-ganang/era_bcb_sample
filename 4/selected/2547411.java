package com.dyuproject.protostuff.me;

import java.io.IOException;
import com.dyuproject.protostuff.me.ByteString;
import com.dyuproject.protostuff.me.Input;
import com.dyuproject.protostuff.me.Message;
import com.dyuproject.protostuff.me.Output;
import com.dyuproject.protostuff.me.Pipe;
import com.dyuproject.protostuff.me.Schema;

public final class Bar implements Message, Schema {

    public interface Status {

        public static final int PENDING = 1;

        public static final int STARTED = 2;

        public static final int COMPLETED = 3;
    }

    public static Schema getSchema() {
        return DEFAULT_INSTANCE;
    }

    public static Bar getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    static final Bar DEFAULT_INSTANCE = new Bar();

    private int someInt;

    private String someString;

    private Baz someBaz;

    private int someEnum = Status.PENDING;

    private ByteString someBytes;

    private Boolean someBoolean;

    private float someFloat;

    private double someDouble;

    private long someLong;

    public Bar() {
    }

    public int getSomeInt() {
        return someInt;
    }

    public void setSomeInt(int someInt) {
        this.someInt = someInt;
    }

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }

    public Baz getSomeBaz() {
        return someBaz;
    }

    public void setSomeBaz(Baz someBaz) {
        this.someBaz = someBaz;
    }

    public int getSomeEnum() {
        return someEnum;
    }

    public void setSomeEnum(int someEnum) {
        this.someEnum = someEnum;
    }

    public ByteString getSomeBytes() {
        return someBytes;
    }

    public void setSomeBytes(ByteString someBytes) {
        this.someBytes = someBytes;
    }

    public Boolean getSomeBoolean() {
        return someBoolean;
    }

    public void setSomeBoolean(Boolean someBoolean) {
        this.someBoolean = someBoolean;
    }

    public float getSomeFloat() {
        return someFloat;
    }

    public void setSomeFloat(float someFloat) {
        this.someFloat = someFloat;
    }

    public double getSomeDouble() {
        return someDouble;
    }

    public void setSomeDouble(double someDouble) {
        this.someDouble = someDouble;
    }

    public long getSomeLong() {
        return someLong;
    }

    public void setSomeLong(long someLong) {
        this.someLong = someLong;
    }

    public Schema cachedSchema() {
        return DEFAULT_INSTANCE;
    }

    public Object newMessage() {
        return new Bar();
    }

    public Class typeClass() {
        return Bar.class;
    }

    public String messageName() {
        return "Bar";
    }

    public String messageFullName() {
        return Bar.class.getName();
    }

    public boolean isInitialized(Object message) {
        return true;
    }

    public void mergeFrom(Input input, Object messageObj) throws IOException {
        Bar message = (Bar) messageObj;
        for (int number = input.readFieldNumber(this); ; number = input.readFieldNumber(this)) {
            switch(number) {
                case 0:
                    return;
                case 1:
                    message.someInt = input.readInt32();
                    break;
                case 2:
                    message.someString = input.readString();
                    break;
                case 3:
                    message.someBaz = (Baz) input.mergeObject(message.someBaz, Baz.getSchema());
                    break;
                case 4:
                    message.someEnum = input.readEnum();
                    break;
                case 5:
                    message.someBytes = input.readBytes();
                    break;
                case 6:
                    message.someBoolean = input.readBool() ? Boolean.TRUE : Boolean.FALSE;
                    break;
                case 7:
                    message.someFloat = input.readFloat();
                    break;
                case 8:
                    message.someDouble = input.readDouble();
                    break;
                case 9:
                    message.someLong = input.readInt64();
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    public void writeTo(Output output, Object messageObj) throws IOException {
        Bar message = (Bar) messageObj;
        if (message.someInt != 0) output.writeInt32(1, message.someInt, false);
        if (message.someString != null) output.writeString(2, message.someString, false);
        if (message.someBaz != null) output.writeObject(3, message.someBaz, Baz.getSchema(), false);
        output.writeEnum(4, message.someEnum, false);
        if (message.someBytes != null) output.writeBytes(5, message.someBytes, false);
        if (message.someBoolean != null) output.writeBool(6, message.someBoolean.booleanValue(), false);
        if (message.someFloat != 0) output.writeFloat(7, message.someFloat, false);
        if (message.someDouble != 0) output.writeDouble(8, message.someDouble, false);
        if (message.someLong != 0) output.writeInt64(9, message.someLong, false);
    }

    public String getFieldName(int number) {
        switch(number) {
            case 1:
                return "someInt";
            case 2:
                return "someString";
            case 3:
                return "someBaz";
            case 4:
                return "someEnum";
            case 5:
                return "someBytes";
            case 6:
                return "someBoolean";
            case 7:
                return "someFloat";
            case 8:
                return "someDouble";
            case 9:
                return "someLong";
            default:
                return null;
        }
    }

    public int getFieldNumber(String name) {
        final Integer number = (Integer) __fieldMap.get(name);
        return number == null ? 0 : number.intValue();
    }

    private static final java.util.Hashtable __fieldMap = new java.util.Hashtable();

    static {
        __fieldMap.put("someInt", new Integer(1));
        __fieldMap.put("someString", new Integer(2));
        __fieldMap.put("someBaz", new Integer(3));
        __fieldMap.put("someEnum", new Integer(4));
        __fieldMap.put("someBytes", new Integer(5));
        __fieldMap.put("someBoolean", new Integer(6));
        __fieldMap.put("someFloat", new Integer(7));
        __fieldMap.put("someDouble", new Integer(8));
        __fieldMap.put("someLong", new Integer(9));
    }

    static final Pipe.Schema PIPE_SCHEMA = new Pipe.Schema(DEFAULT_INSTANCE) {

        protected void transfer(Pipe pipe, Input input, Output output) throws IOException {
            for (int number = input.readFieldNumber(wrappedSchema); ; number = input.readFieldNumber(wrappedSchema)) {
                switch(number) {
                    case 0:
                        return;
                    case 1:
                        output.writeInt32(number, input.readInt32(), false);
                        break;
                    case 2:
                        input.transferByteRangeTo(output, true, number, false);
                        break;
                    case 3:
                        output.writeObject(number, pipe, Baz.getPipeSchema(), false);
                        break;
                    case 4:
                        output.writeEnum(number, input.readEnum(), false);
                        break;
                    case 5:
                        input.transferByteRangeTo(output, false, number, false);
                        break;
                    case 6:
                        output.writeBool(number, input.readBool(), false);
                        break;
                    case 7:
                        output.writeFloat(number, input.readFloat(), false);
                        break;
                    case 8:
                        output.writeDouble(number, input.readDouble(), false);
                        break;
                    case 9:
                        output.writeInt64(number, input.readInt64(), false);
                        break;
                    default:
                        input.handleUnknownField(number, wrappedSchema);
                }
            }
        }
    };

    public static Pipe.Schema getPipeSchema() {
        return PIPE_SCHEMA;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((someBaz == null) ? 0 : someBaz.hashCode());
        result = prime * result + ((someBoolean == null) ? 0 : someBoolean.hashCode());
        result = prime * result + ((someBytes == null) ? 0 : someBytes.hashCode());
        long temp;
        temp = Double.doubleToLongBits(someDouble);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + someEnum;
        result = prime * result + Float.floatToIntBits(someFloat);
        result = prime * result + someInt;
        result = prime * result + (int) (someLong ^ (someLong >>> 32));
        result = prime * result + ((someString == null) ? 0 : someString.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Bar other = (Bar) obj;
        if (someBaz == null) {
            if (other.someBaz != null) return false;
        } else if (!someBaz.equals(other.someBaz)) return false;
        if (someBoolean == null) {
            if (other.someBoolean != null) return false;
        } else if (!someBoolean.equals(other.someBoolean)) return false;
        if (someBytes == null) {
            if (other.someBytes != null) return false;
        } else if (!someBytes.equals(other.someBytes)) return false;
        if (Double.doubleToLongBits(someDouble) != Double.doubleToLongBits(other.someDouble)) return false;
        if (someEnum != other.someEnum) return false;
        if (Float.floatToIntBits(someFloat) != Float.floatToIntBits(other.someFloat)) return false;
        if (someInt != other.someInt) return false;
        if (someLong != other.someLong) return false;
        if (someString == null) {
            if (other.someString != null) return false;
        } else if (!someString.equals(other.someString)) return false;
        return true;
    }
}
