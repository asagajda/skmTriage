/** 
 * Copyright (c) 2007-2012, Regents of the University of Colorado 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.eval.Evaluation_ImplBase;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import edu.isi.bmkeg.skm.triage.cleartk.cr.filteredLineReader.FilteredLineReader;

/**
 * <br>
 * Copyright (c) 2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * <p>
 * This evaluation class provides a concrete example of how to train and
 * evaluate classifiers. Specifically this class will train a document
 * categorizer using a subset of the 20 newsgroups dataset. It evaluates
 * performance using 2-fold cross validation as well as a holdout set.
 * <p>
 * 
 * Key points: <br>
 * <ul>
 * <li>Creating training and evaluation pipelines
 * <li>Example of feature transformation / normalization
 * </ul>
 * 
 * 
 * @author Lee Becker
 */
public abstract class CrossValidationEvaluation extends
		Evaluation_ImplBase<Integer, AnnotationStatistics<String>> {

	private static Logger logger = Logger.getLogger(CrossValidationEvaluation.class);
	
	public static final String GOLD_VIEW_NAME = "DocumentClassificationGoldView";

	public static final String SYSTEM_VIEW_NAME = CAS.NAME_DEFAULT_SOFA;
	
	File dataDirectory;
	
	List<Integer> trainIds;
	List<Integer> testIds;
	
	int nFolds;

//	AnnotatorMode mode;
	
//	List<String> trainingArguments;

//	public static enum AnnotatorMode {
//		TRAIN, TEST, CLASSIFY
//	}

	public CrossValidationEvaluation(File baseDirectory, File dataDirectory) {
		super(baseDirectory);
		this.dataDirectory = dataDirectory;
	}

	public static List<File> getFilesFromDirectory(File directory) throws Exception {
		
		IOFileFilter fileFilter = FileFilterUtils
				.makeSVNAware(HiddenFileFilter.VISIBLE);
		IOFileFilter dirFilter = FileFilterUtils.makeSVNAware(FileFilterUtils.andFileFilter(
									  FileFilterUtils.directoryFileFilter(),
									  HiddenFileFilter.VISIBLE));
		
		if( !directory.exists() || !directory.isDirectory() )
			throw new Exception(directory.getPath() + " is not a directory!");
		
		return new ArrayList<File>(
				FileUtils.listFiles(directory, fileFilter, dirFilter)
				);
		
	}

	public AnnotationStatistics<String> runMain() throws Exception {

		if( nFolds == 0 ) 
			throw new Exception("Number of folds cannot be 0");
		
		File trainingDir = new File (this.dataDirectory.getPath() + "/train");
		File testingDir = new File (this.dataDirectory.getPath() + "/test");

		List<File> trainFiles = getFilesFromDirectory(trainingDir);
		List<File> testFiles = getFilesFromDirectory(testingDir);

		this.trainIds = this.loadIdsFromFiles(trainFiles);
		this.testIds = this.loadIdsFromFiles(testFiles);

		//
		// Runs the cross validation 
		//
		//this.mode = AnnotatorMode.TRAIN;
		List<AnnotationStatistics<String>> foldStats = this
				.crossValidation(this.trainIds, this.nFolds);
		AnnotationStatistics<String> crossValidationStats = AnnotationStatistics
				.addAll(foldStats);

		System.out.println("Cross Validation Results:");
		System.out.print(crossValidationStats);
		System.out.println();
		System.out.println(crossValidationStats.confusions());
		System.out.println();

		// Run Holdout Set
		//this.mode = AnnotatorMode.TEST;
		AnnotationStatistics<String> holdoutStats = this.trainAndTest(
				this.trainIds, this.testIds);
		System.out.println("Holdout Set Results:");
		System.out.print(holdoutStats);
		System.out.println();
		System.out.println(holdoutStats.confusions());
		
		return holdoutStats;
		
	}

	public AnnotationStatistics<String> runTrainAndTestOnly() throws Exception {

		if( nFolds == 0 ) 
			throw new Exception("Number of folds cannot be 0");
		
		File trainingDir = new File (this.dataDirectory.getPath() + "/train");
		File testingDir = new File (this.dataDirectory.getPath() + "/test");

		List<File> trainFiles = getFilesFromDirectory(trainingDir);
		List<File> testFiles = getFilesFromDirectory(testingDir);

		this.trainIds = this.loadIdsFromFiles(trainFiles);
		this.testIds = this.loadIdsFromFiles(testFiles);

		// Run Holdout Set
		//this.mode = AnnotatorMode.TEST;
		AnnotationStatistics<String> holdoutStats = this.trainAndTest(
				this.trainIds, this.testIds);
		System.out.println("Holdout Set Results:");
		System.out.print(holdoutStats);
		System.out.println();
		System.out.println(holdoutStats.confusions());
		
		return holdoutStats;
		
	}
	
	public List<Integer> loadIdsFromFiles(List<File> files)
			throws FileNotFoundException, IOException {
		
		Pattern patt = Pattern.compile("(\\d+)\\t(.*)$");
		List<Integer> ids = new ArrayList<Integer>();
		for( File file : files) {
			
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				Matcher m = patt.matcher(line);
				if( m.find() ) {
					Integer id = new Integer(m.group(1));
					ids.add(id);
				}
			}
			br.close();
			
		}
		
		return ids;
		
	}
	
	@Override
	protected CollectionReader getCollectionReader(List<Integer> items)
			throws Exception {
		
		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("uimaTypes.vpdmf-triage",
						"edu.isi.bmkeg.skm.cleartk.TypeSystem");
		
		Integer[] filterIds = items.toArray(new Integer[]{});
		
		String dataPath = this.dataDirectory.getPath();
		
		FilteredLineReader lr = (FilteredLineReader) CollectionReaderFactory
				.createCollectionReader( FilteredLineReader.class, typeSystem, 
						FilteredLineReader.PARAM_FILE_OR_DIRECTORY_NAME, dataPath,
						FilteredLineReader.PARAM_SUFFIXES, new String[]{".txt"},
						FilteredLineReader.PARAM_FILTER_IDS, filterIds,
						FilteredLineReader.PARAM_DELIMITER, "\t"
			    );
			
		return lr;
		
	}
	
	@Override
	public abstract void train(CollectionReader collectionReader, File outputDirectory) throws Exception;

	@Override
	protected abstract AnnotationStatistics<String> test(CollectionReader collectionReader,
			File directory) throws Exception;

}