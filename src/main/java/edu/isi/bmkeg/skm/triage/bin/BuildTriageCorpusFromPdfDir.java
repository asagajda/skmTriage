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

import bsh.This;

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

		@Option(name = "-corpus", usage = "Corpus name", required = true, metaVar = "CORPUS")
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
			
			Map<Integer,Long> pmidMap1 = te.insertPmidPdfFileOrDir(options.pdfFileOrDir);
			
			Pattern pmidPatt = Pattern.compile("^(\\d+).*\\.pdf$");			
			
			Corpus_qo cq = new Corpus_qo();
			List<LightViewInstance> cList = te.getDigLibDao().getCoreDao().list(cq, "ArticleCorpus");
			for( LightViewInstance lvi : cList ) {
				Corpus c = te.getCitDao().getCoreDao().findById(lvi.getVpdmfId(), new Corpus(), "Corpus");
				if( c.getRegex() == null )
					continue;
				
				Pattern p1 = Pattern.compile("^(\\d+)_(.*" + c.getRegex() +".*)\\.pdf$");
				Pattern p2 = Pattern.compile("^(\\d+)\\.pdf$");
				
				Map<Integer,String> pmidCodes = new HashMap<Integer,String>();
				
				Map<Integer, String> codeList = new HashMap<Integer, String>();
				
				List<File> pdfList = new ArrayList<File>(
						Converters.recursivelyListFiles(options.pdfFileOrDir).values()
						);
				for( File f : pdfList ) {
					Matcher m = pmidPatt.matcher(f.getName());
					if (m.find()) {
						Integer id = new Integer(m.group(1));
						codeList.put(id, f.getName());
					}
					
				}
				
				if (options.codeList != null) {
					FileInputStream fis = new FileInputStream(options.codeList);
					BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
					String line = "";
					while ((line = br.readLine()) != null) {
						Matcher m = pmidPatt.matcher(line);
						if (m.find()) {
							Integer id = new Integer(m.group(1));
							codeList.put(id, line);
						}
					}
				} 
					
				for( String s : codeList.values() ) {
					
					Matcher m = pmidPatt.matcher(s);
					if (!m.find()) {
						continue;
					}
					Integer id = new Integer(m.group(1));
					
					m = p2.matcher(s);
					if (m.find()) {
						pmidCodes.put(id, TriageCode.UNCLASSIFIED);
						continue;
					}
					
					m = p1.matcher(s);
					if (m.find()) {
						pmidCodes.put(id, TriageCode.IN);
					} else {
						pmidCodes.put(id, TriageCode.OUT);
					}

				}
			
				te.populateArticleTriageCorpus(tc.getName(), c.getName(), pmidCodes);
				
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
