package no.nav.skanmotovrig.ovrig.decrypt.pgpDecryptNew;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.dataformat.zipfile.ZipIterator;

import java.io.InputStream;

public class PgpZipSplitter implements Expression {

	public Object evaluate(Exchange exchange) {
		return new ZipIterator(exchange, exchange.getIn().getBody(InputStream.class));
	}

	public <T> T evaluate(Exchange exchange, Class<T> type) {
		Object result = this.evaluate(exchange);
		return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
	}


}
