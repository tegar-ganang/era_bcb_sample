package devbureau.fstore.common.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.apache.log4j.Logger;

public class DeleteDeliveryDBTask extends DBTask {

    private Integer deliveryId = null;

    private static Logger logger = Logger.getLogger(DeleteDeliveryDBTask.class);

    public static final String SQL_UPDATE_ITEM_MIN_QTTY = "update STORE.ITEM I set I.BALANCE = I.BALANCE - (select DL.QUANTITY from STORE.DELIVERY_LINE DL where DL.DELIVERY_ID = ? and DL.ITEM_ID = I.ID) where I.ID = (select DL.ITEM_ID from STORE.DELIVERY_LINE DL where DL.DELIVERY_ID = ? and DL.ITEM_ID = I.ID)";

    public static final String SQL_DELETE_DELIVERY_LINE = "delete from STORE.DELIVERY_LINE DL where DL.DELIVERY_ID = ?";

    public static final String SQL_DELETE_DELIVERY = "delete from STORE.DELIVERY D where D.ID = ?";

    public DeleteDeliveryDBTask(Connection connection, Integer deliveryId) {
        super(connection);
        this.deliveryId = deliveryId;
    }

    public void run() throws Exception {
        logger.debug("#run enter");
        PreparedStatement ps = null;
        try {
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(SQL_UPDATE_ITEM_MIN_QTTY);
            ps.setInt(1, deliveryId);
            ps.setInt(2, deliveryId);
            ps.executeUpdate();
            ps.close();
            logger.debug("#run update STORE.ITEM ok");
            ps = connection.prepareStatement(SQL_DELETE_DELIVERY_LINE);
            ps.setInt(1, deliveryId);
            ps.executeUpdate();
            ps.close();
            logger.debug("#run delete STORE.DELIVERY_LINE ok");
            ps = connection.prepareStatement(SQL_DELETE_DELIVERY);
            ps.setInt(1, deliveryId);
            ps.executeUpdate();
            ps.close();
            logger.debug("#run delete STORE.DELIVERY ok");
            connection.commit();
        } catch (Exception ex) {
            logger.error("#run Transaction roll back ", ex);
            connection.rollback();
            throw new Exception("#run Не удалось загрузить в БД информацию об обновлении склада. Ошибка : " + ex.getMessage());
        } finally {
            connection.setAutoCommit(true);
        }
        logger.debug("#run exit");
    }
}
