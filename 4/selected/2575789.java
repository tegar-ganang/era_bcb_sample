package fr.mouniroudev.commore.preferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import fr.mouniroudev.commore.popup.action.GenerateFromIdl;
import fr.mouniroudev.commore.utils.FileUtils;

public class CommoreGenerator {

    private static CommoreGenerator m_instance;

    private final IWorkspace m_iWorkspace;

    private File m_javaGeneratorHelperUsed;

    private File m_javaGeneratorExeUsed;

    private File m_commoreJarUsed;

    public CommoreGenerator(IWorkspace iWorkspace) {
        this.m_iWorkspace = iWorkspace;
        checkRequiresFiles(m_iWorkspace);
    }

    private void checkRequiresFiles(IWorkspace mIWorkspace) {
        File workSpaceLocation = m_iWorkspace.getRoot().getLocation().toFile();
        File commoreSettingDir = new File(workSpaceLocation, ".commore_plugin");
        commoreSettingDir.mkdirs();
        m_javaGeneratorHelperUsed = new File(commoreSettingDir, "java_generator_helper.jar");
        if (!m_javaGeneratorHelperUsed.exists()) {
            InputStream javaGeneratorHelperInJar = GenerateFromIdl.class.getResourceAsStream("/ressources/java_generator_helper.jar");
            FileOutputStream dest;
            try {
                dest = new FileOutputStream(m_javaGeneratorHelperUsed);
                FileUtils.copyFile(javaGeneratorHelperInJar, dest);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        m_javaGeneratorExeUsed = new File(commoreSettingDir, "java_generator.exe");
        if (!m_javaGeneratorExeUsed.exists()) {
            InputStream javaGeneratorExeInJar = GenerateFromIdl.class.getResourceAsStream("/ressources/java_generator.exe");
            FileOutputStream dest;
            try {
                dest = new FileOutputStream(m_javaGeneratorExeUsed);
                FileUtils.copyFile(javaGeneratorExeInJar, dest);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        m_commoreJarUsed = new File(commoreSettingDir, "commore.jar");
        if (!m_commoreJarUsed.exists()) {
            InputStream commoreJarUsedInJar = GenerateFromIdl.class.getResourceAsStream("/ressources/commore.jar");
            FileOutputStream dest;
            try {
                dest = new FileOutputStream(m_commoreJarUsed);
                FileUtils.copyFile(commoreJarUsedInJar, dest);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static CommoreGenerator getInstance() {
        if (m_instance == null) {
            IWorkspace iWorkspace = ResourcesPlugin.getWorkspace();
            m_instance = new CommoreGenerator(iWorkspace);
        }
        return m_instance;
    }

    public String getDefaultCommoreJarPath() {
        return m_commoreJarUsed.getAbsolutePath();
    }

    public String getDefaultJavaGeneratorExePath() {
        return m_javaGeneratorExeUsed.getAbsolutePath();
    }

    public String getDefaultJavaGeneratorHelperPath() {
        return m_javaGeneratorHelperUsed.getAbsolutePath();
    }
}
