package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.Map;

public class TestModelNode
{
    public Map<TestModelNode, TestModelNode> parent = new HashMap<TestModelNode, TestModelNode>();
    public Map<TestModelNode, Long> distFrom = new HashMap<TestModelNode, Long>();
}
