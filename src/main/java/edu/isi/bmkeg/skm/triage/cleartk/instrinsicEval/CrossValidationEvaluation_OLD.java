/** 
 * Copyright (c) 2007-2012, Regents of the University of Colorado 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.cleartk.classifier.CleartkAnnotator;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.feature.transform.InstanceDataWriter;
import org.cleartk.classifier.feature.transform.InstanceStream;
import org.cleartk.classifier.feature.transform.extractor.CentroidTfidfSimilarityExtractor;
import org.cleartk.classifier.feature.transform.extractor.MinMaxNormalizationExtractor;
import org.cleartk.classifier.feature.transform.extractor.TfidfExtractor;
import org.cleartk.classifier.feature.transform.extractor.ZeroMeanUnitStddevExtractor;
import org.cleartk.classifier.jar.DefaultDataWriterFactory;
import org.cleartk.classifier.jar.DirectoryDataWriterFactory;
import org.cleartk.classifier.jar.GenericJarClassifierFactory;
import org.cleartk.classifier.jar.JarClassifierBuilder;
import org.cleartk.classifier.libsvm.LIBSVMStringOutcomeDataWriter;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.eval.Evaluation_ImplBase;
import org.cleartk.syntax.opennlp.SentenceAnnotator;
import org.cleartk.token.stem.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.Option;
import org.uimafit.component.ViewTextCopierAnnotator;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.testing.util.HideOutput;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Function;

import edu.isi.bmkeg.lapdf.extraction.Extractor;
import edu.isi.bmkeg.skm.cleartk.type.CatorgorizedFtdText;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.GoldDocumentCategoryAnnotator;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.TfIdfCentroidAnnotator;
import edu.isi.bmkeg.skm.triage.cleartk.cr.filteredLineReader.FilteredLineReader;

/**
 * <br>
 * Copyright (c) 2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * <p>
 * This evaluation class provides a concrete example of how to train and
 * evaluate classifiers. Specifically this class will train a document
 * categorizer using a subset of the 20 newsgroups dataset. It evaluates
 * performance using 2-fold cross validation as well as a holdout set.
 * <p>
 * 
 * Key points: <br>
 * <ul>
 * <li>Creating training and evaluation pipelines
 * <li>Example of feature transformation / normalization
 * </ul>
 * 
 * 
 * @author Lee Becker
 */
public class CrossValidationEvaluation_OLD extends
		Evaluation_ImplBase<Integer, AnnotationStatistics<String>> {

	private static Logger logger = Logger.getLogger(CrossValidationEvaluation_OLD.class);
	
	public static class Options extends Options_ImplBase {
		
		@Option(name = "-base", usage = "Specify the base directory. Training " +
				"documents will be found in <base>/train and testing documents will " +
				"be found in <base>/test")
		public File baseDir;
		
		@Option(name = "-nFolds", usage = "Number of folds to use in the evaluation")
		public int nFolds = 1;
		
		@Option(name = "-models", usage = "specify the directory in which to write out the trained model files")
		public File modelsDirectory = new File(
				"target/document_classification/models");

		@Option(name = "-training-args", usage = "specify training arguments to be passed to the learner.  For multiple values specify -ta for each - e.g. '-ta -t -ta 0'")
		public List<String> trainingArguments = Arrays.asList("-t", "0");
	}
	
	private List<Integer> trainIds;
	private List<Integer> testIds;
	
	private AnnotatorMode mode;
	
	public static enum AnnotatorMode {
		TRAIN, TEST, CLASSIFY
	}

	public static List<File> getFilesFromDirectory(File directory) {
		
		IOFileFilter fileFilter = FileFilterUtils
				.makeSVNAware(HiddenFileFilter.VISIBLE);
		IOFileFilter dirFilter = FileFilterUtils.makeSVNAware(FileFilterUtils.andFileFilter(
									  FileFilterUtils.directoryFileFilter(),
									  HiddenFileFilter.VISIBLE));
									 
		return new ArrayList<File>(FileUtils.listFiles(directory, fileFilter,
				dirFilter));
	}

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.parseOptions(args);
		
		CrossValidationEvaluation_OLD eval = new CrossValidationEvaluation_OLD(
				options.baseDir,
				options.trainingArguments);

		File trainingDir = new File (eval.baseDirectory.getPath() + "/data/train");
		File testingDir = new File (eval.baseDirectory.getPath() + "/data/test");

		List<File> trainFiles = getFilesFromDirectory(trainingDir);
		List<File> testFiles = getFilesFromDirectory(testingDir);

		eval.trainIds = eval.loadIdsFromFiles(trainFiles);
		eval.testIds = eval.loadIdsFromFiles(testFiles);

		//
		// Runs the cross validation 
		//
		eval.mode = AnnotatorMode.TRAIN;
		List<AnnotationStatistics<String>> foldStats = eval
				.crossValidation(eval.trainIds, options.nFolds);
		AnnotationStatistics<String> crossValidationStats = AnnotationStatistics
				.addAll(foldStats);

		System.err.println("Cross Validation Results:");
		System.err.print(crossValidationStats);
		System.err.println();
		System.err.println(crossValidationStats.confusions());
		System.err.println();

		// Run Holdout Set
		eval.mode = AnnotatorMode.TEST;
		AnnotationStatistics<String> holdoutStats = eval.trainAndTest(
				eval.trainIds, eval.testIds);
		System.err.println("Holdout Set Results:");
		System.err.print(holdoutStats);
		System.err.println();
		System.err.println(holdoutStats.confusions());
	}

	public List<Integer> loadIdsFromFiles(List<File> files)
			throws FileNotFoundException, IOException {
		
		Pattern patt = Pattern.compile("(\\d+)\\t(.*)$");
		List<Integer> ids = new ArrayList<Integer>();
		for( File file : files) {
			
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				Matcher m = patt.matcher(line);
				if( m.find() ) {
					Integer id = new Integer(m.group(1));
					ids.add(id);
				}
			}
			br.close();
			
		}
		
		return ids;
		
	}

	public static final String GOLD_VIEW_NAME = "DocumentClassificationGoldView";

	public static final String SYSTEM_VIEW_NAME = CAS.NAME_DEFAULT_SOFA;

	private List<String> trainingArguments;

	public CrossValidationEvaluation_OLD(File baseDirectory) {
		super(baseDirectory);
		this.trainingArguments = Arrays.<String> asList();
	}

	public CrossValidationEvaluation_OLD(File baseDirectory, 
			List<String> trainingArguments) {

		super(baseDirectory);
		this.trainingArguments = trainingArguments;
		
	}
	
	@Override
	protected CollectionReader getCollectionReader(List<Integer> items)
			throws Exception {
		
		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("edu.isi.bmkeg.skm.cleartk.TypeSystem");
		
		Integer[] filterIds = items.toArray(new Integer[]{});
		
		String dataPath = this.baseDirectory.getPath() + "/data";
		
		FilteredLineReader lr = (FilteredLineReader) CollectionReaderFactory
				.createCollectionReader( FilteredLineReader.class, typeSystem, 
						FilteredLineReader.PARAM_FILE_OR_DIRECTORY_NAME, dataPath,
						FilteredLineReader.PARAM_SUFFIXES, new String[]{".txt"},
						FilteredLineReader.PARAM_FILTER_IDS, filterIds,
						FilteredLineReader.PARAM_DELIMITER, "\t"
			    );
			
		return lr;
		
	}


	@Override
	public void train(CollectionReader collectionReader, File outputDirectory)
			throws Exception {
		
		// ////////////////////////////////////////////////////////////////////////////////
		// Step 1: Extract features and serialize the raw instance objects
		// Note: DocumentClassificationAnnotator sets the various extractor URI
		// values to null by
		// default. This signals to the feature extractors that they are being
		// written out for training
		// ////////////////////////////////////////////////////////////////////////////////
		logger.info("1. Extracting features and writing raw instances data");

		// Create and run the document classification training pipeline
		AggregateBuilder builder = CrossValidationEvaluation_OLD
				.createDocumentClassificationAggregate(outputDirectory,
						AnnotatorMode.TRAIN);
		SimplePipeline.runPipeline(collectionReader,
				builder.createAggregateDescription());

		// Load the serialized instance data
		Iterable<Instance<String>> instances = InstanceStream
				.loadFromDirectory(outputDirectory);

		// ////////////////////////////////////////////////////////////////////////////////
		// Step 2: Transform features and write training data
		// In this phase, the normalization statistics are computed and the raw
		// features are transformed into normalized features.
		// Then the adjusted values are written with a DataWriter (libsvm in
		// this case)
		// for training
		// ////////////////////////////////////////////////////////////////////////////////
		logger.info("2. Collection feature normalization statistics");		
		
		// Collect TF*IDF stats for computing tf*idf values on extracted tokens
		URI tfIdfDataURI = TfIdfCentroidAnnotator
				.createTokenTfIdfDataURI(outputDirectory);
		TfidfExtractor<String> extractor = new TfidfExtractor<String>(
				TfIdfCentroidAnnotator.TFIDF_EXTRACTOR_KEY);
		extractor.train(instances);
		extractor.save(tfIdfDataURI);

		// Collect TF*IDF Centroid stats for computing similarity to corpus
		// centroid
		URI tfIdfCentroidSimDataURI = TfIdfCentroidAnnotator
				.createIdfCentroidSimilarityDataURI(outputDirectory);
		CentroidTfidfSimilarityExtractor<String> simExtractor = new CentroidTfidfSimilarityExtractor<String>(
				TfIdfCentroidAnnotator.CENTROID_TFIDF_SIM_EXTRACTOR_KEY);
		simExtractor.train(instances);
		simExtractor.save(tfIdfCentroidSimDataURI);

		// Collect ZMUS stats for feature normalization
		URI zmusDataURI = TfIdfCentroidAnnotator
				.createZmusDataURI(outputDirectory);
		ZeroMeanUnitStddevExtractor<String> zmusExtractor = new ZeroMeanUnitStddevExtractor<String>(
				TfIdfCentroidAnnotator.ZMUS_EXTRACTOR_KEY);
		zmusExtractor.train(instances);
		zmusExtractor.save(zmusDataURI);

		// Collect MinMax stats for feature normalization
		URI minmaxDataURI = TfIdfCentroidAnnotator
				.createMinMaxDataURI(outputDirectory);
		MinMaxNormalizationExtractor<String> minmaxExtractor = new MinMaxNormalizationExtractor<String>(
				TfIdfCentroidAnnotator.MINMAX_EXTRACTOR_KEY);
		minmaxExtractor.train(instances);
		minmaxExtractor.save(minmaxDataURI);

		// Rerun training data writer pipeline, to transform the extracted
		// instances -- an alternative,
		// more costly approach would be to reinitialize the
		// DocumentClassificationAnnotator above with
		// the URIs for the feature
		// extractor.
		//
		// In this example, we now write in the libsvm format
		logger.info("3. Write out model training data");
		LIBSVMStringOutcomeDataWriter dataWriter = new LIBSVMStringOutcomeDataWriter(
				outputDirectory);
		for (Instance<String> instance : instances) {
			instance = extractor.transform(instance);
			instance = simExtractor.transform(instance);
			instance = zmusExtractor.transform(instance);
			instance = minmaxExtractor.transform(instance);
			dataWriter.write(instance);
		}
		dataWriter.finish();

		// //////////////////////////////////////////////////////////////////////////////
		// Stage 3: Train and write model
		// Now that the features have been extracted and normalized, we can
		// proceed
		// in running machine learning to train and package a model
		// //////////////////////////////////////////////////////////////////////////////
		logger.info("4. Train model and write model.jar file.\n");
		HideOutput hider = new HideOutput();
		JarClassifierBuilder.trainAndPackage(outputDirectory,
				this.trainingArguments
						.toArray(new String[this.trainingArguments.size()]));
		hider.restoreOutput();
	}

	/**
	 * Creates the preprocessing pipeline needed for document classification.
	 * Specifically this consists of:
	 * <ul>
	 * <li>Populating the default view with the document text (as specified in
	 * the URIView)
	 * <li>Sentence segmentation
	 * <li>Tokenization
	 * <li>Stemming
	 * <li>[optional] labeling the document with gold-standard document
	 * categories
	 * </ul>
	 */
	public static AggregateBuilder createPreprocessingAggregate(
			File modelDirectory, AnnotatorMode mode)
			throws ResourceInitializationException {
		
		AggregateBuilder builder = new AggregateBuilder();
		
		// NLP pre-processing components
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());
		builder.add(DefaultSnowballStemmer.getDescription("English"));

		// Now annotate documents with gold standard labels
		switch (mode) {
		case TRAIN:
			// If this is training, put the label categories directly into the
			// default view
			builder.add(AnalysisEngineFactory
					.createPrimitiveDescription(GoldDocumentCategoryAnnotator.class));
			break;

		case TEST:
			// Copies the text from the default view to a separate gold view
			builder.add(AnalysisEngineFactory.createPrimitiveDescription(
					ViewTextCopierAnnotator.class,
					ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME,
					CAS.NAME_DEFAULT_SOFA,
					ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME,
					GOLD_VIEW_NAME));

			// If this is testing, put the document categories in the gold view
			// The extra parameters to add() map the default view to the gold
			// view.
			builder.add(
					AnalysisEngineFactory
							.createPrimitiveDescription(GoldDocumentCategoryAnnotator.class),
					CAS.NAME_DEFAULT_SOFA, GOLD_VIEW_NAME);
			break;

		case CLASSIFY:
		default:
			// In normal mode don't deal with gold labels
			break;
		}

		return builder;
	}

	/**
	 * Creates the aggregate builder for the document classification pipeline
	 */
	public static AggregateBuilder createDocumentClassificationAggregate(
			File modelDirectory, AnnotatorMode mode)
			throws ResourceInitializationException {

		AggregateBuilder builder = CrossValidationEvaluation_OLD
				.createPreprocessingAggregate(modelDirectory, mode);

		switch (mode) {
		case TRAIN:
			// For training we will create DocumentClassificationAnnotator that
			// Extracts the features as is, and then writes out the data to
			// a serialized instance file.
			builder.add(AnalysisEngineFactory.createPrimitiveDescription(
					TfIdfCentroidAnnotator.class,
					DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, InstanceDataWriter.class.getName(),
					DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, modelDirectory.getPath()));
			break;
			
		case TEST:
		
		case CLASSIFY:
		
		default:
			
			// For testing and standalone classification, 
			// we want to create a
			// DocumentClassificationAnnotator using
			// all of the model data computed during training. 
			// This includes
			// feature normalization data
			// and the model jar file for the classifying algorithm
			AnalysisEngineDescription documentClassificationAnnotator = AnalysisEngineFactory
					.createPrimitiveDescription(
							TfIdfCentroidAnnotator.class,
							CleartkAnnotator.PARAM_IS_TRAINING,
							false,
							GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
							new File(modelDirectory, "model.jar").getPath());

			ConfigurationParameterFactory
					.addConfigurationParameters(
							documentClassificationAnnotator,
							TfIdfCentroidAnnotator.PARAM_TF_IDF_URI,
							TfIdfCentroidAnnotator
									.createTokenTfIdfDataURI(modelDirectory),
							TfIdfCentroidAnnotator.PARAM_TF_IDF_CENTROID_SIMILARITY_URI,
							TfIdfCentroidAnnotator
									.createIdfCentroidSimilarityDataURI(modelDirectory),
							TfIdfCentroidAnnotator.PARAM_MINMAX_URI,
							TfIdfCentroidAnnotator
									.createMinMaxDataURI(modelDirectory),
							TfIdfCentroidAnnotator.PARAM_ZMUS_URI,
							TfIdfCentroidAnnotator
									.createZmusDataURI(modelDirectory));
			builder.add(documentClassificationAnnotator);
			break;
		}
		return builder;
	}

	@Override
	protected AnnotationStatistics<String> test(
			CollectionReader collectionReader, File directory) throws Exception {
		AnnotationStatistics<String> stats = new AnnotationStatistics<String>();
		
		// Create the document classification pipeline
		AggregateBuilder builder = CrossValidationEvaluation_OLD
				.createDocumentClassificationAggregate(directory,
						AnnotatorMode.TEST);
		AnalysisEngine engine = builder.createAggregate();

		// Run and evaluate
		Function<CatorgorizedFtdText, ?> getSpan = AnnotationStatistics
				.annotationToSpan();
		Function<CatorgorizedFtdText, String> getCategory = AnnotationStatistics
				.annotationToFeatureValue("category");
		for (JCas jCas : new JCasIterable(collectionReader, engine)) {
			JCas goldView = jCas.getView(GOLD_VIEW_NAME);
			JCas systemView = jCas.getView(CrossValidationEvaluation_OLD.SYSTEM_VIEW_NAME);

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