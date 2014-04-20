/** 
 * Copyright (c) 2012, Regents of the University of Colorado 
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
package edu.isi.bmkeg.skm.triage.cleartk.annotators;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.feature.extractor.CleartkExtractor;
import org.cleartk.classifier.feature.extractor.CleartkExtractorException;
import org.cleartk.classifier.feature.extractor.CombinedExtractor1;
import org.cleartk.classifier.feature.extractor.CoveredTextExtractor;
import org.cleartk.classifier.feature.extractor.FeatureExtractor1;
import org.cleartk.classifier.feature.transform.extractor.MinMaxNormalizationExtractor;
import org.cleartk.classifier.feature.transform.extractor.TfidfExtractor;
import org.cleartk.classifier.feature.transform.extractor.ZeroMeanUnitStddevExtractor;
import org.cleartk.classifier.jar.GenericJarClassifierFactory;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.util.JCasUtil;

/**
 * <br>
 * Copyright (c) 2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * 
 * This class demonstrates how to write a new CleartkAnnotator. Like the
 * {@link UnigramCountAnnotator}, this class is used for building and
 * categorizing documents according to their Usenet group. The feature
 * extraction flow illustrates how to extract more complex features that require
 * aggregating statistics for transformation prior to training and
 * classification.
 * 
 * @author Lee Becker
 * 
 */
public class TfIdfCentroidAnnotator extends CategorizedFtdAnnotator {

	private static Logger logger = Logger
			.getLogger(TfIdfCentroidAnnotator.class);

	public static final String PARAM_TF_IDF_URI = ConfigurationParameterFactory
			.createConfigurationParameterName(TfIdfCentroidAnnotator.class,
					"tfIdfUri");

	@ConfigurationParameter(mandatory = false, description = "provides a URI where the tf*idf map "
			+ "will be written")
	protected URI tfIdfUri;

	public static final String PARAM_TF_IDF_CENTROID_SIMILARITY_URI = ConfigurationParameterFactory
			.createConfigurationParameterName(TfIdfCentroidAnnotator.class,
					"tfIdfCentroidSimilarityUri");

	@ConfigurationParameter(mandatory = false, description = "provides a URI where the tf*idf "
			+ "centroid data will be written")
	protected URI tfIdfCentroidSimilarityUri;

	public static final String PARAM_ZMUS_URI = ConfigurationParameterFactory
			.createConfigurationParameterName(TfIdfCentroidAnnotator.class,
					"zmusUri");

	@ConfigurationParameter(mandatory = false, description = "provides a URI where the Zero Mean, "
			+ "Unit Std Dev feature data will be written")
	protected URI zmusUri;

	public static final String PARAM_MINMAX_URI = ConfigurationParameterFactory
			.createConfigurationParameterName(TfIdfCentroidAnnotator.class,
					"minmaxUri");

	@ConfigurationParameter(mandatory = false, description = "provides a URI where the min-max feature "
			+ "normalization data will be written")
	protected URI minmaxUri;

	public static final String PREDICTION_VIEW_NAME = "ExampleDocumentClassificationPredictionView";

	public static final String TFIDF_EXTRACTOR_KEY = "Token";

	public static final String CENTROID_TFIDF_SIM_EXTRACTOR_KEY = "CentroidTfIdfSimilarity";

	public static final String ZMUS_EXTRACTOR_KEY = "ZmusLengthFeatures";

	public static final String MINMAX_EXTRACTOR_KEY = "MinmaxLengthFeatures";

	private CombinedExtractor1 extractor;

	public static URI createTokenTfIdfDataURI(File outputDirectoryName) {
		File f = new File(outputDirectoryName, TFIDF_EXTRACTOR_KEY
				+ "_tfidf_extractor.dat");
		return f.toURI();
	}

	public static URI createIdfCentroidSimilarityDataURI(
			File outputDirectoryName) {
		File f = new File(outputDirectoryName, CENTROID_TFIDF_SIM_EXTRACTOR_KEY);
		return f.toURI();
	}

	public static URI createZmusDataURI(File outputDirectoryName) {
		File f = new File(outputDirectoryName, ZMUS_EXTRACTOR_KEY
				+ "_zmus_extractor.dat");
		return f.toURI();
	}

	public static URI createMinMaxDataURI(File outputDirectoryName) {
		File f = new File(outputDirectoryName, MINMAX_EXTRACTOR_KEY
				+ "_minmax_extractor.dat");
		return f.toURI();
	}

	// I excluded the CentroidTfidfSimilarityExtractor because it was causing
	// the
	// LIBSVM classifier to not terminate. I have no idea why this happened but
	// excluding that extractor fixed that problem [MT]
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		try {
			TfidfExtractor<String,Token> tfIdfExtractor = initTfIdfExtractor();
			// CentroidTfidfSimilarityExtractor<String> simExtractor =
			// initCentroidTfIdfSimilarityExtractor();
			ZeroMeanUnitStddevExtractor<String,Token> zmusExtractor = initZmusExtractor();
			MinMaxNormalizationExtractor<String,Token> minmaxExtractor = initMinMaxExtractor();

			this.extractor = new CombinedExtractor1(
					tfIdfExtractor,
					// simExtractor,
					zmusExtractor, 
					minmaxExtractor);
		
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
		
	}

	private TfidfExtractor<String, Token> initTfIdfExtractor() throws IOException {
		CleartkExtractor countsExtractor = new CleartkExtractor(Token.class,
				new CoveredTextExtractor(), new CleartkExtractor.Count(
						new CleartkExtractor.Covered()));

		TfidfExtractor<String, Token> tfIdfExtractor = new TfidfExtractor<String, Token>(
				TfIdfCentroidAnnotator.TFIDF_EXTRACTOR_KEY, countsExtractor);

		if (this.tfIdfUri != null) {
			tfIdfExtractor.load(this.tfIdfUri);
		}
		return tfIdfExtractor;
	}

	// private CentroidTfidfSimilarityExtractor<String>
	// initCentroidTfIdfSimilarityExtractor()
	// throws IOException {
	// CleartkExtractor countsExtractor = new CleartkExtractor(Token.class,
	// new CoveredTextExtractor(), new CleartkExtractor.Count(
	// new CleartkExtractor.Covered()));
	//
	// CentroidTfidfSimilarityExtractor<String> simExtractor = new
	// CentroidTfidfSimilarityExtractor<String>(
	// TfIdfCentroidAnnotator.CENTROID_TFIDF_SIM_EXTRACTOR_KEY,
	// countsExtractor);
	//
	// if (this.tfIdfCentroidSimilarityUri != null) {
	// simExtractor.load(this.tfIdfCentroidSimilarityUri);
	// }
	// return simExtractor;
	// }

	private ZeroMeanUnitStddevExtractor<String, Token> initZmusExtractor()
			throws IOException {
		CombinedExtractor1 featuresToNormalizeExtractor = new CombinedExtractor1(
				new CountAnnotationExtractor(Sentence.class),
				new CountAnnotationExtractor(Token.class));

		ZeroMeanUnitStddevExtractor<String, Token> zmusExtractor = new ZeroMeanUnitStddevExtractor<String, Token>(
				ZMUS_EXTRACTOR_KEY, featuresToNormalizeExtractor);

		if (this.zmusUri != null) {
			zmusExtractor.load(this.zmusUri);
		}

		return zmusExtractor;
	}

	private MinMaxNormalizationExtractor<String, Token> initMinMaxExtractor()
			throws IOException {
		CombinedExtractor1 featuresToNormalizeExtractor = new CombinedExtractor1(
				new CountAnnotationExtractor(Sentence.class),
				new CountAnnotationExtractor(Token.class));

		MinMaxNormalizationExtractor<String, Token> minmaxExtractor = 
				new MinMaxNormalizationExtractor<String, Token>(
				MINMAX_EXTRACTOR_KEY, featuresToNormalizeExtractor);

		if (this.minmaxUri != null) {
			minmaxExtractor.load(this.minmaxUri);
		}

		return minmaxExtractor;
	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		DocumentAnnotation doc = (DocumentAnnotation) jCas
				.getDocumentAnnotationFs();

		Instance<Boolean> instance = new Instance<Boolean>();
		instance.addAll(this.extractor.extract(jCas, doc));

		if (isTraining()) {

//			writeInstance(jCas, instance);

		} else {

			createCategorizedFtdAnnotation(jCas, instance.getFeatures());

		}
	}

	public static AnalysisEngineDescription getClassifierDescription(
			File classifierJarFile) throws ResourceInitializationException {
		return AnalysisEngineFactory.createPrimitiveDescription(
				TfIdfCentroidAnnotator.class,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				classifierJarFile.toString());
	}

	public static class CountAnnotationExtractor implements FeatureExtractor1 {

		@SuppressWarnings("rawtypes")
		private Class annotationType;

		@SuppressWarnings("rawtypes")
		public CountAnnotationExtractor(Class annotationType) {
			this.annotationType = annotationType;
		}

		@Override
		public List<Feature> extract(JCas view, Annotation focusAnnotation)
				throws CleartkExtractorException {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			List annotations = JCasUtil.selectCovered(this.annotationType,
					focusAnnotation);
			return Arrays.asList(new Feature("Count_"
					+ annotationType.getName(), annotations.size()));
		}
	}

}
