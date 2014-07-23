package edu.isi.bmkeg.skm.triage.cleartk.annotators;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.transform.extractor.TfidfExtractor;
import org.uimafit.util.JCasUtil;

import edu.isi.bmkeg.skm.cleartk.type.CatorgorizedFtdText;

public abstract class CategorizedFtdAnnotator extends CleartkAnnotator<Boolean> {

	static protected void scoredOutcomeToCategorizedFtdText(
			boolean outcome,
			Double score, 
			CatorgorizedFtdText document) {
		
		double inScore = 0;
		
		if (outcome) { // Outcome == true means "in"
			inScore = score;
		}

		String category = outcome ? "in" : "out";
		document.setCategory(category);
		document.setInScore( (float) inScore);

	}
	
	static protected Boolean categorizedFtdTextToOutcome(CatorgorizedFtdText document) {

		if ("in".equals(document.getCategory())) {
			return Boolean.TRUE;
		} else if ("out".equals(document.getCategory())) {
			return Boolean.FALSE;
		} else return null;

	}
	
	protected void createCategorizedFtdAnnotation(JCas jCas, List<Feature> features)
			throws CleartkProcessingException {
		
		//
		// this is where we need to run the transformation loop 
		// for this instance based on our training data tf-idf values
		//
		Boolean outcome = this.classifier.classify(features);
		Map<Boolean, Double> score = this.classifier.score(features);
		CatorgorizedFtdText document = new CatorgorizedFtdText(jCas, 0,
				jCas.getDocumentText().length());
	
		scoredOutcomeToCategorizedFtdText(outcome, score.get(true), document);
		
		document.addToIndexes();

	}

	protected void writeInstance(JCas jCas, List<Feature> features)
			throws CleartkProcessingException {
		
		Instance<Boolean> instance = new Instance<Boolean>(features);

		writeInstance(jCas, instance);

	}

	protected void writeInstance(JCas jCas, Instance<Boolean> instance)
			throws CleartkProcessingException {

		Collection<CatorgorizedFtdText> documents = JCasUtil.select(jCas,
				CatorgorizedFtdText.class);

		if (documents.size() == 1) {
			CatorgorizedFtdText document = documents.iterator().next();

			Boolean outcome = categorizedFtdTextToOutcome(document);

			if (outcome != null) {

				instance.setOutcome(outcome);
				this.dataWriter.write(instance);

			}
		}
	}
	
}
