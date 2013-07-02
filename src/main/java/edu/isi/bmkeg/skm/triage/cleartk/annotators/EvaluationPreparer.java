package edu.isi.bmkeg.skm.triage.cleartk.annotators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.util.JCasUtil;

import edu.isi.bmkeg.skm.triage.model.TriageCode;
import edu.isi.bmkeg.triage.uimaTypes.TriageScore;
import edu.isi.bmkeg.utils.Converters;

public class EvaluationPreparer extends JCasAnnotator_ImplBase {

	public final static String PARAM_TOP_DIR_PATH = ConfigurationParameterFactory
			.createConfigurationParameterName( EvaluationPreparer.class, "topDirPath" );
	@ConfigurationParameter(mandatory = true, description = "The place to put the document files to be classified.")
	String topDirPath;

	public final static String PARAM_TRIAGE_CORPUS_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName( EvaluationPreparer.class, "triageCorpusName");
	@ConfigurationParameter(mandatory = true, description = "The name of the triage corpus being used")
	private String triageCorpusName;

	public final static String PARAM_TARGET_CORPUS_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName( EvaluationPreparer.class, "targetCorpusName");
	@ConfigurationParameter(mandatory = true, description = "The name of the target corpus being used")
	private String targetCorpusName;

	public final static String PARAM_P_HOLDOUT = ConfigurationParameterFactory
			.createConfigurationParameterName( EvaluationPreparer.class, "pHoldout");
	@ConfigurationParameter(mandatory = false, description = "The proportion of held out documents")
	private Float pHoldout = 0.05f;
	
	private File topDir;
	private File corpusDir;
	private File test;
	private File train;
	
	public void initialize(UimaContext context)
			throws ResourceInitializationException {

		super.initialize(context);
		
		this.topDirPath = (String) context.getConfigParameterValue(PARAM_TOP_DIR_PATH);
		this.triageCorpusName = (String) context.getConfigParameterValue(PARAM_TRIAGE_CORPUS_NAME);
		this.targetCorpusName = (String) context.getConfigParameterValue(PARAM_TARGET_CORPUS_NAME);
		this.pHoldout = (Float) context.getConfigParameterValue(PARAM_P_HOLDOUT);
		
		triageCorpusName = triageCorpusName.replaceAll("\\s+", "_");
		targetCorpusName = targetCorpusName.replaceAll("\\s+", "_");
		
		// set up the file system.
		//
		// corpus
		//  |
		//  +-- categoryLabel_<train|test|eval>.txt
		//
		//  each category contains one line per document with line breaks stripped away
		//
		try {
			
			this.topDir = new File(this.topDirPath);
			
			this.corpusDir = new File(this.topDirPath + "/" + targetCorpusName + "/" + triageCorpusName);
			if( corpusDir.exists() ) {
				Map<String, File> filesToZip = Converters.recursivelyListFiles(this.corpusDir);
				try {
					SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy-HHmm");
					String suffix = df.format(new Date());
					File targetZip = new File(this.topDirPath + "/" + 
							targetCorpusName + "_" +
							triageCorpusName + "_" + suffix + ".zip");
					Converters.zipIt(filesToZip, targetZip);					
				} catch (Exception e) {}
				Converters.recursivelyDeleteFiles(this.corpusDir);
			}
			this.corpusDir.mkdirs();
			
			test = new File(this.corpusDir.getPath() + "/test" );
			test.mkdir();

			train = new File(this.corpusDir.getPath() + "/train" );
			train.mkdir();
			
			
		} catch (Exception e) {
			
			throw new ResourceInitializationException(e);
			
		}
		
	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		TriageScore doc = JCasUtil.selectSingle(jCas, TriageScore.class);
		String code = doc.getInOutCode();
		
//		If code is not "in" or "out" then return
		if( ! TriageCode.IN.equals(code) && ! TriageCode.OUT.equals(code) ) 
			return; 
		
		double p = Math.random();
		boolean holdOut = false;
		if( p < this.pHoldout ) {
			holdOut = true;
		}
		
		File outFile = (holdOut) ? new File(test.getPath() + "/" + code + ".txt" ) 
				: new File(train.getPath() + "/" + code + ".txt" ); 
		
		try {
			
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile, true)));

			out.print(doc.getVpdmfId() + "	");

			for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
				List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, sentence);	
				if (tokens.size() <= 0) { continue; }
				
				List<String> tokenStrings = JCasUtil.toText(tokens);						
				for (int i = 0; i < tokens.size(); i++) {
					out.print(tokenStrings.get(i) + " ");
				}

			}

			out.print("\n");
			out.close();
		
		} catch (IOException e) {
			
			throw new AnalysisEngineProcessException(e);
			 
		}

	}

}
