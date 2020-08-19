package org.uu.nl.embedding.kale.model;

import org.uu.nl.embedding.kale.struct.Triple;
import org.uu.nl.embedding.kale.struct.KaleMatrix;


/**
 * Class imported from iieir-km / KALE on GitHub
 * (https://github.com/iieir-km/KALE/tree/817474edb0da54a76b562bed2328e96284557b87)
 *
 */
public class TripleGradient {
	public Triple PosTriple;
	public Triple NegTriple;
	public KaleMatrix MatrixE;
	public KaleMatrix MatrixR;
	public KaleMatrix MatrixEGradient;
	public KaleMatrix MatrixRGradient;
	public double dDelta;
	double dPosPi;
	double dNegPi;
	boolean isGlove;
	
	public TripleGradient(
			Triple inPosTriple,
			Triple inNegTriple,
			KaleMatrix inMatrixE, 
			KaleMatrix inMatrixR,
			KaleMatrix inMatrixEGradient, 
			KaleMatrix inMatrixRGradient,
			double inDelta,
			final boolean isGlove) {
		PosTriple = inPosTriple;
		NegTriple = inNegTriple;
		MatrixE = inMatrixE;
		MatrixR = inMatrixR;
		MatrixEGradient = inMatrixEGradient;
		MatrixRGradient = inMatrixRGradient;
		dDelta = inDelta;
		this.isGlove = isGlove;
	}
	
	/**
	 * 
	 * @param weight
	 * @throws Exception
	 * @author Euan Westenbroek
	 */
	public void calculateGradient() throws Exception {
		if (this.isGlove) calculateGradientGlove(); // VERDER GAAN WAAR GEBLEVEN
		else calculateGradientDefault();
	}
	
	public void calculateGradientGlove() throws Exception {
		int iNumberOfFactors = this.MatrixE.columns();
		int iPosHead = this.PosTriple.head();
		int iPosTail = this.PosTriple.tail();
		int iPosRelation = this.PosTriple.relation();
		int iNegHead = this.NegTriple.head();
		int iNegTail = this.NegTriple.tail();
		int iNegRelation = this.NegTriple.relation();
		
		/*
		 * From paper:
		 * 1 / (3d)
		 * Where d is the dimension of the embedding space.
		 */
		double dValue = 1.0 / (3.0 * Math.sqrt(iNumberOfFactors));
		double tripleSum;
		
		this.dPosPi = 0.0;
		for (int p = 0; p < iNumberOfFactors; p++) {
			/*
			 * From paper:
			 * ||e_i + r_k - e_j||_1
			 * 
			 * Where e_i, r_k, e_j are the GloVe vector embedding of
			 * head entity, relation, and tail entity respectively.
			 */
			tripleSum = this.MatrixE.get(iPosHead, p) + this.MatrixR.get(iPosRelation, p) - this.MatrixE.get(iPosTail, p);
			this.dPosPi -= Math.abs(tripleSum);
		}
		this.dPosPi *= dValue;
		this.dPosPi += 1.0;
		
		// Repeat for negative triple
		this.dNegPi = 0.0;
		for (int p = 0; p < iNumberOfFactors; p++) {
			tripleSum = this.MatrixE.get(iNegHead, p) + this.MatrixR.get(iNegRelation, p) - this.MatrixE.get(iNegTail, p);
			this.dNegPi -= Math.abs(tripleSum);
		}
		this.dNegPi *= dValue;
		this.dNegPi += 1.0;
         
		// Update gradient if negative value is delta higher than
		// positive value.
		if (this.dDelta - this.dPosPi + this.dNegPi > 0.0) {
			for (int p = 0; p < iNumberOfFactors; p++) {
				// Update gradient based on positive triple
				double dPosSgn = 0.0;
				tripleSum = this.MatrixE.get(iPosHead, p) + this.MatrixR.get(iPosRelation, p) - this.MatrixE.get(iPosTail, p);
				if (tripleSum > 0)  	dPosSgn = 1.0;
				else if (tripleSum < 0) dPosSgn = -1.0;
				
				this.MatrixEGradient.add(iPosHead, p, (dPosSgn * dValue));
				this.MatrixRGradient.add(iPosRelation, p, (dPosSgn * dValue));
				this.MatrixEGradient.add(iPosTail, p, (-1.0 * dPosSgn * dValue));

				// Update gradient based on negative triple
				double dNegSgn = 0.0;
				tripleSum = this.MatrixE.get(iNegHead, p) + this.MatrixR.get(iNegRelation, p) - this.MatrixE.get(iNegTail, p);
				if (tripleSum > 0) 		dNegSgn = 1.0;
				else if (tripleSum < 0) dNegSgn = -1.0;
				
				this.MatrixEGradient.add(iNegHead, p, (-1.0 * dValue * dNegSgn));
				this.MatrixRGradient.add(iNegRelation, p, (-1.0 * dValue * dNegSgn));
				this.MatrixEGradient.add(iNegTail, p, (dValue * dNegSgn));
			}
		}
	}
	
	public void calculateGradientDefault() throws Exception {
		int iNumberOfFactors = MatrixE.columns();
		int iPosHead = PosTriple.head();
		int iPosTail = PosTriple.tail();
		int iPosRelation = PosTriple.relation();
		int iNegHead = NegTriple.head();
		int iNegTail = NegTriple.tail();
		int iNegRelation = NegTriple.relation();

		double dValue = 1.0 / (3.0 * Math.sqrt(iNumberOfFactors));
		dPosPi = 0.0;
		for (int p = 0; p < iNumberOfFactors; p++) {
			dPosPi -= Math.abs(MatrixE.get(iPosHead, p) + MatrixR.get(iPosRelation, p) - MatrixE.get(iPosTail, p));
		}
		dPosPi *= dValue;
		dPosPi += 1.0;
		
		dNegPi = 0.0;
		for (int p = 0; p < iNumberOfFactors; p++) {
			dNegPi -= Math.abs(MatrixE.get(iNegHead, p) + MatrixR.get(iNegRelation, p) - MatrixE.get(iNegTail, p));
		}
		dNegPi *= dValue;
		dNegPi += 1.0;
         
		
		if (dDelta - dPosPi + dNegPi > 0.0) {
//		if (dDeltaAdapt - dPosPi + dNegPi > 0.0) {
			for (int p = 0; p < iNumberOfFactors; p++) {
				double dPosSgn = 0.0;
				if (MatrixE.get(iPosHead, p) + MatrixR.get(iPosRelation, p) - MatrixE.get(iPosTail, p) > 0) {
					dPosSgn = 1.0;
				} else if (MatrixE.get(iPosHead, p) + MatrixR.get(iPosRelation, p) - MatrixE.get(iPosTail, p) < 0) {
					dPosSgn = -1.0;
				}
				MatrixEGradient.add(iPosHead, p, dValue * dPosSgn);
				MatrixEGradient.add(iPosTail, p, -1.0 * dValue * dPosSgn);
				MatrixRGradient.add(iPosRelation, p, dValue * dPosSgn);
//				System.out.println("true0:"+  dValue * dPosSgn);
				double dNegSgn = 0.0;
				if (MatrixE.get(iNegHead, p) + MatrixR.get(iNegRelation, p) - MatrixE.get(iNegTail, p) > 0) {
					dNegSgn = 1.0;
				} else if (MatrixE.get(iNegHead, p) + MatrixR.get(iNegRelation, p) - MatrixE.get(iNegTail, p) < 0) {
					dNegSgn = -1.0;
				}
				MatrixEGradient.add(iNegHead, p, -1.0 * dValue * dNegSgn);
				MatrixEGradient.add(iNegTail, p, dValue * dNegSgn);
				MatrixRGradient.add(iNegRelation, p, -1.0 * dValue * dNegSgn);
			}
		}
		
	}
}