package com.bix.util.blizfiles.m2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.bix.util.blizfiles.BufferUtils;
import com.bix.util.blizfiles.skin.SkinFile;

/**
 * This class encapsulates the functionality of the Blizzard M2 file format. An
 * M2 file is, essentially, a file that contains a 3D model with ALL of the
 * necessary information to build a functioning, animated, and textured, 3D
 * model. There are other files necessary for this to work correctly such as
 * skins, bitmaps, etc.).
 * 
 * @author squid
 * 
 * @version 1.0.0
 */
public abstract class M2File {

    private Log log = LogFactory.getLog(M2File.class);

    private M2FileHeader header = new M2FileHeader();

    private List<M2Vertex> vertices;

    private List<SkinFile> skins;

    private List<M2Texture> textures;

    private short[] textureLookups;

    private List<M2Bone> bones;

    private short[] boneLookups;

    private short[] keyBoneLookups;

    private List<M2AnimationSequence> animationSequences;

    private short[] animationLookups;

    private M2Color[] colors;

    private short[] transparencies;

    private M2TextureAnimation[] textureAnimations;

    private String modelName;

    /**
	 * Implementers of this class need to override this method. This method should
	 * take the name of a resource and use whatever means necessary to find and
	 * locate the resource, returning a URL to that resource.
	 * 
	 * @param resource
	 *          The name of the resource to load.
	 * 
	 * @return The URL to the desired resource.
	 */
    public abstract URL getResource(String resource);

    public M2File() {
    }

    /**
	 * Create a new instance of a M2File object using the given file handle and a
	 * directory to locate the skins and other resources.
	 * 
	 * @param file
	 *          The file to load the model from.
	 * @param resourceDirectory
	 *          The directory to locate the dependent resources.
	 * 
	 * @throws IOException
	 * @throws M2FileException
	 * @throws URISyntaxException
	 */
    public M2File(File file) throws IOException, M2FileException, URISyntaxException {
        load(file);
    }

    /**
	 * This method loads the skins for the model.
	 * 
	 * @return The list of skin objects loaded from the files.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
    protected List<SkinFile> loadSkins() throws IOException, URISyntaxException {
        List<SkinFile> skinFiles = new ArrayList<SkinFile>();
        for (int i = 0; i < this.header.getViews(); i++) {
            String skinFilename = this.modelName + "0" + i + ".skin";
            log.debug(" Loading skin file[" + skinFilename + "]");
            URL skinURL = this.getResource(skinFilename);
            if (skinURL == null) {
                return skinFiles;
            }
            File skinFile = new File(skinURL.toURI());
            skinFiles.add(new SkinFile(skinFile));
        }
        return skinFiles;
    }

    /**
	 * Returns the name of the model.
	 * 
	 * @return The name of the model.
	 */
    public String getModelName() {
        return this.modelName;
    }

    /**
	 * Get the global vertex list used for this model.
	 * 
	 * @return The global vertex list.
	 */
    public List<M2Vertex> getVertices() {
        return this.vertices;
    }

    /**
	 * Returns the list of skins contained in this model. Each skin is a set of
	 * meshes that references the global vertex list.
	 * 
	 * @return The list of skins.
	 * 
	 * @see SkinFile
	 */
    public List<SkinFile> getSkins() {
        return this.skins;
    }

    /**
	 * Load a M2File object from a resource. This method uses a resource locator
	 * to locate the resource to be loaded.
	 * 
	 * @param resource
	 *          The name of a resource to load a M2File from.
	 * 
	 * @throws IOException
	 * @throws M2FileException
	 * @throws URISyntaxException
	 */
    public void load(String resource) throws IOException, M2FileException, URISyntaxException {
        URL url = this.getResource(resource);
        if (url == null) {
            throw new M2FileException("Resource was not found.");
        }
        File f = new File(url.toURI());
        if (f == null) {
            throw new M2FileException("File is null.");
        }
        this.load(f);
    }

    public void load(File file) throws IOException, M2FileException, URISyntaxException {
        FileChannel fc = new FileInputStream(file).getChannel();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        read(bb.order(ByteOrder.LITTLE_ENDIAN));
    }

    /**
	 * Load a M2File object from a ByteBuffer. The buffer should have been set to
	 * LITTLE endian format prior to calling this method (more than likely).
	 * 
	 * @param bb
	 *          The ByteBuffer to read the object from.
	 * 
	 * @throws M2FileException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
    public void read(ByteBuffer bb) throws M2FileException, IOException, URISyntaxException {
        header.read(bb);
        this.modelName = BufferUtils.getString(bb, header.getModelName());
        log.debug("modelName[" + this.modelName + "]");
        log.debug("Global Sequences[" + this.header.getGlobalSequences().getCount() + "]:");
        bb.position(header.getGlobalSequences().getOffset());
        for (int i = 0; i < this.header.getGlobalSequences().getCount(); i++) {
            int value = bb.getInt();
            log.debug("  Global Sequence #" + i + "[" + value + "]");
        }
        log.debug("Animation Sequences[" + this.header.getAnimations().getCount() + "]:");
        if (this.header.getAnimations().getCount() > 0) {
            this.animationSequences = new ArrayList<M2AnimationSequence>();
            bb.position(header.getAnimations().getOffset());
            for (int i = 0; i < this.header.getAnimations().getCount(); i++) {
                log.debug("Animation Sequence #" + i);
                this.animationSequences.add(new M2AnimationSequence(bb));
            }
        }
        log.debug("Animation Lookups[" + this.header.getAnimationLookups().getCount() + "]:");
        this.animationLookups = BufferUtils.getShortArray(bb, this.header.getAnimationLookups());
        for (int i = 0; i < this.header.getAnimationLookups().getCount(); i++) {
            log.debug("  Animation Lookup #" + i + "[" + this.animationLookups[i] + "]");
        }
        log.debug("Bones[" + this.header.getBones().getCount() + "]:");
        this.bones = new ArrayList<M2Bone>();
        if (this.header.getBones().getCount() > 0) {
            bb.position(this.header.getBones().getOffset());
            for (int i = 0; i < this.header.getBones().getCount(); i++) {
                log.debug(" Bone #" + i + ":");
                this.bones.add(new M2Bone("Bone #" + i, bb));
            }
            for (M2Bone bone : this.bones) {
                if (bone.getParentBoneId() != -1) {
                    bone.setParentBone(this.bones.get(bone.getParentBoneId()));
                }
            }
        }
        log.debug("Key Bone Lookups[" + this.header.getKeyBoneLookupTable().getCount() + "]:");
        this.keyBoneLookups = BufferUtils.getShortArray(bb, this.header.getKeyBoneLookupTable());
        for (int i = 0; i < this.header.getKeyBoneLookupTable().getCount(); i++) {
            log.debug("  Key Bone #" + i + "[" + this.keyBoneLookups[i] + "]");
        }
        log.debug("Bone Lookups[" + this.header.getBoneLookupTable().getCount() + "]:");
        this.boneLookups = BufferUtils.getShortArray(bb, this.header.getBoneLookupTable());
        for (int i = 0; i < this.header.getBoneLookupTable().getCount(); i++) {
            log.debug("  Bone #" + i + "[" + this.boneLookups[i] + "]");
        }
        log.debug("Texture Lookups[" + this.header.getTextureLookupEntries().getCount() + "]:");
        this.textureLookups = BufferUtils.getShortArray(bb, this.header.getTextureLookupEntries());
        for (int i = 0; i < this.header.getTextureLookupEntries().getCount(); i++) {
            log.debug("  Texture Lookup #" + i + "[" + this.textureLookups[i] + "]");
        }
        log.debug("Vertices[" + this.header.getVertices().getCount() + "]:");
        this.vertices = new ArrayList<M2Vertex>();
        if (this.header.getVertices().getCount() > 0) {
            bb.position(this.header.getVertices().getOffset());
            for (int i = 0; i < this.header.getVertices().getCount(); i++) {
                log.debug(" Vertex #" + i + ":");
                this.vertices.add(i, new M2Vertex(bb));
            }
        }
        log.debug("Colors[" + this.header.getColors().getCount() + "]:");
        if (this.header.getColors().getCount() >= 0) {
            bb.position(this.header.getColors().getOffset());
            this.colors = new M2Color[this.header.getColors().getCount()];
            for (int i = 0; i < this.header.getColors().getCount(); i++) {
                log.debug(" Color #" + i + ":");
                this.colors[i] = new M2Color(bb);
            }
        }
        this.textures = new ArrayList<M2Texture>();
        log.debug("Textures[" + this.header.getTextures().getCount() + "]:");
        if (this.header.getTextures().getCount() > 0) {
            bb.position(this.header.getTextures().getOffset());
            for (int i = 0; i < this.header.getTextures().getCount(); i++) {
                log.debug(" Texture #" + i + ":");
                this.textures.add(new M2Texture(bb));
            }
        }
        log.debug("Transparencies[" + this.header.getTransparencies().getCount() + "]:");
        this.transparencies = BufferUtils.getShortArray(bb, this.header.getTransparencies());
        for (int i = 0; i < this.header.getTransparencies().getCount(); i++) {
            log.debug(" Transparencies #" + i + "[" + this.transparencies[i] + "]");
        }
        log.debug("Texture Animations[" + this.header.getTextureAnimations().getCount() + "]:");
        if (this.header.getTextureAnimations().getCount() >= 0) {
            bb.position(this.header.getTextureAnimations().getOffset());
            this.textureAnimations = new M2TextureAnimation[this.header.getTextureAnimations().getCount()];
            for (int i = 0; i < this.header.getTextureAnimations().getCount(); i++) {
                log.debug("  TextureAnimation #" + i + ":");
                this.textureAnimations[i] = new M2TextureAnimation(bb);
            }
        }
        log.debug("Triangles[" + this.header.getBoundingTriangles().getCount() + "]");
        if (this.header.getBoundingTriangles().getCount() > 0) {
        }
        log.debug("Loading views[" + this.header.getViews() + "]");
        this.skins = this.loadSkins();
    }

    /**
	 * @return the textures
	 */
    public List<M2Texture> getTextures() {
        return textures;
    }

    /**
	 * @return the header
	 */
    public M2FileHeader getHeader() {
        return header;
    }

    /**
	 * @return the textureLookups
	 */
    public short[] getTextureLookups() {
        return textureLookups;
    }

    /**
	 * Get the bones.
	 * 
	 * @return the bones
	 */
    public List<M2Bone> getBones() {
        return bones;
    }

    /**
	 * Get the key bone lookups.
	 * 
	 * @return
	 */
    public short[] getKeyBoneLookups() {
        return this.keyBoneLookups;
    }

    /**
	 * Get the bone lookups.
	 * 
	 * @return
	 */
    public short[] getBoneLookups() {
        return this.boneLookups;
    }

    /**
	 * @return the animationSequences
	 */
    public List<M2AnimationSequence> getAnimationSequences() {
        return animationSequences;
    }

    /**
	 * @return the animationLookups
	 */
    public short[] getAnimationLookups() {
        return animationLookups;
    }

    /**
	 * @return the colors
	 */
    public M2Color[] getColors() {
        return colors;
    }

    /**
	 * @return the transparencies
	 */
    public short[] getTransparencies() {
        return transparencies;
    }

    /**
	 * @return the textureAnimations
	 */
    public M2TextureAnimation[] getTextureAnimations() {
        return textureAnimations;
    }
}
