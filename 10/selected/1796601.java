package devbureau.fstore.common.db;

import devbureau.fstore.common.Order;
import devbureau.fstore.common.OrderLine;
import devbureau.fstore.util.DBHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 *
 * @author buyakov
 */
public class CreateOrderDBTask extends DBTask {

    private static Logger logger = Logger.getLogger(CreateOrderDBTask.class);

    public static final String COUNT_ACTIVE_ORDERS = "SELECT COUNT(*) FROM STORE.ORDER WHERE seller =  ? AND status_id = 0";

    public static final String NEXT_ORDER = "values nextval for STORE.ORDER_SEQ";

    public static final String INSERT_ORDER = "insert into STORE.ORDER(ID,CUSTOMER_ID,SELLER,status_id) values (?,?,?,0)";

    private Order order = null;

    private Integer orderId = null;

    private String login = null;

    public CreateOrderDBTask(Connection arg_connection, Order arg_order, String login) {
        super(arg_connection);
        order = arg_order;
        this.login = login;
    }

    public void run() throws Exception {
        logger.debug("#run enter");
        PreparedStatement psNextId = null;
        ResultSet rsNextId = null;
        PreparedStatement ps = null;
        try {
            PreparedStatement psCount = connection.prepareStatement(COUNT_ACTIVE_ORDERS);
            psCount.setString(1, login);
            ResultSet rsCount = psCount.executeQuery();
            if (rsCount.next()) {
                Integer count = rsCount.getInt(1);
                if (count > 0) {
                    DBHelper.closeAll(null, rsCount, psCount);
                    throw new RuntimeException("У вас уже есть один активный заказ. Вы не можете создать второй.");
                }
            }
            connection.setAutoCommit(false);
            psNextId = connection.prepareStatement(NEXT_ORDER);
            rsNextId = psNextId.executeQuery();
            if (rsNextId.next()) {
                orderId = rsNextId.getInt(1);
            }
            rsNextId.close();
            psNextId.close();
            logger.info("#run orderId  = " + orderId);
            ps = connection.prepareStatement(INSERT_ORDER);
            ps.setInt(1, orderId);
            if (order.getCustomerId() != null) {
                ps.setInt(2, order.getCustomerId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, order.getSellerLogin());
            ps.executeUpdate();
            ps.close();
            logger.info("#run order insert OK");
            Iterator<OrderLine> lines = order.getLines().iterator();
            while (lines.hasNext()) {
                OrderLine line = lines.next();
                CreateOrderLineAction action = new CreateOrderLineAction();
                action.execute(connection, orderId, line);
            }
            connection.commit();
        } catch (SQLException ex) {
            logger.error("SQLException", ex);
            connection.rollback();
            throw new Exception("Не удалось создать заказ. Ошибка : " + ex.getMessage());
        } finally {
            connection.setAutoCommit(true);
        }
        logger.debug("#run exit");
    }

    public Integer getOrderId() {
        return orderId;
    }
}
