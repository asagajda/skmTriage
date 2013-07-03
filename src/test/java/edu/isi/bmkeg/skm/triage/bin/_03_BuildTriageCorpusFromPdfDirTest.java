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
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class _03_BuildTriageCorpusFromPdfDirTest {
	
	ApplicationContext ctx;
	
	String login, password, dbUrl;
	String corpusName;
	File archiveFile, pmidFile_allChecked, triageCodes, pdfDir;
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

		File pdf1 = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/mgi/pdfs/19763139.pdf").getFile();

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
				
		corpusName = "TriageCorpus";
		
		String[] args = new String[] { 
				"-name", corpusName, 
				"-desc", "Test triage corpus", 
				"-owner", "Gully Burns",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password 
				};

		EditTriageCorpus.main(args);
		
	}

	@After
	public void tearDown() throws Exception {
		
		builder.destroyDatabase(dbUrl);
		
	}
	
	@Test
	public final void testBuildTriageCorpusFromScratch() throws Exception {

		String[] args = new String[] { 
				"-pdfs", pdfDir.getPath(), 
				"-corpus", corpusName, 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password
				};

		AddPmidEncodedPdfsToCorpus.main(args);
				
						
	}
		
}

