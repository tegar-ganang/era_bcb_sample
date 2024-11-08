package org.crappydbms.relations.schemas.attributes;

import java.io.DataInputStream;
import java.io.IOException;
import org.crappydbms.exceptions.CrappyDBMSError;
import org.crappydbms.relations.fields.Field;
import org.crappydbms.relations.fields.StringField;

/**
 * @author Facundo Manuel Quiroga
 * Nov 4, 2008
 * 
 */
public class StringAttributeType implements AttributeType {

    protected static StringAttributeType instance;

    public static StringAttributeType getInstance() {
        if (StringAttributeType.instance == null) {
            StringAttributeType.instance = new StringAttributeType();
        }
        return StringAttributeType.instance;
    }

    private StringAttributeType() {
    }

    public String toString() {
        return "string";
    }

    @Override
    public int getSerializedSize() {
        return 50;
    }

    public int getMaximumLength() {
        return 49;
    }

    @Override
    public Field read(DataInputStream input) throws IOException {
        int size = this.getSerializedSize();
        byte[] bytes = new byte[size];
        if (input.read(bytes, 0, size) != size) {
            throw new CrappyDBMSError("Cant read from in-memory byte array");
        } else {
            byte stringLength = bytes[0];
            if (stringLength > this.getMaximumLength()) {
                throw new CrappyDBMSError("String length (" + stringLength + ") surpasses maximum length (" + this.getMaximumLength() + ")");
            }
            byte[] string = new byte[stringLength];
            for (int i = 0; i < stringLength; i++) {
                string[i] = bytes[i + 1];
            }
            return StringField.valueOf(new String(string));
        }
    }

    @Override
    public boolean isTypeOf(Field field) {
        return StringField.class.isInstance(field);
    }
}
