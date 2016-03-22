package ca.ubc.salt.model.instrumenter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class InstrumenterVisitor extends ASTVisitor
{
    LinkedList<VariableDeclarationFragment> varDecs = new LinkedList<VariableDeclarationFragment>();
    Stack<Integer> varDecLen = new Stack<Integer>();
    ASTRewrite astRewrite;
    int randomNumber;
    int counter = 2;
    String methodName;

    public InstrumenterVisitor(ASTRewrite astRewrite, int randomNumber, String methodName)
    {
	this.astRewrite = astRewrite;
	this.randomNumber = randomNumber;
	this.methodName = methodName;
    }

    @Override
    public boolean preVisit2(ASTNode node)
    {
	if (node instanceof Statement && node.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT
		&& node.getNodeType() != ASTNode.VARIABLE_DECLARATION_EXPRESSION
		&& node.getNodeType() != ASTNode.VARIABLE_DECLARATION_FRAGMENT)
	{
	    varDecLen.push(varDecs.size());
	}
	return true;

    }

    public boolean visit(VariableDeclarationFragment node)
    {
	varDecs.add(node);
	// System.out.println(node.toString());
	// System.out.println(varDecs);
	addDumpCode(node.getParent());
	return false; // do not continue
    }

    public boolean visit(ExpressionStatement node)
    {
	if (node.getNodeType() != ASTNode.RETURN_STATEMENT)
	    addDumpCode(node);
	// System.out.println(node.toString());
	// System.out.println(varDecs);
	return false;
    }

    private void addDumpCode(ASTNode node)
    {
	ASTNode newCode = TestClassInstrumenter.generateInstrumentationBlock(randomNumber, varDecs, methodName, counter++ );

	ASTNode parent = node.getParent();
	ListRewrite listRewrite;
	if ((parent instanceof Block))
	{
	    listRewrite = astRewrite.getListRewrite(parent, Block.STATEMENTS_PROPERTY);
	    listRewrite.insertAfter(newCode, node, null);
	}
	else
	{
	    //TODO fill here create a new node and replace it parent list
	}
    }

    @Override
    public void postVisit(ASTNode node)
    {
	if (node instanceof Statement && node.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT
		&& node.getNodeType() != ASTNode.VARIABLE_DECLARATION_EXPRESSION
		&& node.getNodeType() != ASTNode.VARIABLE_DECLARATION_FRAGMENT)
	{
	    int size = varDecLen.pop();
	    while (varDecs.size() > size)
		varDecs.removeLast();
	}
    }
    // public boolean visit(SimpleName node)
    // {
    // if (this.names.contains(node.getIdentifier()))
    // {
    // System.out.println("Usage of '" + node + "' at line " +
    // cu.getLineNumber(node.getStartPosition()));
    // }
    // return true;
    // }

    public ASTNode getFirstParentWithTypeOf(ASTNode node, int parentType)
    {
	if (node.getNodeType() == parentType)
	    return node;

	ASTNode parent = node.getParent();
	while (parent != null && parent.getNodeType() != parentType)
	{
	    node = parent;
	    parent = node.getParent();
	}

	return parent;
    }

    public ASTNode getChildOfFirstParentWithTypeOf(ASTNode node, int parentType)
    {
	if (node.getNodeType() == parentType)
	    return null;

	ASTNode parent = node.getParent();
	while (parent != null && parent.getNodeType() != parentType)
	{
	    node = parent;
	    parent = node.getParent();
	}
	if (parent != null)
	    return node;
	return null;
    }

}
