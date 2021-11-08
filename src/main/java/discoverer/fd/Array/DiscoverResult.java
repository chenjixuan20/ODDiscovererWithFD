package discoverer.fd.Array;

import dataStructures.EquivalenceClass;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.FDCandidate;

import java.util.HashMap;
import java.util.List;

public class DiscoverResult {
    public FDTreeArray FDTree;
    public List<FDCandidate> fdCandidates;
    public HashMap<String, EquivalenceClass> listEcMap;


    public DiscoverResult(FDTreeArray FDTree, List<FDCandidate> fdCandidates){
        this.FDTree = FDTree;
        this.fdCandidates = fdCandidates;
    }
    public DiscoverResult(FDTreeArray FDTree, List<FDCandidate> fdCandidates, HashMap<String, EquivalenceClass> listEcMap){
        this.FDTree = FDTree;
        this.fdCandidates = fdCandidates;
        this.listEcMap = listEcMap;
    }
}
