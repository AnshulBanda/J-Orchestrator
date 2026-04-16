package com.jorchestrator.model.job;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * OOAD — Concrete Product (Factory Method pattern).
 *
 * <p>Represents a job that executes a shell/Python/arbitrary script inline.
 * The script body is stored in {@code script_content} (TEXT column).
 * Created exclusively by {@link com.jorchestrator.factory.ScriptJobFactory#createJob}.
 */
@Entity
@DiscriminatorValue("SCRIPT")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ScriptJob extends Job {

    /** Full script body (e.g. a bash or Python script). */
    @Column(columnDefinition = "TEXT")
    private String scriptContent;

    /**
     * Interpreter used to run the script (e.g. {@code bash}, {@code python3}).
     * The worker agent will prepend this when constructing the OS command.
     */
    private String interpreter;
}