package ch.evolutionsoft.example.dl4j.tictactoe.feedforward;

import static ch.evolutionsoft.example.dl4j.tictactoe.TicTacToeConstants.*;
import static ch.evolutionsoft.example.dl4j.tictactoe.commonnet.NeuralNetConstants.*;

import java.io.IOException;
import java.util.List;

import org.deeplearning4j.datasets.iterator.INDArrayDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.saver.InMemoryModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxScoreIterationTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.primitives.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.evolutionsoft.example.dl4j.tictactoe.commonnet.NeuralDataHelper;
import ch.evolutionsoft.example.dl4j.tictactoe.commonnet.TicTacToeNeuralDataConverter;

public class FeedForwardCommon {

  private static final Logger logger = LoggerFactory.getLogger(FeedForwardCommon.class);

  public static final int NET_ITERATIONS = 15;

  public static final double NESTEROVS_MOMENTUM = 0.99;

  protected MultiLayerNetwork createNetworkModel(MultiLayerConfiguration multiLayerConfiguration) {

    String message = "Build model ...";
    logger.info(message);

    MultiLayerNetwork net = new MultiLayerNetwork(multiLayerConfiguration);
    net.init();

    return net;
  }

  protected DataSet trainNetworkModel(MultiLayerNetwork net) throws IOException {

    String message = "Generate adapted net input and labels ...";
    logger.info(message);

    List<Pair<INDArray, INDArray>> allPlaygrounds = NeuralDataHelper.readAll();
    List<Pair<INDArray, INDArray>> convertedMiniMaxLabels = TicTacToeNeuralDataConverter.convertMiniMaxLabels(allPlaygrounds);
    
    List<Pair<INDArray, INDArray>> printExamples = NeuralDataHelper.printRandomFeedForwardNetInputAndLabels(
        convertedMiniMaxLabels, DEFAULT_FEATURE_EXAMPLE_NUMBER_LOG);
    NeuralDataHelper.printRandomMiniMaxData(printExamples, DEFAULT_FEATURE_EXAMPLE_NUMBER_LOG);
    
    DataSetIterator dataSetIterator = new INDArrayDataSetIterator(convertedMiniMaxLabels, allPlaygrounds.size());

    EarlyStoppingConfiguration<MultiLayerNetwork> earlyStoppingConfiguration =
        createEarlyStoppingConfiguration(dataSetIterator);

    EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(earlyStoppingConfiguration, net, dataSetIterator);

    trainer.fit();

    Pair<INDArray, INDArray> stackedPlaygroundLabels =
        TicTacToeNeuralDataConverter.stackFeedForwardPlaygroundLabels(convertedMiniMaxLabels);

    return new org.nd4j.linalg.dataset.DataSet(stackedPlaygroundLabels.getFirst(), stackedPlaygroundLabels.getSecond());
  }

  protected void evaluateNetworkPerformance(MultiLayerNetwork net, DataSet dataSet) {

    INDArray output = net.output(dataSet.getFeatures());
    Evaluation eval = new Evaluation(COLUMN_NUMBER);
    eval.eval(dataSet.getLabels(), output);

    if (logger.isInfoEnabled()) {
      logger.info(eval.stats());
    }

    logger.info("Answer to center field opening: {}", net.output(CENTER_FIELD_MOVE));
    logger.info("Answer to last corner field opening: {}", net.output(LAST_CORNER_FIELD_MOVE));
  }

  protected EarlyStoppingConfiguration<MultiLayerNetwork> createEarlyStoppingConfiguration(
      DataSetIterator dataSetIterator) {

    return new EarlyStoppingConfiguration.Builder<MultiLayerNetwork>()
        .epochTerminationConditions(new MaxEpochsTerminationCondition(DEFAULT_NUMBER_OF_EPOCHS))
        .iterationTerminationConditions(new MaxScoreIterationTerminationCondition(DEFAULT_MAX_SCORE_EARLY_STOP))
        .scoreCalculator(new DataSetLossCalculator(dataSetIterator, true))
        .evaluateEveryNEpochs(1)
        .modelSaver(new InMemoryModelSaver<>())
        .build();
  }
}
