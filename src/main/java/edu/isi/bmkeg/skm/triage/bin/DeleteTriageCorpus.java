package edu.isi.bmkeg.skm.triage.bin;

import java.util.List;

import org.apache.log4j.Logger;
import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.triage.model.qo.TriageCorpus_qo;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class DeleteTriageCorpus {

	public static class Options extends Options_ImplBase {

		@Option(name = "-triageCorpus", usage = "Database login", required = true, metaVar = "LOGIN")
		public String triageCorpus = "";

		@Option(name = "-l", usage = "Database login", required = true, metaVar = "LOGIN")
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true, metaVar = "PASSWD")
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true, metaVar = "DBNAME")
		public String dbName = "";

	}

	private static Logger logger = Logger.getLogger(DeleteTriageCorpus.class);

	private VPDMf top;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();

		CmdLineParser parser = new CmdLineParser(options);

		try {

			parser.parseArgument(args);

			DigitalLibraryEngine de = null;

			de = new DigitalLibraryEngine();
			de.initializeVpdmfDao(options.login, options.password, options.dbName);

			CoreDao dao = de.getCitDao().getCoreDao();
			TriageCorpus_qo qo = new TriageCorpus_qo();
			qo.setName(options.triageCorpus);
			
			List<LightViewInstance> list = dao.list(qo, "TriageCorpus");
			for( LightViewInstance lvi : list) {
				boolean deleted = dao.deleteById(lvi.getVpdmfId(), "TriageCorpus");
				if( deleted ) {
					logger.info("Deletion of Triage Corpus: " + lvi.getVpdmfLabel() + " succeeded");
				} else {
					logger.info("Deletion of Triage Corpus: " + lvi.getVpdmfLabel() + " failed");
				}
			}

		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);

			System.exit(-1);

		}

	}

}
