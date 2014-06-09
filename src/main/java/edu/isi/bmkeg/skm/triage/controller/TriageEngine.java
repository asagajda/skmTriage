package edu.isi.bmkeg.skm.triage.controller;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.model.citations.Corpus;
import edu.isi.bmkeg.digitalLibrary.model.citations.Journal;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.digitalLibrary.utils.pubmed.EFetcher;
import edu.isi.bmkeg.skm.triage.dao.TriageDaoEx;
import edu.isi.bmkeg.skm.triage.dao.vpdmf.TriageDaoExImpl;
import edu.isi.bmkeg.skm.triage.model.TriageCode;
import edu.isi.bmkeg.triage.dao.TriageDao;
import edu.isi.bmkeg.triage.dao.impl.TriageDaoImpl;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.triage.model.TriageFeature;
import edu.isi.bmkeg.triage.model.TriageScore;
import edu.isi.bmkeg.triage.model.qo.TriageCorpus_qo;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.ChangeEngineImpl;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class TriageEngine extends DigitalLibraryEngine 
		implements SharedResourceObject {

	private static Logger logger = Logger.getLogger(TriageEngine.class);

	private TriageDaoEx exTriageDao;

	private TriageDao triageDao;

	public TriageEngine() throws Exception {
		super();
	}

	public TriageEngine(File pdfRuleFile) throws Exception {
		super(pdfRuleFile);
		
		if( !pdfRuleFile.exists() )
			throw new FileNotFoundException(pdfRuleFile.getPath() + " not found.");
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Getters and Setters
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public TriageDaoEx getExTriageDao() {
		return exTriageDao;
	}

	public void setExTriageDao(TriageDaoEx triageDao) {
		this.exTriageDao = triageDao;
	}

	public TriageDao getTriageDao() {
		return triageDao;
	}

	public void setTriageDao(TriageDao triageDao) {
		this.triageDao = triageDao;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// When being used in a UIMA Pipeline.
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void load(DataResource aData) throws ResourceInitializationException {
		// TODO Auto-generated method stub
		int pause = 0;
		pause++;
		
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// High-level API Functions
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public void buildTriageCorpusFromPdfFileOrDir(TriageCorpus tc,
			File pdfFileOrDir, File codeFile, boolean skip) throws Exception {

		this.insertPmidPdfFileOrDir(pdfFileOrDir, skip);

		Map<Integer, String> codeList = this.compileCodeList(pdfFileOrDir);
		if (codeFile != null)
			codeList.putAll(this.compileCodeList(codeFile));

		this.addCodeListToCorpus(tc, codeList);

	}

	public void buildTriageCorpusFromCodeFile(TriageCorpus tc, File codeFile)
			throws Exception {

		Map<Integer, String> codeList = this.compileCodeList(codeFile);

		this.addCodeListToCorpus(tc, codeList);

	}

	public void deleteArticlesFromTriageCorpusBasedOnCodeFile(TriageCorpus tc,
			File codeFile) throws Exception {

		Map<Integer, String> pmidCodes = this.compileCodeList(codeFile);

		ChangeEngineImpl ce = (ChangeEngineImpl) this.getDigLibDao().getCoreDao()
				.getCe();

		List<Integer> pmids = new ArrayList<Integer>(pmidCodes.keySet());
		int nRowsChanged = 0;
		Collections.sort(pmids);
		Iterator<Integer> it = pmids.iterator();
		while (it.hasNext()) {
			Integer pmid = it.next();

			String sql = "DELETE ts.*, vt.* " + "FROM TriageScore AS ts, "
					+ " ViewTable AS vt, "
					+ " LiteratureCitation AS litcit, "
					+ " ArticleCitation AS artcit, "
					+ " Corpus AS triagec "
					+ "WHERE vt.vpdmfId = ts.vpdmfId "
					+ "  AND ts.citation_id = litcit.vpdmfId "
					+ "  AND litcit.vpdmfId = artcit.vpdmfId "
					+ "  AND artcit.pmid = '" + pmid + "'"
					+ "  AND ts.triageCorpus_id = triagec.vpdmfId "
					+ "  AND triagec.name = '" + tc.getName() + "';";

			nRowsChanged += ce.executeRawUpdateQuery(sql);

			ce.prettyPrintSQL(sql);

		}

		ce.commitTransaction();
		logger.info(nRowsChanged + " rows altered.");

	}

	public void addCodeListToCorpus(TriageCorpus tc,
			Map<Integer, String> codeList) throws Exception {

		Corpus_qo cq = new Corpus_qo();
		List<LightViewInstance> cList = this.getDigLibDao().getCoreDao()
				.listInTrans(cq, "Corpus");
		for (LightViewInstance lvi : cList) {
			Corpus c = this.getDigLibDao().getCoreDao().findByIdInTrans(
					lvi.getVpdmfId(), 
					new Corpus(),
					"Corpus");
			if (c.getRegex() == null)
				continue;

			this.addCodeListToCorpus(tc, c, codeList);
			
		}
	}

	public void addCodeListToCorpus(TriageCorpus tc, 
			Corpus c, Map<Integer, String> codeList) throws Exception {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Are there any codes assigned? If so, we assume that
		// ALL PAPERS IN THE COLLECTION ARE 'IN' OR 'OUT'
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		boolean allInOut = false;
		for (String s : codeList.values()) {
			if (s.length() > 0) {
				allInOut = true;
				break;
			}
		}

		if (allInOut) {
			logger.info("ASSUMING THAT ALL DOCUMENTS ARE CLASSIFED");
		} else {
			logger.info("ASSUMING THAT ALL DOCUMENTS ARE UNCLASSIFED");
		}

		Map<Integer, String> pmidCodes = new HashMap<Integer, String>();
		for (Integer pmid : codeList.keySet()) {
			String code = codeList.get(pmid);
			if (!allInOut) {
				pmidCodes.put(pmid, TriageCode.UNCLASSIFIED);
			} else {
				if (code.contains(c.getRegex())) {
					pmidCodes.put(pmid, TriageCode.IN);
				} else {
					pmidCodes.put(pmid, TriageCode.OUT);
				}
			}
		}

		this.exTriageDao.addTriageDocumentsToCorpus(tc.getName(),
				c.getName(), pmidCodes);

	}
	
	
	public Map<Integer, String> compileCodeList(File inputFile)
			throws Exception {
		Map<Integer, String> codeMap = new HashMap<Integer, String>();

		Pattern pmidPatt = Pattern.compile("^(\\d+).*\\.pdf$");
		Pattern noCodePatt = Pattern.compile("^(\\d+)\\.pdf$");
		Pattern codePatt = Pattern.compile("^(\\d+)_(.*)\\.pdf$");

		// if file is a PDF (*.pdf), process the file name.
		// if file is a directory, process the file names.
		// if file is a text file (*.txt) process the contents.

		if (inputFile.getName().endsWith(".pdf") && !inputFile.isDirectory()) {

			Matcher m = pmidPatt.matcher(inputFile.getName());
			if (m.find()) {
				Integer id = new Integer(m.group(1));
				codeMap.put(id, "");
			}

			Matcher noCodeMatch = noCodePatt.matcher(inputFile.getName());
			if (noCodeMatch.find()) {
				Integer id = new Integer(noCodeMatch.group(1));
				codeMap.put(id, "");
			}

			Matcher codeMatch = codePatt.matcher(inputFile.getName());
			if (codeMatch.find()) {
				Integer id = new Integer(codeMatch.group(1));
				codeMap.put(id, codeMatch.group(2));
			}

		} else if (inputFile.getName().endsWith(".txt")
				&& !inputFile.isDirectory()) {

			FileInputStream fis = new FileInputStream(inputFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis,
					Charset.forName("UTF-8")));
			String line = "";
			while ((line = br.readLine()) != null) {
				Matcher noCodeMatch = noCodePatt.matcher(line);
				if (noCodeMatch.find()) {
					Integer id = new Integer(noCodeMatch.group(1));
					codeMap.put(id, "");
				}
				Matcher codeMatch = codePatt.matcher(line);
				if (codeMatch.find()) {
					Integer id = new Integer(codeMatch.group(1));
					codeMap.put(id, codeMatch.group(2));
				}
			}

		} else if (inputFile.isDirectory()) {

			List<File> pdfList = new ArrayList<File>(Converters
					.recursivelyListFiles(inputFile).values());
			for (File f : pdfList) {
				Matcher m = pmidPatt.matcher(f.getName());
				if (m.find()) {
					Integer id = new Integer(m.group(1));
					codeMap.put(id, "");
				}
			}

			for (File f : pdfList) {
				Matcher noCodeMatch = noCodePatt.matcher(f.getName());
				if (noCodeMatch.find()) {
					Integer id = new Integer(noCodeMatch.group(1));
					codeMap.put(id, "");
				}
				Matcher codeMatch = codePatt.matcher(f.getName());
				if (codeMatch.find()) {
					Integer id = new Integer(codeMatch.group(1));
					codeMap.put(id, codeMatch.group(2));
				}
			}

		} else {

			throw new Exception("Not sure what sort of file this is: "
					+ inputFile.getPath());

		}

		return codeMap;

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// VPDMf functions
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Builds dao objects to input and output data to a VPDMf store.
	 */
	public void initializeVpdmfDao(String login, String password, String dbName, String workingDirectory)
			throws Exception {

		super.initializeVpdmfDao(login, password, dbName, workingDirectory);

		CoreDao coreDao = this.getFtdDao().getCoreDao();

		this.exTriageDao = new TriageDaoExImpl(coreDao);
		this.triageDao = new TriageDaoImpl(coreDao);

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public TriageCorpus findTriageCorpusByNameInTrans(String name) throws Exception {

		TriageCorpus_qo tQo = new TriageCorpus_qo();
		tQo.setName(name);
		
		List<LightViewInstance> lviList = this.triageDao.getCoreDao().listInTrans(tQo, "TriageCorpus");
		if( lviList.size() != 1 )
			return null;

		TriageCorpus tc = this.triageDao.getCoreDao().findByIdInTrans(
				lviList.get(0).getVpdmfId(), new TriageCorpus(), "TriageCorpus");
		
		return tc;
	
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<Integer> loadArticlesFromESearch(String queryString,
			Set<Integer> pmidsToSkip) throws Exception {

		EFetcher f = new EFetcher(queryString, pmidsToSkip);
		while (f.hasNext()) {
			ArticleCitation a = f.next();

			if (a == null)
				continue;

			if (a.getVolume() == null || a.getVolume().length() == 0) {
				a.setVolume("-");
				a.setVolValue(-1);
			}
			if (a.getPages() == null || a.getPages().length() == 0) {
				a.setPages("-");
			}

			String jStr = a.getJournal().getAbbr();
			if (!this.getjLookup().containsKey(jStr)) {
				logger.info("'" + jStr
						+ "' not found in lookup, skipping PMID=" + a.getPmid());
				continue;
			}

			Journal j = this.getjLookup().get(jStr);
			a.setJournal(j);

			try {
				logger.info("inserting article, PMID=" + a.getPmid());
				this.getDigLibDao().insertArticleCitation(a);
			} catch (Exception e) {
				logger.info("article insert failed, PMID=" + a.getPmid());
				e.printStackTrace();
			}

		}

		return f.getAllIds();

	}

	/**
	 * Read triage-encoded files:
	 * 
	 * 12345673 OUT or '-' => code = -1 12345670 UNCLASSIFIED or '' => code = 0
	 * 12345671 MAYBE or '?' => code = 1 12345672 IN or '+' => code = 2
	 * 
	 * @param pmidFile
	 * @return
	 * @throws Exception
	 */
	public Map<Integer, String> loadCodesFromPmidFile(File pmidFile)
			throws Exception {

		// Load file line by line as Map<String, Map<String,String>>
		Map<Integer, String> idMap = new HashMap<Integer, String>();
		FileInputStream fstream = new FileInputStream(pmidFile);

		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		int i = 1;
		String strLine;
		List<String> missedLines = new ArrayList<String>();
		while ((strLine = br.readLine()) != null) {
			String[] splitLn = strLine.split("\\t+");
			String pmid = splitLn[0];

			if (splitLn.length == 1) {
				idMap.put(new Integer(pmid), TriageCode.UNCLASSIFIED);
				continue;
			}

			String s = splitLn[1];
			if (s.equals("OUT") || s.equals("-")) {

				idMap.put(new Integer(pmid), TriageCode.OUT);

			} else if (s.equals("IN") || s.equals("+")) {

				idMap.put(new Integer(pmid), TriageCode.IN);

			} else if (s.equals("UNCLASSIFIED")) {

				idMap.put(new Integer(pmid), TriageCode.UNCLASSIFIED);

			} else if (s.equals("MAYBE") || s.equals("?")) {

				idMap.put(new Integer(pmid), TriageCode.MAYBE);

			} else {
				missedLines.add(strLine);

			}

		}

		in.close();

		return idMap;

	}

	public Set<Integer> loadCodedPmidsFromFile(File triageCodesFile, String code)
			throws Exception {

		Set<Integer> pmids = new HashSet<Integer>();

		Map<Integer, String> pmidCodes = this
				.loadCodesFromPmidFile(triageCodesFile);
		Iterator<Integer> pmidIt = pmidCodes.keySet().iterator();
		while (pmidIt.hasNext()) {
			Integer pmid = pmidIt.next();
			if (pmidCodes.get(pmid).equals(code)) {
				pmids.add(pmid);
			}
		}

		return pmids;

	}

	public void updateInScore(long vpdmfId, float inScore, Date timestamp)
			throws Exception {

		TriageScore td = this.triageDao.getCoreDao().findByIdInTrans(vpdmfId,
				new TriageScore(), "TriagedArticle");

		if (td == null) {
			logger.warn("Failed to find TriagedDocument with id:" + vpdmfId);
			return;
		}

		td.setInScore(inScore);
		td.setScoredTimestamp(timestamp);

		getExTriageDao().getCoreDao().updateInTrans(td, "TriagedArticle");

	}

	public int removeAllExplanationFeatures(
			Long tsId
			) throws Exception {

		ChangeEngineImpl ce = (ChangeEngineImpl) this.getDigLibDao().getCoreDao().getCe();
		
		String sql = "DELETE tf.* FROM TriageFeature as tf " + 
				"WHERE tf.score_id = " + tsId +";";
	
		ce.prettyPrintSQL(sql);				
		
		return ce.executeRawUpdateQuery(sql);
						
	}
	
	/**
	 * TODO GENERALIZE THIS ASPECT OF THIS SYSTEM!!!
	 * 
	 * @param tfMap
	 * @throws Exception
	 */
	public int insertExplanationFeatures(
			Long tsId, TriageFeature tf
			) throws Exception {

		ChangeEngineImpl ce = (ChangeEngineImpl) this.getDigLibDao().getCoreDao().getCe();
		
		String sql = "INSERT INTO TriageFeature " +
				" (featName, featValue, score_id) " +
				" VALUES (\"" + 
				tf.getFeatName() + "\", \"" + 
				tf.getFeatValue() + "\"," +
				tsId + ");";
	
		ce.prettyPrintSQL(sql);				
		
		return ce.executeRawUpdateQuery(sql);
						
	}
	
}
