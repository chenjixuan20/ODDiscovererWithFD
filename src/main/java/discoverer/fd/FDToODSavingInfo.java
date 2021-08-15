package discoverer.fd;

import dataStructures.EquivalenceClass;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FDToODSavingInfo {
    public List<FDCandidate> fdCandidates;
    public HashMap<List<Integer>, EquivalenceClass> attibuteListToEqcMap;

    public FDToODSavingInfo(){
        fdCandidates = new ArrayList<>();
        attibuteListToEqcMap = new HashMap<>();
    }


    public void setListAndEquivalenceClasses(List<Integer> left, EquivalenceClass equivalenceClass){
       attibuteListToEqcMap.put(left, equivalenceClass);
    }

    public void setFdCandidates(List<FDCandidate> fdCandidates){
        this.fdCandidates = fdCandidates;
    }
}
