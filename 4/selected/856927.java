package pl.pyrkon.dsgen.server;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.widgets.form.validator.Validator;

/**
 * A class that holds methods that allow for getting its fields for creation of
 * data source.
 * 
 * @author Piotr Berłowski
 */
public class EntityAnalyzer {

    /**
	 * Analyzes the entity (Java Bean) and provides the descriptions of all the
	 * fields that are eligible to be used as data source fields.
	 * 
	 * @param entityType
	 *            The class of the entity.
	 * @return list of field descriptions.
	 * @throws SecurityException
	 *             if PropertyUtils fail.
	 */
    public static Map<String, FieldDescription> analyzeEntity(Class<?> entityType) throws SecurityException {
        Map<String, FieldDescription> descriptions = new HashMap<String, FieldDescription>();
        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(entityType);
        for (PropertyDescriptor desc : descriptors) {
            try {
                Field field = entityType.getDeclaredField(desc.getName());
                DataAccessMatcher dam = DataAccessMatcher.getInstance();
                Class<?> fieldType = field.getType();
                Class<? extends DataSourceField> dsf = dam.matchField(fieldType);
                String recordAccessor = dam.matchRecordMethod(fieldType);
                if (dsf != null) {
                    String name = desc.getName();
                    String readMethod = desc.getReadMethod().getName();
                    String writeMethod = desc.getWriteMethod().getName();
                    Class<? extends Validator> validator = dam.matchValidator(fieldType);
                    descriptions.put(name, new FieldDescription(name, readMethod, writeMethod, dsf, recordAccessor, validator, true));
                }
            } catch (NoSuchFieldException e) {
            }
        }
        return descriptions;
    }
}
