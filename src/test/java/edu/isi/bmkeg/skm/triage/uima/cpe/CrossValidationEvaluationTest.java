package edu.isi.bmkeg.skm.triage.uima.cpe;

import java.io.File;
import java.net.URL;

import org.cleartk.ml.libsvm.LibSvmBooleanOutcomeDataWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_BigramCount;
import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_Uni_and_BigramCount;
import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_UnigramCount;
import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.SearchTermsBaselineEvaluation;
import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.TriageBaselineEvaluation;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

public class CrossValidationEvaluationTest {
	
	File archiveFile, pmidFile_allChecked, triageCodes, pdfDir;
	VPDMfKnowledgeBaseBuilder builder;
	
	File dataDir, outputDir;
	
	String queryString;
	
	@Before
	public void setUp() throws Exception {
		
		URL u = this.getClass().getClassLoader().getResource(
				"edu/isi/bmkeg/skm/triage/small/instances/data");	

		dataDir = new File(u.getPath());
		outputDir = new File("target/document_classification");
		Converters.recursivelyDeleteFiles(outputDir);

	}

	@After
	public void tearDown() throws Exception {
		
	}
	
	@Test
	public final void testTrainUnigram() throws Exception {
		
		String[] args = new String[] { 
				"-data", dataDir.getPath(), 
				"-nFolds", "3", 
				"-base", outputDir.getPath(),
				"-data-writer", LibSvmBooleanOutcomeDataWriter.class.getName()
				};

		CrossValEval_UnigramCount.main(args);
		
	}

	@Test
	public final void testTrainUnigramLiblinear() throws Exception {
		
		String[] args = new String[] { 
				"-data", dataDir.getPath(), 
				"-nFolds", "3", 
				"-base", outputDir.getPath(),
//				"-data-writer", LibLINEARBooleanOutcomeDataWriter.class.getName(),
				"-training-args", "-s", "-training-args", "1"
				};

		CrossValEval_UnigramCount.main(args);
		
	}

	@Test
	public final void testTriageBaseline() throws Exception {
		
		String[] args = new String[] { 
				"-data", dataDir.getPath(), 
				"-nFolds", "3", 
				"-base", outputDir.getPath()
				};

		TriageBaselineEvaluation.main(args);
		
	}

	@Test
	public final void testSearchTermBaseline() throws Exception {
		
		String[] args = new String[] { 
				"-data", dataDir.getPath(), 
				"-nFolds", "3", 
				"-base", outputDir.getPath()
				};

		SearchTermsBaselineEvaluation.main(args);
		
	}

	@Test 
	public final void testTrainBigram() throws Exception {
		
		String[] args = new String[] { 
				"-data", dataDir.getPath(), 
				"-nFolds", "3", 
				"-base", outputDir.getPath()
				};

		CrossValEval_BigramCount.main(args);
		
	}

	@Test
	public final void testTrainUnigramBigram() throws Exception {
		
		String[] args = new String[] { 
				"-data", dataDir.getPath(), 
				"-nFolds", "3", 
				"-base", outputDir.getPath()
				};

		CrossValEval_Uni_and_BigramCount.main(args);
		
	}
	
}

