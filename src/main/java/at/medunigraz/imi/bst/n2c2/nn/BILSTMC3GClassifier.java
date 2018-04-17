package at.medunigraz.imi.bst.n2c2.nn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingModelSaver;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.saver.InMemoryModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxScoreIterationTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition;
import org.deeplearning4j.earlystopping.termination.ScoreImprovementEpochTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.earlystopping.trainer.IEarlyStoppingTrainer;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.eval.EvaluationBinary;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesBidirectionalLSTM;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.conf.preprocessor.FeedForwardToRnnPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.RnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import at.medunigraz.imi.bst.n2c2.classifier.Classifier;
import at.medunigraz.imi.bst.n2c2.model.Criterion;
import at.medunigraz.imi.bst.n2c2.model.Eligibility;
import at.medunigraz.imi.bst.n2c2.model.Patient;

/**
 * BI-LSTM classifier for n2c2 task 2018 refactored from dl4j examples.
 * 
 * @author Markus
 *
 */
public class BILSTMC3GClassifier implements Classifier {

	// size of mini-batch for training
	private int miniBatchSize = 10;

	// length for truncated backpropagation through time
	private int tbpttLength = 50;

	// total number of training epochs
	private int nEpochs = 35;

	// specifies time series length
	public int truncateLength = 64;

	public int vectorSize;

	// training data
	private List<Patient> patientExamples;

	// multi layer network
	private MultiLayerNetwork net;

	// criterion index
	private Map<Criterion, Integer> criterionIndex = new HashMap<Criterion, Integer>();

	public NGramIterator fullSetIteration;

	private static final Logger LOG = LogManager.getLogger();

	public BILSTMC3GClassifier() {
		throw new org.apache.commons.lang.NotImplementedException(
				"BILSTMClassifier empty constructor is not implemented!");
	}

	public BILSTMC3GClassifier(String pathToWordVectors, String modelName) {

		initializeCriterionIndex();

		// TODO load from persisted variable via properties file
		this.truncateLength = 0;
		this.initializeNetworkFromFile(modelName);

	}

	public BILSTMC3GClassifier(List<Patient> examples) {

		this.patientExamples = examples;

		initializeNetworkBinaryMultiLabel();
		// initializeNetworkDebug();
		initializeMonitoring();

		LOG.info("Minibatchsize  :\t" + miniBatchSize);
		LOG.info("tbptt length   :\t" + tbpttLength);
		LOG.info("Epochs         :\t" + nEpochs);
		LOG.info("Truncate length:\t" + truncateLength);
	}

	private void initializeCriterionIndex() {
		this.criterionIndex.put(Criterion.ABDOMINAL, 0);
		this.criterionIndex.put(Criterion.ADVANCED_CAD, 1);
		this.criterionIndex.put(Criterion.ALCOHOL_ABUSE, 2);
		this.criterionIndex.put(Criterion.ASP_FOR_MI, 3);
		this.criterionIndex.put(Criterion.CREATININE, 4);
		this.criterionIndex.put(Criterion.DIETSUPP_2MOS, 5);
		this.criterionIndex.put(Criterion.DRUG_ABUSE, 6);
		this.criterionIndex.put(Criterion.ENGLISH, 7);
		this.criterionIndex.put(Criterion.HBA1C, 8);
		this.criterionIndex.put(Criterion.KETO_1YR, 9);
		this.criterionIndex.put(Criterion.MAJOR_DIABETES, 10);
		this.criterionIndex.put(Criterion.MAKES_DECISIONS, 11);
		this.criterionIndex.put(Criterion.MI_6MOS, 12);
	}

	public void initializeNetworkFromFile(String modelFile) {
		try {
			File networkFile = new File(getClass().getResource("/models/" + modelFile).getPath());
			this.net = ModelSerializer.restoreMultiLayerNetwork(networkFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * SIGMOID activation and XENT loss function for binary multi-label
	 * classification.
	 */
	private void initializeNetworkDebug() {

		// https://deeplearning4j.org/workspaces
		// Nd4j.getMemoryManager().setAutoGcWindow(10000);
		Nd4j.getMemoryManager().togglePeriodicGc(false);

		try {
			fullSetIteration = new NGramIterator(patientExamples, miniBatchSize);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		vectorSize = fullSetIteration.vectorSize;
		truncateLength = fullSetIteration.maxSentences;

		// seed for reproducibility
		final int seed = 12345;
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed)
				.updater(Adam.builder().beta1(0.9).beta2(0.999).build()).regularization(true).l2(1e-5)
				.weightInit(WeightInit.XAVIER).gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
				.gradientNormalizationThreshold(1.0).learningRate(2e-2).trainingWorkspaceMode(WorkspaceMode.SEPARATE)
				.inferenceWorkspaceMode(WorkspaceMode.SEPARATE).list()
				.layer(0, new GravesLSTM.Builder().nIn(vectorSize).nOut(256).activation(Activation.TANH).build())
				.layer(1,
						new RnnOutputLayer.Builder().activation(Activation.SIGMOID)
								.lossFunction(LossFunctions.LossFunction.XENT).nIn(256).nOut(13).build())
				.pretrain(false).backprop(true).build();

		this.net = new MultiLayerNetwork(conf);
		this.net.init();
		this.net.setListeners(new ScoreIterationListener(1));
	}

	/**
	 * SIGMOID activation and XENT loss function for binary multi-label
	 * classification.
	 */
	private void initializeNetworkBinaryMultiLabel() {

		// settings for memory management:
		// https://deeplearning4j.org/workspaces

		Nd4j.getMemoryManager().setAutoGcWindow(10000);
		//Nd4j.getMemoryManager().togglePeriodicGc(false);

		try {
			fullSetIteration = new NGramIterator(patientExamples, miniBatchSize);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		vectorSize = fullSetIteration.vectorSize;
		truncateLength = fullSetIteration.maxSentences;

		// seed for reproducibility
		final int seed = 12345;
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed)
				.updater(Adam.builder().beta1(0.9).beta2(0.999).build()).regularization(true).l2(1e-5)
				.weightInit(WeightInit.XAVIER).gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
				.gradientNormalizationThreshold(1.0).learningRate(2e-2).trainingWorkspaceMode(WorkspaceMode.NONE)
				.inferenceWorkspaceMode(WorkspaceMode.NONE).list()
				.layer(0,
						new DenseLayer.Builder().activation(Activation.RELU).nIn(vectorSize).nOut(300)
								.weightInit(WeightInit.RELU).updater(AdaGrad.builder().learningRate(0.01).build())
								.gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
								.gradientNormalizationThreshold(10).learningRate(0.01).build())
				.layer(1, new GravesBidirectionalLSTM.Builder().nIn(300).nOut(300).activation(Activation.TANH).build())
				.layer(2, new GravesLSTM.Builder().nIn(300).nOut(300).activation(Activation.TANH).build())
				.layer(3,
						new RnnOutputLayer.Builder().activation(Activation.SIGMOID)
								.lossFunction(LossFunctions.LossFunction.XENT).nIn(300).nOut(13).build())
				.inputPreProcessor(0, new RnnToFeedForwardPreProcessor())
				.inputPreProcessor(1, new FeedForwardToRnnPreProcessor()).pretrain(false).backprop(true).build();

		this.net = new MultiLayerNetwork(conf);
		this.net.init();
		this.net.setListeners(new ScoreIterationListener(1));
	}

	/**
	 * Initialize monitoring.
	 * 
	 */
	private void initializeMonitoring() {
		// setting monitor
		UIServer uiServer = UIServer.getInstance();

		// Configure where the network information (gradients, score vs. time
		// etc) is to be stored. Here: store in memory.
		// Alternative: new FileStatsStorage(File), for saving and loading later
		StatsStorage statsStorage = new InMemoryStatsStorage();

		// Attach the StatsStorage instance to the UI: this allows the contents
		// of the StatsStorage to be visualized
		uiServer.attach(statsStorage);

		// Then add the StatsListener to collect this information from the
		// network, as it trains
		net.setListeners(new StatsListener(statsStorage));
	}

	/**
	 * 
	 * 
	 * @param examples
	 * @param trainingSplit
	 * @param validationSplit
	 * @param testSplit
	 */
	private void getSplits(List<Patient> examples, List<Patient> trainingSplit, List<Patient> validationSplit,
			List<Patient> testSplit) {

		// TODO generalize
		// --------------------------------------
		// abdominal 60 split: 77 / 125 - 46 / 75
		// abdominal 20 split: 77 / 125 - 16 / 25
		// abdominal 20 split: 77 / 125 - 15 / 25
		// --------------------------------------
		// abdominal 100 : 77 / 125 - 77 / 25

		int counterPos = 1;
		int counterNeg = 1;

		for (Patient patient : examples) {
			if (patient.getEligibility(Criterion.ABDOMINAL).equals(Eligibility.MET)) {
				if (counterPos > 62) {
					testSplit.add(patient);
				} else if (counterPos > 46) {
					validationSplit.add(patient);
					counterPos++;
				} else {
					trainingSplit.add(patient);
					counterPos++;
				}
			} else {
				if (counterNeg > 100) {
					testSplit.add(patient);
				} else if (counterNeg > 75) {
					validationSplit.add(patient);
					counterNeg++;
				} else {
					trainingSplit.add(patient);
					counterNeg++;
				}
			}
		}
	}

	private void trainWithEarlyStoppingBML(List<Patient> examples) {

		List<Patient> trainingSplit = new ArrayList<Patient>();
		List<Patient> validationSplit = new ArrayList<Patient>();
		List<Patient> testSplit = new ArrayList<Patient>();

		// generate splits (60 20 20)
		getSplits(examples, trainingSplit, validationSplit, testSplit);

		NGramIterator training;
		NGramIterator validation;
		NGramIterator test;

		try {
			training = new NGramIterator(trainingSplit, miniBatchSize);
			validation = new NGramIterator(validationSplit, miniBatchSize, training.characterNGram_3,
					training.char3GramToIdxMap);
			test = new NGramIterator(testSplit, miniBatchSize, training.characterNGram_3, training.char3GramToIdxMap);

			// early stopping on validation
			EarlyStoppingModelSaver<MultiLayerNetwork> saver = new InMemoryModelSaver<>();
			EarlyStoppingConfiguration<MultiLayerNetwork> esConf = new EarlyStoppingConfiguration.Builder<MultiLayerNetwork>()
					.epochTerminationConditions(new MaxEpochsTerminationCondition(100),
							new ScoreImprovementEpochTerminationCondition(5))
					.iterationTerminationConditions(new MaxTimeIterationTerminationCondition(4, TimeUnit.HOURS),
							new MaxScoreIterationTerminationCondition(7.5))
					.scoreCalculator(new DataSetLossCalculator(validation, true)).modelSaver(saver).build();

			// conduct early stopping training
			IEarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf, net, training);
			EarlyStoppingResult result = trainer.fit();

			LOG.info("Termination reason: " + result.getTerminationReason());
			LOG.info("Termination details: " + result.getTerminationDetails());
			LOG.info("Total epochs: " + result.getTotalEpochs());
			LOG.info("Best epoch number: " + result.getBestModelEpoch());
			LOG.info("Score at best epoch: " + result.getBestModelScore());

			// run evaluation on test data
			LOG.info("Printing TEST evaluation measurements");
			Evaluation evaluationTest = net.evaluate(test);
			LOG.info(evaluationTest.stats());

			// run evaluation on validation data
			LOG.info("Printing VALIDAITON evaluation measurements");
			Evaluation evaluationValidation = net.evaluate(validation);
			LOG.info(evaluationValidation.stats());

			// run evaluation on test data
			LOG.info("Printing TRAINING evaluation measurements");
			Evaluation evaluationTraining = net.evaluate(training);
			LOG.info(evaluationTraining.stats());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void trainWithEarlyStopping(List<Patient> examples) {

		List<Patient> trainingSplit = new ArrayList<Patient>();
		List<Patient> validationSplit = new ArrayList<Patient>();
		List<Patient> testSplit = new ArrayList<Patient>();

		// generate splits (60 20 20)
		getSplits(examples, trainingSplit, validationSplit, testSplit);

		NGramIterator training;
		NGramIterator validation;
		NGramIterator test;

		try {
			training = new NGramIterator(trainingSplit, miniBatchSize);
			validation = new NGramIterator(validationSplit, miniBatchSize, training.characterNGram_3,
					training.char3GramToIdxMap);
			test = new NGramIterator(testSplit, miniBatchSize, training.characterNGram_3, training.char3GramToIdxMap);

			// early stopping on validation
			EarlyStoppingModelSaver<MultiLayerNetwork> saver = new InMemoryModelSaver<>();
			EarlyStoppingConfiguration<MultiLayerNetwork> esConf = new EarlyStoppingConfiguration.Builder<MultiLayerNetwork>()
					.epochTerminationConditions(new MaxEpochsTerminationCondition(100),
							new ScoreImprovementEpochTerminationCondition(5))
					.iterationTerminationConditions(new MaxTimeIterationTerminationCondition(4, TimeUnit.HOURS),
							new MaxScoreIterationTerminationCondition(7.5))
					.scoreCalculator(new DataSetLossCalculator(validation, true)).modelSaver(saver).build();

			// conduct early stopping training
			IEarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf, net, training);
			EarlyStoppingResult result = trainer.fit();

			LOG.info("Termination reason: " + result.getTerminationReason());
			LOG.info("Termination details: " + result.getTerminationDetails());
			LOG.info("Total epochs: " + result.getTotalEpochs());
			LOG.info("Best epoch number: " + result.getBestModelEpoch());
			LOG.info("Score at best epoch: " + result.getBestModelScore());

			// run evaluation on test data
			LOG.info("Printing TEST evaluation measurements");
			Evaluation evaluationTest = net.evaluate(test);
			LOG.info(evaluationTest.stats());

			// run evaluation on validation data
			LOG.info("Printing VALIDAITON evaluation measurements");
			Evaluation evaluationValidation = net.evaluate(validation);
			LOG.info(evaluationValidation.stats());

			// run evaluation on test data
			LOG.info("Printing TRAINING evaluation measurements");
			Evaluation evaluationTraining = net.evaluate(training);
			LOG.info(evaluationTraining.stats());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Training for binary multi label classifcation.
	 * 
	 * @param examples
	 *            Patient training examples.
	 */
	private void trainFullSetBML(List<Patient> examples) {

		List<Patient> trainingSplit = new ArrayList<Patient>();
		List<Patient> validationSplit = new ArrayList<Patient>();
		List<Patient> combinedSplit = new ArrayList<Patient>();
		List<Patient> testSplit = new ArrayList<Patient>();

		// generate splits (60 20 20)
		getSplits(examples, trainingSplit, validationSplit, testSplit);
		combinedSplit.addAll(trainingSplit);
		combinedSplit.addAll(validationSplit);

		NGramIterator training;
		NGramIterator validation;
		NGramIterator combined;
		NGramIterator test;
		NGramIterator fullSet;

		Random rng = new Random(12345);

		// print the number of parameters in the network (and for each layer)
		Layer[] layers = net.getLayers();
		int totalNumParams = 0;
		for (int i = 0; i < layers.length; i++) {
			int nParams = layers[i].numParams();
			LOG.info("Number of parameters in layer " + i + ": " + nParams);
			totalNumParams += nParams;
		}
		LOG.info("Total number of network parameters: " + totalNumParams);

		try {

			// combined = new NGramIterator(combinedSplit, miniBatchSize);
			// test = new NGramIterator(testSplit, miniBatchSize,
			// combined.characterNGram_3, combined.char3GramToIdxMap);
			// fullSet = new NGramIterator(examples, miniBatchSize,
			// combined.characterNGram_3, combined.char3GramToIdxMap);

			for (int i = 0; i < nEpochs; i++) {

				net.fit(fullSetIteration);
				fullSetIteration.reset();

				LOG.info("Epoch " + i + " complete.");
				LOG.info("Starting FULL SET evaluation:");

				EvaluationBinary eb = new EvaluationBinary();
				while (fullSetIteration.hasNext()) {
					DataSet t = fullSetIteration.next();
					INDArray features = t.getFeatureMatrix();
					INDArray lables = t.getLabels();
					INDArray inMask = t.getFeaturesMaskArray();
					INDArray outMask = t.getLabelsMaskArray();
					INDArray predicted = net.output(features, false, inMask, outMask);

					eb.eval(lables, predicted, outMask);
				}
				fullSetIteration.reset();
				LOG.info(eb.stats());

				// net.fit(combined);
				// combined.reset();
				//
				// LOG.info("Epoch " + i + " complete.");
				// LOG.info("Starting COMBINED (TRAINING + VALIDATION)
				// evaluation:");
				//
				// EvaluationBinary eb = new EvaluationBinary();
				// while (combined.hasNext()) {
				// DataSet t = combined.next();
				// INDArray features = t.getFeatureMatrix();
				// INDArray lables = t.getLabels();
				// INDArray inMask = t.getFeaturesMaskArray();
				// INDArray outMask = t.getLabelsMaskArray();
				// INDArray predicted = net.output(features, false, inMask,
				// outMask);
				//
				// eb.eval(lables, predicted, outMask);
				// }
				// combined.reset();
				// LOG.info(System.getProperty("line.separator") + eb.stats());
				//
				// LOG.info("Starting TEST evaluation:");
				// // run evaluation on test data
				// eb = new EvaluationBinary();
				// while (test.hasNext()) {
				// DataSet t = test.next();
				// INDArray features = t.getFeatureMatrix();
				// INDArray lables = t.getLabels();
				// INDArray inMask = t.getFeaturesMaskArray();
				// INDArray outMask = t.getLabelsMaskArray();
				// INDArray predicted = net.output(features, false, inMask,
				// outMask);
				//
				// eb.eval(lables, predicted, outMask);
				// }
				// test.reset();
				// LOG.info(System.getProperty("line.separator") + eb.stats());
			}

			// save model after n epochs
			File locationToSave = new File("N2c2BILSTMC3G_MBL_Full_" + nEpochs + ".zip");
			boolean saveUpdater = true;
			ModelSerializer.writeModel(net, locationToSave, saveUpdater);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void trainFullSet(List<Patient> examples) {
		// Print the number of parameters in the network (and for each layer)
		Layer[] layers = net.getLayers();
		int totalNumParams = 0;
		for (int i = 0; i < layers.length; i++) {
			int nParams = layers[i].numParams();
			LOG.info("Number of parameters in layer " + i + ": " + nParams);
			totalNumParams += nParams;
		}
		LOG.info("Total number of network parameters: " + totalNumParams);

		// start training
		try {
			NGramIterator train = new NGramIterator(examples, miniBatchSize);

			LOG.info("Starting training");
			for (int i = 0; i < nEpochs; i++) {
				net.fit(train);
				train.reset();

				LOG.info("Epoch " + i + " complete. Starting evaluation:");

				// run evaluation on training data (change to test data)
				Evaluation evaluation = net.evaluate(train);
				LOG.info(evaluation.stats());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load features from narrative.
	 *
	 * @param reviewContents
	 *            Narrative content.
	 * @param maxLength
	 *            Maximum length of token series length.
	 * @return Time series feature presentation of narrative.
	 */
	private INDArray loadFeaturesForNarrative(String reviewContents, int maxLength) {

		// TODO
		return null;
	}

	@Override
	public void train(List<Patient> examples) {
		// if (examples == predefined list) // 2 lists: 1 for 100% (?); 1 for
		// 60% (size?)
		// do not train;
		// try to load the model from a file;
		// if (file does not exist)
		// retrain()

		// else
		// train on new list

		trainFullSetBML(examples);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * at.medunigraz.imi.bst.n2c2.classifier.Classifier#predict(at.medunigraz.
	 * imi.bst.n2c2.model.Patient)
	 */
	@Override
	public Eligibility predict(Patient p) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * at.medunigraz.imi.bst.n2c2.classifier.Classifier#predict(at.medunigraz.
	 * imi.bst.n2c2.model.Patient, at.medunigraz.imi.bst.n2c2.model.Criterion)
	 */
	@Override
	public Eligibility predict(Patient p, Criterion c) {

		String patientNarrative = p.getText();

		INDArray features = loadFeaturesForNarrative(patientNarrative, this.truncateLength);
		INDArray networkOutput = net.output(features);

		int timeSeriesLength = networkOutput.size(2);
		INDArray probabilitiesAtLastWord = networkOutput.get(NDArrayIndex.point(0), NDArrayIndex.all(),
				NDArrayIndex.point(timeSeriesLength - 1));

		double probabilityForCriterion = 0.0;
		probabilityForCriterion = probabilitiesAtLastWord.getDouble(criterionIndex.get(c));
		Eligibility eligibility = probabilityForCriterion > 0.5 ? Eligibility.MET : Eligibility.NOT_MET;

		LOG.info("\n\n-------------------------------");
		LOG.info("Patient narrative: \n" + patientNarrative);
		LOG.info("\n\nProbabilities at last time step for {}", c.name());
		LOG.info("Probability\t" + c.name() + ": " + probabilityForCriterion);
		LOG.info("Eligibility\t" + c.name() + ": " + eligibility.name());

		return eligibility;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * at.medunigraz.imi.bst.n2c2.classifier.Classifier#predict(java.util.List)
	 */
	@Override
	public List<Patient> predict(List<Patient> patientList) {
		patientList.forEach((k) -> {
			criterionIndex.forEach((c, v) -> {
				k.withCriterion(c, this.predict(k, c));
			});
		});
		return patientList;
	}
}
