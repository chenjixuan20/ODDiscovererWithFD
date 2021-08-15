package discoverer.fd.PLI;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;

import dataStructures.fd.PLI.FDTreeNodeEquivalenceClassesPLI;
import dataStructures.fd.FDTree;

import java.util.*;

public class BFSFDDiscovererPLI {
    public List<FDCandidate> discoverCandidateRefinement(DataFrame data){
        List<FDCandidate> fdCandidates = new ArrayList<>();
        FDTree result = new FDTree(data.getColumnCount());
        FDTree.FDTreeNode node = result.getRoot();
        int attributeNum = data.getColumnCount();
        Queue<FDDiscoverNodeSavingInfoPLI> queue = new LinkedList<>();
        FDTreeNodeEquivalenceClassesPLI fdep = new FDTreeNodeEquivalenceClassesPLI();
        queue.offer(new FDDiscoverNodeSavingInfoPLI(node, fdep, new ArrayList<>()));
        while (!queue.isEmpty()){
            FDDiscoverNodeSavingInfoPLI info = queue.poll();
            FDTree.FDTreeNode parent = info.nodeInResultTree;
            //这个for循环i是生成左侧时加入的属性
            for(int i = parent.attribute + 1; i < attributeNum; i++) {
//                int index = 0;
                //初始化一个child节点，attr=i
                FDTree.FDTreeNode child = result.new FDTreeNode(parent, i, attributeNum);
                //clone父节点的left
                List<Integer> left = info.listDeepClone(info.left);
                //得到当前节点的left
                left.add(i);
                //clone父节点的ec
                FDTreeNodeEquivalenceClassesPLI fdTreeNodeEquivalenceClassesPLI = info.fdTreeNodeEquivalenceClassesPLI.deepClone();
//                System.out.println("克隆后的等价类: " + fdTreeNodeEquivalenceClassesPLI);
                //得到当前节点的ec
                fdTreeNodeEquivalenceClassesPLI.mergeLeftNode(i, data);
//                System.out.println("处理后的等价类: " + fdTreeNodeEquivalenceClassesPLI);
//                System.out.println("左侧为:" + left.toString());
//                System.out.println("处理前左侧的RHS: " + child.fdRHS);
//                这个for循环是生成上面左侧的右侧，验证右侧，并对节点做出记录
                for (int k = 0; k < attributeNum; k++) {
//                    System.out.println("--右侧为第" + k + "个属性");
                    //若已经在父结点成立，则必成立，不用验证
                    //对应若A->B成立, 则AX->B剪枝
                    if(child.confirmed.get(k)) {
//                        System.out.println("--根据左侧父结点可推成立，不用处理");
                        continue;
                    }
                    //判断是否平凡
                    else if (isValueInList(k, left)) {
//                        System.out.println("----trivial");
                        child.fdRHSCandidate.set(k, false);
                    }
                    //checkFD
                    else{
//                        fdTreeNodeEquivalenceClassesPLI.initializeRight(k, data);
                        String fdResult = fdTreeNodeEquivalenceClassesPLI.checkFDRefinement(data, k);
//                        System.out.println("----" + fdResult);
                        if (fdResult == "non-valid") {
                            child.fdRHSCandidate.set(k, false);
                        }
                    }
                }
//                System.out.println("处理后左侧的RHS: " + child.fdRHS);
                child.confirmed = child.comfirmedDeepClone(child.fdRHSCandidate, attributeNum);
                child.mininal = checkFDMinimal(child, parent, left, fdCandidates);
//                System.out.println("最小FD: " + child.mininal);
                for (int j = 0; j < attributeNum; j++) {
                    if (child.mininal.get(j)) {
                        fdCandidates.add(new FDCandidate(left, j));
                    }
                }
//                parent.children[index] = child;
//                index++;
                queue.offer(new FDDiscoverNodeSavingInfoPLI(child, fdTreeNodeEquivalenceClassesPLI, left));
            }
        }
        return fdCandidates;
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
