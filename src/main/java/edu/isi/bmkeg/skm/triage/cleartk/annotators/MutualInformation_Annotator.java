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
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.feature.extractor.CleartkExtractor;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Focus;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Ngram;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.classifier.feature.extractor.CombinedExtractor1;
import org.cleartk.classifier.feature.extractor.CoveredTextExtractor;
import org.cleartk.classifier.feature.extractor.FeatureExtractor1;
import org.cleartk.classifier.feature.function.FeatureFunctionExtractor;
import org.cleartk.classifier.feature.function.FeatureFunctionExtractor.BaseFeatures;
import org.cleartk.classifier.feature.function.LowerCaseFeatureFunction;
import org.cleartk.classifier.feature.selection.MutualInformationFeatureSelectionExtractor;
import org.cleartk.classifier.feature.transform.extractor.TfidfExtractor;
import org.cleartk.classifier.jar.DirectoryDataWriterFactory;
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
public class MutualInformation_Annotator extends CategorizedFtdAnnotator {
	
	private static Logger logger = Logger.getLogger(MutualInformation_Annotator.class);

	public static final String UNI_MODE = "uni";
	public static final String BI_MODE = "bi";
	public static final String TRI_MODE = "tri";
	public static final String ALL_MODE = "all";
	
	public static final String PARAM_MODE = "miMode";
	@ConfigurationParameter(
			name = PARAM_MODE,
			mandatory = false, 
			description = "set this this parameter to the URI for unigram MI scores")
	private String mode = UNI_MODE;
	
	public static final String PARAM_UNI_MI_URI = "uniMiUri";
	@ConfigurationParameter(
			name = PARAM_UNI_MI_URI,
			mandatory = false, 
			description = "set this this parameter to the URI for biigram MI scores")
	private URI uniMiUri;
	
	private MutualInformationFeatureSelectionExtractor<Boolean, DocumentAnnotation> miExtractor1;
	
	public static final String PARAM_BI_MI_URI = "biMiUri";
	@ConfigurationParameter(
			name = PARAM_BI_MI_URI,
			mandatory = false, 
			description = "set this this parameter to the URI for triigram MI scores")
	private URI biMiUri;
	
	private MutualInformationFeatureSelectionExtractor<Boolean, DocumentAnnotation> miExtractor2;
			
	public static final String PARAM_TRI_MI_URI = "triMiUri";
	@ConfigurationParameter(
			name = PARAM_TRI_MI_URI,
			mandatory = false, 
			description = "provides a URI where the trigram MI scores will be written")
	private URI triMiUri;
	
	private MutualInformationFeatureSelectionExtractor<Boolean, DocumentAnnotation> miExtractor3;
	
	public static URI createDataURI(File outputDirectoryName, String code) {
		File f = new File(outputDirectoryName, code + "_mi_extractor.dat");
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

		this.miExtractor1 = new MutualInformationFeatureSelectionExtractor<Boolean, DocumentAnnotation>(
				PARAM_UNI_MI_URI, 
				uniExtractor);
		
		Object uniMiContext = context.getConfigParameterValue(PARAM_UNI_MI_URI);
		try {
			if( uniMiContext != null ) {
				this.uniMiUri = new URI( (String) uniMiContext);
				miExtractor1.load(this.uniMiUri);
			 }
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		} catch (URISyntaxException e) {
			throw new ResourceInitializationException(e);
		}

		CleartkExtractor<Token, Token> biExtractor1 = new CleartkExtractor<Token, Token>(Token.class,
				lowerCaseExtractor1,
			    new Ngram(new Preceding(1), new Focus()));
		
		CleartkExtractor<DocumentAnnotation, Token> biExtractor2 =  
				   new CleartkExtractor<DocumentAnnotation, Token>(Token.class,
						   biExtractor1,
					        new CleartkExtractor.Count(new CleartkExtractor.Covered()));
		
		this.miExtractor2 = new MutualInformationFeatureSelectionExtractor<Boolean, DocumentAnnotation>(
				PARAM_BI_MI_URI, 
				biExtractor2);

		Object biMiContext = context.getConfigParameterValue(PARAM_BI_MI_URI);
		try {
			if( biMiContext != null ) {
				this.biMiUri = new URI( (String) biMiContext);
				miExtractor2.load(this.biMiUri);
			 }
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		} catch (URISyntaxException e) {
			throw new ResourceInitializationException(e);
		}
		
		CleartkExtractor<Token, Token> triExtractor1 = new CleartkExtractor<Token, Token>(Token.class,
				lowerCaseExtractor1,
			    new Ngram(new Preceding(2), new Focus()));
		
		CleartkExtractor<DocumentAnnotation, Token> triExtractor2 =  
				   new CleartkExtractor<DocumentAnnotation, Token>(Token.class,
						   triExtractor1,
					        new CleartkExtractor.Count(new CleartkExtractor.Covered()));
		
		this.miExtractor3 = new MutualInformationFeatureSelectionExtractor<Boolean, DocumentAnnotation>(
				PARAM_TRI_MI_URI, 
				triExtractor2);

		Object triMiContext = context.getConfigParameterValue(PARAM_TRI_MI_URI);
		try {
			if( triMiContext != null ) {
				this.triMiUri = new URI( (String) triMiContext);
				miExtractor3.load(this.triMiUri);
			 }
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		} catch (URISyntaxException e) {
			throw new ResourceInitializationException(e);
		}
	
	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		//
		// use the extractor to create features for the document
		//
		DocumentAnnotation doc = (DocumentAnnotation) jCas
				.getDocumentAnnotationFs();
				
		Instance<Boolean> instance = new Instance<Boolean>();
		
		if( mode.equals(UNI_MODE) || mode.equals(ALL_MODE) ) {
			
			instance.addAll( this.miExtractor1.extract(jCas, doc) );	
		
		} else if( mode.equals(BI_MODE) || mode.equals(ALL_MODE) ) {
			
			instance.addAll( this.miExtractor2.extract(jCas, doc) );
			
		} else if( mode.equals(TRI_MODE) || mode.equals(ALL_MODE) ) {

			instance.addAll( this.miExtractor3.extract(jCas, doc) );

		} else {
		
			instance.addAll( this.miExtractor1.extract(jCas, doc) );	
			
		}
		
		if (isTraining()) {
			
			// during training, get the label for this 
			// document from the CAS
			writeInstance(jCas, instance);
			
		} else {
			
			/*if( this.uniMiUri == null ) { //|| this.biTfIdfUri == null ) {
				throw new AnalysisEngineProcessException(new Exception("Must have instantiated MI scores"));
			}
			
			TfidfExtractor<Boolean, DocumentAnnotation> extractor1 = 
					new TfidfExtractor<Boolean, DocumentAnnotation>("unigram");
			Instance<Boolean> instance2 = extractor1.transform(instance);
			//TfidfExtractor<Boolean, DocumentAnnotation> extractor2 = 
			//		new TfidfExtractor<Boolean, DocumentAnnotation>("biigram");
			//Instance<Boolean> instance3 = extractor2.transform(instance2);
			createCategorizedFtdAnnotation(jCas, instance2.getFeatures());*/
		
		}
		
	}
	
}
