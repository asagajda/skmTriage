package edu.isi.bmkeg.skm.triage.bin;

import java.util.Date;

import org.apache.log4j.Logger;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.model.citations.Corpus;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;

public class DeleteTriageCorpus {

	public static String USAGE = "arguments: <corpus-name> " + 
			"<dbName> <login> <password> "; 

	private static Logger logger = Logger.getLogger(DeleteTriageCorpus.class);

	private VPDMf top;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		if (args.length != 4) {
			System.err.println(USAGE);
			System.exit(1);
		}
		
		String name = args[0];
		String description = args[1];
		String owner = args[2];
		String dbName = args[3];
		String login = args[4];
		String password = args[5];
		
		DigitalLibraryEngine de = null;
		
		de = new DigitalLibraryEngine();
		de.initializeVpdmfDao(login, password, dbName);
		
		de.deleteCorpus(name);

	}

}
