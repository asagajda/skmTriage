package edu.isi.bmkeg.skm.triage.bin;

import java.io.File;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.digitalLibrary.bin.EditArticleCorpus;
import edu.isi.bmkeg.digitalLibrary.dao.ExtendedDigitalLibraryDao;
import edu.isi.bmkeg.skm.triage.cleartk.bin.TriageDocumentsClassifier;
import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.triage.model.qo.TriageScore_qo;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class MGIBugTest {
ApplicationContext ctx;
	
	String login, password, dbUrl;
	String origUserHomeProp;
	File archiveFile, pmidFile_allChecked, triageCodes, pdfDir, pdfDir2, pdfDir3, outDir;
	VPDMfKnowledgeBaseBuilder builder;
	TriageEngine te;
	ExtendedDigitalLibraryDao dao;
	
	String queryString;
	
	@Before
	public void setUp() throws Exception {
		
		ctx = AppContext.getApplicationContext();
		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");

		login = prop.getDbUser();
		password = prop.getDbPassword();
		dbUrl = prop.getDbUrl();
		String wd = prop.getWorkingDirectory();
		
		int l = dbUrl.lastIndexOf("/");
		if (l != -1)
			dbUrl = dbUrl.substring(l + 1, dbUrl.length());
	
		archiveFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/triage-mysql.zip").getFile();
		
		outDir = new File("target");
		
		File pdf1 = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/small/pdfs/19763139_A.pdf").getFile();
		pdfDir = pdf1.getParentFile();
		triageCodes = new File(pdfDir.getParent() + "/triageCodes.txt");

		File pdf2 = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/small2/pdfs/21306482.pdf").getFile();
		pdfDir2 = pdf2.getParentFile();

		File pdf3 = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/small3/pdfs/21884797.pdf").getFile();
		pdfDir3 = pdf3.getParentFile();

		builder = new VPDMfKnowledgeBaseBuilder(archiveFile, 
				login, password, dbUrl); 

		try {
			
			builder.destroyDatabase(dbUrl);
	
		} catch (SQLException sqlE) {		
			
			// Gully: Make sure that this runs, avoid silly issues.
			if( !sqlE.getMessage().contains("database doesn't exist") ) {
				sqlE.printStackTrace();
			}
			
		} 
		
		builder.buildDatabaseFromArchive();
		
		te = new TriageEngine();
		te.initializeVpdmfDao(login, password, dbUrl);

		origUserHomeProp = System.getProperty("user.home");
		File homeDir = new File(outDir, "userHome");	
		System.setProperty("user.home", homeDir.getAbsolutePath());
				
	}

	@After
	public void tearDown() throws Exception {
		
//		builder.destroyDatabase(dbUrl);
		
		if (origUserHomeProp != null) {
			System.setProperty("user.home", origUserHomeProp);				
		}		
		
	}
		
	@Test
	public final void testMGIBug() throws Exception {

		// Create target corpora
		
		String targetCorpus = "AP";
		
		String[] args = new String[] { 
				"-name", targetCorpus, 
				"-desc", "AP corpus", 
				"-regex", "A", 
				"-owner", "Gully Burns",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password 
				};

		EditArticleCorpus.main(args);

		// Create first triage corpus
		
		String triageCorpusName = "triageCorpus1";
		args = new String[] { 
				"-name", triageCorpusName, 
				"-desc", triageCorpusName + " corpus", 
				"-owner", "Gully Burns",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password 
				};

		EditTriageCorpus.main(args);

		args = new String[] { 
				"-pdfs", pdfDir.getPath(), 
				"-triageCorpus", triageCorpusName, 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};

		BuildTriageCorpusFromPdfDir.main(args);

		int count = te.getDigLibDao().getCoreDao().countView(new TriageScore_qo(), "TriageScore");
		Assert.assertEquals(5, count);

		// Train model

		args = new String[] {
				"-train",
				"-targetCorpus", targetCorpus, 
				"-l", login, 
				"-p", password, 
				"-db", dbUrl
				};

		TriageDocumentsClassifier.main(args);
		
		// Add more PDFs to first triage corpus
		
		args = new String[] { 
				"-pdfs", pdfDir2.getPath(), 
				"-triageCorpus", triageCorpusName, 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};

		BuildTriageCorpusFromPdfDir.main(args);		
		
		count = te.getDigLibDao().getCoreDao().countView(new TriageScore_qo(), "TriageScore");
		Assert.assertEquals(8, count);

		// Scores first triage corpus

		args = new String[] {
				"-predict",
				"-targetCorpus", targetCorpus,
				"-triageCorpus", triageCorpusName,
				"-l", login, 
				"-p", password, 
				"-db", dbUrl
				};

		TriageDocumentsClassifier.main(args);

		// Create second triage corpus
		
		triageCorpusName = "triageCorpus2";
		args = new String[] { 
				"-name", triageCorpusName, 
				"-desc", triageCorpusName + " corpus", 
				"-owner", "Gully Burns",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password 
				};

		EditTriageCorpus.main(args);
		
		// Add PDFs to second triage corpus
		
		args = new String[] { 
				"-pdfs", pdfDir3.getPath(), 
				"-triageCorpus", triageCorpusName, 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};

		BuildTriageCorpusFromPdfDir.main(args);

		count = te.getDigLibDao().getCoreDao().countView(new TriageScore_qo(), "TriageScore");
		Assert.assertEquals(11, count);

	}
	
}

