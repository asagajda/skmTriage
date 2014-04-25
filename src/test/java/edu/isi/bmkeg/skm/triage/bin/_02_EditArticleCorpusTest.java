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

import edu.isi.bmkeg.digitalLibrary.bin.EditArticleCorpus;
import edu.isi.bmkeg.digitalLibrary.dao.ExtendedDigitalLibraryDao;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class _02_EditArticleCorpusTest {
	
	ApplicationContext ctx;
	
	String login, password, dbUrl, wd;
	File archiveFile, pmidFile_allChecked, triageCodes, pdfDir;
	VPDMfKnowledgeBaseBuilder builder;
	
	ExtendedDigitalLibraryDao dao;
	
	String queryString;
	
	@Before
	public void setUp() throws Exception {
		
		ctx = AppContext.getApplicationContext();
		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");

		login = prop.getDbUser();
		password = prop.getDbPassword();
		dbUrl = prop.getDbUrl();
		wd = prop.getWorkingDirectory();
		
		pdfDir = new File( wd + "/pdfs" );
		
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
		
		builder.destroyDatabase(dbUrl);
		
	}
	
	@Test
	public final void testBuildArticleCorpus() throws Exception {

		String[] args = new String[] { 
				"-name", "Allele Phenotype", 
				"-desc", "MGI Allele Phenotype", 
				"-regex", "_(.*A.*)\\.pdf",
				"-owner", "Gully Burns",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password,
				"-wd", wd
				};

		EditArticleCorpus.main(args);
						
	}
		
	@Test
	public final void testBuildArticleCorpusAndEditIt() throws Exception {

		String[] args = new String[] { 
				"-name", "Allele Phenotype", 
				"-desc", "CHANGE THIS", 
				"-regex", "_(.*A.*)\\.pdf",
				"-owner", "CHANGE THIS",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password,
				"-wd", wd 
				};

		EditArticleCorpus.main(args);

		args = new String[] { 
				"-name", "Allele Phenotype", 
				"-desc", "MGI Allele Phenotype", 
				"-regex", "_(.*A.*)\\.pdf",
				"-owner", "Gully Burns",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password,
				"-wd", wd
				};

		EditArticleCorpus.main(args);
		
	}
	
}

