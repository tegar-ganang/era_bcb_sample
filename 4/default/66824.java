import Euclid.*;

public class EuclidExport implements EuclidExporter {

    public String[] getExports() {
        return new String[] { "open", "close", "read", "write" };
    }

    public VarType open(VarType o) {
        if (o instanceof SymbVarType) return open(((SymbVarType) o).val); else if (o instanceof StringVarType) return new FileVarType(((StringVarType) o).val); else return null;
    }

    public VarType close(VarType o) {
        if (o instanceof SymbVarType) return close(((SymbVarType) o).val); else if (o instanceof FileVarType) return new StringVarType("Success"); else return null;
    }
}
