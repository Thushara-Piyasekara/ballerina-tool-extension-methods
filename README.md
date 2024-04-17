[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=Xenowa_ballerina-tool-extension-methods)](https://sonarcloud.io/summary/new_code?id=Xenowa_ballerina-tool-extension-methods)
[![codecov](https://codecov.io/gh/Xenowa/ballerina-tool-extension-methods/graph/badge.svg?token=CMYX8SZFDK)](https://codecov.io/gh/Xenowa/ballerina-tool-extension-methods)

# Architecture: Compiler Diagnostics based Approach

```mermaid
sequenceDiagram
    participant Bridge Tool
    participant MyScannerPlugin extends CompilerPlugin
    activate Bridge Tool
    Bridge Tool ->> Bridge Tool: Perform core scans
    Bridge Tool ->> MyScannerPlugin extends CompilerPlugin: Engage through package compilation
    activate MyScannerPlugin extends CompilerPlugin
    MyScannerPlugin extends CompilerPlugin ->> MyScannerPlugin extends CompilerPlugin: Report issues as diagnostics
    MyScannerPlugin extends CompilerPlugin ->> Bridge Tool: Get diagnostics
    deactivate MyScannerPlugin extends CompilerPlugin
    Bridge Tool ->> Bridge Tool: Filter Issue diagnostics
    Bridge Tool ->> Bridge Tool: Add Filtered issues to all issues array
    deactivate Bridge Tool
```

# Ballerina Tool extension methods

Ballerina language has two main extension points:

- Ballerina Tools
- Ballerina Compiler Plugins

This project aims to extend the functionalities of a Ballerina tool via compiler plugins and simulate a
simplified version of the Ballerina scan tool

## Ballerina Tools

Allow developers to create custom Ballerina commands like:

```cmd
bal bridge
```

## Ballerina Compiler Plugins

Allow developers to extend features of the Ballerina compiler

```bal
import org/custom_compiler_plugin as _;

public function main(){
}
```

## Prerequisites

The following software should be installed locally

- [Java version: 17](https://adoptium.net/temurin/releases/?version=17)
- [Ballerina version: 2201.8.5](https://ballerina.io/downloads/archived/#swan-lake-archived-versions)

## Getting started

1. Create the custom compiler plugin by following the
   instructions [here](https://github.com/Xenowa/ballerina-tool-extension-methods/tree/compiler-diagnostics-based-approach/CustomCompilerPlugin)

2. Create the bal bridge tool by following the
   instructions [here](https://github.com/Xenowa/ballerina-tool-extension-methods/tree/compiler-diagnostics-based-approach/BridgeCommand)

3. Navigate to the test-bridge-command directory

4. Run "bal bridge" command
Testing
