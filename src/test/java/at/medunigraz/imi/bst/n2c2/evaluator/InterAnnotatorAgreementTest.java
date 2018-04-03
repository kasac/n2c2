package at.medunigraz.imi.bst.n2c2.evaluator;

import at.medunigraz.imi.bst.n2c2.model.Criterion;
import at.medunigraz.imi.bst.n2c2.model.Eligibility;
import at.medunigraz.imi.bst.n2c2.model.Patient;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class InterAnnotatorAgreementTest {

    private static final String GOLD = "/gold-standard/";
    private static final String RESULTS = "/results/";

    @Before
    public void SetUp() {
        Assume.assumeTrue(InterAnnotatorAgreement.scriptExists());
    }

    @Test
    public void evaluate() {
        File goldStandard = new File(getClass().getResource(GOLD).getFile());
        File results = new File(getClass().getResource(RESULTS).getFile());

        InterAnnotatorAgreement iaa = new InterAnnotatorAgreement(goldStandard, results);
        assertEquals(1, iaa.getF1(), 0.00001);
    }

    public void evaluateWithParameters() throws FileNotFoundException {
        InterAnnotatorAgreement iaa = new InterAnnotatorAgreement();

        List<Patient> gold = new ArrayList<>();
        gold.add(new Patient().withID("a").withCriterion(Criterion.ABDOMINAL, Eligibility.MET));

        List<Patient> results = new ArrayList<>();
        results.add(new Patient().withID("a").withCriterion(Criterion.ABDOMINAL, Eligibility.MET));

        iaa.evaluate(gold, results);
        assertEquals(1, iaa.getF1(), 0.00001);
    }
}