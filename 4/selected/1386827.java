package com.sun.star.comp.helper;

import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.RuntimeException;

/** The class provides a set of methods which create instances of the
	com.sun.star.lang.RegistryServiceManager service. 
	
    @deprecated use class Bootstrap instead
*/
public class RegistryServiceFactory {

    static {
        System.loadLibrary("juh");
    }

    private static native Object createRegistryServiceFactory(String writeRegistryFile, String readRegistryFile, boolean readOnly, ClassLoader loader);

    /**
 	 * This bootstraps an initial service factory working on a registry. If the first or both 
 	 * parameters contain a value then the service factory is initialized with a simple registry
 	 * or a nested registry. Otherwise the service factory must be initialized later with a valid
 	 * registry.
 	 *<BR>
 	 * @param writeRegistryFile	file name of the simple registry or the first registry file of
 	 *								the nested registry which will be opened with read/write rights. This
 	 *								file will be created if necessary.
 	 * @param readRegistryFile		file name of the second registry file of the nested registry 
 	 *								which will be opened with readonly rights.
	 * @return a new RegistryServiceFactory.
 	 */
    public static XMultiServiceFactory create(String writeRegistryFile, String readRegistryFile) throws com.sun.star.uno.Exception {
        return create(writeRegistryFile, readRegistryFile, false);
    }

    /**
 	 * This bootstraps an initial service factory working on a registry. If the first or both 
 	 * parameters contain a value then the service factory is initialized with a simple registry
 	 * or a nested registry. Otherwise the service factory must be initialized later with a valid
 	 * registry.
 	 *<BR>
     * @param writeRegistryFile	    file name of the simple registry or the first registry file of
     *								the nested registry which will be opened with read/write rights. This
     *								file will be created if necessary.
     * @param readRegistryFile		file name of the second registry file of the nested registry 
     *								which will be opened with readonly rights.
     * @param readOnly				flag which specify that the first registry file will be opened with 
     *								readonly rights. Default is FALSE. If this flag is used the registry 
     *								will not be created if not exist.
     *
 	 * @return a new RegistryServiceFactory
 	 */
    public static XMultiServiceFactory create(String writeRegistryFile, String readRegistryFile, boolean readOnly) throws com.sun.star.uno.Exception {
        String vm_info = System.getProperty("java.vm.info");
        if (vm_info != null && vm_info.indexOf("green") != -1) throw new RuntimeException(RegistryServiceFactory.class.toString() + ".create - can't use binary UNO with green threads");
        if (writeRegistryFile == null && readRegistryFile == null) throw new com.sun.star.uno.Exception("No registry is specified!");
        Object obj = createRegistryServiceFactory(writeRegistryFile, readRegistryFile, readOnly, RegistryServiceFactory.class.getClassLoader());
        return (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class, obj);
    }

    /**
	 * This bootstraps an initial service factory working on a registry file.
	 *<BR>
	 * @param registryFile			file name of the registry to use/ create; if this is an empty
	 *								string, the default registry is used instead
	 *
	 * @return a new RegistryServiceFactory.
	 */
    public static XMultiServiceFactory create(String registryFile) throws com.sun.star.uno.Exception {
        return create(registryFile, null, false);
    }

    /**
	 * This bootstraps an initial service factory working on a registry file.
	 *<BR>
     * @param registryFile			file name of the registry to use/ create; if this is an empty
     *								string, the default registry is used instead
     * @param readOnly				flag which specify that the registry file will be opened with 
     *								readonly rights. Default is FALSE. If this flag is used the registry 
     *								will not be created if not exist.
	 *								
	 * @return a new RegistryServiceFactory.
	 */
    public static XMultiServiceFactory create(String registryFile, boolean readOnly) throws com.sun.star.uno.Exception {
        return create(registryFile, null, readOnly);
    }

    /**
	 * This bootstraps a service factory without initialize a registry.
	 *<BR>
	 * @return a new RegistryServiceFactory.
	 */
    public static XMultiServiceFactory create() throws com.sun.star.uno.Exception {
        return create(null, null, false);
    }
}
