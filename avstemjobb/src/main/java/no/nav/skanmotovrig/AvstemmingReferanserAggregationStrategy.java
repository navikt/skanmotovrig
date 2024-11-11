package no.nav.skanmotovrig;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import java.util.HashSet;
import java.util.Set;

public class AvstemmingReferanserAggregationStrategy implements AggregationStrategy {
	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		Set<String> avstemReferens;

		if (oldExchange == null) {
			avstemReferens = new HashSet<>();
			newExchange.getIn().setBody(avstemReferens);
		} else {
			avstemReferens = oldExchange.getIn().getBody(Set.class);
		}

		avstemReferens.add(newExchange.getIn().getBody(String.class));

		return oldExchange != null ? oldExchange : newExchange;
	}
}
