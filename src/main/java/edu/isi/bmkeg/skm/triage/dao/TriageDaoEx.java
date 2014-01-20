package edu.isi.bmkeg.skm.triage.dao;

import java.util.List;
import java.util.Map;

import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.triage.model.TriageScore;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

/**
 * Defines the interface to a Data Access Object that manage the data persistent
 * storage. A Spring bean implementing this interface can be injected in other
 * Spring beans, like the ArticleServiceImpl bean.
 * 
 */
public interface TriageDaoEx {

	public void setCoreDao(CoreDao coreDao);

	public CoreDao getCoreDao();

	// ~~~~~~~~~~~~~~~
	// Count functions
	// ~~~~~~~~~~~~~~~
	
	public int countTriagedArticlesInCorpus(String corpusName) throws Exception;
	
	// ~~~~~~~~~~~~~~~~~~~
	// Insert Functions
	// ~~~~~~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~~~~~~
	// Update Functions
	// ~~~~~~~~~~~~~~~~~~~
	
	// ~~~~~~~~~~~~~~~~~~~
	// Delete Functions
	// ~~~~~~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~~~~~~~
	// Find by id Functions
	// ~~~~~~~~~~~~~~~~~~~~

	public TriageCorpus findTriageCorpusByName(String name) throws Exception;

	// ~~~~~~~~~~~~~~~~~~~~
	// check Functions
	// ~~~~~~~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~~~~~~~
	// Retrieve functions
	// ~~~~~~~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~
	// List functions
	// ~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~~~~~~~
	// Add x to y functions
	// ~~~~~~~~~~~~~~~~~~~~

	public void addTriageDocumentsToCorpus(String triageCorpus, 
			String targetCorpus,
			Map<Integer, String> pmidCodes) throws Exception;



	// ~~~~~~~~~~~~~~~~~~~~~~~~~
	// Remove x from y functions
	// ~~~~~~~~~~~~~~~~~~~~~~~~~
	

}