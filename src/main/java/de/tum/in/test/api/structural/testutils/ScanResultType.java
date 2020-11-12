package de.tum.in.test.api.structural.testutils;

/**
 * @author Stephan Krusche (krusche@in.tum.de)
 * @version 5.0 (11.11.2020)
 */
public enum ScanResultType {
    CORRECT_NAME_CORRECT_PLACE,
    CORRECT_NAME_MISPLACED,
    CORRECT_NAME_MULTIPLE_TIMES_PRESENT,
    WRONG_CASE_CORRECT_PLACE,
    WRONG_CASE_MISPLACED,
    WRONG_CASE_MULTIPLE_TIMES_PRESENT,
    TYPOS_CORRECT_PLACE,
    TYPOS_MISPLACED,
    TYPOS_MULTIPLE_TIMES_PRESENT,
    NOTFOUND,
    UNDEFINED
}
