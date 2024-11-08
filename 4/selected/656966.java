package edu.columbia.hypercontent.util;

import org.jasig.portal.IChannelRegistryStore;
import org.jasig.portal.ChannelRegistryStoreFactory;
import org.jasig.portal.ChannelDefinition;
import org.jasig.portal.ChannelCategory;
import org.jasig.portal.security.IAuthorizationPrincipal;
import org.jasig.portal.security.IUpdatingPermissionManager;
import org.jasig.portal.security.IPermission;
import org.jasig.portal.security.provider.PersonImpl;
import org.jasig.portal.groups.IEntityGroup;
import org.jasig.portal.services.GroupService;
import org.jasig.portal.services.AuthorizationService;
import edu.columbia.hypercontent.contentmanager.CContentManager;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: Nov 20, 2003
 * Time: 5:45:20 PM
 * To change this template use Options | File Templates.
 */
public class AutoPublish {

    public static void main(String[] args) {
        boolean unPublish = (args.length > 0 && args[0].equals("unpublish"));
        try {
            boolean changed = false;
            IChannelRegistryStore store = ChannelRegistryStoreFactory.getChannelRegistryStoreImpl();
            ChannelDefinition def = store.getChannelDefinition("contentmanager");
            if (def == null && !unPublish) {
                def = store.newChannelDefinition();
                def.setFName("contentmanager");
                def.setDescription("Administrative front-end for HyperContent");
                def.setTitle("Content Manager");
                def.setName("Content Manager");
                def.setEditable(false);
                def.setHasAbout(false);
                def.setHasHelp(false);
                def.setJavaClass(CContentManager.class.getName());
                def.setPublishDate(new Date());
                def.setTimeout(60000);
                def.setTypeId(-1);
                ChannelCategory all = store.getTopLevelChannelCategory();
                ChannelCategory addTo = all;
                ChannelCategory[] top = store.getChildCategories(all);
                for (int i = 0; i < top.length; i++) {
                    ChannelCategory cat = top[i];
                    if (cat.getName().equals("Applications")) {
                        addTo = cat;
                        break;
                    }
                }
                store.saveChannelDefinition(def);
                store.addChannelToCategory(def, addTo);
                IEntityGroup admin = GroupService.getDistinguishedGroup(GroupService.PORTAL_ADMINISTRATORS);
                AuthorizationService auth = AuthorizationService.instance();
                IAuthorizationPrincipal ap = auth.newPrincipal(admin);
                IUpdatingPermissionManager up = auth.newUpdatingPermissionManager("UP_FRAMEWORK");
                IPermission perm = up.newPermission(ap);
                perm.setActivity(perm.CHANNEL_SUBSCRIBER_ACTIVITY);
                perm.setTarget(perm.CHANNEL_PREFIX + def.getId());
                perm.setType("GRANT");
                up.addPermissions(new IPermission[] { perm });
                store.approveChannelDefinition(def, new PersonImpl(), new Date());
                changed = true;
            } else if (def != null & unPublish) {
                store.deleteChannelDefinition(def);
                changed = true;
            }
            if (!changed) {
                System.out.println("No change required");
            } else if (unPublish) {
                System.out.println("Content Manager channel successfully un-published");
            } else {
                System.out.println("Content Manager channel successfully published");
            }
            System.exit(0);
        } catch (Exception e) {
            if (unPublish) {
                System.out.println("Unable to un-publish Content Manager channel");
            } else {
                System.out.println("Unable to publish Content Manager channel");
            }
            e.printStackTrace();
        }
    }
}
