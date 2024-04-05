package org.wso2.ballerina;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.cli.BLauncherCmd;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.diagnostics.DiagnosticPropertyKind;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "bridge", description = "Link with compiler plugins")
public class BridgeCommand implements BLauncherCmd {
    // =============================
    // Ballerina Launcher Attributes
    // =============================
    private final PrintStream outputStream;
    private final PrintStream errorStream;
    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();
    private String projectPath;
    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden = true)
    private boolean helpFlag;

    public BridgeCommand() {
        this.outputStream = System.out;
        this.errorStream = System.err;
    }

    public BridgeCommand(PrintStream outputStream) {
        this.outputStream = outputStream;
        this.errorStream = outputStream;
    }


    // =====================
    // bal help INFO Methods
    // =====================
    @Override
    public String getName() {
        return "bridge";
    }

    @Override
    public void printLongDesc(StringBuilder out) {
        out.append("Tool for linking Ballerina compiler plugins\n\n");
        out.append("bal bridge <ballerina-file>\n\n");
    }

    @Override
    public void printUsage(StringBuilder out) {
        out.append("Tool for linking Ballerina compiler plugins");
    }

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {
    }

    // ====================
    // Main Program Methods
    // ====================
    // MAIN method
    @Override
    public void execute() {
        // if bal scan --help is passed
        if (helpFlag) {
            StringBuilder builder = helpMessage();
            outputStream.println(builder);
            return;
        }

        // Simulate loading a project and engaging a compiler plugin
        String userPath = checkPath();

        // Terminate program if the path is invalid
        if (userPath == null) {
            return;
        }

        // Get access to the project API
        Project project = ProjectLoader.loadProject(Path.of(userPath));

        // Array to hold all issues
        ArrayList<Issue> issues = new ArrayList<>();

        // Iterate through each module of the project
        project.currentPackage().moduleIds().forEach(moduleId -> {
            // Get access to the project modules
            Module module = project.currentPackage().module(moduleId);

            // Iterate through each document of the module
            module.documentIds().forEach(documentId -> {
                // Get access to the module documents
                Document document = module.document(documentId);

                // Retrieve the syntax tree from the parsed ballerina document
                SyntaxTree syntaxTree = document.syntaxTree();

                // Retrieve the compilation of the module
                ModuleCompilation compilation = module.getCompilation();

                // Retrieve the semantic model from the ballerina document compilation
                SemanticModel semanticModel = compilation.getSemanticModel();

                // Retrieve the current document path
                Path documentPath = project.documentPath(documentId).orElse(null);

                // Retrieve the current module name
                String moduleName = module.moduleName().toString();

                // Retrieve the current document name
                String documentName = document.name();

                // Initialize the reporter
                ScannerContextImpl context = new ScannerContextImpl(issues,
                        document,
                        module,
                        project);

                // Simulating performing a local analysis by reporting a local issue for each document
                context.getReporter().reportIssue(0,
                        0,
                        0,
                        0,
                        "S107",
                        "Local issue",
                        "INTERNAL_CHECK_VIOLATION",
                        context.getCurrentDocument(),
                        context.getCurrentModule(),
                        context.getCurrentProject());

                // Iterate through received diagnostics and add reported issues
                project.currentPackage().getCompilation().diagnosticResult().diagnostics().forEach(diagnostic -> {
                    // Filter through the diagnostics
                    String issueType = diagnostic.diagnosticInfo().code();
                    if (issueType.equals("SCAN_TOOL_DIAGNOSTICS")) {
                        List<DiagnosticProperty<?>> properties = diagnostic.properties();

                        properties.forEach(diagnosticProperty -> {
                            // Validating the type of diagnostic property
                            if (diagnosticProperty.kind().equals(DiagnosticPropertyKind.OTHER)) {
                                // Creating issue through reflection
                                Object pluginIssue = diagnosticProperty.value();
                                try {
                                    Issue externalIssue = new Issue(
                                            (Integer) pluginIssue.getClass().getMethod("getStartLine").invoke(pluginIssue),
                                            (Integer) pluginIssue.getClass().getMethod("getStartLineOffset").invoke(pluginIssue),
                                            (Integer) pluginIssue.getClass().getMethod("getEndLine").invoke(pluginIssue),
                                            (Integer) pluginIssue.getClass().getMethod("getEndLineOffset").invoke(pluginIssue),
                                            pluginIssue.getClass().getMethod("getRuleID").invoke(pluginIssue).toString(),
                                            pluginIssue.getClass().getMethod("getMessage").invoke(pluginIssue).toString(),
                                            pluginIssue.getClass().getMethod("getIssueType").invoke(pluginIssue).toString(),
                                            pluginIssue.getClass().getMethod("getFileName").invoke(pluginIssue).toString(),
                                            pluginIssue.getClass().getMethod("getReportedFilePath").invoke(pluginIssue).toString()
                                    );

                                    // Adding the retrieved diagnostic from compiler plugin to the tool
                                    issues.add(externalIssue);
                                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                });
            });
        });

        // output scanned results to console
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        outputStream.println(gson.toJson(issuesAsJson));
    }

    private StringBuilder helpMessage() {
        InputStream inputStream = BridgeCommand.class.getResourceAsStream("/cli-help/ballerina-bridge.help");
        StringBuilder builder = new StringBuilder();
        if (inputStream != null) {
            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(inputStreamReader)
            ) {
                String content = br.readLine();
                builder.append(content);
                while ((content = br.readLine()) != null) {
                    builder.append("\n").append(content);
                }
            } catch (IOException ex) {
                builder.append("Help text is not available.");
                throw new RuntimeException(ex);
            }
        }
        return builder;
    }

    public String checkPath() {
        // retrieve the user passed argument or the current working directory
        this.projectPath = argList.isEmpty() ? null : String.valueOf(Paths.get(argList.get(0)));
        String userFilePath = this.projectPath != null ? this.projectPath : System.getProperty("user.dir");

        // Check if the user provided path is a file or a directory
        File file = new File(userFilePath);
        if (file.exists()) {
            if (file.isFile()) {
                // Check if the file extension is '.bal'
                if (!userFilePath.endsWith(ProjectConstants.BLANG_SOURCE_EXT)) {
                    this.outputStream.println("Invalid file format received!\n File format should be of type '.bal'");
                    return null;
                } else {
                    // Perform check if the user has provided the file in "./balFileName.bal" format and if so remove
                    // the trailing slash
                    if (userFilePath.startsWith("./") || userFilePath.startsWith(".\\")) {
                        userFilePath = userFilePath.substring(2);
                    }

                    return userFilePath;
                }
            } else {
                // If it's a directory, validate it's a ballerina build project
                File ballerinaTomlFile = new File(userFilePath, ProjectConstants.BALLERINA_TOML);
                if (!ballerinaTomlFile.exists() || !ballerinaTomlFile.isFile()) {
                    this.outputStream.println("ballerina: Invalid Ballerina package directory: " +
                            userFilePath +
                            ", cannot find 'Ballerina.toml' file.");
                    return null;
                } else {
                    // Following is to mitigate the issue when "." is encountered in the scanning process
                    if (userFilePath.equals(".")) {
                        return Path.of(userFilePath)
                                .toAbsolutePath()
                                .getParent()
                                .toString();
                    }

                    return userFilePath;
                }
            }
        } else {
            this.outputStream.println("No such file or directory exists!\n Please check the file path and" +
                    "then re-run the command.");
            return null;
        }
    }
}