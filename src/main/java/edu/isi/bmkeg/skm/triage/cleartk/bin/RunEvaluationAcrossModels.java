package edu.isi.bmkeg.skm.triage.cleartk.bin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cleartk.classifier.liblinear.LibLinearBooleanOutcomeDataWriter;
import org.cleartk.classifier.libsvm.LibSvmBooleanOutcomeDataWriter;
import org.cleartk.classifier.opennlp.MaxentBooleanOutcomeDataWriter;
import org.cleartk.classifier.svmlight.SvmLightBooleanOutcomeDataWriter;
import org.cleartk.classifier.tksvmlight.TkSvmLightBooleanOutcomeDataWriter;
import org.cleartk.eval.AnnotationStatistics;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_BigramCount;
import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;

public class RunEvaluationAcrossModels {

	public static String USAGE = "-corpus <corpusName> -dir <dir> -prop <propHeldOut> -l <login> -p <password> -db <dbName>";

	public static class Options extends Options_ImplBase {
		@Option(name = "-triageCorpus", usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus", required = true, usage = "The target corpus to be evaluated")
		public String targetCorpus = "";

		@Option(name = "-dir", required = true, usage = "Target directory")
		public File dir;

		@Option(name = "-prop", usage = "Proportion of documents to be held out")
		public float prop = 0.0f;

		@Option(name = "-annotator", usage = "Features to use")
		public String annotatorString = "BigramCountAnnotator";

		@Option(name = "-nFolds", usage = "N folds for cross validation")
		public int nFolds = 4;

		@Option(name = "-l", usage = "Database login")
		public String login = "";

		@Option(name = "-p", usage = "Database password")
		public String password = "";

		@Option(name = "-db", usage = "Database name")
		public String dbName = "";
		
		@Option(name = "-wd", usage = "Working Directory")
		public File workingDirectory;
	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.parseOptions(args);
		
		String dirPath = options.dir.getPath().replaceAll("\n", "");
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		List<Map<String, AnnotationStatistics<String>>> statsList = new ArrayList<Map<String, AnnotationStatistics<String>>>();

		Map<String, AnnotationStatistics<String>> statsMap = new HashMap<String, AnnotationStatistics<String>>();
		statsList.add(statsMap);

		File dataDir = new File(dir.getPath() + "/data");
		dataDir.mkdirs();
		String[] args2 = new String[] { "-triageCorpus",
				options.triageCorpus, "-targetCorpus",
				options.targetCorpus, "-dir", dataDir.getPath(), "-prop",
				options.prop + "", "-db", options.dbName, "-l",
				options.login, "-p", options.password, 
				"-wd", options.workingDirectory.getPath() };
		PreprocessTriageScores.main(args2);

		String triageCorpusName = options.triageCorpus.replaceAll("\\s+",
				"_");
		String targetCorpusName = options.targetCorpus.replaceAll("\\s+",
				"_");
		File dataDir2 = new File(dataDir.getPath() + "/" + targetCorpusName
				+ "/" + triageCorpusName);

		Map<String, Class> modelMap = new HashMap<String, Class>();
		//modelMap.put("libLinear", LibLinearBooleanOutcomeDataWriter.class);
		//modelMap.put("mallet1", MalletBooleanOutcomeDataWriter.class);
		//modelMap.put("mallet2", MalletBooleanOutcomeDataWriter.class);
		//modelMap.put("mallet3", MalletBooleanOutcomeDataWriter.class);
		//modelMap.put("mallet4", MalletBooleanOutcomeDataWriter.class);
		//modelMap.put("maxent", MaxentBooleanOutcomeDataWriter.class);
		//modelMap.put("svmLight", SvmLightBooleanOutcomeDataWriter.class);
		//modelMap.put("TkSvmLight", TkSvmLightBooleanOutcomeDataWriter.class);

		Map<String, String> paramMap = new HashMap<String, String>();
		//paramMap.put("libLinear", "");
		//paramMap.put("mallet1", "NaiveBayes");
		//paramMap.put("mallet2", "C45");
		//paramMap.put("mallet3", "MaxEnt");
		//paramMap.put("mallet4", "RankMaxEnt");
		//paramMap.put("maxent", "");
		//paramMap.put("svmLight", "-z c");
		//paramMap.put("TkSvmLight", "-z c");
		
		modelMap.put("libSvm", LibSvmBooleanOutcomeDataWriter.class);
		paramMap.put("libSvm", "-t 0 ");
		
		for( int c=-5; c<=2; c++) {
			//for( int g=-8; g<=2; g++) {				
				String s = "-c " + Math.pow(10, c);
				//String s = "-c " + Math.pow(10, c) + " -g " + Math.pow(10, g);
				String ss = s.replaceAll("\\s+", "_");
				modelMap.put("libSvm" + ss, LibSvmBooleanOutcomeDataWriter.class);
				paramMap.put("libSvm" + ss , "-t 0 " + s );
			//}
		}
		
		String[] keys = modelMap.keySet().toArray(new String[modelMap.keySet().size()]);
		Arrays.sort(keys);
			
		for (String key : keys) {
			File modelDir = new File(dir.getPath() + "/" + key
					+ "_" + options.annotatorString);
			modelDir.mkdirs();
			System.out.println( key );
			CrossValEval_BigramCount eval2 = new CrossValEval_BigramCount(
					modelDir,
					Arrays.asList(paramMap.get(key).split("\\s+")),
					modelMap.get(key).getName(),
					// options.annotatorString,
					options.nFolds, dataDir2);
			
			statsMap.put(key + "_" + options.annotatorString,
					eval2.runTrainAndTestOnly());
		}

		StringBuilder sb = new StringBuilder();
		sb.append("i\tset\tP\tR\tF1\t#gold\t#system\t#correct\tCategory\n");
		for (int i = 0; i < statsList.size(); i++) {
			Map<String, AnnotationStatistics<String>> map = statsList.get(i);
			for (String key : map.keySet()) {
				AnnotationStatistics<String> stats = map.get(key);
				String raw = stats.toString();
				String[] lines = raw.split("\n");
				for (int j = 1; j < lines.length; j++) {
					sb.append(i);
					sb.append("\t");
					sb.append(key);
					sb.append("\t");
					sb.append(lines[j]);
					sb.append("\n");
				}
			}
		}
		String results = sb.toString();
		File resultsFile = new File(dir.getPath() + "/results.txt");
		FileUtils.writeStringToFile(resultsFile, results);

	}

}
