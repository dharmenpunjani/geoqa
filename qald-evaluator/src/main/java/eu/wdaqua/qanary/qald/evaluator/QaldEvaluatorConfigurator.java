package eu.wdaqua.qanary.qald.evaluator;

import java.util.List;

/**
 * holds the configuration of the evaluator
 * 		
 * @author AnBo
 *
 */
public class QaldEvaluatorConfigurator {

	/**
	 * the number of questions that have to be checked per gold standard file
	 */
	private final Integer maxQuestionsToEvaluate;

	/**
	 * the component configurations (components comma separated in a string) as
	 * list (each configuration separated by comma) e.g.,
	 * "agdistis,dbpedia-spotlight", "alchemy,dbpedia-spotlight" (in
	 * application.properties or similar)
	 */
	private final List<String> componentConfigurationsForComparison;

	/**
	 * endpoint providing access to a Qanary question answering system
	 */
	private final String qaSystemEndpoint;

	/**
	 * uri of QALD file used to get executable questions
	 */
	private final String pathToQaldFile;
	
	public QaldEvaluatorConfigurator(Integer maxQuestionsToEvaluate, List<String> componentConfigurationsForComparison,
			String qaSystemEndpoint, String pathToQaldFile) {
			
		this.maxQuestionsToEvaluate = maxQuestionsToEvaluate;
		this.componentConfigurationsForComparison = componentConfigurationsForComparison;
		this.qaSystemEndpoint = qaSystemEndpoint;
		this.pathToQaldFile = pathToQaldFile;
	}

	public int getMaxQuestionsToEvaluate() {
		return this.maxQuestionsToEvaluate;
	}

	public List<String> getComponentConfigurationsForComparison() {
		return this.componentConfigurationsForComparison;
	}

	// TODO: change to URI
	public String getQaSystemEndpoint() {
		return this.qaSystemEndpoint;
	}

	// TODO: change to URI
	public String getPathToQaldFile() {
		return this.pathToQaldFile;
	}
}
