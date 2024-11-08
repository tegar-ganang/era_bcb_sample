package org.openorb.orb.io;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.BoxedValueHelper;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.omg.CORBA_2_3.portable.InputStream;
import org.openorb.orb.core.typecode.TypeCodeBase;
import org.openorb.orb.core.MinorCodes;
import org.openorb.util.RepoIDHelper;

/**
 * Utility class for copying the contents of an input stream to an output
 * stream, given a typecode for the contained data.
 *
 * @author Chris Wood
 * @version $Revision: 1.11 $ $Date: 2004/02/19 07:21:31 $
 */
public final class StreamHelper {

    private StreamHelper() {
    }

    /**
     * Copy input stream to output stream, according to the typecode passed.
     *
     * @param type the typecode of the data to be copied.
     * @param is the input stream source of the data.
     * @param os the destination for the copy.
     */
    public static void copy_stream(TypeCode type, InputStream is, OutputStream os) {
        try {
            type = TypeCodeBase._base_type(type);
            switch(type.kind().value()) {
                case TCKind._tk_null:
                case TCKind._tk_void:
                    return;
                case TCKind._tk_short:
                    os.write_short(is.read_short());
                    return;
                case TCKind._tk_long:
                    os.write_long(is.read_long());
                    return;
                case TCKind._tk_ushort:
                    os.write_ushort(is.read_ushort());
                    return;
                case TCKind._tk_ulong:
                    os.write_ulong(is.read_ulong());
                    return;
                case TCKind._tk_float:
                    os.write_float(is.read_float());
                    return;
                case TCKind._tk_double:
                    os.write_double(is.read_double());
                    return;
                case TCKind._tk_boolean:
                    os.write_boolean(is.read_boolean());
                    return;
                case TCKind._tk_char:
                    os.write_char(is.read_char());
                    return;
                case TCKind._tk_octet:
                    os.write_octet(is.read_octet());
                    return;
                case TCKind._tk_any:
                    os.write_any(is.read_any());
                    return;
                case TCKind._tk_TypeCode:
                    os.write_TypeCode(is.read_TypeCode());
                    return;
                case TCKind._tk_Principal:
                    os.write_Principal(is.read_Principal());
                    return;
                case TCKind._tk_objref:
                    os.write_Object(is.read_Object());
                    return;
                case TCKind._tk_except:
                    os.write_string(is.read_string());
                case TCKind._tk_struct:
                    for (int i = 0; i < type.member_count(); ++i) {
                        copy_stream(type.member_type(i), is, os);
                    }
                    return;
                case TCKind._tk_enum:
                    os.write_ulong(is.read_ulong());
                    return;
                case TCKind._tk_string:
                    os.write_string(is.read_string());
                    return;
                case TCKind._tk_longlong:
                    os.write_longlong(is.read_longlong());
                    return;
                case TCKind._tk_ulonglong:
                    os.write_ulonglong(is.read_ulonglong());
                    return;
                case TCKind._tk_longdouble:
                    throw new NO_IMPLEMENT();
                case TCKind._tk_wchar:
                    os.write_wchar(is.read_wchar());
                    return;
                case TCKind._tk_wstring:
                    os.write_wstring(is.read_wstring());
                    return;
                case TCKind._tk_fixed:
                    {
                        java.math.BigDecimal val;
                        if (is instanceof ExtendedInputStream) {
                            val = ((ExtendedInputStream) is).read_fixed(type);
                        } else {
                            val = is.read_fixed();
                        }
                        if (os instanceof ExtendedOutputStream) {
                            ((ExtendedOutputStream) os).write_fixed(val, type);
                        } else {
                            os.write_fixed(val);
                        }
                    }
                    return;
                case TCKind._tk_value:
                    os.write_value(is.read_value(type.id()));
                    return;
                case TCKind._tk_value_box:
                    {
                        BoxedValueHelper boxhelp = null;
                        String repo_id = type.id();
                        try {
                            String boxname = RepoIDHelper.idToClass(repo_id, RepoIDHelper.TYPE_HELPER);
                            boxhelp = (BoxedValueHelper) Thread.currentThread().getContextClassLoader().loadClass(boxname).newInstance();
                        } catch (Exception ex) {
                            boxhelp = new TypeCodeValueBoxHelper(os.orb(), type);
                        }
                        os.write_value(is.read_value(boxhelp), boxhelp);
                        return;
                    }
                case TCKind._tk_abstract_interface:
                    os.write_abstract_interface(is.read_abstract_interface());
                    return;
                case TCKind._tk_sequence:
                case TCKind._tk_array:
                    {
                        int length = type.length();
                        if (type.kind() == TCKind.tk_sequence) {
                            int truelen = is.read_ulong();
                            if (length != 0 && truelen > length) {
                                throw new MARSHAL("Sequence length out of bounds", MinorCodes.MARSHAL_SEQ_BOUND, CompletionStatus.COMPLETED_MAYBE);
                            }
                            length = truelen;
                            os.write_ulong(length);
                        }
                        TypeCode content = TypeCodeBase._base_type(type.content_type());
                        handleArrayContent(is, os, content, length);
                        return;
                    }
                case TCKind._tk_union:
                    {
                        TypeCode dsct = TypeCodeBase._base_type(type.discriminator_type());
                        handleUnionContent(is, os, type, dsct);
                        return;
                    }
                case TCKind._tk_alias:
                case TCKind._tk_native:
                default:
                    org.openorb.orb.util.Trace.signalIllegalCondition(null, "Unexpected type kind().value()==" + type.kind().value() + ".");
            }
        } catch (BadKind ex) {
            throw new CascadingRuntimeException("Unexpected BadKind exception during value copy", ex);
        } catch (Bounds ex) {
            throw new CascadingRuntimeException("Unexpected Bounds exception during value copy", ex);
        }
    }

    private static void handleUnionContent(InputStream is, OutputStream os, TypeCode type, TypeCode dsct) throws BadKind, Bounds {
        switch(dsct.kind().value()) {
            case TCKind._tk_boolean:
                {
                    boolean disc = is.read_boolean();
                    os.write_boolean(disc);
                    int defl = type.default_index();
                    for (int i = 0; i < type.member_count(); ++i) {
                        if (i != defl && type.member_label(i).extract_boolean() == disc) {
                            copy_stream(type.member_type(i), is, os);
                            return;
                        }
                    }
                    if (defl < 0) {
                        throw new MARSHAL("Union discriminator out of range", MinorCodes.MARSHAL_UNION_DISC, CompletionStatus.COMPLETED_NO);
                    }
                    copy_stream(type.member_type(defl), is, os);
                }
                return;
            case TCKind._tk_char:
                {
                    char disc = is.read_char();
                    os.write_char(disc);
                    int defl = type.default_index();
                    for (int i = 0; i < type.member_count(); ++i) {
                        if (i != defl && type.member_label(i).extract_char() == disc) {
                            copy_stream(type.member_type(i), is, os);
                            return;
                        }
                    }
                    if (defl < 0) {
                        throw new MARSHAL("Union discriminator out of range", MinorCodes.MARSHAL_UNION_DISC, CompletionStatus.COMPLETED_NO);
                    }
                    copy_stream(type.member_type(defl), is, os);
                }
                return;
            case TCKind._tk_wchar:
                {
                    char disc = is.read_wchar();
                    os.write_wchar(disc);
                    int defl = type.default_index();
                    for (int i = 0; i < type.member_count(); ++i) {
                        if (i != defl && type.member_label(i).extract_wchar() == disc) {
                            copy_stream(type.member_type(i), is, os);
                            return;
                        }
                    }
                    if (defl < 0) {
                        throw new MARSHAL("Union discriminator out of range", MinorCodes.MARSHAL_UNION_DISC, CompletionStatus.COMPLETED_NO);
                    }
                    copy_stream(type.member_type(defl), is, os);
                }
                return;
            case TCKind._tk_short:
                {
                    short disc = is.read_short();
                    os.write_short(disc);
                    int defl = type.default_index();
                    for (int i = 0; i < type.member_count(); ++i) {
                        if (i != defl && type.member_label(i).extract_short() == disc) {
                            copy_stream(type.member_type(i), is, os);
                            return;
                        }
                    }
                    if (defl < 0) {
                        throw new MARSHAL("Union discriminator out of range", MinorCodes.MARSHAL_UNION_DISC, CompletionStatus.COMPLETED_NO);
                    }
                    copy_stream(type.member_type(defl), is, os);
                }
                return;
            case TCKind._tk_ushort:
                {
                    short disc = is.read_ushort();
                    os.write_ushort(disc);
                    int defl = type.default_index();
                    for (int i = 0; i < type.member_count(); ++i) {
                        if (i != defl && type.member_label(i).extract_ushort() == disc) {
                            copy_stream(type.member_type(i), is, os);
                            return;
                        }
                    }
                    if (defl < 0) {
                        throw new MARSHAL("Union discriminator out of range", MinorCodes.MARSHAL_UNION_DISC, CompletionStatus.COMPLETED_NO);
                    }
                    copy_stream(type.member_type(defl), is, os);
                }
                return;
            case TCKind._tk_long:
                {
                    int disc = is.read_long();
                    os.write_long(disc);
                    int defl = type.default_index();
                    for (int i = 0; i < type.member_count(); ++i) {
                        if (i != defl && type.member_label(i).extract_long() == disc) {
                            copy_stream(type.member_type(i), is, os);
                            return;
                        }
                    }
                    if (defl < 0) {
                        throw new MARSHAL("Union discriminator out of range", MinorCodes.MARSHAL_UNION_DISC, CompletionStatus.COMPLETED_NO);
                    }
                    copy_stream(type.member_type(defl), is, os);
                }
                return;
            case TCKind._tk_enum:
            case TCKind._tk_ulong:
                {
                    int disc = is.read_ulong();
                    os.write_ulong(disc);
                    int defl = type.default_index();
                    for (int i = 0; i < type.member_count(); ++i) {
                        if (i != defl && type.member_label(i).extract_ulong() == disc) {
                            copy_stream(type.member_type(i), is, os);
                            return;
                        }
                    }
                    if (defl < 0) {
                        throw new MARSHAL("Union discriminator out of range", MinorCodes.MARSHAL_UNION_DISC, CompletionStatus.COMPLETED_NO);
                    }
                    copy_stream(type.member_type(defl), is, os);
                }
                return;
            case TCKind._tk_longlong:
                {
                    long disc = is.read_longlong();
                    os.write_longlong(disc);
                    int defl = type.default_index();
                    for (int i = 0; i < type.member_count(); ++i) {
                        if (i != defl && type.member_label(i).extract_longlong() == disc) {
                            copy_stream(type.member_type(i), is, os);
                            return;
                        }
                    }
                    if (defl < 0) {
                        throw new MARSHAL("Union discriminator out of range", MinorCodes.MARSHAL_UNION_DISC, CompletionStatus.COMPLETED_NO);
                    }
                    copy_stream(type.member_type(defl), is, os);
                }
                return;
            case TCKind._tk_ulonglong:
                {
                    long disc = is.read_ulonglong();
                    os.write_ulonglong(disc);
                    int defl = type.default_index();
                    for (int i = 0; i < type.member_count(); ++i) {
                        if (i != defl && type.member_label(i).extract_ulonglong() == disc) {
                            copy_stream(type.member_type(i), is, os);
                            return;
                        }
                    }
                    if (defl < 0) {
                        throw new MARSHAL("Union discriminator out of range", MinorCodes.MARSHAL_UNION_DISC, CompletionStatus.COMPLETED_NO);
                    }
                    copy_stream(type.member_type(defl), is, os);
                }
                return;
            default:
                org.openorb.orb.util.Trace.signalIllegalCondition(null, "Unexpected union discriminator type kind().value()==" + dsct.kind().value() + ".");
        }
    }

    private static void handleArrayContent(InputStream is, OutputStream os, TypeCode content, int length) throws BadKind, Bounds {
        switch(content.kind().value()) {
            case TCKind._tk_null:
            case TCKind._tk_void:
                return;
            case TCKind._tk_short:
                {
                    short[] tmp = new short[length];
                    is.read_short_array(tmp, 0, length);
                    os.write_short_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_ushort:
                {
                    short[] tmp = new short[length];
                    is.read_ushort_array(tmp, 0, length);
                    os.write_ushort_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_long:
                {
                    int[] tmp = new int[length];
                    is.read_long_array(tmp, 0, length);
                    os.write_long_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_ulong:
                {
                    int[] tmp = new int[length];
                    is.read_ulong_array(tmp, 0, length);
                    os.write_ulong_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_float:
                {
                    float[] tmp = new float[length];
                    is.read_float_array(tmp, 0, length);
                    os.write_float_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_double:
                {
                    double[] tmp = new double[length];
                    is.read_double_array(tmp, 0, length);
                    os.write_double_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_boolean:
                {
                    boolean[] tmp = new boolean[length];
                    is.read_boolean_array(tmp, 0, length);
                    os.write_boolean_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_char:
                {
                    char[] tmp = new char[length];
                    is.read_char_array(tmp, 0, length);
                    os.write_char_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_octet:
                {
                    byte[] tmp = new byte[length];
                    is.read_octet_array(tmp, 0, length);
                    os.write_octet_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_any:
                for (int i = 0; i < length; ++i) {
                    os.write_any(is.read_any());
                }
                return;
            case TCKind._tk_TypeCode:
                for (int i = 0; i < length; ++i) {
                    os.write_TypeCode(is.read_TypeCode());
                }
                return;
            case TCKind._tk_Principal:
                for (int i = 0; i < length; ++i) {
                    os.write_Principal(is.read_Principal());
                }
                return;
            case TCKind._tk_objref:
                for (int i = 0; i < length; ++i) {
                    os.write_Object(is.read_Object());
                }
                return;
            case TCKind._tk_enum:
                for (int i = 0; i < length; ++i) {
                    os.write_ulong(is.read_ulong());
                }
                return;
            case TCKind._tk_string:
                for (int i = 0; i < length; ++i) {
                    os.write_string(is.read_string());
                }
                return;
            case TCKind._tk_longlong:
                {
                    long[] tmp = new long[length];
                    is.read_longlong_array(tmp, 0, length);
                    os.write_longlong_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_ulonglong:
                {
                    long[] tmp = new long[length];
                    is.read_ulonglong_array(tmp, 0, length);
                    os.write_ulonglong_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_longdouble:
                throw new NO_IMPLEMENT();
            case TCKind._tk_wchar:
                {
                    char[] tmp = new char[length];
                    is.read_wchar_array(tmp, 0, length);
                    os.write_wchar_array(tmp, 0, length);
                }
                return;
            case TCKind._tk_wstring:
                for (int i = 0; i < length; ++i) {
                    os.write_wstring(is.read_wstring());
                }
                return;
            case TCKind._tk_fixed:
                for (int i = 0; i < length; ++i) {
                    os.write_fixed(is.read_fixed());
                }
                return;
            case TCKind._tk_value:
            case TCKind._tk_value_box:
                for (int i = 0; i < length; ++i) {
                    os.write_value(is.read_value());
                }
                return;
            case TCKind._tk_abstract_interface:
                for (int i = 0; i < length; ++i) {
                    os.write_abstract_interface(is.read_abstract_interface());
                }
                return;
            case TCKind._tk_struct:
            case TCKind._tk_except:
                for (int i = 0; i < length; ++i) {
                    for (int j = 0; j < content.member_count(); ++j) {
                        copy_stream(content.member_type(j), is, os);
                    }
                }
                return;
            case TCKind._tk_union:
            case TCKind._tk_sequence:
            case TCKind._tk_array:
                for (int i = 0; i < length; ++i) {
                    copy_stream(content, is, os);
                }
                return;
            case TCKind._tk_alias:
            case TCKind._tk_native:
            default:
                org.openorb.orb.util.Trace.signalIllegalCondition(null, "Unexpected array content type kind().value()==" + content.kind().value() + ".");
        }
    }
}
