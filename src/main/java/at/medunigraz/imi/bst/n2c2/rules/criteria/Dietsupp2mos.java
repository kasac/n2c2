package at.medunigraz.imi.bst.n2c2.rules.criteria;

import at.medunigraz.imi.bst.n2c2.model.Eligibility;
import at.medunigraz.imi.bst.n2c2.model.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Dietsupp2mos extends BaseClassifiable {

    private static final List<Pattern> POSITIVE_MARKERS = new ArrayList<>();

    static {
        // Vitamin D is excluded
        POSITIVE_MARKERS.add(Pattern.compile("folate", Pattern.CASE_INSENSITIVE));
        POSITIVE_MARKERS.add(Pattern.compile("calcium carbonate", Pattern.CASE_INSENSITIVE));
        POSITIVE_MARKERS.add(Pattern.compile("supplement", Pattern.CASE_INSENSITIVE));
        POSITIVE_MARKERS.add(Pattern.compile("multivitamin", Pattern.CASE_INSENSITIVE));
    }

    @Override
    public Eligibility isMet(Patient p) {
        return findAnyPattern(p.getText(), POSITIVE_MARKERS) ? Eligibility.MET : Eligibility.NOT_MET;
    }
}

/* following can occur in any part of a document.

alpha, beta, etc. can be in greek as well
categories separated by underlines
if in braces, acronym of the previous
if there is an s in the end, it can be removed
'chelate' can be substituted by names of anionic amino acids
_______________________________
Fructo-oligosaccharides
Galacto-oligosaccharides 
Human milk oligosaccharides 
Isomalto-oligosaccharides 
Maltotriose
Mannan oligosaccharides 
Raffinose 
stachyose 
verbascose
________________________________
Chondroitin sulfate
Beta-glucan
Beta mannan
psylium
chitin
________________________________
Alanine
Arginine
Asparagine
Aspartic acid
Cysteine
Glutamic acid
Glutamine
Glycine
Histidine
Isoleucine
Leucine
Lysine
Methionine
Phenylalanine
Proline
Selenocysteine
Serine
Threonine
Tryptophan
Tyrosine
Valine
________________________________
Citrulline
Cystine
Gama aminobutyric acid (GABA)
Ornithine
Theanine
________________________________
Betaine
Carnitine
Carnosine
Creatine
Hydroxyproline
Hydroxytryptophan
N-acetyl cysteine
S-Adenosyl methionine (SAM-e)
Taurine
Tyramine
_________________________________
Omega-3
Alpha-linolenic acid 
(ALA)
Eicosapentaenoic 
(EPA) 
Docosahexaenoic acid 
(DHA)
Omega-6
Arachidonic acid 
(AA)
Linoleic acid
Conjugated linoleic acid 
(CLA)
_________________________________
Lecithin 
Phosphatidylcholine
phytosterols
_________________________________
Astaxanthin
Lutein
zeaxanthin
Lycopene
Vitamin A
carotenoids
________________________________
Vitamin A 
Retinol and retinal
Vitamin B1 
Thiamine
Vitamin B2
Riboflavin
Vitamin B3 
Niacin
Vitamin B5 
Pantothenic acid
Vitamin B6 
Pyridoxine
Vitamin B7 
Biotin
Vitamin B9 
Folic acid
Vitamin B12 
Cobalamin
Choline
Vitamin C 
Ascorbic acid
Vitamin D 
Ergocalciferol and cholecalciferol
Vitamin E 
Tocopherol
Vitamin K 
Phylloquinone
___________________________________
Curcumin
FLAVONOIDS
Anthocyanidins
Echinacea
St. John's Wort
Valerian
Hoodia
Milk Thistle
Saw Palmetto
Ginkgo
Ginseng
Glucosamine
Yohimbe

Sambucus Elderberry 
Flavanols
flavonones
Isothiocyanates
Lignin
Phytic acid (inositol hexaphosphate)
Piperine
Proanthocyanidins
anthocyanes
anthocianine
hesperdin
quercetin
diosmin
luteolin
Isoflavones: daidzein, genistein
Caffeic acid
Chlorogenic acid
Lignans
Resveratrol
Tannins
Tannic acid
Allicin
Chlorophyll and chlorophyllin
Indole-3-carbinol
______________________________
Chromium nicotinate
Chromium citrate
High-chromium yeast
Chromium chloride
Chromium picolinate 

Calcium carbonate
Coral calcium
Bio-calcium
Rock calcium
Calcium citrate
calcium malate 
Calcium magnesium citrate 
Calcium lactate 
calcium gluconate
Calcium phosphate 
Calcium oxide 
calcium hydroxide 
calcium supplement
calcium orotate
Calcium hydroxiapatite

Potassium iodide

Iron dextran 
ferumoxytol 
Ferric gluconate
iron sucrose 

Magnesium Ascorbate 
Magnesium Citrate 
Magnesium Citramate
Potassium-magnesium citrate
Magnesium Fumarate 
Magnesium Gluconate 
Magnesium Lactate 
Magnesium Malate 
Magnesium Pidolate 
Magnesium Salicylate 
Magnesium Threonate 
Magnesium Chelate
Magnesium Arginate 
Magnesium L-Aspartate
Magnesium Glutamate 
Magnesium Glycinate 
Magnesium Lysinate 
Magnesium Orotate 
Magnesium Taurate 
Magnesium Taurinate 
Inorganic magnesium 
Magnesium Carbonate 
Magnesium chloride
Magnesium hydroxide 
milk of magnesia
Magnesium oxide
Magnesium sulfate 
Epsom salt 
Enteric-coated Magnesium 
magnesium acetate
magnesium phosphate
Magnesium stearate

glucosamine hydrochloride
Manganese gluconat
manganese sulfate 
manganese ascorbate

Molybdenum
Molybdenium

sodium molybdate 
ammonium molybdate

sodium selenite 
Na2SeO3
sodium selenate
Na2SeO4
selenomethionine
Selenium-enriched yeast

Potassium chloride
KCl Er
EC KCl
potassium bicarbonate
potassium citrate 
potassium gluconate
Magnesium potassium aspartate
Potassium orotate
Potassium chelate
Potassium acetate

Zinc acetate
zinc gluconate
zinc-histidine
zinc picolinate
zinc sulfate

Boric Anhydride
Boric Tartrate
Sodium Borate

glutathion

vanadyl sulfate 
sodium metavanadate
*/
