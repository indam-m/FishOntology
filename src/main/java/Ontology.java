import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import java.io.File;

/**
 * Created by Anggi on 30/11/2015.
 */
public class Ontology {
    String PATH = "C:\\Users\\Anggi\\Documents\\kuliah\\Semester 7\\RPP\\Protege\\fishclassification\\tmp\\fish.owl";

    public void load() throws OWLOntologyCreationException {
        // Get hold of an ontology manager
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        // Load an ontology from the Web
//        IRI iri = IRI.create("http://protege.stanford.edu/ontologies/pizza/pizza.owl");
//        OWLOntology pizzaOntology = manager.loadOntologyFromOntologyDocument(iri);
//        System.out.println("Loaded ontology: " + pizzaOntology);

        // Remove the ontology so that we can load a local copy.
//        manager.removeOntology(pizzaOntology);

        // We can also load ontologies from files. Create a file object that points to the local copy
        File file = new File(PATH);

        // Load the local copy
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
        System.out.println("Loaded ontology: " + ontology);

        // We can always obtain the location where an ontology was loaded from
        IRI documentIRI = manager.getOntologyDocumentIRI(ontology);
        System.out.println(" from: " + documentIRI);

        Reasoner hermit=new Reasoner(ontology);
        System.out.println(hermit.isConsistent());

//        for(OWLNamedIndividual ind: ontology.getIndividualsInSignature()){
        System.out.println("=OWL CLASS=");
        for(OWLClass d: ontology.getClassesInSignature()){
            System.out.println(d.toString());
            for(OWLClassExpression ce1: d.getSuperClasses(ontology)){
                if(!ce1.isClassExpressionLiteral()) {
                    System.out.println(ce1.toString());
                    System.out.println(ce1.getNestedClassExpressions());
                    System.out.println(ce1.getSignature());
                    System.out.println(ce1.getAnonymousIndividuals());
                    System.out.println("here: " + ce1.getObjectPropertiesInSignature());
                    System.out.println(ce1.getDataPropertiesInSignature());
                    System.out.println(ce1.getIndividualsInSignature());
                    System.out.println(ce1.getClassesInSignature());
                    System.out.println(ce1.getDatatypesInSignature());
//                    for (OWLClassExpression ce2 : ce1.getNestedClassExpressions()) {
//                            System.out.println(ce2.toString());
//
//                    }
                }
            }
            System.out.println();
        }
    }

    public static void main(String[] args){
        Ontology on = new Ontology();
        try {
            on.load();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }
}
