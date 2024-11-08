package nz.org.venice.prefs.settings;

import javax.swing.JDesktopPane;
import java.util.*;
import nz.org.venice.main.Module;
import nz.org.venice.alert.AlertModule;
import nz.org.venice.alert.AlertManager;
import nz.org.venice.alert.AlertWriter;
import nz.org.venice.alert.AlertReader;
import nz.org.venice.alert.AlertException;
import nz.org.venice.prefs.PreferencesModule;
import nz.org.venice.prefs.PreferencesManager;
import nz.org.venice.prefs.PreferencesException;
import nz.org.venice.prefs.settings.SettingsWriter;

public class AlertModuleSettings extends AbstractSettings {

    List symbols;

    public AlertModuleSettings() {
        super(Settings.ALERTS, Settings.MODULE);
    }

    public AlertModuleSettings(List symbols) {
        super(Settings.ALERTS, Settings.MODULE);
        this.symbols = symbols;
    }

    public void setSymbols(List symbols) {
        this.symbols = symbols;
    }

    public Module getModule(JDesktopPane desktop) {
        AlertReader reader = AlertManager.getReader();
        AlertWriter writer = AlertManager.getWriter();
        Module alertModule = null;
        try {
            if (symbols == null) {
                alertModule = new AlertModule(desktop, reader, writer);
            } else {
                alertModule = new AlertModule(desktop, symbols, reader, writer);
            }
        } catch (AlertException e) {
        } finally {
            return alertModule;
        }
    }
}
