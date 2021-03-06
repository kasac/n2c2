package at.medunigraz.imi.bst.n2c2;

import at.medunigraz.imi.bst.n2c2.classifier.factory.ClassifierFactory;
import at.medunigraz.imi.bst.n2c2.classifier.factory.SVMClassifierFactory;
import at.medunigraz.imi.bst.n2c2.evaluator.OfficialEvaluator;
import at.medunigraz.imi.bst.n2c2.model.Patient;
import at.medunigraz.imi.bst.n2c2.model.metrics.MetricSet;
import at.medunigraz.imi.bst.n2c2.stats.CSVStatsWriter;
import at.medunigraz.imi.bst.n2c2.stats.StatsWriter;
import at.medunigraz.imi.bst.n2c2.stats.XMLStatsWriter;
import at.medunigraz.imi.bst.n2c2.util.DatasetUtil;
import at.medunigraz.imi.bst.n2c2.validation.CrossValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BestClassifierRunner {

    private static final Logger LOG = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        final File dataFolder = new File("data");
        final File xmlStatsFile = new File("stats/best.xml");
        final File csvStatsFile = new File("stats/best.csv");

        List<Patient> patients = DatasetUtil.loadFromFolder(dataFolder);
        ClassifierFactory factory = new SVMClassifierFactory();
        OfficialEvaluator evaluator = new OfficialEvaluator();

        CrossValidator cv = new CrossValidator(patients, factory, evaluator);
        MetricSet metrics = (MetricSet) cv.validate();
        LOG.info(metrics);

        StatsWriter xmlWriter = new XMLStatsWriter(xmlStatsFile);
        xmlWriter.write(metrics);
        xmlWriter.close();

        StatsWriter csvWriter = new CSVStatsWriter(csvStatsFile);
        csvWriter.write(metrics);
        csvWriter.close();
    }
}
