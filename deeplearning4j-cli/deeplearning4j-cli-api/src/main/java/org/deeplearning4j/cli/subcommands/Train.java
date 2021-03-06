package org.deeplearning4j.cli.subcommands;

import org.deeplearning4j.cli.api.SubCommand;
import org.kohsuke.args4j.Option;

/**
 * Subcommand for training model
 *
 * Options:
 *      Required:
 *          --input: input data file for model
 *          --model: json configuration for model
 *
 * Created by sonali on 2/10/15.
 */
public class Train extends BaseSubCommand {

    @Option(name = "--input", usage = "input data",aliases = "-i", required = true)
    private String input = "input.txt";

    @Option(name = "--model", usage = "model for prediction", aliases = "-m", required = true)
    private String model = "model.json";

    public Train(String[] args) {
        super(args);
    }

    @Override
    public void exec() {

    }
}
