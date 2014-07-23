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
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Focus;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Ngram;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.CombinedExtractor1;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor.BaseFeatures;
import org.cleartk.ml.feature.function.LowerCaseFeatureFunction;
import org.cleartk.ml.feature.transform.extractor.TfidfExtractor;
import org.cleartk.token.type.Token;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.ConfigurationParameterFactory;


/**
 * <br>
 * Copyright (c) 2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * 
 * Simple class to demonstrate how to write a new CleartkAnnotator. This class
 * implements a basic document classification annotator whose only features are
 * the counts of the words in the document.
 * 
 * @author Lee Becker
 * 
 */
public class TfIdf_Annotator extends CategorizedFtdAnnotator {
	
	private static Logger logger = Logger.getLogger(TfIdfCentroidAnnotator.class);
	
	public static final String PARAM_UNI_TF_IDF_URI = "uniTfIdfUri";
	@ConfigurationParameter(
			name = PARAM_UNI_TF_IDF_URI,
			mandatory = false, 
			description = "provides a URI where the tf*idf map will be written")
	private URI uniTfIdfUri;

	public static final String PARAM_BI_TF_IDF_URI = "biTfIdfUri";
	@ConfigurationParameter(
			name = PARAM_BI_TF_IDF_URI,
			mandatory = false, 
			description = "provides a URI where the tf*idf map will be written")
	private URI biTfIdfUri;
	
	private int count = 0;

	private TfidfExtractor<Boolean, DocumentAnnotation> tfidfExtractor1;
	private CleartkExtractor<DocumentAnnotation, Token> countBiExtractor;
			
	public static URI createTokenTfIdfDataURI(File outputDirectoryName, String code) {
		File f = new File(outputDirectoryName, code + "_tfidf_extractor.dat");
		return f.toURI();
	}

	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		
		super.initialize(context);		
				
		FeatureExtractor1<Token> lowerCaseExtractor1 = new FeatureFunctionExtractor<Token>(
				new CoveredTextExtractor<Token>(),
				BaseFeatures.EXCLUDE,
				new LowerCaseFeatureFunction());
						
		CleartkExtractor<DocumentAnnotation, Token> uniExtractor =  
				   new CleartkExtractor<DocumentAnnotation, Token>(Token.class,
					        lowerCaseExtractor1,
					        new CleartkExtractor.Count(new CleartkExtractor.Covered()));

		this.tfidfExtractor1 = new TfidfExtractor<Boolean, DocumentAnnotation>("unigram", uniExtractor);

		Object uniTfIdfContext = context.getConfigParameterValue(PARAM_UNI_TF_IDF_URI);
		try {
			if( uniTfIdfContext != null ) {
				this.uniTfIdfUri = new URI( (String) uniTfIdfContext);
				tfidfExtractor1.load(this.uniTfIdfUri);
			 }
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		} catch (URISyntaxException e) {
			throw new ResourceInitializationException(e);
		}
		
	    FeatureExtractor1<Token> lowerCaseExtractor2 = new FeatureFunctionExtractor<Token>(
				new CoveredTextExtractor<Token>(),
				BaseFeatures.EXCLUDE,
				new LowerCaseFeatureFunction());

		CleartkExtractor<Token, Token> biExtractor = new CleartkExtractor<Token, Token>(Token.class,
				lowerCaseExtractor2,
			    new Ngram(new Preceding(1), new Focus()));

		this.countBiExtractor = new CleartkExtractor<DocumentAnnotation, Token>(Token.class,
				biExtractor,
			    new CleartkExtractor.Count(new CleartkExtractor.Covered()));

		/*this.tfidfExtractor2 = 
				   new TfidfExtractor<Boolean, DocumentAnnotation>("bigram", countBiExtractor);

		Object biTfIdfContext = context.getConfigParameterValue(PARAM_BI_TF_IDF_URI);
		try {
			if( biTfIdfContext != null ) {
				this.biTfIdfUri = new URI( (String) biTfIdfContext);
				tfidfExtractor2.load(this.biTfIdfUri);
			 }
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		} catch (URISyntaxException e) {
			throw new ResourceInitializationException(e);
		}*/
		
	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		//
		// use the extractor to create features for the document
		//
		DocumentAnnotation doc = (DocumentAnnotation) jCas
				.getDocumentAnnotationFs();
				
		Instance<Boolean> instance = new Instance<Boolean>();
		instance.addAll( this.tfidfExtractor1.extract(jCas, doc) );
		instance.addAll( this.countBiExtractor.extract(jCas, doc) );
		
		if (isTraining()) {
			
			// during training, get the label for this 
			// document from the CAS
			writeInstance(jCas, instance);
			
		} else {
			
			if( this.uniTfIdfUri == null ) { //|| this.biTfIdfUri == null ) {
				throw new AnalysisEngineProcessException(new Exception("Must have instantiated TF-IDF counts"));
			}
			
			TfidfExtractor<Boolean, DocumentAnnotation> extractor1 = 
					new TfidfExtractor<Boolean, DocumentAnnotation>("unigram");
			Instance<Boolean> instance2 = extractor1.transform(instance);
			//TfidfExtractor<Boolean, DocumentAnnotation> extractor2 = 
			//		new TfidfExtractor<Boolean, DocumentAnnotation>("biigram");
			//Instance<Boolean> instance3 = extractor2.transform(instance2);
			createCategorizedFtdAnnotation(jCas, instance2.getFeatures());
		
		}
		
	}
	
}
