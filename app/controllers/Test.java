package controllers;

public class Test {

	public int somme(int a, int b) {
		int somme = 0;
		somme = a + b;
		return somme;

	}

	public int somme(int a, int b, boolean c) {
		int val = 0;
		if (c == true) {
			val = a + b;
		} else {
			val = a - b;
		}
		return val;
		
	}
	
	public  void main(String[] args) {
		System.out.println("la somme est" + this.somme(10, 15));

	}
}
