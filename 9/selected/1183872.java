package net.sourceforge.rcp.harpoon.app;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import net.sourceforge.harpoon.geom.DMSCoordinate;
import net.sourceforge.harpoon.geom.DMSPoint;
import net.sourceforge.harpoon.model.AirportUnit;
import net.sourceforge.harpoon.model.SensorsModel;
import net.sourceforge.harpoon.model.units.MovableUnit;
import net.sourceforge.harpoon.model.units.Unit;
import net.sourceforge.harpoon.model.units.WarUnit;
import net.sourceforge.harpoon.parts.RootMapPart;
import net.sourceforge.rcp.harpoon.editors.SceneryEditor;
import net.sourceforge.rcp.harpoon.editors.SceneryPage;
import net.sourceforge.rcp.harpoon.model.Scenery;

public class HarpoonLoop implements Runnable {

    private static Logger logger = Logger.getLogger("net.sourceforge.rcp.harpoon.app");

    static {
        logger.setLevel(Level.ALL);
    }

    private static final String NOT_CACHED = "NOT_CACHED";

    private static final String UPDATED = "UPDATED";

    private static final String MODIFIED = "MODIFIED";

    private final String name;

    protected Scenery referenceScenery;

    protected String cacheState = NOT_CACHED;

    protected Vector<Unit> friendUnits;

    protected Vector<Unit> enemyUnits;

    protected Vector<MovableUnit> movableUnits;

    protected HashMap<Unit, Unit> fading;

    public HarpoonLoop(Scenery sce, String name) {
        referenceScenery = sce;
        this.name = name;
    }

    /**
	 * Perform a single pass on the Enemy Detection Block and activate any enemies detected by the sensors that
	 * are active. Actions to be performed are:
	 * <ul>
	 * <li>Scan the list of units and get separate lists for friend War units, and enemy or other units.</li>
	 * <li>For each other unit, advance the time since it was detected, but only if it was detected.</li>
	 * <li>If this unit has been detected since at least 60 seconds, the move its state in the detection
	 * ladder.</li>
	 * <li>For any friend unit, get the sensors that have activated.</li>
	 * <li>If any unit has activated Radar sensors, scan the list of units for War units of Surface or Air type
	 * then are in range and then increment their detection state in the detection ladder.</li>
	 * <li>If any unit has the ECM sensor active, detect any unit that is Radio detectable (usually Airports).</li>
	 * </ul>
	 * <br>
	 * <br>
	 * <br>
	 * This loop is fired every 15 seconds on the exact minute seconds of 0, 15, 30 and 45. The actions to be
	 * performed on this periodic task loop are next:
	 * <ul>
	 * <li>Get the model units and classify them is the unit caches are empty or we have received a model
	 * change event.</li>
	 * <li>Match friend units with active sensors against other units to see if some detection event is
	 * generated. Update enemy uni state is detection is generated.</li>
	 * <li>If enemy units have moved to a undetected state, calculate the teoretical position area where the
	 * unit may have moved and update the model with this information.</li>
	 * <li>For each of the visible units calculate the new position from the movement algorithm. Compare this
	 * position with previous position and activate higlighting if the unit has moved.</li>
	 * </ul>
	 * 
	 */
    public void run() {
        logger.info(">>> Entering Processing Enemy Detection Loop");
        updateUnitData();
        movementPhase();
        int dum = 1;
        System.gc();
        logger.info("<<< Exiting Processing Enemy Detection Loop");
    }

    /**
	 * Checks the state of the unit cache. If some event has changed the number of elements on the model then we
	 * invalidate the caches and reprocess them again.
	 */
    protected void updateUnitData() {
        if (UPDATED.equals(cacheState)) return;
        final Iterator<Unit> it = referenceScenery.getModel().getChildren().iterator();
        friendUnits = new Vector<Unit>();
        enemyUnits = new Vector<Unit>();
        movableUnits = new Vector<MovableUnit>();
        while (it.hasNext()) {
            final Unit unit = it.next();
            if (Unit.FRIEND_SIDE == unit.getSide()) friendUnits.add(unit); else enemyUnits.add(unit);
            if (unit instanceof MovableUnit) movableUnits.add((MovableUnit) unit);
        }
        cacheState = UPDATED;
    }

    protected void updateEnemyDetection() {
        final Iterator<Unit> undetectedIt = enemyUnits.iterator();
        while (undetectedIt.hasNext()) {
            final Unit unit = undetectedIt.next();
            final String state = unit.getDetectState();
            if (HarpoonConstants.NOT_DETECTED_STATE.equals(state)) {
                fading.put(unit, unit);
            } else {
                logger.info("Enemy: " + unit.toString() + " goes undetected to " + state);
                unit.degradeDetection();
            }
        }
    }

    protected void detectionPhase() {
        logger.info(">>> Entering detection phase");
        final Iterator<Unit> friendIt = friendUnits.iterator();
        while (friendIt.hasNext()) {
            final Unit unit = friendIt.next();
            if (unit instanceof WarUnit) {
                WarUnit war = (WarUnit) unit;
                logger.info("-- Detecting with unit " + war.toString());
                final SensorsModel sensors = war.getSensorInformation();
                if (sensors.getRadarState()) radarDetectionPhase(war);
                if (sensors.getRadioState()) radarDetectionPhase(war);
            }
        }
    }

    /** Radar detection tries to detect and identify Movable Surface and Movable Air units. */
    protected void radarDetectionPhase(WarUnit war) {
        logger.info(">>> Entering RADAR detection phase");
        Iterator<Unit> enemyIt = enemyUnits.iterator();
        while (enemyIt.hasNext()) {
            Unit unit = enemyIt.next();
            if (unit instanceof MovableUnit) {
                final MovableUnit detectableUnit = (MovableUnit) unit;
                final String type = detectableUnit.getUnitType();
                if ((HarpoonConstants.UNIT_SURFACE.equals(type)) || (HarpoonConstants.UNIT_AIR.equals(type))) {
                    DMSPoint targetLocation = new DMSPoint(detectableUnit.getDMSLatitude(), detectableUnit.getDMSLongitude());
                    DMSPoint sourceLocation = new DMSPoint(war.getDMSLatitude(), war.getDMSLongitude());
                    DMSPoint targetVector = sourceLocation.offset(targetLocation);
                    final int range = war.getSensorRange(HarpoonConstants.RADAR_TYPE);
                    if (targetVector.getModule() < range) {
                        detectableUnit.upgradeDetection();
                    }
                }
            }
        }
    }

    /**
	 * This phase iterates over all Movable units to move the location over the movement path it it exists. In
	 * cases where the path has completed, the unit has to continue moving on the same direction indefinitely.<br>
	 * The new location of the unit is calculated by the space run from the starting point that is the first
	 * movement path point plus the delta movement that is calculated as the current speed by the elapsed time
	 * since the last movement adjustment.<br>
	 * A movement adjustment happens when the user changes the movement path or when there is a change on the
	 * speed. At that time point the current location is recalculated and it becomes the initial movement point
	 * for next movement calculations.
	 */
    protected void movementPhase() {
        logger.info(">>> Entering MOVEMENT phase");
        Iterator<MovableUnit> it = movableUnits.iterator();
        while (it.hasNext()) {
            MovableUnit unit = it.next();
            DMSPoint startLocation = unit.getStartMovementPoint();
            double elapsed = unit.elapsedMoveTime();
            if (elapsed <= 0.0) continue;
            int direction = unit.getBearing();
            double alpha = StrictMath.toRadians((360 - (direction - 90)) % 360);
            SceneryEditor editor = (SceneryEditor) referenceScenery.getEditor();
            SceneryPage page = editor.getMapPage();
            GraphicalViewer viewer = page.getGraphicalViewer();
            EditPart root = viewer.getContents();
            double space = 0.0;
            if (root instanceof RootMapPart) {
                RootMapPart rootPart1 = (RootMapPart) root;
                double speed = new Double(unit.getSpeed()).doubleValue();
                Point spoint = RootMapPart.dms2xy(rootPart1, new DMSPoint(DMSCoordinate.fromSeconds(new Double(speed).longValue(), DMSCoordinate.LATITUDE), DMSCoordinate.fromSeconds(new Double(speed).longValue(), DMSCoordinate.LONGITUDE)));
                space = StrictMath.abs(spoint.x * (elapsed / (60.0 * 60.0)));
            }
            double latDelta = space * StrictMath.sin(alpha);
            double lonDelta = space * StrictMath.cos(alpha);
            DMSPoint lastLocation = unit.getLocation();
            DMSPoint currentLocation = startLocation.translate(latDelta, lonDelta);
            if (root instanceof RootMapPart) {
                RootMapPart rootPart = (RootMapPart) root;
                Point prevXY = RootMapPart.dms2xy(rootPart, lastLocation);
                Point newXY = RootMapPart.dms2xy(rootPart, currentLocation);
                if (!prevXY.equals(newXY)) {
                    logger.info("Unit " + unit.toString() + " has moved to new coordinates " + newXY.toString());
                    unit.addTrace(currentLocation);
                    unit.setLatitude(currentLocation.getDMSLatitude());
                    unit.setLongitude(currentLocation.getDMSLongitude());
                }
            }
        }
    }

    /**
	 * Radio emission detection detects any unit that is using the radio. Airports and neutral Air units are
	 * automatically detected by this sensor.
	 * 
	 * @param war
	 *          friendly unit that is using the detector
	 */
    protected void radioEmissionDetectionPhase(WarUnit war) {
        logger.info(">>> Entering RARIO detection phase");
        final Iterator<Unit> enemyIt = enemyUnits.iterator();
        while (enemyIt.hasNext()) {
            Unit unit = enemyIt.next();
            if (unit instanceof AirportUnit) unit.upgradeDetection();
            if (unit instanceof MovableUnit) {
                final MovableUnit detectableUnit = (MovableUnit) unit;
                final String type = detectableUnit.getUnitType();
                if (HarpoonConstants.UNIT_AIR.equals(type)) {
                    detectableUnit.upgradeDetection();
                }
            }
        }
    }

    @Override
    public String toString() {
        return name + super.toString();
    }
}
