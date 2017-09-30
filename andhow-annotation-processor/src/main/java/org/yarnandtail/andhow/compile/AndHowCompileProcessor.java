package org.yarnandtail.andhow.compile;

import com.sun.source.util.Trees;
import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;


import javax.tools.FileObject;
import org.yarnandtail.andhow.api.Property;

/**
 *
 * Note: check to ensure that Props are not referenced in static init blocks b/c
 * we may need to load the class (and run its init) before andHow init can
 * complete, causing a circular init loop.
 *
 * @author ericeverman
 */
@SupportedAnnotationTypes("*")
public class AndHowCompileProcessor extends AbstractProcessor {

	public static final String GENERATED_CLASS_PREFIX = "$GlobalPropGrpStub";
	public static final String GENERATED_CLASS_NESTED_SEP = "$";

	private static final String SERVICES_PACKAGE = "";
	private static final String RELATIVE_NAME = "META-INF/services/org.yarnandtail.andhow.GlobalPropertyGroupStub";

	//Static to insure all generated classes have the same timestamp
	private static Calendar runDate;

	private Trees trees;

	public AndHowCompileProcessor() {
		//required by Processor API
		runDate = new GregorianCalendar();
	}


	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		Filer filer = this.processingEnv.getFiler();


		StringBuilder existingServiceFileContent = new StringBuilder();
		StringBuilder newServiceFileContent = new StringBuilder();

		try {

			FileObject groupFile = filer.getResource(
					javax.tools.StandardLocation.SOURCE_OUTPUT,
					SERVICES_PACKAGE, RELATIVE_NAME);

			if (groupFile != null) {
				existingServiceFileContent.append(groupFile.getCharContent(true));
			}

		} catch (IOException ex) {
			//Ignore - This just means the file doesn't exist
		}

		//
		//Scan all the Compilation units (i.e. class files) for AndHow Properties
		Iterator<? extends Element> it = roundEnv.getRootElements().iterator();
		for (Element e : roundEnv.getRootElements()) {


			TypeElement te = (TypeElement) e;
			AndHowElementScanner7 st = new AndHowElementScanner7(this.processingEnv, Property.class.getCanonicalName());
			CompileUnit ret = st.scan(e);

			if (ret.hasRegistrations()) {
				
				PropertyRegistrarClassGenerator gen = new PropertyRegistrarClassGenerator(ret, AndHowCompileProcessor.class, runDate);
				
				PropertyRegistrationList regs = ret.getRegistrations();
				
				for (PropertyRegistration p : ret.getRegistrations()) {
					System.out.println("Found Property '" + p.getCanonicalPropertyName() + 
							"' in : " + p.getCanonicalRootName() + " parent class: " + p.getJavaCanonicalParentName());
				}
				
				try {
					writeClassFile(filer, gen, e);
				} catch (Exception ex) {
					error("Unable to write generated classfile '" + gen.buildGeneratedClassFullName() + "'", ex);
				}
			}

			for (String err : ret.getErrors()) {
				System.out.println("Found Error: " + err);
			}

		}

		/*
		for (Element e : globalGroups) {

			if (e.getKind().equals(ElementKind.INTERFACE)) {
				TypeElement ie = (TypeElement) e;

				GroupModel groupModel = new GroupModel(ie);

				trace("Writing proxy class for " + ie.getQualifiedName());

				writeClassFile(filer, groupModel);

				newServiceFileContent.append(groupModel.buildNameModel().getQualifiedName());
				newServiceFileContent.append(System.lineSeparator());

			} else {
				throw new RuntimeException("Not able to handle anything other than an interface currently");
			}

		}

		if (existingServiceFileContent.length() == 0) {
			trace("New " + RELATIVE_NAME + " file created");
			existingServiceFileContent.append("# GENERATED BY THE AndHow AndHowCompileProcessor.");
		}

		try {

			FileObject groupFile = filer.createResource(javax.tools.StandardLocation.SOURCE_OUTPUT,
					SERVICES_PACKAGE, RELATIVE_NAME, globalGroups.toArray(new Element[globalGroups.size()]));

			try (Writer writer = groupFile.openWriter()) {
				writer.write(existingServiceFileContent.toString());
				writer.write(System.lineSeparator());
				writer.write(newServiceFileContent.toString());
			}

		} catch (IOException ex) {
			System.err.println("FAILED TO RUN COMPILE PROCESSOR");
			Logger.getLogger(AndHowCompileProcessor.class.getName()).log(Level.SEVERE, null, ex);
		}

		*/

		return false;

	}

	public void writeClassFile(Filer filer, PropertyRegistrarClassGenerator generator, Element causingElement) throws Exception {

		String genFullName = null;
		
			
		String classContent = generator.generateSource();

		trace("Writing " + generator.buildGeneratedClassFullName() + " as a generated source file");

		FileObject classFile = filer.createSourceFile(generator.buildGeneratedClassFullName(), causingElement);

		try (Writer writer = classFile.openWriter()) {
			writer.write(classContent);
		}
			
	}
		

	public static void trace(String msg) {
		System.out.println("AndHowCompileProcessor: " + msg);
	}

	public static void error(String msg) {
		error(msg, null);
	}

	public static void error(String msg, Exception e) {
		System.err.println("AndHowCompileProcessor: " + msg);
		e.printStackTrace(System.err);
		throw new RuntimeException(msg, e);
	}

}
