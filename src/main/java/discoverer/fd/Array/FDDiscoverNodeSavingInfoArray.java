package discoverer.fd.Array;

import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.FDTree;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;

import java.util.ArrayList;
import java.util.List;

public class FDDiscoverNodeSavingInfoArray {


    public FDTreeArray.FDTreeNode nodeInResultTree;
    public FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses;
    public List<Integer> left;
    public Boolean[] parentRHs;
    public boolean hasChildren;
    public List<FDTreeArray.FDTreeNode> newChildren;


    public FDDiscoverNodeSavingInfoArray(FDTreeArray.FDTreeNode nodeInResultTree, FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses, List<Integer> left){
        this.nodeInResultTree = nodeInResultTree;
        this.fdTreeNodeEquivalenceClasses = fdTreeNodeEquivalenceClasses;
        this.left = left;
    }

    public FDDiscoverNodeSavingInfoArray(FDTreeArray.FDTreeNode nodeInResultTree, FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses, List<Integer> left, Boolean[] parentRHs){
        this.nodeInResultTree = nodeInResultTree;
        this.fdTreeNodeEquivalenceClasses = fdTreeNodeEquivalenceClasses;
        this.left = left;
        this.parentRHs = parentRHs;
    }

    public List<Integer> listDeepClone(List<Integer> list){
        List<Integer> result = new ArrayList<>();
        for(int i = 0; i < list.size(); i++){
            result.add(list.get(i));
        }
        return result;
    }
}
