
package edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.cleartk.token.type.Token;
import org.uimafit.component.ViewTextCopierAnnotator;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Function;

import edu.isi.bmkeg.skm.cleartk.type.CatorgorizedFtdText;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.GoldDocumentCategoryAnnotator;

/**
 * <p>
 * This evaluation class computes a Triage baseline evaluation by
 * assigning into the IN category every document that contains any 
 * of the desired search terms. 
 * <p>
 */
public class SearchTermsBaselineEvaluation extends CrossValidationEvaluation {

	private static Logger logger = Logger.getLogger(SearchTermsBaselineEvaluation.class);
	
	public String[] searchTerms = new String[] {"mouse", "mice", "murine"};
	
	public static void main(String[] args) throws Exception {
		CrossValEvalOptions options = new CrossValEvalOptions();
		options.parseOptions(args);
		
		SearchTermsBaselineEvaluation eval = new SearchTermsBaselineEvaluation(
				options.baseDir,
				options.nFolds,
				options.dataDirectory);

		eval.runMain();

	}
	
	public SearchTermsBaselineEvaluation(File baseDirectory,
			int nFolds,
			File dataDir) {

		super(baseDirectory, dataDir);
		this.nFolds = nFolds;

	}

	@Override
	public void train(CollectionReader collectionReader, File outputDirectory)
			throws Exception {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		logger.info("Skipping training...");
	}

	@Override
	protected AnnotationStatistics<String> test(
			CollectionReader collectionReader, File directory) throws Exception {
		AnnotationStatistics<String> stats = new AnnotationStatistics<String>();
		
		// Create the document classification pipeline
		AggregateBuilder builder = new AggregateBuilder();
		
		// NLP pre-processing components
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());

		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				ViewTextCopierAnnotator.class,
				ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME,
				CAS.NAME_DEFAULT_SOFA,
				ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME,
				GOLD_VIEW_NAME));

		builder.add(
				AnalysisEngineFactory
						.createPrimitiveDescription(GoldDocumentCategoryAnnotator.class),
				CAS.NAME_DEFAULT_SOFA, GOLD_VIEW_NAME);
				
		AnalysisEngine engine = builder.createAggregate();

		// Run and evaluate
		Function<CatorgorizedFtdText, ?> getSpan = AnnotationStatistics
				.annotationToSpan();
		Function<CatorgorizedFtdText, String> getCategory = AnnotationStatistics
				.annotationToFeatureValue("category");
		
		for (JCas jCas : new JCasIterable(collectionReader, engine)) {
			JCas goldView = jCas.getView(GOLD_VIEW_NAME);
			JCas systemView = jCas.getView(SearchTermsBaselineEvaluation.SYSTEM_VIEW_NAME);

			// Creates system category.
			CatorgorizedFtdText document = new CatorgorizedFtdText(jCas, 0,
					jCas.getDocumentText().length());
			document.addToIndexes();
			boolean found = false;
			for (String token : JCasUtil.toText(JCasUtil.selectCovered(Token.class, document))) {
				if (Arrays.asList(searchTerms).contains(token.toLowerCase())) {
					found  = true;
					break;
				}
			}
			document.setCategory((found) ? "in" : "out");

			// Get results from system and gold views, and update results
			// accordingly
			Collection<CatorgorizedFtdText> goldCategories = JCasUtil.select(
					goldView, CatorgorizedFtdText.class);
			Collection<CatorgorizedFtdText> systemCategories = JCasUtil.select(
					systemView, CatorgorizedFtdText.class);

			stats.add(goldCategories, systemCategories, getSpan, getCategory);
		}

		return stats;
	}

}