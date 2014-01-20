package edu.isi.bmkeg.skm.triage.bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.digitalLibrary.model.citations.Corpus;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.skm.triage.model.TriageCode;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class DeleteTriageScoresFromCodeFile {

//	public static String USAGE = "arguments: <pdf-dir-or-file> <corpus-name> <dbName> <login> <password> [<rule-file>]"; 

	private static Logger logger = Logger.getLogger(DeleteTriageScoresFromCodeFile.class);
	
	
	public static class Options {

		@Option(name = "-triageCorpus", usage = "Triage Corpus name", required = true, metaVar = "CORPUS")
		public String corpusName;
		
		@Option(name = "-codeList", usage = "Encoded files", required = false, metaVar = "CODES")
		public File codeList = null;
		
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
		parser.parseArgument(args);

		TriageEngine te = new TriageEngine();
		te.initializeVpdmfDao(options.login, options.password, options.dbName);

		try {

			te.getDigLibDao().getCoreDao().connectToDb();
			
			
			TriageCorpus tc = te.findTriageCorpusByNameInTrans(options.corpusName);
			if( tc == null ) {
				throw new Exception("TriageCorpus " + options.corpusName + " does not exist.");
			}
			
			te.deleteArticlesFromTriageCorpusBasedOnCodeFile(tc, options.codeList);
			
			te.getDigLibDao().getCoreDao().commitTransaction();
			
		} catch (CmdLineException e) {
			
			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);

			te.getDigLibDao().getCoreDao().rollbackTransaction();
			
		} finally {
			
			te.getDigLibDao().getCoreDao().closeDbConnection();
			
		}
		

	}
	

}
