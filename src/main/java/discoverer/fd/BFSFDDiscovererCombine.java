package discoverer.fd;

import dataStructures.DataFrame;
import dataStructures.EquivalenceClass;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTree;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;

import java.util.*;

public class BFSFDDiscovererCombine {

    /**
     * 使用refinement方法，并且使用fd计算出来的信息传递给od去使用
     * @param data
     * @return
     */
    public FDToODSavingInfo discoverCandidateRefinementPlus(DataFrame data){
        FDToODSavingInfo infoTransfer = new FDToODSavingInfo();
        List<FDCandidate> fdCandidates = new ArrayList<>();
        FDTree result = new FDTree(data.getColumnCount());
        FDTree.FDTreeNode node = result.getRoot();
        int attributeNum = data.getColumnCount();

        Queue<FDDiscoverNodeSavingInfo> queue = new LinkedList<>();
        FDTreeNodeEquivalenceClasses fde = new FDTreeNodeEquivalenceClasses();
        queue.offer(new FDDiscoverNodeSavingInfo(node, fde, new ArrayList<>()));

        while (!queue.isEmpty()){
            FDDiscoverNodeSavingInfo info = queue.poll();
            FDTree.FDTreeNode parent = info.nodeInResultTree;
            for(int i = parent.attribute + 1; i < attributeNum; i++){
//                int index = 0;
                FDTree.FDTreeNode child = result.new FDTreeNode(parent, i, attributeNum);
                List<Integer> left = info.listDeepClone(info.left);
                FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = info.fdTreeNodeEquivalenceClasses.deepClone();
                fdTreeNodeEquivalenceClasses.mergeLeftNode(i, data);
                EquivalenceClass leftside = fdTreeNodeEquivalenceClasses.left;
                left.add(i);
                infoTransfer.setListAndEquivalenceClasses(left, leftside);
                for(int k = 0; k < attributeNum; k++){
                    //若已经在父结点成立，则必成立，不用验证
                    if(child.confirmed.get(k)){
                        continue;
                    }else if(isSubSet(left, k, fdCandidates)){
                        continue;
                    }else if(isValueInList(k,left)){
                        child.fdRHSCandidate.set(k, false);
                    }else{
                        fdTreeNodeEquivalenceClasses.initializeRight(k, data);
                        FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(k,data);
                        if(fdResult.status == "non-valid"){
                            child.fdRHSCandidate.set(k, false);
                        }
                    }
                }
                child.confirmed = child.comfirmedDeepClone(child.fdRHSCandidate, attributeNum);
                child.mininal = checkFDMinimal(child, parent, left, fdCandidates);
                for(int j = 0; j < attributeNum; j++){
                    if(child.mininal.get(j)){
                        fdCandidates.add(new FDCandidate(left, j));
                    }
                }
//                parent.children[index] = child;
//                index++;
                queue.offer(new FDDiscoverNodeSavingInfo(child, fdTreeNodeEquivalenceClasses, left));
            }
        }
        infoTransfer.setFdCandidates(fdCandidates);
        return infoTransfer;
    }

    public boolean isSubSet(List<Integer> left, int right, List<FDCandidate> fdCandidates){
        boolean result = false;
        for(int i = 0; i < fdCandidates.size(); i++){
            if(fdCandidates.get(i).right == right){
                result = left.containsAll(fdCandidates.get(i).left);
                if(result == true) break;;
            }
        }
        return result;
    }
    //检测是否为平凡函数依赖
    public boolean isValueInList(int value, List<Integer> list){
        boolean reslut = false;
        for(int i = 0; i < list.size(); i++){
            if(value == list.get(i)){
                reslut = true;
                break;
            }
        }
        return reslut;
    }

    public BitSet checkFDMinimal(FDTree.FDTreeNode child, FDTree.FDTreeNode parent,
                                 List<Integer> left, List<FDCandidate> fdCandidates){
        List<Integer> fdRightOfParent = new ArrayList<>();
        for(int i = 0; i < parent.fdRHSCandidate.size(); i++){
            if(parent.fdRHSCandidate.get(i)){
                fdRightOfParent.add(i);
            }
        }
        for(int i = 0; i < child.fdRHSCandidate.size(); i++){
            if(child.fdRHSCandidate.get(i) && !isValueInList(i,fdRightOfParent)){
                child.mininal.set(i);
            }
        }
        for(int i = 0; i < child.fdRHSCandidate.size(); i++){
            if(isSubSet(left, i, fdCandidates)){
                child.mininal.set(i, false);
            }
        }
        return child.mininal;
    }

}
