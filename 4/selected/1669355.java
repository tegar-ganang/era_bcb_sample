package wheel.util;

import java.io.*;

public class ProjectCreator {

    private static String projectNameS;

    private static String basePackageS;

    private static String type;

    private static String baseDir;

    private static String webAppDir;

    public static void main(String[] args) throws IOException {
        projectNameS = args[0];
        basePackageS = args[1];
        type = args[2];
        baseDir = new File("").getAbsolutePath().replace("\\", "\\\\");
        if (type.equals("simple")) {
            webAppDir = ".";
            processFile(".project");
            processFile("build.xml");
            processFile("src/Home.java");
            new File("src/" + basePackageS.replace('.', '/') + "/" + projectNameS + "/pages").mkdirs();
            new File("src/" + basePackageS.replace('.', '/') + "/" + projectNameS + "/pages/Home.java").createNewFile();
            copyFile("src/Home.java", "src/" + basePackageS.replace('.', '/') + "/" + projectNameS + "/pages/Home.java");
            new File("pom.xml").delete();
        } else if (type.equals("maven2")) {
            webAppDir = "src/main/webapp";
            processFile("pom.xml");
            processFile("src/Home.java");
            new File("src/main/java/" + basePackageS.replace('.', '/') + "/" + projectNameS + "/pages").mkdirs();
            new File("src/main/java/" + basePackageS.replace('.', '/') + "/" + projectNameS + "/pages/Home.java").createNewFile();
            copyFile("src/Home.java", "src/main/java/" + basePackageS.replace('.', '/') + "/" + projectNameS + "/pages/Home.java");
            new File("build.xml").delete();
        }
        processFile("jetty.launch");
        processFile("src/Home.java");
        processFile("WEB-INF/web.xml");
        new File("src/Home.java").delete();
    }

    private static void processFile(String file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        int read = 0;
        byte[] buf = new byte[2048];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        while ((read = in.read(buf)) > 0) bout.write(buf, 0, read);
        in.close();
        String converted = bout.toString().replaceAll("@project.name@", projectNameS).replaceAll("@base.package@", basePackageS).replaceAll("@base.dir@", baseDir).replaceAll("@webapp.dir@", webAppDir).replaceAll("path=\"target/classes\"", "path=\"src/main/webapp/WEB-INF/classes\"");
        FileOutputStream out = new FileOutputStream(file);
        out.write(converted.getBytes());
        out.close();
    }

    private static void copyFile(String from, String to) throws IOException {
        FileReader in = new FileReader(from);
        FileWriter out = new FileWriter(to);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }
}
