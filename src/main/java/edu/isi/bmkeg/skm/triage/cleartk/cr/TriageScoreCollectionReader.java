package edu.isi.bmkeg.skm.triage.cleartk.cr;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.uimafit.component.JCasCollectionReader_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.skm.triage.model.TriageCode;
import edu.isi.bmkeg.triage.uimaTypes.TriageScore;

/**
 * This Collection Reader runs over every triageScore for a given target corpus to provide
 * access to the full-text of the document and its triageCode (e.g., in, out, unknown). 
 * This collection reader will retrieve at most one item per citation. If no triage corpus parameter
 * is specified it will aggregate the triageScores corresponding to all existing triage corpora. If a 
 * single citation has different triage codes in different triage corpora it will use the following rule
 * to assign an aggregated triage code: 1) "in" will override "out" and "unknown" and 2) "out" will override
 * "unknown".
 * We want to optimize this interaction for speed, so we run a
 * manual query over the underlying database involving a minimal subset of
 * tables.
 * 
 * @author burns
 * 
 */
public class TriageScoreCollectionReader extends JCasCollectionReader_ImplBase {
	
	private static class AggregatedScore {
		public long vpdmfId;
		public long citId;
		public String code;
		public String text;
		
		public AggregatedScore(long vpdmfId, long citId, String code, String text) {
			this.vpdmfId = vpdmfId;
			this.citId = citId;
			this.code = code;
			this.text = text;
		}
	}
	
	private static Logger logger = Logger.getLogger(TriageScoreCollectionReader.class);

	public static final String TRIAGE_CORPUS_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(TriageScoreCollectionReader.class,
					"triageCorpusName");
	@ConfigurationParameter(mandatory = false, 
			description = "If specified, the triageScores will be restricted to the given triage corpus, else it will not restrict triageScores to any triage corpus")
	protected String triageCorpusName;

	public static final String TARGET_CORPUS_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(TriageScoreCollectionReader.class,
					"targetCorpusName");
	@ConfigurationParameter(mandatory = true, description = "The name of the target corpus to be read")
	protected String targetCorpusName;

	public static final String LOGIN = ConfigurationParameterFactory
			.createConfigurationParameterName(TriageScoreCollectionReader.class,
					"login");
	@ConfigurationParameter(mandatory = true, description = "Login for the Digital Library")
	protected String login;

	public static final String PASSWORD = ConfigurationParameterFactory
			.createConfigurationParameterName(TriageScoreCollectionReader.class,
					"password");
	@ConfigurationParameter(mandatory = true, description = "Password for the Digital Library")
	protected String password;

	public static final String DB_URL = ConfigurationParameterFactory
			.createConfigurationParameterName(TriageScoreCollectionReader.class,
					"dbUrl");
	@ConfigurationParameter(mandatory = true, description = "The Digital Library URL")
	protected String dbUrl;
	
	public static final String SKIP_UNKNOWNS = ConfigurationParameterFactory
			.createConfigurationParameterName(TriageScoreCollectionReader.class,
					"skipUnknowns");
	@ConfigurationParameter(mandatory = false, description = "Skip triageScores with value 'unknown'", 
			defaultValue = "false")
	protected boolean skipUnknowns;
	
	protected ResultSet rs;

	private boolean eof = false;

	private boolean isAggregate;
	
	/**
	 * The next AggregatedScore or null if EOF
	 */
	private AggregatedScore currentAs = null;
	
	protected long startTime, endTime;

	protected int pos = 0, count = 0;

	private TriageEngine triageEngine;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {

		try {

			isAggregate =  (triageCorpusName == null || triageCorpusName.length() == 0);
			triageEngine = new TriageEngine();
			triageEngine.initializeVpdmfDao(login, password, dbUrl);	
			
			// Query based on a query constructed with SqlQueryBuilder based on the TriagedArticle view.
			String sql = "SELECT DISTINCT FTD_0__FTD.pmcXml, " + 
					" TriageScore_0__TriageScore.inOutCode, " +
					" TriageScore_0__TriageScore.vpdmfId, " + 
					" TriageScore_0__TriageScore.citation_id " + 
					"FROM FTD AS FTD_0__FTD, " + 
					" LiteratureCitation AS LiteratureCitation_0__LiteratureCitation, " + 
					" Corpus AS TargetCorpus_0__Corpus, " +
					" TriageScore AS TriageScore_0__TriageScore " + 
					"WHERE " + 
					" TargetCorpus_0__Corpus.name = '" + targetCorpusName +  "' AND " +
					" LiteratureCitation_0__LiteratureCitation.vpdmfId=TriageScore_0__TriageScore.citation_id AND " +
					" TargetCorpus_0__Corpus.vpdmfId=TriageScore_0__TriageScore.targetCorpus_id AND " +
					" FTD_0__FTD.vpdmfId=LiteratureCitation_0__LiteratureCitation.fullText_id";
			
			if (!isAggregate) {
				sql += " AND TriageScore_0__TriageScore.triageCorpus_id IN " + 
						"(SELECT TriageCorpus_0__Corpus.vpdmfId " + 
						"FROM " +
						" Corpus AS TriageCorpus_0__Corpus " +
						"WHERE " +
						" TriageCorpus_0__Corpus.name = '" + triageCorpusName + "')";				
			}


			sql += " ORDER BY TriageScore_0__TriageScore.citation_id";
			
			triageEngine.getDigLibDao().getCoreDao().getCe().connectToDB();
			
			this.rs = triageEngine.getDigLibDao().getCoreDao().getCe().executeRawSqlQuery(sql);
			
			this.rs.last();
			this.count = this.rs.getRow();
			
			this.rs.beforeFirst();			
			this.pos = 0;
			
			this.startTime = System.currentTimeMillis();

			//
			// Calling moveNext() to compute the first currentAs
			//
			eof = ! rs.next();
			moveNext();
			
		} catch (Exception e) {

			throw new ResourceInitializationException(e);

		}

	}

	/**
	 * @see com.ibm.uima.collection.CollectionReader#getNext(com.ibm.uima.cas.CAS)
	 */
	public void getNext(JCas jcas) throws IOException, CollectionException {

		try {
						
			if( currentAs.text == null )
			    jcas.setDocumentText("");
			else
				jcas.setDocumentText( currentAs.text );

			TriageScore doc = new TriageScore(jcas);
		    doc.setVpdmfId(currentAs.vpdmfId);
		    doc.setCitation_id(currentAs.citId);
		    doc.setInOutCode(currentAs.code);
		    doc.addToIndexes(jcas);

		    moveNext();
		    
		} catch (Exception e) {

			throw new CollectionException(e);

		}

	}

	/**
	 * @see com.ibm.uima.arg0collection.base_cpm.BaseCollectionReader#hasNext()
	 */
	public boolean hasNext() throws IOException, CollectionException {
		return currentAs != null;
	}
		
	public void close() throws IOException {
		try {
			triageEngine.getDigLibDao().getCoreDao().getCe().closeDbConnection();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	/**
	 * sets the next currentAs.
	 * 
	 * If skipUnknowns is true it will skips the citations whose aggregated code is "unknown"
	 * @throws TransformerException 
	 * @throws IOException 
	 */
	private void moveNext() throws SQLException, IOException, TransformerException {
		currentAs = computeAggregatedScore();
		
		if (skipUnknowns) {
			while (currentAs != null) {
				if (TriageCode.IN.equals(currentAs.code) || TriageCode.OUT.equals(currentAs.code) ) break;
				currentAs = computeAggregatedScore();				
			}
		}
	}
	
	/** 
	 * Computes the next AggregatedScore and advances the rs cursor
	 * 
	 * @return the next AggregatedScore or null if EOF. 
	 *
	 * This function expects a state in which either
 	 * a) rs already contains a valid record and this record corresponds to the first time 
	 * the current vpdmfId is seen or
	 * b) eof is true
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws TransformerException 
	 * 
	 */
	private AggregatedScore computeAggregatedScore() throws 
			SQLException, IOException, TransformerException {

		if (eof)
			return null;
		
		Long vpdmfId = rs.getLong("vpdmfId");
		Long citation_id = rs.getLong("citation_id");
		String inOutCode = rs.getString("inOutCode");
		String pmcXml = rs.getString("pmcXml");
		
		StringReader inputReader = new StringReader(pmcXml);
		StringWriter outputWriter = new StringWriter();
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Resource xslResource = new ClassPathResource(
				"jatsPreviewStyleSheets/xslt/main/jats-html.xsl"
				);
		StreamSource xslt = new StreamSource(xslResource.getInputStream());
		Transformer transformer = tf.newTransformer(xslt);

		StreamSource source = new StreamSource(inputReader);
		StreamResult result = new StreamResult(outputWriter);
		transformer.transform(source, result);
		String html = outputWriter.toString();  
				
		Document doc = Jsoup.parse(html);
		HtmlToPlainText formatter = new HtmlToPlainText();
        String plainText = formatter.getPlainText(doc);
		
		AggregatedScore as = new AggregatedScore(
				vpdmfId, citation_id, inOutCode, plainText
				);
		
		eof = !rs.next();
		
		while (!eof && rs.getLong("citation_id") == as.citId) {
			String code =  rs.getString("inOutCode");
			if (TriageCode.IN.equals(code)) 
				as.code = code;
			else if (TriageCode.OUT.equals(code) && !TriageCode.IN.equals(as.code))
				as.code = code;

			eof = !rs.next();
			
		}
		
		return as;
	}
	
	protected void error(String message) {
		logger.error(message);
	}

	@SuppressWarnings("unused")
	private void warn(String message) {
		logger.warn(message);
	}

	@SuppressWarnings("unused")
	private void debug(String message) {
		logger.error(message);
	}

	public Progress[] getProgress() {		
		Progress progress = new ProgressImpl(
				this.pos, 
				this.count, 
				Progress.ENTITIES);
		
        return new Progress[] { progress };
	}

}
