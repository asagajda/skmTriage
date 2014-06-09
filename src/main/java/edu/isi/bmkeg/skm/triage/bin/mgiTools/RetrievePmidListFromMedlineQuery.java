package edu.isi.bmkeg.skm.triage.bin.mgiTools;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.utils.pubmed.ESearcher;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;

public class RetrievePmidListFromMedlineQuery {

	private static Logger logger = Logger.getLogger(RetrievePmidListFromMedlineQuery.class);

	private VPDMf top;
	
	public static class Options {

		@Option(name = "-query", usage = "Medline query", required = true, metaVar = "QUERY")
		public String queryString;
	
		@Option(name = "-pmidFile", usage = "PMID file", required = true, metaVar = "FILE")
		public File pmidFile;
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();
		
		CmdLineParser parser = new CmdLineParser(options);

		DigitalLibraryEngine dlEng = new DigitalLibraryEngine ();
		
		try {

			parser.parseArgument(args);

			String queryString = options.queryString;	
			
			ESearcher eSearcher = new ESearcher(queryString);
			int maxCount = eSearcher.getMaxCount();
			List<Integer> esearchIds = new ArrayList<Integer>();
			for(int i=0; i<maxCount; i=i+1000) {
	
				long t = System.currentTimeMillis();
				
				esearchIds.addAll( eSearcher.executeESearch(i, 1000) );
				
				Thread.sleep(1000);
			}
	
			// dump this list to a file.
			if( options.pmidFile.exists() ) 
				options.pmidFile.delete();
			
			FileWriter fw = new FileWriter(options.pmidFile);
			for(Integer pmid : esearchIds) {
				fw.write(pmid + "\n"); 				
			}
		    fw.flush();
		    fw.close();

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
