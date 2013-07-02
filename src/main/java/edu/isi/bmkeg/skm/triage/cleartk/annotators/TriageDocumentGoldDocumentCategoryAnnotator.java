package edu.isi.bmkeg.skm.triage.cleartk.annotators;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.util.JCasUtil;

import edu.isi.bmkeg.skm.cleartk.type.CatorgorizedFtdText;
import edu.isi.bmkeg.skm.triage.model.TriageCode;
import edu.isi.bmkeg.triage.uimaTypes.TriageScore;

/**
 * <br>
 * This class will assign the gold-standard document categories for the documents 
 * looking at TriageDocument type.
 * 
 */
public class TriageDocumentGoldDocumentCategoryAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
	  TriageScore doc = JCasUtil.selectSingle(jCas, TriageScore.class);
	String code = doc.getInOutCode();

//	If code is not "in" or "out" then return
	if( ! TriageCode.IN.equals(code) && ! TriageCode.OUT.equals(code) ) 
		return; 
		
      // Create UsenetDocument annotation
      // The document category will come from the document directory structure
      CatorgorizedFtdText document = 
    		  new CatorgorizedFtdText(jCas, 0, jCas.getDocumentText().length());

      document.setCategory(code);
      document.addToIndexes();
	      
  }

}
