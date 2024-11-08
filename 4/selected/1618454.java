package server.gameObjects;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import server.ChatChannelListener;
import server.LocalSpaceChannelListener;
import server.localSpaces.ThemeManager;
import server.player.Player;
import server.tasks.BestEffortUpdateTask;
import server.tasks.LocalSpaceDeactivationCheckTask;
import server.tasks.WarpTransferTask;
import shared.network.LocalSpaceInformationPacket;
import shared.network.NetworkProtocol;
import shared.network.SerialUniversePacket;
import shared.network.UpdateHeaderPacket;
import util.MathHelper;
import util.Vector;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;

public class ServerLocalSpace implements ManagedObject, Serializable {

    private static final long serialVersionUID = 1L;

    private static final String updatePrefix = "localSpaceUpdate-";

    private static final String chatPrefix = NetworkProtocol.chatChannelPrefix + NetworkProtocol.localChatPrefix;

    public static final float extraBoundryGiveDistance = 300;

    private static final float tetherForceFactor = 0.001f;

    private static final float maxTetherForcePerSecond = 10000;

    private static final float minWarpSlices = 8;

    private static final float maxWarpSlices = 36;

    private static final float smallSpaceSize = 300;

    private static final float largeSpaceSize = 10000;

    private static final float minWarpActivationAmount = 1;

    private static final float warpDistLinearToSq = 12;

    private static final float angleNormalizationFactor = 3.5f;

    private static final float angleNormalizationBand = MathHelper.PI / 8;

    private static int localSpaceEnum = 0;

    protected final float location_x, location_y;

    private ArrayList<ManagedReference<? extends ServerSimulatedObject>> actors = new ArrayList<ManagedReference<? extends ServerSimulatedObject>>();

    private boolean active;

    private final SerialUniversePacket.LocalSpaceThemeTypes spaceType;

    private final int localSpaceId;

    private final String localSpaceName;

    private final float radius;

    private final float radiusSq;

    private final float boundryBorder;

    private final float boundryBorderSq;

    private final TravelSliceActivation[] warpDests;

    private final float warpAngleDelta;

    private ManagedReference<ServerLocalSpace> selfReference = null;

    private ManagedReference<ThemeManager> themeManager = null;

    /**
	 * This object creates a reference of itself in the data manager as part of
	 * its creation.
	 */
    public ServerLocalSpace(SerialUniversePacket.LocalSpaceThemeTypes type, float x, float y, String name, float radius) {
        spaceType = type;
        localSpaceId = localSpaceEnum++;
        location_x = x;
        location_y = y;
        localSpaceName = localSpaceId + "_" + name;
        this.radius = radius;
        radiusSq = radius * radius;
        boundryBorder = radius + extraBoundryGiveDistance;
        boundryBorderSq = boundryBorder * boundryBorder;
        float effectiveRad = Math.min(largeSpaceSize, radius);
        effectiveRad = Math.max(smallSpaceSize, effectiveRad);
        int slices = Math.round((radius - smallSpaceSize) / (largeSpaceSize - smallSpaceSize) * (maxWarpSlices - minWarpSlices) + minWarpSlices);
        warpDests = new TravelSliceActivation[slices];
        warpAngleDelta = MathHelper.tPI / slices;
        for (int i = 0; i < warpDests.length; i++) warpDests[i] = new TravelSliceActivation();
    }

    /**
	 * Registers the local space, 
	 *  adding an entry into data manager,
	 *  and adding chat and update channels
	 */
    public ManagedReference<ServerLocalSpace> register() {
        AppContext.getChannelManager().createChannel(chatPrefix + localSpaceName, new ChatChannelListener(), Delivery.UNRELIABLE);
        AppContext.getChannelManager().createChannel(updatePrefix + localSpaceName, new LocalSpaceChannelListener(), Delivery.RELIABLE);
        selfReference = AppContext.getDataManager().createReference(this);
        themeManager = spaceType.getManager(this).register();
        return selfReference;
    }

    public int getId() {
        return localSpaceId;
    }

    public SerialUniversePacket.LocalSpaceThemeTypes getType() {
        return spaceType;
    }

    public String getName() {
        return localSpaceName;
    }

    public float getRadius() {
        return radius;
    }

    public Vector getLocation() {
        return new Vector(location_x, location_y);
    }

    public ThemeManager getThemeManager() {
        return themeManager.get();
    }

    public void updateWarpDestinations(ServerLocalSpace tar) {
        boolean changed = false;
        Point2D.Float loc = new Point2D.Float(location_x, location_y);
        Point2D.Float tarLoc = new Point2D.Float(tar.location_x, tar.location_y);
        float dist = (float) loc.distance(tarLoc);
        float distanceWeight = 1 / dist;
        if (dist > warpDistLinearToSq) distanceWeight *= warpDistLinearToSq / (dist);
        float radiusWeight = (float) Math.sqrt((tar.radius + radius) / 2);
        for (int i = 0; i < warpDests.length; i++) changed |= setBestWarpDest(i, radiusWeight, distanceWeight, tar);
        if (changed && active) getUpdateChannel().send(LocalSpaceInformationPacket.create(localSpaceId, radius, localSpaceName, warpDests));
    }

    private final boolean setBestWarpDest(int slice, float radWeight, float distWeight, ServerLocalSpace tar) {
        float actualAng = (float) Math.atan2(tar.location_y - location_y, tar.location_x - location_x);
        float sliceAngle = MathHelper.AngleNormalizationToPlusMinusPIScale(slice * warpAngleDelta + warpAngleDelta / 2);
        float angDiff = MathHelper.AngleNormalizationToPlusMinusPIScale(sliceAngle - actualAng);
        if (Math.abs(angDiff) < warpAngleDelta / 2) angDiff = 0;
        if (Math.abs(angDiff) > MathHelper.hPI) return false;
        float angleWeight = MathHelper.getNormalDistValue(angDiff, angleNormalizationFactor, 0, angleNormalizationBand);
        float weight = angleWeight * radWeight * distWeight;
        if (weight > warpDests[slice].activationLevel && weight > minWarpActivationAmount) {
            warpDests[slice].activationLevel = weight;
            warpDests[slice].dest = tar.selfReference;
            return true;
        }
        return false;
    }

    public ManagedReference<ServerLocalSpace> getReference() {
        return selfReference;
    }

    public Channel getUpdateChannel() {
        return AppContext.getChannelManager().getChannel(updatePrefix + localSpaceName);
    }

    public Channel getChatChannel() {
        return AppContext.getChannelManager().getChannel(chatPrefix + localSpaceName);
    }

    public HashSet<Integer> getWarpDestinations() {
        HashSet<Integer> ret = new HashSet<Integer>();
        for (TravelSliceActivation t : warpDests) if (t.dest != null) ret.add(t.dest.get().getId());
        return ret;
    }

    /**
	 * Runs through all actions needed to synch a player to
	 * a local space.
	 * This doesn't necessarily imply anything about the player's
	 *  ship, just his session.
	 * @param session
	 */
    public void playerJoinLocalSpace(Player player) {
        player.changeViewedWorld(selfReference);
        player.sendMessageToPlayer(LocalSpaceInformationPacket.create(localSpaceId, radius, localSpaceName, warpDests));
        broadCastObjectIDsToPlayer(player);
        broadCastLastUpdateToPlayer(player);
        activate();
    }

    public void playerLeaveLocalSpace() {
        AppContext.getTaskManager().scheduleTask(new LocalSpaceDeactivationCheckTask(selfReference), 5000);
    }

    public boolean attemptControlledWarpTransfer(ServerSimulatedObject actor) {
        if (actor.position.magnitude2() < radiusSq) return false;
        float relAngle = MathHelper.AngleNormalizationTo2PIScale((float) Math.atan2(actor.position.y, actor.position.x));
        int slice = (int) (relAngle / warpAngleDelta);
        if (warpDests[slice].dest == null) return false;
        if (actor.getSelfReference() == null) {
            System.out.println("actor is null! " + actor.toString());
            return false;
        }
        System.out.println("Attempting Warp! Dest is " + warpDests[slice].dest.get().getName() + " actor ref is " + actor.getSelfReference().toString());
        AppContext.getTaskManager().scheduleTask(new WarpTransferTask(actor.getSelfReference(), warpDests[slice].dest));
        return true;
    }

    /**
	 * Handles a warp in from another local space.
	 * @param actorRef The actor being moved
	 */
    public void warpObjectIn(ManagedReference<? extends ServerSimulatedObject> actorRef, ServerLocalSpace source) {
        System.out.println("Warping Object into " + localSpaceName + " from " + source.getName());
        float angle = (float) Math.atan2(source.location_y - location_y, source.location_x - location_x);
        Vector entryAngle = new Vector(angle);
        entryAngle.scale(boundryBorder + extraBoundryGiveDistance * 5);
        ServerSimulatedObject actor = (ServerSimulatedObject) actorRef.getForUpdate();
        actor.position.setEqualTo(entryAngle);
        actor.velocity.x = 0;
        actor.velocity.y = 0;
        addActor(actorRef);
    }

    /**
	 * For this to work right, this is the only function that
	 *  can pick up SimulatedObjects for Update
	 * @param elapsedSeconds
	 */
    public void update(float elapsedSeconds) {
        int updateSize = actors.size() - 1;
        for (int i = updateSize; i >= 0; i--) {
            ManagedReference<? extends ServerSimulatedObject> actorRef = actors.get(i);
            ServerSimulatedObject actor = actorRef.getForUpdate();
            if (actor.isAlive()) {
                if (actor instanceof ServerPlayerShip && actor.position.magnitude2() > boundryBorderSq) {
                    Vector opp = Vector.scale(actor.position.getNormalized(), -1);
                    float deltaVMag = (actor.position.magnitude2() - boundryBorderSq) * tetherForceFactor * elapsedSeconds;
                    actor.velocity.addScaled(opp, Math.min(deltaVMag, elapsedSeconds * maxTetherForcePerSecond));
                }
                actor.update(elapsedSeconds);
            } else {
                getUpdateChannel().send(actor.makeObjectRemovalPacket());
                actors.remove(i);
                updateSize--;
                actor.unRegister();
            }
        }
        updateSize = actors.size() - 1;
        for (int i = 0; i <= updateSize; i++) {
            ServerSimulatedObject a = actors.get(i).getForUpdate();
            for (int j = i + 1; j <= updateSize; j++) {
                ServerSimulatedObject b = actors.get(j).getForUpdate();
                Collision.testAndHandleCollision(a, b, elapsedSeconds);
            }
        }
        for (int i = 0; i <= updateSize; i++) actors.get(i).getForUpdate().simulateMovement(elapsedSeconds);
    }

    /**
	 * Gets all actors whose radius touch or are contained by a defined circle
	 * Note: O(n) based on actors.
	 * @param pos
	 * @param radius
	 * @return
	 */
    public ArrayList<ServerSimulatedObject> getActorsNearPoint(Vector pos, float radius) {
        ArrayList<ServerSimulatedObject> ret = new ArrayList<ServerSimulatedObject>();
        for (ManagedReference<? extends ServerSimulatedObject> actorRef : actors) {
            ServerSimulatedObject actor = actorRef.get();
            float rdistsq = (radius + actor.radius) * (radius + actor.radius);
            if (Vector.sub(actor.position, pos).magnitude2() <= rdistsq) ret.add(actor);
        }
        return ret;
    }

    /**
	 * More general purpose function for getting all actors in a 
	 *  local space that are inside or touching a defined Area.
	 * @param area
	 * @return
	 */
    public ArrayList<ServerSimulatedObject> getActorsNearArbitarySelection(Area area) {
        ArrayList<ServerSimulatedObject> ret = new ArrayList<ServerSimulatedObject>();
        for (ManagedReference<? extends ServerSimulatedObject> actorRef : actors) {
            ServerSimulatedObject actor = actorRef.get();
            Rectangle.Float actorRect = new Rectangle.Float(actor.position.x - actor.radius, actor.position.y - actor.radius, 2 * actor.radius, 2 * actor.radius);
            if (area.contains(actor.position.x, actor.position.y) || area.intersects(actorRect)) ret.add(actor);
        }
        return ret;
    }

    public ArrayList<ServerSimulatedObject> getActorsOnLineSegment(Line2D.Float line) {
        ArrayList<ServerSimulatedObject> ret = new ArrayList<ServerSimulatedObject>();
        for (ManagedReference<? extends ServerSimulatedObject> actorRef : actors) {
            ServerSimulatedObject actor = actorRef.get();
            if (line.ptSegDistSq(actor.position.x, actor.position.y) <= actor.radius * actor.radius) ret.add(actor);
        }
        return ret;
    }

    /**
	 * Getter for all actors. O(n) on actors.
	 * @return
	 */
    public ArrayList<ServerSimulatedObject> getActors() {
        ArrayList<ServerSimulatedObject> ret = new ArrayList<ServerSimulatedObject>();
        for (ManagedReference<? extends ServerSimulatedObject> actorRef : actors) ret.add(actorRef.get());
        return ret;
    }

    /**
	 * Adds an actor to be simulated by this local space. As a result of the
	 * call, the object will be added to the data manager.
	 * 
	 * @param actor
	 *            A simulated object that needs to be updated by this
	 *            LocalSpace.
	 */
    public ManagedReference<? extends ServerSimulatedObject> addAndRegisterActor(ServerSimulatedObject actor) {
        actor.setLocalSpaceRef(selfReference);
        ManagedReference<? extends ServerSimulatedObject> ret = actor.register();
        actors.add(ret);
        getUpdateChannel().send(actor.makeObjectIDPacket());
        return ret;
    }

    public void addActor(ManagedReference<? extends ServerSimulatedObject> actor) {
        actors.add(actor);
        ServerSimulatedObject actorResolved = (ServerSimulatedObject) actor.get();
        AppContext.getDataManager().markForUpdate(actorResolved);
        if (actorResolved.getLocalSpace() != selfReference) actorResolved.setLocalSpaceRef(selfReference);
        actorResolved.setDirty();
        getUpdateChannel().send(actorResolved.makeObjectIDPacket());
    }

    public boolean removeActorNoUnRegister(ManagedReference<? extends ServerSimulatedObject> actor) {
        getUpdateChannel().send(actor.get().makeObjectRemovalPacket());
        return actors.remove(actor);
    }

    public boolean isActive() {
        return active;
    }

    /**
	 * Sends ObjectPositionPackets for all tracked objects
	 *  on the update channel.
	 */
    public void broadCastObjectInformation(long timestamp) {
        Channel updateChan = getUpdateChannel();
        updateChan.send(UpdateHeaderPacket.create(timestamp));
        for (ManagedReference<? extends ServerSimulatedObject> man : actors) {
            ServerSimulatedObject actor = man.getForUpdate();
            if (actor.isDirty()) {
                ByteBuffer packet = actor.makeInformationBroadCastPacket();
                if (packet != null) updateChan.send(packet);
                actor.postUpdateBroadcastEvent();
            }
        }
    }

    public void broadCastObjectIDsToPlayer(Player player) {
        for (ManagedReference<? extends ServerSimulatedObject> s : actors) player.sendMessageToPlayer(s.get().makeObjectIDPacket());
    }

    public void broadCastLastUpdateToPlayer(Player player) {
        for (ManagedReference<? extends ServerSimulatedObject> s : actors) {
            ByteBuffer message = s.get().makeInformationBroadCastPacket();
            if (message != null) player.sendMessageToPlayer(message);
        }
    }

    /**
	 * Activates the local space, causing a task to be queued
	 * for simulation execution. The task will automatically
	 * reschedule itself but will cease execution if the
	 * local space de-activates.
	 */
    public void activate() {
        if (!active) {
            System.out.println("Local space " + localSpaceName + " is being activated.");
            active = true;
            AppContext.getTaskManager().scheduleTask(new BestEffortUpdateTask(selfReference));
            themeManager.getForUpdate().activateEvent();
        }
    }

    public void deactivate() {
        if (active) {
            System.out.println("Local space " + localSpaceName + " is being deactivated.");
            active = false;
            themeManager.getForUpdate().deactivateEvent();
        }
    }

    public class TravelSliceActivation implements Serializable {

        private static final long serialVersionUID = 1L;

        public ManagedReference<ServerLocalSpace> dest = null;

        public float activationLevel = 0;
    }
}
