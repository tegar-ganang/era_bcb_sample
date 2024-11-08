package org.amhm.core.constants;

import java.util.HashMap;
import java.util.Map;

public final class ErrorsTracer {

    private static ErrorsTracer instance;

    private Map<AmhmErrors, String> mapping;

    static {
        instance = new ErrorsTracer();
    }

    private ErrorsTracer() {
        mapping = new HashMap<AmhmErrors, String>();
        mapping.put(AmhmErrors.FILENOTFOUND, "File has not been found");
        mapping.put(AmhmErrors.MARSHALLINGFAILED, "An error occured while trying to marshall file.");
        mapping.put(AmhmErrors.IOException, "An error occured while trying to read/write file.");
    }

    public static String Trace(AmhmErrors en, Exception e) {
        String str = instance.mapping.get(en);
        if (str == null) {
            return "";
        } else {
            return "Error " + en + " : " + str;
        }
    }
}
