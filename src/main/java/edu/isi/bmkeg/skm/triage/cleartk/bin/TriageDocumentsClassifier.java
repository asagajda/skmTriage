package edu.isi.bmkeg.skm.triage.cleartk.bin;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.cleartk.classifier.CleartkAnnotator;
import org.cleartk.classifier.jar.DefaultDataWriterFactory;
import org.cleartk.classifier.jar.DirectoryDataWriterFactory;
import org.cleartk.classifier.jar.GenericJarClassifierFactory;
import org.cleartk.classifier.jar.JarClassifierBuilder;
import org.cleartk.classifier.libsvm.LIBSVMBooleanOutcomeDataWriter;
import org.cleartk.syntax.opennlp.SentenceAnnotator;
import org.cleartk.token.stem.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.springframework.flex.messaging.MessageTemplate;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.util.JCasUtil;

import edu.isi.bmkeg.digitalLibrary.model.citations.Corpus;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.skm.cleartk.type.CatorgorizedFtdText;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.SaveFeaturesToDbAnnotator;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.TriageDocumentGoldDocumentCategoryAnnotator;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.Uni_and_BigramCountAnnotator;
import edu.isi.bmkeg.skm.triage.cleartk.cr.TriageScoreCollectionReader;
import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.triage.model.TriageClassificationModel;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.triage.model.qo.TriageClassificationModel_qo;
import edu.isi.bmkeg.triage.model.qo.TriageCorpus_qo;
import edu.isi.bmkeg.triage.uimaTypes.TriageScore;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public class TriageDocumentsClassifier {

	public static class Options {

		@Option(name = "-triageCorpus", usage = "The triage corpus to be evaluated. It is required if -predict is used.", required = false, metaVar = "NAME")
		public String triageCorpus = "";

		@Option(name = "-targetCorpus", usage = "The target corpus that we're linking to", required = true, metaVar = "NAME")
		public String targetCorpus = "";

		@Option(name = "-homeDir", usage = "Directory where application data will be persisted", required = false, metaVar = "DIR")
		public File homeDir;

		@Option(name = "-l", usage = "Database login", required = true, metaVar = "LOGIN")
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true, metaVar = "PASSWD")
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true, metaVar = "DBNAME")
		public String dbName = "";

		@Option(name = "-train", usage = "If present will train and generate model, if absent will compute and update prediction scores in Triage Document. Either -train or -predict should be specified.")
		public boolean train = false;

		@Option(name = "-predict", usage = "If present will compute and update prediction scores in Triage Document. Either -train or -predict should be specified.")
		public boolean predict = false;
	}

	public static enum AnnotatorMode {
		TRAIN, CLASSIFY
	}

	public static String DATA_WRITER_NAME = LIBSVMBooleanOutcomeDataWriter.class
			.getName();

	public static String[] TRAINING_ARGS = new String[] { "-t", "0" };

	public String triageCorpus;
	public String targetCorpus;
	public File modelDir;
	public String login;
	public String password;
	public String dbName;

	private TriageEngine te;
	private CollectionReader cr;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// For use in a web-application context
	//
	// - to get feedback from the process for Flex messaging,
	// set msgTemplate to a value from the service using
	// classifier.
	// (e.g., edu.isi.bmkeg.triage.services.impl.ExtendedTriageServiceImpl)
	//
	// - to send responses to client via messaging,
	// (1) check if msgTemplate is not null
	// (2) call msgTemplate.send("serverUpdates",
	// "<count>/<max>, <text-message>");
	//
	private MessageTemplate msgTemplate;

	public MessageTemplate getMsgTemplate() {
		return msgTemplate;
	}

	public void setMsgTemplate(MessageTemplate msgTemplate) {
		this.msgTemplate = msgTemplate;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public TriageDocumentsClassifier(String triageCorpus, String targetCorpus,
			File modelDir, String login, String password, String dbName)
			throws Exception {

		this.triageCorpus = triageCorpus;
		this.targetCorpus = targetCorpus;
		this.modelDir = modelDir;
		this.login = login;
		this.password = password;
		this.dbName = dbName;
		te = new TriageEngine();
		te.initializeVpdmfDao(login, password, dbName);

	}

	public void extractFeatures() throws Exception {

		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("uimaTypes.triage",
						"edu.isi.bmkeg.skm.cleartk.TypeSystem");

		//
		// read the text from the database
		//
		CollectionReader cr = CollectionReaderFactory.createCollectionReader(
				TriageScoreCollectionReader.class, typeSystem,
				TriageScoreCollectionReader.TARGET_CORPUS_NAME, targetCorpus,
				TriageScoreCollectionReader.LOGIN, login,
				TriageScoreCollectionReader.PASSWORD, password,
				TriageScoreCollectionReader.DB_URL, dbName);

		AggregateBuilder builder = new AggregateBuilder();

		//
		// generic preprocessing steps
		//
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());
		builder.add(DefaultSnowballStemmer.getDescription("English"));

		//
		// saving explanation features to database
		//
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				SaveFeaturesToDbAnnotator.class,
				SaveFeaturesToDbAnnotator.PARAM_VPDMf_LOGIN, login,
				SaveFeaturesToDbAnnotator.PARAM_VPDMf_PASSWORD, password,
				SaveFeaturesToDbAnnotator.PARAM_VPDMf_DBNAME, dbName));

		//
		// generate description
		//
		AnalysisEngine ae = builder.createAggregate();

		for (JCas jCas : new JCasIterable(cr, ae)) {
			if (this.msgTemplate != null) {
				Progress[] pStack = cr.getProgress();
				Progress p = pStack[pStack.length - 1];
				this.msgTemplate.send("serverUpdates", p.getCompleted() + "/"
						+ p.getCompleted());
			}
		}
		ae.collectionProcessComplete();

	}

	public void train() throws Exception {

		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("uimaTypes.triage",
						"edu.isi.bmkeg.skm.cleartk.TypeSystem");

		//
		// read the text from the database
		//
		CollectionReader cr = CollectionReaderFactory.createCollectionReader(
				TriageScoreCollectionReader.class, typeSystem,
				TriageScoreCollectionReader.TRIAGE_CORPUS_NAME, triageCorpus,
				TriageScoreCollectionReader.TARGET_CORPUS_NAME, targetCorpus,
				TriageScoreCollectionReader.SKIP_UNKNOWNS, true,
				TriageScoreCollectionReader.LOGIN, login,
				TriageScoreCollectionReader.PASSWORD, password,
				TriageScoreCollectionReader.DB_URL, dbName);

		AggregateBuilder builder = new AggregateBuilder();

		//
		// generic preprocessing steps
		//
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());
		builder.add(DefaultSnowballStemmer.getDescription("English"));

		//
		// preprocessing to generate gold standards.
		//
		builder.add(AnalysisEngineFactory
				.createPrimitiveDescription(TriageDocumentGoldDocumentCategoryAnnotator.class));

		//
		// feature extraction
		//
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				Uni_and_BigramCountAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING, true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				DATA_WRITER_NAME,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, modelDir));

		//
		// generate description
		//
		AnalysisEngine ae = builder.createAggregate();

		for (JCas jCas : new JCasIterable(cr, ae)) {
			if (this.msgTemplate != null) {
				Progress[] pStack = cr.getProgress();
				Progress p = pStack[pStack.length - 1];
				this.msgTemplate.send("serverUpdates", p.toString());
			}
		}
		ae.collectionProcessComplete();

		// HideOutput hider = new HideOutput();
		JarClassifierBuilder.trainAndPackage(modelDir, TRAINING_ARGS);
		// hider.restoreOutput();

		// Finally, generate a TriageClassificationModel view
		// and insert it into the database.
		try {

			this.te.getDigLibDao().getCoreDao().getCe().connectToDB();
			this.te.getDigLibDao().getCoreDao().getCe().turnOffAutoCommit();

			TriageClassificationModel tcm = this
					.generateNewTriageClassificationModel();

			this.te.getDigLibDao().getCoreDao().getCe().commitTransaction();

		} catch (Exception e) {

			e.printStackTrace();
			this.te.getDigLibDao().getCoreDao().getCe().rollbackTransaction();

		} finally {

			this.te.getDigLibDao().getCoreDao().getCe().closeDbConnection();

		}

	}

	public void predict() throws Exception {

		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("uimaTypes.triage",
						"edu.isi.bmkeg.skm.cleartk.TypeSystem");

		//
		// read the text from the database
		//
		CollectionReader cr = CollectionReaderFactory.createCollectionReader(
				TriageScoreCollectionReader.class, typeSystem,
				TriageScoreCollectionReader.TRIAGE_CORPUS_NAME, triageCorpus,
				TriageScoreCollectionReader.TARGET_CORPUS_NAME, targetCorpus,
				TriageScoreCollectionReader.LOGIN, login,
				TriageScoreCollectionReader.PASSWORD, password,
				TriageScoreCollectionReader.DB_URL, dbName);

		AggregateBuilder builder = new AggregateBuilder();

		//
		// generic preprocessing steps
		//
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());
		builder.add(DefaultSnowballStemmer.getDescription("English"));

		//
		// feature extraction
		//
		File classifierJarPath = new File(modelDir, "model.jar");
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				Uni_and_BigramCountAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING, false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, classifierJarPath));

		//
		// generate description
		//
		AnalysisEngine ae = builder.createAggregate();

		Date timestamp = new Date();

		try {

			this.te.getDigLibDao().getCoreDao().getCe().connectToDB();
			this.te.getDigLibDao().getCoreDao().getCe().turnOffAutoCommit();

			for (JCas jCas : new JCasIterable(cr, ae)) {

				CatorgorizedFtdText document = JCasUtil.selectSingle(jCas,
						CatorgorizedFtdText.class);

				TriageScore ts = JCasUtil.selectSingle(jCas, TriageScore.class);
				long tsId = ts.getVpdmfId();

				te.updateInScore(tsId, document.getInScore(), timestamp);

				// send messages to Flex.
				if (this.msgTemplate != null) {
					Progress[] pStack = cr.getProgress();
					Progress p = pStack[pStack.length - 1];
					this.msgTemplate.send("serverUpdates", p.toString());
				}

			}

			ae.collectionProcessComplete();
			
			this.te.getDigLibDao().getCoreDao().getCe().commitTransaction();
			
		} catch (Exception e) {

			e.printStackTrace();
			this.te.getDigLibDao().getCoreDao().getCe().rollbackTransaction();

		} finally {

			this.te.getDigLibDao().getCoreDao().getCe().closeDbConnection();

		}

	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		CmdLineParser parser = new CmdLineParser(options);

		try {

			parser.parseArgument(args);

			boolean error = false;

			if (options.train && options.predict) {
				String errString = "Only one of -predict or -train can be specified.";
				throw new Exception(errString);
			}

			if (!options.train && !options.predict) {
				String errString = "One of -predict or -train should be specified.";
				throw new Exception(errString);
			}

			if ((options.triageCorpus == null || options.triageCorpus.length() == 0)
					&& options.predict) {
				String errString = "-triageCorpus is required if -predict is used.";
				throw new Exception(errString);
			}

		} catch (Exception e) {

			System.err.print("Usage: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			System.exit(-1);
		}

		if (options.homeDir == null) {
			System.out
					.println("homeDir parameter not specified. Using user's home directory instead.");
			options.homeDir = new File(System.getProperty("user.home")
					+ "/bmkeg");
		}

		if (!options.homeDir.exists()) {
			System.out.println("Home directory ["
					+ options.homeDir.getAbsolutePath()
					+ "] doesn't exists. Creating it ...");
			options.homeDir.mkdirs();
		}

		File modelDir = new File(options.homeDir, options.dbName + "/"
				+ options.targetCorpus);
		if (options.triageCorpus != null && options.triageCorpus.length() > 0)
			modelDir = new File(modelDir, options.triageCorpus);

		if (!modelDir.exists())
			modelDir.mkdirs();

		String triageCorpus = options.triageCorpus;
		String targetCorpus = options.targetCorpus;
		String login = options.login;
		String password = options.password;
		String dbName = options.dbName;
		boolean train = options.train;

		TriageDocumentsClassifier cl = new TriageDocumentsClassifier(
				triageCorpus, targetCorpus, modelDir, login, password, dbName);

		try {

			cl.te.getDigLibDao().getCoreDao().connectToDb();

			cl.run(train);

			cl.te.getDigLibDao().getCoreDao().commitTransaction();

		} catch (Exception e) {

			e.printStackTrace();
			cl.te.getDigLibDao().getCoreDao().rollbackTransaction();

		} finally {

			cl.te.getDigLibDao().getCoreDao().closeDbConnection();

		}

	}

	public void run(boolean train) throws Exception {

		if (train) {

			System.out.println("Training Triage Documents Classifier.");
			System.out.println("TriageCorpus: " + triageCorpus);
			System.out.println("TargetCorpus: " + targetCorpus);
			System.out.println("Model dir: " + modelDir);

			long startTime = System.currentTimeMillis();

			this.train();

			long endTime = System.currentTimeMillis();

			System.out.println("Elapsed time in mins: " + (endTime - startTime)
					/ 60000);

		} else {

			System.out.println("Predicting Triage Documents Classifier.");
			System.out.println("TriageCorpus: " + triageCorpus);
			System.out.println("TargetCorpus: " + targetCorpus);
			System.out.println("Model dir: " + modelDir);

			long startTime = System.currentTimeMillis();

			this.predict();

			long endTime = System.currentTimeMillis();

			System.out.println("Elapsed time in mins: " + (endTime - startTime)
					/ 60000);

		}

	}

	/**
	 * Generate a TriageClassificationModel view and insert it into the
	 * database.
	 * 
	 * @return
	 * @throws Exception
	 */
	private TriageClassificationModel generateNewTriageClassificationModel()
			throws Exception {

		TriageCorpus tc = null;
		if (this.triageCorpus != null && this.triageCorpus.length() > 0) {
			TriageCorpus_qo tcQo = new TriageCorpus_qo();
			tcQo.setName(this.triageCorpus);
			List<LightViewInstance> lviList = this.te.getDigLibDao()
					.getCoreDao().listInTrans(tcQo, "TriageCorpus");
			if (lviList.size() == 1) {
				tc = this.te
						.getDigLibDao()
						.getCoreDao()
						.findByIdInTrans(lviList.get(0).getVpdmfId(),
								new TriageCorpus(), "TriageCorpus");
			} else {
				throw new Exception("Triage Corpus " + triageCorpus
						+ "not found");
			}
		}

		Corpus_qo cQo = new Corpus_qo();
		cQo.setName(this.targetCorpus);
		Corpus c = null;

		List<LightViewInstance> lviList = this.te.getDigLibDao().getCoreDao()
				.listInTrans(cQo, "Corpus");
		if (lviList.size() == 1) {
			c = this.te
					.getDigLibDao()
					.getCoreDao()
					.findByIdInTrans(lviList.get(0).getVpdmfId(), new Corpus(),
							"ArticleCorpus");
		} else {
			throw new Exception("Target Corpus " + targetCorpus + "found");
		}

		String tcmName = this.targetCorpus;
		if (this.triageCorpus != null && this.triageCorpus.length() > 0)
			tcmName += "__" + this.triageCorpus;

		Date now = new Date();
		TriageClassificationModel_qo tcmQo = new TriageClassificationModel_qo();
		tcmQo.setName(tcmName);
		lviList = this.te.getDigLibDao().getCoreDao()
				.listInTrans(tcmQo, "TriageClassificationModel");
		TriageClassificationModel tcm = null;
		if (lviList.size() == 0) {

			tcm = new TriageClassificationModel();
			tcm.setWhenCreated(now);
			tcm.setName(tcmName);
			tcm.setTriageCorpus(tc);
			tcm.setTargetCorpus(c);
			tcm.setModelFilePath(this.modelDir.getPath());
			this.te.getDigLibDao().getCoreDao()
					.insertInTrans(tcm, "TriageClassificationModel");

		} else if (lviList.size() == 1) {

			tcm = this.te
					.getDigLibDao()
					.getCoreDao()
					.findByIdInTrans(lviList.get(0).getVpdmfId(),
							new TriageClassificationModel(),
							"TriageClassificationModel");
			tcm.setWhenCreated(now);
			tcm.setModelFilePath(this.modelDir.getPath());
			this.te.getDigLibDao().getCoreDao()
					.updateInTrans(tcm, "TriageClassificationModel");

		} else {

			throw new Exception("TriageClassificationModel " + tcmName
					+ " ambiguous.");

		}

		return tcm;

	}

}
