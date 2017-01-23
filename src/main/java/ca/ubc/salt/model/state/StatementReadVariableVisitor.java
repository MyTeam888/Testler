package ca.ubc.salt.model.state;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import ca.ubc.salt.model.utils.Utils;

public class StatementReadVariableVisitor extends ASTVisitor
{
    Set<SimpleName> readVars = new HashSet<SimpleName>();
    Map<String, Set<VarDefinitionPreq>> needToBeDefinedVars;
    String methodName;
    int counter = -1;
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
	    if (nodeBinding == null || nodeBinding instanceof IVariableBinding)
	    {
		IVariableBinding ivb = (IVariableBinding) nodeBinding;
		
		readVars.add(node);
		
		IBinding binding = node.resolveTypeBinding();
		if (binding != null)
		{
		    String type = binding.getName();
//		    type = RunningState.addFinalLiteral(left, type);
		    Utils.addToTheSetInMap(needToBeDefinedVars, methodName + "-" + counter + ".xml",
			    new VarDefinitionPreq(node, type));
		}
		else
		{
		    System.out.println("binding is null for " + node.toString());
		}
		
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


    public Set<SimpleName> getReadVars()
    {
        return readVars;
    }


    public void setReadVars(Set<SimpleName> readVars)
    {
        this.readVars = readVars;
    }


	public Map<String, Set<VarDefinitionPreq>> getNeedToBeDefinedVars() {
		return needToBeDefinedVars;
	}


	public void setNeedToBeDefinedVars(Map<String, Set<VarDefinitionPreq>> needToBeDefinedVars) {
		this.needToBeDefinedVars = needToBeDefinedVars;
	}


	public String getMethodName() {
		return methodName;
	}


	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}


	public int getCounter() {
		return counter;
	}


	public void setCounter(int counter) {
		this.counter = counter;
	}
    
    
    
    
}