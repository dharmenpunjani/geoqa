package eu.wdaqua.qanary.geospatialsearch;

import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test class to check the correct behavior of the {@link RelationDetector}
 * 
 * @author AnBo
 *
 */
public class RelationDetectorTest {

	/**
	 * positive tests of geospatial relation detector
	 * @throws URISyntaxException 
	 */
	@Ignore 
	@Test
	public void testRecognitionOfGeospatialRelations() throws URISyntaxException {
		// map of test data and expected processor's output
		Map<String, RelationDetectorAnswer> testStrings = new HashMap<>();

		// TODO: replace strings of relation with enum entries
		// test questions need to pass the test
		testStrings.put("Museum in Athens", new RelationDetectorAnswer(true, GeospatialRelation.IN,1));
		testStrings.put("Hotels near by Barcelona", new RelationDetectorAnswer(true, GeospatialRelation.NEAR_BY,1));
		testStrings.put("Hotels on the outskirts of Barcelona", new RelationDetectorAnswer(true, GeospatialRelation.AT_THE_BORDER_OF,3));
		testStrings.put("Hotels at the border of Barcelona", new RelationDetectorAnswer(true, GeospatialRelation.AT_THE_BORDER_OF,3));

		RelationDetector myRelationDetector = new RelationDetector();

		for (Map.Entry<String, RelationDetectorAnswer> entry : testStrings.entrySet()) {
			String question = entry.getKey();
			RelationDetectorAnswer check = entry.getValue();
			
			assertTrue("check question: " + question, check.equals(myRelationDetector.process(question)));
		}
	}

	/**
	 * negative tests of geospatial relation detector
	 * @throws URISyntaxException 
	 */
	@Test
	public void testNoExistingGeospatialRelations() throws URISyntaxException {
		// map of test data and expected processor's output
		List<String> testStrings = new LinkedList<>();

		// test questions need NOT to pass the test
		testStrings.add("Museum far away Athens");
		testStrings.add("Hotels Barcelona");

		RelationDetector myRelationDetector = new RelationDetector();

		for (String question : testStrings) {			
			assertTrue("check question: " + question, null == myRelationDetector.process(question));
		}
	}

}
