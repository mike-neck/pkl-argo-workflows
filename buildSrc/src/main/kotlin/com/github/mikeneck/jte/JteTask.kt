package com.github.mikeneck.jte

import gg.jte.CodeResolver
import gg.jte.ContentType
import gg.jte.TemplateEngine
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.io.path.exists

abstract class JteTask @Inject constructor(private val workerExecutor: WorkerExecutor) :
    DefaultTask() {
  @get:InputFiles abstract val classpath: ConfigurableFileCollection
  @get:InputFiles abstract val src: DirectoryProperty
  @get:Input abstract val packageName: Property<String>

  @get:OutputDirectory abstract val destination: DirectoryProperty

    @TaskAction
    fun execute() {
        val queue = workerExecutor.classLoaderIsolation { spec ->
            spec.classpath.from(classpath)
        }
        queue.submit<Param>(JteWorker::class.java) { param ->
            param.src.set(src)
            param.destination.set(destination)
            param.packageName.set(packageName)
        }
        queue.await()
    }
}

interface Param: WorkParameters {
    val src: DirectoryProperty
    val packageName: Property<String>
    val destination: DirectoryProperty
}

abstract class JteWorker: WorkAction<Param> {

    private val srcDir: Path by lazy { parameters.src.get().asFile.toPath() }
    private val files: FileCollection by lazy { parameters.src.asFileTree }
    private val packageName: String by lazy { parameters.packageName.get() }
    private val destination: Path by lazy { parameters.destination.get().asFile.toPath() }


    override fun execute() {
        val start = Instant.now(Clock.systemUTC())
        val logger = LoggerFactory.getLogger(JteWorker::class.java)
        val resolver = FileResolver(srcDir, files, logger)
        val templateEngine = TemplateEngine.create(
            resolver, destination, ContentType.Plain, null, packageName
        )
        val result = runCatching {
            templateEngine.cleanAll()
            val finish = templateEngine.precompileAll()
            val end = Instant.now(Clock.systemUTC())
            finish to end
        }
        result.onFailure {
            logger.error("Failed to generate pre-compiled template by ${it.javaClass} ${it.message}", it)
            throw JteFailedException("Failed to generate pre-compiled template by ${it.javaClass} ${it.message}", it)
        }.onSuccess { pair ->
            val processTime = Duration.between(start, pair.second)
            val count = pair.first.size
            logger.info("Successfully pre-compiled templates in ${processTime.seconds} sec")
        }
    }
}

class JteFailedException(message: String, e: Throwable) : GradleException(message, e)

class FileResolver(
    val abstractRootPath: Path,
    private val files: FileCollection,
    private val logger: Logger
): CodeResolver {

    init {
        logger.info("source root: ${abstractRootPath}")
    }

    override fun resolve(name: String?): String? {
        if (name == null) {
            logger.warn("[resolve] failed to resolve null name")
            return null
        }
        val found = files.find { it.name == name }
        if (found == null) {
            logger.warn("[resolve] failed to resolve file $name, not existing")
            return null
        }
        return found.readText(Charsets.UTF_8)
    }

    override fun getLastModified(name: String?): Long {
        if (name == null) {
            logger.warn("[getLastModified] failed to get last modified of null name")
            return 0L
        }
        val found = files.find { it.name == name }
        if (found == null) {
            logger.warn("[getLastModified] failed to get last modified of $name, not existing")
            return 0L
        }
        try {
            val lastModifiedTime = Files.getLastModifiedTime(found.toPath())
            logger.info("[getLastModified] successfully get modified time of $name($found), ${lastModifiedTime.toInstant()}")
            return lastModifiedTime.toMillis()
        } catch (e: Exception) {
            logger.warn("[getLastModified] failed to get last modified of $name, by exception: ${e.javaClass} ${e.message}")
            return 0L
        }
    }

    override fun resolveAllTemplateNames(): List<String> = files.map { it.name }
}
