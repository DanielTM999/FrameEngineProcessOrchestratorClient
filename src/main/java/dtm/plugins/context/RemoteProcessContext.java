package dtm.plugins.context;

import dtm.apps.core.extension.PluginContext;
import dtm.manager.process.definition.ProcessDefinition;
import dtm.plugins.views.components.pipeline.ProcessSidebarItem;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class RemoteProcessContext extends BaseTerminalOutputContext {

    private final ProcessSidebarItem processSidebar;
    private final AtomicBoolean isRunningRef;
    private final AtomicBoolean isMonitoringRef;
    private final AtomicReference<ProcessDefinition> processDefinitionAtomicRef;

    public RemoteProcessContext(
            PluginContext pluginContext,
            ProcessSidebarItem processSidebar,
            ProcessDefinition processDefinition
    ) {
        super(pluginContext);
        this.processSidebar = processSidebar;
        this.isRunningRef = new AtomicBoolean();
        this.isMonitoringRef = new AtomicBoolean();
        this.processDefinitionAtomicRef = new AtomicReference<>(processDefinition);
    }


    public String getProcessDefinitionId(){
        return (getProcessDefinition() != null) ? getProcessDefinition().getProcessId() : "";
    }

    public String getProcessName(){
        return (getProcessDefinition() != null) ? getProcessDefinition().getProcessName() : "";
    }

    public ProcessDefinition getProcessDefinition() {
        return processDefinitionAtomicRef.get();
    }

    public void setProcessDefinition(ProcessDefinition processDefinition) {
        processDefinitionAtomicRef.set(processDefinition);
    }

    public void setRunning(boolean isRunning) {
        this.isRunningRef.set(isRunning);
        processSidebar.setRunning(isRunning);
    }

    public void setMonitoring(boolean isMonitoring) {
        this.isMonitoringRef.set(isMonitoring);
        processSidebar.setMonitoring(isMonitoring);
    }

    public boolean isRunning() {
        return isRunningRef.get();
    }

    public boolean isMonitoring() {
        return isMonitoringRef.get();
    }
}
