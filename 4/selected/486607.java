package at.fhjoanneum.aim.sdi.project.service.impl;

import java.util.Vector;
import at.fhjoanneum.aim.sdi.project.exceptions.CreateGroupException;
import at.fhjoanneum.aim.sdi.project.svnconfig.Directory;
import at.fhjoanneum.aim.sdi.project.svnconfig.Permission;
import at.fhjoanneum.aim.sdi.project.svnconfig.Repository;
import at.fhjoanneum.aim.sdi.project.svnconfig.Target;

/**
 * 
 * @author WaltCh
 *
 */
public class CreatePermission {

    /**
	 * Use this method to permit or deny access of a group to a certain directory of a 
	 * repository.
	 * @param acGroup, an AccessGroup Object that is (not) allowed to access the target.
	 * @param target, a Directory object that is a path in the current SVJ repository.
	 * @param perm, read and write permissions on a certain directory.
	 * @throws CreateGroupException
	 */
    public void createPermissionToTarget(Target groupOrUser, Directory target, Permission perm) throws CreateGroupException {
        if (!groupOrUser.getName().startsWith("@")) {
            groupOrUser.setName("@" + groupOrUser.getName());
        }
        for (int i = 0; i < RepositoryAccessService.getRepoAccess().size(); i++) {
            if (((Vector<Repository>) RepositoryAccessService.getRepoAccess()).get(i).getName().equals(target.getPath().substring(0, target.getPath().indexOf("/")) + "/]")) {
                for (int j = 0; j < ((Vector<Repository>) RepositoryAccessService.getRepoAccess()).get(i).getDirectories().size(); j++) {
                    if (((Vector<Repository>) RepositoryAccessService.getRepoAccess()).get(i).getDirectories().get(j).getPath().equals(target.getPath())) {
                        ((Vector<Repository>) RepositoryAccessService.getRepoAccess()).get(i).getDirectories().get(j).addDirAccess(groupOrUser, perm);
                        break;
                    }
                }
            }
        }
    }

    /**
	 * Use this method to permit or deny access of a group to a certain directory of a 
	 * repository.
	 * @param acGroup, an AccessGroup Object that is (not) allowed to access the target.
	 * @param target, a Directory object that is a path in the current SVJ repository.
	 * @param readAccess, if true read access to the current directory is permitted, otherwise denied.
	 * @param writeAccess, if true write access to the current directory is permitted, otherwise denied.
	 * @throws CreateGroupException
	 */
    public void createPermissionToTarget(Target groupOrUser, Directory target, boolean readAccess, boolean writeAccess) throws CreateGroupException {
        Permission p = new Permission();
        p.setReadable(readAccess);
        p.setWriteable(writeAccess);
        createPermissionToTarget(groupOrUser, target, p);
    }
}
