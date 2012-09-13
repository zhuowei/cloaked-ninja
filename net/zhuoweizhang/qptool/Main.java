package net.zhuoweizhang.qptool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.List;

/** A tool for producing QuickPatches from diffs. Based on Snowbound's PatchTool. */

public class Main {
	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("usage: tool diff original modified");
			return;
		}
		if(args[0].equals("diff")){
			try {
				diff(args[1], args[2]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
	}

	public static byte[] readPatch(int index, String[] patches)
			throws IOException {
		File patch = new File(patches[index]);
		byte[] ret = new byte[(int) patch.length()];
		InputStream is = new FileInputStream(patches[index]);
		is.read(ret, 0, ret.length);
		is.close();
		return ret;
	}
	
	public static byte[] readPatch(String patch)
			throws IOException {
		File patchf = new File(patch);
		byte[] ret = new byte[(int) patchf.length()];
		InputStream is = new FileInputStream(patch);
		is.read(ret, 0, ret.length);
		is.close();
		return ret;
	}
	public static final byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	public static void sendViaADB(String patch) {
		try {
			String line;
			Process p = Runtime.getRuntime().exec("adb push " + patch + " /mnt/sdcard");
			BufferedReader bri = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader bre = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			while ((line = bri.readLine()) != null) {
				System.out.println(line);
			}
			bri.close();
			while ((line = bre.readLine()) != null) {
				System.out.println(line);
			}
			bre.close();
			p.waitFor();
			System.out.println("Done.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void diff(String oldf, String newf) throws IOException {
		byte[] oldData = readPatch(oldf);
		byte[] newData = readPatch(newf);
		
		if(oldData.length != newData.length){
			System.err.println("Error: The new file's length does not match the old file's length. Aborting...");
			return;
		}
		
		byte[][] patchData;
		
		int numPatches = 0;

		List<PatchSegment> segments = new ArrayList<PatchSegment>();

		for(int i = 0; i < oldData.length; i++){
			if(oldData[i] != newData[i]){
				numPatches++;
				int offset = i;
				List<Byte> originalValue = new ArrayList<Byte>();
				List<Byte> newValue = new ArrayList<Byte>();
				while(oldData[i] != newData[i]){
					originalValue.add(oldData[i]);
					newValue.add(newData[i]);
					i++;
				}
				PatchSegment segment = new PatchSegment();
				segment.originalValue = originalValue.toArray(new Byte[0]);
				segment.newValue = newValue.toArray(new Byte[0]);
				segment.offset = offset;
				segments.add(segment);
			}
		}
		System.err.println("Number of Patches: " + numPatches);
		String patchOut = writePatches(segments);
		System.out.println(patchOut);
	}

	public static String writePatches(List<PatchSegment> segments) {
		StringBuilder builder = new StringBuilder();
		builder.append("    \"initial\": {\n");
		for (int i = 0; i < segments.size(); i++) {
			PatchSegment s = segments.get(i);
			builder.append("        \"" + Integer.toString(s.offset, 16) + "\": ");
			writeByteArray(builder, s.originalValue);
			if (i < segments.size() - 1) builder.append(",");
			builder.append("\n");
		}
		builder.append( "    },\n" + 
				"    \"options\": {\n" + 
				"        \"Applied\": {\n");
		for (int i = 0; i < segments.size(); i++) {
			PatchSegment s = segments.get(i);
			builder.append("            \"" + Integer.toString(s.offset, 16) + "\": ");
			writeByteArray(builder, s.newValue);
			if (i < segments.size() - 1) builder.append(",");
			builder.append("\n");
		}
		builder.append("        }\n    }");
		return builder.toString();
	}

	public static void writeByteArray(StringBuilder builder, Byte[] val) {
		builder.append("[");
		for (int i = 0; i < val.length; i++) {
			int byteVal = val[i].intValue();
			if (byteVal < 0) {
				byteVal = 256 + byteVal;
			}
			builder.append('"');
			builder.append(Integer.toString(byteVal, 16));
			builder.append('"');
			if (i != val.length - 1) {
				builder.append(", ");
			}
		}
		builder.append("]");
	}
			

	public static class PatchSegment {
		public Byte[] newValue;
		public Byte[] originalValue;
		public int offset;
	}

}
