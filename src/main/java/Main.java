import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.HashMap;
import java.util.Optional;

public class Main {

    private static final String FILE_PATH = "src/main/java/com/test/privet/TestClass.java";
    private static final String SUBTYPING_ANNOTATION_NAME = "Subtyping";

    public static void main(String[] args) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(new File(FILE_PATH));

        HashMap<String, String> methodArgs = new HashMap<>();
        cu.accept(new MethodArgsCollector(true), methodArgs);
        System.out.println("method arguments collected: " + methodArgs);

        System.out.println();

        HashMap<String, String> fields = new HashMap<>();
        cu.accept(new FieldVisitor(), fields);
        System.out.println("fields collected: " + fields);

        System.out.println();

        cu.accept(new MethodCheckVisitor(fields), null);

    }

    private static class MethodArgsCollector extends VoidVisitorAdapter<HashMap<String, String>> {
        private final boolean fullName;

        public MethodArgsCollector(boolean fullName) {
            this.fullName = fullName;
        }

        @Override
        public void visit(MethodDeclaration method, HashMap<String, String> collector) {
            super.visit(method, collector);

            NodeList<Parameter> paramList = method.getParameters();

            for (Parameter param : paramList) {
                NodeList<AnnotationExpr> annotationList = param.getAnnotations();

                Optional<String> subtypeName = findSubtypingAnnotation(annotationList);
                if (subtypeName.isPresent()) {
                    String prefix = "";
                    if (fullName) {
                        prefix = getClassNameFromMethod(method) + "." + method.getName() + ".";
                    }
//                    String className = getClassNameFromMethod(method);
                    collector.put(prefix + param.getNameAsString(), subtypeName.get());
                }
            }
        }
    }

    private static class MethodCheckVisitor extends VoidVisitorAdapter {

        private final HashMap<String, String> globalVars;

        public MethodCheckVisitor(HashMap<String, String> globalVars) {
            this.globalVars = globalVars;
        }

        @Override
        public void visit(MethodDeclaration method, Object arg) {
            super.visit(method, arg);

            HashMap<String, String> localVariables = new HashMap<>();
            new ExprVisitor().visit(method, localVariables);
            System.out.println("For method " + method.getName() + " collected " + localVariables);

            // аргументы метода (вообще их уже обходили, надо было как-то иначе сохранять)
            HashMap<String, String> methodArgs = new HashMap<>();
            method.accept(new MethodArgsCollector(false), methodArgs);
            System.out.println("method arguments collected: " + methodArgs);

            // здесь надо обойти все присваивания, например
            AssignmentChecker assignmentChecker = new AssignmentChecker(globalVars, localVariables, methodArgs);
            method.accept(assignmentChecker, null);
        }
    }

    private static class AssignmentChecker extends VoidVisitorAdapter {
        private final HashMap<String, String> globalVars;
        private final HashMap<String, String> localVars;
        private final HashMap<String, String> methodArgs;

        public AssignmentChecker(HashMap<String, String> globalVars,
                                 HashMap<String, String> localVars,
                                 HashMap<String, String> methodArgs) {
            this.globalVars = globalVars;
            this.localVars = localVars;
            this.methodArgs = methodArgs;
        }

        @Override
        public void visit(AssignExpr assignExpr, Object arg) {
            // TODO: handle assignment at the time of declaration
            super.visit(assignExpr, arg);

            String leftName = assignExpr.getTarget().asNameExpr().getNameAsString();
            String rightName = assignExpr.getValue().asNameExpr().getNameAsString();

            Optional<String> leftSubtype = findSubtype(leftName);

            if (leftSubtype.isPresent()) {
                Optional<String> rightSubtype = findSubtype(rightName);
                if (rightSubtype.isPresent()) {

                    if (!leftSubtype.get().equals(rightSubtype.get())) {
                        System.out.println("Error at assignment: " + assignExpr +
                                "target type = " + leftSubtype.get() + ", " +
                                "value type = " + rightSubtype.get());
                    }

                }

            }
        }

        private Optional<String> findSubtype(String name) {
            Optional<String> subtypeOpt = Optional.ofNullable(methodArgs.getOrDefault(name, null));

            if (!subtypeOpt.isPresent())
                subtypeOpt = Optional.ofNullable(localVars.getOrDefault(name, null));

            // TODO: handle global args

            return subtypeOpt;
        }
    }


    private static class ExprVisitor extends VoidVisitorAdapter<HashMap<String, String>> {

        @Override
        public void visit(VariableDeclarationExpr variableDeclaration, HashMap<String, String> collector) {
            super.visit(variableDeclaration, collector);

            Optional<String> subtypeName = findSubtypingAnnotation(variableDeclaration.getAnnotations());
            if (subtypeName.isPresent()) {
                variableDeclaration.getVariables().stream().forEach(variableDeclarator -> {
                    collector.put(variableDeclarator.getNameAsString(), subtypeName.get());
                });
            }
        }
    }

    private static class FieldVisitor extends VoidVisitorAdapter<HashMap<String, String>> {

        @Override
        public void visit(FieldDeclaration df, HashMap<String, String> collector) {
            super.visit(df, collector);

            Optional<ClassOrInterfaceDeclaration> fieldClazzNode = df.findAncestor(ClassOrInterfaceDeclaration.class);
            String fieldClazzName = "";
            if (fieldClazzNode.isPresent()) {
                fieldClazzName = fieldClazzNode.get().getFullyQualifiedName().get();
            }

            Optional<String> subtypeName = findSubtypingAnnotation(df.getAnnotations());

            if (subtypeName.isPresent()) {
                String finalFieldClazzName = fieldClazzName;
                df.getVariables().stream().forEach(variableDeclarator -> {
                    collector.put(finalFieldClazzName + "." + variableDeclarator.getNameAsString(), subtypeName.get());
                });
            }
        }
    }

    private static Optional<String> findSubtypingAnnotation(NodeList<AnnotationExpr> annotations) {
        Optional<AnnotationExpr> annotationExpr = annotations
                .stream()
                .filter(annotation -> annotation.getNameAsString().equals(SUBTYPING_ANNOTATION_NAME))
                .findAny();

        if (annotationExpr.isPresent()) {
            AnnotationExpr annotation = annotationExpr.get();
            String subtypeName = annotation.toNormalAnnotationExpr().get()
                    .getPairs()
                    .stream()
                    .map(pair -> pair.getValue().asStringLiteralExpr().asString())
                    .findAny()
                    .get();
            return Optional.ofNullable(subtypeName);
        }
        return Optional.empty();
    }

    private static String getClassNameFromMethod(MethodDeclaration method) {
        Optional<ClassOrInterfaceDeclaration> methodClazzNode;
        String methodClazzName = null;

        methodClazzNode = method.findAncestor(ClassOrInterfaceDeclaration.class);
        if (methodClazzNode.isPresent()) {
            methodClazzName = methodClazzNode.get().getFullyQualifiedName().get();
        }
        return methodClazzName;
    }
}
