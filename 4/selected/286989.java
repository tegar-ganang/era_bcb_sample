package logicalDocParts;

import imagesJAI.ImprovedImage;
import imagesJAI.ImprovedImageImpl;
import imagesJAI.WrongPositionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Page extends AbstractUserImage {

    private static final long serialVersionUID = 3328458663351023384L;

    private static final String PATH_PAGES = System.getProperty("user.home") + "/.improveDocs/pages/";

    private static final String PAGES_EXTENTION = ".idsP";

    private ImageDiagram pageDiagram;

    private ArrayList<SymboleChar> symboles;

    private ArrayList<TextLine> lines;

    private double bestDiagramVariance = 0.D;

    float bestAngle = -180.F;

    public Page(ImprovedImage image, String name) {
        this.image = image;
        this.name = name;
        lines = new ArrayList<TextLine>();
        symboles = new ArrayList<SymboleChar>();
        new File(PATH_PAGES).mkdir();
    }

    public Page(String pageName) {
        open(pageName);
    }

    public float detectSkew(int pixelStep, int binaryThreshold) {
        int nbthread = Runtime.getRuntime().availableProcessors();
        int[] xMin = new int[nbthread], xMax = new int[nbthread];
        int stepX = (int) Math.ceil((double) (getHeight() * 2) / (double) nbthread);
        Thread[] threads = new Thread[nbthread];
        for (int i = 0; i < nbthread; i++) {
            xMin[i] = Math.min(-getHeight() + stepX * (i) + 1, getHeight());
            if (i == 0) xMin[0]--;
            xMax[i] = Math.min(xMin[i] + stepX, getHeight());
            threads[i] = new Thread(new DetectSkewThreadPart(this, pixelStep, xMin[i], xMax[i]));
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) try {
            threads[i].join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return bestAngle;
    }

    private class DetectSkewThreadPart implements Runnable {

        private int pixelStep;

        private UserImage image;

        private int xMin;

        private int xMax;

        private DetectSkewThreadPart(UserImage image, int pixelStep, int xMin, int xMax) {
            this.image = image;
            this.pixelStep = pixelStep;
            this.xMin = xMin;
            this.xMax = xMax;
        }

        public void run() {
            ImageDiagram tempDiagram;
            for (int offset = xMin; offset <= xMax; offset += pixelStep) {
                tempDiagram = new ImageDiagram(image, pixelStep, offset);
                if (tempDiagram.getVarianceValue() > bestDiagramVariance) {
                    bestAngle = tempDiagram.getAngle();
                    bestDiagramVariance = tempDiagram.getVarianceValue();
                }
            }
        }
    }

    public void correctSkew(float angle) {
        image.rotate((float) Math.toRadians(angle));
        generatePageDiagram();
    }

    private void generatePageDiagram() {
        pageDiagram = new ImageDiagram(this, 1, 0);
    }

    public ArrayList<TextLine> detectLines(double noisePerLine) {
        pageDiagram = new ImageDiagram(this, 1, 0);
        boolean existLine = true;
        int lineStart = 0;
        int lineEnd = 0;
        while (existLine) {
            lineStart = detectStart(lineEnd, noisePerLine);
            existLine = lineStart != -1;
            if (existLine) {
                lineEnd = detectEnd(lineStart, noisePerLine);
                lines.add(new TextLine(lineStart, lineEnd));
            }
        }
        return lines;
    }

    private int detectStart(int x, double noisePerLine) {
        for (int i = x; i < pageDiagram.size(); i++) if (pageDiagram.get(i) > (getWidth() * noisePerLine)) return i;
        return -1;
    }

    private int detectEnd(int x, double noisePerLine) {
        int i;
        for (i = x + 1; i < pageDiagram.size(); i++) if (pageDiagram.get(i) <= (getWidth() * noisePerLine * 0.15)) break;
        return i;
    }

    public ArrayList<TextLine> getLines() {
        return lines;
    }

    public void save(String savingPath) {
        new File(savingPath).mkdir();
        new File(PATH_PAGES).mkdir();
        String nameSave = PATH_PAGES + getName() + PAGES_EXTENTION;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(nameSave);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            try {
                objectOutputStream.writeObject(this);
                objectOutputStream.flush();
            } finally {
                try {
                    objectOutputStream.close();
                } finally {
                    fileOutputStream.close();
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void open(String name) {
        String idsPAbsolutePath = PATH_PAGES + name + PAGES_EXTENTION;
        Page tmp = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(idsPAbsolutePath);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            try {
                tmp = (Page) objectInputStream.readObject();
            } finally {
                try {
                    objectInputStream.close();
                } finally {
                    fileInputStream.close();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        this.name = name;
        pageDiagram = tmp.getPageDiagram();
        lines = tmp.getLines();
        image = tmp.getImage();
        symboles = tmp.getSymboleList();
    }

    private ImageDiagram getPageDiagram() {
        return pageDiagram;
    }

    public static String getPageAbsolutePath(String pageName) {
        return PATH_PAGES + pageName + PAGES_EXTENTION;
    }

    public static ArrayList<String> getPageRestor() {
        File pagesDiretory = new File(PATH_PAGES);
        if (pagesDiretory.list() != null) {
            ArrayList<String> pagesList = new ArrayList<String>();
            for (File idsPFile : pagesDiretory.listFiles()) {
                String pageFileName = idsPFile.getName();
                String extention = pageFileName.substring(pageFileName.lastIndexOf("."), pageFileName.length());
                if (extention.equals(PAGES_EXTENTION)) pagesList.add(pageFileName.substring(0, pageFileName.lastIndexOf(".")));
            }
            return pagesList;
        } else return null;
    }

    public void replaceChar(Char character, String savingPath) throws WrongPositionException {
        for (AssociationPageStains belka : character.getAssociationPageStains()) {
            if (name.compareTo(belka.getPageName()) == 0) {
                if (getWidth() <= (character.getWidth() + belka.getPositionCharInPage().x) || getHeight() <= (character.getHeight() + belka.getPositionCharInPage().y)) throw new WrongPositionException();
                for (int y = belka.getPositionCharInPage().y, yy = 0; yy < character.getHeight(); y++, yy++) for (int x = belka.getPositionCharInPage().x, xx = 0; xx < character.getWidth(); x++, xx++) if (character.getMatrix()[xx][yy]) image.writeGrayData(x, y, character.getImage().readGray(xx, yy));
                save(savingPath);
            }
        }
    }

    public void detectSymbolesInLine(int lineNumber) {
        TextLine line = getLines().get(lineNumber);
        SymboleChar findSymbole = new SymboleChar(line, 0, line.startLimit, this);
        while (findSymbole != null) {
            findSymbole = findSymbole.detectSymbole(line, findSymbole.right, findSymbole.top, this);
            if (findSymbole != null && findSymbole.getWidth() > 1) symboles.add(findSymbole);
        }
    }

    public void detectSymbolesInPage() {
        for (int i = 0; i < getLines().size(); i++) {
            detectSymbolesInLine(i);
        }
    }

    public ArrayList<SymboleChar> getSymboleList() {
        return symboles;
    }

    public boolean getMatrix(int i, int j) {
        return true;
    }

    public UserImage clown() {
        return new Page(new ImprovedImageImpl(this.getImage().getImageIcon()), getName());
    }
}
