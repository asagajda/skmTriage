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
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Count;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Covered;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor;
import org.cleartk.ml.feature.function.LowerCaseFeatureFunction;
import org.cleartk.ml.feature.selection.MutualInformationFeatureSelectionExtractor;
import org.cleartk.token.type.Token;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.util.JCasUtil;

import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.triage.model.TriageFeature;
import edu.isi.bmkeg.triage.uimaTypes.TriageScore;

/**
 * Very simple annotator to read the JCas for mentions of the words 'mouse',
 * 'mice' or 'murine'
 */
public class SaveFeaturesToDbAnnotator extends JCasAnnotator_ImplBase {

	public static final String PARAM_VPDMf_LOGIN = ConfigurationParameterFactory
			.createConfigurationParameterName(
					SaveFeaturesToDbAnnotator.class, "vpdmfLogin");

	@ConfigurationParameter(mandatory = true, description = "vpdmfLogin")
	private String vpdmfLogin;

	public static final String PARAM_VPDMf_PASSWORD = ConfigurationParameterFactory
			.createConfigurationParameterName(
					SaveFeaturesToDbAnnotator.class, "vpdmfPassword");

	@ConfigurationParameter(mandatory = true, description = "vpdmfPassword")
	private String vpdmfPassword;

	public static final String PARAM_VPDMf_DBNAME = ConfigurationParameterFactory
			.createConfigurationParameterName(
					SaveFeaturesToDbAnnotator.class, "vpdmfDbName");

	@ConfigurationParameter(mandatory = true, description = "vpdmfDbName")
	private String vpdmfDbName;

	public static final String PARAM_VPDMf_WORKINGDIR = ConfigurationParameterFactory
			.createConfigurationParameterName(
					SaveFeaturesToDbAnnotator.class, "vpdmfDbName");

	@ConfigurationParameter(mandatory = true, description = "vpdmfWorkingDir")
	private String vpdmfWorkingDir;

	
	private FeatureExtractor1 extractor;

	private TriageEngine triageEngine;

	public void initialize(UimaContext context)
			throws ResourceInitializationException {

		super.initialize(context);

		//
		// Create an extractor that gives word counts for a document
		//
		FeatureExtractor1 ex1 = new FeatureFunctionExtractor(
				new CoveredTextExtractor(), new LowerCaseFeatureFunction());

		this.extractor = new CleartkExtractor(Token.class, ex1, new Count(
				new Covered()));

		try {

			this.triageEngine = new TriageEngine();
			this.triageEngine.initializeVpdmfDao(this.vpdmfLogin,
					this.vpdmfPassword, this.vpdmfDbName,
					this.vpdmfWorkingDir);

			this.triageEngine.getExTriageDao().getCoreDao().getCe()
					.connectToDB();
			this.triageEngine.getExTriageDao().getCoreDao().getCe()
					.turnOffAutoCommit();

		} catch (Exception e) {

			throw new ResourceInitializationException(e);

		}

	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {

		DocumentAnnotation doc = (DocumentAnnotation) jCas
				.getDocumentAnnotationFs();

		TriageScore ts = JCasUtil.selectSingle(jCas, TriageScore.class);
		long tsId = ts.getVpdmfId();

		if (tsId == -1)
			return;

		List<TriageFeature> fList = new ArrayList<TriageFeature>();

		// for each token, extract features and the outcome
		List<Feature> features = new ArrayList<Feature>();
		features.addAll(this.extractor.extract(jCas, doc));

		for (Feature f : features) {

			if (f.getName().equals("Count_Covered_LowerCase_mouse")
					|| f.getName().equals("Count_Covered_LowerCase_mice")
					|| f.getName().equals("Count_Covered_LowerCase_murine")) {

				TriageFeature tf = new TriageFeature();

				String s = f.getName();
				String stem = s.substring(s.lastIndexOf("_") + 1, s.length());
				stem = "Count of word: '" + stem + "'";

				tf.setFeatName(stem);
				tf.setFeatValue(f.getValue().toString());
				fList.add(tf);
			}

		}

		try {
			
			triageEngine.removeAllExplanationFeatures(tsId);
			
			for (TriageFeature tf : fList) {

				triageEngine.insertExplanationFeatures(tsId, tf);

			}

		} catch (Exception e) {

			try {

				this.triageEngine.getExTriageDao().getCoreDao().getCe()
						.rollbackTransaction();
				this.triageEngine.getExTriageDao().getCoreDao().getCe()
						.closeDbConnection();

			} catch (Exception e2) {

				throw new AnalysisEngineProcessException(e2);

			}
			throw new AnalysisEngineProcessException(e);

		}

	}

	public void collectionProcessComplete()
			throws AnalysisEngineProcessException {

		try {

			this.triageEngine.getExTriageDao().getCoreDao().getCe()
					.commitTransaction();
			this.triageEngine.getExTriageDao().getCoreDao().getCe()
					.closeDbConnection();

		} catch (Exception e) {

			throw new AnalysisEngineProcessException(e);

		}

	}

}
