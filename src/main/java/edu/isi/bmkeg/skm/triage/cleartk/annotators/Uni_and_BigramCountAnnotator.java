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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.feature.extractor.CleartkExtractor;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Count;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Covered;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Ngrams;
import org.cleartk.classifier.feature.extractor.CoveredTextExtractor;
import org.cleartk.token.type.Token;


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
public class Uni_and_BigramCountAnnotator extends CategorizedFtdAnnotator {

	private CleartkExtractor uniExtractor;
	
	private CleartkExtractor biExtractor;
	
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		//
		// Create an extractor that gives word counts for a document
		//
		this.uniExtractor = new CleartkExtractor(Token.class,
				new CoveredTextExtractor(), 
				new Count(new Covered()));

		this.biExtractor = new CleartkExtractor(Token.class,
		        new CoveredTextExtractor(),
		        new Ngrams(2, new Covered())
		);

	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		//
		// use the extractor to create features for the document
		//
		DocumentAnnotation doc = (DocumentAnnotation) jCas
				.getDocumentAnnotationFs();

		List<Feature> features = this.biExtractor.extract(jCas, doc);
		
		Map<String, Integer> countFeatures = new HashMap<String, Integer>();
		for( Feature f : features ) {
			String fKey = f.getName() + "_" + f.getValue();
			if( !countFeatures.containsKey( fKey ) ) {
				countFeatures.put(fKey, 1);				
			} else {
				countFeatures.put(fKey, countFeatures.get(fKey)+1);								
			}
		}
		
		features = new ArrayList<Feature>();
		for( String fKey : countFeatures.keySet() ) {
			Feature f = new Feature();
			f.setName(fKey);
			f.setValue(countFeatures.get(fKey));
			features.add(f);
		}
		
		features.addAll( this.uniExtractor.extract(jCas, doc) );
				
		if (isTraining()) {
			
			// during training, get the label for this 
			// document from the CAS
			writeInstance(jCas, features);
			
		}

		else {
			
			// during classification, use the classifier's output 
			// to create a CAS annotation
			createCategorizedFtdAnnotation(jCas, features);
		
		}
		
	}
	
}
