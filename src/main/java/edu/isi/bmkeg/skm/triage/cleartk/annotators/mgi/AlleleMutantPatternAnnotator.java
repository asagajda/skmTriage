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
package edu.isi.bmkeg.skm.triage.cleartk.annotators.mgi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.feature.extractor.CleartkExtractor;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Covered;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Ngrams;
import org.cleartk.classifier.feature.extractor.CoveredTextExtractor;
import org.cleartk.token.type.Token;

import edu.isi.bmkeg.skm.triage.cleartk.annotators.CategorizedFtdAnnotator;

/**
 * <br>
 * Copyright (c) 2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * 
 * Simple annotator to look for 'geneName (+/-)' patterns.
 * 
 * @author Gully Burns
 * 
 */
public class AlleleMutantPatternAnnotator extends CategorizedFtdAnnotator {

	private CleartkExtractor<DocumentAnnotation, Token> extractor;
	
	Map<String,Pattern> pattHash = new HashMap<String,Pattern>();

	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		pattHash.put("gentoype_allele_pattern", Pattern.compile("[\\+\\-]_{0,1}\\/_{0,1}[\\-\\+]") );		
		pattHash.put("mouseMiceMurine_pattern", Pattern.compile("[mM](ice|ouse|urine)") );		

		//
		// Create an extractor that counts 3-grams
		//
		this.extractor = new CleartkExtractor<DocumentAnnotation, Token>(Token.class,
				new CoveredTextExtractor<Token>(),
		        new Ngrams(2, new Covered())
		);
		   
	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		//
		// use the extractor to count number of genotype allele combinations in document
		//
		DocumentAnnotation doc = (DocumentAnnotation) jCas
				.getDocumentAnnotationFs();
		List<Feature> features = this.extractor.extract(jCas, doc);
		List<Feature> f2 = this.extractor.extract(jCas, doc);
		
		for(String key : pattHash.keySet()) {
			Pattern patt = pattHash.get(key);
			boolean goFlag = false;
			
			Map<String, Integer> geneotypeFeature = new HashMap<String, Integer>();
			LOOP: for( Feature f : features ) {
				String fKey = f.getName() + "_" + f.getValue();
			
				Matcher m = patt.matcher((CharSequence) f.getValue());
				if( m.find() ) {
					goFlag = true;
					break LOOP;
				}
			}
		
			Feature f = new Feature();
			f2.add(f);
			f.setName(key);
			if( goFlag ) {
				f.setValue(1);
			} else {
				f.setValue(0);			
			}
		}	
		
		if (isTraining()) {
			
			// during training, get the label for this 
			// document from the CAS
			writeInstance(jCas, f2);
			
		}

		else {
			
			// during classification, use the classifier's output 
			// to create a CAS annotation
			createCategorizedFtdAnnotation(jCas, features);
		
		}
		
	}
	
}
