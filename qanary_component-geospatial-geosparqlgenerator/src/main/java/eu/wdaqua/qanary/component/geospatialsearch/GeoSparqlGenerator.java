package eu.wdaqua.qanary.component.geospatialsearch;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryUtils;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class GeoSparqlGenerator extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(GeoSparqlGenerator.class);

	public static String lemmatize(String documentText) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		List<String> lemmas = new ArrayList<>();
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
				lemmas.add(token.get(LemmaAnnotation.class));
				lemmetizedQuestion += token.get(LemmaAnnotation.class) + " ";
			}
		}
		return lemmetizedQuestion;
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question

		String detectedPattern = "";
		List<String> properties = new ArrayList<String>();
		List<String> propertiesValue = new ArrayList<String>();
		List<Integer> indexOfConcepts = new ArrayList<Integer>();
		List<Integer> indexOfInstances = new ArrayList<Integer>();
		Map<String, List<Integer>> mapOfRelationIdex = new HashMap<String, List<Integer>>();
		Map<Integer, String> patternForQueryGeneration = new HashMap<Integer, String>();
		Map<Integer, String> mapOfGeoRelation = new TreeMap<Integer, String>();
		Map<Integer, List<Concept>> sameConcepts = new HashMap<Integer, List<Concept>>();
		Map<Integer, List<Entity>> sameInstances = new HashMap<Integer, List<Entity>>();
		try {
			logger.info("store data in graph {}", myQanaryMessage.getValues().get(new URL(QanaryMessage.endpointKey)));
			// TODO: insert data in QanaryMessage.outgraph

			QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
			QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
			String myQuestion = myQanaryQuestion.getTextualRepresentation();
			myQuestion = lemmatize(myQuestion);
			logger.info("Question: {}", myQuestion);

			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			Annotation document = new Annotation(myQuestion);
			pipeline.annotate(document);
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				// traversing the words in the current sentence
				// a CoreLabel is a CoreMap with additional token-specific methods
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					// this is the text of the token
					String word = token.get(TextAnnotation.class); // this is the POS tag of the token
					String pos = token.get(PartOfSpeechAnnotation.class); // this is the NER label of the token
					String ne = token.get(NamedEntityTagAnnotation.class);
				}
				// this is the parse tree of the current sentence
				Tree tree = sentence.get(TreeAnnotation.class);

				tree.pennPrint();

				// this is the Stanford dependency graph of the current sentence
				SemanticGraph dependencies = sentence
						.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);

				dependencies.prettyPrint();

			}

			// String prefixs = "PREFIX geo:
			// <http://www.opengis.net/ont/geosparql#> "
			// + "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
			// "
			// + "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
			// + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
			// + "PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/> "
			// + "PREFIX strdf: <http://strdf.di.uoa.gr/ontology#> " + "PREFIX
			// postgis: <http://postgis.net/> ";
			// String selectClause = "Select * where { ";
			//
			// String conceptTemplate = " ?poi a <poiURI>; " + "geo:hasGeometry
			// ?poiGeom. "
			// + "?poiGeom geo:asWKT ?poiWKT. " + "?poi rdfs:label ?poiLabel. ";
			// String instanceTemplate = " ?instance owl:sameAs <instanceURI>; "
			// + "geo:hasGeometry ?instanceGeom. "
			// + "?instanceGeom geo:asWKT ?instanceWKT. ";

			String sparql;
			boolean dbpediaPropertyFlag = false;
			boolean dbpediaPropertyValueFlag = false;
			Entity ent = new Entity();
			Concept concept = new Concept();
			List<Concept> concepts = new ArrayList<>();
			List<String> geoSPATIALRelations = new ArrayList<String>();
			List<Entity> entities = new ArrayList<Entity>();
			ResultSet r;
			String geoRelation = null;
			String thresholdDistance = "";
			String unitDistance = "";
			List<String> distanceUnits = new ArrayList<String>();
			distanceUnits.add("kilometer");
			distanceUnits.add("km");
			distanceUnits.add("metre");
			distanceUnits.add("meter");
			String geoSparqlQuery = "";// prefixs + selectClause;
			boolean thresholdFlag = false;
			// Identify distance threshold

			thresholdDistance = myQuestion.replaceAll("[^-\\d]+", "");
			logger.info("Question without numbers: {}", myQuestion.replaceAll("[^-\\d]+", ""));
			if (!thresholdDistance.equals("")) {
				for (String tempUnit : distanceUnits) {

					Pattern p = Pattern.compile("\\b" + tempUnit + "\\b", Pattern.CASE_INSENSITIVE);
					Matcher m = p.matcher(myQuestion.replaceAll(thresholdDistance, ""));
					if (m.find()) {
						unitDistance = tempUnit;
						break;
					}
				}

				if (unitDistance.equalsIgnoreCase("km") || unitDistance.equalsIgnoreCase("kilometer")
						|| unitDistance.equalsIgnoreCase("kms")) {

					thresholdDistance = thresholdDistance + "000";
					thresholdFlag = true;
				}
				if (unitDistance.contains("meter")||unitDistance.contains("metre")) {
					thresholdFlag = true;
				}
			}

			// property
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
					+ "SELECT  ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
					+ "WHERE { " //
					+ "  ?a a qa:AnnotationOfRelation . " + "  ?a oa:hasTarget [ " + " a    oa:SpecificResource; "
					+ "           oa:hasSource    ?q; " + "  ]; "
					+ "     oa:hasBody ?uri ;oa:AnnotatedAt ?time} order by(?time)";

			r = myQanaryUtils.selectFromTripleStore(sparql);

			while (r.hasNext()) {
				QuerySolution s = r.next();
				properties.add(s.getResource("uri").getURI());
				dbpediaPropertyFlag = true;
				logger.info("DBpedia uri info {}", s.getResource("uri").getURI());
			}

//			// property value
//			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
//					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
//					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
//					+ "SELECT  ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
//					+ "WHERE { " //
//					+ "  ?a a qa:AnnotationOfRelation . " + "  ?a oa:hasTarget [ " + " a    oa:SpecificResource; "
//					+ "           oa:hasSource    ?q; " + "  ]; "
//					+ "     oa:hasValue ?uri ; oa:AnnotatedAt ?time} order by(?time)";
//
//			r = myQanaryUtils.selectFromTripleStore(sparql);
//
//			while (r.hasNext()) {
//				QuerySolution s = r.next();
//				String valueProperty = s.get("uri").toString();
//				valueProperty = valueProperty.substring(valueProperty.lastIndexOf("/") + 1);
//				propertiesValue.add(valueProperty);
//				dbpediaPropertyValueFlag = true;
//				logger.info("DBpedia property value uri value info {}", valueProperty);
//			}

			// String newGeoSparqlQuery = prefixs + selectClause;
			// TODO: refactor this to an enum or config file
			Map<String, String> mappingOfGeospatialRelationsToGeosparqlFunctions = new HashMap<>();
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("geof:sfWithin", "within");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("geof:sfCrosses", "crosses");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("geof:distance", "near");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:above", "north");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:above_left", "north_west");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:above_right", "north_east");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:below", "south");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:below_right", "south_east");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:below_left", "south_west");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:right", "east");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:left", "west");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("postgis:ST_Centroid", "center");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("geof:boundary", "boundry");
			// implement the CRI pattern

			// 1. concepts: Retrieve via SPARQL the concepts identified for the
			// given question

			// 2. relation in the question: Retrieves the spatial function
			// supported by the GeoSPARQL from the graph for e.g.
			// fetch the geospatial relation identifier
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
					+ "SELECT ?geoRelation ?start " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
					+ "WHERE { " //
					+ "    ?a a qa:AnnotationOfRelation . " + "?a oa:hasTarget [ "
					+ "		     a               oa:SpecificResource; " //
					+ "		     oa:hasSource    ?q; " //
					+ "	         oa:hasRelation  [ " //
					+ "			         a        oa:GeoRelation ; " //
					+ "			         oa:geoRelation ?geoRelation ; " //
					+ "	         		 oa:hasSelector  [ " //
					+ "			         		a        oa:TextPositionSelector ; " //
					+ "			         		oa:start ?start ; " //
					+ "		     ] " //
					+ "		     ] " //
					+ "    ] ; " //
					+ "} " //
					+ "ORDER BY ?start ";

			r = myQanaryUtils.selectFromTripleStore(sparql);

			while (r.hasNext()) {
				QuerySolution s = r.next();
				logger.info("found relation : {} at {}", s.getResource("geoRelation").getURI().toString(),
						s.getLiteral("start").getInt());
				String geoSpatialRelation = s.getResource("geoRelation").getURI().toString();
				int geoSpatialRelationIndex = s.getLiteral("start").getInt();
				geoSPATIALRelations.add(geoSpatialRelation);
				if (mapOfRelationIdex.size() == 0) {
					List<Integer> indexes = new ArrayList<Integer>();
					indexes.add(geoSpatialRelationIndex);
					mapOfRelationIdex.put(geoSpatialRelation, indexes);
				} else {
					if (mapOfRelationIdex.keySet().contains(geoSpatialRelation)) {
						List<Integer> indexes = mapOfRelationIdex.remove(geoSpatialRelation);
						indexes.add(geoSpatialRelationIndex);
						mapOfRelationIdex.put(geoSpatialRelation, indexes);
					} else {
						List<Integer> indexes = new ArrayList<Integer>();
						indexes.add(geoSpatialRelationIndex);
						mapOfRelationIdex.put(geoSpatialRelation, indexes);
					}
				}
			}

			// map the given relation identifier to a GeoSPARQL function
			String geosparqlFunction = mappingOfGeospatialRelationsToGeosparqlFunctions.get(geoRelation);

			// STEP 3.0 Retrieve concepts from Triplestore

			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
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
				indexOfConcepts.add(conceptTemp.begin);
				concepts.add(conceptTemp);

				logger.info("Concept start {}, end {}, URI{}", conceptTemp.begin, conceptTemp.end, conceptTemp.link);

			}

			// 3.1 Instance: Retrieve Starting and ending Index of the Instance
			// (Point of Interest) as well as URI
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
					+ "SELECT ?start ?end ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
					+ "WHERE { " //
					+ "    ?a a qa:AnnotationOfInstance . " + "?a oa:hasTarget [ "
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

				Entity entityTemp = new Entity();
				entityTemp.begin = s.getLiteral("start").getInt();

				entityTemp.end = s.getLiteral("end").getInt();

				entityTemp.uri = s.getResource("uri").getURI();

				entityTemp.namedEntity = myQuestion.substring(ent.begin, ent.end);

				// geoSparqlQuery += "" +
				// instanceTemplate.replace("instanceURI",
				// entityTemp.uri).replaceAll("instance",
				// "instance" + entityTemp.begin);
				// newGeoSparqlQuery += "" +
				// instanceTemplate.replace("instanceURI",
				// entityTemp.uri).replaceAll("instance",
				// "instance" + entityTemp.begin);

				boolean flg = true;
				for (Concept conceptTemp : concepts) {

					if (entityTemp.begin <= conceptTemp.begin && entityTemp.end >= conceptTemp.end) {
						indexOfConcepts.remove((Integer) conceptTemp.begin);
						concepts.remove(conceptTemp);
//						System.out.println("Insie the concept remover if overlaps Instance=============================");
						break;
					}

					// if (entityTemp.begin < conceptTemp.begin) {
					// if (entityTemp.end >= conceptTemp.begin) {
					//// flg = false;
					// indexOfConcepts.remove((Integer)
					// conceptTemp.begin);
					// concepts.remove(conceptTemp);
					// break;
					// }
					// }
				}

				indexOfInstances.add(entityTemp.begin);
				entities.add(entityTemp);

				logger.info("Instance start {}, end {}, instance {}, URI{}", entityTemp.begin, entityTemp.end,
						entityTemp.namedEntity, entityTemp.uri);

			}

			// // Create the Stanford CoreNLP pipeline
			// Properties props = new Properties();
			// props.setProperty("annotators",
			// "tokenize,ssplit,pos,lemma,parse,depparse,natlog,openie");
			// StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			//
			// // Annotate an example document.
			// Annotation doc = new Annotation(myQuestion);
			// pipeline.annotate(doc);
			//
			// // Loop over sentences in the document
			// System.out.println("The relations");
			// for (CoreMap sentence :
			// doc.get(CoreAnnotations.SentencesAnnotation.class)) {
			// // Get the OpenIE triples for the sentence
			// Collection<RelationTriple> triples =
			// sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
			// // Print the triples
			// for (RelationTriple triple : triples) {
			// System.out.println(triple.confidence + "\t" +
			// triple.subjectLemmaGloss() + "\t" +
			// triple.relationLemmaGloss() + "\t" +
			// triple.objectLemmaGloss());
			// }
			// }

			geoSPATIALRelations.clear();

			// removing extra identified relations : ie.e "within North within"
			// woul be " noth wihtin"
			for (Entry<String, List<Integer>> geoSpatialRelation : mapOfRelationIdex.entrySet()) {

				switch (geoSpatialRelation.getKey()) {
				case "geof:sfWithin":

					List<Integer> indexOfrelation = geoSpatialRelation.getValue();
					if (indexOfrelation.size() > 1) {

						Collections.sort(indexOfrelation);
						for (int i = 1; i < indexOfrelation.size(); i++) {

							boolean flagIndex = false;
							int indPrev = indexOfrelation.get(i - 1);
							int indexCurrent = indexOfrelation.get(i);
							for (Integer indValConcept : indexOfConcepts) {
								if (indPrev < indValConcept && indexCurrent > indValConcept) {
									flagIndex = true;
								}
							}
							for (Integer indValInstance : indexOfInstances) {
								if (indPrev < indValInstance && indexCurrent > indValInstance) {
									flagIndex = true;
								}
							}
							if (!flagIndex) {
								indexOfrelation.remove(i - 1);
								i--;
								System.out.println(" Insideeeeeeeeeeeeee ===========");
							}
						}

						mapOfRelationIdex.replace("geof:sfWithin", indexOfrelation);
					}
					break;

				default:
					break;
				}

			}

			// if((entities.size()+concepts.size())<=mapOfRelationIdex.size()){
			// for (Entry<String, List<Integer>> geoSpatialRelation :
			// mapOfRelationIdex.entrySet()) {
			// List<Integer> indexOfrelation = geoSpatialRelation.getValue();
			// for (Integer indexRelation : indexOfrelation) {
			//
			//
			//// patternForQueryGeneration.put(indexRelation, "r");
			//// mapOfGeoRelation.put(indexRelation,
			// geoSpatialRelation.getKey());
			// }
			// }
			// }

			// putting identified C , R and I in one place with their Index
			geoSPATIALRelations.clear();
			for (Integer indexConcept : indexOfConcepts) {
				patternForQueryGeneration.put(indexConcept, "c");
			}
			for (Integer indexInstanc : indexOfInstances) {
				System.out.println("index of instance: " + indexInstanc);
				patternForQueryGeneration.put(indexInstanc, "i");
			}
			for (Entry<String, List<Integer>> geoSpatialRelation : mapOfRelationIdex.entrySet()) {
				List<Integer> indexOfrelation = geoSpatialRelation.getValue();
				for (Integer indexRelation : indexOfrelation) {
					patternForQueryGeneration.put(indexRelation, "r");
					mapOfGeoRelation.put(indexRelation, geoSpatialRelation.getKey());
				}
			}

			// sorting the C, R, I based on Index puting in a treemap.
			TreeMap<Integer, String> sortedPatternBasedOnIndex = new TreeMap<Integer, String>(
					patternForQueryGeneration);
			// String previousRelation = "";
			// int previousIndex = -1;

			for (Entry<String, List<Integer>> geoSpatialRelation : mapOfRelationIdex.entrySet()) {
				List<Integer> indexOfrelation = geoSpatialRelation.getValue();
				for (Integer indexRelation : indexOfrelation) {
					for (Entity entity : entities) {
						if (entity.begin <= indexRelation && entity.end >= indexRelation) {
							mapOfGeoRelation.remove(indexRelation);
							mapOfRelationIdex.remove(indexRelation);
//							System.out.println(	"Inside the relation remover if overlaps Instance=============================");
						}
					}
				}
			}

			if ((entities.size() + concepts.size()) <= mapOfRelationIdex.size()) {
				for (Entry<String, List<Integer>> geoSpatialRelation : mapOfRelationIdex.entrySet()) {
					List<Integer> indexOfrelation = geoSpatialRelation.getValue();
					for (Integer indexRelation : indexOfrelation) {
						for (Integer indexConcept : indexOfConcepts) {
							if (indexRelation < indexConcept) {
								for (Integer indexInstance : indexOfInstances) {
									if (indexInstance > indexConcept) {
										mapOfGeoRelation.remove(indexRelation);
//										System.out.println(	"Inside the relation remover if extra relation is present=============================");
									}
								}
							}
						}

						// patternForQueryGeneration.put(indexRelation, "r");
						// mapOfGeoRelation.put(indexRelation,
						// geoSpatialRelation.getKey());
					}
				}
			}

			for (Concept conc : concepts) {
				List<Concept> tConcept = new ArrayList<Concept>();
				if (sameConcepts.isEmpty()) {
					tConcept.add(conc);
					sameConcepts.put(conc.begin, tConcept);
				} else {
					if (sameConcepts.containsKey(conc.begin)) {
						tConcept = sameConcepts.remove(conc.begin);
					}
					tConcept.add(conc);
					sameConcepts.put(conc.begin, tConcept);
				}
			}

			for (Entity ents : entities) {
				List<Entity> tEnt = new ArrayList<Entity>();
				if (sameInstances.isEmpty()) {
					tEnt.add(ents);
					sameInstances.put(ents.begin, tEnt);
				} else {
					if (sameInstances.containsKey(ents.begin)) {
						tEnt = sameInstances.remove(ents.begin);
					}
					tEnt.add(ents);
					sameInstances.put(ents.begin, tEnt);
				}
			}
			for (Concept conc : concepts) {
				System.out.println("Concept Index :" + conc.begin + ": " + sameConcepts.get(conc.begin).size());
			}

			for (Entity ents : entities) {
				System.out.println("Instance Index :" + ents.begin + ": " + sameInstances.get(ents.begin).size());
			}
			geoSPATIALRelations.clear();

			int count = 0;
			for (Map.Entry<Integer, String> entry : sortedPatternBasedOnIndex.entrySet()) {
				System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
				// String currentRelation = entry.getValue();
				// int currentIndex = entry.getKey();
				// if(currentRelation.equalsIgnoreCase("r")){
				//
				// if ( previousRelation.equalsIgnoreCase("r")) {
				//
				// }PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
				// }
				// if(currentRelation.equalsIgnoreCase("r")){
				//
				// }
				// else{
				// previousRelation = currentRelation;
				// }

			}

			// Collections.sort(patternIndex);
			//
			// logger.info("The generated pattern Index is: {} " +
			// patternIndex.toString());

			// logger.info("The generate geosparql is :{} ", newGeoSparqlQuery);
			// geoSparqlQuery = "";

			// geoSPATIALRelations = (List<String>) mapOfGeoRelation.values();
			String templateFileName = "";

			String host = "pyravlos1.di.uoa.gr";
			Integer port = 8080;
			String appName = "geoqa/Query";
			String query = "";
			String format = "TSV";
			String sparqlQuesry = "";

			for (String spatialTempRelation : mapOfGeoRelation.values()) {
				geoSPATIALRelations.add(spatialTempRelation);
			}

			List<String> allSparqlQueries = new ArrayList<String>();
//			for(Concept conc:concepts) {
//				String sparqlQuery="";
//				for(Entity ents:entities) {
//					if(conc.link.contains("dbpedia")&&ents.uri.contains("dbpedia")) {
//						
//					}
//				}
//			}

			String identifiedPattern = "";
			if (sameConcepts.size() == 2 && geoSPATIALRelations.size() == 1 && sameInstances.size() == 0) {
				System.out.println("Detected Pattern : CRC ");
				identifiedPattern = "CRC";
				List<List<Concept>> concpetLists = new ArrayList<List<Concept>>();
				for (Map.Entry<Integer, List<Concept>> entry : sameConcepts.entrySet()) {
					List<Concept> iteratorConcept = entry.getValue();
					concpetLists.add(iteratorConcept);

				}

				String spatialRelation = mappingOfGeospatialRelationsToGeosparqlFunctions
						.get(geoSPATIALRelations.get(0));

				for (Concept con1 : concpetLists.get(0)) {
					for (Concept con2 : concpetLists.get(1)) {
						if (con1.link.contains("dbpedia")) {
							if (con2.link.contains("dbpedia")) {
								if (spatialRelation.contains("within")) {
									String sparqlQ = "select ?x where { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
											+ con1.link + ">. ?y rdf:type <" + con2.link + ">. ?x ?p1 ?y.} }";
									allSparqlQueries.add(sparqlQ);
								}

								if (spatialRelation.contains("crosses")) {
									String sparqlQ = "select ?x where { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
											+ con1.link + ">. ?y rdf:type <" + con2.link + ">. ?x dbo:crosses ?y. }}";
									allSparqlQueries.add(sparqlQ);
								}

							}
						} else {
							if (spatialRelation.contains("within")) {
								String sparqlQ = "select ?x where { ?x rdf:type <" + con1.link
										+ ">; geo:hasGeometry ?cGeom1. ?cGeom1 geo:asWKT ?cWKT1. ?y rdf:type <"
										+ con2.link
										+ ">; geo:hasGeometry ?cGeom2. ?cGeom2 geo:asWKT ?cWKT2. FILTER(geof:sfWithin(?cWKT1,?cWKT2)}";
								allSparqlQueries.add(sparqlQ);
							}

							if (spatialRelation.contains("near")) {

								String sparqlQ = "select ?x where { ?x rdf:type <" + con1.link
										+ ">; geo:hasGeometry ?cGeom1. ?cGeom1 geo:asWKT ?cWKT1. ?y rdf:type <"
										+ con2.link
										+ ">; geo:hasGeometry ?cGeom2. ?cGeom2 geo:asWKT ?cWKT2. FILTER(geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000)}";
								if (thresholdFlag) {
									sparqlQ = sparqlQ.replace("1000", thresholdDistance);
								}

								else {
									if (con2.link.contains("Restaurant") || con2.link.contains("Park")) {
										sparqlQ = sparqlQ.replace("1000", "500");
									}
									if (con2.link.contains("City")) {
										sparqlQ = sparqlQ.replace("1000", "5000");
									}
								}
								allSparqlQueries.add(sparqlQ);
							}

//							if (spatialRelation.contains("crosses")) {
//								String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
//										+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <" + ents.uri
//										+ "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. FILTER(geof:sfCrosses(?cWKT,?iWKT)}";
//								allSparqlQueries.add(sparqlQ);
//							}
						}
					}
				}
			}

			if (sameConcepts.size() == 2 && geoSPATIALRelations.size() == 2 && sameInstances.size() == 1) {
				System.out.println("Detected Pattern : CRCRI ");
				identifiedPattern = "CRCRI";
				List<Entity> instanceLists = new ArrayList<Entity>();
				for (Map.Entry<Integer, List<Entity>> entry : sameInstances.entrySet()) {
					instanceLists = entry.getValue();
				}

				List<List<Concept>> concpetLists = new ArrayList<List<Concept>>();
				for (Map.Entry<Integer, List<Concept>> entry : sameConcepts.entrySet()) {
					List<Concept> iteratorConcept = entry.getValue();
					concpetLists.add(iteratorConcept);
				}

				List<String> geoSpatialRelations = new ArrayList<>();
				geoSpatialRelations
						.add(mappingOfGeospatialRelationsToGeosparqlFunctions.get(geoSPATIALRelations.get(0)));
				geoSpatialRelations
						.add(mappingOfGeospatialRelationsToGeosparqlFunctions.get(geoSPATIALRelations.get(1)));

				for (Concept con1 : concpetLists.get(0)) {
					for (Concept con2 : concpetLists.get(1)) {
						for (Entity ents : instanceLists) {
							if (con1.link.contains("dbpedia")) {
								if (con2.link.contains("dbpedia")) {
									if (ents.uri.contains("dbpedia.org")) {
										if (geoSPATIALRelations.get(0).contains("within")) {
											if (geoSPATIALRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
														+ con1.link + ">. ?y rdf:type <" + con2.link + ">. ?x ?p1 ?y. <"
														+ con2.link + "> ?p2 <" + ents.uri + ">.  } }";
												allSparqlQueries.add(sparqlQ);
											}
										}
									}
								}
							} else {
								if (!con2.link.contains("dbpedia")) {
									if (ents.uri.contains("dbpedia.org")) {
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con1.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?y rdf:type <"
														+ con2.link
														+ ">; geo:hasGeometry ?geom2. ?geom2 geo:asWKT ?cWKT2. ?instance owl:sameAs <"
														+ ents.uri
														+ ">; geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT.   FILTER(geof:sfWithin(?cWKT1, ?cWKT2) && geof:sfWithin(?cWKT2, ?iWKT) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("near")) {
											if (geoSpatialRelations.get(1).contains("within")) {

												String sparqlQ = "select ?x { ?x rdf:type <" + con1.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?y rdf:type <"
														+ con2.link
														+ ">; geo:hasGeometry ?geom2. ?geom2 geo:asWKT ?cWKT2. ?instance owl:sameAs <"
														+ ents.uri
														+ ">; geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT.   FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000)  && geof:sfWithin(?cWKT2, ?iWKT) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con2.link.contains("Restaurant")
															|| con2.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con2.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}
												allSparqlQueries.add(sparqlQ);

											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("near")) {

												String sparqlQ = "select ?x { ?x rdf:type <" + con1.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?y rdf:type <"
														+ con2.link
														+ ">; geo:hasGeometry ?geom2. ?geom2 geo:asWKT ?cWKT2. ?instance owl:sameAs <"
														+ ents.uri
														+ ">; geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT.   FILTER((geof:distance(?cWKT2, ?iWKT,uom:metre) < 1000)  && geof:sfWithin(?cWKT1, ?cWKT2) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con2.link.contains("Restaurant")
															|| con2.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con2.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}
												allSparqlQueries.add(sparqlQ);
											}
										}

									} else {
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con1.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?y rdf:type <"
														+ con2.link
														+ ">; geo:hasGeometry ?geom2. ?geom2 geo:asWKT ?cWKT2. <"
														+ ents.uri
														+ "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT.   FILTER(geof:sfWithin(?cWKT1, ?cWKT2) && geof:sfWithin(?cWKT2, ?iWKT) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("near")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con1.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?y rdf:type <"
														+ con2.link
														+ ">; geo:hasGeometry ?geom2. ?geom2 geo:asWKT ?cWKT2. <"
														+ ents.uri
														+ "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT.   FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000)  && geof:sfWithin(?cWKT2, ?iWKT) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con2.link.contains("Restaurant")
															|| con2.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con2.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}

												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("near")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con1.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?y rdf:type <"
														+ con2.link
														+ ">; geo:hasGeometry ?geom2. ?geom2 geo:asWKT ?cWKT2. <"
														+ ents.uri
														+ "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT.   FILTER((geof:distance(?cWKT2, ?iWKT,uom:metre) < 1000)  && geof:sfWithin(?cWKT1, ?cWKT2) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con2.link.contains("Restaurant")
															|| con2.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con2.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}

												allSparqlQueries.add(sparqlQ);
											}
										}

									}
								}
							}
						}
					}
				}

			}
			if (sameConcepts.size() == 1 && geoSPATIALRelations.size() == 2 && sameInstances.size() == 2) {
				System.out.println("Detected Pattern : CRIRI ");
				identifiedPattern = "CRIRI";
				List<Concept> concpetLists = new ArrayList<Concept>();
				for (Map.Entry<Integer, List<Concept>> entry : sameConcepts.entrySet()) {
					concpetLists = entry.getValue();
				}

				List<List<Entity>> instanceLists = new ArrayList<List<Entity>>();
				for (Map.Entry<Integer, List<Entity>> entry : sameInstances.entrySet()) {
					List<Entity> iteratorInstance = entry.getValue();
					instanceLists.add(iteratorInstance);
				}

				List<String> geoSpatialRelations = new ArrayList<>();
				geoSpatialRelations
						.add(mappingOfGeospatialRelationsToGeosparqlFunctions.get(geoSPATIALRelations.get(0)));
				geoSpatialRelations
						.add(mappingOfGeospatialRelationsToGeosparqlFunctions.get(geoSPATIALRelations.get(1)));

				for (Concept con : concpetLists) {

					for (Entity ent1 : instanceLists.get(0)) {
						for (Entity ent2 : instanceLists.get(1)) {
							if (con.link.contains("dbpedia.org")) {
								if (ent1.uri.contains("dbpedia.org")) {
									if (ent2.uri.contains("dbpedia.org")) {
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
														+ con.link + ">. ?x ?p1 <" + ent1.uri + ">. <" + ent1.uri
														+ "> ?p2 <" + ent2.uri + ">. } }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("crosses")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
														+ con.link + ">. ?x dbp:crosses <" + ent1.uri + ">. <"
														+ ent1.uri + "> ?p2 <" + ent2.uri + ">. } }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("crosses")) {
												String sparqlQ = "select ?x { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
														+ con.link + ">. ?x ?p1 <" + ent1.uri + ">. <" + ent1.uri
														+ "> dbp:crosses <" + ent2.uri + ">.  } }";
												allSparqlQueries.add(sparqlQ);
											}
										}

									}
								}
							} else {
								if (ent1.uri.contains("dbpedia.org")) {
									if (ent2.uri.contains("dbpedia.org")) {
//										System.out.println("Inside the====== osm dbp dbp===========");
//										System.out.println("geospatial relation : "+ geoSPATIALRelations.get(0)+"::::"+geoSPATIALRelations.get(1));
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance2 owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("near")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance2 owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER((geof:distance(?cWKT, ?iWKT1, uom:metre) <= 1000) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												} else {
													if (con.link.contains("Restaurant") || con.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("near")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance2 owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER((geof:distance(?cWKT, ?iWKT2, uom:metre) <= 1000) && geof:sfWithin(?cWKT, ?iWKT1) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												} else {
													if (con.link.contains("Restaurant") || con.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}
												allSparqlQueries.add(sparqlQ);
											}
										}

										if (geoSpatialRelations.get(0).contains("crosses")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance2 owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfCrosses(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("crosses")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance2 owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfCrosses(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("boundry")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance2 owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfTouches(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("boundry")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance2 owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfTouches(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
									}
								}
								if (ent1.uri.contains("dbpedia.org")) {
									if (!ent2.uri.contains("dbpedia.org")) {
//										System.out.println("Inside the====== osm dbp not-dbp===========");
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("near")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER((geof:distance(?cWKT, ?iWKT1, uom:metre) <= 1000) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con.link.contains("Restaurant") || con.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}

												allSparqlQueries.add(sparqlQ);
											}
										}

										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("near")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER((geof:distance(?cWKT, ?iWKT2, uom:metre) <= 1000) && geof:sfWithin(?cWKT, ?iWKT1) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con.link.contains("Restaurant") || con.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}

												allSparqlQueries.add(sparqlQ);
											}
										}

										if (geoSpatialRelations.get(0).contains("crosses")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfCrosses(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("crosses")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfCrosses(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("boundry")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfTouches(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("boundry")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. ?instance1 owl:sameAs <"
														+ ent1.uri
														+ ">; geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfTouches(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
									}
								}
								if (!ent1.uri.contains("dbpedia.org")) {
									if (ent2.uri.contains("dbpedia.org")) {
//										System.out.println("Inside the====== osm not-dbp dbp===========");
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("near")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER((geof:distance(?cWKT, ?iWKT1, uom:metre) <= 1000) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con.link.contains("Restaurant") || con.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}

												allSparqlQueries.add(sparqlQ);
											}
										}

										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("near")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER((geof:distance(?cWKT, ?iWKT2, uom:metre) <= 1000) && geof:sfWithin(?cWKT, ?iWKT1) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con.link.contains("Restaurant") || con.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}

												allSparqlQueries.add(sparqlQ);
											}
										}

										if (geoSpatialRelations.get(0).contains("crosses")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfCrosses(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("crosses")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfCrosses(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("boundry")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfTouches(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("boundry")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  ?instance owl:sameAs <"
														+ ent2.uri
														+ ">; geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfTouches(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
									}
								}
								if (!ent1.uri.contains("dbpedia.org")) {
									if (!ent2.uri.contains("dbpedia.org")) {
//										System.out.println("Inside the====== osm not-dbp not-dbp===========");
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("near")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER((geof:distance(?cWKT, ?iWKT1, uom:metre) <= 1000) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con.link.contains("Restaurant") || con.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}

												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("near")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && (geof:distance(?cWKT, ?iWKT2,uom:metre) < 1000) ) }";

												if (thresholdFlag) {
													sparqlQ = sparqlQ.replace("1000", thresholdDistance);
												}

												else {
													if (con.link.contains("Restaurant") || con.link.contains("Park")) {
														sparqlQ = sparqlQ.replace("1000", "500");
													}
													if (con.link.contains("City")) {
														sparqlQ = sparqlQ.replace("1000", "5000");
													}
												}

												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("crosses")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfCrosses(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("crosses")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfCrosses(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("within")) {
											if (geoSpatialRelations.get(1).contains("boundry")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?cWKT, ?iWKT1) && geof:sfTouches(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
										if (geoSpatialRelations.get(0).contains("boundry")) {
											if (geoSpatialRelations.get(1).contains("within")) {
												String sparqlQ = "select ?x { ?x rdf:type <" + con.link
														+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?cWKT. <"
														+ ent1.uri
														+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1.  <"
														+ ent2.uri
														+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfTouches(?cWKT, ?iWKT1) && geof:sfWithin(?iWKT1, ?iWKT2) ) }";
												allSparqlQueries.add(sparqlQ);
											}
										}
									}
								}
							}
						}
					}

				}

			}
			if (sameConcepts.size() == 0 && (geoSPATIALRelations.size() == 2 || geoSPATIALRelations.size() == 1)
					&& sameInstances.size() == 2) {
				System.out.println("Detected Pattern : IRI ");
				identifiedPattern = "IRI";
				List<List<Entity>> instanceLists = new ArrayList<List<Entity>>();
				for (Map.Entry<Integer, List<Entity>> entry : sameInstances.entrySet()) {
					List<Entity> iteratorInstance = entry.getValue();
					instanceLists.add(iteratorInstance);

				}

				String spatialRelation = mappingOfGeospatialRelationsToGeosparqlFunctions
						.get(geoSPATIALRelations.get(0));
				for (Entity ent1 : instanceLists.get(0)) {
					for (Entity ent2 : instanceLists.get(1)) {
						if (ent1.uri.contains("dbpedia.org")) {
							if (ent2.uri.contains("dbpedia.org")) {
								if (spatialRelation.contains("within")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> ?y <" + ent2.uri + ">. } }";
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("crosses")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbo:crosses <" + ent2.uri + ">. } }";
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("east")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbp:east <" + ent2.uri + ">. } }";
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("west")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbp:west <" + ent2.uri + ">. } }";
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("north")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbp:north <" + ent2.uri + ">. } }";
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("south")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbp:south <" + ent2.uri + ">. } }";
									allSparqlQueries.add(sparqlQ);
								}
							}
						} else if (!ent2.uri.contains("dbpedia.org")) {
							if (spatialRelation.contains("within")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?iWKT1, ?iWKT2)) }";
								allSparqlQueries.add(sparqlQ);
							}
							if (spatialRelation.contains("near")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:distance(?iWKT1, ?iWKT2) < 10000) }";
								allSparqlQueries.add(sparqlQ);
							}

							if (spatialRelation.contains("east")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(strdf:right(?iWKT1, ?iWKT2)) }";
								allSparqlQueries.add(sparqlQ);
							}
							if (spatialRelation.contains("west")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(strdf:left(?iWKT1, ?iWKT2)) }";
								allSparqlQueries.add(sparqlQ);
							}
							if (spatialRelation.contains("north")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(strdf:above(?iWKT1, ?iWKT2)) }";
								allSparqlQueries.add(sparqlQ);
							}
							if (spatialRelation.contains("south")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(strdf:below(?iWKT1, ?iWKT2)) }";
								allSparqlQueries.add(sparqlQ);
							}
						}
					}
				}

			}
			if (sameConcepts.size() == 1 && geoSPATIALRelations.size() == 1 && sameInstances.size() == 1) {
				System.out.println("Detected Pattern : CRI ");
				identifiedPattern = "CRI";
				for (Map.Entry<Integer, List<Concept>> entry : sameConcepts.entrySet()) {
					List<Concept> iteratorConcept = entry.getValue();
					for (Map.Entry<Integer, List<Entity>> entryE : sameInstances.entrySet()) {
						List<Entity> iteratorEntity = entryE.getValue();
						String spatialRelation = mappingOfGeospatialRelationsToGeosparqlFunctions
								.get(geoSPATIALRelations.get(0));

						for (Concept con : iteratorConcept) {
							for (Entity ents : iteratorEntity) {

								if (con.link.contains("dbpedia")) {
									if (ents.uri.contains("dbpedia.org")) {
										if (spatialRelation.contains("within")) {
											String sparqlQ = "select ?x where { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
													+ con.link + ">. ?x ?p1 <" + ents.uri + ">.} }";
											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("crosses")) {
											String sparqlQ = "select ?x where { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
													+ con.link + ">. ?x dbo:crosses <" + ents.uri + ">. } }";
											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("north")) {
											String sparqlQ = "select ?x where { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
													+ con.link + ">. ?x dbp:north <" + ents.uri + ">. } }";
											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("east")) {
											String sparqlQ = "select ?x where { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
													+ con.link + ">. ?x dbp:east <" + ents.uri + ">. } }";
											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("west")) {
											String sparqlQ = "select ?x where { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
													+ con.link + ">. ?x dbp:west <" + ents.uri + ">. } }";
											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("south")) {
											String sparqlQ = "select ?x where { SERVICE <http://dbpedia.org/sparql> { ?x rdf:type <"
													+ con.link + ">. ?x dbp:south <" + ents.uri + ">. } }";
											allSparqlQueries.add(sparqlQ);
										}

									}
								} else {
									if (ents.uri.contains("dbpedia.org")) {

										if (spatialRelation.contains("within")) {

											String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
													+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. ?instance owl:sameAs <"
													+ ents.uri
													+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT. FILTER(geof:sfWithin(?cWKT,?iWKT))}";
											allSparqlQueries.add(sparqlQ);
										}
										if (spatialRelation.contains("near")) {

											String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
													+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. ?instance owl:sameAs <"
													+ ents.uri
													+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT. FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }";

											if (thresholdFlag) {
												sparqlQ = sparqlQ.replace("1000", thresholdDistance);
											}

											else {
												if (con.link.contains("Restaurant") || con.link.contains("Park")) {
													sparqlQ = sparqlQ.replace("1000", "500");
												}
												if (con.link.contains("City")) {
													sparqlQ = sparqlQ.replace("1000", "5000");
												}
											}
											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("crosses")) {

											String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
													+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. ?instance owl:sameAs <"
													+ ents.uri
													+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT. FILTER(geof:sfCrosses(?cWKT,?iWKT))}";
											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("boundry")) {

											String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
													+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. ?instance owl:sameAs <"
													+ ents.uri
													+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT. FILTER(geof:sfTouches(?cWKT,?iWKT))}";
											allSparqlQueries.add(sparqlQ);
										}
									} else {
										if (spatialRelation.contains("within")) {
											String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
													+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <" + ents.uri
													+ "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. FILTER(geof:sfWithin(?cWKT,?iWKT))}";
											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("near")) {
											String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
													+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <" + ents.uri
													+ "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }";

											if (thresholdFlag) {
												sparqlQ = sparqlQ.replace("1000", thresholdDistance);
											}

											else {
												if (con.link.contains("Restaurant") || con.link.contains("Park")) {
													sparqlQ = sparqlQ.replace("1000", "500");
												}
												if (con.link.contains("City")) {
													sparqlQ = sparqlQ.replace("1000", "5000");
												}
											}

											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("crosses")) {
											String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
													+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <" + ents.uri
													+ "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. FILTER(geof:sfCrosses(?cWKT,?iWKT))}";
											allSparqlQueries.add(sparqlQ);
										}

										if (spatialRelation.contains("boundry")) {

											String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
													+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <" + ents.uri
													+ "> geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT. FILTER(geof:sfTouches(?cWKT,?iWKT))}";
											allSparqlQueries.add(sparqlQ);
										}
									}
								}

							}
						}
					}
				}
			}

			if (allSparqlQueries.isEmpty()) {
				System.out.println("Can not Answer ");
			}
			String finalQuery = "";

			if (allSparqlQueries.size() == 1) {
				finalQuery = allSparqlQueries.get(0);
			} else {
				int index = -1;
				for (int i = 0; i < allSparqlQueries.size(); i++) {
					if (identifiedPattern.equals("IRI")) {
//						System.out.println("getting Inside:===========");
						if (allSparqlQueries.get(i).contains("dbpedia.org/resource")) {
//							System.out.println("Selecting query====");
							index = i;
							break;
						}
					} else if (allSparqlQueries.get(i).contains("dbpedia.org/ontology")) {
						index = i;
						break;
					}
				}
				if (index != -1) {
					finalQuery = allSparqlQueries.get(index);
//					System.out.println("Selected Index: " + index);
				} else {
					if (finalQuery.equals("")) {
						for (int i = 0; i < allSparqlQueries.size(); i++) {
							if (allSparqlQueries.get(i).contains("http://www.app-lab.eu/gadm/AdministrativeUnit"))
								index = i;
						}

					}
					if (index != -1) {
						finalQuery = allSparqlQueries.get(index);
					} else {
						if (finalQuery.equals("")) {
							for (int i = 0; i < allSparqlQueries.size(); i++) {
								if (allSparqlQueries.get(i).contains("http://www.app-lab.eu/osm/england")
										|| allSparqlQueries.get(i).contains("http://www.app-lab.eu/osm/scotland")
										|| allSparqlQueries.get(i).contains("http://www.app-lab.eu/osm/wales")
										|| allSparqlQueries.get(i)
												.contains("http://www.app-lab.eu/osm/irelandandnorthernireland"))
									index = i;
							}

						}
						if (index != -1) {
							finalQuery = allSparqlQueries.get(index);
						}
					}
				}
			}
			for (String queries : allSparqlQueries) {
				System.out.println("Generated Query: " + queries);
			}
			System.out.println("Total number of Generated queries: "+allSparqlQueries.size());
			if (!finalQuery.equals("")) {
				System.out.println("Selected query : " + finalQuery);
			}
//			if (!allSparqlQueries.isEmpty()) {
//				
//				if(myQuestion.contains("Is there ")||myQuestion.contains("Are there ")) {
//					if (finalQuery.contains("select")) {
//						finalQuery = finalQuery.replace("select ?x where", " ASK ");
//					}
//				}
//				
//				if (finalQuery.contains("select")) {
//					finalQuery = finalQuery.replace("select", "select distinct ");
//					System.out.println("Selected query : " + finalQuery);
//
//					String prefixString = "PREFIX geo: <http://www.opengis.net/ont/geosparql#> "
//							+ "PREFIX geof: <http://www.opengis.net/def/function/geosparql/> "
//							+ "PREFIX geor: <http://www.opengis.net/def/rule/geosparql/> "
//							+ "PREFIX strdf: <http://strdf.di.uoa.gr/ontology#> "
//							+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
//							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
//							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
//							+ "PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/> "
//							+ "PREFIX dbo: <http://dbpedia.org/ontology/> " + "PREFIX gadmr: <http://www.app-lab.eu/gadm/> "
//							+ "PREFIX gadmo: <http://www.app-lab.eu/gadm/ontology/> "
//							+ "PREFIX osm: <http://www.app-lab.eu/osm/ontology#> "
//							+ "PREFIX dbp: <http://dbpedia.org/property/> "
//							+ "PREFIX dbc: <http://dbpedia.org/resource/Category:> "
//							+ "PREFIX dct: <http://purl.org/dc/terms/> " + "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
//							+ "PREFIX dbr: <http://dbpedia.org/resource/>    ";
//					finalQuery = prefixString + finalQuery;
//					Query executableQuery = QueryFactory.create(finalQuery);
////					System.out.println("sparql query :" + query.toString());
//					QueryExecution exec = QueryExecutionFactory.sparqlService("http://pyravlos2.di.uoa.gr:8080/geoqa/Query",
//							executableQuery);
//					ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
//
//					BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmen/results_gir.csv", true));
//					bw.newLine();
//					bw.write(myQuestion + ", ");
//
//					if (!results.hasNext()) {
//
//					} else {
//						while (results.hasNext()) {
//
//							QuerySolution qs = results.next();
//
//							String selectVariable = "";
//							if (finalQuery.contains("select ")) {
//								selectVariable = "x";
//								String uria = qs.get("x").toString();
//								// String links = qs.get("link").toString();
////								System.out.println("x: "+uria );
//								bw.write(uria);
//								bw.write(",");
//							} 
//						}
//					}
//					bw.close();
//				}
//				if(finalQuery.contains("ASK")) {
//					String prefixString = "PREFIX geo: <http://www.opengis.net/ont/geosparql#> "
//							+ "PREFIX geof: <http://www.opengis.net/def/function/geosparql/> "
//							+ "PREFIX geor: <http://www.opengis.net/def/rule/geosparql/> "
//							+ "PREFIX strdf: <http://strdf.di.uoa.gr/ontology#> "
//							+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
//							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
//							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
//							+ "PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/> "
//							+ "PREFIX dbo: <http://dbpedia.org/ontology/> " + "PREFIX gadmr: <http://www.app-lab.eu/gadm/> "
//							+ "PREFIX gadmo: <http://www.app-lab.eu/gadm/ontology/> "
//							+ "PREFIX osm: <http://www.app-lab.eu/osm/ontology#> "
//							+ "PREFIX dbp: <http://dbpedia.org/property/> "
//							+ "PREFIX dbc: <http://dbpedia.org/resource/Category:> "
//							+ "PREFIX dct: <http://purl.org/dc/terms/> " + "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
//							+ "PREFIX dbr: <http://dbpedia.org/resource/>    ";
//					finalQuery = prefixString + finalQuery;
//					Query executableQuery = QueryFactory.create(finalQuery);
//					//System.out.println("sparql query :" + query.toString());
//					QueryExecution exec = QueryExecutionFactory.sparqlService("http://pyravlos2.di.uoa.gr:8080/geoqa/Query",
//							executableQuery);
//					boolean answer = exec.execAsk();
//					BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmen/results_gir.csv", true));
//					bw.newLine();
//					bw.write(myQuestion + ", ");
//					bw.write(""+answer+"");
//					bw.close();
//				}
//
//			} else {
//				BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmen/results_gir.csv", true));
//				bw.newLine();
//				bw.write(myQuestion + ", ");
//				bw.write("Can not answer");
//				bw.close();
//			}

			Map<String, String> parameters = new HashMap<>();
//			if (concepts.size() == 1) {
//				concept = concepts.get(0);
//				logger.info("Inside the concept size 1");
//				logger.info("The relation size is: {} ", geoSPATIALRelations.size());
//				if (geoSPATIALRelations.size() == 1) { // CRI pattern
//					logger.info("Inside the relation size 1");
//					String spatialRelation = geoSPATIALRelations.get(0);
//					if (entities.size() == 1) {
//
//						logger.info("Inside the instance size 1" + spatialRelation);
//						ent = entities.get(0);
//						// if
//						// (mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialRelation)
//						// .equalsIgnoreCase("within")) {
//						detectedPattern = "cri_"
//								+ mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialRelation);
//						if (thresholdFlag && !mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialRelation)
//								.contains("cross")) {
//							System.out.println("thresholdDistance: " + thresholdDistance);
//							detectedPattern += "_th";
//						}
//						templateFileName = detectedPattern + ".sparql";
//						logger.info("Generated File name: {} ", templateFileName);
//						geoSparqlQuery = QueryFactory.read("./src/main/resources/config/" + templateFileName)
//								.toString();
//						// instance = ent.namedEntity;
//						// instanceURI = ent.uri;
//						// poi = myQuestion.substring(concept.begin,
//						// concept.end);
//						if (thresholdFlag) {
//							geoSparqlQuery = geoSparqlQuery.replaceAll("3000", thresholdDistance);
//						}
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poiURI", concept.link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("instanceURI", ent.uri);
//
//						if (dbpediaPropertyFlag) {
//							String DBpediaTriples = ". ?poi owl:sameAs ?DBpediapoi.";
//
//							int counter = 0;
//							List<String> listOfTriples = new ArrayList<String>();
//							if (dbpediaPropertyValueFlag) {
//								for (int i = 0; i < propertiesValue.size(); i++) {
//									listOfTriples.add("?DBpediapoi <" + properties.get(i) + "> \""
//											+ propertiesValue.get(i) + "\". ");
//									counter++;
//								}
//							}
//							for (int i = counter; i < properties.size(); i++) {
//								listOfTriples.add("?DBpediapoi <" + properties.get(i) + "> ?value"
//										+ properties.get(i).substring(properties.get(i).lastIndexOf("/") + 1) + ". ");
//								// geoSparqlQuery =
//								// geoSparqlQuery.replace("FILTER", ".
//								// ?DBpediapoi <"+property+">
//								// ?value"+property.substring(property.lastIndexOf("/")+1)+"
//								// FILTER");
//							}
//							for (String singleTriple : listOfTriples) {
//								DBpediaTriples += singleTriple;
//							}
//							geoSparqlQuery = geoSparqlQuery.replace("FILTER", DBpediaTriples + " FILTER");
//							// sparqlQuesry = "select * from
//							// <http://dbpedia.org> where { " +DBpediaTriples+ "
//							// }";
//
//							System.out.println("DBpedia sparql query is : " + sparqlQuesry);
//
//							// if(!thresholdDistance.equals("")){
//							// DBpediaTriples += "";
//							// }
//
//						}
//						if (myQuestion.toLowerCase().contains("how many")) {
//							geoSparqlQuery = geoSparqlQuery.replace("SELECT  ?poi",
//									"SELECT  (count( DISTINCT ?poi) as ?total) ");
//						}
//
//						if (myQuestion.toLowerCase().contains("are there")
//								|| myQuestion.toLowerCase().contains("is there")) {
//							geoSparqlQuery = geoSparqlQuery.replace("SELECT  ?poi", "ASK ");
//							geoSparqlQuery = geoSparqlQuery.replace("WHERE", " ");
//						}
//						// parameters.put("poiURI", concept.link);
//						// parameters.put("instanceURI", ent.uri);
//						logger.info("{} with parameters: {}", detectedPattern, parameters);
//						// geoSparqlQuery =
//						// bindVariablesInSparqlQuery(geoSparqlQuery,
//						// parameters);
//
//						// newGeoSparqlQuery += "FILTER().";
//						if (templateFileName.contains("within")) {
//							String conc = concept.link;// .substring(concept.link.lastIndexOf("#")
//														// + 1);
//							if (conc.contains("app-lab")) {
//								conc = conc.substring(concept.link.lastIndexOf("#") + 1);
//								conc = "http://dbpedia.org/ontology/" + conc.substring(0, 1).toUpperCase()
//										+ conc.substring(1);
//							}
//
//							String testSparqlQuery = "select ?poi where { ?poi ?p1 <" + conc + ">. ?poi ?p2 <" + ent.uri
//									+ "> . }";
//							// System.out.println("sparql query is:
//							// "+testSparqlQuery);
//
//							Query querym = QueryFactory.create(testSparqlQuery);
//
//							QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql",
//									querym);
//							ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
//							if (!results.hasNext()) {
//
//							} else {
//								while (results.hasNext()) {
//
//									QuerySolution qs = results.next();
//									String uria = qs.get("poi").toString();
//									// String links = qs.get("link").toString();
//									System.out.println("poi: " + uria);
//								}
//							}
//							System.out.println("sparql query :" + querym.toString());
//						}
//					}
//				}
//				if (geoSPATIALRelations.size() == 2 || geoSPATIALRelations.size() == 3) {
//					String spatialRelation = "";
//					logger.info("Inside the relation size 2");
//
//					if (entities.size() == 1) {
//
//						spatialRelation = "";
//						logger.info("Inside the instance size 1");
//						ent = entities.get(0);
//						// if
//						// (mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialRelation)
//						// .equalsIgnoreCase("within")) {
//						for (String spatialTempRelation : geoSPATIALRelations) {
//							// System.out.println("The Relation is : "
//							// +
//							// mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialTempRelation));
//							String tempSpatialRelation = mappingOfGeospatialRelationsToGeosparqlFunctions
//									.get(spatialTempRelation);
//							if ((tempSpatialRelation.contains("west") || tempSpatialRelation.contains("east")
//									|| tempSpatialRelation.contains("south") || tempSpatialRelation.contains("north"))
//									&& !spatialRelation.contains("within")) {
//								spatialRelation += mappingOfGeospatialRelationsToGeosparqlFunctions
//										.get(spatialTempRelation) + "_";
//								System.out.println("The Relation is : ===  " + spatialRelation);
//							} else {
//								if (spatialRelation.length() > 2
//										&& (!spatialRelation.contains("west") || !spatialRelation.contains("east")
//												|| !spatialRelation.contains("south")
//												|| !spatialRelation.contains("north"))
//										&& !tempSpatialRelation.contains("within")) {
//									spatialRelation = mappingOfGeospatialRelationsToGeosparqlFunctions
//											.get(spatialTempRelation) + "_";
//								} else if (spatialRelation.length() < 2) {
//									spatialRelation += mappingOfGeospatialRelationsToGeosparqlFunctions
//											.get(spatialTempRelation) + "_";
//								}
//								System.out.println("The Relation is : " + spatialRelation);
//							}
//						}
//						detectedPattern = "cri_" + spatialRelation.substring(0, spatialRelation.lastIndexOf('_'));
//
//						if (StringUtils.countMatches(spatialRelation, "_") < 3 && !thresholdDistance.equals("")) {
//							detectedPattern += "_th";
//						}
//						templateFileName = detectedPattern + ".sparql";
//						logger.info("Generated File name: {} ", templateFileName);
//						geoSparqlQuery = QueryFactory.read("./src/main/resources/config/" + templateFileName)
//								.toString();
//						// instance = ent.namedEntity;
//						// instanceURI = ent.uri;
//						// poi = myQuestion.substring(concept.begin,
//						// concept.end);
//						if (!thresholdDistance.equals("")) {
//							geoSparqlQuery = geoSparqlQuery.replaceAll("3000", thresholdDistance);
//						}
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poiURI", concept.link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("instanceURI", ent.uri);
//						// parameters.put("poiURI", concept.link);
//						// parameters.put("instanceURI", ent.uri);
//						logger.info("{} with parameters: {}", detectedPattern, parameters);
//						// geoSparqlQuery =
//						// bindVariablesInSparqlQuery(geoSparqlQuery,
//						// parameters);
//
//						// newGeoSparqlQuery += "FILTER().";
//
//						if (dbpediaPropertyFlag) {
//							String DBpediaTriples = "?poi owl:sameAs ?DBpediapoi. ";
//							int counter = 0;
//							List<String> listOfTriples = new ArrayList<String>();
//							if (dbpediaPropertyValueFlag) {
//								for (int i = 0; i < propertiesValue.size(); i++) {
//									listOfTriples.add("?DBpediapoi <" + properties.get(i) + "> \""
//											+ propertiesValue.get(i) + "\". ");
//									counter++;
//								}
//							}
//							for (int i = counter; i < properties.size(); i++) {
//								listOfTriples.add("?DBpediapoi <" + properties.get(i) + "> ?value"
//										+ properties.get(i).substring(properties.get(i).lastIndexOf("/") + 1) + ". ");
//								// geoSparqlQuery =
//								// geoSparqlQuery.replace("FILTER", ".
//								// ?DBpediapoi <"+property+">
//								// ?value"+property.substring(property.lastIndexOf("/")+1)+"
//								// FILTER");
//							}
//							for (String singleTriple : listOfTriples) {
//								DBpediaTriples += singleTriple;
//							}
//
//							// if(!thresholdDistance.equals("")){
//							// DBpediaTriples += "";
//							// }
//
//							geoSparqlQuery = geoSparqlQuery.replace("FILTER", ". " + DBpediaTriples + " FILTER");
//
//						}
//
//					}
//					if (entities.size() == 2) {
//						ent = entities.get(0);
//						for (String spatialTempRelation : geoSPATIALRelations) {
//							spatialRelation += mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialTempRelation)
//									+ "_";
//						}
//						detectedPattern = "criri_" + spatialRelation.substring(0, spatialRelation.lastIndexOf('_'));
//						templateFileName = detectedPattern + ".sparql";
//						logger.info("Generated File name: {} ", templateFileName);
//						geoSparqlQuery = QueryFactory.read("./src/main/resources/config/" + templateFileName)
//								.toString();
//						// poi = myQuestion.substring(concept.begin,
//						// concept.end);
//						// geoSparqlQuery = geoSparqlQuery.replaceAll("poi",
//						// myQuestion.substring(concept.begin,
//						// concept.end));
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poiURI", concept.link);
//						// geoSparqlQuery =
//						// geoSparqlQuery.replaceAll("instance",
//						// ent.namedEntity);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("instanceURI", ent.uri);
//						ent = entities.get(1);
//						// geoSparqlQuery =
//						// geoSparqlQuery.replaceAll("instance1",
//						// ent.namedEntity);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("instance1URI", ent.uri);
//
//						// parameters.put("poiURI", concept.link);
//						// parameters.put("instanceURI", entities.get(0).uri);
//						// parameters.put("instance1URI",entities.get(1).uri);
//						logger.info("{} with parameters: {}", detectedPattern, parameters);
//						// geoSparqlQuery =
//						// bindVariablesInSparqlQuery(geoSparqlQuery,
//						// parameters);
//
//						// }
//					}
//				}
//
//			}
//
//			if (concepts.size() == 2) {
//				if (entities.size() == 0) { // CRC pattern
//					String spatialRelation = geoSPATIALRelations.get(0);
//					switch (geoSPATIALRelations.size()) {
//					case 1:
//						logger.info("Inside the instance size 1" + spatialRelation);
//						detectedPattern = "crc_"
//								+ mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialRelation);
//						templateFileName = detectedPattern + ".sparql";
//						logger.info("Generated File name: {} ", templateFileName);
//						geoSparqlQuery = QueryFactory.read("./src/main/resources/config/" + templateFileName)
//								.toString();
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poiURI", concepts.get(0).link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poi1URI", concepts.get(1).link);
//						logger.info("{} with parameters: {}", detectedPattern, parameters);
//						break;
//					case 2:
//
//						for (String spatialTempRelation : geoSPATIALRelations) {
//							// System.out.println("The Relation is : "
//							// +
//							// mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialTempRelation));
//							if (!mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialTempRelation)
//									.equalsIgnoreCase("within")) {
//								spatialRelation += mappingOfGeospatialRelationsToGeosparqlFunctions
//										.get(spatialTempRelation) + "_";
//							}
//						}
//						detectedPattern = "crc_" + spatialRelation.substring(0, spatialRelation.lastIndexOf('_'));
//
//						templateFileName = detectedPattern + ".sparql";
//						logger.info("Generated File name: {} ", templateFileName);
//						geoSparqlQuery = QueryFactory.read("./src/main/resources/config/" + templateFileName)
//								.toString();
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poiURI", concepts.get(0).link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poi1URI", concepts.get(1).link);
//						// logger.info("{} with parameters: {}",
//						// detectedPattern, parameters);
//						break;
//					case 3:
//
//						for (String spatialTempRelation : geoSPATIALRelations) {
//							if (!mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialTempRelation)
//									.equalsIgnoreCase("within")) {
//								spatialRelation += mappingOfGeospatialRelationsToGeosparqlFunctions
//										.get(spatialTempRelation) + "_";
//							}
//						}
//						detectedPattern = "crc_" + spatialRelation.substring(0, spatialRelation.lastIndexOf('_'));
//						templateFileName = detectedPattern + ".sparql";
//						logger.info("Generated File name: {} ", templateFileName);
//						geoSparqlQuery = QueryFactory.read("./src/main/resources/config/" + templateFileName)
//								.toString();
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poiURI", concepts.get(0).link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poi1URI", concepts.get(1).link);
//						logger.info("{} with parameters: {}", detectedPattern, parameters);
//						break;
//					default:
//						break;
//					}
//				}
//				if (entities.size() == 1) {
//					ent = entities.get(0);
//					String spatialRelation = "";
//					switch (geoSPATIALRelations.size()) {
//					case 2:
//
//						for (String spatialTempRelation : geoSPATIALRelations) {
//							System.out.println("The Relation is : "
//									+ mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialTempRelation));
//							spatialRelation += mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialTempRelation)
//									+ "_";
//						}
//						detectedPattern = "crcri_" + spatialRelation.substring(0, spatialRelation.lastIndexOf('_'));
//						if (!thresholdDistance.equals("")) {
//							System.out.println("thresholdDistance: " + thresholdDistance);
//							detectedPattern += "_th";
//						}
//
//						templateFileName = detectedPattern + ".sparql";
//						logger.info("Generated File name: {} ", templateFileName);
//						geoSparqlQuery = QueryFactory.read("./src/main/resources/config/" + templateFileName)
//								.toString();
//
//						if (!thresholdDistance.equals("")) {
//							geoSparqlQuery = geoSparqlQuery.replaceAll("3000", thresholdDistance);
//						}
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poiURI", concepts.get(0).link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poi1URI", concepts.get(1).link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("instanceURI", ent.uri);
//						logger.info("{} with parameters: {}", detectedPattern, parameters);
//						break;
//					case 3:
//
//						for (String spatialTempRelation : geoSPATIALRelations) {
//							spatialRelation += mappingOfGeospatialRelationsToGeosparqlFunctions.get(spatialTempRelation)
//									+ "_";
//						}
//						detectedPattern = "crcri_" + spatialRelation.substring(0, spatialRelation.lastIndexOf('_'));
//						templateFileName = detectedPattern + ".sparql";
//						logger.info("Generated File name: {} ", templateFileName);
//						geoSparqlQuery = QueryFactory.read("./src/main/resources/config/" + templateFileName)
//								.toString();
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poiURI", concepts.get(0).link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("poi1URI", concepts.get(1).link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("instanceURI", ent.uri);
//						logger.info("{} with parameters: {}", detectedPattern, parameters);
//						break;
//					default:
//						break;
//					}
//				}
//			}
//
//			if (entities.size() == 2) {
//				ent = entities.get(0);
//				if (concepts.size() == 0) {
//					if (geoSPATIALRelations.size() == 1 || geoSPATIALRelations.size() == 2) {
//						String spatialRelation = "";
//						spatialRelation += mappingOfGeospatialRelationsToGeosparqlFunctions
//								.get(geoSPATIALRelations.get(0)) + "_";
//						detectedPattern = "iri_" + spatialRelation.substring(0, spatialRelation.lastIndexOf('_'));
//
//						if (StringUtils.countMatches(spatialRelation, "_") < 3 && !thresholdDistance.equals("")) {
//							detectedPattern += "_th";
//						}
//						templateFileName = detectedPattern + ".sparql";
//						logger.info("Generated File name: {} ", templateFileName);
//						geoSparqlQuery = QueryFactory.read("./src/main/resources/config/" + templateFileName)
//								.toString();
//						// instance = ent.namedEntity;
//						// instanceURI = ent.uri;
//						// poi = myQuestion.substring(concept.begin,
//						// concept.end);
//						if (!thresholdDistance.equals("")) {
//							geoSparqlQuery = geoSparqlQuery.replaceAll("3000", thresholdDistance);
//						}
//						// geoSparqlQuery = geoSparqlQuery.replaceAll("poiURI",
//						// concept.link);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("instanceURI", ent.uri);
//						ent = entities.get(1);
//						// geoSparqlQuery =
//						// geoSparqlQuery.replaceAll("instance1",
//						// ent.namedEntity);
//						geoSparqlQuery = geoSparqlQuery.replaceAll("instance1URI", ent.uri);
//						// parameters.put("poiURI", concept.link);
//						// parameters.put("instanceURI", ent.uri);
//						logger.info("{} with parameters: {}", detectedPattern, parameters);
//						// geoSparqlQuery =
//						// bindVariablesInSparqlQuery(geoSparqlQuery,
//						// parameters);
//
//						// newGeoSparqlQuery += "FILTER().";
//
//						if (dbpediaPropertyFlag) {
//							String DBpediaTriples = "?poi owl:sameAs ?DBpediapoi. ";
//							int counter = 0;
//							List<String> listOfTriples = new ArrayList<String>();
//							if (dbpediaPropertyValueFlag) {
//								for (int i = 0; i < propertiesValue.size(); i++) {
//									listOfTriples.add("?DBpediapoi <" + properties.get(i) + "> \""
//											+ propertiesValue.get(i) + "\". ");
//									counter++;
//								}
//							}
//							for (int i = counter; i < properties.size(); i++) {
//								listOfTriples.add("?DBpediapoi <" + properties.get(i) + "> ?value"
//										+ properties.get(i).substring(properties.get(i).lastIndexOf("/") + 1) + ". ");
//								// geoSparqlQuery =
//								// geoSparqlQuery.replace("FILTER", ".
//								// ?DBpediapoi <"+property+">
//								// ?value"+property.substring(property.lastIndexOf("/")+1)+"
//								// FILTER");
//							}
//							for (String singleTriple : listOfTriples) {
//								DBpediaTriples += singleTriple;
//							}
//
//							// if(!thresholdDistance.equals("")){
//							// DBpediaTriples += "";
//							// }
//
//							geoSparqlQuery = geoSparqlQuery.replace("FILTER", ". " + DBpediaTriples + " FILTER");
//
//						}
//
//					}
//				}
//			}

			// BufferedWriter bw = new BufferedWriter(new
			// FileWriter("/home/dharmen/generatedGeoSparql.csv",true));
			// try{
			// bw.append(myQuestion+","+templateFileName.replace(".sparql", "
			// ")+","+geoSparqlQuery.replaceAll("\n", ""));
			// bw.newLine();
			// }catch(Exception e){
			// e.printStackTrace();
			// }
			// bw.close();

			logger.debug("store the generated GeoSPARQL query in triplestore: {}", finalQuery);
			// STEP 3: Push the GeoSPARQL query to the triplestore
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryUtils.getInGraph() + "> { " //
					+ " ?a a qa:AnnotationOfAnswerSPARQL . " //
					+ " ?a oa:hasTarget <URIAnswer> . " //
					+ " ?a oa:hasBody \"" + finalQuery.replaceAll("\n", " ") + "\" ;" //
					+ " oa:annotatedBy <urn:qanary:geosparqlgenerator> ; " //
					+ " oa:AnnotatedAt ?time . " //
					+ "}} " //
					+ "WHERE { " //
					+ " BIND (IRI(str(RAND())) AS ?a) ." //
					+ " BIND (now() as ?time) " //
					+ "}";
			myQanaryUtils.updateTripleStore(sparql);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return myQanaryMessage;
	}

	/**
	 * 
	 * 
	 * @param sparqlQuery
	 * @return
	 */
	public static String bindVariablesInSparqlQuery(String sparqlQuery, Map<String, String> variablesToBind) {

		String variableBindings = "";
		String uri;
		String name;

		// for each used variable create a bind statement
		for (Map.Entry<String, String> variable : variablesToBind.entrySet()) {
			name = variable.getKey();
			uri = variable.getValue();
			variableBindings += "\tBIND(<" + uri + "> AS ?" + name + ").\n";
		}

		// insert new bindings block at the end of the SPARQL query
		StringBuffer concreteSparqlQueryWithBindings = new StringBuffer(sparqlQuery);
		int position = concreteSparqlQueryWithBindings.lastIndexOf("}");
		concreteSparqlQueryWithBindings.insert(position, variableBindings);

		return concreteSparqlQueryWithBindings.toString();

	}

	class Concept {
		public int begin;
		public int end;
		public String link;
	}

	class Entity {

		public int begin;
		public int end;
		public String namedEntity;
		public String uri;

		public void print() {
			System.out.println("Start: " + begin + "\t End: " + end + "\t Entity: " + namedEntity);
		}
	}
}
