package no.nav.skanmotovrig;

import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiraapi.JiraServiceImp;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan
@Configuration
public class CoreConfig {

	@Bean
	public JiraService jiraService(JiraClient jiraClient) {
		return new JiraServiceImp(jiraClient);
	}

	@Bean
	public JiraClient jiraClient(SkanmotovrigProperties properties) {
		SkanmotovrigProperties.JiraProperties jira = properties.getJira();
		return new JiraClient(JiraProperties.builder()
				.jiraServieUser(new JiraProperties.JiraServieUser(jira.getUsername(), jira.getPassword()))
				.url(properties.getJira().getUrl())
				.build());
	}
}
