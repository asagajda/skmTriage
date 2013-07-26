package edu.isi.bmkeg.skm.triage.bin;

import java.io.File;
import java.sql.SQLException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.digitalLibrary.bin.EditArticleCorpus;
import edu.isi.bmkeg.digitalLibrary.dao.vpdmf.VpdmfCitationsDao;
import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class _08_RunDeletionFunctionsTest {
	
	ApplicationContext ctx;
	
	String login, password, dbUrl;
	String corpusName;
	File archiveFile, pmidFile_allChecked, triageCodes, pdfDir, pdfDir2;
	VPDMfKnowledgeBaseBuilder builder;
	TriageEngine te;
	VpdmfCitationsDao dao;
	
	String queryString;
	
	// Cheat flag for debugging
	private boolean rebuildDb = false;
	
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

		File pdf1 = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/small/pdfs/19763139_A.pdf").getFile();
		pdfDir = pdf1.getParentFile();
		triageCodes = new File(pdfDir.getParent() + "/triageCodes.txt");

		File pdf2 = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/small3/pdfs/21884797.pdf").getFile();
		pdfDir2 = pdf2.getParentFile();

		builder = new VPDMfKnowledgeBaseBuilder(archiveFile, 
				login, password, dbUrl); 

		if( rebuildDb ) {
			try {
				
				builder.destroyDatabase(dbUrl);
		
			} catch (SQLException sqlE) {		
				
				// Gully: Make sure that this runs, avoid silly issues.
				if( !sqlE.getMessage().contains("database doesn't exist") ) {
					sqlE.printStackTrace();
				}
				
			} 
			
			builder.buildDatabaseFromArchive();
		}
	
		te = new TriageEngine();
		te.initializeVpdmfDao(login, password, dbUrl);
			
		corpusName = "TriageCorpus";
	
		if( rebuildDb ) {
			String[] args = new String[] { 
					"-name", "AP", 
					"-desc", "Test AP article corpus",  
					"-regex", "A", 
					"-owner", "Gully Burns",
					"-db", dbUrl, 
					"-l", login, 
					"-p", password 
					};
	
			EditArticleCorpus.main(args);
			
			args = new String[] { 
					"-name", "GO", 
					"-desc", "Test GO article corpus",  
					"-regex", "G", 
					"-owner", "Gully Burns",
					"-db", dbUrl, 
					"-l", login, 
					"-p", password 
					};
	
			EditArticleCorpus.main(args);
			
			args = new String[] { 
					"-name", corpusName, 
					"-desc", "Test triage corpus", 
					"-owner", "Gully Burns",
					"-db", dbUrl, 
					"-l", login, 
					"-p", password 
					};
	
			EditTriageCorpus.main(args);
			
			args = new String[] { 
					"-pdfs", pdfDir.getPath(), 
					"-triageCorpus", corpusName, 
					"-db", dbUrl, 
					"-l", login, 
					"-p", password
					};
	
			BuildTriageCorpusFromPdfDir.main(args);
			
			args = new String[] { 
					"-pdfs", pdfDir2.getPath(), 
					"-triageCorpus", corpusName, 
					"-db", dbUrl, 
					"-l", login, 
					"-p", password
					};
	
			BuildTriageCorpusFromPdfDir.main(args);

		}
		
	}

	@After
	public void tearDown() throws Exception {
		
		if(rebuildDb)
			builder.destroyDatabase(dbUrl);
		
	}
		
/*	@Test
	public final void testDeleteTargetCorpus() throws Exception {

		int c1 = te.getCitDao().getCoreDao().countView("TriagedArticle");

		String[] args = new String[] { 
				"-targetCorpus", "GO", 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};
		
		DeleteTargetCorpus.main(args);
				
		int c2 = te.getCitDao().getCoreDao().countView("TriagedArticle");
		
		Assert.assertEquals(16, c1);
		Assert.assertEquals(8, c2);
	}

	@Test
	public final void testDeleteTriageCorpus() throws Exception {

		int c1 = te.getCitDao().getCoreDao().countView("TriageScore");

		String[] args = new String[] { 
				"-triageCorpus", corpusName, 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};
		
		DeleteTriageCorpus.main(args);
				
		int c2 = te.getCitDao().getCoreDao().countView("TriageScore");
		
		Assert.assertEquals(16, c1);
		Assert.assertEquals(0, c2);
	}*/
	
	@Test
	public final void testRunDeleteFromCodes() throws Exception {

		int c1 = te.getCitDao().getCoreDao().countView("TriageScore");

		String[] args = new String[] { 
				"-triageCorpus", corpusName, 
				"-codeList", triageCodes.getPath(),
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};
		
		DeleteTriageScoresFromCodeFile.main(args);
				
		int c2 = te.getCitDao().getCoreDao().countView("TriageScore");
		
		Assert.assertEquals(16, c1);
		Assert.assertEquals(0, c2);
	}
		
}

