package discoverer.fd.Array;

import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.FDCandidate;

import java.util.List;

public class DiscoverResult {
    public FDTreeArray FDTree;
    public List<FDCandidate> fdCandidates;

    public DiscoverResult(FDTreeArray FDTree, List<FDCandidate> fdCandidates){
        this.FDTree = FDTree;
        this.fdCandidates = fdCandidates;
    }
}
