package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class ReadVariableVisitor extends ASTVisitor
{

    Map<String, Set<SimpleName>> readVars;
    String methodName;
    int counter = 0;

    public ReadVariableVisitor(String methodName)
    {
	this.methodName = methodName;
    }

    public boolean visit(ExpressionStatement node)
    {
	// System.out.println(node.toString());
	getReadVars(node);
	return false; // do not continue
    }

    public boolean visit(VariableDeclarationFragment node)
    {
	// System.out.println(node.toString());
	// System.out.println(varDecs);

	getReadVars(node.getInitializer());

	return false;
    }

    public void getReadVars(ASTNode node)
    {
	if (node == null)
	    return;
	StatementReadVariableVisitor srvv = new StatementReadVariableVisitor();
	node.accept(srvv);
	readVars.put(methodName + "-" + counter + ".xml", srvv.readVars);
	counter++;
    }

    public Map<String, Set<SimpleName>> getReadVars()
    {
	return readVars;
    }

    public void setReadVars(Map<String, Set<SimpleName>> readVars)
    {
	this.readVars = readVars;
    }

}