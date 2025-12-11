package qz.installer.provision.invoker;

import qz.build.provision.Step;

/**
 * Stub class for deploying an otherwise "action-less" resource, only to be used by other tasks
 */
public class ResourceInvoker extends InvokableResource {
    private Step step;

    public ResourceInvoker(Step step) {
        this.step = step;
    }

    @Override
    public boolean invoke() throws Exception {
        return dataToFile(step) != null;
    }
}
