package edu.isi.bmkeg.skm.triage.bin.mgiTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.model.citations.Corpus;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class AssignMgiIdDumpToCorpora {

	public static class Options {

		@Option(name = "-file", usage = "Path to MGI Id Dump File", required = true, metaVar = "DIR")
		public File file;

		@Option(name = "-l", usage = "Database login", required = true, metaVar = "LOGIN")
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true, metaVar = "PASSWD")
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true, metaVar = "DBNAME")
		public String dbName = "";

		@Option(name = "-wd", usage = "Working directory", required = true, metaVar = "WDIR")
		public String workingDirectory = "";

	}

	private static Logger logger = Logger.getLogger(AssignMgiIdDumpToCorpora.class);

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();
		Map<String,List<Integer>> corpora = new HashMap<String,List<Integer>>();
		
		CmdLineParser parser = new CmdLineParser(options);
		try {

			parser.parseArgument(args);;

			BufferedReader input = new BufferedReader(new FileReader(
					options.file));
			
			try {
			
				String line = null;
				
				/* 
				 * readLine returns the content of a line MINUS the newline. 
				 * It returns null only for the END of the stream. 
				 * it returns an empty String if two newlines appear in
				 * a row.
				 * 
				 * We want columns 2 (PubmedId)
				 * columns 4 + 5 indicate 'in' given groups. 
				 */
				while ((line = input.readLine()) != null) {
					String[] lineArray = line.split("\\t");
					
					// Skip this if there aren't any entries.
					if( lineArray.length > 2 && 
							lineArray[1].toLowerCase().equals("none") ) {
						continue;
					}

					if( lineArray.length != 10) {
						continue;
					}
					
					if( lineArray[1] == null || lineArray[1].equals("null") )
						continue;
					
					Integer pmid = new Integer(lineArray[1]);
					Set<String> cSet = new HashSet<String>();
					cSet.addAll( readSetOfStrings(lineArray[3]) );
					cSet.addAll( readSetOfStrings(lineArray[4]) );
					
					for( String c : cSet ) {
						
						if( c.length() == 0 ) 
							continue;
						
						List<Integer> pmidSet = null;
						if( corpora.containsKey(c) ) {
							pmidSet = corpora.get(c);
						} else {
							pmidSet = new ArrayList<Integer>();
							corpora.put(c, pmidSet);
						}
						pmidSet.add(pmid);
					}
					
				}				
				
			} finally {
				input.close();
			}

		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			System.exit(-1);

		} catch (Exception e2) {

			e2.printStackTrace();

		}
				
		//
		// UPLOAD LITERATURE CITATIONS TO CORPORA
		//
		DigitalLibraryEngine de = new DigitalLibraryEngine();
		de.initializeVpdmfDao(options.login, options.password, options.dbName, options.workingDirectory);

		de.getDigLibDao().getCoreDao().connectToDb();		
		
		for( String cName : corpora.keySet() ) {
			
			Corpus c = new Corpus();
			c.setCorpusType("ArticleCorpus");
			c.setName(cName);
			c.setRegex(cName.substring(0, 1).toUpperCase());
			
			Corpus_qo cQo = new Corpus_qo();
			cQo.setName(cName);
			
			List<LightViewInstance> list = de.getDigLibDao().getCoreDao().listInTrans(cQo, "ArticleCorpus");
			
			if( list.size() == 0 ) {
				de.getDigLibDao().getCoreDao().insertInTrans(c, "ArticleCorpus");
			}
			
			de.getExtDigLibDao().addArticlesToCorpusInTrans(corpora.get(cName), cName);
	
		}

		de.getDigLibDao().getCoreDao().commitTransaction();
		
	}

	private static Set<String> readSetOfStrings(String str) {
 		String[] strArray = str.split(":");
		Set<String> strSet = new HashSet<String>();
		for(String s : strArray) { 
			strSet.add(s); 
		}
		return strSet;
	}
	
	
}
