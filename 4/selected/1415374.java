package net.pyxzl.rob.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import net.pyxzl.rob.chassis.logger.Log;
import net.pyxzl.rob.chassis.players.PlayerSetting;

public class Fighter extends GameObject implements Cloneable {

    private static final int keyHistoryMax = 10;

    private static final int moveHistoryMax = 40;

    private static final String defaultName = "Ghost Fighter";

    List<Move> moves = new LinkedList<Move>();

    List<PlayerSetting> keyHistory = new LinkedList<PlayerSetting>();

    List<Move> moveHistory = new LinkedList<Move>();

    /**
	 * 
	 * @author docwex
	 */
    public Fighter() {
    }

    /**
	 * Load a configuration from a given file
	 * @param config The File to load from
	 * @author docwex
	 */
    public void load(final File config) {
        try {
            final FileInputStream fis = new FileInputStream(config);
            final Properties props = new Properties();
            props.loadFromXML(fis);
            this.name = props.getProperty("name", Fighter.defaultName);
            for (final Object key : props.keySet()) if (key instanceof Move) this.moves.add((Move) key);
        } catch (final IOException e) {
            Log.getSingleton().write("#020 Couldn't read file to load fighter: " + config.getPath());
            Log.getSingleton().write(" -> using fall back settings");
            this.name = "Fall Back";
        }
    }

    /**
	 * @author docwex
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            Log.getSingleton().write("#021 Could not clone Fighter " + this.name);
            throw new InternalError(e.toString());
        }
    }

    /**
	 * 
	 * @author docwex
	 */
    public void act(final PlayerSetting trigger) {
        this.keyHistory.add(trigger);
        if (this.keyHistory.size() > Fighter.keyHistoryMax) this.keyHistory = this.keyHistory.subList(0, Fighter.keyHistoryMax - 1);
        for (final Move move : this.moves) {
            if (move.checkMove(this.keyHistory)) {
                this.moveHistory.add(move);
                if (this.moveHistory.size() > Fighter.moveHistoryMax) this.moveHistory = this.moveHistory.subList(0, Fighter.moveHistoryMax - 1);
                move.act(null);
                return;
            }
        }
        switch(trigger) {
            case UP:
                break;
            case DOWN:
                break;
            case LEFT:
                break;
            case RIGHT:
                break;
        }
    }

    /**
	 * This function is called by the other fighter or a special move.
	 * @param object Object to which to react to
	 * @param special Is the attack a special move?
	 * @author docwex
	 */
    public void react(final GameObject object, final boolean special) {
    }
}
