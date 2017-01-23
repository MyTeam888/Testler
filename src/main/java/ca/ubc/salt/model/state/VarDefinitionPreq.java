package ca.ubc.salt.model.state;

import org.eclipse.jdt.core.dom.SimpleName;

public class VarDefinitionPreq
{

    SimpleName name;
    String type;
    
    public VarDefinitionPreq(SimpleName name, String type)
    {
	this.name = name;
	this.type = type;
    }

    public SimpleName getName()
    {
        return name;
    }

    public void setName(SimpleName name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
    
    @Override
    public String toString()
    {
        // TODO Auto-generated method stub
        return this.type + " " + this.name;
    }
    
    @Override
    public boolean equals(Object obj) {
    	// TODO Auto-generated method stub
    	
    	if (obj instanceof VarDefinitionPreq) {
    		VarDefinitionPreq vdp = (VarDefinitionPreq) obj;
    		return vdp.getName().equals(this.name) && vdp.getType().equals(this.type);
    	}
    	return false;
    }
    
}
