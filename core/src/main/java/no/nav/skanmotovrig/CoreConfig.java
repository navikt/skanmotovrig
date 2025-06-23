package no.nav.skanmotovrig;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraProperties.JiraServiceUser;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties.JiraConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan
@Configuration
public class CoreConfig {

	@Bean
	public MethodsClient slackClient(SkanmotovrigProperties skanmotovrigProperties) {
		return Slack.getInstance().methods(skanmotovrigProperties.getSlack().getToken());
	}

	@Bean
	public JiraService jiraService(JiraClient jiraClient) {
		return new JiraService(jiraClient);
	}

	@Bean
	public JiraClient jiraClient(SkanmotovrigProperties properties) {
		JiraConfigProperties jira = properties.getJira();

		return new JiraClient(JiraProperties.builder()
				.jiraServiceUser(new JiraServiceUser(jira.getUsername(), jira.getPassword()))
				.url(properties.getJira().getUrl())
				.build());
	}
}
