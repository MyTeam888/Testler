package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class ReadVariableVisitor extends ASTVisitor
{

    Map<String, Set<SimpleName>> readVars;
    String methodName;
    int counter = 1;

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

class StatementReadVariableVisitor extends ASTVisitor
{
    Set<SimpleName> readVars = new HashSet<SimpleName>();

    // public boolean visit(MethodInvocation node)
    // {
    // Expression e = node.getExpression();
    // ITypeBinding itb = e.resolveTypeBinding();
    // if (e.getNodeType() == ASTNode.SIMPLE_NAME)
    // {
    // readVars.add(itb.getQualifiedName());
    // }
    //
    // for (Object n : node.arguments())
    // {
    // if (n instanceof ASTNode)
    // {
    // ASTNode nd = (ASTNode) n;
    // if (nd.getNodeType() == ASTNode.SIMPLE_NAME)
    // {
    // SimpleName sn = (SimpleName) nd;
    // readVars.add(sn.toString());
    // }
    //
    // }
    // }
    //
    // return true;
    // }

    public boolean visit(SimpleName node)
    {
	if (!node.isDeclaration())
	{
	    final IBinding nodeBinding = node.resolveBinding();
	    if (nodeBinding instanceof IVariableBinding)
	    {
		IVariableBinding ivb = (IVariableBinding) nodeBinding;
		readVars.add(node);
		// System.out.println(ivb.getName());
		// System.out.println(ivb.getType().getQualifiedName());
	    }
	    // else
	    // {
	    // System.out.println(node + " " + nodeBinding);
	    // }
	}
	return true;
    }
}