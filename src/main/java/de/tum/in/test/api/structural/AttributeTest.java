package de.tum.in.test.api.structural;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;

/**
 * @author Stephan Krusche (krusche@in.tum.de)
 * @version 5.0 (11.11.2020)
 * <br><br>
 * This test evaluates if the specified attributes in the structure oracle are correctly implemented with the expected type, visibility modifiers and annotations,
 * based on its definition in the structure oracle (test.json).
 */
public abstract class AttributeTest extends StructuralTest {

    /**
     * This method collects the classes in the structure oracle file for which attributes are specified.
     * These classes are then transformed into JUnit 5 dynamic tests.
     * @return A dynamic test container containing the test for each class which is then executed by JUnit.
     * @throws URISyntaxException an exception if the URI of the class name cannot be generated (which seems to be unlikely)
     */
    protected DynamicContainer generateTestsForAllClasses() throws URISyntaxException {
        List<DynamicNode> tests = new ArrayList<>();

        if (structureOracleJSON == null) {
            fail("The AttributeTest test can only run if the structural oracle (test.json) is present. If you do not provide it, delete AttributeTest.java!");
        }

        for (int i = 0; i < structureOracleJSON.length(); i++) {
            JSONObject expectedClassJSON = structureOracleJSON.getJSONObject(i);

            // Only test the classes that have attributes defined in the oracle.
            if(expectedClassJSON.has(JSON_PROPERTY_CLASS) && (expectedClassJSON.has(JSON_PROPERTY_ATTRIBUTES) || expectedClassJSON.has("enumValues"))) {
                JSONObject expectedClassPropertiesJSON = expectedClassJSON.getJSONObject(JSON_PROPERTY_CLASS);
                String expectedClassName = expectedClassPropertiesJSON.getString(JSON_PROPERTY_NAME);
                String expectedPackageName = expectedClassPropertiesJSON.getString(JSON_PROPERTY_PACKAGE);
                ExpectedClassStructure expectedClassStructure = new ExpectedClassStructure(expectedClassName, expectedPackageName, expectedClassJSON);
                tests.add(dynamicTest("testAttributes[" + expectedClassName + "]", () -> testAttributes(expectedClassStructure)));
            }
        }
        if (tests.isEmpty()) {
            fail("No tests for attributes available in the structural oracle (test.json). Either provide attributes information or delete AttributeTest.java!");
        }
        // Using a custom URI here to workaround surefire rendering the JUnit XML without the correct test names.
        return dynamicContainer(getClass().getName(), new URI(getClass().getName()), tests.stream());
    }

    /**
     * This method gets passed the expected class structure generated by the method generateTestsForAllClasses(), checks if the class is found
     * at all in the assignment and then proceeds to check its attributes.
     * @param expectedClassStructure: The class structure that we expect to find and test against.
     */
    protected void testAttributes(ExpectedClassStructure expectedClassStructure) {
        String expectedClassName = expectedClassStructure.getExpectedClassName();
        Class<?> observedClass = findClassForTestType(expectedClassStructure, "attribute");
        if (observedClass == null) {
            fail("Class " + expectedClassName + " not found for class test");
            return;
        }

        if (expectedClassStructure.hasProperty(JSON_PROPERTY_ATTRIBUTES)) {
            JSONArray expectedAttributes = expectedClassStructure.getPropertyAsJsonArray(JSON_PROPERTY_ATTRIBUTES);
            checkAttributes(expectedClassName, observedClass, expectedAttributes);
        }
        if (expectedClassStructure.hasProperty("enumValues")) {
            JSONArray expectedEnumValues = expectedClassStructure.getPropertyAsJsonArray("enumValues");
            checkEnumValues(expectedClassName, observedClass, expectedEnumValues);
        }
    }

    /**
     * This method checks if a observed class' attributes match the expected ones defined in the structure oracle.
     * @param expectedClassName: The simple name of the class, mainly used for error messages.
     * @param observedClass: The class that needs to be checked as a Class object.
     * @param expectedAttributes: The information on the expected attributes contained in a JSON array. This information consists
     * of the name, the type and the visibility modifiers of each attribute.
     */
    protected void checkAttributes(String expectedClassName, Class<?> observedClass, JSONArray expectedAttributes) {
        for(int i = 0; i < expectedAttributes.length(); i++) {
            JSONObject expectedAttribute = expectedAttributes.getJSONObject(i);
            String expectedName = expectedAttribute.getString(JSON_PROPERTY_NAME);
            String expectedTypeName = expectedAttribute.getString(JSON_PROPERTY_TYPE);
            JSONArray expectedModifiers = expectedAttribute.has(JSON_PROPERTY_MODIFIERS) ? expectedAttribute.getJSONArray(JSON_PROPERTY_MODIFIERS) : new JSONArray();
            JSONArray expectedAnnotations = expectedAttribute.has(JSON_PROPERTY_ANNOTATIONS) ? expectedAttribute.getJSONArray(JSON_PROPERTY_ANNOTATIONS) : new JSONArray();

            // We check for each expected attribute if the name and the type is right.
            boolean nameIsRight = false;
            boolean typeIsRight = false;
            boolean modifiersAreRight = false;
            boolean annotationsAreRight = false;

            for(Field observedAttribute : observedClass.getDeclaredFields()) {
                String observedName = observedAttribute.getName();
                String[] observedModifiers = Modifier.toString(observedAttribute.getModifiers()).split(" ");
                Annotation[] observedAnnotations = observedAttribute.getAnnotations();

                // If the names don't match, then proceed to the next observed attribute
                if(!expectedName.equals(observedName)) {
                    //TODO: we should also take wrong case and typos into account
                    continue;
                } else {
                    nameIsRight = true;
                }

                typeIsRight = checkType(observedAttribute, expectedTypeName);
                modifiersAreRight = checkModifiers(observedModifiers, expectedModifiers);
                annotationsAreRight = checkAnnotations(observedAnnotations, expectedAnnotations);

                // If all are correct, then we found our attribute and we can break the loop
                if(typeIsRight && modifiersAreRight && annotationsAreRight) {
                    break;
                }
            }

            String expectedAttributeInformation = "the expected attribute '" + expectedName + "' of the class '" + expectedClassName + "'";

            if (!nameIsRight) {
                fail("The name of " + expectedAttributeInformation + " is not implemented as expected.");
            }
            if (!typeIsRight) {
                fail("The type of " + expectedAttributeInformation + " is not implemented as expected.");
            }
            if (!modifiersAreRight) {
                fail("The modifier(s) (access type, abstract, etc.) of " + expectedAttributeInformation + " are not implemented as expected.");
            }
            if (!annotationsAreRight) {
                fail("The annotation(s) of " + expectedAttributeInformation + " are not implemented as expected.");
            }
        }
    }

    /**
     * This method checks if the observed enum values match the expected ones defined in the structure oracle.
     * @param expectedClassName: The simple name of the class, mainly used for error messages.
     * @param observedClass: The enum that needs to be checked as a Class object.
     * @param expectedEnumValues: The information on the expected enum values contained in a JSON array. This information consists
     * of the name of each enum value.
     */
    protected void checkEnumValues(String expectedClassName, Class<?> observedClass, JSONArray expectedEnumValues) {
        Object[] observedEnumValues = observedClass.getEnumConstants();

        if (observedEnumValues == null) {
            fail("The enum '" + expectedClassName + "' does not contain any enum constants. Make sure to implement them.");
        }
        if (expectedEnumValues.length() != observedEnumValues.length) {
            fail("The enum '" + expectedClassName + "' does not contain all the expected enum values. Make sure to implement the missing enums.");
        }

        for(int i = 0; i < expectedEnumValues.length(); i++) {
            String expectedEnumValue = expectedEnumValues.getString(i);

            boolean enumValueExists = false;
            for(Object observedEnumValue : observedEnumValues) {

                if(expectedEnumValue.equals(observedEnumValue.toString())) {
                    enumValueExists = true;
                    break;
                }
            }
            if(!enumValueExists) {
                fail("The class '" + expectedClassName + "' does not include the enum value: " + expectedEnumValue
                        + ". Make sure to implement it as expected.");
            }
        }
    }

    /**
     * This method checks if the type of an observed attribute matches the expected one.
     * It first checks if the type of the attribute is a generic one or not.
     * In the first case, it sees if the main and the generic types match, otherwise
     * it only looks up the simple name of the attribute.
     * @param observedAttribute: The observed attribute we need to check.
     * @param expectedTypeName: The name of the expected type.
     * @return True, if the types match, false otherwise.
     */
    protected boolean checkType(Field observedAttribute, String expectedTypeName) {
        boolean expectedTypeIsGeneric = expectedTypeName.contains("<") && expectedTypeName.contains(">");

        if(expectedTypeIsGeneric) {
            boolean mainTypeIsRight;
            boolean genericTypeIsRight = false;

            String expectedMainTypeName = expectedTypeName.split("<")[0];
            String observedMainTypeName = observedAttribute.getType().getSimpleName();
            mainTypeIsRight = expectedMainTypeName.equals(observedMainTypeName);

            String expectedGenericTypeName = expectedTypeName.split("<")[1].replace(">", "");
            if(observedAttribute.getGenericType() instanceof ParameterizedType) {
                Type observedGenericType = ((ParameterizedType) observedAttribute.getGenericType()).getActualTypeArguments()[0];
                String observedGenericTypeName = observedGenericType.toString().substring(observedGenericType.toString().lastIndexOf(".") + 1);
                genericTypeIsRight = expectedGenericTypeName.equals(observedGenericTypeName);
            }

            return mainTypeIsRight && genericTypeIsRight;
        } else {
            String observedTypeName = observedAttribute.getType().getSimpleName();
            return expectedTypeName.equals(observedTypeName);
        }
    }
}
