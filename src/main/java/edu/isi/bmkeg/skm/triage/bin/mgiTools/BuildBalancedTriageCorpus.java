package edu.isi.bmkeg.skm.triage.bin.mgiTools;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.digitalLibrary.model.citations.Corpus;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class BuildBalancedTriageCorpus {

	public static class Options {

		@Option(name = "-targetCorpus", usage = "", required = true, metaVar = "TARGET")
		public String targetCorpus;

		@Option(name = "-triageCorpus", usage = "", required = true, metaVar = "TRIAGE")
		public String triageCorpus;

		@Option(name = "-l", usage = "Database login", required = true, metaVar = "LOGIN")
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true, metaVar = "PASSWD")
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true, metaVar = "DBNAME")
		public String dbName = "";

		@Option(name = "-wd", usage = "Working directory", required = true, metaVar = "WDIR")
		public String workingDirectory = "";

	}

	private static Logger logger = Logger
			.getLogger(BuildBalancedTriageCorpus.class);

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();
		Map<String, List<Integer>> corpora = new HashMap<String, List<Integer>>();

		CmdLineParser parser = new CmdLineParser(options);
		TriageEngine te = new TriageEngine();

		try {

			parser.parseArgument(args);

			te.initializeVpdmfDao(options.login, options.password,
					options.dbName, options.workingDirectory);

			te.getDigLibDao().getCoreDao().connectToDb();
			
			TriageCorpus tc = new TriageCorpus();
			tc.setName(options.triageCorpus);
			
			//
			// Part 1 add all the data to the 'in' set
			//
			// Query based on a query constructed with SqlQueryBuilder based on the TriagedArticle view.
			String selectSql = "SELECT * ";

			String countSql = "SELECT COUNT(*) ";
						
			String fromWhereSql = "FROM LiteratureCitation AS l, " +
								" ArticleCitation AS a, " +
								" Journal AS j, " +
								" Corpus AS c, " +
								" Corpus_corpora__resources_LiteratureCitation AS cl, " +
								" FTD AS f " + 
								"WHERE l.vpdmfId=a.vpdmfId AND " +
								" l.vpdmfId=cl.resources_id AND " +
								" c.vpdmfId=cl.corpora_id AND " +
								" j.vpdmfId=a.journal_id AND " +
								" c.name='" + options.targetCorpus + "' AND " +
								" f.vpdmfId=l.fullText_id";

			fromWhereSql += " ORDER BY l.vpdmfId";

			te.getDigLibDao().getCoreDao().getCe().connectToDB();
						
			ResultSet countRs = te.getDigLibDao().getCoreDao().getCe().executeRawSqlQuery(
								countSql + fromWhereSql);
			countRs.next();
			int count = countRs.getInt(1);
			countRs.close();

			ResultSet rs = te.getDigLibDao().getCoreDao().getCe().executeRawSqlQuery(
								selectSql + fromWhereSql);

			Map<Long,Map<String,String>> lookup = new HashMap<Long,Map<String,String>>();
			
			Map<Integer, String> codeMap = new HashMap<Integer, String>();
			Map<Integer, Integer> yearMap = new HashMap<Integer, Integer>();
			Map<Integer, String> journalMap = new HashMap<Integer, String>();
			
			Set<String> journalYearSet = new HashSet<String>();
			String code = options.targetCorpus.substring(0,1).toUpperCase();
			
			while( rs.next() ) {

				int pmid = rs.getInt("a.pmid");
				codeMap.put(pmid, code);

				int year = rs.getInt("l.pubYear");
				yearMap.put(pmid, year);
				
				String jAbbr = rs.getString("j.abbr");
				journalMap.put(pmid, jAbbr);

				journalYearSet.add(jAbbr + "___" + year);

			}
			rs.close();

			Corpus_qo cQo = new Corpus_qo();
			cQo.setName(options.targetCorpus);
			List<LightViewInstance> listLvi = te.getDigLibDao().getCoreDao().listInTrans(cQo, "ArticleCorpus");
			if( listLvi.size() != 1 ) {
				throw new Exception("Need to have a unique corpus: " + options.targetCorpus);
			}
			
			LightViewInstance lvi = listLvi.get(0);
			Corpus c = te.getDigLibDao().getCoreDao().findByIdInTrans(lvi.getVpdmfId(), new Corpus(), "ArticleCorpus");
			
			//te.addCodeListToCorpus(tc, c, codeMap);

			Map<Integer, String> codeMap2 = new HashMap<Integer, String>();
			Set<Integer> candidates = new HashSet<Integer>();
			int itemsToGet = 0;

			for(String key: journalYearSet) {
				String[] keyPieces = key.split("___");
				String jAbbr = keyPieces[0];
				String year = keyPieces[1];
				
				selectSql = "SELECT a.pmid ";
				fromWhereSql = "FROM LiteratureCitation AS l, "
						+ "ArticleCitation AS a, "
						+ "Journal as j, "
						+ "URL AS u "
						+ "WHERE "
						+ " l.vpdmfId=a.vpdmfId AND "
						+ " l.vpdmfId=u.resource_id AND "
						+ " j.vpdmfId=a.journal_id AND "
						+ " l.pubYear = "+  year + " AND "
						+ " j.abbr = '" + jAbbr + "'";
								 				
				countRs = te.getDigLibDao().getCoreDao().getCe().executeRawSqlQuery(
									countSql + fromWhereSql);
				countRs.next();
				count = countRs.getInt(1);
				countRs.close();
				
				rs = te.getDigLibDao().getCoreDao().getCe().executeRawSqlQuery(
									selectSql + fromWhereSql);
				
				while( rs.next() ) {
					candidates.add( rs.getInt("a.pmid") );
				}
				
				rs.close();
					
			}
			
			for(int pmid: candidates) {
				if( !codeMap.containsKey(pmid) ) {
				  codeMap2.put(pmid, "12345");					
				} 
				if( codeMap2.size() >= codeMap.size() ) {
					break;
				}
			}
			
			te.addCodeListToCorpus(tc, c, codeMap2);
			
			te.getDigLibDao().getCoreDao().commitTransaction();

		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			System.exit(-1);

		} 
		
	}

	private static Set<String> readSetOfStrings(String str) {
		String[] strArray = str.split(":");
		Set<String> strSet = new HashSet<String>();
		for (String s : strArray) {
			strSet.add(s);
		}
		return strSet;
	}

}
