package edu.isi.bmkeg.skm.triage.cleartk.bin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cleartk.classifier.libsvm.LibSvmBooleanOutcomeDataWriter;
import org.cleartk.eval.AnnotationStatistics;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_BigramCount;
import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_TfIdf;
import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_Uni_and_BigramCount;
import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_UnigramCount;
import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;

public class RunEvaluationOld {

	public static String USAGE = 
			"-corpus <corpusName> -dir <dir> -prop <propHeldOut> -l <login> -p <password> -db <dbName>";
	
	public static class Options extends Options_ImplBase {
		@Option(name = "-triageCorpus", usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus", usage = "The target corpus to be evaluated")
		public String targetCorpus = "";

		@Option(name = "-dir", usage = "Target directory")
		public File dir;
		
		@Option(name = "-prop", usage = "Proportion of documents to be held out")
		public float prop = 0.0f;

		@Option(name = "-nRepeats", usage = "Proportion of documents to be held out")
		public int nRep = 1;

		@Option(name = "-nFolds", usage = "N folds for cross validation")
		public int nFolds = 4;
		
		@Option(name = "-l", usage = "Database login")
		public String login = "";

		@Option(name = "-p", usage = "Database password")
		public String password = "";

		@Option(name = "-db", usage = "Database name")
		public String dbName = "";

	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.parseOptions(args);

		if (options.targetCorpus.length() == 0 
				|| options.triageCorpus.length() == 0 
				|| options.login.length() == 0
				|| options.password.length() == 0
				|| options.dbName.length() == 0) {
			System.err.print(USAGE);
			System.exit(-1);
		}

		String dirPath = options.dir.getPath().replaceAll("\n", "");
		File dir = new File(dirPath); 
		if( !dir.exists()  ) {
			dir.mkdirs(); 
		}

		List<Map<String,AnnotationStatistics<String>>> statsList = new ArrayList
				<Map<String,AnnotationStatistics<String>>>();

		
		// Run entire evaluation across multiple classifiers N times, 
		// print out the results.
		for( int i=0; i<options.nRep; i++) {

			Map<String,AnnotationStatistics<String>> statsMap = new HashMap
					<String,AnnotationStatistics<String>>();
			statsList.add(statsMap);
			
			File dataDir = new File(dir.getPath() + "/" + i + "/data");
			dataDir.mkdirs();
			String[] args2 = new String[] { 
					"-triageCorpus", options.triageCorpus,
					"-targetCorpus", options.targetCorpus,
					"-dir", dataDir.getPath(),
					"-prop", options.prop + "",
					"-db", options.dbName, 
					"-l", options.login, 
					"-p",options.password
			};
			SetUpClassificationExperiment.main(args2);
			
			String triageCorpusName = options.triageCorpus.replaceAll("\\s+", "_");
			String targetCorpusName = options.targetCorpus.replaceAll("\\s+", "_");
			triageCorpusName = triageCorpusName.replaceAll("\\/", "_");
			targetCorpusName = targetCorpusName.replaceAll("\\/", "_");
			
			File dataDir2 = new File(dataDir.getPath() + 
					"/" + targetCorpusName + 
					"/" + triageCorpusName );
			
			File unigramDir = new File(dir.getPath() + "/" + i + "/unigram");
			unigramDir.mkdirs();			
			CrossValEval_UnigramCount eval2 = new CrossValEval_UnigramCount(
					unigramDir,
					Arrays.asList("-t", "0"),
					LibSvmBooleanOutcomeDataWriter.class.getName(),
					options.nFolds,
					dataDir2);
			statsMap.put("unigrams", eval2.runMain());
						
			File bigramDir = new File(dir.getPath() + "/" + i + "/bigram");
			bigramDir.mkdirs();			
			CrossValEval_BigramCount eval3 = new CrossValEval_BigramCount(
					bigramDir,
					Arrays.asList("-t", "0"),
					LibSvmBooleanOutcomeDataWriter.class.getName(),
					options.nFolds,
					dataDir2);
			statsMap.put("bigrams", eval3.runMain());

			File uniBiDir = new File(dir.getPath() + "/" + i + "/uniBigram");
			uniBiDir.mkdirs();			
			CrossValEval_Uni_and_BigramCount eval4 = new CrossValEval_Uni_and_BigramCount(
					uniBiDir,
					Arrays.asList("-t", "0"),
					LibSvmBooleanOutcomeDataWriter.class.getName(),
					options.nFolds,
					dataDir2);
			statsMap.put("uni+bigrams", eval4.runMain());

			File featureEngineerDir = new File(dir.getPath() + "/" + i + "/uniTfIdf");
			featureEngineerDir.mkdirs();			
			CrossValEval_TfIdf eval5 = new CrossValEval_TfIdf(
					featureEngineerDir,
					Arrays.asList("-t", "0"),
					LibSvmBooleanOutcomeDataWriter.class.getName(),
					options.nFolds,
					dataDir2);
			statsMap.put("uniTfIdf+bigrams", eval5.runMain());
			
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("i\tset\tP\tR\tF1\t#gold\t#system\t#correct\tCategory\n");
		for( int i=0; i<statsList.size(); i++ ){
			Map<String,AnnotationStatistics<String>> map = statsList.get(i);			
			for( String key : map.keySet() ) {
				AnnotationStatistics<String> stats = map.get(key);
				String raw = stats.toString();
				String[] lines = raw.split("\n");
				for( int j=1; j<lines.length; j++) {
					sb.append( i );
					sb.append( "\t" );
					sb.append( key );
					sb.append( "\t" );
					sb.append( lines[j] );
					sb.append("\n");					
				}	
			}
		}
		String results = sb.toString();
		File resultsFile = new File(dir.getPath() + "/results.txt");
		FileUtils.writeStringToFile(resultsFile, results);
		
	}
	
}
