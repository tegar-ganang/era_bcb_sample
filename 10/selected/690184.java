package devbureau.fstore.common.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;

public class DeleteOrderDBTask extends DBTask {

    private static Logger logger = Logger.getLogger(CreateOrderDBTask.class);

    public static final String SQL_SELECT_ORDER_LINE = "select ID,ITEM_ID,QUANTITY from STORE.ORDER_LINE where ORDER_ID = ? for update";

    public static final String SQL_DELETE_ORDER = "delete from STORE.ORDER where ID = ?";

    private Integer orderId = null;

    public DeleteOrderDBTask(Connection arg_connection, Integer orderId) {
        super(arg_connection);
        this.orderId = orderId;
    }

    public void run() throws Exception {
        logger.debug("#run enter");
        logger.debug("#run orderId = " + orderId);
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(SQL_SELECT_ORDER_LINE);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            DeleteOrderLineAction action = new DeleteOrderLineAction();
            while (rs.next()) {
                Integer lineId = rs.getInt("ID");
                Integer itemId = rs.getInt("ITEM_ID");
                Integer quantity = rs.getInt("QUANTITY");
                action.execute(connection, lineId, itemId, quantity);
            }
            rs.close();
            ps.close();
            ps = connection.prepareStatement(SQL_DELETE_ORDER);
            ps.setInt(1, orderId);
            ps.executeUpdate();
            ps.close();
            logger.info("#run order delete OK");
            connection.commit();
        } catch (SQLException ex) {
            logger.error("SQLException", ex);
            connection.rollback();
            throw new Exception("Не удалось удалить заказ. Ошибка : " + ex.getMessage());
        } finally {
            connection.setAutoCommit(true);
        }
        logger.debug("#run exit");
    }
}
