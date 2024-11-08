package internal.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.lightcommons.io.FileUtils;
import org.lightcommons.logger.Log;
import org.lightcommons.logger.LogFactory;
import org.lightcommons.resource.FileSystemResourceLoader;
import org.lightcommons.template.Template;
import org.lightcommons.template.TemplateFactory;
import org.lightcommons.util.PatternMap;
import org.lightcommons.util.StringUtils;
import org.lightmtv.LightMtvException;

/**
 * 代码生成辅助函数
 */
public class GenerateUtils {

    private static Log log = LogFactory.getLog("lmtv-gen");

    public static void setLog(Log log) {
        GenerateUtils.log = log;
    }

    /**
	 * 从模板目录生成目标文件
	 * @param factory
	 * @param templateRoot
	 * @param outputRoot
	 * @param context
	 * @param overwrite
	 * @throws Exception
	 */
    public static void generateAll(TemplateFactory factory, String templateRoot, String outputRoot, Map<String, Object> context, boolean overwrite) throws Exception {
        String mappingText = FileUtils.readFileToString(new File(templateRoot + "/generator.map"), "utf-8");
        FileMapping fileMapping = getFileMapping(factory, mappingText, context);
        generateAll(factory, templateRoot, outputRoot, context, overwrite, fileMapping, "");
    }

    /**
	 * 从模板目录生成目标文件
	 * @param factory
	 * @param templateRoot
	 * @param outputRoot
	 * @param context
	 * @param overwrite
	 * @param fileMapping 文件映射
	 * @throws Exception
	 */
    public static void generateAll(TemplateFactory factory, String templateRoot, String outputRoot, Map<String, Object> context, boolean overwrite, FileMapping fileMapping, String updatingFolder) throws Exception {
        String newTplRoot = StringUtils.cleanDirPath(templateRoot);
        factory.setResourceLoader(new FileSystemResourceLoader(newTplRoot));
        File root = new File(newTplRoot);
        List<File> files = new ArrayList<File>();
        FileHelper.listFiles(root, files);
        for (File f : files) {
            String relativePath = FileHelper.getRelativePath(root, f);
            String fileName = StringUtils.cleanPath(f.getPath());
            log.info("fileName:" + fileName);
            String targetFileName = fileMapping.getTargetFileName(relativePath, outputRoot, context);
            if (targetFileName == null || !targetFileName.startsWith(updatingFolder)) continue;
            File target = new File(targetFileName);
            if (f.isDirectory()) {
                log.info("make dir: " + targetFileName);
                target.mkdirs();
            } else if (FileHelper.isTemplate(fileName)) {
                String insertBlock = FileHelper.getInsertBlock(fileName);
                if (StringUtils.hasText(insertBlock)) {
                    log.info("insertblock:" + insertBlock);
                    String toInsert = parseFile(factory, relativePath, context);
                    try {
                        String src = FileUtils.readFileToString(target, "utf-8");
                        String newText = StringUtils.replace(src, insertBlock, toInsert);
                        log.info("...insert into: " + targetFileName);
                        FileUtils.writeStringToFile(target, newText, "utf-8");
                        log.info("...done!\n");
                    } catch (FileNotFoundException e) {
                        log.warn(e);
                        FileUtils.writeStringToFile(new File(targetFileName + ".insert"), toInsert, "utf-8");
                    }
                } else {
                    if (target.exists() && !overwrite) {
                        log.info("ignore exists file:" + targetFileName);
                        continue;
                    }
                    String text = parseFile(factory, relativePath, context);
                    log.info("...write file to: " + targetFileName);
                    FileUtils.writeStringToFile(target, text, "utf-8");
                    log.info("...done!\n");
                }
            } else if (target.exists() && !overwrite) {
                log.info("ignore exists file:" + targetFileName);
                continue;
            } else {
                log.info("copying file: " + relativePath);
                FileUtils.copyFile(f, target);
                log.info("...done!\n");
            }
        }
    }

    public static String parseFile(TemplateFactory factory, String relativePath, Map<?, ?> context) {
        log.info("parsing file: " + relativePath);
        Template template;
        try {
            template = factory.getTemplate(relativePath, "utf-8");
            return template.render(context);
        } catch (Exception e) {
            log.error(e);
            throw new LightMtvException("error parse file:" + relativePath, e);
        }
    }

    public static FileMapping getFileMapping(TemplateFactory factory, String mappingText, Map<?, ?> context) {
        try {
            FileMapping fm = new FileMapping();
            PatternMap<String> map = new PatternMap<String>();
            fm.setPatternMap(map);
            Template template = null;
            template = factory.compile("mapping.$ignore", mappingText);
            if (template != null) {
                String text = template.render(context);
                String[] lines = text.split("\\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.length() > 0) {
                        if (line.startsWith("%error ")) {
                            throw new IllegalArgumentException(line.substring(6));
                        }
                        String[] kv = line.split(">");
                        if (kv.length != 2) throw new IllegalArgumentException("error in line:'" + line + "'"); else {
                            String key = kv[0].trim();
                            String value = kv[1].trim();
                            map.put(key, value);
                        }
                    }
                }
            }
            return fm;
        } catch (Exception e) {
            LogFactory.getLog(GenerateUtils.class).error(e);
            throw new LightMtvException(e);
        }
    }
}
