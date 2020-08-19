package org.uu.nl.embedding.kale.model;

import java.util.ArrayList;

import org.uu.nl.embedding.kale.struct.Triple;
import org.uu.nl.embedding.kale.struct.TripleSet;
import org.uu.nl.embedding.kale.struct.TripleRule;
import org.uu.nl.embedding.kale.struct.KaleMatrix;


/**
 * Class imported from iieir-km / KALE on GitHub
 * (https://github.com/iieir-km/KALE/tree/817474edb0da54a76b562bed2328e96284557b87)
 *
 */
public class StochasticUpdater {
	public ArrayList<Triple> lstPosTriples;
	public ArrayList<Triple> lstHeadNegTriples;
	public ArrayList<Triple> lstTailNegTriples;
	public ArrayList<TripleRule> lstRules;
//	public ArrayList<Rule> lstFstRelNegRules;
	public ArrayList<TripleRule> lstSndRelNegRules;

	public KaleMatrix MatrixE;
	public KaleMatrix MatrixR;
	public KaleMatrix MatrixEGradient;
	public KaleMatrix MatrixRGradient;
	public double dGammaE;
	public double dGammaR;
	public double dDelta;
	public double m_Weight;
	boolean isGlove;
	
	public StochasticUpdater(
			ArrayList<Triple> inLstPosTriples,
			ArrayList<Triple> inLstHeadNegTriples,
			ArrayList<Triple> inLstTailNegTriples,
			ArrayList<TripleRule> inlstRules,
			ArrayList<TripleRule> inlstSndRelNegRules,
			KaleMatrix inMatrixE, 
			KaleMatrix inMatrixR,
			KaleMatrix inMatrixEGradient, 
			KaleMatrix inMatrixRGradient,
			double inGammaE,
			double inGammaR,
			double inDelta,
			double in_m_Weight,
			final boolean isGlove) {
		lstPosTriples = inLstPosTriples;
		lstHeadNegTriples = inLstHeadNegTriples;
		lstTailNegTriples = inLstTailNegTriples;
		lstRules = inlstRules;
		lstSndRelNegRules = inlstSndRelNegRules;
		MatrixE = inMatrixE;
		MatrixR = inMatrixR;
		MatrixEGradient = inMatrixEGradient;
		MatrixRGradient = inMatrixRGradient;
		m_Weight = in_m_Weight;
		dGammaE = inGammaE;
		dGammaR = inGammaR;
		dDelta = inDelta;
		this.isGlove = isGlove;
	}
	
	/**
	 * 
	 * @throws Exception
	 * @author Euan Westenbroek
	 */
	public void stochasticIteration() throws Exception {
		if (this.isGlove) stochasticIterationGlove();
		else stochasticIterationDefault();
	}
	
	/**
	 * 
	 * @throws Exception
	 * @author Euan Westenbroek, based on iieir-km's method.
	 */
	public void stochasticIterationGlove() throws Exception {
		this.MatrixEGradient.setToValue(0.0);
		this.MatrixRGradient.setToValue(0.0);
		
		// Calculate gradients for triples as well as rules.

		// Loop through positive triples and calculate
		// gradients.
		for (int iID = 0; iID < this.lstPosTriples.size(); iID++) {
			Triple PosTriple = this.lstPosTriples.get(iID);
			Triple HeadNegTriple = this.lstHeadNegTriples.get(iID);
			Triple TailNegTriple = this.lstTailNegTriples.get(iID);
			
			// Calculate gradient for head altered triple.
			TripleGradient headGradient = new TripleGradient(
					PosTriple,
					HeadNegTriple,
					this.MatrixE,
					this.MatrixR,
					this.MatrixEGradient,
					this.MatrixRGradient,
					this.dDelta,
					this.isGlove);
			headGradient.calculateGradientGlove();

			// Calculate gradient for tail altered triple.
			TripleGradient tailGradient = new TripleGradient(
					PosTriple,
					TailNegTriple,
					this.MatrixE,
					this.MatrixR,
					this.MatrixEGradient,
					this.MatrixRGradient,
					this.dDelta,
					this.isGlove);
			tailGradient.calculateGradientGlove();
		}

		// Calculate gradient for altered rule.
		for (int iID = 0; iID < this.lstRules.size(); iID++) {
			TripleRule rule = this.lstRules.get(iID);
			TripleRule sndRelNegrule = this.lstSndRelNegRules.get(iID);
			
			RuleGradient sndRuleGradient = new RuleGradient(
					rule,
					sndRelNegrule,
					this.MatrixE,
					this.MatrixR,
					this.MatrixEGradient,
					this.MatrixRGradient,
					this.dDelta,
					this.isGlove);
			sndRuleGradient.calculateGradientGlove(this.m_Weight);	
		}
		
		this.MatrixEGradient.rescaleByRow();
		this.MatrixRGradient.rescaleByRow();
		
		// Loop through entity-gradient matrix and
		// update entity matrix.
		for (int i = 0; i < this.MatrixE.rows(); i++) {
			for (int j = 0; j < this.MatrixE.columns(); j++) {
				
				// Get current gradient.
				double dValue = this.MatrixEGradient.get(i, j);
				this.MatrixEGradient.accumulatedByGrad(i, j);
				// Calculate learned rate and add 1e-8 to prevent division by zero.
				double dLearnRate = Math.sqrt(this.MatrixEGradient.getSum(i, j)) + 1e-8;
				double dUpdatedValue = (-1.0 * this.dGammaE * dValue / dLearnRate);
				this.MatrixE.add(i, j, dUpdatedValue);
			}
		}
		// Loop through relation-gradient matrix and
		// update relation matrix.
		for (int i = 0; i < this.MatrixR.rows(); i++) {
			for (int j = 0; j < this.MatrixR.columns(); j++) {

				// Get current gradient.
				double dValue = this.MatrixRGradient.get(i, j);
				this.MatrixRGradient.accumulatedByGrad(i, j);
				// Calculate learned rate and add 1e-8 to prevent division by zero.
				double dLearnRate = Math.sqrt(this.MatrixRGradient.getSum(i, j)) + 1e-8;
				double dUpdatedValue = (-1.0 * this.dGammaR * dValue / dLearnRate);
				this.MatrixR.add(i, j, dUpdatedValue);
			}
		}
		this.MatrixE.normalizeByRow();
		this.MatrixR.normalizeByRow();
	}
	
	public void stochasticIterationDefault() throws Exception {
		MatrixEGradient.setToValue(0.0);
		MatrixRGradient.setToValue(0.0);


		for (int iID = 0; iID < lstPosTriples.size(); iID++) {
			Triple PosTriple = lstPosTriples.get(iID);
			Triple HeadNegTriple = lstHeadNegTriples.get(iID);
			Triple TailNegTriple = lstTailNegTriples.get(iID);
			
			TripleGradient headGradient = new TripleGradient(
					PosTriple,
					HeadNegTriple,
					MatrixE,
					MatrixR,
					MatrixEGradient,
					MatrixRGradient,
					dDelta,
					this.isGlove);
			headGradient.calculateGradient();

			TripleGradient tailGradient = new TripleGradient(
					PosTriple,
					TailNegTriple,
					MatrixE,
					MatrixR,
					MatrixEGradient,
					MatrixRGradient,
					dDelta,
					this.isGlove);
			tailGradient.calculateGradient();
		}

		for (int iID = 0; iID < lstRules.size(); iID++) {
			TripleRule rule = lstRules.get(iID);
			TripleRule sndRelNegrule = lstSndRelNegRules.get(iID);
			
			RuleGradient tailruleGradient = new RuleGradient(
					rule,
					sndRelNegrule,
					MatrixE,
					MatrixR,
					MatrixEGradient,
					MatrixRGradient,
					dDelta,
					this.isGlove);
			tailruleGradient.calculateGradient(m_Weight);	
		}
		
		MatrixEGradient.rescaleByRow();
		MatrixRGradient.rescaleByRow();
		
		for (int i = 0; i < MatrixE.rows(); i++) {
			for (int j = 0; j < MatrixE.columns(); j++) {
				double dValue = MatrixEGradient.get(i, j);
				MatrixEGradient.accumulatedByGrad(i, j);
				double dLrate = Math.sqrt(MatrixEGradient.getSum(i, j)) + 1e-8;
				MatrixE.add(i, j, -1.0 * dGammaE * dValue / dLrate);
			}
		}
		for (int i = 0; i < MatrixR.rows(); i++) {
			for (int j = 0; j < MatrixR.columns(); j++) {
				double dValue = MatrixRGradient.get(i, j);
				MatrixRGradient.accumulatedByGrad(i, j);
				double dLrate = Math.sqrt(MatrixRGradient.getSum(i, j)) + 1e-8;
				MatrixR.add(i, j, -1.0 * dGammaR * dValue / dLrate);
			}
		}
		MatrixE.normalizeByRow();
		MatrixR.normalizeByRow();
	}
}