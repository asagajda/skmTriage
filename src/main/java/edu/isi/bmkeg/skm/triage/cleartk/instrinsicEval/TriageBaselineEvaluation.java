
package edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval;

import java.io.File;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.cleartk.eval.AnnotationStatistics;
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
 * assigning every document to the OUT category.
 * <p>
 */
public class TriageBaselineEvaluation extends CrossValidationEvaluation {

	private static Logger logger = Logger.getLogger(TriageBaselineEvaluation.class);
	
	public static void main(String[] args) throws Exception {
		CrossValEvalOptions options = new CrossValEvalOptions();
		options.parseOptions(args);
		
		TriageBaselineEvaluation eval = new TriageBaselineEvaluation(
				options.baseDir,
				options.nFolds,
				options.dataDirectory);

		eval.runMain();

	}
	
	public TriageBaselineEvaluation(File baseDirectory,
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
			JCas systemView = jCas.getView(TriageBaselineEvaluation.SYSTEM_VIEW_NAME);

			// Creates system category as "out"
			CatorgorizedFtdText document = new CatorgorizedFtdText(jCas, 0,
					jCas.getDocumentText().length());
			document.setCategory("out");
			document.addToIndexes();

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