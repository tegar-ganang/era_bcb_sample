package de.cinek.rssview;

import java.util.ResourceBundle;

/**
 * @version $Id: ArticleTableModel.java,v 1.11 2004/07/12 10:50:04 saintedlama Exp $
 */
public class ArticleTableModel extends javax.swing.table.AbstractTableModel implements de.cinek.rssview.event.ChannelListener {

    private static final int COLNAME_NEW = 0;

    private static final int COLNAME_TITLE = 1;

    private static final int COLNAME_DATE = 2;

    private String[] columnNames;

    private Channel channel;

    protected ArticleTableModel(Channel channel) {
        ResourceBundle rb = ResourceBundle.getBundle("rssview");
        columnNames = new String[] { rb.getString("table_head_new"), rb.getString("table_head_title"), rb.getString("table_head_date") };
        setChannel(channel);
    }

    public ArticleTableModel() {
        this(new RssChannel());
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return channel.getArticleCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Article article = getArticle(rowIndex);
        switch(columnIndex) {
            case COLNAME_NEW:
                return Boolean.valueOf(!article.isRead());
            case COLNAME_TITLE:
                return article.getTitle();
            case COLNAME_DATE:
                return article.getPublishingDate();
            default:
                return "";
        }
    }

    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    public Class getColumnClass(int columnIndex) {
        switch(columnIndex) {
            case COLNAME_NEW:
                return Boolean.class;
            case COLNAME_TITLE:
                return String.class;
            case COLNAME_DATE:
                return java.util.Date.class;
            default:
                return Object.class;
        }
    }

    public void articleRemoved(de.cinek.rssview.event.ChannelEvent event) {
        fireTableRowsDeleted(event.getStartIndex(), event.getStartIndex());
    }

    public void articleStateChanged(de.cinek.rssview.event.ChannelEvent event) {
        fireTableRowsUpdated(event.getStartIndex(), event.getStartIndex());
    }

    public void articlesAdded(de.cinek.rssview.event.ChannelEvent event) {
        fireTableRowsInserted(event.getStartIndex(), event.getEndIndex());
    }

    public Article getArticle(int row) {
        return channel.get(row);
    }

    /** Getter for property model.
	 * @return Value of property model.
	 */
    public Channel getChannel() {
        return channel;
    }

    /** 
	 * Setter for property channel.
	 * @param channel New value of property channel.
	 */
    public void setChannel(Channel channel) {
        if (this.channel != null) {
            this.channel.removeChannelListener(this);
        }
        this.channel = channel;
        channel.addChannelListener(this);
        fireTableDataChanged();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (columnIndex == COLNAME_NEW);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == COLNAME_NEW) {
            this.channel.setRead(rowIndex, !((Boolean) aValue).booleanValue());
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
