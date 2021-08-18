package validator.fd;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;
import dataStructures.od.*;

import java.util.*;

public class FDBruteForceFullValidator extends FDValidator {

    @Override
    public Set<Integer> validate(List<FDCandidate> fds, DataFrame data){
        Set<Integer> result =new HashSet<>();
        fds = chooseFDs(fds);
        System.out.println("choose FD number: " + fds.size());
        for(FDCandidate fd: fds){
            FDValidationResult res = validateOneFD(fd,data);
            if(res.status.equals("valid")){
                fd.fdTreeNode.vaildRhsInTotal.add(fd.right);
            }else {
                result.addAll(res.violationRows);
            }
        }
        return result;
    }

    protected List<FDCandidate> chooseFDs(List<FDCandidate> fds){
        return fds;
    }

    public FDValidationResult validateOneFD(FDCandidate fd, DataFrame data){
        return getEquivalenceClassFromFDCandidate(fd,data).checkFDRefinement(fd.right,data);
    }

    public FDTreeNodeEquivalenceClasses getEquivalenceClassFromFDCandidate(FDCandidate fd, DataFrame data){
        return getEquivalenceClassFromTwoLists(fd.left, data);
    }

    public FDTreeNodeEquivalenceClasses getEquivalenceClassFromTwoLists
            (List<Integer> left, DataFrame data){
        FDTreeNodeEquivalenceClasses equivalenceClasses
                =new FDTreeNodeEquivalenceClasses();
        //这个地方有优化的空间，因为也要算pli,可以考虑记录父子节点关系，可以减少算pli的次数
        for (Integer column : left) {
            equivalenceClasses.left.fdMerge(column,data);
        }
        return equivalenceClasses;
    }
}
