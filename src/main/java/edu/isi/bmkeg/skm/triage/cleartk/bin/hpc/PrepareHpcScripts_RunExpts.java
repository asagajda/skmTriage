package edu.isi.bmkeg.skm.triage.cleartk.bin.hpc;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;

public class PrepareHpcScripts_RunExpts {

	public static class Options extends Options_ImplBase {
		@Option(name = "-triageCorpus", required = true, usage = "The triage corpus to be evaluated")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus",  required = true, usage = "The target corpus to be evaluated")
		public String targetCorpus = "";

		@Option(name = "-dir", required = true, usage = "Target directory")
		public File dir;
		
		@Option(name = "-nRepeats", required = false, usage = "Number of repeats")
		public int nRep = 10;
		
		@Option(name = "-hrs", required = false, usage = "Walltime (hours)")
		public int nHours = 10;
		
		@Option(name = "-features",  required = true, usage = "Feature sets being used " +
				"[BigramCount|TfIdf_|" +
				"UnigramCount|Uni_and_BigramCount|ALL]")
		public String features = "";
	
		@Option(name = "-classifier",  required = true, usage = "Classifier being used " +
				"[LibSvm]")
		public String classifier = "";

		@Option(name = "-params",  required = true, usage = "Params to set for the classifier" +
				"[see docs]")
		public String params = "";
		
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
		String[] features = {"BigramCount", 
				"TfIdf_", 
			"UnigramCount", 
			"Uni_and_BigramCount"
		};
		if( !options.features.toLowerCase().equals("all") ) {
			features = options.features.split(",");
		}
			
		for( int i=0; i<options.nRep; i++) {

			for( String f : features ) {

				String javaExec = "edu.isi.bmkeg.skm.triage.cleartk.bin.RunEvaluation";
				String setUpClExptScript = 
						"#!/bin/csh\n" +
						"#PBS -l nodes=1:ppn=2\n" +
						"#PBS -l walltime=0" + options.nHours + ":00:00\n" + 
						"cd " + options.dir + "\n" + 
						"source ${HOME}/.cshrc\n" + 
						"echo \"Running " + javaExec + "\"\n" + 				
						"java -Xmx4096M -Xms1024M " + javaExec +
						" -triageCorpus \"" + options.triageCorpus + "\" " +
						" -targetCorpus \"" + options.targetCorpus + "\" " + 
						" -dir \"" + options.dir + "/" + i + "\" " +
						" -features \"" + f + "Annotator\" " +
						" -classifier \"" + options.classifier + "\" " +
						" -params \"" + options.params + "\"\n\n";
				
				String sig = i + "_" + f + "_" + options.classifier + "_" + 
						options.params.replaceAll("\\s+", "");
				
				File setUpClExptScriptFile = new File(options.dir.getPath() + 
						"/pbsScripts/runExpt_" + sig + ".pbs"
						);
				FileUtils.writeStringToFile(setUpClExptScriptFile, setUpClExptScript);
	
				execSetUpExptScript += "qsub pbsScripts/runExpt_" + sig + ".pbs\n";

			}
			
		}		

		File execScriptFile = new File(options.dir.getPath() + 
				"/execRunExptScript.csh"
				);
		FileUtils.writeStringToFile(execScriptFile, execSetUpExptScript);
		
	}
	
}
