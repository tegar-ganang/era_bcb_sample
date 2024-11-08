package espresso3d.engine.fileloaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import espresso3d.engine.E3DEngine;
import espresso3d.engine.fileloaders.base.E3DGeometryLoader;
import espresso3d.engine.logger.E3DEngineLogger;
import espresso3d.engine.lowlevel.geometry.E3DTriangle;
import espresso3d.engine.lowlevel.map.E3DHashListMap;
import espresso3d.engine.lowlevel.string.E3DStringHelper;
import espresso3d.engine.lowlevel.vector.E3DVector3F;
import espresso3d.engine.world.sector.actor.E3DActor;
import espresso3d.engine.world.sector.actor.skeleton.E3DBone;
import espresso3d.engine.world.sector.actor.skeleton.animation.E3DAnimation;
import espresso3d.engine.world.sector.actor.skeleton.animation.E3DAnimationCommand;
import espresso3d.engine.world.sector.actor.skeleton.animation.E3DAnimationCommandRotate;
import espresso3d.engine.world.sector.actor.skeleton.animation.E3DAnimationCommandTranslate;
import espresso3d.engine.world.sector.actor.skeleton.animation.E3DAnimationKeyFrame;

/**
 * @author espresso3d
 *
 * Used to load an actors geometry and textures into an actor object.
 */
public class E3DActorLoader extends E3DGeometryLoader {

    private static final String ACTORTAG_TEXTURESET = "TEXTURESET";

    private static final String ACTORTAG_MODEL = "MODEL";

    private static final String ACTORTAG_BONE = "BONE";

    private static final String ACTORTAG_TRIANGLE = "TRIANGLE";

    private static final String ACTORTAG_ANIMATION = "ANIMATION";

    private static final String ACTORTAG_ANIMATION_FRAME = "FRAME";

    private static final String ACTORTAG_ANIMATION_FRAME_ROTATE = "ROTATE";

    private static final String ACTORTAG_ANIMATION_FRAME_TRANSLATE = "TRANSLATE";

    public static void loadActor(String actorFileName, E3DActor actor) throws Exception {
        InputStream is = null;
        BufferedReader actorFile = null;
        try {
            is = openFile(actorFileName);
            actorFile = new BufferedReader(new InputStreamReader(is));
            readActor(actorFile, actor);
        } finally {
            if (actorFile != null) actorFile.close();
            if (is != null) is.close();
        }
    }

    public static void readActor(BufferedReader actorFile, E3DActor actor) throws Exception {
        StringTokenizer tokenizer = null;
        String line = "";
        String str;
        while ((line = actorFile.readLine()) != null) {
            tokenizer = new StringTokenizer(line, TOKENIZER_CHARS);
            while (tokenizer.hasMoreTokens()) {
                str = readNextValue(tokenizer);
                if (ACTORTAG_TEXTURESET.equals(str)) {
                    String path = tokenizer.nextToken();
                    String jar = tokenizer.nextToken();
                    actor.getWorld().loadTextureSet(path);
                } else if (ACTORTAG_MODEL.equals(str)) {
                    if (!readActor(actorFile, tokenizer, actor)) actor.getEngine().getLogger().writeLine(E3DEngineLogger.SEVERITY_ERROR, "Unable to read actor model: " + actorFile);
                } else if (COMMENT.equalsIgnoreCase(str)) {
                    break;
                }
            }
        }
    }

    private static boolean readActor(BufferedReader actorFile, StringTokenizer tokenizer, E3DActor actor) throws IOException {
        String str;
        String line;
        boolean startBlockFound = false;
        boolean endBlockFound = false;
        E3DHashListMap orphanedBones = new E3DHashListMap();
        HashMap orphanedTriangleBoneAttachments = new HashMap();
        while (!endBlockFound && (line = actorFile.readLine()) != null) {
            tokenizer = new StringTokenizer(line, TOKENIZER_CHARS);
            while (!endBlockFound && tokenizer.hasMoreTokens()) {
                str = readNextValue(tokenizer);
                if (START_BLOCK.equals(str)) {
                    startBlockFound = true;
                } else if (ACTORTAG_BONE.equals(str)) {
                    if (!startBlockFound) return false;
                    String boneID = readNextValue(tokenizer);
                    String parentBoneID = readNextValue(tokenizer);
                    E3DBone bone = readBone(actor.getEngine(), actorFile, boneID);
                    if (bone != null) {
                        if (!actor.getSkeleton().addBone(bone, parentBoneID)) orphanedBones.put(parentBoneID, bone); else {
                            linkOrphanedBonesToNewBone(actor, bone, orphanedBones);
                        }
                    } else actor.getEngine().getLogger().writeLine(E3DEngineLogger.SEVERITY_WARNING, "Unable to read bone: " + E3DStringHelper.getNonNullableString(boneID) + ".  Possibly missing attributes or malformed definition.");
                } else if (ACTORTAG_TRIANGLE.equals(str)) {
                    if (!startBlockFound) return false;
                    TempTriangle tempTriangle = readTriangle(actor.getEngine(), actorFile);
                    if (tempTriangle != null) {
                        E3DTriangle triangle = tempTriangle.triangle;
                        VertexBoneInformation boneInformation = tempTriangle.vertexBoneInformation;
                        if (triangle != null) {
                            actor.getMesh().addTriangle(triangle);
                            if (boneInformation != null) orphanedTriangleBoneAttachments.put(triangle, boneInformation);
                        }
                    }
                } else if (ACTORTAG_ANIMATION.equals(str)) {
                    if (!startBlockFound) return false;
                    String animationName = readNextValue(tokenizer);
                    String fps = readNextValue(tokenizer);
                    double dFPS = 20.0;
                    try {
                        dFPS = Double.parseDouble(fps);
                    } catch (NumberFormatException e) {
                        actor.getEngine().getLogger().writeLine(E3DEngineLogger.SEVERITY_WARNING, "No FPS specified for animation.  20 FPS will be used by default (1 frame == 0.5s)");
                    }
                    if (dFPS == 0) dFPS = 20.0;
                    E3DAnimation animation = readAnimation(actor.getEngine(), animationName, dFPS, actorFile);
                    if (animation != null) actor.getSkeleton().addAnimation(animation);
                } else if (COMMENT.equals(str)) break; else if (END_BLOCK.equals(str)) endBlockFound = true; else actor.getEngine().getLogger().writeLine(E3DEngineLogger.SEVERITY_INFO, "Unknown tag in actor model definition: " + str);
            }
        }
        linkTrianglesToBones(actor, orphanedTriangleBoneAttachments);
        return endBlockFound;
    }

    private static E3DBone readBone(E3DEngine engine, BufferedReader actorFile, String boneID) throws IOException {
        String line;
        StringTokenizer tokenizer;
        String str;
        boolean startBlockFound = false;
        E3DBone bone;
        E3DVector3F position = null, forward = null, up = null;
        boolean endBlock = false;
        while (!endBlock && (line = actorFile.readLine()) != null) {
            tokenizer = new StringTokenizer(line, TOKENIZER_CHARS);
            while (!endBlock && tokenizer.hasMoreTokens()) {
                str = readNextValue(tokenizer);
                if (START_BLOCK.equals(str)) {
                    startBlockFound = true;
                } else if (POSITION.equals(str)) {
                    if (!startBlockFound) continue;
                    String subLine = actorFile.readLine();
                    position = readVector3F(subLine);
                } else if (FORWARD.equals(str)) {
                    if (!startBlockFound) continue;
                    String subLine = actorFile.readLine();
                    forward = readVector3F(subLine);
                } else if (UP.equals(str)) {
                    if (!startBlockFound) continue;
                    String subLine = actorFile.readLine();
                    up = readVector3F(subLine);
                } else if (COMMENT.equals(str)) break; else if (END_BLOCK.equals(str)) endBlock = true; else engine.getLogger().writeLine(E3DEngineLogger.SEVERITY_INFO, "Unknown tag in bone: " + str);
            }
        }
        if (position != null && forward != null && up != null) {
            bone = new E3DBone(engine, boneID, position, forward, up);
            return bone;
        }
        return null;
    }

    private static E3DAnimation readAnimation(E3DEngine engine, String animationID, double fps, BufferedReader actorFile) throws IOException {
        boolean endBlockFound = false;
        boolean startBlockFound = false;
        StringTokenizer tokenizer;
        String line, str;
        E3DAnimation animation = new E3DAnimation(engine, animationID);
        while (!endBlockFound && (line = actorFile.readLine()) != null) {
            tokenizer = new StringTokenizer(line, TOKENIZER_CHARS);
            while (!endBlockFound && tokenizer.hasMoreTokens()) {
                str = readNextValue(tokenizer);
                if (START_BLOCK.equals(str)) {
                    startBlockFound = true;
                } else if (ACTORTAG_ANIMATION_FRAME.equals(str)) {
                    if (!startBlockFound) continue;
                    String frameID = readNextValue(tokenizer);
                    E3DAnimationKeyFrame frame = readAnimationFrame(engine, animationID, frameID, fps, animation, actorFile);
                    if (frame != null) animation.addKeyFrame(frame);
                } else if (END_BLOCK.equals(str)) {
                    endBlockFound = true;
                } else engine.getLogger().writeLine(E3DEngineLogger.SEVERITY_INFO, "Unknown tag in animation: " + str);
            }
        }
        if (endBlockFound) return animation;
        return null;
    }

    private static E3DAnimationKeyFrame readAnimationFrame(E3DEngine engine, String animationID, String frameID, double fps, E3DAnimation animation, BufferedReader actorFile) throws IOException {
        boolean endBlockFound = false;
        boolean startBlockFound = false;
        StringTokenizer tokenizer;
        String line, str;
        double frameTime = 1.0 / fps;
        E3DAnimationKeyFrame animationKeyFrame = new E3DAnimationKeyFrame(engine, frameID, frameTime);
        while (!endBlockFound && (line = actorFile.readLine()) != null) {
            tokenizer = new StringTokenizer(line, TOKENIZER_CHARS);
            while (!endBlockFound && tokenizer.hasMoreTokens()) {
                str = readNextValue(tokenizer);
                if (START_BLOCK.equals(str)) {
                    startBlockFound = true;
                } else if (ACTORTAG_BONE.equals(str)) {
                    if (!startBlockFound) continue;
                    String manipulatedBoneID = readNextValue(tokenizer);
                    addAnimationCommands(engine, manipulatedBoneID, animationKeyFrame, actorFile);
                } else if (END_BLOCK.equals(str)) {
                    endBlockFound = true;
                } else engine.getLogger().writeLine(E3DEngineLogger.SEVERITY_INFO, "Unknown tag in animation frame: " + str);
            }
        }
        if (!endBlockFound) return null;
        return animationKeyFrame;
    }

    private static boolean addAnimationCommands(E3DEngine engine, String manipulatedBoneID, E3DAnimationKeyFrame animationKeyFrame, BufferedReader actorFile) throws IOException {
        boolean endBlockFound = false;
        boolean startBlockFound = false;
        StringTokenizer tokenizer;
        String line, str;
        E3DAnimationCommand command;
        while (!endBlockFound && (line = actorFile.readLine()) != null) {
            tokenizer = new StringTokenizer(line, TOKENIZER_CHARS);
            while (!endBlockFound && tokenizer.hasMoreTokens()) {
                str = readNextValue(tokenizer);
                if (START_BLOCK.equals(str)) {
                    startBlockFound = true;
                } else if (ACTORTAG_ANIMATION_FRAME_ROTATE.equals(str)) {
                    if (!startBlockFound) continue;
                    String subLine = actorFile.readLine();
                    StringTokenizer rotateTokenizer = new StringTokenizer(subLine, TOKENIZER_CHARS);
                    String fSpan = readNextValue(rotateTokenizer);
                    int frameSpan = Integer.parseInt(fSpan);
                    E3DVector3F rotation = readVector3F(rotateTokenizer);
                    command = new E3DAnimationCommandRotate(engine, manipulatedBoneID, frameSpan * animationKeyFrame.getFrameLength(), rotation);
                    animationKeyFrame.addAnimationCommand(command);
                } else if (ACTORTAG_ANIMATION_FRAME_TRANSLATE.equals(str)) {
                    if (!startBlockFound) continue;
                    String subLine = actorFile.readLine();
                    StringTokenizer translateTokenizer = new StringTokenizer(subLine, TOKENIZER_CHARS);
                    String fTime = readNextValue(translateTokenizer);
                    double dfTime = Double.parseDouble(fTime);
                    E3DVector3F translation = readVector3F(translateTokenizer);
                    command = new E3DAnimationCommandTranslate(engine, manipulatedBoneID, dfTime, translation);
                    animationKeyFrame.addAnimationCommand(command);
                } else if (END_BLOCK.equals(str)) {
                    endBlockFound = true;
                } else engine.getLogger().writeLine(E3DEngineLogger.SEVERITY_INFO, "Unknown tag in animation frame command listing: " + str);
            }
        }
        if (endBlockFound && animationKeyFrame.getCommands().isEmpty()) return false; else if (endBlockFound) return true;
        return false;
    }

    private static void linkOrphanedBonesToNewBone(E3DActor actor, E3DBone possibleParentBone, E3DHashListMap orphanedBones) {
        if (orphanedBones.containsKey(possibleParentBone.getBoneID())) {
            ArrayList boneList = (ArrayList) orphanedBones.get(possibleParentBone.getBoneID());
            for (int i = 0; i < boneList.size(); i++) {
                E3DBone childBone = (E3DBone) boneList.get(i);
                actor.getSkeleton().addBone(childBone, possibleParentBone.getBoneID());
                linkOrphanedBonesToNewBone(actor, childBone, orphanedBones);
            }
            orphanedBones.remove(possibleParentBone.getBoneID());
        }
    }

    private static void linkTrianglesToBones(E3DActor actor, HashMap orphanedTriangleBoneAttachments) {
        ArrayList triangleList = actor.getTriangleList();
        E3DTriangle triangle = null;
        for (int i = 0; i < triangleList.size(); i++) {
            triangle = (E3DTriangle) triangleList.get(i);
            if (orphanedTriangleBoneAttachments.containsKey(triangle)) {
                VertexBoneInformation boneInfo = (VertexBoneInformation) orphanedTriangleBoneAttachments.get(triangle);
                E3DBone bone;
                for (int b = 0; b < 3; b++) {
                    bone = actor.getSkeleton().findBoneByID(boneInfo.boneIDs[b]);
                    if (bone != null) bone.attachVertex(triangle.getVertex(b)); else actor.getEngine().getLogger().writeLine(E3DEngineLogger.SEVERITY_WARNING, "The bone: " + E3DStringHelper.getNonNullableString(boneInfo.boneIDs[b]) + " could not be found in the actor thus vertices were not linked to it.");
                }
            }
        }
    }
}
