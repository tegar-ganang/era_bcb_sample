package org.ofbiz.accounting.thirdparty.valuelink;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.IvParameterSpec;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * ValueLinkApi - Implementation of ValueLink Encryption & Transport
 */
public class ValueLinkApi {

    public static final String module = ValueLinkApi.class.getName();

    private static Map objectCache = new HashMap();

    protected GenericDelegator delegator = null;

    protected Properties props = null;

    protected SecretKey kek = null;

    protected SecretKey mwk = null;

    protected String merchantId = null;

    protected String terminalId = null;

    protected Long mwkIndex = null;

    protected boolean debug = false;

    protected ValueLinkApi() {
    }

    protected ValueLinkApi(GenericDelegator delegator, Properties props) {
        String mId = (String) props.get("payment.valuelink.merchantId");
        String tId = (String) props.get("payment.valuelink.terminalId");
        this.delegator = delegator;
        this.merchantId = mId;
        this.terminalId = tId;
        this.props = props;
        if ("Y".equalsIgnoreCase((String) props.get("payment.valuelink.debug"))) {
            this.debug = true;
        }
        if (debug) {
            Debug.log("New ValueLinkApi instance created", module);
            Debug.log("Merchant ID : " + merchantId, module);
            Debug.log("Terminal ID : " + terminalId, module);
        }
    }

    /**
     * Obtain an instance of the ValueLinkApi
     * @param delegator GenericDelegator used to query the encryption keys
     * @param props Properties to use for the Api (usually payment.properties)
     * @param reload When true, will replace an existing instance in the cache and reload all properties
     * @return ValueLinkApi reference
     */
    public static ValueLinkApi getInstance(GenericDelegator delegator, Properties props, boolean reload) {
        String merchantId = (String) props.get("payment.valuelink.merchantId");
        if (props == null) {
            throw new IllegalArgumentException("Properties cannot be null");
        }
        ValueLinkApi api = (ValueLinkApi) objectCache.get(merchantId);
        if (api == null || reload) {
            synchronized (ValueLinkApi.class) {
                api = (ValueLinkApi) objectCache.get(merchantId);
                if (api == null || reload) {
                    api = new ValueLinkApi(delegator, props);
                    objectCache.put(merchantId, api);
                }
            }
        }
        if (api == null) {
            throw new RuntimeException("Runtime problems with ValueLinkApi; unable to create instance");
        }
        return api;
    }

    /**
     * Obtain an instance of the ValueLinkApi; this method will always return an existing reference if one is available
     * @param delegator GenericDelegator used to query the encryption keys
     * @param props Properties to use for the Api (usually payment.properties)
     * @return
     */
    public static ValueLinkApi getInstance(GenericDelegator delegator, Properties props) {
        return getInstance(delegator, props, false);
    }

    /**
     * Encrypt the defined pin using the configured keys
     * @param pin Plain text String of the pin
     * @return Hex String of the encrypted pin (EAN) for transmission to ValueLink
     */
    public String encryptPin(String pin) {
        Cipher mwkCipher = this.getCipher(this.getMwkKey(), Cipher.ENCRYPT_MODE);
        byte[] pinBytes = pin.getBytes();
        byte[] random = this.getRandomBytes(7);
        byte[] checkSum = this.getPinCheckSum(pinBytes);
        byte[] eanBlock = new byte[16];
        int i;
        for (i = 0; i < random.length; i++) {
            eanBlock[i] = random[i];
        }
        eanBlock[7] = checkSum[0];
        for (i = 0; i < pinBytes.length; i++) {
            eanBlock[i + 8] = pinBytes[i];
        }
        String encryptedEanHex = null;
        try {
            byte[] encryptedEan = mwkCipher.doFinal(eanBlock);
            encryptedEanHex = StringUtil.toHexString(encryptedEan);
        } catch (IllegalStateException e) {
            Debug.logError(e, module);
        } catch (IllegalBlockSizeException e) {
            Debug.logError(e, module);
        } catch (BadPaddingException e) {
            Debug.logError(e, module);
        }
        if (debug) {
            Debug.log("encryptPin : " + pin + " / " + encryptedEanHex, module);
        }
        return encryptedEanHex;
    }

    /**
     * Decrypt an encrypted pin using the configured keys
     * @param pin Hex String of the encrypted pin (EAN)
     * @return Plain text String of the pin
     */
    public String decryptPin(String pin) {
        Cipher mwkCipher = this.getCipher(this.getMwkKey(), Cipher.DECRYPT_MODE);
        String decryptedPinString = null;
        try {
            byte[] decryptedEan = mwkCipher.doFinal(StringUtil.fromHexString(pin));
            byte[] decryptedPin = getByteRange(decryptedEan, 8, 8);
            decryptedPinString = new String(decryptedPin);
        } catch (IllegalStateException e) {
            Debug.logError(e, module);
        } catch (IllegalBlockSizeException e) {
            Debug.logError(e, module);
        } catch (BadPaddingException e) {
            Debug.logError(e, module);
        }
        if (debug) {
            Debug.log("decryptPin : " + pin + " / " + decryptedPinString, module);
        }
        return decryptedPinString;
    }

    /**
     * Transmit a request to ValueLink
     * @param request Map of request parameters
     * @return Map of response parameters
     * @throws HttpClientException
     */
    public Map send(Map request) throws HttpClientException {
        return send((String) props.get("payment.valuelink.url"), request);
    }

    /**
     * Transmit a request to ValueLink
     * @param url override URL from what is defined in the properties
     * @param request request Map of request parameters
     * @return Map of response parameters
     * @throws HttpClientException
     */
    public Map send(String url, Map request) throws HttpClientException {
        if (debug) {
            Debug.log("Request : " + url + " / " + request, module);
        }
        String timeoutString = (String) props.get("payment.valuelink.timeout");
        int timeout = 34;
        try {
            timeout = Integer.parseInt(timeoutString);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to set timeout to " + timeoutString + " using default " + timeout);
        }
        HttpClient client = new HttpClient(url, request);
        client.setTimeout(timeout * 1000);
        client.setDebug(debug);
        client.setClientCertificateAlias((String) props.get("payment.valuelink.certificateAlias"));
        String response = client.post();
        return this.parseResponse(response);
    }

    /**
     * Output the creation of public/private keys + KEK to the console for manual database update
     */
    public StringBuffer outputKeyCreation(boolean kekOnly, String kekTest) {
        return this.outputKeyCreation(0, kekOnly, kekTest);
    }

    private StringBuffer outputKeyCreation(int loop, boolean kekOnly, String kekTest) {
        StringBuffer buf = new StringBuffer();
        loop++;
        if (loop > 100) {
            throw new IllegalStateException("Unable to create 128 byte keys in 100 tries");
        }
        DHPrivateKey privateKey = null;
        DHPublicKey publicKey = null;
        if (!kekOnly) {
            KeyPair keyPair = null;
            try {
                keyPair = this.createKeys();
            } catch (NoSuchAlgorithmException e) {
                Debug.logError(e, module);
            } catch (InvalidAlgorithmParameterException e) {
                Debug.logError(e, module);
            } catch (InvalidKeySpecException e) {
                Debug.logError(e, module);
            }
            if (keyPair != null) {
                publicKey = (DHPublicKey) keyPair.getPublic();
                privateKey = (DHPrivateKey) keyPair.getPrivate();
                if (publicKey == null || publicKey.getY().toByteArray().length != 128) {
                    return this.outputKeyCreation(loop, kekOnly, kekTest);
                }
            } else {
                Debug.log("Returned a null KeyPair", module);
                return this.outputKeyCreation(loop, kekOnly, kekTest);
            }
        } else {
            try {
                privateKey = (DHPrivateKey) this.getPrivateKey();
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        }
        byte[] kekBytes = null;
        try {
            kekBytes = this.generateKek(privateKey);
        } catch (NoSuchAlgorithmException e) {
            Debug.logError(e, module);
        } catch (InvalidKeySpecException e) {
            Debug.logError(e, module);
        } catch (InvalidKeyException e) {
            Debug.logError(e, module);
        }
        SecretKey loadedKek = this.getDesEdeKey(kekBytes);
        byte[] loadKekBytes = loadedKek.getEncoded();
        Cipher cipher = this.getCipher(this.getKekKey(), Cipher.ENCRYPT_MODE);
        byte[] kekTestB = { 0, 0, 0, 0, 0, 0, 0, 0 };
        byte[] kekTestC = new byte[0];
        if (kekTest != null) {
            kekTestB = StringUtil.fromHexString(kekTest);
        }
        try {
            kekTestC = cipher.doFinal(kekTestB);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
        if (!kekOnly) {
            BigInteger y = publicKey.getY();
            byte[] yBytes = y.toByteArray();
            String yHex = StringUtil.toHexString(yBytes);
            buf.append("======== Begin Public Key (Y @ " + yBytes.length + " / " + yHex.length() + ") ========\n");
            buf.append(yHex + "\n");
            buf.append("======== End Public Key ========\n\n");
            BigInteger x = privateKey.getX();
            byte[] xBytes = x.toByteArray();
            String xHex = StringUtil.toHexString(xBytes);
            buf.append("======== Begin Private Key (X @ " + xBytes.length + " / " + xHex.length() + ") ========\n");
            buf.append(xHex + "\n");
            buf.append("======== End Private Key ========\n\n");
            byte[] privateBytes = privateKey.getEncoded();
            String privateHex = StringUtil.toHexString(privateBytes);
            buf.append("======== Begin Private Key (Full @ " + privateBytes.length + " / " + privateHex.length() + ") ========\n");
            buf.append(privateHex + "\n");
            buf.append("======== End Private Key ========\n\n");
        }
        if (kekBytes != null) {
            buf.append("======== Begin KEK (" + kekBytes.length + ") ========\n");
            buf.append(StringUtil.toHexString(kekBytes) + "\n");
            buf.append("======== End KEK ========\n\n");
            buf.append("======== Begin KEK (DES) (" + loadKekBytes.length + ") ========\n");
            buf.append(StringUtil.toHexString(loadKekBytes) + "\n");
            buf.append("======== End KEK (DES) ========\n\n");
            buf.append("======== Begin KEK Test (" + kekTestC.length + ") ========\n");
            buf.append(StringUtil.toHexString(kekTestC) + "\n");
            buf.append("======== End KEK Test ========\n\n");
        } else {
            Debug.logError("KEK came back empty", module);
        }
        return buf;
    }

    /**
     * Create a set of public/private keys using ValueLinks defined parameters
     * @return KeyPair object containing both public and private keys
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     */
    public KeyPair createKeys() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        DHPublicKey publicKey = (DHPublicKey) this.getValueLinkPublicKey();
        DHParameterSpec dhParamSpec = publicKey.getParams();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(dhParamSpec);
        KeyPair keyPair = keyGen.generateKeyPair();
        return keyPair;
    }

    /**
     * Generate a key exchange key for use in encrypting the mwk
     * @param privateKey The private key for the merchant
     * @return byte array containing the kek
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     */
    public byte[] generateKek(PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        PublicKey vlPublic = this.getValueLinkPublicKey();
        KeyAgreement ka = KeyAgreement.getInstance("DH");
        ka.init(privateKey);
        ka.doPhase(vlPublic, true);
        byte[] secretKey = ka.generateSecret();
        if (debug) {
            Debug.log("Secret Key : " + StringUtil.toHexString(secretKey) + " / " + secretKey.length, module);
        }
        MessageDigest md = MessageDigest.getInstance("SHA1");
        byte[] digest = md.digest(secretKey);
        byte[] des2 = getByteRange(digest, 0, 16);
        byte[] first8 = getByteRange(des2, 0, 8);
        byte[] kek = copyBytes(des2, first8, 0);
        if (debug) {
            Debug.log("Generated KEK : " + StringUtil.toHexString(kek) + " / " + kek.length, module);
        }
        return kek;
    }

    /**
     * Get a public key object for the ValueLink supplied public key
     * @return PublicKey object of ValueLinks's public key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public PublicKey getValueLinkPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String publicValue = (String) props.get("payment.valuelink.publicValue");
        byte[] publicKeyBytes = StringUtil.fromHexString(publicValue);
        DHParameterSpec dhParamSpec = this.getDHParameterSpec();
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        BigInteger publicKeyInt = new BigInteger(publicKeyBytes);
        DHPublicKeySpec dhPublicSpec = new DHPublicKeySpec(publicKeyInt, dhParamSpec.getP(), dhParamSpec.getG());
        PublicKey vlPublic = keyFactory.generatePublic(dhPublicSpec);
        return vlPublic;
    }

    /**
     * Get merchant Private Key
     * @return PrivateKey object for the merchant
     */
    public PrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] privateKeyBytes = this.getPrivateKeyBytes();
        DHParameterSpec dhParamSpec = this.getDHParameterSpec();
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        BigInteger privateKeyInt = new BigInteger(privateKeyBytes);
        DHPrivateKeySpec dhPrivateSpec = new DHPrivateKeySpec(privateKeyInt, dhParamSpec.getP(), dhParamSpec.getG());
        PrivateKey privateKey = keyFactory.generatePrivate(dhPrivateSpec);
        return privateKey;
    }

    /**
     * Generate a new MWK
     * @return Hex String of the new encrypted MWK ready for transmission to ValueLink
     */
    public byte[] generateMwk() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("DES");
        } catch (NoSuchAlgorithmException e) {
            Debug.logError(e, module);
        }
        SecretKey des1 = keyGen.generateKey();
        SecretKey des2 = keyGen.generateKey();
        if (des1 != null && des2 != null) {
            byte[] desByte1 = des1.getEncoded();
            byte[] desByte2 = des2.getEncoded();
            byte[] desByte3 = des1.getEncoded();
            try {
                if (DESKeySpec.isWeak(des1.getEncoded(), 0) || DESKeySpec.isWeak(des2.getEncoded(), 0)) {
                    return generateMwk();
                }
            } catch (Exception e) {
                Debug.logError(e, module);
            }
            byte[] des3 = copyBytes(desByte1, copyBytes(desByte2, desByte3, 0), 0);
            return generateMwk(des3);
        } else {
            Debug.log("Null DES keys returned", module);
        }
        return null;
    }

    /**
     * Generate a new MWK
     * @param desBytes byte array of the DES key (24 bytes)
     * @return Hex String of the new encrypted MWK ready for transmission to ValueLink
     */
    public byte[] generateMwk(byte[] desBytes) {
        if (debug) {
            Debug.log("DES Key : " + StringUtil.toHexString(desBytes) + " / " + desBytes.length, module);
        }
        SecretKeyFactory skf1 = null;
        SecretKey mwk = null;
        try {
            skf1 = SecretKeyFactory.getInstance("DESede");
        } catch (NoSuchAlgorithmException e) {
            Debug.logError(e, module);
        }
        DESedeKeySpec desedeSpec2 = null;
        try {
            desedeSpec2 = new DESedeKeySpec(desBytes);
        } catch (InvalidKeyException e) {
            Debug.logError(e, module);
        }
        if (skf1 != null && desedeSpec2 != null) {
            try {
                mwk = skf1.generateSecret(desedeSpec2);
            } catch (InvalidKeySpecException e) {
                Debug.logError(e, module);
            }
        }
        if (mwk != null) {
            return generateMwk(mwk);
        } else {
            return null;
        }
    }

    /**
     * Generate a new MWK
     * @param mwkdes3 pre-generated DES3 SecretKey
     * @return Hex String of the new encrypted MWK ready for transmission to ValueLink
     */
    public byte[] generateMwk(SecretKey mwkdes3) {
        byte[] zeros = { 0, 0, 0, 0, 0, 0, 0, 0 };
        byte[] random = new byte[8];
        Random ran = new Random();
        ran.nextBytes(random);
        Cipher cipher = this.getCipher(mwkdes3, Cipher.ENCRYPT_MODE);
        byte[] encryptedZeros = new byte[0];
        try {
            encryptedZeros = cipher.doFinal(zeros);
        } catch (IllegalStateException e) {
            Debug.logError(e, module);
        } catch (IllegalBlockSizeException e) {
            Debug.logError(e, module);
        } catch (BadPaddingException e) {
            Debug.logError(e, module);
        }
        byte[] newMwk = copyBytes(mwkdes3.getEncoded(), encryptedZeros, 0);
        newMwk = copyBytes(random, newMwk, 0);
        if (debug) {
            Debug.log("Random 8 byte : " + StringUtil.toHexString(random), module);
            Debug.log("Encrypted 0's : " + StringUtil.toHexString(encryptedZeros), module);
            Debug.log("Decrypted MWK : " + StringUtil.toHexString(mwkdes3.getEncoded()) + " / " + mwkdes3.getEncoded().length, module);
            Debug.log("Encrypted MWK : " + StringUtil.toHexString(newMwk) + " / " + newMwk.length, module);
        }
        return newMwk;
    }

    /**
     * Use the KEK to encrypt a value usually the MWK
     * @param content byte array to encrypt
     * @return encrypted byte array
     */
    public byte[] encryptViaKek(byte[] content) {
        return cryptoViaKek(content, Cipher.ENCRYPT_MODE);
    }

    /**
     * Ue the KEK to decrypt a value
     * @param content byte array to decrypt
     * @return decrypted byte array
     */
    public byte[] decryptViaKek(byte[] content) {
        return cryptoViaKek(content, Cipher.DECRYPT_MODE);
    }

    /**
     * Returns a date string formatted as directed by ValueLink
     * @return ValueLink formatted date String
     */
    public String getDateString() {
        String format = (String) props.get("payment.valuelink.timestamp");
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date());
    }

    /**
     * Returns the current working key index
     * @return Long number of the current working key index
     */
    public Long getWorkingKeyIndex() {
        if (this.mwkIndex == null) {
            synchronized (this) {
                if (this.mwkIndex == null) {
                    this.mwkIndex = this.getGenericValue().getLong("workingKeyIndex");
                }
            }
        }
        if (debug) {
            Debug.log("Current Working Key Index : " + this.mwkIndex, module);
        }
        return this.mwkIndex;
    }

    /**
     * Returns a ValueLink formatted amount String
     * @param amount Double value to format
     * @return Formatted String
     */
    public String getAmount(Double amount) {
        if (amount == null) {
            return "0.00";
        }
        String currencyFormat = UtilProperties.getPropertyValue("general.properties", "currency.decimal.format", "##0.00");
        DecimalFormat formatter = new DecimalFormat(currencyFormat);
        String amountString = formatter.format(amount.doubleValue());
        Double newAmount = null;
        try {
            newAmount = new Double(formatter.parse(amountString).doubleValue());
        } catch (ParseException e) {
            Debug.logError(e, "Unable to parse amount Double");
        }
        String formattedString = null;
        if (newAmount != null) {
            double amountDouble = newAmount.doubleValue() * 100;
            formattedString = Integer.toString(new Double(amountDouble).intValue());
        }
        return formattedString;
    }

    /**
     * Returns a Double from a ValueLink formatted amount String
     * @param amount The ValueLink formatted amount String
     * @return Double object
     */
    public Double getAmount(String amount) {
        if (amount == null) {
            return new Double(0.00);
        }
        Double doubleAmount = new Double(amount);
        return new Double(doubleAmount.doubleValue() / 100);
    }

    public String getCurrency(String currency) {
        return "840";
    }

    /**
     * Creates a Map of initial request values (MerchID, AltMerchNo, Modes, MerchTime, TermTxnNo, EncryptID)
     * Note: For 2010 (assign working key) transaction, the EncryptID will need to be adjusted
     * @return Map containing the inital request values
     */
    public Map getInitialRequestMap(Map context) {
        Map request = new HashMap();
        request.put("MerchID", merchantId + terminalId);
        request.put("AltMerchNo", props.get("payment.valuelink.altMerchantId"));
        String modes = (String) props.get("payment.valuelink.modes");
        if (modes != null && modes.length() > 0) {
            request.put("Modes", modes);
        }
        String merchTime = (String) context.get("MerchTime");
        if (merchTime == null) {
            merchTime = this.getDateString();
        }
        request.put("MerchTime", merchTime);
        String termTxNo = (String) context.get("TermTxnNo");
        if (termTxNo == null) {
            termTxNo = delegator.getNextSeqId("ValueLinkKey");
        }
        request.put("TermTxnNo", termTxNo);
        request.put("EncryptID", this.getWorkingKeyIndex());
        if (debug) {
            Debug.log("Created Initial Request Map : " + request, module);
        }
        return request;
    }

    /**
     * Gets the cached value object for this merchant's keys
     * @return Cached GenericValue object
     */
    public GenericValue getGenericValue() {
        GenericValue value = null;
        try {
            value = delegator.findByPrimaryKeyCache("ValueLinkKey", UtilMisc.toMap("merchantId", merchantId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (value == null) {
            throw new RuntimeException("No ValueLinkKey record found for Merchant ID : " + merchantId);
        }
        return value;
    }

    /**
     * Reloads the keys in the object cache; use this when re-creating keys
     */
    public void reload() {
        this.kek = null;
        this.mwk = null;
        this.mwkIndex = null;
    }

    protected DHParameterSpec getDHParameterSpec() {
        String primeHex = (String) props.get("payment.valuelink.prime");
        String genString = (String) props.get("payment.valuelink.generator");
        byte[] primeByte = StringUtil.fromHexString(primeHex);
        BigInteger prime = new BigInteger(1, primeByte);
        BigInteger generator = new BigInteger(genString);
        DHParameterSpec dhParamSpec = new DHParameterSpec(prime, generator, 1024);
        return dhParamSpec;
    }

    protected byte[] cryptoViaKek(byte[] content, int mode) {
        Cipher cipher = this.getCipher(this.getKekKey(), mode);
        byte[] dec = new byte[0];
        try {
            dec = cipher.doFinal(content);
        } catch (IllegalStateException e) {
            Debug.logError(e, module);
        } catch (IllegalBlockSizeException e) {
            Debug.logError(e, module);
        } catch (BadPaddingException e) {
            Debug.logError(e, module);
        }
        return dec;
    }

    protected Cipher getCipher(SecretKey key, int mode) {
        byte[] zeros = { 0, 0, 0, 0, 0, 0, 0, 0 };
        IvParameterSpec iv = new IvParameterSpec(zeros);
        Cipher mwkCipher = null;
        try {
            mwkCipher = Cipher.getInstance("DESede/CBC/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            Debug.logError(e, module);
            return null;
        } catch (NoSuchPaddingException e) {
            Debug.logError(e, module);
        }
        try {
            mwkCipher.init(mode, key, iv);
        } catch (InvalidKeyException e) {
            Debug.logError(e, "Invalid key", module);
        } catch (InvalidAlgorithmParameterException e) {
            Debug.logError(e, module);
        }
        return mwkCipher;
    }

    protected byte[] getPinCheckSum(byte[] pinBytes) {
        byte[] checkSum = new byte[1];
        checkSum[0] = 0;
        for (int i = 0; i < pinBytes.length; i++) {
            checkSum[0] += pinBytes[i];
        }
        return checkSum;
    }

    protected byte[] getRandomBytes(int length) {
        Random rand = new Random();
        byte[] randomBytes = new byte[length];
        rand.nextBytes(randomBytes);
        return randomBytes;
    }

    protected SecretKey getMwkKey() {
        if (mwk == null) {
            mwk = this.getDesEdeKey(getByteRange(getMwk(), 8, 24));
        }
        if (debug) {
            Debug.log("Raw MWK : " + StringUtil.toHexString(getMwk()), module);
            Debug.log("MWK : " + StringUtil.toHexString(mwk.getEncoded()), module);
        }
        return mwk;
    }

    protected SecretKey getKekKey() {
        if (kek == null) {
            kek = this.getDesEdeKey(getKek());
        }
        if (debug) {
            Debug.log("Raw KEK : " + StringUtil.toHexString(getKek()), module);
            Debug.log("KEK : " + StringUtil.toHexString(kek.getEncoded()), module);
        }
        return kek;
    }

    protected SecretKey getDesEdeKey(byte[] rawKey) {
        SecretKeyFactory skf = null;
        try {
            skf = SecretKeyFactory.getInstance("DESede");
        } catch (NoSuchAlgorithmException e) {
            Debug.logError(e, module);
            return null;
        }
        if (rawKey.length > 0) {
            DESedeKeySpec desedeSpec1 = null;
            try {
                desedeSpec1 = new DESedeKeySpec(rawKey);
            } catch (InvalidKeyException e) {
                Debug.logError(e, "Not a valid DESede key", module);
                return null;
            }
            SecretKey key = null;
            try {
                key = skf.generateSecret(desedeSpec1);
            } catch (InvalidKeySpecException e) {
                Debug.logError(e, module);
            }
            return key;
        } else {
            throw new RuntimeException("No valid DESede key available");
        }
    }

    protected byte[] getMwk() {
        return StringUtil.fromHexString(this.getGenericValue().getString("workingKey"));
    }

    protected byte[] getKek() {
        return StringUtil.fromHexString(this.getGenericValue().getString("exchangeKey"));
    }

    protected byte[] getPrivateKeyBytes() {
        return StringUtil.fromHexString(this.getGenericValue().getString("privateKey"));
    }

    protected Map parseResponse(String response) {
        if (debug) {
            Debug.log("Raw Response : " + response, module);
        }
        String subResponse = response.toLowerCase();
        int firstIndex = subResponse.indexOf("<tr>");
        int lastIndex = subResponse.lastIndexOf("</tr>");
        subResponse = subResponse.substring(firstIndex, lastIndex);
        String history = null;
        List historyMapList = null;
        if (subResponse.indexOf("<table") > -1) {
            int startHistory = subResponse.indexOf("<table");
            int endHistory = subResponse.indexOf("</table>") + 8;
            history = subResponse.substring(startHistory, endHistory);
            subResponse = StringUtil.replaceString(subResponse, history, "[_HISTORY_]");
            historyMapList = this.parseHistoryResponse(history);
        }
        subResponse = StringUtil.replaceString(subResponse, "</tr>", "|");
        subResponse = StringUtil.replaceString(subResponse, "</td><td>", "=");
        subResponse = StringUtil.replaceString(subResponse, "<tr>", "");
        subResponse = StringUtil.replaceString(subResponse, "<td>", "");
        subResponse = StringUtil.replaceString(subResponse, "</td>", "");
        Map responseMap = StringUtil.strToMap(subResponse, true);
        responseMap.put("_rawHtmlResponse", response);
        if (history != null) {
            responseMap.put("_rawHistoryHtml", history);
            responseMap.put("history", historyMapList);
        }
        if (debug) {
            Debug.log("Response Map : " + responseMap, module);
        }
        return responseMap;
    }

    private List parseHistoryResponse(String response) {
        if (debug) {
            Debug.log("Raw History : " + response, module);
        }
        String subResponse = response.toLowerCase();
        int firstIndex = subResponse.indexOf("<tr>");
        int lastIndex = subResponse.lastIndexOf("</tr>");
        subResponse = subResponse.substring(firstIndex, lastIndex);
        subResponse = StringUtil.replaceString(subResponse, "<td>", "");
        subResponse = StringUtil.replaceString(subResponse, "</td>", "|");
        String testResponse = StringUtil.replaceString(subResponse, "<tr>", "");
        testResponse = StringUtil.replaceString(testResponse, "</tr>", "");
        testResponse = StringUtil.replaceString(testResponse, "|", "");
        testResponse = testResponse.trim();
        if (testResponse.length() == 0) {
            if (debug) {
                Debug.log("History did not contain any fields, returning null", module);
            }
            return null;
        }
        int valueStart = subResponse.indexOf("</tr>");
        String keys = subResponse.substring(4, valueStart - 1);
        String values = subResponse.substring(valueStart + 9, subResponse.length() - 6);
        values = StringUtil.replaceString(values, "|</tr><tr>", "&");
        List valueList = StringUtil.split(values, "&");
        List valueMap = new ArrayList();
        for (int i = 0; i < valueList.size(); i++) {
            valueMap.add(StringUtil.createMap(StringUtil.split(keys, "|"), StringUtil.split((String) valueList.get(i), "|")));
        }
        if (debug) {
            Debug.log("History Map : " + valueMap, module);
        }
        return valueMap;
    }

    /**
     * Returns a new byte[] from the offset of the defined byte[] with a specific number of bytes
     * @param bytes The byte[] to extract from
     * @param offset The starting postition
     * @param length The number of bytes to copy
     * @return a new byte[]
     */
    public static byte[] getByteRange(byte[] bytes, int offset, int length) {
        byte[] newBytes = new byte[length];
        for (int i = 0; i < length; i++) {
            newBytes[i] = bytes[offset + i];
        }
        return newBytes;
    }

    /**
     * Copies a byte[] into another byte[] starting at a specific position
     * @param source byte[] to copy from
     * @param target byte[] coping into
     * @param position the position on target where source will be copied to
     * @return a new byte[]
     */
    public static byte[] copyBytes(byte[] source, byte[] target, int position) {
        byte[] newBytes = new byte[target.length + source.length];
        for (int i = 0, n = 0, x = 0; i < newBytes.length; i++) {
            if (i < position || i > (position + source.length - 2)) {
                newBytes[i] = target[n];
                n++;
            } else {
                for (; x < source.length; x++) {
                    newBytes[i] = source[x];
                    if (source.length - 1 > x) {
                        i++;
                    }
                }
            }
        }
        return newBytes;
    }
}
