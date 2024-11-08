package play.modules.jqvalidate;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import play.Logger;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.data.validation.Email;
import play.data.validation.Equals;
import play.data.validation.InFuture;
import play.data.validation.InPast;
import play.data.validation.Match;
import play.data.validation.Max;
import play.data.validation.MaxSize;
import play.data.validation.Min;
import play.data.validation.MinSize;
import play.data.validation.Range;
import play.data.validation.Required;
import play.data.validation.URL;
import play.modules.jqvalidate.singleton.MapSingleton;

/**
 * PLAY! Plugin to do some logic on startup
 * 
 * @author Ahmed
 * 
 */
public class StartUp extends PlayPlugin {

    /**
	 * Called at application start (and at each reloading) Time to analyze the
	 * models and update the map containing the fields and their validations
	 */
    @Override
    public void onApplicationStart() {
        Logger.info("Jqvalidation startup plugin Started");
        if (Play.mode == Mode.DEV) {
            MapSingleton.setClassFieldValidation(null);
        }
        if (MapSingleton.getClassFieldValidation() != null) {
            return;
        }
        Map<String, Map<String, String>> classFieldValidation = new HashMap<String, Map<String, String>>();
        try {
            RandomAccessFile validationRulesFile = prepareValidationEngineRules();
            @SuppressWarnings("rawtypes") List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);
            classes.addAll(getSienaModels());
            Logger.info("Siena Classes are %s", getSienaModels());
            for (Class<?> c : classes) {
                Map<String, String> fieldsValidation = getClassFields(c, validationRulesFile);
                if (!fieldsValidation.isEmpty()) {
                    classFieldValidation.put(c.getSimpleName(), fieldsValidation);
                }
            }
            String jsEnd = "\n};}};$.validationEngineLanguage.newLang();})(jQuery);";
            validationRulesFile.writeBytes(jsEnd);
            validationRulesFile.getChannel().truncate(validationRulesFile.getChannel().position());
            validationRulesFile.close();
        } catch (ClassNotFoundException e) {
            Logger.error(e, "");
        } catch (IOException e) {
            e.printStackTrace();
            Logger.error(e, "");
        } catch (NullPointerException e) {
            e.printStackTrace();
            Logger.error(e, "");
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e, "");
        }
        MapSingleton.setClassFieldValidation(classFieldValidation);
        Logger.info("Jqvalidation startup plugin Finished\n%s", MapSingleton.getClassFieldValidation());
    }

    /**
	 * Scans the fields in the given Class {@code c} which has validation
	 * annotations {@code @play.data.validation.*} and map each field using its
	 * name as a {@code key} to its validation string which is built depending
	 * on the annotations.
	 * 
	 * @param c
	 *            the Class whose Fields will be scanned.
	 * @param validationRulesFile
	 * @return map mapping each field Name as a Key to its validation String.
	 *         NOTE: The returned map will not contain the fields who are not
	 *         annotated with {@code @play.data.validation.*}
	 */
    private static Map<String, String> getClassFields(Class<?> c, RandomAccessFile validationRulesFile) {
        Map<String, String> fieldValidation = new HashMap<String, String>();
        Field[] fields = c.getFields();
        for (Field field : fields) {
            String validationString = getFieldValidation(c.getSimpleName(), field, validationRulesFile);
            if (!validationString.isEmpty()) {
                fieldValidation.put(field.getName(), validationString);
            }
        }
        return fieldValidation;
    }

    /**
	 * Generates Validation for the given Field {@code field}.
	 * 
	 * @param field
	 *            is the field to generate validation for.
	 * @param validationRulesFile
	 * @return String for validation engine to validate this {@code field}.
	 */
    private static String getFieldValidation(String className, Field field, RandomAccessFile validationRulesFile) {
        InFuture inFuture = field.getAnnotation(InFuture.class);
        Equals equals = field.getAnnotation(Equals.class);
        InPast inPast = field.getAnnotation(InPast.class);
        Match match = field.getAnnotation(Match.class);
        Max max = field.getAnnotation(Max.class);
        MaxSize maxSize = field.getAnnotation(MaxSize.class);
        Min min = field.getAnnotation(Min.class);
        MinSize minSize = field.getAnnotation(MinSize.class);
        Required required = field.getAnnotation(Required.class);
        URL url = field.getAnnotation(URL.class);
        Email email = field.getAnnotation(Email.class);
        Range range = field.getAnnotation(Range.class);
        StringBuffer validation = new StringBuffer();
        if (inFuture != null) {
            validation.append("future[");
            if (inFuture.value().trim().isEmpty()) {
                validation.append("NOW");
            } else {
                validation.append(inFuture.value().trim());
            }
            validation.append("],");
            addErrorMessage(validation, inFuture.message());
        }
        if (equals != null && !equals.value().trim().isEmpty()) {
            validation.append(",equals[");
            validation.append(equals.value());
            validation.append("],");
            addErrorMessage(validation, equals.message());
        }
        if (inPast != null) {
            validation.append(",past[");
            if (inPast.value().trim().isEmpty()) {
                validation.append("NOW");
            } else {
                validation.append(inPast.value().trim());
            }
            validation.append("],");
            addErrorMessage(validation, inPast.message());
        }
        if (match != null && !match.value().isEmpty()) {
            addValidationRule(className, field.getName(), validationRulesFile, match.value(), match.message());
            validation.append(",custom[");
            validation.append(className);
            validation.append(".");
            validation.append(field.getName());
            validation.append("]");
        }
        if (max != null) {
            validation.append(",max[");
            validation.append(max.value());
            validation.append("],");
            addErrorMessage(validation, max.message());
        }
        if (maxSize != null) {
            validation.append(",maxSize[");
            validation.append(maxSize.value());
            validation.append("],");
            addErrorMessage(validation, maxSize.message());
        }
        if (min != null) {
            validation.append(",min[");
            validation.append(min.value());
            validation.append("],");
            addErrorMessage(validation, min.message());
        }
        if (minSize != null) {
            validation.append(",minSize[");
            validation.append(minSize.value());
            validation.append("],");
            addErrorMessage(validation, minSize.message());
        }
        if (required != null) {
            validation.append(",required,");
            addErrorMessage(validation, required.message());
        }
        if (url != null) {
            validation.append(",custom[url],");
            addErrorMessage(validation, url.message());
        }
        if (email != null) {
            validation.append(",custom[email],");
            addErrorMessage(validation, email.message());
        }
        if (range != null) {
            validation.append(",range[");
            validation.append(range.min());
            validation.append(",");
            validation.append(range.max());
            validation.append("],");
            addErrorMessage(validation, range.message());
        }
        return validation.toString();
    }

    private static void addErrorMessage(StringBuffer validation, String errorMsg) {
        if (!errorMsg.trim().isEmpty()) {
            validation.append("msg:");
            validation.append(errorMsg);
        }
    }

    private static void addValidationRule(String className, String fieldName, RandomAccessFile validationRulesFile, String regex, String msg) {
        final String RULE_TEMPLATE = ",\n" + "                \"%s.%s\": {\n" + "                    \"regex\":/%s/,\n" + "                    \"alertText\":\"*%s\"" + "                \n}";
        String rule = null;
        try {
            rule = String.format(RULE_TEMPLATE, className, fieldName, regex, msg);
            validationRulesFile.writeBytes(rule);
        } catch (IOException e) {
            try {
                validationRulesFile.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } catch (Exception e) {
            Logger.warn("can't write to validation file");
        }
    }

    private static RandomAccessFile prepareValidationEngineRules() throws Exception {
        Logger.info("Started to read the validation rules file");
        File file = new File(Play.modules.get("jqvalidation").getRealFile().getAbsolutePath(), "public/javascripts/jquery.validationEngine-en.js");
        RandomAccessFile validationRules = null;
        try {
            validationRules = new RandomAccessFile(file, "rwd");
            String line = "";
            while ((line = validationRules.readLine()) != null) {
                if (line.endsWith("}")) {
                    break;
                }
            }
            Logger.info("Created a Pointer to the append after the last pre-defined rule");
        } catch (IOException e) {
            try {
                if (validationRules != null) {
                    validationRules.close();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
                Logger.error(e1, "");
            }
            e.printStackTrace();
            Logger.error(e, "");
            return null;
        } catch (Exception e) {
            Logger.warn(e, "");
            return null;
        }
        return validationRules;
    }

    public List<Class<siena.Model>> getSienaModels() {
        ArrayList<Class<siena.Model>> classes = new ArrayList<Class<siena.Model>>();
        List<Class> allClasses = Play.classloader.getAllClasses();
        for (Class c : allClasses) {
            if (isSienaModel(c)) {
                classes.add(c);
            }
        }
        return classes;
    }

    public boolean isSienaModel(Class c) {
        Class superClass = c.getSuperclass();
        while (superClass != null) {
            if (superClass.equals(siena.Model.class)) {
                return true;
            } else if (superClass.equals(java.lang.Object.class)) {
                return false;
            }
            superClass = superClass.getSuperclass();
        }
        return false;
    }
}

class FilesFilter implements FilenameFilter {

    private String endsWith;

    public FilesFilter(String endsWith) {
        this.endsWith = endsWith;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.endsWith(endsWith);
    }
}
