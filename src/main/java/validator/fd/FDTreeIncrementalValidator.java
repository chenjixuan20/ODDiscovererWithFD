package validator.fd;

import dataStructures.DataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;

import java.util.*;

public class FDTreeIncrementalValidator extends FDValidator {

    @Override
    public Set<Integer> validate(List<FDCandidate> fds, DataFrame data) {
        Set<Integer> result =new HashSet<>();
        fds = chooseFDs(fds);
        System.out.println("被选fds：");
        System.out.println(fds);
        System.out.println("choose FD number: " + fds.size());
        for(FDCandidate fd: fds) {
            FDTreeArray.FDTreeNode node = fd.fdTreeNode;
//            System.out.println("node attribute: " + node.attribute);
            List<Integer> nodeLeft = getLeftFromNode(node);
//            System.out.println("fd left: " + fd.left);
//            System.out.println("node left: " + nodeLeft);
            node.ecInDataSet = getNodeECInTotalDate(node, data);
//            System.out.println("node pli: " + node.ecInDataSet);
            FDTreeNodeEquivalenceClasses left = node.ecInDataSet.deepClone();
            FDValidationResult fdResult = left.checkFDRefinement(fd.right, data);
            if(fdResult.status.equals("valid")){
//                System.out.println("v");
                fd.fdTreeNode.vaildRhsInTotal.add(fd.right);
            }else {
//                System.out.println("n-v");
//                System.out.println(fdResult.violationRows);
                result.addAll(fdResult.violationRows);
            }
        }
        return result;
    }

    public static List<Integer> getLeftFromNode(FDTreeArray.FDTreeNode node){
        List<Integer> left = new ArrayList<>();
        FDTreeArray.FDTreeNode index = node;
       while (index.parent != null){
           left.add(index.attribute);
           index = index.parent;
       }
        Collections.reverse(left);
       return left;
    }

    public FDTreeNodeEquivalenceClasses getNodeECInTotalDate(FDTreeArray.FDTreeNode node, DataFrame data){
        if(node.parent == null){
            node.ecInDataSet = new FDTreeNodeEquivalenceClasses();
        }else{
            FDTreeArray.FDTreeNode parent = node.parent;
            if(parent.ecInDataSet != null){
                node.ecInDataSet = parent.ecInDataSet.deepClone().mergeLeftNode(node.attribute, data);
            }else{
                node.ecInDataSet = getNodeECInTotalDate(parent, data).deepClone().mergeLeftNode(node.attribute, data);
            }
        }
        return node.ecInDataSet;
    }


    private List<FDCandidate> chooseFDs(List<FDCandidate> fds) {
        List<FDCandidate> needValidate = new ArrayList<>();
        for(FDCandidate fd: fds){
            if(!fd.fdTreeNode.vaildRhsInTotal.contains(fd.right)){
                needValidate.add(fd);
            }
        }
        return needValidate;
    }
}

