package no.nav.skanmotovrig.pgpDecrypt;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import no.nav.skanmotovrig.decrypt.ZipIteratorEncrypted;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Date;

public class zipSplitterPgpEncrypted extends ZipSplitter {

	private final String passphrase;

	@Inject
	public zipSplitterPgpEncrypted(@Value("${skanmotovrig.secret.passphrase}") String passphrase) {
		this.passphrase = passphrase;
	}

	@Override
	public InputStream evaluate(Exchange exchange) {
		Message inputMessage = exchange.getIn();
		ZipInputStream zip = new ZipInputStream(inputMessage.getBody(InputStream.class), passphrase.toCharArray());
		return null;
	}

	@Override
	public <T> T evaluate(Exchange exchange, Class<T> type) {
		Object result = this.evaluate(exchange);
		return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
	}

	public static byte[] createEncryptedData(
			PGPPublicKey encryptionKey,
			byte[] data)
			throws PGPException, IOException {

		PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
				new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
						.setWithIntegrityPacket(true)
						.setSecureRandom(new SecureRandom()).setProvider("BC"));
		encGen.addMethod(
				new JcePublicKeyKeyEncryptionMethodGenerator(encryptionKey)
						.setProvider("BC"));

		ByteArrayOutputStream encOut = new ByteArrayOutputStream();
		// create an indefinite length encrypted stream
		OutputStream cOut = encGen.open(encOut, new byte[4096]);
		// write out the literal data
		PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
		OutputStream pOut = lData.open(
				cOut, PGPLiteralData.BINARY,
				PGPLiteralData.CONSOLE, data.length, new Date());
		pOut.write(data);
		pOut.close();
		// finish the encryption
		cOut.close();
		return encOut.toByteArray();
	}

	public static byte[] extractPlainTextData(
			PGPPrivateKey privateKey,
			byte[] pgpEncryptedData)
			throws PGPException, IOException {
		PGPObjectFactory pgpFact = new JcaPGPObjectFactory(pgpEncryptedData);
		PGPEncryptedDataList encList = (PGPEncryptedDataList) pgpFact.nextObject();
		// find the matching public key encrypted data packet.
		PGPPublicKeyEncryptedData encData = null;
		for (PGPEncryptedData pgpEnc : encList) {
			PGPPublicKeyEncryptedData pkEnc
					= (PGPPublicKeyEncryptedData) pgpEnc;
			if (pkEnc.getKeyID() == privateKey.getKeyID()) {
				encData = pkEnc;
				break;
			}
		}
		if (encData == null) {
			throw new IllegalStateException("matching encrypted data not found");
		}
		// build decryptor factory
		PublicKeyDataDecryptorFactory dataDecryptorFactory =
				new JcePublicKeyDataDecryptorFactoryBuilder()
						.setProvider("BC")
						.build(privateKey);
		InputStream clear = encData.getDataStream(dataDecryptorFactory);
		byte[] literalData = Streams.readAll(clear);
		clear.close();
		// check data decrypts okay
		if (encData.verify()) {
			// parse out literal data
			PGPObjectFactory litFact = new JcaPGPObjectFactory(literalData);
			PGPLiteralData litData = (PGPLiteralData) litFact.nextObject();
			byte[] data = Streams.readAll(litData.getInputStream());
			return data;
		}
		throw new IllegalStateException("modification check failed");
	}
}
