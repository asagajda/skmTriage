package edu.isi.bmkeg.skm.triage.bin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.triage.model.qo.TriageCorpus_qo;
import edu.isi.bmkeg.triage.model.qo.TriageScore_qo;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class ReportCorpusCounts  {

	public static class Options extends Options_ImplBase {

		@Option(name = "-l", usage = "Database login", required = true, metaVar = "LOGIN")
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true, metaVar = "PASSWD")
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true, metaVar = "DBNAME")
		public String dbName = "";

	}

	private static Logger logger = Logger.getLogger(DeleteTriageCorpus.class);

	private VPDMf top;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();

		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);

		DigitalLibraryEngine de = new DigitalLibraryEngine();
		de.initializeVpdmfDao(options.login, options.password, options.dbName);

		CoreDao dao = de.getDigLibDao().getCoreDao();
		
		try {
						
			Map<Long, Integer> counts = new HashMap<Long, Integer>();
			Map<Long, String> corpora = new HashMap<Long, String>();
			Map<Long, String> triageCorpora = new HashMap<Long, String>();
			
			TriageCorpus_qo qo = new TriageCorpus_qo();
			List<LightViewInstance> cList = dao.list(qo, "Corpus");			
			for( LightViewInstance lvi : cList) {
			
				if( lvi.getDefName().contains("TriageCorpus") ) {
					triageCorpora.put(lvi.getVpdmfId(), lvi.getVpdmfLabel());
				} else {
					corpora.put(lvi.getVpdmfId(), lvi.getVpdmfLabel());
				}
				
			}

			Iterator<Long> targetIdIt = corpora.keySet().iterator();
			if( targetIdIt.hasNext() ) {
				Long firstTargetId = targetIdIt.next();

				for( Long triageId : triageCorpora.keySet() ) {
	
					TriageScore_qo tsQo = new TriageScore_qo();
					TriageCorpus_qo tcQo = new TriageCorpus_qo();
					tcQo.setVpdmfId(triageId + "");
					tsQo.setTriageCorpus(tcQo);
					Corpus_qo cQo = new Corpus_qo();
					cQo.setVpdmfId(firstTargetId + "");
					tsQo.setTargetCorpus(cQo);
					
					int c = dao.countView(tsQo, "TriageScore");
					counts.put(triageId, c);
				
				}
			
			}
				
			Iterator<Long> triageIdIt = triageCorpora.keySet().iterator();
			if( triageIdIt.hasNext() ) {
				Long firstTriageId = triageIdIt.next();
			
				for( Long targetId : corpora.keySet() ) {

					TriageScore_qo tsQo = new TriageScore_qo();
					tsQo.setInOutCode("in");
					TriageCorpus_qo tcQo = new TriageCorpus_qo();
					tcQo.setVpdmfId(firstTriageId + "");
					tsQo.setTriageCorpus(tcQo);
					Corpus_qo cQo = new Corpus_qo();
					cQo.setVpdmfId(targetId + "");
					tsQo.setTargetCorpus(cQo);
					
					int c = dao.countView(tsQo, "TriageScore");
					counts.put(targetId, c);
				
				}
			}
			
			System.out.println("\nSciKnowMine Triage Corpus Counts");
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

			System.out.println("#	Triage Corpus");
			for( Long id : triageCorpora.keySet() ) {
				String name = triageCorpora.get(id);
				Integer c = counts.get(id);
				System.out.println(c + "	" + name );
			}
			
			System.out.println("\n#	Target Corpus");
			for( Long id : corpora.keySet() ) {
				String name = corpora.get(id);
				Integer c = counts.get(id);
				System.out.println(c + "	" + name );
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
