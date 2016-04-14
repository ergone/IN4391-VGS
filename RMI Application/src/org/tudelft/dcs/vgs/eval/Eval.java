package org.tudelft.dcs.vgs.eval;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class Eval {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		String path = "C:\\Users\\viktor\\Downloads\\";
		String file = path + "GS=3_RM=10_PROC=85.properties";
		Properties prop = new Properties();
		prop.load(new InputStreamReader(new FileInputStream(file)));
		long min = Long.MAX_VALUE;
		long max = 0;
		double avg = 0.0;
		for (Object obj : prop.values()) {
			Integer t = Integer.valueOf((String) obj);
			if (t > max) {
				max = t;
			}
			if (t < min) {
				min = t;
			}
			avg += t;
		}
		avg = avg / prop.size();
		System.out.println(file);
		System.out.println(min);
		System.out.println(max);
		System.out.println(avg);
	}

}
