package no.nav.skanmotovrig.ovrig.decrypt.pgpDecryptNew;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import static no.nav.skanmotovrig.ovrig.decrypt.PGPKeyUtil.findSecretKey;

@Component
@Slf4j
public class PgpDecryptService {

	//TODO: Fix så disse ikke er lagra i klassen
	@NotNull
	private final char[] passwd;
	@NotNull
	private final File privateKey;

	@Inject
	public PgpDecryptService(@Value("${pgp.password}") char[] passwd,
							 @Value("${pgp.privateKey}") File privateKey) {
		this.passwd = passwd;
		this.privateKey = new File("src/test/resources/pgp/privateKeyRSA.gpg");
	}

	@Handler
	public void decryptMessage(Exchange exchange) throws IOException, PGPException {
		//exchange.getIn().setBody(decryptFile(exchange.getIn().getBody(InputStream.class)));
		exchange.getIn().setBody(decryptFile(exchange.getIn().getBody(InputStream.class)));
	}

	private InputStream decryptFile(InputStream encryptedDataStream) throws IOException, PGPException  {
		//InputStream privateKeyStream = new ByteArrayInputStream(getPrivateKey().getBytes(StandardCharsets.UTF_8));
		InputStream in = PGPUtil.getDecoderStream(encryptedDataStream);
		InputStream privateKeyStream = getPrivateKey();

		try (privateKeyStream) {
			PGPEncryptedDataList enc = getPgpEncryptedData(in);
			InputStream clear = findPrivateKeyAndDecrypt(privateKeyStream, passwd, enc);
			//return findPrivateKeyAndDecrypt(privateKeyStream, passwd, enc).readAllBytes();

			JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clear);
			PGPCompressedData cData = (PGPCompressedData) plainFact.nextObject();
			InputStream compressedStream = new BufferedInputStream(cData.getDataStream());
			JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(compressedStream);
			Object message = pgpFact.nextObject();

			if (message instanceof PGPLiteralData literalDataMessage) {
				/*OutputStream out = new BufferedOutputStream(new FileOutputStream("test123.zip"));
				out.write(literalDataMessage.getDataStream().readAllBytes());
				out.close();*/
				return literalDataMessage.getDataStream();
			} else if (message instanceof PGPOnePassSignatureList) {
				throw new PGPException("Encrypted message contains a signed message - not literal data.");
			} else {
				throw new PGPException("Message is not a simple encrypted file - type unknown.");
			}

		} catch (PGPException e) {
			System.err.println(e);
			//TODO: handle
			throw e;
			/*if (e.getUnderlyingException() != null) {
				e.getUnderlyingException().printStackTrace();
			}*/
		}

	}

	private InputStream findPrivateKeyAndDecrypt(InputStream privateKeyStream, char[] passwd, PGPEncryptedDataList encryptedDataList) throws IOException, PGPException {
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

			pgpPrivateKey = findSecretKey(pgpKeyRing, publicKeyEncryptedData.getKeyID(), passwd);
		}

		if (pgpPrivateKey == null) {
			throw new IllegalArgumentException("Secret key for message not found.");
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

}
