package at.medunigraz.imi.bst.n2c2.model;

import org.w3c.dom.Element;

public class XMLTag {

    private Criterion criterion;
    private Eligibility eligibility;

    private XMLTag() {
    }

    public static XMLTag fromElement(Element element) {
        XMLTag c = new XMLTag();

        String name = element.getTagName();
        c.withCriterion(Criterion.get(name));

        String met = element.getAttribute("met");
        c.withEligibility(Eligibility.get(met));

        return c;
    }

    private XMLTag withCriterion(Criterion criterion) {
        this.criterion = criterion;
        return this;
    }

    private XMLTag withEligibility(Eligibility eligibility) {
        this.eligibility = eligibility;
        return this;
    }

    public Criterion getCriterion() {
        return criterion;
    }

    public Eligibility getEligibility() {
        return eligibility;
    }
}