package edu.isi.bmkeg.skm.triage.uima.cpe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.digitalLibrary.dao.vpdmf.VpdmfCitationsDao;
import edu.isi.bmkeg.skm.triage.cleartk.bin.PreprocessTriageScores;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;
import edu.isi.bmkeg.vpdmf.test.VPDMfTestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class PreprocessTriageDocumentsTest {
	
	ApplicationContext ctx;
	
	String login, password, dbUrl, triageCorpusName, targetCorpusName;
	File  outputDir;
	
	VpdmfCitationsDao dao;
		
	@Before
	public void setUp() throws Exception {
		
		ctx = AppContext.getApplicationContext();
		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");

		login = prop.getDbUser();
		password = prop.getDbPassword();
		dbUrl = prop.getDbUrl()+"_triage"; // Database created by test AA_CreateTriageDBTest

		int l = dbUrl.lastIndexOf("/");
		if (l != -1)
			dbUrl = dbUrl.substring(l + 1, dbUrl.length());

		triageCorpusName = "Small";
		targetCorpusName = "AP";
		
		outputDir = new File("target/mgi/small");
		if (outputDir.exists()) {
			Converters.recursivelyDeleteFiles(outputDir);
		}
		outputDir.mkdirs();
	}

	@After
	public void tearDown() throws Exception {
				
	}
	
	@Test
	public final void testPreprocessTriageDocuments() throws Exception {
		
		String[] args = new String[] { 
				"-triageCorpus", triageCorpusName, 
				"-targetCorpus", targetCorpusName, 
				"-dir", outputDir.getAbsolutePath(), 
				"-prop", "0.50", 
				"-l", login, 
				"-p", password, 
				"-db", dbUrl 
				};

		PreprocessTriageScores.main(args);
		
		// checks output files include all instances
		
		File trainIn = new File(outputDir, targetCorpusName + "/" + triageCorpusName + "/train/in.txt");
		File trainOut = new File(outputDir, targetCorpusName + "/" + triageCorpusName + "/train/out.txt");
		File testIn = new File(outputDir, targetCorpusName + "/" + triageCorpusName + "/test/in.txt");
		File testOut = new File(outputDir, targetCorpusName + "/" + triageCorpusName + "/test/out.txt");

		int cnt = 0;
		if (trainIn.exists()) cnt += countLines(trainIn);
		if (trainOut.exists()) cnt += countLines(trainOut);
		if (testIn.exists()) cnt += countLines(testIn);
		if (testOut.exists()) cnt += countLines(testOut);
		
		Assert.assertEquals(5, cnt);
						
	}
	
	private int countLines(File instancesFile) throws IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(instancesFile));
		int cnt = 0;
		while (reader.readLine() != null) cnt++;
		return cnt;
	}
		
}

