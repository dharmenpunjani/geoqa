package eu.wdaqua.qanary.component.geospatialsearch;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.util.Assert;

public class TestGeoSparqlGenerator {

	@Test
	public void bindVariablesInSparqlQuery(){
		String templateQuery = "{}";
		Map<String,String> variablesToBind = new HashMap<>();
		variablesToBind.put("x", "urn:test");
		String generatedQuery = GeoSparqlGenerator.bindVariablesInSparqlQuery(templateQuery, variablesToBind);
		String correctQuery = "{BIND(<urn:test> AS ?x).}";
		
		
		correctQuery = cleanStringFromWhitespaces(correctQuery);
		generatedQuery = cleanStringFromWhitespaces(generatedQuery);
		
		Assert.state(generatedQuery.toLowerCase().compareTo(correctQuery.toLowerCase()) == 0);
	}
	
	private String cleanStringFromWhitespaces(String in){
		return in.replace("\n", " ").replace("\t", " ").replaceAll("\\s", "");
	}
	
}
