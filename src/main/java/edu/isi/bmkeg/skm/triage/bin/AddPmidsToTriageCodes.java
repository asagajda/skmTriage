package edu.isi.bmkeg.skm.triage.bin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;

public class AddPmidsToTriageCodes {

	public static String USAGE = "arguments: <pmidCodeFile> <newPmidFile> <newCode> [overwrite?]";

	private static Logger logger = Logger.getLogger(AddPmidsToTriageCodes.class);

	private VPDMf top;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		if( args.length < 3 || args.length > 4 ) {
			System.err.println(USAGE);
			System.exit(-1);
		}

		File pmidCodes = new File(args[0]);
		File newPmids = new File(args[1]);
		String code = args[2];
		
		boolean overwrite = false;
		if( args.length == 4)
			overwrite = true;
		
		TriageEngine te = new TriageEngine();
		
		Map<Integer, String> pmidCodeMap = te.loadCodesFromPmidFile(pmidCodes);
		Iterator<Integer> pmidIt = te.loadIdSetFromPmidFile(newPmids).iterator();
		while( pmidIt.hasNext() ) {
			Integer pmid = pmidIt.next();
			
			if( pmidCodeMap.containsKey( pmid ) && !overwrite ) 
				continue;
		
			pmidCodeMap.put(pmid, code);

		}
		
		FileWriter fw = new FileWriter(pmidCodes);
		BufferedWriter bw = new BufferedWriter(fw);
		
		List<Integer> keys = new ArrayList<Integer>(pmidCodeMap.keySet());
		Collections.sort(keys);
		Iterator<Integer> keyIt = keys.iterator();
		while( keyIt.hasNext() ) {
			Integer key = keyIt.next();
			
			if( pmidCodeMap.get(key).equals(-1) ) {
				
				bw.write(key + "	-\n");				

			} else if( pmidCodeMap.get(key).equals(0) ) {
			
				bw.write(key + "\n");				

			} else if( pmidCodeMap.get(key).equals(1) ) {
			
				bw.write(key + "	?\n");				

			} else if( pmidCodeMap.get(key).equals(2) ) {
			
				bw.write(key + "	+\n");				

			}
			
		}
		
		bw.close();
		
	}

}
