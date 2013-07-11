package edu.isi.bmkeg.skm.triage;

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
import edu.isi.bmkeg.skm.triage.bin.BuildTriageCorpusFromPdfDir;
import edu.isi.bmkeg.skm.triage.bin.BuildTriageCorpusFromPmidList;
import edu.isi.bmkeg.skm.triage.bin.EditTriageCorpus;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class AA_CreateTriageDBTest {
	
	ApplicationContext ctx;
	
	String login, password, dbUrl;
	File archiveFile, pmidFile_allChecked, triageCodes, pdfDir;
	VPDMfKnowledgeBaseBuilder builder;
	
	VpdmfCitationsDao dao;
	
	@Before
	public void setUp() throws Exception {
		
		ctx = AppContext.getApplicationContext();
		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");

		login = prop.getDbUser();
		password = prop.getDbPassword();
		dbUrl = prop.getDbUrl()+"_triage";
		triageCodes = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/small/triageCodes.txt").getFile();
		
		pdfDir = new File(triageCodes.getParentFile(), "/pdfs" );
		
		int l = dbUrl.lastIndexOf("/");
		if (l != -1)
			dbUrl = dbUrl.substring(l + 1, dbUrl.length());
	
		archiveFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/triage-mysql.zip").getFile();

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
		
	}

	@After
	public void tearDown() throws Exception {
		return;
	}
	
	@Test
	public final void testBuildTriageCorpusFromScratch() throws Exception {

		String targetCorpusName = "AP";

		String[] args = new String[] { 
				"-name", targetCorpusName, 
				"-desc", "The primary triage corpus for AP", 
				"-regex", "A", 
				"-owner", "MGI",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};

		EditArticleCorpus.main(args);
				
		String triageCorpusName = "Small";
		
		args = new String[] { 
				"-name", triageCorpusName, 
				"-desc", "Test triage corpus", 
				"-owner", "rocky",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password, 
				};

		EditTriageCorpus.main(args);

		args = new String[] { 
				"-pdfs", pdfDir.getPath(), 
				"-corpus", triageCorpusName, 
				"-codeList", triageCodes.getPath(), 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};

		BuildTriageCorpusFromPdfDir.main(args);
		
	}
		
}

