package org.uu.nl.embedding.logic.cnf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.uu.nl.embedding.lensr.DdnnfGraph;
import org.uu.nl.embedding.logic.LogicRule;
import org.uu.nl.embedding.logic.ddnnf.DdnnfLogicRule;
import org.uu.nl.embedding.logic.util.SimpleDate;
import org.uu.nl.embedding.util.ArrayUtils;

/**
 * Interface class for logic CNF formulae 
 * to be used in CNF.
 * 
 * DISCLAIMER: This class is partly based 
 * on the code by Rodion "rodde" Efremov.
 * 
 * @author Euan Westenbroek
 * @version 1.0
 * @since 07-06-2020
 */
public class CnfFormula implements CnfLogicRule {

    private final static Logger logger = Logger.getLogger(CnfFormula.class);

	private boolean assignment, hasCheckedSecondDate = false;
	private SimpleDate firstDate = null;
	private SimpleDate secondDate = null;
	private List<HashMap<String, Boolean>> trueAssignments, falseAssignments;
	
	private String name = null;
	private String cnfName;
	private String ddnnfName;

	private DdnnfGraph ddnnfGraph;
	private DdnnfLogicRule ddnnfRule;
	
	/**
	 * The list of clauses that belong to this formula.
	 */
	private final ArrayList<Clause> clauseList = new ArrayList<>();
	
    /**
     * The set of positive literals present in this clause.
     */
    private final Set<LogicLiteral> positiveLiteralSet = 
            new TreeSet<>();
    /**
     * The set of negative literals present in this clause.
     */
    private final Set<LogicLiteral> negativeLiteralSet = 
            new TreeSet<>();


    
    
    /**
     * Constructor method for this class.
     * @param cnfFormula The CnfFormula that make up this formula.
	 * @param nfRule The Normal Form formula this CNF formula
	 * is generated from.
    public CnfFormula(CnfFormula cnfFormula, NormalLogicRule nfRule) {
    	super();
    	if(cnfFormula instanceof CnfFormula) {
    		ArrayList<Clause> clauses = cnfFormula.getClauses();
        	for(Clause clause : clauses) { addClause(clause); }
        	
    		this.name = this.toString();
    		this.cnfName = this.name;
    		
    		/*
    		if(nfRule == null) { nfRule = generateNf(); }
    		this.inNf = nfRule;
    		this.inCnf = this;
    		this.inDdnnf = nfRule.getDdnnfRule(); // Checken of dit goed gaat qua compile volgorde!!!!!!
    		this.ddnnfName = nfRule.getDdnnfName();
    		
    		this.assignment = isSatisfied();
    	}
    }
    */
    
    /**
     * Constructor method for this class.
     * @param clauses The clauses that make up this formula.
	 * @param nfRule The Normal Form formula this CNF formula
	 * is generated from.
	 */
    public CnfFormula(Clause[] clauses) {
		super();
    	for(Clause clause : clauses) { addClause(clause); }
    	
		this.name = this.toString();
		this.cnfName = this.name;
		
		/*
		if(nfRule == null) { nfRule = generateNf(); }
		this.inNf = nfRule;
		this.inCnf = this;
		this.inDdnnf = nfRule.getDdnnfRule(); // Checken of dit goed gaat qua compile volgorde!!!!!!
		this.ddnnfName = nfRule.getDdnnfName();
		*/
		this.assignment = isSatisfied();
		this.trueAssignments = fetchTrueAssignments();
		this.falseAssignments = fetchFalseAssignments();
	}
    
    public CnfFormula(CnfFormula formula) {
    	super();
    	for(Clause clause : formula.getClauses()) { addClause(clause); }
    	
		this.name = this.toString();
		this.cnfName = this.name;

		this.assignment = isSatisfied();
		this.trueAssignments = fetchTrueAssignments();
		this.falseAssignments = fetchFalseAssignments();
    }
    
    public CnfFormula(CnfFormula formula, final String secondDate) {
    	super();
    	int counter;
    	Clause newClause;
    	LogicLiteral newLiteral;
    	ArrayList<Boolean> orderedNegated = new ArrayList<Boolean>();
    	ArrayList<LogicLiteral> literals = new ArrayList<LogicLiteral>();
    	
    	for(Clause clause : formula.getClauses()) {
        	counter = 0;
    		for (LogicLiteral literal : clause.getPositiveLiterals()) {
    			
				if (clause.getPositiveLiterals().contains(literal)) { orderedNegated.add(true); }
				else { orderedNegated.add(false); }
				
    			if (literal instanceof CnfDateLogic && counter < 1) {
    				if (counter == 1) { 
    					newLiteral = new CnfDateLogic(secondDate, literal.getName(), literal.getAssignment(), literal.isNegated());
    					literals.add(newLiteral);
    				}
    				else { ; }
    			}
    			else if (literal instanceof CnfDateComparer) {
    				CnfDateComparer newComparer = (CnfDateComparer) literal;
					newLiteral = new CnfDateComparer(newComparer.getDates()[0], 
												newComparer.getDates()[0], newComparer.getName(), 
												newComparer.getAssignment(), newComparer.isNegated());
					literals.add(newLiteral);
    			} else {
    				newLiteral = new LogicLiteral(literal.getName(), literal.getAssignment(), orderedNegated.get(counter));
    				literals.add(newLiteral);
    			}
    			counter++; 
    		}
    		newClause = new Clause(literals.toArray(new LogicLiteral[0]), ArrayUtils.toArray(orderedNegated, true));
    		addClause(newClause); 
    	}
    	
		this.name = this.toString();
		this.cnfName = this.name;

		this.assignment = isSatisfied();
		this.trueAssignments = fetchTrueAssignments();
		this.falseAssignments = fetchFalseAssignments();
    }

    /**
     * Constructor method for this class.
     * @param clauses The clause that makes up this formula.
	 * @param nfRule The Normal Form formula this CNF formula
	 * is generated from.
	 */
	public CnfFormula(Clause clause) {
		super();
		addClause(clause);
		
		this.name = this.toString();
		this.cnfName = this.name;
		
		/*
		if(nfRule == null) { nfRule = generateNf(); }
		this.inNf = nfRule;
		this.inCnf = this;
		this.inDdnnf = nfRule.getDdnnfRule(); // Checken of dit goed gaat qua compile volgorde!!!!!!
		this.ddnnfName = nfRule.getDdnnfName();
		*/
		this.assignment = isSatisfied();
		this.trueAssignments = fetchTrueAssignments();
		this.falseAssignments = fetchFalseAssignments();
	}
	
	/**
	 * Method to add a clause to the formula.
	 * @param clause The clause to be added.
	 */
	public void addClause(Clause clause) {
		this.clauseList.add(clause);
		
		this.positiveLiteralSet.addAll(
				clause.getPositiveLiterals());
		this.negativeLiteralSet.addAll(
				clause.getNegativeLiterals());
		
		this.assignment = isSatisfied();
	}

	/**
	 * Method to remove a clause from the formula.
	 * @param clause The clause to be removed.
	 */
	public void removeClause(String clauseName) {
		this.positiveLiteralSet.clear();
		this.negativeLiteralSet.clear();
		
		for(Clause clause : clauseList) {
			if(clause.getName() == clauseName) {
				clauseList.remove(clause);
			}
			else {
				
				this.positiveLiteralSet.addAll(
						clause.getPositiveLiterals());
				this.negativeLiteralSet.addAll(
						clause.getNegativeLiterals());
			}
		}
		this.assignment = isSatisfied();
		
	}

    /**
     * Adds a non-negated literal to the given clause.
     * 
     * @param LogicLiteral the literal to add.
     * @param targetClause The clause to which to add.
     */
    public void addPositiveLiteral(LogicLiteral literal, Clause targetClause) {
    	targetClause.addPositiveLiteral(literal);
 		this.assignment = isSatisfied();
    }

    /**
     * Adds a negated literal to the given clause.
     * 
     * @param LogicLiteral The literal to add.
     * @param targetClause The clause to which to add.
     */
    public void addNegativeLiteral(LogicLiteral literal, Clause targetClause) {
    	targetClause.addNegativeLiteral(literal);
		this.assignment = isSatisfied();
    }

    /**
     * Checks whether the input assignment satisfies this formula.
     * 
     * @return {@code true} if the assignment satisfies this clause, and 
     *         {@code false} otherwise.
     */
    public boolean isSatisfied() {
    	for(Clause clause : clauseList) {
    		if(!clause.isSatisfied()) { return false; }
    	}
    	return true;
    }
    
    /**
     * Method to merge two CNF formulae to one.
     * @param formula The other formula to merge this one with
     * @return Returns the resulting formula from this formula and
     * the given formula.
     */
    public CnfFormula mergeWith(CnfFormula formula) {
    	List<Clause> uniqueClauses = this.clauseList;
    	boolean samePos, sameNeg;
    	
    	// Loop through local (this formula) and 
    	// incoming clauses (other formula)
    	for(Clause localClause : getClauses()) {
    		for(Clause incClause : formula.getClauses()) {
    			samePos = localClause.getPositiveLiterals().equals(
    					incClause.getPositiveLiterals());
    			sameNeg = localClause.getNegativeLiterals().equals(
    					incClause.getNegativeLiterals());
    			
    			if(!(samePos && sameNeg)) {
    				uniqueClauses.add(incClause);
    			}
    			// else No need for duplicate clauses.
    		}
    	}
    	Clause[] resClauses = uniqueClauses.toArray(new Clause[0]);
    	CnfFormula resForm = new CnfFormula(resClauses);
    	//resForm.cleanClauses();
    	
    	return resForm;
    }
    
    /**
     * 
     * @return
     */
    public boolean hasDateComparer() {
    	if (this.hasCheckedSecondDate && this.secondDate != null) { return true; }
    	else if (this.hasCheckedSecondDate && this.secondDate == null) { return false; }
    	else {

	    	this.hasCheckedSecondDate = true;
	    	for (Clause clause : this.clauseList) {
	    		for (LogicLiteral literal : clause.getLiterals()) {
	        		if (literal instanceof CnfDateComparer) {
	        			CnfDateComparer comparer = (CnfDateComparer) literal;
	        			this.firstDate = comparer.getDates()[0];
	        			this.secondDate = comparer.getDates()[1];
	        			return true;
	        		}
	    	}}
	    	return false;
    	}
    }
    
    /**
     * 
     * @param pattern
     */
    public void changeSecondDate(String pattern) {
    	SimpleDate newDate = new SimpleDate(pattern);
    	CnfDateComparer newComparer, comparer;
    	
    	for (Clause clause : this.clauseList) {
    		for (LogicLiteral literal : clause.getLiterals()) {
        		if (literal instanceof CnfDateComparer) {
        			
        			comparer = (CnfDateComparer) literal;
        			newComparer = new CnfDateComparer(comparer.getDates()[0], newDate, comparer.getName(), comparer.getAssignment(), comparer.isNegated());
        			
        			if (clause.getPositiveLiterals().contains(literal)) {
            			clause.removeLiteral(literal);
            			clause.addPositiveLiteral(newComparer); } 
        			else { 
            			clause.removeLiteral(literal);
            			clause.addNegativeLiteral(newComparer); }
        		}
    	}}
    }
    
    public String trueAssignmentsToString() {
    	String result = " - TRUE ASSIGNMENTS - \n";
    	List<HashMap<String, Boolean>> assignList = (ArrayList) getTrueAssignments();
    	HashMap<String, Boolean> map;
    	
    	for (int i = 0; i < assignList.size(); i++) {

    		result += "No.: " + i + " of TRUE assignment\n";
    		map = assignList.get(i);
    		for (Map.Entry<String, Boolean> entry : map.entrySet()) {
    			result += (entry.getKey().toString() + ": " + entry.getValue().toString() + "\n");
    		}
    		result += "\n\n";
    	}
    	
    	return result;
    }
    
    public List<HashMap<String, Boolean>> getTrueAssignments() {
    	return this.trueAssignments;
    }

    private List<HashMap<String, Boolean>> fetchTrueAssignments() {
    	List<HashMap<String, Boolean>> list1, list2;
    	HashMap<String, Boolean> intersectContra, intersectContraAll;
    	intersectContraAll = new HashMap<String, Boolean>();
    	
    	Clause clause1, clause2;
    	
    	// Check for contradicting literals in the different clauses.
    	for (int i = 0; i < this.clauseList.size(); i++) {

    		clause1 = this.clauseList.get(i);
	    	list1 = clause1.getTrueAssignments();
    		for (int j = i+1; j < this.clauseList.size(); j++) {
        		clause2 = this.clauseList.get(j);
    			// Skip checks if clause2 is clause1.
    			if (clause1 == clause2) { continue; }

    	    	list2 = clause2.getTrueAssignments();
    	    	
    	    	// Check if there is no intersection of literals between clauses.
    	    	intersectContra = new HashMap<String, Boolean>(list1.get(0));
    	    	intersectContra.keySet().retainAll(list2.get(0).keySet());
    	    	boolean allContra = true;
    	    	
    			if (intersectContra.size() != 0) {
    				for (Map.Entry<String, Boolean> entry : intersectContra.entrySet()) {
    					if (clause1.getPositiveNameSet().contains(entry.getKey()) && 
    							clause2.getNegativeNameSet().contains(entry.getKey())) {
    						intersectContra.put(entry.getKey(), true);
    						intersectContraAll.put(entry.getKey(), true);
    						
    					} else if (clause1.getNegativeNameSet().contains(entry.getKey()) && 
    							clause2.getPositiveNameSet().contains(entry.getKey())) {
    						intersectContra.put(entry.getKey(), true);
    						intersectContraAll.put(entry.getKey(), true);
    						
    					} else { 
    						intersectContra.put(entry.getKey(), false);
    						if (!intersectContraAll.containsKey(entry.getKey())) { intersectContraAll.put(entry.getKey(), false); }
    						allContra = false; }
    				}

        			// Check for all contradictions and if sizes are the same as any of the literal sets
        			if (allContra && (intersectContra.size() == list1.get(0).size() || intersectContra.size() == list2.get(0).size())) {
        				return new ArrayList<HashMap<String, Boolean>>(); 
        			}
    			}
    		}
    	}
    	
    	/*
    	 * Add all literals unless they are an intersecting literal.
    	 */
    	List<HashMap<String, Boolean>> oldList, newList, curMapList;
    	oldList = new ArrayList<HashMap<String, Boolean>>(); 
    	HashMap<String, Boolean> oldLitsMap, newLitsMap;
    	for (int i = 0; i < this.clauseList.size(); i++) {
    		
    		clause1 = this.clauseList.get(i);
    		// if it is the first clause, get the assignments and
    		// skip the rest to the next iteration.
    		if (i == 0) {
    			for (HashMap<String, Boolean> map : clause1.getTrueAssignments()) {

    				newLitsMap = new HashMap<String, Boolean>();
    				for (Map.Entry<String, Boolean> entry : map.entrySet()) {
    					
    					if (!intersectContraAll.containsKey(entry.getKey())) {
        					newLitsMap.put(entry.getKey(), entry.getValue());
    					} else {
    					}
    				}
    				oldList.add(newLitsMap);
    			}
    			// Skip over rest of loop; go to next iteration.
    			continue;
    		}
    		
			curMapList = clause1.getTrueAssignments();
			newList = new ArrayList<HashMap<String, Boolean>>();
    		
    		for (int j = 0; j < oldList.size(); j++) {
    			oldLitsMap = new HashMap<String, Boolean>(oldList.get(i));
    			
    			for (int s = 0; s < curMapList.size(); s++) {
        			newLitsMap = new HashMap<String, Boolean>(oldLitsMap);
        			for (Map.Entry<String, Boolean> entry : curMapList.get(s).entrySet()) {
        				if (!intersectContraAll.containsKey(entry.getKey())) {
            				newLitsMap.put(entry.getKey(), entry.getValue());
        				}
        			}
        			newList.add(newLitsMap);
    			}
    			// newList has all combinations of oldMaps looped over new maps
    		}
    		// Update the oldList with all expanded maps.
    		oldList = new ArrayList<HashMap<String, Boolean>>(newList);
    	}

    	/*
    	 * Add all intersecting literals and possibly their different values.
    	 */
    	newList = new ArrayList<HashMap<String, Boolean>>();
    	boolean addSecondMap;
		for (Map.Entry<String, Boolean> entry : intersectContraAll.entrySet()) {

	    	addSecondMap = false;
	    	// Add intersecting literals and their potentially first value
			for (int i = 0; i < oldList.size(); i++) {
				newLitsMap = new HashMap<String, Boolean>(oldList.get(i));;
				
				if(intersectContraAll.get(entry.getKey())) {
					newLitsMap.put(entry.getKey(), true);
					addSecondMap = true;
					
				} else { //intersecting literal != contradicting between clauses.
					for (int j = 0; j < this.clauseList.size(); j++) {
			    		
			    		if(this.clauseList.get(j).getPositiveNameSet().contains(entry.getKey())) {
			    			newLitsMap.put(entry.getKey(), true);
			    			break;
			    			
			    		} else if(this.clauseList.get(j).getNegativeNameSet().contains(entry.getKey())) {
			    			newLitsMap.put(entry.getKey(), false);
			    			break;
			    		}
					}
				}
				// Add newly generated map to the list.
				newList.add(newLitsMap);
			}
	    	
			// Redo this iteration but then for the other value.
			if(addSecondMap) { 
				for (int i = 0; i < oldList.size(); i++) {
					newLitsMap = new HashMap<String, Boolean>(oldList.get(i));
					
					if(intersectContraAll.get(entry.getKey())) {
						newLitsMap.put(entry.getKey(), false);
						
					} else { //intersecting literal != contradicting between clauses.
						for (int j = 0; j < this.clauseList.size(); j++) {
				    		
				    		if(this.clauseList.get(j).getPositiveNameSet().contains(entry.getKey())) {
				    			newLitsMap.put(entry.getKey(), true);
				    			break;
				    			
				    		} else if(this.clauseList.get(j).getNegativeNameSet().contains(entry.getKey())) {
				    			newLitsMap.put(entry.getKey(), false);
				    			break;
				    		}
						}
					}
					// Add newly generated map to the list.
					newList.add(newLitsMap);
				}
			}
    		// Update the oldList with all expanded maps.
    		oldList = new ArrayList<HashMap<String, Boolean>>(newList);
		}
		// Finally return the generated complete list of maps.
    	return oldList;
    	
    }

    
    public String falseAssignmentsToString() {
    	String result = " - FALSE ASSIGNMENTS - \n";
    	List<HashMap<String, Boolean>> assignList = (ArrayList) getFalseAssignments();
    	HashMap<String, Boolean> map;
    	
    	for (int i = 0; i < assignList.size(); i++) {

    		result += "No.: " + i + " of FALSE assignment\n";
    		map = assignList.get(i);
    		for (Map.Entry<String, Boolean> entry : map.entrySet()) {
    			result += (entry.getKey().toString() + ": " + entry.getValue().toString() + "\n");
    		}
    		result += "\n\n";
    	}
    	
    	return result;
    }
    
    public List<HashMap<String, Boolean>> getFalseAssignments() {
    	return this.falseAssignments;
    }

    private List<HashMap<String, Boolean>> fetchFalseAssignments() {

    	ArrayList<HashMap<String, Boolean>> subResultAssignmentsList = new ArrayList<HashMap<String, Boolean>>();
    	ArrayList<HashMap<String, Boolean>> resultAssignmentsList = new ArrayList<HashMap<String, Boolean>>();
    	ArrayList<HashMap<String, Boolean>> emptyInputMap = new ArrayList<HashMap<String, Boolean>>();
    	
    	List<ArrayList<HashMap<String, Boolean>>> allFalseAssignmentLists, allTrueAssignmentLists;
    	allFalseAssignmentLists = new ArrayList<ArrayList<HashMap<String, Boolean>>>();
    	allTrueAssignmentLists = new ArrayList<ArrayList<HashMap<String, Boolean>>>();
    	
    	ArrayList<Integer> falseClauses, trueClauses;
    	falseClauses = new ArrayList<Integer>();
    	trueClauses = new ArrayList<Integer>();
    	int numClauses = this.clauseList.size();
    	int firstTrueClause = numClauses;
    	
    	
    	/*
    	 * Start of looping through the clauses.
    	 */
    	for (int i = 0; i < numClauses; i++) {
    		
    		falseClauses.add(i);
    		
    		while (firstTrueClause > i) {
        		for (int j = i+1; j < numClauses; j++) {
        			
        			if (j < firstTrueClause) {
        				if (trueClauses.contains(j)) { trueClauses.remove(j); }
        				
        				falseClauses.add(j);
        			} else {
        				if (falseClauses.contains(j)) { falseClauses.remove(j); }
        				trueClauses.add(j);
        			}
        		}
        		
        		// Add all the mappings lists to correct list of lists.
        		allFalseAssignmentLists.clear();
        		for (int iFalse = 0; iFalse < falseClauses.size(); iFalse++) {
        			allFalseAssignmentLists.add((ArrayList) this.clauseList.get(iFalse).getFalseAssignments());
        		}
        		allTrueAssignmentLists.clear();
        		for (int iTrue = 0; iTrue < trueClauses.size(); iTrue++) {
        			allTrueAssignmentLists.add((ArrayList) this.clauseList.get(iTrue).getTrueAssignments());
        		}
        		
        		subResultAssignmentsList = (ArrayList<HashMap<String, Boolean>>) assignmentCombinationUtil(allFalseAssignmentLists, allTrueAssignmentLists,
        											emptyInputMap, 0, numClauses);
        		resultAssignmentsList.addAll(subResultAssignmentsList);
        		
        		// Decrement first true clause.
        		firstTrueClause--;
    		}

    		if (i < numClauses-1) { 
        		falseClauses.remove(i);
        		trueClauses.add(i);
        	}
    	}
    	return resultAssignmentsList;
    	
    }
    
    private List<HashMap<String, Boolean>> assignmentCombinationUtil(List<ArrayList<HashMap<String, Boolean>>> allFalseAssignmentLists,
    										List<ArrayList<HashMap<String, Boolean>>> allTrueAssignmentLists,
    										List<HashMap<String, Boolean>> curMapList, int curClause, final int totalNoClauses) {
    	// The resulting list.
    	List<HashMap<String, Boolean>> newCurMapList = new ArrayList<HashMap<String, Boolean>>();
    	List<HashMap<String, Boolean>> mergedMapsList = new ArrayList<HashMap<String, Boolean>>();
    	
    	// The false assignment variables
    	final int falseSize, trueSize;
    	List<HashMap<String, Boolean>> falseMapList, trueMapList;
    	
    	if(allFalseAssignmentLists.size() > curClause) {
    		falseMapList = allFalseAssignmentLists.get(curClause);
        	falseSize = falseMapList.size();
    	} else { 
    		falseMapList = new ArrayList<HashMap<String, Boolean>>(); 
        	falseSize = 0; 
        }
    	HashMap<String, Boolean> falseMap = new HashMap<String, Boolean>();
    	// The true assignment variables
    	if (allTrueAssignmentLists.size() > curClause) {
        	trueMapList = allTrueAssignmentLists.get(curClause);
        	trueSize = trueMapList.size();
    	} else {
        	trueMapList = new ArrayList<HashMap<String, Boolean>>();
        	trueSize = 0;
    	}
    	HashMap<String, Boolean> trueMap = new HashMap<String, Boolean>();
    	
    	// Edge case 1.
    	if (curClause == 0) {
    		// for each false assignment in clause, add
    		// that assignment to newCurMapList.
    		for (int i = 0; i < falseSize; i ++) {
        		falseMap = new HashMap<String, Boolean>(falseMapList.get(i));
    			newCurMapList.add(falseMap);
    		}
    		// for each true assignment in clause, add
    		// that assignment also to newCurMapList.
    		for (int i = 0; i < trueSize; i ++) {
        		trueMap = new HashMap<String, Boolean>(trueMapList.get(i));
    			newCurMapList.add(trueMap);
    		}
    		
    		return assignmentCombinationUtil(allFalseAssignmentLists, allTrueAssignmentLists, 
    												newCurMapList, curClause+1, totalNoClauses);
    	}
    	// Edge case 2
    	else if (curClause >= totalNoClauses) {
    		return curMapList;
    		
    	} else {
    		// for each false assignment in clause, add
    		// that assignment to all maps in curMapList.
    		for (int i = 0; i < falseSize; i ++) {
        		falseMap = new HashMap<String, Boolean>(falseMapList.get(i));
        		
        		// for each map in curMapList, add map from
        		// false mapping of current clause with index i.
    			for (HashMap<String, Boolean> map : curMapList) {
    				mergedMapsList = mergeMaps(map, falseMap);
    				for (HashMap<String, Boolean> mergedMap : mergedMapsList) {
            			newCurMapList.add(mergedMap);
    				}
    			}
    		}

    		// for each false assignment in clause, add
    		// that assignment to all maps in curMapList.
    		for (int i = 0; i < trueSize; i ++) {
        		trueMap = new HashMap<String, Boolean>(trueMapList.get(i));
        		
        		// for each map in curMapList, add map from
        		// false mapping of current clause with index i.
    			for (HashMap<String, Boolean> map : curMapList) {
    				mergedMapsList = mergeMaps(map, trueMap);
    				for (HashMap<String, Boolean> mergedMap : mergedMapsList) {
            			newCurMapList.add(mergedMap);
    				}
    			}
    		}
    		
    		return assignmentCombinationUtil(allFalseAssignmentLists, allTrueAssignmentLists, 
    												newCurMapList, curClause+1, totalNoClauses);
    	}
    }
    
    private List<HashMap<String, Boolean>> mergeMaps(HashMap<String, Boolean> firstMap, HashMap<String, Boolean> secondMap) {
    	
    	List<HashMap<String, Boolean>> resultMapList = new ArrayList<HashMap<String, Boolean>>();
    	boolean differentKeyVals = false;
    	HashMap<String, Boolean> resultMap1 = new HashMap<String, Boolean>();
    	HashMap<String, Boolean> resultMap2 = new HashMap<String, Boolean>();
    	
    	// Get all entries and put them in both result maps, whilst
    	// keeping track if there are duplicate keys with different vals.
    	for (Map.Entry<String, Boolean> entry : firstMap.entrySet()) {

    		if ( (secondMap.containsKey(entry.getKey()) ) && (!entry.getValue() == secondMap.get(entry.getKey())) ) {
    				differentKeyVals = true;
    	    		resultMap1.put(entry.getKey(), entry.getValue());
    	    		resultMap2.put(entry.getKey(), secondMap.get(entry.getKey()));
    		}
    		else {
        		resultMap1.put(entry.getKey(), entry.getValue());
        		resultMap2.put(entry.getKey(), entry.getValue());
    		}
    	}
    	// Get all entries and put them in both result maps if
    	// the entry is not already in the maps.
    	for (Map.Entry<String, Boolean> entry : secondMap.entrySet()) {
    		if (!firstMap.containsKey(entry.getKey())) {
        		resultMap1.put(entry.getKey(), entry.getValue());
        		resultMap2.put(entry.getKey(), entry.getValue());
    		}
    	}
    	// Add duplicate, contradicting keys.
    	if (differentKeyVals) {
    		resultMapList.add(resultMap1);
    		resultMapList.add(resultMap2);
    	}
    	else {
    		resultMapList.add(resultMap1); }
    	
    	return resultMapList;
    }
    
    /**
     * Method for satisfying this clause.
     */
    private void satisfy() {
    	
    	// If formula is already satisfied, re-satisfy the formula
    	// by satisfying each clause with another literal.
    	if(isSatisfied()) {
    		
			// Loop through clauses and re-satisfy each clause.
            for(Clause clause : clauseList) {
            	clause.setTrue();
            }
    	}
    	else {
    		// Loop through clauses and satisfy the unsatisfied
    		// clauses.
            for(Clause clause : clauseList) {
            	if(clause.isFalse()) { clause.setTrue(); }
            }
    	}
		// Re-assign clause.
		this.assignment = isSatisfied();
    }
    
    /**
     * Method for unsatisfying this clause.
     */
    private void unsatisfy() {
    	if(isSatisfied()) {
    		// Loop through clauses and unsatisfy the first
    		// clause.
            for(Clause clause : clauseList) {
            	clause.setFalse();
            	break;
            }
    	}
    	else {
        	boolean isFirst = true;
        	boolean clauseFound = false;
        	Clause runClause = new Clause("tempClause", false, false);
    		Clause firstClause = new Clause("tempClause2", false, false);
    		Clause lastSatClause = new Clause("tempClause3", false, false);
			// Loop through clauses and re-unsatisfy each clause.
            for(Clause clause : clauseList) {
            	runClause = clause;
            	if(isFirst) {
            		firstClause = clause; 
            		isFirst = false;
            	}
            	// If this clause is true and previous clause
            	// was unsatisfied, then falsify and break loop
            	if(clause.isTrue() && clauseFound) {
            		clause.setFalse();
            		break;
            	}
            	if(clause.isFalse() && !clauseFound) { 
            		clause.setTrue();
            		lastSatClause = clause;
            		clauseFound = true;
            	}
            }
            if(lastSatClause == runClause) { firstClause.setFalse(); } // Gaat dit goed???
    	}
		// Re-assign clause.
		this.assignment = isSatisfied();
    }
    
    /**
     * Private method to generate a "normal form" formula if
     * this clause was not based on a normal form formula
     * already.
    private NormalLogicRule generateNf() {
    	Disjunction conj = null;
    	Clause firstClause = null, secondClause = null;
    	
        for(Clause clause : clauseList) {
        	if(firstClause == null) { firstClause = clause; }
        	if(secondClause == null) { secondClause = clause; }
        	if(!(firstClause == null || secondClause == null)) {
        		if(conj==null) {
        			conj = new Disjunction(firstClause, secondClause);
        		}
        		else {
        			conj = new Disjunction(conj, clause);
        		}
        	}
        }
        return conj;
    }
    */
    
    
    
	
	/*
	 * Below are all interface methods implemented
	 */

    /**
     * Converts this formula to one of its possible
     * versions that results in the provided 
     * assignment, by changing the assignment of one
     * or several of its literals.
     */
    @Override
    public void setAssignment(boolean assignment) {
    	if(assignment) { satisfy(); }
    	else { unsatisfy(); }
    }
    
    /**
     * @return Returns the current assignment of
     * this formula
     */
    @Override
    public boolean getAssignment() {
        return this.assignment;
    }
    
    /**
     * Sets the assignment of this logic
     * rule to false
     */
	@Override
    public void setFalse() {
        unsatisfy();
    }
    
    /**
     * Sets the assignment of this logic
     * rule to true
     */
    @Override
    public void setTrue() {
        satisfy();
    }

    /**
     * @return Returns true if the assignment
     * has value false, else it returns false.
     */
    @Override
    public boolean isFalse() {
        return !this.assignment;
    }

    /**
     * @return Returns true if the assignment
     * has value true, else it returns false.
     */
    @Override
    public boolean isTrue() {
        return this.assignment;
    }
    
    /**
     * @return Returns the name of this formula
     * (same as toString()).
     */
    @Override
    public String getName() {
    	return this.name;
    }
    
    /**
     * @return Returns this formula as a
     * string, using the names of the 
     * literals.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<Clause> allClauses = getClauses();

        sb.append("(");
        String separator = "";

        for(Clause clause : allClauses) {
            sb.append(separator);
            sb.append(clause.getCnfName());
            separator = " AND ";
        }
        return sb.append(")").toString();
    }

    /**
     * @return Returns this clause as a
     * string, using the values of the 
     * literals.
     */
    @Override
    public String toValueString() {
        StringBuilder sb = new StringBuilder();
        List<Clause> allClauses = getClauses();

        sb.append("(");
        String separator = "";

        for(Clause clause : allClauses) {
            sb.append(separator);
            sb.append(clause.toValueString());
            separator = " AND ";
        }
        return sb.append(")").toString();
    }
    
    /**
     * @return Returns the name in CNF of this
     * formula
     */
    @Override
    public String getCnfName() {
    	return this.cnfName;
    }
    
    /**
     * @return Returns the name in d-DNNF of this
     * formula
     */
    @Override
    public String getDdnnfName() {
    	return this.ddnnfName;
    }
    
    /**
     * @return Returns all literals of this
     * formula in an ArrayList.
     */
    @Override
    public Set<LogicLiteral> getLiterals() {
        Set<LogicLiteral> LogicLiteralSet = new TreeSet<>(positiveLiteralSet);
        LogicLiteralSet.addAll(negativeLiteralSet);
        
        return LogicLiteralSet;
    }
    
    /**
     * @return Returns all positive literals of this 
     * clause in an ArrayList.
     */
    @Override
    public Set<LogicLiteral> getPositiveLiterals() {
        return this.positiveLiteralSet;
    }
    
    /**
     * @return Returns all negative literals of this 
     * clause in an ArrayList.
     */
    @Override
    public Set<LogicLiteral> getNegativeLiterals() {
        return this.negativeLiteralSet;
    }
    
    /**
     * @return Returns an list with all
     * clauses of this formula.
     */
    @Override
    public ArrayList<Clause> getClauses() {
    	return this.clauseList;
    }

	@Override
	public DdnnfGraph getDdnnfGraph() {
		if(this.ddnnfRule == null) { logger.warn("No d-DNNF graph to return."); }
		return this.ddnnfGraph;
	}

	@Override
	public DdnnfLogicRule getDdnnfRule() {
		if(this.ddnnfRule == null) { logger.warn("No d-DNNF rule to return."); }
		return this.ddnnfRule;
	}

	@Override
	public void setDdnnfGraph(DdnnfGraph graph) {
		this.ddnnfGraph = graph;
		
	}

	@Override
	public void setDdnnfRule(DdnnfLogicRule rule) {
		this.ddnnfRule = rule;
	}
    
    /**
     * @return Returns this formula as 
     * precedent of this logic rule
     */
    @Override
    public LogicRule getPrecedent() {
    	return this;
    }
    
    /**
     * @return Returns this formula as 
     * antecedent of this logic rule
     */
    @Override
    public LogicRule getAntecedent() {
    	return this;
    }
    

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!getClass().equals(obj.getClass())) {
            return false;
        }

        return this.getName() == ((CnfFormula) obj).getName();
    }

    @Override 
    public int hashCode() {
		int hash = 7;
		for (int i = 0; i < this.name.length(); i++) {
			hash = hash*31 + this.name.charAt(i);
		}
		return hash;
    }

    @Override 
    public int compareTo(LogicRule other) {

      if (this.hashCode() < other.hashCode()) {
        return -1;
      }
      return this.hashCode() == other.hashCode() ? 0 : 1;
    }

}

