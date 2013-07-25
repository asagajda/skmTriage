package edu.isi.bmkeg.skm.triage.bin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.isi.bmkeg.digitalLibrary.utils.pubmed.ESearcher;
import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.skm.triage.model.TriageCode;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;

public class BuildTriageCorpusFromMedlineQuery {

	public static String USAGE = "arguments: <triageCorpus> <targetCorpus> <queryString> <masterCorpus> " 
			+ "<dbName> <login> <password>";

	private static Logger logger = Logger.getLogger(BuildTriageCorpusFromMedlineQuery.class);

	private VPDMf top;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		if( args.length != 7 ) {
			System.err.println(USAGE);
			System.exit(-1);
		}

		String triageCorpus = args[0];
		String targetCorpus = args[1];
		String queryString = args[2];
		String masterCorpus = args[3];

		String dbName = args[4];
		String login = args[5];
		String password = args[6];
		
		TriageEngine te = new TriageEngine();
		te.initializeVpdmfDao(login, password, dbName);
		
		te.createEmptyTriageCorpus(triageCorpus, queryString, masterCorpus);
		
		ESearcher eSearcher = new ESearcher(queryString);
		int maxCount = eSearcher.getMaxCount();
		Set<Integer> esearchIds = new HashSet<Integer>();
		for(int i=0; i<maxCount; i=i+1000) {

			long t = System.currentTimeMillis();
			
			esearchIds.addAll( eSearcher.executeESearch(i, 1000) );
			
			long deltaT = System.currentTimeMillis() - t;
			logger.info("    esearch 1000 entries: " + deltaT / 1000.0
					+ " s\n");
			
			logger.info("    wait 3 secs");
			Thread.sleep(3000);
		}

		Map<Integer, String> pmidCodes = new HashMap<Integer, String>();
		
		// remove pubmed ids that are already in the system. 
		Iterator<Integer> it = esearchIds.iterator();
		while( it.hasNext() ) {
			Integer pmid = it.next();
			pmidCodes.put(pmid, TriageCode.UNCLASSIFIED);
		}

		te.getExTriageDao().addTriageDocumentsToCorpus(triageCorpus, targetCorpus, pmidCodes);

	}

}
