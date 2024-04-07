package org.wso2.ballerina;

import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BridgeCommandTest {
    @Test(description = "test bridge command with help flag")
    void testBridgeCommandWithHelpFlag() throws IOException {
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(console, true, StandardCharsets.UTF_8);
        BridgeCommand bridgeCmd = new BridgeCommand(outputStream);
        String[] args = {"--help"};
        new CommandLine(bridgeCmd).parseArgs(args);
        bridgeCmd.execute();
        String scanLog = console.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        console.close();
        outputStream.close();
        String expected = "Tool for linking Ballerina compiler plugins\n";
        Assert.assertEquals(scanLog, expected);
    }

    @Test(description = "test bridge command")
    void testBridgeCommand() throws IOException {
        // After the Ballerina home is set from the gradle side for tests, the current directory is changed
        String previousUserDir = System.getProperty("user.dir");
        Path userDir = Paths.get("src", "test", "resources", "test-resources", "test-bridge-command");
        System.setProperty("user.dir", userDir.toString());

        // The output of the bridge command is read
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(console, true, StandardCharsets.UTF_8);
        BridgeCommand bridgeCmd = new BridgeCommand(outputStream);
        bridgeCmd.execute();
        String scanLog = console.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");

        // Reset everything back to its original state
        console.close();
        outputStream.close();
        System.setProperty("user.dir", previousUserDir);

        // Perform comparisons
        Path output = Paths.get("src", "test", "resources", "command-outputs",
                "test-bridge-command-output.txt");
        String expected = Files.readString(output).replace("\r\n", "\n");
        Assert.assertTrue(scanLog.contains(expected));
    }
}
