package eu.wdaqua.qanary.dbpediarelationextractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryUtils;
import net.ricecode.similarity.JaroWinklerStrategy;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class DBpediaRelationExtractor extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(DBpediaRelationExtractor.class);

	// public static void testRelationExtractor() {
	// try {
	// Properties props = new Properties();
	// props.setProperty("annotators", "tokenize,ssplit,lemma,pos,parse,ner");
	// StanfordCoreNLP pipeline = new StanfordCoreNLP();
	// String sentence = "Barack Obama lives in America. Obama works for the
	// Federal Goverment.";
	// Annotation doc = new Annotation(sentence);
	// pipeline.annotate(doc);
	// RelationExtractorAnnotator r = new RelationExtractorAnnotator(props);
	// r.annotate(doc);
	// for (CoreMap s : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
	// System.out.println("For sentence " +
	// s.get(CoreAnnotations.TextAnnotation.class));
	// List<RelationMention> rls = s.get(RelationMentionsAnnotation.class);
	// for (RelationMention rl : rls) {
	// System.out.println(rl.toString());
	// }
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public static List<String> getVerbsNouns(String documentText) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		List<String> postags = new ArrayList<>();
		String lemmetizedQuestion = "";
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the
				// list of lemmas
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.contains("VB") || pos.contains("IN") || pos.contains("NN") || pos.contains("JJ")) {
					postags.add(token.get(LemmaAnnotation.class));
				}
			}
		}
		return postags;
	}

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
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("Question: {}", myQuestion);

		List<String> allVerbs = getVerbsNouns(myQuestion);
		List<String> relationList = new ArrayList<String>();
		List<String> valuePropertyList = new ArrayList<String>();
		boolean valueFlag = false;
		List<String> coonceptsUri = new ArrayList<String>();
		ResultSet r;
		List<Concept> concepts = new ArrayList<>();
		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT ?start ?end ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "    ?a a qa:AnnotationOfConcepts . " + "?a oa:hasTarget [ "
				+ "		     a               oa:SpecificResource; " //
				+ "		     oa:hasSource    ?q; " //
				+ "	         oa:hasSelector  [ " //
				+ "			         a        oa:TextPositionSelector ; " //
				+ "			         oa:start ?start ; " //
				+ "			         oa:end   ?end " //
				+ "		     ] " //
				+ "    ] . " //
				+ " ?a oa:hasBody ?uri ; " + "    oa:annotatedBy ?annotator " //
				+ "} " + "ORDER BY ?start ";

		r = myQanaryUtils.selectFromTripleStore(sparql);
		while (r.hasNext()) {
			QuerySolution s = r.next();

			Concept conceptTemp = new Concept();
			conceptTemp.begin = s.getLiteral("start").getInt();

			conceptTemp.end = s.getLiteral("end").getInt();

			conceptTemp.link = s.getResource("uri").getURI();

			// geoSparqlQuery += "" + conceptTemplate.replace("poiURI",
			// conceptTemp.link).replaceAll("poi",
			// "poi" + conceptTemp.begin);
			// newGeoSparqlQuery += "" + conceptTemplate.replace("poiURI",
			// conceptTemp.link).replaceAll("poi",
			// "poi" + conceptTemp.begin);
			if (conceptTemp.link.contains("dbpedia.org")) {
				concepts.add(conceptTemp);
				coonceptsUri.add(conceptTemp.link);
				logger.info("Concept start {}, end {} concept {} link {}", conceptTemp.begin, conceptTemp.end,
						myQuestion.substring(conceptTemp.begin, conceptTemp.end), conceptTemp.link);
			}

		}
		// for (int i = 0; i < concepts.size(); i++) {
		// myQuestion = myQuestion
		// .replace(coonceptsUri.get(i).substring(coonceptsUri.get(i).lastIndexOf("#")
		// + 1).toLowerCase(), "");
		// System.out.println("myQuestion: " + myQuestion);
		// System.out.println("The class labels: "
		// + coonceptsUri.get(i).substring(coonceptsUri.get(i).lastIndexOf("#")
		// + 1).toLowerCase());
		// }
		for (String concept : coonceptsUri) {
			String classLabel = concept.substring(concept.lastIndexOf("#") + 1);
			classLabel = "http://dbpedia.org/ontology/" + classLabel.substring(0, 1).toUpperCase()
					+ classLabel.substring(1);
			System.out.println("class label : " + classLabel);
			String classLabelValue = classLabel.substring(classLabel.lastIndexOf("/") + 1).toLowerCase();
			String sparqlQuery = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ "prefix geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
					+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ "prefix dbo: <http://dbpedia.org/ontology/> " + " select DISTINCT ?p ?label ?o" + " where {"
					+ " ?uri a <" + concept + ">."
					+ " ?uri ?p ?o. ?p rdfs:label ?label.  FILTER langMatches(lang(?label),'en')" + "}  ";

			System.out.println("Sparql Query : " + sparqlQuery + "\n class label: " + classLabel);
			Query query = QueryFactory.create(sparqlQuery);

			QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);

			ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
			if (!results.hasNext()) {
				break;
			} else {
				while (results.hasNext()) {
					QuerySolution qs = results.next();
					String dbpediaProperty = qs.get("p").toString();
					// String geom = qs.getLiteral("geom").getString();
					if (!dbpediaProperty.contains(classLabelValue)
							&& (dbpediaProperty.contains("http://dbpedia.org/ontology/")
									|| dbpediaProperty.contains("http://dbpedia.org/property/"))) {
						// System.out.println("Property : " + dbpediaProperty);
						String labelProperty = qs.get("label").toString().toLowerCase();
						String valueProperty = qs.get("o").toString();
						labelProperty = labelProperty.substring(0, labelProperty.indexOf("@"));
						double score = 0.0;
						SimilarityStrategy strategy = new JaroWinklerStrategy();

						StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
						// for (String word : myQuestion.split(" ")) {
						// score = service.score(labelProperty, word);
						// if (score > 0.95) {
						// if (relationList.size() == 0) {
						// relationList.add(dbpediaProperty);
						// } else if (!relationList.contains(dbpediaProperty)) {
						// relationList.add(dbpediaProperty);
						// }
						// System.out.println("Found : " + dbpediaProperty + "
						// :" + labelProperty);
						// }
						// score =
						// service.score(valueProperty.toLowerCase().replace(labelProperty,
						// " ").trim(), word);
						if (valueProperty.length() < 20) {
							for (String verb : allVerbs) {
								Pattern p = Pattern.compile("\\b" + valueProperty + "\\b", Pattern.CASE_INSENSITIVE);
//								System.out.println("Keyword: "+verb+"=================================="+valueProperty);
								Matcher m = p.matcher(verb);
								if (!verb.equalsIgnoreCase(concept)) {
									if (m.find() && !valueProperty.equalsIgnoreCase("crosses")) {
										valueFlag = true;
										if (relationList.size() == 0) {
											relationList.add(dbpediaProperty);
											valuePropertyList.add(valueProperty);
										} else if (!relationList.contains(dbpediaProperty)) {
											relationList.add(dbpediaProperty);
											valuePropertyList.add(valueProperty);
										}
										System.out.println("Found Value: " + dbpediaProperty + " :" + valueProperty);
									}
								}
							}
						}
						// }
					}
				}
			}

			for (String DBpediaProperty : valuePropertyList) {
				sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
						+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
						+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
						+ "prefix dbp: <http://dbpedia.org/property/> " + "INSERT { " + "GRAPH <"
						+ myQanaryQuestion.getOutGraph() + "> { " + "  ?a a qa:AnnotationOfRelation . "
						+ "  ?a oa:hasTarget [ " + "           a    oa:SpecificResource; "
						+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " + "  ] ; "
						+ "     oa:hasValue <" + DBpediaProperty + ">;"
						+ "     oa:annotatedBy <http:DBpedia-RelationExtractor.com> ; " + "	    oa:AnnotatedAt ?time  "
						+ "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ." + "BIND (now() as ?time) " + "}";
				logger.info("Sparql query {}", sparql);
				myQanaryUtils.updateTripleStore(sparql);
			}

			sparqlQuery = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ "prefix geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
					+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ "prefix dbo: <http://dbpedia.org/ontology/> " + " select DISTINCT ?p ?label " + " where {"
					+ " ?uri a <" + concept + ">."
					+ " ?uri ?p ?o. ?p rdfs:label ?label.  FILTER langMatches(lang(?label),'en')" + "}  ";

			System.out.println("Sparql Query : " + sparqlQuery + "\n class label: " + concept);
			query = QueryFactory.create(sparqlQuery);
			exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
			results = ResultSetFactory.copyResults(exec.execSelect());
			if (!results.hasNext()) {
				break;
			} else {
				while (results.hasNext()) {
					QuerySolution qs = results.next();
					String dbpediaProperty = qs.get("p").toString();
					// String geom = qs.getLiteral("geom").getString();
					if (!dbpediaProperty.contains(classLabelValue)
							&& (dbpediaProperty.contains("http://dbpedia.org/ontology/")
									|| dbpediaProperty.contains("http://dbpedia.org/property/"))) {
						// System.out.println("Property : " + dbpediaProperty);
						String labelProperty = qs.get("label").toString().toLowerCase();

						labelProperty = labelProperty.substring(0, labelProperty.indexOf("@"));
						// double score = 0.0;
						// SimilarityStrategy strategy = new
						// JaroWinklerStrategy();
						//
						// StringSimilarityService service = new
						// StringSimilarityServiceImpl(strategy);
						// for (String word : myQuestion.split(" ")) {
						// score = service.score(labelProperty, word);
						
						Pattern p = Pattern.compile("\\b" + labelProperty + "\\b", Pattern.CASE_INSENSITIVE);
						for (String verb : allVerbs) {
//							System.out.println("Keyword: "+verb+"======================"+labelProperty);
							Matcher m = p.matcher(verb);
							if (!verb.equalsIgnoreCase(concept)) {
								if (m.find() && !labelProperty.equalsIgnoreCase("crosses")
										&& !labelProperty.equalsIgnoreCase("runs")
										&& !labelProperty.equalsIgnoreCase("south") && labelProperty.length() > 2) {
									if (relationList.size() == 0) {
										relationList.add(dbpediaProperty);
									} else if (!relationList.contains(dbpediaProperty)) {
										relationList.add(dbpediaProperty);
									}
									System.out.println("Found : " + dbpediaProperty + " :" + labelProperty);
								}
							}
						}
						// }
					}
				}
			}

		}

		for (String DBpediaProperty : relationList) {
			sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
					+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " + "prefix dbp: <http://dbpedia.org/property/> "
					+ "INSERT { " + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { "
					+ "  ?a a qa:AnnotationOfRelation . " + "  ?a oa:hasTarget [ "
					+ "           a    oa:SpecificResource; " + "           oa:hasSource    <"
					+ myQanaryQuestion.getUri() + ">; " + "  ] ; " + "     oa:hasBody <" + DBpediaProperty + "> ;"
					+ "     oa:annotatedBy <http:DBpedia-RelationExtractor.com> ; " + "	    oa:AnnotatedAt ?time  "
					+ "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ." + "BIND (now() as ?time) " + "}";
			logger.info("Sparql query {}", sparql);
			myQanaryUtils.updateTripleStore(sparql);
		}
		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)

		return myQanaryMessage;
	}

	class Concept {
		public int begin;
		public int end;
		public String link;
	}
}
