plugins {
    id("org.pkl-lang") version "0.27.0"
}

pkl {
    evaluators {
        val workflowsDir = layout.dir(provider { file("workflows") })
        val pklOutputDir = layout.buildDirectory.dir("example")
        fileTree("workflows").filterNot {
            it.name in listOf("PklProject.pkl", "PklProject.deps.json")
        }.forEach { pklFile ->
            val pklName = pklFile.name.replace(".pkl", "")
            val yamlFile = pklOutputDir.map { it.file("${pklName}.yaml") }
            create(pklName) {
                projectDir.set(workflowsDir)
                sourceModules.addAll(pklFile)
                outputFile.set(yamlFile)
                outputFormat = "yaml"
            }
            tasks.named(pklName).configure {
                group = "pkl"
                description = "Generates ${pklFile.name}"
                outputs.file(yamlFile)
            }
        }
    }
}
