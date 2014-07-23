package edu.isi.bmkeg.skm.triage.cleartk.bin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cleartk.eval.AnnotationStatistics;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.triage.cleartk.annotators.MutualInformation_Annotator;
import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_FeatureSelection;
import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;

public class AnalyzeFeatures {

	public static class Options extends Options_ImplBase {
		@Option(name = "-triageCorpus", required = true, usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus", required = true, usage = "The target corpus to be evaluated")
		public String targetCorpus = "";

		@Option(name = "-dir", required = true,  usage = "Base directory")
		public File dir;

		@Option(name = "-classifier", required = false, usage = "The classifier to be used")
		public String engine = "LibLinear";

		@Option(name = "-params", required = false, usage = "Parameters for the classifier")
		public String params = "-t 0";

		@Option(name = "-nFolds", required = false, usage = "N folds for cross validation")
		public int nFolds = 4;
				
		@Option(name = "-mode", required = false, usage = "[uni / bi / tri / all]")
		public String mode = "uni";
		
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
			
		File analysisDir = new File(dir.getPath() + "/MutualInformation/" + options.engine);
		analysisDir.mkdirs();			
		CrossValEval_FeatureSelection eval2 = new CrossValEval_FeatureSelection(
					analysisDir,
					Arrays.asList(options.params.split("\\s+")),
					options.engine,
					options.nFolds,
					dataDir2);
			
		String mode = MutualInformation_Annotator.UNI_MODE;
		if( options.mode.equals("uni") ) {
			mode = MutualInformation_Annotator.UNI_MODE;
		} else if( options.mode.equals("bi") ) {
			mode = MutualInformation_Annotator.BI_MODE;
		} else if( options.mode.equals("tri") ) {
			mode = MutualInformation_Annotator.TRI_MODE;
		} else if( options.mode.equals("all") ) {
			mode = MutualInformation_Annotator.ALL_MODE;
		} else {
			throw new Exception(options.mode + " is not uni / bi / tri / all");
		}
			
		eval2.runFeatureAnalysis(mode);
		
		/*AnnotationStatistics<String> stats = 
						
		StringBuilder sb = new StringBuilder();
		sb.append("features\tengine\tparams\tP\tR\tF1\t#gold\t#system\t#correct\tCategory\n");
		String raw = stats.toString();
		String[] lines = raw.split("\n");
		for( int j=1; j<lines.length; j++) {
			sb.append( "MutualInformation" );
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
				"MutualInformation" + "_" + 
				options.engine + "_" + 
				options.params.replaceAll("\\s+", "") +".txt");
		FileUtils.writeStringToFile(resultsFile, results);*/
		
	}
	
}
