package edu.isi.bmkeg.skm.triage.bin;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.ArticleCitation_qo;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;
import edu.isi.bmkeg.triage.model.qo.TriageScore_qo;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class ReportTargetCorpusContents  {

	public static class Options extends Options_ImplBase {

		@Option(name = "-targetCorpus", usage = "Target Corpus Name", required = true, metaVar = "CNAME")
		public String targetCorpusName = "";
		
		@Option(name = "-l", usage = "Database login", required = true, metaVar = "LOGIN")
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true, metaVar = "PASSWD")
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true, metaVar = "DBNAME")
		public String dbName = "";

		@Option(name = "-wd", usage = "Working directory", required = true, metaVar  = "WDIR")
		public String workingDirectory = "";
		
	}

	private static Logger logger = Logger.getLogger(DeleteTriageCorpus.class);

	private VPDMf top;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();

		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);

		DigitalLibraryEngine de = new DigitalLibraryEngine();
		de.initializeVpdmfDao(
				options.login, options.password, 
				options.dbName, options.workingDirectory);
		
		CoreDao dao = de.getDigLibDao().getCoreDao();

		try {

			VPDMf top = dao.getTop();

			dao.connectToDb();
			
			TriageScore_qo tsQo = new TriageScore_qo();
			tsQo.setInOutCode("in");
			Corpus_qo cQo = new Corpus_qo();
			cQo.setName(options.targetCorpusName);
			tsQo.setTargetCorpus(cQo);				
			ArticleCitation_qo acQo = new ArticleCitation_qo();
			tsQo.setCitation(acQo);
			acQo.setPmid("<vpdmf-sort-0>");

			System.out.println("\nSciKnowMine Corpus Composition");
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

			System.out.println("Database: " + options.dbName);
			System.out.println("TargetCorpus: " + options.targetCorpusName );
						
			System.out.println(String.format("%-9s %s", 
					"PMID", "CITATION"));

			List<LightViewInstance> tsList = dao.listInTrans(tsQo, "TriagedArticle"); 
			for( LightViewInstance lvi : tsList) {

				Map<String, String> itm = lvi.readIndexTupleMap(top);
				
				itm.get("[ArticleCitation]LiteratureCitation|ArticleCitation.pmid");

				System.out.println(String.format("%-9s %s", 
						itm.get("[ArticleCitation]LiteratureCitation|ArticleCitation.pmid"),
						itm.get("[TriagedArticle]LiteratureCitation|ViewTable.vpdmfLabel")));
			
			}
			
		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);

			System.exit(-1);

		} catch (Exception e) {
		
			e.printStackTrace();
			
		} finally {
			
			dao.closeDbConnection();
		
		}

	}

}
