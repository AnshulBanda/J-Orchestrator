package com.jorchestrator.model.job;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * OOAD — Concrete Product (Factory Method pattern).
 *
 * <p>Represents a job that executes a Java Archive ({@code .jar}).
 * Created exclusively by {@link com.jorchestrator.factory.JarJobFactory#createJob}.
 *
 * <p>The {@code @DiscriminatorValue("JAR")} ensures that Spring/Hibernate resolves
 * the correct subtype when loading a {@link Job} row from the database.
 */
@Entity
@DiscriminatorValue("JAR")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class JarJob extends Job {

    /** Absolute path to the JAR file on the file system accessible by worker nodes. */
    private String jarFilePath;

    /** Fully-qualified main class to invoke (e.g. {@code com.example.MainPipeline}). */
    private String mainClass;

    /** Optional extra JVM arguments (e.g. {@code -Xmx512m -Denv=prod}). */
    private String jvmArgs;
}