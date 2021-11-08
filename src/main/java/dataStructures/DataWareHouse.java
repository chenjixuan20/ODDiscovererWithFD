package dataStructures;

import discoverer.fd.Array.FDDiscoverNodeSavingInfoArray;

import java.util.HashMap;

public class DataWareHouse {

    public HashMap<String, EquivalenceClass> listEcMap = new HashMap<>();

    private static DataWareHouse instance = new DataWareHouse();

    private DataWareHouse() {
    }

    public static DataWareHouse getInstance(){
        return instance;
    }





}
