package cz.fit.tsm.config;

import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.error.ErrorPageRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ErrorPageConfig {

	@Bean
	ErrorPageRegistrar notFoundErrorPage() {
		return registry -> registry.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/not-found.xhtml"));
	}
}
