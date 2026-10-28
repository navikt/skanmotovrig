package no.nav.skanmotovrig.config.properties;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("jira.cloud")
public record JiraAuthProperties(
		@NotEmpty String accessToken
) {
	@Override
	public String toString() {
		return "accessToken=****";
	}
}
