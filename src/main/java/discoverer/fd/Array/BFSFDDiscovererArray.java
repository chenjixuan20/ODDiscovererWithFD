package discoverer.fd.Array;

import dataStructures.DataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.Array.FDTreeArray.FDTreeNode;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;
import discoverer.fd.FDDiscoverer;

import java.util.*;

public class BFSFDDiscovererArray extends FDDiscoverer {
    Queue<FDDiscoverNodeSavingInfoArray> queue = new LinkedList<>();

    public DiscoverResult discoverFirstTimes(DataFrame data){
        fdCandidates.clear();
        int attributeNum = data.getColumnCount();
        fdTreeArray = new FDTreeArray(attributeNum);
        FDTreeNode root = fdTreeArray.getRoot();
        //手动做第0层
        for(int i = 0; i < attributeNum; i++){
            FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
            FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i,data);
            if(fdResult.status.equals("valid")){
                root.RHSCandidate[i] = true;
                fdCandidates.add(new FDCandidate(new ArrayList<>(), i, root));
            }
        }

        //先手动做第一层的|lhs|=1
        for(int i = 0; i < attributeNum; i++){
            FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNode(i, data, root, null,
                    true, null);
            queue.add(infoArray);
        }

        while (!queue.isEmpty()){
            FDDiscoverNodeSavingInfoArray info = queue.poll();
            FDTreeNode parent = info.nodeInResultTree;
//            prune规则2  key剪枝
            if(trueRHSCounts(parent.RHSCandidate) != attributeNum){
//                设attribute set中的attribute升序排列
//                生成子节点，当parent的attribute一定时，其生成的子节点的attribute从比他大开始（避免重复12，21的情况），比如说当属性为2时，则只生成3，4，5
                for(int i = parent.attribute + 1; i < attributeNum; i++){
                    List<Integer> left = info.listDeepClone(info.left);
//                    prune规则4     XY->Z,则XYZ->M否由XY->M否决定，XY->M成立则不是最小，XY->M不成立则不成立
                    if(hasSubSet(left,i,fdCandidates)){
                        continue;
                    }
                    FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNode(i,data,parent,attributeToConfirmed.get(i),false, info);
                    queue.offer(infoArray);
                }
            }
        }
        return new DiscoverResult(fdTreeArray, fdCandidates);
    }

    public DiscoverResult discoverAfterVaildate(DataFrame data, FDTreeArray reference){
        fdCandidates.clear();
        fdTreeArray = reference;
        int attributeNum = data.getColumnCount();
        FDTreeNode root = fdTreeArray.getRoot();
        /*处理根节点*/
        for(int i = 0; i < attributeNum; i++){
            if(root.RHSCandidate[i]){
                //存在[]->x,需要重新检查
                FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
                FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i,data);
                if(fdResult.status.equals("non-valid")){
                    //结果变为non-valid,子节点中所有rhs[i]变为false
                    root.RHSCandidate[i] = false;
                }else {
                    fdCandidates.add(new FDCandidate(new ArrayList<>(), i, root));
                }
            }
        }

        /*先手动做第一层，得到map，完善后面的剪枝*/
        for(FDTreeNode child : root.children){
            FDDiscoverNodeSavingInfoArray infoArray = reTraverseNode(child, root, null, data, true);
            queue.add(infoArray);
        }

        while(!queue.isEmpty()){
            FDDiscoverNodeSavingInfoArray info = queue.poll();
            FDTreeNode parent = info.nodeInResultTree;

            //处理该parent节点中本来就存在的子节点
            for(FDTreeNode child : parent.children){
                FDDiscoverNodeSavingInfoArray infoArray = reTraverseNode(child, parent, info, data, false);
                queue.add(infoArray);

            }
            //在补充并处理需要加上子节点
            if(info.hasChildren){
                for(FDTreeNode child : info.newChildren){
                    FDDiscoverNodeSavingInfoArray infoArray= vaildateNode(child,data,info,parent);
                    queue.offer(infoArray);
                }
            }

        }
        return new DiscoverResult(fdTreeArray, fdCandidates);
    }


    @Override
    public void addFDCandidate(FDTreeNode node, int attributeNum, List<Integer> left) {
        for(int j = 0; j <  attributeNum; j++){
            if(node.minimal[j]){
                fdCandidates.add(new FDCandidate(left, j, node));
            }
        }
    }
}
