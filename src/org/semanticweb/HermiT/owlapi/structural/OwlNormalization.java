// Copyright 2008 by Oxford University; see license.txt for details
package org.semanticweb.HermiT.owlapi.structural;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owl.model.OWLAntiSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLAxiomAnnotationAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLClassAssertionAxiom;
import org.semanticweb.owl.model.OWLDataAllRestriction;
import org.semanticweb.owl.model.OWLDataExactCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataMaxCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataMinCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLDataPropertyExpression;
import org.semanticweb.owl.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLDataRange;
import org.semanticweb.owl.model.OWLDataSomeRestriction;
import org.semanticweb.owl.model.OWLDataSubPropertyAxiom;
import org.semanticweb.owl.model.OWLDataValueRestriction;
import org.semanticweb.owl.model.OWLDeclarationAxiom;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLDescriptionVisitorEx;
import org.semanticweb.owl.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owl.model.OWLDisjointClassesAxiom;
import org.semanticweb.owl.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLEntityAnnotationAxiom;
import org.semanticweb.owl.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owl.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owl.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLImportsDeclaration;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLIndividualAxiom;
import org.semanticweb.owl.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectAllRestriction;
import org.semanticweb.owl.model.OWLObjectComplementOf;
import org.semanticweb.owl.model.OWLObjectExactCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectIntersectionOf;
import org.semanticweb.owl.model.OWLObjectMaxCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectMinCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectOneOf;
import org.semanticweb.owl.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyChainSubPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLObjectSelfRestriction;
import org.semanticweb.owl.model.OWLObjectSomeRestriction;
import org.semanticweb.owl.model.OWLObjectSubPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectUnionOf;
import org.semanticweb.owl.model.OWLObjectValueRestriction;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyAnnotationAxiom;
import org.semanticweb.owl.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLSameIndividualsAxiom;
import org.semanticweb.owl.model.OWLSubClassAxiom;
import org.semanticweb.owl.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLTransitiveObjectPropertyAxiom;

/**
 * This class implements the structural transformation from our new tableau
 * paper. This transformation departs in the following way from the paper: it
 * keeps the concepts of the form \exists R.{ a_1, ..., a_n }, \forall R.{ a_1,
 * ..., a_n }, and \forall R.\neg { a } intact. These concepts are then
 * clausified in a more efficient way.
 */
public class OwlNormalization {
    protected final Map<OWLDescription, OWLDescription> m_definitions;
    protected final Map<OWLObjectOneOf, OWLClass> m_definitionsForNegativeNominals;
    protected final Collection<OWLDescription[]> m_conceptInclusions;
    protected Collection<OWLObjectPropertyExpression[]> m_objectPropertyInclusions;
    protected final Collection<OWLDataPropertyExpression[]> m_dataPropertyInclusions;
    protected final Set<OWLObjectPropertyExpression> m_asymmetricObjectProperties;
    protected final Set<OWLObjectPropertyExpression> m_reflexiveObjectProperties;
    protected final Set<OWLObjectPropertyExpression> m_irreflexiveObjectProperties;
    protected final Set<OWLObjectPropertyExpression> m_transitiveObjectProperties;
    protected final Set<OWLObjectPropertyExpression[]> m_disjointObjectProperties;
    protected final Set<OWLDataPropertyExpression[]> m_disjointDataProperties;
    protected final Collection<OWLIndividualAxiom> m_facts;
    protected final OWLDataFactory m_factory;
    protected final RoleManager roleManager;

    public OwlNormalization(OWLDataFactory factory) {
        m_definitions = new HashMap<OWLDescription, OWLDescription>();
        m_definitionsForNegativeNominals = new HashMap<OWLObjectOneOf, OWLClass>();
        m_conceptInclusions = new ArrayList<OWLDescription[]>();
        m_objectPropertyInclusions = new ArrayList<OWLObjectPropertyExpression[]>();
        m_dataPropertyInclusions = new ArrayList<OWLDataPropertyExpression[]>();
        m_asymmetricObjectProperties = new HashSet<OWLObjectPropertyExpression>();
        m_reflexiveObjectProperties = new HashSet<OWLObjectPropertyExpression>();
        m_irreflexiveObjectProperties = new HashSet<OWLObjectPropertyExpression>();
        m_transitiveObjectProperties = new HashSet<OWLObjectPropertyExpression>();
        m_disjointObjectProperties = new HashSet<OWLObjectPropertyExpression[]>();
        m_disjointDataProperties = new HashSet<OWLDataPropertyExpression[]>();
        m_facts = new HashSet<OWLIndividualAxiom>();
        m_factory = factory;
        roleManager = new TransitivityManager();
    }

    /**
     * Normalises the ontology and performs the structural transformation. After
     * executing this method, the getter methods of this class (e.g.,
     * getConceptInclusions, getAsymmetricObjectProperties, etc) can be used to
     * retrieve the normalised axioms and assertions from the ontology.
     * 
     * @param OWLOntology
     *            the ontology to be normalized
     * @throws OWLException
     */
    public void processOntology(OWLOntology OWLOntology) throws OWLException {
        // Each entry in the inclusions list represents a disjunction of
        // concepts, i.e., each OWLDescription in an entry contributes a
        // disjunct. It is thus not really inclusions, but rather a disjunction
        // of concepts that represents an inclusion axiom.
        List<OWLDescription[]> inclusions = new ArrayList<OWLDescription[]>();
        for (OWLAxiom untyped_axiom : OWLOntology.getAxioms()) {
            if (untyped_axiom instanceof OWLInverseObjectPropertiesAxiom) {
                OWLInverseObjectPropertiesAxiom axiom
                    = (OWLInverseObjectPropertiesAxiom) untyped_axiom;
                OWLObjectPropertyExpression first = axiom.getFirstProperty();
                OWLObjectPropertyExpression second = axiom.getSecondProperty();
                roleManager.addInclusion(first, second.getInverseProperty());
                roleManager.addInclusion(second, first.getInverseProperty());
            } else if (untyped_axiom instanceof OWLObjectSubPropertyAxiom) {
                OWLObjectSubPropertyAxiom axiom
                    = (OWLObjectSubPropertyAxiom) untyped_axiom;
                roleManager.addInclusion(axiom.getSubProperty(),
                                            axiom.getSuperProperty());
            } else if (untyped_axiom instanceof OWLObjectPropertyChainSubPropertyAxiom) {
                OWLObjectPropertyChainSubPropertyAxiom axiom
                    = (OWLObjectPropertyChainSubPropertyAxiom) untyped_axiom;
                roleManager.addInclusion(axiom.getPropertyChain(), axiom.getSuperProperty());
            } else if (untyped_axiom instanceof OWLEquivalentObjectPropertiesAxiom) {
                OWLEquivalentObjectPropertiesAxiom axiom
                    = (OWLEquivalentObjectPropertiesAxiom) untyped_axiom;
                roleManager.addEquivalence(axiom.getProperties());
            } else if (untyped_axiom instanceof OWLDataSubPropertyAxiom) {
                OWLDataSubPropertyAxiom axiom = (OWLDataSubPropertyAxiom) (untyped_axiom);
                OWLDataPropertyExpression subDataProperty = axiom.getSubProperty();
                OWLDataPropertyExpression superDataProperty = axiom.getSuperProperty();
                m_dataPropertyInclusions.add(new OWLDataPropertyExpression[] {
                        subDataProperty, superDataProperty });
            } else if (untyped_axiom instanceof OWLEquivalentDataPropertiesAxiom) {
                OWLEquivalentDataPropertiesAxiom axiom = (OWLEquivalentDataPropertiesAxiom) (untyped_axiom);
                OWLDataPropertyExpression[] dataProperties = new OWLDataPropertyExpression[axiom.getProperties().size()];
                axiom.getProperties().toArray(dataProperties);
                for (int i = 0; i < dataProperties.length - 1; i++) {
                    m_dataPropertyInclusions.add(new OWLDataPropertyExpression[] {
                            dataProperties[i], dataProperties[i + 1] });
                    m_dataPropertyInclusions.add(new OWLDataPropertyExpression[] {
                            dataProperties[i + 1], dataProperties[i] });
                }
            } else if (untyped_axiom instanceof OWLSubClassAxiom) {
                OWLSubClassAxiom axiom = (OWLSubClassAxiom) (untyped_axiom);
                inclusions.add(new OWLDescription[] {
                        axiom.getSubClass().getComplementNNF(),
                        axiom.getSuperClass().getNNF() });
            } else if (untyped_axiom instanceof OWLEquivalentClassesAxiom) {
                OWLEquivalentClassesAxiom axiom = (OWLEquivalentClassesAxiom) (untyped_axiom);
                OWLDescription[] descriptions = new OWLDescription[axiom.getDescriptions().size()];
                axiom.getDescriptions().toArray(descriptions);
                for (int i = 0; i < descriptions.length - 1; i++) {
                    inclusions.add(new OWLDescription[] {
                            descriptions[i].getComplementNNF(),
                            descriptions[i + 1].getNNF() });
                    inclusions.add(new OWLDescription[] {
                            descriptions[i + 1].getComplementNNF(),
                            descriptions[i].getNNF() });
                }
            } else if (untyped_axiom instanceof OWLDisjointClassesAxiom) {
                OWLDisjointClassesAxiom axiom = (OWLDisjointClassesAxiom) (untyped_axiom);
                OWLDescription[] descriptions = new OWLDescription[axiom.getDescriptions().size()];
                axiom.getDescriptions().toArray(descriptions);
                for (int i = 0; i < descriptions.length; i++)
                    descriptions[i] = descriptions[i].getComplementNNF();
                for (int i = 0; i < descriptions.length; i++)
                    for (int j = i + 1; j < descriptions.length; j++)
                        inclusions.add(new OWLDescription[] { descriptions[i],
                                descriptions[j] });
            } else if (untyped_axiom instanceof OWLFunctionalObjectPropertyAxiom) {
                OWLFunctionalObjectPropertyAxiom axiom = (OWLFunctionalObjectPropertyAxiom) (untyped_axiom);
                inclusions.add(new OWLDescription[] { m_factory.getOWLObjectMaxCardinalityRestriction(
                        axiom.getProperty().getSimplified(), 1) });
            } else if (untyped_axiom instanceof OWLInverseFunctionalObjectPropertyAxiom) {
                OWLInverseFunctionalObjectPropertyAxiom axiom = (OWLInverseFunctionalObjectPropertyAxiom) (untyped_axiom);
                inclusions.add(new OWLDescription[] { m_factory.getOWLObjectMaxCardinalityRestriction(
                        axiom.getProperty().getSimplified().getInverseProperty(),
                        1) });
            } else if (untyped_axiom instanceof OWLSymmetricObjectPropertyAxiom) {
                OWLSymmetricObjectPropertyAxiom axiom = (OWLSymmetricObjectPropertyAxiom) (untyped_axiom);
                OWLObjectPropertyExpression objectProperty = axiom.getProperty().getSimplified();
                roleManager.addInclusion(objectProperty,
                        objectProperty.getInverseProperty().getSimplified());
            } else if (untyped_axiom instanceof OWLTransitiveObjectPropertyAxiom) {
                OWLTransitiveObjectPropertyAxiom axiom = (OWLTransitiveObjectPropertyAxiom) (untyped_axiom);
                roleManager.makeTransitive(axiom.getProperty().getSimplified());
                m_transitiveObjectProperties.add(axiom.getProperty().getSimplified());
            } else if (untyped_axiom instanceof OWLFunctionalDataPropertyAxiom) {
                OWLFunctionalDataPropertyAxiom axiom = (OWLFunctionalDataPropertyAxiom) (untyped_axiom);
                inclusions.add(new OWLDescription[] { m_factory.getOWLDataMaxCardinalityRestriction(
                        axiom.getProperty(), 1) });
            } else if (untyped_axiom instanceof OWLObjectPropertyDomainAxiom) {
                OWLObjectPropertyDomainAxiom axiom = (OWLObjectPropertyDomainAxiom) (untyped_axiom);
                OWLObjectAllRestriction allPropertyNohting = m_factory.getOWLObjectAllRestriction(
                        axiom.getProperty().getSimplified(),
                        m_factory.getOWLNothing());
                inclusions.add(new OWLDescription[] { axiom.getDomain(),
                        allPropertyNohting });
            } else if (untyped_axiom instanceof OWLObjectPropertyRangeAxiom) {
                OWLObjectPropertyRangeAxiom axiom = (OWLObjectPropertyRangeAxiom) (untyped_axiom);
                OWLObjectAllRestriction allPropertyRange = m_factory.getOWLObjectAllRestriction(
                        axiom.getProperty().getSimplified(),
                        axiom.getRange().getNNF());
                inclusions.add(new OWLDescription[] { allPropertyRange });
            } else if (untyped_axiom instanceof OWLDataPropertyDomainAxiom) {
                OWLDataPropertyDomainAxiom axiom = (OWLDataPropertyDomainAxiom) (untyped_axiom);
                OWLDataRange dataNothing = m_factory.getOWLDataComplementOf(m_factory.getOWLDataType(URI.create("http://www.w3.org/2000/01/rdf-schema#Literal")));
                OWLDataAllRestriction allPropertyNothing = m_factory.getOWLDataAllRestriction(
                        axiom.getProperty(), dataNothing);
                inclusions.add(new OWLDescription[] { axiom.getDomain(),
                        allPropertyNothing });
            } else if (untyped_axiom instanceof OWLDataPropertyRangeAxiom) {
                OWLDataPropertyRangeAxiom axiom = (OWLDataPropertyRangeAxiom) (untyped_axiom);
                OWLDataAllRestriction allPropertyRange = m_factory.getOWLDataAllRestriction(
                        axiom.getProperty(), axiom.getRange());
                inclusions.add(new OWLDescription[] { allPropertyRange });
            } else if (untyped_axiom instanceof OWLAntiSymmetricObjectPropertyAxiom) {
                OWLAntiSymmetricObjectPropertyAxiom axiom = (OWLAntiSymmetricObjectPropertyAxiom) (untyped_axiom);
                OWLObjectPropertyExpression objectProperty = axiom.getProperty().getSimplified();
                m_asymmetricObjectProperties.add(objectProperty);
            } else if (untyped_axiom instanceof OWLDisjointDataPropertiesAxiom) {
                OWLDisjointDataPropertiesAxiom axiom = (OWLDisjointDataPropertiesAxiom) (untyped_axiom);
                OWLDataPropertyExpression[] dataProperties = new OWLDataPropertyExpression[axiom.getProperties().size()];
                axiom.getProperties().toArray(dataProperties);
                m_disjointDataProperties.add(dataProperties);
            } else if (untyped_axiom instanceof OWLDisjointObjectPropertiesAxiom) {
                OWLDisjointObjectPropertiesAxiom axiom = (OWLDisjointObjectPropertiesAxiom) (untyped_axiom);
                OWLObjectPropertyExpression[] objectProperties = new OWLObjectPropertyExpression[axiom.getProperties().size()];
                axiom.getProperties().toArray(objectProperties);
                for (int i = 0; i < objectProperties.length; i++)
                    objectProperties[i] = objectProperties[i].getSimplified();
                m_disjointObjectProperties.add(objectProperties);
            } else if (untyped_axiom instanceof OWLIrreflexiveObjectPropertyAxiom) {
                OWLIrreflexiveObjectPropertyAxiom axiom = (OWLIrreflexiveObjectPropertyAxiom) (untyped_axiom);
                OWLObjectPropertyExpression objectProperty = axiom.getProperty().getSimplified();
                m_irreflexiveObjectProperties.add(objectProperty);
            } else if (untyped_axiom instanceof OWLReflexiveObjectPropertyAxiom) {
                OWLReflexiveObjectPropertyAxiom axiom = (OWLReflexiveObjectPropertyAxiom) (untyped_axiom);
                OWLObjectPropertyExpression objectProperty = axiom.getProperty().getSimplified();
                m_reflexiveObjectProperties.add(objectProperty);
            } else if (untyped_axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom axiom = (OWLClassAssertionAxiom) (untyped_axiom);
                OWLDescription desc = simplify(axiom.getDescription().getNNF(), m_factory);
                if (!isSimple(desc)) {
                    boolean[] alreadyExists = new boolean[1];
                    OWLDescription definition = getDefinitionFor(desc,
                            alreadyExists);
                    if (!alreadyExists[0])
                        inclusions.add(new OWLDescription[] {
                                definition.getComplementNNF(), desc });
                    desc = definition;
                }
                if (desc == axiom.getDescription())
                    m_facts.add(axiom);
                else
                    m_facts.add(m_factory.getOWLClassAssertionAxiom(
                            axiom.getIndividual(), desc));
            } else if (untyped_axiom instanceof OWLObjectPropertyAssertionAxiom) {
                OWLObjectPropertyAssertionAxiom axiom = (OWLObjectPropertyAssertionAxiom) (untyped_axiom);
                m_facts.add(m_factory.getOWLObjectPropertyAssertionAxiom(
                        axiom.getSubject(),
                        axiom.getProperty().getSimplified(), axiom.getObject()));
            } else if (untyped_axiom instanceof OWLNegativeObjectPropertyAssertionAxiom) {
                OWLNegativeObjectPropertyAssertionAxiom axiom = (OWLNegativeObjectPropertyAssertionAxiom) (untyped_axiom);
                OWLObjectOneOf nominal = m_factory.getOWLObjectOneOf(axiom.getObject());
                OWLDescription not_nominal = m_factory.getOWLObjectComplementOf(nominal);
                OWLDescription restriction = m_factory.getOWLObjectAllRestriction(
                        axiom.getProperty().getSimplified(), not_nominal);
                OWLClassAssertionAxiom rewrittenAxiom = m_factory.getOWLClassAssertionAxiom(
                        axiom.getSubject(), restriction);
                OWLDescription desc = simplify(rewrittenAxiom.getDescription().getNNF(), m_factory);
                if (!isSimple(desc)) {
                    boolean[] alreadyExists = new boolean[1];
                    OWLDescription definition = getDefinitionFor(desc,
                            alreadyExists);
                    if (!alreadyExists[0])
                        inclusions.add(new OWLDescription[] {
                                definition.getComplementNNF(), desc });
                    desc = definition;
                }
                if (desc == rewrittenAxiom.getDescription())
                    m_facts.add(axiom);
                else
                    m_facts.add(m_factory.getOWLClassAssertionAxiom(
                            rewrittenAxiom.getIndividual(), desc));
            } else if (untyped_axiom instanceof OWLNegativeDataPropertyAssertionAxiom) {
                OWLNegativeDataPropertyAssertionAxiom axiom = (OWLNegativeDataPropertyAssertionAxiom) (untyped_axiom);
                OWLDataRange dataRange = m_factory.getOWLDataOneOf(axiom.getObject());
                OWLDataRange notDataRange = m_factory.getOWLDataComplementOf(dataRange);
                OWLDescription restriction = m_factory.getOWLDataAllRestriction(
                        axiom.getProperty(), notDataRange);
                boolean[] alreadyExists = new boolean[1];
                OWLDescription definition = getDefinitionFor(restriction,
                        alreadyExists);
                if (!alreadyExists[0]) {
                    inclusions.add(new OWLDescription[] {
                            definition.getComplementNNF(), restriction });
                }
                m_facts.add(m_factory.getOWLClassAssertionAxiom(
                        axiom.getSubject(), definition));
            } else if (untyped_axiom instanceof OWLSameIndividualsAxiom) {
                OWLSameIndividualsAxiom axiom = (OWLSameIndividualsAxiom) (untyped_axiom);
                m_facts.add(axiom);
            } else if (untyped_axiom instanceof OWLDifferentIndividualsAxiom) {
                OWLDifferentIndividualsAxiom axiom = (OWLDifferentIndividualsAxiom) (untyped_axiom);
                m_facts.add(axiom);
            } else if (untyped_axiom instanceof OWLAxiomAnnotationAxiom
                    || untyped_axiom instanceof OWLEntityAnnotationAxiom
                    || untyped_axiom instanceof OWLOntologyAnnotationAxiom
                    || untyped_axiom instanceof OWLDeclarationAxiom
                    || untyped_axiom instanceof OWLImportsDeclaration) {
                // do nothing; these axiom types have no effect on reasoning
            } else if (untyped_axiom instanceof OWLDataPropertyAssertionAxiom) {
                OWLDataPropertyAssertionAxiom axiom = (OWLDataPropertyAssertionAxiom) (untyped_axiom);
                OWLDataRange filler = m_factory.getOWLDataOneOf(axiom.getObject());
                OWLDataSomeRestriction restriction = m_factory.getOWLDataSomeRestriction(
                        axiom.getProperty(), filler);
                boolean[] alreadyExists = new boolean[1];
                OWLDescription definition = getDefinitionFor(restriction,
                        alreadyExists);
                if (!alreadyExists[0]) {
                    inclusions.add(new OWLDescription[] {
                            definition.getComplementNNF(), restriction });
                }
                m_facts.add(m_factory.getOWLClassAssertionAxiom(
                        axiom.getSubject(), definition));
            } else {
                throw new RuntimeException("Unsupported axiom type:"
                        + untyped_axiom.getAxiomType().toString());
            }
        }
        OWLDescriptionVisitorEx<OWLDescription> normalizer = new NormalizationVisitor(
                inclusions, this.m_factory);
        normalizeInclusions(inclusions, m_conceptInclusions, m_facts, m_factory, normalizer);
        roleManager.rewriteConceptInclusions(m_conceptInclusions, m_factory);
        m_objectPropertyInclusions = roleManager.getSimpleInclusions();
    }
    
    protected void normalize(List<OWLDescription[]> ioInclusions, Collection<OWLIndividualAxiom> ioFacts) {
        List<OWLDescription[]> outInclusions = new LinkedList<OWLDescription[]>();
        OWLDescriptionVisitorEx<OWLDescription> normalizer = new NormalizationVisitor(
                outInclusions, this.m_factory);
        normalizeInclusions(ioInclusions, outInclusions, ioFacts, m_factory, normalizer);
        roleManager.rewriteConceptInclusions(outInclusions, m_factory);
        ioInclusions.clear();
        ioInclusions.addAll(outInclusions);
    }

    public OWLClass define(OWLDescription desc, List<OWLDescription[]> outInclusions, Collection<OWLIndividualAxiom> outFacts) {
        assert outInclusions.isEmpty();
        assert outFacts.isEmpty();
        boolean[] alreadyExists = new boolean[1];
        OWLClass out = getClassFor(desc, alreadyExists);
        if (!alreadyExists[0]) {
            outInclusions.add(new OWLDescription[] {
                    out.getComplementNNF(), desc });
            outInclusions.add(new OWLDescription[] {
                    desc.getComplementNNF(), out });
            normalize(outInclusions, outFacts);
        }
        return out;
    }

    /**
     * Takes an OWL description and visits it with a new SimplificationVisitor. 
     * During the visit, trivial contradictions are replaced by bottom and 
     * trivial tautologies are replaced with top.
     * @param d an OWL class description
     * @return a simplified OWL description
     */
    protected static OWLDescription simplify(OWLDescription d, OWLDataFactory f) {
        return d.accept(new SimplificationVisitor(f));
    }

    /**
     * @param description an OWL class description
     * @return true if description is a literal or a data range, false otherwise
     */
    protected static boolean isSimple(OWLDescription description) {
        return description instanceof OWLClass
                || (description instanceof OWLObjectComplementOf && ((OWLObjectComplementOf) description).getOperand() instanceof OWLClass)
                // assuming that we do not further normalize data ranges for now
                || (description instanceof OWLDataRange);
    }

    static protected void normalizeInclusions(List<OWLDescription[]> inclusions,
            Collection<OWLDescription[]> outInclusions,
            Collection<OWLIndividualAxiom> ioFacts,
            OWLDataFactory factory,
            OWLDescriptionVisitorEx<OWLDescription> normalizer) {
        //List<OWLDescription[]> inclusions = new LinkedList<OWLDescription[]>(ioInclusions);
        while (!inclusions.isEmpty()) {
            OWLDescription simplifiedDescription = simplify(factory.getOWLObjectUnionOf(inclusions.remove(inclusions.size() - 1)), factory);
            if (!simplifiedDescription.isOWLThing()) {
                if (simplifiedDescription instanceof OWLObjectUnionOf) {
                    OWLObjectUnionOf objectOr = (OWLObjectUnionOf) simplifiedDescription;
                    OWLDescription[] descriptions = new OWLDescription[objectOr.getOperands().size()];
                    objectOr.getOperands().toArray(descriptions);
                    if (!distributeUnionOverAnd(descriptions, inclusions)
                            && !optimizedNegativeOneOfTranslation(descriptions,
                                    factory, ioFacts)) {
                        for (int index = 0; index < descriptions.length; index++)
                            descriptions[index] = descriptions[index].accept(normalizer);
                        outInclusions.add(descriptions);
                    }
                } else if (simplifiedDescription instanceof OWLObjectIntersectionOf) {
                    OWLObjectIntersectionOf objectAnd = (OWLObjectIntersectionOf) simplifiedDescription;
                    for (OWLDescription conjunct : objectAnd.getOperands())
                        inclusions.add(new OWLDescription[] { conjunct });
                } else {
                    OWLDescription normalized = simplifiedDescription.accept(normalizer);
                    outInclusions.add(new OWLDescription[] { normalized });
                }
            }
        }

    }

    protected static boolean optimizedNegativeOneOfTranslation(
            OWLDescription[] descriptions,
            OWLDataFactory factory, Collection<OWLIndividualAxiom> ioFacts) {
        if (descriptions.length == 2) {
            OWLObjectOneOf nominal = null;
            OWLDescription other = null;
            if (descriptions[0] instanceof OWLObjectComplementOf
                    && ((OWLObjectComplementOf) descriptions[0]).getOperand()
                        instanceof OWLObjectOneOf) {
                nominal = (OWLObjectOneOf) ((OWLObjectComplementOf) descriptions[0]).getOperand();
                other = descriptions[1];
            } else if (descriptions[1] instanceof OWLObjectComplementOf
                    && ((OWLObjectComplementOf) descriptions[1]).getOperand()
                        instanceof OWLObjectOneOf) {
                other = descriptions[0];
                nominal = (OWLObjectOneOf) ((OWLObjectComplementOf) descriptions[1]).getOperand();
            }
            if (nominal != null &&
                    (other instanceof OWLClass ||
                        (other instanceof OWLObjectComplementOf &&
                            ((OWLObjectComplementOf) other).getOperand()
                                instanceof OWLClass))) {
                for (OWLIndividual individual : nominal.getIndividuals()) {
                    ioFacts.add(factory.getOWLClassAssertionAxiom(individual,
                            other));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * If exactly one of the classes in descriptions is an intersection, new 
     * inclusions are added that each contain one intersection operand and all 
     * the other descriptions. 
     * @param descriptions an array of OWL class descriptions that represent a 
     *                     disjunction of classes that originate from a GCI
     * @param inclusions   The set of GCIs from this ontology written a 
     *                     disjunctions 
     * @return true if exactly one of the descriptions is an intersection and 
     * false otherwise  
     */
    protected static boolean distributeUnionOverAnd(OWLDescription[] descriptions,
            List<OWLDescription[]> inclusions) {
        int andIndex = -1;
        for (int index = 0; index < descriptions.length; index++) {
            OWLDescription desc = descriptions[index];
            if (!isSimple(desc))
                if (desc instanceof OWLObjectIntersectionOf) {
                    if (andIndex == -1)
                        andIndex = index;
                    else
                        return false;
                } else
                    return false;
        }
        if (andIndex == -1)
            return false;
        OWLObjectIntersectionOf objectAnd = (OWLObjectIntersectionOf) descriptions[andIndex];
        for (OWLDescription desc : objectAnd.getOperands()) {
            OWLDescription[] newDescriptions = descriptions.clone();
            newDescriptions[andIndex] = desc;
            inclusions.add(newDescriptions);
        }
        return true;
    }

    protected OWLDescription getDefinitionFor(OWLDescription desc,
            boolean[] alreadyExists, boolean forcePositive) {
        OWLDescription definition = m_definitions.get(desc);
        if (definition == null ||
            (forcePositive && !(definition instanceof OWLClass))) {
            definition = m_factory.getOWLClass(URI.create("internal:q#"
                    + m_definitions.size()));
            if (!forcePositive && !desc.accept(PLVisitor.INSTANCE)) {
                definition = m_factory.getOWLObjectComplementOf(definition);
            }
            // TODO: it's a little ugly to switch the definition
            // to positive polarity if it would naturally be negative and
            // could make future normalization less efficient, but in practice
            // we only demand positive polarity for class definitions after
            // the main ontology has already been clausified, so it shouldn't
            // hurt us *too* much.
            m_definitions.put(desc, definition);
            alreadyExists[0] = false;
        } else
            alreadyExists[0] = true;
        return definition;
    }

    protected OWLDescription getDefinitionFor(OWLDescription desc,
            boolean[] alreadyExists) {
        return getDefinitionFor(desc, alreadyExists, false);
    }

    protected OWLClass getClassFor(OWLDescription desc,
            boolean[] alreadyExists) {
        return (OWLClass) getDefinitionFor(desc, alreadyExists, true);
    }

    protected OWLClass getDefinitionForNegativeNominal(OWLObjectOneOf nominal,
            boolean[] alreadyExists) {
        OWLClass definition = m_definitionsForNegativeNominals.get(nominal);
        if (definition == null) {
            definition = m_factory.getOWLClass(URI.create("internal:nnq#"
                    + m_definitionsForNegativeNominals.size()));
            m_definitionsForNegativeNominals.put(nominal, definition);
            alreadyExists[0] = false;
        } else
            alreadyExists[0] = true;
        return definition;
    }

    public Collection<OWLDescription[]> getConceptInclusions() {
        return m_conceptInclusions;
    }

    public Collection<OWLObjectPropertyExpression[]> getObjectPropertyInclusions() {
        return m_objectPropertyInclusions;
    }

    public Collection<OWLDataPropertyExpression[]> getDataPropertyInclusions() {
        return m_dataPropertyInclusions;
    }

    public Set<OWLObjectPropertyExpression> getAsymmetricObjectProperties() {
        return m_asymmetricObjectProperties;
    }

    public Set<OWLObjectPropertyExpression> getReflexiveObjectProperties() {
        return m_reflexiveObjectProperties;
    }

    public Set<OWLObjectPropertyExpression> getIrreflexiveObjectProperties() {
        return m_irreflexiveObjectProperties;
    }

    public Set<OWLObjectPropertyExpression> getTransitiveObjectProperties() {
        return m_transitiveObjectProperties;
    }

    public Set<OWLObjectPropertyExpression[]> getDisjointObjectProperties() {
        return m_disjointObjectProperties;
    }

    public Set<OWLDataPropertyExpression[]> getDisjointDataProperties() {
        return m_disjointDataProperties;
    }

    public Collection<OWLIndividualAxiom> getFacts() {
        return m_facts;
    }

    protected class NormalizationVisitor implements
            OWLDescriptionVisitorEx<OWLDescription> {
        protected final Collection<OWLDescription[]> m_newInclusions;
        protected final boolean[] m_alreadyExists;
        OWLDataFactory factory;

        public NormalizationVisitor(Collection<OWLDescription[]> newInclusions,
                                    OWLDataFactory factory) {
            m_newInclusions = newInclusions;
            m_alreadyExists = new boolean[1];
            this.factory = factory;
        }

        public OWLDescription visit(OWLDataAllRestriction desc) {
            return desc;
        }

        public OWLDescription visit(OWLDataSomeRestriction desc) {
            return desc;
        }

        public OWLDescription visit(OWLDataExactCardinalityRestriction desc) {
            OWLDataPropertyExpression dataProperty = desc.getProperty();
            OWLDataRange dataRange = desc.getFiller();
            OWLDescription definition = getDefinitionFor(desc,
                    m_alreadyExists);
            if (!m_alreadyExists[0]) {
                m_newInclusions.add(new OWLDescription[] {
                        definition.getComplementNNF(),
                        factory.getOWLDataMaxCardinalityRestriction(
                                dataProperty, desc.getCardinality(),
                                dataRange) });
                m_newInclusions.add(new OWLDescription[] {
                        definition.getComplementNNF(),
                        factory.getOWLDataMinCardinalityRestriction(
                                dataProperty, desc.getCardinality(),
                                dataRange) });
            }
            return definition;
        }

        public OWLDescription visit(OWLDataMaxCardinalityRestriction desc) {
            return desc;
        }

        public OWLDescription visit(OWLDataMinCardinalityRestriction desc) {
            if (desc.getCardinality() <= 0)
                return factory.getOWLThing();
            else return desc;
        }

        public OWLDescription visit(OWLDataValueRestriction desc) {
            OWLDataRange dataRange = factory.getOWLDataOneOf(desc.getValue());
            return factory.getOWLDataSomeRestriction(desc.getProperty(),
                    dataRange);
        }

        public OWLDescription visit(OWLClass object) {
            return object;
        }

        public OWLDescription visit(OWLObjectAllRestriction object) {
            OWLDescription description = object.getFiller();
            if (isSimple(description)
                    || description instanceof OWLObjectOneOf
                    || (description instanceof OWLObjectComplementOf
                            && ((OWLObjectComplementOf) description).getOperand() instanceof OWLObjectOneOf && ((OWLObjectOneOf) ((OWLObjectComplementOf) description).getOperand()).getIndividuals().size() == 1))
                // The ObjectOneof cases are optimizations.
                return object;
            else {
                OWLDescription definition = getDefinitionFor(description,
                        m_alreadyExists);
                if (!m_alreadyExists[0])
                    m_newInclusions.add(new OWLDescription[] {
                            definition.getComplementNNF(), description });
                return m_factory.getOWLObjectAllRestriction(
                        object.getProperty().getSimplified(), definition);
            }
        }

        public OWLDescription visit(OWLObjectSomeRestriction object) {
            OWLDescription description = object.getFiller();
            if (isSimple(description) || description instanceof OWLObjectOneOf)
                // The ObjectOneof cases is an optimization.
                return object;
            else {
                OWLDescription definition = getDefinitionFor(description,
                        m_alreadyExists);
                if (!m_alreadyExists[0])
                    m_newInclusions.add(new OWLDescription[] {
                            definition.getComplementNNF(), description });
                return m_factory.getOWLObjectSomeRestriction(
                        object.getProperty().getSimplified(), definition);
            }
        }

        public OWLDescription visit(OWLObjectExactCardinalityRestriction object) {
            OWLObjectPropertyExpression objectProperty = object.getProperty().getSimplified();
            OWLDescription filler = object.getFiller();
            OWLDescription definition = getDefinitionFor(object,
                    m_alreadyExists);
            if (!m_alreadyExists[0]) {
                m_newInclusions.add(new OWLDescription[] {
                        definition.getComplementNNF(),
                        factory.getOWLObjectMaxCardinalityRestriction(
                                objectProperty, object.getCardinality(),
                                filler) });
                m_newInclusions.add(new OWLDescription[] {
                        definition.getComplementNNF(),
                        factory.getOWLObjectMinCardinalityRestriction(
                                objectProperty, object.getCardinality(),
                                filler) });
            }
            return definition;
        }

        public OWLDescription visit(OWLObjectMinCardinalityRestriction object) {
            OWLObjectPropertyExpression objectProperty = object.getProperty().getSimplified();
            OWLDescription filler = object.getFiller();
            if (object.getCardinality() <= 0)
                return factory.getOWLThing();
            else if (isSimple(filler))
                return object;
            else if (object.getCardinality() == 1
                    && filler instanceof OWLObjectOneOf) {
                // This is an optimization
                return factory.getOWLObjectSomeRestriction(
                        objectProperty, filler);
            } else {
                OWLDescription definition = getDefinitionFor(filler,
                        m_alreadyExists);
                if (!m_alreadyExists[0])
                    m_newInclusions.add(new OWLDescription[] {
                            definition.getComplementNNF(), filler });
                return factory.getOWLObjectMinCardinalityRestriction(
                        objectProperty, object.getCardinality(), definition);
            }
        }

        public OWLDescription visit(OWLObjectMaxCardinalityRestriction object) {
            OWLObjectPropertyExpression objectProperty = object.getProperty().getSimplified();
            OWLDescription description = object.getFiller();
            if (object.getCardinality() <= 0) {
                return factory.getOWLObjectAllRestriction(objectProperty,
                        description.getComplementNNF()).accept(this);
            } else if (isSimple(description))
                return object;
            else {
                OWLDescription complementDescription = description.getComplementNNF();
                OWLDescription definition = getDefinitionFor(
                        complementDescription, m_alreadyExists);
                if (!m_alreadyExists[0])
                    m_newInclusions.add(new OWLDescription[] {
                            definition.getComplementNNF(),
                            complementDescription });
                return factory.getOWLObjectMaxCardinalityRestriction(
                        objectProperty, object.getCardinality(),
                        definition.getComplementNNF());
            }
        }

        public OWLDescription visit(OWLObjectComplementOf object) {
            if (object.getOperand() instanceof OWLObjectOneOf) {
                OWLObjectOneOf objectOneOf = (OWLObjectOneOf) object.getOperand();
                OWLClass definition = getDefinitionForNegativeNominal(
                        objectOneOf, m_alreadyExists);
                if (!m_alreadyExists[0]) {
                    for (OWLIndividual individual : objectOneOf.getIndividuals())
                        m_facts.add(factory.getOWLClassAssertionAxiom(
                                individual, definition));
                }
                return factory.getOWLObjectComplementOf(definition);
            } else
                return object;
        }

        public OWLDescription visit(OWLObjectUnionOf object) {
            throw new RuntimeException(
                    "OR should be broken down at the outermost level");
        }

        public OWLDescription visit(OWLObjectIntersectionOf object) {
            OWLDescription definition = getDefinitionFor(object,
                    m_alreadyExists);
            if (!m_alreadyExists[0]) {
                for (OWLDescription description : object.getOperands())
                    m_newInclusions.add(new OWLDescription[] {
                            definition.getComplementNNF(), description });
            }
            return definition;
        }

        public OWLDescription visit(OWLObjectOneOf object) {
            return object;
        }

        public OWLDescription visit(OWLObjectValueRestriction object) {
            OWLObjectOneOf objectOneOf = factory.getOWLObjectOneOf(object.getValue());
            return factory.getOWLObjectSomeRestriction(
                    object.getProperty().getSimplified(), objectOneOf);
        }

        public OWLDescription visit(OWLObjectSelfRestriction object) {
            return object;
        }
    }

    /**
     * checks the polarity
     */
    protected static class PLVisitor implements
            OWLDescriptionVisitorEx<Boolean> {
        protected static final PLVisitor INSTANCE = new PLVisitor();

        public Boolean visit(OWLClass object) {
            if (object.isOWLThing())
                return Boolean.FALSE;
            else if (object.isOWLNothing())
                return Boolean.FALSE;
            else
                return Boolean.TRUE;
        }

        public Boolean visit(OWLObjectAllRestriction object) {
            return object.getFiller().accept(this);
        }

        public Boolean visit(OWLObjectSomeRestriction object) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLObjectMinCardinalityRestriction object) {
            return object.getCardinality() > 0;
        }

        public Boolean visit(OWLObjectMaxCardinalityRestriction object) {
            return object.getCardinality() > 0 ? Boolean.TRUE
                    : object.getFiller().getComplementNNF().accept(this);
        }

        public Boolean visit(OWLObjectExactCardinalityRestriction object) {
            return object.getCardinality() > 0 ? Boolean.TRUE
                    : object.getFiller().getComplementNNF().accept(this);
        }

        public Boolean visit(OWLObjectComplementOf object) {
            return Boolean.FALSE;
        }

        public Boolean visit(OWLObjectUnionOf object) {
            for (OWLDescription desc : object.getOperands())
                if (desc.accept(this))
                    return Boolean.TRUE;
            return Boolean.FALSE;
        }

        public Boolean visit(OWLObjectIntersectionOf object) {
            for (OWLDescription desc : object.getOperands())
                if (desc.accept(this))
                    return Boolean.TRUE;
            return Boolean.FALSE;
        }

        public Boolean visit(OWLObjectOneOf object) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLObjectValueRestriction object) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLObjectSelfRestriction object) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataAllRestriction desc) {
            return Boolean.FALSE;
        }

        public Boolean visit(OWLDataSomeRestriction desc) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataExactCardinalityRestriction desc) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataMaxCardinalityRestriction desc) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataMinCardinalityRestriction desc) {
            return Boolean.TRUE;
        }

        public Boolean visit(OWLDataValueRestriction desc) {
            return Boolean.FALSE;
        }
    }

    static public abstract class RoleManager {
        public void addInclusion(OWLObjectPropertyExpression sub,
                            OWLObjectPropertyExpression sup) {
            List<OWLObjectPropertyExpression> chain
                = new ArrayList<OWLObjectPropertyExpression>();
            chain.add(sub);
            addInclusion(chain, sup);
        }
        
        public abstract void addInclusion
            (List<OWLObjectPropertyExpression> chain,
                OWLObjectPropertyExpression implied);
        
        public void addEquivalence(Set<OWLObjectPropertyExpression> props) {
            OWLObjectPropertyExpression canonical = null;
            for (OWLObjectPropertyExpression p : props) {
                if (canonical == null) {
                    canonical = p;
                } else {
                    addInclusion(p, canonical);
                    addInclusion(canonical, p);
                }
            }
        }
        
        public void makeTransitive(OWLObjectPropertyExpression p) {
            List<OWLObjectPropertyExpression> chain
                = new ArrayList<OWLObjectPropertyExpression>();
            chain.add(p);
            chain.add(p);
            addInclusion(chain, p);
        }
        
        public abstract void rewriteConceptInclusions
            (Collection<OWLDescription[]> inclusions,
                OWLDataFactory factory);
        
        // Should get rid of this:
        public abstract Collection<OWLObjectPropertyExpression[]> getSimpleInclusions();
    }

    protected static class SimplificationVisitor implements
            OWLDescriptionVisitorEx<OWLDescription> {
        protected final OWLDataFactory factory;

        public SimplificationVisitor(OWLDataFactory f) {
            factory = f;
        }

        public OWLDescription visit(OWLClass d) {
            return d;
        }

        public OWLDescription visit(OWLObjectAllRestriction d) {
            if (d.getFiller().isOWLThing())
                return factory.getOWLThing();
            return factory.getOWLObjectAllRestriction(
                    d.getProperty().getSimplified(), d.getFiller().accept(this));
        }

        public OWLDescription visit(OWLObjectSomeRestriction d) {
            if (d.getFiller().isOWLNothing())
                return factory.getOWLNothing();
            return factory.getOWLObjectSomeRestriction(
                    d.getProperty().getSimplified(), d.getFiller().accept(this));
        }

        public OWLDescription visit(OWLObjectMinCardinalityRestriction d) {
            if (d.getCardinality() <= 0)
                return factory.getOWLThing();
            if (d.getFiller().isOWLNothing())
                return factory.getOWLNothing();
            return factory.getOWLObjectMinCardinalityRestriction(
                    d.getProperty().getSimplified(), d.getCardinality(),
                    d.getFiller().accept(this));
        }

        public OWLDescription visit(OWLObjectMaxCardinalityRestriction d) {
            if (d.getFiller().isOWLNothing())
                return factory.getOWLThing();
            if (d.getCardinality() <= 0) {
                return factory.getOWLObjectAllRestriction(
                        d.getProperty().getSimplified(),
                        factory.getOWLObjectComplementOf(d.getFiller()).accept(
                                this));
            }
            return factory.getOWLObjectMaxCardinalityRestriction(
                    d.getProperty().getSimplified(), d.getCardinality(),
                    d.getFiller().accept(this));
        }

        public OWLDescription visit(OWLObjectExactCardinalityRestriction d) {
            if (d.getCardinality() < 0)
                return factory.getOWLNothing();
            if (d.getCardinality() == 0)
                return factory.getOWLObjectAllRestriction(
                        d.getProperty().getSimplified(),
                        factory.getOWLObjectComplementOf(d.getFiller()).accept(
                                this));
            if (d.getFiller().isOWLNothing())
                return factory.getOWLNothing();
            return factory.getOWLObjectExactCardinalityRestriction(
                    d.getProperty().getSimplified(), d.getCardinality(),
                    d.getFiller().accept(this));
        }

        public OWLDescription visit(OWLObjectComplementOf d) {
            OWLDescription s = d.getOperand().accept(this);
            if (s instanceof OWLObjectComplementOf) {
                return ((OWLObjectComplementOf) s).getOperand();
            }
            if (s.isOWLThing())
                return factory.getOWLNothing();
            if (s.isOWLNothing())
                return factory.getOWLThing();
            return factory.getOWLObjectComplementOf(s);
        }

        public OWLDescription visit(OWLObjectUnionOf d) {
            java.util.HashSet<OWLDescription> newDisjuncts = new java.util.HashSet<OWLDescription>();
            for (OWLDescription cur : d.getOperands()) {
                OWLDescription s = cur.accept(this);
                if (s.isOWLThing())
                    return s;
                if (s.isOWLNothing())
                    continue;
                if (s instanceof OWLObjectUnionOf) {
                    newDisjuncts.addAll(((OWLObjectUnionOf) s).getOperands());
                } else
                    newDisjuncts.add(s);
            }
            return factory.getOWLObjectUnionOf(newDisjuncts);
        }

        public OWLDescription visit(OWLObjectIntersectionOf d) {
            java.util.HashSet<OWLDescription> newConjuncts = new java.util.HashSet<OWLDescription>();
            for (OWLDescription cur : d.getOperands()) {
                OWLDescription s = cur.accept(this);
                if (s.isOWLThing())
                    continue;
                if (s.isOWLNothing())
                    return s;
                if (s instanceof OWLObjectIntersectionOf) {
                    newConjuncts.addAll(((OWLObjectIntersectionOf) s).getOperands());
                } else
                    newConjuncts.add(s);
            }
            return factory.getOWLObjectIntersectionOf(newConjuncts);
        }

        public OWLDescription visit(OWLObjectOneOf d) {
            return d;
        }

        public OWLDescription visit(OWLObjectValueRestriction d) {
            return d;
        }

        public OWLDescription visit(OWLObjectSelfRestriction d) {
            return d;
        }

        public OWLDescription visit(OWLDataAllRestriction d) {
            return d;
        }

        public OWLDescription visit(OWLDataExactCardinalityRestriction d) {
            return d;
        }

        public OWLDescription visit(OWLDataMaxCardinalityRestriction d) {
            return d;
        }

        public OWLDescription visit(OWLDataMinCardinalityRestriction d) {
            return d;
        }

        public OWLDescription visit(OWLDataSomeRestriction d) {
            return d;
        }

        public OWLDescription visit(OWLDataValueRestriction d) {
            return d;
        }
    }
}