import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import java.io.File;

/**
 * Created by Anggi on 30/11/2015.
 */
public class Ontology {

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
        File file = new File("C:\\Users\\Anggi\\Documents\\kuliah\\Semester 7\\RPP\\Protege\\fishclassification\\tmp\\pizza.owl");

        // Load the local copy
        OWLOntology localPizza = manager.loadOntologyFromOntologyDocument(file);
        System.out.println("Loaded ontology: " + localPizza);

        // We can always obtain the location where an ontology was loaded from
        IRI documentIRI = manager.getOntologyDocumentIRI(localPizza);
        System.out.println(" from: " + documentIRI);

        // Remove the ontology again so we can reload it later
//        manager.removeOntology(pizzaOntology);

        // When a local copy of one of more ontologies is used, an ontology IRI mapper can be used
        // to provide a redirection mechanism. This means that ontologies can be loaded as if they
        // were located on the Web. In this example, we simply redirect the loading from
        // http://www.co-ode.org/ontologies/pizza/pizza.owl to our local copy above.
//        manager.addIRIMapper(new SimpleIRIMapper(iri, IRI.create(file)));

        // Load the ontology as if we were loading it from the Web (from its ontology IRI)
//        IRI pizzaOntologyIRI = IRI.create("http://protege.stanford.edu/ontologies/pizza/pizza.owl");
//        OWLOntology redirectedPizza = manager.loadOntology(pizzaOntologyIRI);
//        System.out.println("Loaded ontology: " + redirectedPizza);
//        System.out.println(" from: " + manager.getOntologyDocumentIRI(redirectedPizza));
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
