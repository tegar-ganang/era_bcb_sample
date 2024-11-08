package ru.ecom.jbossinstaller.service.impl.modify;

import ru.ecom.jbossinstaller.service.impl.file.IFile;
import ru.ecom.jbossinstaller.service.IMainModel;
import java.io.*;

/**
 * copy files
 */
public class CopyFilesModify implements IModify {

    public CopyFilesModify(String aName, IFile[] aFiles, String aDescription, String aJarDir) {
        theFiles = aFiles;
        theName = aName;
        theDescription = aDescription;
        theJarDir = aJarDir;
    }

    public IFile[] getAffectedFiles() {
        return theFiles;
    }

    public CanApplyResult canApply(IMainModel aMainModel) {
        boolean isEquals = true;
        try {
            for (IFile file : theFiles) {
                String name = file.getName();
                String fullJarName = "/" + theJarDir + "/" + name;
                InputStream in = getClass().getResourceAsStream(fullJarName);
                if (in == null) throw new ErrorApplyException("Файл " + fullJarName + " не найден во внутренних ресурсах программы");
                try {
                    File destFile = aMainModel.getFile(file);
                    if (destFile.exists()) {
                        BufferedInputStream jbossIn = new BufferedInputStream(new FileInputStream(destFile));
                        try {
                            if (!isInputStreamEquals(in, jbossIn)) {
                                isEquals = false;
                            }
                        } finally {
                            jbossIn.close();
                        }
                    } else {
                        return new CanApplyResult();
                    }
                } catch (FileNotFoundException e) {
                    throw new FileNotFoundException("Файл " + aMainModel.getFile(file).getAbsolutePath() + " не найден");
                } finally {
                    in.close();
                }
            }
            return isEquals ? new CanApplyResult("Все файлы одинаковые") : new CanApplyResult();
        } catch (IOException e) {
            throw new ErrorApplyException("Ошибка при проверке", e);
        }
    }

    protected static boolean isInputStreamEquals(InputStream aSource, InputStream aDest) throws IOException {
        if (aSource == null) throw new NullPointerException("aSource is null");
        if (aDest == null) throw new NullPointerException("aDest is null");
        int sourceReaded;
        int destReaded = Integer.MIN_VALUE;
        while ((sourceReaded = aSource.read()) >= 0 && (destReaded = aDest.read()) >= 0) {
            if (sourceReaded != destReaded) {
                return false;
            }
        }
        if (destReaded >= 0) destReaded = aDest.read();
        return sourceReaded == destReaded;
    }

    public void apply(IMainModel aModel) {
        try {
            for (IFile file : theFiles) {
                String name = file.getName();
                String fullJarName = "/" + theJarDir + "/" + name;
                InputStream in = getClass().getResourceAsStream(fullJarName);
                if (in == null) throw new ErrorApplyException("Файл " + fullJarName + " не найден во внутренних ресурсах программы");
                try {
                    File destFile = aModel.getFile(file);
                    File destDir = destFile.getParentFile();
                    if (!destDir.exists()) destDir.mkdirs();
                    BufferedOutputStream jbossIn = new BufferedOutputStream(new FileOutputStream(destFile));
                    try {
                        copyInputStream(in, jbossIn);
                    } finally {
                        jbossIn.close();
                    }
                } catch (FileNotFoundException e) {
                    throw new FileNotFoundException("Файл " + aModel.getFile(file).getAbsolutePath() + " не найден");
                } finally {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new ErrorApplyException("Ошибка при проверке", e);
        }
    }

    private void copyInputStream(InputStream aSource, BufferedOutputStream aDest) throws IOException {
        byte[] buf = new byte[1024];
        int readed;
        while ((readed = aSource.read(buf, 0, 1024)) > 0) {
            aDest.write(buf, 0, readed);
        }
    }

    public String getName() {
        return theName;
    }

    public String getDescription() {
        return theDescription;
    }

    private final IFile[] theFiles;

    private final String theName;

    private final String theDescription;

    private final String theJarDir;
}
