package com.nhncorp.cubridqa.test.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TestLinuxAnswersToAnswers {

    private String path = "/home/xdbms/workspace/qa_repository/scenario/sql/";

    @Test
    public void copyLinuToAnswers() {
        File parent = new File(path);
        copy(parent);
    }

    private void copy(File parent) {
        System.out.println("parent---" + parent.getAbsolutePath());
        if (parent.isDirectory() && !".svn".equals(parent.getName())) {
            File cases = new File(parent, "cases");
            if (cases.exists()) {
                File answers = new File(parent, "answers");
                if (!answers.exists()) {
                    answers.mkdir();
                    File answers_linux = new File(parent, "answers_linux");
                    for (File file : answers_linux.listFiles()) {
                        if (file.isDirectory()) {
                            continue;
                        }
                        File target = new File(answers, file.getName());
                        try {
                            IOUtils.copy(new FileInputStream(file), new FileOutputStream(target));
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            } else {
                for (File son : parent.listFiles()) {
                    System.out.println("parent---" + son.getAbsolutePath());
                    copySon(son);
                }
            }
        }
    }

    private void copySon(File parent) {
        System.out.println("parent---" + parent.getAbsolutePath());
        if (parent.isDirectory() && !".svn".equals(parent.getName())) {
            File cases = new File(parent, "cases");
            if (cases.exists()) {
                File answers = new File(parent, "answers");
                if (!answers.exists()) {
                    answers.mkdir();
                    File answers_linux = new File(parent, "answers_linux");
                    for (File file : answers_linux.listFiles()) {
                        if (file.isDirectory()) {
                            continue;
                        }
                        File target = new File(answers, file.getName());
                        try {
                            IOUtils.copy(new FileInputStream(file), new FileOutputStream(target));
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            } else {
                for (File son : parent.listFiles()) {
                    System.out.println("parent---" + son.getAbsolutePath());
                    copySon(son);
                }
            }
        }
    }
}
