package org.xith3d.loaders.models.animations;

import java.util.Map;
import org.jagatoo.datatypes.NamedObject;
import org.jagatoo.loaders.models._util.AnimationType;
import org.xith3d.scenegraph.Geometry;
import org.xith3d.scenegraph.Shape3D;

/**
 * Insert type comment here.
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class MeshDeformationKeyFrameController extends KeyFrameController {

    private final Shape3D shape;

    public final MeshDeformationKeyFrame getFrame(int index) {
        return ((MeshDeformationKeyFrame) getKeyFrame(index));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Shape3D getTarget() {
        return (shape);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateTarget(float absAnimTime, int baseFrameIndex, int nextFrameIndex, float alpha, ModelAnimation animation) {
        MeshDeformationKeyFrame baseFrame = getFrame(baseFrameIndex);
        MeshDeformationKeyFrame nextFrame = getFrame(nextFrameIndex);
        float[] coords0 = baseFrame.getCoordinates();
        float[] coords1 = nextFrame.getCoordinates();
        Geometry geom = shape.getGeometry();
        int j;
        float[] buffer = new float[3];
        if (baseFrame == nextFrame) {
            geom.setCoordinates(0, coords0);
        } else {
            j = 0;
            for (int i = 0; i < coords0.length; i += 3) {
                buffer[0] = coords0[i + 0] + ((coords1[i + 0] - coords0[i + 0]) * alpha);
                buffer[1] = coords0[i + 1] + ((coords1[i + 1] - coords0[i + 1]) * alpha);
                buffer[2] = coords0[i + 2] + ((coords1[i + 2] - coords0[i + 2]) * alpha);
                geom.setCoordinate(j++, buffer);
            }
        }
        if (baseFrame.getNormals() != null) {
            float[] normals0 = baseFrame.getNormals();
            float[] normals1 = nextFrame.getNormals();
            if (baseFrame == nextFrame) {
                geom.setNormals(0, normals0);
            } else {
                j = 0;
                for (int i = 0; i < normals0.length; i += 3) {
                    buffer[0] = normals0[i + 0] + ((normals1[i + 0] - normals0[i + 0]) * alpha);
                    buffer[1] = normals0[i + 1] + ((normals1[i + 1] - normals0[i + 1]) * alpha);
                    buffer[2] = normals0[i + 2] + ((normals1[i + 2] - normals0[i + 2]) * alpha);
                    geom.setNormal(j++, buffer);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MeshDeformationKeyFrameController sharedCopy(Map<String, NamedObject> namedObjects) {
        String shapeName = this.shape.getName();
        Shape3D newShape = (Shape3D) namedObjects.get(shapeName);
        if (newShape == null) throw new Error("Can't clone this AnimationController!");
        return (new MeshDeformationKeyFrameController((MeshDeformationKeyFrame[]) this.getKeyFrames(), newShape));
    }

    public MeshDeformationKeyFrameController(MeshDeformationKeyFrame[] frames, Shape3D shape) {
        super(AnimationType.MESH_DEFORMATION, frames);
        this.shape = shape;
    }
}
