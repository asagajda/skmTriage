package edu.isi.bmkeg.skm.triage.cleartk.bin.hpc;

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

import edu.isi.bmkeg.skm.triage.cleartk.bin.SetUpClassificationExperiment;
import edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval.CrossValEval_Multiway;
import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;

public class PrepareHpcScripts {

	public static class Options extends Options_ImplBase {
		@Option(name = "-triageCorpus", required = true, usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus",  required = true, usage = "The target corpus to be evaluated")
		public String targetCorpus = "";

		@Option(name = "-dir", required = true, usage = "Target directory")
		public File dir;
		
		@Option(name = "-prop", required = false, usage = "Proportion of documents to be held out")
		public float prop = 0.1f;

		@Option(name = "-nRepeats", required = false, usage = "Number of repeats")
		public int nRep = 10;
		
		@Option(name = "-hrs", required = true, usage = "Walltime (hours)")
		public int nHours = 2;
				
	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.parseOptions(args);
		
		//
		// Generate scripts for a given run.
		// Assume data is in dir/baseDir
		// Put scripts into dir/psbScripts
		//
		String execSetUpExptScript = "#!/bin/csh\n";
		for( int i=0; i<options.nRep; i++) {
			
			String setUpClExptScript = 
					"#!/bin/csh\n" +
					"#PBS -l nodes=1:ppn=2\n" +						
					"#PBS -l walltime=0" + options.nHours + ":00:00\n" + 
					"cd " + options.dir + "\n" + 
					"source ${HOME}/.cshrc\n" + 
					"echo \"Running SetUpClassificationExperiment\"\n" + 				
					"java edu.isi.bmkeg.skm.triage.cleartk.bin.SetUpClassificationExperiment" +
					" -triageCorpus \"" + options.triageCorpus + "\" " +
					" -targetCorpus \"" + options.targetCorpus + "\" " + 
					" -dir \"" + options.dir + "/" + i + "\" " +
					" -prop \""+ options.prop + "\" " + 
					" -baseData \"" + options.dir + "/baseData\"\n\n";
			
			File setUpClExptScriptFile = new File(options.dir.getPath() + 
					"/pbsScripts/setUpExpt_" + i + ".pbs"
					);
			FileUtils.writeStringToFile(setUpClExptScriptFile, setUpClExptScript);

			execSetUpExptScript += "qsub pbsScripts/setUpExpt_" + i + ".pbs\n";
			
		}		

		File execScriptFile = new File(options.dir.getPath() + 
				"/execSetUpExptScript.csh"
				);
		FileUtils.writeStringToFile(execScriptFile, execSetUpExptScript);
		
	}
	
}
