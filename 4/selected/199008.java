package net.rptools.chartool.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.rptools.chartool.model.property.AbstractScript;
import net.rptools.chartool.model.property.PropertyDescriptor;
import net.rptools.chartool.model.property.PropertyDescriptorMap;
import net.rptools.chartool.model.property.PropertyDescriptorSet;
import net.rptools.chartool.model.property.PropertyList;
import net.rptools.chartool.model.property.PropertyMap;
import net.rptools.chartool.model.property.Script;
import net.rptools.chartool.model.property.SlotPropertyValue;
import net.rptools.chartool.ui.component.RPIcon;

/**
 * A token file is a zip file that contains MT and CT/IT data. Most of this is completely separate data. 
 * This class handles the shared data.
 *  
 * @author Jay
 */
public class RPTokSupport {

    /**
   * The handler that is used to perform the read & write actions when working with a token.
   */
    private RPTokHandler handler;

    /**
   * Macros are given an index which must be unique. 
   */
    private int nextMacroIndex;

    /**
   * Name of the property that holds the name value. The value is an {@link RPIcon}.
   */
    public static final String MACROS_PROP_NAME = "rpSystemMacros";

    /**
   * Name of the property that holds the name value. The value is an {@link RPIcon}.
   */
    public static final String USER_MACROS_PROP_NAME = "rpUserMacros";

    /**
   * Logger instance for this class.
   */
    private static final Logger LOGGER = Logger.getLogger(RPTokSupport.class.getName());

    /**
   * Create a support with a specific handler.
   * 
   * @param handler The handler for reads and writes.
   */
    public RPTokSupport(RPTokHandler handler) {
        this.handler = handler;
    }

    /**
   * Take a MT/CT property map and copy the property values from one set to the other.
   * 
   * @param character Character being read or written
   * @param reading Are we reading or writing?
   * @param map The mappings of property names from MT to CT. 
   * @param pds The property descriptor set for the character.
   * @return The value <code>true</code> if the XML document was modified.
   */
    public boolean mapTokenProperties(PropertyMap character, boolean reading, Properties map, PropertyDescriptorSet pds) {
        if (map == null) return false;
        boolean changed = false;
        for (String mtProp : map.stringPropertyNames()) {
            String ctProp = map.getProperty(mtProp).trim();
            boolean oneWay = ctProp.startsWith("*");
            if (oneWay && reading) continue;
            if (oneWay) ctProp = ctProp.substring(1);
            PropertyDescriptor pd = pds.contains(ctProp) ? pds.get(ctProp) : null;
            boolean isScript = pd == null;
            if (isScript && reading) continue;
            if (isScript) {
                try {
                    Script script = AbstractScript.createScript(ctProp);
                    if (script == null) {
                        LOGGER.log(Level.WARNING, "The field '" + ctProp + "' has no script defined for it and is ignored.");
                        continue;
                    }
                    Object value = script.execute(character);
                    changed |= handler.write(mtProp, value);
                } catch (RuntimeException e) {
                    LOGGER.log(Level.WARNING, "Exception executing script to get property. The MT property '" + mtProp + "' could not be set:\n" + ctProp, e);
                }
                continue;
            }
            switch(pd.getType()) {
                case MAP:
                    if (!reading && pd.getMapProperties().getDefaultPropertyName() != null) changed |= handler.write(mtProp, character.get(ctProp));
                    break;
                case BOOLEAN:
                case NUMBER:
                case STRING:
                    if (reading) {
                        String value = handler.read(mtProp);
                        if (value == null || value.trim().length() == 0) break;
                        try {
                            character.put(ctProp, pd.getType().propertyFromString(value, pd.getDefaultValue()));
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Property value of '" + mtProp + "' in token file not a valid string: " + value, e);
                        }
                    } else {
                        changed |= handler.write(mtProp, character.get(ctProp));
                    }
                    break;
                case SCRIPT:
                    if (!reading) changed |= handler.write(mtProp, character.get(ctProp));
                    break;
                case SLOT:
                    if (!reading && pd.getMapProperties() != null && pd.getMapProperties().getDefaultPropertyName() != null) changed |= handler.write(mtProp, character.get(ctProp));
                    break;
                case IMAGE:
                case LIST:
                    break;
                default:
                    assert false : "Unknown property type: " + ctProp + "=" + pd.getType().toString();
            }
        }
        return changed;
    }

    /**
   * Read the macros from the handler, split them into system and user macros, then read/write them as needed.
   * Macros are stored in {@link SlotPropertyValue}'s on CT/IT side. On the MT side they are stored as maps of strings.
   * To support backwards compatibility there is a mapping from <code>name</code> to <code>index</code> and 
   * <code>macroText</code> to <code>command</code> from CT/IT to MT.
   * 
   * @param character The character being updated
   * @param reading Reading or writing the MT data 
   * @return The value <code>true</code> if the XML has been changed.
   */
    public boolean updateMacros(PropertyMap character, boolean reading) {
        boolean change = false;
        PropertyList macros = (PropertyList) character.get(MACROS_PROP_NAME);
        Set<String> keys = handler.getMacroNames();
        if (macros != null && !macros.isEmpty() && !reading) {
            Iterator<Object> i = macros.iterator();
            while (i.hasNext()) {
                PropertyMap macro = (PropertyMap) i.next();
                Map<String, String> macroOut = toMacroOut(macro, nextMacroIndex++);
                change |= handler.writeMacro(macroOut.get("index"), macroOut);
                keys.remove(macroOut.get("index"));
            }
        }
        PropertyList userMacros = (PropertyList) character.get(USER_MACROS_PROP_NAME);
        if (userMacros == null) return change;
        if (reading) {
            for (String key : keys) {
                Map<String, String> userMacro = handler.readMacro(key);
                if (macros != null) {
                    Iterator<Object> i = macros.iterator();
                    while (i.hasNext()) {
                        PropertyMap macro = (PropertyMap) i.next();
                        if (userMacro.get("label").equals(macro.get("name"))) {
                            String systemMacro = (String) macro.get("macroText");
                            String userCommand = userMacro.get("command");
                            userCommand = userCommand == null ? null : userCommand.trim();
                            if (systemMacro != null && systemMacro.trim().equals(userCommand)) userMacro = null;
                            break;
                        }
                    }
                }
                if (userMacro == null) continue;
                if (userMacros == null) {
                    userMacros = new PropertyList();
                    character.put(USER_MACROS_PROP_NAME, userMacros);
                }
                userMacros.add(toSlotPropertyValue(userMacro, null));
            }
        } else {
            for (Object oMacro : userMacros) {
                SlotPropertyValue macro = (SlotPropertyValue) oMacro;
                Map<String, String> macroOut = toMacroOut(macro, nextMacroIndex++);
                change |= handler.writeMacro(macroOut.get("index"), macroOut);
                keys.remove(macro.get("name"));
            }
            for (String key : keys) change |= handler.writeMacro(key, null);
        }
        return change;
    }

    /**
   * Convert a property map into an output macro format. Reset all of the index values when writing.
   * 
   * @param macro Property map describing the macro.
   * @param nextMacroIndex The index for this macro. Set to -1 if no index should be set.
   * @return A map with all values converted to strings.
   */
    public static Map<String, String> toMacroOut(PropertyMap macro, int nextMacroIndex) {
        Map<String, String> macroOut = new HashMap<String, String>();
        for (String key : macro.keySet()) {
            if (key.equals("macroSetName")) continue;
            String macroKey = key;
            if (key.equals("macroText")) macroKey = "command";
            if (key.equals("name")) macroKey = "label";
            Object o = macro.get(key);
            if (o != null) macroOut.put(macroKey, o.toString().trim());
        }
        if (nextMacroIndex >= 0) macroOut.put("index", Integer.toString(nextMacroIndex));
        macroOut.put("saveLocation", "Token");
        macroOut.put("commonMacro", "false");
        return macroOut;
    }

    /**
   * Convert a macro map to a {@link SlotPropertyValue}. Convert values from strings to their property
   * type. Ignore values that are not saved in the slot value.
   * 
   * @param macroOut The macro map being converted.
   * @param spv Optional slot property value to be edited.
   * @return The slot property value for a macro in this system.
   */
    public static SlotPropertyValue toSlotPropertyValue(Map<String, String> macroOut, SlotPropertyValue spv) {
        if (spv == null) spv = new SlotPropertyValue("macro");
        PropertyDescriptorMap pds = spv.getPropertyDescriptors();
        for (String key : spv.keySet()) {
            String macroKey = key;
            if (key.equals("macroText")) macroKey = "command";
            if (key.equals("name")) macroKey = "label";
            PropertyDescriptor pd = pds.get(key);
            if (pd != null && pd.isSlotModifiable()) spv.put(key, pd.getType().propertyFromString(macroOut.get(macroKey), pd.getDefaultValue()));
        }
        return spv;
    }

    /**
   * This interface is used to supply the read and write handlers for an instance of {@link RPTokSupport}.
   * 
   * @author Jay
   */
    public interface RPTokHandler {

        /**
     * The names of all MT property values
     */
        public static final String[] MT_PROP_NAMES = { "saveLocation", "index", "colorKey", "hotKey", "command", "label", "group", "sortby", "autoExecute", "includeLabel", "applyToTokens", "fontColorKey", "fontSize", "minWidth", "maxWidth", "allowPlayerEdits", "toolTip", "commonMacro", "compareGroup", "compareSortPrefix", "compareCommand", "compareIncludeLabel", "compareAutoExecute", "compareApplyToSelectedTokens" };

        /**
     * Read an MT property value.
     * 
     * @param mtProp The MT property being read.
     * @return The value of the MT property.
     */
        String read(String mtProp);

        /**
     * Write a MT property from a CT property value.
     * 
     * @param mtProp The MT property being written.
     * @param ctPropValue The CT property value.
     * @return The value <code>true</code> if the MT property was modified.
     */
        boolean write(String mtProp, Object ctPropValue);

        /**
     * Get the macro names from MT. 
     * 
     * @return A set containing all of the macro key names. <em>NOTE:</em>It must be editable
     */
        Set<String> getMacroNames();

        /**
     * Read an MT macro.
     * 
     * @param macroName The name of the MT macro being read.
     * @return The MT macro.
     */
        Map<String, String> readMacro(String macroName);

        /**
     * Write a MT property from a CT property value.
     * 
     * @param macroName The name of the MT macro being written.
     * @param macro The macro being written. If this value is <code>null</code> the macro should be removed.
     * @return The value <code>true</code> if the MT macro was modified.
     */
        boolean writeMacro(String macroName, Map<String, String> macro);
    }
}
