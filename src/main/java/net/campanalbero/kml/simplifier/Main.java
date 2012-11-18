package net.campanalbero.kml.simplifier;

public class Main {
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("2 args need");
		}
		
		try {
			KmlRewriter rewriter = new KmlRewriter(args[0], args[1], 200);
			rewriter.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}