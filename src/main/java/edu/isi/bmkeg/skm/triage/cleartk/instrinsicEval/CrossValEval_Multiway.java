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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.cleartk.classifier.CleartkAnnotator;
import org.cleartk.classifier.DataWriter;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.feature.transform.InstanceDataWriter;
import org.cleartk.classifier.feature.transform.InstanceStream;
import org.cleartk.classifier.feature.transform.extractor.TfidfExtractor;
import org.cleartk.classifier.jar.DefaultDataWriterFactory;
import org.cleartk.classifier.jar.DirectoryDataWriterFactory;
import org.cleartk.classifier.jar.GenericJarClassifierFactory;
import org.cleartk.classifier.jar.JarClassifierBuilder;
import org.cleartk.classifier.libsvm.LibSvmBooleanOutcomeDataWriter;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.syntax.opennlp.SentenceAnnotator;
import org.cleartk.token.stem.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.uimafit.component.ViewTextCopierAnnotator;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.testing.util.HideOutput;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Function;

import edu.isi.bmkeg.skm.cleartk.type.CatorgorizedFtdText;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.GoldDocumentCategoryAnnotator;
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
 * @
 */
public class CrossValEval_Multiway extends
		CrossValidationEvaluation {

	private static Logger logger = Logger
			.getLogger(CrossValEval_Multiway.class);

	public List<String> trainingArguments;

	public boolean dumpTrainingDataFirstFlag = false;

	public Class<? extends AnalysisComponent> annotatorClass;
	
	public String dataWriterClassName;

	public static void main(String[] args) throws Exception {
		CrossValEvalOptions options = new CrossValEvalOptions();
		options.parseOptions(args);

		if (options.trainingArguments.size() == 0
				&& options.dataWriterClassName
						.equals(LibSvmBooleanOutcomeDataWriter.class.getName())) {
			options.trainingArguments = Arrays.asList("-t", "0");
		}

		CrossValEval_Multiway eval = new CrossValEval_Multiway(
				options.baseDir, 
				options.trainingArguments,
				options.dataWriterClassName,
				options.annotatorClassName,
				options.nFolds,
				options.dataDirectory);

		eval.runMain();

	}

	public CrossValEval_Multiway(File baseDirectory,
			List<String> trainingArguments,
			String dataWriterClassName,			
			String features,			
			int nFolds,
			File dataDir) throws Exception {

		super(baseDirectory, dataDir);
		this.trainingArguments = trainingArguments;
		
		/*
		 * Restrict options for types of feature sets allowed here.
		 */
		if( !features.equals("BigramCount") && 
				!features.equals("TfIdf") &&  
				!features.equals("UnigramCount") && 
				!features.equals("Uni_and_BigramCount") && 
				!features.equals("AlleleMutantPattern")) {
			
			throw new Exception("annotationClassName must be " +
					"'BigramCount' or " +
					"'TfIdf' or " + 
					"'UnigramCount' or " + 
					"'Unigram_and_BigramCount' or " +
					"'AlleleMutantPattern'");
		
		}

		if(features.equals("TfIdf_Annotator")) {
			dumpTrainingDataFirstFlag = true;
		}
		
		if( dataWriterClassName.equals("LibSvm") ) {

			this.dataWriterClassName = "org.cleartk.classifier.libsvm.LibSvmBooleanOutcomeDataWriter";
		
		} else if( dataWriterClassName.equals("LibLinear") ) {

			this.dataWriterClassName = "org.cleartk.classifier.liblinear.LibLinearBooleanOutcomeDataWriter";
		
		} else if( dataWriterClassName.equals("Mallet") ) {

			this.dataWriterClassName = "org.cleartk.classifier.mallet.MalletBooleanOutcomeDataWriter";
		
		} else {
			
			throw new Exception("dataWriterClassName must be 'LibSvm' or "
					+ "'Mallet' or "
					+ "'LibLinear'");
			
		}
		
		this.nFolds = nFolds;
	
	}

	@Override
	public void train(CollectionReader collectionReader, File outputDirectory)
			throws Exception {

		/**
		 *  Step 1: Extract features and serialize the raw instance objects
		 */ 
		logger.info("1. Extracting features and writing raw instances data");

		// Create and run the document classification training pipeline
		AggregateBuilder builder = new AggregateBuilder();

		//
		// NLP pre-processing components
		//
		//builder.add(SentenceAnnotator.getDescription());
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				TokenAnnotator.class,
				TokenAnnotator.PARAM_WINDOW_TYPE_NAME, "org.apache.uima.jcas.tcas.DocumentAnnotation"));
		
		builder.add(DefaultSnowballStemmer.getDescription("English"));
		builder.add(AnalysisEngineFactory
				.createPrimitiveDescription(GoldDocumentCategoryAnnotator.class));

		// If we're running an aggregated data writer (TF-IDF),
		// then we need to use InstandDataWriter to save the raw 
		// scores for subsequent inclusion.
		if( this.annotatorClass == TfIdf_Annotator.class ) {
			
			builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				this.annotatorClass,
				CleartkAnnotator.PARAM_IS_TRAINING, true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, InstanceDataWriter.class.getName(),
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, outputDirectory));

		} 
		// Otherwise just use the dataWriterClassName as is.
		else {

			builder.add(AnalysisEngineFactory.createPrimitiveDescription(
					this.annotatorClass,
					CleartkAnnotator.PARAM_IS_TRAINING, true,
					DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, this.dataWriterClassName,
					DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, outputDirectory));
			
		}
			
		// run the pipeline
		SimplePipeline.runPipeline(collectionReader, builder.createAggregateDescription());

		// If this is running based on an annotator that does not 
		// require aggregated data, train the model and return.
		if( this.annotatorClass != TfIdf_Annotator.class ) {
			logger.info("2. Train model and write model.jar file.\n");
			String[] tArgs = this.trainingArguments.toArray(new String[this.trainingArguments.size()]);
			//HideOutput hider = new HideOutput();
			JarClassifierBuilder.trainAndPackage(outputDirectory, tArgs);
			//hider.restoreOutput();
			return;
		}
		
		logger.info("2. Collection feature normalization statistics");

		// Load the serialized instance data
		Iterable<Instance<Boolean>> instances = InstanceStream
				.loadFromDirectory(outputDirectory);

		// Collect TF*IDF stats for computing tf*idf values on extracted tokens
		URI unigramTfIdfDataURI = TfIdf_Annotator.createTokenTfIdfDataURI(outputDirectory, "unigram");
		TfidfExtractor<Boolean, DocumentAnnotation> extractor1 = new TfidfExtractor<Boolean, DocumentAnnotation>("unigram");
		extractor1.train(instances);
		extractor1.save(unigramTfIdfDataURI);

		/*URI bigramTfIdfDataURI = MGI_FeatureEngineering_Annotator.createTokenTfIdfDataURI(outputDirectory, "bigram");
		TfidfExtractor<Boolean, DocumentAnnotation> extractor2 = new TfidfExtractor<Boolean, DocumentAnnotation>("bigram");
		extractor2.train(instances);
		extractor2.save(bigramTfIdfDataURI);*/
		
		/**
		 * Step 3: Iterate through the instances of existing features and run the tfIdf
		 * transform on them
		 */
		logger.info("3. Write out model training data");
		DataWriter<Boolean> dataWriter = createDataWriter(
				this.dataWriterClassName, 
				outputDirectory);	
		for (Instance<Boolean> instance : instances) {
			Instance<Boolean> instance2 = extractor1.transform(instance);
			//Instance<Boolean> instance3 = extractor2.transform(instance2);
			dataWriter.write(instance2);
		}
		dataWriter.finish();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		logger.info("4. Train model and write model.jar file.\n");
		String[] tArgs = this.trainingArguments
				.toArray(new String[this.trainingArguments.size()]);
		HideOutput hider = new HideOutput();
		JarClassifierBuilder.trainAndPackage(outputDirectory, tArgs);
		hider.restoreOutput();
	}

	@Override
	protected AnnotationStatistics<String> test (CollectionReader collectionReader, 
			File directory) throws Exception {
		
		AnnotationStatistics<String> stats = new AnnotationStatistics<String>();

		// Create the document classification pipeline
		AggregateBuilder builder = new AggregateBuilder();

		// NLP pre-processing components
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());
		builder.add(DefaultSnowballStemmer.getDescription("English"));

		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				ViewTextCopierAnnotator.class,
				ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME,
				SYSTEM_VIEW_NAME,
				ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME,
				GOLD_VIEW_NAME));

		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				GoldDocumentCategoryAnnotator.class
				), SYSTEM_VIEW_NAME, GOLD_VIEW_NAME);

		File classifierJarPath = new File(directory, "model.jar");

		if( this.annotatorClass == TfIdf_Annotator.class ) {
		
			builder.add(AnalysisEngineFactory.createPrimitiveDescription(
					TfIdf_Annotator.class,
					TfIdf_Annotator.PARAM_UNI_TF_IDF_URI, 
					TfIdf_Annotator.createTokenTfIdfDataURI(directory, "unigram"),
					CleartkAnnotator.PARAM_IS_TRAINING, false,
					GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
					classifierJarPath));
		
		} else {
			
		    builder.add(AnalysisEngineFactory.createPrimitiveDescription(
		    		this.annotatorClass,
		    		CleartkAnnotator.PARAM_IS_TRAINING, false,
		    		GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, classifierJarPath
					));
			
		}

		AnalysisEngine engine = builder.createAggregate();

		// Run and evaluate
		Function<CatorgorizedFtdText, ?> getSpan = AnnotationStatistics.annotationToSpan();
		Function<CatorgorizedFtdText, String> getCategory = AnnotationStatistics.annotationToFeatureValue("category");

		for (JCas jCas : new JCasIterable(collectionReader, engine)) {
			
			JCas goldView = jCas.getView(GOLD_VIEW_NAME);
			JCas systemView = jCas.getView(SYSTEM_VIEW_NAME);

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
	
	@SuppressWarnings("unchecked")
	private DataWriter<Boolean> createDataWriter(String dataWriterClassName, File outputDirectory) {
		
		try {

			Class<? extends DataWriter<Boolean>> cls = (Class<? extends DataWriter<Boolean>>) 
					Class.forName(dataWriterClassName).asSubclass(DataWriter.class);
			DataWriter<Boolean> dw = cls.getConstructor(File.class).newInstance(outputDirectory);

			return dw;
			
		} catch (Exception e) {
		
			throw new IllegalStateException("Failed to create an instance of DataWriter<boolean> " +
					"from classname: " + dataWriterClassName, e);
		
		}
	
	}

}