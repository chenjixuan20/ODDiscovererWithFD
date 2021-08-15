package validator.od;


import dataStructures.DataFrame;
import dataStructures.od.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ODBruteForceFullValidator extends ODValidator{

    @Override
    public Set<Integer> validate(ODTree tree, DataFrame data){
        Set<Integer> result =new HashSet<>();
        List<ODCandidate> ods=tree.getAllOdsOrderByDFS();
        ods=chooseODs(ods);
        for(ODCandidate od: ods){
            result.addAll(validateOneOD(od,data).violationRows);
        }
        return result;
    }
    protected List<ODCandidate> chooseODs(List<ODCandidate> ods){
        return ods;
    }

    public ODValidationResult validateOneOD(ODCandidate od, DataFrame data){
        return getEquivalenceClassFromODCandidate(od,data).check(data);
    }

    public ODTreeNodeEquivalenceClasses getEquivalenceClassFromODCandidate(ODCandidate od, DataFrame data){
        return getEquivalenceClassFromTwoLists(od.leftAndRightAttributeList.left,
                od.leftAndRightAttributeList.right,data);
    }

    public ODTreeNodeEquivalenceClasses getEquivalenceClassFromTwoLists
            (List<AttributeAndDirection> left, List<AttributeAndDirection> right, DataFrame data){
        ODTreeNodeEquivalenceClasses equivalenceClasses
                =new ODTreeNodeEquivalenceClasses();
        for (AttributeAndDirection column : left) {
            equivalenceClasses.left.merge(data,column);
        }
        for (AttributeAndDirection column : right) {
            equivalenceClasses.right.merge(data,column);
        }
        return equivalenceClasses;
    }
}
