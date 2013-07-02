package edu.isi.bmkeg.skm.triage.cleartk.cr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;

import edu.isi.bmkeg.skm.core.exceptions.StringCleanerException;
import edu.isi.bmkeg.skm.core.uima.cr.DirectoryCollectionReader;
import edu.isi.bmkeg.skm.core.utils.ISI_UIMA_Util;
import edu.isi.bmkeg.skm.core.utils.ReadWriteTextFileWithEncoding;
import edu.isi.bmkeg.skm.core.utils.string.LocalIdentifierToIDExtractor;
import edu.isi.bmkeg.skm.triage.utils.string.MGILocalIdentifierToIDExtractor;

public class MGIDirectoryCollectionReader extends DirectoryCollectionReader
{
	public static String PDF = "pdf";
	@Override
	public void initialize() throws ResourceInitializationException
	{
		fileSuffixesToProcess = new ArrayList<String>();
		numberOfFilesProcessed = 0;
		/* get input parameters from descriptor file */
		directory = (String) getConfigParameterValue(PARAM_DIRECTORY_PATH);
		itemsToSkip = (Integer) getConfigParameterValue(PARAM_ITEMS_TO_SKIP);
		endIndex = (Integer) getConfigParameterValue(PARAM_END_INDEX);
		recurseIntoDirectoryStructure = ((Boolean) getConfigParameterValue(PARAM_RECURSE_INTO_DIRECTORY_STRUCTURE))
		.booleanValue();

		fileSuffix = (String) getConfigParameterValue(PARAM_FILE_SUFFIX);
		fileSuffixesToProcess.add(fileSuffix);
		
		identiferPattern = (String) getConfigParameterValue(PARAM_ID_PATTERN_IN_FILENAME);
		if(identiferPattern.startsWith(MGILocalIdentifierToIDExtractor.MGI_ID))
			filenameToIDTranslator = new MGILocalIdentifierToIDExtractor(false,MGILocalIdentifierToIDExtractor.MGI_ID.replace(":", "")+LocalIdentifierToIDExtractor.defaultPattern);
		else
			filenameToIDTranslator = new MGILocalIdentifierToIDExtractor(true,MGILocalIdentifierToIDExtractor.JID.replace(":", "")+LocalIdentifierToIDExtractor.defaultPattern);
		
		/* initialize list to hold files to process */
		filesToProcess = new ArrayList<File>();

		/* Recurse through directories to get files to process */
		System.err
		.println("Initializing DirectoryOfFilesCollectionReader on directory: "
				+ directory);
		File root = new File(directory);
		if (root.isFile()) {
			filesToProcess.add(root);

		} else if (root.isDirectory()) {
			processDirectory(root, filesToProcess);

		} else {
			error("Invalid input detected. Document collection root is neither a file nor a directory.");

		}
		fileIterator = filesToProcess.iterator();
		if(itemsToSkip>0){
			int i = itemsToSkip;
			while(i>0){
				fileIterator.next();
				i--;
			}
			System.out.println("Skipping "+itemsToSkip+" files");
		}
		System.err.println("CR initialization complete. # files to process: "
				+ filesToProcess.size());
		//filesToProcess = null;
	}
	/**
	 * @see com.ibm.uima.collection.CollectionReader#getNext(com.ibm.uima.cas.CAS)
	 */
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		JCas jcas;
		String fullPath = null;
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			throw new CollectionException(e);
		}
		String name=fileIterator.next().getAbsolutePath();
		System.out.println(name);
		fullPath = name;
		if(!fileSuffix.contains(PDF))
			jcas.setDocumentText(ReadWriteTextFileWithEncoding.read(name, "UTF-8"));
		numberOfFilesProcessed++;
		try
		{
			name = filenameToIDTranslator.cleanItUp(name);
			System.out.println(name);
		} catch (StringCleanerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		try
		{
			String idPrefix = filenameToIDTranslator.getIdType().replace(":", "");
			StringArray s = new StringArray(jcas, 2);
			s.set(0, name.replace(idPrefix, filenameToIDTranslator.getIdType()));
			s.set(1, "PATH:"+fullPath);
			ISI_UIMA_Util.setDocumentSecondaryIDs(jcas, s);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
}