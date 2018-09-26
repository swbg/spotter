package application;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class Prop {
	
	private static final String CONFIGPATH = "config/config.properties";
	private static final String TYPEPATH = "config/type.tsv";
	
	public int defaultRows;
	public int defaultCols;
	public int defaultSize;
	
	public double fractionBrightestPixels;
	public int minBrightestPixels;
	
	public int maxLevel;
	public double[] levelSpacing;
	
	public ArrayList<String> types = new ArrayList<>();
	public ArrayList<Double[]> typeMeans = new ArrayList<>();
	
	public Prop() {
		Properties prop = new Properties();
		try {
			InputStream in = new FileInputStream(CONFIGPATH);
			prop.load(in);
		} catch (IOException e) {
			System.out.println("Error loading conifg.properties.");
			System.exit(1);
		}
		System.out.println("### Loading config.properties... ###");
		defaultRows = Integer.parseInt(prop.getProperty("defaultRows", "5"));
		System.out.println("defaultRows=" + defaultRows);
		defaultCols = Integer.parseInt(prop.getProperty("defaultCols", "16"));
		System.out.println("defaultCols=" + defaultCols);
		defaultSize = Integer.parseInt(prop.getProperty("defaultSize", "20"));
		System.out.println("defaultSize=" + defaultSize);
		fractionBrightestPixels = Double.parseDouble(prop.getProperty("fractionBrightestPixels", "0.1"));
		System.out.println("fractionBrightestPixels=" + fractionBrightestPixels);
		minBrightestPixels = Integer.parseInt(prop.getProperty("minBrightestPixels", "10"));
		System.out.println("minBrightestPixels=" + minBrightestPixels);
		maxLevel = Integer.parseInt(prop.getProperty("maxLevel", "3"));
		System.out.println("maxLevel=" + maxLevel);
		if (prop.getProperty("levelSpacing", "null").equals("default")) {
			calculateDefaultSpacing();
		} else {
			try {
				String[] s = prop.getProperty("levelSpacing", "default").split(";");
				levelSpacing = new double[maxLevel+2];
				for (int i = 0; i < maxLevel+1; i++) {
					levelSpacing[i] = Double.parseDouble(s[i]);
				}
				levelSpacing[maxLevel+1] = 1.0;
			} catch (Exception e) {
				System.out.println("Error determining custom level spacing, falling back to default.");
				calculateDefaultSpacing();
			}
		}
		System.out.println("levelSpacing=" + Arrays.toString(levelSpacing));
		
		System.out.println("### Loading type.tsv ###");
		Reader in;
		try {
			in = new FileReader(TYPEPATH);
			Iterable<CSVRecord> records = CSVFormat.TDF.parse(in);
			for (CSVRecord record : records) {
				types.add(record.get(0));
				Double[] d = new Double[record.size()-1];
				for (int i = 1; i < record.size(); i++) {
					int l = Integer.parseInt(record.get(i));
					d[i-1] = (levelSpacing[l] + levelSpacing[l+1])/2;
				}
			    typeMeans.add(d);
			}
		} catch (IOException e) {
			System.out.println("Error loading type.tsv.");
			System.exit(1);
		}
		for (int i = 0; i < types.size(); i++) {
			System.out.println(types.get(i) + ": " + Arrays.toString(typeMeans.get(i)));
		}
	}
	
	private void calculateDefaultSpacing() {
		levelSpacing = new double[maxLevel+2];
		for (int i = 0; i < maxLevel+1; i++) {
			levelSpacing[i] = 1.0*i / (maxLevel+1);
		}
		levelSpacing[maxLevel+1] = 1.0;
	}
}
