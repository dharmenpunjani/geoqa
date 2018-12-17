package eu.wdaqua.qanary.qald.evaluator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ch.qos.logback.core.net.SyslogOutputStream;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.FileReader;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;

@Component
public class QaldEvaluator {

	private static final Logger logger = LoggerFactory.getLogger(QaldEvaluatorApplication.class);

	@Autowired
	private QaldEvaluatorConfigurator myQaldEvaluatorConfigurator;

	public void process(String pathToQaldFile, int maxQuestionsToBeProcessed, String components, String uriServer)
			throws UnsupportedEncodingException, IOException {
		Double globalPrecision = 0.0;
		Double globalRecall = 0.0;
		Double globalF1Measure = 0.0;
		int count = 0;

		ArrayList<Integer> fullRecall = new ArrayList<Integer>();
		ArrayList<Integer> fullF1Measure = new ArrayList<Integer>();

		FileReader filereader = new FileReader(pathToQaldFile);

		filereader.getQuestion(1).getUris();

		// send to pipeline
		List<QaldQuestion> questions = new LinkedList<>(filereader.getQuestions());

		for (int i = 0; i < questions.size()
				&& (i < maxQuestionsToBeProcessed || maxQuestionsToBeProcessed == 0); i++) {
			List<String> expectedAnswers = questions.get(i).getResourceUrisAsString();
			logger.info("{}. Question: {}", questions.get(i).getQaldId(), questions.get(i).getQuestion());

			// questions.get(0).setQuestion("How many goals did Pelé score?");

			// Send the question
			RestTemplate restTemplate = new RestTemplate();
			UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(uriServer);

			// set parameter
			MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
			bodyMap.add("question", questions.get(i).getQuestion());
			bodyMap.add("componentlist[]", components);
			String response = restTemplate.postForObject(service.build().encode().toUri(), bodyMap, String.class);
			logger.info("Response pipline: {}", response);

			// Retrieve the computed uris
			JSONObject responseJson = new JSONObject(response);
			String endpoint = responseJson.getString("endpoint");
			String namedGraph = responseJson.getString("graph");
			logger.debug("{}. named graph: {}", questions.get(i).getQaldId(), namedGraph);
			String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "SELECT ?uri { " //
					+ "  GRAPH <" + namedGraph + "> { " //
					+ "    ?a a qa:AnnotationOfInstance . " //
					+ "    ?a oa:hasBody ?uri " //
					+ "} }";
			logger.debug("SPARQL: {}", sparql);
			ResultSet r = selectTripleStore(sparql, endpoint);
			List<String> systemAnswers = new ArrayList<String>();
			while (r.hasNext()) {
				QuerySolution s = r.next();
				if (s.getResource("uri") != null && !s.getResource("uri").toString().endsWith("null")) {
					logger.info("System answers: {} ", s.getResource("uri").toString());
					systemAnswers.add(s.getResource("uri").toString());
				}
			}

			// Retrieve the expected resources from the SPARQL query
			// List<String> expectedAnswers =
			// questions.get(i).getResourceUrisAsString();
			for (String expected : expectedAnswers) {
				logger.info("Expected answers: {} ", expected);
			}

			// Compute precision and recall
			Metrics m = new Metrics();
			m.compute(expectedAnswers, systemAnswers);
			globalPrecision += m.precision;
			globalRecall += m.recall;
			globalF1Measure += m.f1Measure;
			count++;

			if (m.recall == 1) {
				fullRecall.add(1);
			} else {
				fullRecall.add(0);
			}
		}
		logger.info("Global Precision={}", globalPrecision / count);
		logger.info("Global Recall={}", globalRecall / count);
		logger.info("Global F1-measure={}", globalF1Measure / count);
	}

	class Metrics {
		private Double precision = 0.0;
		private Double recall = 0.0;
		private Double f1Measure = 0.0;

		public void compute(List<String> expectedAnswers, List<String> systemAnswers) {
			// Compute the number of retrieved answers
			int correctRetrieved = 0;
			for (String s : systemAnswers) {
				if (expectedAnswers.contains(s)) {
					correctRetrieved++;
				}
			}
			// mpute precision and recall following the evaluation metrics of
			// QALD
			if (expectedAnswers.size() == 0) {
				if (systemAnswers.size() == 0) {
					recall = 1.0;
					precision = 1.0;
					f1Measure = 1.0;
				} else {
					recall = 0.0;
					precision = 0.0;
					f1Measure = 0.0;
				}
			} else {
				if (systemAnswers.size() == 0) {
					recall = 0.0;
					precision = 1.0;
				} else {
					precision = (double) correctRetrieved / systemAnswers.size();
					recall = (double) correctRetrieved / expectedAnswers.size();
				}
				if (precision == 0 && recall == 0) {
					f1Measure = 0.0;
				} else {
					f1Measure = (2 * precision * recall) / (precision + recall);
				}
			}
		}
	}

	private ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		return qExe.execSelect();
	}

	public void processAllTests() throws UnsupportedEncodingException, IOException {
		List<String> componentConfigurations = myQaldEvaluatorConfigurator.getComponentConfigurationsForComparison();
		int maxQuestionsToEvaluate = myQaldEvaluatorConfigurator.getMaxQuestionsToEvaluate();
		String qaSystemEndpoint = myQaldEvaluatorConfigurator.getQaSystemEndpoint();
		String pathToQaldFile = myQaldEvaluatorConfigurator.getPathToQaldFile();

		logger.info("componentConfigurationsForComparison: {}", componentConfigurations);
		logger.info("getPathToQaldFile: {}", pathToQaldFile);
		logger.info("qaSystemEndpoint: {}", qaSystemEndpoint);
		logger.info("maxQuestionsToEvaluate: {}", pathToQaldFile);

		// for each configuration run the questions
		for (String componentConfiguration : componentConfigurations) {
			this.process(pathToQaldFile, maxQuestionsToEvaluate, componentConfiguration, qaSystemEndpoint);
		}

	}

}
