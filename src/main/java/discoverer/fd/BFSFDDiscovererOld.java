package discoverer.fd;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTree;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;

import java.util.*;

public class BFSFDDiscovererOld {
    /**
     * 使用传统方法check fd
     * @param data
     * @return
     */
    public List<FDCandidate> discoverCandidate(DataFrame data){
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
//                System.out.println("克隆后的等价类: " + fdTreeNodeEquivalenceClasses);
                fdTreeNodeEquivalenceClasses.mergeLeftNode(i, data);
//                System.out.println("处理后的等价类: " + fdTreeNodeEquivalenceClasses);
                left.add(i);
//                System.out.println("左侧为:" + left.toString());
//                System.out.println("处理前左侧的RHS: " + child.fdRHS);
                for(int k = 0; k < attributeNum; k++){
//                        System.out.println("--右侧为第" + k + "个属性");
                    //若已经在父结点成立，则必成立，不用验证
                    if(child.confirmed.get(k)){
//                            System.out.println("--根据左侧父结点可推成立，不用处理");
                        continue;
                    }
//                      //如果右侧属于左侧，则平凡
                    else if(isValueInList(k,left)){
//                            System.out.println("----trivial");
                        child.fdRHSCandidate.set(k, false);
                    }else{
                        fdTreeNodeEquivalenceClasses.initializeRight(k, data);
                        String fdResult = fdTreeNodeEquivalenceClasses.checkFDOld();
//                            System.out.println("----" + fdResult);
                        if(fdResult == "non-valid"){
                            child.fdRHSCandidate.set(k, false);
                        }
                    }
                }
//                System.out.println("处理后左侧的RHS: " + child.fdRHS);
                child.confirmed = child.comfirmedDeepClone(child.fdRHSCandidate, attributeNum);
                child.mininal = checkFDMinimal(child, parent, left, fdCandidates);
//                System.out.println("最小FD: " + child.mininal);
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
        return fdCandidates;
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

    //fdCandidates中是否已存在fd X->right,其中X是left的子集
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
}
