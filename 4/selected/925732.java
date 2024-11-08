package eg.nileu.cis.nilestore.uri;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bitpedia.util.Base32;
import eg.nileu.cis.nilestore.interfaces.uri.IMutableFileURI;
import eg.nileu.cis.nilestore.interfaces.uri.IVerifierURI;
import eg.nileu.cis.nilestore.utils.hashutils.Hash;

/**
 * The Class WritableSSKFileURI.
 * 
 * @author Mahmoud Ismail <mahmoudahmedismail@gmail.com>
 */
public class WritableSSKFileURI implements IMutableFileURI {

    /** The Constant BASE_STRING. */
    public static final String BASE_STRING = "URI:SSK:";

    /** The pattern. */
    private final Pattern pattern = Pattern.compile(BASE_STRING + uri.BASE32_CHARS + ":" + uri.BASE32_CHARS);

    /** The writekey. */
    private final byte[] writekey;

    /** The readkey. */
    private final byte[] readkey;

    /** The fingerprint. */
    private final byte[] fingerprint;

    /** The storage index. */
    private final byte[] storageIndex;

    /**
	 * Instantiates a new writable ssk file uri.
	 * 
	 * @param writekey
	 *            the writekey
	 * @param fingerprint
	 *            the fingerprint
	 */
    public WritableSSKFileURI(byte[] writekey, byte[] fingerprint) {
        this.writekey = writekey;
        this.readkey = Hash.ssk_readkey_hash(writekey);
        this.storageIndex = Hash.ssk_storage_index_hash(readkey);
        this.fingerprint = fingerprint;
    }

    /**
	 * Instantiates a new writable ssk file uri.
	 * 
	 * @param cap
	 *            the cap
	 */
    public WritableSSKFileURI(String cap) {
        Matcher matcher = pattern.matcher(cap);
        if (matcher.matches()) {
            this.writekey = Base32.decode(matcher.group(1));
            this.readkey = Hash.ssk_readkey_hash(writekey);
            this.storageIndex = Hash.ssk_storage_index_hash(readkey);
            this.fingerprint = Base32.decode(matcher.group(2));
        } else {
            writekey = null;
            readkey = null;
            storageIndex = null;
            fingerprint = null;
        }
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public boolean isReadonly() {
        return false;
    }

    @Override
    public byte[] getStorageIndex() {
        return storageIndex;
    }

    @Override
    public ReadonlySSKFileURI getReadonlyCap() {
        return new ReadonlySSKFileURI(readkey, fingerprint);
    }

    @Override
    public IVerifierURI getVerifyCap() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s%s:%s", BASE_STRING, Base32.encode(writekey), Base32.encode(fingerprint));
    }
}
