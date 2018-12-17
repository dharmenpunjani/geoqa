package eu.wdaqua.qanary.qald.evaluator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * starts the spring application via command line runner
 *
 */
@SpringBootApplication
@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan
@EnableAutoConfiguration
public class QaldEvaluatorApplication implements CommandLineRunner {
	@Autowired
	private QaldEvaluator myQaldEvaluator;

	@Bean
	QaldEvaluatorConfigurator myQaldEvaluatorConfigurator(
			@Value("${qaldevaluator.maxQuestionsToEvaluate}") Integer maxQuestionsToEvaluate,
			@Value("${qaldevaluator.componentConfigurationsForComparison}") List<String> componentConfigurationsForComparison,
			@Value("${qaldevaluator.qaSystemEndpoint}") String qaSystemEndpoint,
			@Value("${qaldevaluator.pathToQaldFile}") String pathToQaldFile) {
		return new QaldEvaluatorConfigurator(maxQuestionsToEvaluate, componentConfigurationsForComparison,
				qaSystemEndpoint, pathToQaldFile);
	}

	public void run(String... args) throws Exception {
		myQaldEvaluator.processAllTests();
	}

	public static void main(String... args) throws UnsupportedEncodingException, IOException {
		SpringApplication.run(QaldEvaluatorApplication.class, args).close();
	}

}
