package at.medunigraz.imi.bst.n2c2.evaluator;

import at.medunigraz.imi.bst.n2c2.model.Criterion;
import at.medunigraz.imi.bst.n2c2.model.Eligibility;
import at.medunigraz.imi.bst.n2c2.model.Patient;
import at.medunigraz.imi.bst.n2c2.model.metrics.BasicMetrics;
import at.medunigraz.imi.bst.n2c2.model.metrics.Metrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Deprecated
public class BasicEvaluator implements Evaluator {

    private static final Logger LOG = LogManager.getLogger();

    private Map<Criterion, BasicMetrics> metricsByCriterion = new HashMap<>();

    public double getOfficialRankingMeasureByCriterion(Criterion c) {
        return getMetricsByCriterion(c).getAccuracy();
    }

    public BasicMetrics getMetricsByCriterion(Criterion c) {
        return metricsByCriterion.get(c);
    }

    @Override
    public void evaluate(List<Patient> gold, List<Patient> results) {
        int count = 0;
        double overallAccuracy = 0;

        // Map of results by patient id for comparison
        Map<String, Patient> resultsMap = results.stream().collect(Collectors.toMap(Patient::getID, p -> p));

        for (Criterion criterion : Criterion.classifiableValues()) {
            int tp = 0, fp = 0, tn = 0, fn = 0;

            // TODO parallel stream
            for (Patient g : gold) {
                Patient actual = resultsMap.get(g.getID());

                Match match = comparePatients(g, actual, criterion);
                switch (match) {
                    case TP:
                        tp++;
                        break;
                    case FP:
                        fp++;
                        break;
                    case TN:
                        tn++;
                        break;
                    case FN:
                        fn++;
                        break;
                }
            }

            BasicMetrics metrics = new BasicMetrics(tp, fp, tn, fn);

            overallAccuracy += metrics.getAccuracy();
            count++;

            metricsByCriterion.put(criterion, metrics);
        }

        metricsByCriterion.put(Criterion.OVERALL_MACRO, new BasicMetrics(overallAccuracy / count));
    }

    @Override
    public Metrics getMetrics() {
        return metricsByCriterion.get(Criterion.OVERALL_MACRO);
    }

    private enum Match {
        TP, FP, TN, FN, UNKNOWN
    }

    private Match comparePatients(Patient gold, Patient actual, Criterion criterion) {
        if (Eligibility.classifiableValues().length != 2) {
            throw new UnsupportedOperationException("Multi-class comparison is not supported.");
        }

        if (gold.getEligibility(criterion) == Eligibility.MET) {
            if (actual.getEligibility(criterion) == Eligibility.MET) {
                return Match.TP;
            } else if (actual.getEligibility(criterion) == Eligibility.NOT_MET) {
                LOG.debug("Got a false negative for {} in {}", criterion, actual.getID());
                return Match.FN;
            }
        } else if (gold.getEligibility(criterion) == Eligibility.NOT_MET) {
            if (actual.getEligibility(criterion) == Eligibility.MET) {
                LOG.debug("Got a false positive for {} in {}", criterion, actual.getID());
                return Match.FP;
            } else if (actual.getEligibility(criterion) == Eligibility.NOT_MET) {
                return Match.TN;
            }
        }

        return Match.UNKNOWN;
    }
}
