package edu.isi.bmkeg.skm.triage.bin;

import java.io.File;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;

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

		TriageEngine te = new TriageEngine();

		parser.parseArgument(args);
		
		te.initializeVpdmfDao(options.login, options.password, options.dbName);
		
		CoreDao coreDao = te.getDigLibDao().getCoreDao();

		try {

			if( !options.pdfFileOrDir.exists() ) {
				throw new CmdLineException(parser, options.pdfFileOrDir.getAbsolutePath() + " does not exist.");
			}
			
			if( options.codeList != null && !options.codeList.exists() ) {
				throw new CmdLineException(parser, options.codeList.getAbsolutePath() + " does not exist.");
			}
			
			if (options.pdfRuleFile != null) {
				logger.info("Using rulefile " + options.pdfRuleFile.getAbsolutePath());
				te.setRuleFile( options.pdfRuleFile );
			} 		
			
			coreDao.connectToDb();
			
			TriageCorpus tc = te.findTriageCorpusByNameInTrans(options.corpusName);
			if( tc == null ) {
				throw new Exception("TriageCorpus " + options.corpusName + " does not exist.");
			}
			
			te.buildTriageCorpusFromPdfFileOrDir(tc, options.pdfFileOrDir, options.codeList);

			coreDao.commitTransaction();
			
		} catch (CmdLineException e) {
			
			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			
			System.exit(-1);
		
		} catch (Exception e) {
		
			e.printStackTrace();
			coreDao.rollbackTransaction();
			
		} finally {
			
			coreDao.closeDbConnection();
		
		}

	}
	

}
