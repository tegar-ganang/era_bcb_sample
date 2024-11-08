package org.mozilla.javascript;

/**
 * This class describes the support needed to implement security.
 * <p>
 * Three main pieces of functionality are required to implement
 * security for JavaScript. First, it must be possible to define
 * classes with an associated security domain. (This security
 * domain may be any object incorporating notion of access
 * restrictions that has meaning to an embedding; for a client-side
 * JavaScript embedding this would typically be
 * java.security.ProtectionDomain or similar object depending on an
 * origin URL and/or a digital certificate.)
 * Next it must be possible to get a security domain object that
 * allows a particular action only if all security domains
 * associated with code on the current Java stack allows it. And
 * finally, it must be possible to execute script code with
 * associated security domain injected into Java stack.
 * <p>
 * These three pieces of functionality are encapsulated in the
 * SecurityController class.
 *
 * @see org.mozilla.javascript.Context#setSecurityController(SecurityController)
 * @see java.lang.ClassLoader
 * @since 1.5 Release 4
 */
public abstract class SecurityController {

    private static SecurityController global;

    static SecurityController global() {
        return global;
    }

    /**
     * Check if global {@link SecurityController} was already installed.
     * @see #initGlobal(SecurityController controller)
     */
    public static boolean hasGlobal() {
        return global != null;
    }

    /**
     * Initialize global controller that will be used for all
     * security-related operations. The global controller takes precedence
     * over already installed {@link Context}-specific controllers and cause
     * any subsequent call to
     * {@link Context#setSecurityController(SecurityController)}
     * to throw an exception.
     * <p>
     * The method can only be called once.
     *
     * @see #hasGlobal()
     */
    public static void initGlobal(SecurityController controller) {
        if (controller == null) throw new IllegalArgumentException();
        if (global != null) {
            throw new SecurityException("Cannot overwrite already installed global SecurityController");
        }
        global = controller;
    }

    /**
     * Get dynamic security domain that allows an action only if it is allowed
     * by the current Java stack and <i>securityDomain</i>. If
     * <i>securityDomain</i> is null, return domain representing permissions
     * allowed by the current stack.
     */
    public abstract Object getDynamicSecurityDomain(Object securityDomain);

    /**
     * Call {@link
     * Callable#call(Context cx, Scriptable scope, Scriptable thisObj,
     *               Object[] args)}
     * of <i>callable</i> under restricted security domain where an action is
     * allowed only if it is allowed according to the Java stack on the
     * moment of the <i>execWithDomain</i> call and <i>securityDomain</i>.
     * Any call to {@link #getDynamicSecurityDomain(Object)} during
     * execution of <tt>callable.call(cx, scope, thisObj, args)</tt>
     * should return a domain incorporate restrictions imposed by
     * <i>securityDomain</i> and Java stack on the moment of callWithDomain
     * invocation.
     * <p>
     * The method should always be overridden, it is not declared abstract
     * for compatibility reasons.
     */
    public Object callWithDomain(Object securityDomain, Context cx, final Callable callable, Scriptable scope, final Scriptable thisObj, final Object[] args) {
        return execWithDomain(cx, scope, new Script() {

            public Object exec(Context cx, Scriptable scope) {
                return callable.call(cx, scope, thisObj, args);
            }
        }, securityDomain);
    }

    /**
     * @deprecated The application should not override this method and instead
     * override
     * {@link #callWithDomain(Object securityDomain, Context cx, Callable callable, Scriptable scope, Scriptable thisObj, Object[] args)}.
     */
    public Object execWithDomain(Context cx, Scriptable scope, Script script, Object securityDomain) {
        throw new IllegalStateException("callWithDomain should be overridden");
    }
}
