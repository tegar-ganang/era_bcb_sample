package ces.platform.infoplat.core;

import ces.platform.infoplat.core.dao.ChannelScheduleDao;
import ces.platform.infoplat.core.tree.TreeNode;

/**
 *
 * <p>Title: ��Ϣƽ̨2.5��վ����</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004 </p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class ChannelSchedule extends TreeNode {

    private String channel;

    private String time;

    private String strChannelPath;

    public ChannelSchedule() {
    }

    ChannelScheduleDao channelScheduleDao = new ChannelScheduleDao();

    public ChannelSchedule(String channelPath) {
        this.strChannelPath = channelPath;
    }

    /**
     * д�����ļ�
     * @throws java.lang.Exception
     */
    public void wirteFile(String time) throws Exception {
        channelScheduleDao.writeFile(time, strChannelPath);
    }

    /**
	 * �õ�Ƶ������ʱ���ά����
	 * @throws java.lang.Exception
	 */
    public String[][] getChannelScheduleTime() throws Exception {
        return channelScheduleDao.getChannelScheduleTime(strChannelPath);
    }

    /**
	 * �ж�Ƶ���ڴ�ʱ�Ƿ񿪷ţ�
	 * @return boolean true ���Ż�δ���� false δ����
	 * @throws java.lang.Exception
	 */
    public boolean isAvilible() {
        return channelScheduleDao.isAvilible(strChannelPath);
    }
}
