package clubmixer.commons.plugins.annotations;

import clubmixer.commons.plugins.communication.PluginCommunicationRuntime;
import clubmixer.commons.plugins.communication.Registry;
import clubmixer.commons.plugins.communication.RemoteMethodEntry;
import clubmixer.commons.plugins.player.IPlayer;
import clubmixer.commons.plugins.queue.IQueueControl;
import com.slychief.clubmixer.commons.ClubmixerPreferences;
import com.slychief.clubmixer.logging.ClubmixerLogger;
import com.slychief.clubmixer.server.library.ClubmixerServerLibrary;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 *
 * @author Alexander Schindler
 */
public class AnnotationHandler {

    private final Object plugin;

    private String pluginName;

    private Field fieldComChannel;

    private Field fieldPreferences;

    private Field fieldQueueManager;

    private Field fieldServerLibrary;

    private Field fieldPlayer;

    private Field fieldQueuecontrol;

    public AnnotationHandler(Object obj) {
        plugin = obj;
        ClubmixerLogger.debug(this, "inject dependencies for: " + obj.toString());
        getAnnotaions(plugin);
    }

    private void getAnnotaions(Object _ressource) {
        Field[] farr = _ressource.getClass().getDeclaredFields();
        for (Field field : farr) {
            Annotation[] aar = field.getAnnotations();
            for (Annotation a : aar) {
                if (a instanceof QueueManager) {
                    this.fieldQueueManager = field;
                } else if (a instanceof Preferences) {
                    this.fieldPreferences = field;
                } else if (a instanceof CommunicationChannel) {
                    this.fieldComChannel = field;
                    pluginName = ((CommunicationChannel) a).pluginname();
                } else if (a instanceof Player) {
                    this.fieldPlayer = field;
                } else if (a instanceof QueueControl) {
                    this.fieldQueuecontrol = field;
                } else if (a instanceof ServerLibrary) {
                    this.fieldServerLibrary = field;
                }
            }
        }
        Method[] methods = _ressource.getClass().getMethods();
        for (Method method : methods) {
            Annotation[] aar = method.getAnnotations();
            for (Annotation annotation : aar) {
                if (annotation instanceof RemoteMethod) {
                    registerRemoteMethod(_ressource, method, (RemoteMethod) annotation);
                }
            }
        }
    }

    public void injectPreferences(ClubmixerPreferences prefs) {
        if (fieldPreferences == null) {
            return;
        }
        try {
            this.fieldPreferences.setAccessible(true);
            this.fieldPreferences.set(plugin, prefs);
        } catch (IllegalArgumentException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        } catch (IllegalAccessException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        }
    }

    public void injectCommunicationChannel(PluginCommunicationRuntime comm) {
        if (fieldComChannel == null) {
            return;
        }
        try {
            this.fieldComChannel.setAccessible(true);
            this.fieldComChannel.set(plugin, comm.getChannel());
        } catch (IllegalArgumentException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        } catch (IllegalAccessException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        }
    }

    public void injectQueueManager(com.slychief.clubmixer.server.library.management.QueueManager queueManager) {
        if (fieldQueueManager == null) {
            return;
        }
        try {
            this.fieldQueueManager.setAccessible(true);
            this.fieldQueueManager.set(plugin, queueManager);
        } catch (IllegalArgumentException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        } catch (IllegalAccessException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        }
    }

    public void injectServerLibrary(ClubmixerServerLibrary lib) {
        if (fieldServerLibrary == null) {
            return;
        }
        try {
            this.fieldServerLibrary.setAccessible(true);
            this.fieldServerLibrary.set(plugin, lib);
        } catch (IllegalArgumentException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        } catch (IllegalAccessException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        }
    }

    public void injectPlayer(IPlayer p) {
        if (fieldPlayer == null) {
            return;
        }
        try {
            this.fieldPlayer.setAccessible(true);
            this.fieldPlayer.set(plugin, p);
        } catch (IllegalArgumentException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        } catch (IllegalAccessException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        }
    }

    public void injectQueueControl(IQueueControl activeControl) {
        if (fieldQueuecontrol == null) {
            return;
        }
        try {
            this.fieldQueuecontrol.setAccessible(true);
            this.fieldQueuecontrol.set(plugin, activeControl);
        } catch (IllegalArgumentException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        } catch (IllegalAccessException ex) {
            ClubmixerLogger.debugUnexpectedException(this, ex);
        }
    }

    private void registerRemoteMethod(Object ressource, Method method, RemoteMethod annotation) {
        String regName;
        if (annotation.name().equals("")) {
            regName = String.format("%s.%s.%s", pluginName, ressource.toString(), method.getName());
        } else {
            regName = annotation.name();
        }
        RemoteMethodEntry entry = new RemoteMethodEntry(ressource, method);
        Registry.getInstance().put(regName, entry);
        ClubmixerLogger.info(this, "Registered RemoteMethod: " + regName);
    }
}
