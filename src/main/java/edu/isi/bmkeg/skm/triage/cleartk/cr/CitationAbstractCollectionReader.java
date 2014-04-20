package edu.isi.bmkeg.skm.triage.cleartk.cr;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.ArticleCitation_qo;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.uimaTypes.ViewTable;

/**
 * This Collection Reader runs over every citation in a given corpus to provide
 * access to the abstract, the full-text of the document or a predefined section
 * of the document. We want to optimize this interaction for speed, so we run a
 * manual query over the underlying database involving a minimal subset of
 * tables.
 * 
 * @author burns
 * 
 */
public class CitationAbstractCollectionReader extends CollectionReader_ImplBase {

	private static Logger logger = Logger.getLogger(CitationAbstractCollectionReader.class);

	public static final String START_AT = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"startAt");
	@ConfigurationParameter(mandatory = false, description = "The starting point of the read.")
	protected int startAt;

	public static final String END_AT = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"endAt");
	@ConfigurationParameter(mandatory = false, description = "The ending point of the read.")
	protected int endAt;

	public static final String CORPUS_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"corpusName");
	@ConfigurationParameter(mandatory = false, description = "The name of the corpus to be read")
	protected String corpusName;

	public static final String LOGIN = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"login");
	@ConfigurationParameter(mandatory = false, description = "Login for the Digital Library")
	protected String login;

	public static final String PASSWORD = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"password");
	@ConfigurationParameter(mandatory = false, description = "Password for the Digital Library")
	protected String password;

	public static final String DB_URL = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"dbUrl");
	@ConfigurationParameter(mandatory = false, description = "The Digital Library URL")
	protected String dbUrl;

	public static final String WORKING_DIRECTORY = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"workingDirectory");
	@ConfigurationParameter(mandatory = false, description = "The Digital Library URL")
	protected String workingDirectory;
	
	protected ResultSet rs;

	protected long startTime, endTime;

	protected int pos = 0, count = 0;

	protected TriageEngine te;

	private Iterator<LightViewInstance> it;

	public static CollectionReader load(
			String corpusName, String login,
			String password, String dbName,
			String workingDirectory)
			throws ResourceInitializationException {

		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("uimaTypes.triage");
		
		return CollectionReaderFactory.createCollectionReader(
				CitationAbstractCollectionReader.class, typeSystem, 
				CORPUS_NAME, corpusName,
				LOGIN, login, 
				PASSWORD, password, 
				DB_URL, dbName, 
				WORKING_DIRECTORY, workingDirectory);
	
	}
	
	@Override
	public void initialize() throws ResourceInitializationException {

		// get input parameters
		//startAt = (Integer) getConfigParameterValue(START_AT);
		//endAt = (Integer) getConfigParameterValue(END_AT);
		corpusName = (String) getConfigParameterValue(CORPUS_NAME);
		login = (String) getConfigParameterValue(LOGIN);
		password = (String) getConfigParameterValue(PASSWORD);
		dbUrl = (String) getConfigParameterValue(DB_URL);
		workingDirectory = (String) getConfigParameterValue(WORKING_DIRECTORY);
		
		try {

			te = new TriageEngine();
			te.initializeVpdmfDao(login, password, dbUrl, workingDirectory);
			
			ArticleCitation_qo acQo = new ArticleCitation_qo();
			Corpus_qo cQo = new Corpus_qo();
			acQo.getCorpora().add(cQo);
			cQo.setName(corpusName);	
			this.count = te.getDigLibDao().countArticleCitation(acQo);

			List<LightViewInstance> lviList = te.getDigLibDao().listArticleCitationDocument(acQo);
			
			te.getDigLibDao().getCoreDao().getCe().connectToDB();
			
			this.it = lviList.iterator();
			this.startTime = System.currentTimeMillis();
			
		} catch (Exception e) {

			throw new ResourceInitializationException(e);

		}

	}

	/**
	 * @see com.ibm.uima.collection.CollectionReader#getNext(com.ibm.uima.cas.CAS)
	 */
	public void getNext(CAS aCAS) throws IOException, CollectionException {

		try {

			JCas jcas = aCAS.getJCas();
			
			LightViewInstance lvi = this.it.next();
			
			// ArticleCitation view instance from the database
			ArticleCitation cit = this.te.getDigLibDao().findArticleCitationById(lvi.getVpdmfId());
						
			if( cit.getAbstractText() == null )
			    jcas.setDocumentText("");
			else
				jcas.setDocumentText( cit.getAbstractText() );

		    ViewTable vpdmfEntity = new ViewTable(jcas);
		    vpdmfEntity.setVpdmfId(cit.getVpdmfId());
		    vpdmfEntity.addToIndexes(jcas);
		    
		} catch (Exception e) {

			throw new CollectionException(e);

		}

	}

	/**
	 * @see com.ibm.uima.arg0collection.base_cpm.BaseCollectionReader#hasNext()
	 */
	public boolean hasNext() throws IOException, CollectionException {

		return this.it.hasNext();

	}

	public void close() throws IOException {
		try {
			te.getDigLibDao().getCoreDao().getCe().closeDbConnection();
		} catch (Exception e) {
			throw new IOException(e);
		}
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
		// TODO Auto-generated method stub
		return null;
	}

}
