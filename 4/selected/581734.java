package watij.iwshruntimelibrary.server;

import com.jniwrapper.*;
import com.jniwrapper.win32.*;
import com.jniwrapper.win32.automation.*;
import com.jniwrapper.win32.automation.impl.*;
import com.jniwrapper.win32.automation.server.*;
import com.jniwrapper.win32.automation.types.*;
import com.jniwrapper.win32.com.*;
import com.jniwrapper.win32.com.impl.*;
import com.jniwrapper.win32.com.server.*;
import com.jniwrapper.win32.com.types.*;
import watij.iwshruntimelibrary.*;
import watij.iwshruntimelibrary.impl.*;

/**
 * Represents VTBL for COM interface ITextStream.
 */
public class ITextStreamVTBL extends IDispatchVTBL {

    public ITextStreamVTBL(CoClassMetaInfo classMetaInfo) {
        super(classMetaInfo);
        addMembers(new VirtualMethodCallback[] { new VirtualMethodCallback("getLine", new HResult(), new Parameter[] { new Pointer(new Int32()) }, 0), new VirtualMethodCallback("getColumn", new HResult(), new Parameter[] { new Pointer(new Int32()) }, 0), new VirtualMethodCallback("getAtEndOfStream", new HResult(), new Parameter[] { new Pointer(new VariantBool()) }, 0), new VirtualMethodCallback("getAtEndOfLine", new HResult(), new Parameter[] { new Pointer(new VariantBool()) }, 0), new VirtualMethodCallback("read", new HResult(), new Parameter[] { new Int32(), new Pointer(new BStr()) }, 1), new VirtualMethodCallback("readLine", new HResult(), new Parameter[] { new Pointer(new BStr()) }, 0), new VirtualMethodCallback("readAll", new HResult(), new Parameter[] { new Pointer(new BStr()) }, 0), new VirtualMethodCallback("write", new HResult(), new Parameter[] { new BStr() }), new VirtualMethodCallback("writeLine", new HResult(), new Parameter[] { new BStr() }), new VirtualMethodCallback("writeBlankLines", new HResult(), new Parameter[] { new Int32() }), new VirtualMethodCallback("skip", new HResult(), new Parameter[] { new Int32() }), new VirtualMethodCallback("skipLine", new HResult(), new Parameter[] {}), new VirtualMethodCallback("close", new HResult(), new Parameter[] {}) });
    }
}
