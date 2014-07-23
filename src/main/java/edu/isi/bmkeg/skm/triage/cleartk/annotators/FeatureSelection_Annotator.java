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
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor.BaseFeatures;
import org.cleartk.ml.feature.function.LowerCaseFeatureFunction;
import org.cleartk.ml.feature.selection.MutualInformationFeatureSelectionExtractor;
import org.cleartk.ml.feature.transform.extractor.TfidfExtractor;
import org.cleartk.token.type.Token;
import org.uimafit.descriptor.ConfigurationParameter;


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
public class FeatureSelection_Annotator extends CategorizedFtdAnnotator {
	
	private static Logger logger = Logger.getLogger(FeatureSelection_Annotator.class);
	
	private MutualInformationFeatureSelectionExtractor<DocumentAnnotation, Token> selector;
	
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

		this.selector = new MutualInformationFeatureSelectionExtractor<DocumentAnnotation, Token>(
				"unigramMutualInformation", 
				lowerCaseExtractor1,
				100);
				
	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		//
		// use the extractor to create features for the document
		//
		DocumentAnnotation doc = (DocumentAnnotation) jCas
				.getDocumentAnnotationFs();
				
		Instance<Boolean> instance = new Instance<Boolean>();
	//	instance.addAll( this.selector.extract(jCas, doc) );
		
		if (isTraining()) {
			
			// during training, get the label for this 
			// document from the CAS
			writeInstance(jCas, instance);
			
		} else {
			
			// TODO: IS THIS RIGHT?
			
		/*	if( this.uniTfIdfUri == null ) { //|| this.biTfIdfUri == null ) {
				throw new AnalysisEngineProcessException(new Exception("Must have instantiated TF-IDF counts"));
			}
			
			TfidfExtractor<Boolean, DocumentAnnotation> extractor1 = 
					new TfidfExtractor<Boolean, DocumentAnnotation>("unigram");
			Instance<Boolean> instance2 = extractor1.transform(instance);*/
			//TfidfExtractor<Boolean, DocumentAnnotation> extractor2 = 
			//		new TfidfExtractor<Boolean, DocumentAnnotation>("biigram");
			//Instance<Boolean> instance3 = extractor2.transform(instance2);
			//createCategorizedFtdAnnotation(jCas, instance2.getFeatures());
		
		}
		
	}
	
}
