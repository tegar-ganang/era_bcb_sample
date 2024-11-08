package net.pyxzl.rob.chassis.settings;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import net.java.games.input.Component.Identifier.Button;
import net.java.games.input.Component.Identifier.Key;
import net.pyxzl.rob.chassis.logger.Log;
import net.pyxzl.rob.input.Action;

public final class Settings {

    private static Settings instance = null;

    private Properties props = new Properties();

    private FileOutputStream out;

    private static final String propFile = "Settings";

    /**
	 * Returns the instance of the singleton
	 * @return the singleton
	 * @author docwex
	 */
    public static synchronized Settings getSingleton() {
        if (Settings.instance == null) Settings.instance = new Settings();
        return Settings.instance;
    }

    /**
	 * Prevents cloning of the singleton, so we really only have one
	 * @author docwex
	 */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
	 * 
	 * @author docwex
	 */
    private Settings() {
        this.restore();
    }

    /**
	 * Save the settings from the Properties class to a file
	 * @author docwex
	 */
    public void store() {
        try {
            this.out = new FileOutputStream(Settings.propFile);
            this.props.store(this.out, "-=|Rush of Blood|=- Settings");
        } catch (final IOException e) {
            Log.getSingleton().write("#016 Couldn't save settings and preferences to disk");
            e.getMessage();
            e.printStackTrace();
        }
    }

    /**
	 * Load the current settings from a file
	 * @author docwex
	 */
    public void restore() {
        try {
            this.props = SettingsLoader.getSingleton().restore(Settings.propFile);
        } catch (IOException e) {
            this.setDefaults();
            Log.getSingleton().write("#017 There was an error reading the settings from disk - Falling back to defaults");
            e.printStackTrace();
        }
    }

    /**
	 * Set an option to a certain setting
	 * @param setting The Setting which is to be set
	 * @param value The value this setting should get
	 * @author docwex
	 */
    public void setSetting(final Setting setting, final Object value) {
        this.props.put(setting.toString(), value.toString());
    }

    /**
	 * Retrieve the setting of an option
	 * @param setting The setting which to get
	 * @return The value of that setting
	 * @author docwex
	 */
    public String getSetting(final Setting setting) {
        return (String) this.props.get(setting.toString());
    }

    /**
	 * Load a set of default settings, when no settings from file have been loaded
	 * @author docwex
	 */
    private void setDefaults() {
        this.setSetting(GlobalSetting.VERSION, Version.V0_1);
        this.setSetting(GlobalSetting.FULLSCREEN, false);
        this.setSetting(GlobalSetting.RESOLUTION, "_800x600");
        this.setSetting(GlobalSetting.MUSIC, true);
        this.setSetting(GlobalSetting.SOUND, true);
        this.setSetting(GlobalSetting.SCREENCOLORS, 32);
        this.setSetting(GlobalSetting.TIMELIMIT, 100);
        this.setSetting(GlobalSetting.DIFFICULTY, Difficulty.MEDIUM);
        this.setSetting(GlobalSetting.FIGHTROUNDS, Rounds.BEST_OF_3);
        this.setSetting(GlobalSetting.LOG_CONSOLE, true);
        this.setSetting(GlobalSetting.LOG_FILE, false);
        this.setSetting(GlobalSetting.LOG_SCREEN, true);
        this.setSetting(Action.MENU_UP, Key.UP);
        this.setSetting(Action.MENU_DOWN, Key.DOWN);
        this.setSetting(Action.MENU_LEFT, Key.LEFT);
        this.setSetting(Action.MENU_RIGHT, Key.RIGHT);
        this.setSetting(Action.MENU_SELECT, Key.RETURN);
        this.setSetting(Action.MENU_CHANGE, Key.SPACE);
        this.setSetting(Action.ALT_MENU_SELECT, Button._0);
        this.setSetting(Action.ALT_MENU_CHANGE, Button._1);
        this.store();
    }
}
