package at.medunigraz.imi.bst.n2c2.validation;

import at.medunigraz.imi.bst.n2c2.classifier.factory.ClassifierFactory;
import at.medunigraz.imi.bst.n2c2.evaluator.Evaluator;
import at.medunigraz.imi.bst.n2c2.model.Patient;
import at.medunigraz.imi.bst.n2c2.model.metrics.Metrics;
import at.medunigraz.imi.bst.n2c2.util.Dataset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class CrossValidator extends AbstractValidator {

    private static final Logger LOG = LogManager.getLogger();

    public CrossValidator(List<Patient> patients, ClassifierFactory classifierFactory, Evaluator evaluator) {
        super(patients, classifierFactory, evaluator);
    }

    public Metrics validate() {
        return validate(Dataset.DEFAULT_FOLDS);
    }

    public Metrics validate(int k) {
        Metrics metrics = null;

        Dataset dataset = new Dataset(patients);
        dataset.splitIntoFolds(k);

        for (int i = 0; i < k; i++) {
            LOG.info("Evaluating fold {}/{}...", i + 1, k);
            List<Patient> train = dataset.getTrainingSet(i);
            List<Patient> test = dataset.getTestSet(i);
            List<Patient> gold = dataset.getGoldSet(i);

            Metrics foldMetrics = validateFold(train, test, gold);

            // First initialization
            if (metrics == null) {
                metrics = foldMetrics;
            } else {
                metrics.add(foldMetrics);
            }
        }

        metrics.divideBy(k);

        return metrics;
    }
}
