package edu.isi.bmkeg.skm.triage.bin;

import java.io.File;

import org.apache.log4j.Logger;

import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;

public class DumpTriageCorpusToTriageCodeFile {

	public static String USAGE = "arguments: <name> <pmidCodeFile> " 
			+ "<dbName> <login> <password>";

	private static Logger logger = Logger.getLogger(BuildTriageCorpusFromMedlineQuery.class);

	private VPDMf top;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		if( args.length != 5) {
			System.err.println(USAGE);
			System.exit(-1);
		}

		String name = args[0];
		File pmidFile = new File( args[1] );

		String dbName = args[3];
		String login = args[4];
		String password = args[5];

		if( !pmidFile.exists() ) {
			System.err.println(pmidFile.getPath() + " does not exist!");
			System.exit(-1);
		}		
		
		TriageEngine te = new TriageEngine();
		te.initializeVpdmfDao(login, password, dbName);
		
		TriageCorpus tc = te.findTriageCorpusByNameInTrans(name);
				
	}

}
