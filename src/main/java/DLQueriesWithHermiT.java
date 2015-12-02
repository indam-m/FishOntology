import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

public class DLQueriesWithHermiT {

    static Set<OWLObjectProperty> alreadyAsked;

    public static String parseOWL(String owl){
        owl = owl.replace("<", "");
        owl = owl.replace(">","");
        return owl.substring(owl.lastIndexOf('#')+1);
    }

    public static String askQuestion(OWLObjectProperty op, OWLOntology ontology){
        // menyiapkan pilihan jawaban
        OWLClassExpression[] pilihan = new OWLClassExpression[2];
        for(OWLClassExpression ce: op.getRanges(ontology)) {
            for(OWLClass cl: ce.getClassesInSignature()){
                int i = 0;
                for(OWLClassExpression ce2: cl.getSubClasses(ontology)) {
                    pilihan[i] = ce2;
                    i++;
                }
            }
        }

        String result = "";
        result += parseOWL(op.toString())+"?"+"\n";
        for (int i = 0; i < 2; i++) {
            result += i+"."+parseOWL(pilihan[i].toString())+"\n";
        }

        System.out.println(result);

        Scanner reader = new Scanner(System.in);
        System.out.print("Answer: ");
        int pil = reader.nextInt();
        String query = " and " + parseOWL(op.toString()) + " only " + parseOWL(pilihan[pil].toString());

        return query;
    }

    public static OWLObjectProperty getNextObject(Set<OWLClass> subclasses, OWLOntology ontology){
        System.out.println("=Get Next Object=");
        boolean found = false;
        OWLObjectProperty result = null;
        for(OWLClass subclass: subclasses){
            // dapetin subclass nya: Actinopterygii atau Sarcopterygii
            // dapetin subclass of yang has_fin
            if(subclasses.size() == 1) {
                for (OWLClassExpression c : subclass.getSubClasses(ontology)) {
                    System.out.println(c.toString());
                    for (OWLClassExpression ce1 : c.asOWLClass().getSuperClasses(ontology)) {
                        if (!ce1.isClassExpressionLiteral()) {
                            for (OWLObjectProperty op : ce1.getObjectPropertiesInSignature()) {
                                System.out.println(op.toString());
                                if (!alreadyAsked.contains(op)) {
                                    result = op;
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
            } else { //jumlah class lebih dari 1
                for (OWLClassExpression ce1 : subclass.getSuperClasses(ontology)) {
                    if (!ce1.isClassExpressionLiteral()) {
                        for (OWLObjectProperty op : ce1.getObjectPropertiesInSignature()) {
                            System.out.println(op.toString());
                            if (!alreadyAsked.contains(op)) {
                                result = op;
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) {
                        break;
                    }
                }
            }
            if(found) {
                break;
            }
        }
        System.out.println(result);
        return result;
    }

    public static void main(String[] args) throws Exception {
        alreadyAsked = new HashSet<OWLObjectProperty>();

        // Load an example ontology.
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File file = new File("C:\\Users\\Anggi\\Documents\\kuliah\\Semester 7\\RPP\\Protege\\fishclassification\\tmp\\fish.owl");

        OWLOntology ontology = manager
                .loadOntologyFromOntologyDocument(file);
        Set<OWLObjectProperty> relations = ontology.getObjectPropertiesInSignature();

                // These two lines are the only relevant difference between this code and the original example
        // This example uses HermiT: http://hermit-reasoner.com/
        OWLReasoner reasoner = new Reasoner.ReasonerFactory().createReasoner(ontology);

        ShortFormProvider shortFormProvider = new SimpleShortFormProvider();
        // Create the DLQueryPrinter helper class. This will manage the
        // parsing of input and printing of results
        DLQueryPrinter dlQueryPrinter = new DLQueryPrinter(new DLQueryEngine(reasoner,
                shortFormProvider), shortFormProvider);
        // Enter the query loop. A user is expected to enter class
        // expression on the command line.

        // get has_skeleton
        OWLObjectProperty op = null;
        for(OWLObjectProperty tempObject: ontology.getObjectPropertiesInSignature()){
            if(parseOWL(tempObject.toString()).equals("has_skeleton")){
                op = tempObject;
                break;
            }
        }
        System.out.println(op);
        // ask question
        String query = "";
        Set<OWLClass> result;
        query += askQuestion(op,ontology);
        query = query.replaceFirst(" and ","");
        while(true) {
            System.out.println(query);
//            String classExpression = br.readLine();
            // Check for exit condition
//            if (classExpression == null || classExpression.equalsIgnoreCase("x")) {
//                break;
//            }
            result = dlQueryPrinter.askQuery(query.trim());

            alreadyAsked.add(op);
            op = getNextObject(result,ontology);
            System.out.println();

            query += askQuestion(op,ontology);
        }
    }
}

class DLQueryEngine {
    private final OWLReasoner reasoner;
    private final DLQueryParser parser;

    public DLQueryEngine(OWLReasoner reasoner, ShortFormProvider shortFormProvider) {
        this.reasoner = reasoner;
        parser = new DLQueryParser(reasoner.getRootOntology(), shortFormProvider);
    }

    public Set<OWLClass> getSuperClasses(String classExpressionString, boolean direct) {
        if (classExpressionString.trim().length() == 0) {
            return Collections.emptySet();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        NodeSet<OWLClass> superClasses = reasoner
                .getSuperClasses(classExpression, direct);
        return superClasses.getFlattened();
    }

    public Set<OWLClass> getEquivalentClasses(String classExpressionString) {
        if (classExpressionString.trim().length() == 0) {
            return Collections.emptySet();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(classExpression);
        Set<OWLClass> result = null;
        if (classExpression.isAnonymous()) {
            result = equivalentClasses.getEntities();
        } else {
            result = equivalentClasses.getEntitiesMinus(classExpression.asOWLClass());
        }
        return result;
    }

    public Set<OWLClass> getSubClasses(String classExpressionString, boolean direct) {
        if (classExpressionString.trim().length() == 0) {
            return Collections.emptySet();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        NodeSet<OWLClass> subClasses = reasoner.getSubClasses(classExpression, direct);
        return subClasses.getFlattened();
    }

    public Set<OWLNamedIndividual> getInstances(String classExpressionString,
                                                boolean direct) {
        if (classExpressionString.trim().length() == 0) {
            return Collections.emptySet();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        NodeSet<OWLNamedIndividual> individuals = reasoner.getInstances(classExpression,
                direct);
        return individuals.getFlattened();
    }
}

class DLQueryParser {
    private final OWLOntology rootOntology;
    private final BidirectionalShortFormProvider bidiShortFormProvider;

    public DLQueryParser(OWLOntology rootOntology, ShortFormProvider shortFormProvider) {
        this.rootOntology = rootOntology;
        OWLOntologyManager manager = rootOntology.getOWLOntologyManager();
        Set<OWLOntology> importsClosure = rootOntology.getImportsClosure();
        // Create a bidirectional short form provider to do the actual mapping.
        // It will generate names using the input
        // short form provider.
        bidiShortFormProvider = new BidirectionalShortFormProviderAdapter(manager,
                importsClosure, shortFormProvider);
    }

    public OWLClassExpression parseClassExpression(String classExpressionString) {
        OWLDataFactory dataFactory = rootOntology.getOWLOntologyManager()
                .getOWLDataFactory();
        ManchesterOWLSyntaxEditorParser parser = new ManchesterOWLSyntaxEditorParser(
                dataFactory, classExpressionString);
        parser.setDefaultOntology(rootOntology);
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(bidiShortFormProvider);
        parser.setOWLEntityChecker(entityChecker);
        try {
            return parser.parseClassExpression();
        } catch (ParserException e) {
            e.printStackTrace();
        }
        return null;
    }
}

class DLQueryPrinter {
    private final DLQueryEngine dlQueryEngine;
    private final ShortFormProvider shortFormProvider;

    public DLQueryPrinter(DLQueryEngine engine, ShortFormProvider shortFormProvider) {
        this.shortFormProvider = shortFormProvider;
        dlQueryEngine = engine;
    }

    public Set<OWLClass> askQuery(String classExpression) {
        if (classExpression.length() == 0) {
            System.out.println("No class expression specified");
        } else {
//            StringBuilder sb = new StringBuilder();
//            sb.append("\\nQUERY:   ").append(classExpression).append("\\n\\n");
//            Set<OWLClass> superClasses = dlQueryEngine.getSuperClasses(
//                    classExpression, false);
//            printEntities("SuperClasses", superClasses, sb);
//            Set<OWLClass> equivalentClasses = dlQueryEngine
//                    .getEquivalentClasses(classExpression);
//            printEntities("EquivalentClasses", equivalentClasses, sb);
            Set<OWLClass> subClasses = dlQueryEngine.getSubClasses(classExpression,
                    true);
//            printEntities("SubClasses", subClasses, sb);
//            Set<OWLNamedIndividual> individuals = dlQueryEngine.getInstances(
//                    classExpression, true);
//            printEntities("Instances", individuals, sb);
//            System.out.println(sb.toString());
            return subClasses;
        }
        return null;
    }

    private void printEntities(String name, Set<? extends OWLEntity> entities,
                               StringBuilder sb) {
        sb.append(name);
        int length = 50 - name.length();
        for (int i = 0; i < length; i++) {
            sb.append(".");
        }
        sb.append("\\n\\n");
        if (!entities.isEmpty()) {
            for (OWLEntity entity : entities) {
                sb.append("\\t").append(shortFormProvider.getShortForm(entity))
                        .append("\\n");
            }
        } else {
            sb.append("\\t[NONE]\\n");
        }
        sb.append("\\n");
    }
}