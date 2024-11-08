package org.bluesock.bluemud.driver;

import org.bluesock.bluemud.lib.MudObject;
import org.python.util.PythonInterpreter;
import org.python.core.PyException;
import java.util.HashMap;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.List;
import java.util.Arrays;

/**
 * The ObjectFactory is responsible for the creation of all in-game
 * objects. Both shared and non-shared objects are supported (by
 * default, created objects are not shared). If an object is shared,
 * only one instance of that object will be created; that instance
 * will always be returned. Note that all objects loaded through
 * this factory MUST ultimately be derived from 
 * org.bluesock.bluemud.lib.MudObject.
 *
 * The factory also supports the concept of aliased objects. These 
 * objects are given a special "alias" by which they can be referred,
 * independent of their actual Python names. Aliased objects are 
 * normally used as bridges between standard and project-specific code
 * (for example, when referring to the initial room that a player 
 * should be moved to upon login).
 *
 * Finally, the factory supports the notion of grouped object instances.
 * Through the addGroupedInstance method, MudObject instances can be
 * classified under named groups. Instances within the group can later
 * be retrieved with the getGroupedInstances method. Instances can be
 * removed from a named group with the removeGroupedInstance method.
 */
public class ObjectFactory {

    private HashMap sharedObjects = new HashMap();

    private HashMap aliasedPaths = new HashMap();

    private HashMap groupedInstances = new HashMap();

    private WeakHashMap allObjects = new WeakHashMap();

    /**
   * Get an instance of a world class. This version of the
   * method only supports non-shared objects.
   *
   * @param dottedPath - The dotted path of the class. For
   *                     example: mudlib.core.Object
   *
   * @return A new instance of the Python class denoted
   *         by the dottedPath, or null if an instance
   *         could not be created.
   */
    MudObject getObject(String dottedPath) {
        return getObject(dottedPath, false, false);
    }

    /**
   * Get an instance of a world class. If shared, the
   * same instance will be returned for all invocations
   * of this method. If not shared, a new instance will
   * be created for each invocation.
   *
   * @param dottedPath - The dotted path of the class. For
   *                     example: mudlib.core.Room
   *
   * @param shared - true if this object should be shared,
   *                 false otherwise
   *
   * @return A new instance of the Python class denoted
   *         by the dottedPath, or null if an instance
   *         could not be created.
   */
    MudObject getObject(String dottedPath, boolean shared) {
        return getObject(dottedPath, shared, false);
    }

    /**
   * Get an instance of a world class. If shared, the
   * same instance will be returned for all invocations
   * of this method. If not shared, a new instance will
   * be created for each invocation.
   *
   * @param dottedPath - The dotted path of the class. For
   *                     example: mudlib.core.Room
   *
   * @param shared - true if this object should be shared,
   *                 false otherwise
   *
   * @param reload - true if the Python module housing the
   *                 given class should first be reloaded
   *                 prior to construction of an instance. If
   *                 the class is shared, a new instance will
   *                 be constructed that will replace the old
   *                 shared instance. False otherwise.
   *
   * @return A new instance of the Python class denoted
   *         by the dottedPath, or null if an instance
   *         could not be created.
   */
    MudObject getObject(String dottedPath, boolean shared, boolean reload) {
        if (dottedPath == null || dottedPath.length() == 0) {
            return null;
        }
        int paren = dottedPath.indexOf('(');
        if (paren >= 0) {
            dottedPath = dottedPath.substring(0, paren);
        }
        if (shared) {
            if (!reload && sharedObjects.containsKey(dottedPath)) {
                return (MudObject) sharedObjects.get(dottedPath);
            } else {
                MudObject loadedObject = loadObject(dottedPath, reload);
                if (loadedObject != null) {
                    sharedObjects.put(dottedPath, loadedObject);
                }
                return loadedObject;
            }
        } else {
            return loadObject(dottedPath, reload);
        }
    }

    /**
   * Get a reference to a world class by way of it's object
   * identifier.
   *
   * @param objectIdentifier - the OID of the world object in question
   *
   * @returns The MudObject referenced by the given OID, or null if
   *          none exist or have been garbage collected.
   */
    MudObject getObject(OID objectIdentifier) {
        return (MudObject) ((WeakReference) allObjects.get(objectIdentifier)).get();
    }

    /**
   * Determines if a loaded instance of the given path is
   * currently being shared.
   *
   * @param dottedPath - The dotted path of the class. For example,
   *                     mudlib.core.Room
   *
   * @returns true if a loaded instance of the given class is
   *          currently being shared, false otherwise.
   */
    boolean hasSharedInstance(String dottedPath) {
        if (dottedPath == null || dottedPath.length() == 0) {
            return false;
        }
        int paren = dottedPath.indexOf('(');
        if (paren >= 0) {
            dottedPath = dottedPath.substring(0, paren);
        }
        return sharedObjects.containsKey(dottedPath);
    }

    /**
   * Get an instance of an aliased world class. If shared,
   * the same instance will be returned for all invocations
   * of this method. If not shared, a new instance will
   * be created for each invocation.
   *
   * @param alias - The alias name of the object.
   *
   * @param shared - true if this object should be shared,
   *                 false otherwise
   *
   * @return A new instance of the Python class aliased
   *         by the alias, or null if an instance
   *         could not be created.
   */
    MudObject getAliasedObject(String alias, boolean shared) {
        if (aliasedPaths.containsKey(alias)) {
            return getObject((String) aliasedPaths.get(alias), shared);
        } else {
            return null;
        }
    }

    /**
   * Classify a named MudObject under the given group. MudObjects
   * in the gruop can later be retrieved through the getGroupedInstances
   * method, or removed from control through the removeNamedInstance
   * method.
   *
   * @param groupName - Group name under which the MudObject should be
   *                    classified.
   * @param instance - The instance of the MudObject to group.
   */
    void addGroupedInstance(String groupName, MudObject instance) {
        MudObject[] instances = (MudObject[]) groupedInstances.get(groupName);
        MudObject[] expandedInstances;
        if (instances == null) {
            expandedInstances = new MudObject[1];
            expandedInstances[0] = instance;
        } else {
            expandedInstances = new MudObject[instances.length + 1];
            for (int i = 0; i < instances.length; i++) {
                expandedInstances[i] = instances[i];
            }
            expandedInstances[instances.length] = instance;
        }
        groupedInstances.put(groupName, expandedInstances);
    }

    /**
   * Returns an array of MudObjects that were previously classified
   * under the given group through a call to addGroupedInstance.
   *
   * @param groupName - The name of the group under which the desired
   *                    MudObjects are classified. This must match a
   *                    group name previously passed to addGroupedInstance.
   *
   * @returns An array of MudObjects that were previously classified under
   *          the given group name, or null if none exist.
   */
    MudObject[] getGroupedInstances(String groupName) {
        return (MudObject[]) groupedInstances.get(groupName);
    }

    /**
   * Removes the given MudObject from indicated group.
   *
   * @param groupName - The name of the group from which the given
   *                    MudObject should be removed. This must match
   *                    a name previously passed to addGroupedInstance.
   * @param instance - The MudObject to remove from the group.
   */
    void removeGroupedInstance(String groupName, MudObject instance) {
        MudObject[] instances = (MudObject[]) groupedInstances.get(groupName);
        if (instances != null) {
            if (instances.length == 1) {
                if (instances[0].equals(instance)) {
                    groupedInstances.remove(groupName);
                }
            } else {
                int removalIndex = -1;
                for (int i = 0; i < instances.length; i++) {
                    if (instances[i].equals(instance)) {
                        removalIndex = i;
                        break;
                    }
                }
                if (removalIndex >= 0) {
                    MudObject[] trimmedInstances = new MudObject[instances.length - 1];
                    for (int i = 0; i < removalIndex; i++) {
                        trimmedInstances[i] = instances[i];
                    }
                    for (int i = removalIndex; i < trimmedInstances.length; i++) {
                        trimmedInstances[i] = instances[i + 1];
                    }
                    groupedInstances.put(groupName, trimmedInstances);
                }
            }
        }
    }

    /**
   * Registers an alias with this factory, associating it
   * with the given dottedPath. Objects pointed to by the
   * dottedPath can be loaded by the alias through the
   * getAliasedObject() method.
   *
   * @param alias - The alias to register
   * @param dottedPath - The dotted path to the object being
   *                     aliased. Example: world.sample.StartRoom
   */
    void registerAlias(String alias, String dottedPath) {
        if (alias != null && alias.length() > 0) {
            aliasedPaths.put(alias, dottedPath);
        }
    }

    /**
   * loadObject is responsible for the actual creation
   * of a Python object, making use of the Python interpreter.
   *
   * @param dottedPath - The dotted path of the class, as
   *                     explained for the getObject method.
   *
   * @returns The Python object (in the form of a MudObject).
   */
    private MudObject loadObject(String dottedPath, boolean reload) {
        PythonInterpreter interpreter = Driver.getInterpreter();
        int lastDot = dottedPath.lastIndexOf('.');
        if (lastDot < 0) {
            return null;
        }
        String moduleName = dottedPath.substring(0, lastDot);
        try {
            interpreter.exec("import " + moduleName);
            if (reload) {
                interpreter.exec("import sys");
                interpreter.exec("reload(sys.modules[\"" + moduleName + "\"])");
            }
            interpreter.exec("__BLUEMUD_OBJECT = " + dottedPath + "()");
            MudObject mo = (MudObject) interpreter.get("__BLUEMUD_OBJECT", Class.forName("org.bluesock.bluemud.lib.MudObject"));
            if (mo != null) {
                allObjects.put(mo.getObjectID(), new WeakReference(mo));
            }
            return mo;
        } catch (PyException e) {
            System.err.println("Could not load world object: " + dottedPath);
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
   * Removes the given object from the map of all loaded objects
   * and from the map of shared objects, if necessary. This method
   * does NOT remove the object from any instance groupings--that
   * must be done explicitly through the removeGroupedInstance
   * method.
   */
    void removeObject(MudObject o) {
        allObjects.remove(o.getObjectID());
        sharedObjects.values().remove(o);
    }
}
