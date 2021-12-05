https://www.hl7.org/fhir/extensibility.html#list
value[x] -> valueString

from here : https://www.hl7.org/fhir/datatypes.html

The Type of the element can be left blank in a differential constraint, in which case the type is inherited from the
resource. Abstract types are not permitted to appear as a type when multiple types are listed.  (I.e. Abstract types
cannot be part of a choice).

**Context**
context field: Identifies the types of resource or data type elements to which the extension can be applied. for example
in an extension this field says where it can be used. See structuredefinition-explicit-type-name extension for example
usage It has type: (fhirpath | element | extension) -> Defines how to interpret the expression that defines what the
context of the extension is. expression: An expression that defines where an extension can be used in resources.

FHIRDefined types (ValueSet) ; A list of all concrete types:

1. data-types
    * CodeSystem -> types in FHIR elements
    * ValueSet -> contains CodeSystem
2. resource-types
    * CodeSystem -> MedicationRequest
    * ValueSet -> contains CodeSystem

element: []                                  array item:     ElementDefinition -> data-types array's item f[type]:
Element -> data-types item's property contains:
Extension -> structuredefinition-explicit-type-name ; ignore this. It says the type "name" should be called "TypeRef"
