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

public class SimpleOneLinePerDocWriter extends JCasAnnotator_ImplBase {

	public final static String PARAM_DIR_PATH = ConfigurationParameterFactory
			.createConfigurationParameterName( SimpleOneLinePerDocWriter.class, "dirPath" );
	@ConfigurationParameter(mandatory = true, description = "The place to put the document files to be classified.")
	String dirPath;
	
	private File baseData;
	
	public void initialize(UimaContext context)
			throws ResourceInitializationException {

		super.initialize(context);
		
		this.dirPath = (String) context.getConfigParameterValue(PARAM_DIR_PATH);
		this.baseData = new File(this.dirPath);
		
		// set up the file system.
		//
		// corpus
		//  |
		//  +-- categoryLabel_<train|test|eval>.txt
		//
		//  each category contains one line per document with line breaks stripped away
		//
		try {
						
			if( baseData.exists() ) {
				Map<String, File> filesToZip = Converters.recursivelyListFiles(this.baseData);
				try {
					SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy-HHmm");
					String suffix = df.format(new Date());
					File targetZip = new File(this.baseData.getParentFile().getParentFile().getName()
							+ "_" + this.baseData.getParentFile().getName() 
							+ "_" + this.baseData.getName() 
							+ "_" + suffix + ".zip");
					Converters.zipIt(filesToZip, targetZip);					
				} catch (Exception e) {}
				Converters.recursivelyDeleteFiles(this.baseData);
			}
			this.baseData.mkdirs();
			
		} catch (Exception e) {
			
			throw new ResourceInitializationException(e);
			
		}
		
	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		TriageScore doc = JCasUtil.selectSingle(jCas, TriageScore.class);
		String code = doc.getInOutCode();
		
		File outFile = new File(baseData.getPath() + "/" + code + ".txt" ); 
		
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
