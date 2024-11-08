package source;

import java.awt.*;
import java.util.*;
import source.view.*;

/**
 * This is the Structure that does all Unit production and healing of Units.
 * Must have a Planet located on the Tile to build the Base. Gets all resources
 * from the Planet the Base resides on.
 * @author Sean Larson
 *
 */
public class Base extends Entity {

    private final int healFactor = 50;

    private final int oreExtractionRate = 999;

    protected UnitType building;

    private StructureType structure;

    private final Planet planet;

    private int mpLeft;

    protected StructureType sType;

    private Vector<Unit> healingUnits;

    /**
	 * Constructor for Base class. Passes all data through to the superclass
	 * constuctors who assign values to attributes.
	 * @param s - the enumeration of StructureType Base belongs to
	 * @param player - PlayerStatus object that controls this base
	 * @param position - the Tile that the base will reside on
	 */
    public Base() {
        super();
        planet = new Planet();
    }

    public Base(StructureType s, PlayerStatus player, Tile position) {
        super(EntityType.STRUCTURE, new Stats(StructureType.BASE, player, position));
        this.sType = s;
        this.planet = (Planet) position.getTerrain();
        this.healingUnits = new Vector<Unit>();
        this.getPlayer().modifyEnergy(this.planet.getEnergy());
    }

    /**
	 * Returns the enumeration value from StuctureType that defines this 
	 * Base
	 * @return StructureType enumeration value
	 */
    public StructureType getStructureType() {
        return this.sType;
    }

    /**
	 * The enumeration value (UnitType) that the Base is currently producing.
	 * @return UnitType enumeration value of the Unit in production
	 */
    public UnitType beingBuilt() {
        return this.building;
    }

    /**
	 * Currently returns the manpower that the Base receives from Planet at the
	 * beginning of each turn.
	 * @return int value - the amount of manpower received at each turn
	 */
    public int getProductionRate() {
        return this.planet.getManpower();
    }

    public Planet getPlanet() {
        return this.planet;
    }

    /**
	 * Request this base begin building the UnitType passed
	 * @param uType UnitType - the enumeration value to be built
	 * @return true if nothing is currently being built; false if the base is busy
	 */
    public boolean beginBuilding(UnitType uType) {
        if (this.building == null) {
            MainScreen.writeToConsole("Base: Unit " + uType.toString() + " began building.", Color.GREEN);
            this.building = uType;
            this.mpLeft = uType.manpower();
            return true;
        } else {
            MainScreen.writeToConsole("Base: Base is already building a " + building, Color.GREEN);
            return false;
        }
    }

    /**
	 * Adds Unit to the list of Units healing at this base. When a Unit heals, it
	 * cannot move until it is fully healed.
	 * <p>
	 * ??? Can Unit defend while healing? ???
	 * @param u - the Unit to be healed.
	 */
    public void healUnit(Unit u) {
        if (!(u instanceof Army)) {
            MainScreen.writeToConsole("Base: Unit added to healing list.", Color.GREEN);
            this.healingUnits.add(u);
            u.setRemainingMoves(0);
        } else MainScreen.writeToConsole("Base: Cannot heal Army.", Color.GREEN);
    }

    /**
	 * Removes a Unit from the list of Units healing
	 * @param u Unit to be removed from the list
	 * @return true if the Unit was in the list and removed; false otherwise
	 */
    public boolean removeHealing(Unit u) {
        return this.healingUnits.remove(u);
    }

    private void healUnits(int manpower) {
        Vector<Unit> toBeRemoved = new Vector<Unit>();
        for (Unit u : this.healingUnits) {
            if (u.getCurrentHP() == u.getMaxHP()) {
                toBeRemoved.add(u);
            } else {
                u.setRemainingMoves(0);
                int h = manpower / this.healFactor;
                if (u.heal(h)) toBeRemoved.add(u);
            }
        }
        for (Unit u : toBeRemoved) {
            this.healingUnits.remove(u);
            MainScreen.writeToConsole("Base: Unit completely healed.", Color.GREEN);
        }
    }

    public boolean newTurn() {
        int mp = 0;
        mp = planet.getManpower();
        this.getPlayer().modifyOre(this.planet.extractOre(this.oreExtractionRate));
        this.healUnits(mp);
        if (this.building != null) {
            this.mpLeft -= mp;
            if (this.mpLeft <= 0) {
                Unit newUnit = UnitFactory.makeUnit(building, super.getPlayer(), super.getPosition());
                this.getPlayer().addUnit(newUnit);
                this.getPosition().addUnit(newUnit);
                MainScreen.writeToConsole("Base:  Finished building " + building, Color.GREEN);
                this.building = null;
            }
        }
        return true;
    }

    public boolean destroy() {
        this.getPlayer().modifyEnergy(-this.planet.getEnergy());
        getPlayer().removeBase(this);
        getPosition().removeBase(this);
        MainScreen.writeToConsole("Base: A base has been destroyed.", Color.GREEN);
        return super.destroy();
    }
}
