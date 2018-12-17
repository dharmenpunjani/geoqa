package eu.wdaqua.qanary.component.geospatialsearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.openrdf.query.resultio.stSPARQLQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import eu.wdaqua.qanary.component.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryUtils;
import eu.earthobservatory.org.StrabonEndpoint.client.EndpointResult;
import eu.earthobservatory.org.StrabonEndpoint.client.SPARQLEndpoint;
import eu.wdaqua.qanary.component.QanaryComponent;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class GeoSPARQLQueryExecutor extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(GeoSPARQLQueryExecutor.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * 
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question

		// retrive the question
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("Question: {}", myQuestion);
		String myQuery = "";

		// Retrieves the spots from the knowledge graph
		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT ?query " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "  ?a a qa:AnnotationOfAnswerSPARQL . " //
				+ "  ?a oa:hasTarget ?ans . " //
				+ "  ?a oa:hasBody ?query;" //
				+ "     oa:annotatedBy <urn:qanary:geosparqlgenerator> ; " //
				+ "	    oa:AnnotatedAt ?time . " //
				+ "} ";

		ResultSet r = myQanaryUtils.selectFromTripleStore(sparql);

		while (r.hasNext()) {
			QuerySolution s = r.next();
			myQuery = s.get("query").asLiteral().getString();
			logger.info("Fetched geosparql query is: {}", myQuery);
		}
		
		
		
		Query query = QueryFactory.create(myQuery);
		System.out.println("sparql query :"+query.toString());
		QueryExecution exec = QueryExecutionFactory.sparqlService("http://pyravlos2.di.uoa.gr:8080/geoqa/Query", query);
		ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
		
		BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmen/results_gir.csv", true));
		bw.newLine();
		bw.write(myQuestion+", ");
		
		if (!results.hasNext()) {
			
		} else {
			while (results.hasNext()) {
				
				QuerySolution qs = results.next();
				
				String selectVariable = "";
				if(myQuery.contains("select ?x")) {
					selectVariable = "x";
					String uria = qs.get("x").toString();
					//String links = qs.get("link").toString();
//					System.out.println("x: "+uria );
					bw.write(uria);
					bw.write(",");
				}
				else {
					bw.write("boolean");
					bw.write(",");
				}
				
			}
		}
		bw.close();
		/*
		 * myQuery = "PREFIX geo: <http://www.opengis.net/ont/geosparql#> " +
		 * " PREFIX geof: <http://www.opengis.net/def/function/geosparql/> " +
		 * "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
		 * "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#label> " +
		 * "PREFIX strdf: <http://strdf.di.uoa.gr/ontology#> " +
		 * "PREFIX postgis: <http://postgis.net/> " +
		 * "PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/> " +
		 * "select ?poi " + " where { " +
		 * "    ?poi a <http://www.app-lab.eu/osm/ontology#park>; " +
		 * "        geo:hasGeometry ?poiGeom. " +
		 * "        ?poiGeom geo:asWKT ?poiWKT. " + "     " + "}";
		 */

//		Properties prop = new Properties();
//		InputStream input = GeoSPARQLQueryExecutor.class.getClassLoader().getResourceAsStream("application.properties");
//
//		if (input == null) {
//			logger.info("sorry, Unable to find file application.properties ");
//		}
//		String host = "pyravlos1.di.uoa.gr";
//		Integer port = 8080;
//		String appName = "geoqa/Query";
//		String query = myQuery;
//		String format = "TSV";
//
//		SPARQLEndpoint endpoint = new SPARQLEndpoint(host, port, appName);
//		if (query.length() > 2) {
//			try {
//
//				EndpointResult result = endpoint.query(query,
//						(stSPARQLQueryResultFormat) stSPARQLQueryResultFormat.valueOf(format));
//
//				System.out.println("<----- Result ----->");
//				System.out.println(result.getResponse().replaceAll("\n", "\n\t"));
//				System.out.println("<----- Result ----->");
//
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)

		return myQanaryMessage;
	}

}
