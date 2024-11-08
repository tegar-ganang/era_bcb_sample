package org.inigma.utopia;

import java.util.Calendar;
import java.util.UUID;
import org.inigma.utopia.utils.CalendarUtils;

/**
 * @author <a href="mailto:sejal@inigma.org">Sejal Patel</a>
 * @version $Revision: 1 $
 */
public class Science {

    public static final double ALCHEMY_FACTOR = 1.4;

    public static final double TOOLS_FACTOR = 1;

    public static final double HOUSING_FACTOR = 0.65;

    public static final double FOOD_FACTOR = 8;

    public static final double MILITARY_FACTOR = 1.4;

    public static final double CRIME_FACTOR = 6;

    public static final double CHANNELING_FACTOR = 6;

    private String id;

    private Province province;

    private int alchemy;

    private int tools;

    private int housing;

    private int food;

    private int military;

    private int crime;

    private int channeling;

    private Calendar lastUpdate;

    public Science() {
        this.id = UUID.randomUUID().toString();
        this.lastUpdate = CalendarUtils.getCalendar();
        this.lastUpdate.setTimeInMillis(0);
    }

    public Science(Province province) {
        this();
        this.province = province;
    }

    public void copy(Science data) {
        alchemy = data.alchemy;
        channeling = data.channeling;
        crime = data.crime;
        food = data.food;
        housing = data.housing;
        lastUpdate = data.lastUpdate;
        military = data.military;
        tools = data.tools;
    }

    public int getAlchemy() {
        return alchemy;
    }

    public int getChanneling() {
        return channeling;
    }

    public int getCrime() {
        return crime;
    }

    public int getFood() {
        return food;
    }

    public int getHousing() {
        return housing;
    }

    public String getId() {
        return id;
    }

    public Calendar getLastUpdate() {
        return lastUpdate;
    }

    public int getMilitary() {
        return military;
    }

    public Province getProvince() {
        return province;
    }

    public int getTools() {
        return tools;
    }

    public void setAlchemy(int alchemy) {
        this.alchemy = alchemy;
    }

    public void setChanneling(int channeling) {
        this.channeling = channeling;
    }

    public void setCrime(int crime) {
        this.crime = crime;
    }

    public void setFood(int food) {
        this.food = food;
    }

    public void setHousing(int housing) {
        this.housing = housing;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLastUpdate(Calendar lastUpdate) {
        this.lastUpdate = CalendarUtils.getCalendar(lastUpdate);
    }

    public void setMilitary(int military) {
        this.military = military;
    }

    public void setProvince(Province province) {
        this.province = province;
    }

    public void setTools(int tools) {
        this.tools = tools;
    }
}
