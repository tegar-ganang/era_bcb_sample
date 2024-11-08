package net.sf.myra.framework;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an artificial ant.
 * 
 * @author Fernando Esteban Barril Otero
 * @version $Revision: 499 $ $Date:: 2008-12-01 13:44:35#$
 */
public class Ant implements PropertyChangeListener, Comparable<Ant> {

    /**
	 * The minimum memory size allowed.
	 */
    public static final int MINIMUM_MEMORY_SIZE = 1;

    /**
	 * The reference to the colony that this ant belongs.
	 */
    private Colony colony;

    /**
	 * The ant trail memory.
	 */
    private Trail[] memory;

    /**
	 * The current ant trail.
	 */
    private Trail trail;

    /**
	 * Creates a new ant.
	 * 
	 * @param colony the colony of the ant.
	 */
    public Ant(Colony colony) {
        this(colony, MINIMUM_MEMORY_SIZE);
    }

    /**
	 * Creates a new ant.
	 * 
	 * @param colony the colony of the ant.
	 * @param memory the ant memory size.
	 */
    public Ant(Colony colony, int memory) {
        if (memory < MINIMUM_MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory size " + memory);
        }
        this.colony = colony;
        this.memory = new Trail[memory];
    }

    /**
	 * Creates a new trail by walking in the problem construction graph.
	 */
    public Trail walk() {
        setTrail(colony.getTrailFactory().createTrail());
        return trail;
    }

    /**
	 * Returns the ordered (descendant order) ant trail memory list.
	 * 
	 * @return the ordered ant trail memory list.
	 */
    public List<Trail> getMemory() {
        ArrayList<Trail> m = new ArrayList<Trail>(memory.length);
        for (int i = 0; i < memory.length; i++) {
            if (memory[i] != null) {
                m.add(memory[i]);
            }
        }
        return m;
    }

    /**
	 * Returns the current ant trail.
	 * 
	 * @return the current ant trail.
	 */
    public Trail getTrail() {
        return trail;
    }

    /**
	 * Returns the best trail constructed by the ant.
	 * 
	 * @return the best trail constructed by the ant.
	 */
    public Trail getBestTrail() {
        return memory[0];
    }

    /**
	 * Sets the current ant trail.
	 * 
	 * @param trail the trail to set.
	 */
    public void setTrail(Trail trail) {
        this.trail = trail;
        trail.addPropertyChangeListener(this);
        if (trail.getCost() != null) {
            updateMemory(trail);
        }
    }

    /**
	 * Updates the ant trail memory, adding the specified trail to the memory
	 * if its quality is greater than a trail contained in the memory.
	 * 
	 * @param t the canditade trail to be added to the memory.
	 */
    private void updateMemory(Trail t) {
        for (int i = 0; i < memory.length; i++) {
            if (memory[i] == t) {
                for (int j = i; j < (memory.length - 1); j++) {
                    memory[j] = memory[j + 1];
                }
                memory[memory.length - 1] = null;
            }
        }
        if (t.getCost() != null) {
            for (int i = 0; i < memory.length; i++) {
                if (memory[i] == null) {
                    memory[i] = t;
                    break;
                } else if ((t.getCost().compareTo(memory[i].getCost()) == 1) || ((t.getCost().compareTo(memory[i].getCost()) == 0) && (t.getVertices().size() < memory[i].getVertices().size()))) {
                    for (int j = (memory.length - 1); j > i; j--) {
                        memory[j] = memory[j - 1];
                    }
                    memory[i] = t;
                    break;
                }
            }
        }
    }

    /**
	 * Updates the memory status when a trail has its cost associated.
	 * 
	 * @param event the <code>PropertyChangeEvent</code> instance.
	 */
    public void propertyChange(PropertyChangeEvent event) {
        updateMemory((Trail) event.getSource());
    }

    public int compareTo(Ant o) {
        boolean ant1 = getBestTrail() != null;
        boolean ant2 = o.getBestTrail() != null;
        if (ant1 && ant2) {
            return getBestTrail().getCost().compareTo(o.getBestTrail().getCost());
        } else if (!(ant1 || ant2)) {
            return 0;
        }
        return (ant1 ? 1 : -1);
    }

    @Override
    public String toString() {
        return "Ant[" + (trail == null ? "<null>" : trail.toString()) + "]";
    }
}
