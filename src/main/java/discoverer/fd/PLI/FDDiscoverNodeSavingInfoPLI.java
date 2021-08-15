package discoverer.fd.PLI;

import dataStructures.fd.FDTree;
import dataStructures.fd.PLI.FDTreeNodeEquivalenceClassesPLI;

import java.util.ArrayList;
import java.util.List;

public class FDDiscoverNodeSavingInfoPLI {
    public FDTree.FDTreeNode nodeInResultTree;
    public FDTreeNodeEquivalenceClassesPLI fdTreeNodeEquivalenceClassesPLI;
    public List<Integer> left ;

    public FDDiscoverNodeSavingInfoPLI(FDTree.FDTreeNode nodeInResultTree, FDTreeNodeEquivalenceClassesPLI fdTreeNodeEquivalenceClassesPLI, List<Integer> left){
        this.nodeInResultTree = nodeInResultTree;
        this.fdTreeNodeEquivalenceClassesPLI = fdTreeNodeEquivalenceClassesPLI;
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
