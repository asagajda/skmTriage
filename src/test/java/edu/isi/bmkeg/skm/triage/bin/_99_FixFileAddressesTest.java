package edu.isi.bmkeg.skm.triage.bin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.dao.ExtendedDigitalLibraryDao;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.ArticleCitation_qo;
import edu.isi.bmkeg.ftd.model.qo.FTD_qo;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
public class _99_FixFileAddressesTest {
	
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
		dbUrl = "sciknowmine_test4";
		wd = prop.getWorkingDirectory();
		
		pdfDir = new File( wd + "/pdfs" );
		
		int l = dbUrl.lastIndexOf("/");
		if (l != -1)
			dbUrl = dbUrl.substring(l + 1, dbUrl.length());
					
	}

	@After
	public void tearDown() throws Exception {	
	}
	
	@Test
	public final void testfixFileAddresses() throws Exception {
		
		/*DigitalLibraryEngine de = new DigitalLibraryEngine();
		de.initializeVpdmfDao(login, password, dbUrl, wd);
		
		CoreDao dao = de.getDigLibDao().getCoreDao();

		try {

			VPDMf top = dao.getTop();

			dao.connectToDb();
			
			FTD_qo ftdQo = new FTD_qo();
			ArticleCitation_qo acQo = new ArticleCitation_qo();
			acQo.setFullText(ftdQo);
			
			Map<Long, String> locMap = new HashMap<Long, String>();
			List<LightViewInstance> tsList = dao.listInTrans(ftdQo, "ArticleCitationDocument"); 
			for( LightViewInstance lvi : tsList) {

				Map<String, String> itm = lvi.readIndexTupleMap(top);
				
				itm.get("[ArticleCitation]LiteratureCitation|ArticleCitation.pmid");

				Long vpdmfId = lvi.getVpdmfId();
				String jAbbr = itm.get("[ArticleCitation]JournalLU|Journal.abbr");
				String year = itm.get("[ArticleCitation]LiteratureCitation|LiteratureCitation.pubYear");
				String vol = itm.get("[ArticleCitation]LiteratureCitation|ArticleCitation.volume");
				String pmid = itm.get("[ArticleCitation]LiteratureCitation|ArticleCitation.pmid");
				
				jAbbr = jAbbr.replaceAll("\\s+", "_");
				String stem = jAbbr + "/" + year + "/" + vol + "/" + pmid;

				File pdfFile = new File(wd + "/pdfs/" + stem + ".pdf");
				File swfFile = new File(wd + "/pdfs/" + stem + ".swf");
				File lapdfFile = new File(wd + "/pdfs/" + stem + "_lapdf.xml");
				File pmcFile = new File(wd + "/pdfs/" + stem + "_pmc.xml");
				
				String sql = "UPDATE FTD,LiteratureCitation SET " + 
						"ftd.name = '" + stem + ".pdf', ftd.pdfLoaded = " + pdfFile.exists() + 
						", ftd.pmcXmlFile='" + stem + "_pmc.xml', ftd.pmcLoaded = " + pmcFile.exists() + 
						", ftd.laswfFile='" + stem + ".swf', ftd.swfLoaded = " + swfFile.exists() + 
						", ftd.xmlFile='" + stem + "_lapdf.xml', ftd.xmlLoaded = " + lapdfFile.exists() +
						" WHERE LiteratureCitation.vpdmfId = " + vpdmfId + " AND " +
						" LiteratureCitation.fullText_id = FTD.vpdmfId";

				dao.getCe().executeRawUpdateQuery(sql);
				
			}
			
			dao.commitTransaction();
			
		} catch (Exception e) {
		
			e.printStackTrace();
			
		} finally {
			
			dao.closeDbConnection();
		
		}*/
		
		
	}
	
}

