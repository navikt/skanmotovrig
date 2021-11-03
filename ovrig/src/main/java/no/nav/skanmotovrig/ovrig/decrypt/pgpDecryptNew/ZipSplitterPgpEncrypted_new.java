package no.nav.skanmotovrig.ovrig.decrypt.pgpDecryptNew;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import no.nav.skanmotovrig.decrypt.ZipIteratorEncrypted;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.dataformat.zipfile.ZipIterator;
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

public class ZipSplitterPgpEncrypted_new extends ZipSplitter {


	@Inject
	public ZipSplitterPgpEncrypted_new() {
	}

	@Override
	public Object evaluate(Exchange exchange) {
		return new ZipIterator(exchange, exchange.getIn().getBody(InputStream.class));
	}

	@Override
	public <T> T evaluate(Exchange exchange, Class<T> type) {
		Object result = this.evaluate(exchange);
		return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
	}


}
