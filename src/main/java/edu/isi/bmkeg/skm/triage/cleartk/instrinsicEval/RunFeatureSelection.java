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

import java.io.File;
import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.cleartk.classifier.CleartkAnnotator;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.feature.extractor.CleartkExtractor;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Focus;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Ngram;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.classifier.feature.extractor.CoveredTextExtractor;
import org.cleartk.classifier.feature.extractor.FeatureExtractor1;
import org.cleartk.classifier.feature.function.FeatureFunctionExtractor;
import org.cleartk.classifier.feature.function.FeatureFunctionExtractor.BaseFeatures;
import org.cleartk.classifier.feature.function.LowerCaseFeatureFunction;
import org.cleartk.classifier.feature.selection.MutualInformationFeatureSelectionExtractor;
import org.cleartk.classifier.feature.transform.InstanceDataWriter;
import org.cleartk.classifier.feature.transform.InstanceStream;
import org.cleartk.classifier.feature.transform.extractor.TfidfExtractor;
import org.cleartk.classifier.jar.DefaultDataWriterFactory;
import org.cleartk.classifier.jar.DirectoryDataWriterFactory;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.syntax.opennlp.SentenceAnnotator;
import org.cleartk.token.stem.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.cleartk.token.type.Token;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.pipeline.SimplePipeline;

import edu.isi.bmkeg.skm.triage.cleartk.annotators.GoldDocumentCategoryAnnotator;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.MutualInformation_Annotator;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.TfIdf_Annotator;

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
public class RunFeatureSelection extends CrossValidationEvaluation {

	private static Logger logger = Logger
			.getLogger(RunFeatureSelection.class);

	public List<String> trainingArguments;
	
	public String dataWriterClassName;
	
	File dataDirectory;
	File baseDir;
	
	List<Integer> trainIds;
	List<Integer> testIds;
	
	int nFolds;
	
	public RunFeatureSelection(File baseDirectory,
			List<String> trainingArguments,
			String dataWriterClassName,			
			int nFolds,
			File dataDir) {

		super(baseDirectory, dataDir);
		this.baseDir = baseDirectory;
		this.dataDirectory = dataDir;
		this.trainingArguments = trainingArguments;
		this.dataWriterClassName = dataWriterClassName;
		this.nFolds = nFolds;
	}

	public static void main(String[] args) throws Exception {
		CrossValEvalOptions options = new CrossValEvalOptions();
		options.parseOptions(args);

		RunFeatureSelection eval = new RunFeatureSelection(
				options.baseDir, 
				options.trainingArguments,
				options.dataWriterClassName,
				options.nFolds,
				options.dataDirectory);

		File trainingDir = new File (eval.dataDirectory.getPath() + "/train");
		File testingDir = new File (eval.dataDirectory.getPath() + "/test");

		List<File> trainFiles = CrossValidationEvaluation.getFilesFromDirectory(trainingDir);
		List<File> testFiles = CrossValidationEvaluation.getFilesFromDirectory(testingDir);

		eval.trainIds = eval.loadIdsFromFiles(trainFiles);
		eval.testIds = eval.loadIdsFromFiles(testFiles);
	
		File subDirectory = new File(eval.baseDirectory, "featureSelection");
		    subDirectory.mkdirs();
		
		/**
		 * Step 1: Extract features and serialize the raw instance objects
		 */
		logger.info("1. Extracting features and writing raw instances data");

		// Create and run the document classification training pipeline
		AggregateBuilder builder = new AggregateBuilder();

		// NLP pre-processing components
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());
		builder.add(DefaultSnowballStemmer.getDescription("English"));
		builder.add(AnalysisEngineFactory
				.createPrimitiveDescription(GoldDocumentCategoryAnnotator.class));
		
		// TF-IDF annotator, need to use InstandDataWriter to save the raw 
		// scores for subsequent inclusion intot 
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				MutualInformation_Annotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING, true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, InstanceDataWriter.class.getName(),
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, subDirectory));

		// run the pipeline
		SimplePipeline.runPipeline(eval.getCollectionReader(eval.trainIds),
				builder.createAggregateDescription());

		/**
		 * Step 2: Transform features and write training data
		 * In this phase, the normalization statistics are computed and the raw
		 * features are transformed into normalized features.
		 * Then the adjusted values are written with a DataWriter
		 * for training
		 */
		logger.info("2. Collection feature normalization statistics");

		// Load the serialized instance data
		Iterable<Instance<Boolean>> instances = InstanceStream
				.loadFromDirectory(subDirectory);

		FeatureExtractor1<Token> lowerCaseExtractor2 = new FeatureFunctionExtractor<Token>(
				new CoveredTextExtractor<Token>(),
				BaseFeatures.EXCLUDE,
				new LowerCaseFeatureFunction());
		
		FeatureExtractor1<Token> biExtractor = new CleartkExtractor<Token, Token>(Token.class,
				lowerCaseExtractor2,
			    new Ngram(new Preceding(1), new Focus()));
		
		CleartkExtractor<DocumentAnnotation, Token> countBiExtractor = new CleartkExtractor<DocumentAnnotation, Token>(Token.class,
				biExtractor,
			    new CleartkExtractor.Count(new CleartkExtractor.Covered()));
				
		MutualInformationFeatureSelectionExtractor<Boolean, DocumentAnnotation> selector = 
				new MutualInformationFeatureSelectionExtractor<Boolean, DocumentAnnotation>(
				"miBigram", 
				countBiExtractor,
				100);
		
		selector.train(instances);
		File f = new File(subDirectory, "features.txt");
		selector.save(f.toURI());
		
		logger.info("Pause here");
		
	
	}

	@Override
	public void train(CollectionReader collectionReader, File outputDirectory)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected AnnotationStatistics<String> test(
			CollectionReader collectionReader, File directory) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}