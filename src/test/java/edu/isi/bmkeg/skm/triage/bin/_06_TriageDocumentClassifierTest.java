package edu.isi.bmkeg.skm.triage.bin;

import java.io.File;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.digitalLibrary.bin.AddPmidEncodedPdfsToCorpus;
import edu.isi.bmkeg.digitalLibrary.bin.EditArticleCorpus;
import edu.isi.bmkeg.digitalLibrary.dao.vpdmf.VpdmfCitationsDao;
import edu.isi.bmkeg.skm.triage.cleartk.bin.PreprocessTriageScores;
import edu.isi.bmkeg.skm.triage.cleartk.bin.TriageDocumentsClassifier;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class _06_TriageDocumentClassifierTest {
ApplicationContext ctx;
	
	String login, password, dbUrl;
	String triageCorpusName, targetCorpusName;
	File archiveFile, pmidFile_allChecked, triageCodes, pdfDir, codeFile, outDir;
	VPDMfKnowledgeBaseBuilder builder;
	
	VpdmfCitationsDao dao;
	
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
		
		codeFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/mgi/testCodes.txt").getFile();

		outDir = codeFile.getParentFile();
		
		File pdf1 = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/mgi/pdfs/19763139_A.pdf").getFile();

		pdfDir = pdf1.getParentFile();
		if( !pdfDir.exists() ) {
			throw new Exception("WorkingDirectory:" + pdfDir.getPath() + "/pdf does not exist");
		}
		
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
				
		triageCorpusName = "TriageCorpus";
		
		String[] args = new String[] { 
				"-name", triageCorpusName, 
				"-desc", "Test triage corpus", 
				"-owner", "Gully Burns",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password 
				};

		EditTriageCorpus.main(args);

		targetCorpusName = "TargetCorpus";
		args = new String[] { 
				"-name", targetCorpusName, 
				"-desc", "Test target corpus", 
				"-owner", "Gully Burns",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password 
				};

		EditArticleCorpus.main(args);
		
		args = new String[] { 
				"-pdfs", pdfDir.getPath(), 
				"-corpus", triageCorpusName, 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};

		AddPmidEncodedPdfsToCorpus.main(args);
		
		args = new String[] { 
				"-triageCorpus", triageCorpusName, 
				"-targetCorpus", targetCorpusName, 
				"-pmidCodes", codeFile.getPath(), 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};

		BuildTriageCorpusFromPmidList.main(args);
		
	}

	@After
	public void tearDown() throws Exception {
		
		builder.destroyDatabase(dbUrl);
		
	}
		
	@Test
	public final void testBuildTriageCorpusFromScratch() throws Exception {

		String[] args = new String[] {
				"-train",
				"-triageCorpus", triageCorpusName, 
				"-targetCorpus", targetCorpusName, 
				"-modelDir", outDir.getAbsolutePath(), 
				"-l", login, 
				"-p", password, 
				"-db", dbUrl
				};

		TriageDocumentsClassifier.main(args);
		
		args[0] = "-predict";

		TriageDocumentsClassifier.main(args);

						
	}
		
}

