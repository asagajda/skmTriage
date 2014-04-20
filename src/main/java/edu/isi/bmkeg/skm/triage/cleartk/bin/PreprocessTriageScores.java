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

import edu.isi.bmkeg.skm.triage.cleartk.annotators.EvaluationPreparer;
import edu.isi.bmkeg.skm.triage.cleartk.cr.TriageScoreCollectionReader;
import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;

public class PreprocessTriageScores {

	public static String USAGE = "-corpus <corpusName> -dir <dir> -prop <propHeldOut> -l <login> -p <password> -db <dbName>";
	
	public static class Options extends Options_ImplBase {
		@Option(name = "-triageCorpus", usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus", usage = "The target corpus to be evaluated")
		public String targetCorpus = "";

		@Option(name = "-dir", usage = "Target directory")
		public File dir;
		
		@Option(name = "-prop", usage = "Proportion of documents to be held out")
		public float prop = 0.0f;
		
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

		if (options.targetCorpus.length() == 0 
				|| options.triageCorpus.length() == 0 
				|| !options.dir.exists() 
				|| options.login.length() == 0
				|| options.password.length() == 0
				|| options.dbName.length() == 0) {
			System.err.print(USAGE);
			System.exit(-1);
		}

		long startTime = System.currentTimeMillis();

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
	    builder.add(AnalysisEngineFactory.createPrimitiveDescription(
	    		EvaluationPreparer.class,
	    		EvaluationPreparer.PARAM_TRIAGE_CORPUS_NAME, options.triageCorpus,
	    		EvaluationPreparer.PARAM_TARGET_CORPUS_NAME, options.targetCorpus,
	    		EvaluationPreparer.PARAM_P_HOLDOUT, options.prop,
	    		EvaluationPreparer.PARAM_TOP_DIR_PATH, options.dir.getPath()));
	    
	    // ///////////////////////////////////////////
	    // Run pipeline to create training data file
	    // ///////////////////////////////////////////
	    SimplePipeline.runPipeline(cr, builder.createAggregateDescription());

	}

}
