package no.nav.skanmotovrig;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraProperties.JiraServiceUser;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.skanmotovrig.config.properties.JiraAuthProperties;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties.JiraConfigProperties;
import no.nav.skanmotovrig.config.properties.SlackProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@ComponentScan
@Configuration
public class CoreConfig {

	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
	public static final ZoneId DEFAULT_ZONE_ID = DEFAULT_TIME_ZONE.toZoneId();

	@Bean
	public MethodsClient slackClient(SlackProperties slackProperties) {
		return Slack.getInstance().methods(slackProperties.token());
	}

	@Bean
	public JiraService jiraService(JiraClient jiraClient) {
		return new JiraService(jiraClient);
	}

	@Bean
	Clock clock() {
		return Clock.system(DEFAULT_ZONE_ID);
	}

	@Bean
	public JiraClient jiraClient(SkanmotovrigProperties properties, JiraAuthProperties jiraAuthProperties) {
		JiraConfigProperties jira = properties.getJira();

		return new JiraClient(JiraProperties.builder()
				.jiraServiceUser(new JiraServiceUser(jiraAuthProperties.username(), jiraAuthProperties.password()))
				.url(jira.getUrl())
				.build());
	}
}
