package org.xith3d.loaders.models.impl.cal3d.core;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import org.xith3d.loaders.models.impl.cal3d.buffer.TexCoord2fBuffer;
import org.xith3d.loaders.models.impl.cal3d.buffer.Vector3fBuffer;
import org.xith3d.loaders.models.impl.cal3d.util.LittleEndianDataOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/** Provides static methods for writing Cal3D elements to backing store.
 *****************************************************************************/
public class CalSaver {

    /** Saves a core animation instance.
     *
     * This function saves a core animation instance to a stream.
     *****************************************************************************/
    public static void saveCoreAnimation(OutputStream file, CalCoreAnimation coreAnimation) throws IOException {
        LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(file);
        out.write(CalLoader.ANIMATION_FILE_MAGIC);
        out.writeInt(CalLoader.CURRENT_FILE_VERSION);
        out.writeFloat(coreAnimation.getDuration());
        List<CalCoreTrack> listCoreTrack = coreAnimation.getListCoreTrack();
        out.writeInt(listCoreTrack.size());
        for (CalCoreTrack track : listCoreTrack) {
            saveCoreTrack(out, track);
        }
    }

    /** Saves a core bone instance.
     *
     * This function saves a core bone instance to a file stream.
     *
     * @param file The file stream to save the core bone instance to.
     * @param coreBone A pointer to the core bone instance that should be saved.
     *****************************************************************************/
    protected static void saveCoreBones(DataOutput file, CalCoreBone coreBone) throws IOException {
        CalPlatform.writeString(file, coreBone.getName());
        Vector3f translation = coreBone.getTranslation();
        file.writeFloat(translation.x);
        file.writeFloat(translation.y);
        file.writeFloat(translation.z);
        Quat4f rotation = coreBone.getRotation();
        file.writeFloat(rotation.x);
        file.writeFloat(rotation.y);
        file.writeFloat(rotation.z);
        file.writeFloat(rotation.w);
        Vector3f translationBoneSpace = coreBone.getTranslationBoneSpace();
        file.writeFloat(translationBoneSpace.x);
        file.writeFloat(translationBoneSpace.y);
        file.writeFloat(translationBoneSpace.z);
        Quat4f rotationBoneSpace = coreBone.getRotationBoneSpace();
        file.writeFloat(rotationBoneSpace.x);
        file.writeFloat(rotationBoneSpace.y);
        file.writeFloat(rotationBoneSpace.z);
        file.writeFloat(rotationBoneSpace.w);
        file.writeInt(coreBone.getParentId());
        List<Integer> listChildId = coreBone.getListChildId();
        file.writeInt(listChildId.size());
        for (int childId : listChildId) {
            file.writeInt(childId);
        }
    }

    /** Saves a core keyframe instance.
     *
     * This function saves a core keyframe instance to a file stream.
     *
     * @param file The file stream to save the core keyframe instance to.
     * @param coreKeyframe A pointer to the core keyframe instance that should be
     *                      saved.
     *****************************************************************************/
    protected static void saveCoreKeyframe(DataOutput file, CalCoreKeyframe coreKeyframe) throws IOException {
        file.writeFloat(coreKeyframe.getTime());
        Vector3f translation = coreKeyframe.getTranslation();
        file.writeFloat(translation.x);
        file.writeFloat(translation.y);
        file.writeFloat(translation.z);
        Quat4f rotation = coreKeyframe.getRotation();
        file.writeFloat(rotation.x);
        file.writeFloat(rotation.y);
        file.writeFloat(rotation.z);
        file.writeFloat(rotation.w);
    }

    /** Saves a core material instance.
     *
     * This function saves a core material instance to a file.
     *
     * @param file The name of the file to save the core material instance
     *                    to.
     * @param coreMaterial A pointer to the core material instance that should
     *                      be saved.
     *****************************************************************************/
    public static void saveCoreMaterial(OutputStream file, CalCoreMaterial coreMaterial) throws IOException {
        LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(file);
        out.write(CalLoader.MATERIAL_FILE_MAGIC);
        out.writeInt(CalLoader.CURRENT_FILE_VERSION);
        CalPlatform.writeColour(out, coreMaterial.getAmbientColor());
        CalPlatform.writeColour(out, coreMaterial.getDiffuseColor());
        CalPlatform.writeColour(out, coreMaterial.getSpecularColor());
        out.writeFloat(coreMaterial.getShininess());
        CalCoreMaterial.Map[] vectorMap = coreMaterial.getMaps();
        out.writeInt(vectorMap.length);
        for (CalCoreMaterial.Map map : vectorMap) {
            CalPlatform.writeString(out, map.filename);
        }
    }

    /** Saves a core mesh instance.
     *
     * This function saves a core mesh instance to a file.
     *
     * @param file The name of the file to save the core mesh instance to.
     * @param coreMesh A pointer to the core mesh instance that should be saved.
     *****************************************************************************/
    public static void saveCoreMesh(OutputStream file, CalCoreMesh coreMesh) throws IOException {
        LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(file);
        out.write(CalLoader.MESH_FILE_MAGIC);
        out.writeInt(CalLoader.CURRENT_FILE_VERSION);
        Vector<CalCoreSubmesh> vectorCoreSubmesh = coreMesh.getVectorCoreSubmesh();
        out.writeInt(vectorCoreSubmesh.size());
        for (CalCoreSubmesh submesh : vectorCoreSubmesh) {
            saveCoreSubmesh(out, submesh);
        }
    }

    /** Saves a core skeleton instance.
     *
     * This function saves a core skeleton instance to a file.
     *
     * @param file The name of the file to save the core skeleton instance
     *                    to.
     * @param coreSkeleton A pointer to the core skeleton instance that should be
     *                      saved.
     *****************************************************************************/
    public static void saveCoreSkeleton(OutputStream file, CalCoreSkeleton coreSkeleton) throws IOException {
        LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(file);
        out.write(CalLoader.SKELETON_FILE_MAGIC);
        out.writeInt(CalLoader.CURRENT_FILE_VERSION);
        out.writeInt(coreSkeleton.getCoreBones().size());
        for (CalCoreBone bone : coreSkeleton.getCoreBones()) {
            saveCoreBones(out, bone);
        }
    }

    /** Saves a core submesh instance.
     *
     * This function saves a core submesh instance to a file stream.
     *
     * @param out The file stream to save the core submesh instance to.
     * @param coreSubmesh A pointer to the core submesh instance that should be
     *                     saved.
     *****************************************************************************/
    protected static void saveCoreSubmesh(DataOutput out, CalCoreSubmesh coreSubmesh) throws IOException {
        out.writeInt(coreSubmesh.getCoreMaterialThreadId());
        CalCoreSubmesh.VertexInfo[] vectorVertex = coreSubmesh.getVectorVertexInfo();
        Vector3fBuffer vertexPositions = coreSubmesh.getVertexPositions();
        Vector3fBuffer vertexNormals = coreSubmesh.getVertexNormals();
        CalCoreSubmesh.Face[] vectorFace = coreSubmesh.getVectorFace();
        float[] vectorPhysicalProperty = coreSubmesh.getVectorPhysicalProperty();
        CalCoreSubmesh.Spring[] vectorSpring = coreSubmesh.getVectorSpring();
        out.writeInt(vectorVertex.length);
        out.writeInt(vectorFace.length);
        out.writeInt(coreSubmesh.getLodCount());
        out.writeInt(coreSubmesh.getSpringCount());
        TexCoord2fBuffer[] textureCoordinates = coreSubmesh.getTextureCoordinates();
        out.writeInt(textureCoordinates.length);
        for (int vertexId = 0; vertexId < vectorVertex.length; vertexId++) {
            CalCoreSubmesh.VertexInfo vertexInfo = vectorVertex[vertexId];
            Vector3f vertexPosition = vertexPositions.get(vertexId);
            Vector3f vertexNormal = vertexNormals.get(vertexId);
            out.writeFloat(vertexPosition.x);
            out.writeFloat(vertexPosition.y);
            out.writeFloat(vertexPosition.z);
            out.writeFloat(vertexNormal.x);
            out.writeFloat(vertexNormal.y);
            out.writeFloat(vertexNormal.z);
            out.writeInt(vertexInfo.collapseId);
            out.writeInt(vertexInfo.faceCollapseCount);
            for (int textureCoordinateId = 0; textureCoordinateId < textureCoordinates.length; textureCoordinateId++) {
                Vector2f texCoord = textureCoordinates[textureCoordinateId].get(vertexId);
                out.writeFloat(texCoord.x);
                out.writeFloat(texCoord.y);
            }
            out.writeInt(vertexInfo.influenceBoneIds.length);
            for (int influenceId = 0; influenceId < vertexInfo.influenceBoneIds.length; influenceId++) {
                out.writeInt(vertexInfo.influenceBoneIds[influenceId]);
                out.writeFloat(vertexInfo.influenceWeights[influenceId]);
            }
            if (coreSubmesh.getSpringCount() > 0) {
                out.writeFloat(vectorPhysicalProperty[vertexId]);
            }
        }
        for (int springId = 0; springId < coreSubmesh.getSpringCount(); springId++) {
            CalCoreSubmesh.Spring spring = vectorSpring[springId];
            out.writeInt(spring.vertexId0);
            out.writeInt(spring.vertexId1);
            out.writeFloat(spring.springCoefficient);
            out.writeFloat(spring.idleLength);
        }
        for (int faceId = 0; faceId < vectorFace.length; faceId++) {
            CalCoreSubmesh.Face face = vectorFace[faceId];
            out.writeInt(face.vertexId[0]);
            out.writeInt(face.vertexId[1]);
            out.writeInt(face.vertexId[2]);
        }
    }

    /** Saves a core track instance.
     *
     * This function saves a core track instance to a file stream.
     *
     * @param file The file stream to save the core track instance to.
     * @param coreTrack A pointer to the core track instance that should be saved.
     *****************************************************************************/
    protected static void saveCoreTrack(DataOutput file, CalCoreTrack coreTrack) throws IOException {
        file.writeInt(coreTrack.getCoreBoneId());
        Set<CalCoreKeyframe> mapCoreKeyframe = coreTrack.getCoreKeyFrames();
        file.writeInt(mapCoreKeyframe.size());
        for (CalCoreKeyframe keyframe : mapCoreKeyframe) {
            saveCoreKeyframe(file, keyframe);
        }
    }
}
