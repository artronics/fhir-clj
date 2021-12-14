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

```
element: []                                  array
 item:     ElementDefinition -> data-types   array's item
  f[type]: Element           -> data-types   item's property
           contains:
            Extension -> structuredefinition-explicit-type-name ; ignore this. It says the type "name" should be called "TypeRef"
```

**value[x] to field**
When there is `foo[x]` it means, the field name can be different depending on the type. For example if it can
have `reference` and `boolean` then the actual name of the field will be either `fooReference` or `fooBoolean`
See `mediationrequest0312.json` and `mediationrequest0310.json`
and [docs](http://hl7.org/fhir/2018Sep/fhirpath.html#polymorphism)

-----

A case where `id` and `path` are not the same: UKCore MedicationRequest

*IMPORTANT* merging differential to snapshot must be performed only on "derivation: specialization" otherwise if "
derivation:constrained"
then there could be non-unique paths.

Below the resource is `derivation: constrained`. This shouldn't be merged because `path` is not unique.

```json
{
  "id": "MedicationRequest.extension:medicationRepeatInformation",
  "path": "MedicationRequest.extension"
}
```

For the validation rules the first item in `element` has the most interesting info. It's related to that resource.

```json
[
  {
    "key": "sdf-1",
    "severity": "error",
    "human": "Element paths must be unique unless the structure is a constraint",
    "expression": "derivation = 'constraint' or snapshot.element.select(path).isDistinct()",
    "xpath": "(f:derivation/@value = 'constraint') or (count(f:snapshot/f:element) = count(distinct-values(f:snapshot/f:element/f:path/@value)))"
  },
  {
    "key": "sdf-5",
    "severity": "error",
    "human": "If the structure defines an extension then the structure must have context information",
    "expression": "type != 'Extension' or derivation = 'specialization' or (context.exists())",
    "xpath": "not(f:type/@value = 'extension') or (f:derivation/@value = 'specialization') or (exists(f:context))"
  },
  {
    "key": "sdf-19",
    "requirements": "custom types only in logical models",
    "severity": "error",
    "human": "FHIR Specification models only use FHIR defined types",
    "expression": "url.startsWith('http://hl7.org/fhir/StructureDefinition') implies (differential.element.type.code.all(matches('^[a-zA-Z0-9]+$') or matches('^http:\\\\/\\\\/hl7\\\\.org\\\\/fhirpath\\\\/System\\\\.[A-Z][A-Za-z]+$')) and snapshot.element.type.code.all(matches('^[a-zA-Z0-9\\\\.]+$') or matches('^http:\\\\/\\\\/hl7\\\\.org\\\\/fhirpath\\\\/System\\\\.[A-Z][A-Za-z]+$')))",
    "xpath": "not(starts-with(f:url/@value, 'http://hl7.org/fhir/StructureDefinition')) or count(f:differential/f:element/f:type/f:code[@value and not(matches(string(@value), '^[a-zA-Z0-9\\.]+$'))]|f:snapshot/f:element/f:type/f:code[@value and not(matches(string(@value), '^[a-zA-Z0-9]+$\\.'))]) =0"
  },
  {
    "key": "sdf-15a",
    "requirements": "No Type on the root element (differential)",
    "severity": "error",
    "human": "If the first element in a differential has no \".\" in the path and it's not a logical model, it has no type",
    "expression": "(kind!='logical'  and differential.element.first().path.contains('.').not()) implies differential.element.first().type.empty()",
    "xpath": "f:kind/@value='logical' or not(f:differential/f:element[1][not(contains(f:path/@value, '.'))]/f:type)"
  }
]
```

Constraints related to ElementDefinition

"human": "Types must be unique by code",
"expression": "type.select(code).isDistinct()",

