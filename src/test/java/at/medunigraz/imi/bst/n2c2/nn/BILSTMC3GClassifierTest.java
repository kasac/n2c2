package at.medunigraz.imi.bst.n2c2.nn;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Ignore;
import org.xml.sax.SAXException;

import at.medunigraz.imi.bst.n2c2.dao.PatientDAO;
import at.medunigraz.imi.bst.n2c2.model.Patient;

public class BILSTMC3GClassifierTest {

	@Ignore
	public void train() {

		// read in patients
		File sampleDirectory = new File("Z:/n2c2/data/samplesTraining");
		List<File> sampleFiles = (List<File>) FileUtils.listFiles(sampleDirectory, TrueFileFilter.INSTANCE,
				TrueFileFilter.INSTANCE);

		List<Patient> patients;
		try {
			patients = new ArrayList<Patient>();
			for (File patientSample : sampleFiles) {
				patients.add(new PatientDAO().fromXML(patientSample));
			}

			BILSTMC3GClassifier classifier = new BILSTMC3GClassifier(patients);
			classifier.train(patients);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		assertEquals(true, true);
	}
}
