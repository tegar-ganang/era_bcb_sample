package org.openmobster.core.synchronizer.server.engine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.apache.commons.beanutils.BeanUtils;
import org.openmobster.cloud.api.sync.MobileBean;
import org.openmobster.cloud.api.sync.MobileBeanId;
import org.openmobster.core.synchronizer.server.SyncContext;

/**
 * 
 * @author openmobster@gmail.com
 */
public class Tools {

    public static String getOid(MobileBean record) {
        try {
            String id = "";
            Class recordClazz = record.getClass();
            Field[] declaredFields = recordClazz.getDeclaredFields();
            for (Field field : declaredFields) {
                Annotation[] annotations = field.getAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation instanceof MobileBeanId) {
                        return BeanUtils.getProperty(record, field.getName());
                    }
                }
            }
            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDeviceId() {
        return SyncContext.getInstance().getDeviceId();
    }

    public static String getChannel() {
        return SyncContext.getInstance().getServerSource();
    }
}
