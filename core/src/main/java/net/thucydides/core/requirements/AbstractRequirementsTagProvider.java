package net.thucydides.core.requirements;

import net.thucydides.core.guice.Injectors;
import net.thucydides.core.requirements.model.RequirementsConfiguration;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.Inflector;

import java.util.List;

public class AbstractRequirementsTagProvider {

    protected final EnvironmentVariables environmentVariables;
    protected final String rootDirectory;
    protected final RequirementsConfiguration requirementsConfiguration;
    protected final RequirementsService requirementsService;

    protected AbstractRequirementsTagProvider(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
        this.requirementsConfiguration = new RequirementsConfiguration(environmentVariables);
        this.requirementsService = Injectors.getInjector().getInstance(RequirementsService.class);
        this.rootDirectory = getDefaultRootDirectory();
    }

    protected String humanReadableVersionOf(String name) {
        String underscoredName = Inflector.getInstance().underscore(name);
        return Inflector.getInstance().humanize(underscoredName);
    }

    protected String getDefaultType(int level, int maxDepth) {
        List<String> types = getRequirementTypes();

        // Flat structure: maxdepth 0
        //      cap, feature | level 0 => [1]
        //      cap, feature,story | level 0 => [2]

        // 1-layer structure: maxdepth 1
        //      cap, feature | level 0 => [0]
        //      cap, feature | level 1 => [1]
        //      cap, feature,story | level 0 => [1]
        //      cap, feature,story | level 1 => [2]

        // 2-layer structure: maxdepth 2
        //      cap, feature, story | level 0 => [0]
        //      cap, feature, story | level 1 => [1]
        //      cap, feature, story | level 2 => [2]
        int relativeLevel = types.size() - 1 - maxDepth + level;

        if (relativeLevel > types.size() - 1) {
            return types.get(types.size() - 1);
        } else {
            return types.get(relativeLevel);
        }
    }

    protected String getDefaultType(int level) {
        return getDefaultType(level, getRequirementTypes().size() - 1);
    }

    protected List<String> getRequirementTypes() {
        return requirementsConfiguration.getRequirementTypes();
    }

    protected String getDefaultRootDirectory() {
        return requirementsConfiguration.getDefaultRootDirectory();
    }
}
