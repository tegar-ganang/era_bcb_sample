package com.bix.util.blizfiles.skin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;
import com.bix.util.blizfiles.BufferUtils;
import com.bix.util.blizfiles.m2.M2CountOffsetPair;
import com.bix.util.blizfiles.m2.M2File;
import com.jme.math.Vector3f;

/**
 * This class encapsulates information about a .skin file. The vast majority of
 * this class was based on formats described <a href="
 * http://madx.dk/wowdev/wiki/index.php?title=M2/WotLK/.skin">here</a>.
 * 
 * @author squid
 * 
 * @version 1.0.0
 */
public class SkinFile {

    private static final Logger log = Logger.getLogger(SkinFile.class.getName());

    public class SkinHeader {

        private byte[] magic;

        private M2CountOffsetPair indices;

        private M2CountOffsetPair triangles;

        private M2CountOffsetPair vertexProperties;

        private M2CountOffsetPair submeshes;

        private M2CountOffsetPair textureUnits;

        private int lod;

        public SkinHeader(ByteBuffer bb) throws IOException {
            read(bb);
        }

        /**
		 * Load a Submesh from a ByteBuffer.
		 * 
		 * @param bb
		 */
        public void read(ByteBuffer bb) {
            this.magic = BufferUtils.getByteArray(bb, 4);
            if (this.magic[0] != 'S' || this.magic[1] != 'K' || this.magic[2] != 'I' || this.magic[3] != 'N') {
                throw new RuntimeException("Invalid SKIN file.");
            }
            this.indices = new M2CountOffsetPair(bb);
            this.triangles = new M2CountOffsetPair(bb);
            this.vertexProperties = new M2CountOffsetPair(bb);
            this.submeshes = new M2CountOffsetPair(bb);
            this.textureUnits = new M2CountOffsetPair(bb);
            this.lod = bb.getInt();
            log.debug("  magic[SKIN]");
            log.debug("  indices[" + this.indices.getCount() + ",0x" + Integer.toHexString(this.indices.getOffset()) + "]");
            log.debug("  triangles[" + this.triangles.getCount() + ",0x" + Integer.toHexString(this.triangles.getOffset()) + "]");
            log.debug("  vertexProperties[" + this.vertexProperties.getCount() + ",0x" + Integer.toHexString(this.vertexProperties.getOffset()) + "]");
            log.debug("  submeshes[" + this.submeshes.getCount() + ",0x" + Integer.toHexString(this.submeshes.getOffset()) + "]");
            log.debug("  textureUnits[" + this.textureUnits.getCount() + ",0x" + Integer.toHexString(this.textureUnits.getOffset()) + "]");
            log.debug("  lod[" + this.lod + "]");
        }
    }

    /**
	 * A TriangleIndex object is an object that represents an indirect reference
	 * to a triangle's vertices. Each vertex is an indirect reference into the
	 * skin's index list, which then references the global vertex list.
	 * 
	 * @author squid
	 * 
	 * @version 1.0.0
	 */
    public class TriangleIndex {

        private short vertexIndices[] = new short[3];

        public TriangleIndex(short xIndex, short yIndex, short zIndex) {
            vertexIndices[0] = xIndex;
            vertexIndices[1] = yIndex;
            vertexIndices[2] = zIndex;
        }

        public TriangleIndex(ByteBuffer bb) {
            read(bb);
        }

        public void read(ByteBuffer bb) {
            this.vertexIndices = BufferUtils.getShortArray(bb, 3);
        }

        /**
		 * Returns the triangle's X vertex index.
		 * 
		 * @return The x vertex index
		 */
        public short getXVertexIndex() {
            return this.vertexIndices[0];
        }

        /**
		 * Returns the triangle's Y vertex index.
		 * 
		 * @return The y vertex index
		 */
        public short getYVertexIndex() {
            return this.vertexIndices[1];
        }

        /**
		 * Returns the triangle's Z vertex index.
		 * 
		 * @return The z vertex index
		 */
        public short getZVertexIndex() {
            return this.vertexIndices[2];
        }

        /**
		 * Returns the array of vertex indices as an array of shorts (3 values).
		 * The first index is the x vertex index, the second index is the y vertex
		 * index, and the third index is the z vertex index.
		 * 
		 * @return	The triangle's vertex indices as an array of three shorts.
		 */
        public short[] getVertexIndices() {
            return this.vertexIndices;
        }
    }

    /**
	 * A submesh is essentially a list of faces (vertexes, triangles, etc) that
	 * reference a model's list of vertices. It represents only a portion of a
	 * full model/mesh. A model is broken up as a series of submeshes.
	 * 
	 * @author squid
	 * 
	 * @version 1.0.0
	 */
    public class Submesh {

        private int partId;

        private short startVertex;

        private short numVertices;

        private short startTriangle;

        private short numTriangleVertices;

        private short numBones;

        private short startBoneIndex;

        private short unknown;

        private short rootBone;

        private Vector3f position;

        private float[] floats;

        public Submesh() {
        }

        public Submesh(ByteBuffer bb) {
            read(bb);
        }

        public void read(ByteBuffer bb) {
            this.partId = bb.getInt();
            this.startVertex = bb.getShort();
            this.numVertices = bb.getShort();
            this.startTriangle = bb.getShort();
            this.numTriangleVertices = bb.getShort();
            this.numBones = bb.getShort();
            this.startBoneIndex = bb.getShort();
            this.unknown = bb.getShort();
            this.rootBone = bb.getShort();
            this.position = BufferUtils.getVector3f(bb);
            this.floats = BufferUtils.getFloatArray(bb, 4);
            log.debug("Submesh.read ()");
            log.debug("  partId[" + this.partId + "]");
            log.debug("  startVertex[" + this.startVertex + "]");
            log.debug("  numVertices[" + this.numVertices + "]");
            log.debug("  startTriangle[" + this.startTriangle + "]");
            log.debug("  numTriangleVertices[" + this.numTriangleVertices + "]");
            log.debug("  numBones[" + this.numBones + "]");
            log.debug("  startBoneIndex[" + this.startBoneIndex + "]");
            log.debug("  unknown[" + this.unknown + "]");
            log.debug("  rootBone[" + this.rootBone + "]");
            log.debug("  position[" + this.position.getX() + "," + this.position.getY() + "," + this.position.getZ() + "]");
            for (int i = 0; i < this.floats.length; i++) {
                log.debug("  float #" + i + "[" + this.floats[i] + "]");
            }
        }

        /**
		 * @return the partId
		 */
        public int getPartId() {
            return partId;
        }

        /**
		 * @return the startVertex
		 */
        public short getStartVertex() {
            return startVertex;
        }

        /**
		 * @return the numVertices
		 */
        public short getNumVertices() {
            return numVertices;
        }

        /**
		 * @return the startTriangle
		 */
        public short getStartTriangle() {
            return startTriangle;
        }

        /**
		 * @return the numTriangles
		 */
        public short getNumTriangleVertices() {
            return numTriangleVertices;
        }

        /**
		 * @return the numBones
		 */
        public short getNumBones() {
            return numBones;
        }

        /**
		 * @return the startBoneIndex
		 */
        public short getStartBoneIndex() {
            return startBoneIndex;
        }

        /**
		 * @return the unknown
		 */
        public short getUnknown() {
            return unknown;
        }

        /**
		 * @return the rootBone
		 */
        public short getRootBone() {
            return rootBone;
        }

        /**
		 * @return the position
		 */
        public Vector3f getPosition() {
            return position;
        }

        /**
		 * @return the floats
		 */
        public float[] getFloats() {
            return floats;
        }
    }

    /**
	 * This class represents a texture unit. From the wiki:
	 * 
	 * <pre>
	 * More specifically, textures for each texture unit. Based on the current 
	 * submesh number, one or two of these are used to determine the texture(s) 
	 * to bind.
	 * 
	 * Offset  Type    Name           Description
	 * 0x00    uint16  Flags          Usually 16 for static textures, and 0 for animated textures.
	 * 0x02    uint16  RenderOrder    Used in skyboxes to ditch the need for depth buffering.
	 * 0x04    uint16  SubmeshIndex   A duplicate entry of a submesh from the list above.
	 * 0x06    uint16  SubmeshIndex2	
	 * 0x08    int16   ColorIndex	    A Color out of the Colors-Block or -1 if none.
	 * 0x0A    uint16  RenderFlags    The renderflags used on this texture-unit.
	 * 0x0C    uint16  TexUnitNumber  Index into the texture unit lookup table.
	 * 0x0E    uint16  Unknown        Always 1.
	 * 0x10    uint16  Texture        The texture to use. That list at ofsTextures
	 * 0x12    uint16  TexUnitNumber2 Duplicate of TexUnitNumber.
	 * 0x14    uint16  Transparency   Index into transparency lookup table.
	 * 0x16    uint16  TextureAnim    Was a index into the texture animation lookup table.
	 * </pre>
	 * 
	 * @author squid
	 * 
	 * @version 1.0.0
	 */
    public class TextureUnit {

        private short flags;

        private short renderOrder;

        private short submeshIndex;

        private short submeshIndex2;

        private short colorIndex;

        private short renderFlags;

        private short textureUnitNumber;

        private short unknown;

        private short texture;

        private short textureUnitNumber2;

        private short transparency;

        private short textureAnim;

        public TextureUnit() {
        }

        /**
		 * Instantiate a TextureUnit from a ByteBuffer.
		 * 
		 * @param bb	The ByteBuffer to read the object from.
		 * 
		 * @see	TextureUnit#read
		 */
        public TextureUnit(ByteBuffer bb) {
            read(bb);
        }

        /**
		 * Read a TextureUnit from a ByteBuffer.
		 * 
		 * @param bb	The ByteBuffer to read the object from.
		 */
        public void read(ByteBuffer bb) {
            log.debug("Reading TextureUnit:");
            this.flags = bb.getShort();
            this.renderOrder = bb.getShort();
            this.submeshIndex = bb.getShort();
            this.submeshIndex2 = bb.getShort();
            this.colorIndex = bb.getShort();
            this.renderFlags = bb.getShort();
            this.textureUnitNumber = bb.getShort();
            this.unknown = bb.getShort();
            this.texture = bb.getShort();
            this.textureUnitNumber2 = bb.getShort();
            this.transparency = bb.getShort();
            this.textureAnim = bb.getShort();
            log.debug("TextureUnit.read ()");
            log.debug("  flags[0x" + Integer.toHexString(this.flags) + "]");
            log.debug("  renderOrder[" + this.renderOrder + "]");
            log.debug("  submeshIndex[" + this.submeshIndex + "]");
            log.debug("  submeshIndex2[" + this.submeshIndex2 + "]");
            log.debug("  colorIndex[" + this.colorIndex + "]");
            log.debug("  renderFlags[" + this.renderFlags + "]");
            log.debug("  textureUnitNumber[" + this.textureUnitNumber + "]");
            log.debug("  unknown[" + this.unknown + "]");
            log.debug("  texture[" + this.texture + "]");
            log.debug("  textureUnitNumber2[" + this.textureUnitNumber2 + "]");
            log.debug("  transparency[" + this.transparency + "]");
            log.debug("  textureAnim[" + this.textureAnim + "]");
        }

        /**
		 * @return the flags
		 */
        public short getFlags() {
            return flags;
        }

        /**
		 * @return the renderOrder
		 */
        public short getRenderOrder() {
            return renderOrder;
        }

        /**
		 * @return the submeshIndex
		 */
        public short getSubmeshIndex() {
            return submeshIndex;
        }

        /**
		 * @return the submeshIndex2
		 */
        public short getSubmeshIndex2() {
            return submeshIndex2;
        }

        /**
		 * @return the colorIndex
		 */
        public short getColorIndex() {
            return colorIndex;
        }

        /**
		 * @return the renderFlags
		 */
        public short getRenderFlags() {
            return renderFlags;
        }

        /**
		 * @return the textureUnitNumber
		 */
        public short getTextureUnitNumber() {
            return textureUnitNumber;
        }

        /**
		 * @return the unknown
		 */
        public short getUnknown() {
            return unknown;
        }

        /**
		 * @return the texture
		 */
        public short getTexture() {
            return texture;
        }

        /**
		 * @return the textureUnitNumber2
		 */
        public short getTextureUnitNumber2() {
            return textureUnitNumber2;
        }

        /**
		 * @return the transparency
		 */
        public short getTransparency() {
            return transparency;
        }

        /**
		 * @return the textureAnim
		 */
        public short getTextureAnim() {
            return textureAnim;
        }
    }

    private SkinHeader header;

    private short[] vertexIndexList;

    private TriangleIndex[] triangleList;

    private int[] vertexPropertyList;

    private Submesh[] submeshList;

    private TextureUnit[] textureUnits;

    public SkinFile(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        read(bb);
    }

    /**
	 * This reads a SkinFile from a ByteBuffer. The buffer should (probably) be
	 * set to LITTLE endian format before calling this.
	 * 
	 * @param bb
	 *          The buffer to read the skin file from.
	 * 
	 * @throws IOException
	 */
    public void read(ByteBuffer bb) throws IOException {
        int index = 0;
        this.header = new SkinHeader(bb);
        log.debug("Loading vertex index list...");
        this.vertexIndexList = BufferUtils.getShortArray(bb, this.header.indices);
        index = 0;
        for (short vertexIndex : vertexIndexList) {
            log.debug("Vertex #" + index++ + ":" + vertexIndex);
        }
        log.debug("Loading triangle vertex index list (Displayed as local indexes then global indexes)...");
        this.triangleList = readTriangleList(this.header.triangles, bb);
        index = 0;
        for (TriangleIndex triangle : triangleList) {
            int xIndex = triangle.getXVertexIndex();
            int yIndex = triangle.getYVertexIndex();
            int zIndex = triangle.getZVertexIndex();
            log.debug("Triangle #" + index++ + " [" + xIndex + "," + yIndex + "," + zIndex + "] - [" + this.vertexIndexList[xIndex] + "," + this.vertexIndexList[yIndex] + "," + this.vertexIndexList[zIndex] + "]");
        }
        log.debug("Loading vertex property list...");
        this.vertexPropertyList = BufferUtils.getIntArray(bb, this.header.vertexProperties);
        if (this.header.submeshes.getCount() > 0) {
            bb.position(this.header.submeshes.getOffset());
            this.submeshList = new Submesh[this.header.submeshes.getCount()];
            for (int i = 0; i < this.submeshList.length; i++) {
                log.debug("Loading submesh #" + i);
                this.submeshList[i] = new Submesh(bb);
            }
        }
        log.debug("Loading texture units...");
        if (this.header.textureUnits.getCount() > 0) {
            bb.position(this.header.textureUnits.getOffset());
            this.textureUnits = new TextureUnit[this.header.textureUnits.getCount()];
            for (int i = 0; i < this.textureUnits.length; i++) {
                this.textureUnits[i] = new TextureUnit(bb);
            }
        }
    }

    /**
	 * This method reads a triangle index list from a ByteBuffer.
	 * 
	 * @param cop
	 * @param bb
	 * 
	 * @return
	 */
    protected TriangleIndex[] readTriangleList(M2CountOffsetPair cop, ByteBuffer bb) {
        TriangleIndex[] triangleIndexList;
        if (cop.getCount() > 0) {
            triangleIndexList = new TriangleIndex[cop.getCount() / 3];
            bb.position(cop.getOffset());
            for (int i = 0; i < cop.getCount() / 3; i++) {
                triangleIndexList[i] = new TriangleIndex(bb);
                for (int j = 0; j < 3; j++) {
                    if (triangleIndexList[i].getVertexIndices()[j] >= this.header.indices.getCount()) {
                        throw new RuntimeException("A triangle vertex index is pointing to an invalid vertex index.");
                    }
                }
            }
        } else {
            triangleIndexList = new TriangleIndex[0];
        }
        return triangleIndexList;
    }

    /**
	 * This method returns the skin's vertex index list. Each index in this list
	 * is an index into the global vertex list that was loaded from the model's
	 * file (the M2File).
	 * 
	 * @return the vertexIndexList
	 * 
	 * @see M2File#getVertices()
	 */
    public short[] getVertexIndexList() {
        return vertexIndexList;
    }

    /**
	 * This method returns the triangle index list. Each entry in this list is a
	 * triangle that is defined by three (3) indirect references. Each indirect
	 * reference is used to cross-reference the skin's vertex index list which, in
	 * turn, is used to cross reference the model's global vertex list.
	 * 
	 * @return The list of triangle indexes.
	 * 
	 * @see SkinFile#getVertexIndexList()
	 */
    public TriangleIndex[] getTriangleIndexList() {
        return triangleList;
    }

    /**
	 * Returns the vertex property list.
	 * 
	 * @return The Vertex Property List
	 */
    public int[] getVertexPropertyList() {
        return vertexPropertyList;
    }

    /**
	 * Returns the list of submeshes in this skin file.
	 * 
	 * @return The submeshList
	 * 
	 * @see SkinFile.Submesh
	 */
    public Submesh[] getSubmeshList() {
        return submeshList;
    }

    /**
	 * @return the textureUnits
	 * 
	 * @see SkinFile.TextureUnit
	 */
    public TextureUnit[] getTextureUnits() {
        return textureUnits;
    }
}
