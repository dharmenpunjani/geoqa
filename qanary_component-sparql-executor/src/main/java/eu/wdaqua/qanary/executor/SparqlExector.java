package eu.wdaqua.qanary.executor;

import java.io.ByteArrayOutputStream;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryUtils;

@Component
/**
 * fetches the results from an SPARQL endpoint (containing target data of the
 * current QA process)
 * 
 * it uses the SPARQL query stored in the Qanary triplestore
 * 
 * it save the results as JSON back in the Qanary triplestore
 */
public class SparqlExector extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(SparqlExector.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		logger.info("process: {}", myQanaryMessage);

		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);

		// 1. fetch SPARQL query Q already prepared and stored in the Qanary
		// triplestore T
		String sparqlQuery = this.fetchSparqlQueryFromQanaryTriplestore(myQanaryMessage, myQanaryUtils);

		// 2. fetch results R while using Q from the data endpoint
		// TODO: fetch endpoint
		String dataEndpoint = null;
		String jsonResults = this.fetchResultsFromDataEndpointAsJson(sparqlQuery, dataEndpoint, myQanaryUtils);

		// 3. store the results R into the Qanary triplestore T
		this.storeJsonResultsIntoQanaryTriplestore(jsonResults, myQanaryMessage, myQanaryUtils);

		return myQanaryMessage;
	}

	/**
	 * fetch SPARQL query Q already prepared and stored in the Qanary
	 * triplestore T
	 * 
	 * @param myQanaryMessage
	 * 
	 * @return
	 */
	private String fetchSparqlQueryFromQanaryTriplestore(QanaryMessage myQanaryMessage, QanaryUtils myQanaryUtils) {

		ResultSet myResultSet = myQanaryUtils.selectFromTripleStore(//
				"SELECT ?query FROM <" + myQanaryMessage.getInGraph().toASCIIString() + "> {" //
						+ "	?query a qa:AnnotationOfAnswerSPARQL . " //
						+ "	?query oa:hasTarget <URIAnswer> . " //
						+ "	?query oa:hasBody ?query ." //
						+ "}",
				myQanaryMessage.getEndpoint().toASCIIString());

		String storedSparqlQuery = null;
		while (myResultSet.hasNext()) {
			QuerySolution binding = myResultSet.nextSolution();
			storedSparqlQuery = binding.get("query").asLiteral().getString();
		}
		return storedSparqlQuery;
	}

	/**
	 * execute the SPARQL query and fetch results from triplestore containing
	 * the data endpoint
	 * 
	 * @param myQanaryMessage
	 * 
	 * @return
	 */
	private String fetchResultsFromDataEndpointAsJson(String sparqlQuery, String endpoint, QanaryUtils myQanaryUtils) {

		ResultSet myResultSet = myQanaryUtils.selectFromTripleStore(sparqlQuery, endpoint);

		// write to a ByteArrayOutputStream
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		// save as JSON
		ResultSetFormatter.outputAsJSON(outputStream, myResultSet);

		// return as String
		return new String(outputStream.toByteArray());
	}

	/**
	 * save the JSON results to the Qanary triplestore
	 * 
	 * @param jsonResults
	 * @param myQanaryMessage
	 * @param myQanaryUtils
	 */
	private void storeJsonResultsIntoQanaryTriplestore(String jsonResults, QanaryMessage myQanaryMessage,
			QanaryUtils myQanaryUtils) {

		// Push the JSON object to the named graph reserved for the question
		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX  oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
				+ "INSERT { " //
				+ "GRAPH <" + myQanaryUtils.getOutGraph() + "> { " //
				+ "  ?jsonAnswer a qa:AnnotationOfAnswerJSON . " //
				+ "  ?jsonAnswer oa:hasTarget <URIAnswer> . " //
				+ "  ?jsonAnswer oa:hasBody \"" + jsonResults.replace("\n", " ").replace("\"", "\\\"") + "\" ;" //
		// it is just the executor logic was done in another component
		// + " oa:annotatedBy <> ; "
				+ "	    oa:annotatedAt ?time  " //
				+ "}} " //
				+ "WHERE { " //
				+ "	BIND (IRI(str(RAND())) AS ?jsonAnswer) ." //
				+ "	BIND (now() as ?time) " //
				+ "}"; //
		myQanaryUtils.updateTripleStore(sparql);
	}

}
