package flow.api;

import DataDefinition.api.DataDefinitions;
import Steps.api.DataDefinitionDeclaration;
import exceptions.OutputsWithSameName;
import flow.Mapping.AutomaticMapping;
import flow.Mapping.CustomMapping;
import flow.api.FlowIO.IO;
import flow.api.FlowIO.SingleFlowIOData;
import jaxb.schema.generated.STFlow;

import java.util.*;

public class FlowDefinitionImpl implements FlowDefinition {

    private final String name;
    private final String description;
    private final List<String> flowOutputs;
    private final List<StepUsageDeclaration> steps;
    private final Map<String, DataDefinitions> name2DataDefinition;
    private final Map<String, String> name2alias;
    private final List<SingleFlowIOData> IOlist;

    public FlowDefinitionImpl(String name, String description) {
        this.name = name;
        this.description = description;
        flowOutputs = new ArrayList<>();
        steps = new LinkedList<>();
        name2DataDefinition = new HashMap<>();
        name2alias = new HashMap<>();
        IOlist = new ArrayList<>();
    }

    @Override
    public void addStepToFlow(StepUsageDeclaration stepUsageDeclaration) {
        steps.add(stepUsageDeclaration);
    }

    public void addFlowOutput(String outputName) {
        flowOutputs.add(outputName);
    }

    @Override
    public List<DataDefinitionDeclaration> getFlowFreeInputs() {
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<StepUsageDeclaration> getFlowSteps() {
        return steps;
    }

    @Override
    public List<String> getFlowFormalOutputs() {
        return flowOutputs;
    }

    @Override
    public void addToName2DDMap(String name, DataDefinitions DD) {
        name2DataDefinition.put(name, DD);
    }

    @Override
    public void addToName2AliasMap(String name, String alias) {
        name2alias.put(name, alias);
    }

    @Override
    public DataDefinitions getDDFromMap(String InputName) {

        return name2DataDefinition.get(InputName);
    }

   @Override
    public String getAliasFromMap(String originalName) {
        return name2alias.get(originalName);
    }

    @Override
    public Map<String, DataDefinitions> getName2DDMap() {
        return name2DataDefinition;
    }

    @Override
    public Map<String, String> getName2aliasMap() {
        return name2alias;
    }

    @Override
    public List<SingleFlowIOData> getIOlist() {
        return IOlist;
    }

    @Override
    public void addElementToIoList(SingleFlowIOData IOElement) {
            IOlist.add(IOElement);
    }
    @Override
    public boolean stepExist(String stepName) {
        boolean isPresent =
                getFlowSteps()
                        .stream()
                        .anyMatch(name -> name.getFinalStepName().equals(stepName));
//the warning is species to aliasing flow def - need to change
        if (!isPresent) {
            String warning = "There is aliasing flow definition for step that does not exist within the Flow definition";
            return false;
        }
        return true;
    }
    @Override
    public boolean dataExist(String stepName, String dataName) {

        boolean isPresentInInput =
                getFlowSteps()
                        .stream()
                        .filter(step -> step.getFinalStepName().equals(stepName))
                        .flatMap(step -> step.getStepDefinition().inputs().stream()) //go to inputs list of step
                        .anyMatch(input -> input.getName().equals(dataName));

        boolean isPresentInOutput =
                getFlowSteps()
                        .stream()
                        .filter(step -> step.getFinalStepName().equals(stepName))
                        .flatMap(step -> step.getStepDefinition().outputs().stream()) //go to output list of step
                        .anyMatch(output -> output.getName().equals(dataName));

//the warning is species to aliasing flow def - need to change
        if (!isPresentInInput && !isPresentInOutput) {
            String warning = "There is aliasing flow definition for source-data-name that does not exist within the Flow definition";
            return false;
        }
        return true;
    }

    @Override
    public boolean isFlowOutputsValid(List<String> outputsNamesList){

        boolean isPresent = true;
        for(String outputName : outputsNamesList) {
            isPresent =
                    getName2aliasMap()
                            .values()
                            .stream()
                            .anyMatch(output -> output.equals(outputName));
        }
//the warning is species to aliasing flow def - need to change
        if (!isPresent) {
            String warning = "Output Flow contains an information item that does not exist";
            return false;
        }
        return true;
    }

    @Override
    public void validateFlowStructure() {
        validateIfOutputsHaveSameName();
        AutomaticMapping automaticMapping = new AutomaticMapping(this);
        CustomMapping customMapping = new CustomMapping(this);
        flowOutputsIsNotExists();
        mandatoryInputsIsNotUserFriendly();

    }
    public void validateIfOutputsHaveSameName() {
        boolean isPresent =
                flowOutputs
                        .stream()
                        .anyMatch(name -> Collections
                                .frequency(flowOutputs, name) > 1);

        if (isPresent) {
            String exception = "Invalid. There are 2 or more outputs with the same name.";
        }
    }

    public void mandatoryInputsIsNotUserFriendly() {
        boolean isPresent =
                getIOlist()
                        .stream()
                        .filter(data -> data.getType() == IO.INPUT)
                        .filter(data -> data.getOptionalInputs().isEmpty())
                        .anyMatch(data -> data.getDD().isUserFriendly() == false);

        if (isPresent) {
            String exception = "Invalid. There are mandatory inputs that is not user friendly.";
        }
    }

    public void flowOutputsIsNotExists(){
       /* for(String output : getFlowFormalOutputs()) {
            boolean isPresent ;

            if (isPresent) {
                String exception = "Invalid. There is at least one flow output that is not exists.";
            }*/
    }

    public void mandatoryInputsWithSameNameAndDifferentType() {
        for(SingleFlowIOData currData  : getIOlist()) {
            boolean isPresent =
                    getIOlist()
                            .stream()
                            .filter(data -> data.getType() == IO.INPUT)
                            .filter(data -> data.getOptionalInputs().isEmpty())
                            .filter(data -> data.getName().equals(currData.getName()))
                            .anyMatch(data -> !data.getType().equals(currData.getType()));

            if (isPresent) {
                String exception = "Invalid. There are mandatory inputs with the same name but different type.";
            }
        }
    }

}