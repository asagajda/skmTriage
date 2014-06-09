package edu.isi.bmkeg.skm.triage.cleartk.bin;

import java.io.File;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.cleartk.syntax.opennlp.SentenceAnnotator;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.kohsuke.args4j.Option;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.pipeline.SimplePipeline;

import edu.isi.bmkeg.skm.triage.cleartk.annotators.SimpleOneLinePerDocWriter;
import edu.isi.bmkeg.skm.triage.cleartk.cr.TriageScoreCollectionReader;
import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;

public class PreprocessTriageCorpus {
	
	public static class Options extends Options_ImplBase {
		@Option(name = "-triageCorpus", usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus", usage = "The target corpus to be evaluated")
		public String targetCorpus = "";

		@Option(name = "-dir", usage = "Target directory")
		public File dir;

		@Option(name = "-l", usage = "Database login")
		public String login = "";

		@Option(name = "-p", usage = "Database password")
		public String password = "";

		@Option(name = "-db", usage = "Database name")
		public String dbName = "";
		
		@Option(name = "-wd", usage = "Working Directory")
		public File workingDirectory;

	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.parseOptions(args);

		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("uimaTypes.vpdmf-triage",
						"edu.isi.bmkeg.skm.cleartk.TypeSystem");
		
		CollectionReader cr = CollectionReaderFactory.createCollectionReader(
				TriageScoreCollectionReader.class, typeSystem, 
				TriageScoreCollectionReader.TRIAGE_CORPUS_NAME, options.triageCorpus,
				TriageScoreCollectionReader.TARGET_CORPUS_NAME, options.targetCorpus,
				TriageScoreCollectionReader.LOGIN, options.login, 
				TriageScoreCollectionReader.PASSWORD, options.password, 
				TriageScoreCollectionReader.DB_URL, options.dbName,
				TriageScoreCollectionReader.WORKING_DIRECTORY, options.workingDirectory,
				TriageScoreCollectionReader.SKIP_UNKNOWNS, true);		

	    AggregateBuilder builder = new AggregateBuilder();

	    builder.add(SentenceAnnotator.getDescription()); // Sentence segmentation
	    builder.add(TokenAnnotator.getDescription()); // Tokenization

//		It would be better to write into the preprocessed instances the whole token 
//		(skip stemming for now) and do Stemming while processing instances if desired. 
//      So the instance processors can have both features, tokens and stems [MT].
//
//	    builder.add(DefaultSnowballStemmer.getDescription("English")); // Stemming

	    // The simple document classification annotator
		String triageCorpusName = options.triageCorpus;
		String targetCorpusName = options.targetCorpus;
		
		triageCorpusName = triageCorpusName.replaceAll("\\s+", "_");
		triageCorpusName = triageCorpusName.replaceAll("\\/", "_");
		
		targetCorpusName = targetCorpusName.replaceAll("\\s+", "_");
		targetCorpusName = targetCorpusName.replaceAll("\\/", "_");

	    
	    String dirPath = options.dir.getPath()  + "/baseDir/" 
	    			+ targetCorpusName + "/" 
	    			+ triageCorpusName;
	    builder.add(AnalysisEngineFactory.createPrimitiveDescription(
	    		SimpleOneLinePerDocWriter.class,
	    		SimpleOneLinePerDocWriter.PARAM_DIR_PATH, dirPath));
	    
	    // ///////////////////////////////////////////
	    // Run pipeline to create training data file
	    // ///////////////////////////////////////////
	    try {
		    SimplePipeline.runPipeline(cr, builder.createAggregateDescription());	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }

	}

}
