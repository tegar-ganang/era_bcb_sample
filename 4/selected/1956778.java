package org.odiem.sdk.mappers;

import java.lang.reflect.Method;
import org.odiem.sdk.annotations.BaseDn;
import org.odiem.sdk.exceptions.OdmException;

public final class BaseDnMapper extends AbstractMapper {

    private BaseDn baseDn;

    public BaseDn getBaseDn() {
        return baseDn;
    }

    public BaseDnMapper(BaseDn baseDn, Method readMethod, Method writeMethod) throws OdmException {
        super(readMethod, writeMethod);
        this.baseDn = baseDn;
    }
}
