package edu.vt.middleware.crypt.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import edu.vt.middleware.crypt.CryptException;

/**
 * Reads symmetric algorithm secret keys.
 *
 * @author  Middleware Services
 * @version  $Revision: 1818 $
 */
public class SecretKeyCredentialReader implements CredentialReader<SecretKey> {

    /** Secret key algorithm. */
    private final String algorithm;

    /**
   * Creates a new instance that can read keys for the given symmetric cipher
   * algorithm.
   *
   * @param  cipherAlgorithm  Cipher algorithm name, e.g. AES.
   */
    public SecretKeyCredentialReader(final String cipherAlgorithm) {
        this.algorithm = cipherAlgorithm;
    }

    /** {@inheritDoc} */
    public SecretKey read(final File file) throws IOException, CryptException {
        final byte[] data = IOHelper.read(new FileInputStream(file).getChannel());
        try {
            return new SecretKeySpec(data, algorithm);
        } catch (Exception e) {
            throw new CryptException("Invalid key format.", e);
        }
    }

    /** {@inheritDoc} */
    public SecretKey read(final InputStream in) throws IOException, CryptException {
        final byte[] data = IOHelper.read(in);
        try {
            return new SecretKeySpec(data, algorithm);
        } catch (Exception e) {
            throw new CryptException("Invalid key format.", e);
        }
    }
}
