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
import edu.isi.bmkeg.skm.triage.cleartk.cr.filteredLineReader.UnfilteredLineReader;
import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;

public class SetUpClassificationExperiment {

	public static class Options extends Options_ImplBase {
		
		@Option(name = "-triageCorpus", required = true, usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus", required = true, usage = "The target corpus to be evaluated")
		public String targetCorpus = "";

		@Option(name = "-dir", required = true, usage = "Data Directory for Experiment")
		public File dir;
		
		@Option(name = "-prop", required = true, usage = "Proportion of documents to be held out")
		public float prop = 0.0f;

		@Option(name = "-baseData", required = true, usage = "Base Data Directory")
		public File baseData;

	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.parseOptions(args);

		long startTime = System.currentTimeMillis();

		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("uimaTypes.vpdmf-triage",
						"edu.isi.bmkeg.skm.cleartk.TypeSystem");
		
		Integer[] filterIds = new Integer[]{};
				
		String triageCorpusName = options.triageCorpus.replaceAll("\\s+", "_");
		String targetCorpusName = options.targetCorpus.replaceAll("\\s+", "_");

		triageCorpusName = triageCorpusName.replaceAll("\\/", "_");
		targetCorpusName = targetCorpusName.replaceAll("\\/", "_");

		File dataDir = new File(options.baseData.getPath() + 
				"/" + targetCorpusName + 
				"/" + triageCorpusName );
		
		CollectionReader cr = CollectionReaderFactory
				.createCollectionReader( UnfilteredLineReader.class, typeSystem, 
						UnfilteredLineReader.PARAM_FILE_OR_DIRECTORY_NAME, dataDir.getPath(),
						UnfilteredLineReader.PARAM_SUFFIXES, new String[]{".txt"},
						UnfilteredLineReader.PARAM_DELIMITER, "\t");
		
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
	    		EvaluationPreparer.PARAM_TOP_DIR_PATH, options.dir.getPath() + "/data"));
	    
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
