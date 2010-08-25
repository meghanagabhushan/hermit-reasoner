/* Copyright 2008, 2009, 2010 by the Oxford University Computing Laboratory

   This file is part of HermiT.

   HermiT is free software: you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   HermiT is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public License
   along with HermiT.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.semanticweb.HermiT.hierarchy;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.semanticweb.HermiT.graph.Graph;
import org.semanticweb.HermiT.hierarchy.DeterministicClassification.GraphNode;
import org.semanticweb.HermiT.hierarchy.HierarchySearch.Relation;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.tableau.ExtensionTable;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.ReasoningTaskDescription;
import org.semanticweb.HermiT.tableau.Tableau;

public class QuasiOrderClassification {
    protected final Tableau m_tableau;
    protected final ClassificationProgressMonitor m_progressMonitor;
    protected final AtomicConcept m_topElement;
    protected final AtomicConcept m_bottomElement;
    protected final Set<AtomicConcept> m_elements;
    protected final Graph<AtomicConcept> m_knownSubsumptions;
    protected final Graph<AtomicConcept> m_possibleSubsumptions;

    public QuasiOrderClassification(Tableau tableau,ClassificationProgressMonitor progressMonitor,AtomicConcept topElement,AtomicConcept bottomElement,Set<AtomicConcept> elements) {
        m_tableau=tableau;
        m_progressMonitor=progressMonitor;
        m_topElement=topElement;
        m_bottomElement=bottomElement;
        m_elements=elements;
        m_knownSubsumptions=new Graph<AtomicConcept>();
        m_possibleSubsumptions=new Graph<AtomicConcept>();
    }
    public Hierarchy<AtomicConcept> classify() {
        Individual freshIndividual=Individual.createAnonymous("fresh-individual");
        if (!m_tableau.isSatisfiable(false,Collections.singleton(Atom.create(m_topElement,freshIndividual)),null,null,Collections.singleton(Atom.create(m_bottomElement,freshIndividual)),null,getSubsumptionTestDescription(m_topElement,m_bottomElement)))
            return Hierarchy.emptyHierarchy(m_elements,m_topElement,m_bottomElement);
        Relation<AtomicConcept> relation=new Relation<AtomicConcept>() {
            public boolean doesSubsume(AtomicConcept parent,AtomicConcept child) {
                Set<AtomicConcept> allKnownSubsumers=getKnownSubsumers(child);
                if (allKnownSubsumers.contains(parent))
                    return true;
                else if (!allKnownSubsumers.contains(parent) && !m_possibleSubsumptions.getSuccessors(child).contains(parent))
                    return false;
                Individual freshIndividual=Individual.createAnonymous("fresh-individual");
                boolean isSubsumedBy=!m_tableau.isSatisfiable(true,Collections.singleton(Atom.create(child,freshIndividual)),null,null,Collections.singleton(Atom.create(parent,freshIndividual)),null,getSubsumptionTestDescription(child,parent));
                if (!isSubsumedBy)
                    prunePossibleSubsumers();
                return isSubsumedBy;
            }
        };
        return buildHierarchy(relation);
    }
    protected Hierarchy<AtomicConcept> buildHierarchy(Relation<AtomicConcept> hierarchyRelation) {
        initializeKnownSubsumptions();
        updateKnownSubsumptionsUsingToldSubsumers();
        Set<AtomicConcept> processedConcepts=updateSubsumptionsUsingLeafNodeStrategy();
        // Unlike Rob's paper our set of possible subsumptions P would only keep unknown possible subsumptions and not known subsumptions as well.
        Set<AtomicConcept> unclassifiedElements=new HashSet<AtomicConcept>();
        for (AtomicConcept element : m_elements) {
            if (!isUnsatisfiable(element)) {
                m_possibleSubsumptions.getSuccessors(element).removeAll(getKnownSubsumers(element));
                if (!m_possibleSubsumptions.getSuccessors(element).isEmpty()) {
                    unclassifiedElements.add(element);
                    continue;
                }
            }
            increaseProgressMonitor(processedConcepts,element);
        }
        Set<AtomicConcept> classifiedElements=new HashSet<AtomicConcept>();
        while (!unclassifiedElements.isEmpty()) {
            AtomicConcept unclassifiedElement=null;
            for (AtomicConcept element : unclassifiedElements) {
                m_possibleSubsumptions.getSuccessors(element).removeAll(getKnownSubsumers(element));
                if (!m_possibleSubsumptions.getSuccessors(element).isEmpty()) {
                    unclassifiedElement=element;
                    break;
                }
                classifiedElements.add(element);
                increaseProgressMonitor(processedConcepts,element);
            }
            unclassifiedElements.removeAll(classifiedElements);
            if (unclassifiedElements.isEmpty())
                break;
            Set<AtomicConcept> unknownPossibleSubsumers=m_possibleSubsumptions.getSuccessors(unclassifiedElement);
            if (!isEveryPossibleSubsumerNonSubsumer(unknownPossibleSubsumers,unclassifiedElement,2,7)){
	            Hierarchy<AtomicConcept> smallHierarchy=buildHierarchyOfUnknownPossible(unknownPossibleSubsumers);
	            checkUnknownSubsumersUsingEnhancedTraversal(hierarchyRelation,smallHierarchy.getTopNode(),unclassifiedElement);
            }
            unknownPossibleSubsumers.clear();
        }
        return buildTransitivelyReducedHierarchy(m_knownSubsumptions);
    }
    private void increaseProgressMonitor(Set<AtomicConcept> processedConcepts,AtomicConcept element) {
    	if (processedConcepts.add(element))
            m_progressMonitor.elementClassified(element);
	}
	protected Hierarchy<AtomicConcept> buildHierarchyOfUnknownPossible(Set<AtomicConcept> unknownSubsumers) {
        Graph<AtomicConcept> smallKnownSubsumptions=new Graph<AtomicConcept>();
        for (AtomicConcept unknownSubsumer0 : unknownSubsumers) {
            smallKnownSubsumptions.addEdge(m_bottomElement,unknownSubsumer0);
            smallKnownSubsumptions.addEdge(unknownSubsumer0,m_topElement);
            Set<AtomicConcept> knownSubsumersOfElement=getKnownSubsumers(unknownSubsumer0);
            for (AtomicConcept unknownSubsumer1 : unknownSubsumers)
                if (knownSubsumersOfElement.contains(unknownSubsumer1))
                    smallKnownSubsumptions.addEdge(unknownSubsumer0,unknownSubsumer1);
        }
        return buildTransitivelyReducedHierarchy(smallKnownSubsumptions);
    }
    protected Set<AtomicConcept> updateSubsumptionsUsingLeafNodeStrategy() {
        Hierarchy<AtomicConcept> hierarchy=buildTransitivelyReducedHierarchy(m_knownSubsumptions);
        Stack<HierarchyNode<AtomicConcept>> toProcess=new Stack<HierarchyNode<AtomicConcept>>();
        toProcess.addAll( hierarchy.getBottomNode().getParentNodes() );
        Set<AtomicConcept> processedConcepts=new HashSet<AtomicConcept>();
        Set<HierarchyNode<AtomicConcept>> unsatHierarchyNodes=new HashSet<HierarchyNode<AtomicConcept>>();
        while( !toProcess.empty() ){
        	HierarchyNode<AtomicConcept> currentHierarchyElement = toProcess.pop();
            AtomicConcept currentHierarchyConcept=currentHierarchyElement.getRepresentative();
            increaseProgressMonitor(processedConcepts,currentHierarchyConcept);
            if (!m_possibleSubsumptions.getSuccessors(currentHierarchyConcept).isEmpty() || isUnsatisfiable(currentHierarchyConcept))
                continue;
            Node rootNodeOfModel=buildModelForConcept(currentHierarchyConcept);
            // If the leaf was unsatisfable we go up to explore its parents, until a satisfiable parent is discovered. Each time a node is unsat this information is propagated downwards.
            if (rootNodeOfModel==null) {
            	makeConceptUnsatisfiable(currentHierarchyConcept);
            	unsatHierarchyNodes.add(currentHierarchyElement);
                toProcess.addAll(currentHierarchyElement.getParentNodes());
                setNewDescendantsUnsat(currentHierarchyElement,unsatHierarchyNodes,toProcess);
            }
            else{
            	 // TODO: Why does a change to .getCanonicalNode() below give incorrect results with ind. reuse? 
                readKnownSubsumersFromRootNode(currentHierarchyConcept,rootNodeOfModel);
                updatePossibleSubsumers();
            }
        }
        return processedConcepts;
    }
    private void setNewDescendantsUnsat(HierarchyNode<AtomicConcept> currentElement,Set<HierarchyNode<AtomicConcept>> unsatHierarchyNodes, Stack<HierarchyNode<AtomicConcept>> toProcess) {
        Set<HierarchyNode<AtomicConcept>> visited=new HashSet<HierarchyNode<AtomicConcept>>();
        Queue<HierarchyNode<AtomicConcept>> toVisit=new LinkedList<HierarchyNode<AtomicConcept>>(currentElement.getChildNodes());
        while (!toVisit.isEmpty()) {
            HierarchyNode<AtomicConcept> current=toVisit.poll();
            if (visited.add(current) && !unsatHierarchyNodes.contains(current)){
                toVisit.addAll(current.getChildNodes());
                unsatHierarchyNodes.add(current);
                makeConceptUnsatisfiable(current.getRepresentative());
                toProcess.remove(current);
            }
        }
	}
	protected Node buildModelForConcept(AtomicConcept concept) {
        Individual freshIndividual=Individual.createAnonymous("fresh-individual");
        Map<Individual,Node> checkedNode=new HashMap<Individual,Node>();
        checkedNode.put(freshIndividual,null);
        if (m_tableau.isSatisfiable(false,Collections.singleton(Atom.create(concept,freshIndividual)),null,null,null,checkedNode,getSatTestDescription(concept)))
        	return checkedNode.get(freshIndividual);
        else
        	return null;
    }
    protected void makeConceptUnsatisfiable(AtomicConcept concept) {
        addKnownSubsumption(concept,m_bottomElement);
    }
    protected boolean isUnsatisfiable(AtomicConcept concept) {
        return m_knownSubsumptions.getSuccessors(concept).contains(m_bottomElement);
    }
    protected void readKnownSubsumersFromRootNode(AtomicConcept subconcept,Node checkedNode) {
        if (checkedNode.getCanonicalNodeDependencySet().isEmpty()) {
            checkedNode=checkedNode.getCanonicalNode();
            addKnownSubsumption(subconcept,m_topElement);
            ExtensionTable.Retrieval retrieval=m_tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[] { false,true },ExtensionTable.View.TOTAL);
            retrieval.getBindingsBuffer()[1]=checkedNode;
            retrieval.open();
            while (!retrieval.afterLast()) {
                Object conceptObject=retrieval.getTupleBuffer()[0];
                if (conceptObject instanceof AtomicConcept && retrieval.getDependencySet().isEmpty() && m_elements.contains(conceptObject))
                    addKnownSubsumption(subconcept,(AtomicConcept)conceptObject);
                retrieval.next();
            }
        }
    }
    protected void updatePossibleSubsumers() {
        ExtensionTable.Retrieval retrieval=m_tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[] { false,false },ExtensionTable.View.TOTAL);
        retrieval.open();
        Object[] tupleBuffer=retrieval.getTupleBuffer();
        while (!retrieval.afterLast()) {
            Object conceptObject=tupleBuffer[0];
            if (conceptObject instanceof AtomicConcept && m_elements.contains(conceptObject)) {
                AtomicConcept atomicConcept=(AtomicConcept)conceptObject;
                Node node=(Node)tupleBuffer[1];
                if (node.isActive() && !node.isBlocked()) {
                    if (m_possibleSubsumptions.getSuccessors(atomicConcept).isEmpty())
                        readPossibleSubsumersFromNodeLabel(atomicConcept,node);
                    else
                        prunePossibleSubsumersOfConcept(atomicConcept,node);
                }
            }
            retrieval.next();
        }
    }
    protected void prunePossibleSubsumers() {
        ExtensionTable.Retrieval retrieval=m_tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[] { false,false },ExtensionTable.View.TOTAL);
        retrieval.open();
        Object[] tupleBuffer=retrieval.getTupleBuffer();
        while (!retrieval.afterLast()) {
            Object conceptObject=tupleBuffer[0];
            if (conceptObject instanceof AtomicConcept && m_elements.contains(conceptObject)) {
                Node node=(Node)tupleBuffer[1];
                if (node.isActive() && !node.isBlocked())
                    prunePossibleSubsumersOfConcept((AtomicConcept)conceptObject,node);
            }
            retrieval.next();
        }
    }
    protected void prunePossibleSubsumersOfConcept(AtomicConcept atomicConcept,Node node) {
        Set<AtomicConcept> possibleSubsumersOfConcept=new HashSet<AtomicConcept>(m_possibleSubsumptions.getSuccessors(atomicConcept));
        for (AtomicConcept atomicCon : possibleSubsumersOfConcept)
            if (!m_tableau.getExtensionManager().containsConceptAssertion(atomicCon,node))
                m_possibleSubsumptions.getSuccessors(atomicConcept).remove(atomicCon);
    }
    protected void readPossibleSubsumersFromNodeLabel(AtomicConcept atomicConcept,Node node) {
        ExtensionTable.Retrieval retrieval=m_tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[] { false,true },ExtensionTable.View.TOTAL);
        retrieval.getBindingsBuffer()[1]=node;
        retrieval.open();
        while (!retrieval.afterLast()) {
            Object concept=retrieval.getTupleBuffer()[0];
            if (concept instanceof AtomicConcept && m_elements.contains(concept))
                addPossibleSubsumption(atomicConcept,(AtomicConcept)concept);
            retrieval.next();
        }
    }
    protected Hierarchy<AtomicConcept> buildTransitivelyReducedHierarchy(Graph<AtomicConcept> knownSubsumptions) {
        final Map<AtomicConcept,GraphNode<AtomicConcept>> allSubsumers=new HashMap<AtomicConcept,GraphNode<AtomicConcept>>();
        for (AtomicConcept element : knownSubsumptions.getElements())
            allSubsumers.put(element,new GraphNode<AtomicConcept>(element,knownSubsumptions.getSuccessors(element)));
        return DeterministicClassification.buildHierarchy(m_topElement,m_bottomElement,allSubsumers);
    }
    protected void updateKnownSubsumptionsUsingToldSubsumers() {
        updateKnownSubsumptionsUsingToldSubsumers(m_tableau.getPermanentDLOntology().getDLClauses());
    }
    protected void updateKnownSubsumptionsUsingToldSubsumers(Set<DLClause> dlClauses) {
        for (DLClause dlClause : dlClauses) {
            if (dlClause.getHeadLength()==1 && dlClause.getBodyLength()==1) {
                DLPredicate headPredicate=dlClause.getHeadAtom(0).getDLPredicate();
                DLPredicate bodyPredicate=dlClause.getBodyAtom(0).getDLPredicate();
                if (headPredicate instanceof AtomicConcept && bodyPredicate instanceof AtomicConcept) {
                    AtomicConcept headConcept=(AtomicConcept)headPredicate;
                    AtomicConcept bodyConcept=(AtomicConcept)bodyPredicate;
                    if (m_elements.contains(headConcept) && m_elements.contains(bodyConcept))
                        addKnownSubsumption(bodyConcept,headConcept);
                }
            }
        }
    }
    protected void checkUnknownSubsumersUsingEnhancedTraversal(Relation<AtomicConcept> hierarchyRelation,HierarchyNode<AtomicConcept> startNode,AtomicConcept pickedElement) {
        Set<HierarchyNode<AtomicConcept>> startSearch=Collections.singleton(startNode);
        Set<HierarchyNode<AtomicConcept>> visited=new HashSet<HierarchyNode<AtomicConcept>>(startSearch);
        Queue<HierarchyNode<AtomicConcept>> toProcess=new LinkedList<HierarchyNode<AtomicConcept>>(startSearch);
        while (!toProcess.isEmpty()) {
            HierarchyNode<AtomicConcept> current=toProcess.remove();
            Set<HierarchyNode<AtomicConcept>> subordinateElements=current.getChildNodes();
            for (HierarchyNode<AtomicConcept> subordinateElement : subordinateElements) {
                AtomicConcept element=subordinateElement.getRepresentative();
                if (visited.contains(subordinateElement))
                    continue;
                if (hierarchyRelation.doesSubsume(element,pickedElement)) {
                    addKnownSubsumption(pickedElement,element);
                    addKnownSubsumptions(pickedElement,subordinateElement.getEquivalentElements());
                    if (visited.add(subordinateElement))
                        toProcess.add(subordinateElement);
                }
                visited.add(subordinateElement);
            }
        }
    }
    protected boolean isEveryPossibleSubsumerNonSubsumer(Set<AtomicConcept> unknownPossibleSubsumers,AtomicConcept pickedElement,int lowerBound,int upperBound) {
        if (unknownPossibleSubsumers.size()>lowerBound && unknownPossibleSubsumers.size()<upperBound) {
            Individual freshIndividual=Individual.createAnonymous("fresh-individual");
            Atom subconceptAssertion=Atom.create(pickedElement,freshIndividual);
            Set<Atom> superconceptAssertions=new HashSet<Atom>();
            Object[] superconcepts=new Object[unknownPossibleSubsumers.size()];
            int index=0;
            for (AtomicConcept unknownSupNode : unknownPossibleSubsumers){
            	Atom atom = Atom.create(unknownSupNode,freshIndividual);
                superconceptAssertions.add(atom);
                superconcepts[index++]=atom.getDLPredicate();
            }
            if (m_tableau.isSatisfiable(false,Collections.singleton(subconceptAssertion),null,null,superconceptAssertions,null,getSubsumedByListTestDescription(pickedElement,superconcepts))) {
                prunePossibleSubsumers();
                return true;
            }
        }
        return false;
    }
    protected Set<AtomicConcept> getKnownSubsumers(AtomicConcept child) {
        return m_knownSubsumptions.getReachableSuccessors(child);
    }
    protected void initializeKnownSubsumptions() {
        for (AtomicConcept element : m_elements) {
            addKnownSubsumption(element,element);
            addKnownSubsumption(element,m_topElement);
            addKnownSubsumption(m_bottomElement,element);
        }
        makeConceptUnsatisfiable(m_bottomElement);
    }
    protected void addKnownSubsumption(AtomicConcept subConcept,AtomicConcept superConcept) {
        m_knownSubsumptions.addEdge(subConcept,superConcept);
    }
    protected void addKnownSubsumptions(AtomicConcept subConcept,Set<AtomicConcept> superConcepts) {
        m_knownSubsumptions.addEdges(subConcept,superConcepts);
    }
    protected void addPossibleSubsumption(AtomicConcept subConcept,AtomicConcept superConcept) {
        m_possibleSubsumptions.addEdge(subConcept,superConcept);
    }
    protected ReasoningTaskDescription getSatTestDescription(AtomicConcept atomicConcept) {
        return ReasoningTaskDescription.isConceptSatisfiable(atomicConcept);
    }
    protected ReasoningTaskDescription getSubsumptionTestDescription(AtomicConcept subConcept,AtomicConcept superConcept) {
        return ReasoningTaskDescription.isConceptSubsumedBy(subConcept,superConcept);
    }
    protected ReasoningTaskDescription getSubsumedByListTestDescription(AtomicConcept subConcept,Object[] superconcepts) {
        return ReasoningTaskDescription.isConceptSubsumedByList(subConcept,superconcepts);
    }
}