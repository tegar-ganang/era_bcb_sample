package meraner81.jets.processing.parser.mixdown.handlers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Color3f;
import meraner81.jets.analysis.AnalysisDetail;
import meraner81.jets.analysis.MapAnalysis;
import meraner81.jets.processing.apps.AutoShader;
import meraner81.jets.processing.parser.mapparser.Q3MapParser;
import meraner81.jets.processing.parser.mixdown.ConfigurationParser;
import meraner81.jets.processing.parser.mixdown.HandleParams;
import meraner81.jets.processing.parser.mixdown.Q3MapProject;
import meraner81.jets.processing.parser.mixdown.filters.Q3MapFilters;
import meraner81.jets.processing.parser.mixdown.transformation.ColorTransformation;
import meraner81.jets.processing.parser.mixdown.transformation.Transformations;
import meraner81.jets.processing.parser.mixdown.utils.CopyTask;
import meraner81.jets.processing.parser.mixdown.utils.CopyTasks;
import meraner81.jets.processing.parser.script.ScriptBlock;
import meraner81.jets.processing.parser.script.ScriptParser;
import meraner81.jets.processing.parser.shader.AutoShaderInfo;
import meraner81.jets.processing.parser.shader.ColorShader;
import meraner81.jets.processing.parser.shader.ShaderInfo;
import meraner81.jets.processing.parser.shader.ShaderInstructions;
import meraner81.jets.processing.parser.shader.ShaderItem;
import meraner81.jets.processing.parser.shader.ShaderItemList;
import meraner81.jets.processing.parser.shader.ShaderParser;
import meraner81.jets.processing.parser.texture.TextureInfo;
import meraner81.jets.processing.parser.texture.TextureParser;
import meraner81.jets.processing.parser.utils.AutoSkyBox;
import meraner81.jets.shared.model.Brush;
import meraner81.jets.shared.model.Entity;
import meraner81.jets.shared.model.Point;
import meraner81.jets.shared.model.Q3Map;
import meraner81.jets.shared.util.Compilation;
import meraner81.jets.shared.util.MapWriter;
import meraner81.jets.shared.util.OperatingSystem;
import meraner81.jets.shared.util.TemplateFileParser;
import meraner81.utilities.io.FileUtils;
import meraner81.utilities.lang.StringUtils;

public class MixdownHandlerDefault implements IMixdownHandler {

    @SuppressWarnings("unchecked")
    public Q3MapProject handleQ3MapFound(Map<HandleParams, Object> handleParameters) throws Exception {
        String path = (String) handleParameters.get(HandleParams.HANDLEPARAM_PATH);
        String mapName = (String) handleParameters.get(HandleParams.HANDLEPARAM_MAPNAME);
        Q3MapFilters filters = (Q3MapFilters) handleParameters.get(HandleParams.HANDLEPARAM_FILTERS);
        Transformations transformations = (Transformations) handleParameters.get(HandleParams.HANDLEPARAM_TRANSFORMATIONS);
        List<ShaderInfo> shaderInfoList = (List<ShaderInfo>) handleParameters.get(HandleParams.HANDLEPARAM_SHADERINFO);
        CopyTasks copyTasks = (CopyTasks) handleParameters.get(HandleParams.HANDLEPARAM_COPYTASKS);
        Q3MapProject retVal = new Q3MapProject();
        mapName = adjustMapName(mapName);
        String pureMapName = pureMapName(mapName);
        Q3MapParser parser = new Q3MapParser(path + MAPS_FOLDER + mapName);
        List<Entity> entities = parser.parseFile();
        if (filters != null) {
            filters.applyToEntities(entities);
        }
        if (transformations != null) {
            transformations.applyToEntities(entities);
        }
        Q3Map map = new Q3Map(entities);
        Map<String, String> params = prepareParameters(map);
        Entity entity;
        for (int i = 0; i < entities.size(); i++) {
            entity = entities.get(i);
            entity.applyParameters(params);
        }
        String scriptFileName = path + MAPS_FOLDER + pureMapName + SCRIPT_FILE_EXTENSION;
        File scriptFile = new File(scriptFileName);
        if (scriptFile.exists()) {
            ScriptParser scriptParser = new ScriptParser(scriptFileName);
            List<ScriptBlock> scriptBlocks = scriptParser.parse();
            if (filters != null) {
                filters.applyToScriptBlocks(scriptBlocks);
            }
            map.addScriptBlocks(scriptBlocks);
        }
        retVal.setMap(map);
        retVal.setShaderInfoList(shaderInfoList);
        retVal.setCopyTasks(copyTasks);
        if (copyTasks != null) {
            for (CopyTask copyTask : copyTasks) {
                copyTask.setParentDirectory(path);
            }
        }
        return retVal;
    }

    private Map<String, String> prepareParameters(Q3Map map) {
        Map<String, String> retVal = new HashMap<String, String>();
        Point lower = map.getLowerBoundPoint();
        Point upper = map.getUpperBoundPoint();
        retVal.put(ConfigurationParser.PARAM_INMAP_PREFIX + "minX", String.valueOf(lower.getX()));
        retVal.put(ConfigurationParser.PARAM_INMAP_PREFIX + "minY", String.valueOf(lower.getY()));
        retVal.put(ConfigurationParser.PARAM_INMAP_PREFIX + "maxX", String.valueOf(upper.getX()));
        retVal.put(ConfigurationParser.PARAM_INMAP_PREFIX + "maxY", String.valueOf(upper.getY()));
        return retVal;
    }

    private String adjustMapName(String mapName) {
        String retVal = mapName;
        if (!retVal.endsWith(MAP_FILE_EXTENSION)) {
            retVal += MAP_FILE_EXTENSION;
        }
        return retVal;
    }

    @SuppressWarnings("unchecked")
    public void handleDestinationFound(Map<HandleParams, Object> handleParameters) throws Exception {
        String path = (String) handleParameters.get(HandleParams.HANDLEPARAM_PATH);
        String mapName = (String) handleParameters.get(HandleParams.HANDLEPARAM_MAPNAME);
        List<Q3MapProject> inputMapProjects = (List<Q3MapProject>) handleParameters.get(HandleParams.HANDLEPARAM_INPUTMAPPROJECTS);
        Compilation compilation = (Compilation) handleParameters.get(HandleParams.HANDLEPARAM_COMPILATION);
        String arenaTemplateFile = (String) handleParameters.get(HandleParams.HANDLEPARAM_ARENATEMPLATE);
        String scriptName = (String) handleParameters.get(HandleParams.HANDLEPARAM_SCRIPTNAME);
        String scriptFile = (String) handleParameters.get(HandleParams.HANDLEPARAM_SCRIPTFILE);
        String objdataFile = (String) handleParameters.get(HandleParams.HANDLEPARAM_OBJDATA);
        AutoSkyBox autoSkyBox = (AutoSkyBox) handleParameters.get(HandleParams.HANDLEPARAM_AUTOSKYBOX);
        ShaderInstructions shaderInstructions = (ShaderInstructions) handleParameters.get(HandleParams.HANDLEPARAM_SHADERINSTRUCTIONS);
        boolean doCopy = ((Boolean) handleParameters.get(HandleParams.HANDLEPARAM_DOCOPY));
        mapName = adjustMapName(mapName);
        String pureMapName = pureMapName(mapName);
        Q3MapProject tmpMapProject;
        Q3Map tmpMap;
        Entity worldSpawn;
        Entity tmpSpawn;
        List<Entity> tmpEntityList;
        List<Entity> entityList = new ArrayList<Entity>();
        Q3Map finalMap = new Q3Map(entityList);
        for (int i = 0; i < inputMapProjects.size(); i++) {
            tmpMapProject = inputMapProjects.get(i);
            tmpMap = tmpMapProject.getMap();
            worldSpawn = finalMap.getWorldSpawn();
            if (worldSpawn == null) {
                entityList.addAll(tmpMap.getEntities());
            } else {
                tmpSpawn = tmpMap.getWorldSpawn();
                worldSpawn.mergeWith(tmpSpawn);
                tmpEntityList = tmpMap.getNonWorldSpawnEntities();
                finalMap.getEntities().addAll(tmpEntityList);
            }
        }
        generateAutoSkyBox(finalMap, autoSkyBox);
        generateAndCopyShader(shaderInstructions, finalMap, inputMapProjects, path);
        generateShaderList(path);
        generateMapFile(finalMap, path, mapName);
        generateCompileScripts(compilation, path, mapName);
        generateArenaFile(arenaTemplateFile, path, pureMapName);
        generateScriptFile(scriptFile, scriptName, finalMap, inputMapProjects, path, pureMapName);
        generateObjectiveFile(objdataFile, path, pureMapName);
        if (doCopy == true) {
            performCopyTasks(inputMapProjects, finalMap, path);
        }
    }

    private void generateShaderList(String path) {
        try {
            String scriptFolder = path + IMixdownHandler.SCRIPTS_FOLDER;
            String fileName = scriptFolder + IMixdownHandler.SHADERLIST_FILE;
            String[] shaders = FileUtils.filesInFolderWithExtension(scriptFolder, IMixdownHandler.SHADER_FILE_EXTENSION, false);
            FileWriter fw = FileUtils.getFileWriterFolderEnsured(fileName);
            for (String shaderFile : shaders) {
                shaderFile = shaderFile.substring(shaderFile.lastIndexOf(OperatingSystem.getPathSeparator()) + 1, shaderFile.length());
                fw.write(FileUtils.trimExtension(shaderFile));
                fw.write(OperatingSystem.getLineSeparator());
            }
            fw.write(OperatingSystem.getLineSeparator());
            fw.flush();
            fw.close();
            System.out.println("HANDLER:\tWrote " + fileName + " successfully with " + shaders.length + " shaders.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean performCopyTasks(List<Q3MapProject> inputMapProjects, Q3Map finalMap, String path) {
        boolean success = true;
        for (Q3MapProject project : inputMapProjects) {
            CopyTasks tasks = project.getCopyTasks();
            if (tasks != null) {
                for (CopyTask task : tasks) {
                    success &= task.execute(path);
                }
            }
        }
        return success;
    }

    private void generateAndCopyShader(ShaderInstructions shaderInstructions, Q3Map finalMap, List<Q3MapProject> inputMapProjects, String path) {
        if (shaderInstructions != null && shaderInstructions.isGenerateFiles()) {
            if (shaderInstructions.isRemoveUnused()) {
                boolean doColorShading = (shaderInstructions.getColorShaderName() != null);
                MapAnalysis analysis = new MapAnalysis(finalMap);
                List<AnalysisDetail> textureDetails = analysis.getTextureDetails();
                ColorShader colors = new ColorShader();
                for (Q3MapProject project : inputMapProjects) {
                    List<ShaderInfo> shaderInfoList = project.getShaderInfoList();
                    if (shaderInfoList != null) {
                        for (ShaderInfo info : shaderInfoList) {
                            generateComplexShader(colors, doColorShading, info, textureDetails, path);
                        }
                    }
                }
                if (doColorShading) {
                    Transformations colorTransformer = new Transformations();
                    ColorTransformation colorTranformation = new ColorTransformation(colors);
                    colorTransformer.addTranformation(colorTranformation);
                    colorTransformer.applyToEntities(finalMap.getEntities());
                    StringBuffer colorContent = colors.getShaderFileContent();
                    if (colorContent != null && colorContent.length() > 0) {
                        writerStringBufferToFile(colorContent, path + IMixdownHandler.SCRIPTS_FOLDER + shaderInstructions.getColorShaderName());
                    }
                }
            } else {
                simpleGenerateAndCopyShader(inputMapProjects, path);
            }
        }
    }

    private void generateComplexShader(ColorShader colors, boolean doColorShading, ShaderInfo info, List<AnalysisDetail> textureDetails, String path) {
        TextureParser textureParser = new TextureParser();
        ShaderParser parser = new ShaderParser(info);
        ShaderItemList sil = parser.extractShaderItems();
        Iterator<ShaderItem> shaderItemIterator = sil.iterator();
        TextureInfo textureInfo;
        while (shaderItemIterator.hasNext()) {
            ShaderItem shaderItem = (ShaderItem) shaderItemIterator.next();
            AnalysisDetail detail = null;
            for (AnalysisDetail textureDetail : textureDetails) {
                if (shaderItem.getName().equalsIgnoreCase("textures/" + textureDetail.getName())) {
                    detail = textureDetail;
                    break;
                }
            }
            if (detail != null) {
                if (doColorShading && shaderItem.isImplicitMap()) {
                    textureInfo = textureParser.findTextureInfo(detail.getName(), info);
                    if (textureInfo != null) {
                        if (textureInfo.isWithContentInfo()) {
                            Color3f color = textureInfo.getUniformColor();
                            if (color != null) {
                                colors.addReplacement(detail.getName(), color);
                            }
                        }
                    }
                }
            } else {
                shaderItemIterator.remove();
            }
        }
        writerStringBufferToFile(sil.toShaderText(), path + IMixdownHandler.SCRIPTS_FOLDER + info.retrieveShaderFileName());
    }

    private static boolean writerStringBufferToFile(StringBuffer buffer, String fileName) {
        boolean retVal = false;
        try {
            if (buffer.length() > 0) {
                FileWriter writer = FileUtils.getFileWriterFolderEnsured(fileName);
                PrintWriter printWriter = new PrintWriter(writer);
                printWriter.write(buffer.toString());
                printWriter.flush();
                printWriter.close();
                retVal = true;
            } else {
                System.out.println("Empty content for file " + fileName + ". No file written.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    private void simpleGenerateAndCopyShader(List<Q3MapProject> inputMapProjects, String path) {
        AutoShader autoShader = new AutoShader();
        for (Q3MapProject project : inputMapProjects) {
            List<ShaderInfo> shaderInfoList = project.getShaderInfoList();
            Set<String> fileNameSet = new HashSet<String>();
            if (shaderInfoList != null) {
                for (ShaderInfo info : shaderInfoList) {
                    String shaderFileName = info.retrieveShaderFileName();
                    if (shaderFileName != null) {
                        if (info instanceof AutoShaderInfo) {
                            AutoShaderInfo autoShaderInfo = (AutoShaderInfo) info;
                            autoShader.autoShade(autoShaderInfo, path + IMixdownHandler.SCRIPTS_FOLDER + shaderFileName);
                        } else {
                            if (fileNameSet.contains(shaderFileName)) {
                                String error = FileUtils.copyFile(info.getInputShaderPath(), path + IMixdownHandler.SCRIPTS_FOLDER + shaderFileName);
                                if (StringUtils.notNullOrEmpty(error)) {
                                    System.out.println(error);
                                }
                            } else {
                                System.out.println("Warning: Shader file " + shaderFileName + " had duplicate name and was overwritten.");
                            }
                        }
                    } else {
                        System.out.println("File not found " + info.getInputShaderPath());
                    }
                }
            }
        }
    }

    private void generateObjectiveFile(String objdataFile, String path, String pureMapName) {
        String objdataFileDestination = path + MAPS_FOLDER + pureMapName + OBJECTIVEDATA_FILE_EXTENSION;
        String outcome = FileUtils.copyFile(objdataFile, objdataFileDestination);
        if (outcome != null) {
            System.err.println(outcome);
        }
    }

    private void generateScriptFile(String scriptFile, String scriptName, Q3Map finalMap, List<Q3MapProject> inputMapProjects, String path, String pureMapName) {
        if (scriptFile != null && scriptName != null) {
            String gameManagerCode = FileUtils.file2String(scriptFile);
            ScriptBlock block = new ScriptBlock(scriptName);
            block.addCodeLine(gameManagerCode);
            finalMap.addScriptBlock(block);
            Q3MapProject scriptTmpMapProject;
            Q3Map scriptTmpMap;
            for (int i = 0; i < inputMapProjects.size(); i++) {
                scriptTmpMapProject = inputMapProjects.get(i);
                scriptTmpMap = scriptTmpMapProject.getMap();
                finalMap.addScriptBlocks(scriptTmpMap.getScriptBlocks());
            }
            try {
                finalMap.generateScriptFile(path + MAPS_FOLDER + pureMapName + SCRIPT_FILE_EXTENSION);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean generateArenaFile(String arenaTemplateFile, String path, String pureMapName) {
        TemplateFileParser tfp = new TemplateFileParser(arenaTemplateFile);
        try {
            tfp.addParameter(TEMPLATE_MAPNAME, pureMapName);
            tfp.writeToFile(path + SCRIPTS_FOLDER + pureMapName + ARENA_FILE_EXTENSION);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean generateCompileScripts(Compilation compilation, String path, String mapName) {
        if (compilation != null) {
            try {
                compilation.generateCompiler(path, MAPS_FOLDER + mapName);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private boolean generateMapFile(Q3Map finalMap, String path, String mapName) {
        MapWriter mapWriter = new MapWriter(finalMap.getEntities());
        try {
            mapWriter.writeToFile(path + MAPS_FOLDER + mapName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void generateAutoSkyBox(Q3Map finalMap, AutoSkyBox autoSkyBox) {
        Point minPoint = finalMap.getLowerBoundPoint();
        Point maxPoint = finalMap.getUpperBoundPoint();
        int lastBrushIndex = finalMap.getWorldSpawn().getBrushCount();
        List<Brush> skybox = autoSkyBox.generateSkyBox(minPoint, maxPoint, lastBrushIndex);
        finalMap.getWorldSpawn().addAll(skybox);
    }

    private String pureMapName(String mapName) {
        return mapName.substring(0, mapName.length() - MAP_FILE_EXTENSION.length());
    }
}
