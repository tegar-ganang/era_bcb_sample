package org.tolk.ui.extension.ipico;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.tolk.ApplicationContextFactory;
import org.tolk.io.extension.ipico.ReaderDao;
import org.tolk.util.extension.ipico.ReaderVo;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 * UI Handler class for ReaderConfigurator zul interface
 * 
 * @author Werner van Rensburg
 */
@SuppressWarnings("serial")
public class ReaderConfigurator extends Window implements EventListener {

    private static final String READER_DAO = "readerDao";

    private static final String READER_NAME_TEXT_BOX_ = "readerNameTextBox";

    private Map<String, ReaderVo> registeredReaderVoMap;

    private Map<String, Row> registeredReaderRowMap;

    private ReaderDao readerDao = null;

    private static final Logger logger = Logger.getLogger(ReaderConfigurator.class);

    /**
     * Populate Grid with data from tbl$reader
     */
    public void populate(Component readerGridRows) {
        this.refresh(readerGridRows);
        this.populateReaderGridRows(readerGridRows);
    }

    /**
     * Clears the current results from the UI and refreshes the registered tags
     */
    public void refresh(Component gridRows) {
        Rows rows = (Rows) gridRows;
        rows.getChildren().clear();
    }

    public void addReader(Component _readerId, Component _readerDescription, Component _readerGridRows) {
        Textbox readerIdTextbox = (Textbox) _readerId;
        Textbox readerDescriptionTextbox = (Textbox) _readerDescription;
        ReaderVo readerVoZul = new ReaderVo(readerIdTextbox.getValue(), readerDescriptionTextbox.getValue());
        this.getReaderDao().write(readerVoZul);
        this.populate((Rows) _readerGridRows);
    }

    /**
     * Reads all the registered tags from the database and populates the 
     * readerGridRows on the UI.
     * 
     * @param readerGridRows
     *            the rows to populate.
     */
    public void populateReaderGridRows(Component _readerGridRows) {
        Rows readerGridRows = (Rows) _readerGridRows;
        this.registeredReaderVoMap = new HashMap<String, ReaderVo>();
        this.registeredReaderRowMap = new HashMap<String, Row>();
        Collection<ReaderVo> allReaders = getReaderDao().readAll();
        if (allReaders != null) {
            for (ReaderVo readerVo : allReaders) {
                addReaderGridItem(readerVo, readerGridRows, readerVo.getReaderId());
                this.registeredReaderVoMap.put(readerVo.getReaderId(), readerVo);
            }
        }
    }

    /**
     * Start feeding the data stream to the UI.
     * 
     * @param grid
     * @throws InterruptedException
     */
    public void start(Component grid, Component rows) throws InterruptedException {
        this.getReaderDao().initTable();
    }

    /**
     * see {@link EventListener#onEvent(Event)}
     */
    public void onEvent(Event event) throws Exception {
        try {
            if (event.getName().equals("onBlur")) {
                onBlur(event);
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * The onBlur event handler.
     * 
     * @param event
     */
    private void onBlur(Event event) {
        if (event.getTarget() instanceof Textbox) {
            Textbox textbox = (Textbox) event.getTarget();
            if (textbox.getId().startsWith(READER_NAME_TEXT_BOX_)) {
                tagTextboxOnBlur(textbox);
            }
        }
    }

    /**
     * The onblur event for tag name textbox.
     * 
     * @param textbox
     */
    private void tagTextboxOnBlur(Textbox readerNameTextbox) {
        Row row = (Row) readerNameTextbox.getParent();
        Label readerIdLabel = (Label) row.getFirstChild();
        ReaderVo readerVo = new ReaderVo();
        readerVo.setReaderId(readerIdLabel.getValue());
        readerVo.setReaderName(readerNameTextbox.getValue());
        if (readerVo.getReaderName() == null) {
            readerVo.setReaderName("");
        }
        logger.info("store reader: " + readerVo.getReaderId() + " " + readerVo.getReaderName());
        getReaderDao().write(readerVo);
    }

    /**
     * Retrieves / Initializes the database readerDao.
     * 
     * @return the database readerDao.
     */
    private ReaderDao getReaderDao() {
        if (this.readerDao == null) {
            this.readerDao = (ReaderDao) ApplicationContextFactory.getBean(READER_DAO);
        }
        return this.readerDao;
    }

    /**
     * Adds a ReaderVo to the registered readers grid.
     * 
     * @param readerVo
     * @param gridRows
     */
    private void addReaderGridItem(ReaderVo readerVo, Rows gridRows, String _readerId) {
        Row row = new Row();
        Label readerId = new Label(_readerId);
        row.appendChild(readerId);
        Textbox readerName = new Textbox();
        if (readerVo != null) {
            readerName.setValue(readerVo.getReaderName());
        }
        readerName.setId(READER_NAME_TEXT_BOX_ + readerName.getId().toString());
        readerName.addEventListener("onBlur", this);
        row.appendChild(readerName);
        gridRows.appendChild(row);
        this.registeredReaderRowMap.put(_readerId, row);
    }
}
