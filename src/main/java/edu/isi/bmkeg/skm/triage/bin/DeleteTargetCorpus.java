package edu.isi.bmkeg.skm.triage.bin;

import java.util.List;

import org.apache.log4j.Logger;
import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.triage.model.qo.TriageCorpus_qo;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.ChangeEngineImpl;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class DeleteTargetCorpus {

	public static class Options extends Options_ImplBase {

		@Option(name = "-targetCorpus", usage = "Target Corpus Name", required = true, metaVar = "TARGET")
		public String targetCorpus = "";

		@Option(name = "-l", usage = "Database login", required = true, metaVar = "LOGIN")
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true, metaVar = "PASSWD")
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true, metaVar = "DBNAME")
		public String dbName = "";

	}

	private static Logger logger = Logger.getLogger(DeleteTargetCorpus.class);

	private VPDMf top;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();

		CmdLineParser parser = new CmdLineParser(options);

		try {

			parser.parseArgument(args);

		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);

			System.exit(-1);

		}
					
		DigitalLibraryEngine de = null;

		de = new DigitalLibraryEngine();
		de.initializeVpdmfDao(options.login, options.password, options.dbName);

		CoreDao dao = de.getDigLibDao().getCoreDao();
		
		ChangeEngineImpl ce = (ChangeEngineImpl) dao.getCe();

		try {

			ce.connectToDB();
			ce.turnOffAutoCommit();

			// Really bare-bones implementation of this.
			String sql = "DELETE ts.*, vt.* " +
					"FROM TriageScore AS ts, " + 
					" ViewTable AS vt, " +
					" Corpus AS targetc " +
					"WHERE vt.vpdmfId = ts.vpdmfId " +	
					"  AND ts.targetCorpus_id = targetc.vpdmfId " +
					"  AND targetc.name = '" + options.targetCorpus + "';";
	
			ce.executeRawUpdateQuery(sql);
			ce.prettyPrintSQL(sql);

			sql = "DELETE targetc.*, vt.* " +
					 "FROM ViewTable AS vt, " +
						" Corpus AS targetc " +
					 "WHERE vt.vpdmfId = targetc.vpdmfId " +
					 "  AND targetc.name = '" + options.targetCorpus + "';";

			ce.executeRawUpdateQuery(sql);
			ce.prettyPrintSQL(sql);
			
			ce.commitTransaction();

		} catch (Exception e) {

			ce.rollbackTransaction();
			throw e;

		} finally {

			ce.closeDbConnection();

		}

	}

}
