package edu.isi.bmkeg.skm.triage.uima.cpe;

import static java.util.Arrays.asList;

import java.io.File;
import java.sql.SQLException;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.util.CasUtil;

import edu.isi.bmkeg.digitalLibrary.bin.EditArticleCorpus;
import edu.isi.bmkeg.digitalLibrary.dao.ExtendedDigitalLibraryDao;
import edu.isi.bmkeg.skm.triage.bin.BuildTriageCorpusFromPdfDir;
import edu.isi.bmkeg.skm.triage.bin.EditTriageCorpus;
import edu.isi.bmkeg.skm.triage.cleartk.cr.TriageScoreCollectionReader;
import edu.isi.bmkeg.triage.uimaTypes.TriageScore;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

/**
 * This unit test generates a populated testing DB used by other unit tests
 * and hast to be executed before them.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class AA_CreateTriageDBTest {
	
	private static String test_triage_corpus_name = "Small"; 
	private static String test_target_A_corpus_name = "AP"; 
	private static int test_corpus_A_in_triage_cnt = 3; 
	private static int test_corpus_A_out_triage_cnt = 2; 
	private static int test_corpus_A_unknown_triage_cnt = 0; 
	private static int test_corpus_A_in_all_cnt = 5; 
	private static int test_corpus_A_out_all_cnt = 3; 
	private static int test_corpus_A_unknown_all_cnt = 1; 
	private static int test_corpus_no_doc_cnt = 0; 
	
	ApplicationContext ctx;
	
	@Autowired
	private BmkegProperties prop;
	
	String login, password, dbUrl, wd;
	File archiveFile, pmidFile_allChecked, triageCodes, pdfDir, pdfDir2, pdfDir3;
	VPDMfKnowledgeBaseBuilder builder;
	
	ExtendedDigitalLibraryDao dao;
	
	@Before
	public void setUp() throws Exception {
		
		ctx = AppContext.getApplicationContext();
		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");

		login = prop.getDbUser();
		password = prop.getDbPassword();
		dbUrl = prop.getDbUrl()+"_triage";
		wd = prop.getWorkingDirectory();
		triageCodes = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/small/triageCodes.txt").getFile();
		
		pdfDir = new File(triageCodes.getParentFile(), "/pdfs" );
		
		pdfDir2 = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/small2/pdfs/21884797_A.pdf").getFile().getParentFile();
				
		pdfDir3 = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/triage/small3/pdfs/21989724.pdf").getFile().getParentFile();
				
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
				"-p", password,
				"-wd", wd
				};

		EditArticleCorpus.main(args);

		String targetCorpusName2 = "GO";

		args = new String[] { 
				"-name", targetCorpusName2, 
				"-desc", "The primary triage corpus for GO", 
				"-regex", "G", 
				"-owner", "MGI",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password,
				"-wd", wd
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
				"-wd", wd
				};

		EditTriageCorpus.main(args);

		String triageCorpusName2 = "Small2";
		
		args = new String[] { 
				"-name", triageCorpusName2, 
				"-desc", "Test triage corpus 2", 
				"-owner", "john",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password,
				"-wd", wd 
				};

		EditTriageCorpus.main(args);

		String triageCorpusName3 = "Small3";
		
		args = new String[] { 
				"-name", triageCorpusName3, 
				"-desc", "Test triage corpus 3", 
				"-owner", "peter",
				"-db", dbUrl, 
				"-l", login, 
				"-p", password,
				"-wd", wd 
				};

		EditTriageCorpus.main(args);

		args = new String[] { 
				"-pdfs", pdfDir.getPath(), 
				"-triageCorpus", triageCorpusName, 
				"-codeList", triageCodes.getPath(), 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password,
				"-wd", wd
				};

		BuildTriageCorpusFromPdfDir.main(args);
		
		args = new String[] { 
				"-pdfs", pdfDir2.getPath(), 
				"-triageCorpus", triageCorpusName2, 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password,
				"-wd", wd
				};

		BuildTriageCorpusFromPdfDir.main(args);
		
		args = new String[] { 
				"-pdfs", pdfDir3.getPath(), 
				"-triageCorpus", triageCorpusName3, 
				"-db", dbUrl, 
				"-l", login, 
				"-p", password,
				"-wd", wd
				};

		BuildTriageCorpusFromPdfDir.main(args);
		
	}
		
	@Test
	public void testTriagedDocumentCollectionReader_corpusA_triage() throws Exception {
		
		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("uimaTypes.vpdmf-triage",
						"edu.isi.bmkeg.skm.cleartk.TypeSystem");
		
		CollectionReader cr = CollectionReaderFactory.createCollectionReader(
				TriageScoreCollectionReader.class, typeSystem, 
				TriageScoreCollectionReader.TRIAGE_CORPUS_NAME, test_triage_corpus_name,
				TriageScoreCollectionReader.TARGET_CORPUS_NAME, test_target_A_corpus_name,
				TriageScoreCollectionReader.LOGIN, prop.getDbUser(),
				TriageScoreCollectionReader.PASSWORD, prop.getDbPassword(), 
				TriageScoreCollectionReader.DB_URL,prop.getDbUrl()+ "_triage",
				TriageScoreCollectionReader.WORKING_DIRECTORY,prop.getWorkingDirectory() 
				);
		

		int inCnt = 0;
		int outCnt = 0;
		int unknownCnt = 0;
		int noDocCnt = 0;
		
		final CAS cas = CasCreationUtils.createCas(asList(cr.getMetaData()));
		
		try {
			// Process
			while (cr.hasNext()) {
				cr.getNext(cas);
				
				String doc = cas.getDocumentText();
				
				if (doc == null || doc.length() == 0)
					noDocCnt++;

				TriageScore cit = (TriageScore) CasUtil.selectSingle(cas, CasUtil.getType(cas, TriageScore.class));
				Assert.assertNotNull(cit);
				Assert.assertTrue(cit.getCitation_id() > 0);
				Assert.assertTrue(cit.getVpdmfId() > 0);
				Assert.assertNotNull(cit.getInOutCode());
				
				if (cit.getInOutCode().equals("in")) inCnt++; 
				else if (cit.getInOutCode().equals("out")) outCnt++; 
				else unknownCnt++;
				cas.reset();
			}
			
		}
		finally {
			// Destroy
			cr.destroy();
		}
		
		/*Assert.assertEquals(test_corpus_A_unknown_triage_cnt, unknownCnt);				
		Assert.assertEquals(test_corpus_A_in_triage_cnt, inCnt);				
		Assert.assertEquals(test_corpus_A_out_triage_cnt, outCnt);				
		Assert.assertEquals(test_corpus_no_doc_cnt, noDocCnt);*/

	}
	
}

