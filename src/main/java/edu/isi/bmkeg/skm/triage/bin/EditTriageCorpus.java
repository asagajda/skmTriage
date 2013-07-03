package edu.isi.bmkeg.skm.triage.bin;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.triage.model.qo.TriageCorpus_qo;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class EditTriageCorpus {

//	public static String USAGE = "Either adds or edits a uniquely named TriageCorpus.\n" +
//			"arguments: <corpus-name> <description> <owner-name> " + 
//			"<dbName> <login> <password> "; 

	private static Logger logger = Logger.getLogger(EditTriageCorpus.class);

	public static class Options {

		@Option(name = "-name", usage = "Corpus name", required = true, metaVar = "NAME")
		public String name;
		
		@Option(name = "-desc", usage = "Corpus description", required = true, metaVar = "DESCRIPTION")
		public String description;
		
		@Option(name = "-owner", usage = "Corpus owner", required = true, metaVar = "OWNER")
		public String owner;
		
		@Option(name = "-l", usage = "Database login", required = true, metaVar = "LOGIN")
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true, metaVar = "PASSWD")
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true, metaVar  = "DBNAME")
		public String dbName = "";
		
	}

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
			System.err.println("Either adds or edits a uniquely named TriageCorpus.");
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			System.exit(-1);
		}
				
		TriageEngine te = null;
		
		te = new TriageEngine();
		te.initializeVpdmfDao(options.login, options.password, options.dbName);

		TriageCorpus_qo qc = new TriageCorpus_qo();
		qc.setName(options.name);
		List<LightViewInstance> lviList = te.getTriageDao().listArticleTriageCorpus(qc);
		
		if( lviList.size() == 0 ) {

			TriageCorpus c = new TriageCorpus();
			
			c.setName(options.name);
			c.setDescription(options.description);
			c.setOwner(options.owner);
			Date d = new Date();
			c.setDate(d.toString());
			
			te.getTriageDao().insertArticleTriageCorpus(c);
		
		} else if( lviList.size() == 1 ) {
			
			LightViewInstance lvi = lviList.get(0);
			
			TriageCorpus c = te.getTriageDao().findArticleTriageCorpusById( lvi.getVpdmfId() );
			
			c.setName(options.name);
			c.setDescription(options.description);
			c.setOwner(options.owner);
			
			te.getTriageDao().updateArticleTriageCorpus(c);
			
		}

	}

}
