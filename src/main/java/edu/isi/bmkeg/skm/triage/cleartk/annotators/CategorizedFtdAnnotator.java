package edu.isi.bmkeg.skm.triage.cleartk.annotators;

import java.util.Collection;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.CleartkAnnotator;
import org.cleartk.classifier.CleartkProcessingException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.ScoredOutcome;
import org.uimafit.util.JCasUtil;

import edu.isi.bmkeg.skm.cleartk.type.CatorgorizedFtdText;
import edu.isi.bmkeg.triage.uimaTypes.TriageScore;

public abstract class CategorizedFtdAnnotator extends CleartkAnnotator<Boolean> {

	static protected void scoredOutcomeToCategorizedFtdText(
			boolean outcome,
			List<ScoredOutcome<Boolean>> scores, 
			CatorgorizedFtdText document) {
		
		double inScore = 0;
		
		for (ScoredOutcome<Boolean> so : scores) {
			double score = 1 - so.getScore();

			if (outcome) { // Outcome == true means "in"
				inScore = score;
				break;
			}
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
		
		boolean outcome = this.classifier.classify(features).booleanValue();
		List<ScoredOutcome<Boolean>> scores = this.classifier.score(features, 2);
		CatorgorizedFtdText document = new CatorgorizedFtdText(jCas, 0,
				jCas.getDocumentText().length());
		
		scoredOutcomeToCategorizedFtdText(outcome, scores, document);

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
