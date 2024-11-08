package org.happycomp.radio.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.happycomp.radio.StopDownloadCondition;
import org.happycomp.radio.StoreStateException;
import org.happycomp.radio.StoreState;
import org.happycomp.radio.downloader.DownloadingItem;
import org.happycomp.radio.io.IOUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class StoreStatesImpl implements StoreState {

    @Inject
    private Provider<File> stateDirProvider;

    @Inject
    private Provider<StopDownloadCondition> conditionProvider;

    @Override
    public void deleteDownloadingItem(DownloadingItem item) throws StoreStateException {
        try {
            File xmlInfoFile = createFileObject(item);
            xmlInfoFile.deleteOnExit();
            xmlInfoFile.delete();
        } catch (IOException e) {
            throw new StoreStateException(e);
        }
    }

    private DownloadingItem cloneDownloadingItem(DownloadingItem ditm) throws IOException {
        File oldFile = ditm.getFile();
        File nFile = newFile(oldFile);
        File[] previousFiles = ditm.getPreviousFiles();
        File[] newPreviousFiles = null;
        if (previousFiles != null) {
            newPreviousFiles = new File[previousFiles.length];
            for (int j = 0; j < newPreviousFiles.length; j++) {
                newPreviousFiles[j] = newFile(oldFile);
            }
        }
        DownloadingItem nitm = new DownloadingItem(nFile, ditm.getId(), ditm.getTitle(), ditm.getPlaylistEntry(), ditm.getDate(), ditm.getProcessPid(), ditm.geStopDownloadingCondition());
        nitm.setState(ditm.getProcessState());
        nitm.setPreviousFiles(newPreviousFiles);
        return nitm;
    }

    private File newFile(File oldFile) throws IOException {
        int counter = 0;
        File nFile = new File(this.stateDirProvider.get() + File.separator + oldFile.getName());
        while (nFile.exists()) {
            nFile = new File(this.stateDirProvider.get() + File.separator + oldFile.getName() + "_" + counter);
        }
        IOUtils.copyFile(oldFile, nFile);
        return nFile;
    }

    @Override
    public void storeDownloadingItem(DownloadingItem origItem) throws StoreStateException {
        OutputStream os = null;
        try {
            DownloadingItem item = cloneDownloadingItem(origItem);
            File xmlInfoFile = createFileObject(item);
            os = new FileOutputStream(xmlInfoFile);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element storeElement = item.storeElement(doc);
            doc.appendChild(storeElement);
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(os);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.transform(domSource, streamResult);
        } catch (DOMException e) {
            throw new StoreStateException(e);
        } catch (ParserConfigurationException e) {
            throw new StoreStateException(e);
        } catch (TransformerConfigurationException e) {
            throw new StoreStateException(e);
        } catch (TransformerException e) {
            throw new StoreStateException(e);
        } catch (FileNotFoundException e) {
            throw new StoreStateException(e);
        } catch (IOException e) {
            throw new StoreStateException(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    throw new StoreStateException(e);
                }
            }
        }
    }

    private File createFileObject(DownloadingItem item) throws IOException {
        File xmlInfoFile = new File(this.stateDirProvider.get(), item.getId() + ".xml");
        xmlInfoFile.createNewFile();
        return xmlInfoFile;
    }

    @Override
    public DownloadingItem[] getDownloadingItems() throws StoreStateException {
        List<DownloadingItem> items = new ArrayList<DownloadingItem>();
        File[] listFiles = this.stateDirProvider.get().listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".xml");
            }
        });
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            for (File file : listFiles) {
                Document parsed = builder.parse(file);
                DownloadingItem item = DownloadingItem.loadFromElement(parsed.getDocumentElement(), this.conditionProvider.get());
                items.add(item);
            }
        } catch (ParserConfigurationException e) {
            throw new StoreStateException(e);
        } catch (SAXException e) {
            throw new StoreStateException(e);
        } catch (IOException e) {
            throw new StoreStateException(e);
        }
        return (DownloadingItem[]) items.toArray(new DownloadingItem[items.size()]);
    }

    @Override
    public void updateDownloadingItem(DownloadingItem item) throws StoreStateException {
        this.storeDownloadingItem(item);
    }

    public Provider<File> getStateDirProvider() {
        return stateDirProvider;
    }

    public void setStateDirProvider(Provider<File> stateDirProvider) {
        this.stateDirProvider = stateDirProvider;
    }

    public Provider<StopDownloadCondition> getConditionProvider() {
        return conditionProvider;
    }

    public void setConditionProvider(Provider<StopDownloadCondition> conditionProvider) {
        this.conditionProvider = conditionProvider;
    }
}
