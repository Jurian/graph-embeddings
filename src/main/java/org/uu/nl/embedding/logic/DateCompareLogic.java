/**
 * 
 */
package org.uu.nl.embedding.logic;

import org.uu.nl.embedding.logic.util.SimpleDate;

/**
 * Abstract class for time logic formulae.
 * The time logic formula defines time constraints
 * 		for the data. It represents the time 
 * 		constraints that apply in practice.
 * 
 * @author Euan Westenbroek
 * @version 1.1
 * @since 14-05-2020
 */
public abstract class DateCompareLogic implements LogicRule {
	
	protected DateLogic firstDay = null;
	protected DateLogic secondDay = null;
	protected Boolean isDate = null;
	protected Boolean logicValue = null;
	protected String name = null;
	protected String str = null;
	
	public DateCompareLogic() {
		/*
		 * This is a filler constructor to use for
		 * creating an undefined class initialization
		 * which makes it possible to use this class
		 * as a logic type only to use its static methods.
		 */ 
	}
	
	public DateCompareLogic(String firstDate, String secondDate) {
		this.firstDay = new DateLogic(firstDate);
		this.secondDay = new DateLogic(secondDate);
	}
	
	public DateCompareLogic(SimpleDate firstDate, SimpleDate secondDate) {
		this.firstDay = new DateLogic(firstDate);
		this.secondDay = new DateLogic(secondDate);
	}
	
	public DateCompareLogic(DateLogic firstDate, DateLogic secondDate) {
		super();
		this.firstDay = firstDate;
		this.secondDay = secondDate;
	}
	
	protected abstract void compareTheseDates();
	
	/**
	 * 
	 * @param 	firstDate A String representing the logic date format 
	 * @param 	secondDate A String representing the logic date format
	 * @param 	daysDifference An integer specifying the allowed number of difference
	 * 			between the two dates
	 * @return 	boolean Returns false if method is not
	 * 			newly specified in child class
	 */
	public static boolean compareTwoDates(String firstDate, String secondDate, int daysDifference) {
		SimpleDate date1 = new SimpleDate(firstDate);
		SimpleDate date2 = new SimpleDate(secondDate);
		long diff = SimpleDate.daysToDate(date1, date2);
		
		if ((long) daysDifference < diff) return false;
		else return true;
	}

	
	/**
	 * @return Returns the Boolean value whether it is the same date (true) or not (false)
	 */
	public boolean getAssignment() {
		return this.logicValue;
	}
	
	/**
	 * 
	 * @return Returns the dates as an integer arrays as [[dd, mm, yyyy], [dd, mm, yyyy]]
	 */
	public int[][] getDateAsIntArray() {
		return new int[][] {this.firstDay.getDateAsIntArray(), this.secondDay.getDateAsIntArray()};
	}
	
	/**
	 * @return Returns the name of the SameDate term (given or generated)
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return Returns the string of the date term (generated)
	 */
	public String toString() {
		return this.str;
	}
	
	/**
	 * @return Returns an array of only the strings of the DateLogic terms (generated),
	 * 				in the "dd-mm-yyyy" format
	 */
	public String[] getDateString() {
		return new String[] {
				this.firstDay.getDateString(),
				this.secondDay.getDateString()
		};
	}
	
	/**
	 * @return Returns an array of SimpleDates of this ExactSameDateLogic (generated)
	 */
	public SimpleDate[] getSimpleDate() {
		return new SimpleDate[] {
				this.firstDay.getSimpleDate(),
				this.secondDay.getSimpleDate()
		};
	}
	
	public LogicRule getPrecedent() {
		return this.firstDay;
	}
	
	public LogicRule getAntecedent() {
		return this.secondDay;
	}
}
