//Main

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class Main {

	public static void main(String[] args) {

		// Parse the OWL file, and create the model for the ontology
		OntModel m =
			ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);

		try {
			m.read("file:///E:/university.owl");
		}
		catch (com.hp.hpl.jena.shared.WrappedIOException e) {
			if (e.getCause() instanceof java.io.FileNotFoundException) {
				System.err.println("A java.io.FileNotFoundException caught: "
						+ e.getCause().getMessage());
				System.err.println("You must alter the path passed to " +
						"OntModel.read() so it finds your university " +
						"ontology");
			}
		}
		catch (Throwable t) {
			System.err.println("Caught exception, message: " + t.getMessage());
		}

		new ClassHierarchy().showHierarchy(System.out, m);
	}
}





// ClassHierarchy

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClassHierarchy {

	protected OntModel m_model;


	public void showHierarchy( PrintStream out, OntModel m ) {

		// create an iterator over the root classes that are not
		// anonymous class expressions
		Iterator<OntClass> i = m.listHierarchyRootClasses()
		.filterDrop(new Filter<OntClass>() {

			public boolean accept(OntClass o) {
				return o.isAnon();
			}});

		while (i.hasNext()) {
			showClass(out, i.next(), new ArrayList<OntClass>(), 0);
		}
	}


	protected void showClass(PrintStream out, OntClass cls, List<OntClass> occurs, int depth) {

		renderClassDescription(out, cls, depth);
		out.println();

		// recurse to the next level down
		if (cls.canAs(OntClass.class) && !occurs.contains(cls)) {
			for (Iterator<OntClass> i = cls.listSubClasses(true); i.hasNext();) {

				OntClass sub = i.next();

				// we push this expression on the occurs list before we recurse
				occurs.add(cls);
				showClass(out, sub, occurs, depth + 1);
				occurs.remove(cls);
			}
		}
	}


	public void renderClassDescription(PrintStream out, OntClass c, int depth) {
		indent(out, depth);

		if (!c.isRestriction() && !c.isAnon()) {
			out.println("classname : " + c.getLocalName());

			// list the instances for this class
			showInstance(out, c, depth + 2);
		}
	}


	protected void showInstance(PrintStream out, OntClass cls, int depth) {

		for(ExtendedIterator<? extends OntResource> iter = cls.listInstances(true); iter.hasNext();){
			indent(out, depth);
			OntResource instance = iter.next();
			out.println("instance : " + instance.getLocalName());
		}

	}


	protected void indent(PrintStream out, int depth) {
		for (int i = 0; i < depth; ++i) {
			out.print("  ");
		}
	}
}
