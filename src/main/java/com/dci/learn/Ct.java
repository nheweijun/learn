package com.dci.learn;

import java.awt.Color;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;
import org.datasyslab.geospark.enums.FileDataSplitter;
import org.datasyslab.geospark.enums.GridType;
import org.datasyslab.geospark.enums.IndexType;
import org.datasyslab.geospark.spatialOperator.JoinQuery;
import org.datasyslab.geospark.spatialRDD.PointRDD;
import org.datasyslab.geospark.spatialRDD.RectangleRDD;
import org.datasyslab.geosparkviz.core.ImageGenerator;
import org.datasyslab.geosparkviz.core.RasterOverlayOperator;
import org.datasyslab.geosparkviz.extension.visualizationEffect.ChoroplethMap;
import org.datasyslab.geosparkviz.extension.visualizationEffect.ScatterPlot;
import org.datasyslab.geosparkviz.utils.ImageType;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;


@SuppressWarnings("serial")
public class Ct implements Serializable{
	
	
	/** The spark context. */
	static JavaSparkContext sparkContext;
	
	/** The prop. */
	static Properties prop;
	
	/** The input prop. */
	static InputStream inputProp;
    
    /** The Point input location. */
    static String PointInputLocation;
    
    /** The Point offset. */
    static Integer PointOffset;
    
    /** The Point splitter. */
    static FileDataSplitter PointSplitter;
    
    /** The Point num partitions. */
    static Integer PointNumPartitions;
    
    /** The Rectangle input location. */
    static String RectangleInputLocation;
    
    /** The Rectangle offset. */
    static Integer RectangleOffset;
    
    /** The Rectangle splitter. */
    static FileDataSplitter RectangleSplitter;
    
    /** The Rectangle num partitions. */
    static Integer RectangleNumPartitions;
    
    /** The Polygon input location. */
    static String PolygonInputLocation;
    
    /** The Polygon offset. */
    static Integer PolygonOffset;
    
    /** The Polygon splitter. */
    static FileDataSplitter PolygonSplitter;
    
    /** The Polygon num partitions. */
    static Integer PolygonNumPartitions;
    
    /** The Line string input location. */
    static String LineStringInputLocation;
    
    /** The Line string offset. */
    static Integer LineStringOffset;
    
    /** The Line string splitter. */
    static FileDataSplitter LineStringSplitter;
    
    /** The Line string num partitions. */
    static Integer LineStringNumPartitions;
    
    /** The US main land boundary. */
    static Envelope USMainLandBoundary;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {		
		//SparkConf sparkConf = new SparkConf().setAppName("Ct").setMaster("local[4]");
		SparkConf sparkConf = new SparkConf().setAppName("Ct").setMaster("local[4]");
		sparkContext = new JavaSparkContext(sparkConf);
		Logger.getLogger("org.apache").setLevel(Level.WARN);
		Logger.getLogger("org.datasyslab").setLevel(Level.DEBUG);
		Logger.getLogger("akka").setLevel(Level.WARN);
        prop = new Properties();      
        PointInputLocation = "hdfs://172.16.220.103:9000/user/pi/POI.csv" ;
        PointOffset = 0;
        PointSplitter = FileDataSplitter.getFileDataSplitter("csv");
        PointNumPartitions = 5;

        RectangleInputLocation ="hdfs://172.16.220.103:9000/user/pi/fishnet-517.csv";
        RectangleOffset = 0;
        RectangleSplitter = FileDataSplitter.getFileDataSplitter("csv");
        RectangleNumPartitions = 5;
        
        USMainLandBoundary = new Envelope(398194.99,436674.99,2509196.91,2545896.91);
	}

	/**
	 * Tear down.
	 *
	 * @throws Exception the exception
	 */
	@AfterClass
	public static void tearDown() throws Exception {
		sparkContext.stop();
	}

	/**
	 * Test rectangle RDD visualization.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testRectangleRDDVisualization() throws Exception  {
		
		System.out.println(PointInputLocation);
		PointRDD spatialRDD = new PointRDD(sparkContext, PointInputLocation, PointOffset, PointSplitter, false, PointNumPartitions, StorageLevel.MEMORY_ONLY());
		RectangleRDD queryRDD = new RectangleRDD(sparkContext, RectangleInputLocation, RectangleSplitter, false, RectangleNumPartitions, StorageLevel.MEMORY_ONLY());

		spatialRDD.spatialPartitioning(GridType.RTREE);
		queryRDD.spatialPartitioning(spatialRDD.grids);	
  		spatialRDD.buildIndex(IndexType.RTREE,true);
  		
  		JavaPairRDD<Polygon,Long> joinResult = JoinQuery.SpatialJoinQueryCountByKey(spatialRDD,queryRDD,true,true);//进行空间重叠搜索

   		ChoroplethMap visualizationOperator = new ChoroplethMap(1000,800,USMainLandBoundary,false);
		visualizationOperator.CustomizeColor(255, 255, 255, 255, Color.RED, true);
		visualizationOperator.Visualize(sparkContext, joinResult);
		
		ScatterPlot frontImage = new ScatterPlot(1000,800,USMainLandBoundary,false);
		frontImage.CustomizeColor(0, 0, 0, 255, Color.GREEN, true);
		frontImage.Visualize(sparkContext, queryRDD);

		RasterOverlayOperator overlayOperator = new RasterOverlayOperator(visualizationOperator.rasterImage);

		
		ImageGenerator imageGenerator = new ImageGenerator();	
		imageGenerator.SaveRasterImageAsHadoopFile(overlayOperator.backRasterImage,"hdfs://172.16.220.103:9000/user/reslut3/test", ImageType.PNG);
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Ct test = new Ct();
		test.testRectangleRDDVisualization();
		Ct.tearDown();
	}

}
