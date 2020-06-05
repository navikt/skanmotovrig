package no.nav.skanmotovrig.helse;

import org.apache.camel.Body;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultMessage;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.io.FilenameUtils.getExtension;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class PostboksHelseSplitter {
    @Handler
    public List<Message> splitMessage(@Body List<Exchange> body, CamelContext camelContext) {
        return body.stream().map(exchange -> {
            DefaultMessage message = new DefaultMessage(camelContext);
            final String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
            message.setHeader(Exchange.FILE_NAME, filename);
            message.setHeader(PostboksHelseRoute.HEADER_FORSENDELSE_FILE_EXTENSION, getExtension(filename));
            message.setBody(exchange.getIn().getBody(InputStream.class));
            return message;
        }).collect(Collectors.toList());
    }
}
