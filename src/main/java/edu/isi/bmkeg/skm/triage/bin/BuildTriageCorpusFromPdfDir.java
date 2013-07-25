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

public class BuildTriageCorpusFromPdfDir {

//	public static String USAGE = "arguments: <pdf-dir-or-file> <corpus-name> <dbName> <login> <password> [<rule-file>]"; 

	private static Logger logger = Logger.getLogger(BuildTriageCorpusFromPdfDir.class);
	
	
	public static class Options {

		@Option(name = "-pdfs", usage = "Pdfs directory or file", required = true, metaVar = "PDF-DIR-OR-FILE")
		public File pdfFileOrDir;

		@Option(name = "-triageCorpus", usage = "Triage Corpus name", required = true, metaVar = "CORPUS")
		public String corpusName;
		
		@Option(name = "-rules", usage = "Rules file", required = false, metaVar = "FILE")
		public File pdfRuleFile = null;

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

		try {
			
			parser.parseArgument(args);
			
			if( !options.pdfFileOrDir.exists() ) {
				throw new CmdLineException(parser, options.pdfFileOrDir.getAbsolutePath() + " does not exist.");
			}
			
			if( options.codeList != null && !options.codeList.exists() ) {
				throw new CmdLineException(parser, options.codeList.getAbsolutePath() + " does not exist.");
			}

			TriageEngine te = new TriageEngine();
						
			if (options.pdfRuleFile != null) {
				logger.info("Using rulefile " + options.pdfRuleFile.getAbsolutePath());
				te = new TriageEngine(options.pdfRuleFile);
			} else {
				te = new TriageEngine();
			}		
			te.initializeVpdmfDao(options.login, options.password, options.dbName);
			
			TriageCorpus tc = te.findTriageCorpusByName(options.corpusName);
			if( tc == null ) {
				throw new Exception("TriageCorpus " + options.corpusName + " does not exist.");
			}
			
			te.buildTriageCorpusFromPdfFileOrDir(tc, options.pdfFileOrDir, options.codeList);
			
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
