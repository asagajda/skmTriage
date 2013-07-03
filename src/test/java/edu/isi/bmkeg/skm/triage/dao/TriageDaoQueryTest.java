package edu.isi.bmkeg.skm.triage.dao;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.triage.model.TriageScore;
import edu.isi.bmkeg.triage.model.qo.TriageCorpus_qo;
import edu.isi.bmkeg.triage.model.qo.TriageScore_qo;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class TriageDaoQueryTest {
ApplicationContext ctx;
	
	String login, password, dbUrl;
	File archiveFile, pmidFile_allChecked, triageCodes, pdfDir;
	VPDMfKnowledgeBaseBuilder builder;
	
	TriageEngine te;
	
	String queryString;
	
	@Before
	public void setUp() throws Exception {
		
		ctx = AppContext.getApplicationContext();
		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");

		login = prop.getDbUser();
		password = prop.getDbPassword();
		dbUrl = prop.getDbUrl();
		String wd = prop.getWorkingDirectory();
		
		pdfDir = new File( wd + "/pdfs" );
		
		int l = dbUrl.lastIndexOf("/");
		if (l != -1)
			dbUrl = dbUrl.substring(l + 1, dbUrl.length());

		archiveFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/triage_data_VPDMf.zip").getFile();

		/*builder = new VPDMfKnowledgeBaseBuilder(archiveFile, 
				login, password, dbUrl); 
		
		try {
			
			builder.destroyDatabase(dbUrl);
	
		} catch (SQLException sqlE) {		
			
			// Gully: Make sure that this runs, avoid silly issues.
			if( !sqlE.getMessage().contains("database doesn't exist") ) {
				sqlE.printStackTrace();
			}
			
		} 

		builder.buildDatabaseFromArchive();*/
		
		te = new TriageEngine();
		te.initializeVpdmfDao(login, password, dbUrl);
	
	}

	@After
	public void tearDown() throws Exception {
		
//		builder.destroyDatabase(dbUrl);
		
	}
	
	@Test // @Ignore("Fails")
	public final void testDaoQueryList() throws Exception {

		TriageScore_qo td = new TriageScore_qo();
		TriageCorpus_qo tc = new TriageCorpus_qo();
		td.setTriageCorpus(tc);
		tc.setName("mgiBase");
	
		List<LightViewInstance> l = te.getExTriageDao().getCoreDao().list(td, "TriagedDocumentList"); 

		int i = 0;
		i++;
		
	}
		
	@Test // @Ignore("Fails")
	public final void testDaoQueryRetrieve() throws Exception {

		TriageScore td = new TriageScore();
		TriageCorpus tc = new TriageCorpus();
		td.setTriageCorpus(tc);
		tc.setName("mgiBase");
	
		List<TriageScore> l = te.getExTriageDao().getCoreDao().retrieve(td, "TriagedDocument"); 

		int i = 0;
		i++;
		
	}
	
	@Test // @Ignore("Fails")
	public final void testDaoQueryFindById() throws Exception {

		TriageScore td = new TriageScore();
	
		TriageScore tdRet = te.getExTriageDao().getCoreDao().findById(26992L, td, "TriagedArticle"); 

		int i = 0;
		i++;
		
	}
	
	@Test // @Ignore("Fails")
	public final void testDaoQueryUpdate() throws Exception {

		TriageScore td = new TriageScore();
	
		TriageScore tdRet = te.getExTriageDao().getCoreDao().findById(26992L, td, "TriagedArticle"); 

		tdRet.setInOutCode("in");

		long vpdmfId = te.getExTriageDao().getCoreDao().update(tdRet, "TriagedArticle");
		
		int i = 0;
		i++;
		
	} 
	
	@Test // @Ignore("Fails")
	public final void testDaoQueryCount() throws Exception {

		TriageScore_qo td = new TriageScore_qo();
		TriageCorpus_qo tc = new TriageCorpus_qo();
		tc.setName("arg");
		td.setTriageCorpus(tc);
	
		int count = te.getExTriageDao().getCoreDao().countView(td, "TriagedArticle"); 
		
		assert(count == 0);
		
		tc.setName("mgiBase");
		
		count = te.getExTriageDao().getCoreDao().countView(td, "TriagedArticle"); 

		assert(count == 24);

		
	}
		
}

