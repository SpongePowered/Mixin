[< Back](../README.md)
# public interface IObfuscationManager IObfuscationManager #
>#### Class Overview ####
>Manages obfuscation things
## Methods ##
### public void init () ###
>#### Method Overview ####
>Initialise the obfuscation environments
>
### public IObfuscationDataProvider getDataProvider () ###
>#### Method Overview ####
>Get the obfuscation mapping source
>
### public IReferenceManager getReferenceManager () ###
>#### Method Overview ####
>Get the reference manager
>
### public IMappingConsumer createMappingConsumer () ###
>#### Method Overview ####
>Create a new mapping consumer
>
### public List getEnvironments () ###
>#### Method Overview ####
>Get available obfuscation environments within this manager
>
### public void writeMappings () ###
>#### Method Overview ####
>Write out generated mappings to the target environments
>
### public void writeReferences () ###
>#### Method Overview ####
>Write out generated refmap
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)