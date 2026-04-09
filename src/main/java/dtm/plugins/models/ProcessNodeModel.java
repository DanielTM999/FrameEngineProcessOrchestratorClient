package dtm.plugins.models;

import dtm.manager.impl.common.ProcessEntry;
import dtm.manager.process.definition.ProcessDefinition;
import dtm.manager.process.dto.ProcessDTO;
import dtm.manager.process.enums.RestartPolicy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;


@Data
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProcessNodeModel {

    @EqualsAndHashCode.Include
    private String processId;
    private String processName;
    private String executionEnvironment;
    private String executablePath;
    private String type;
    private long startTimeout;
    private String command;
    private List<String> args;
    private RestartPolicy restartPolicy;
    private List<ProcessNodeModel> subProcessesBefore;
    private List<ProcessNodeModel> subProcessesAfter;
    private Object executionCondition;

    public ProcessNodeModel() {
        this.args = new ArrayList<>();
        this.subProcessesBefore = new ArrayList<>();
        this.subProcessesAfter = new ArrayList<>();
    }

    public static ProcessNodeModel from(ProcessDefinition def) {
        if (def == null) return null;
        ProcessNodeModel data = new ProcessNodeModel();
        data.processId = def.getProcessId();
        data.processName = def.getProcessName();
        data.executionEnvironment = def.getExecutionEnvironment();
        data.executablePath = def.getExecutablePath();
        data.type = def.getType();
        data.startTimeout = def.getStartTimeout();
        data.command = def.getCommand();
        data.args = def.getArgs() != null ? new ArrayList<>(def.getArgs()) : new ArrayList<>();
        data.restartPolicy = def.getRestartPolicy();
        data.executionCondition = def.getExecutionCondition() != null ? def.getExecutionCondition() : null;

        if (def.getSubProcessesBefore() != null) {
            for (ProcessDefinition sub : def.getSubProcessesBefore()) {
                data.subProcessesBefore.add(ProcessNodeModel.from(sub));
            }
        }
        if (def.getSubProcessesAfter() != null) {
            for (ProcessDefinition sub : def.getSubProcessesAfter()) {
                data.subProcessesAfter.add(ProcessNodeModel.from(sub));
            }
        }
        return data;
    }

    public boolean hasSub() {
        return (subProcessesBefore != null && !subProcessesBefore.isEmpty())
                || (subProcessesAfter != null && !subProcessesAfter.isEmpty());
    }


    public static ProcessEntry toProcessDTO(ProcessNodeModel processNodeModel) {
        return ProcessEntry.builder()
                .processName(processNodeModel.getProcessName())
                .executionEnvironment(processNodeModel.getExecutionEnvironment())
                .executablePath(processNodeModel.getExecutablePath())
                .type(processNodeModel.getType())
                .startTimeout(processNodeModel.getStartTimeout())
                .command(processNodeModel.getCommand())
                .args(processNodeModel.getArgs())
                .restartPolicy(processNodeModel.getRestartPolicy())
                .executionCondition(processNodeModel.getExecutionCondition())
                .subProcessesBefore(toProcessDTO(processNodeModel.getSubProcessesBefore()))
                .subProcessesAfter(toProcessDTO(processNodeModel.getSubProcessesAfter()))
                .build();
    }

    private static List<ProcessEntry> toProcessDTO(List<ProcessNodeModel> processNodeModels) {
        if(processNodeModels == null) return new ArrayList<>();
        List<ProcessEntry> processDTOs = new ArrayList<>();
        for (ProcessNodeModel processNodeModel : processNodeModels) {
            processDTOs.add(toProcessDTO(processNodeModel));
        }
        return processDTOs;
    }
}
