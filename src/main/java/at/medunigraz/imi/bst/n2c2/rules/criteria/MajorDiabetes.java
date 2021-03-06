package at.medunigraz.imi.bst.n2c2.rules.criteria;

import at.medunigraz.imi.bst.n2c2.model.Eligibility;
import at.medunigraz.imi.bst.n2c2.model.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MajorDiabetes extends BaseClassifiable {

    private static final List<Pattern> POSITIVE_MARKERS = new ArrayList<>();
    static {
        // From the guidelines
        POSITIVE_MARKERS.add(Pattern.compile("amputation", Pattern.CASE_INSENSITIVE));
        POSITIVE_MARKERS.add(Pattern.compile("kidney damage", Pattern.CASE_INSENSITIVE));
        //POSITIVE_MARKERS.add(Pattern.compile("skin condition", Pattern.CASE_INSENSITIVE));    // No clear distinction?
        POSITIVE_MARKERS.add(Pattern.compile("retinopathy", Pattern.CASE_INSENSITIVE));
        POSITIVE_MARKERS.add(Pattern.compile("nephropathy", Pattern.CASE_INSENSITIVE));
        POSITIVE_MARKERS.add(Pattern.compile("neuropathy", Pattern.CASE_INSENSITIVE));

        // From the annotated examples
        POSITIVE_MARKERS.add(Pattern.compile("macular degeneration", Pattern.CASE_INSENSITIVE));
        POSITIVE_MARKERS.add(Pattern.compile("lue weakness", Pattern.CASE_INSENSITIVE));
        POSITIVE_MARKERS.add(Pattern.compile("retinal venous occlusion", Pattern.CASE_INSENSITIVE));
    }

    @Override
    public Eligibility isMet(Patient p) {
        return findAnyPattern(p.getText(), POSITIVE_MARKERS) ? Eligibility.MET : Eligibility.NOT_MET;
    }
}