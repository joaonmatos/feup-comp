import java.util.HashMap;
import java.util.ArrayList;


class ConstantOptimization {

    private HashMap<String, VarInfo> vars;
    private ArrayList<String> fields;

    public ConstantOptimization() {
        this.vars = new HashMap<String, VarInfo>();
        this.fields = new ArrayList<String>();
    }

    public void init(SimpleNode root) {
        SimpleNode classNode = (SimpleNode) root.jjtGetChild(root.jjtGetNumChildren() - 1);

        for (int i = 0; i < classNode.jjtGetNumChildren(); i++) {
            SimpleNode child = (SimpleNode) classNode.jjtGetChild(i);
            String childName = child.jjtGetName();
            
            if (childName.equals("Method")){
                optimize(child);
            }
            else if (childName.equals("VarDeclaration")) {
                fields.add((String) ((SimpleNode) child.jjtGetChild(1)).jjtGetValue());
            }
            else {
                continue;
            }
        }
    }

    public void optimize(SimpleNode method) {
        this.vars = new HashMap<String, VarInfo>();

        SimpleNode paramList, body;
        if (((String) method.jjtGetValue()).equals("main")) {
            paramList = (SimpleNode) method.jjtGetChild(0);
            body = (SimpleNode) method.jjtGetChild(1);
        }
        else {
            paramList = (SimpleNode) method.jjtGetChild(1);
            body = (SimpleNode) method.jjtGetChild(2);
        }

        // process ParamList
        for (int i = 0; i < paramList.jjtGetNumChildren(); i++) {
            SimpleNode child = (SimpleNode) paramList.jjtGetChild(i);
            processVarDeclaration(child, false);
        }

        // process Body
        for (int i = 0; i < body.jjtGetNumChildren();) {
            SimpleNode child = (SimpleNode) body.jjtGetChild(i);
            String childName = child.jjtGetName();

            if (childName.equals("VarDeclaration")) {
                if (processVarDeclaration(child, true)) {
                    body.jjtRemoveChild(i);
                    continue;
                }
            }
            else if (childName.equals("Assign")) {
                i = handleAssignment(child, i, body);
            }
            else if (childName.equals("IfStatement")) {
                i = handleIfStatement(child, i, body);
            }
            else if (childName.equals("While"))
                i = handleWhileLoop(child, i, body);
            else {
                execute(child);
            }
            i++;
        }

    }

    public void execute(SimpleNode node) {
        String nodeName = node.jjtGetName();

        if (nodeName.equals("MethodInvocation"))
            processMethodInvocation(node);
        else if (nodeName.equals("NewIntArray"))
            executeChildren(node);
        else if (nodeName.equals("Return"))
            executeChildren(node);
            
        // OPERATORS
        else if (isArithmetic(node))    // +, -, * and /
            processArithmeticOperation(node);
        else if (isLogical(node))       // !, && and <
            processLogicalOperation(node);

        // TERMINALS
        else if (nodeName.equals("Identifier"))
            processIdentifier(node);
        else if (nodeName.equals("Array"))
            processArray(node);
    }

    private void executeChildren(SimpleNode node) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++)
            execute((SimpleNode) node.jjtGetChild(i));
    }

    private void processMethodInvocation(SimpleNode node) {
        SimpleNode argList = (SimpleNode) node.jjtGetChild(node.jjtGetNumChildren() - 1);
        executeChildren(argList);
    }

    private void processArray(SimpleNode node) {
        execute((SimpleNode) node.jjtGetChild(1));
    }

    private boolean processVarDeclaration(SimpleNode node, boolean local) {
        String type = (String) ((SimpleNode) node.jjtGetChild(0)).jjtGetValue();

        if (!type.equals("int") && !type.equals("boolean"))
            return false;

        String name = (String) ((SimpleNode) node.jjtGetChild(1)).jjtGetValue();

        vars.put(name, new VarInfo(type, local, false));

        return true;
    }

    private SimpleNode processIfStatement(SimpleNode node, SimpleNode body) {
        // condition
        SimpleNode condition = (SimpleNode) node.jjtGetChild(0);
        executeChildren(condition);

        if (condition.jjtGetNumChildren() == 1) {
            SimpleNode valueNode = (SimpleNode) condition.jjtGetChild(0);
            if (valueNode.jjtGetName().equals("Boolean")) {
                int index = ((Boolean) valueNode.jjtGetValue()) ? 1 : 2;
                return (SimpleNode) node.jjtGetChild(index);
            }
        }

        for (int j = 1; j < 3; j++) {
            SimpleNode scope = (SimpleNode) node.jjtGetChild(j);

            for (int i = 0; i < scope.jjtGetNumChildren(); ) {
                SimpleNode child = (SimpleNode) scope.jjtGetChild(i);
                String childName = child.jjtGetName();
    
                if (childName.equals("Assign"))
                    i = handleAssignment(child, i, body);
                else if (childName.equals("IfStatement"))
                    i = handleIfStatement(child, i, body);
                else if (childName.equals("While"))
                    i = handleWhileLoop(child, i, body);
                else
                    execute(child);

                i++;
            }
        }

        return null;
    }

    private int handleWhileLoop(SimpleNode node, int index, SimpleNode body) {
        SimpleNode conditionCopy = new SimpleNode((SimpleNode) node.jjtGetChild(0));
        executeChildren(conditionCopy);

        // if the condition is false the loop code is removed
        if (conditionCopy.jjtGetNumChildren() == 1) {
            SimpleNode child = (SimpleNode) conditionCopy.jjtGetChild(0); 
            if (child.jjtGetName().equals("Boolean")) {
                if (!(Boolean) child.jjtGetValue()) {
                    ((SimpleNode) node.jjtGetParent()).jjtRemoveChild(index);
                    return index - 1;
                }
            }
        }

        SimpleNode scope = (SimpleNode) node.jjtGetChild(1);

        scanAssignments(scope);

        for (int i = 0; i < scope.jjtGetNumChildren();) {
            SimpleNode child = (SimpleNode) scope.jjtGetChild(i);
            String childName = child.jjtGetName();

            if (childName.equals("Assign"))
                i = handleAssignment(child, i, body);
            else if (childName.equals("IfStatement"))
                i = handleIfStatement(child, i, body);
            else if (childName.equals("While"))
                i = handleWhileLoop(child, i, body);
            else
                execute(child);

            i++;
        }
        
        SimpleNode condition = (SimpleNode) node.jjtGetChild(0);
        execute((SimpleNode) condition.jjtGetChild(0));

        // analyse the loop scope
        // after the scope is analysed
        // analyse the condition again
        // if true then this is an infinite loop, since the condition is constant
            // infinite loop
        // if false then the loop only executes once

        return index;
    }

    private void scanAssignments(SimpleNode node) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            String childName = child.jjtGetName();

            if (childName.equals("Assign")) {
                SimpleNode identifier = (SimpleNode) child.jjtGetChild(0);
                if (identifier.jjtGetName().equals("Identifier")) {
                    String varName = (String) identifier.jjtGetValue();

                    System.out.println("variable : " + varName);

                    if (!vars.containsKey(varName))
                        continue;
                    
                    vars.get(varName).setConstant(false);
                }
            }
            else if (childName.equals("IfStatement")) {
                scanAssignments((SimpleNode) child.jjtGetChild(1));
                scanAssignments((SimpleNode) child.jjtGetChild(2));
            }
            else if (childName.equals("While")) {
                scanAssignments((SimpleNode) child.jjtGetChild(1));
            }
        }
    }

    private int handleIfStatement(SimpleNode node, int index, SimpleNode body) {
        SimpleNode scope = processIfStatement(node, body);

        if (scope == null)
            return index;

        SimpleNode parent = (SimpleNode) node.jjtGetParent();
        
        parent.jjtRemoveChild(index);
        int scopeLength = scope.jjtGetNumChildren();
                
        if (scopeLength < 1)
            return index - 1;
                
        parent.jjtAddChildAt(scope.jjtGetChild(0), index);
        for (int i = 1; i < scopeLength; i++) {
            SimpleNode instruction = (SimpleNode) scope.jjtGetChild(i);
            instruction.jjtSetParent(parent);
            parent.jjtAddChildAt(instruction, index + i);
        }

        return index;
    }

    /**
     * node -> the assignment node
     * body -> the method body node
     */
    private int handleAssignment(SimpleNode node, int index, SimpleNode body) {
        if (processAssignment(node)) {
            // ((SimpleNode) node.jjtGetParent()).jjtRemoveChild(index);
            // return index - 1;
            return index;
        }

        String nodeName = ((SimpleNode) node.jjtGetChild(0)).jjtGetName();
        if (!nodeName.equals("Identifier"))
            return index;
            
        String varName = (String) ((SimpleNode) node.jjtGetChild(0)).jjtGetValue();
        if (!vars.containsKey(varName))
            return index;
            
        if (!vars.get(varName).getLocal()) {
            vars.get(varName).setLocal(false);
            return index;
        }

        // create a new VarDeclaration node and insert it in the method body
        SimpleNode declaration = new SimpleNode(ParserTreeConstants.JJTVARDECLARATION, body);
        SimpleNode type = new SimpleNode(ParserTreeConstants.JJTTYPE, vars.get(varName).getType(), declaration);
        SimpleNode identifier = new SimpleNode(ParserTreeConstants.JJTIDENTIFIER, varName, declaration);
                
        declaration.jjtAppendChild(type);
        declaration.jjtAppendChild(identifier);
                
        body.jjtAddChildAt(declaration, 0);

        VarInfo info = vars.get(varName);
        info.setLocal(false);
        info.setConstant(false);

        // insertAssignment((SimpleNode) node.jjtGetParent(), index, varName);

        return index;
    }

    /**
     * Returns true if the right side of the expression is a constant, false otherwise
     */
    private boolean processAssignment(SimpleNode node) {
        String nodeType = ((SimpleNode) node.jjtGetChild(0)).jjtGetName();
        
        if (!nodeType.equals("Identifier")) {
            execute((SimpleNode) node.jjtGetChild(0));
            return false;
        }

        String identifier = (String) ((SimpleNode) node.jjtGetChild(0)).jjtGetValue();

        for (int i = 1; i < node.jjtGetNumChildren(); i++){
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            execute(child);
        }

        if (fields.contains(identifier)) 
            return false;
        
        String expression = ((SimpleNode) node.jjtGetChild(1)).jjtGetName();
        if (!expression.equals("Integer") && !expression.equals("Boolean")) {
            return false;
        }

        if (vars.containsKey(identifier)) {
            VarInfo info = vars.get(identifier);
            info.setValue(((SimpleNode) node.jjtGetChild(1)).jjtGetValue());
            info.setConstant(true);
        }
        else {
            String type = expression.equals("Integer") ? "int" : "boolean";
            Object value = ((SimpleNode) node.jjtGetChild(1)).jjtGetValue();
            vars.put(identifier, new VarInfo(type, value, false, true));
        }

        if (((SimpleNode) node.jjtGetParent()).jjtGetName().equals("Scope")) {
            vars.get(identifier).setConstant(false);
            return false;
        }
            
        return true;
    }

    private boolean isArithmetic(SimpleNode node) {
        String nodeName = node.jjtGetName();
        return nodeName.equals("Add") || nodeName.equals("Sub") || nodeName.equals("Mul") || nodeName.equals("Div");
    }

    private boolean isLogical(SimpleNode node) {
        String nodeName = node.jjtGetName();
        return nodeName.equals("Not") || nodeName.equals("Less") || nodeName.equals("And");
    }

    private void processLogicalOperation(SimpleNode node) {
        int count = 0;
        int index = 0;

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            execute(child);

            String childName = child.jjtGetName();
            if (childName.equals("Integer") || childName.equals("Boolean")) {
                index = i;
                count++;
            }
        }

        String nodeName = node.jjtGetName();
        if (nodeName.equals("Not")) {
            if (count != 1)
                return;
            
            Boolean value = (Boolean) ((SimpleNode) node.jjtGetChild(0)).jjtGetValue();
            node.setId(ParserTreeConstants.JJTBOOLEAN);
            node.jjtSetValue(!value);
            node.jjtRemoveChildren();
        }
        else if (nodeName.equals("And")) {
            if (count < 1)
                return;
            else if (count == 1) {
                SimpleNode expression = (SimpleNode) node.jjtGetChild((index + 1) % 2);
                boolean value = (Boolean) ((SimpleNode) node.jjtGetChild(index)).jjtGetValue();

                if (value) {
                    node.setId(expression.getId());
                    node.jjtSetValue(expression.jjtGetValue());
                    node.jjtRemoveChildren();
                    for (int i = 0; i < expression.jjtGetNumChildren(); i++)
                        node.jjtAppendChild(expression.jjtGetChild(i));
                }
                else {
                    node.setId(ParserTreeConstants.JJTBOOLEAN);
                    node.jjtSetValue(false);
                    node.jjtRemoveChildren();
                }
            }
            else if (count == 2) {
                boolean value1 = (Boolean) ((SimpleNode) node.jjtGetChild(0)).jjtGetValue();
                boolean value2 = (Boolean) ((SimpleNode) node.jjtGetChild(1)).jjtGetValue();

                node.setId(ParserTreeConstants.JJTBOOLEAN);
                node.jjtSetValue(value1 && value2);
                node.jjtRemoveChildren();
            }

        }
        else if (nodeName.equals("Less")) {
            if (count < 2)
                return;
            else if (count == 2) {
                int value1 = (Integer) ((SimpleNode) node.jjtGetChild(0)).jjtGetValue();
                int value2 = (Integer) ((SimpleNode) node.jjtGetChild(1)).jjtGetValue();

                node.setId(ParserTreeConstants.JJTBOOLEAN);
                node.jjtSetValue(value1 < value2);
                node.jjtRemoveChildren();
            }
        }
    }

    /**
     * Returns the number of children that are Integers
     */
    private int processArithmeticOperation(SimpleNode node) {
        int count = 0;
        int index = 0;
        int child_count = 0;

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);

            if (isArithmetic(child))
                child_count = processArithmeticOperation(child);
            else
                execute(child);
            
            if (child.jjtGetName().equals("Integer")) {
                index = i;
                count++;
            }
        }

        if (count == 0) {
            // checks if expression is an arithmetic expression with one integer child
            if (child_count < 1) {
                return 1;
            }

            // TODO: 
            // ((a + 2) + b) + 1

            return 0;
        }
        // in case the outer expression has an integer
        // example:  (a + 2) + 1 -> a + 3
        else if (count == 1) {
            // checks if expression is an arithmetic expression with one integer child
            if (child_count < 1) {
                return 1;
            }

            SimpleNode integer = (SimpleNode) node.jjtGetChild(index);
            SimpleNode expression = (SimpleNode) node.jjtGetChild((index + 1) % 2);

            String nodeName = node.jjtGetName();
            String expName = expression.jjtGetName();

            if (((nodeName.equals("Add") || nodeName.equals("Sub")) && expName.equals("Add"))
                || (nodeName.equals("Mul") && expName.equals("Mul"))) {

                SimpleNode[] children = new SimpleNode[2];
                children[0] = (SimpleNode) expression.jjtGetChild(0);
                children[1] = (SimpleNode) expression.jjtGetChild(1);
                
                index = children[0].jjtGetName().equals("Integer") ? 0 : 1;
                children[index].jjtSetValue(calculate(children[index], nodeName, integer));
                children[index].jjtSetParent(node);
                node.jjtAddChild(children[index], index);
                
                index = (index + 1) % 2;
                children[index].jjtSetParent(node);
                node.jjtAddChild(children[index], index);

                node.setId(expression.getId());
            }
            else if ((nodeName.equals("Add") || nodeName.equals("Sub")) && expName.equals("Sub")) {

            }
            else if (nodeName.equals("Mul") && expName.equals("Div")) {
                // TODO:
            }
            else if (nodeName.equals("Div") && expName.equals("Mul")) {
                // TODO:
            }
            else if (nodeName.equals("Div") && expName.equals("Div")) {
                // TODO:
            }

            return 1;
        }
        // in case both children are integers
        // example: 3 + 4 -> 7
        else if (count == 2) {
            int value = calculate((SimpleNode) node.jjtGetChild(0), node.jjtGetName(), (SimpleNode) node.jjtGetChild(1));
            node.jjtSetValue(value);
            node.setId(ParserTreeConstants.JJTINTEGER);     // change the node type to "Integer"
            node.jjtRemoveChildren();                       // remove the node children
            return 0;
        }

        return 0;
    }

    private void processIdentifier(SimpleNode node) {
        String varName = (String) node.jjtGetValue();

        if (!vars.containsKey(varName) || !vars.get(varName).getConstant())
            return;

        VarInfo info = vars.get(varName);
        if (info.getType().equals("int")) {
            node.setId(ParserTreeConstants.JJTINTEGER);
            node.jjtSetValue((Integer) info.getValue());
        }
        else if (info.getType().equals("boolean")) {
            node.setId(ParserTreeConstants.JJTBOOLEAN);
            node.jjtSetValue((Boolean) info.getValue());
        }
    }
        
    private int calculate(SimpleNode operand1, String operation, SimpleNode operand2) {
        int value1 = (Integer) operand1.jjtGetValue();
        int value2 = (Integer) operand2.jjtGetValue();

        int value = 0;

        if (operation.equals("Add"))
            value = value1 + value2;
        else if (operation.equals("Sub"))
            value = value1 - value2;
        else if (operation.equals("Mul"))
            value = value1 * value2;
        else if (operation.equals("Div"))
            value = value1 / value2;
        
        return value;
    }

    private void insertAssignment(SimpleNode node, int index, String varName) {
        if (!vars.containsKey(varName))
            return;

        VarInfo info = vars.get(varName);
        if (info.getValue() == null)
            return;

        SimpleNode assignment, identifier, expression;
        assignment = new SimpleNode(ParserTreeConstants.JJTASSIGN, node);
        identifier = new SimpleNode(ParserTreeConstants.JJTIDENTIFIER, varName, assignment);
        
        
        if (info.getType().equals("int")) {
            expression = new SimpleNode(ParserTreeConstants.JJTINTEGER, (Integer) info.getValue(), assignment);
            System.out.println(varName + " " + (Integer) info.getValue());
        }
        else{
            expression = new SimpleNode(ParserTreeConstants.JJTBOOLEAN, (Boolean) info.getValue(), assignment);

        }

        assignment.jjtAppendChild(identifier);
        assignment.jjtAppendChild(expression);

        node.jjtAddChildAt(assignment, index);
    }
}
