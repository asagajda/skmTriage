package edu.isi.bmkeg.skm.triage.cleartk.bin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cleartk.ml.libsvm.LibSvmBooleanOutcomeDataWriter;
import org.cleartk.eval.AnnotationStatistics;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_Multiway;
import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;

public class RunEvaluation {

	public static class Options extends Options_ImplBase {
		@Option(name = "-triageCorpus", required = true, usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus", required = true, usage = "The target corpus to be evaluated")
		public String targetCorpus = "";

		@Option(name = "-dir", required = true,  usage = "Base directory")
		public File dir;

		@Option(name = "-features", required = true, usage = "The feature set to be used")
		public String features = "";

		@Option(name = "-classifier", required = true, usage = "The classifier to be used")
		public String engine = "";

		@Option(name = "-params", required = true, usage = "Parameters for the classifier")
		public String params = "";

		@Option(name = "-nFolds", required = false, usage = "N folds for cross validation")
		public int nFolds = 4;
				
	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.parseOptions(args);

		String dirPath = options.dir.getPath().replaceAll("\n", "");
		File dir = new File(dirPath); 
		if( !dir.exists()  ) {
			dir.mkdirs(); 
		}

		List<Map<String,AnnotationStatistics<String>>> statsList = new ArrayList
				<Map<String,AnnotationStatistics<String>>>();

		Map<String,AnnotationStatistics<String>> statsMap = new HashMap
					<String,AnnotationStatistics<String>>();
		statsList.add(statsMap);
			
		String triageCorpusName = options.triageCorpus.replaceAll("\\s+", "_");
		String targetCorpusName = options.targetCorpus.replaceAll("\\s+", "_");

		triageCorpusName = triageCorpusName.replaceAll("\\/", "_");
		targetCorpusName = targetCorpusName.replaceAll("\\/", "_");
			
		File dataDir2 = new File(dir.getPath() + 
					"/data/" + targetCorpusName + 
					"/" + triageCorpusName );
			
		File analysisDir = new File(dir.getPath() + "/" + options.features + "/" + options.engine);
		analysisDir.mkdirs();			
		CrossValEval_Multiway eval2 = new CrossValEval_Multiway(
					analysisDir,
					Arrays.asList(options.params.split("\\s+")),
					options.engine,
					options.features,
					options.nFolds,
					dataDir2);
			
		AnnotationStatistics<String> stats = eval2.runTrainAndTestOnly();
						
		StringBuilder sb = new StringBuilder();
		sb.append("features\tengine\tparams\tP\tR\tF1\t#gold\t#system\t#correct\tCategory\n");
		String raw = stats.toString();
		String[] lines = raw.split("\n");
		for( int j=1; j<lines.length; j++) {
			sb.append( options.features );
			sb.append( "\t" );
			sb.append( options.engine );
			sb.append( "\t" );
			sb.append( options.params.replaceAll("\\t", " ") );
			sb.append( "\t" );
			sb.append( lines[j] );
			sb.append("\n");					
		}
		String results = sb.toString();
		File resultsFile = new File(dir.getPath() + "/results_" + 
				options.features + "_" + 
				options.engine + "_" + 
				options.params.replaceAll("\\s+", "") +".txt");
		FileUtils.writeStringToFile(resultsFile, results);
		
	}
	
}
