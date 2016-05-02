package operations;

import model.ThroughputEntry;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PreProcessorTest {

    private ThroughputJSON calcs;
    private Map<Long, BigDecimal> testMap;
    private ThroughputEntry throughputTest;
    private List<ThroughputEntry> tpListTest;
    private double tpTest;
    private BigDecimal tp;
    private Instant now;
    private int addDay;

    // Setup the test environment
    @Before
    public void setUp() throws Exception {
        tp = null;
        testMap = new LinkedHashMap<>();
        now = Instant.now();
        tpTest = 5;
        addDay =86400;
        int addMinute =60;
        tpListTest = new ArrayList<>();

        for(int i=0; i < tpTest; i++){
            throughputTest = new ThroughputEntry();
            throughputTest.setThroughput(tpTest + i);
            throughputTest.setEnvironment("A3");
            throughputTest.setName("actwebui");
            throughputTest.setRetrieved(now.plusSeconds(addMinute));
            tpListTest.add(throughputTest);
            addMinute+=60;
        }
    }

    // Test the Total calculation method
    @Test
    public void TotalTest() throws Exception {
        calcs = new ThroughputJSON();

        for(ThroughputEntry testEntry: tpListTest){
            calcs.Generate(testEntry);
            calcs.Total(testEntry);
        }

        testMap = calcs.GetMap();
        for(Map.Entry<Long, BigDecimal> entry: testMap.entrySet()){
             tp = entry.getValue();
        }
        BigDecimal val = new BigDecimal("35.0");
        assertEquals(val, tp);
    }

    // Test the Min calculation method
    @Test
    public void MinTest() throws Exception {
        calcs = new ThroughputJSON();
        for(ThroughputEntry testEntry: tpListTest){
            calcs.Generate(testEntry);
            calcs.Min(testEntry);
        }

        testMap = calcs.GetMap();
        for(Map.Entry<Long, BigDecimal> entry: testMap.entrySet()){
            tp = entry.getValue();
        }
        BigDecimal val = new BigDecimal("5.0");
        assertEquals(val, tp);
    }

    // Test the Max calculation method
    @Test
    public void MaxTest() throws Exception {
        calcs = new ThroughputJSON();
        for(ThroughputEntry testEntry: tpListTest){
            calcs.Generate(testEntry);
            calcs.Max(testEntry);
        }

        testMap = calcs.GetMap();
        for(Map.Entry<Long, BigDecimal> entry: testMap.entrySet()){
            tp = entry.getValue();
        }
        BigDecimal val = new BigDecimal("9.0");
        assertEquals(val, tp);
    }

    // Test the Mean calculation method
    @Test
    public void MeanTest() throws Exception {
        throughputTest = new ThroughputEntry();
        throughputTest.setThroughput(tpTest + 1);
        throughputTest.setRetrieved(now.plusSeconds(addDay));
        tpListTest.add(throughputTest);
        calcs = new ThroughputJSON();
        for(ThroughputEntry testEntry: tpListTest){
            calcs.Generate(testEntry);
            calcs.Mean(testEntry);
        }

        testMap = calcs.GetMap();

        for(Map.Entry<Long, BigDecimal> entry: testMap.entrySet()){
            tp = entry.getValue();
        }
        BigDecimal val = new BigDecimal("7.000");
        assertEquals(val, tp);
    }
}
