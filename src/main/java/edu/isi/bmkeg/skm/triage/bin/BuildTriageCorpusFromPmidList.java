package edu.isi.bmkeg.skm.triage.bin;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;

public class BuildTriageCorpusFromPmidList {
		
	public static String USAGE = "arguments: -triageCorpus <triageCorpus> " +
			"-targetCorpus <triageCorpus> " +
			"[-pmidCodes <pmidCodeFilePath>] " +
			"-db <dbName> " +
			"-l <login> " +
			"-p <password>";
	
	private static Logger logger = Logger.getLogger(BuildTriageCorpusFromPmidList.class);
	
	public static class Options extends Options_ImplBase {
		
		@Option(name = "-triageCorpus", usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus", usage = "Target directory")
		public String targetCorpus = "";

		@Option(name = "-pmidCodes", usage = "Location of the rules file")
		public File pmidFile;
		
		@Option(name = "-db", usage = "Database name")
		public String dbName = "";
		
		@Option(name = "-l", usage = "Database login")
		public String login = "";

		@Option(name = "-p", usage = "Database password")
		public String password = "";

	}	
	
	private VPDMf top;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.parseOptions(args);

		if (options.triageCorpus.length() == 0 
				|| options.targetCorpus.length() == 0 
				|| options.login.length() == 0
				|| options.password.length() == 0
				|| options.dbName.length() == 0) {
			System.err.print(USAGE);
			System.exit(-1);
		}	
		
		String triageCorpus = options.triageCorpus;
		String targetCorpus = options.targetCorpus;
		String dbName = options.dbName;
		String login = options.login;
		String password = options.password;
		File pmidFile = options.pmidFile;
		
		if( !pmidFile.exists() ) {
			System.err.println(pmidFile.getPath() + " does not exist!");
			System.exit(-1);
		}		
		
		TriageEngine te = new TriageEngine();
		te.initializeVpdmfDao(login, password, dbName);
		
		TriageCorpus tc = te.findTriageCorpusByName(triageCorpus);
		if( tc == null ) {
			throw new Exception("TriageCorpus " + triageCorpus + " does not exist.");
		}
		
		Map<Integer, String> pmidCodes = te.loadCodesFromPmidFile(pmidFile);
		Set<Integer> pmids = pmidCodes.keySet();
		
		// remove pubmed ids that are already in the system. 
		Map<Integer, Long> lookup = te.buildPmidLookup(pmids);
		Set<Integer> toAdd = new HashSet<Integer>(pmidCodes.keySet());
		toAdd.removeAll(lookup.keySet());
		te.insertArticlesFromPmidList(toAdd);

		te.populateArticleTriageCorpus(triageCorpus, targetCorpus, pmidCodes);
		
	}

}
