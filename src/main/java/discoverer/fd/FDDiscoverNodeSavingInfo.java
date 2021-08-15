package discoverer.fd;

import dataStructures.fd.FDTree;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;

import java.util.ArrayList;
import java.util.List;

public class FDDiscoverNodeSavingInfo {
    public FDTree.FDTreeNode nodeInResultTree;
    public FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses;
    public List<Integer> left ;

    public FDDiscoverNodeSavingInfo(FDTree.FDTreeNode nodeInResultTree, FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses, List<Integer> left){
        this.nodeInResultTree = nodeInResultTree;
        this.fdTreeNodeEquivalenceClasses = fdTreeNodeEquivalenceClasses;
        this.left = left;
    }

    public List<Integer> listDeepClone(List<Integer> list){
        List<Integer> result = new ArrayList<>();
        for(int i = 0; i < list.size(); i++){
            result.add(list.get(i));
        }
        return result;
    }

}
