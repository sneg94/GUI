## My Changes

- Modified column "Description" to "Constraints".
- Added column "Constraint Desc." to describe individual constraints.
- Updated to use AssumptionGraph REST API for enhanced functionality.

The associated Sub-assumption Analyzer, which leverages the extended AssumptionGraph REST API and conducts data-flow analysis to verify sub-assumptions in order to check whether assumptions are fulfilled, can be found in the following repository: [SubAssumptionAnalyzer](https://github.com/sneg94/DataFlowAnalysis).


# <center>Praktikum: Werkzeuge für Agile Modellierung - SS2023</center>

## <center>Thema: Anforderungsprüfung</center>
## <center>Tim Bächle</center>

## Installation

- Clone this repository
- Let Maven resolve the dependencies (Instructions for IntelliJ IDEA below)
  - Right-click the <code>pom.xml</code> located on the top-level of the repository.
  - Select the <code>Maven</code> option
  - Select <code>Reload project</code>
- Assumption Analyzer can then be started by executing <code>src/main/java/Main.java</code>

## Further Documentation

For further resources please refer to the <code>documentation</code> directory located on the top-level of the repository. It contains the following documents: 

- [Praesentation_Praktikum_WAM.pdf](documentation/Praesentation_Praktikum_WAM.pdf): The slides used for the final presentation on 09/18/23.
- [Praesentation_Praktikum_WAM.zip](documentation/Praesentation_Praktikum_WAM.zip): The LaTeX source files used to create the pdf mentioned above.
- [Ausarbeitung_Praktikum_WAM.pdf](documentation/Ausarbeitung_Praktikum_WAM.pdf): A detailed description of Assumption Analyzer, detailing its goal, architecture, usage, and selected design decisions.
- [Ausarbeitung_Praktikum_WAM.zip](documentation/Ausarbeitung_Praktikum_WAM.zip): The LaTeX source files used to create the pdf mentioned above.

## Quick Links

- The [Abunai microservice repo](https://github.com/TDot305/UncertaintyImpactAnalysis).
