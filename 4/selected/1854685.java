package org.mmtk.utility.gcspy;

import org.mmtk.vm.gcspy.AbstractDriver;
import org.mmtk.utility.FreeListVMResource;
import com.ibm.JikesRVM.VM_Address;
import com.ibm.JikesRVM.VM_Uninterruptible;
import org.mmtk.plan.Plan;
import org.mmtk.policy.TreadmillLocal;
import org.mmtk.utility.Conversions;
import org.mmtk.utility.Treadmill;
import org.mmtk.utility.Log;
import org.mmtk.vm.gcspy.Color;
import org.mmtk.vm.gcspy.AbstractTile;
import org.mmtk.vm.gcspy.Subspace;
import org.mmtk.vm.gcspy.ServerInterpreter;
import org.mmtk.vm.gcspy.ServerSpace;
import org.mmtk.vm.gcspy.Stream;
import org.mmtk.vm.gcspy.StreamConstants;
import org.mmtk.vm.VM_Interface;
import com.ibm.JikesRVM.VM_Offset;

/**
 * This class implements a simple driver for the JMTk treadmill space.
 *
 * @author <a href="http://www.ukc.ac.uk/people/staff/rej">Richard Jones</a>
 * @version $Revision: 7030 $
 * @date $Date: 2004-04-09 13:07:29 -0400 (Fri, 09 Apr 2004) $
 */
public class TreadmillDriver extends AbstractDriver implements VM_Uninterruptible {

    public static final String Id = "$Id: TreadmillDriver.java 7030 2004-04-09 17:07:29Z perry-oss $";

    private static final int LOS_USED_SPACE_STREAM = 0;

    private static final int LOS_OBJECTS_STREAM = 1;

    private static final int BUFSIZE = 128;

    /**
   * Representation of a tile
   *
   * We count the number of objects in each tile and the space they use
   */
    class Tile extends AbstractTile implements VM_Uninterruptible {

        short objects;

        int usedSpace;

        /**
     * Create a new tile with statistics zeroed
     */
        public Tile() {
            zero();
        }

        /**
     * Zero a tile's statistics
     */
        public void zero() {
            super.zero();
            objects = 0;
            usedSpace = 0;
        }
    }

    private Stream usedSpaceStream;

    private Stream objectsStream;

    private Tile[] tiles;

    private Subspace subspace;

    private int totalObjects = 0;

    private int totalUsedSpace = 0;

    private VM_Address maxAddr;

    private FreeListVMResource losVM;

    /**
   * Only used by sub-classes
   */
    TreadmillDriver() {
    }

    /**
   * Create a new driver for this collector
   * 
   * @param name The name of this driver
   * @param losVM the VMResource for this allocator
   * @param blocksize The tile size
   * @param start The address of the start of the space
   * @param end The address of the end of the space
   * @param size The size (in blocks) of the space
   * @param threshold the size threshold of the LOS
   * @param mainSpace Is this the main space?
   */
    public TreadmillDriver(String name, FreeListVMResource losVM, int blockSize, VM_Address start, VM_Address end, int size, int threshold, boolean mainSpace) {
        this.losVM = losVM;
        this.blockSize = blockSize;
        maxAddr = start;
        int maxTileNum = countTileNum(start, end, blockSize);
        tiles = new Tile[maxTileNum];
        for (int i = 0; i < maxTileNum; i++) tiles[i] = new Tile();
        subspace = new Subspace(start, start, 0, blockSize, 0);
        allTileNum = 0;
        String tmp = (blockSize < 1024) ? "Block Size: " + blockSize + " bytes\n" : "Block Size: " + (blockSize / 1024) + " bytes\n";
        space = new ServerSpace(Plan.getNextServerSpaceId(), name, "JMTk Treadmill Space", "Block ", tmp, size, "UNUSED", mainSpace);
        setTilenames(0);
        usedSpaceStream = new Stream(space, LOS_USED_SPACE_STREAM, StreamConstants.INT_TYPE, "Used Space stream", 0, blockSize, 0, 0, "Space used: ", " bytes", StreamConstants.PRESENTATION_PERCENT, StreamConstants.PAINT_STYLE_ZERO, 0, Color.Red);
        objectsStream = new Stream(space, LOS_OBJECTS_STREAM, StreamConstants.SHORT_TYPE, "Objects stream", 0, (int) (blockSize / threshold), 0, 0, "No. of objects = ", " objects", StreamConstants.PRESENTATION_PLUS, StreamConstants.PAINT_STYLE_ZERO, 0, Color.Green);
        zero();
    }

    /**
   * Setup tile names
   *
   * @param numTiles the number of tiles to name
   */
    private void setTilenames(int numTiles) {
        int tile = 0;
        VM_Address start = subspace.getStart();
        int first = subspace.getFirstIndex();
        int bs = subspace.getBlockSize();
        for (int i = 0; i < numTiles; ++i) {
            if (subspace.indexInRange(i)) space.setTilename(i, start.add((i - first) * bs), start.add((i + 1 - first) * bs));
        }
    }

    /**
   * Zero tile stats
   */
    public void zero() {
        for (int i = 0; i < tiles.length; i++) tiles[i].zero();
        totalObjects = 0;
        totalUsedSpace = 0;
    }

    private void checkspace(int index, int length, String err) {
        if (length > 0) {
            Log.write("..added ", length);
            Log.write(" to index ", index);
            Log.writeln(", now ", tiles[index].usedSpace);
        }
        int max = usedSpaceStream.getMaxValue();
        if (tiles[index].usedSpace > max) {
            Log.write("Treadmill.traceObject: usedSpace too high at ", index);
            Log.write(": ", tiles[index].usedSpace);
            Log.write(", max=", max);
            Log.write(" in ");
            Log.writeln(err);
            tiles[index].usedSpace = max;
        }
    }

    /**
   * Update the tile statistics
   * In this case, we are accounting for super-page objects, rather than
   * simply for the object they contain.
   * 
   * @param addr The address of the current object
   */
    public void traceObject(VM_Address addr) {
        VM_Address sp = TreadmillLocal.getSuperPage(addr);
        int index = subspace.getIndex(sp);
        int length = Conversions.pagesToBytes(losVM.getSize(sp)).toInt();
        totalObjects++;
        tiles[index].objects++;
        if (VM_Interface.VerifyAssertions) VM_Interface._assert(blockSize <= usedSpaceStream.getMaxValue());
        totalUsedSpace += length;
        int remainder = subspace.spaceRemaining(addr);
        if (VM_Interface.VerifyAssertions) VM_Interface._assert(remainder <= blockSize);
        if (length <= remainder) {
            tiles[index].usedSpace += length;
        } else {
            tiles[index].usedSpace += remainder;
            length -= remainder;
            index++;
            while (length >= blockSize) {
                tiles[index].usedSpace += blockSize;
                length -= blockSize;
                index++;
            }
            tiles[index].usedSpace += length;
        }
        if (addr.GT(maxAddr)) maxAddr = addr;
    }

    /**
   * Get the length of the superpage
   *
   * @param addr an address in the super-page
   * @return the length of the super-page
   */
    private int getSuperPageLength(VM_Address addr) {
        VM_Address sp = TreadmillLocal.getSuperPage(addr);
        return Conversions.pagesToBytes(losVM.getSize(sp)).toInt();
    }

    /**
   * Finish a transmission
   * 
   * @param event The event
   */
    public void finish(int event) {
        if (ServerInterpreter.isConnected(event)) {
            send(event);
        }
    }

    /**
   * Send the data for an event
   * 
   * @param event The event
   */
    private void send(int event) {
        VM_Address start = subspace.getStart();
        int required = countTileNum(start, maxAddr, blockSize);
        int current = subspace.getBlockNum();
        if (required > current || maxAddr != subspace.getEnd()) {
            subspace.reset(start, maxAddr, 0, required);
            allTileNum = required;
            space.resize(allTileNum);
            setTilenames(allTileNum);
        }
        space.startComm();
        int numTiles = allTileNum;
        space.stream(LOS_USED_SPACE_STREAM, numTiles);
        for (int i = 0; i < numTiles; ++i) {
            space.streamIntValue(tiles[i].usedSpace);
        }
        space.streamEnd();
        space.summary(LOS_USED_SPACE_STREAM, 2);
        space.summaryValue(totalUsedSpace);
        space.summaryValue(subspace.getEnd().diff(subspace.getStart()).toInt());
        space.summaryEnd();
        space.stream(LOS_OBJECTS_STREAM, numTiles);
        for (int i = 0; i < numTiles; ++i) {
            space.streamShortValue(tiles[i].objects);
        }
        space.streamEnd();
        space.summary(LOS_OBJECTS_STREAM, 1);
        space.summaryValue(totalObjects);
        space.summaryEnd();
        controlValues(tiles, AbstractTile.CONTROL_USED, subspace.getFirstIndex(), subspace.getBlockNum());
        space.controlEnd(numTiles, tiles);
        VM_Offset size = subspace.getEnd().diff(subspace.getStart());
        sendSpaceInfoAndEndComm(size);
    }

    public TreadmillDriver(String name, FreeListVMResource losVM, int blockSize, VM_Address start, VM_Address end, int size, int threshold, boolean mainSpace) {
    }
}
