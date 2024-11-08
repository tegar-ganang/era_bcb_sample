package org.lightmtv.generator;

import java.io.File;
import java.io.IOException;
import org.lightcommons.io.FileUtils;
import org.lightcommons.util.StringUtils;
import org.lightmtv.generator.launch.Launcher;
import org.lightmtv.generator.util.Utility;

public class IntegratedTest {

    public static void main(String[] args) throws IOException {
        System.setProperty(Launcher.APPHOME_PROPERTY, Config.getHome());
        IntegratedTest test = new IntegratedTest();
        test.createApp("quickstart");
        cd("quickstart");
        test.createModel("TopModel1");
        test.createModel("sub1.SubModel11");
        test.gen("view", "TopModel1");
        test.genAll("sub1.SubModel11");
        test.genModelAndAll("TopModel2");
        test.genModelAndAll("TopModel3");
        test.genModelAndAll("EmbededSample");
        test.genModelAndAll("sub1.SubModel12");
        test.genModelAndAll("sub1.SubModel13");
        test.genModelAndAll("sub2.SubModel21");
        test.genModelAndAll("sub2.SubModel22");
        test.genModelAndAll("sub2.SubModel23");
        test.genModelAndAll("sub3.SubModel31");
        test.genModelAndAll("sub3.SubModel32");
        test.genModelAndAll("sub3.SubModel33");
    }

    private void genModelAndAll(String modelName) {
        createModel(modelName);
        genAll(modelName);
    }

    private void createApp(String projectName) throws IOException {
        System.out.println("creating " + projectName + "...");
        System.setProperty("user.dir", Config.getProperty("test.out"));
        File dir = new File(Utility.getCurrentDir() + "/" + projectName);
        if (dir.exists()) try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        execute("create-app", projectName, "org.groupId", "org.groupId." + projectName, "versionId", "maven2");
        FileUtils.copyFile(new File(Config.getHome() + "/src/test/EmbededSample.txt"), new File(Utility.getCurrentDir() + "/" + projectName + "/src/main/java/org/groupId/" + projectName + "/models/EmbededSample.java"));
        FileUtils.copyFile(new File(Config.getHome() + "/src/test/${modelName}.txt"), new File(Utility.getCurrentDir() + "/" + projectName + "/generator/model/src_java/${basePackagePath}/models/${subPackagePath}/${modelName}.java"));
    }

    private void createModel(String name) {
        System.out.println("creating model " + name + "...");
        execute("create-model", name);
    }

    private void gen(String templateName, String modelAndPack) {
        System.out.println("gen  " + templateName + " " + modelAndPack + "...");
        execute("gen", templateName, modelAndPack);
    }

    private void genAll(String name) {
        System.out.println("gen-all " + name + "...");
        execute("gen-all", name);
    }

    private static void cd(String path) {
        System.setProperty("user.dir", StringUtils.cleanDirPath(Utility.getCurrentDir() + "/" + path));
    }

    private void execute(String... args) {
        Main main = new Main();
        main.startApp(args, null, null);
    }
}
