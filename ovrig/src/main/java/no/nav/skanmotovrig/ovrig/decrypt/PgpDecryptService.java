package no.nav.skanmotovrig.ovrig.decrypt;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Iterator;

@Component
@Slf4j
public class PgpDecryptService {

	@NotNull
	private final char[] privateKeyPassword;

	@NotNull
	private final File privateKey;

	@Autowired
	public PgpDecryptService(@Value("${pgp.passphrase}") char[] privateKeyPassword,
							 @Value("${skanmotovrig.pgp.privateKey}") String pathToPgpPrivateKey) {
		this.privateKeyPassword = privateKeyPassword;
		this.privateKey = new File(pathToPgpPrivateKey);

		Security.addProvider(new BouncyCastleProvider());
	}

	@Handler
	public void decryptMessage(Exchange exchange) throws IOException, PGPException {
		exchange.getIn().setBody(decryptFile(exchange.getIn().getBody(InputStream.class)));
	}

	private InputStream decryptFile(InputStream encryptedDataStream) throws IOException, PGPException  {
		InputStream in = PGPUtil.getDecoderStream(encryptedDataStream);
		InputStream privateKeyStream = getPrivateKey();

		try (privateKeyStream) {
			PGPEncryptedDataList enc = getPgpEncryptedData(in);
			InputStream clear = findPrivateKeyAndDecrypt(privateKeyStream, privateKeyPassword, enc);

			JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clear);
			PGPCompressedData cData = (PGPCompressedData) plainFact.nextObject();
			InputStream compressedStream = new BufferedInputStream(cData.getDataStream());
			JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(compressedStream);
			Object message = pgpFact.nextObject();

			if (message instanceof PGPLiteralData literalDataMessage) {
				return literalDataMessage.getDataStream();
			} else if (message instanceof PGPOnePassSignatureList) {
				throw new PGPException("PGP-kryptert melding inneholder en signert melding - ingen faktiske data.");
			} else {
				throw new PGPException("PGP-kryptert melding har en ukjent datatype.");
			}

		} catch (PGPException e) {
			log.error("DecryptFile feilet med melding: " + e.getMessage(), e);
			throw e;
		}

	}

	private InputStream findPrivateKeyAndDecrypt(InputStream privateKeyStream, char[] privateKeyPassword, PGPEncryptedDataList encryptedDataList) throws IOException, PGPException {
		// Find secret key (private key)
		PGPPrivateKey pgpPrivateKey = null;
		PGPPublicKeyEncryptedData publicKeyEncryptedData = null;
		PGPSecretKeyRingCollection pgpKeyRing = new PGPSecretKeyRingCollection(
				PGPUtil.getDecoderStream(privateKeyStream),
				new JcaKeyFingerprintCalculator()
		);

		Iterator<PGPEncryptedData> it = encryptedDataList.getEncryptedDataObjects();
		while (pgpPrivateKey == null && it.hasNext()) {
			publicKeyEncryptedData = (PGPPublicKeyEncryptedData) it.next();

			pgpPrivateKey = findSecretKey(pgpKeyRing, publicKeyEncryptedData.getKeyID(), privateKeyPassword);
		}

		if (pgpPrivateKey == null) {
			throw new IllegalArgumentException("Gyldig privatnøkkel for melding ble ikke funnet.");
		}

		return publicKeyEncryptedData.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(pgpPrivateKey));
	}

	private PGPEncryptedDataList getPgpEncryptedData(InputStream in) throws IOException {
		JcaPGPObjectFactory pgpFactory = new JcaPGPObjectFactory(in);

		// The first object might be a PGP marker packet.
		Object nextObjectInStream = pgpFactory.nextObject();
		if (nextObjectInStream instanceof PGPEncryptedDataList) {
			return (PGPEncryptedDataList) nextObjectInStream;
		} else {
			return (PGPEncryptedDataList) pgpFactory.nextObject();
		}
	}

	private BufferedInputStream getPrivateKey() throws FileNotFoundException {
		FileInputStream privateKeyStream = new FileInputStream(privateKey);

		return new BufferedInputStream(privateKeyStream);
	}

	private PGPPrivateKey findSecretKey(PGPSecretKeyRingCollection pgpSec, long keyID, char[] pass) throws PGPException {
		PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);

		if (pgpSecKey == null) {
			return null;
		}

		return pgpSecKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pass));
	}

}
