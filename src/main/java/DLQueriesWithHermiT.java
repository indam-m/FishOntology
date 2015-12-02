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

    static Set<OWLClass> alreadyAsked;

    public static String parseOWL(String owl){
        owl = owl.replace("<", "");
        owl = owl.replace(">","");
        return owl.substring(owl.lastIndexOf('#')+1);
    }

    public static OWLClass getSuperClass (OWLClass subclass, OWLOntology ontology){
        OWLClass superclass = null;
        for(OWLClassExpression ce2: subclass.getSuperClasses(ontology)){
            if(ce2.isClassExpressionLiteral()) { // get superclass
                superclass = ce2.asOWLClass();
            }
        }
        return superclass;
    }

    // get next value of a class of FIsh Name to be a question
    public static OWLClass getValueObjectProperty (OWLClass cl,OWLOntology ontology){
//        System.out.println("=GetValueObjectProperty=");
        OWLClass value = null;
        boolean found = false;
        for(OWLClassExpression ce1: cl.getSuperClasses(ontology)) {
            if (!ce1.isClassExpressionLiteral()) { // get only objectAllValuesFrom or something like "has_blabla only blabla"
//                System.out.println(ce1.toString());
//                System.out.println(ce1.getClassesInSignature()); // get array of OWLClass [value],
                for(OWLClass val: ce1.getClassesInSignature()){
                    if(!alreadyAsked.contains(getSuperClass(val,ontology))){ // jika property belum ditanyakan
                        alreadyAsked.add(getSuperClass(val,ontology));
                        value = val;
                        found = true;
                        break;
                    }
                }
            }
            if(found){
                break;
            }
        }
        return value;
    }

    // menanyakan pertanyaan selanjutnya, dengan input class cl adalah class property
    public static String askQuestion(OWLClass cl, OWLOntology ontology){
        // menyiapkan pilihan jawaban
        OWLClass[] pilihan = new OWLClass[2];
        int i = 0;
        for(OWLClassExpression subclass: cl.getSubClasses(ontology)){
            pilihan[i] = subclass.asOWLClass();
            i++;
        }

        // menyusun pertanyaan
        String result = "";
        result += "How is the characteristic of "+parseOWL(getSuperClass(pilihan[0].asOWLClass(),ontology).toString())+"?"+"\n";
        for (i = 0; i < 2; i++) {
            result += i+"."+parseOWL(pilihan[i].toString()).replace("_","")+"\n";
        }

        // menampilkan pertanyaan
        System.out.println(result);

        // meminta jawaban
        Scanner reader = new Scanner(System.in);
        System.out.print("Answer: ");
        int pil = reader.nextInt();

        // jika salah memasukkan jawaban
        while(pil != 0 && pil != 1){
            System.out.println("There is no answer "+pil);
            System.out.println(result);// menampilkan pertanyaan
            // meminta jawaban
            System.out.print("Answer: ");
            pil = reader.nextInt();
        }

        // menyusun query dari jawaban
        String query = " and has_characteristic only " + parseOWL(pilihan[pil].toString());

        return query;
    }

    // get next value to be a question from result of query
    public static OWLClass getNextValue(Set<OWLClass> subclasses, OWLOntology ontology){
//        System.out.println("=Get Next Object=");
        boolean found = false;
        OWLClass result = null;
        for(OWLClass subclass: subclasses){
            if(subclasses.size() == 1) {
                for (OWLClassExpression c : subclass.getSubClasses(ontology)) { // get subclass
//                    System.out.println(c.toString());
                    result = getValueObjectProperty(c.asOWLClass(),ontology);
                    found = true;
                    break;
                }
            } else { //jumlah class lebih dari 1
//                System.out.println(subclass.toString());
                result = getValueObjectProperty(subclass,ontology);
                found = true;
            }
            if(found) {
                break;
            }
        }
//        System.out.println(result);
        return result;
    }

    public static void main(String[] args) throws Exception {
        alreadyAsked = new HashSet<OWLClass>();

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
        OWLClass name = null;
        for(OWLClass tempClass: ontology.getClassesInSignature()){
            if(parseOWL(tempClass.toString()).equals("Name")){
                name = tempClass;
                break;
            }
        }
//        System.out.println(name);

        // ask question
        String query = "";
        Set<OWLClass> result = new HashSet<>();
        boolean found = false;

        result.add(name);
        OWLClass value = getNextValue(result,ontology);
//        System.out.println(value);

        query += askQuestion(getSuperClass(value,ontology),ontology); // ambil pertanyaan pertama dari value yang didapat dari kelas Name
        query = query.replaceFirst(" and ","");
        while(true) {
//            System.out.println(query);
//            String classExpression = br.readLine();
            // Check for exit condition
//            if (classExpression == null || classExpression.equalsIgnoreCase("x")) {
//                break;
//            }
            result = dlQueryPrinter.askQuery(query.trim()); // ambil hasil kelas dari query

            // jika sudah mencapai jawaban
            if(result.size() == 1){
                for(OWLClass res: result){
                    if(res.getSubClasses(ontology).size() == 0){ // tidak punya subclass lagi
                        System.out.println("RESULT: "+parseOWL(res.toString()).replace("owl:",""));
                        found = true;
                    } else if(res.getSubClasses(ontology).size() == 1){ // punya subclass hanya 1
                        for(OWLClassExpression ce: res.getSubClasses(ontology)){
                            System.out.println("RESULT: "+parseOWL(ce.toString()).replace("owl:",""));
                        }
                        found = true;
                    }
                    if(found){
                        break;
                    }
                }
            }
            if(found){
                break;
            }

            // proses menanyakan pertanyaan selanjutnya
            value = getNextValue(result,ontology); // dapat value selanjutnya untuk ditanyakan
            System.out.println();

            query += askQuestion(getSuperClass(value,ontology),ontology);
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