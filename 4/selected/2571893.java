package org.inigma.utopia.parser;

import org.inigma.utopia.Coordinate;
import org.inigma.utopia.Race;
import org.inigma.utopia.Science;

public class ScienceParserData {

    private String provinceName;

    private Coordinate coordinate;

    private Science science;

    private boolean raw;

    public void adjustScience(int acres, Race race) {
        science.setAlchemy(getRaw(acres, race, science.getAlchemy(), Science.ALCHEMY_FACTOR));
        science.setTools(getRaw(acres, race, science.getTools(), Science.TOOLS_FACTOR));
        science.setHousing(getRaw(acres, race, science.getHousing(), Science.HOUSING_FACTOR));
        science.setFood(getRaw(acres, race, science.getFood(), Science.FOOD_FACTOR));
        science.setMilitary(getRaw(acres, race, science.getMilitary(), Science.MILITARY_FACTOR));
        science.setCrime(getRaw(acres, race, science.getCrime(), Science.CRIME_FACTOR));
        science.setChanneling(getRaw(acres, race, science.getChanneling(), Science.CHANNELING_FACTOR));
    }

    private int getRaw(int acres, Race race, int value, double factor) {
        if (!raw) {
            return value;
        }
        double raceFactor = 1.0;
        if (race == Race.Human) {
            raceFactor = 1.25;
        }
        return (int) (Math.pow(((value / 10.0) / factor) / raceFactor, 2) * acres);
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public Science getScience() {
        return science;
    }

    public boolean isRaw() {
        return raw;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    public void setScience(Science science) {
        this.science = science;
    }
}
