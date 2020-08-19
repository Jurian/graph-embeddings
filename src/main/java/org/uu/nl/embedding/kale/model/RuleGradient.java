package org.uu.nl.embedding.kale.model;

import org.uu.nl.embedding.kale.struct.KaleMatrix;
import org.uu.nl.embedding.kale.struct.TripleRule;


/**
 * Class imported from iieir-km / KALE on GitHub
 * (https://github.com/iieir-km/KALE/tree/817474edb0da54a76b562bed2328e96284557b87)
 *
 */
public class RuleGradient {
	public TripleRule Rule;
	public TripleRule NegRule;
	public KaleMatrix MatrixE;
	public KaleMatrix MatrixR;
	public KaleMatrix MatrixEGradient;
	public KaleMatrix MatrixRGradient;
	double dDelta;
	double dFstPi;
	double dSndPi;
	double dTrdPi;
	double dNegFstPi;
	double dNegSndPi;
	double dNegTrdPi;
	double costrule;
	double Negcostrule;
	boolean isGlove;
	
	public RuleGradient(
			TripleRule inRule,
			TripleRule inNegRule,
			KaleMatrix inMatrixE, 
			KaleMatrix inMatrixR,
			KaleMatrix inMatrixEGradient, 
			KaleMatrix inMatrixRGradient,
			double inDelta,
			boolean isGlove) {
		Rule = inRule;
		NegRule = inNegRule;
		MatrixE = inMatrixE;
		MatrixR = inMatrixR;
		MatrixEGradient = inMatrixEGradient;
		MatrixRGradient = inMatrixRGradient;
		dDelta = inDelta;
		this.isGlove = isGlove;
	}
	
	public void calculateGradient(double weight) throws Exception {
		if (this.isGlove) calculateGradientGlove(weight); // VERDER GAAN WAAR GEBLEVEN
		else calculateGradientDefault(weight);
	}
	
	/**
	 * 
	 * @param weight
	 * @throws Exception
	 * @author Euan Westenbroek
	 */
	public void calculateGradientGlove(final double weight) throws Exception {
		// Initialization of variables.
		int iNumberOfFactors = this.MatrixE.columns();
		int iFstHead = this.Rule.getFirstTriple().head();
		int iFstTail = this.Rule.getFirstTriple().tail();
		int iFstRelation = this.Rule.getFirstTriple().relation();
		int iSndHead = this.Rule.getSecondTriple().head();
		int iSndTail = this.Rule.getSecondTriple().tail();
		int iSndRelation = this.Rule.getSecondTriple().relation();
		
		int iNegFstHead = this.NegRule.getFirstTriple().head();
		int iNegFstTail = this.NegRule.getFirstTriple().tail();
		int iNegFstRelation = this.NegRule.getFirstTriple().relation();
		int iNegSndHead = this.NegRule.getSecondTriple().head();
		int iNegSndTail = this.NegRule.getSecondTriple().tail();
		int iNegSndRelation = this.NegRule.getSecondTriple().relation();

		/*
		 * From paper:
		 * 1 / (3d)
		 * Where d is the dimension of the embedding space.
		 */
		double dValue = 1.0 / (3.0 * Math.sqrt(iNumberOfFactors));
		double tripleSum;
		
		if (this.Rule.getThirdTriple() == null){
			// Start with positive alterations.
			this.dFstPi = 0.0;
			double dPosPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				/*
				 * From paper:
				 * ||e_i + r_k - e_j||_1
				 * 
				 * Where e_i, r_k, e_j are the GloVe vector embedding of
				 * head entity, relation, and tail entity respectively.
				 */
				tripleSum = this.MatrixE.get(iFstHead, p) + this.MatrixR.get(iFstRelation, p) - this.MatrixE.get(iFstTail, p);
				this.dFstPi -= Math.abs(tripleSum);
			}
			this.dFstPi *= dValue;
			this.dFstPi += 1.0;
			
			this.dSndPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				tripleSum = this.MatrixE.get(iSndHead, p) + this.MatrixR.get(iSndRelation, p) - this.MatrixE.get(iSndTail, p);
				this.dSndPi -= Math.abs(tripleSum);
			}
			this.dSndPi *= dValue;
			this.dSndPi += 1.0;
//----------------------------------------------------------------------------------------------------------------------------------------------------
			/*
			 * WAT GEBEURT HIER?
			 */
			// Calculate positive value I.
			dPosPi = this.dFstPi * (this.dSndPi - 1.0) + 1.0;
			
			// Repeat for negative alterations.
			this.dNegFstPi = 0.0;
			double dNegPi=0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				tripleSum = this.MatrixE.get(iNegFstHead, p) + this.MatrixR.get(iNegFstRelation, p) - this.MatrixE.get(iNegFstTail, p);
				this.dNegFstPi -= Math.abs(tripleSum);
			}
			this.dNegFstPi *= dValue;
			this.dNegFstPi += 1.0;
			
			this.dNegSndPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				tripleSum = this.MatrixE.get(iNegSndHead, p) + this.MatrixR.get(iNegSndRelation, p) - this.MatrixE.get(iNegSndTail, p);
				this.dNegSndPi -= Math.abs(tripleSum);
			}
			this.dNegSndPi *= dValue;
			this.dNegSndPi += 1.0;
//----------------------------------------------------------------------------------------------------------------------------------------------------
			// Calculate negative predicate value I.
			dNegPi = this.dNegFstPi * (this.dNegSndPi - 1.0) + 1.0;

			// If negative value is delta higher than positive value,
			// add resulting gradients to their respective matrices.
			if (this.dDelta - dPosPi + dNegPi > 0.0) {
				for (int p = 0; p < iNumberOfFactors; p++) {
					// Calculate 'sum' of triple and determine sign 
					// for absolute value.
					double dFstSgn = 0.0;
					tripleSum = this.MatrixE.get(iFstHead, p) + this.MatrixR.get(iFstRelation, p) - this.MatrixE.get(iFstTail, p);
					if (tripleSum > 0) 		dFstSgn = 1.0;
					else if (tripleSum < 0) dFstSgn = -1.0;
					// Add resulting gradients to respective matrices.
					
// WAAROM HIER dSndPi -1 EN DAN NOG EEN KEER * dValue??????????????????????????????????????????
					this.MatrixEGradient.add(iFstHead, p, (weight * (this.dSndPi-1) * dValue * dFstSgn));
					this.MatrixRGradient.add(iFstRelation, p, (weight * (this.dSndPi-1) * dValue * dFstSgn));
					this.MatrixEGradient.add(iFstTail, p, (-1.0 * weight * (this.dSndPi-1) * dValue * dFstSgn));

					// Calculate 'sum' of triple and determine sign 
					// for absolute value.
					double dSndSgn = 0.0;
					tripleSum = this.MatrixE.get(iSndHead, p) + this.MatrixR.get(iSndRelation, p) - this.MatrixE.get(iSndTail, p);
					if (tripleSum > 0) 		dSndSgn = 1.0;
					else if (tripleSum < 0) dSndSgn = -1.0;
					// Add resulting gradients to respective matrices.
					this.MatrixEGradient.add(iSndHead, p, (weight * this.dFstPi * dValue * dSndSgn));
					this.MatrixRGradient.add(iSndRelation, p, (weight * this.dFstPi * dValue * dSndSgn));
					this.MatrixEGradient.add(iSndTail, p, (-1.0 * weight * this.dFstPi * dValue * dSndSgn));

					// Calculate 'sum' of triple and determine sign 
					// for absolute value.
					double dNegFstSgn = 0.0;
					tripleSum = this.MatrixE.get(iNegFstHead, p) + this.MatrixR.get(iNegFstRelation, p) - this.MatrixE.get(iNegFstTail, p);
					if (tripleSum > 0) 		dNegFstSgn = 1.0;
					else if (tripleSum < 0) dNegFstSgn = -1.0;
					// Add resulting gradients to respective matrices.
					this.MatrixEGradient.add(iNegFstHead, p,  -1.0 * weight * (this.dNegSndPi-1) * dValue * dNegFstSgn);
					this.MatrixEGradient.add(iNegFstTail, p, weight * (this.dNegSndPi-1) * dValue * dNegFstSgn);
					this.MatrixRGradient.add(iNegFstRelation, p,  -1.0 * weight * (this.dNegSndPi-1) * dValue * dNegFstSgn);

					// Calculate 'sum' of triple and determine sign 
					// for absolute value.		
					double dNegSndSgn = 0.0;
					tripleSum = this.MatrixE.get(iNegSndHead, p) + this.MatrixR.get(iNegSndRelation, p) - this.MatrixE.get(iNegSndTail, p);
					if (tripleSum > 0) 		dNegSndSgn = 1.0;
					else if (tripleSum < 0) dNegSndSgn = -1.0;
					// Add resulting gradients to respective matrices.
					this.MatrixEGradient.add(iNegSndHead, p, (-1.0 * weight * this.dNegFstPi * dValue * dNegSndSgn));
					this.MatrixEGradient.add(iNegSndTail, p, (weight * this.dNegFstPi * dValue * dNegSndSgn));
					this.MatrixRGradient.add(iNegSndRelation, p, (-1.0 * weight * this.dNegFstPi * dValue * dNegSndSgn));

				}
			}
		} // END if (this.Rule.getThirdTriple() == null)
		else {// START else (this.Rule.getThirdTriple() != null)
			int iTrdHead = this.Rule.getThirdTriple().head();
			int iTrdTail = this.Rule.getThirdTriple().tail();
			int iTrdRelation = this.Rule.getThirdTriple().relation();
			
			int iNegTrdHead = this.NegRule.getThirdTriple().head();
			int iNegTrdTail = this.NegRule.getThirdTriple().tail();
			int iNegTrdRelation = this.NegRule.getThirdTriple().relation();
			
			// Start with positive alterations.
			// First positive triple.
			this.dFstPi = 0.0;
			double dPosPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				tripleSum = this.MatrixE.get(iFstHead, p) + this.MatrixR.get(iFstRelation, p) - this.MatrixE.get(iFstTail, p);
				this.dFstPi -= Math.abs(tripleSum);
			}
			this.dFstPi *= dValue;
			this.dFstPi += 1.0;
			
			// Second positive triple.
			this.dSndPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				tripleSum = this.MatrixE.get(iSndHead, p) + this.MatrixR.get(iSndRelation, p) - this.MatrixE.get(iSndTail, p);
				this.dSndPi -= Math.abs(tripleSum);
			}
			this.dSndPi *= dValue;
			this.dSndPi += 1.0;
			
			// Third positive triple.
			this.dTrdPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				tripleSum = this.MatrixE.get(iTrdHead, p) + this.MatrixR.get(iTrdRelation, p) - this.MatrixE.get(iTrdTail, p);
				this.dTrdPi -= Math.abs(tripleSum);
			}
			this.dTrdPi *= dValue;
			this.dTrdPi += 1.0;
			
			// Calculate positive predicate value I.
			dPosPi = (this.dFstPi * this.dSndPi) * (this.dTrdPi - 1.0 ) + 1.0;
			
			// Repeat for negative triples.
			// First negative triple.
			this.dNegFstPi = 0.0;
			double dNegPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				tripleSum = this.MatrixE.get(iNegFstHead, p) + this.MatrixR.get(iNegFstRelation, p) - this.MatrixE.get(iNegFstTail, p);
				this.dNegFstPi -= Math.abs(tripleSum);
			}
			this.dNegFstPi *= dValue;
			this.dNegFstPi += 1.0;
			
			// Second negative triple.
			this.dNegSndPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				tripleSum = this.MatrixE.get(iNegSndHead, p) + this.MatrixR.get(iNegSndRelation, p) - this.MatrixE.get(iNegSndTail, p);
				this.dNegSndPi -= Math.abs(tripleSum);
			}
			this.dNegSndPi *= dValue;
			this.dNegSndPi += 1.0;
			
			// Third negative triple.
			this.dNegTrdPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				tripleSum = this.MatrixE.get(iNegTrdHead, p) + this.MatrixR.get(iNegTrdRelation, p) - this.MatrixE.get(iNegTrdTail, p);
				this.dNegTrdPi -= Math.abs(tripleSum);
			}
			this.dNegTrdPi *= dValue;
			this.dNegTrdPi += 1.0;
			
			// Calculate negative predicate value I.
			dNegPi = (this.dNegFstPi * this.dNegSndPi) * (this.dNegTrdPi - 1.0) + 1.0;
			

			// If negative value is delta higher than positive value,
			// add resulting gradients to their respective matrices.
			if (this.dDelta - dPosPi + dNegPi > 0.0) {
				for (int p = 0; p < iNumberOfFactors; p++) {
					// Calculate 'sum' of triple and determine sign 
					// for absolute value.
					double dFstSgn = 0.0;
					tripleSum = this.MatrixE.get(iFstHead, p) + this.MatrixR.get(iFstRelation, p) - this.MatrixE.get(iFstTail, p);
					if (tripleSum > 0) 		dFstSgn = 1.0;
					else if (tripleSum < 0) dFstSgn = -1.0;
					// Add resulting gradients to respective matrices.
					this.MatrixEGradient.add(iFstHead, p, (weight * this.dSndPi * (this.dTrdPi-1) * dValue * dFstSgn));
					this.MatrixRGradient.add(iFstRelation, p, (weight * this.dSndPi * (this.dTrdPi-1) * dValue * dFstSgn));
					this.MatrixEGradient.add(iFstTail, p, (-1.0 * weight * this.dSndPi * (this.dTrdPi-1) * dValue * dFstSgn));

					// Calculate 'sum' of triple and determine sign 
					// for absolute value.
					double dSndSgn = 0.0;
					tripleSum = this.MatrixE.get(iSndHead, p) + this.MatrixR.get(iSndRelation, p) - this.MatrixE.get(iSndTail, p);
					if (tripleSum > 0) 		dSndSgn = 1.0;
					else if (tripleSum < 0) dSndSgn = -1.0;
					// Add resulting gradients to respective matrices.
					this.MatrixEGradient.add(iSndHead, p, (weight * this.dFstPi * (this.dTrdPi-1) * dValue * dSndSgn));
					this.MatrixRGradient.add(iSndRelation, p, (weight * this.dFstPi * (this.dTrdPi-1) * dValue * dSndSgn));
					this.MatrixEGradient.add(iSndTail, p, (-1.0 * weight * this.dFstPi * (this.dTrdPi-1) * dValue * dSndSgn));

					// Calculate 'sum' of triple and determine sign 
					// for absolute value.
					double dTrdSgn = 0.0;
					tripleSum = this.MatrixE.get(iTrdHead, p) + this.MatrixR.get(iTrdRelation, p) - this.MatrixE.get(iTrdTail, p);
					if (tripleSum > 0) 		dTrdSgn = 1.0;
					else if (tripleSum < 0) dTrdSgn = -1.0;
					// Add resulting gradients to respective matrices.
					this.MatrixEGradient.add(iTrdHead, p, (weight * this.dFstPi * this.dSndPi * dValue * dTrdSgn));
					this.MatrixRGradient.add(iTrdRelation, p, (weight * this.dFstPi * this.dSndPi * dValue * dTrdSgn));
					this.MatrixEGradient.add(iTrdTail, p, (-1.0 * weight * this.dFstPi * this.dSndPi * dValue * dTrdSgn));

					// Calculate 'sum' of triple and determine sign 
					// for absolute value.
					double dNegFstSgn = 0.0;
					tripleSum = this.MatrixE.get(iNegFstHead, p) + this.MatrixR.get(iNegFstRelation, p) - this.MatrixE.get(iNegFstTail, p);
					if (tripleSum > 0) 		dNegFstSgn = 1.0;
					else if (tripleSum < 0) dNegFstSgn = -1.0;
					// Add resulting gradients to respective matrices.
					this.MatrixEGradient.add(iNegFstHead, p,  (-1.0 * weight * this.dNegSndPi * ( this.dNegTrdPi - 1.0 ) * dValue * dNegFstSgn));
					this.MatrixRGradient.add(iNegFstRelation, p,  (-1.0 * weight * this.dNegSndPi * ( this.dNegTrdPi - 1.0 ) * dValue * dNegFstSgn));
					this.MatrixEGradient.add(iNegFstTail, p, (weight * this.dNegSndPi * ( this.dNegTrdPi - 1.0 ) * dValue * dNegFstSgn));

					// Calculate 'sum' of triple and determine sign 
					// for absolute value.	
					double dNegSndSgn = 0.0;
					tripleSum = this.MatrixE.get(iNegSndHead, p) + this.MatrixR.get(iNegSndRelation, p) - this.MatrixE.get(iNegSndTail, p);
					if (tripleSum > 0) 		dNegSndSgn = 1.0;
					else if (tripleSum < 0) dNegSndSgn = -1.0;
					// Add resulting gradients to respective matrices.
					this.MatrixEGradient.add(iNegSndHead, p, (-1.0 * weight * this.dNegFstPi * ( this.dNegTrdPi - 1.0 ) * dValue * dNegSndSgn));
					this.MatrixRGradient.add(iNegSndRelation, p, (-1.0 * weight * this.dNegFstPi * ( this.dNegTrdPi - 1.0 ) * dValue * dNegSndSgn));
					this.MatrixEGradient.add(iNegSndTail, p, (weight * this.dNegFstPi * ( this.dNegTrdPi - 1.0 ) * dValue * dNegSndSgn));

					// Calculate 'sum' of triple and determine sign 
					// for absolute value.
					double dNegTrdSgn = 0.0;
					tripleSum = this.MatrixE.get(iNegTrdHead, p) + this.MatrixR.get(iNegTrdRelation, p) - this.MatrixE.get(iNegTrdTail, p);
					if (tripleSum > 0) 		dNegTrdSgn = 1.0;
					else if (tripleSum < 0) dNegTrdSgn = -1.0;
					// Add resulting gradients to respective matrices.
					this.MatrixEGradient.add(iNegTrdHead, p, (-1.0 * weight * this.dNegFstPi * this.dNegSndPi * dValue * dNegTrdSgn));
					this.MatrixRGradient.add(iNegTrdRelation, p, (-1.0 * weight * this.dNegFstPi * this.dNegSndPi * dValue * dNegTrdSgn));
					this.MatrixEGradient.add(iNegTrdTail, p, (weight * this.dNegFstPi * this.dNegSndPi * dValue * dNegTrdSgn));
				}
			}
			
		}// END else (this.Rule.getThirdTriple() != null)
	}
	
	
	public void calculateGradientDefault(double weight) throws Exception {
		int iNumberOfFactors = MatrixE.columns();
		int iFstHead = Rule.getFirstTriple().head();
		int iFstTail = Rule.getFirstTriple().tail();
		int iFstRelation = Rule.getFirstTriple().relation();
		int iSndHead = Rule.getSecondTriple().head();
		int iSndTail = Rule.getSecondTriple().tail();
		int iSndRelation = Rule.getSecondTriple().relation();
		
		int iNegFstHead = NegRule.getFirstTriple().head();
		int iNegFstTail = NegRule.getFirstTriple().tail();
		int iNegFstRelation = NegRule.getFirstTriple().relation();
		int iNegSndHead = NegRule.getSecondTriple().head();
		int iNegSndTail = NegRule.getSecondTriple().tail();
		int iNegSndRelation = NegRule.getSecondTriple().relation();

		
		double dValue = 1.0 / (3.0 * Math.sqrt(iNumberOfFactors));
		
		if(Rule.getThirdTriple()==null){
			dFstPi = 0.0;
			double dPosPi =0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dFstPi -= Math.abs(MatrixE.get(iFstHead, p) + MatrixR.get(iFstRelation, p) - MatrixE.get(iFstTail, p));
			}
			dFstPi *= dValue;
			dFstPi += 1.0;
			
			dSndPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dSndPi -= Math.abs(MatrixE.get(iSndHead, p) + MatrixR.get(iSndRelation, p) - MatrixE.get(iSndTail, p));
			}
			dSndPi *= dValue;
			dSndPi += 1.0;
			dPosPi = dFstPi *(dSndPi- 1.0 ) + 1.0;
			
			dNegFstPi = 0.0;
			double dNegPi=0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dNegFstPi -= Math.abs(MatrixE.get(iNegFstHead, p) + MatrixR.get(iNegFstRelation, p) - MatrixE.get(iNegFstTail, p));
			}
			dNegFstPi *= dValue;
			dNegFstPi += 1.0;
			
			dNegSndPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dNegSndPi -= Math.abs(MatrixE.get(iNegSndHead, p) + MatrixR.get(iNegSndRelation, p) - MatrixE.get(iNegSndTail, p));
			}
			dNegSndPi *= dValue;
			dNegSndPi += 1.0;
			dNegPi = dNegFstPi *(dNegSndPi- 1.0 ) + 1.0;

			if (dDelta - dPosPi + dNegPi > 0.0) {
				for (int p = 0; p < iNumberOfFactors; p++) {
					double dFstSgn = 0.0;
					if (MatrixE.get(iFstHead, p) + MatrixR.get(iFstRelation, p) - MatrixE.get(iFstTail, p) > 0) {
						dFstSgn = 1.0;
					} else if (MatrixE.get(iFstHead, p) + MatrixR.get(iFstRelation, p) - MatrixE.get(iFstTail, p) < 0) {
						dFstSgn = -1.0;
					}
					MatrixEGradient.add(iFstHead, p, weight * (dSndPi-1) * dValue * dFstSgn);
					MatrixEGradient.add(iFstTail, p, -1.0 * weight * (dSndPi-1) * dValue * dFstSgn);
					MatrixRGradient.add(iFstRelation, p, weight * (dSndPi-1) * dValue * dFstSgn);
					
					double dSndSgn = 0.0;
					if (MatrixE.get(iSndHead, p) + MatrixR.get(iSndRelation, p) - MatrixE.get(iSndTail, p) > 0) {
						dSndSgn = 1.0;
					} else if (MatrixE.get(iSndHead, p) + MatrixR.get(iSndRelation, p) - MatrixE.get(iSndTail, p) < 0) {
						dSndSgn = -1.0;
					}
					MatrixEGradient.add(iSndHead, p, weight * dFstPi * dValue * dSndSgn);
					MatrixEGradient.add(iSndTail, p, -1.0 * weight * dFstPi * dValue * dSndSgn);
					MatrixRGradient.add(iSndRelation, p, weight * dFstPi * dValue * dSndSgn);
					
					double dNegFstSgn = 0.0;
					if (MatrixE.get(iNegFstHead, p) + MatrixR.get(iNegFstRelation, p) - MatrixE.get(iNegFstTail, p) > 0) {
						dNegFstSgn = 1.0;
					} else if (MatrixE.get(iNegFstHead, p) + MatrixR.get(iNegFstRelation, p) - MatrixE.get(iNegFstTail, p) < 0) {
						dNegFstSgn = -1.0;
					}
					MatrixEGradient.add(iNegFstHead, p,  -1.0 * weight * (dNegSndPi-1) * dValue * dNegFstSgn);
					MatrixEGradient.add(iNegFstTail, p, weight * (dNegSndPi-1) * dValue * dNegFstSgn);
					MatrixRGradient.add(iNegFstRelation, p,  -1.0 * weight * (dNegSndPi-1) * dValue * dNegFstSgn);
											
					double dNegSndSgn = 0.0;
					if (MatrixE.get(iNegSndHead, p) + MatrixR.get(iNegSndRelation, p) - MatrixE.get(iNegSndTail, p) > 0) {
						dNegSndSgn = 1.0;
					} else if (MatrixE.get(iNegSndHead, p) + MatrixR.get(iNegSndRelation, p) - MatrixE.get(iNegSndTail, p) < 0) {
						dNegSndSgn = -1.0;
					}
					MatrixEGradient.add(iNegSndHead, p, -1.0 * weight * dNegFstPi * dValue * dNegSndSgn);
					MatrixEGradient.add(iNegSndTail, p, weight * dNegFstPi * dValue * dNegSndSgn);
					MatrixRGradient.add(iNegSndRelation, p, -1.0 * weight * dNegFstPi * dValue * dNegSndSgn);

				}
				
			}
		}
//		long rule
		else{
//			System.out.println("#######################long rule");
			int iTrdHead = Rule.getThirdTriple().head();
			int iTrdTail = Rule.getThirdTriple().tail();
			int iTrdRelation = Rule.getThirdTriple().relation();
			
			int iNegTrdHead = NegRule.getThirdTriple().head();
			int iNegTrdTail = NegRule.getThirdTriple().tail();
			int iNegTrdRelation = NegRule.getThirdTriple().relation();
			
			dFstPi = 0.0;
			double dPosPi =0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dFstPi -= Math.abs(MatrixE.get(iFstHead, p) + MatrixR.get(iFstRelation, p) - MatrixE.get(iFstTail, p));
			}
			dFstPi *= dValue;
			dFstPi += 1.0;
			
			dSndPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dSndPi -= Math.abs(MatrixE.get(iSndHead, p) + MatrixR.get(iSndRelation, p) - MatrixE.get(iSndTail, p));
			}
			dSndPi *= dValue;
			dSndPi += 1.0;
			
			
			dTrdPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dTrdPi -= Math.abs(MatrixE.get(iTrdHead, p) + MatrixR.get(iTrdRelation, p) - MatrixE.get(iTrdTail, p));
			}
			dTrdPi *= dValue;
			dTrdPi += 1.0;
			dPosPi = (dFstPi * dSndPi) * (dTrdPi - 1.0 ) + 1.0;
			
			dNegFstPi = 0.0;
			double dNegPi=0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dNegFstPi -= Math.abs(MatrixE.get(iNegFstHead, p) + MatrixR.get(iNegFstRelation, p) - MatrixE.get(iNegFstTail, p));
			}
			dNegFstPi *= dValue;
			dNegFstPi += 1.0;
			
			dNegSndPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dNegSndPi -= Math.abs(MatrixE.get(iNegSndHead, p) + MatrixR.get(iNegSndRelation, p) - MatrixE.get(iNegSndTail, p));
			}
			dNegSndPi *= dValue;
			dNegSndPi += 1.0;
			
			dNegTrdPi = 0.0;
			for (int p = 0; p < iNumberOfFactors; p++) {
				dNegTrdPi -= Math.abs(MatrixE.get(iNegTrdHead, p) + MatrixR.get(iNegTrdRelation, p) - MatrixE.get(iNegTrdTail, p));
			}
			dNegTrdPi *= dValue;
			dNegTrdPi += 1.0;
			
			dNegPi = (dNegFstPi * dNegSndPi) * ( dNegTrdPi - 1.0 ) + 1.0;
			
			if (dDelta - dPosPi + dNegPi > 0.0) {;
				for (int p = 0; p < iNumberOfFactors; p++) {
					double dFstSgn = 0.0;
					if (MatrixE.get(iFstHead, p) + MatrixR.get(iFstRelation, p) - MatrixE.get(iFstTail, p) > 0) {
						dFstSgn = 1.0;
					} else if (MatrixE.get(iFstHead, p) + MatrixR.get(iFstRelation, p) - MatrixE.get(iFstTail, p) < 0) {
						dFstSgn = -1.0;
					}
					MatrixEGradient.add(iFstHead, p, weight * dSndPi * (dTrdPi-1) * dValue * dFstSgn);
					MatrixEGradient.add(iFstTail, p, -1.0 * weight * dSndPi * (dTrdPi-1) * dValue * dFstSgn);
					MatrixRGradient.add(iFstRelation, p, weight * dSndPi * (dTrdPi-1) * dValue * dFstSgn);
					
					double dSndSgn = 0.0;
					if (MatrixE.get(iSndHead, p) + MatrixR.get(iSndRelation, p) - MatrixE.get(iSndTail, p) > 0) {
						dSndSgn = 1.0;
					} else if (MatrixE.get(iSndHead, p) + MatrixR.get(iSndRelation, p) - MatrixE.get(iSndTail, p) < 0) {
						dSndSgn = -1.0;
					}
					MatrixEGradient.add(iSndHead, p, weight * dFstPi * (dTrdPi-1) * dValue * dSndSgn);
					MatrixEGradient.add(iSndTail, p, -1.0 * weight * dFstPi * (dTrdPi-1) * dValue * dSndSgn);
					MatrixRGradient.add(iSndRelation, p, weight * dFstPi * (dTrdPi-1) * dValue * dSndSgn);
					
					double dTrdSgn = 0.0;
					if (MatrixE.get(iTrdHead, p) + MatrixR.get(iTrdRelation, p) - MatrixE.get(iTrdTail, p) > 0) {
						dTrdSgn = 1.0;
					} else if (MatrixE.get(iTrdHead, p) + MatrixR.get(iTrdRelation, p) - MatrixE.get(iTrdTail, p) < 0) {
						dTrdSgn = -1.0;
					}
					MatrixEGradient.add(iTrdHead, p, weight * dFstPi * dSndPi * dValue * dTrdSgn);
					MatrixEGradient.add(iTrdTail, p, -1.0 * weight * dFstPi * dSndPi * dValue * dTrdSgn);
					MatrixRGradient.add(iTrdRelation, p, weight * dFstPi * dSndPi * dValue * dTrdSgn);
					
					double dNegFstSgn = 0.0;
					if (MatrixE.get(iNegFstHead, p) + MatrixR.get(iNegFstRelation, p) - MatrixE.get(iNegFstTail, p) > 0) {
						dNegFstSgn = 1.0;
					} else if (MatrixE.get(iNegFstHead, p) + MatrixR.get(iNegFstRelation, p) - MatrixE.get(iNegFstTail, p) < 0) {
						dNegFstSgn = -1.0;
					}
					MatrixEGradient.add(iNegFstHead, p,  -1.0 * weight * dNegSndPi * ( dNegTrdPi - 1.0 ) * dValue * dNegFstSgn);
					MatrixEGradient.add(iNegFstTail, p, weight * dNegSndPi * ( dNegTrdPi - 1.0 ) * dValue * dNegFstSgn);
					MatrixRGradient.add(iNegFstRelation, p,  -1.0 * weight * dNegSndPi * ( dNegTrdPi - 1.0 ) * dValue * dNegFstSgn);
										
					double dNegSndSgn = 0.0;
					if (MatrixE.get(iNegSndHead, p) + MatrixR.get(iNegSndRelation, p) - MatrixE.get(iNegSndTail, p) > 0) {
						dNegSndSgn = 1.0;
					} else if (MatrixE.get(iNegSndHead, p) + MatrixR.get(iNegSndRelation, p) - MatrixE.get(iNegSndTail, p) < 0) {
						dNegSndSgn = -1.0;
					}
					MatrixEGradient.add(iNegSndHead, p, -1.0 * weight * dNegFstPi * ( dNegTrdPi - 1.0 ) * dValue * dNegSndSgn);
					MatrixEGradient.add(iNegSndTail, p, weight * dNegFstPi * ( dNegTrdPi - 1.0 ) * dValue * dNegSndSgn);
					MatrixRGradient.add(iNegSndRelation, p, -1.0 * weight * dNegFstPi * ( dNegTrdPi - 1.0 ) * dValue * dNegSndSgn);

					double dNegTrdSgn = 0.0;
					if (MatrixE.get(iNegTrdHead, p) + MatrixR.get(iNegTrdRelation, p) - MatrixE.get(iNegTrdTail, p) > 0) {
						dNegTrdSgn = 1.0;
					} else if (MatrixE.get(iNegTrdHead, p) + MatrixR.get(iNegTrdRelation, p) - MatrixE.get(iNegTrdTail, p) < 0) {
						dNegTrdSgn = -1.0;
					}
					MatrixEGradient.add(iNegTrdHead, p, -1.0 * weight * dNegFstPi * dNegSndPi * dValue * dNegTrdSgn);
					MatrixEGradient.add(iNegTrdTail, p, weight * dNegFstPi * dNegSndPi * dValue * dNegTrdSgn);
					MatrixRGradient.add(iNegTrdRelation, p, -1.0 * weight * dNegFstPi * dNegSndPi * dValue * dNegTrdSgn);

				}
				
			}
		}		
	}	
}