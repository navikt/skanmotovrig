package no.nav.skanmotovrig.ovrig.decrypt;

import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.util.Iterator;

import static no.nav.skanmotovrig.ovrig.decrypt.PGPKeyUtil.findSecretKey;

public class PGPDecryptUtil {

	/**
	 * Decrypt the passed in message stream
	 *
	 * @return InputStream
	 */
	public static InputStream decryptFile(
			InputStream encryptedDataStream,
			InputStream privateKeyStream,
			char[] passwd)
			throws IOException, NoSuchProviderException {
		InputStream in = PGPUtil.getDecoderStream(encryptedDataStream);

		try (privateKeyStream) {
			PGPEncryptedDataList enc = getPgpEncryptedData(in);
			InputStream clear = findPrivateKeyAndDecrypt(privateKeyStream, passwd, enc);

			JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clear);
			PGPCompressedData cData = (PGPCompressedData) plainFact.nextObject();
			InputStream compressedStream = new BufferedInputStream(cData.getDataStream());
			JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(compressedStream);
			Object message = pgpFact.nextObject();

			if (message instanceof PGPLiteralData literalDataMessage) {
				return literalDataMessage.getInputStream();
			} else if (message instanceof PGPOnePassSignatureList) {
				throw new PGPException("Encrypted message contains a signed message - not literal data.");
			} else {
				throw new PGPException("Message is not a simple encrypted file - type unknown.");
			}
			/*
			if (publicKeyEncryptedData.isIntegrityProtected()) {
				if (!publicKeyEncryptedData.verify()) {
					System.err.println("message failed integrity check");
				} else {
					System.err.println("message integrity check passed");
				}
			} else {
				System.err.println("no message integrity check");
			}*/
		} catch (PGPException e) {
			System.err.println(e);
			if (e.getUnderlyingException() != null) {
				e.getUnderlyingException().printStackTrace();
			}
		}

		throw new SkanmotovrigFunctionalException("Noe galt");
	}

	private static InputStream findPrivateKeyAndDecrypt(InputStream privateKeyStream, char[] passwd, PGPEncryptedDataList encryptedDataList) throws IOException, PGPException, NoSuchProviderException {
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

	private static PGPEncryptedDataList getPgpEncryptedData(InputStream in) throws IOException {
		JcaPGPObjectFactory pgpFactory = new JcaPGPObjectFactory(in);

		// The first object might be a PGP marker packet.
		Object nextObjectInStream = pgpFactory.nextObject();
		if (nextObjectInStream instanceof PGPEncryptedDataList) {
			return (PGPEncryptedDataList) nextObjectInStream;
		} else {
			return (PGPEncryptedDataList) pgpFactory.nextObject();
		}
	}
}
