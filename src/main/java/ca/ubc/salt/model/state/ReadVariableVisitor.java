package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

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
    
    
    public boolean visit(IfStatement node)
    {
	getReadVars(node);
	return false;
    }
    public boolean visit(WhileStatement node)
    {
	getReadVars(node);
	return false;
    }
    public boolean visit(EnhancedForStatement node)
    {
	getReadVars(node);
	return false;
    }
    public boolean visit(ForStatement node)
    {
	getReadVars(node);
	return false;
    }
    public boolean visit(TryStatement node)
    {
	getReadVars(node);
	return false;
    }
    public boolean visit(SwitchStatement node)
    {
	getReadVars(node);
	return false;
    }
    public boolean visit(SwitchCase node)
    {
	getReadVars(node);
	return false;
    }
    public boolean visit(DoStatement node)
    {
	getReadVars(node);
	return false;
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